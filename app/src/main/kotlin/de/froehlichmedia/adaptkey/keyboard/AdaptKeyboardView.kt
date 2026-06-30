package de.froehlichmedia.adaptkey.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.touch.OffsetModel

/**
 * Self-drawn keyboard view.
 *
 * Lays each row out horizontally by [Key.weight] (proportions from [proportions], L-02 / L-04)
 * and resolves a tap from the initial contact point only (T-01): the key is decided at
 * [MotionEvent.ACTION_DOWN]; subsequent movement is ignored and does not trigger swipe
 * behaviour. When an [offsetModel] is attached it both refines the resolution (T-03) and is fed
 * the confirmed tap so it keeps learning. The raw down coordinates are also forwarded to the
 * listener for later token-level correction (T-02).
 *
 * The resolved character is emitted on release ([MotionEvent.ACTION_UP]); holding a key past the
 * long-press timeout instead emits its secondary symbol (L-05 / L-06) via [onLongPressListener] and
 * suppresses the tap. Resolution and offset-model learning still happen at ACTION_DOWN, so T-01 / T-03
 * are unaffected.
 */
class AdaptKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    /** Callback invoked once per resolved tap, on release (carrying the ACTION_DOWN coordinates). */
    fun interface OnKeyListener {
        
        fun onKey(key: Key, downX: Float, downY: Float)
    }
    
    /** Callback invoked when a key is held past the long-press timeout (L-05 / L-06). */
    fun interface OnLongPressListener {
        
        fun onLongPress(symbol: String)
    }
    
    var onKeyListener: OnKeyListener? = null
    
    var onLongPressListener: OnLongPressListener? = null
    
    /** Personal offset model (T-03); when null the view resolves taps purely geometrically. */
    var offsetModel: OffsetModel? = null
    
    var shifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    var proportions: KeyProportions = KeyProportions.DEFAULT
        set(value) {
            field = value
            rebuildRows()
        }
    
    var showNumberRow: Boolean = true
        set(value) {
            field = value
            rebuildRows()
        }
    
    /** Per-letter secondary-symbol map drawn as corner hints (L-05 / C-08). */
    var letterHints: Map<Char, String> = KeyboardLayout.DEFAULT_LETTER_HINTS
        set(value) {
            field = value
            rebuildRows()
        }
    
    /** Whether the corner hint glyphs are drawn (C-08); the long-press function stays active either way. */
    var hintsEnabled: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    
    private var rows = KeyboardLayout.rows(proportions, showNumberRow, letterHints)
    private val keyRects = ArrayList<Pair<Key, RectF>>()
    private var pressedKey: Key? = null
    
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressFired = false
    private var downX = 0f
    private var downY = 0f
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
    
    private val rowHeightPx = dp(54f)
    private val gapPx = dp(3f)
    private val keyRadiusPx = dp(6f)
    
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background)
    }
    
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_special)
    }
    
    private val pressedKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_special)
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_text)
        textAlign = Paint.Align.CENTER
        textSize = dp(20f)
    }
    
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_hint)
        textAlign = Paint.Align.RIGHT
        textSize = dp(11f)
    }
    
    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_background))
    }
    
    private fun rebuildRows() {
        rows = KeyboardLayout.rows(proportions, showNumberRow, letterHints)
        if (width > 0) {
            layoutKeys(width)
        }
        requestLayout()
        invalidate()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (rows.size * rowHeightPx + gapPx).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys(w)
    }
    
    private fun layoutKeys(totalWidth: Int) {
        keyRects.clear()
        val left = paddingLeft.toFloat()
        val usableWidth = totalWidth - paddingLeft - paddingRight
        var top = paddingTop.toFloat() + gapPx
        
        for (row in rows) {
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = (usableWidth - gapPx * (row.size + 1)) / totalWeight
            var x = left + gapPx
            for (key in row) {
                val keyWidth = unit * key.weight
                val rect = RectF(x, top, x + keyWidth, top + rowHeightPx - gapPx)
                keyRects.add(key to rect)
                x += keyWidth + gapPx
            }
            top += rowHeightPx
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((key, rect) in keyRects) {
            val paint = when {
                key === pressedKey -> pressedKeyPaint
                key.code == KeyCode.CHAR -> keyPaint
                else -> specialKeyPaint
            }
            canvas.drawRoundRect(rect, keyRadiusPx, keyRadiusPx, paint)
            
            val label = labelFor(key)
            val cx = rect.centerX()
            val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, cx, baseline, textPaint)
            
            val hint = key.hint
            if (hintsEnabled && hint != null) {
                canvas.drawText(hint, rect.right - dp(6f), rect.top + dp(14f), hintPaint)
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            // T-01: the initial contact point is the authoritative tap coordinate.
            MotionEvent.ACTION_DOWN -> {
                val resolved = resolveKey(event.x, event.y) ?: return true
                val (key, rect) = resolved
                pressedKey = key
                longPressFired = false
                downX = event.x
                downY = event.y
                invalidate()
                // T-03: feed the confirmed tap back into the personal offset model.
                offsetModel?.record(key.id, rect.centerX(), rect.centerY(), event.x, event.y)
                scheduleLongPress(key)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Movement does not change the resolved key (T-01); it only cancels the pending long-press.
                if (pressedKey != null && movedBeyondSlop(event.x, event.y)) {
                    cancelPendingLongPress()
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                cancelPendingLongPress()
                val key = pressedKey
                pressedKey = null
                invalidate()
                if (key != null && !longPressFired) {
                    onKeyListener?.onKey(key, downX, downY)
                }
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> {
                cancelPendingLongPress()
                pressedKey = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun scheduleLongPress(key: Key) {
        val symbol = KeyboardLayout.longPressSymbol(key) ?: return
        val runnable = Runnable {
            if (pressedKey === key) {
                longPressFired = true
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onLongPressListener?.onLongPress(symbol)
            }
        }
        longPressRunnable = runnable
        longPressHandler.postDelayed(runnable, longPressTimeoutMs)
    }
    
    private fun cancelPendingLongPress() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
    }
    
    private fun movedBeyondSlop(x: Float, y: Float): Boolean {
        val dx = x - downX
        val dy = y - downY
        return dx * dx + dy * dy > touchSlopPx * touchSlopPx
    }
    
    private fun resolveKey(x: Float, y: Float): Pair<Key, RectF>? {
        if (keyRects.isEmpty()) {
            return null
        }
        val model = offsetModel
        if (model != null) {
            val candidates = keyRects.map { (key, rect) ->
                OffsetModel.Candidate(key.id, rect.centerX(), rect.centerY(), rect.width() / 2f, rect.height() / 2f)
            }
            val chosen = model.resolve(candidates, x, y)
            if (chosen != null) {
                val match = keyRects.firstOrNull { it.first.id == chosen.id }
                if (match != null) {
                    return match
                }
            }
        }
        return keyRects.firstOrNull { it.second.contains(x, y) }
    }
    
    private fun labelFor(key: Key): String {
        val ch = key.char
        return if (key.code == KeyCode.CHAR && shifted && ch != null) {
            ch.uppercaseChar().toString()
        } else {
            key.label
        }
    }
    
    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}

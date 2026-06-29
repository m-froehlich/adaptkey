package de.froehlichmedia.adaptkey.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R

/**
 * Minimal self-drawn keyboard view for the scaffold.
 *
 * Lays each row out horizontally by [Key.weight] and resolves a tap from the initial
 * contact point only (T-01): the key is decided at [MotionEvent.ACTION_DOWN]; subsequent
 * movement is ignored and does not trigger swipe behaviour. The raw down coordinates are
 * forwarded to the listener so the later offset model (T-03) can consume them.
 */
class AdaptKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    /** Callback invoked once per resolved tap, at ACTION_DOWN. */
    fun interface OnKeyListener {
        
        fun onKey(key: Key, downX: Float, downY: Float)
    }
    
    var onKeyListener: OnKeyListener? = null
    
    var shifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    private val rows = KeyboardLayout.rows()
    private val keyRects = ArrayList<Pair<Key, RectF>>()
    private var pressedKey: Key? = null
    
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
            if (hint != null) {
                canvas.drawText(hint, rect.right - dp(6f), rect.top + dp(14f), hintPaint)
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            // T-01: the initial contact point is the authoritative tap coordinate.
            MotionEvent.ACTION_DOWN -> {
                val key = keyAt(event.x, event.y)
                if (key != null) {
                    pressedKey = key
                    invalidate()
                    onKeyListener?.onKey(key, event.x, event.y)
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (pressedKey != null) {
                    pressedKey = null
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun keyAt(x: Float, y: Float): Key? {
        for ((key, rect) in keyRects) {
            if (rect.contains(x, y)) {
                return key
            }
        }
        return null
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

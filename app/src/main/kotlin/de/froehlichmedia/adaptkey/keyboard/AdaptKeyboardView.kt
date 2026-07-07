// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import kotlin.math.abs
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.gesture.SwipeDirection
import de.froehlichmedia.adaptkey.gesture.SwipeGesture
import de.froehlichmedia.adaptkey.touch.AmbiguityBands
import de.froehlichmedia.adaptkey.touch.AmbiguityResult
import de.froehlichmedia.adaptkey.touch.KeyBox
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.TapAmbiguity

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
 * long-press timeout instead fires [onLongPressListener] (L-05 / L-06 secondary symbols, or the L-03
 * combined key switching to the numeric/symbol layer) and suppresses the tap. Resolution and
 * offset-model learning still happen at ACTION_DOWN, so T-01 / T-03 are unaffected.
 *
 * [surface] selects which layout is drawn: the letter view or the [SymbolLayout] numeric/symbol
 * layer (L-03); the emoji panel itself is a separate view swapped in by the caller.
 */
class AdaptKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    /** Callback invoked once per resolved tap, on release (carrying the ACTION_DOWN coordinates). */
    fun interface OnKeyListener {
        
        fun onKey(key: Key, downX: Float, downY: Float, ambiguity: AmbiguityResult)
    }
    
    /**
     * Callback invoked when a key is held past the long-press timeout (L-05 / L-06 secondary
     * symbols, or the L-03 combined key switching to the numeric/symbol layer); see
     * [KeyboardLayout.hasLongPressAction] for which keys this fires on.
     */
    fun interface OnLongPressListener {
        
        fun onLongPress(key: Key)
    }
    
    /**
     * Callback invoked on release when the touch travelled far enough to be a swipe gesture (§4).
     * Returning true marks the swipe as consumed, which suppresses the would-be tap; returning false
     * lets the tap fire normally.
     */
    fun interface OnSwipeListener {
        
        fun onSwipe(key: Key, direction: SwipeDirection): Boolean
    }
    
    /**
     * Callback invoked repeatedly while the backspace key is held (D-07 / D-31). [step] is the 0-based
     * repeat index (step 0 resets the hold). The service performs the deletion and returns the delay in
     * milliseconds before the next repeat, so the cadence follows the character/word-wise phase.
     */
    fun interface OnBackspaceRepeatListener {
        
        fun onBackspaceRepeat(step: Int): Long
    }
    
    /**
     * Callback invoked when a D-01 multi-alternative long-press popup is released on an alternative
     * (finger-tracking selection): carries the pressed key and the chosen alternative string.
     */
    fun interface OnLongPressPopupListener {
        
        fun onLongPressAlternative(key: Key, alternative: String)
    }
    
    var onKeyListener: OnKeyListener? = null
    
    var onLongPressListener: OnLongPressListener? = null
    
    var onSwipeListener: OnSwipeListener? = null
    
    var onBackspaceRepeatListener: OnBackspaceRepeatListener? = null
    
    var onLongPressPopupListener: OnLongPressPopupListener? = null
    
    /**
     * Callback invoked at every ACTION_DOWN with the raw contact point and the resolved key's centre
     * (D-09 diagnostic). Set only by the calibration screen when raw-tap recording is enabled; null (and
     * therefore free) during normal typing.
     */
    fun interface OnRawTapListener {
        
        fun onRawTap(keyId: String, keyCenterX: Float, keyCenterY: Float, tapX: Float, tapY: Float)
    }
    
    var onRawTapListener: OnRawTapListener? = null
    
    /**
     * D-03: the label drawn on the space bar, showing the current input language (e.g. "Deutsch",
     * "English", "Ελληνικά") instead of the word "Space". Set by the service when the language changes.
     */
    var spaceLabel: String = ""
        set(value) {
            field = value
            invalidate()
        }
    
    /** Personal offset model (T-03); when null the view resolves taps purely geometrically. */
    var offsetModel: OffsetModel? = null
    
    var shifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    /** D-15: Caps Lock - persistent uppercase (engaged by a double-tap of Shift) until Shift is pressed again. */
    var capsLock: Boolean = false
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
    
    /** Which layout is drawn (L-03): the letter view or the numeric/symbol layer. */
    var surface: InputSurface = InputSurface.LETTERS
        set(value) {
            field = value
            rebuildRows()
        }
    
    /** The active numeric/symbol layer page (L-03), 1 or 2; only relevant while [surface] is SYMBOLS. */
    var symbolPage: Int = 1
        set(value) {
            field = value
            rebuildRows()
        }
    
    /** Whether the letter surface shows the Greek alphabet (G-01) instead of the Latin QWERTZ layout. */
    var greek: Boolean = false
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
    
    /** D-24: draws the learned touch model (expected strike point + spread per key) as an overlay. */
    var showTouchModel: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    /** D-47: whether the emoji panel is enabled; when off, the combined key shows "?123" instead of 😊. */
    var emojiEnabled: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    
    /** D-05: whether a click sound plays on each key press (default off). */
    var soundEnabled: Boolean = false
    
    /** D-06: whether a short vibration fires on each key press (default off). */
    var hapticsEnabled: Boolean = false
    
    private var rows = KeyboardLayout.rows(proportions, showNumberRow, letterHints)
    private val keyRects = ArrayList<Pair<Key, RectF>>()
    private var pressedKey: Key? = null
    
    // T-05: classifies a bottom-row tap into the space/letter ambiguity bands. Computed at ACTION_DOWN
    // from the raw contact point and carried to the listener on release for later token-level repair.
    private val ambiguityBands = AmbiguityBands()
    private var pendingAmbiguity = AmbiguityResult(TapAmbiguity.NONE)
    
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressFired = false
    
    // D-04: keeps a just-tapped key highlighted for a brief moment after release, so even a very quick
    // tap (down and up within a frame) produces a visible flash acknowledging the press.
    private var flashKey: Key? = null
    private val flashRunnable = Runnable {
        flashKey = null
        invalidate()
    }
    // D-04 / D-28 (§13/§14): a very short, Gboard-like flash - just long enough to be seen (~2 frames) but
    // short enough that typing feels snappy and a rapid double-tap shows two distinct blinks.
    private val flashDurationMs = 28L
    
    // D-07: the accelerating backspace-on-hold repeat. Scheduled on ACTION_DOWN of the backspace key and
    // cancelled on release / move; backspaceRepeated suppresses the would-be single-delete tap once at
    // least one repeat has fired, so a hold never double-counts the initial deletion.
    private var backspaceRepeatRunnable: Runnable? = null
    private var backspaceStep = 0
    private var backspaceRepeated = false
    
    private var downX = 0f
    private var downY = 0f
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    
    /**
     * D-32: delay before a long-press fires (the alt char / popup). Set from the settings; defaults to
     * ~20 % below the system long-press timeout so the popup comes up sooner even without a stored value.
     */
    var longPressDelayMs: Long = (ViewConfiguration.getLongPressTimeout() * 0.8f).toLong()
    // D-20 / D-35: field gestures (dismiss-down, surface swipe, word-delete) need a clearly larger travel
    // so a faint motion no longer triggers them; the space-bar language swipe (G-01) stays smaller,
    // proportional to the narrow space bar, but still deliberate.
    private val fieldSwipeThresholdPx = dp(110f)
    private val spaceSwipeThresholdPx = dp(44f)
    
    // D-05: lazily resolved so the AudioManager is only fetched when the sound feedback is actually used.
    // D-05/D-06 (§14 fix): the toggles must be authoritative, so the feedback bypasses the system
    // "touch sounds"/"touch vibration" settings that silenced AudioManager.playSoundEffect and
    // performHapticFeedback on device. Sound = a short ToneGenerator click; haptic = the Vibrator directly.
    private val toneGenerator by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, TONE_VOLUME) }.getOrNull()
    }
    private val vibrator by lazy { context.getSystemService(Vibrator::class.java) }
    
    private val rowHeightPx = dp(54f)
    // D-42: the number row is shorter than the letter rows, so its touch zone does not reach down into the
    // top letter row (where a tap meant for a letter was landing on the digit above it).
    private val numberRowHeightPx = dp(44f)
    // D-21 (§13): a few px of cell padding between keys (Gboard-like) so they read as separate keys.
    private val gapPx = dp(5f)
    private val keyRadiusPx = dp(6f)
    
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background)
    }
    
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_special)
    }
    
    // D-04: a distinct pressed colour (not key_background_special, which the special keys already use at
    // rest) so every key - including the space bar, shift and enter - visibly flashes when pressed.
    private val pressedKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_pressed)
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
    
    // D-01 / D-44: the horizontal long-press popup. Active while popupKey is non-null. The alternatives are
    // a single row of cells above the key, centred over the stem so the pre-selected cell (the key's own
    // character) sits over the finger; sliding the finger left/right below the row changes popupSelectedIndex
    // (the cell above the pointer x), and release commits it.
    private var popupKey: Key? = null
    private var popupAlternatives: List<String> = emptyList()
    private var popupSelectedIndex = 0
    private var popupRowLeft = 0f
    private var popupRowTop = 0f
    private val popupCellWidthPx = dp(40f)
    private val popupCellHeightPx = dp(42f)
    
    private val popupBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background)
    }
    
    private val popupSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_pressed)
    }
    
    private val popupBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_divider)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    
    // D-24: touch-model overlay - a translucent spread ellipse plus a solid dot at the expected strike point.
    private val touchModelFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x3300796B.toInt()
        style = Paint.Style.FILL
    }
    
    private val touchModelStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8800796B.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    
    private val touchModelDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE0D32F2F.toInt()
        style = Paint.Style.FILL
    }
    
    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_background))
    }
    
    private fun rebuildRows() {
        rows = when (surface) {
            // G-01: the letter surface is either the Latin QWERTZ layout or the Greek alphabet.
            InputSurface.LETTERS -> if (greek) {
                GreekLayout.rows(proportions, showNumberRow)
            } else {
                KeyboardLayout.rows(proportions, showNumberRow, letterHints)
            }
            
            InputSurface.SYMBOLS -> SymbolLayout.rows(symbolPage, proportions)
            // The emoji panel is a separate view; this surface is never actually drawn.
            InputSurface.EMOJI -> emptyList()
        }
        if (width > 0) {
            layoutKeys(width)
        }
        requestLayout()
        invalidate()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rowsHeight = rows.sumOf { rowHeight(it).toDouble() }.toFloat()
        val height = (rowsHeight + gapPx).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }
    
    /** D-42: the (shorter) number row versus a regular row. */
    private fun rowHeight(row: List<Key>): Float {
        return if (isNumberRow(row)) numberRowHeightPx else rowHeightPx
    }
    
    private fun isNumberRow(row: List<Key>): Boolean {
        return row.isNotEmpty() && row.all { it.code == KeyCode.CHAR && it.char?.isDigit() == true }
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
            val height = rowHeight(row)
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = (usableWidth - gapPx * (row.size + 1)) / totalWeight
            var x = left + gapPx
            for (key in row) {
                val keyWidth = unit * key.weight
                val rect = RectF(x, top, x + keyWidth, top + height - gapPx)
                keyRects.add(key to rect)
                x += keyWidth + gapPx
            }
            top += height
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((key, rect) in keyRects) {
            val paint = when {
                key === pressedKey || key === flashKey -> pressedKeyPaint
                key.code == KeyCode.CHAR -> keyPaint
                else -> specialKeyPaint
            }
            canvas.drawRoundRect(rect, keyRadiusPx, keyRadiusPx, paint)
            
            val label = labelFor(key)
            val cx = rect.centerX()
            val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, cx, baseline, textPaint)
            
            val hint = key.hint
            // D-47: no "123" corner hint on the combined key when it already reads "?123" (emoji off).
            val suppressHint = key.code == KeyCode.SYMBOL && !emojiEnabled
            if (hintsEnabled && hint != null && !suppressHint) {
                canvas.drawText(hint, rect.right - dp(6f), rect.top + dp(14f), hintPaint)
            }
        }
        if (showTouchModel) {
            drawTouchModel(canvas)
        }
        drawLongPressPopup(canvas)
    }
    
    /**
     * D-24: draws the learned touch model over the keys - for each trained character key, a translucent
     * ellipse of the tap spread (per-axis std deviation) centred on the expected strike point (key centre
     * plus the learned mean offset), with a solid dot at that point.
     */
    private fun drawTouchModel(canvas: Canvas) {
        val model = offsetModel ?: return
        val minRadius = dp(3f)
        for ((key, rect) in keyRects) {
            if (key.code != KeyCode.CHAR) {
                continue
            }
            val spread = model.spreadFor(key.id) ?: continue
            val cx = rect.centerX() + spread.meanDx.toFloat()
            val cy = rect.centerY() + spread.meanDy.toFloat()
            val rx = maxOf(spread.stdDevX.toFloat(), minRadius)
            val ry = maxOf(spread.stdDevY.toFloat(), minRadius)
            val oval = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
            canvas.drawOval(oval, touchModelFillPaint)
            canvas.drawOval(oval, touchModelStrokePaint)
            canvas.drawCircle(cx, cy, dp(2.5f), touchModelDotPaint)
        }
    }
    
    /**
     * D-44: draws the horizontal long-press popup (when active) on top of the keys - the alternatives in a
     * single row above the key, left to right, with the selected cell highlighted.
     */
    private fun drawLongPressPopup(canvas: Canvas) {
        if (popupKey == null || popupAlternatives.isEmpty()) {
            return
        }
        popupAlternatives.forEachIndexed { index, text ->
            val left = popupRowLeft + index * popupCellWidthPx
            drawPopupCell(canvas, left, popupRowTop, text, index == popupSelectedIndex)
        }
    }
    
    private fun drawPopupCell(canvas: Canvas, left: Float, top: Float, text: String, selected: Boolean) {
        val cell = RectF(left, top, left + popupCellWidthPx, top + popupCellHeightPx)
        canvas.drawRoundRect(cell, keyRadiusPx, keyRadiusPx, popupBackgroundPaint)
        if (selected) {
            canvas.drawRoundRect(cell, keyRadiusPx, keyRadiusPx, popupSelectedPaint)
        }
        canvas.drawRoundRect(cell, keyRadiusPx, keyRadiusPx, popupBorderPaint)
        val baseline = cell.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, cell.centerX(), baseline, textPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            // T-01: the initial contact point is the authoritative tap coordinate.
            MotionEvent.ACTION_DOWN -> {
                val resolved = resolveKey(event.x, event.y) ?: return true
                val (key, rect) = resolved
                cancelFlash()
                dismissPopup()
                // D-30: clear the backspace-repeat suppression from any previous hold, so a new touch is
                // never wrongly swallowed (otherwise the keyboard freezes after a held backspace).
                backspaceRepeated = false
                // D-05 / D-06: optional press feedback (both default off).
                playKeyFeedback()
                pressedKey = key
                longPressFired = false
                downX = event.x
                downY = event.y
                invalidate()
                // T-03: feed the confirmed tap back into the personal offset model.
                // event.size (T-04) lets the model track contact area for typing-pattern detection.
                offsetModel?.record(key.id, rect.centerX(), rect.centerY(), event.x, event.y, event.size)
                // D-09: forward the raw tap + resolved key centre for the calibration diagnostic (opt-in).
                onRawTapListener?.onRawTap(key.id, rect.centerX(), rect.centerY(), event.x, event.y)
                // T-05: classify the raw contact point into the space/letter ambiguity bands.
                pendingAmbiguity = ambiguityBands.classify(key.id, event.x, event.y, bottomLetterBoxes(), spaceBox(), offsetModel)
                scheduleLongPress(key)
                // D-07: holding the backspace key starts an accelerating repeat delete.
                if (key.code == KeyCode.DELETE) {
                    scheduleBackspaceRepeat()
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // D-44: while the alternatives popup is open the finger slides left/right to pick a cell.
                if (popupKey != null) {
                    updatePopupSelection(event.x)
                    return true
                }
                // Movement does not change the resolved key (T-01); it only cancels the pending long-press
                // and any backspace repeat (a swipe on backspace is a word-delete gesture, not a hold).
                if (pressedKey != null && movedBeyondSlop(event.x, event.y)) {
                    cancelPendingLongPress()
                    cancelBackspaceRepeat()
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                cancelPendingLongPress()
                cancelBackspaceRepeat()
                // D-01: releasing over the alternatives popup commits the highlighted alternative.
                if (popupKey != null) {
                    commitPopupSelection()
                    pressedKey = null
                    invalidate()
                    return true
                }
                val key = pressedKey
                pressedKey = null
                invalidate()
                // D-07: once a backspace hold has repeated, the release must not also fire a tap delete.
                if (key != null && !longPressFired && !backspaceRepeated) {
                    // §4 / D-20: a swipe is offered to the listener only once it clears the per-gesture
                    // threshold (small for the space-bar language swipe, large for the field gestures); if
                    // consumed it suppresses the tap, otherwise the resolved key is emitted as usual (T-01).
                    val consumed = resolveSwipe(key, event.x - downX, event.y - downY)
                    if (!consumed) {
                        // D-04: briefly flash the key so even a fast tap is visibly acknowledged.
                        flash(key)
                        onKeyListener?.onKey(key, downX, downY, pendingAmbiguity)
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> {
                cancelPendingLongPress()
                cancelBackspaceRepeat()
                dismissPopup()
                pressedKey = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun scheduleLongPress(key: Key) {
        if (!KeyboardLayout.hasLongPressAction(key)) {
            return
        }
        val runnable = Runnable {
            if (pressedKey === key) {
                longPressFired = true
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                // D-01 / D-14: any key with an alternative shows the popup - a visible on-keyboard preview
                // (Gboard-style) that confirms the long-press and commits on release. A single-alternative
                // key shows a one-cell popup; a key with no alternative (the combined ?123 key) falls back
                // to its listener action (switching to the numeric/symbol layer).
                val alternatives = popupAlternativesFor(key)
                if (alternatives.isNotEmpty()) {
                    openPopup(key, alternatives)
                } else {
                    onLongPressListener?.onLongPress(key)
                }
            }
        }
        longPressRunnable = runnable
        longPressHandler.postDelayed(runnable, longPressDelayMs)
    }
    
    /**
     * The long-press popup alternatives for [key] (D-01 / D-14): its explicit [Key.alternatives] when
     * present, otherwise its single [Key.hint] as a one-item list for a character key. Empty for a key
     * with no secondary (e.g. the combined ?123 key), which falls back to its listener action.
     *
     * @param key the pressed key
     * @return the alternatives to show in the popup, or empty when there is none
     */
    private fun popupAlternativesFor(key: Key): List<String> {
        return when {
            key.alternatives.isNotEmpty() -> key.alternatives
            key.code == KeyCode.CHAR && key.hint != null -> listOf(key.hint)
            else -> emptyList()
        }
    }
    
    /**
     * D-44: opens the horizontal long-press popup for [key] showing [alternatives] as a row of cells above
     * the key. The row is centred over the stem so the pre-selected cell (the key's own character, or the
     * first alternative when the key's character is not among them) sits over the finger; the row is clamped
     * to stay within the view.
     */
    private fun openPopup(key: Key, alternatives: List<String>) {
        val rect = keyRects.firstOrNull { it.first === key }?.second ?: return
        val preSelected = preSelectedIndexFor(key, alternatives)
        val rowWidth = alternatives.size * popupCellWidthPx
        val maxLeft = (width - gapPx - rowWidth).coerceAtLeast(gapPx)
        popupRowLeft = HorizontalLongPressPopup.rowLeft(rect.centerX(), popupCellWidthPx, alternatives.size, preSelected, gapPx, maxLeft)
        popupRowTop = rect.top - gapPx - popupCellHeightPx
        popupAlternatives = alternatives
        popupSelectedIndex = preSelected
        popupKey = key
        invalidate()
    }
    
    /**
     * The pre-selected cell for [key]'s [alternatives] (D-44): the index of the key's own character, so a
     * straight-up release types the key's normal glyph; falls back to the first alternative (index 0) when
     * the key's character is not in the list (e.g. a letter whose only alternative is its umlaut).
     */
    private fun preSelectedIndexFor(key: Key, alternatives: List<String>): Int {
        val self = key.char?.toString() ?: return 0
        val index = alternatives.indexOf(self)
        return if (index < 0) 0 else index
    }
    
    private fun updatePopupSelection(pointerX: Float) {
        if (popupKey == null || popupAlternatives.isEmpty()) {
            return
        }
        val next = HorizontalLongPressPopup.selectedIndex(pointerX, popupRowLeft, popupCellWidthPx, popupAlternatives.size)
        if (next != popupSelectedIndex) {
            popupSelectedIndex = next
            invalidate()
        }
    }
    
    private fun commitPopupSelection() {
        val key = popupKey
        val alternative = popupAlternatives.getOrNull(popupSelectedIndex)
        dismissPopup()
        if (key != null && alternative != null) {
            onLongPressPopupListener?.onLongPressAlternative(key, alternative)
        }
    }
    
    private fun dismissPopup() {
        if (popupKey != null) {
            popupKey = null
            popupAlternatives = emptyList()
            invalidate()
        }
    }
    
    private fun cancelPendingLongPress() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
    }
    
    /**
     * D-04: highlights [key] for a brief moment after a tap, then repaints, so a quick press produces a
     * visible flash. Replaces any previous lingering flash.
     */
    private fun flash(key: Key) {
        longPressHandler.removeCallbacks(flashRunnable)
        flashKey = key
        invalidate()
        longPressHandler.postDelayed(flashRunnable, flashDurationMs)
    }
    
    private fun cancelFlash() {
        longPressHandler.removeCallbacks(flashRunnable)
        flashKey = null
    }
    
    /**
     * D-05 / D-06: plays the optional key-press feedback, each gated by its own setting (both default
     * off). [performHapticFeedback] routes through the window system, so no VIBRATE permission is needed
     * (keeping AdaptKey's minimal-permission stance).
     */
    private fun playKeyFeedback() {
        if (soundEnabled) {
            // A short click tone on the system stream - audible regardless of the system touch-sound setting.
            runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS) }
        }
        if (hapticsEnabled) {
            val v = vibrator
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(HAPTIC_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
    
    /**
     * D-07: schedules the accelerating backspace repeat. The first repeat fires after
     * [BackspaceRepeat.INITIAL_DELAY_MS]; each tick reports its step to the listener (which performs the
     * deletion) and reschedules at the next, shorter [BackspaceRepeat] interval until the key is released.
     */
    private fun scheduleBackspaceRepeat() {
        backspaceStep = 0
        backspaceRepeated = false
        val runnable = object : Runnable {
            override fun run() {
                if (pressedKey?.code != KeyCode.DELETE) {
                    return
                }
                backspaceRepeated = true
                // D-31: the service deletes and returns the delay before the next repeat (char/word phase).
                val next = onBackspaceRepeatListener?.onBackspaceRepeat(backspaceStep) ?: return
                backspaceStep++
                longPressHandler.postDelayed(this, next)
            }
        }
        backspaceRepeatRunnable = runnable
        longPressHandler.postDelayed(runnable, BackspaceRepeat.INITIAL_DELAY_MS)
    }
    
    private fun cancelBackspaceRepeat() {
        backspaceRepeatRunnable?.let { longPressHandler.removeCallbacks(it) }
        backspaceRepeatRunnable = null
    }
    
    private fun movedBeyondSlop(x: Float, y: Float): Boolean {
        val dx = x - downX
        val dy = y - downY
        return dx * dx + dy * dy > touchSlopPx * touchSlopPx
    }
    
    /**
     * Resolves a release displacement into a swipe and offers it to the listener (§4 / D-20). The
     * direction is first detected with the small threshold, then the dominant-axis travel must clear the
     * threshold that applies to that gesture: the small [spaceSwipeThresholdPx] for the space-bar
     * language swipe (G-01), the larger [fieldSwipeThresholdPx] for every field gesture (dismiss-down,
     * surface swipe, word-delete). Returns true when the listener consumed the swipe.
     *
     * @param key the key the swipe started on
     * @param dx the horizontal release displacement
     * @param dy the vertical release displacement
     * @return true when the swipe was recognised and consumed (suppressing the tap)
     */
    private fun resolveSwipe(key: Key, dx: Float, dy: Float): Boolean {
        val direction = SwipeGesture.classify(dx, dy, spaceSwipeThresholdPx)
        if (direction == SwipeDirection.NONE) {
            return false
        }
        val horizontal = direction == SwipeDirection.LEFT || direction == SwipeDirection.RIGHT
        val travel = if (horizontal) abs(dx) else abs(dy)
        val isLanguageSwipe = key.code == KeyCode.SPACE && horizontal
        // D-46: a field swipe must travel roughly three key-widths before it counts, so a short drift no
        // longer flips the page. A key-width is a tenth of the keyboard width (the top row holds ten keys);
        // fall back to the fixed distance until the view has been measured.
        val fieldRequired = if (width > 0) maxOf(fieldSwipeThresholdPx, 3f * (width / 10f)) else fieldSwipeThresholdPx
        val required = if (isLanguageSwipe) spaceSwipeThresholdPx else fieldRequired
        if (travel < required) {
            return false
        }
        return onSwipeListener?.onSwipe(key, direction) == true
    }
    
    private fun bottomLetterBoxes(): List<KeyBox> {
        return keyRects
            .filter { (key, _) -> key.code == KeyCode.CHAR && key.char in BOTTOM_ROW_LETTERS }
            .map { (key, rect) -> KeyBox(key.id, key.char, rect.left, rect.top, rect.right, rect.bottom) }
    }
    
    private fun spaceBox(): KeyBox? {
        val match = keyRects.firstOrNull { (key, _) -> key.code == KeyCode.SPACE } ?: return null
        val (key, rect) = match
        return KeyBox(key.id, key.char, rect.left, rect.top, rect.right, rect.bottom)
    }
    
    /**
     * Geometry of the char keys, for typing-pattern detection (T-04). Empty until the view is laid out.
     *
     * @return one [OffsetModel.Candidate] per char key, with the centre and half-size in view pixels
     */
    fun charKeyGeometry(): List<OffsetModel.Candidate> {
        return keyRects
            .filter { (key, _) -> key.code == KeyCode.CHAR }
            .map { (key, rect) -> OffsetModel.Candidate(key.id, rect.centerX(), rect.centerY(), rect.width() / 2f, rect.height() / 2f) }
    }
    
    private fun resolveKey(x: Float, y: Float): Pair<Key, RectF>? {
        if (keyRects.isEmpty()) {
            return null
        }
        // D-27: a tap geometrically inside the (large) space bar always resolves to space - the personal
        // offset model must not pull a clear space tap, e.g. one near the space bar's top edge, up to the
        // letter above it (c / v ...). The T-05 letter-ambiguity flag is still computed separately.
        val spaceHit = keyRects.firstOrNull { it.first.code == KeyCode.SPACE && it.second.contains(x, y) }
        if (spaceHit != null) {
            return spaceHit
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
        return when {
            // D-03: the space bar shows the current input language instead of its layout label.
            key.code == KeyCode.SPACE && spaceLabel.isNotEmpty() -> spaceLabel
            // D-15: the Shift key shows a lock glyph while Caps Lock is engaged.
            key.code == KeyCode.SHIFT && capsLock -> "⇪"
            // D-47: with the emoji panel off, the combined key is a plain ?123 key, not the emoji.
            key.code == KeyCode.SYMBOL && !emojiEnabled -> "?123"
            key.code == KeyCode.CHAR && (shifted || capsLock) && ch != null -> ch.uppercaseChar().toString()
            else -> key.label
        }
    }
    
    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // D-05: release the tone generator's audio resources when the view goes away.
        runCatching { toneGenerator?.release() }
    }
    
    companion object {
        
        // The bottom letter keys that sit directly above the space bar (T-05 ambiguity zone).
        private val BOTTOM_ROW_LETTERS = setOf('c', 'v', 'b', 'n', 'm')
        
        // D-05: key-click tone volume (0-100) and duration; D-06: haptic pulse duration (D-34: long enough
        // to actually be felt - a very short pulse was imperceptible).
        private const val TONE_VOLUME = 70
        private const val TONE_DURATION_MS = 40
        private const val HAPTIC_DURATION_MS = 40L
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
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
     * Callback invoked repeatedly while the backspace key is held (D-07), at the accelerating cadence
     * of [BackspaceRepeat]. [step] is the 0-based repeat index so the service can decide between
     * character- and word-wise deletion.
     */
    fun interface OnBackspaceRepeatListener {
        
        fun onBackspaceRepeat(step: Int)
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
    private val flashDurationMs = 80L
    
    // D-07: the accelerating backspace-on-hold repeat. Scheduled on ACTION_DOWN of the backspace key and
    // cancelled on release / move; backspaceRepeated suppresses the would-be single-delete tap once at
    // least one repeat has fired, so a hold never double-counts the initial deletion.
    private var backspaceRepeatRunnable: Runnable? = null
    private var backspaceStep = 0
    private var backspaceRepeated = false
    
    private var downX = 0f
    private var downY = 0f
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
    // A swipe must travel clearly past the tap jitter slop before it is treated as a gesture (§4).
    private val swipeThresholdPx = dp(36f)
    
    // D-05: lazily resolved so the AudioManager is only fetched when the sound feedback is actually used.
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    
    private val rowHeightPx = dp(54f)
    private val gapPx = dp(3f)
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
    
    // D-01: the multi-alternative long-press popup. Active while popupKey is non-null; the finger slides
    // over the equal-width cells to change popupSelectedIndex, and releasing commits the highlighted one.
    private var popupKey: Key? = null
    private var popupAlternatives: List<String> = emptyList()
    private var popupSelectedIndex = 0
    private var popupLeft = 0f
    private var popupTop = 0f
    private var popupCellWidth = 0f
    private val popupCellWidthPx = dp(40f)
    private val popupHeightPx = dp(46f)
    
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
            if (hintsEnabled && hint != null) {
                canvas.drawText(hint, rect.right - dp(6f), rect.top + dp(14f), hintPaint)
            }
        }
        drawLongPressPopup(canvas)
    }
    
    /**
     * D-01: draws the multi-alternative long-press popup (when active) on top of the keys - a rounded
     * bar of equal-width cells above the pressed key, with the currently selected alternative highlighted.
     */
    private fun drawLongPressPopup(canvas: Canvas) {
        if (popupKey == null || popupAlternatives.isEmpty()) {
            return
        }
        val width = popupCellWidth * popupAlternatives.size
        val bounds = RectF(popupLeft, popupTop, popupLeft + width, popupTop + popupHeightPx)
        canvas.drawRoundRect(bounds, keyRadiusPx, keyRadiusPx, popupBackgroundPaint)
        popupAlternatives.forEachIndexed { index, alt ->
            val cellLeft = popupLeft + index * popupCellWidth
            val cell = RectF(cellLeft, popupTop, cellLeft + popupCellWidth, popupTop + popupHeightPx)
            if (index == popupSelectedIndex) {
                canvas.drawRoundRect(cell, keyRadiusPx, keyRadiusPx, popupSelectedPaint)
            }
            val baseline = cell.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(alt, cell.centerX(), baseline, textPaint)
        }
        canvas.drawRoundRect(bounds, keyRadiusPx, keyRadiusPx, popupBorderPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            // T-01: the initial contact point is the authoritative tap coordinate.
            MotionEvent.ACTION_DOWN -> {
                val resolved = resolveKey(event.x, event.y) ?: return true
                val (key, rect) = resolved
                cancelFlash()
                dismissPopup()
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
                // D-01: while the alternatives popup is open the finger slides to pick a cell, not swipe.
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
                    // §4: a swipe past the gesture threshold is offered to the listener; if consumed it
                    // suppresses the tap, otherwise the resolved key is emitted as usual (T-01).
                    val direction = SwipeGesture.classify(event.x - downX, event.y - downY, swipeThresholdPx)
                    val consumed = direction != SwipeDirection.NONE &&
                        onSwipeListener?.onSwipe(key, direction) == true
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
                // D-01: a key with two or more alternatives opens the finger-tracking popup instead of
                // applying a single secondary immediately.
                if (key.alternatives.size >= 2) {
                    openPopup(key)
                } else {
                    onLongPressListener?.onLongPress(key)
                }
            }
        }
        longPressRunnable = runnable
        longPressHandler.postDelayed(runnable, longPressTimeoutMs)
    }
    
    /**
     * D-01: opens the multi-alternative popup above [key]. The cells are equal-width and centred over
     * the key, clamped to stay within the view; the most common alternative (index 0) is pre-selected.
     */
    private fun openPopup(key: Key) {
        val rect = keyRects.firstOrNull { it.first === key }?.second ?: return
        val count = key.alternatives.size
        // Shrink the cells if the row of alternatives would be wider than the view.
        val usable = (width - gapPx * 2f)
        popupCellWidth = minOf(popupCellWidthPx, usable / count)
        val popupWidth = popupCellWidth * count
        val centred = rect.centerX() - popupWidth / 2f
        popupLeft = centred.coerceIn(gapPx, (width - gapPx - popupWidth).coerceAtLeast(gapPx))
        popupTop = (rect.top - popupHeightPx - gapPx).coerceAtLeast(0f)
        popupAlternatives = key.alternatives
        popupSelectedIndex = 0
        popupKey = key
        invalidate()
    }
    
    private fun updatePopupSelection(pointerX: Float) {
        if (popupKey == null) {
            return
        }
        val next = LongPressPopup.selectedIndex(pointerX, popupLeft, popupCellWidth, popupAlternatives.size)
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
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }
        if (hapticsEnabled) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
                onBackspaceRepeatListener?.onBackspaceRepeat(backspaceStep)
                val next = BackspaceRepeat.nextDelayMs(backspaceStep)
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
            key.code == KeyCode.CHAR && shifted && ch != null -> ch.uppercaseChar().toString()
            else -> key.label
        }
    }
    
    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
    
    companion object {
        
        // The bottom letter keys that sit directly above the space bar (T-05 ambiguity zone).
        private val BOTTOM_ROW_LETTERS = setOf('c', 'v', 'b', 'n', 'm')
    }
}

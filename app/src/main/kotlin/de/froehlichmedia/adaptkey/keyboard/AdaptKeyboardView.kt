// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    
    /**
     * D-58: switches to [newSurface] (and, for the numeric/symbol layer, [newSymbolPage]) with a
     * perceptible slide, instead of the instant [surface] / [symbolPage] swap - the plain redraw was easy
     * to miss. [forward] mirrors the swipe/tap that triggered the change - matching a page-swipe's usual
     * "content follows the finger" feel (D-76): a forward transition (a right swipe / D-19 next, or
     * drilling into the numeric layer) slides the outgoing page off to the **right** and brings the new
     * page in from the **left**; backward is the mirror image. A no-op switch (already showing the target)
     * is skipped, and the very first switch - before the view has a width to animate across - falls back to
     * an immediate change.
     *
     * D-76: [surface] / [symbolPage]'s own `requestLayout()` (via [rebuildRows]) is deferred until the
     * animation ends. Letters and the numeric/symbol layer do not have the same row count (the number row
     * is optional on letters but always shown on symbols), so applying the resize immediately would jump
     * the whole view's height - and everything below the shortened rows, most visibly the bottom-most space
     * row - out from under the still-running slide.
     *
     * @param newSurface the surface to show
     * @param newSymbolPage the numeric/symbol page to show; only meaningful when [newSurface] is
     *        [InputSurface.SYMBOLS]
     * @param forward true when the new page conceptually lies "ahead" of the current one
     */
    fun switchPage(newSurface: InputSurface, newSymbolPage: Int = 1, forward: Boolean = true) {
        if (newSurface == surface && (newSurface != InputSurface.SYMBOLS || newSymbolPage == symbolPage)) {
            return
        }
        if (width <= 0) {
            surface = newSurface
            symbolPage = newSymbolPage
            return
        }
        slideAnimator?.cancel()
        val outgoing = ArrayList(keyRects)
        deferRequestLayout = true
        surface = newSurface
        symbolPage = newSymbolPage
        deferRequestLayout = false
        slideOutKeyRects = outgoing
        // D-76: forward slides the outgoing page off to the right (matching a right/"next" swipe dragging
        // it away) and brings the new page in from the left - see drawKeys()'s use of slideSign.
        slideSign = if (forward) -1f else 1f
        slideT = 0f
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = SLIDE_DURATION_MS
        animator.addUpdateListener {
            slideT = it.animatedValue as Float
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                slideAnimator = null
                slideOutKeyRects = emptyList()
                // D-76: the resize deferred above is applied now that the slide has finished.
                requestLayout()
                invalidate()
            }
        })
        slideAnimator = animator
        animator.start()
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
    
    /**
     * D-59: whether the combined ?123 key offers its numeric/symbol function. When this is off **and**
     * [emojiEnabled] is off, the combined key disappears (drawn blank, taps ignored) but keeps its slot.
     */
    var symbolKeyEnabled: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    
    /** D-55: extra vertical spacing (dp) below the number row. */
    var extraSpaceBelowNumberRowDp: Int = 7
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    
    /** D-55: extra vertical spacing (dp) above the space/enter row. */
    var extraSpaceAboveSpaceRowDp: Int = 7
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    
    /** D-05: whether a click sound plays on each key press (default off). */
    var soundEnabled: Boolean = false
    
    /** D-06: whether a short vibration fires on each key press (default off). */
    var hapticsEnabled: Boolean = false
    
    private var rows = KeyboardLayout.rows(proportions, showNumberRow, letterHints)
    private val keyRects = ArrayList<Pair<Key, RectF>>()
    private var pressedKey: Key? = null
    
    // D-58: the perceptible slide animation for a surface/page change (see switchPage()). slideOutKeyRects
    // holds the outgoing page's geometry for the duration of the animation; empty when no slide is active.
    private var slideAnimator: ValueAnimator? = null
    private var slideOutKeyRects: List<Pair<Key, RectF>> = emptyList()
    private var slideSign = 1f
    private var slideT = 0f
    // D-76: while true, requestLayout() (below) is a no-op - see switchPage()'s KDoc for why.
    private var deferRequestLayout = false
    
    override fun requestLayout() {
        if (deferRequestLayout) {
            return
        }
        super.requestLayout()
    }
    
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
    
    // D-05: lazily resolved so SoundPool is only built when the sound feedback is actually used. D-05/D-06
    // (§14 fix): the toggles must be authoritative, so the feedback bypasses the system "touch sounds" /
    // "touch vibration" settings that silenced AudioManager.playSoundEffect and performHapticFeedback on
    // device. Sound is now a short, bundled typewriter-click sample (D-70) instead of a synthesised
    // ToneGenerator DTMF-style beep, which cannot render anything but a pure/dual sine tone; haptic is the
    // Vibrator directly (below).
    private val soundPool by lazy {
        runCatching {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder().setMaxStreams(SOUND_MAX_STREAMS).setAudioAttributes(attributes).build()
        }.getOrNull()
    }
    
    // D-70: SoundPool.load() decodes off-thread; a key pressed before it finishes must not crash or play
    // garbage, so playback is gated on this flag rather than assuming the id is ready once loaded() returns.
    private var clickSoundId = 0
    private var clickSoundLoaded = false
    // D-66: Context.getSystemService(Vibrator::class.java) is deprecated from API 31 (S) onward in favour
    // of VibratorManager.getDefaultVibrator() - on some OEM builds targeting the newer API the legacy path
    // silently produced no vibration at all, which matches the on-device symptom (D-06/D-34's amplitude and
    // duration tuning never mattered because nothing reached the hardware).
    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
        }.getOrNull()
    }
    
    private val rowHeightPx = dp(54f)
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
    // D-54: how far a single-cell popup is nudged towards the keyboard centre so the finger does not hide it
    // (5 units - a little less than the D-55 space-row gap; a half-cell was too much).
    private val popupSingleNudgePx = dp(5f)
    
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
        val height = (rows.size * rowHeightPx + gapPx + extraSpacingPx()).toInt() + paddingTop + paddingBottom
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
            // D-55: extra gap directly above the space/enter row.
            if (isSpaceRow(row)) {
                top += dp(extraSpaceAboveSpaceRowDp.toFloat())
            }
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
            // D-55: extra gap directly below the number row.
            if (isNumberRow(row)) {
                top += dp(extraSpaceBelowNumberRowDp.toFloat())
            }
        }
    }
    
    /** D-55: total extra vertical spacing added by the number-row / space-row gaps, in pixels. */
    private fun extraSpacingPx(): Float {
        var extra = 0f
        for (row in rows) {
            if (isNumberRow(row)) {
                extra += dp(extraSpaceBelowNumberRowDp.toFloat())
            }
            if (isSpaceRow(row)) {
                extra += dp(extraSpaceAboveSpaceRowDp.toFloat())
            }
        }
        return extra
    }
    
    private fun isNumberRow(row: List<Key>): Boolean {
        return row.isNotEmpty() && row.all { it.code == KeyCode.CHAR && it.char?.isDigit() == true }
    }
    
    private fun isSpaceRow(row: List<Key>): Boolean {
        return row.any { it.code == KeyCode.SPACE }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // D-58: while a page switch is animating, the outgoing page slides out and the current (new) page
        // slides in from the opposite edge; slideSign carries the direction (see switchPage()).
        if (slideOutKeyRects.isNotEmpty()) {
            canvas.save()
            canvas.translate(-slideT * width * slideSign, 0f)
            drawKeys(canvas, slideOutKeyRects)
            canvas.restore()
            canvas.save()
            canvas.translate((1f - slideT) * width * slideSign, 0f)
            drawKeys(canvas, keyRects)
            canvas.restore()
        } else {
            drawKeys(canvas, keyRects)
        }
        if (showTouchModel) {
            drawTouchModel(canvas)
        }
        drawLongPressPopup(canvas)
    }
    
    private fun drawKeys(canvas: Canvas, rects: List<Pair<Key, RectF>>) {
        for ((key, rect) in rects) {
            // D-59: the disabled combined key is drawn as empty space, keeping its slot (neighbours do not grow).
            if (isHiddenSymbolKey(key)) {
                continue
            }
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
     *
     * D-53: the row always sits above the key. For the number row this reaches above the keyboard into the
     * suggestion-bar area; the service disables child-clipping on the container/root so it is drawn on top
     * rather than clipped. D-54: a single-cell popup is nudged a few units towards the keyboard centre (right
     * on the left half, left on the right half) so the finger does not cover it.
     */
    private fun openPopup(key: Key, alternatives: List<String>) {
        val rect = keyRects.firstOrNull { it.first === key }?.second ?: return
        val preSelected = preSelectedIndexFor(key, alternatives)
        val rowWidth = alternatives.size * popupCellWidthPx
        val maxLeft = (width - gapPx - rowWidth).coerceAtLeast(gapPx)
        // D-54: only single-cell popups are nudged; multi-cell popups stay centred over the stem (D-44).
        val nudge = if (alternatives.size == 1) {
            if (rect.centerX() < width / 2f) popupSingleNudgePx else -popupSingleNudgePx
        } else {
            0f
        }
        popupRowLeft = HorizontalLongPressPopup.rowLeft(rect.centerX() + nudge, popupCellWidthPx, alternatives.size, preSelected, gapPx, maxLeft)
        // D-53: always place the row above the key. For the number row this reaches above the keyboard, which
        // is fine: the service disables child-clipping on the container/root, so the popup draws over the
        // suggestion bar rather than being clipped.
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
     * off). Both go straight to hardware - a bundled sample via [SoundPool] and a direct [Vibrator.vibrate]
     * call - bypassing the system "touch sounds" / "touch vibration" toggles that silenced
     * {@code android.media.AudioManager.playSoundEffect} / [performHapticFeedback] on device (D-06/D-34),
     * which is why the app declares the VIBRATE permission itself instead of relying on the window-routed
     * haptic API.
     */
    private fun playKeyFeedback() {
        if (soundEnabled) {
            ensureClickSoundLoaded()
            if (clickSoundLoaded) {
                runCatching { soundPool?.play(clickSoundId, CLICK_VOLUME, CLICK_VOLUME, 1, 0, 1f) }
            }
        }
        if (hapticsEnabled) {
            // D-66: wrapped like the tone generator above - a SecurityException or vendor-specific failure
            // in the vibration path must never take down key handling with it.
            runCatching {
                val v = vibrator
                if (v != null && v.hasVibrator()) {
                    val effect = VibrationEffect.createOneShot(HAPTIC_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
                    // D-75 (D-66 still not firing on device): a plain vibrate(VibrationEffect) with no
                    // attributes falls into an unclassified usage bucket that some OEM vibration-intensity
                    // settings scale to zero independently of the (already-bypassed) "touch vibration"
                    // toggle. USAGE_TOUCH is the category Android itself documents for on-screen-keyboard-
                    // style UI feedback, so it is explicitly requested wherever available (API 33+;
                    // VibrationAttributes does not exist below that).
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
                    } else {
                        v.vibrate(effect)
                    }
                }
            }
        }
    }
    
    /**
     * D-70: kicks off the (idempotent, async) decode of the bundled click sample the first time sound
     * feedback is actually used, rather than eagerly in the constructor - most sessions never enable it.
     * [clickSoundLoaded] only flips once [SoundPool.setOnLoadCompleteListener] confirms the sample is
     * actually ready, so a key pressed in the brief window before that just stays silent instead of risking
     * a play() call on a not-yet-decoded sample.
     */
    private fun ensureClickSoundLoaded() {
        val pool = soundPool ?: return
        if (clickSoundId != 0) {
            return
        }
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == clickSoundId) {
                clickSoundLoaded = true
            }
        }
        clickSoundId = runCatching { pool.load(context, R.raw.key_click, 1) }.getOrDefault(0)
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
     * threshold that applies to that gesture: the small [spaceSwipeThresholdPx] (D-57: +15%) for the
     * space-bar language swipe (G-01), a three-key-width distance (D-46; D-57: -15%) for the horizontal page
     * swipe, and the plain [fieldSwipeThresholdPx] three-key-width distance for the vertical field gestures
     * (dismiss-down, up-to-symbols). Returns true when the listener consumed the swipe.
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
        // D-57: the horizontal page swipe was a touch too wide - shorten it by 15%. The space-bar language
        // swipe, still too easy to trigger by accident, is made 15% harder. Vertical field gestures
        // (swipe-down-to-dismiss, up-to-symbols) keep the plain fieldRequired distance and are untouched.
        val required = when {
            isLanguageSwipe -> spaceSwipeThresholdPx * SPACE_SWIPE_FACTOR
            horizontal -> fieldRequired * PAGE_SWIPE_FACTOR
            else -> fieldRequired
        }
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
                if (match != null && !isHiddenSymbolKey(match.first)) {
                    return match
                }
            }
        }
        // D-59: a tap on the reserved (hidden) combined-key slot resolves to nothing, so it is inert.
        return keyRects.firstOrNull { it.second.contains(x, y) && !isHiddenSymbolKey(it.first) }
    }
    
    /** D-59: whether [key] is the combined ?123 key that is currently disabled (emoji off and symbol off). */
    private fun isHiddenSymbolKey(key: Key): Boolean {
        return key.code == KeyCode.SYMBOL && !emojiEnabled && !symbolKeyEnabled
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
        // D-05 / D-70: release SoundPool's decoded sample and native resources when the view goes away.
        runCatching { soundPool?.release() }
        // D-58: an in-flight page-slide must not keep animating (or later invalidate()) a detached view.
        slideAnimator?.cancel()
    }
    
    companion object {
        
        // The bottom letter keys that sit directly above the space bar (T-05 ambiguity zone).
        private val BOTTOM_ROW_LETTERS = setOf('c', 'v', 'b', 'n', 'm')
        
        // D-05 / D-70: key-click sample playback volume (SoundPool's 0f..1f linear range) and concurrent
        // stream budget (an accidental rapid multi-touch must not drop/queue clicks awkwardly). D-06: haptic
        // pulse duration (D-34: long enough to actually be felt - a very short pulse was imperceptible).
        private const val CLICK_VOLUME = 0.9f
        private const val SOUND_MAX_STREAMS = 4
        private const val HAPTIC_DURATION_MS = 40L
        
        // D-57: the horizontal page swipe needs 15% less travel; the space-bar language swipe needs 15% more.
        private const val PAGE_SWIPE_FACTOR = 0.85f
        private const val SPACE_SWIPE_FACTOR = 1.15f
        
        // D-58: the page-change slide - quick enough to stay snappy, slow enough to actually be seen.
        private const val SLIDE_DURATION_MS = 180L
    }
}

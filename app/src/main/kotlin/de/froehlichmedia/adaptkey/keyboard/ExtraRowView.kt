// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.gesture.SwipeDirection
import de.froehlichmedia.adaptkey.gesture.SwipeGesture

/**
 * §48 / §51 / §69: the swipe-up extra row - the emoji button (left edge), a clear-clipboard button and
 * a settings gear (right edge, clear-clipboard immediately to its left), revealed above the suggestion bar
 * (the topmost row while open) by an upward swipe anywhere on the keyboard (mirroring G-03's
 * downward-dismiss-anywhere).
 *
 * D-189: renamed from `SettingsRowView` - the row grew well past its original settings-button-only purpose
 * (emoji/clear-clipboard/touch-zone/credential-mode/URL-mode buttons all followed), and "settings" as the
 * name kept reading as narrower than what the row actually does, by direct request.
 *
 * D-132 (second pass - the first attempt below didn't fix the reported "pops in" complaint): the row sits
 * in the same parent [android.widget.LinearLayout] as the suggestion bar and keyboard *below* it, so the
 * originally-D-86-inspired "jump `layoutParams.height` to the target immediately, only animate content
 * afterwards" approach made the suggestion bar and keyboard visibly jump down by the row's full height the
 * instant the swipe was recognised - the row's own background was never actually the problem (it happens
 * to match the root view's own background colour exactly, so it was never visually distinguishable in the
 * first place; the *device-visible* pop was the sibling views jumping, not a background appearing). D-86's
 * "resize right away" shortcut works for [AdaptKeyboardView]'s own self-contained page-slide, which has no
 * siblings to push around - it does not transfer to a row stacked above other views.
 *
 * Fixed properly: [layoutParams] `height` itself is now animated (`ValueAnimator.ofInt` +
 * `requestLayout()` per frame, the standard Android pattern for an animatable view height - there is no
 * dedicated height property to animate directly), so the suggestion bar and keyboard shift down/up in the
 * very same motion as the row's own growth. [content] keeps a *fixed* [ROW_HEIGHT_DP] height, anchored to
 * the row's bottom edge (`Gravity.BOTTOM`) rather than stretching with it - as the row's own (animating,
 * smaller-than-`content`) bounds clip `content` (a `ViewGroup`'s default `clipChildren`), progressively more
 * of it becomes visible from the bottom up as the row grows, which is what makes the buttons read as
 * sliding up into place without any separate `translationY` animation being needed at all.
 *
 * D-144: a downward swipe anywhere on the row (over any button, or the gaps between them) notifies
 * [onSwipeDown] - mirroring [AdaptKeyboardView]'s own G-03 swipe-down-to-dismiss, which previously only
 * reacted on the keyboard's own key field, not this row.
 */
class ExtraRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    /** Invoked when the emoji button is tapped. */
    fun interface OnEmojiClickListener {
        
        fun onEmojiClick()
    }
    
    /** Invoked when the settings gear is tapped. */
    fun interface OnSettingsClickListener {
        
        fun onSettingsClick()
    }
    
    /** §69: invoked when the clear-clipboard button is tapped. */
    fun interface OnClearClipboardClickListener {
        
        fun onClearClipboardClick()
    }
    
    /** D-142: invoked when the credential-mode button is tapped. */
    fun interface OnCredentialModeClickListener {
        
        fun onCredentialModeClick()
    }
    
    /** D-156: invoked when the touch-zone-visualisation toggle button is tapped. */
    fun interface OnTouchZoneToggleClickListener {
        
        fun onTouchZoneToggleClick()
    }
    
    /** D-185: invoked when the URL-keyboard toggle button is tapped. */
    fun interface OnUrlModeToggleClickListener {
        
        fun onUrlModeToggleClick()
    }
    
    /** D-144: invoked when a downward swipe anywhere on the row dismisses/closes, mirroring G-03. */
    fun interface OnSwipeDownListener {
        
        fun onSwipeDown()
    }
    
    var onEmojiClick: OnEmojiClickListener? = null
    
    var onSettingsClick: OnSettingsClickListener? = null
    
    var onClearClipboardClick: OnClearClipboardClickListener? = null
    
    var onCredentialModeClick: OnCredentialModeClickListener? = null
    
    var onTouchZoneToggleClick: OnTouchZoneToggleClickListener? = null
    
    var onUrlModeToggleClick: OnUrlModeToggleClickListener? = null
    
    var onSwipeDown: OnSwipeDownListener? = null
    
    /** Whether the row is currently open (or mid-opening); false once fully closed. */
    var isOpen: Boolean = false
        private set
    
    /**
     * D-142: reflects whether credential mode is currently active for the focused field, so the button's
     * own background shows the state - reliably-detected email/password fields set this without the
     * button ever being tapped; a weak-signal or undetected field only sets it once the user actually taps.
     */
    var credentialModeActive: Boolean = false
        set(value) {
            field = value
            credentialModeButton.background = credentialModeBackground(value)
        }
    
    /**
     * D-156: reflects whether the live D-24 touch-zone overlay is currently showing on the keyboard itself
     * (as opposed to [de.froehlichmedia.adaptkey.settings.TouchModelActivity]'s own, separate preview
     * keyboard) - the button's background mirrors [credentialModeButton]'s own active/inactive styling.
     */
    var touchZoneVisible: Boolean = false
        set(value) {
            field = value
            touchZoneToggleButton.background = credentialModeBackground(value)
        }
    
    /**
     * D-185: reflects whether the URL keyboard is currently active for the focused field (vs. toggled off
     * to the ordinary letter keyboard while still inside the same URL field) - the button's background
     * mirrors [credentialModeButton]'s own active/inactive styling. Independent of [urlModeButtonVisible]:
     * a URL field always starts with this true (D-185's own "defaults to on" rule), so it only actually
     * matters once the row has been opened and the user can see it.
     */
    var urlModeActive: Boolean = false
        set(value) {
            field = value
            urlModeToggleButton.background = credentialModeBackground(value)
        }
    
    /**
     * D-185: whether the URL-keyboard toggle button is shown at all - only while the focused field is
     * itself a URL-variation field (reserving no space in the row otherwise, unlike every other button
     * here which is always present). Tracks the field's own type, not the live toggle state
     * ([urlModeActive]) - the button must stay reachable after being toggled off, so the user can toggle
     * it back on without leaving the field. Since [content] is a [FrameLayout] (fixed per-child margins,
     * not a flow layout), [urlModeToggleButton] permanently occupies [emojiButton]'s original slot
     * (immediately after [credentialModeButton], per D-185); [emojiButton] itself is shifted one slot
     * further right for as long as this is visible, and restored to its original slot when it is not.
     */
    var urlModeButtonVisible: Boolean = false
        set(value) {
            field = value
            urlModeToggleButton.visibility = if (value) View.VISIBLE else View.GONE
            emojiButton.layoutParams = (emojiButton.layoutParams as LayoutParams).apply {
                marginStart = if (value) slotMarginStart(3) else slotMarginStart(2)
            }
            emojiButton.requestLayout()
        }
    
    private val buttonSizePx = dp(BUTTON_SIZE_DP)
    private val marginPx = dp(BUTTON_MARGIN_DP)
    
    private val credentialModeButton = buttonFor("🔑") { onCredentialModeClick?.onCredentialModeClick() }
    private val emojiButton = buttonFor("😊") { onEmojiClick?.onEmojiClick() }
    private val settingsButton = buttonFor("⚙") { onSettingsClick?.onSettingsClick() }
    private val touchZoneToggleButton = buttonFor("🎯") { onTouchZoneToggleClick?.onTouchZoneToggleClick() }
    
    // D-185: sits between credentialModeButton and emojiButton (see [urlModeButtonVisible]) - GONE by
    // default, since it must not appear at all outside a URL field.
    private val urlModeToggleButton = buttonFor("🌐") { onUrlModeToggleClick?.onUrlModeToggleClick() }.apply {
        visibility = View.GONE
    }
    
    // Reported: a bare 🗑 gives no clue *what* it clears unless you already know - so nobody would ever
    // press it. A clipboard glyph with a small trash badge overlaid in the corner reads as "clear the
    // clipboard" directly, the same way a "ligature" of the two ideas would.
    private val clearClipboardButton = badgedButtonFor("📋", "🗑") { onClearClipboardClick?.onClearClipboardClick() }
    private val content = FrameLayout(context)
    private var heightAnimator: ValueAnimator? = null
    private var credentialFlashAnimator: ValueAnimator? = null
    
    // D-144: downward-swipe-to-dismiss state, mirroring SuggestionBarView's own D-144 handling and
    // AdaptKeyboardView's resolveSwipe() two-stage claim-early/confirm-late pattern.
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var swipeDownIntercepting = false
    private val interceptThresholdPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val swipeDownThresholdPx = dp(110).toFloat()
    
    init {
        content.setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_background))
        // D-142: the credential-mode button sits at the row's very left edge, ahead of the emoji button.
        content.addView(
            credentialModeButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = slotMarginStart(1) }
        )
        // D-185: permanently at slot 2 - GONE by default (see [urlModeToggleButton]'s own declaration), so
        // it neither reserves space nor is drawn until [urlModeButtonVisible] shows it.
        content.addView(
            urlModeToggleButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = slotMarginStart(2) }
        )
        content.addView(
            emojiButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.START or Gravity.CENTER_VERTICAL).apply {
                marginStart = slotMarginStart(2)
            }
        )
        // §69: sits directly left of the settings gear, offset by the gear's own width plus one more
        // button-margin's worth of gap so the two read as a distinct pair at the row's right edge.
        content.addView(
            clearClipboardButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = marginPx * 2 + buttonSizePx
            }
        )
        // D-156: one more slot further left, same spacing convention as clearClipboardButton's own offset
        // from the gear.
        content.addView(
            touchZoneToggleButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = marginPx * 3 + buttonSizePx * 2
            }
        )
        content.addView(
            settingsButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = marginPx }
        )
        // D-132: a fixed height, bottom-anchored - see the class KDoc for why this (not a translationY
        // slide) is what actually makes the buttons read as rising into place as the row itself grows.
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, dp(ROW_HEIGHT_DP), Gravity.BOTTOM))
        visibility = View.GONE
    }
    
    /**
     * D-144: claims a downward swipe away from whichever button would otherwise track it as a press
     * (every button here is plain-clickable) - the same early-claim reasoning
     * [de.froehlichmedia.adaptkey.suggestion.SuggestionBarView] uses for its own D-144 handling and
     * [AdaptKeyboardView.resolveSwipe] already uses for the keyboard body.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.x
                touchDownY = ev.y
                swipeDownIntercepting = false
            }
            
            MotionEvent.ACTION_MOVE -> {
                val direction = SwipeGesture.classify(ev.x - touchDownX, ev.y - touchDownY, interceptThresholdPx)
                if (direction == SwipeDirection.DOWN) {
                    swipeDownIntercepting = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
    
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (swipeDownIntercepting) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_UP -> {
                    val direction = SwipeGesture.classify(ev.x - touchDownX, ev.y - touchDownY, swipeDownThresholdPx)
                    swipeDownIntercepting = false
                    if (direction == SwipeDirection.DOWN) {
                        onSwipeDown?.onSwipeDown()
                    }
                    return true
                }
                
                MotionEvent.ACTION_CANCEL -> {
                    swipeDownIntercepting = false
                    return true
                }
            }
            return true
        }
        // D-144 follow-up (second pass - the first attempt below wasn't enough): a touch that starts on the
        // row's own empty background (not on any button) is never offered to a clickable child, so
        // ViewGroup's own dispatch never establishes a touch target for it at ACTION_DOWN. Per Android's own
        // documented dispatchTouchEvent() contract, onInterceptTouchEvent() is only re-consulted for a later
        // event when a touch target already exists ("intercepted = mFirstTouchTarget == null ||
        // onInterceptTouchEvent(ev)" only runs that check at all when a target is present; otherwise it is
        // forced true and every event goes straight to this view's own onTouchEvent()) - so the
        // ACTION_MOVE-based detection in onInterceptTouchEvent() above is structurally *unreachable* for
        // exactly this case, no matter how correct it is. Claiming ACTION_DOWN alone (this fix's first
        // attempt) was not enough on its own: it satisfied the "someone claims it" requirement, but the
        // swipe-direction check itself still only lived in onInterceptTouchEvent(), which this exact
        // scenario never calls again. This branch is the necessary, fully self-contained fallback: it
        // re-implements the same detection directly against whichever events onTouchEvent() itself receives,
        // only ever reached when nothing else claimed the gesture (a touch landing on a button is claimed by
        // the button itself long before dispatch would reach here).
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            
            MotionEvent.ACTION_MOVE -> {
                if (SwipeGesture.classify(ev.x - touchDownX, ev.y - touchDownY, interceptThresholdPx) == SwipeDirection.DOWN) {
                    swipeDownIntercepting = true
                }
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(ev)
    }
    
    /**
     * Reveals the row: animates its reserved layout height from 0 up to [ROW_HEIGHT_DP], so the suggestion
     * bar/keyboard below it shift down in the same motion as the row's own content becoming visible.
     * [onOpened] runs once fully expanded (also invoked synchronously when already open) - D-189: needed so
     * a caller-triggered effect on one of the row's own buttons (e.g. [flashCredentialModeButton]) can wait
     * until the row is actually visible, instead of running while it is still sliding into place.
     */
    fun open(onOpened: () -> Unit = {}) {
        if (isOpen) {
            onOpened()
            return
        }
        isOpen = true
        visibility = View.VISIBLE
        animateHeight(from = 0, to = dp(ROW_HEIGHT_DP), onOpened)
    }
    
    /**
     * Animates the reserved height back down to 0. [onClosed] runs once fully collapsed (also invoked
     * synchronously when already closed).
     */
    fun close(onClosed: () -> Unit = {}) {
        if (!isOpen) {
            onClosed()
            return
        }
        isOpen = false
        animateHeight(from = dp(ROW_HEIGHT_DP), to = 0) {
            visibility = View.GONE
            onClosed()
        }
    }
    
    /**
     * Resets the row to closed with no animation - a fresh keyboard presentation should never carry over
     * an extra row left open from a previous session (e.g. the keyboard was dismissed some other way while
     * the row was showing).
     */
    fun closeImmediately() {
        heightAnimator?.cancel()
        heightAnimator = null
        isOpen = false
        swipeDownIntercepting = false
        visibility = View.GONE
        layoutParams = layoutParams?.apply { height = 0 }
        requestLayout()
    }
    
    /**
     * D-132: animates [layoutParams]' `height` itself from [from] to [to] px, so the row's reserved space
     * in its parent [android.widget.LinearLayout] grows/shrinks smoothly (the standard `ValueAnimator.ofInt`
     * + `requestLayout()`-per-frame pattern - there is no dedicated Android height property to animate).
     */
    private fun animateHeight(from: Int, to: Int, onEnd: () -> Unit) {
        heightAnimator?.cancel()
        layoutParams = layoutParams.apply { height = from }
        requestLayout()
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = SLIDE_DURATION_MS
        animator.addUpdateListener {
            layoutParams = layoutParams.apply { height = it.animatedValue as Int }
            requestLayout()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            
            override fun onAnimationEnd(animation: Animator) {
                heightAnimator = null
                onEnd()
            }
        })
        heightAnimator = animator
        animator.start()
    }
    
    /**
     * D-142: briefly pulses the credential-mode button's opacity a few times - used when a weak signal
     * suggests the focused field might be a username field, but the signal is not reliable enough to
     * switch suggestion behaviour automatically; this nudges the user toward the manual toggle instead of
     * silently guessing. Independent of [credentialModeActive]'s own background state.
     */
    fun flashCredentialModeButton() {
        credentialFlashAnimator?.cancel()
        val animator = ValueAnimator.ofFloat(1f, 0.3f)
        animator.duration = FLASH_DURATION_MS
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = FLASH_REPEAT_COUNT
        animator.addUpdateListener { credentialModeButton.alpha = it.animatedValue as Float }
        animator.addListener(object : AnimatorListenerAdapter() {
            
            override fun onAnimationEnd(animation: Animator) {
                credentialModeButton.alpha = 1f
                credentialFlashAnimator = null
            }
        })
        credentialFlashAnimator = animator
        animator.start()
    }
    
    private fun credentialModeBackground(active: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, if (active) R.color.key_background_pressed else R.color.key_background_special))
            cornerRadius = dp(BUTTON_CORNER_RADIUS_DP).toFloat()
        }
    }
    
    private fun buttonFor(glyph: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = glyph
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.key_background_special))
                cornerRadius = dp(BUTTON_CORNER_RADIUS_DP).toFloat()
            }
            isClickable = true
            setOnClickListener { onClick() }
        }
    }
    
    /**
     * A button whose icon is a "ligature" of two glyphs: [baseGlyph] fills the whole button, [badgeGlyph]
     * sits as a small badge in the bottom-right corner - so the icon reads as "[badgeGlyph] applied to
     * [baseGlyph]" (e.g. clipboard + trash = "clear the clipboard"), not as two unrelated symbols. A single
     * overlaid emoji sequence was considered and rejected - there is no defined ZWJ combination for an
     * arbitrary emoji pair, so it would just render as two separate glyphs with no visual relationship at
     * all. Confirmed on-device that the badge needs no contrast background of its own to read clearly.
     */
    private fun badgedButtonFor(baseGlyph: String, badgeGlyph: String, onClick: () -> Unit): View {
        val badgeSizePx = dp(BADGE_SIZE_DP)
        val badgeMarginPx = dp(BADGE_MARGIN_DP)
        val base = TextView(context).apply {
            text = baseGlyph
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
        // Confirmed on-device: the badge reads clearly on its own, no contrast pill needed underneath it.
        val badge = TextView(context).apply {
            text = badgeGlyph
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        return FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.key_background_special))
                cornerRadius = dp(BUTTON_CORNER_RADIUS_DP).toFloat()
            }
            isClickable = true
            setOnClickListener { onClick() }
            addView(base, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            addView(
                badge,
                LayoutParams(badgeSizePx, badgeSizePx, Gravity.BOTTOM or Gravity.END).apply {
                    marginEnd = badgeMarginPx
                    bottomMargin = badgeMarginPx
                }
            )
        }
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    
    /**
     * D-185: the `marginStart` for the [index]-th button from the row's left edge (1-based: slot 1 =
     * [credentialModeButton]'s own `marginPx`, slot 2 = [emojiButton]'s original `marginPx * 2 +
     * buttonSizePx`), so [urlModeButtonVisible] can shift [emojiButton] between slot 2 (its position
     * outside a URL field) and slot 3 (once [urlModeToggleButton] claims slot 2) without duplicating the
     * spacing formula.
     */
    private fun slotMarginStart(index: Int): Int {
        return marginPx * index + buttonSizePx * (index - 1)
    }
    
    companion object {
        
        /** The row's open height in dp, matching the suggestion bar's own height. */
        const val ROW_HEIGHT_DP = 44
        
        private const val BUTTON_SIZE_DP = 36
        private const val BUTTON_MARGIN_DP = 8
        // D-142: three reverse-repeats of a 220ms fade reads as a clear, brief pulse without lingering.
        private const val FLASH_DURATION_MS = 220L
        private const val FLASH_REPEAT_COUNT = 3
        private const val BUTTON_CORNER_RADIUS_DP = 8
        private const val BADGE_SIZE_DP = 18
        private const val BADGE_MARGIN_DP = 1
        
        // D-58 precedent (AdaptKeyboardView's own page-slide): quick enough to stay snappy, slow enough
        // to actually be seen.
        private const val SLIDE_DURATION_MS = 180L
    }
}

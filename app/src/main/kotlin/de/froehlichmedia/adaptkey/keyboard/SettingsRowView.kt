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
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R

/**
 * §48 / §51 / §69: the swipe-up settings row - the emoji button (left edge), a clear-clipboard button and
 * a settings gear (right edge, clear-clipboard immediately to its left), revealed above the suggestion bar
 * (the topmost row while open) by an upward swipe anywhere on the keyboard (mirroring G-03's
 * downward-dismiss-anywhere).
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
 */
class SettingsRowView @JvmOverloads constructor(
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
    
    var onEmojiClick: OnEmojiClickListener? = null
    
    var onSettingsClick: OnSettingsClickListener? = null
    
    var onClearClipboardClick: OnClearClipboardClickListener? = null
    
    /** Whether the row is currently open (or mid-opening); false once fully closed. */
    var isOpen: Boolean = false
        private set
    
    private val emojiButton = buttonFor("😊") { onEmojiClick?.onEmojiClick() }
    private val settingsButton = buttonFor("⚙") { onSettingsClick?.onSettingsClick() }
    private val clearClipboardButton = buttonFor("🗑") { onClearClipboardClick?.onClearClipboardClick() }
    private val content = FrameLayout(context)
    private var heightAnimator: ValueAnimator? = null
    
    init {
        content.setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_background))
        val buttonSizePx = dp(BUTTON_SIZE_DP)
        val marginPx = dp(BUTTON_MARGIN_DP)
        content.addView(
            emojiButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = marginPx }
        )
        // §69: sits directly left of the settings gear, offset by the gear's own width plus one more
        // button-margin's worth of gap so the two read as a distinct pair at the row's right edge.
        content.addView(
            clearClipboardButton,
            LayoutParams(buttonSizePx, buttonSizePx, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = marginPx * 2 + buttonSizePx
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
     * Reveals the row: animates its reserved layout height from 0 up to [ROW_HEIGHT_DP], so the suggestion
     * bar/keyboard below it shift down in the same motion as the row's own content becoming visible. A
     * no-op while already open.
     */
    fun open() {
        if (isOpen) {
            return
        }
        isOpen = true
        visibility = View.VISIBLE
        animateHeight(from = 0, to = dp(ROW_HEIGHT_DP)) {}
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
     * a settings row left open from a previous session (e.g. the keyboard was dismissed some other way
     * while the row was showing).
     */
    fun closeImmediately() {
        heightAnimator?.cancel()
        heightAnimator = null
        isOpen = false
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
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    
    companion object {
        
        /** The row's open height in dp, matching the suggestion bar's own height. */
        const val ROW_HEIGHT_DP = 44
        
        private const val BUTTON_SIZE_DP = 36
        private const val BUTTON_MARGIN_DP = 8
        private const val BUTTON_CORNER_RADIUS_DP = 8
        
        // D-58 precedent (AdaptKeyboardView's own page-slide): quick enough to stay snappy, slow enough
        // to actually be seen.
        private const val SLIDE_DURATION_MS = 180L
    }
}

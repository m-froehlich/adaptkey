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
 * [open] / [close] jump the reserved layout space to its target height immediately (the D-86 precedent:
 * growing resizes right away, so nothing ever waits for space), then slide the row's own content - not
 * its outer bounds - up out of / back down into that already-reserved space. The content is clipped by
 * the row's own bounds (a plain [ViewGroup][android.view.ViewGroup]'s default `clipChildren`), so it
 * reads as emerging into view rather than sliding in over whatever is below it.
 *
 * D-132: the row's own background lives on [content], not on the outer view - `content`'s `translationY`
 * is what actually animates, so its background and its buttons slide together as one piece. The instant
 * layout-height jump above is otherwise invisible (nothing is painted at the outer view's own bounds), so
 * the two together read as one continuous reveal rather than the background "popping in" at full height
 * before the buttons catch up.
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
    private var slideAnimator: ValueAnimator? = null
    
    init {
        // D-132: on content, not here - see the class KDoc for why.
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
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        visibility = View.GONE
    }
    
    /**
     * Reveals the row: reserves its target height immediately, then slides the content up into view. A
     * no-op while already open.
     */
    fun open() {
        if (isOpen) {
            return
        }
        isOpen = true
        val targetHeightPx = dp(ROW_HEIGHT_DP)
        layoutParams = layoutParams.apply { height = targetHeightPx }
        visibility = View.VISIBLE
        requestLayout()
        slide(from = targetHeightPx.toFloat(), to = 0f) {}
    }
    
    /**
     * Slides the content back down, then reclaims the reserved space. [onClosed] runs once the row has
     * fully collapsed (also invoked synchronously when already closed).
     */
    fun close(onClosed: () -> Unit = {}) {
        if (!isOpen) {
            onClosed()
            return
        }
        isOpen = false
        val targetHeightPx = dp(ROW_HEIGHT_DP)
        slide(from = 0f, to = targetHeightPx.toFloat()) {
            visibility = View.GONE
            layoutParams = layoutParams.apply { height = 0 }
            requestLayout()
            onClosed()
        }
    }
    
    /**
     * Resets the row to closed with no animation - a fresh keyboard presentation should never carry over
     * a settings row left open from a previous session (e.g. the keyboard was dismissed some other way
     * while the row was showing).
     */
    fun closeImmediately() {
        slideAnimator?.cancel()
        slideAnimator = null
        isOpen = false
        visibility = View.GONE
        content.translationY = 0f
        layoutParams = layoutParams?.apply { height = 0 }
        requestLayout()
    }
    
    private fun slide(from: Float, to: Float, onEnd: () -> Unit) {
        slideAnimator?.cancel()
        content.translationY = from
        val animator = ValueAnimator.ofFloat(from, to)
        animator.duration = SLIDE_DURATION_MS
        animator.addUpdateListener { content.translationY = it.animatedValue as Float }
        animator.addListener(object : AnimatorListenerAdapter() {
            
            override fun onAnimationEnd(animation: Animator) {
                slideAnimator = null
                onEnd()
            }
        })
        slideAnimator = animator
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

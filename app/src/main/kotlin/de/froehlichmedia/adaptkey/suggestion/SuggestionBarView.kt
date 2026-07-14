// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.gesture.DragToTrash

/**
 * Horizontally scrollable suggestion strip (S-01).
 *
 * Renders the entries produced by {@link SuggestionController}; the most probable suggestion is at
 * the far left and the strip scrolls horizontally when entries overflow. The verbatim "keep as
 * typed" chip (S-06) is rendered in a distinct style. Tapping an entry notifies [onItemClick].
 *
 * G-04: dragging an ordinary suggestion upward past a threshold arms a trash drop zone that is drawn
 * over the bar; releasing while armed notifies [onBlacklist] so the word is permanently blacklisted
 * (A-04). The gesture is intentionally a deliberate upward drag (not a swipe) and never fires on the
 * verbatim chip, so it cannot be triggered accidentally while scrolling or tapping.
 */
class SuggestionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    
    /** Invoked when the user taps a suggestion entry. */
    fun interface OnItemClickListener {
        
        fun onItemClick(item: SuggestionController.DisplayItem)
    }
    
    /** Invoked when a suggestion is dragged into the trash zone (G-04 / A-04). */
    fun interface OnBlacklistListener {
        
        fun onBlacklist(word: String)
    }
    
    var onItemClick: OnItemClickListener? = null
    
    var onBlacklist: OnBlacklistListener? = null
    
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    }
    
    // The entries currently rendered, in the same order as the container's child chips, so a touch
    // can be mapped back to the suggestion it lands on (G-04).
    private val items = ArrayList<SuggestionController.DisplayItem>()
    
    // G-04 drag-to-trash state.
    private var dragDownX = 0f
    private var dragDownY = 0f
    private var dragWord: String? = null
    private var dragIntercepting = false
    private var trashArmed = false
    private val dragThresholdPx = dp(48).toFloat()
    // D-64: the gesture must be claimed away from the HorizontalScrollView's own horizontal-scroll
    // interception (which reacts at the much smaller system touch slop) as soon as the drag already
    // looks vertical-dominant - otherwise the scroll view wins the race well before dragThresholdPx is
    // ever reached and the upward drag can never arm. Claiming the gesture early only decides who owns
    // it; whether it actually arms (shows the trash zone / commits on release) still needs dragThresholdPx.
    private val interceptThresholdPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    
    private val trashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_trash_background)
    }
    
    private val trashTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_trash_text)
        textAlign = Paint.Align.CENTER
        textSize = dp(16).toFloat()
    }
    
    // D-88: the colour of the brief acceptance flash (see flashAccepted()) - defaults to the same green
    // as the C-04/S-05 recognised-word highlight for visual consistency; the service pushes the user's
    // actual configured highlight colour here (settings.highlightColor) so a customised colour matches too.
    var flashColor: Int = SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR
    
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var flashAnimator: ValueAnimator? = null
    
    init {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(ContextCompat.getColor(context, R.color.suggestion_bar_background))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
        addView(container)
    }
    
    /**
     * Replaces the displayed entries.
     *
     * @param items the ordered entries from the controller (left to right)
     */
    fun setItems(items: List<SuggestionController.DisplayItem>) {
        container.removeAllViews()
        scrollX = 0
        cancelDrag()
        this.items.clear()
        this.items.addAll(items)
        for (item in items) {
            container.addView(chipFor(item))
        }
    }
    
    private fun chipFor(item: SuggestionController.DisplayItem): View {
        val verbatim = item.kind == SuggestionController.Kind.VERBATIM
        val colorRes = if (verbatim) R.color.suggestion_verbatim_text else R.color.suggestion_text
        return TextView(context).apply {
            text = item.text
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, colorRes))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            if (verbatim) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
            isClickable = true
            setOnClickListener { onItemClick?.onItemClick(item) }
        }
    }
    
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragDownX = ev.x
                dragDownY = ev.y
                // Only ordinary suggestions can be trashed (never the verbatim chip).
                dragWord = normalWordAt(ev.x)
                dragIntercepting = false
                setTrashArmed(false)
            }
            
            MotionEvent.ACTION_MOVE -> {
                // D-64: claim the gesture at the small interceptThresholdPx, not the larger dragThresholdPx -
                // otherwise HorizontalScrollView's own scroll interception (system touch slop) wins the race
                // first and the drag can never take over. Once claimed, whether it is actually armed (trash
                // zone shown / release commits) is still gated by the full dragThresholdPx.
                val dx = ev.x - dragDownX
                val dy = ev.y - dragDownY
                if (dragWord != null && DragToTrash.isArmed(dx, dy, interceptThresholdPx)) {
                    dragIntercepting = true
                    setTrashArmed(DragToTrash.isArmed(dx, dy, dragThresholdPx))
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
    
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragIntercepting) {
            return super.onTouchEvent(ev)
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                setTrashArmed(DragToTrash.isArmed(ev.x - dragDownX, ev.y - dragDownY, dragThresholdPx))
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                val word = dragWord
                val commit = trashArmed
                cancelDrag()
                if (commit && word != null) {
                    onBlacklist?.onBlacklist(word)
                }
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
                return true
            }
        }
        return true
    }
    
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // G-04: while armed, paint the trash drop zone over the visible bar (drawn after the children,
        // in view coordinates so it stays fixed regardless of horizontal scroll).
        if (trashArmed) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), trashPaint)
            val baseline = height / 2f - (trashTextPaint.descent() + trashTextPaint.ascent()) / 2f
            canvas.drawText(context.getString(R.string.suggestion_trash_label), width / 2f, baseline, trashTextPaint)
        }
        // D-88: the acceptance flash, if currently animating - drawn last so it is visible over the chips.
        if (flashPaint.alpha > 0) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
        }
    }
    
    /**
     * D-88: a brief highlight flash confirming a correction or suggestion was just accepted - the visual
     * counterpart to [de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView.playSuggestionAcceptedSound],
     * used instead of it when key-press sound (D-05) is off, so the change is noticeable either way. Fades
     * from [FLASH_PEAK_ALPHA] to fully transparent over [FLASH_DURATION_MS]; a flash already in progress is
     * restarted from the peak rather than left to layer with the new one.
     */
    fun flashAccepted() {
        flashAnimator?.cancel()
        flashAnimator = ValueAnimator.ofInt(FLASH_PEAK_ALPHA, 0).apply {
            duration = FLASH_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                flashPaint.color = flashColor
                flashPaint.alpha = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Maps a touch x (in this view's coordinates) to the ordinary suggestion under it.
     *
     * @param viewX the touch x in view coordinates
     * @return the word of the ordinary suggestion at that position, or null for the verbatim chip,
     *         a gap, or an empty bar
     */
    private fun normalWordAt(viewX: Float): String? {
        val contentX = viewX + scrollX
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            if (contentX >= child.left && contentX <= child.right) {
                val item = items.getOrNull(index) ?: return null
                return if (item.kind == SuggestionController.Kind.NORMAL) item.word else null
            }
        }
        return null
    }
    
    private fun setTrashArmed(value: Boolean) {
        if (trashArmed != value) {
            trashArmed = value
            invalidate()
        }
    }
    
    private fun cancelDrag() {
        dragWord = null
        dragIntercepting = false
        setTrashArmed(false)
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // D-88: an in-flight flash must not keep posting invalidate() against a detached view.
        flashAnimator?.cancel()
    }
    
    companion object {
        
        // D-88: peak alpha (of 255) the acceptance flash starts at, and how long it takes to fade out.
        private const val FLASH_PEAK_ALPHA = 110
        private const val FLASH_DURATION_MS = 280L
    }
}

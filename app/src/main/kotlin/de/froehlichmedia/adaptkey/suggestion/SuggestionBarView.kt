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
import de.froehlichmedia.adaptkey.gesture.SwipeDirection
import de.froehlichmedia.adaptkey.gesture.SwipeGesture

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
 *
 * D-247: a [SuggestionController.Kind.LEARNED] chip ("Gelernt: X") gets its own, deliberately different
 * two-zone variant of the same upward drag instead of G-04's single-zone one: a shallow "Vergessen" zone
 * (green, [onForgetLearned] - unlearns only) and, past a second, deeper threshold, a "Verbieten" zone
 * (reuses G-04's own red styling, [onForbidLearned] - permanent blacklist). Scoped to this one chip kind
 * only - an ordinary suggestion keeps exactly G-04's original single-zone behaviour.
 *
 * D-144: a downward swipe anywhere on the bar (any chip, the verbatim chip, or an empty gap) notifies
 * [onSwipeDown] - mirroring [de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView]'s own G-03
 * swipe-down-to-dismiss, which previously only reacted on the keyboard's own key field, not this row.
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
    
    /** D-247: invoked when a [SuggestionController.Kind.LEARNED] chip is dragged into its shallow
     * "Vergessen" zone - unlearn only, no blacklist mark. */
    fun interface OnForgetLearnedListener {
        
        fun onForgetLearned(word: String)
    }
    
    /** D-247: invoked when a [SuggestionController.Kind.LEARNED] chip is dragged into its deep "Verbieten"
     * zone - permanent blacklist, bypassing G-04's own bundled/self-taught origin check. */
    fun interface OnForbidLearnedListener {
        
        fun onForbidLearned(word: String)
    }
    
    /** D-144: invoked when a downward swipe anywhere on the bar dismisses/closes, mirroring G-03. */
    fun interface OnSwipeDownListener {
        
        fun onSwipeDown()
    }
    
    var onItemClick: OnItemClickListener? = null
    
    var onBlacklist: OnBlacklistListener? = null
    
    var onForgetLearned: OnForgetLearnedListener? = null
    
    var onForbidLearned: OnForbidLearnedListener? = null
    
    var onSwipeDown: OnSwipeDownListener? = null
    
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    }
    
    // The entries currently rendered, in the same order as the container's child chips, so a touch
    // can be mapped back to the suggestion it lands on (G-04).
    private val items = ArrayList<SuggestionController.DisplayItem>()
    
    /** D-247: the drop zone currently armed by an upward drag - [BLACKLIST] is the only zone an ordinary
     * (G-04) suggestion ever reaches; [FORGET] exists only for a [SuggestionController.Kind.LEARNED] chip. */
    private enum class DragZone { NONE, FORGET, BLACKLIST }
    
    // G-04 drag-to-trash state, and D-144's downward-swipe-to-dismiss - both track the same touch-down
    // point (dragDownX/dragDownY), just resolved against opposite vertical directions.
    private var dragDownX = 0f
    private var dragDownY = 0f
    private var dragWord: String? = null
    // D-247: which kind is being dragged decides both the zone thresholds (zoneFor()) and what a release
    // in each zone means (onTouchEvent's ACTION_UP branch) - null while nothing is being dragged.
    private var dragKind: SuggestionController.Kind? = null
    private var dragIntercepting = false
    private var swipeDownIntercepting = false
    private var dragZone = DragZone.NONE
    // D-247: also G-04's own single-zone arm distance, reused as-is for a LEARNED chip's shallow "Vergessen"
    // zone entry point per the user's own request ("das hat immer gut gepasst").
    private val dragThresholdPx = dp(48).toFloat()
    // D-247: the LEARNED chip's own deeper "Verbieten" zone - double the shallow entry point.
    private val learnedForbidThresholdPx = dragThresholdPx * 2f
    // D-144: the confirm distance for the downward dismiss swipe - matches AdaptKeyboardView's own
    // fieldSwipeThresholdPx, so the gesture feels consistent whether it starts on the keyboard body or here.
    private val swipeDownThresholdPx = dp(110).toFloat()
    // D-64: the gesture must be claimed away from the HorizontalScrollView's own horizontal-scroll
    // interception (which reacts at the much smaller system touch slop) as soon as the drag already
    // looks vertical-dominant - otherwise the scroll view wins the race well before dragThresholdPx is
    // ever reached and the upward drag can never arm. Claiming the gesture early only decides who owns
    // it; whether it actually arms (drop zone shown / release commits) still needs the zone's own threshold.
    private val interceptThresholdPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    
    private val trashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_trash_background)
    }
    
    private val trashTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_trash_text)
        textAlign = Paint.Align.CENTER
        textSize = dp(16).toFloat()
    }
    
    // D-247: the LEARNED chip's own shallow "Vergessen" zone - a fixed green, independent of the
    // user-configurable C-04/S-05 highlight colour (see the colour resource's own comment).
    private val forgetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_forget_background)
    }
    
    private val forgetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.suggestion_forget_text)
        textAlign = Paint.Align.CENTER
        textSize = dp(16).toFloat()
    }
    
    // §56 (was D-88's flash, replaced per user feedback): the colour of the accepted word as it flies up
    // and out - defaults to the same green as the C-04/S-05 recognised-word highlight for visual
    // consistency; the service pushes the user's actual configured highlight colour here
    // (settings.highlightColor) so a customised colour matches too.
    var flyColor: Int = SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR
    
    private val flyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        alpha = 0
    }
    private var flyWord: String? = null
    private var flyOriginX = 0f
    private var flyProgress = 0f
    private var flyAnimator: ValueAnimator? = null
    
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
        // D-247: a fixed green, independent of the C-04/S-05 highlight colour (see the colour resource's
        // own comment) - colours the text only, mirroring D-25's own "colour the text, not the background"
        // precedent, same as the verbatim chip already does.
        val colorRes = when {
            verbatim -> R.color.suggestion_verbatim_text
            item.kind == SuggestionController.Kind.LEARNED -> R.color.suggestion_learned_text
            else -> R.color.suggestion_text
        }
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
                // Only an ordinary or LEARNED suggestion can be dragged (never the verbatim chip).
                val draggable = draggableItemAt(ev.x)
                dragWord = draggable?.word
                dragKind = draggable?.kind
                dragIntercepting = false
                swipeDownIntercepting = false
                setDragZone(DragZone.NONE)
            }
            
            MotionEvent.ACTION_MOVE -> {
                // D-64: claim the gesture at the small interceptThresholdPx, not the larger dragThresholdPx -
                // otherwise HorizontalScrollView's own scroll interception (system touch slop) wins the race
                // first and the drag can never take over. Once claimed, whether it is actually armed (drop
                // zone shown / release commits) is still gated by zoneFor()'s own, larger thresholds.
                val dx = ev.x - dragDownX
                val dy = ev.y - dragDownY
                if (dragWord != null && DragToTrash.isArmed(dx, dy, interceptThresholdPx)) {
                    dragIntercepting = true
                    setDragZone(zoneFor(dx, dy))
                    return true
                }
                // D-144: the same early-claim reasoning as G-04 above, mirrored for the opposite (downward)
                // direction - regardless of dragWord, so this also fires over the verbatim chip or a gap.
                if (SwipeGesture.classify(dx, dy, interceptThresholdPx) == SwipeDirection.DOWN) {
                    swipeDownIntercepting = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
    
    /**
     * D-247: which [DragZone] the current displacement arms, given [dragKind] - [SuggestionController.
     * Kind.LEARNED] gets the two-zone "Vergessen"/"Verbieten" variant, an ordinary suggestion (G-04) keeps
     * the original single [DragZone.BLACKLIST] zone at [dragThresholdPx] with no [DragZone.FORGET] stage at
     * all.
     */
    private fun zoneFor(dx: Float, dy: Float): DragZone {
        return when (dragKind) {
            SuggestionController.Kind.LEARNED -> when {
                DragToTrash.isArmed(dx, dy, learnedForbidThresholdPx) -> DragZone.BLACKLIST
                DragToTrash.isArmed(dx, dy, dragThresholdPx) -> DragZone.FORGET
                else -> DragZone.NONE
            }
            SuggestionController.Kind.NORMAL -> if (DragToTrash.isArmed(dx, dy, dragThresholdPx)) DragZone.BLACKLIST else DragZone.NONE
            else -> DragZone.NONE
        }
    }
    
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (dragIntercepting) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    setDragZone(zoneFor(ev.x - dragDownX, ev.y - dragDownY))
                    return true
                }
                
                MotionEvent.ACTION_UP -> {
                    val word = dragWord
                    val kind = dragKind
                    val zone = dragZone
                    cancelDrag()
                    // D-247: a LEARNED chip's two zones route to two different listeners; an ordinary
                    // suggestion only ever reaches BLACKLIST (zoneFor() never arms FORGET for it) and keeps
                    // G-04's original onBlacklist callback exactly as before.
                    if (word != null) {
                        when {
                            kind == SuggestionController.Kind.LEARNED && zone == DragZone.FORGET ->
                                onForgetLearned?.onForgetLearned(word)
                            kind == SuggestionController.Kind.LEARNED && zone == DragZone.BLACKLIST ->
                                onForbidLearned?.onForbidLearned(word)
                            zone == DragZone.BLACKLIST -> onBlacklist?.onBlacklist(word)
                        }
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
        // D-144: only the confirm distance differs from the interception check above - the direction must
        // still hold at release, exactly mirroring AdaptKeyboardView's own resolveSwipe() two-stage pattern.
        if (swipeDownIntercepting) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_UP -> {
                    val direction = SwipeGesture.classify(ev.x - dragDownX, ev.y - dragDownY, swipeDownThresholdPx)
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
        // bar's own empty background (no chip under it - most notably an entirely empty bar, D-50) is never
        // offered to a clickable child, so ViewGroup's own dispatch never establishes a touch target for it
        // at ACTION_DOWN. Per Android's own documented dispatchTouchEvent() contract, onInterceptTouchEvent()
        // is only re-consulted for a later event when a touch target already exists ("intercepted =
        // mFirstTouchTarget == null || onInterceptTouchEvent(ev)" only runs that check at all when a target
        // is present; otherwise it is forced true and every event goes straight to this view's own
        // onTouchEvent()) - so the ACTION_MOVE-based detection in onInterceptTouchEvent() above is
        // structurally *unreachable* for exactly this case, no matter how correct it is. Claiming
        // ACTION_DOWN alone (this fix's first attempt) was not enough on its own: it satisfied the "someone
        // claims it" requirement, but the swipe-direction check itself still only lived in
        // onInterceptTouchEvent(), which this exact scenario never calls again. This branch is the
        // necessary, fully self-contained fallback: it re-implements the same detection directly against
        // whichever events onTouchEvent() itself receives, only ever reached when nothing else claimed the
        // gesture (a touch landing on a chip is claimed by the chip itself long before dispatch would reach
        // here).
        // D-162: this fallback previously consumed every ACTION_DOWN/ACTION_MOVE unconditionally, which
        // was necessary for the D-144 swipe-down-on-background case but also silently swallowed an
        // ordinary horizontal scroll drag starting off a chip - HorizontalScrollView's own onTouchEvent()
        // never got a chance to run at all. super.onTouchEvent() is now always fed every event first (so
        // native scrolling tracks and responds normally); only a confirmed downward swipe is additionally
        // claimed here, everything else falls through to whatever the scroll view itself decided.
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                super.onTouchEvent(ev)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val scrolled = super.onTouchEvent(ev)
                if (SwipeGesture.classify(ev.x - dragDownX, ev.y - dragDownY, interceptThresholdPx) == SwipeDirection.DOWN) {
                    swipeDownIntercepting = true
                    return true
                }
                return scrolled
            }
            
            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(ev)
    }
    
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // G-04 / D-247: while armed, paint the current drop zone over the visible bar (drawn after the
        // children, in view coordinates so it stays fixed regardless of horizontal scroll). FORGET only
        // ever arms for a LEARNED chip; BLACKLIST's own label differs by kind (an ordinary suggestion keeps
        // G-04's original "🗑 Delete", a LEARNED chip's deeper zone reads "Verbieten" instead).
        when (dragZone) {
            DragZone.FORGET -> {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), forgetPaint)
                val baseline = height / 2f - (forgetTextPaint.descent() + forgetTextPaint.ascent()) / 2f
                canvas.drawText(context.getString(R.string.suggestion_forget_label), width / 2f, baseline, forgetTextPaint)
            }
            DragZone.BLACKLIST -> {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), trashPaint)
                val labelRes = if (dragKind == SuggestionController.Kind.LEARNED) {
                    R.string.suggestion_forbid_label
                } else {
                    R.string.suggestion_trash_label
                }
                val baseline = height / 2f - (trashTextPaint.descent() + trashTextPaint.ascent()) / 2f
                canvas.drawText(context.getString(labelRes), width / 2f, baseline, trashTextPaint)
            }
            DragZone.NONE -> {}
        }
        // §56: the accepted word, if currently flying - drawn last so it is visible over the chips. Negative
        // y values (above the bar's own top edge) are deliberate and only reach the screen because root
        // (AdaptKeyService.onCreateInputView()) already disables clipChildren for the same reason the D-53
        // long-press popup escapes upward over this same bar - a sibling view's overflow is not clipped by
        // a ViewGroup with clipChildren = false, only its own explicit bounds would be.
        val word = flyWord
        if (word != null && flyTextPaint.alpha > 0) {
            val progress = flyProgress
            val y = height / 2f - progress * dp(FLY_RISE_DP)
            flyTextPaint.textSize = dp(FLY_BASE_TEXT_DP) * (1f - progress * FLY_SHRINK_FRACTION)
            canvas.drawText(word, flyOriginX, y, flyTextPaint)
        }
    }
    
    /**
     * §56 (replaces D-88's flat bar flash, per user feedback that it read as too minimal), §57 (anchored to
     * the accepted suggestion's own position, per further feedback that a fixed centre origin felt too
     * detached from the bar): the accepted word rises from where its own chip currently sits - or the bar's
     * horizontal centre when no matching chip is found (§57's own "ungefähre Position reicht" allowance,
     * see [originXFor]) - shrinking and fading out as it exits upward past the bar's own top edge. Visible,
     * but deliberately subtle ([FLY_RISE_DP] is modest, [FLY_DURATION_MS] short). Played independently of
     * [de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView.playSuggestionAcceptedSound] - always,
     * regardless of whether key-press sound (D-05) is on. A flight already in progress is replaced outright
     * rather than left to layer with the new one.
     */
    fun flyAccepted(word: String) {
        flyAnimator?.cancel()
        flyWord = word
        flyOriginX = originXFor(word)
        flyAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FLY_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                flyProgress = it.animatedValue as Float
                flyTextPaint.color = flyColor
                flyTextPaint.alpha = ((1f - flyProgress) * 255).toInt().coerceIn(0, 255)
                invalidate()
            }
            start()
        }
    }
    
    /**
     * §57: the horizontal centre (in this view's own coordinates, i.e. already offset by [scrollX]) of the
     * currently displayed chip whose [SuggestionController.DisplayItem.word] matches [word] - the tapped
     * suggestion for a bar-tap acceptance, or (since [notifySuggestionAccepted] fires before the bar's next
     * refresh) still the top suggestion chip for an autocorrect-on-commit acceptance, when it happens to be
     * the same word. Falls back to the bar's own horizontal centre when no chip matches (the exact
     * "ungefähre Position reicht" case named in the request) rather than searching further or guessing.
     */
    private fun originXFor(word: String): Float {
        val index = items.indexOfFirst { it.word.equals(word, ignoreCase = true) }
        val child = if (index >= 0) container.getChildAt(index) else null
        return if (child != null) (child.left + child.right) / 2f - scrollX else width / 2f
    }
    
    /**
     * Maps a touch x (in this view's coordinates) to the draggable suggestion under it (G-04 / D-247).
     *
     * @param viewX the touch x in view coordinates
     * @return the ordinary or LEARNED entry at that position, or null for the verbatim chip, a
     *         CLIPBOARD/CREDENTIAL chip, a gap, or an empty bar
     */
    private fun draggableItemAt(viewX: Float): SuggestionController.DisplayItem? {
        val contentX = viewX + scrollX
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            if (contentX >= child.left && contentX <= child.right) {
                val item = items.getOrNull(index) ?: return null
                return if (item.kind == SuggestionController.Kind.NORMAL || item.kind == SuggestionController.Kind.LEARNED) {
                    item
                } else {
                    null
                }
            }
        }
        return null
    }
    
    private fun setDragZone(value: DragZone) {
        if (dragZone != value) {
            dragZone = value
            invalidate()
        }
    }
    
    private fun cancelDrag() {
        dragWord = null
        dragKind = null
        dragIntercepting = false
        // D-144: setItems() (a fresh suggestion refresh mid-gesture) also calls this - a stale downward-swipe
        // interception should not carry over into whatever now occupies the bar.
        swipeDownIntercepting = false
        setDragZone(DragZone.NONE)
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // §56: an in-flight animation must not keep posting invalidate() against a detached view.
        flyAnimator?.cancel()
    }
    
    companion object {
        
        // §56: how far the accepted word rises (in dp, from the bar's vertical centre) and how long the
        // whole flight takes - both deliberately modest ("dezent") so it stays a quiet confirmation rather
        // than something that draws attention on every single acceptance.
        private const val FLY_RISE_DP = 34
        private const val FLY_DURATION_MS = 380L
        private const val FLY_BASE_TEXT_DP = 15
        private const val FLY_SHRINK_FRACTION = 0.22f
    }
}

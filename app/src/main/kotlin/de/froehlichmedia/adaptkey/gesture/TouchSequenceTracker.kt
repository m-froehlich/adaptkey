// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

/**
 * Pure single-pointer-per-gesture tracker (D-495). One instance per view follows one touch stream and
 * answers the only three questions its touch handling actually has: does this stream still belong to
 * the finger that started it, may this release act on the stream's remembered state at all, and did
 * the sequence genuinely move.
 * 
 * The bug it exists for: the keyboard views branch on `actionMasked` only, so a second finger landing
 * while the first is still down (overlapping taps - routine in fast rolling/two-thumb typing) is never
 * seen. The stream's remembered down-point survives until the **last** pointer's `ACTION_UP`, whose
 * coordinates belong to a *different* finger - so `release - down` became the distance between two
 * separate taps and classified as a swipe (the reported "a then quick SPACE flips to the numbers page",
 * and the vertical twin "fast top-row/bottom-row typing dismisses the keyboard"). With this tracker a
 * second pointer kills the sequence outright, so no later event of that stream can commit a tap, a
 * swipe, or a popup selection from the first finger's remembered state.
 * 
 * Pure by design (no Android types): the call sites resolve `MotionEvent` pointer ids / action indices
 * themselves and feed this class plain ints. Unit-tested directly - [AdaptKeyboardView] and the bar/row
 * views stay in the project's established untested Android-glue layer.
 */
class TouchSequenceTracker {
    
    /** The pointer id the live sequence belongs to, or [NO_POINTER] when no sequence is live. */
    var trackedPointerId: Int = NO_POINTER
        private set
    
    /**
     * Whether a sequence is live: started by [onDown] and never killed by [onExtraPointerDown] or
     * [end]. Only a live sequence can own a release.
     */
    val hasSequence: Boolean
        get() = trackedPointerId != NO_POINTER
    
    /**
     * D-495's continuity gate: whether the live sequence delivered at least one move beyond the touch
     * slop. A release from a sequence that never moved can only ever be a tap - it may not classify as
     * a swipe, however far the coordinates happen to sit apart (a dead sequence records nothing, so
     * another finger's movement can never satisfy this either).
     */
    var motionWasObserved: Boolean = false
        private set
    
    /**
     * A stream begins: [pointerId] owns it. Clears every per-stream flag, so a stale dead sequence can
     * never leak into the new one.
     */
    fun onDown(pointerId: Int) {
        trackedPointerId = pointerId
        motionWasObserved = false
    }
    
    /**
     * A second pointer landed while the sequence was live (overlapping taps): the sequence is dead from
     * this instant on - it can never own a release again, and it stops recording motion. Deliberately
     * conservative: the first tap of an overlapped pair stays lost (that was already true before
     * D-495; rescuing it is a separate future decision), but it can no longer drag a later event into
     * a bogus gesture. Safe to call with no live sequence (a second stream's `ACTION_POINTER_DOWN`
     * arriving after the tracked view already ended its own interest).
     */
    fun onExtraPointerDown() {
        trackedPointerId = NO_POINTER
        motionWasObserved = false
    }
    
    /**
     * The live sequence moved beyond the touch slop (call-site computed). Dead sequences ignore this -
     * the surviving finger's movement must not resurrect a killed sequence's swipe eligibility.
     */
    fun onMoveBeyondSlop() {
        if (hasSequence) {
            motionWasObserved = true
        }
    }
    
    /**
     * Whether this release may act on the stream's remembered state (tap, swipe, popup selection):
     * only a live sequence's own pointer. A dead sequence owns nothing, and a foreign pointer never
     * does - that is the whole point of D-495.
     */
    fun ownsRelease(pointerId: Int): Boolean {
        return trackedPointerId == pointerId
    }
    
    /** The stream is over (or a mode like the D-401 cursor control hands it back): full reset. */
    fun end() {
        trackedPointerId = NO_POINTER
        motionWasObserved = false
    }
    
    companion object {
        /** Sentinel for "no pointer tracked". Matches `MotionEvent.INVALID_POINTER_ID`'s spirit; kept Android-free. */
        const val NO_POINTER = -1
    }
}

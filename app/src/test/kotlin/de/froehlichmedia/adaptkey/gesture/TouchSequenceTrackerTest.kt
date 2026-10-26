// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-495 single-pointer-per-gesture tracker: stream ownership across multi-touch
 * overlaps, the continuity gate, and cross-stream isolation. The scenarios mirror the reported bug -
 * the `a`-then-quick-SPACE overlap that used to compose two taps into one swipe displacement.
 */
class TouchSequenceTrackerTest {
    
    private val tracker = TouchSequenceTracker()
    private val pointerA = 0
    private val pointerB = 1
    
    @Test
    fun `a single-finger tap owns its own release and never observes motion`() {
        tracker.onDown(pointerA)
        assertTrue(tracker.ownsRelease(pointerA))
        assertFalse(tracker.motionWasObserved)
    }
    
    @Test
    fun `a single-finger swipe owns its release and records the observed motion`() {
        tracker.onDown(pointerA)
        tracker.onMoveBeyondSlop()
        assertTrue(tracker.ownsRelease(pointerA))
        assertTrue(tracker.motionWasObserved)
    }
    
    @Test
    fun `the reported overlap repro - a then quick SPACE - lets neither finger act`() {
        // Finger 1 lands on 'a', finger 2 lands on SPACE before finger 1 lifts, then both lift.
        tracker.onDown(pointerA)
        tracker.onExtraPointerDown()
        assertFalse(tracker.ownsRelease(pointerA), "the cancelled sequence must not commit a tap or swipe")
        assertFalse(tracker.ownsRelease(pointerB), "the second finger must not act on the first finger's state")
        tracker.onMoveBeyondSlop()
        assertFalse(tracker.motionWasObserved, "the surviving finger's movement must not resurrect swipe eligibility")
    }
    
    @Test
    fun `a foreign pointer never owns a release even while the sequence is live`() {
        tracker.onDown(pointerA)
        assertTrue(tracker.ownsRelease(pointerA))
        assertFalse(tracker.ownsRelease(pointerB))
    }
    
    @Test
    fun `a sequence that never moved past the slop cannot satisfy the continuity gate`() {
        tracker.onDown(pointerA)
        assertFalse(tracker.motionWasObserved)
    }
    
    @Test
    fun `sub-slop-only movement never sets the continuity flag`() {
        // The call site only reports moves that actually cleared the slop; a tracker fed nothing
        // stays motionless - the release is a tap, never a swipe.
        tracker.onDown(pointerA)
        tracker.end()
        tracker.onDown(pointerB)
        assertFalse(tracker.motionWasObserved)
    }
    
    @Test
    fun `end fully resets so the next stream starts clean`() {
        tracker.onDown(pointerA)
        tracker.onMoveBeyondSlop()
        tracker.end()
        assertFalse(tracker.ownsRelease(pointerA))
        assertFalse(tracker.motionWasObserved)
        tracker.onDown(pointerB)
        assertTrue(tracker.ownsRelease(pointerB))
        assertFalse(tracker.motionWasObserved)
    }
    
    @Test
    fun `sequential taps each own their own stream - no cross-stream state`() {
        tracker.onDown(pointerA)
        tracker.end()
        tracker.onDown(pointerB)
        tracker.end()
        tracker.onDown(pointerA)
        assertTrue(tracker.ownsRelease(pointerA))
        assertFalse(tracker.motionWasObserved)
    }
    
    @Test
    fun `an extra pointer down without a live sequence is a safe no-op`() {
        tracker.onExtraPointerDown()
        assertFalse(tracker.hasSequence)
        assertFalse(tracker.ownsRelease(pointerA))
        tracker.onDown(pointerA)
        tracker.onExtraPointerDown()
        tracker.onExtraPointerDown()
        assertFalse(tracker.ownsRelease(pointerA))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CursorControlGestureTest {
    
    @Test
    fun stepsForZeroDistanceIsZeroSteps() {
        val steps = CursorControlGesture.stepsFor(0f, 0f, 2f)
        assertEquals(0, steps.characters)
        assertEquals(0, steps.lines)
    }
    
    @Test
    fun stepsForExactMultipleOfCharacterStep() {
        val density = 2f
        val dx = CursorControlGesture.DP_PER_CHARACTER_STEP * density * 3f
        val steps = CursorControlGesture.stepsFor(dx, 0f, density)
        assertEquals(3, steps.characters)
        assertEquals(0, steps.lines)
    }
    
    @Test
    fun stepsForExactMultipleOfLineStep() {
        val density = 2f
        val dy = CursorControlGesture.DP_PER_LINE_STEP * density * 2f
        val steps = CursorControlGesture.stepsFor(0f, dy, density)
        assertEquals(0, steps.characters)
        assertEquals(2, steps.lines)
    }
    
    @Test
    fun stepsForNegativeDistanceGoesBackward() {
        val density = 1f
        val dx = -CursorControlGesture.DP_PER_CHARACTER_STEP * density * 2f
        val steps = CursorControlGesture.stepsFor(dx, 0f, density)
        assertEquals(-2, steps.characters)
    }
    
    @Test
    fun stepsForRoundsToNearestStepRatherThanTruncating() {
        val density = 1f
        // 1.6 character-steps worth of distance rounds up to 2, not down to 1.
        val dx = CursorControlGesture.DP_PER_CHARACTER_STEP * density * 1.6f
        val steps = CursorControlGesture.stepsFor(dx, 0f, density)
        assertEquals(2, steps.characters)
    }
    
    @Test
    fun stepsForBelowHalfAStepRoundsToZero() {
        val density = 1f
        val dx = CursorControlGesture.DP_PER_CHARACTER_STEP * density * 0.4f
        val steps = CursorControlGesture.stepsFor(dx, 0f, density)
        assertEquals(0, steps.characters)
    }
    
    @Test
    fun stepsForCombinesHorizontalAndVerticalWhenVerticalDominates() {
        val density = 1.5f
        val dx = CursorControlGesture.DP_PER_CHARACTER_STEP * density * 4f
        val dy = -CursorControlGesture.DP_PER_LINE_STEP * density * 1f
        val steps = CursorControlGesture.stepsFor(dx, dy, density)
        assertEquals(4, steps.characters)
        assertEquals(-1, steps.lines)
    }
    
    @Test
    fun stepsForSuppressesLineWhenHorizontalDragDominates() {
        // D-401-followup: the exact real-device shape that caused an unintended line change - a large
        // horizontal distance with only a modest amount of vertical drift, which would round to a nonzero
        // line step if evaluated on its own (dy is a full line step here) but must not fire while dx is the
        // larger of the two.
        val density = 1f
        val dx = CursorControlGesture.DP_PER_LINE_STEP * density * 3f
        val dy = CursorControlGesture.DP_PER_LINE_STEP * density * 1f
        val steps = CursorControlGesture.stepsFor(dx, dy, density)
        assertEquals(0, steps.lines)
    }
    
    @Test
    fun stepsForAllowsLineWhenVerticalDragDominates() {
        val density = 1f
        val dx = CursorControlGesture.DP_PER_LINE_STEP * density * 0.5f
        val dy = CursorControlGesture.DP_PER_LINE_STEP * density * 2f
        val steps = CursorControlGesture.stepsFor(dx, dy, density)
        assertEquals(2, steps.lines)
    }
    
    @Test
    fun stepsForGateComparesMagnitudeNotSign() {
        // A negative dx (dragging left) must not itself allow a small dy to sneak past the gate just
        // because a raw dy > dx comparison (rather than |dy| > |dx|) would have been satisfied.
        val density = 1f
        val dx = -CursorControlGesture.DP_PER_LINE_STEP * density * 3f
        val dy = CursorControlGesture.DP_PER_LINE_STEP * density * 1f
        val steps = CursorControlGesture.stepsFor(dx, dy, density)
        assertEquals(0, steps.lines)
    }
    
    @Test
    fun targetPointForScalesTheHorizontalAxisContinuously() {
        val target = CursorControlGesture.targetPointFor(100f, 200f, 40f, 0f, rowHeightPx = 50f, gainHorizontal = 0.5f)
        assertEquals(120f, target.x, 0.001f)
        assertEquals(200f, target.y, 0.001f)
    }
    
    @Test
    fun theVerticalAxisIsWeightedLessThanTheHorizontalOne() {
        // A pivoting thumb arcs, so vertical travel during a sideways drag is mostly drift, not intent.
        assertTrue(CursorControlGesture.SCREEN_SPACE_GAIN_VERTICAL < CursorControlGesture.SCREEN_SPACE_GAIN_HORIZONTAL)
    }
    
    @Test
    fun targetPointForMovesTheCaretLessThanTheFinger() {
        // The user's own explicit requirement: 1:1 would make the gesture no better than tapping directly.
        val fingerTravel = 200f
        val target = CursorControlGesture.targetPointFor(0f, 0f, fingerTravel, 0f, rowHeightPx = 50f)
        assertTrue(target.x < fingerTravel)
        assertTrue(target.x > 0f)
    }
    
    @Test
    fun targetPointForWithoutTravelIsTheOriginItself() {
        val target = CursorControlGesture.targetPointFor(37f, 91f, 0f, 0f, rowHeightPx = 50f)
        assertEquals(37f, target.x, 0.001f)
        assertEquals(91f, target.y, 0.001f)
    }
    
    @Test
    fun targetPointForKeepsTheRowUntilAWholeRowOfTravelIsReached() {
        val rowHeight = 50f
        val justUnderOneRow = (rowHeight * 0.99f) / CursorControlGesture.SCREEN_SPACE_GAIN_VERTICAL
        val target = CursorControlGesture.targetPointFor(0f, 200f, 0f, justUnderOneRow, rowHeightPx = rowHeight)
        assertEquals(200f, target.y, 0.001f)
    }
    
    @Test
    fun targetPointForChangesRowOnceAWholeRowOfTravelIsReached() {
        val rowHeight = 50f
        val justOverOneRow = (rowHeight * 1.01f) / CursorControlGesture.SCREEN_SPACE_GAIN_VERTICAL
        val target = CursorControlGesture.targetPointFor(0f, 200f, 0f, justOverOneRow, rowHeightPx = rowHeight)
        assertEquals(250f, target.y, 0.001f)
    }
    
    @Test
    fun targetPointForQuantisesUpwardsTheSameWay() {
        val rowHeight = 50f
        val justOverOneRow = -(rowHeight * 1.01f) / CursorControlGesture.SCREEN_SPACE_GAIN_VERTICAL
        val target = CursorControlGesture.targetPointFor(0f, 200f, 0f, justOverOneRow, rowHeightPx = rowHeight)
        assertEquals(150f, target.y, 0.001f)
    }
    
    @Test
    fun targetPointForAlwaysLandsOnARowCentreNeverBetweenTwo() {
        // The caret must never end up balanced on a boundary, where a pixel of drift could tip it either way.
        val rowHeight = 50f
        for (travel in 0..600 step 7) {
            val target = CursorControlGesture.targetPointFor(0f, 200f, 0f, travel.toFloat(), rowHeightPx = rowHeight)
            assertEquals(0f, (target.y - 200f) % rowHeight, 0.001f)
        }
    }
    
    @Test
    fun targetPointForLeavesTheVerticalAxisContinuousWithoutAKnownRowHeight() {
        val target = CursorControlGesture.targetPointFor(0f, 200f, 0f, 100f, rowHeightPx = 0f, gainVertical = 0.5f)
        assertEquals(250f, target.y, 0.001f)
    }
}

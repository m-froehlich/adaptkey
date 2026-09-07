// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
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
}

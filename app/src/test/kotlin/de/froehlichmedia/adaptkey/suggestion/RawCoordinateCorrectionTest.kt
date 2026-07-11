// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.TapPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-39 raw-coordinate per-character correction.
 */
class RawCoordinateCorrectionTest {
    
    // 'a' and 'b' sit almost on top of each other (an ambiguous pair); 'x' sits far away, unambiguous.
    private val a = OffsetModel.Candidate("c:a", centerX = 0f, centerY = 0f, halfWidth = 10f, halfHeight = 10f)
    private val b = OffsetModel.Candidate("c:b", centerX = 5f, centerY = 0f, halfWidth = 10f, halfHeight = 10f)
    private val x = OffsetModel.Candidate("c:x", centerX = 100f, centerY = 0f, halfWidth = 10f, halfHeight = 10f)
    private val candidates = listOf(a, b, x)
    
    @Test
    fun `an ambiguous tap proposes the runner-up key, ranked ahead of an unambiguous one`() {
        // Position 0: typed "a", but the tap (x=3) is actually closer to "b" - a genuinely ambiguous pair.
        // Position 1: typed "x" dead-centre - completely unambiguous, its runner-up is a poor fit.
        val taps = listOf(TapPoint(3f, 0f), TapPoint(100f, 0f))
        
        val respellings = RawCoordinateCorrection.respellings("ax", taps, candidates, OffsetModel())
        
        assertEquals(listOf("bx", "ab"), respellings)
    }
    
    @Test
    fun `case is preserved at the substituted position`() {
        val taps = listOf(TapPoint(3f, 0f), TapPoint(100f, 0f))
        
        val respellings = RawCoordinateCorrection.respellings("Ax", taps, candidates, OffsetModel())
        
        assertEquals("Bx", respellings.first())
    }
    
    @Test
    fun `a position with no alternative candidate is skipped`() {
        val respellings = RawCoordinateCorrection.respellings("a", listOf(TapPoint(0f, 0f)), listOf(a), OffsetModel())
        
        assertTrue(respellings.isEmpty())
    }
    
    @Test
    fun `a tap-count mismatch yields no candidates`() {
        val taps = listOf(TapPoint(3f, 0f))
        
        assertTrue(RawCoordinateCorrection.respellings("ax", taps, candidates, OffsetModel()).isEmpty())
    }
    
    @Test
    fun `an empty token or empty key geometry yields no candidates`() {
        assertTrue(RawCoordinateCorrection.respellings("", emptyList(), candidates, OffsetModel()).isEmpty())
        assertTrue(RawCoordinateCorrection.respellings("ax", listOf(TapPoint(0f, 0f), TapPoint(0f, 0f)), emptyList(), OffsetModel()).isEmpty())
    }
}

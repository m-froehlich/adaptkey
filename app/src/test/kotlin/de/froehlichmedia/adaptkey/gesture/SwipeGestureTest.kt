// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure swipe classifier: thresholding, dominant-axis selection and sign handling.
 */
class SwipeGestureTest {
    
    private val threshold = 40f
    
    @Test
    fun `movement below the threshold on both axes is not a swipe`() {
        assertEquals(SwipeDirection.NONE, SwipeGesture.classify(10f, -15f, threshold))
        assertEquals(SwipeDirection.NONE, SwipeGesture.classify(-39f, 39f, threshold))
    }
    
    @Test
    fun `horizontal swipes resolve by sign when the horizontal axis dominates`() {
        assertEquals(SwipeDirection.LEFT, SwipeGesture.classify(-60f, 10f, threshold))
        assertEquals(SwipeDirection.RIGHT, SwipeGesture.classify(60f, -10f, threshold))
    }
    
    @Test
    fun `vertical swipes resolve by sign when the vertical axis dominates`() {
        assertEquals(SwipeDirection.UP, SwipeGesture.classify(10f, -60f, threshold))
        assertEquals(SwipeDirection.DOWN, SwipeGesture.classify(-10f, 60f, threshold))
    }
    
    @Test
    fun `a tie on the dominant axis is treated as horizontal`() {
        assertEquals(SwipeDirection.RIGHT, SwipeGesture.classify(50f, 50f, threshold))
        assertEquals(SwipeDirection.LEFT, SwipeGesture.classify(-50f, -50f, threshold))
    }
    
    @Test
    fun `exactly reaching the threshold counts as a swipe`() {
        assertEquals(SwipeDirection.DOWN, SwipeGesture.classify(0f, 40f, threshold))
    }
    
    @Test
    fun `a negative threshold is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { SwipeGesture.classify(10f, 10f, -1f) }
    }
}

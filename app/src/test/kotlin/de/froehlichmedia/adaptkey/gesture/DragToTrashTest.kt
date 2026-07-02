// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure G-04 drag-to-trash arming decision.
 */
class DragToTrashTest {
    
    private val threshold = 48f
    
    @Test
    fun `an upward drag past the threshold arms`() {
        assertTrue(DragToTrash.isArmed(0f, -60f, threshold))
    }
    
    @Test
    fun `an upward drag below the threshold does not arm`() {
        assertFalse(DragToTrash.isArmed(0f, -40f, threshold))
    }
    
    @Test
    fun `a downward drag never arms`() {
        assertFalse(DragToTrash.isArmed(0f, 60f, threshold))
    }
    
    @Test
    fun `a predominantly horizontal drag does not arm`() {
        assertFalse(DragToTrash.isArmed(80f, -60f, threshold))
    }
    
    @Test
    fun `upward travel exactly at the threshold arms when vertical dominates`() {
        assertTrue(DragToTrash.isArmed(48f, -48f, threshold))
    }
    
    @Test
    fun `a negative threshold is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { DragToTrash.isArmed(0f, -10f, -1f) }
    }
}

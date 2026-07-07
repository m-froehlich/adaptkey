// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-44 horizontal long-press popup layout + selection geometry.
 */
class HorizontalLongPressPopupTest {
    
    // A three-cell row (`? . !`) of 40px cells: with `.` (index 1) centred over a key at x=300, the row
    // should start at 300 - 1.5*40 = 240, giving cells [240,280) idx0, [280,320) idx1, [320,360) idx2.
    @Test
    fun `the row is centred so the pre-selected cell sits over the stem`() {
        assertEquals(240f, HorizontalLongPressPopup.rowLeft(300f, 40f, 3, 1, 0f, 1000f))
    }
    
    @Test
    fun `the row is clamped to the left edge`() {
        assertEquals(5f, HorizontalLongPressPopup.rowLeft(20f, 40f, 3, 1, 5f, 1000f))
    }
    
    @Test
    fun `the row is clamped to the right edge`() {
        assertEquals(600f, HorizontalLongPressPopup.rowLeft(2000f, 40f, 3, 1, 5f, 600f))
    }
    
    @Test
    fun `an impossible clamp range falls back to the minimum`() {
        assertEquals(5f, HorizontalLongPressPopup.rowLeft(300f, 40f, 3, 1, 5f, 0f))
    }
    
    @Test
    fun `the finger over a cell selects that cell`() {
        assertEquals(0, HorizontalLongPressPopup.selectedIndex(250f, 240f, 40f, 3))
        assertEquals(1, HorizontalLongPressPopup.selectedIndex(300f, 240f, 40f, 3))
        assertEquals(2, HorizontalLongPressPopup.selectedIndex(350f, 240f, 40f, 3))
    }
    
    @Test
    fun `the finger past either end clamps to the edge cell`() {
        assertEquals(0, HorizontalLongPressPopup.selectedIndex(-500f, 240f, 40f, 3))
        assertEquals(2, HorizontalLongPressPopup.selectedIndex(5000f, 240f, 40f, 3))
    }
    
    @Test
    fun `invalid geometry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.selectedIndex(0f, 0f, 0f, 3) }
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.selectedIndex(0f, 0f, 40f, 0) }
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.rowLeft(0f, 0f, 3, 1, 0f, 100f) }
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.rowLeft(0f, 40f, 0, 0, 0f, 100f) }
    }
}

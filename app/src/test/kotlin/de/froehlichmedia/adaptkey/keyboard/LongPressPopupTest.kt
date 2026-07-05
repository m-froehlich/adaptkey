// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-01 long-press popup selection geometry.
 */
class LongPressPopupTest {
    
    @Test
    fun `a pointer inside a cell selects that cell`() {
        // Cells: [100,150) [150,200) [200,250), width 50.
        assertEquals(0, LongPressPopup.selectedIndex(120f, 100f, 50f, 3))
        assertEquals(1, LongPressPopup.selectedIndex(170f, 100f, 50f, 3))
        assertEquals(2, LongPressPopup.selectedIndex(240f, 100f, 50f, 3))
    }
    
    @Test
    fun `a cell boundary belongs to the right-hand cell`() {
        assertEquals(1, LongPressPopup.selectedIndex(150f, 100f, 50f, 3))
    }
    
    @Test
    fun `a pointer left of the popup clamps to the first cell`() {
        assertEquals(0, LongPressPopup.selectedIndex(10f, 100f, 50f, 3))
    }
    
    @Test
    fun `a pointer right of the popup clamps to the last cell`() {
        assertEquals(2, LongPressPopup.selectedIndex(9_999f, 100f, 50f, 3))
    }
    
    @Test
    fun `a single-cell popup always selects index zero`() {
        assertEquals(0, LongPressPopup.selectedIndex(-500f, 0f, 40f, 1))
        assertEquals(0, LongPressPopup.selectedIndex(500f, 0f, 40f, 1))
    }
    
    @Test
    fun `a non-positive cell width is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { LongPressPopup.selectedIndex(10f, 0f, 0f, 3) }
    }
    
    @Test
    fun `a non-positive count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { LongPressPopup.selectedIndex(10f, 0f, 40f, 0) }
    }
}

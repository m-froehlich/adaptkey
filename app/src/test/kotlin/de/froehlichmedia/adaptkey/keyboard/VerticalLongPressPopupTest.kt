// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-23 vertical long-press popup selection geometry.
 */
class VerticalLongPressPopupTest {
    
    // Stack bottom at y=200, cell height 40, two secondaries: cells [160,200) -> idx1, [120,160) -> idx2.
    @Test
    fun `the finger on the key selects the primary`() {
        assertEquals(0, VerticalLongPressPopup.selectedIndex(210f, 200f, 40f, 2))
        assertEquals(0, VerticalLongPressPopup.selectedIndex(200f, 200f, 40f, 2))
    }
    
    @Test
    fun `moving up one cell selects the first secondary`() {
        assertEquals(1, VerticalLongPressPopup.selectedIndex(180f, 200f, 40f, 2))
    }
    
    @Test
    fun `moving further up selects higher secondaries`() {
        assertEquals(2, VerticalLongPressPopup.selectedIndex(140f, 200f, 40f, 2))
    }
    
    @Test
    fun `above the top secondary clamps to the highest`() {
        assertEquals(2, VerticalLongPressPopup.selectedIndex(-500f, 200f, 40f, 2))
    }
    
    @Test
    fun `with no secondaries only the primary is selectable`() {
        assertEquals(0, VerticalLongPressPopup.selectedIndex(-500f, 200f, 40f, 0))
    }
    
    @Test
    fun `invalid geometry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { VerticalLongPressPopup.selectedIndex(0f, 0f, 0f, 2) }
        assertThrows(IllegalArgumentException::class.java) { VerticalLongPressPopup.selectedIndex(0f, 0f, 40f, -1) }
    }
}

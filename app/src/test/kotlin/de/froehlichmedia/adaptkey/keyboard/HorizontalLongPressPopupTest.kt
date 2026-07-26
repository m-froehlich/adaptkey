// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-44 horizontal long-press popup layout + selection geometry - D-144: cells may
 * have different widths (content-sized), not just a single uniform value.
 */
class HorizontalLongPressPopupTest {
    
    // A three-cell row (`? . !`) of equal 40px cells: with `.` (index 1) centred over a key at x=300, the
    // row should start at 300 - 1.5*40 = 240, giving cells [240,280) idx0, [280,320) idx1, [320,360) idx2.
    @Test
    fun `equal-width cells - the row is centred so the pre-selected cell sits over the stem`() {
        assertEquals(240f, HorizontalLongPressPopup.rowLeft(300f, listOf(40f, 40f, 40f), 1, 0f, 1000f))
    }
    
    @Test
    fun `equal-width cells - the row is clamped to the left edge`() {
        assertEquals(5f, HorizontalLongPressPopup.rowLeft(20f, listOf(40f, 40f, 40f), 1, 5f, 1000f))
    }
    
    @Test
    fun `equal-width cells - the row is clamped to the right edge`() {
        assertEquals(600f, HorizontalLongPressPopup.rowLeft(2000f, listOf(40f, 40f, 40f), 1, 5f, 600f))
    }
    
    @Test
    fun `an impossible clamp range falls back to the minimum`() {
        assertEquals(5f, HorizontalLongPressPopup.rowLeft(300f, listOf(40f, 40f, 40f), 1, 5f, 0f))
    }
    
    @Test
    fun `equal-width cells - the finger over a cell selects that cell`() {
        assertEquals(0, HorizontalLongPressPopup.selectedIndex(250f, 240f, listOf(40f, 40f, 40f)))
        assertEquals(1, HorizontalLongPressPopup.selectedIndex(300f, 240f, listOf(40f, 40f, 40f)))
        assertEquals(2, HorizontalLongPressPopup.selectedIndex(350f, 240f, listOf(40f, 40f, 40f)))
    }
    
    @Test
    fun `equal-width cells - the finger past either end clamps to the edge cell`() {
        assertEquals(0, HorizontalLongPressPopup.selectedIndex(-500f, 240f, listOf(40f, 40f, 40f)))
        assertEquals(2, HorizontalLongPressPopup.selectedIndex(5000f, 240f, listOf(40f, 40f, 40f)))
    }
    
    // D-144: a wide cell (e.g. "https://") among narrower ones - each cell's own span is honoured, not a
    // uniform division of the row.
    @Test
    fun `variable-width cells - the pre-selected cell centres over the stem using its own width`() {
        // Cells [20, 30, 100] (px), pre-selecting index 2 (the 100px-wide one): offset to its centre is
        // 20 + 30 + 50 = 100, so the row starts at keyCenterX - 100.
        assertEquals(200f, HorizontalLongPressPopup.rowLeft(300f, listOf(20f, 30f, 100f), 2, 0f, 1000f))
    }
    
    @Test
    fun `variable-width cells - each cell's own span is used for hit-testing`() {
        val widths = listOf(20f, 30f, 100f)
        // Cell spans from rowLeft=0: [0,20) idx0, [20,50) idx1, [50,150) idx2.
        assertEquals(0, HorizontalLongPressPopup.selectedIndex(10f, 0f, widths))
        assertEquals(1, HorizontalLongPressPopup.selectedIndex(35f, 0f, widths))
        assertEquals(2, HorizontalLongPressPopup.selectedIndex(120f, 0f, widths))
    }
    
    @Test
    fun `variable-width cells - the finger past either end clamps to the edge cell`() {
        val widths = listOf(20f, 30f, 100f)
        assertEquals(0, HorizontalLongPressPopup.selectedIndex(-500f, 0f, widths))
        assertEquals(2, HorizontalLongPressPopup.selectedIndex(5000f, 0f, widths))
    }
    
    @Test
    fun `invalid geometry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.selectedIndex(0f, 0f, emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.selectedIndex(0f, 0f, listOf(0f, 40f)) }
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.rowLeft(0f, emptyList(), 1, 0f, 100f) }
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.rowLeft(0f, listOf(0f, 40f), 0, 0f, 100f) }
    }
    
    // D-282: a 7-cell popup (matching PI_ALTERNATIVES' own width) pre-selected at index 0, centred on a key
    // near the keyboard's right edge - the same shape the D-99 'p' key hits. Ideal left would be
    // 980 - 20 = 960, past maxLeft=800, so this must clamp.
    @Test
    fun `wouldClampRight is true for a right-edge key with a wide popup`() {
        val widths = List(7) { 40f }
        assertEquals(true, HorizontalLongPressPopup.wouldClampRight(980f, widths, 0, 800f))
    }
    
    // The same key position, but a narrow 2-cell popup (matching O_ALTERNATIVES' own shape) fits without
    // clamping: ideal left = 980 - 20 = 960, still past 800 here too - so a genuinely narrow popup on a
    // key comfortably inside maxLeft's own reach must NOT clamp.
    @Test
    fun `wouldClampRight is false when the popup comfortably fits`() {
        val widths = listOf(40f, 40f)
        assertEquals(false, HorizontalLongPressPopup.wouldClampRight(300f, widths, 0, 800f))
    }
    
    @Test
    fun `wouldClampRight is false for a mid-row key even with a wide popup`() {
        val widths = List(7) { 40f }
        assertEquals(false, HorizontalLongPressPopup.wouldClampRight(300f, widths, 0, 800f))
    }
    
    @Test
    fun `wouldClampRight uses the pre-selected index, not always the first cell`() {
        val widths = listOf(40f, 40f, 40f)
        // Pre-selecting a later cell shrinks the offset-adjusted ideal position (more of the row already
        // sits left of keyCenterX), so the same keyCenterX/maxLeft clamps for index 0 but not for index 2.
        assertEquals(true, HorizontalLongPressPopup.wouldClampRight(980f, widths, 0, 900f))
        assertEquals(false, HorizontalLongPressPopup.wouldClampRight(980f, widths, 2, 900f))
    }
    
    @Test
    fun `wouldClampRight rejects an empty cell list`() {
        assertThrows(IllegalArgumentException::class.java) { HorizontalLongPressPopup.wouldClampRight(0f, emptyList(), 0, 100f) }
    }
}

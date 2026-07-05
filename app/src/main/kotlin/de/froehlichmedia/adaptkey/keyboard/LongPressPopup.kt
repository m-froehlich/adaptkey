// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure selection geometry for the D-01 multi-alternative long-press popup.
 *
 * The popup lays out [count] equal-width cells starting at a left edge; sliding the finger left/right
 * over them changes the highlighted alternative, and releasing commits it. This helper maps a pointer
 * x-coordinate to a cell index, clamped to the valid range, so the view only owns the drawing and the
 * touch plumbing while the index maths stays Android-free and unit-testable.
 */
object LongPressPopup {
    
    /**
     * The cell index the pointer at [pointerX] falls on, clamped to `0 until count`. A pointer left of
     * the popup selects the first cell, one to the right of it the last.
     *
     * @param pointerX the current pointer x in the same coordinate space as [popupLeft]
     * @param popupLeft the left edge of the first cell
     * @param cellWidth the width of one cell; must be > 0
     * @param count the number of cells; must be > 0
     * @return the selected index in `0 until count`
     * @throws IllegalArgumentException when [cellWidth] is not positive or [count] is not positive
     */
    fun selectedIndex(pointerX: Float, popupLeft: Float, cellWidth: Float, count: Int): Int {
        require(cellWidth > 0f) { "cellWidth must be > 0" }
        require(count > 0) { "count must be > 0" }
        val raw = ((pointerX - popupLeft) / cellWidth).toInt()
        return raw.coerceIn(0, count - 1)
    }
}

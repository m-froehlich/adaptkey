// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure layout + selection geometry for the D-44 horizontal long-press popup.
 *
 * The alternatives are shown as a single row of equal-width cells directly above the pressed key. The row
 * is centred over the key's stem so the pre-selected cell (the key's own character) sits right above the
 * finger; the finger then slides left/right **below** the row and the cell above the pointer's x is
 * selected. This replaces the D-23 vertical column: it maps a pointer x-coordinate to the selected index
 * and computes the (clamped) row origin, leaving the drawing and touch plumbing to the view.
 */
object HorizontalLongPressPopup {
    
    /**
     * The left edge of the cell row so that the [preSelectedIndex]-th cell is centred over [keyCenterX],
     * clamped so the whole row stays within `[minLeft, maxLeft]` (the visible keyboard width).
     *
     * @param keyCenterX the x-centre of the pressed key (the stem the row is centred over)
     * @param cellWidth the width of one cell; must be > 0
     * @param count the number of alternatives; must be >= 1
     * @param preSelectedIndex the pre-selected cell index (the key's own character); clamped into range
     * @param minLeft the smallest allowed row-left (usually a small edge gap)
     * @param maxLeft the largest allowed row-left (view width - row width - edge gap)
     * @return the clamped x of the row's left edge
     * @throws IllegalArgumentException when [cellWidth] is not positive or [count] is < 1
     */
    fun rowLeft(keyCenterX: Float, cellWidth: Float, count: Int, preSelectedIndex: Int, minLeft: Float, maxLeft: Float): Float {
        require(cellWidth > 0f) { "cellWidth must be > 0" }
        require(count >= 1) { "count must be >= 1" }
        val index = preSelectedIndex.coerceIn(0, count - 1)
        val ideal = keyCenterX - (index + 0.5f) * cellWidth
        if (maxLeft < minLeft) {
            return minLeft
        }
        return ideal.coerceIn(minLeft, maxLeft)
    }
    
    /**
     * The selected alternatives index for a pointer at [pointerX], picking the cell above the finger.
     *
     * @param pointerX the current pointer x (view pixels)
     * @param rowLeft the x of the row's left edge (from [rowLeft])
     * @param cellWidth the width of one cell; must be > 0
     * @param count the number of alternatives; must be >= 1
     * @return the selected index in `0 until count` (clamped in range)
     * @throws IllegalArgumentException when [cellWidth] is not positive or [count] is < 1
     */
    fun selectedIndex(pointerX: Float, rowLeft: Float, cellWidth: Float, count: Int): Int {
        require(cellWidth > 0f) { "cellWidth must be > 0" }
        require(count >= 1) { "count must be >= 1" }
        return ((pointerX - rowLeft) / cellWidth).toInt().coerceIn(0, count - 1)
    }
}

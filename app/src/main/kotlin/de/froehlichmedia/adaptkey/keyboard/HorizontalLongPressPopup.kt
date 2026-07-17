// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure layout + selection geometry for the D-44 horizontal long-press popup.
 *
 * The alternatives are shown as a single row of cells directly above the pressed key. The row is centred
 * over the key's stem so the pre-selected cell (the key's own character) sits right above the finger; the
 * finger then slides left/right **below** the row and the cell above the pointer's x is selected. This
 * replaces the D-23 vertical column: it maps a pointer x-coordinate to the selected index and computes the
 * (clamped) row origin, leaving the drawing and touch plumbing to the view.
 *
 * D-144: cells are no longer assumed equal-width - [cellWidths] gives each cell its own width (the view
 * sizes each to its own text content, e.g. a URL protocol popup's `https://`/`ftp://` entries need far more
 * room than a single glyph), so every position here is computed from the actual per-cell widths rather than
 * a uniform `index * cellWidth`.
 */
object HorizontalLongPressPopup {
    
    /**
     * The left edge of the cell row so that the [preSelectedIndex]-th cell is centred over [keyCenterX],
     * clamped so the whole row stays within `[minLeft, maxLeft]` (the visible keyboard width).
     *
     * @param keyCenterX the x-centre of the pressed key (the stem the row is centred over)
     * @param cellWidths each cell's own width, left to right; must be non-empty, every width > 0
     * @param preSelectedIndex the pre-selected cell index (the key's own character); clamped into range
     * @param minLeft the smallest allowed row-left (usually a small edge gap)
     * @param maxLeft the largest allowed row-left (view width - row width - edge gap)
     * @return the clamped x of the row's left edge
     * @throws IllegalArgumentException when [cellWidths] is empty or any width is not positive
     */
    fun rowLeft(keyCenterX: Float, cellWidths: List<Float>, preSelectedIndex: Int, minLeft: Float, maxLeft: Float): Float {
        require(cellWidths.isNotEmpty()) { "cellWidths must not be empty" }
        require(cellWidths.all { it > 0f }) { "every cell width must be > 0" }
        val index = preSelectedIndex.coerceIn(0, cellWidths.size - 1)
        val offsetToPreSelectedCenter = cellWidths.take(index).sum() + cellWidths[index] / 2f
        val ideal = keyCenterX - offsetToPreSelectedCenter
        if (maxLeft < minLeft) {
            return minLeft
        }
        return ideal.coerceIn(minLeft, maxLeft)
    }
    
    /**
     * The selected alternatives index for a pointer at [pointerX], picking whichever cell's own span
     * (measured left to right from [rowLeft], using each cell's own [cellWidths] entry) contains it.
     *
     * @param pointerX the current pointer x (view pixels)
     * @param rowLeft the x of the row's left edge (from [rowLeft])
     * @param cellWidths each cell's own width, left to right; must be non-empty, every width > 0
     * @return the selected index in `0 until cellWidths.size` (clamped in range)
     * @throws IllegalArgumentException when [cellWidths] is empty or any width is not positive
     */
    fun selectedIndex(pointerX: Float, rowLeft: Float, cellWidths: List<Float>): Int {
        require(cellWidths.isNotEmpty()) { "cellWidths must not be empty" }
        require(cellWidths.all { it > 0f }) { "every cell width must be > 0" }
        var right = rowLeft
        for (index in cellWidths.indices) {
            right += cellWidths[index]
            if (pointerX < right) {
                return index
            }
        }
        return cellWidths.size - 1
    }
}

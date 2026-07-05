// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure selection geometry for the D-23 vertical long-press popup.
 *
 * The alternatives list is `[primary, secondary1, secondary2, ...]`. The primary (index 0) sits offset
 * at the top-left of the key and is pre-selected; the secondaries are stacked in a vertical column
 * directly above the finger, bottom-to-top - so `secondary1` (index 1) is the cell immediately above the
 * key and higher indices are further up. Sliding the finger up the column selects higher secondaries;
 * keeping it on the key keeps the primary. This helper maps a pointer y-coordinate to the selected index,
 * leaving the drawing and touch plumbing to the view.
 */
object VerticalLongPressPopup {
    
    /**
     * The selected alternatives index for a pointer at [pointerY].
     *
     * @param pointerY the current pointer y (view pixels)
     * @param stackBottom the y of the bottom of the secondary column (the key's top edge)
     * @param cellHeight the height of one secondary cell; must be > 0
     * @param secondaryCount the number of secondaries (alternatives after the primary); must be >= 0
     * @return 0 for the primary, or `1 + k` for the k-th secondary from the bottom (clamped in range)
     * @throws IllegalArgumentException when [cellHeight] is not positive or [secondaryCount] is negative
     */
    fun selectedIndex(pointerY: Float, stackBottom: Float, cellHeight: Float, secondaryCount: Int): Int {
        require(cellHeight > 0f) { "cellHeight must be > 0" }
        require(secondaryCount >= 0) { "secondaryCount must be >= 0" }
        if (secondaryCount == 0 || pointerY >= stackBottom) {
            return 0
        }
        val fromBottom = ((stackBottom - pointerY) / cellHeight).toInt().coerceIn(0, secondaryCount - 1)
        return 1 + fromBottom
    }
}

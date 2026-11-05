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
 *
 * D-510 (Alt-Belegungs-Reform plan §2.11): the right-edge overflow handling is a **drift-gated clamp**,
 * replacing D-282's unconditional order reversal - the row is shifted left by exactly the missing pixels
 * as long as the pre-selected cell's centre stays within the pressed key's own horizontal bounds
 * ([placementStrategy]'s [PopupRowStrategy.DRIFT_CLAMPED]); only when the drift would push that cell past
 * the key's left edge does D-282's own reversal remain the last resort ([PopupRowStrategy.REVERSED], the
 * shape the `p` key keeps). A key's dual-corner popup ([isDualCornerStraddle]) is placed by
 * [cornerRowPlacement]: centred over the host key (at most one extra, an edge overflow flipping the extra
 * toward the screen interior), or growing toward the screen centre with the first extra kept in the
 * middle between the corners (two or more extras, the `7` key's `\ ⁷ / ÷ &`).
 */
object HorizontalLongPressPopup {
    
    /**
     * D-510 (plan §2.11): how a popup row must be placed.
     *
     * [CENTRED] - D-44's centring (with the ordinary clamps) fits; the row's pre-selected cell sits over
     * the key centre. [DRIFT_CLAMPED] - centring would overflow the screen's right edge, but shifting the
     * row left by exactly the missing pixels keeps the pre-selected cell's centre within the pressed key's
     * own bounds (the o-key fix: `[ö over key, Ø right of it]`). [REVERSED] - even that drift would push
     * the pre-selected cell out of the key, so D-282's own order reversal stays the last resort (the p
     * key's π-right-aligned, extras-grow-left shape).
     */
    enum class PopupRowStrategy { CENTRED, DRIFT_CLAMPED, REVERSED }
    
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
    
    /**
     * Whether centring [cellWidths] over [keyCenterX] (with [preSelectedIndex] as the pre-selected cell)
     * would need [rowLeft] to clamp the row leftward - i.e. the popup has no room to grow rightward
     * from the key's own position, exactly the geometry a key near a row's right edge runs into.
     *
     * D-510: no longer the reversal decision itself (that moved to [placementStrategy], whose
     * drift-gated clamp answers the overflow with a plain leftward shift whenever it can) - this stays
     * as the raw overflow predicate for tests and any caller that only needs to know whether the ideal
     * position overflows.
     *
     * @param keyCenterX the x-centre of the pressed key
     * @param cellWidths each cell's own width, left to right, in the caller's own (unreversed) order
     * @param preSelectedIndex the pre-selected cell index in that same unreversed order; clamped into range
     * @param maxLeft the largest allowed row-left (view width - row width - edge gap)
     * @return true when the unclamped ideal position would exceed [maxLeft]
     * @throws IllegalArgumentException when [cellWidths] is empty
     */
    fun wouldClampRight(keyCenterX: Float, cellWidths: List<Float>, preSelectedIndex: Int, maxLeft: Float): Boolean {
        require(cellWidths.isNotEmpty()) { "cellWidths must not be empty" }
        val index = preSelectedIndex.coerceIn(0, cellWidths.size - 1)
        val offsetToPreSelectedCenter = cellWidths.take(index).sum() + cellWidths[index] / 2f
        val ideal = keyCenterX - offsetToPreSelectedCenter
        return ideal > maxLeft
    }
    
    /**
     * D-510 (plan §2.11.3/4): the drift-gated clamp decision for a right-edge overflow, replacing D-282's
     * unconditional reversal. Centring is answered with [PopupRowStrategy.CENTRED] while it fits;
     * [PopupRowStrategy.DRIFT_CLAMPED] shifts the row left by exactly the missing pixels when that keeps
     * the pre-selected cell's centre within the pressed key's own horizontal bounds (a leftward shift can
     * only carry that cell toward the key's interior, so only the key's left edge can be violated); and
     * only [PopupRowStrategy.REVERSED] - D-282's own purpose, kept as the last resort - when even the
     * drift would push it past the key's left edge (the p key's shape). A single-cell row is never
     * reversed (there is nothing to reorder); it keeps the plain clamp it always had, which is the same
     * position the drift produces.
     *
     * @param keyCenterX the x-centre the row is centred over (the view passes the D-54-nudged centre)
     * @param keyLeft the pressed key's own left edge - the drift guard's boundary
     * @param cellWidths each cell's own width, left to right, in the caller's own (unreversed) order
     * @param preSelectedIndex the pre-selected cell index in that same unreversed order; clamped into range
     * @param maxLeft the largest allowed row-left (view width - row width - edge gap)
     * @throws IllegalArgumentException when [cellWidths] is empty
     */
    fun placementStrategy(
        keyCenterX: Float,
        keyLeft: Float,
        cellWidths: List<Float>,
        preSelectedIndex: Int,
        maxLeft: Float
    ): PopupRowStrategy {
        require(cellWidths.isNotEmpty()) { "cellWidths must not be empty" }
        val index = preSelectedIndex.coerceIn(0, cellWidths.size - 1)
        val offsetToPreSelectedCenter = cellWidths.take(index).sum() + cellWidths[index] / 2f
        val ideal = keyCenterX - offsetToPreSelectedCenter
        if (ideal <= maxLeft) {
            return PopupRowStrategy.CENTRED
        }
        val overflow = ideal - maxLeft
        return if (cellWidths.size == 1 || keyCenterX - overflow >= keyLeft) {
            // A single-cell row is never reversed (there is nothing to reorder); it keeps the plain clamp
            // it always had, which is exactly the position the drift produces.
            PopupRowStrategy.DRIFT_CLAMPED
        } else {
            PopupRowStrategy.REVERSED
        }
    }
    
    /**
     * D-510 (plan §2.11.1): whether [alternatives] is a dual-corner key's own corner popup - the reform's
     * explicit data shape `[leftCorner, extras…, rightCorner]`, recognised by the key carrying both
     * corners ([Key.leftHint] and [Key.hint]) and the list starting with the one and ending with the
     * other. Such a row is placed by [cornerRowPlacement]; any other list (a centred popup, a pack-defined
     * extra set that does not follow the corner contract) stays on the ordinary placement.
     */
    fun isDualCornerStraddle(key: Key, alternatives: List<String>): Boolean {
        val left = key.leftHint
        val right = key.hint
        return left != null && right != null &&
            alternatives.size >= 2 &&
            alternatives.first() == left &&
            alternatives.last() == right
    }
    
    /**
     * D-510 follow-up (device-corrected placement): the placed corner row - the (possibly reordered)
     * alternatives and their matching widths, the pre-selected cell index (always the right-corner
     * symbol, the release default), and the row's left edge.
     */
    data class CornerRow(
        val rowLeft: Float,
        val alternatives: List<String>,
        val cellWidths: List<Float>,
        val preSelectedIndex: Int
    )
    
    /**
     * The corner row's cells reordered so the EXTRAS TRAIL BEHIND the right corner: `[left, right, extras…]`.
     * The display-time flip for a corner row that would overflow the screen's LEFT edge (the `1` key:
     * `~ ¹ !` becomes `~ ! ¹`, so both corner symbols stay near the host and the superscript hangs right
     * where there is room); the release default stays the right corner, now the second cell.
     */
    fun <T> extrasTrailingRow(row: List<T>): List<T> {
        require(row.size >= 2) { "a corner row carries both corners" }
        return listOf(row.first(), row.last()) + row.subList(1, row.lastIndex)
    }
    
    /**
     * The corner row's cells reordered so the EXTRAS LEAD AHEAD of the left corner: `[extras…, left, right]`.
     * The display-time flip for a corner row that would overflow the screen's RIGHT edge (the `0` key:
     * `= ⁰ )` becomes `⁰ = )`, so both corner symbols stay near the host and the superscript hangs left
     * where there is room); the release default stays the right corner, still the last cell.
     */
    fun <T> extrasLeadingRow(row: List<T>): List<T> {
        require(row.size >= 2) { "a corner row carries both corners" }
        return row.subList(1, row.lastIndex) + listOf(row.first(), row.last())
    }
    
    /**
     * The corner row's cells reordered for the GROW case (three or more extras, i.e. four or more cells):
     * the FIRST extra stays in the middle between the corners over the host (the `7` key's division sign),
     * and the remaining extras move to the grow side - `[left, middle, right, rest…]` when the row grows
     * RIGHT (a key in the screen's left half), `[rest…, left, middle, right]` when it grows LEFT (a key
     * in the screen's right half, the `7` key: `\ ⁷ / ÷ &`). The release default stays the right corner.
     */
    fun <T> growTrailingRow(row: List<T>): List<T> {
        require(row.size >= 4) { "the grow split needs both corners, a middle cell and at least one grow extra" }
        return listOf(row[0], row[1], row.last()) + row.subList(2, row.lastIndex)
    }
    
    /** The grow-case mirror of [growTrailingRow]: the rest lead ahead, the middle cell stays between the corners. */
    fun <T> growLeadingRow(row: List<T>): List<T> {
        require(row.size >= 4) { "the grow split needs both corners, a middle cell and at least one grow extra" }
        return row.subList(2, row.lastIndex) + listOf(row[0], row[1], row.last())
    }
    
    /**
     * D-510 follow-up (device-corrected placement, second device pass): positions a dual-corner key's
     * popup row.
     *
     * With at most ONE extra (two or three cells) the row is CENTRED over the host key - the left-corner
     * cell lands over the key's left half, the right-corner cell (pre-selected, the release default) over
     * its right edge, the extra in between; at a screen-edge overflow the single extra flips to the far
     * side so the corners never leave the host: trailing behind the right corner at the left edge
     * ([extrasTrailingRow], the `1` key's `~ ! ¹`), leading ahead of the left corner at the right edge
     * ([extrasLeadingRow], the `0` key's `⁰ = )`) - with D-44's own pre-selected centring placing the
     * flipped row, so the right-corner symbol stays over the key.
     *
     * With TWO OR MORE extras (four or more cells) they cannot all fit between the corners over the host,
     * so the row GROWS toward the screen centre (D-54's own direction: a key in the left half grows
     * right, a key in the right half grows left): the FIRST extra stays in the middle between the two
     * corners directly over the host (the `7` key's `÷`), the remaining extras attach on the grow side,
     * and the placement anchors the middle cell over the key centre - the `7` key renders as
     * `\ ⁷ / ÷ &` with `/ ÷ &` directly over the host and the backslash/superscript hanging left.
     * The release default stays the right-corner cell in every order.
     *
     * @param keyCenterX the host key's own centre
     * @param viewWidth the view's own width (decides which screen half the key sits in, hence the grow side)
     * @param alternatives the corner row `[leftCorner, extras…, rightCorner]` (the straddle contract)
     * @param cellWidths the matching per-cell widths
     * @param minLeft the smallest allowed row-left (usually a small edge gap)
     * @param maxLeft the largest allowed row-left (view width - row width - edge gap)
     * @throws IllegalArgumentException when [alternatives] has fewer than two cells or the widths do not match
     */
    fun cornerRowPlacement(
        keyCenterX: Float,
        viewWidth: Float,
        alternatives: List<String>,
        cellWidths: List<Float>,
        minLeft: Float,
        maxLeft: Float
    ): CornerRow {
        require(alternatives.size >= 2) { "a corner row carries both corners" }
        require(cellWidths.size == alternatives.size) { "every cell needs its own width" }
        val lastIndex = alternatives.lastIndex
        if (alternatives.size <= 3) {
            val ideal = keyCenterX - cellWidths.sum() / 2f
            return when {
                ideal >= minLeft && ideal <= maxLeft -> CornerRow(ideal, alternatives, cellWidths, lastIndex)
                ideal < minLeft -> {
                    val widths = extrasTrailingRow(cellWidths)
                    CornerRow(rowLeft(keyCenterX, widths, 1, minLeft, maxLeft), extrasTrailingRow(alternatives), widths, 1)
                }
                else -> {
                    val widths = extrasLeadingRow(cellWidths)
                    CornerRow(rowLeft(keyCenterX, widths, widths.lastIndex, minLeft, maxLeft), extrasLeadingRow(alternatives), widths, widths.lastIndex)
                }
            }
        }
        return if (keyCenterX < viewWidth / 2f) {
            // Left half: grow right - the rest trail behind the right corner, the middle cell over the key centre.
            val widths = growTrailingRow(cellWidths)
            CornerRow(rowLeft(keyCenterX, widths, 1, minLeft, maxLeft), growTrailingRow(alternatives), widths, 2)
        } else {
            // Right half: grow left - the rest lead ahead of the left corner, the middle cell over the key centre.
            val widths = growLeadingRow(cellWidths)
            CornerRow(rowLeft(keyCenterX, widths, widths.lastIndex - 1, minLeft, maxLeft), growLeadingRow(alternatives), widths, widths.lastIndex)
        }
    }
}

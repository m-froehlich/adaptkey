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
 * toward the screen interior); from two extras on the row stays symmetric (D-525) - the corners keep the
 * flanking positions and the extras interleave right/left in turn around the centre (the `7` key's
 * `/ \ ÷ ⁷ &`).
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
     * D-525: the symmetric corner row - the placed display order for a dual-corner key's popup with TWO
     * OR MORE extras. Both corners keep the two-symbol flank positions in the row (left corner first,
     * right corner last - over the host key's edges once the row is centred), and the extras interleave
     * RIGHT AND LEFT IN TURN around the row's centre, the first extra landing nearest the centre on the
     * right side: `[left, x4, x2, x1, x3, right]` for extras `x1..x4`. This replaces the D-510
     * one-sided grow split (all extras attaching on one grow side, which hung the far cells far too
     * wide of the host); rows with at most ONE extra keep the plain centred arrangement, which already
     * reads exactly like this.
     */
    fun <T> symmetricCornerRow(row: List<T>): List<T> {
        require(row.size >= 4) { "the symmetric arrangement needs both corners and at least two extras" }
        val extras = row.subList(1, row.lastIndex)
        val rights = extras.filterIndexed { index, _ -> index % 2 == 0 }
        val lefts = extras.filterIndexed { index, _ -> index % 2 == 1 }
        return listOf(row.first()) + lefts.asReversed() + rights + listOf(row.last())
    }
    
    /**
     * D-520: the currency slot's alternating popup - the placed row and the anchor's index within it.
     */
    data class AlternatingRow<T>(val row: List<T>, val anchorIndex: Int)
    
    /**
     * D-520: the alternating arrangement for the letters page's currency slot - the ANCHOR cell (the
     * key's most relevant secondary currency, the popup's first entry past the key's own left corner)
     * stays directly over the host key, and the remaining cells alternate left/right around it in the
     * list's own relevance order, so the second entry lands immediately left of the anchor, the third
     * immediately right, the fourth two-left, and so on (the German example: euro on the d key, dollar
     * centred above it, yuan left, pound right). Any cells AHEAD of the anchor keep their order at the
     * row's far left - a dual-corner key's left corner (`°` on the d key), preserving the straddle
     * convention that a left corner is the row's first cell.
     *
     * The row is placed by the ordinary [rowLeft] centring with [AlternatingRow.anchorIndex] as the
     * pre-selected position - the anchor over the host key centre, clamped at the screen edges like
     * every wide popup - but carries NO pre-selected cell in the selection sense: the key's own
     * release default (the corner glyph drawn on the key itself, deliberately absent from this popup)
     * commits on a straight release, and only a genuine slide commits the cell the finger reached.
     *
     * @param row the alternatives in relevance order, `[leftCorner]?, anchor, rest…`
     * @param anchorIndex the anchor cell's index in [row] - the first entry past the key's own left corner
     * @throws IllegalArgumentException when [row] is empty or [anchorIndex] is out of range
     */
    fun <T> alternatingRow(row: List<T>, anchorIndex: Int): AlternatingRow<T> {
        require(row.isNotEmpty()) { "a popup row is never empty" }
        require(anchorIndex in row.indices) { "anchor index $anchorIndex out of range for ${row.size} cells" }
        val anchor = row[anchorIndex]
        val before = row.subList(0, anchorIndex)
        val after = row.subList(anchorIndex + 1, row.size)
        val lefts = after.filterIndexed { index, _ -> index % 2 == 0 }.reversed()
        val rights = after.filterIndexed { index, _ -> index % 2 == 1 }
        return AlternatingRow(before + lefts + listOf(anchor) + rights, before.size + lefts.size)
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
     * With TWO OR MORE extras (four or more cells) the row stays SYMMETRIC (D-525, replacing the D-510
     * one-sided grow split whose far cells hung far too wide of the host): [symmetricCornerRow]
     * interleaves the extras right/left in turn around the row's centre - the first extra nearest the
     * centre on the right side, the corners keeping the row's flanking positions - and the whole row is
     * centred over the host key (plainly clamped at the screen edges). The `7` key now renders as
     * `/ \ ÷ ⁷ &` and the `2` key as `" ² ' ₂ @`, both corners over the host's edges. The release
     * default stays the right-corner cell in every arrangement.
     *
     * @param keyCenterX the host key's own centre
     * @param viewWidth the view's own width (kept for placement-context parity with the grow case it replaces)
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
        // D-525: the symmetric wide case - the interleaved row centred over the host key, plainly
        // clamped at the screen edges; the pre-selected cell stays the right corner (the row's last).
        val display = symmetricCornerRow(alternatives)
        val widths = symmetricCornerRow(cellWidths)
        val ideal = keyCenterX - widths.sum() / 2f
        val rowLeft = if (maxLeft < minLeft) minLeft else ideal.coerceIn(minLeft, maxLeft)
        return CornerRow(rowLeft, display, widths, lastIndex)
    }
}

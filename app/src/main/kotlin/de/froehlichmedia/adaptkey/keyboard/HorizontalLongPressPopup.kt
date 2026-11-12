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
 * [cornerRowPlacement]: the corner pair centred over the host key (at most one extra, an edge overflow
 * flipping the extra toward the screen interior); from two extras on the pair STILL stays centred
 * (D-526) - the extras queue outward around it, alternating right-first, one-sided once a side runs
 * out of room (the `7` key's `\ / & ÷ ⁷` with / & centred over the host).
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
     * D-526: the placed corner row for the confirmed placement strategy - the corner PAIR stays centred
     * over the host key in every arrangement, and the extras queue outward around it, alternating
     * right-first; a side that has run out of room (screen edge) receives all remaining extras alone.
     */
    data class CornerRow(
        val rowLeft: Float,
        val alternatives: List<String>,
        val cellWidths: List<Float>,
        val preSelectedIndex: Int
    )
    
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
     * D-526 (the user's confirmed strategy, superseding the D-510 grow split and the D-525 symmetric
     * interleave): positions a dual-corner key's popup row.
     *
     * THE CORNER PAIR STAYS CENTRED OVER THE HOST KEY in every arrangement, exactly where the
     * two-symbol popup puts it: the left-corner cell over the key's left corner, the right-corner cell
     * (the release default, always the pre-selected cell) over its right corner. From the THIRD symbol
     * on, the extras queue OUTWARD around that pair, alternately: the first extra immediately RIGHT of
     * the pair, the second immediately LEFT, the third right again outside the first, and so on, in the
     * extras' own list order. When one side has run out of room (the screen edge), all remaining
     * extras queue only on the other side; the pair itself only ever shifts when even it cannot sit
     * centred within the bounds, and the whole row is clamped if (never today: at most three extras)
     * both sides are exhausted at once.
     *
     * The `7` key (`/ ÷ \ ⁷ &`) renders as `\ / & ÷ ⁷` with `/ &` centred over the host, the `2` key
     * (`" ' ² ₂ @`) as `² " @ ' ₂` with `" @` centred; the `1` key at the screen's left edge keeps
     * `~ ! ¹` (¹ queues right where the room is) and the `0` key at the right edge keeps `⁰ = )`
     * (⁰ queues left) - the same shapes the old edge flips produced, now as the general rule.
     *
     * @param keyCenterX the host key's own centre
     * @param alternatives the corner row `[leftCorner, extras…, rightCorner]` (the straddle contract)
     * @param cellWidths the matching per-cell widths
     * @param minLeft the smallest allowed row-left (usually a small edge gap)
     * @param maxRight the largest allowed row-right (view width - the same edge gap)
     * @throws IllegalArgumentException when [alternatives] has fewer than two cells or the widths do not match
     */
    fun cornerRowPlacement(
        keyCenterX: Float,
        alternatives: List<String>,
        cellWidths: List<Float>,
        minLeft: Float,
        maxRight: Float
    ): CornerRow {
        require(alternatives.size >= 2) { "a corner row carries both corners" }
        require(cellWidths.size == alternatives.size) { "every cell needs its own width" }
        // The pair sits exactly where the two-symbol popup puts it (row centred on the pair), clamped
        // at the screen edges - it never moves again once the extras queue around it.
        val pairWidth = cellWidths.first() + cellWidths.last()
        var leftExtent = (keyCenterX - pairWidth / 2f).coerceIn(minLeft, (maxRight - pairWidth).coerceAtLeast(minLeft))
        var rightExtent = leftExtent + pairWidth
        val rights = ArrayDeque<String>()
        val rightWidths = ArrayDeque<Float>()
        val lefts = ArrayDeque<String>()
        val leftWidths = ArrayDeque<Float>()
        var rightExhausted = false
        var leftExhausted = false
        var nextRight = true
        for (index in 1 until alternatives.lastIndex) {
            val extra = alternatives[index]
            val width = cellWidths[index]
            val wantRight = (nextRight && !rightExhausted) || leftExhausted
            val placedRight = if (wantRight) {
                if (rightExtent + width <= maxRight) {
                    rights.addLast(extra)
                    rightWidths.addLast(width)
                    rightExtent += width
                    true
                } else {
                    // The right side has run out of room - this and every further extra queue left.
                    rightExhausted = true
                    false
                }
            } else {
                false
            }
            if (!placedRight) {
                if (!leftExhausted && leftExtent - width >= minLeft) {
                    lefts.addFirst(extra)
                    leftWidths.addFirst(width)
                    leftExtent -= width
                } else if (rightExtent + width <= maxRight) {
                    // The left side has run out of room - this and every further extra queue right.
                    leftExhausted = true
                    rights.addLast(extra)
                    rightWidths.addLast(width)
                    rightExtent += width
                } else {
                    // Both sides exhausted (never today: at most three extras) - queue right anyway;
                    // the row is then wider than the bounds allow and the caller's own placement
                    // clamps it. Documented fallback, not a tuned path.
                    rights.addLast(extra)
                    rightWidths.addLast(width)
                    rightExtent += width
                }
            }
            nextRight = !nextRight
        }
        val display = lefts.toList() + listOf(alternatives.first(), alternatives.last()) + rights.toList()
        val widths = leftWidths.toList() + listOf(cellWidths.first(), cellWidths.last()) + rightWidths.toList()
        // The release default stays the right-corner cell: immediately right of the row's centre.
        return CornerRow(leftExtent, display, widths, lefts.size + 1)
    }
}

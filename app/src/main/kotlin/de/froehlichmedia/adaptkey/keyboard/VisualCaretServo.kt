// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * D-401-followup: the pure half of the space-bar cursor gesture's screen-space model - "drive the caret
 * towards a point on the screen", replacing "advance the caret by N characters through the document".
 *
 * **Why a servo and not a calculation.** The gesture's whole remaining problem was that an IME cannot see
 * the target app's text layout: a paragraph the app soft-wraps across three visible lines is one single
 * line to `InputConnection`, so "one line down" could only ever mean "next paragraph". `CursorAnchorInfo`
 * closes exactly that gap without exposing the layout - it reports where the caret is *drawn*, so a soft
 * wrap is observable as a jump in the caret's own y coordinate. A device log (Google Keep, §472) confirmed
 * it: within one 107-character paragraph with no newline at all, offset 0 was drawn at y 539 and offsets
 * 55-80 at y 596, one row lower.
 *
 * Those reports arrive one at a time, only for wherever the caret currently is - never as a layout table -
 * so the position cannot be *computed*, only *converged on*: this class proposes an offset, the caller
 * applies it, the editor reports where that landed, and the error feeds the next proposal. Holding the
 * finger still therefore settles within a few frames rather than requiring the estimate to be right first
 * time. That also makes it robust against the one thing a computation would have to get right and could
 * not: the same log shows per-character advances between 11 and 24 pixels (an ordinary proportional font),
 * so no fixed character width would ever place the caret correctly.
 *
 * **The user's own two rules fall out of the geometry rather than being enforced by thresholds.** Dragging
 * sideways cannot change lines, because a sideways drag targets a point on the same row - and the row a
 * given offset lives on is learned from real observations ([rowBounds]), not guessed. Dragging down changes
 * the *visible* line, because the target point is simply one row height lower. The previous model's
 * dominant-axis gate and its dp-per-step constants are both unnecessary here: hand wobble of 40 px against
 * a 53 px row height targets the same row, geometrically, with nothing to tune.
 *
 * Deliberately holds no Android type at all (the caller converts [android.view.inputmethod.CursorAnchorInfo]
 * into [observe] calls), mirroring the split [CursorControlGesture] already uses.
 *
 * @param rowTolerancePx how far apart two reported caret tops may be and still count as the same visible
 *        row - absorbs the small variation real editors report (the device log had 53 px rows next to 57
 *        and 58 px ones, depending on the line's own content)
 */
class VisualCaretServo(private val rowTolerancePx: Float = DEFAULT_ROW_TOLERANCE_PX) {
    
    /** One reported "text offset [offset] is drawn here" fact, in the caret coordinates the caller uses. */
    data class Observation(val offset: Int, val x: Float, val top: Float, val bottom: Float)
    
    private val observations = LinkedHashMap<Int, Observation>()
    private var current: Observation? = null
    private var pendingOffset: Int? = null
    
    /** Where the caret was last *reported* to be, or null while nothing has been reported yet. */
    fun currentObservation(): Observation? = current
    
    /** Whether enough has been reported for [targetOffset] to be able to propose anything at all. */
    fun isReady(): Boolean = current != null
    
    /** Forgets everything - a new gesture, or a new field, shares nothing with the previous one. */
    fun reset() {
        observations.clear()
        current = null
        pendingOffset = null
    }
    
    /**
     * Records that the caller has just asked the editor to put the caret at [offset], so the stale report
     * still in flight for the *previous* offset is not mistaken for the caret's new position - the device
     * log shows those arriving routinely, one report behind.
     */
    fun expect(offset: Int) {
        pendingOffset = offset
    }
    
    /**
     * Records a reported caret position. Every report is kept as layout knowledge (that is what makes
     * [rowBounds] exact rather than guessed), but only a report the caller is actually waiting for - or any
     * report at all while it waits for none - becomes the caret's current position.
     */
    fun observe(offset: Int, x: Float, top: Float, bottom: Float) {
        if (bottom <= top) {
            return
        }
        val observation = Observation(offset, x, top, bottom)
        observations[offset] = observation
        val pending = pendingOffset
        if (pending == null) {
            current = observation
        } else if (pending == offset) {
            current = observation
            pendingOffset = null
        }
    }
    
    /**
     * The caret's own average character advance, learned from pairs of reports that landed on the same
     * visible row - the one number the offset estimate below is scaled by.
     *
     * @return the learned advance in pixels, or a row-height-derived fallback while nothing has been
     *         learned yet (deliberately derived from the row height rather than a fixed pixel constant, so
     *         it is at least in the right order of magnitude on any display density)
     */
    fun averageCharWidth(): Float {
        var totalPixels = 0f
        var totalChars = 0
        for (row in observations.values.groupBy { rowKey(it.top) }.values) {
            if (row.size < 2) {
                continue
            }
            val first = row.minBy { it.offset }
            val last = row.maxBy { it.offset }
            val chars = last.offset - first.offset
            val pixels = last.x - first.x
            if (chars > 0 && pixels > 0f) {
                totalPixels += pixels
                totalChars += chars
            }
        }
        if (totalChars > 0) {
            return totalPixels / totalChars
        }
        val rowHeight = current?.let { it.bottom - it.top } ?: return FALLBACK_CHAR_WIDTH_PX
        return rowHeight * FALLBACK_CHAR_WIDTH_PER_ROW_HEIGHT
    }
    
    /**
     * The offset to move the caret to so that it lands as close as possible to ([targetX], [targetY]), or
     * null when it is already there (or when nothing has been reported yet, so there is no error to act
     * on). One step of the loop, not a final answer: the caller applies it, the editor reports where that
     * actually landed, and the next call refines from there.
     *
     * A proposal is clamped into whatever is *known* about the target row's own offset range, so a
     * horizontal drag can never leave its row even when the character-width estimate briefly overshoots -
     * and the bound tightens on its own as soon as an offset on a neighbouring row has been seen once.
     *
     * @param minOffset the lowest offset the caller can address (usually the start of the text window it
     *        can read); the proposal is clamped into `[minOffset, maxOffset]`
     * @param maxOffset the highest offset the caller can address
     */
    fun targetOffset(targetX: Float, targetY: Float, minOffset: Int, maxOffset: Int): Int? {
        val cur = current ?: return null
        val rowHeight = cur.bottom - cur.top
        val charWidth = averageCharWidth()
        if (rowHeight <= 0f || charWidth <= 0f) {
            return null
        }
        val rowSteps = ((targetY - (cur.top + rowHeight / 2f)) / rowHeight).roundToInt()
        val targetRowTop = cur.top + rowSteps * rowHeight
        if (rowSteps == 0 && abs(targetX - cur.x) < charWidth * SETTLED_FRACTION_OF_CHAR) {
            return null
        }
        val anchor = anchorOn(targetRowTop, targetX)
        val estimate = if (anchor != null) {
            anchor.offset + ((targetX - anchor.x) / charWidth).roundToInt()
        } else {
            cur.offset + rowSteps * charsPerRow(charWidth) + ((targetX - cur.x) / charWidth).roundToInt()
        }
        val bounds = rowBounds(targetRowTop)
        val proposal = estimate.coerceIn(bounds.first, bounds.last).coerceIn(minOffset, maxOffset)
        return if (proposal == cur.offset) null else proposal
    }
    
    /**
     * The best report to measure the estimate from: one already known to be on the target row, whose own x
     * is nearest the target - so returning to a row this gesture has already visited is close to exact
     * rather than re-derived from a character-width average. Null when that row has never been seen.
     */
    private fun anchorOn(rowTop: Float, targetX: Float): Observation? {
        return observations.values
            .filter { abs(it.top - rowTop) <= rowTolerancePx }
            .minByOrNull { abs(it.x - targetX) }
    }
    
    /**
     * What is *known* about the offset range of the row at [rowTop]: rows are contiguous ranges of offsets,
     * so a single report on a row above bounds this row from below, and one on a row below bounds it from
     * above. Unbounded in whichever direction nothing has been seen yet.
     */
    private fun rowBounds(rowTop: Float): IntRange {
        var lower = Int.MIN_VALUE
        var upper = Int.MAX_VALUE
        for (observation in observations.values) {
            if (observation.top < rowTop - rowTolerancePx) {
                lower = maxOf(lower, observation.offset + 1)
            } else if (observation.top > rowTop + rowTolerancePx) {
                upper = minOf(upper, observation.offset - 1)
            }
        }
        return if (lower > upper) lower..lower else lower..upper
    }
    
    /**
     * How many characters a full visible row holds - only ever needed to reach a row that has never been
     * reported, and deliberately allowed to be a poor estimate: too small simply costs another pass round
     * the loop, while the row's own reports take over as the anchor the moment it has been visited once.
     */
    private fun charsPerRow(charWidth: Float): Int {
        val acrossRows = learnedCharsPerRow(charWidth)
        if (acrossRows != null) {
            return acrossRows
        }
        val widest = observations.values.maxOfOrNull { it.x } ?: return MIN_CHARS_PER_ROW
        val narrowest = observations.values.minOfOrNull { it.x } ?: return MIN_CHARS_PER_ROW
        return maxOf(MIN_CHARS_PER_ROW, ((widest - narrowest) / charWidth).roundToInt())
    }
    
    /**
     * The same figure taken from real evidence where possible: two reports on vertically adjacent rows
     * differ by exactly one row's worth of characters, once the horizontal distance between them is
     * discounted.
     */
    private fun learnedCharsPerRow(charWidth: Float): Int? {
        val rows = observations.values.groupBy { rowKey(it.top) }.toSortedMap()
        val tops = rows.keys.toList()
        for (index in 0 until tops.size - 1) {
            val upper = rows[tops[index]]?.maxByOrNull { it.offset } ?: continue
            val lower = rows[tops[index + 1]]?.minByOrNull { it.offset } ?: continue
            if (lower.offset <= upper.offset) {
                continue
            }
            val columnDifference = ((lower.x - upper.x) / charWidth).roundToInt()
            val perRow = lower.offset - upper.offset - columnDifference
            if (perRow >= MIN_CHARS_PER_ROW) {
                return perRow
            }
        }
        return null
    }
    
    /** Collapses reported tops that differ only by [rowTolerancePx] onto one comparable row identity. */
    private fun rowKey(top: Float): Int = (top / maxOf(rowTolerancePx, 1f)).roundToInt()
    
    companion object {
        
        /** Reported caret tops within this distance of each other are treated as the same visible row. */
        const val DEFAULT_ROW_TOLERANCE_PX = 8f
        
        /**
         * How close to the target the caret must already be for [targetOffset] to propose nothing at all -
         * without a dead band the loop would keep stepping back and forth by one character around a target
         * that falls between two of them.
         */
        const val SETTLED_FRACTION_OF_CHAR = 0.5f
        
        /** A row must be assumed to hold at least this many characters; only ever an initial guess. */
        const val MIN_CHARS_PER_ROW = 8
        
        /** The character-width guess before anything has been learned, as a fraction of the row height. */
        const val FALLBACK_CHAR_WIDTH_PER_ROW_HEIGHT = 0.4f
        
        /** The last-resort character width, used only when not even a row height is known yet. */
        const val FALLBACK_CHAR_WIDTH_PX = 20f
    }
}

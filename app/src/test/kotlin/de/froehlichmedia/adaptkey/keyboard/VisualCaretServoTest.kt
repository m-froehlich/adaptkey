// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * D-401-followup: exercises [VisualCaretServo] against a synthetic text layout that behaves like a real
 * editor - a proportional font, soft wrapping at a fixed width, and hard newlines - so the loop can be run
 * to completion in a plain JVM test: the servo proposes an offset, the fake layout reports where that
 * offset is drawn, and the servo refines from there, exactly as the real
 * [android.view.inputmethod.CursorAnchorInfo] callbacks drive it on a device.
 *
 * The soft-wrap cases are the point of the whole class: those are the ones the previous, newline-only model
 * could not express at all.
 */
class VisualCaretServoTest {
    
    /**
     * A deliberately proportional, deliberately soft-wrapping fake editor layout - the two properties that
     * made a computed (rather than converged-on) caret position impossible in the first place.
     */
    private class FakeLayout(private val text: String, private val maxWidth: Float = 200f) {
        
        val rows: List<IntRange> = buildRows()
        val rowHeight = 50f
        val leftEdge = 10f
        
        private fun widthOf(char: Char): Float = when (char) {
            'i', 'l', '.' -> 5f
            'm', 'w' -> 25f
            else -> 10f
        }
        
        private fun buildRows(): List<IntRange> {
            val result = ArrayList<IntRange>()
            var start = 0
            var width = 0f
            var index = 0
            while (index < text.length) {
                val char = text[index]
                if (char == '\n') {
                    result.add(start..index)
                    start = index + 1
                    width = 0f
                    index++
                    continue
                }
                val advance = widthOf(char)
                if (width + advance > maxWidth && index > start) {
                    result.add(start..index - 1)
                    start = index
                    width = 0f
                    continue
                }
                width += advance
                index++
            }
            result.add(start..text.length)
            return result
        }
        
        fun rowIndexOf(offset: Int): Int = rows.indexOfFirst { offset in it }
        
        fun xOf(offset: Int): Float {
            val row = rows[rowIndexOf(offset)]
            var x = leftEdge
            for (index in row.first until offset) {
                x += widthOf(text[index])
            }
            return x
        }
        
        fun topOf(offset: Int): Float = rowIndexOf(offset) * rowHeight
        
        /** The offset whose drawn position is closest to ([x], [y]) - what the servo should converge on. */
        fun nearestOffset(x: Float, y: Float): Int {
            val rowIndex = (y / rowHeight).toInt().coerceIn(0, rows.size - 1)
            return rows[rowIndex].minBy { abs(xOf(it) - x) }
        }
    }
    
    /**
     * Runs the closed loop the real service runs: propose, apply, report back, repeat - until the servo
     * proposes nothing further, which is what "the caret has arrived" means here.
     *
     * @return the offset the loop settled on
     */
    private fun settle(servo: VisualCaretServo, layout: FakeLayout, startOffset: Int, targetX: Float, targetY: Float): Int {
        servo.observe(startOffset, layout.xOf(startOffset), layout.topOf(startOffset), layout.topOf(startOffset) + layout.rowHeight)
        var offset = startOffset
        repeat(MAX_LOOP_PASSES) {
            val next = servo.targetOffset(targetX, targetY, 0, layout.rows.last().last) ?: return offset
            servo.expect(next)
            servo.observe(next, layout.xOf(next), layout.topOf(next), layout.topOf(next) + layout.rowHeight)
            offset = next
        }
        return offset
    }
    
    @Test
    fun `proposes nothing before the first position has been reported`() {
        val servo = VisualCaretServo()
        
        assertFalse(servo.isReady())
        assertNull(servo.targetOffset(100f, 100f, 0, 500))
        assertNull(servo.currentObservation())
    }
    
    @Test
    fun `converges rightwards within one row`() {
        val layout = FakeLayout("the quick brown fox jumps over the lazy dog")
        val servo = VisualCaretServo()
        val expected = layout.nearestOffset(120f, 25f)
        
        val settled = settle(servo, layout, 2, 120f, 25f)
        
        assertEquals(expected, settled)
        assertEquals(0, layout.rowIndexOf(settled))
    }
    
    @Test
    fun `converges leftwards within one row`() {
        val layout = FakeLayout("the quick brown fox jumps over the lazy dog")
        val servo = VisualCaretServo()
        val expected = layout.nearestOffset(30f, 25f)
        
        val settled = settle(servo, layout, 15, 30f, 25f)
        
        assertEquals(expected, settled)
    }
    
    @Test
    fun `a sideways target never leaves its own row even when it points past the row end`() {
        val layout = FakeLayout("the quick brown fox jumps over the lazy dog")
        val servo = VisualCaretServo()
        
        val settled = settle(servo, layout, 5, 10_000f, 25f)
        
        assertEquals(0, layout.rowIndexOf(settled))
        assertEquals(layout.rows[0].last, settled)
    }
    
    @Test
    fun `a sideways target never leaves its own row towards the start either`() {
        val layout = FakeLayout("the quick brown fox jumps over the lazy dog")
        val servo = VisualCaretServo()
        val startOffset = layout.rows[1].first + 3
        
        val settled = settle(servo, layout, startOffset, -10_000f, layout.rowHeight * 1.5f)
        
        assertEquals(1, layout.rowIndexOf(settled))
        assertEquals(layout.rows[1].first, settled)
    }
    
    @Test
    fun `moves one visible row down inside a soft-wrapped paragraph that contains no newline at all`() {
        val text = "the quick brown fox jumps over the lazy dog and keeps running well past the wrap"
        val layout = FakeLayout(text)
        val servo = VisualCaretServo()
        assertFalse(text.contains('\n'))
        assertTrue(layout.rows.size >= 3)
        val targetX = layout.xOf(4)
        val targetY = layout.rowHeight * 1.7f
        
        val settled = settle(servo, layout, 4, targetX, targetY)
        
        assertEquals(1, layout.rowIndexOf(settled))
        assertTrue(abs(layout.xOf(settled) - targetX) <= COLUMN_TOLERANCE_PX)
    }
    
    @Test
    fun `moves one visible row up inside a soft-wrapped paragraph`() {
        val layout = FakeLayout("the quick brown fox jumps over the lazy dog and keeps running well past the wrap")
        val servo = VisualCaretServo()
        val startOffset = layout.rows[2].first + 2
        val targetX = layout.xOf(startOffset)
        val targetY = layout.rowHeight * 1.3f
        
        val settled = settle(servo, layout, startOffset, targetX, targetY)
        
        assertEquals(1, layout.rowIndexOf(settled))
    }
    
    @Test
    fun `comes back to the target row after an early proposal overshoots onto another one`() {
        // The loop's first proposal knows nothing yet and can genuinely land a row or two out; refusing to
        // return would strand the caret there. The deadband that prevents an *unintended* line change lives
        // at the input instead - see CursorControlGesture.targetPointFor.
        val layout = FakeLayout("first paragraph\n\nthird paragraph")
        val servo = VisualCaretServo()
        val targetRow = layout.rows[2]
        servo.observe(0, layout.xOf(0), layout.topOf(0), layout.topOf(0) + layout.rowHeight)
        val overshoot = layout.rows.last().last
        servo.expect(overshoot)
        servo.observe(overshoot, layout.xOf(overshoot), layout.topOf(overshoot), layout.topOf(overshoot) + layout.rowHeight)
        
        val settled = settle(servo, layout, overshoot, layout.leftEdge, layout.rowHeight * 2.5f)
        
        assertEquals(2, layout.rowIndexOf(settled))
        assertEquals(targetRow.first, settled)
    }
    
    
    @Test
    fun `crosses several visible rows in one drag`() {
        val layout = FakeLayout("the quick brown fox jumps over the lazy dog and keeps running well past the wrap")
        val servo = VisualCaretServo()
        val lastRow = layout.rows.size - 1
        val targetY = lastRow * layout.rowHeight + layout.rowHeight / 2f
        
        val settled = settle(servo, layout, 3, layout.leftEdge + 20f, targetY)
        
        assertEquals(lastRow, layout.rowIndexOf(settled))
    }
    
    @Test
    fun `handles hard newlines and a genuinely empty line between two paragraphs`() {
        val layout = FakeLayout("first paragraph\n\nthird paragraph")
        val servo = VisualCaretServo()
        val emptyRow = layout.rows[1]
        assertEquals(emptyRow.first, emptyRow.last)
        
        val settled = settle(servo, layout, 3, layout.leftEdge + 500f, layout.rowHeight * 1.7f)
        
        assertEquals(emptyRow.first, settled)
    }
    
    @Test
    fun `leaves an empty line again on the next vertical move`() {
        val layout = FakeLayout("first paragraph\n\nthird paragraph")
        val servo = VisualCaretServo()
        val emptyOffset = layout.rows[1].first
        settle(servo, layout, 3, layout.leftEdge + 500f, layout.rowHeight * 1.7f)
        
        val settled = settle(servo, layout, emptyOffset, layout.leftEdge, layout.rowHeight * 2.7f)
        
        assertEquals(2, layout.rowIndexOf(settled))
    }
    
    @Test
    fun `learns a character width from reports on the same row`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        servo.observe(20, 300f, 0f, 50f)
        
        assertEquals(20f, servo.averageCharWidth(), 0.001f)
    }
    
    @Test
    fun `falls back to a row-height-derived character width while nothing is learned`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        
        assertEquals(50f * VisualCaretServo.FALLBACK_CHAR_WIDTH_PER_ROW_HEIGHT, servo.averageCharWidth(), 0.001f)
    }
    
    @Test
    fun `ignores a stale report for an offset that is no longer the one being waited for`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        servo.expect(20)
        
        servo.observe(15, 200f, 0f, 50f)
        
        assertEquals(10, servo.currentObservation()?.offset)
    }
    
    @Test
    fun `accepts the report it is waiting for and stops waiting afterwards`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        servo.expect(20)
        servo.observe(20, 300f, 0f, 50f)
        
        servo.observe(25, 400f, 0f, 50f)
        
        assertEquals(25, servo.currentObservation()?.offset)
    }
    
    @Test
    fun `ignores a report with no vertical extent at all`() {
        val servo = VisualCaretServo()
        
        servo.observe(10, 100f, 40f, 40f)
        
        assertFalse(servo.isReady())
    }
    
    @Test
    fun `proposes nothing once the caret is already within half a character of the target`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        servo.observe(20, 300f, 0f, 50f)
        
        assertNull(servo.targetOffset(305f, 25f, 0, 500))
    }
    
    @Test
    fun `resetting forgets every learned position`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        servo.observe(20, 300f, 0f, 50f)
        assertNotNull(servo.currentObservation())
        
        servo.reset()
        
        assertFalse(servo.isReady())
        assertNull(servo.currentObservation())
        assertEquals(VisualCaretServo.FALLBACK_CHAR_WIDTH_PX, servo.averageCharWidth(), 0.001f)
    }
    
    @Test
    fun `never proposes an offset outside the addressable window`() {
        val servo = VisualCaretServo()
        servo.observe(10, 100f, 0f, 50f)
        servo.observe(20, 300f, 0f, 50f)
        
        val proposal = servo.targetOffset(10_000f, 25f, 5, 15)
        
        assertNotNull(proposal)
        assertTrue(proposal!! in 5..15)
    }
    
    private companion object {
        
        /** Enough passes for any convergence in these fixtures; a real drag settles in two or three. */
        const val MAX_LOOP_PASSES = 40
        
        /** A settled caret may sit up to one fake glyph away from a target that falls between two of them. */
        const val COLUMN_TOLERANCE_PX = 25f
    }
}

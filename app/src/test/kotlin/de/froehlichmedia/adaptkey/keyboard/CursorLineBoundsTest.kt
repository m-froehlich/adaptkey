// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * D-401-followup: the Stage 2 cases are the reason this class exists - a device log showed the selection
 * freezing after one move because the surrounding text on the far side of the selection was never consulted
 * at all.
 */
class CursorLineBoundsTest {
    
    @Test
    fun collapsedSelectionUsesTheSurroundingTextDirectly() {
        val bounds = CursorLineBounds.of(10, "0123456789", "", "abc\ndef", movingEndIsSelectionEnd = true)
        
        assertEquals(0, bounds.first)
        assertEquals(13, bounds.last)
    }
    
    @Test
    fun collapsedSelectionIsUnaffectedByTheDirectionFlag() {
        val forwards = CursorLineBounds.of(10, "0123456789", "", "abc\ndef", movingEndIsSelectionEnd = true)
        val backwards = CursorLineBounds.of(10, "0123456789", "", "abc\ndef", movingEndIsSelectionEnd = false)
        
        assertEquals(forwards, backwards)
    }
    
    @Test
    fun findsTheLineStartAfterALineBreakInTheSurroundingText() {
        val bounds = CursorLineBounds.of(10, "abc\ndef123", "", "xy", movingEndIsSelectionEnd = true)
        
        assertEquals(4, bounds.first)
        assertEquals(12, bounds.last)
    }
    
    @Test
    fun aForwardSelectionCountsItsOwnTextTowardsTheLineStart() {
        // The regression: the moving end is the selection's end, so the text before it is everything before
        // the selection *plus* the selection itself - not the selection alone.
        val bounds = CursorLineBounds.of(20, "0123456789", "abcdefghij", "rest", movingEndIsSelectionEnd = true)
        
        assertEquals(0, bounds.first)
        assertEquals(24, bounds.last)
    }
    
    @Test
    fun aForwardSelectionWithoutAnyLineBreakStillYieldsAUsableLineStart() {
        // Previously this returned null and abandoned the move entirely, freezing the selection on device.
        val bounds = CursorLineBounds.of(67, "x".repeat(66), "y", "z".repeat(66), movingEndIsSelectionEnd = true)
        
        assertEquals(0, bounds.first)
        assertEquals(133, bounds.last)
    }
    
    @Test
    fun aForwardSelectionHonoursALineBreakInsideItself() {
        val bounds = CursorLineBounds.of(20, "0123456789", "abcd\nefghi", "rest", movingEndIsSelectionEnd = true)
        
        assertEquals(15, bounds.first)
        assertEquals(24, bounds.last)
    }
    
    @Test
    fun aBackwardSelectionCountsItsOwnTextTowardsTheLineEnd() {
        val bounds = CursorLineBounds.of(10, "0123456789", "abcdefghij", "rest", movingEndIsSelectionEnd = false)
        
        assertEquals(0, bounds.first)
        assertEquals(24, bounds.last)
    }
    
    @Test
    fun aBackwardSelectionWithoutAnyLineBreakStillYieldsAUsableLineEnd() {
        // The mirror image of the same device-reported freeze, reached by dragging left instead of right.
        val bounds = CursorLineBounds.of(61, "x".repeat(61), "y".repeat(6), "z".repeat(66), movingEndIsSelectionEnd = false)
        
        assertEquals(0, bounds.first)
        assertEquals(133, bounds.last)
    }
    
    @Test
    fun aBackwardSelectionHonoursALineBreakInsideItself() {
        val bounds = CursorLineBounds.of(10, "0123456789", "abcd\nefghi", "rest", movingEndIsSelectionEnd = false)
        
        assertEquals(0, bounds.first)
        assertEquals(14, bounds.last)
    }
    
    @Test
    fun anEmptyLineCollapsesToASinglePosition() {
        val bounds = CursorLineBounds.of(108, "text\n", "", "\nmore", movingEndIsSelectionEnd = true)
        
        assertEquals(108, bounds.first)
        assertEquals(108, bounds.last)
    }
    
    @Test
    fun theStartOfTheDocumentIsAValidLineStart() {
        val bounds = CursorLineBounds.of(0, "", "", "line", movingEndIsSelectionEnd = true)
        
        assertEquals(0, bounds.first)
        assertEquals(4, bounds.last)
    }
    
    @Test
    fun theEndOfTheDocumentIsAValidLineEnd() {
        val bounds = CursorLineBounds.of(4, "line", "", "", movingEndIsSelectionEnd = true)
        
        assertEquals(0, bounds.first)
        assertEquals(4, bounds.last)
    }
    
    @Test
    fun aNegativePositionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CursorLineBounds.of(-1, "", "", "", movingEndIsSelectionEnd = true)
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure accelerating-backspace policy (D-07 / D-31).
 */
class BackspaceRepeatTest {
    
    @Test
    fun `character-wise deletion starts at the moderate start interval`() {
        assertEquals(BackspaceRepeat.CHAR_START_DELAY_MS, BackspaceRepeat.nextDelayMs(0))
    }
    
    @Test
    fun `character-wise deletion accelerates only moderately down to a floor`() {
        var previous = Long.MAX_VALUE
        var reachedFloor = false
        for (chars in 0 until BackspaceRepeat.WORD_MODE_AFTER_CHARS) {
            val delay = BackspaceRepeat.nextDelayMs(chars)
            assertTrue(delay <= previous, "delay must not grow at $chars chars")
            assertTrue(delay >= BackspaceRepeat.CHAR_MIN_DELAY_MS, "delay must never drop below the floor")
            if (delay == BackspaceRepeat.CHAR_MIN_DELAY_MS) {
                reachedFloor = true
            }
            previous = delay
        }
        assertTrue(reachedFloor, "the moderate acceleration reaches the floor before word mode")
    }
    
    @Test
    fun `word-wise deletion uses the slower fixed cadence`() {
        assertEquals(BackspaceRepeat.WORD_DELAY_MS, BackspaceRepeat.nextDelayMs(BackspaceRepeat.WORD_MODE_AFTER_CHARS))
        assertEquals(BackspaceRepeat.WORD_DELAY_MS, BackspaceRepeat.nextDelayMs(BackspaceRepeat.WORD_MODE_AFTER_CHARS + 40))
    }
    
    @Test
    fun `the word-wise cadence is clearly slower than the fastest character-wise one (D-31)`() {
        assertTrue(BackspaceRepeat.WORD_DELAY_MS > BackspaceRepeat.CHAR_MIN_DELAY_MS)
    }
    
    @Test
    fun `a negative count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { BackspaceRepeat.nextDelayMs(-1) }
    }
    
    @Test
    fun `deletion stays character-wise below the word threshold`() {
        assertFalse(BackspaceRepeat.deletesWord(0))
        assertFalse(BackspaceRepeat.deletesWord(BackspaceRepeat.WORD_MODE_AFTER_CHARS - 1))
    }
    
    @Test
    fun `deletion switches to word-wise at and beyond the threshold`() {
        assertTrue(BackspaceRepeat.deletesWord(BackspaceRepeat.WORD_MODE_AFTER_CHARS))
        assertTrue(BackspaceRepeat.deletesWord(BackspaceRepeat.WORD_MODE_AFTER_CHARS + 30))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure accelerating-backspace policy (D-07).
 */
class BackspaceRepeatTest {
    
    @Test
    fun `first repeat starts at the slow interval`() {
        assertEquals(BackspaceRepeat.START_DELAY_MS, BackspaceRepeat.nextDelayMs(0))
    }
    
    @Test
    fun `the interval strictly decreases while accelerating`() {
        var previous = Long.MAX_VALUE
        var reachedFloor = false
        for (step in 0..40) {
            val delay = BackspaceRepeat.nextDelayMs(step)
            assertTrue(delay <= previous, "delay must not grow at step $step")
            assertTrue(delay >= BackspaceRepeat.MIN_DELAY_MS, "delay must never drop below the floor")
            if (delay == BackspaceRepeat.MIN_DELAY_MS) {
                reachedFloor = true
            }
            previous = delay
        }
        assertTrue(reachedFloor, "the acceleration must eventually reach the floor")
    }
    
    @Test
    fun `the interval is clamped to the floor and never goes below it`() {
        assertEquals(BackspaceRepeat.MIN_DELAY_MS, BackspaceRepeat.nextDelayMs(1000))
    }
    
    @Test
    fun `a negative step is rejected`() {
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

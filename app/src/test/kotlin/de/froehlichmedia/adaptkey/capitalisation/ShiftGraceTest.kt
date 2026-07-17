// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure C-07 shift-grace decisions: word-start auto-arming, which arms are guarded,
 * and when a Shift press is suppressed within the grace window.
 */
class ShiftGraceTest {
    
    @Test
    fun `WORDS and CHARACTERS auto-arm every word start`() {
        assertTrue(ShiftGrace.autoArmAtWordStart(CapsMode.WORDS, sentenceStart = false))
        assertTrue(ShiftGrace.autoArmAtWordStart(CapsMode.WORDS, sentenceStart = true))
        assertTrue(ShiftGrace.autoArmAtWordStart(CapsMode.CHARACTERS, sentenceStart = false))
    }
    
    @Test
    fun `SENTENCES auto-arms only at a sentence start`() {
        assertTrue(ShiftGrace.autoArmAtWordStart(CapsMode.SENTENCES, sentenceStart = true))
        assertFalse(ShiftGrace.autoArmAtWordStart(CapsMode.SENTENCES, sentenceStart = false))
    }
    
    @Test
    fun `NONE still auto-arms at a sentence start - D-110, AdaptKey's own rule applies regardless of the field`() {
        assertTrue(ShiftGrace.autoArmAtWordStart(CapsMode.NONE, sentenceStart = true))
        assertFalse(ShiftGrace.autoArmAtWordStart(CapsMode.NONE, sentenceStart = false))
    }
    
    @Test
    fun `only a mid-sentence per-word field mandate is guarded`() {
        assertTrue(ShiftGrace.isGuardedArm(CapsMode.WORDS, sentenceStart = false))
        assertTrue(ShiftGrace.isGuardedArm(CapsMode.CHARACTERS, sentenceStart = false))
        assertFalse(ShiftGrace.isGuardedArm(CapsMode.WORDS, sentenceStart = true))
        assertFalse(ShiftGrace.isGuardedArm(CapsMode.SENTENCES, sentenceStart = false))
        assertFalse(ShiftGrace.isGuardedArm(CapsMode.NONE, sentenceStart = false))
    }
    
    @Test
    fun `a guarded armed press inside the window is suppressed`() {
        assertTrue(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = true, windowMs = 300L, elapsedMs = 0L))
        assertTrue(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = true, windowMs = 300L, elapsedMs = 299L))
    }
    
    @Test
    fun `a press at or past the window boundary toggles normally`() {
        assertFalse(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = true, windowMs = 300L, elapsedMs = 300L))
        assertFalse(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = true, windowMs = 300L, elapsedMs = 5_000L))
    }
    
    @Test
    fun `a zero window disables the guard entirely`() {
        assertFalse(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = true, windowMs = 0L, elapsedMs = 0L))
    }
    
    @Test
    fun `a non-guarded or non-armed press is never suppressed`() {
        assertFalse(ShiftGrace.suppressesShiftPress(guardedArm = false, currentlyUpper = true, windowMs = 300L, elapsedMs = 10L))
        assertFalse(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = false, windowMs = 300L, elapsedMs = 10L))
    }
    
    @Test
    fun `a negative elapsed time is not treated as inside the window`() {
        assertFalse(ShiftGrace.suppressesShiftPress(guardedArm = true, currentlyUpper = true, windowMs = 300L, elapsedMs = -5L))
    }
}

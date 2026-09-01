// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-389 learned-word expiry window enum and its stored-value resolution.
 */
class LearnedWordExpiryWindowTest {
    
    @Test
    fun `the default is never`() {
        assertEquals(LearnedWordExpiryWindow.NEVER, LearnedWordExpiryWindow.DEFAULT)
    }
    
    @Test
    fun `never has no day count`() {
        assertNull(LearnedWordExpiryWindow.NEVER.days)
    }
    
    @Test
    fun `each finite window's own day count matches its concrete duration label`() {
        assertEquals(30, LearnedWordExpiryWindow.ONE_MONTH.days)
        assertEquals(120, LearnedWordExpiryWindow.FOUR_MONTHS.days)
        assertEquals(365, LearnedWordExpiryWindow.ONE_YEAR.days)
    }
    
    @Test
    fun `known keys resolve regardless of case and surrounding space`() {
        assertEquals(LearnedWordExpiryWindow.ONE_MONTH, LearnedWordExpiryWindow.fromKey("one_month"))
        assertEquals(LearnedWordExpiryWindow.FOUR_MONTHS, LearnedWordExpiryWindow.fromKey("FOUR_MONTHS"))
        assertEquals(LearnedWordExpiryWindow.ONE_YEAR, LearnedWordExpiryWindow.fromKey("  one_year "))
        assertEquals(LearnedWordExpiryWindow.NEVER, LearnedWordExpiryWindow.fromKey("never"))
    }
    
    @Test
    fun `null blank and unknown keys fall back to the default`() {
        assertEquals(LearnedWordExpiryWindow.DEFAULT, LearnedWordExpiryWindow.fromKey(null))
        assertEquals(LearnedWordExpiryWindow.DEFAULT, LearnedWordExpiryWindow.fromKey("   "))
        assertEquals(LearnedWordExpiryWindow.DEFAULT, LearnedWordExpiryWindow.fromKey("bogus"))
    }
}

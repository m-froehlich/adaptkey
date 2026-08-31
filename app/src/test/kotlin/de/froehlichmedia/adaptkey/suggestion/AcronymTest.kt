// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-404-followup acronym shape check. */
class AcronymTest {
    
    @Test
    fun `a fully-uppercase word is an acronym`() {
        assertTrue(Acronym.isAcronym("ETF"))
        assertTrue(Acronym.isAcronym("AVD"))
    }
    
    @Test
    fun `a two-letter fully-uppercase word is still an acronym`() {
        assertTrue(Acronym.isAcronym("EU"))
    }
    
    @Test
    fun `a single uppercase letter is not an acronym`() {
        assertFalse(Acronym.isAcronym("E"))
    }
    
    @Test
    fun `an ordinary sentence-start-capitalised word is not an acronym`() {
        assertFalse(Acronym.isAcronym("Hund"))
    }
    
    @Test
    fun `an all-lowercase word is not an acronym`() {
        assertFalse(Acronym.isAcronym("etf"))
    }
    
    @Test
    fun `a word with one embedded capital but not fully uppercase is not an acronym`() {
        // The weaker, coincidental signal hasEmbeddedCapital() uses - a suspected unsplit compound like
        // "MeinAuto" must not be mistaken for an acronym.
        assertFalse(Acronym.isAcronym("MeinAuto"))
    }
    
    @Test
    fun `an empty string is not an acronym`() {
        assertFalse(Acronym.isAcronym(""))
    }
}

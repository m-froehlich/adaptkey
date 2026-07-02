// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the §6 abbreviation / enumerator recognition.
 */
class AbbreviationsTest {
    
    @Test
    fun `known German abbreviations are recognised case-insensitively`() {
        assertTrue(Abbreviations.isAbbreviation("usw."))
        assertTrue(Abbreviations.isAbbreviation("Nr."))
        assertTrue(Abbreviations.isAbbreviation("z.B."))
        assertTrue(Abbreviations.isAbbreviation("Z.B."))
    }
    
    @Test
    fun `ordinary words are not abbreviations`() {
        assertFalse(Abbreviations.isAbbreviation("Haus."))
        assertFalse(Abbreviations.isAbbreviation("danke."))
    }
    
    @Test
    fun `enumerators are digit runs followed by a period`() {
        assertTrue(Abbreviations.isEnumerator("1."))
        assertTrue(Abbreviations.isEnumerator("10."))
        assertFalse(Abbreviations.isEnumerator("1"))
        assertFalse(Abbreviations.isEnumerator("1.2"))
        assertFalse(Abbreviations.isEnumerator("a."))
    }
    
    @Test
    fun `isNonTerminalPeriod covers both abbreviations and enumerators`() {
        assertTrue(Abbreviations.isNonTerminalPeriod("usw."))
        assertTrue(Abbreviations.isNonTerminalPeriod("2."))
        assertFalse(Abbreviations.isNonTerminalPeriod("Satz."))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import org.junit.jupiter.api.Assertions.assertEquals
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
        assertTrue(Abbreviations.isAbbreviation("Stk."))
        assertTrue(Abbreviations.isAbbreviation("bzgl."))
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
    
    @Test
    fun `D-434 isAbbreviation only matches the explicitly passed-in set, not GERMAN unconditionally`() {
        assertFalse(Abbreviations.isAbbreviation("bzgl.", emptySet()))
        assertTrue(Abbreviations.isAbbreviation("etc.", setOf("etc.")))
        assertFalse(Abbreviations.isAbbreviation("etc.", setOf("z.b.")))
    }
    
    @Test
    fun `D-434 isAbbreviation defaults to GERMAN when no set is passed, matching the old behaviour`() {
        assertTrue(Abbreviations.isAbbreviation("bzgl."))
        assertFalse(Abbreviations.isAbbreviation("haus."))
    }
    
    @Test
    fun `D-434 isNonTerminalPeriod forwards the passed-in set to isAbbreviation`() {
        assertFalse(Abbreviations.isNonTerminalPeriod("bzgl.", emptySet()))
        assertTrue(Abbreviations.isNonTerminalPeriod("2.", emptySet()))
        assertTrue(Abbreviations.isNonTerminalPeriod("etc.", setOf("etc.")))
    }
    
    @Test
    fun `D-434 parse reads one lower-cased abbreviation per line`() {
        assertEquals(setOf("etc.", "vs."), Abbreviations.parse("etc.\nVS.\n"))
    }
    
    @Test
    fun `D-434 parse ignores blank lines and comment lines`() {
        assertEquals(setOf("etc."), Abbreviations.parse("\n# a comment\netc.\n   \n"))
    }
    
    @Test
    fun `D-434 parse of empty content yields an empty set`() {
        assertEquals(emptySet<String>(), Abbreviations.parse(""))
    }
}

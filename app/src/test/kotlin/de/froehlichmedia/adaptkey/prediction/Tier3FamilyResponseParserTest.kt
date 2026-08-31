// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-404 family-learning response parser. */
class Tier3FamilyResponseParserTest {
    
    @Test
    fun `parses a well-formed answer`() {
        val result = Tier3FamilyResponseParser.parse("Hund; WORTART=NOUN; FORMEN=Hunde,Hundes,Hunden")
        assertEquals("Hund", result.lemma)
        assertEquals(PartOfSpeech.NOUN, result.category)
        assertEquals(listOf("Hund", "Hunde", "Hundes", "Hunden"), result.family)
    }
    
    @Test
    fun `is tolerant of extra whitespace and lower-case keys`() {
        val result = Tier3FamilyResponseParser.parse("  Hund ;  wortart = noun ; formen = Hunde , Hunden ")
        assertEquals("Hund", result.lemma)
        assertEquals(PartOfSpeech.NOUN, result.category)
        assertTrue(result.family.containsAll(listOf("Hund", "Hunde", "Hunden")))
    }
    
    @Test
    fun `no plausible lemma yields the empty result`() {
        val result = Tier3FamilyResponseParser.parse("; WORTART=NOUN; FORMEN=Hunde")
        assertEquals(Tier3FamilyResult.EMPTY, result)
    }
    
    @Test
    fun `a lemma containing non-letters yields the empty result`() {
        val result = Tier3FamilyResponseParser.parse("H3llo; WORTART=NOUN; FORMEN=Hunde")
        assertEquals(Tier3FamilyResult.EMPTY, result)
    }
    
    @Test
    fun `a single-letter lemma yields the empty result`() {
        val result = Tier3FamilyResponseParser.parse("H; WORTART=NOUN; FORMEN=Hunde")
        assertEquals(Tier3FamilyResult.EMPTY, result)
    }
    
    @Test
    fun `an unrecognised category token yields a null category, not a discarded result`() {
        val result = Tier3FamilyResponseParser.parse("Hund; WORTART=UNKNOWN_TAG; FORMEN=Hunde")
        assertEquals("Hund", result.lemma)
        assertNull(result.category)
    }
    
    @Test
    fun `a missing category field yields a null category`() {
        val result = Tier3FamilyResponseParser.parse("Hund; FORMEN=Hunde")
        assertEquals("Hund", result.lemma)
        assertNull(result.category)
    }
    
    @Test
    fun `a missing forms field still yields the lemma alone in the family`() {
        val result = Tier3FamilyResponseParser.parse("Hund; WORTART=NOUN")
        assertEquals(listOf("Hund"), result.family)
    }
    
    @Test
    fun `forms containing non-letters are dropped, not the whole result`() {
        val result = Tier3FamilyResponseParser.parse("Hund; FORMEN=Hunde,H3, ,Hunden")
        assertTrue("Hunde" in result.family)
        assertTrue("Hunden" in result.family)
        assertTrue(result.family.none { it.any { c -> c.isDigit() } })
    }
    
    @Test
    fun `duplicate forms are de-duplicated case-insensitively`() {
        val result = Tier3FamilyResponseParser.parse("Hund; FORMEN=hund,Hund,HUND,Hunde")
        assertEquals(listOf("Hund", "Hunde"), result.family)
    }
    
    @Test
    fun `the family size is capped so a degenerate generation cannot flood the lexicon`() {
        val manyForms = (1..30).joinToString(",") { "form$it" }
        val result = Tier3FamilyResponseParser.parse("Grundform; FORMEN=$manyForms")
        assertTrue(result.family.size <= 16)
    }
    
    @Test
    fun `completely garbled text yields the empty result`() {
        val result = Tier3FamilyResponseParser.parse("###!!! nothing usable here 12345")
        assertEquals(Tier3FamilyResult.EMPTY, result)
    }
}

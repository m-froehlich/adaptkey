// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-404 conservative, lookup-only base-form linking. */
class LearnedLemmaLinkingTest {
    
    private fun learned(vararg words: String): (String) -> Boolean {
        val set = words.toSet()
        return { word -> set.contains(word) }
    }
    
    @Test
    fun `findLemma strips a genitive -es ending`() {
        assertEquals("hund", LearnedLemmaLinking.findLemma("hundes", learned("hund")))
    }
    
    @Test
    fun `findLemma strips a plural -e ending`() {
        assertEquals("hund", LearnedLemmaLinking.findLemma("hunde", learned("hund")))
    }
    
    @Test
    fun `findLemma strips a plural -en ending`() {
        assertEquals("frau", LearnedLemmaLinking.findLemma("frauen", learned("frau")))
    }
    
    @Test
    fun `findLemma strips a dative-plural -n ending`() {
        assertEquals("katze", LearnedLemmaLinking.findLemma("katzen", learned("katze")))
    }
    
    @Test
    fun `findLemma falls back to a verb personal ending when no noun ending matches`() {
        assertEquals("kaufen", LearnedLemmaLinking.findLemma("kauft", learned("kaufen")))
    }
    
    @Test
    fun `findLemma returns null when no candidate is already learned`() {
        assertNull(LearnedLemmaLinking.findLemma("hundes", learned("katze")))
    }
    
    @Test
    fun `findLemma returns null for a word too short to strip any ending`() {
        assertNull(LearnedLemmaLinking.findLemma("es", learned()))
    }
    
    @Test
    fun `findLemma never self-matches when reinforcing an already-learned word with no lemma yet`() {
        // D-404: a non-matching ending must not fall back to the whole word via a no-op removeSuffix -
        // "baum" does not end in any NOUN_ENDINGS suffix, but was previously vulnerable to exactly this.
        assertNull(LearnedLemmaLinking.findLemma("baum", learned("baum")))
    }
    
    @Test
    fun `findLemma is case-insensitive on its own input`() {
        assertEquals("hund", LearnedLemmaLinking.findLemma("Hundes", learned("hund")))
    }
    
    @Test
    fun `candidateInflections generates noun-style forms for any base`() {
        val forms = LearnedLemmaLinking.candidateInflections("Hund")
        assertTrue("hundes" in forms)
        assertTrue("hunde" in forms)
        assertTrue("hunden" in forms)
    }
    
    @Test
    fun `candidateInflections also generates verb-personal forms when the base looks like an infinitive`() {
        val forms = LearnedLemmaLinking.candidateInflections("kaufen")
        assertTrue("kauft" in forms)
        assertTrue("kaufte" in forms)
        assertTrue("kauftest" in forms)
    }
    
    @Test
    fun `candidateInflections omits verb forms for a base not ending in -en`() {
        val forms = LearnedLemmaLinking.candidateInflections("Baum")
        assertTrue(forms.none { it.endsWith("test") || it.endsWith("tet") })
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the German umlaut / ß folding used by the fuzzy suggestion matching (D-12).
 */
class UmlautTest {
    
    @Test
    fun `umlauts fold to their base vowel`() {
        assertEquals("grun", Umlaut.fold("grün"))
        assertEquals("schon", Umlaut.fold("schön"))
        assertEquals("uber", Umlaut.fold("über"))
    }
    
    @Test
    fun `sharp s folds to double s`() {
        assertEquals("grosse", Umlaut.fold("große"))
        assertEquals("strasse", Umlaut.fold("straße"))
    }
    
    @Test
    fun `uppercase umlauts fold to the lowercase base`() {
        assertEquals("aroa", Umlaut.fold("ÄröÄ"))
    }
    
    @Test
    fun `text without umlauts is unchanged`() {
        assertEquals("default", Umlaut.fold("default"))
        assertEquals("", Umlaut.fold(""))
    }
    
    @Test
    fun `a folded diacritic-less word equals its correct form`() {
        assertEquals(Umlaut.fold("grün"), Umlaut.fold("grun"))
        assertEquals(Umlaut.fold("straße"), Umlaut.fold("strasse"))
    }
    
    @Test
    fun `D-144 a diacritic-less prefix always includes itself first`() {
        assertEquals("default", Umlaut.unfoldCandidates("default").first())
    }
    
    @Test
    fun `D-144 text with no fold-eligible characters has exactly one candidate`() {
        // "wir" has no a/o/u and no ss run - nothing to unfold at all.
        assertEquals(listOf("wir"), Umlaut.unfoldCandidates("wir"))
    }
    
    @Test
    fun `D-144 a single umlaut-eligible letter offers both spellings`() {
        assertEquals(listOf("grun", "grün"), Umlaut.unfoldCandidates("grun"))
    }
    
    @Test
    fun `D-144 the exact reported case - tatsachl must reach tatsächlich's own prefix`() {
        val candidates = Umlaut.unfoldCandidates("tatsachl")
        
        assertTrue(candidates.contains("tatsächl"))
        assertTrue("tatsächlich".startsWith(candidates.first { it.contains('ä') }))
    }
    
    @Test
    fun `D-144 multiple fold-eligible positions are combined`() {
        val candidates = Umlaut.unfoldCandidates("konnuberlassen")
        
        assertTrue(candidates.contains("könnüberlassen"))
        assertTrue(candidates.contains("könnuberlassen"))
        assertTrue(candidates.contains("konnüberlassen"))
        assertTrue(candidates.contains("konnuberlassen"))
    }
    
    @Test
    fun `D-144 a double-s run may have originally been a sharp s`() {
        // "biss" has no a/o/u - isolates the ss/ß branch alone, avoiding the vowel-fold combinatorics.
        assertEquals(listOf("biss", "biß"), Umlaut.unfoldCandidates("biss"))
    }
    
    @Test
    fun `D-144 an isolated single s is never turned into a sharp s`() {
        // "bist" has a single, non-doubled s and no a/o/u - the lone s must never become ß.
        assertEquals(listOf("bist"), Umlaut.unfoldCandidates("bist"))
    }
    
    @Test
    fun `D-144 candidate generation is capped for a pathological all-vowel input`() {
        val candidates = Umlaut.unfoldCandidates("aaaaaaaaaaaaaaaaaaaa")
        
        assertTrue(candidates.size <= 32)
        assertTrue(candidates.contains("aaaaaaaaaaaaaaaaaaaa"))
    }
}

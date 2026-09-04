// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-436 data-driven [DiacriticFolding], generalising [Umlaut] beyond German - built
 * against a small synthetic Turkish-shaped table (single variant per base) and a French-shaped one (several
 * variants per base), the two real cases D-387's own follow-up discussion named.
 */
class DataDiacriticFoldingTest {
    
    private val turkish = DataDiacriticFolding(mapOf('g' to listOf('ğ'), 's' to listOf('ş'), 'i' to listOf('ı')))
    private val french = DataDiacriticFolding(mapOf('e' to listOf('é', 'è', 'ê', 'ë')))
    
    @Test
    fun `fold replaces a known variant with its own base letter`() {
        assertEquals("dag", turkish.fold("dağ"))
        assertEquals("sise", turkish.fold("şişe"))
    }
    
    @Test
    fun `fold is a no-op for text with no known variants`() {
        assertEquals("default", turkish.fold("default"))
        assertEquals("", turkish.fold(""))
    }
    
    @Test
    fun `fold folds any of several variants of the same base letter to that base`() {
        assertEquals("mere", french.fold("mère"))
        assertEquals("mere", french.fold("mêre"))
        assertEquals("mere", french.fold("mere"))
    }
    
    @Test
    fun `fold folds an upper-case variant to the lower-case base`() {
        assertEquals("dag", turkish.fold("daĞ"))
    }
    
    @Test
    fun `unfoldCandidates always includes the literal text itself first`() {
        assertEquals("dag", turkish.unfoldCandidates("dag").first())
    }
    
    @Test
    fun `unfoldCandidates offers the base spelling and every known variant at an eligible position`() {
        assertEquals(setOf("dag", "dağ"), turkish.unfoldCandidates("dag").toSet())
    }
    
    @Test
    fun `unfoldCandidates offers every variant of a base letter with several, not just one`() {
        // A single eligible position ("e") with four known variants - five candidates in total.
        val candidates = french.unfoldCandidates("le").toSet()
        assertEquals(setOf("le", "lé", "lè", "lê", "lë"), candidates)
    }
    
    @Test
    fun `unfoldCandidates combines multiple eligible positions`() {
        // "s" and "g" are each eligible (-> ş / ğ), "a" is not - four combinations in total.
        val candidates = turkish.unfoldCandidates("sag").toSet()
        assertEquals(setOf("sag", "şag", "sağ", "şağ"), candidates)
    }
    
    @Test
    fun `unfoldCandidates has exactly one entry when nothing is eligible`() {
        // None of "w"/"o"/"b" have a known variant in the Turkish table (unlike "i" -> "ı", easy to miss).
        assertEquals(listOf("wob"), turkish.unfoldCandidates("wob"))
    }
    
    @Test
    fun `unfoldCandidates is capped for a pathological all-eligible input`() {
        val candidates = turkish.unfoldCandidates("gggggggggggggggggggg")
        assertTrue(candidates.size <= 32)
        assertTrue(candidates.contains("gggggggggggggggggggg"))
    }
    
    @Test
    fun `foldVariants always returns exactly one entry, equal to fold`() {
        assertEquals(listOf(turkish.fold("dağ")), turkish.foldVariants("dağ"))
        assertEquals(listOf("default"), turkish.foldVariants("default"))
    }
    
    @Test
    fun `variantsOf returns every known variant of a base letter`() {
        assertEquals(setOf('é', 'è', 'ê', 'ë'), french.variantsOf('e'))
        assertEquals(setOf('ğ'), turkish.variantsOf('g'))
    }
    
    @Test
    fun `variantsOf is empty for a character with no known variants`() {
        assertEquals(emptySet<Char>(), turkish.variantsOf('x'))
        assertEquals(emptySet<Char>(), DataDiacriticFolding(emptyMap()).variantsOf('a'))
    }
    
    @Test
    fun `two instances built from equal tables are equal`() {
        assertEquals(DataDiacriticFolding(mapOf('g' to listOf('ğ'))), DataDiacriticFolding(mapOf('g' to listOf('ğ'))))
    }
}

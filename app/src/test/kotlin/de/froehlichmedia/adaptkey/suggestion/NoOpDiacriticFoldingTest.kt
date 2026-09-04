// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-435 no-op [DiacriticFolding] default - every language without its own real
 * implementation ([de.froehlichmedia.adaptkey.language.DiacriticFoldingRegistry]) gets this, not [Umlaut].
 */
class NoOpDiacriticFoldingTest {
    
    @Test
    fun `fold leaves text unchanged, umlauts included`() {
        assertEquals("grün", NoOpDiacriticFolding.fold("grün"))
        assertEquals("café", NoOpDiacriticFolding.fold("café"))
    }
    
    @Test
    fun `unfoldCandidates returns only the text itself`() {
        assertEquals(listOf("grun"), NoOpDiacriticFolding.unfoldCandidates("grun"))
    }
    
    @Test
    fun `foldVariants returns only the text itself`() {
        assertEquals(listOf("grüße"), NoOpDiacriticFolding.foldVariants("grüße"))
    }
    
    @Test
    fun `variantsOf is always empty`() {
        assertEquals(emptySet<Char>(), NoOpDiacriticFolding.variantsOf('a'))
        assertEquals(emptySet<Char>(), NoOpDiacriticFolding.variantsOf('o'))
    }
}

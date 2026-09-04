// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.suggestion.NoOpDiacriticFolding
import de.froehlichmedia.adaptkey.suggestion.Umlaut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-435 per-language [de.froehlichmedia.adaptkey.suggestion.DiacriticFolding] resolver.
 */
class DiacriticFoldingRegistryTest {
    
    @Test
    fun `German resolves to Umlaut`() {
        assertEquals(Umlaut, DiacriticFoldingRegistry.foldingFor(Language.GERMAN))
    }
    
    @Test
    fun `a language without a real implementation resolves to the no-op default`() {
        assertEquals(NoOpDiacriticFolding, DiacriticFoldingRegistry.foldingFor(Language.ENGLISH))
        assertEquals(NoOpDiacriticFolding, DiacriticFoldingRegistry.foldingFor(Language.FRENCH))
        assertEquals(NoOpDiacriticFolding, DiacriticFoldingRegistry.foldingFor(Language.GREEK))
    }
}

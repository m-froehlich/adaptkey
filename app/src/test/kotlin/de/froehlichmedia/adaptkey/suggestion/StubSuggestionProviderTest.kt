// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the placeholder suggestion provider.
 */
class StubSuggestionProviderTest {
    
    private val provider = StubSuggestionProvider()
    
    @Test
    fun `suggestionsFor returns prefix matches sorted by descending score`() {
        val suggestions = provider.suggestionsFor("de", null)
        val matchedWords = suggestions.map { it.word }
        
        assertTrue(matchedWords.contains("der"))
        assertTrue(matchedWords.contains("den"))
        assertTrue(matchedWords.all { it.startsWith("de") })
        for (i in 1 until suggestions.size) {
            assertTrue(suggestions[i - 1].score >= suggestions[i].score)
        }
    }
    
    @Test
    fun `isKnownWord is case-insensitive`() {
        assertTrue(provider.isKnownWord("der"))
        assertTrue(provider.isKnownWord("DER"))
        assertFalse(provider.isKnownWord("xyzzy"))
    }
    
    @Test
    fun `autocorrectFor returns null for a known word`() {
        assertNull(provider.autocorrectFor("der", null))
    }
    
    @Test
    fun `autocorrectFor returns null for a too-short token`() {
        assertNull(provider.autocorrectFor("d", null))
    }
    
    @Test
    fun `autocorrectFor returns null when nothing is within one edit`() {
        assertNull(provider.autocorrectFor("xyzzy", null))
    }
    
    @Test
    fun `autocorrectFor proposes the most frequent single-edit neighbour`() {
        // "dee" is one substitution away from der/den/des/dem; der is the most frequent.
        assertEquals("der", provider.autocorrectFor("dee", null))
    }
    
    @Test
    fun `paragraph 44 shouldOverrideKnownWord defaults to false without frequency data`() {
        assertFalse(provider.shouldOverrideKnownWord("due", "die"))
    }
}

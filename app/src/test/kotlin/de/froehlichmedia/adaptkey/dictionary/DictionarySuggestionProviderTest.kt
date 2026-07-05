// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-1 dictionary suggestion provider (n-gram ranking, A-01, A-04).
 */
class DictionarySuggestionProviderTest {
    
    private val store = InMemoryDictionaryStore()
    private val provider = DictionarySuggestionProvider(store)
    
    @Test
    fun `suggestionsFor ranks prefix matches by frequency`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 100L))
        
        val words = provider.suggestionsFor("h", null).map { it.word }
        assertEquals(listOf("Haus", "Hund"), words)
    }
    
    @Test
    fun `bigram context can outrank a more frequent unigram`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 100L))
        store.putBigram("der", "Hund", 50L)
        
        assertEquals("Haus", provider.suggestionsFor("h", null).first().word)
        assertEquals("Hund", provider.suggestionsFor("h", "der").first().word)
    }
    
    @Test
    fun `A-04 blacklisted words are excluded from suggestions`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 100L))
        store.blacklist("Haus", BlacklistCategory.USER)
        
        val words = provider.suggestionsFor("h", null).map { it.word }
        assertEquals(listOf("Hund"), words)
    }
    
    @Test
    fun `A-04 a blacklisted word is not reported as known`() {
        store.putWord(WordEntry("Haus", 100L))
        store.blacklist("Haus", BlacklistCategory.USER)
        
        assertFalse(provider.isKnownWord("Haus"))
    }
    
    @Test
    fun `isKnownWord is true for a known non-blacklisted word`() {
        store.putWord(WordEntry("Haus", 100L))
        assertTrue(provider.isKnownWord("haus"))
    }
    
    @Test
    fun `A-01 a known word is never autocorrected`() {
        store.putWord(WordEntry("Haus", 100L))
        assertNull(provider.autocorrectFor("haus", null))
    }
    
    @Test
    fun `autocorrectFor returns null for a too-short token`() {
        store.putWord(WordEntry("der", 100L))
        assertNull(provider.autocorrectFor("d", null))
    }
    
    @Test
    fun `autocorrectFor proposes the most frequent single-edit neighbour`() {
        store.putWord(WordEntry("der", 100L))
        store.putWord(WordEntry("den", 50L))
        
        assertEquals("der", provider.autocorrectFor("dee", null))
    }
    
    @Test
    fun `A-04 a blacklisted neighbour is not proposed as an autocorrection`() {
        store.putWord(WordEntry("der", 100L))
        store.putWord(WordEntry("den", 50L))
        store.blacklist("der", BlacklistCategory.USER)
        
        assertEquals("den", provider.autocorrectFor("dee", null))
    }
    
    @Test
    fun `D-12 a close neighbour is offered even when the typed word is itself valid`() {
        store.putWord(WordEntry("Mut", 50L))
        store.putWord(WordEntry("mit", 100L))
        
        val words = provider.suggestionsFor("mut", null).map { it.word }
        assertTrue(words.contains("mit"), "the intended \"mit\" must still be offered for a valid \"mut\"")
    }
    
    @Test
    fun `D-12 an umlaut variant is offered for a diacritic-less prefix`() {
        store.putWord(WordEntry("grün", 80L))
        
        val words = provider.suggestionsFor("grun", null).map { it.word }
        assertTrue(words.contains("grün"), "\"grun\" must surface \"grün\"")
    }
    
    @Test
    fun `D-12 a single mistype surfaces the intended word`() {
        store.putWord(WordEntry("Default", 30L))
        
        val words = provider.suggestionsFor("defaukt", null).map { it.word }
        assertTrue(words.contains("Default"), "\"defaukt\" must surface \"Default\"")
    }
    
    @Test
    fun `D-12 autocorrect is umlaut-aware`() {
        store.putWord(WordEntry("grün", 80L))
        
        assertEquals("grün", provider.autocorrectFor("grun", null))
    }
    
    @Test
    fun `fuzzy neighbours are not offered for a one or two letter token`() {
        store.putWord(WordEntry("mit", 100L))
        
        // "mu" must not fuzzy-match "mit"; only prefix completion applies at this length.
        assertFalse(provider.suggestionsFor("mu", null).map { it.word }.contains("mit"))
    }
}

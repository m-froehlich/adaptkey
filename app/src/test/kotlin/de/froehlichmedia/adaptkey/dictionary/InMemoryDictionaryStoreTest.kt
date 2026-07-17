// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the in-memory dictionary store (and, by proxy, the store contract).
 */
class InMemoryDictionaryStoreTest {
    
    private val store = InMemoryDictionaryStore()
    
    @Test
    fun `putWord stores frequency and part-of-speech case-insensitively`() {
        store.putWord(WordEntry("Haus", 5L, setOf(PartOfSpeech.NOUN)))
        
        assertTrue(store.isKnownWord("haus"))
        assertEquals(5L, store.frequencyOf("HAUS"))
        assertEquals(setOf(PartOfSpeech.NOUN), store.partsOfSpeech("haus"))
    }
    
    @Test
    fun `learn creates an unknown word with frequency one`() {
        store.learn("neu", null)
        assertEquals(1L, store.frequencyOf("neu"))
    }
    
    @Test
    fun `learn increments an existing word and its bigram`() {
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", "der")
        store.learn("hund", "der")
        
        assertEquals(5L, store.frequencyOf("hund"))
        assertEquals(2L, store.bigramFrequency("der", "hund"))
    }
    
    @Test
    fun `unlearn reverses a learn that created a brand new word`() {
        store.learn("neu", "ganz")
        store.unlearn("neu", "ganz")
        
        assertFalse(store.isKnownWord("neu"))
        assertEquals(0L, store.bigramFrequency("ganz", "neu"))
    }
    
    @Test
    fun `unlearn reverses a learn that reinforced an already-known word`() {
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", "der")
        store.unlearn("hund", "der")
        
        assertTrue(store.isKnownWord("hund"))
        assertEquals(3L, store.frequencyOf("hund"))
        assertEquals(0L, store.bigramFrequency("der", "hund"))
    }
    
    @Test
    fun `unlearn on an unknown word is a harmless no-op`() {
        store.unlearn("nie-gelernt", "kontext")
        
        assertFalse(store.isKnownWord("nie-gelernt"))
    }
    
    @Test
    fun `unigramsByPrefix returns case-insensitive prefix matches sorted by descending frequency`() {
        store.putWord(WordEntry("Haus", 5L))
        store.putWord(WordEntry("Hund", 9L))
        store.putWord(WordEntry("Auto", 100L))
        
        val words = store.unigramsByPrefix("h", 10).map { it.word }
        assertEquals(listOf("Hund", "Haus"), words)
    }
    
    @Test
    fun `unigramsByPrefix honours the limit`() {
        store.putWord(WordEntry("ha", 1L))
        store.putWord(WordEntry("hb", 2L))
        store.putWord(WordEntry("hc", 3L))
        
        assertEquals(2, store.unigramsByPrefix("h", 2).size)
    }
    
    @Test
    fun `bigramFrequency defaults to zero`() {
        assertEquals(0L, store.bigramFrequency("der", "hund"))
    }
    
    @Test
    fun `nextWords returns canonical-case successors ordered by count - D-43`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Hut", 10L))
        store.putBigram("der", "hund", 40L)
        store.putBigram("der", "hut", 5L)
        // A different context that must not leak into "der".
        store.putBigram("dere", "hut", 99L)
        
        assertEquals(listOf("Hund", "Hut"), store.nextWords("der", 10))
        assertEquals(listOf("Hund"), store.nextWords("der", 1))
        assertEquals(emptyList<String>(), store.nextWords("der", 0))
        assertEquals(emptyList<String>(), store.nextWords("", 10))
    }
    
    @Test
    fun `blacklist add check category and remove`() {
        store.blacklist("Daß", BlacklistCategory.OLD_SPELLING)
        
        assertTrue(store.isBlacklisted("daß"))
        assertEquals(BlacklistCategory.OLD_SPELLING, store.blacklistCategory("daß"))
        
        store.unblacklist("daß")
        assertFalse(store.isBlacklisted("daß"))
    }
    
    @Test
    fun `blacklistedWords lists stored keys sorted and empty by default`() {
        assertTrue(store.blacklistedWords().isEmpty())
        
        store.blacklist("Zebra", BlacklistCategory.USER)
        store.blacklist("Apfel", BlacklistCategory.PROFANITY)
        
        assertEquals(listOf("apfel", "zebra"), store.blacklistedWords())
    }
    
    @Test
    fun `allKnownWords returns canonical forms`() {
        store.putWord(WordEntry("Berlin", 1L, setOf(PartOfSpeech.PROPER_NOUN)))
        assertEquals(listOf("Berlin"), store.allKnownWords())
    }
    
    @Test
    fun `isEmpty reflects whether unigrams exist`() {
        assertTrue(store.isEmpty())
        store.putWord(WordEntry("a", 1L))
        assertFalse(store.isEmpty())
    }
}

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
    fun `learn with two-word context records a trigram alongside the bigram - D-246`() {
        store.putWord(WordEntry("Nachbar", 3L))
        store.learn("Nachbar", "der", "ist")
        store.learn("Nachbar", "der", "ist")
        
        assertEquals(2L, store.trigramFrequency("ist", "der", "Nachbar"))
        assertEquals(2L, store.bigramFrequency("der", "Nachbar"))
    }
    
    @Test
    fun `learn without previousPreviousWord records no trigram - D-246`() {
        store.learn("Nachbar", "der")
        
        assertEquals(0L, store.trigramFrequency("ist", "der", "Nachbar"))
        assertEquals(1L, store.bigramFrequency("der", "Nachbar"))
    }
    
    @Test
    fun `unlearn reverses a trigram exactly once - D-246`() {
        store.learn("Nachbar", "der", "ist")
        store.learn("Nachbar", "der", "ist")
        store.unlearn("Nachbar", "der", "ist")
        
        assertEquals(1L, store.trigramFrequency("ist", "der", "Nachbar"))
        assertEquals(1L, store.bigramFrequency("der", "Nachbar"))
    }
    
    @Test
    fun `unlearn removes a trigram entry entirely once its count reaches zero - D-246`() {
        store.learn("Nachbar", "der", "ist")
        store.unlearn("Nachbar", "der", "ist")
        
        assertEquals(0L, store.trigramFrequency("ist", "der", "Nachbar"))
    }
    
    @Test
    fun `nextWordsTrigram returns canonical-case successors ordered by count, scoped to the exact pair - D-246`() {
        store.putWord(WordEntry("Nachbar", 3L))
        store.putWord(WordEntry("Nachbarin", 2L))
        store.learn("Nachbar", "der", "ist")
        store.learn("Nachbar", "der", "ist")
        store.learn("Nachbarin", "der", "ist")
        // A different two-word context that must not leak in.
        store.learn("Nachbarin", "der", "hier")
        
        assertEquals(listOf("Nachbar", "Nachbarin"), store.nextWordsTrigram("ist", "der", 10))
        assertEquals(listOf("Nachbar"), store.nextWordsTrigram("ist", "der", 1))
        assertEquals(emptyList<String>(), store.nextWordsTrigram("ist", "der", 0))
        assertEquals(emptyList<String>(), store.nextWordsTrigram("", "der", 10))
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
    
    @Test
    fun `isBundledWord is true only for words added via putWord - D-177`() {
        store.putWord(WordEntry("hund", 3L))
        store.learn("aks", null)
        
        assertTrue(store.isBundledWord("hund"))
        assertFalse(store.isBundledWord("aks"))
    }
    
    @Test
    fun `isBundledWord stays true after a bundled word is also learned - D-177`() {
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", null)
        
        assertTrue(store.isBundledWord("hund"))
    }
    
    @Test
    fun `forget removes a learned word outright regardless of accumulated frequency - D-177`() {
        store.learn("aks", null)
        store.learn("aks", null)
        store.learn("aks", null)
        
        store.forget("aks")
        
        assertFalse(store.isKnownWord("aks"))
    }
    
    @Test
    fun `forget on a bundled word leaves its bundled frequency untouched - D-177`() {
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", null)
        
        store.forget("hund")
        
        assertTrue(store.isKnownWord("hund"))
        assertEquals(3L, store.frequencyOf("hund"))
    }
    
    @Test
    fun `learnedWords returns only learned entries sorted by descending frequency - D-177`() {
        store.putWord(WordEntry("hund", 100L))
        store.learn("aks", null)
        store.learn("neu", null)
        store.learn("neu", null)
        
        val words = store.learnedWords().map { it.word }
        assertEquals(listOf("neu", "aks"), words)
    }
    
    @Test
    fun `learnedWords is empty by default`() {
        assertTrue(store.learnedWords().isEmpty())
    }
    
    @Test
    fun `pending blacklist mark check and clear round-trip - D-177`() {
        assertEquals(null, store.pendingBlacklistedSince("aks"))
        
        store.markPendingBlacklist("aks", 12345L)
        assertEquals(12345L, store.pendingBlacklistedSince("aks"))
        
        store.clearPendingBlacklist("aks")
        assertEquals(null, store.pendingBlacklistedSince("aks"))
    }
    
    @Test
    fun `a word that is both bundled and learned reports the summed frequency - D-177`() {
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", null)
        store.learn("hund", null)
        
        assertEquals(5L, store.frequencyOf("hund"))
        assertEquals(listOf("hund"), store.unigramsByPrefix("h", 10).map { it.word })
        assertEquals(5L, store.unigramsByPrefix("h", 10).first().frequency)
    }
}

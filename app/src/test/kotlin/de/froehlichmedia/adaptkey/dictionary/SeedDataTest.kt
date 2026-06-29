package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Sanity tests for the built-in dictionary seed.
 */
class SeedDataTest {
    
    private val store = InMemoryDictionaryStore()
    
    @Test
    fun `seed populates words bigrams and part-of-speech`() {
        SeedData.seed(store)
        
        assertFalse(store.isEmpty())
        assertTrue(store.isKnownWord("Haus"))
        assertTrue(store.partsOfSpeech("Haus").contains(PartOfSpeech.NOUN))
        assertEquals(50L, store.bigramFrequency("der", "Hund"))
    }
    
    @Test
    fun `ambiguous seed words carry multiple part-of-speech tags`() {
        SeedData.seed(store)
        
        // "gegenüber" is both a noun and a preposition (capitalisation hierarchy rule 5).
        assertEquals(setOf(PartOfSpeech.NOUN, PartOfSpeech.PREPOSITION), store.partsOfSpeech("gegenüber"))
    }
    
    @Test
    fun `proper nouns are tagged as such`() {
        SeedData.seed(store)
        assertTrue(store.partsOfSpeech("Berlin").contains(PartOfSpeech.PROPER_NOUN))
    }
}

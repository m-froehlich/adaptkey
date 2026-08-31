// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.dictionary.InMemoryDictionaryStore
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Unit tests for the D-404 with-LLM family application. */
class Tier3FamilyApplierTest {
    
    private val store = InMemoryDictionaryStore()
    
    @Test
    fun `learns every family member, each with the model's own category`() {
        val result = Tier3FamilyResult(PartOfSpeech.NOUN, "Hund", listOf("Hund", "Hunde", "Hundes"))
        Tier3FamilyApplier.apply(store, result)
        
        assertEquals(setOf(PartOfSpeech.NOUN), store.partsOfSpeech("hund"))
        assertEquals(setOf(PartOfSpeech.NOUN), store.partsOfSpeech("hunde"))
        assertEquals(setOf(PartOfSpeech.NOUN), store.partsOfSpeech("hundes"))
    }
    
    @Test
    fun `links every non-lemma family member to the lemma, leaving the lemma itself unlinked`() {
        val result = Tier3FamilyResult(PartOfSpeech.NOUN, "Hund", listOf("Hund", "Hunde", "Hundes"))
        Tier3FamilyApplier.apply(store, result)
        
        assertEquals("hund", store.entryOf("hunde")?.lemma)
        assertEquals("hund", store.entryOf("hundes")?.lemma)
        assertNull(store.entryOf("hund")?.lemma)
    }
    
    @Test
    fun `an empty result is a no-op`() {
        Tier3FamilyApplier.apply(store, Tier3FamilyResult.EMPTY)
        assertFalse(store.isKnownWord("hund"))
    }
    
    @Test
    fun `never overrides an already-manually-set lemma link on a family member`() {
        store.learn("Hunde", null)
        store.setLearnedLemma("hunde", "manuellekorrektur")
        
        val result = Tier3FamilyResult(PartOfSpeech.NOUN, "Hund", listOf("Hund", "Hunde"))
        Tier3FamilyApplier.apply(store, result)
        
        assertEquals("manuellekorrektur", store.entryOf("hunde")?.lemma)
    }
    
    @Test
    fun `never overrides an already-known learned category with the model's own category hint`() {
        store.learn("Hunde", null, categoryHint = PartOfSpeech.VERB)
        
        val result = Tier3FamilyResult(PartOfSpeech.NOUN, "Hund", listOf("Hund", "Hunde"))
        Tier3FamilyApplier.apply(store, result)
        
        assertEquals(setOf(PartOfSpeech.VERB), store.partsOfSpeech("hunde"))
    }
    
    @Test
    fun `reinforces an already-learned family member instead of duplicating it`() {
        store.learn("Hunde", null)
        val before = store.frequencyOf("hunde")
        
        val result = Tier3FamilyResult(PartOfSpeech.NOUN, "Hund", listOf("Hund", "Hunde"))
        Tier3FamilyApplier.apply(store, result)
        
        assertEquals(before + 1L, store.frequencyOf("hunde"))
    }
}

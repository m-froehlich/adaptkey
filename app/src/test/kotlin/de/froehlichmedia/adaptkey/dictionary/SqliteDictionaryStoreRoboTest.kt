// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, real SQLite, no emulator) for the SQLite store's bounded autocorrect candidate
 * query — the fix for the per-keystroke whole-lexicon scan that made typing laggy with the real
 * dictionaries — and the end-to-end single-edit autocorrect over the SQLite store.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SqliteDictionaryStoreRoboTest {
    
    private fun store(name: String): SqliteDictionaryStore {
        return SqliteDictionaryStore(RuntimeEnvironment.getApplication(), name)
    }
    
    @Test
    fun correctionCandidatesAreBoundedByFirstCharAndLength() {
        val store = store("cand.db")
        listOf("haus", "hase", "maus", "h", "hausboot").forEach { store.putWord(WordEntry(it, 10L, emptySet())) }
        
        val candidates = store.correctionCandidates("haus")
        
        assertTrue(candidates.contains("haus"))
        assertTrue(candidates.contains("hase")) // same first char, length within one
        assertFalse(candidates.contains("maus")) // different first char
        assertFalse(candidates.contains("hausboot")) // length differs by more than one
        assertFalse(candidates.contains("h")) // length differs by more than one
        store.close()
    }
    
    @Test
    fun autocorrectSuggestsTheSingleEditNeighbour() {
        val store = store("auto.db")
        store.putWord(WordEntry("haus", 100L, emptySet()))
        val provider = DictionarySuggestionProvider(store)
        
        assertEquals("haus", provider.autocorrectFor("hous", null))
        assertEquals(null, provider.autocorrectFor("haus", null)) // A-01: a known word is not corrected
        store.close()
    }
    
    @Test
    fun nextWordsReturnsCanonicalSuccessorsByCount() {
        val store = store("next.db")
        store.putWord(WordEntry("Hund", 10L, emptySet()))
        store.putWord(WordEntry("Hut", 10L, emptySet()))
        store.putBigram("der", "Hund", 40L)
        store.putBigram("der", "Hut", 5L)
        
        assertEquals(listOf("Hund", "Hut"), store.nextWords("der", 10))
        assertEquals(listOf("Hund"), store.nextWords("der", 1))
        assertTrue(store.nextWords("unbekannt", 10).isEmpty())
        store.close()
    }
}

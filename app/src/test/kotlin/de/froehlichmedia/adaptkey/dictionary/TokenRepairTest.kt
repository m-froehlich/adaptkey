// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure retroactive token repair: word split (A-05) and word merge (A-06), each
 * requiring a valid linguistic result rather than mere spatial proximity.
 */
class TokenRepairTest {
    
    private lateinit var store: InMemoryDictionaryStore
    private lateinit var repair: TokenRepair
    
    @BeforeEach
    fun setUp() {
        store = InMemoryDictionaryStore()
        listOf("und", "das", "aber", "bald", "ist", "ich").forEach { word ->
            store.putWord(WordEntry(word, frequency = 10L))
        }
        repair = TokenRepair(store)
    }
    
    @Test
    fun `a flagged space-ambiguous character is split into two words`() {
        // "undxdas" with the 'x' at index 3 flagged -> drop it and split.
        val result = repair.trySplit("undxdas", setOf(3))
        assertEquals(SplitResult("und", "das"), result)
    }
    
    @Test
    fun `a fully missed space is split by inserting a space`() {
        val result = repair.trySplit("aberdas", emptySet())
        assertEquals(SplitResult("aber", "das"), result)
    }
    
    @Test
    fun `a token with no valid split is left alone`() {
        assertNull(repair.trySplit("abcdef", setOf(3)))
    }
    
    @Test
    fun `a known word is never split`() {
        assertNull(repair.trySplit("aber", setOf(2)))
    }
    
    @Test
    fun `a split is rejected when a half is blacklisted`() {
        store.blacklist("das", BlacklistCategory.USER)
        assertNull(repair.trySplit("aberdas", emptySet()))
    }
    
    @Test
    fun `a spurious space is merged back into a known word`() {
        val merged = repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "ald")
        assertEquals("bald", merged)
    }
    
    @Test
    fun `a merge yielding no known word is rejected`() {
        assertNull(repair.tryMerge(previousWord = "ich", inferredChar = 'q', token = "xyz"))
    }
    
    @Test
    fun `a merge is rejected when the following token is already a word`() {
        assertNull(repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "und"))
    }
    
    @Test
    fun `a high-probability bigram justifies a merge even without a known word`() {
        // "bold" is not a unigram, but follows "ich" frequently enough.
        store.putBigram("ich", "bold", TokenRepair.MIN_BIGRAM)
        val merged = repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "old")
        assertEquals("bold", merged)
    }
    
    @Test
    fun `a blacklisted merge candidate is rejected`() {
        store.blacklist("bald", BlacklistCategory.USER)
        assertNull(repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "ald"))
    }
}

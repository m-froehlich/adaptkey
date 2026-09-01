// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-389 learned-word expiry sweep.
 */
class LearnedWordExpirySweepTest {
    
    private val dayMillis = 24 * 60 * 60 * 1000L
    
    @Test
    fun `a word touched well within the window survives`() {
        var now = 0L
        val store = InMemoryDictionaryStore(clock = { now })
        store.learn("apfel", null)
        now += 10 * dayMillis
        
        val expired = LearnedWordExpirySweep.sweep(store, now, LearnedWordExpiryWindow.EARLY)
        
        assertTrue(expired.isEmpty())
        assertTrue(store.isKnownWord("apfel"))
    }
    
    @Test
    fun `a word untouched past the window is un-learned`() {
        var now = 0L
        val store = InMemoryDictionaryStore(clock = { now })
        store.learn("apfel", null)
        now += LearnedWordExpiryWindow.EARLY.days * dayMillis + 1
        
        val expired = LearnedWordExpirySweep.sweep(store, now, LearnedWordExpiryWindow.EARLY)
        
        assertEquals(listOf("apfel"), expired)
        assertFalse(store.isKnownWord("apfel"))
    }
    
    @Test
    fun `re-learning a word resets its own clock, extending the window`() {
        var now = 0L
        val store = InMemoryDictionaryStore(clock = { now })
        store.learn("apfel", null)
        now += LearnedWordExpiryWindow.EARLY.days * dayMillis - 1
        // Touched again just before it would have expired - the clock restarts from here.
        store.learn("apfel", null)
        now += LearnedWordExpiryWindow.EARLY.days * dayMillis - 1
        
        val expired = LearnedWordExpirySweep.sweep(store, now, LearnedWordExpiryWindow.EARLY)
        
        assertTrue(expired.isEmpty())
        assertTrue(store.isKnownWord("apfel"))
    }
    
    @Test
    fun `only words past the currently configured window expire, not every word ever learned`() {
        var now = 0L
        val store = InMemoryDictionaryStore(clock = { now })
        store.learn("alt", null)
        now += LearnedWordExpiryWindow.EARLY.days * dayMillis + 1
        store.learn("neu", null)
        
        val expired = LearnedWordExpirySweep.sweep(store, now, LearnedWordExpiryWindow.EARLY)
        
        assertEquals(listOf("alt"), expired)
        assertFalse(store.isKnownWord("alt"))
        assertTrue(store.isKnownWord("neu"))
    }
    
    @Test
    fun `the same age survives a longer window that would have expired it under a shorter one`() {
        var now = 0L
        val store = InMemoryDictionaryStore(clock = { now })
        store.learn("apfel", null)
        now += LearnedWordExpiryWindow.EARLY.days * dayMillis + 1
        
        val expired = LearnedWordExpirySweep.sweep(store, now, LearnedWordExpiryWindow.LATE)
        
        assertTrue(expired.isEmpty())
        assertTrue(store.isKnownWord("apfel"))
    }
    
    @Test
    fun `a bundled dictionary word is never touched by the sweep`() {
        val store = InMemoryDictionaryStore(clock = { 0L })
        store.putWord(WordEntry("haus", 5_000L))
        
        val expired = LearnedWordExpirySweep.sweep(store, 999L * 24 * 60 * 60 * 1000, LearnedWordExpiryWindow.EARLY)
        
        assertTrue(expired.isEmpty())
        assertTrue(store.isKnownWord("haus"))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the A-03 character n-gram normalization and extraction. The normalization contract
 * is shared with the profile builder, so these also pin down that contract.
 */
class CharNgramsTest {
    
    @Test
    fun `normalize lowercases, drops non-letters and wraps in single spaces`() {
        assertEquals(" hallo welt ", CharNgrams.normalize("Hallo, Welt!"))
    }
    
    @Test
    fun `normalize collapses whitespace and punctuation runs`() {
        assertEquals(" a b ", CharNgrams.normalize("  a -- b  "))
    }
    
    @Test
    fun `normalize of letter-free or empty text is a single space`() {
        assertEquals(" ", CharNgrams.normalize(""))
        assertEquals(" ", CharNgrams.normalize("123 !!"))
    }
    
    @Test
    fun `normalize keeps accented and non-ascii letters`() {
        assertEquals(" über café ", CharNgrams.normalize("Über  Café."))
    }
    
    @Test
    fun `counts produces bigrams and trigrams with boundary spaces`() {
        val counts = CharNgrams.counts("ab")
        
        // normalized = " ab "
        assertEquals(1, counts[" a"])
        assertEquals(1, counts["ab"])
        assertEquals(1, counts["b "])
        assertEquals(1, counts[" ab"])
        assertEquals(1, counts["ab "])
    }
    
    @Test
    fun `rankedProfile orders by frequency then ngram, capped at size`() {
        // "aaa" -> normalized " aaa " -> "aa" appears twice, dominating.
        val ranked = CharNgrams.rankedProfile("aaa", 3)
        
        assertEquals(3, ranked.size)
        assertEquals("aa", ranked.first())
    }
    
    @Test
    fun `rankedProfile is deterministic for tied ngrams`() {
        val a = CharNgrams.rankedProfile("der die das", 200)
        val b = CharNgrams.rankedProfile("der die das", 200)
        
        assertEquals(a, b)
        assertTrue(a.isNotEmpty())
    }
}

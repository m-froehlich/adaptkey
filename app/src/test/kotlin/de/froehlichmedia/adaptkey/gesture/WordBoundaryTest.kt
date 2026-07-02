// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure G-02 word-deletion span computation.
 */
class WordBoundaryTest {
    
    @Test
    fun `empty text deletes nothing`() {
        assertEquals(0, WordBoundary.wordDeleteLength(""))
    }
    
    @Test
    fun `a single word is deleted whole`() {
        assertEquals(5, WordBoundary.wordDeleteLength("hallo"))
    }
    
    @Test
    fun `only the last word is deleted, leaving the preceding space`() {
        assertEquals(3, WordBoundary.wordDeleteLength("foo bar"))
    }
    
    @Test
    fun `a trailing space is consumed together with the word before it`() {
        assertEquals(4, WordBoundary.wordDeleteLength("foo bar "))
    }
    
    @Test
    fun `multiple trailing spaces are all consumed with the word`() {
        assertEquals(5, WordBoundary.wordDeleteLength("foo bar  "))
    }
    
    @Test
    fun `pure whitespace is deleted entirely`() {
        assertEquals(3, WordBoundary.wordDeleteLength("   "))
    }
    
    @Test
    fun `a hyphenated compound counts as a single word`() {
        assertEquals(10, WordBoundary.wordDeleteLength("E-Mail-Tor"))
    }
}

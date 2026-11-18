// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-62 mid-word reclaim computation.
 */
class WordExtentTest {
    
    @Test
    fun `both sides empty when the caret touches no letters`() {
        val reclaim = WordExtent.reclaim("foo ", " bar")
        assertEquals("", reclaim.before)
        assertEquals("", reclaim.after)
    }
    
    @Test
    fun `caret inside a word reclaims both halves`() {
        val reclaim = WordExtent.reclaim("Beis", "piel")
        assertEquals("Beis", reclaim.before)
        assertEquals("piel", reclaim.after)
    }
    
    @Test
    fun `caret at the end of a word reclaims only the left half`() {
        val reclaim = WordExtent.reclaim("Hunde", ". Text")
        assertEquals("Hunde", reclaim.before)
        assertEquals("", reclaim.after)
    }
    
    @Test
    fun `caret at the start of a word reclaims only the right half`() {
        val reclaim = WordExtent.reclaim("Text. ", "Hunde")
        assertEquals("", reclaim.before)
        assertEquals("Hunde", reclaim.after)
    }
    
    @Test
    fun `only the run directly touching the caret is reclaimed, not the whole sentence`() {
        val reclaim = WordExtent.reclaim("der Hu", "nd rennt")
        assertEquals("Hu", reclaim.before)
        assertEquals("nd", reclaim.after)
    }
    
    @Test
    fun `both empty texts reclaim nothing`() {
        val reclaim = WordExtent.reclaim("", "")
        assertEquals("", reclaim.before)
        assertEquals("", reclaim.after)
    }
    
    @Test
    fun `D-544 the apostrophe is a word character`() {
        assertTrue(WordExtent.isWordChar('\''))
        assertTrue(WordExtent.isWordChar('a'))
        assertTrue(WordExtent.isWordChar('Ä'))
        // Real delimiters stay delimiters.
        assertFalse(WordExtent.isWordChar(' '))
        assertFalse(WordExtent.isWordChar('.'))
        assertFalse(WordExtent.isWordChar('-'))
        assertFalse(WordExtent.isWordChar('"'))
    }
    
    @Test
    fun `D-544 a caret between the apostrophe and the s reclaims the whole word`() {
        // "ist's" committed, caret parked between "ist'" and "s": the reclaim run must not stop at the
        // apostrophe as if it were a delimiter - the whole contracted form is one word.
        val reclaim = WordExtent.reclaim("ist'", "s")
        assertEquals("ist'", reclaim.before)
        assertEquals("s", reclaim.after)
    }
    
    @Test
    fun `D-544 a trailing apostrophe fragment glues onto the preceding word`() {
        // The "sieht's" shape typed right-to-left of the caret: the "'s" fragment belongs to "sieht",
        // not to whatever delimiter precedes it.
        val reclaim = WordExtent.reclaim("sieht", "'s")
        assertEquals("sieht", reclaim.before)
        assertEquals("'s", reclaim.after)
    }
    
    @Test
    fun `D-544 a leading apostrophe opens the run it belongs to`() {
        // The elision shape ("'s ist"): the leading apostrophe is part of the following run, while the
        // real delimiter before it still ends the reclaim.
        val reclaim = WordExtent.reclaim("Text. ", "'s ist")
        assertEquals("", reclaim.before)
        assertEquals("'s", reclaim.after)
    }
}

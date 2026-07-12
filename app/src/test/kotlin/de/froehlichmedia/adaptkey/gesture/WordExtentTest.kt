// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertEquals
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
}

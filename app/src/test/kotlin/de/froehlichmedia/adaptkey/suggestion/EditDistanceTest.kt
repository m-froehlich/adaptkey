// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the shared single-edit distance helper.
 */
class EditDistanceTest {
    
    @Test
    fun `equal strings are within one edit`() {
        assertTrue(EditDistance.atMostOne("der", "der"))
    }
    
    @Test
    fun `a single substitution is within one edit`() {
        assertTrue(EditDistance.atMostOne("der", "dor"))
    }
    
    @Test
    fun `a single insertion is within one edit`() {
        assertTrue(EditDistance.atMostOne("de", "der"))
    }
    
    @Test
    fun `a single deletion is within one edit`() {
        assertTrue(EditDistance.atMostOne("derb", "der"))
    }
    
    @Test
    fun `two edits are not within one edit`() {
        assertFalse(EditDistance.atMostOne("dxr", "den"))
    }
    
    @Test
    fun `a length difference of two is not within one edit`() {
        assertFalse(EditDistance.atMostOne("de", "derb"))
    }
    
    // A cost model where a substitution of the fixed pair (z, t) is cheap (1) and any other is 2; indel 2.
    private val cheapZt: (Char, Char) -> Int = { x, y ->
        when {
            x == y -> 0
            (x == 'z' && y == 't') || (x == 't' && y == 'z') -> 1
            else -> 2
        }
    }
    
    @Test
    fun `weighted distance is zero for equal strings`() {
        assertEquals(0, EditDistance.weightedDistance("komplett", "komplett", 2, cheapZt))
    }
    
    @Test
    fun `weighted distance sums the cheap substitutions`() {
        // Two cheap z->t substitutions.
        assertEquals(2, EditDistance.weightedDistance("komplezz", "komplett", 2, cheapZt))
    }
    
    @Test
    fun `weighted distance charges the full cost for unrelated substitutions`() {
        // One expensive substitution.
        assertEquals(2, EditDistance.weightedDistance("kat", "kot", 2, cheapZt))
        // Two expensive substitutions.
        assertEquals(4, EditDistance.weightedDistance("kase", "kobe", 2, cheapZt))
    }
    
    @Test
    fun `weighted distance counts an insertion or deletion at the indel cost`() {
        assertEquals(2, EditDistance.weightedDistance("kat", "kart", 2, cheapZt))
        assertEquals(2, EditDistance.weightedDistance("kart", "kat", 2, cheapZt))
    }
}

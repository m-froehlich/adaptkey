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
        assertEquals(0, EditDistance.weightedDistance("komplett", "komplett", 2, substitutionCost = cheapZt))
    }
    
    @Test
    fun `weighted distance sums the cheap substitutions`() {
        // Two cheap z->t substitutions.
        assertEquals(2, EditDistance.weightedDistance("komplezz", "komplett", 2, substitutionCost = cheapZt))
    }
    
    @Test
    fun `weighted distance charges the full cost for unrelated substitutions`() {
        // One expensive substitution.
        assertEquals(2, EditDistance.weightedDistance("kat", "kot", 2, substitutionCost = cheapZt))
        // Two expensive substitutions.
        assertEquals(4, EditDistance.weightedDistance("kase", "kobe", 2, substitutionCost = cheapZt))
    }
    
    @Test
    fun `weighted distance counts an insertion or deletion at the indel cost`() {
        assertEquals(2, EditDistance.weightedDistance("kat", "kart", 2, substitutionCost = cheapZt))
        assertEquals(2, EditDistance.weightedDistance("kart", "kat", 2, substitutionCost = cheapZt))
    }
    
    // §125 / D-194: banded computation (maxCost) must match the exact unbounded distance whenever the
    // true distance is within the band, and must still report "more than maxCost" (never a false "close
    // enough") whenever the band was too narrow to reach it - the two properties every current call site
    // (isCloseMatch / bestCorrection / wideFuzzyNeighbours) actually relies on.
    
    @Test
    fun `banded distance matches the unbounded distance when within maxCost`() {
        assertEquals(2, EditDistance.weightedDistance("komplezz", "komplett", 2, maxCost = 2, substitutionCost = cheapZt))
        assertEquals(2, EditDistance.weightedDistance("kat", "kart", 2, maxCost = 2, substitutionCost = cheapZt))
        assertEquals(0, EditDistance.weightedDistance("komplett", "komplett", 2, maxCost = 0, substitutionCost = cheapZt))
    }
    
    @Test
    fun `banded distance still reports the exact distance right at the maxCost boundary`() {
        // Two expensive substitutions, true distance 4 - maxCost set to exactly 4.
        assertEquals(4, EditDistance.weightedDistance("kase", "kobe", 2, maxCost = 4, substitutionCost = cheapZt))
    }
    
    @Test
    fun `banded distance reports more than maxCost when the true distance exceeds it`() {
        // True distance is 4 (two expensive substitutions); a maxCost of 1 makes the band too narrow to
        // ever reach it - the result must still come back greater than maxCost, never a wrong exact value
        // that could be mistaken for "close enough".
        val result = EditDistance.weightedDistance("kase", "kobe", 2, maxCost = 1, substitutionCost = cheapZt)
        assertTrue(result > 1)
    }
    
    @Test
    fun `banded distance handles a length difference wider than the band`() {
        // "de" -> "derb" is a true distance of 4 (two insertions at indelCost 2 each) - a maxCost of 2
        // gives a band of 1, narrower than the length difference of 2, so it must come back above maxCost.
        val result = EditDistance.weightedDistance("de", "derb", 2, maxCost = 2, substitutionCost = cheapZt)
        assertTrue(result > 2)
        // The same pair with a wide-enough maxCost still resolves to the exact distance.
        assertEquals(4, EditDistance.weightedDistance("de", "derb", 2, maxCost = 4, substitutionCost = cheapZt))
    }
    
    @Test
    fun `banded distance agrees with the unbounded distance across longer garbled tokens`() {
        // A long, deliberately non-word token against a same-length real-ish candidate - the exact case
        // shape (long composing tokens with no close dictionary match) that motivated the banding.
        val a = "qxjklmnopqrstuvw"
        val b = "qzjklmnopqrstuvx"
        val exact = EditDistance.weightedDistance(a, b, 2, substitutionCost = cheapZt)
        assertEquals(exact, EditDistance.weightedDistance(a, b, 2, maxCost = exact, substitutionCost = cheapZt))
        assertTrue(EditDistance.weightedDistance(a, b, 2, maxCost = exact - 1, substitutionCost = cheapZt) > exact - 1)
    }
}

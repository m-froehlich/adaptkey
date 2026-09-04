// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityQwertz], the QWERTZ key-adjacency map used by proximity-aware
 * correction (D-28).
 */
class KeyboardProximityQwertzTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityQwertz.adjacent('t', 'z'))
        assertTrue(KeyboardProximityQwertz.adjacent('z', 't'))
        assertTrue(KeyboardProximityQwertz.adjacent('a', 's'))
        assertTrue(KeyboardProximityQwertz.adjacent('k', 'l'))
    }
    
    @Test
    fun `vertically and diagonally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityQwertz.adjacent('e', 'd'))
        assertTrue(KeyboardProximityQwertz.adjacent('a', 'q'))
        assertTrue(KeyboardProximityQwertz.adjacent('a', 'w'))
    }
    
    @Test
    fun `D-372 g and b are already adjacent - g,h are the diagonal pair straddling b's stagger position`() {
        // D-372's own named example: the column-index grid places "asdfghjkl" directly above "yxcvbnm" with
        // no stagger offset, so `g` (row 2, col 4) sits in the same column as `b` (row 3, col 4) - already
        // within the row/col <= 1 window this class has used since its introduction, confirmed adjacent here
        // directly against the real compiled adjacency map, not just re-derived by hand. `h` (row 2, col 5)
        // is the genuinely diagonal neighbour of `b` in this same grid and is already adjacent too.
        assertTrue(KeyboardProximityQwertz.adjacent('g', 'b'))
        assertTrue(KeyboardProximityQwertz.adjacent('h', 'b'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityQwertz.adjacent('q', 'p'))
        assertFalse(KeyboardProximityQwertz.adjacent('a', 'l'))
        assertFalse(KeyboardProximityQwertz.adjacent('q', 'm'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityQwertz.adjacent('t', 't'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        // The number row sits above qwertzuiop, so `8` neighbours the `i`/`o` gap ("W8rt" is "Wort").
        assertTrue(KeyboardProximityQwertz.adjacent('8', 'o'))
        assertTrue(KeyboardProximityQwertz.adjacent('8', 'i'))
        assertTrue(KeyboardProximityQwertz.adjacent('1', 'q'))
        assertTrue(KeyboardProximityQwertz.adjacent('0', 'p'))
        // ...and their horizontal digit neighbours.
        assertTrue(KeyboardProximityQwertz.adjacent('8', '9'))
        // ...but not far-away keys.
        assertFalse(KeyboardProximityQwertz.adjacent('1', 'p'))
        assertFalse(KeyboardProximityQwertz.adjacent('8', 'a'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        for (a in 'a'..'z') {
            for (b in 'a'..'z') {
                assertTrue(KeyboardProximityQwertz.adjacent(a, b) == KeyboardProximityQwertz.adjacent(b, a), "asymmetry at $a/$b")
            }
        }
    }
}

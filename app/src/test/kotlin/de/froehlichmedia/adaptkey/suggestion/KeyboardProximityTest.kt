// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the QWERTZ key-adjacency map used by proximity-aware correction (D-28).
 */
class KeyboardProximityTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximity.adjacent('t', 'z'))
        assertTrue(KeyboardProximity.adjacent('z', 't'))
        assertTrue(KeyboardProximity.adjacent('a', 's'))
        assertTrue(KeyboardProximity.adjacent('k', 'l'))
    }
    
    @Test
    fun `vertically and diagonally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximity.adjacent('e', 'd'))
        assertTrue(KeyboardProximity.adjacent('a', 'q'))
        assertTrue(KeyboardProximity.adjacent('a', 'w'))
    }
    
    @Test
    fun `D-372 g and b are already adjacent - g,h are the diagonal pair straddling b's stagger position`() {
        // D-372's own named example: the column-index grid places "asdfghjkl" directly above "yxcvbnm" with
        // no stagger offset, so `g` (row 2, col 4) sits in the same column as `b` (row 3, col 4) - already
        // within the row/col <= 1 window this class has used since its introduction, confirmed adjacent here
        // directly against the real compiled adjacency map, not just re-derived by hand. `h` (row 2, col 5)
        // is the genuinely diagonal neighbour of `b` in this same grid and is already adjacent too.
        assertTrue(KeyboardProximity.adjacent('g', 'b'))
        assertTrue(KeyboardProximity.adjacent('h', 'b'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximity.adjacent('q', 'p'))
        assertFalse(KeyboardProximity.adjacent('a', 'l'))
        assertFalse(KeyboardProximity.adjacent('q', 'm'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximity.adjacent('t', 't'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        // The number row sits above qwertzuiop, so `8` neighbours the `i`/`o` gap ("W8rt" is "Wort").
        assertTrue(KeyboardProximity.adjacent('8', 'o'))
        assertTrue(KeyboardProximity.adjacent('8', 'i'))
        assertTrue(KeyboardProximity.adjacent('1', 'q'))
        assertTrue(KeyboardProximity.adjacent('0', 'p'))
        // ...and their horizontal digit neighbours.
        assertTrue(KeyboardProximity.adjacent('8', '9'))
        // ...but not far-away keys.
        assertFalse(KeyboardProximity.adjacent('1', 'p'))
        assertFalse(KeyboardProximity.adjacent('8', 'a'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        for (a in 'a'..'z') {
            for (b in 'a'..'z') {
                assertTrue(KeyboardProximity.adjacent(a, b) == KeyboardProximity.adjacent(b, a), "asymmetry at $a/$b")
            }
        }
    }
}

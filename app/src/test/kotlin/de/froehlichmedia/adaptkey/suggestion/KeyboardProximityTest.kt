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

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityQwerty] - English's own row geometry, differing from
 * [KeyboardProximityQwertz] only in where `y`/`z` sit (top row vs. bottom row).
 */
class KeyboardProximityQwertyTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityQwerty.adjacent('a', 's'))
        assertTrue(KeyboardProximityQwerty.adjacent('t', 'y'))
    }
    
    @Test
    fun `z sits on the bottom row here, unlike QWERTZ - adjacent to a-s-x, not t`() {
        assertTrue(KeyboardProximityQwerty.adjacent('z', 'a'))
        assertTrue(KeyboardProximityQwerty.adjacent('z', 's'))
        assertTrue(KeyboardProximityQwerty.adjacent('z', 'x'))
        assertFalse(KeyboardProximityQwerty.adjacent('z', 't'))
    }
    
    @Test
    fun `y sits on the top row here, unlike QWERTZ - adjacent to t-u, not the bottom row`() {
        assertTrue(KeyboardProximityQwerty.adjacent('y', 't'))
        assertTrue(KeyboardProximityQwerty.adjacent('y', 'u'))
        assertFalse(KeyboardProximityQwerty.adjacent('y', 'x'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximityQwerty.adjacent('8', 'i'))
        assertTrue(KeyboardProximityQwerty.adjacent('1', 'q'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityQwerty.adjacent('q', 'p'))
        assertFalse(KeyboardProximityQwerty.adjacent('a', 'l'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityQwerty.adjacent('t', 't'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        for (a in 'a'..'z') {
            for (b in 'a'..'z') {
                assertTrue(KeyboardProximityQwerty.adjacent(a, b) == KeyboardProximityQwerty.adjacent(b, a), "asymmetry at $a/$b")
            }
        }
    }
}

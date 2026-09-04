// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityAzerty] - French's own row geometry, matching
 * [de.froehlichmedia.adaptkey.keyboard.AzertyLayout]'s `TOP_ROW`/`MIDDLE_ROW`/`THIRD_ROW` exactly
 * (`q`/`w`/`a`/`z`/`m` move between rows entirely, not merely within one).
 */
class KeyboardProximityAzertyTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityAzerty.adjacent('a', 'z'))
        assertTrue(KeyboardProximityAzerty.adjacent('q', 's'))
    }
    
    @Test
    fun `a and q sit at the start of the top and middle rows here, vertically adjacent`() {
        assertTrue(KeyboardProximityAzerty.adjacent('a', 'q'))
        assertTrue(KeyboardProximityAzerty.adjacent('q', 'w'))
    }
    
    @Test
    fun `m sits at the end of the middle row here, not the bottom row`() {
        assertTrue(KeyboardProximityAzerty.adjacent('m', 'l'))
        assertFalse(KeyboardProximityAzerty.adjacent('m', 'n'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximityAzerty.adjacent('1', 'a'))
        assertTrue(KeyboardProximityAzerty.adjacent('8', 'o'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityAzerty.adjacent('a', 'p'))
        assertFalse(KeyboardProximityAzerty.adjacent('q', 'm'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityAzerty.adjacent('a', 'a'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        for (a in 'a'..'z') {
            for (b in 'a'..'z') {
                assertTrue(KeyboardProximityAzerty.adjacent(a, b) == KeyboardProximityAzerty.adjacent(b, a), "asymmetry at $a/$b")
            }
        }
    }
}

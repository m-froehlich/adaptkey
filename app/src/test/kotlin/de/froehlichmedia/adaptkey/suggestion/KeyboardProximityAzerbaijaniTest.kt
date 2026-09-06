// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityAzerbaijani], matching
 * [de.froehlichmedia.adaptkey.keyboard.AzerbaijaniLayout]'s own `TOP_ROW`/`MIDDLE_ROW`/`THIRD_ROW` exactly.
 */
class KeyboardProximityAzerbaijaniTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityAzerbaijani.adjacent('p', 'ü'))
        assertTrue(KeyboardProximityAzerbaijani.adjacent('g', 'ı'))
    }
    
    @Test
    fun `vertically neighbouring keys across rows are adjacent`() {
        assertTrue(KeyboardProximityAzerbaijani.adjacent('p', 'g'))
        assertTrue(KeyboardProximityAzerbaijani.adjacent('g', 'ç'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximityAzerbaijani.adjacent('1', 'p'))
        assertTrue(KeyboardProximityAzerbaijani.adjacent('8', 'y'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityAzerbaijani.adjacent('p', 'j'))
        assertFalse(KeyboardProximityAzerbaijani.adjacent('g', 'ğ'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityAzerbaijani.adjacent('a', 'a'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        val letters = "püşudbmyohxjgısarnəlicğçfvtkeqzö"
        for (a in letters) {
            for (b in letters) {
                assertTrue(
                    KeyboardProximityAzerbaijani.adjacent(a, b) == KeyboardProximityAzerbaijani.adjacent(b, a),
                    "asymmetry at $a/$b"
                )
            }
        }
    }
}

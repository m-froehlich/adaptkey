// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityGreek], matching
 * [de.froehlichmedia.adaptkey.keyboard.GreekLayout]'s own `ROW_TOP`/`ROW_MIDDLE`/`ROW_BOTTOM` exactly.
 */
class KeyboardProximityGreekTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityGreek.adjacent('ς', 'ε'))
        assertTrue(KeyboardProximityGreek.adjacent('α', 'σ'))
    }
    
    @Test
    fun `vertically neighbouring keys across rows are adjacent`() {
        assertTrue(KeyboardProximityGreek.adjacent('ς', 'α'))
        assertTrue(KeyboardProximityGreek.adjacent('α', 'ζ'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximityGreek.adjacent('1', 'ς'))
        assertTrue(KeyboardProximityGreek.adjacent('8', 'ο'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityGreek.adjacent('ς', 'π'))
        assertFalse(KeyboardProximityGreek.adjacent('α', 'λ'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityGreek.adjacent('α', 'α'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        val letters = "ςερτυθιοπασδφγηξκλζχψωβνμ"
        for (a in letters) {
            for (b in letters) {
                assertTrue(KeyboardProximityGreek.adjacent(a, b) == KeyboardProximityGreek.adjacent(b, a), "asymmetry at $a/$b")
            }
        }
    }
}

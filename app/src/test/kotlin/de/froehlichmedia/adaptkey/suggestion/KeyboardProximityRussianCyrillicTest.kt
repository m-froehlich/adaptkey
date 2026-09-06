// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityRussianCyrillic], matching
 * [de.froehlichmedia.adaptkey.keyboard.JcukenLayout]'s own Russian rows exactly.
 */
class KeyboardProximityRussianCyrillicTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityRussianCyrillic.adjacent('й', 'ц'))
        assertTrue(KeyboardProximityRussianCyrillic.adjacent('ф', 'ы'))
    }
    
    @Test
    fun `vertically neighbouring keys across rows are adjacent`() {
        assertTrue(KeyboardProximityRussianCyrillic.adjacent('й', 'ф'))
        assertTrue(KeyboardProximityRussianCyrillic.adjacent('ф', 'я'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximityRussianCyrillic.adjacent('1', 'й'))
        assertTrue(KeyboardProximityRussianCyrillic.adjacent('8', 'ш'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityRussianCyrillic.adjacent('й', 'ъ'))
        assertFalse(KeyboardProximityRussianCyrillic.adjacent('ф', 'э'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityRussianCyrillic.adjacent('а', 'а'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        val letters = "йцукенгшщзхъфывапролджэячсмитьбю"
        for (a in letters) {
            for (b in letters) {
                assertTrue(
                    KeyboardProximityRussianCyrillic.adjacent(a, b) == KeyboardProximityRussianCyrillic.adjacent(b, a),
                    "asymmetry at $a/$b"
                )
            }
        }
    }
}

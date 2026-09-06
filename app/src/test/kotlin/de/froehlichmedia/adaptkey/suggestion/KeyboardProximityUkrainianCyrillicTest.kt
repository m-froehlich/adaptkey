// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximityUkrainianCyrillic], matching
 * [de.froehlichmedia.adaptkey.keyboard.JcukenLayout]'s own Ukrainian rows exactly.
 */
class KeyboardProximityUkrainianCyrillicTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximityUkrainianCyrillic.adjacent('й', 'ц'))
        assertTrue(KeyboardProximityUkrainianCyrillic.adjacent('ф', 'і'))
    }
    
    @Test
    fun `vertically neighbouring keys across rows are adjacent, including the leading ge`() {
        assertTrue(KeyboardProximityUkrainianCyrillic.adjacent('й', 'ф'))
        assertTrue(KeyboardProximityUkrainianCyrillic.adjacent('ф', 'ґ'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximityUkrainianCyrillic.adjacent('1', 'й'))
        assertTrue(KeyboardProximityUkrainianCyrillic.adjacent('8', 'ш'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximityUkrainianCyrillic.adjacent('й', 'ї'))
        assertFalse(KeyboardProximityUkrainianCyrillic.adjacent('ф', 'є'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximityUkrainianCyrillic.adjacent('а', 'а'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        val letters = "йцукенгшщзхїфівапролджєґячсмитьбю"
        for (a in letters) {
            for (b in letters) {
                assertTrue(
                    KeyboardProximityUkrainianCyrillic.adjacent(a, b) == KeyboardProximityUkrainianCyrillic.adjacent(b, a),
                    "asymmetry at $a/$b"
                )
            }
        }
    }
}

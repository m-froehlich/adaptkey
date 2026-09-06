// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KeyboardProximitySerbianCyrillic], matching
 * [de.froehlichmedia.adaptkey.keyboard.SerbianLayout]'s own `ROW_TOP`/`ROW_MIDDLE`/`ROW_BOTTOM` exactly.
 */
class KeyboardProximitySerbianCyrillicTest {
    
    @Test
    fun `horizontally neighbouring keys are adjacent`() {
        assertTrue(KeyboardProximitySerbianCyrillic.adjacent('љ', 'њ'))
        assertTrue(KeyboardProximitySerbianCyrillic.adjacent('а', 'с'))
    }
    
    @Test
    fun `vertically neighbouring keys across rows are adjacent`() {
        assertTrue(KeyboardProximitySerbianCyrillic.adjacent('љ', 'а'))
        assertTrue(KeyboardProximitySerbianCyrillic.adjacent('а', 'џ'))
    }
    
    @Test
    fun `digits neighbour the letters below them - D-41`() {
        assertTrue(KeyboardProximitySerbianCyrillic.adjacent('1', 'љ'))
        assertTrue(KeyboardProximitySerbianCyrillic.adjacent('8', 'о'))
    }
    
    @Test
    fun `distant keys are not adjacent`() {
        assertFalse(KeyboardProximitySerbianCyrillic.adjacent('љ', 'ж'))
        assertFalse(KeyboardProximitySerbianCyrillic.adjacent('а', 'ћ'))
    }
    
    @Test
    fun `a key is not adjacent to itself`() {
        assertFalse(KeyboardProximitySerbianCyrillic.adjacent('а', 'а'))
    }
    
    @Test
    fun `adjacency is symmetric`() {
        val letters = "љњертзуиопшђжасдфгхјклчћџцвбнм"
        for (a in letters) {
            for (b in letters) {
                assertTrue(
                    KeyboardProximitySerbianCyrillic.adjacent(a, b) == KeyboardProximitySerbianCyrillic.adjacent(b, a),
                    "asymmetry at $a/$b"
                )
            }
        }
    }
}

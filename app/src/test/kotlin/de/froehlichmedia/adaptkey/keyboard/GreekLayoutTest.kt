// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the Greek key map (full Greek input): row structure, tonos accents as long-press
 * secondaries, and the shared control / bottom row.
 */
class GreekLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `default layout has five rows`() {
        assertEquals(5, GreekLayout.rows().size)
    }
    
    @Test
    fun `hiding the number row drops the top row`() {
        val rows = GreekLayout.rows(showNumberRow = false)
        
        assertEquals(4, rows.size)
        assertEquals('ς', rows.first().first().char)
    }
    
    @Test
    fun `the three letter rows carry the standard Greek alphabet`() {
        val rows = GreekLayout.rows(showNumberRow = false)
        
        assertEquals("ςερτυθιοπ", rows[0].joinToString("") { it.char.toString() })
        assertEquals("ασδφγηξκλ", rows[1].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertEquals("ζχψωβνμ", rows[2].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
    }
    
    @Test
    fun `stressed vowels carry their tonos form as a long-press secondary`() {
        val rows = GreekLayout.rows()
        
        assertEquals("ά", KeyboardLayout.longPressSymbol(rows[2].byChar('α')))
        assertEquals("έ", KeyboardLayout.longPressSymbol(rows[1].byChar('ε')))
        assertEquals("ώ", KeyboardLayout.longPressSymbol(rows[3].byChar('ω')))
    }
    
    @Test
    fun `consonants have no accent secondary`() {
        val rows = GreekLayout.rows()
        
        assertNull(KeyboardLayout.longPressSymbol(rows[2].byChar('σ')))
        assertTrue(GreekLayout.ACCENTS['β'] == null)
    }
    
    @Test
    fun `an accented vowel is a letter, so its long-press has an action`() {
        val alpha = GreekLayout.rows()[2].byChar('α')
        
        assertTrue(KeyboardLayout.hasLongPressAction(alpha))
        assertTrue(GreekLayout.ACCENTS['α']!!.all { it.isLetter() })
    }
    
    @Test
    fun `the bottom row mirrors the Latin layout controls`() {
        val bottomRow = GreekLayout.rows().last()
        
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals('.', bottomRow.byChar('.').char)
    }
    
    @Test
    fun `custom proportions flow into the Greek layout`() {
        val bottomRow = GreekLayout.rows(KeyProportions(spaceWeight = 2f)).last()
        
        assertEquals(2f, bottomRow[2].weight, 1e-4f)
    }
    
    @Test
    fun `the number row carries digits with shifted-symbol hints`() {
        val numberRow = GreekLayout.rows().first()
        
        assertEquals(10, numberRow.size)
        assertEquals('1', numberRow.first().char)
        assertEquals("!", numberRow.byChar('1').hint)
    }
}

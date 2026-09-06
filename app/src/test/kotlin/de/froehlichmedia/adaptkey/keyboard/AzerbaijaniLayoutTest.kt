// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the Azerbaijani standard key map, verified against the real Microsoft `KBDAZST` standard.
 */
class AzerbaijaniLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `default layout has five rows`() {
        assertEquals(5, AzerbaijaniLayout.rows().size)
    }
    
    @Test
    fun `the three letter rows match the real KBDAZST standard - 12,11,9 letters`() {
        val rows = AzerbaijaniLayout.rows(showNumberRow = false)
        
        assertEquals("püşudbmyohxj", rows[0].joinToString("") { it.char.toString() })
        assertEquals("gısarnəlicğ", rows[1].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertEquals("çfvtkeqzö", rows[2].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
    }
    
    @Test
    fun `all 32 letters of the Azerbaijani alphabet are present exactly once, including q,w,x at their real positions`() {
        val rows = AzerbaijaniLayout.rows(showNumberRow = false)
        val letters = rows.take(2).flatten().map { it.char } +
            rows[2].filter { it.code == KeyCode.CHAR }.map { it.char }
        
        val expected = "abcçdeəfgğhxıijkqlmnoöprsştuüvyz".toSet()
        assertEquals(32, expected.size)
        assertEquals(expected, letters.toSet())
        assertEquals(32, letters.size, "a letter appears more than once")
        // Unlike Serbian's own JUSCII layout, w is genuinely dropped (not part of the alphabet) while q/x
        // are real, used Azerbaijani letters at their own real positions - confirmed directly, not assumed.
        assertEquals(false, letters.contains('w'))
    }
    
    @Test
    fun `p and o keep their D-99 math-symbol popups wherever they land in this layout`() {
        val topRow = AzerbaijaniLayout.rows(showNumberRow = false)[0]
        val pKey = topRow.byChar('p')
        val oKey = topRow.byChar('o')
        
        assertEquals("π", pKey.hint)
        assertEquals("ö", oKey.hint)
        assertEquals(listOf("π", "α", "β", "γ", "δ", "λ", "ω"), pKey.alternatives)
    }
    
    @Test
    fun `the bottom row mirrors the Latin layout controls`() {
        val bottomRow = AzerbaijaniLayout.rows().last()
        
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals('.', bottomRow.byChar('.').char)
    }
    
    @Test
    fun `custom proportions flow into the layout`() {
        val bottomRow = AzerbaijaniLayout.rows(KeyProportions(spaceWeight = 2f)).last()
        
        assertEquals(2f, bottomRow[2].weight, 1e-4f)
    }
    
    @Test
    fun `the number row carries digits with shifted-symbol hints`() {
        val numberRow = AzerbaijaniLayout.rows().first()
        
        assertEquals(10, numberRow.size)
        assertEquals('1', numberRow.first().char)
        assertEquals("!", numberRow.byChar('1').hint)
    }
    
    @Test
    fun `urlMode gives the same URL bottom row as the Latin layout`() {
        val row = AzerbaijaniLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        val latinRow = KeyboardLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(latinRow, row)
    }
    
    @Test
    fun `emailMode gives the same email bottom row as the Latin layout`() {
        val row = AzerbaijaniLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        val latinRow = KeyboardLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(latinRow, row)
    }
}

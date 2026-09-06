// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the Serbian Cyrillic key map: row structure (verified against the real Microsoft `KBDYCC`
 * standard, D-450-followup), and the shared control/bottom row.
 */
class SerbianLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `default layout has five rows`() {
        assertEquals(5, SerbianLayout.rows().size)
    }
    
    @Test
    fun `hiding the number row drops the top row`() {
        val rows = SerbianLayout.rows(showNumberRow = false)
        
        assertEquals(4, rows.size)
        assertEquals('љ', rows.first().first().char)
    }
    
    @Test
    fun `the three letter rows carry the real Serbian Cyrillic standard - top 13, middle 11, bottom 6`() {
        val rows = SerbianLayout.rows(showNumberRow = false)
        
        assertEquals("љњертзуиопшђж", rows[0].joinToString("") { it.char.toString() })
        assertEquals("асдфгхјклчћ", rows[1].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertEquals("џцвбнм", rows[2].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
    }
    
    @Test
    fun `all 30 letters of the Serbian alphabet are present, exactly once each`() {
        val rows = SerbianLayout.rows(showNumberRow = false)
        val letters = rows.take(2).flatten().map { it.char } +
            rows[2].filter { it.code == KeyCode.CHAR }.map { it.char }
        
        val expected = "абвгдђежзијклљмнњопрстћуфхцчџш".toSet()
        assertEquals(30, expected.size)
        assertEquals(expected, letters.toSet())
        assertEquals(30, letters.size, "a letter appears more than once")
    }
    
    @Test
    fun `the historical non-Serbian dze filler from the desktop ISO key is not included`() {
        val rows = SerbianLayout.rows(showNumberRow = false)
        val letters = rows.flatten().map { it.char }.toSet()
        
        assertTrue('ѕ' !in letters, "ѕ (Cyrillic dze, not a Serbian letter) must not appear")
    }
    
    @Test
    fun `letters have no long-press hint or alternatives - unlike Latin diacritics, Serbian letters are standalone`() {
        // The three alphabet rows only (index 1-3 with the number row shown) - excludes the trailing control
        // row, whose comma/period keys legitimately carry punctuation alternatives like the Latin layout's.
        val rows = SerbianLayout.rows()
        
        for (key in rows.subList(1, 4).flatten().filter { it.code == KeyCode.CHAR }) {
            assertEquals(null, key.hint, "unexpected hint on ${key.char}")
            assertTrue(key.alternatives.isEmpty(), "unexpected alternatives on ${key.char}")
        }
    }
    
    @Test
    fun `the bottom row mirrors the Latin layout controls`() {
        val bottomRow = SerbianLayout.rows().last()
        
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals('.', bottomRow.byChar('.').char)
    }
    
    @Test
    fun `custom proportions flow into the Serbian layout`() {
        val bottomRow = SerbianLayout.rows(KeyProportions(spaceWeight = 2f)).last()
        
        assertEquals(2f, bottomRow[2].weight, 1e-4f)
    }
    
    @Test
    fun `the number row carries digits with shifted-symbol hints`() {
        val numberRow = SerbianLayout.rows().first()
        
        assertEquals(10, numberRow.size)
        assertEquals('1', numberRow.first().char)
        assertEquals("!", numberRow.byChar('1').hint)
    }
    
    @Test
    fun `the number row also gets the superscript second alternative`() {
        val numberRow = SerbianLayout.rows().first()
        
        assertEquals(listOf("!", "¹"), numberRow.byChar('1').alternatives)
        assertEquals(listOf("=", "⁰"), numberRow.byChar('0').alternatives)
    }
    
    @Test
    fun `D-143 urlMode gives the Serbian layout the same URL bottom row as the Latin layout`() {
        val serbianRow = SerbianLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        val latinRow = KeyboardLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(latinRow, serbianRow)
    }
    
    @Test
    fun `D-143 without urlMode the Serbian bottom row is unaffected`() {
        assertEquals(SerbianLayout.rows().last(), SerbianLayout.rows(locale = Locale.US).last())
    }
    
    @Test
    fun `D-158 emailMode gives the Serbian layout the same email bottom row as the Latin layout`() {
        val serbianRow = SerbianLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        val latinRow = KeyboardLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(latinRow, serbianRow)
    }
}

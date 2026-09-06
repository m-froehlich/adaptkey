// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the shared Russian/Ukrainian ЙЦУКЕН key map, verified against the real Microsoft `KBDRU`/
 * `KBDUR` standards.
 */
class JcukenLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `default layout has five rows in both variants`() {
        assertEquals(5, JcukenLayout.rows().size)
        assertEquals(5, JcukenLayout.rows(ukrainian = true).size)
    }
    
    @Test
    fun `Russian rows match the real KBDRU standard - 12,11,9 letters`() {
        val rows = JcukenLayout.rows(showNumberRow = false)
        
        assertEquals("йцукенгшщзхъ", rows[0].joinToString("") { it.char.toString() })
        assertEquals("фывапролджэ", rows[1].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertEquals("ячсмитьбю", rows[2].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
    }
    
    @Test
    fun `Ukrainian rows match the real KBDUR standard - 12,11,10 letters, three substitutions plus a leading ge`() {
        val rows = JcukenLayout.rows(showNumberRow = false, ukrainian = true)
        
        assertEquals("йцукенгшщзхї", rows[0].joinToString("") { it.char.toString() })
        assertEquals("фівапролджє", rows[1].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertEquals("ґячсмитьбю", rows[2].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
    }
    
    @Test
    fun `Russian e offers yo as a long-press secondary, Ukrainian does not`() {
        val ruTop = JcukenLayout.rows(showNumberRow = false)[0]
        val ukTop = JcukenLayout.rows(showNumberRow = false, ukrainian = true)[0]
        
        assertEquals("ё", KeyboardLayout.longPressSymbol(ruTop.byChar('е')))
        assertNull(KeyboardLayout.longPressSymbol(ukTop.byChar('е')))
    }
    
    @Test
    fun `no other letter carries a long-press secondary in either variant`() {
        val ru = JcukenLayout.rows()
        val uk = JcukenLayout.rows(ukrainian = true)
        
        for (key in ru.subList(1, 4).flatten().filter { it.code == KeyCode.CHAR && it.char != 'е' }) {
            assertNull(key.hint, "unexpected hint on Russian ${key.char}")
        }
        for (key in uk.subList(1, 4).flatten().filter { it.code == KeyCode.CHAR }) {
            assertNull(key.hint, "unexpected hint on Ukrainian ${key.char}")
        }
    }
    
    @Test
    fun `the bottom row mirrors the Latin layout controls`() {
        val bottomRow = JcukenLayout.rows().last()
        
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals('.', bottomRow.byChar('.').char)
    }
    
    @Test
    fun `custom proportions flow into both variants`() {
        assertEquals(2f, JcukenLayout.rows(KeyProportions(spaceWeight = 2f)).last()[2].weight, 1e-4f)
        assertEquals(2f, JcukenLayout.rows(KeyProportions(spaceWeight = 2f), ukrainian = true).last()[2].weight, 1e-4f)
    }
    
    @Test
    fun `the number row carries digits with shifted-symbol hints`() {
        val numberRow = JcukenLayout.rows().first()
        
        assertEquals(10, numberRow.size)
        assertEquals('1', numberRow.first().char)
        assertEquals("!", numberRow.byChar('1').hint)
    }
    
    @Test
    fun `urlMode gives the same URL bottom row as the Latin layout, in both variants`() {
        val ruRow = JcukenLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        val ukRow = JcukenLayout.rows(urlMode = true, locale = Locale.GERMANY, ukrainian = true).last()
        val latinRow = KeyboardLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(latinRow, ruRow)
        assertEquals(latinRow, ukRow)
    }
    
    @Test
    fun `emailMode gives the same email bottom row as the Latin layout`() {
        val row = JcukenLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        val latinRow = KeyboardLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(latinRow, row)
    }
    
    @Test
    fun `all 33 Ukrainian letters are present exactly once, and all 32 core Russian layout letters plus yo`() {
        fun letterChars(rows: List<List<Key>>): List<Char?> =
            rows.take(2).flatten().map { it.char } + rows[2].filter { it.code == KeyCode.CHAR }.map { it.char }
        
        val ru = letterChars(JcukenLayout.rows(showNumberRow = false)).toMutableList()
        ru.add('ё') // Russian's own long-press secondary, not a primary key.
        val ruExpected = "абвгдежзийклмнопрстуфхцчшщъыьэюя".toSet() + 'ё'
        assertEquals(ruExpected, ru.toSet())
        assertEquals(33, ruExpected.size)
        
        val uk = letterChars(JcukenLayout.rows(showNumberRow = false, ukrainian = true))
        val ukExpected = "абвгґдежзийклмнопрстуфхцчшщьюяіїє".toSet()
        assertEquals(ukExpected, uk.toSet())
        assertEquals(33, ukExpected.size)
        assertEquals(33, uk.size, "a Ukrainian letter appears more than once")
    }
}

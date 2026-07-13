// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the D-92 / D-100 `?123` layer: page 1 (calculator) and page 2 (leftover catch-all).
 */
class SymbolLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `page 1 is a calculator with five rows`() {
        val rows = SymbolLayout.rows(1)
        
        assertEquals(5, rows.size)
    }
    
    @Test
    fun `D-100 page 1 row one carries everyday symbols and backspace, no page-toggle key`() {
        val row = SymbolLayout.rows(1)[0]
        
        assertEquals("(°√π~&|".toList(), row.dropLast(1).map { it.char })
        assertEquals(KeyCode.DELETE, row.last().code)
        assertTrue(row.none { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `D-101 the bracket key absorbs the whole bracket family as its popup`() {
        val row = SymbolLayout.rows(1)[0]
        
        assertEquals(
            listOf("(", ")", "{", "}", "[", "]", "<", ">"),
            row.byChar('(').alternatives
        )
        assertNull(row.firstOrNull { it.char == ')' })
    }
    
    @Test
    fun `D-100 rows 2 to 4 end with space, currency and the optional abc key`() {
        val rows = SymbolLayout.rows(1, locale = Locale.GERMANY)
        
        assertEquals(listOf('7', '8', '9', '÷'), rows[1].dropLast(1).map { it.char })
        assertEquals(KeyCode.SPACE, rows[1].last().code)
        
        assertEquals(listOf('4', '5', '6', '×'), rows[2].dropLast(1).map { it.char })
        assertEquals('€', rows[2].last().char)
        
        assertEquals(listOf('1', '2', '3', '−'), rows[3].dropLast(1).map { it.char })
        assertEquals(KeyCode.LETTERS, rows[3].last().code)
    }
    
    @Test
    fun `D-100 row 5 is 0, decimal separator, equals and plus, ending with enter`() {
        val row = SymbolLayout.rows(1, locale = Locale.GERMANY)[4]
        
        assertEquals('0', row[0].char)
        assertEquals(',', row[1].char)
        assertEquals('=', row[2].char)
        assertEquals('+', row[3].char)
        assertEquals(KeyCode.ENTER, row[4].code)
    }
    
    @Test
    fun `page 1's own 2 and 3 keys carry squared and cubed hints`() {
        val digitRow = SymbolLayout.rows(1)[3]
        
        assertEquals("²", digitRow.byChar('2').hint)
        assertEquals("³", digitRow.byChar('3').hint)
    }
    
    @Test
    fun `the operator keys carry their alt-popups`() {
        val rows = SymbolLayout.rows(1)
        
        assertEquals(listOf("*", "×", "·"), rows[2].byChar('×').alternatives)
        assertEquals(listOf("/", "÷", ":"), rows[1].byChar('÷').alternatives)
        assertEquals(listOf("=", "→", "≈", "≙"), rows[4].byChar('=').alternatives)
    }
    
    @Test
    fun `D-98 and D-100 the currency popup order favours leftward growth`() {
        val row = SymbolLayout.rows(1, locale = Locale.GERMANY)[2]
        
        assertEquals(listOf("¥", "£", "$", "€"), row.byChar('€').alternatives)
    }
    
    @Test
    fun `D-100 the calculator column cells share the same weight`() {
        val rows = SymbolLayout.rows(1, locale = Locale.GERMANY)
        
        val space = rows[1].last().weight
        val currency = rows[2].last().weight
        val abc = rows[3].last().weight
        val enter = rows[4].last().weight
        
        assertEquals(space, currency, 1e-4f)
        assertEquals(space, abc, 1e-4f)
        assertEquals(space, enter, 1e-4f)
    }
    
    @Test
    fun `page 1's currency and decimal separator follow the system locale, not a hardcoded default`() {
        val row = SymbolLayout.rows(1, locale = Locale.US)[4]
        
        assertEquals('$', SymbolLayout.rows(1, locale = Locale.US)[2].last().char)
        assertEquals('.', row[1].char)
        assertEquals(",", row[1].hint)
    }
    
    @Test
    fun `D-102 page 2 is now five rows, upgraded from a plain leftover catch-all`() {
        val rows = SymbolLayout.rows(2)
        
        assertEquals(5, rows.size)
    }
    
    @Test
    fun `D-100 and D-102 page 2 row one carries the leftover symbols and backspace, no page-toggle key`() {
        val row = SymbolLayout.rows(2)[0]
        
        assertEquals("@_'•©±".toList(), row.dropLast(1).map { it.char })
        assertEquals(KeyCode.DELETE, row.last().code)
        assertTrue(row.none { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `D-102 page 2 row two is a fixed digit row, independent of the number-row setting`() {
        val row = SymbolLayout.rows(2)[1]
        
        assertEquals("1234567890".toList(), row.map { it.char })
    }
    
    @Test
    fun `D-102 page 2 row three is the main number row's shifted symbols, directly tappable`() {
        val row = SymbolLayout.rows(2)[2]
        
        assertEquals("!\"§$%&/()=".toList(), row.map { it.char })
        assertTrue(row.all { it.hint == null && it.alternatives.isEmpty() })
    }
    
    @Test
    fun `D-102 page 2 row four distributes the main page's letter alt-hint symbols`() {
        val row = SymbolLayout.rows(2)[3]
        
        assertEquals("€#-+°×÷*".toList(), row.map { it.char })
    }
    
    @Test
    fun `D-102 no character repeats anywhere on page 2`() {
        val chars = SymbolLayout.rows(2).flatten().mapNotNull { it.char }
        
        assertEquals(chars.size, chars.toSet().size)
    }
    
    @Test
    fun `page 2 bottom row hosts abc, space and enter with no sentence punctuation`() {
        val bottomRow = SymbolLayout.rows(2).last()
        
        assertEquals(KeyCode.LETTERS, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[1].code)
        assertEquals(KeyCode.ENTER, bottomRow[2].code)
        assertNull(bottomRow.firstOrNull { it.char == ',' || it.char == '.' })
    }
    
    @Test
    fun `proportions flow into both pages`() {
        val proportions = KeyProportions(spaceWeight = 2f, enterWeight = 4f)
        
        assertEquals(2f, SymbolLayout.rows(2, proportions).last().byChar(' ').weight, 1e-4f)
        assertEquals(4f, SymbolLayout.rows(2, proportions).last().first { it.code == KeyCode.ENTER }.weight, 1e-4f)
    }
    
    @Test
    fun `with the combined symbol key disabled both pages drop abc, and page 1 also drops the column cell`() {
        val page1Row4 = SymbolLayout.rows(1, symbolKeyEnabled = false)[3]
        val page2Bottom = SymbolLayout.rows(2, symbolKeyEnabled = false).last()
        
        assertEquals(4, page1Row4.size)
        assertTrue(page1Row4.none { it.code == KeyCode.LETTERS })
        
        assertNull(page2Bottom.firstOrNull { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `rows rejects pages other than 1 or 2`() {
        assertThrows(IllegalArgumentException::class.java) { SymbolLayout.rows(3) }
    }
}

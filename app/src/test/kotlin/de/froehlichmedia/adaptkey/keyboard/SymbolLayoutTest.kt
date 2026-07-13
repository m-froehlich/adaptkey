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
 * Unit tests for the D-92 `?123` layer: page 1 (calculator) and page 2 (leftover catch-all).
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
    fun `page 1 row one carries everyday symbols, the page toggle and backspace`() {
        val row = SymbolLayout.rows(1)[0]
        
        assertEquals("(°√π~&|".toList(), row.dropLast(2).map { it.char })
        assertEquals(KeyCode.SYMBOL_PAGE, row[row.size - 2].code)
        assertEquals("1/2", row[row.size - 2].label)
        assertEquals(KeyCode.DELETE, row.last().code)
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
    fun `page 1 has a real digit block with an operator ending each row`() {
        val rows = SymbolLayout.rows(1)
        
        assertEquals(listOf('7', '8', '9', '÷'), rows[1].map { it.char })
        assertEquals(listOf('4', '5', '6', '×'), rows[2].map { it.char })
        assertEquals(listOf('1', '2', '3', '−'), rows[3].map { it.char })
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
    fun `page 1 bottom row hosts abc, currency, zero, decimal separator, plus, equals, space and enter`() {
        val bottomRow = SymbolLayout.rows(1, locale = Locale.GERMANY).last()
        
        assertEquals(KeyCode.LETTERS, bottomRow[0].code)
        assertEquals('€', bottomRow.byChar('€').char)
        assertEquals(listOf("€", "$", "£", "¥"), bottomRow.byChar('€').alternatives)
        assertEquals('0', bottomRow.byChar('0').char)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals(".", bottomRow.byChar(',').hint)
        assertEquals('+', bottomRow.byChar('+').char)
        assertEquals(KeyCode.SPACE, bottomRow[bottomRow.size - 2].code)
        assertEquals(KeyCode.ENTER, bottomRow.last().code)
    }
    
    @Test
    fun `page 1's currency and decimal separator follow the system locale, not a hardcoded default`() {
        val usBottomRow = SymbolLayout.rows(1, locale = Locale.US).last()
        
        assertEquals('$', usBottomRow.byChar('$').char)
        assertEquals('.', usBottomRow.byChar('.').char)
        assertEquals(",", usBottomRow.byChar('.').hint)
    }
    
    @Test
    fun `page 2 is a leftover catch-all with three rows and no number row`() {
        val rows = SymbolLayout.rows(2)
        
        assertEquals(3, rows.size)
        assertTrue(rows.none { row -> row.any { it.char?.isDigit() == true } })
    }
    
    @Test
    fun `page 2 row one carries the leftover symbols, the page toggle and backspace`() {
        val row = SymbolLayout.rows(2)[0]
        
        assertEquals("@_\"'•©±".toList(), row.dropLast(2).map { it.char })
        assertEquals(KeyCode.SYMBOL_PAGE, row[row.size - 2].code)
        assertEquals("2/2", row[row.size - 2].label)
        assertEquals(KeyCode.DELETE, row.last().code)
    }
    
    @Test
    fun `page 2 row two is the bracket family`() {
        val row = SymbolLayout.rows(2)[1]
        
        assertEquals("{}[]<>".toList(), row.map { it.char })
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
        val proportions = KeyProportions(spaceWeight = 2f, commaWeight = 3f, enterWeight = 4f)
        
        assertEquals(2f, SymbolLayout.rows(2, proportions).last().byChar(' ').weight, 1e-4f)
        assertEquals(3f, SymbolLayout.rows(1, proportions, locale = Locale.GERMANY).last().byChar(',').weight, 1e-4f)
        assertEquals(
            4f,
            SymbolLayout.rows(1, proportions).last().first { it.code == KeyCode.ENTER }.weight,
            1e-4f
        )
    }
    
    @Test
    fun `with the combined symbol key disabled both pages drop the page toggle and abc`() {
        val page1Row1 = SymbolLayout.rows(1, symbolKeyEnabled = false)[0]
        val page1Bottom = SymbolLayout.rows(1, symbolKeyEnabled = false).last()
        val page2Row1 = SymbolLayout.rows(2, symbolKeyEnabled = false)[0]
        val page2Bottom = SymbolLayout.rows(2, symbolKeyEnabled = false).last()
        
        assertNull(page1Row1.firstOrNull { it.code == KeyCode.SYMBOL_PAGE })
        assertEquals(KeyCode.DELETE, page1Row1.last().code)
        assertNull(page1Bottom.firstOrNull { it.code == KeyCode.LETTERS })
        
        assertNull(page2Row1.firstOrNull { it.code == KeyCode.SYMBOL_PAGE })
        assertEquals(KeyCode.DELETE, page2Row1.last().code)
        assertNull(page2Bottom.firstOrNull { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `togglePage swaps between page 1 and 2`() {
        assertEquals(2, SymbolLayout.togglePage(1))
        assertEquals(1, SymbolLayout.togglePage(2))
    }
    
    @Test
    fun `rows and togglePage reject pages other than 1 or 2`() {
        assertThrows(IllegalArgumentException::class.java) { SymbolLayout.rows(3) }
        assertThrows(IllegalArgumentException::class.java) { SymbolLayout.togglePage(0) }
    }
}

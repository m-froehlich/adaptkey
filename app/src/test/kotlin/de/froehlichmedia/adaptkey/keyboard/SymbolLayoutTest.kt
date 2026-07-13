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
 * Unit tests for the D-92 / D-100 / D-102 `?123` layer: page 1 (calculator) and page 2 (number-row
 * alternative / catch-all).
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
        
        assertEquals("()°√π~&|".toList(), row.dropLast(1).map { it.char })
        assertEquals(KeyCode.DELETE, row.last().code)
        assertTrue(row.none { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `D-101 corrected - the open and close bracket keys stay separate, each with its own family popup`() {
        val row = SymbolLayout.rows(1)[0]
        
        assertEquals(listOf("(", "[", "{", "<"), row.byChar('(').alternatives)
        assertEquals(listOf(")", "]", "}", ">"), row.byChar(')').alternatives)
    }
    
    @Test
    fun `the calculator page's own pi key also offers the Greek-letter popup`() {
        val row = SymbolLayout.rows(1)[0]
        
        assertEquals(listOf("π", "α", "β", "γ", "δ", "λ", "ω"), row.byChar('π').alternatives)
    }
    
    @Test
    fun `corrected - the operator column reads plus, minus, times, divide top to bottom`() {
        val rows = SymbolLayout.rows(1, locale = Locale.GERMANY)
        
        assertEquals(listOf('7', '8', '9', '+'), rows[1].dropLast(1).map { it.char })
        assertEquals(KeyCode.SPACE, rows[1].last().code)
        
        assertEquals(listOf('4', '5', '6', '−'), rows[2].dropLast(1).map { it.char })
        assertEquals('€', rows[2].last().char)
        
        assertEquals(listOf('1', '2', '3', '×'), rows[3].dropLast(1).map { it.char })
        assertEquals('=', rows[3].last().char)
        
        val row5 = rows[4]
        assertEquals('0', row5[0].char)
        assertEquals(',', row5[1].char)
        assertEquals(KeyCode.LETTERS, row5[2].code)
        assertEquals('÷', row5[3].char)
        assertEquals(KeyCode.ENTER, row5[4].code)
    }
    
    @Test
    fun `the 0 key carries a hash long-press hint for phone-number-style fields`() {
        val zeroKey = SymbolLayout.rows(1)[4][0]
        
        assertEquals('0', zeroKey.char)
        assertEquals("#", zeroKey.hint)
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
        
        assertEquals(listOf("*", "×", "·"), rows[3].byChar('×').alternatives)
        assertEquals(listOf("/", "÷", ":"), rows[4].byChar('÷').alternatives)
        assertEquals(listOf("=", "→", "≈", "≙"), rows[3].byChar('=').alternatives)
    }
    
    @Test
    fun `D-98 and D-100 the currency popup order favours leftward growth`() {
        val row = SymbolLayout.rows(1, locale = Locale.GERMANY)[2]
        
        assertEquals(listOf("¥", "£", "$", "€"), row.byChar('€').alternatives)
    }
    
    @Test
    fun `D-100 corrected - the calculator column cells share the same weight`() {
        val rows = SymbolLayout.rows(1, locale = Locale.GERMANY)
        
        val space = rows[1].last().weight
        val currency = rows[2].last().weight
        val equals = rows[3].last().weight
        val enter = rows[4].last().weight
        
        assertEquals(space, currency, 1e-4f)
        assertEquals(space, equals, 1e-4f)
        assertEquals(space, enter, 1e-4f)
    }
    
    @Test
    fun `page 1's currency and decimal separator follow the system locale, not a hardcoded default`() {
        val row5 = SymbolLayout.rows(1, locale = Locale.US)[4]
        
        assertEquals('$', SymbolLayout.rows(1, locale = Locale.US)[2].last().char)
        assertEquals('.', row5[1].char)
        assertEquals(",", row5[1].hint)
    }
    
    @Test
    fun `D-102 page 2 is now five rows, upgraded from a plain leftover catch-all`() {
        val rows = SymbolLayout.rows(2)
        
        assertEquals(5, rows.size)
    }
    
    @Test
    fun `paragraph 29 follow-up - page 2 row one is led by euro, then the leftover symbols, then backspace`() {
        val row = SymbolLayout.rows(2)[0]
        
        assertEquals("€•@©®Øƒ".toList(), row.dropLast(1).map { it.char })
        assertEquals(KeyCode.DELETE, row.last().code)
        assertTrue(row.none { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `paragraph 29 follow-up - page 2 row one's euro key gets a currency popup growing rightward`() {
        val row = SymbolLayout.rows(2)[0]
        
        assertEquals(listOf("€", "$", "£", "¥"), row.byChar('€').alternatives)
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
        assertTrue(row.none { it.char != '(' && it.char != ')' && (it.hint != null || it.alternatives.isNotEmpty()) })
    }
    
    @Test
    fun `D-102 corrected - the parentheses in row three also get bracket-family popups`() {
        val row = SymbolLayout.rows(2)[2]
        
        assertEquals(listOf("(", "[", "{", "<"), row.byChar('(').alternatives)
        assertEquals(listOf(")", "]", "}", ">"), row.byChar(')').alternatives)
    }
    
    @Test
    fun `paragraph 29 follow-up - page 2 row four distributes the remaining letter alt-hint symbols`() {
        val row = SymbolLayout.rows(2)[3]
        
        assertEquals("#'^°-+×÷*±".toList(), row.map { it.char })
    }
    
    @Test
    fun `D-102 corrected - the minus key in row four gets an underscore alt`() {
        val row = SymbolLayout.rows(2)[3]
        
        assertEquals("_", row.byChar('-').hint)
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
    fun `corrected - both pages' abc key is always present, its reserved-slot hiding is AdaptKeyboardView's job`() {
        val page1Row5 = SymbolLayout.rows(1)[4]
        val page2Bottom = SymbolLayout.rows(2).last()
        
        assertEquals(5, page1Row5.size)
        assertEquals(KeyCode.LETTERS, page1Row5[2].code)
        
        assertEquals(KeyCode.LETTERS, page2Bottom[0].code)
    }
    
    @Test
    fun `rows rejects pages other than 1 or 2`() {
        assertThrows(IllegalArgumentException::class.java) { SymbolLayout.rows(3) }
    }
}

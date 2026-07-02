// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the L-03 numeric/symbol layer key map.
 */
class SymbolLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `page 1 has four rows with digits on top`() {
        val rows = SymbolLayout.rows(1)
        
        assertEquals(4, rows.size)
        assertEquals(10, rows[0].size)
        assertEquals('1', rows[0].first().char)
        assertEquals('0', rows[0].last().char)
    }
    
    @Test
    fun `the third row starts with the page toggle and ends with backspace`() {
        val rows = SymbolLayout.rows(1)
        val thirdRow = rows[2]
        
        assertEquals(KeyCode.SYMBOL_PAGE, thirdRow.first().code)
        assertEquals("1/2", thirdRow.first().label)
        assertEquals(KeyCode.DELETE, thirdRow.last().code)
    }
    
    @Test
    fun `page 2 shows a different page label and different symbols`() {
        val page1 = SymbolLayout.rows(1)
        val page2 = SymbolLayout.rows(2)
        
        assertEquals("2/2", page2[2].first().label)
        assertNotEquals(page1[1].map { it.char }, page2[1].map { it.char })
    }
    
    @Test
    fun `bottom row hosts abc comma space full-stop and enter`() {
        val bottomRow = SymbolLayout.rows(1).last()
        
        assertEquals(KeyCode.LETTERS, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals('.', bottomRow.byChar('.').char)
    }
    
    @Test
    fun `proportions flow into the symbol layout`() {
        val proportions = KeyProportions(spaceWeight = 2f)
        
        assertEquals(2f, SymbolLayout.rows(1, proportions).last()[2].weight, 1e-4f)
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

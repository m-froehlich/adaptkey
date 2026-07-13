// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the QWERTZ key map (L-01) including the number row (L-06), long-press hints
 * (L-05), proportion wiring (L-02 / L-04) and the offset-model key ids.
 */
class KeyboardLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `default layout has five rows`() {
        assertEquals(5, KeyboardLayout.rows().size)
    }
    
    @Test
    fun `hiding the number row drops the top row`() {
        val rows = KeyboardLayout.rows(showNumberRow = false)
        assertEquals(4, rows.size)
        assertEquals('q', rows.first().first().char)
    }
    
    @Test
    fun `number row carries digits one to zero with shifted hints`() {
        val numberRow = KeyboardLayout.rows().first()
        
        assertEquals(10, numberRow.size)
        assertEquals('1', numberRow.first().char)
        assertEquals('0', numberRow.last().char)
        assertEquals("!", numberRow.byChar('1').hint)
        assertEquals("/", numberRow.byChar('7').hint)
        assertEquals("=", numberRow.byChar('0').hint)
    }
    
    @Test
    fun `letters carry the AltGr long-press hints`() {
        val rows = KeyboardLayout.rows()
        
        assertEquals("@", rows[1].byChar('q').hint)
        assertEquals("€", rows[1].byChar('e').hint)
        assertEquals("°", rows[2].byChar('d').hint)
        assertEquals("#", rows[2].byChar('h').hint)
        assertEquals("+", rows[3].byChar('n').hint)
        assertEquals("-", rows[3].byChar('m').hint)
        // D-90: math-symbol hint on p.
        assertEquals("π", rows[1].byChar('p').hint)
        // D-96: reorganised math-symbol hints - × on x, ÷ on c, / on v, * on b.
        assertEquals("×", rows[3].byChar('x').hint)
        assertEquals("÷", rows[3].byChar('c').hint)
        assertEquals("/", rows[3].byChar('v').hint)
        assertEquals("*", rows[3].byChar('b').hint)
        // §29 follow-up: the function-symbol hint on f.
        assertEquals("ƒ", rows[2].byChar('f').hint)
    }
    
    @Test
    fun `D-99 the pi key offers a Greek-letter popup alongside pi itself`() {
        val pKey = KeyboardLayout.rows()[1].byChar('p')
        
        assertEquals(listOf("π", "α", "β", "γ", "δ", "λ", "ω"), pKey.alternatives)
    }
    
    @Test
    fun `D-99 a reassigned p key loses the Greek-letter popup`() {
        val pKey = KeyboardLayout.rows(letterHints = mapOf('p' to "!"))[1].byChar('p')
        
        assertEquals("!", pKey.hint)
        assertTrue(pKey.alternatives.isEmpty())
    }
    
    @Test
    fun `the o key offers an average-symbol popup alongside its umlaut`() {
        val oKey = KeyboardLayout.rows()[1].byChar('o')
        
        assertEquals(listOf("ö", "Ø"), oKey.alternatives)
    }
    
    @Test
    fun `a reassigned o key loses the average-symbol popup`() {
        val oKey = KeyboardLayout.rows(letterHints = mapOf('o' to "!"))[1].byChar('o')
        
        assertEquals("!", oKey.hint)
        assertTrue(oKey.alternatives.isEmpty())
    }
    
    @Test
    fun `longPressSymbol returns the secondary for character keys and null otherwise`() {
        val rows = KeyboardLayout.rows()
        
        assertEquals("@", KeyboardLayout.longPressSymbol(rows[1].byChar('q')))
        assertEquals("/", KeyboardLayout.longPressSymbol(rows.first().byChar('7')))
        assertNull(KeyboardLayout.longPressSymbol(rows[2].byChar('g')))
        assertNull(KeyboardLayout.longPressSymbol(rows[3].first()))
    }
    
    @Test
    fun `hasLongPressAction is true for hinted letters, number-row digits and the combined key`() {
        val rows = KeyboardLayout.rows()
        
        assertTrue(KeyboardLayout.hasLongPressAction(rows[1].byChar('q')))
        assertTrue(KeyboardLayout.hasLongPressAction(rows.first().byChar('7')))
        assertTrue(KeyboardLayout.hasLongPressAction(rows.last().first()))
        assertFalse(KeyboardLayout.hasLongPressAction(rows[2].byChar('g')))
        assertFalse(KeyboardLayout.hasLongPressAction(rows.last()[2]))
    }
    
    @Test
    fun `the combined key carries a 123 corner hint`() {
        val symbolKey = KeyboardLayout.rows().last().first()
        
        assertEquals(KeyCode.SYMBOL, symbolKey.code)
        assertEquals("123", symbolKey.hint)
    }
    
    @Test
    fun `a custom hint map drives the long-press secondary`() {
        val rows = KeyboardLayout.rows(letterHints = mapOf('q' to "!"))
        
        assertEquals("!", KeyboardLayout.longPressSymbol(rows[1].byChar('q')))
        assertNull(KeyboardLayout.longPressSymbol(rows[1].byChar('e')))
    }
    
    @Test
    fun `third row starts with shift and ends with a widened backspace`() {
        val proportions = KeyProportions()
        val thirdRow = KeyboardLayout.rows(proportions)[3]
        
        assertEquals(KeyCode.SHIFT, thirdRow.first().code)
        assertEquals(KeyCode.DELETE, thirdRow.last().code)
        assertEquals(proportions.backspaceWeight, thirdRow.last().weight, 1e-4f)
        assertEquals(proportions.thirdRowLetterWeight(7), thirdRow.byChar('y').weight, 1e-4f)
    }
    
    @Test
    fun `bottom row hosts symbol comma space full-stop and enter`() {
        val proportions = KeyProportions()
        val bottomRow = KeyboardLayout.rows(proportions).last()
        
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(proportions.commaWeight, bottomRow.byChar(',').weight, 1e-4f)
        assertEquals(proportions.periodWeight, bottomRow.byChar('.').weight, 1e-4f)
        assertEquals(proportions.spaceWeight, bottomRow[2].weight, 1e-4f)
    }
    
    @Test
    fun `custom proportions flow into the layout`() {
        val bottomRow = KeyboardLayout.rows(KeyProportions(spaceWeight = 2f)).last()
        assertEquals(2f, bottomRow[2].weight, 1e-4f)
    }
    
    @Test
    fun `key ids distinguish characters from control keys`() {
        val rows = KeyboardLayout.rows()
        
        assertEquals("c:q", rows[1].byChar('q').id)
        assertEquals("c:,", rows.last().byChar(',').id)
        assertEquals("SPACE", rows.last()[2].id)
        assertEquals("SHIFT", rows[3].first().id)
    }
}

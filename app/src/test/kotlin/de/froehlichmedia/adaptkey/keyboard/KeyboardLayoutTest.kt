// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

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
    fun `D-105 every digit key now has two alternatives - its shifted symbol and its own superscript`() {
        val numberRow = KeyboardLayout.rows().first()
        
        // The shifted symbol (unchanged) stays index 0, pre-selected; the superscript is the new index 1.
        assertEquals(listOf("!", "¹"), numberRow.byChar('1').alternatives)
        assertEquals(listOf("\"", "²"), numberRow.byChar('2').alternatives)
        assertEquals(listOf("§", "³"), numberRow.byChar('3').alternatives)
        assertTrue(KeyboardLayout.hasLongPressAction(numberRow.byChar('2')))
    }
    
    @Test
    fun `D-105 the 0 key - at the row's right edge - has its alternatives reversed`() {
        val numberRow = KeyboardLayout.rows().first()
        
        assertEquals(listOf("⁰", "="), numberRow.byChar('0').alternatives)
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
        
        // §34: reversed relative to KeyboardLayout.PI_ALTERNATIVES - p sits at the row's right edge, where
        // the popup grows leftward, so pi itself must be last (nearest the key) rather than first.
        assertEquals(listOf("ω", "λ", "δ", "γ", "β", "α", "π"), pKey.alternatives)
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
    fun `paragraph 31 hasLongPressAction is true for the calculator minus key despite no hint or alternatives`() {
        val minusKey = SymbolLayout.rows(1)[2].byChar(SymbolLayout.MINUS_SIGN)
        
        assertTrue(minusKey.hint == null && minusKey.alternatives.isEmpty())
        assertTrue(KeyboardLayout.hasLongPressAction(minusKey))
    }
    
    @Test
    fun `paragraph 53 hasLongPressAction is true for both the multi-alternative and single-hint TEXT keys`() {
        val row = SymbolLayout.rows(1)[0]
        val sinKey = row.first { it.code == KeyCode.TEXT && it.label == "sin" }
        val degKey = row.first { it.code == KeyCode.TEXT && it.label == "deg" }
        
        assertTrue(KeyboardLayout.hasLongPressAction(sinKey))
        assertTrue(KeyboardLayout.hasLongPressAction(degKey))
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
    
    @Test
    fun `qwertz is the default - z sits in the top row, y leads the third row`() {
        val rows = KeyboardLayout.rows()
        
        assertEquals('z', rows[1][5].char)
        assertEquals('y', rows[3][1].char)
    }
    
    @Test
    fun `qwerty swaps y and z - y sits in the top row, z leads the third row`() {
        val rows = KeyboardLayout.rows(qwerty = true)
        
        assertEquals('y', rows[1][5].char)
        assertEquals('z', rows[3][1].char)
    }
    
    @Test
    fun `qwerty keeps every other row and hint identical to qwertz`() {
        val qwertz = KeyboardLayout.rows()
        val qwerty = KeyboardLayout.rows(qwerty = true)
        
        assertEquals(qwertz[0], qwerty[0]) // number row
        assertEquals(qwertz[2], qwerty[2]) // middle row (asdfghjkl)
        assertEquals(qwertz.last(), qwerty.last()) // bottom row
        assertEquals(qwertz[1].byChar('p').hint, qwerty[1].byChar('p').hint) // π hint follows the char
    }
    
    @Test
    fun `D-143 urlMode replaces the bottom row with the URL row`() {
        val bottomRow = KeyboardLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(7, bottomRow.size)
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.TEXT, bottomRow[1].code)
        assertEquals("https://", bottomRow[1].label)
        assertEquals(KeyCode.TEXT, bottomRow[2].code)
        assertEquals("www.", bottomRow[2].label)
        assertEquals(KeyCode.CHAR, bottomRow[3].code)
        assertEquals('/', bottomRow[3].char)
        assertEquals(KeyCode.SPACE, bottomRow[4].code)
        assertEquals(KeyCode.CHAR, bottomRow[5].code)
        assertEquals('.', bottomRow[5].char)
        assertEquals(KeyCode.ENTER, bottomRow[6].code)
    }
    
    @Test
    fun `D-143 without urlMode the bottom row is unaffected by the locale parameter`() {
        assertEquals(KeyboardLayout.rows().last(), KeyboardLayout.rows(locale = Locale.US).last())
    }
    
    @Test
    fun `D-144 the slash key's popup is exactly comma's original list, no redundant slash entry`() {
        val bottomRow = KeyboardLayout.rows(urlMode = true).last()
        
        assertEquals(KeyboardLayout.COMMA_ALTERNATIVES, bottomRow.byChar('/').alternatives)
        assertFalse(bottomRow.byChar('/').alternatives.contains("/"))
    }
    
    @Test
    fun `D-143 the https key offers the other everyday protocols, www has neither hint nor alternatives`() {
        val bottomRow = KeyboardLayout.rows(urlMode = true).last()
        val httpsKey = bottomRow.first { it.code == KeyCode.TEXT && it.label == "https://" }
        val wwwKey = bottomRow.first { it.code == KeyCode.TEXT && it.label == "www." }
        
        assertEquals(listOf("https://", "http://", "ftp://", "file://"), httpsKey.alternatives)
        assertNull(wwwKey.hint)
        assertTrue(wwwKey.alternatives.isEmpty())
    }
    
    @Test
    fun `D-143 the period key's popup is locale-resolved TLDs instead of the sentence terminators`() {
        val germanRow = KeyboardLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        val usRow = KeyboardLayout.rows(urlMode = true, locale = Locale.US).last()
        
        assertEquals(UrlLocale.periodAlternatives(Locale.GERMANY), germanRow.byChar('.').alternatives)
        assertEquals(UrlLocale.periodAlternatives(Locale.US), usRow.byChar('.').alternatives)
    }
    
    @Test
    fun `D-143 urlMode leaves every other row identical to the ordinary layout`() {
        val ordinary = KeyboardLayout.rows()
        val url = KeyboardLayout.rows(urlMode = true)
        
        assertEquals(ordinary[0], url[0]) // number row
        assertEquals(ordinary[1], url[1]) // top row (p / π etc.)
        assertEquals(ordinary[2], url[2]) // middle row
        assertEquals(ordinary[3], url[3]) // third row (shift / backspace)
    }
    
    @Test
    fun `D-158 emailMode replaces the bottom row with the email row`() {
        val bottomRow = KeyboardLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(6, bottomRow.size)
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.CHAR, bottomRow[1].code)
        assertEquals('@', bottomRow[1].char)
        assertEquals(KeyCode.CHAR, bottomRow[2].code)
        assertEquals('-', bottomRow[2].char)
        assertEquals(KeyCode.SPACE, bottomRow[3].code)
        assertEquals(KeyCode.CHAR, bottomRow[4].code)
        assertEquals('.', bottomRow[4].char)
        assertEquals(KeyCode.ENTER, bottomRow[5].code)
    }
    
    @Test
    fun `D-158 the at key's popup is exactly comma's original list, no redundant at entry`() {
        val bottomRow = KeyboardLayout.rows(emailMode = true).last()
        
        assertEquals(KeyboardLayout.COMMA_ALTERNATIVES, bottomRow.byChar('@').alternatives)
        assertFalse(bottomRow.byChar('@').alternatives.contains("@"))
    }
    
    @Test
    fun `D-158 the dash key has a single underscore secondary and no popup`() {
        val bottomRow = KeyboardLayout.rows(emailMode = true).last()
        val dashKey = bottomRow.byChar('-')
        
        assertEquals("_", dashKey.hint)
        assertTrue(dashKey.alternatives.isEmpty())
    }
    
    @Test
    fun `D-158 the period key's popup is locale-resolved TLDs, exactly like urlMode's`() {
        val emailRow = KeyboardLayout.rows(emailMode = true, locale = Locale.GERMANY).last()
        val urlRow = KeyboardLayout.rows(urlMode = true, locale = Locale.GERMANY).last()
        
        assertEquals(UrlLocale.periodAlternatives(Locale.GERMANY), emailRow.byChar('.').alternatives)
        assertEquals(urlRow.byChar('.').alternatives, emailRow.byChar('.').alternatives)
    }
    
    @Test
    fun `D-158 emailMode leaves every other row identical to the ordinary layout`() {
        val ordinary = KeyboardLayout.rows()
        val email = KeyboardLayout.rows(emailMode = true)
        
        assertEquals(ordinary[0], email[0]) // number row
        assertEquals(ordinary[1], email[1]) // top row (p / π etc.)
        assertEquals(ordinary[2], email[2]) // middle row
        assertEquals(ordinary[3], email[3]) // third row (shift / backspace)
    }
    
    @Test
    fun `D-158 urlMode takes priority over emailMode when both are somehow set`() {
        assertEquals(KeyboardLayout.rows(urlMode = true).last(), KeyboardLayout.rows(urlMode = true, emailMode = true).last())
    }
}

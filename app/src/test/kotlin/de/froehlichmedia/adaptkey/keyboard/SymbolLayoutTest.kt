// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        
        // §53: the row now also carries the sin/deg TEXT keys, between the symbols and backspace.
        assertEquals("()°√π~&|".toList(), row.dropLast(3).map { it.char })
        assertEquals(KeyCode.DELETE, row.last().code)
        assertTrue(row.none { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `paragraph 53 page 1 row one carries the sin and deg TEXT keys before backspace`() {
        val row = SymbolLayout.rows(1)[0]
        val sinKey = row[row.size - 3]
        val degKey = row[row.size - 2]
        
        assertEquals(KeyCode.TEXT, sinKey.code)
        assertEquals("sin", sinKey.label)
        assertEquals(listOf("sin", "cos", "tan", "log"), sinKey.alternatives)
        
        assertEquals(KeyCode.TEXT, degKey.code)
        assertEquals("deg", degKey.label)
        assertEquals("rad", degKey.hint)
    }
    
    @Test
    fun `paragraph 53 the sin and deg TEXT keys have distinct ids for the T-03 offset model`() {
        val row = SymbolLayout.rows(1)[0]
        val sinKey = row.first { it.code == KeyCode.TEXT && it.label == "sin" }
        val degKey = row.first { it.code == KeyCode.TEXT && it.label == "deg" }
        
        assertEquals("t:sin", sinKey.id)
        assertEquals("t:deg", degKey.id)
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
        
        // D-394: digit block mirrored vertically to phone-style order (1 2 3 / 4 5 6 / 7 8 9) - the operator
        // column itself (+ − × ÷ top to bottom) is unaffected, it stays tied to its row.
        assertEquals(listOf('1', '2', '3', '+'), rows[1].dropLast(1).map { it.char })
        assertEquals(KeyCode.SPACE, rows[1].last().code)
        
        assertEquals(listOf('4', '5', '6', '−'), rows[2].dropLast(1).map { it.char })
        assertEquals('€', rows[2].last().char)
        
        assertEquals(listOf('7', '8', '9', '×'), rows[3].dropLast(1).map { it.char })
        assertEquals('=', rows[3].last().char)
        
        val row5 = rows[4]
        assertEquals(KeyCode.LETTERS, row5[0].code)
        assertEquals('0', row5[1].char)
        assertEquals(',', row5[2].char)
        assertEquals('÷', row5[3].char)
        assertEquals(KeyCode.ENTER, row5[4].code)
    }
    
    @Test
    fun `the 0 key carries a hash long-press hint for phone-number-style fields`() {
        val zeroKey = SymbolLayout.rows(1)[4][1]
        
        assertEquals('0', zeroKey.char)
        assertEquals("#", zeroKey.hint)
    }
    
    @Test
    fun `paragraph 36 and 37 the 0 key is centred under 2, flanked by equally-narrow ABC and decimal-separator keys`() {
        val row5 = SymbolLayout.rows(1, locale = Locale.GERMANY)[4]
        
        assertEquals(0.75f, row5[0].weight, 1e-4f)
        assertEquals(1.5f, row5[1].weight, 1e-4f)
        assertEquals(0.75f, row5[2].weight, 1e-4f)
        // The row's own total (3, matching rows 2-4's three digit/operator cells) confirms nothing else
        // silently absorbed the weight freed up from the two narrowed flanking keys.
        assertEquals(3f, row5[0].weight + row5[1].weight + row5[2].weight, 1e-4f)
    }
    
    @Test
    fun `page 1's own 2 and 3 keys carry squared and cubed hints`() {
        // D-394: 2/3 moved to row 2 (index 1) with the digit-block mirror - was row 4 (index 3).
        val digitRow = SymbolLayout.rows(1)[1]
        
        assertEquals("²", digitRow.byChar('2').hint)
        assertEquals("³", digitRow.byChar('3').hint)
    }
    
    @Test
    fun `the operator keys carry their alt-popups`() {
        val rows = SymbolLayout.rows(1)
        
        assertEquals(listOf("*", "×", "·"), rows[3].byChar('×').alternatives)
        // D-498: the backslash joins the divide popup right after the slash; ÷ stays pre-selected by value.
        assertEquals(listOf("/", "\\", "÷", ":"), rows[4].byChar('÷').alternatives)
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
        assertEquals('.', row5[2].char)
        assertEquals(",", row5[2].hint)
    }
    
    @Test
    fun `D-102 page 2 is now five rows, upgraded from a plain leftover catch-all`() {
        val rows = SymbolLayout.rows(2)
        
        assertEquals(5, rows.size)
    }
    
    @Test
    fun `paragraph 29 follow-up - page 2 row one is led by the leftover symbols, then backspace`() {
        val row = SymbolLayout.rows(2)[0]
        
        assertEquals("•@©®Øƒ".toList(), row.drop(1).dropLast(1).map { it.char })
        assertEquals(KeyCode.DELETE, row.last().code)
        assertTrue(row.none { it.code == KeyCode.LETTERS })
    }
    
    @Test
    fun `D-519 the tilde replaces the currency key as row one's leading cell`() {
        val row = SymbolLayout.rows(2)[0]
        
        assertEquals('~', row.first().char)
        // The former € key's currency popup is gone with the key itself - the currency lives on the
        // letters page's d key now (D-520), and page 1's calculator column keeps its own D-92 key.
        assertTrue(row.first().alternatives.isEmpty())
        assertNull(row.first().hint)
    }
    
    @Test
    fun `D-102 page 2 row two is a fixed digit row, independent of the number-row setting`() {
        val row = SymbolLayout.rows(2)[1]
        
        assertEquals("1234567890".toList(), row.map { it.char })
    }
    
    @Test
    fun `D-102 page 2 row three is the main number row's shifted symbols, directly tappable`() {
        // D-523: the row's $ position is the currency key now (a §53 TEXT key with no char of its own),
        // so this verbatim-row assertion pins Locale.US - there the key's own label IS the dollar - and
        // reads each key's own display character.
        val row = SymbolLayout.rows(2, locale = Locale.US)[2]
        
        assertEquals("!\"§$%&/()=".toList(), row.map { it.char ?: it.label.first() })
        // D-498: `/` additionally carries the backslash as its single direct-commit hint; everything else
        // except the bracket keys and the currency key (index 3) stays plain.
        assertTrue(row.filterIndexed { index, _ -> index != 3 }.none { key ->
            key.char != '(' && key.char != ')' && key.char != '/' && (key.hint != null || key.alternatives.isNotEmpty())
        })
    }
    
    @Test
    fun `D-498 page 2 row three's slash key carries the backslash as its single direct hint`() {
        val row = SymbolLayout.rows(2)[2]
        
        assertEquals("\\", row.byChar('/').hint)
        assertTrue(row.byChar('/').alternatives.isEmpty())
    }
    
    @Test
    fun `D-102 corrected - the parentheses in row three also get bracket-family popups`() {
        val row = SymbolLayout.rows(2)[2]
        
        assertEquals(listOf("(", "[", "{", "<"), row.byChar('(').alternatives)
        assertEquals(listOf(")", "]", "}", ">"), row.byChar(')').alternatives)
    }
    
    @Test
    fun `D-523 the currency key takes the shifted-symbols row's dollar position with the alternating top-10 popup`() {
        val currencyKey = SymbolLayout.rows(2, locale = Locale.GERMANY)[2][3]
        
        // The key's own identity IS the region's currency: a §53 TEXT key whose label commits verbatim
        // (one glyph or two - "Fr"/"R$" work, unlike a CHAR key's single char), deliberately without a
        // corner hint (the label already shows the primary).
        assertEquals(KeyCode.TEXT, currencyKey.code)
        assertEquals("€", currencyKey.label)
        assertNull(currencyKey.char)
        assertNull(currencyKey.hint)
        assertNull(currencyKey.leftHint)
        // The popup is exactly the world's other top currencies in relevance order - the primary never
        // repeats - and the flag is what the view places the alternating arrangement by (the anchor cell
        // directly over the key, the rest left/right in turn, a straight release committing the key's
        // own symbol via the view's sentinel fallback).
        assertEquals(listOf("$", "¥", "£", "₹", "₩", "₽", "₺", "Fr", "R$"), currencyKey.alternatives)
        assertTrue(currencyKey.alternatingPopup)
        // D-527: an ordinary symbol key in role - it must not wear the muted function-key styling.
        assertFalse(currencyKey.specialStyling)
        assertFalse(HorizontalLongPressPopup.isDualCornerStraddle(currencyKey, currencyKey.alternatives))
        assertTrue(KeyboardLayout.hasLongPressAction(currencyKey))
        // A dollar-primary device keeps its own $ as the key, the euro leading the popup.
        val usKey = SymbolLayout.rows(2, locale = Locale.US)[2][3]
        assertEquals("$", usKey.label)
        assertEquals(listOf("€", "¥", "£", "₹", "₩", "₽", "₺", "Fr", "R$"), usKey.alternatives)
        assertTrue(usKey.alternatingPopup)
    }
    
    @Test
    fun `D-523 a Swiss device's currency key carries the two-glyph Fr as its label, excluded from the popup`() {
        val currencyKey = SymbolLayout.rows(2, locale = Locale("de", "CH"))[2][3]
        
        assertEquals("Fr", currencyKey.label)
        assertEquals(CurrencyLocale.popupSymbols("Fr"), currencyKey.alternatives)
        assertFalse(currencyKey.alternatives.contains("Fr"))
        assertTrue(currencyKey.alternatingPopup)
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
        assertEquals(KeyCode.LETTERS, page1Row5[0].code)
        
        assertEquals(KeyCode.LETTERS, page2Bottom[0].code)
    }
    
    @Test
    fun `rows rejects pages other than 1 or 2`() {
        assertThrows(IllegalArgumentException::class.java) { SymbolLayout.rows(3) }
    }
}

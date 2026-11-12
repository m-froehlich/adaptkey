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
 * Unit tests for the French AZERTY key map (D-314): row structure (letters moving between rows,
 * unlike QWERTZ's within-row y/z swap), the shared number row / control row / bottom row, and the
 * reused p/o math-symbol popups.
 */
class AzertyLayoutTest {
    
    private fun List<Key>.byChar(c: Char): Key {
        return first { it.char == c }
    }
    
    @Test
    fun `default layout has five rows`() {
        assertEquals(5, AzertyLayout.rows().size)
    }
    
    @Test
    fun `hiding the number row drops the top row`() {
        val rows = AzertyLayout.rows(showNumberRow = false)
        
        assertEquals(4, rows.size)
        assertEquals('a', rows.first().first().char)
    }
    
    @Test
    fun `the three letter rows follow the standard French AZERTY arrangement`() {
        val rows = AzertyLayout.rows(showNumberRow = false)
        
        assertEquals("azertyuiop", rows[0].joinToString("") { it.char.toString() })
        // D-512 §2.2: the dedicated apostrophe key sits right of m on AZERTY.
        assertEquals("qsdfghjklm'", rows[1].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertEquals("wxcvbn", rows[2].filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
    }
    
    @Test
    fun `D-515 the AZERTY apostrophe key is gated by showApostropheKey too`() {
        // Off (the app's default for non-English layouts): the AZERTY middle row keeps its canonical length.
        val without = AzertyLayout.rows(showApostropheKey = false)[2]
        assertEquals("qsdfghjklm", without.filter { it.code == KeyCode.CHAR }.joinToString("") { it.char.toString() })
        assertFalse(without.any { it.char == '\'' })
        // On: the reform's shape is back.
        assertEquals(
            AzertyLayout.rows()[2],
            AzertyLayout.rows(showApostropheKey = true)[2]
        )
    }
    
    @Test
    fun `m sits on the home row, not the third row`() {
        val rows = AzertyLayout.rows()
        
        assertTrue(rows[2].any { it.char == 'm' })
        assertFalse(rows[3].any { it.char == 'm' })
    }
    
    @Test
    fun `letters carry the AltGr long-press hints by character, same as the Latin layout`() {
        val rows = AzertyLayout.rows()
        
        assertEquals("ä", rows[1].byChar('a').hint)
        assertEquals("ß", rows[2].byChar('s').hint)
        assertEquals("€", rows[1].byChar('e').hint)
    }
    
    @Test
    fun `the pi key offers its Greek-letter popup exactly like the Latin layout, at the same top-row position`() {
        val azertyP = AzertyLayout.rows()[1].byChar('p')
        val latinP = KeyboardLayout.rows()[1].byChar('p')
        
        assertEquals(latinP.hint, azertyP.hint)
        assertEquals(latinP.alternatives, azertyP.alternatives)
        assertEquals(KeyboardLayout.PI_ALTERNATIVES, azertyP.alternatives)
    }
    
    @Test
    fun `the o key offers its average-symbol popup exactly like the Latin layout`() {
        val azertyO = AzertyLayout.rows()[1].byChar('o')
        
        assertEquals(listOf("ö", "Ø"), azertyO.alternatives)
    }
    
    @Test
    fun `a reassigned p key loses the Greek-letter popup, same as the Latin layout`() {
        val pKey = AzertyLayout.rows(letterHints = mapOf('p' to "!"))[1].byChar('p')
        
        assertEquals("!", pKey.hint)
        assertTrue(pKey.alternatives.isEmpty())
    }
    
    @Test
    fun `D-336 the a key offers the Nordic ligatures, same as the Latin layout, at the AZERTY top row`() {
        val aKey = AzertyLayout.rows()[1].byChar('a')
        
        assertEquals(listOf("ä", "æ", "å", "à"), aKey.alternatives)
    }
    
    @Test
    fun `D-336 the e key offers the French diacritics, same as the Latin layout`() {
        val eKey = AzertyLayout.rows()[1].byChar('e')
        
        assertEquals(listOf("€", "é", "è", "ê", "ë"), eKey.alternatives)
    }
    
    @Test
    fun `D-336 D-512 the n key offers the universal tilde-n right corner with its paren left corner, at the AZERTY third row`() {
        val nKey = AzertyLayout.rows()[3].byChar('n')
        
        assertEquals("ñ", nKey.hint)
        assertEquals("(", nKey.leftHint)
        assertEquals(listOf("(", "ñ"), nKey.alternatives)
    }
    
    @Test
    fun `D-512 the apostrophe key sits right of m and the w key leads the third row with the plus slot`() {
        val rows = AzertyLayout.rows()
        
        val apostropheKey = rows[2].last()
        assertEquals('\'', apostropheKey.char)
        // D-524: `"` is the apostrophe key's single corner (the long-press release default, committing
        // the double quote directly); the apostrophe itself is no longer its own alternative.
        assertEquals("\"", apostropheKey.hint)
        assertNull(apostropheKey.leftHint)
        assertTrue(apostropheKey.alternatives.isEmpty())
        // §2.8: w (AZERTY's first third-row key) carries the + left corner, screen-stable as a slot -
        // left-corner-only, so the release default is the + itself (left-corner fallback).
        assertEquals("+", rows[3].byChar('w').leftHint)
        assertNull(rows[3].byChar('w').hint)
        assertEquals("+", rows[3].byChar('w').defaultCornerHint)
    }
    
    @Test
    fun `a custom hint map drives the long-press secondary`() {
        val rows = AzertyLayout.rows(letterHints = mapOf('a' to "!"))
        
        assertEquals("!", KeyboardLayout.longPressSymbol(rows[1].byChar('a')))
        assertNull(KeyboardLayout.longPressSymbol(rows[1].byChar('z')))
    }
    
    @Test
    fun `third row starts with shift and ends with a widened backspace`() {
        val proportions = KeyProportions()
        val thirdRow = AzertyLayout.rows(proportions)[3]
        
        assertEquals(KeyCode.SHIFT, thirdRow.first().code)
        assertEquals(KeyCode.DELETE, thirdRow.last().code)
        assertEquals(proportions.backspaceWeight, thirdRow.last().weight, 1e-4f)
        assertEquals(proportions.thirdRowLetterWeight(6), thirdRow.byChar('w').weight, 1e-4f)
    }
    
    @Test
    fun `D-511 the number row shares the reform's dual-corner row`() {
        val numberRow = AzertyLayout.rows().first()
        
        assertEquals(10, numberRow.size)
        assertEquals('1', numberRow.first().char)
        assertEquals("!", numberRow.byChar('1').hint)
        assertEquals("~", numberRow.byChar('1').leftHint)
        assertEquals(listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"), numberRow.map { it.hint })
    }
    
    @Test
    fun `D-105 D-511 every digit key rides the reformed popup orders`() {
        val numberRow = AzertyLayout.rows().first()
        
        assertEquals(listOf("~", "¹", "!"), numberRow.byChar('1').alternatives)
        assertEquals(listOf("=", "⁰", ")"), numberRow.byChar('0').alternatives)
        // D-498: the backslash rides along on the shared number-row builder, all layouts alike - now with
        // the middle division sign ahead of it and the ampersand as the release default.
        assertEquals(listOf("/", "÷", "\\", "⁷", "&"), numberRow.byChar('7').alternatives)
    }
    
    @Test
    fun `the bottom row mirrors the Latin layout controls`() {
        val bottomRow = AzertyLayout.rows().last()
        
        assertEquals(KeyCode.SYMBOL, bottomRow[0].code)
        assertEquals(KeyCode.SPACE, bottomRow[2].code)
        assertEquals(KeyCode.ENTER, bottomRow[4].code)
        assertEquals(',', bottomRow.byChar(',').char)
        assertEquals('.', bottomRow.byChar('.').char)
    }
    
    @Test
    fun `custom proportions flow into the AZERTY layout`() {
        val bottomRow = AzertyLayout.rows(KeyProportions(spaceWeight = 2f)).last()
        
        assertEquals(2f, bottomRow[2].weight, 1e-4f)
    }
    
    @Test
    fun `urlMode gives the AZERTY layout the same URL bottom row as the Latin layout`() {
        val azertyRow = AzertyLayout.rows(urlMode = true, locale = Locale.FRANCE).last()
        val latinRow = KeyboardLayout.rows(urlMode = true, locale = Locale.FRANCE).last()
        
        assertEquals(latinRow, azertyRow)
    }
    
    @Test
    fun `without urlMode the AZERTY bottom row is unaffected by the locale parameter`() {
        assertEquals(AzertyLayout.rows().last(), AzertyLayout.rows(locale = Locale.US).last())
    }
    
    @Test
    fun `emailMode gives the AZERTY layout the same email bottom row as the Latin layout`() {
        val azertyRow = AzertyLayout.rows(emailMode = true, locale = Locale.FRANCE).last()
        val latinRow = KeyboardLayout.rows(emailMode = true, locale = Locale.FRANCE).last()
        
        assertEquals(latinRow, azertyRow)
    }
}

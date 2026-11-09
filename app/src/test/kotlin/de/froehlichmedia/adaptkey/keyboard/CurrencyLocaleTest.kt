// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the D-520 currency slot's pure resolver - the system region decides the currency,
 * the world's ten most relevant symbols (de-duplicated by glyph) form the popup, and the primary is
 * never repeated in it.
 */
class CurrencyLocaleTest {
    
    @Test
    fun `the top-10 regions resolve their own currency symbol deterministically`() {
        assertEquals("$", CurrencyLocale.primarySymbol(Locale.US))
        assertEquals("€", CurrencyLocale.primarySymbol(Locale.GERMANY))
        assertEquals("€", CurrencyLocale.primarySymbol(Locale.FRANCE))
        assertEquals("€", CurrencyLocale.primarySymbol(Locale("de", "AT")))
        assertEquals("£", CurrencyLocale.primarySymbol(Locale.UK))
        assertEquals("¥", CurrencyLocale.primarySymbol(Locale.CHINA))
        assertEquals("¥", CurrencyLocale.primarySymbol(Locale.JAPAN))
        assertEquals("₹", CurrencyLocale.primarySymbol(Locale("en", "IN")))
        assertEquals("₩", CurrencyLocale.primarySymbol(Locale.KOREA))
        assertEquals("₽", CurrencyLocale.primarySymbol(Locale("ru", "RU")))
        assertEquals("₺", CurrencyLocale.primarySymbol(Locale("tr", "TR")))
        assertEquals("R$", CurrencyLocale.primarySymbol(Locale("pt", "BR")))
    }
    
    @Test
    fun `the Swiss franc resolves to the two-glyph Fr, never the three-letter ISO code`() {
        assertEquals("Fr", CurrencyLocale.primarySymbol(Locale("de", "CH")))
        assertEquals("Fr", CurrencyLocale.primarySymbol(Locale("it", "CH")))
        assertEquals("Fr", CurrencyLocale.primarySymbol(Locale("de", "LI")))
    }
    
    @Test
    fun `a region the tables do not know falls back to the JDK's own short symbol`() {
        // Sweden's JDK symbol is the short "kr" - passes through the length check unchanged.
        assertEquals("kr", CurrencyLocale.primarySymbol(Locale("sv", "SE")))
    }
    
    @Test
    fun `a region-less locale falls back to the euro, the D-92 calculator precedent`() {
        assertEquals("€", CurrencyLocale.primarySymbol(Locale.GERMAN))
        assertEquals("€", CurrencyLocale.primarySymbol(Locale("xx", "")))
    }
    
    @Test
    fun `every resolved primary is within the corner glyph budget`() {
        for (region in listOf("US", "DE", "GB", "CN", "JP", "IN", "KR", "RU", "TR", "CH", "BR", "SE", "PL")) {
            val symbol = CurrencyLocale.primarySymbol(Locale("", region))
            assertTrue(symbol.isNotEmpty() && symbol.length <= 2, region)
        }
    }
    
    @Test
    fun `the popup is the relevance order minus the primary - euro primary keeps dollar first`() {
        assertEquals(
            listOf("$", "¥", "£", "₹", "₩", "₽", "₺", "Fr", "R$"),
            CurrencyLocale.popupSymbols("€")
        )
    }
    
    @Test
    fun `a dollar primary keeps the euro first in the popup`() {
        assertEquals(
            listOf("€", "¥", "£", "₹", "₩", "₽", "₺", "Fr", "R$"),
            CurrencyLocale.popupSymbols("$")
        )
    }
    
    @Test
    fun `the primary never repeats in the popup and the relevance order never repeats a glyph`() {
        val primary = CurrencyLocale.primarySymbol(Locale.GERMANY)
        val popup = CurrencyLocale.popupSymbols(primary)
        
        assertFalse(popup.contains(primary))
        assertEquals(popup.size, popup.toSet().size)
        assertEquals(CurrencyLocale.RELEVANCE_ORDER.size - 1, popup.size)
    }
    
    @Test
    fun `a long-tail primary outside the relevance list leaves the full ten in the popup`() {
        assertEquals(CurrencyLocale.RELEVANCE_ORDER, CurrencyLocale.popupSymbols("kr"))
        assertEquals(CurrencyLocale.RELEVANCE_ORDER, CurrencyLocale.popupSymbols("zł"))
    }
    
    @Test
    fun `dollar and euro are always among the ten, whichever is the primary`() {
        val dollarPrimary = listOf("$") + CurrencyLocale.popupSymbols("$")
        val euroPrimary = listOf("€") + CurrencyLocale.popupSymbols("€")
        
        assertTrue(dollarPrimary.contains("$") && dollarPrimary.contains("€"))
        assertTrue(euroPrimary.contains("$") && euroPrimary.contains("€"))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the D-92 calculator-page locale resolution: the system locale, not the active keyboard
 * alphabet, decides the currency and decimal/thousands separators (see [CalculatorLocale]'s doc comment).
 */
class CalculatorLocaleTest {
    
    @Test
    fun `Germany resolves to euro with a comma decimal separator`() {
        val format = CalculatorLocale.resolve(Locale.GERMANY)
        
        assertEquals('€', format.currencyBase)
        assertEquals(',', format.decimalSeparator)
        assertEquals(".", format.thousandsSeparatorHint)
    }
    
    @Test
    fun `Greece agrees with Germany on both points`() {
        val format = CalculatorLocale.resolve(Locale("el", "GR"))
        
        assertEquals('€', format.currencyBase)
        assertEquals(',', format.decimalSeparator)
    }
    
    @Test
    fun `a US system locale resolves to dollar with a full-stop decimal separator`() {
        val format = CalculatorLocale.resolve(Locale.US)
        
        assertEquals('$', format.currencyBase)
        assertEquals('.', format.decimalSeparator)
        assertEquals(",", format.thousandsSeparatorHint)
    }
    
    @Test
    fun `a UK system locale resolves to pound sterling`() {
        val format = CalculatorLocale.resolve(Locale.UK)
        
        assertEquals('£', format.currencyBase)
        assertEquals('.', format.decimalSeparator)
    }
    
    @Test
    fun `a Japanese system locale resolves to yen`() {
        val format = CalculatorLocale.resolve(Locale.JAPAN)
        
        // The JDK's own locale data renders this as the fullwidth yen sign (U+FFE5) for ja_JP
        // specifically, not the halfwidth "¥" (U+00A5) used in the common-currency popup list.
        assertEquals('￥', format.currencyBase)
    }
    
    @Test
    fun `the currency popup always includes the base and the common currencies`() {
        val format = CalculatorLocale.resolve(Locale.US)
        
        assertTrue(format.currencyAlternatives.contains(format.currencyBase.toString()))
        assertTrue(format.currencyAlternatives.containsAll(listOf("$", "£", "€", "¥")))
    }
    
    @Test
    fun `D-100 the popup orders the common currencies so the base tends to land near the end`() {
        val germany = CalculatorLocale.resolve(Locale.GERMANY)
        
        assertEquals(listOf("¥", "£", "$", "€"), germany.currencyAlternatives)
    }
    
    @Test
    fun `D-100 an uncommon currency is appended, not prepended`() {
        val format = CalculatorLocale.resolve(Locale.KOREA)
        
        assertEquals(listOf("¥", "£", "$", "€", "₩"), format.currencyAlternatives)
    }
    
    @Test
    fun `a locale without a resolvable currency falls back to the euro instead of crashing`() {
        val format = CalculatorLocale.resolve(Locale("xx"))
        
        assertEquals('€', format.currencyBase)
    }
}

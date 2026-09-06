// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for D-449-followup's Turkish dotted/dotless İ/I casing.
 */
class TurkishCasingRulesTest {
    
    @Test
    fun `uppercasing plain i yields dotted capital I`() {
        assertEquals("İstanbul", TurkishCasingRules.uppercaseFirst("istanbul"))
    }
    
    @Test
    fun `lowercasing plain I yields dotless small i`() {
        assertEquals("ışık", TurkishCasingRules.lowercaseFirst("Işık"))
    }
    
    @Test
    fun `uppercasing dotless small i yields plain capital I unchanged from default`() {
        assertEquals("IŞIK", TurkishCasingRules.uppercaseAll("ışık"))
    }
    
    @Test
    fun `lowercasing dotted capital I yields plain i unchanged from default`() {
        assertEquals("istanbul", TurkishCasingRules.lowercaseFirst("İstanbul"))
    }
    
    @Test
    fun `uppercaseAll dots every plain i in the word`() {
        assertEquals("İZMİR", TurkishCasingRules.uppercaseAll("izmir"))
    }
    
    @Test
    fun `non-i characters use the ordinary default mapping`() {
        assertEquals("Ankara", TurkishCasingRules.uppercaseFirst("ankara"))
        assertEquals("ankara", TurkishCasingRules.lowercaseFirst("Ankara"))
    }
    
    @Test
    fun `empty word is returned unchanged by uppercaseAll`() {
        assertEquals("", TurkishCasingRules.uppercaseAll(""))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the German umlaut / ß folding used by the fuzzy suggestion matching (D-12).
 */
class UmlautTest {
    
    @Test
    fun `umlauts fold to their base vowel`() {
        assertEquals("grun", Umlaut.fold("grün"))
        assertEquals("schon", Umlaut.fold("schön"))
        assertEquals("uber", Umlaut.fold("über"))
    }
    
    @Test
    fun `sharp s folds to double s`() {
        assertEquals("grosse", Umlaut.fold("große"))
        assertEquals("strasse", Umlaut.fold("straße"))
    }
    
    @Test
    fun `uppercase umlauts fold to the lowercase base`() {
        assertEquals("aroa", Umlaut.fold("ÄröÄ"))
    }
    
    @Test
    fun `text without umlauts is unchanged`() {
        assertEquals("default", Umlaut.fold("default"))
        assertEquals("", Umlaut.fold(""))
    }
    
    @Test
    fun `a folded diacritic-less word equals its correct form`() {
        assertEquals(Umlaut.fold("grün"), Umlaut.fold("grun"))
        assertEquals(Umlaut.fold("straße"), Umlaut.fold("strasse"))
    }
}

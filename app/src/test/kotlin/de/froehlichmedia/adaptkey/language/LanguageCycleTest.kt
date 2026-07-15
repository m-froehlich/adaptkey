// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Unit tests for the D-106 stage 1 German/English/Greek keyboard-language cycle. */
class LanguageCycleTest {
    
    @Test
    fun `next steps German to English to Greek and wraps back to German`() {
        assertEquals(Language.ENGLISH, LanguageCycle.next(Language.GERMAN))
        assertEquals(Language.GREEK, LanguageCycle.next(Language.ENGLISH))
        assertEquals(Language.GERMAN, LanguageCycle.next(Language.GREEK))
    }
    
    @Test
    fun `previous steps German to Greek to English and wraps back to German`() {
        assertEquals(Language.GREEK, LanguageCycle.previous(Language.GERMAN))
        assertEquals(Language.ENGLISH, LanguageCycle.previous(Language.GREEK))
        assertEquals(Language.GERMAN, LanguageCycle.previous(Language.ENGLISH))
    }
    
    @Test
    fun `next and previous are inverse of each other around the whole cycle`() {
        for (language in LanguageCycle.LANGUAGES) {
            assertEquals(language, LanguageCycle.previous(LanguageCycle.next(language)))
            assertEquals(language, LanguageCycle.next(LanguageCycle.previous(language)))
        }
    }
}

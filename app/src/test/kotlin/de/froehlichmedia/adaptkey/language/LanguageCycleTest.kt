// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Unit tests for the D-106 stage 1 / D-280 keyboard-language cycle. */
class LanguageCycleTest {
    
    @Test
    fun `English-only cycle never advances anywhere else`() {
        assertEquals(Language.ENGLISH, LanguageCycle.next(Language.ENGLISH, emptySet()))
        assertEquals(Language.ENGLISH, LanguageCycle.previous(Language.ENGLISH, emptySet()))
    }
    
    @Test
    fun `next steps English to German to Greek and wraps back to English`() {
        val installed = setOf(Language.GERMAN, Language.GREEK)
        assertEquals(Language.GERMAN, LanguageCycle.next(Language.ENGLISH, installed))
        assertEquals(Language.GREEK, LanguageCycle.next(Language.GERMAN, installed))
        assertEquals(Language.ENGLISH, LanguageCycle.next(Language.GREEK, installed))
    }
    
    @Test
    fun `previous steps English to Greek to German and wraps back to English`() {
        val installed = setOf(Language.GERMAN, Language.GREEK)
        assertEquals(Language.GREEK, LanguageCycle.previous(Language.ENGLISH, installed))
        assertEquals(Language.GERMAN, LanguageCycle.previous(Language.GREEK, installed))
        assertEquals(Language.ENGLISH, LanguageCycle.previous(Language.GERMAN, installed))
    }
    
    @Test
    fun `next and previous are inverse of each other around the whole cycle`() {
        val installed = setOf(Language.GERMAN, Language.GREEK)
        for (language in LanguageCycle.languages(installed)) {
            assertEquals(language, LanguageCycle.previous(LanguageCycle.next(language, installed), installed))
            assertEquals(language, LanguageCycle.next(LanguageCycle.previous(language, installed), installed))
        }
    }
    
    @Test
    fun `cycle order is deterministic regardless of install order`() {
        assertEquals(
            LanguageCycle.languages(setOf(Language.GREEK, Language.GERMAN)),
            LanguageCycle.languages(setOf(Language.GERMAN, Language.GREEK))
        )
    }
    
    @Test
    fun `a newly installed language is reachable without disturbing the existing ones`() {
        val beforeFrench = setOf(Language.GERMAN, Language.GREEK)
        val afterFrench = beforeFrench + Language.FRENCH
        assertEquals(listOf(Language.ENGLISH, Language.GERMAN, Language.GREEK), LanguageCycle.languages(beforeFrench))
        assertEquals(
            listOf(Language.ENGLISH, Language.GERMAN, Language.GREEK, Language.FRENCH),
            LanguageCycle.languages(afterFrench)
        )
    }
    
    @Test
    fun `an active language no longer in the cycle falls back to the first entry`() {
        assertEquals(Language.ENGLISH, LanguageCycle.next(Language.GERMAN, emptySet()))
        assertEquals(Language.ENGLISH, LanguageCycle.previous(Language.GERMAN, emptySet()))
    }
}

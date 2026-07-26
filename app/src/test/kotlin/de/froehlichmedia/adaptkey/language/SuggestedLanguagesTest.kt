// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/** Unit tests for the D-280 onboarding language suggestion. */
class SuggestedLanguagesTest {
    
    private val available = listOf(Language.GERMAN, Language.GREEK)
    
    @Test
    fun `a matching device locale is suggested`() {
        assertEquals(listOf(Language.GERMAN), SuggestedLanguages.from(listOf(Locale.GERMANY), available))
    }
    
    @Test
    fun `multiple device locales are suggested in preference order`() {
        val locales = listOf(Locale.forLanguageTag("el-GR"), Locale.GERMANY)
        assertEquals(listOf(Language.GREEK, Language.GERMAN), SuggestedLanguages.from(locales, available))
    }
    
    @Test
    fun `a device locale with no available pack is skipped`() {
        assertTrue(SuggestedLanguages.from(listOf(Locale.FRENCH), available).isEmpty())
    }
    
    @Test
    fun `English never appears since it is not offered as a pack`() {
        assertTrue(SuggestedLanguages.from(listOf(Locale.ENGLISH), available).isEmpty())
    }
    
    @Test
    fun `no device locales yields no suggestions`() {
        assertTrue(SuggestedLanguages.from(emptyList(), available).isEmpty())
    }
    
    @Test
    fun `the same language is never suggested twice`() {
        val locales = listOf(Locale.GERMANY, Locale.GERMANY)
        assertEquals(listOf(Language.GERMAN), SuggestedLanguages.from(locales, available))
    }
}

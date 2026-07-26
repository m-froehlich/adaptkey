// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-280 language-pack catalog data itself. */
class LanguagePackCatalogTest {
    
    @Test
    fun `the catalog is not empty and every entry has a plausible https URL`() {
        assertTrue(LanguagePackCatalog.ENTRIES.isNotEmpty())
        LanguagePackCatalog.ENTRIES.forEach { entry ->
            assertTrue(entry.downloadUrl.startsWith("https://"))
        }
    }
    
    @Test
    fun `no language is listed twice`() {
        val languages = LanguagePackCatalog.ENTRIES.map { it.language }
        assertEquals(languages.size, languages.toSet().size)
    }
    
    @Test
    fun `never lists English, which is already bundled`() {
        assertFalse(LanguagePackCatalog.ENTRIES.any { it.language == Language.ENGLISH })
    }
    
    @Test
    fun `German and Greek are both available`() {
        val languages = LanguagePackCatalog.ENTRIES.map { it.language }
        assertTrue(languages.containsAll(listOf(Language.GERMAN, Language.GREEK)))
    }
}

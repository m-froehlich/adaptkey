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
    
    @Test
    fun `D-307 every entry has a positive version`() {
        LanguagePackCatalog.ENTRIES.forEach { entry ->
            assertTrue(entry.version > 0, "${entry.language} has a non-positive version: ${entry.version}")
        }
    }
    
    // ---- D-593: the space-bar update marker predicate (the LanguagePacksActivity.buildRow() mirror) ----
    
    @Test
    fun `D-593 installed with a strictly newer catalog version marks`() {
        val entry = LanguagePackCatalog.entryFor(Language.GERMAN)!!
        assertTrue(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = true,
                installedVersion = entry.version - 1,
                suppressedCatalogVersion = 0
            )
        )
    }
    
    @Test
    fun `D-593 installed but already current does not mark`() {
        val entry = LanguagePackCatalog.entryFor(Language.GERMAN)!!
        assertFalse(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = true,
                installedVersion = entry.version,
                suppressedCatalogVersion = 0
            )
        )
        // A locally newer install (the stale-hosted-archive case from the other direction) is current too.
        assertFalse(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = true,
                installedVersion = entry.version + 1,
                suppressedCatalogVersion = 0
            )
        )
    }
    
    @Test
    fun `D-593 not installed does not mark even with a newer catalog`() {
        val entry = LanguagePackCatalog.entryFor(Language.GERMAN)!!
        assertFalse(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = false,
                installedVersion = entry.version - 1,
                suppressedCatalogVersion = 0
            )
        )
    }
    
    @Test
    fun `D-334 a suppression at or past the catalog version hides the hint, an older one does not`() {
        val entry = LanguagePackCatalog.entryFor(Language.GERMAN)!!
        assertFalse(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = true,
                installedVersion = entry.version - 1,
                suppressedCatalogVersion = entry.version
            )
        )
        assertFalse(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = true,
                installedVersion = entry.version - 1,
                suppressedCatalogVersion = entry.version + 1
            )
        )
        assertTrue(
            LanguagePackCatalog.updateAvailableFor(
                Language.GERMAN,
                isInstalled = true,
                installedVersion = entry.version - 1,
                suppressedCatalogVersion = entry.version - 1
            )
        )
    }
    
    @Test
    fun `D-593 a language with no catalog entry never marks`() {
        // English is bundled and deliberately never listed (pinned by the test above) - its space bar
        // has nothing recognisable to update, whatever the version numbers claim.
        assertFalse(
            LanguagePackCatalog.updateAvailableFor(
                Language.ENGLISH,
                isInstalled = true,
                installedVersion = 0,
                suppressedCatalogVersion = 0
            )
        )
    }
}

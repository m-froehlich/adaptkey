// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, real SharedPreferences) for [InstalledLanguagesStore], in particular the D-307
 * per-language version tracking the D-280 language-pack update mechanism relies on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstalledLanguagesStoreTest {
    
    @Test
    fun `a language not installed at all defaults to DEFAULT_VERSION`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, InstalledLanguagesStore.installedVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `add without an explicit version records DEFAULT_VERSION`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN)
        assertTrue(Language.GERMAN in InstalledLanguagesStore.load(context))
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, InstalledLanguagesStore.installedVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `add with an explicit version records exactly that version`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN, version = 2)
        assertEquals(2, InstalledLanguagesStore.installedVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `re-adding the same language with a newer version overwrites the recorded version`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN, version = 1)
        InstalledLanguagesStore.add(context, Language.GERMAN, version = 2)
        assertEquals(2, InstalledLanguagesStore.installedVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `remove clears both the installed flag and the recorded version`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN, version = 2)
        InstalledLanguagesStore.remove(context, Language.GERMAN)
        assertFalse(Language.GERMAN in InstalledLanguagesStore.load(context))
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, InstalledLanguagesStore.installedVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `each language tracks its own version independently`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN, version = 2)
        InstalledLanguagesStore.add(context, Language.GREEK, version = 1)
        assertEquals(2, InstalledLanguagesStore.installedVersion(context, Language.GERMAN))
        assertEquals(1, InstalledLanguagesStore.installedVersion(context, Language.GREEK))
    }
    
    @Test
    fun `D-334 suppressedCatalogVersion defaults to zero when nothing was suppressed`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(0, InstalledLanguagesStore.suppressedCatalogVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `D-334 suppressCatalogVersion records exactly the given catalog version`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.suppressCatalogVersion(context, Language.GERMAN, 5)
        assertEquals(5, InstalledLanguagesStore.suppressedCatalogVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `D-334 suppressCatalogVersion overwrites a previously suppressed version`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.suppressCatalogVersion(context, Language.GERMAN, 5)
        InstalledLanguagesStore.suppressCatalogVersion(context, Language.GERMAN, 6)
        assertEquals(6, InstalledLanguagesStore.suppressedCatalogVersion(context, Language.GERMAN))
    }
    
    @Test
    fun `D-334 each language tracks its own suppressed catalog version independently`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.suppressCatalogVersion(context, Language.GERMAN, 5)
        InstalledLanguagesStore.suppressCatalogVersion(context, Language.GREEK, 1)
        assertEquals(5, InstalledLanguagesStore.suppressedCatalogVersion(context, Language.GERMAN))
        assertEquals(1, InstalledLanguagesStore.suppressedCatalogVersion(context, Language.GREEK))
    }
    
    @Test
    fun `D-334 remove clears the suppressed catalog version alongside the installed version`() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN, version = 3)
        InstalledLanguagesStore.suppressCatalogVersion(context, Language.GERMAN, 5)
        
        InstalledLanguagesStore.remove(context, Language.GERMAN)
        
        assertEquals(0, InstalledLanguagesStore.suppressedCatalogVersion(context, Language.GERMAN))
    }
}

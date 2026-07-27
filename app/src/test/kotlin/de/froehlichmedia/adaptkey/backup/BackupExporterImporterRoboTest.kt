// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.backup

import de.froehlichmedia.adaptkey.credential.CredentialEntry
import de.froehlichmedia.adaptkey.credential.CredentialStore
import de.froehlichmedia.adaptkey.credential.LoginFieldKind
import de.froehlichmedia.adaptkey.dictionary.BlacklistCategory
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.PendingBlacklistEntry
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
import de.froehlichmedia.adaptkey.language.Language
import de.froehlichmedia.adaptkey.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, real SQLite/SharedPreferences) for the [BackupExporter]/[BackupImporter] round
 * trip (D-278, §21) - in particular the two direct user requirements from the design discussion: a re-import
 * merges additively rather than replacing, and a language section for a language not installed on *this*
 * device is skipped rather than crashing or silently force-installing it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupExporterImporterRoboTest {
    
    @Test
    fun reimportingTheSameBundleAddsToAlreadyLearnedDataInsteadOfReplacingIt() {
        val context = RuntimeEnvironment.getApplication()
        InstalledLanguagesStore.add(context, Language.GERMAN)
        val germanStore = SqliteDictionaryStore(context, DictionaryLoader.databaseName(Language.GERMAN))
        germanStore.learn("Nachbar", "der")
        germanStore.learn("Nachbar", "der")
        germanStore.blacklist("unwort", BlacklistCategory.USER)
        SettingsStore.prefs(context).edit().putInt(SettingsStore.KEY_MAX_SUGGESTIONS, 6).apply()
        CredentialStore.learn(context, "user@example.com", LoginFieldKind.EMAIL)
        
        val bundle = BackupExporter.export(context)
        val roundTripped = BackupJsonCodec.decode(BackupJsonCodec.encode(bundle))
        requireNotNull(roundTripped)
        val result = BackupImporter.import(context, roundTripped)
        
        assertTrue(Language.GERMAN in result.importedLanguages)
        assertTrue(result.skippedLanguages.isEmpty())
        assertFalse(result.formatTooNew)
        // Additive: the re-import doubled the frequency the export itself had already captured.
        assertEquals(4L, germanStore.learnedWords().single().frequency)
        // Blacklisting/settings/credentials are idempotent upserts, so a re-import leaves them unchanged
        // rather than duplicating - still correct, not a regression of the additive-merge requirement.
        assertEquals(listOf("unwort"), germanStore.userBlacklistedWords())
        assertEquals(6, SettingsStore.prefs(context).getInt(SettingsStore.KEY_MAX_SUGGESTIONS, -1))
        assertEquals(2L, CredentialStore.all(context).single().frequency)
    }
    
    @Test
    fun importSkipsALanguageSectionForALanguageNotInstalledOnThisDevice() {
        val context = RuntimeEnvironment.getApplication()
        // Deliberately not installed: InstalledLanguagesStore has nothing added, and Greek is never bundled.
        val bundle = BackupBundle(
            formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
            appVersionName = "0.9.2",
            exportedAtEpochMillis = 0L,
            settings = emptyMap(),
            credentials = emptyList(),
            languages = mapOf(
                Language.GREEK to LanguageSection(
                    learnedWords = emptyList(),
                    learnedBigrams = emptyList(),
                    learnedTrigrams = emptyList(),
                    blacklist = listOf("κακή"),
                    pendingBlacklist = emptyList()
                )
            )
        )
        
        val result = BackupImporter.import(context, bundle)
        
        assertEquals(listOf(Language.GREEK), result.skippedLanguages)
        assertTrue(result.importedLanguages.isEmpty())
        assertFalse(result.formatTooNew)
    }
    
    @Test
    fun exportedSettingsFollowTheSettingsScreensOwnDisplayOrderNotAnArbitraryHashOrder() {
        val context = RuntimeEnvironment.getApplication()
        // Deliberately written in reverse-of-screen-order, so a hash-based (or insertion-based) export
        // order would not accidentally happen to match the expected one below.
        val prefs = SettingsStore.prefs(context).edit()
        prefs.putBoolean(SettingsStore.KEY_KEY_HAPTICS, true)
        prefs.putInt(SettingsStore.KEY_SHIFT_EXTRA, 5)
        prefs.putInt(SettingsStore.KEY_MAX_SUGGESTIONS, 6)
        prefs.putBoolean(SettingsStore.KEY_SAVE_CREDENTIALS, false)
        prefs.apply()
        
        val bundle = BackupExporter.export(context)
        
        // D-304: KEY_SAVE_CREDENTIALS precedes KEY_MAX_SUGGESTIONS precedes KEY_SHIFT_EXTRA precedes
        // KEY_KEY_HAPTICS on the settings screen itself - the export must reproduce exactly that relative
        // order for these four keys, regardless of the order they were written above.
        val orderedKeys = bundle.settings.keys.filter {
            it in setOf(SettingsStore.KEY_SAVE_CREDENTIALS, SettingsStore.KEY_MAX_SUGGESTIONS, SettingsStore.KEY_SHIFT_EXTRA, SettingsStore.KEY_KEY_HAPTICS)
        }
        assertEquals(
            listOf(SettingsStore.KEY_SAVE_CREDENTIALS, SettingsStore.KEY_MAX_SUGGESTIONS, SettingsStore.KEY_SHIFT_EXTRA, SettingsStore.KEY_KEY_HAPTICS),
            orderedKeys
        )
    }
    
    @Test
    fun exportNeverIncludesTheDiagnosticLogToggle() {
        val context = RuntimeEnvironment.getApplication()
        SettingsStore.prefs(context).edit().putBoolean(SettingsStore.KEY_DIAGNOSTIC_LOG_ENABLED, true).apply()
        
        val bundle = BackupExporter.export(context)
        
        assertFalse(SettingsStore.KEY_DIAGNOSTIC_LOG_ENABLED in bundle.settings)
    }
    
    @Test
    fun importingABundleNeverTouchesTheTargetDevicesOwnDiagnosticLogSetting() {
        val context = RuntimeEnvironment.getApplication()
        SettingsStore.prefs(context).edit().putBoolean(SettingsStore.KEY_DIAGNOSTIC_LOG_ENABLED, true).apply()
        val bundle = BackupBundle(
            formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
            appVersionName = "0.9.16",
            exportedAtEpochMillis = 0L,
            // Simulates an old export file from before D-304 that still carried this key, or a hand-edited
            // one - even then, import must not let it override the importing device's own value.
            settings = mapOf(SettingsStore.KEY_DIAGNOSTIC_LOG_ENABLED to false),
            credentials = emptyList(),
            languages = emptyMap()
        )
        
        BackupImporter.import(context, bundle)
        
        assertTrue(SettingsStore.prefs(context).getBoolean(SettingsStore.KEY_DIAGNOSTIC_LOG_ENABLED, false))
    }
    
    @Test
    fun importRefusesABundleWithANewerFormatVersionAndAppliesNothing() {
        val context = RuntimeEnvironment.getApplication()
        SettingsStore.prefs(context).edit().putInt(SettingsStore.KEY_MAX_SUGGESTIONS, 8).apply()
        val bundle = BackupBundle(
            formatVersion = BackupBundle.CURRENT_FORMAT_VERSION + 1,
            appVersionName = "99.0",
            exportedAtEpochMillis = 0L,
            settings = mapOf<String, Any>(SettingsStore.KEY_MAX_SUGGESTIONS to 6),
            credentials = listOf(CredentialEntry("later@example.com", LoginFieldKind.EMAIL, 1L)),
            languages = emptyMap()
        )
        
        val result = BackupImporter.import(context, bundle)
        
        assertTrue(result.formatTooNew)
        assertTrue(result.importedLanguages.isEmpty())
        assertTrue(result.skippedLanguages.isEmpty())
        assertEquals(8, SettingsStore.prefs(context).getInt(SettingsStore.KEY_MAX_SUGGESTIONS, -1))
        assertTrue(CredentialStore.isEmpty(context))
    }
}

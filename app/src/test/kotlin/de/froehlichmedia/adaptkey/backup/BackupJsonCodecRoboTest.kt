// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.backup

import de.froehlichmedia.adaptkey.credential.CredentialEntry
import de.froehlichmedia.adaptkey.credential.LoginFieldKind
import de.froehlichmedia.adaptkey.dictionary.BlacklistCategory
import de.froehlichmedia.adaptkey.dictionary.BlacklistEntry
import de.froehlichmedia.adaptkey.dictionary.BlacklistOrigin
import de.froehlichmedia.adaptkey.dictionary.DictionaryAssetParser
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import de.froehlichmedia.adaptkey.dictionary.PendingBlacklistEntry
import de.froehlichmedia.adaptkey.dictionary.TrigramEntry
import de.froehlichmedia.adaptkey.dictionary.WordEntry
import de.froehlichmedia.adaptkey.language.Language
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, real `org.json`) for [BackupJsonCodec] (D-278, §21) - encode/decode is only
 * exercised under Robolectric because `org.json` itself is an Android platform class with no real
 * implementation on plain JVM (see [de.froehlichmedia.adaptkey.credential.CredentialStoreRoboTest] for the
 * same reason applied to the existing JSON-backed stores).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupJsonCodecRoboTest {
    
    private fun sampleBundle(): BackupBundle {
        return BackupBundle(
            formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
            appVersionName = "0.9.2",
            exportedAtEpochMillis = 1234567890L,
            // Int/Boolean/String only - matches every real SettingsStore key (see SettingsStore.load(),
            // which never reads a Long). Plain JSON numbers have no int/long distinction of their own -
            // org.json's own parser always decodes an integral literal to the smallest type it fits
            // (mirrored by BackupJsonCodec.decodeSettings), so a genuine Long value would not round-trip
            // losslessly here - moot in practice, since no such value is ever written.
            settings = mapOf(
                "c09_number_row" to true,
                "c03_max_suggestions" to 8,
                "c04_highlight_color" to "#FF43A047",
                "c07_shift_grace_ms" to 300
            ),
            credentials = listOf(CredentialEntry("user@example.com", LoginFieldKind.EMAIL, 3L)),
            languages = mapOf(
                Language.GERMAN to LanguageSection(
                    learnedWords = listOf(WordEntry("Nachbar", 5L, setOf(PartOfSpeech.NOUN))),
                    learnedBigrams = listOf(DictionaryAssetParser.Bigram("der", "nachbar", 5L)),
                    learnedTrigrams = listOf(TrigramEntry("ist", "der", "nachbar", 2L)),
                    blacklist = listOf(BlacklistEntry("unwort", BlacklistCategory.USER, BlacklistOrigin.GESTURE_G04, 999L)),
                    pendingBlacklist = listOf(PendingBlacklistEntry("bla", 111L))
                )
            )
        )
    }
    
    @Test
    fun encodeThenDecodeRoundTripsTheFullBundle() {
        val original = sampleBundle()
        
        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(original))
        
        assertEquals(original.formatVersion, decoded?.formatVersion)
        assertEquals(original.appVersionName, decoded?.appVersionName)
        assertEquals(original.exportedAtEpochMillis, decoded?.exportedAtEpochMillis)
        assertEquals(original.settings, decoded?.settings)
        assertEquals(original.credentials, decoded?.credentials)
        assertEquals(original.languages, decoded?.languages)
    }
    
    @Test
    fun decodeReturnsNullForGarbageInput() {
        assertNull(BackupJsonCodec.decode("this is not json"))
    }
    
    @Test
    fun decodeReturnsNullWhenARequiredFieldIsMissing() {
        assertNull(BackupJsonCodec.decode("""{"formatVersion":1}"""))
    }
    
    @Test
    fun decodeSkipsALanguageSectionWithAnUnknownLanguageCode() {
        val json = """
            {
              "formatVersion": 1,
              "appVersionName": "0.9.2",
              "exportedAtEpochMillis": 0,
              "settings": {},
              "credentials": [],
              "languages": {
                "xx": {
                  "learnedWords": [],
                  "learnedBigrams": [],
                  "learnedTrigrams": [],
                  "blacklist": [],
                  "pendingBlacklist": []
                }
              }
            }
        """.trimIndent()
        
        val decoded = BackupJsonCodec.decode(json)
        
        assertEquals(emptyMap<Language, LanguageSection>(), decoded?.languages)
    }
    
    @Test
    fun encodedSettingsPreservesAStringSetValue() {
        val bundle = sampleBundle().copy(settings = mapOf("some_key" to setOf("a", "b")))
        
        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(bundle))
        
        assertEquals(setOf("a", "b"), decoded?.settings?.get("some_key"))
    }
    
    @Test
    fun encodeWritesBlacklistProvenanceObjectsAndRoundTripsThem() {
        val json = BackupJsonCodec.encode(sampleBundle())
        
        // D-488: the v2 shape is an array of provenance objects, not plain strings.
        val section = JSONObject(json).getJSONObject("languages").getJSONObject("de")
        val blacklist = section.getJSONArray("blacklist")
        assertEquals(1, blacklist.length())
        val entry = blacklist.getJSONObject(0)
        assertEquals("unwort", entry.getString("word"))
        assertEquals("GESTURE_G04", entry.getString("origin"))
        assertEquals(999L, entry.getLong("addedAtMillis"))
        
        val decoded = BackupJsonCodec.decode(json)
        val roundTripped = decoded?.languages?.get(Language.GERMAN)?.blacklist?.single()
        assertEquals(BlacklistEntry("unwort", BlacklistCategory.USER, BlacklistOrigin.GESTURE_G04, 999L), roundTripped)
    }
    
    @Test
    fun decodeStillReadsAV1PlainStringBlacklistAsUnknownProvenance() {
        // D-488: a pre-v2 file's blacklist is a plain string array - still accepted, read as
        // UNKNOWN/0 so the importer can stamp it BACKUP_IMPORT on this device.
        val languages = JSONObject().put(
            "de",
            JSONObject()
                .put("learnedWords", JSONArray())
                .put("learnedBigrams", JSONArray())
                .put("learnedTrigrams", JSONArray())
                .put("blacklist", JSONArray().put("unwort"))
                .put("pendingBlacklist", JSONArray())
        )
        val json = JSONObject()
            .put("formatVersion", 1)
            .put("appVersionName", "1.2.70")
            .put("exportedAtEpochMillis", 0L)
            .put("settings", JSONObject())
            .put("credentials", JSONArray())
            .put("languages", languages)
            .toString()
        
        val decoded = BackupJsonCodec.decode(json)
        
        val entry = decoded?.languages?.get(Language.GERMAN)?.blacklist?.single()
        assertEquals(BlacklistEntry("unwort", BlacklistCategory.USER, BlacklistOrigin.UNKNOWN, 0L), entry)
        assertTrue(decoded?.formatVersion == 1)
    }
}

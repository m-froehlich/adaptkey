// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.backup

import de.froehlichmedia.adaptkey.credential.CredentialEntry
import de.froehlichmedia.adaptkey.credential.LoginFieldKind
import de.froehlichmedia.adaptkey.dictionary.DictionaryAssetParser
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import de.froehlichmedia.adaptkey.dictionary.PendingBlacklistEntry
import de.froehlichmedia.adaptkey.dictionary.TrigramEntry
import de.froehlichmedia.adaptkey.dictionary.WordEntry
import de.froehlichmedia.adaptkey.language.Language
import org.json.JSONArray
import org.json.JSONObject

/**
 * D-278: JSON (de)serialisation of a [BackupBundle] (§21), the one file format for the export/import
 * feature - deliberately plain `org.json` (already the established persistence format in this codebase, see
 * [de.froehlichmedia.adaptkey.credential.CredentialStore]/[de.froehlichmedia.adaptkey.touch.OffsetStore]),
 * not a new dependency. Human-readable by design, so a user can in principle inspect the exported file
 * directly.
 *
 * [decode] never throws - a corrupt or foreign file must not crash the importer, only be rejected.
 */
object BackupJsonCodec {
    
    private const val FIELD_FORMAT_VERSION = "formatVersion"
    private const val FIELD_APP_VERSION_NAME = "appVersionName"
    private const val FIELD_EXPORTED_AT = "exportedAtEpochMillis"
    private const val FIELD_SETTINGS = "settings"
    private const val FIELD_CREDENTIALS = "credentials"
    private const val FIELD_LANGUAGES = "languages"
    
    private const val FIELD_VALUE = "value"
    private const val FIELD_KIND = "kind"
    private const val FIELD_FREQUENCY = "frequency"
    
    private const val FIELD_LEARNED_WORDS = "learnedWords"
    private const val FIELD_LEARNED_BIGRAMS = "learnedBigrams"
    private const val FIELD_LEARNED_TRIGRAMS = "learnedTrigrams"
    private const val FIELD_BLACKLIST = "blacklist"
    private const val FIELD_PENDING_BLACKLIST = "pendingBlacklist"
    
    private const val FIELD_WORD = "word"
    private const val FIELD_POS = "pos"
    private const val FIELD_PREVIOUS_WORD = "previousWord"
    private const val FIELD_PREVIOUS_PREVIOUS_WORD = "previousPreviousWord"
    private const val FIELD_COUNT = "count"
    private const val FIELD_TIMESTAMP = "timestampMillis"
    
    /**
     * @param bundle the bundle to serialise
     * @return the bundle as a pretty-printed JSON string
     */
    fun encode(bundle: BackupBundle): String {
        val root = JSONObject()
        root.put(FIELD_FORMAT_VERSION, bundle.formatVersion)
        root.put(FIELD_APP_VERSION_NAME, bundle.appVersionName)
        root.put(FIELD_EXPORTED_AT, bundle.exportedAtEpochMillis)
        root.put(FIELD_SETTINGS, encodeSettings(bundle.settings))
        root.put(FIELD_CREDENTIALS, encodeCredentials(bundle.credentials))
        root.put(FIELD_LANGUAGES, encodeLanguages(bundle.languages))
        return root.toString(2)
    }
    
    /**
     * @param json the raw file content, as picked by the user
     * @return the decoded bundle, or null when [json] is not a valid AdaptKey backup file
     */
    fun decode(json: String): BackupBundle? {
        return runCatching {
            val root = JSONObject(json)
            BackupBundle(
                formatVersion = root.getInt(FIELD_FORMAT_VERSION),
                appVersionName = root.optString(FIELD_APP_VERSION_NAME, ""),
                exportedAtEpochMillis = root.optLong(FIELD_EXPORTED_AT, 0L),
                settings = decodeSettings(root.getJSONObject(FIELD_SETTINGS)),
                credentials = decodeCredentials(root.getJSONArray(FIELD_CREDENTIALS)),
                languages = decodeLanguages(root.getJSONObject(FIELD_LANGUAGES))
            )
        }.getOrNull()
    }
    
    private fun encodeSettings(settings: Map<String, Any>): JSONObject {
        val obj = JSONObject()
        settings.forEach { (key, value) ->
            when (value) {
                is Boolean, is Int, is Long, is Float, is String -> obj.put(key, value)
                is Set<*> -> obj.put(key, JSONArray(value.filterIsInstance<String>()))
                // Any other preference type is not one SettingsStore ever writes - silently skipped rather
                // than failing the whole export over a single unrecognised entry.
            }
        }
        return obj
    }
    
    private fun decodeSettings(obj: JSONObject): Map<String, Any> {
        val result = LinkedHashMap<String, Any>()
        obj.keys().forEach { key ->
            val value = obj.get(key)
            result[key] = if (value is JSONArray) {
                (0 until value.length()).map { value.getString(it) }.toSet()
            } else {
                value
            }
        }
        return result
    }
    
    private fun encodeCredentials(credentials: List<CredentialEntry>): JSONArray {
        val array = JSONArray()
        credentials.forEach { entry ->
            val obj = JSONObject()
            obj.put(FIELD_VALUE, entry.value)
            obj.put(FIELD_KIND, entry.kind.name)
            obj.put(FIELD_FREQUENCY, entry.frequency)
            array.put(obj)
        }
        return array
    }
    
    private fun decodeCredentials(array: JSONArray): List<CredentialEntry> {
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val kind = runCatching { LoginFieldKind.valueOf(obj.getString(FIELD_KIND)) }.getOrNull() ?: return@mapNotNull null
            CredentialEntry(obj.getString(FIELD_VALUE), kind, obj.getLong(FIELD_FREQUENCY))
        }
    }
    
    private fun encodeLanguages(languages: Map<Language, LanguageSection>): JSONObject {
        val obj = JSONObject()
        languages.forEach { (language, section) -> obj.put(language.code, encodeSection(section)) }
        return obj
    }
    
    private fun decodeLanguages(obj: JSONObject): Map<Language, LanguageSection> {
        val result = LinkedHashMap<Language, LanguageSection>()
        obj.keys().forEach { code ->
            val language = Language.fromCode(code) ?: return@forEach
            result[language] = decodeSection(obj.getJSONObject(code))
        }
        return result
    }
    
    private fun encodeSection(section: LanguageSection): JSONObject {
        val obj = JSONObject()
        val words = JSONArray()
        section.learnedWords.forEach { entry ->
            val wordObj = JSONObject()
            wordObj.put(FIELD_WORD, entry.word)
            wordObj.put(FIELD_FREQUENCY, entry.frequency)
            wordObj.put(FIELD_POS, JSONArray(entry.partsOfSpeech.map { it.name }))
            words.put(wordObj)
        }
        obj.put(FIELD_LEARNED_WORDS, words)
        val bigrams = JSONArray()
        section.learnedBigrams.forEach { bigram ->
            val bigramObj = JSONObject()
            bigramObj.put(FIELD_PREVIOUS_WORD, bigram.previousWord)
            bigramObj.put(FIELD_WORD, bigram.word)
            bigramObj.put(FIELD_COUNT, bigram.count)
            bigrams.put(bigramObj)
        }
        obj.put(FIELD_LEARNED_BIGRAMS, bigrams)
        val trigrams = JSONArray()
        section.learnedTrigrams.forEach { trigram ->
            val trigramObj = JSONObject()
            trigramObj.put(FIELD_PREVIOUS_PREVIOUS_WORD, trigram.previousPreviousWord)
            trigramObj.put(FIELD_PREVIOUS_WORD, trigram.previousWord)
            trigramObj.put(FIELD_WORD, trigram.word)
            trigramObj.put(FIELD_COUNT, trigram.count)
            trigrams.put(trigramObj)
        }
        obj.put(FIELD_LEARNED_TRIGRAMS, trigrams)
        obj.put(FIELD_BLACKLIST, JSONArray(section.blacklist))
        val pending = JSONArray()
        section.pendingBlacklist.forEach { entry ->
            val pendingObj = JSONObject()
            pendingObj.put(FIELD_WORD, entry.word)
            pendingObj.put(FIELD_TIMESTAMP, entry.timestampMillis)
            pending.put(pendingObj)
        }
        obj.put(FIELD_PENDING_BLACKLIST, pending)
        return obj
    }
    
    private fun decodeSection(obj: JSONObject): LanguageSection {
        val words = obj.getJSONArray(FIELD_LEARNED_WORDS)
        val learnedWords = (0 until words.length()).map { i ->
            val wordObj = words.getJSONObject(i)
            val posArray = wordObj.getJSONArray(FIELD_POS)
            val pos = (0 until posArray.length())
                .mapNotNull { runCatching { PartOfSpeech.valueOf(posArray.getString(it)) }.getOrNull() }
                .toSet()
            WordEntry(wordObj.getString(FIELD_WORD), wordObj.getLong(FIELD_FREQUENCY), pos)
        }
        val bigrams = obj.getJSONArray(FIELD_LEARNED_BIGRAMS)
        val learnedBigrams = (0 until bigrams.length()).map { i ->
            val bigramObj = bigrams.getJSONObject(i)
            DictionaryAssetParser.Bigram(
                bigramObj.getString(FIELD_PREVIOUS_WORD), bigramObj.getString(FIELD_WORD), bigramObj.getLong(FIELD_COUNT)
            )
        }
        val trigrams = obj.getJSONArray(FIELD_LEARNED_TRIGRAMS)
        val learnedTrigrams = (0 until trigrams.length()).map { i ->
            val trigramObj = trigrams.getJSONObject(i)
            TrigramEntry(
                trigramObj.getString(FIELD_PREVIOUS_PREVIOUS_WORD),
                trigramObj.getString(FIELD_PREVIOUS_WORD),
                trigramObj.getString(FIELD_WORD),
                trigramObj.getLong(FIELD_COUNT)
            )
        }
        val blacklistArray = obj.getJSONArray(FIELD_BLACKLIST)
        val blacklist = (0 until blacklistArray.length()).map { blacklistArray.getString(it) }
        val pendingArray = obj.getJSONArray(FIELD_PENDING_BLACKLIST)
        val pendingBlacklist = (0 until pendingArray.length()).map { i ->
            val pendingObj = pendingArray.getJSONObject(i)
            PendingBlacklistEntry(pendingObj.getString(FIELD_WORD), pendingObj.getLong(FIELD_TIMESTAMP))
        }
        return LanguageSection(learnedWords, learnedBigrams, learnedTrigrams, blacklist, pendingBlacklist)
    }
}

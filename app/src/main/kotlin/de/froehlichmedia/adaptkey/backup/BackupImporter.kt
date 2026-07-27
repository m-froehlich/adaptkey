// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.backup

import android.content.Context
import de.froehlichmedia.adaptkey.credential.CredentialStore
import de.froehlichmedia.adaptkey.dictionary.BlacklistCategory
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.language.Language
import de.froehlichmedia.adaptkey.settings.SettingsStore

/**
 * D-278: applies a decoded [BackupBundle] to this device (§21), the Android-facing counterpart to the pure
 * [BackupJsonCodec].
 *
 * Every merge is additive/non-destructive, never a wholesale replace - settings are the one exception, since
 * a single-valued preference has no meaningful "merge" (the imported value simply wins, exactly as if the
 * user had changed it themselves). Direct user requirement: a language section whose language is not
 * currently installed on *this* device must never be force-applied (there is no store to write it into, and
 * creating one would silently resurrect a language the user deliberately removed) - such a section is
 * skipped outright and reported back via [Result.skippedLanguages], never discarded silently.
 */
object BackupImporter {
    
    /**
     * @property importedLanguages every language section that was actually applied
     * @property skippedLanguages every language section that was skipped because that language is not
     *           currently installed on this device
     * @property formatTooNew true when [BackupBundle.formatVersion] is newer than this build understands -
     *           nothing is applied at all in that case
     */
    data class Result(
        val importedLanguages: List<Language>,
        val skippedLanguages: List<Language>,
        val formatTooNew: Boolean
    )
    
    /**
     * @param context any valid context (the input method service, or a settings [android.app.Activity])
     * @param bundle the decoded bundle to apply
     * @return a summary of what was actually applied
     */
    fun import(context: Context, bundle: BackupBundle): Result {
        if (bundle.formatVersion > BackupBundle.CURRENT_FORMAT_VERSION) {
            return Result(emptyList(), emptyList(), formatTooNew = true)
        }
        importSettings(context, bundle.settings)
        CredentialStore.restoreAll(context, bundle.credentials)
        val installed = DictionaryLoader.activeLanguages(context).toSet()
        val imported = ArrayList<Language>()
        val skipped = ArrayList<Language>()
        bundle.languages.forEach { (language, section) ->
            if (language !in installed) {
                skipped.add(language)
                return@forEach
            }
            importLanguageSection(context, language, section)
            imported.add(language)
        }
        return Result(imported, skipped, formatTooNew = false)
    }
    
    private fun importLanguageSection(context: Context, language: Language, section: LanguageSection) {
        val store = SqliteDictionaryStore(context, DictionaryLoader.databaseName(language))
        section.learnedWords.forEach { entry ->
            store.restoreLearnedWord(entry.word, entry.frequency, entry.partsOfSpeech)
        }
        section.learnedBigrams.forEach { bigram ->
            store.restoreLearnedBigram(bigram.previousWord, bigram.word, bigram.count)
        }
        section.learnedTrigrams.forEach { trigram ->
            store.restoreLearnedTrigram(trigram.previousPreviousWord, trigram.previousWord, trigram.word, trigram.count)
        }
        section.blacklist.forEach { word -> store.blacklist(word, BlacklistCategory.USER) }
        // Never overwrites an already-pending local mark's own timestamp (G-04's expiry window is measured
        // from it) - only a word with no local mark at all adopts the imported one.
        section.pendingBlacklist.forEach { entry ->
            if (store.pendingBlacklistedSince(entry.word) == null) {
                store.markPendingBlacklist(entry.word, entry.timestampMillis)
            }
        }
    }
    
    private fun importSettings(context: Context, settings: Map<String, Any>) {
        val editor = SettingsStore.prefs(context).edit()
        settings.forEach { (key, value) ->
            // D-304: never applied even if present - a pre-D-304 export, or a hand-edited file, could still
            // carry one of these keys; see SettingsStore.EXPORT_EXCLUDED_KEYS's own KDoc for why each one is
            // excluded from export in the first place.
            if (key in SettingsStore.EXPORT_EXCLUDED_KEYS) {
                return@forEach
            }
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.apply()
    }
}

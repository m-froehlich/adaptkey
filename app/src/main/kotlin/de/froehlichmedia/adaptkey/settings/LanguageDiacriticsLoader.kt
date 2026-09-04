// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage
import de.froehlichmedia.adaptkey.language.Language
import de.froehlichmedia.adaptkey.suggestion.DiacriticTable

/**
 * D-436: reads a language's own base-letter -> diacritic-variants table - `diacritics.tsv` bundled inside
 * the APK (under `assets/<code>/`) for [DictionaryLoader.BUNDLED_LANGUAGES] (English), or
 * [LanguagePackStorage]'s installed copy for every other language - mirroring exactly
 * [LanguageAbbreviationsLoader]'s own "bundled asset or installed pack, same fixed filename either way"
 * mechanism. Optional: a language without a diacritics file of its own simply has none here, and the caller
 * falls back to [de.froehlichmedia.adaptkey.suggestion.NoOpDiacriticFolding] (or, for German, the compiled-in
 * [de.froehlichmedia.adaptkey.suggestion.Umlaut] - see [SettingsStore.loadDiacriticFolding] for the full
 * precedence).
 */
object LanguageDiacriticsLoader {
    
    private fun assetName(language: Language) = "${language.code}/diacritics.tsv"
    
    /**
     * @param context any valid context
     * @param language the language whose own diacritics table to load
     * @return the parsed base-letter -> variants map, or null when no diacritics file exists (or is
     *         unreadable, or is empty once parsed) for [language]
     */
    fun loadFor(context: Context, language: Language): Map<Char, List<Char>>? {
        val raw = if (language in DictionaryLoader.BUNDLED_LANGUAGES) {
            readAsset(context, assetName(language))
        } else {
            LanguagePackStorage.readDiacritics(context, language)
        }
        val parsed = raw?.let { DiacriticTable.parse(it) }
        return parsed?.takeIf { it.isNotEmpty() }
    }
    
    private fun readAsset(context: Context, name: String): String? {
        return runCatching {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import de.froehlichmedia.adaptkey.capitalisation.Abbreviations
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage
import de.froehlichmedia.adaptkey.language.Language

/**
 * D-434: reads a language's own §6 sentence-boundary abbreviation list - `abbreviations.tsv` bundled inside
 * the APK (under `assets/<code>/`) for [DictionaryLoader.BUNDLED_LANGUAGES] (English), or
 * [LanguagePackStorage]'s installed copy for every other language - mirroring exactly
 * [LanguageLetterHintsLoader]'s own "bundled asset or installed pack, same fixed filename either way"
 * mechanism for `hints.tsv`. Optional: a language without an abbreviation file of its own simply has none
 * here, and the caller falls back to [Abbreviations.GERMAN] - the same "functional but not tailored"
 * fallback role [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout.DEFAULT_LETTER_HINTS] already plays for
 * L-05/D-281.
 *
 * The file content is [Abbreviations]'s own persisted format (one lower-cased abbreviation per line) -
 * deliberately reusing [Abbreviations.parse] rather than inventing a second format.
 */
object LanguageAbbreviationsLoader {
    
    private fun assetName(language: Language) = "${language.code}/abbreviations.tsv"
    
    /**
     * @param context any valid context
     * @param language the language whose own abbreviation list to load
     * @return the parsed abbreviation set, or null when no abbreviations file exists (or is unreadable) for
     *         [language]
     */
    fun loadFor(context: Context, language: Language): Set<String>? {
        val raw = if (language in DictionaryLoader.BUNDLED_LANGUAGES) {
            readAsset(context, assetName(language))
        } else {
            LanguagePackStorage.readAbbreviations(context, language)
        }
        val parsed = raw?.let { Abbreviations.parse(it) }
        return parsed?.takeIf { it.isNotEmpty() }
    }
    
    private fun readAsset(context: Context, name: String): String? {
        return runCatching {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}

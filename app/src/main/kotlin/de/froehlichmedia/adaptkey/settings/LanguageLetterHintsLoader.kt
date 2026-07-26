// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage
import de.froehlichmedia.adaptkey.language.Language

/**
 * D-281: reads a language's own default L-05/C-08 letter-hint set - `hints_<code>.tsv` bundled inside the
 * APK for [DictionaryLoader.BUNDLED_LANGUAGES] (English), or [LanguagePackStorage]'s installed copy for
 * every other language - mirroring exactly how [DictionaryLoader] itself sources a language's dictionary
 * from one place or the other. Optional: a language pack without a hints file (or Greek, which never had
 * Latin AltGr hints to begin with) simply has none here, and the caller falls back to
 * [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout.DEFAULT_LETTER_HINTS].
 *
 * The file content is exactly [LetterHints.encode]'s own format (`"a=ä;e=€;..."`) - deliberately reusing
 * the already-validated, already-tested encode/parse/sanitize core rather than inventing a second format.
 */
object LanguageLetterHintsLoader {
    
    private fun assetName(language: Language) = "hints_${language.code}.tsv"
    
    /**
     * @param context any valid context
     * @param language the language whose own default hint set to load
     * @return the validated per-key map, or null when no hints file exists (or is unreadable) for
     *         [language]
     */
    fun loadFor(context: Context, language: Language): Map<Char, String>? {
        val raw = if (language in DictionaryLoader.BUNDLED_LANGUAGES) {
            readAsset(context, assetName(language))
        } else {
            LanguagePackStorage.readHints(context, language)
        }
        val parsed = raw?.let { LetterHints.parse(it.trim()) }
        return parsed?.takeIf { it.isNotEmpty() }
    }
    
    private fun readAsset(context: Context, name: String): String? {
        return runCatching {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}

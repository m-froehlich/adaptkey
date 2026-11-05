// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage
import de.froehlichmedia.adaptkey.language.Language

/**
 * D-281: reads a language's own default L-05/C-08 letter-hint set - `hints.tsv` bundled inside the APK
 * (under `assets/<code>/`, D-310) for [DictionaryLoader.BUNDLED_LANGUAGES] (English), or
 * [LanguagePackStorage]'s installed copy for every other language - mirroring exactly how [DictionaryLoader]
 * itself sources a language's dictionary from one place or the other. Optional: a language pack without a
 * hints file (or Greek, which never had Latin AltGr hints to begin with) simply has none here.
 *
 * D-513: the file is a hints DELTA against the compiled-in universal base (the Alt-Belegungs-Reform's
 * union merge), read via [loadDeltaFor]; [loadFor] keeps the pre-reform whole-map read for the
 * non-Latin-layout languages that keep the replace semantics (§2.12).
 *
 * The file content is [LetterHints]'s own persisted format (`"a=ä;e=€;..."`, plus the D-513 empty-value
 * removal marker) - deliberately reusing the already-validated, already-tested parse/sanitize core rather
 * than inventing a second format.
 */
object LanguageLetterHintsLoader {
    
    private fun assetName(language: Language) = "${language.code}/hints.tsv"
    
    /**
     * @param context any valid context
     * @param language the language whose own default hint set to load
     * @return the validated per-key map, or null when no hints file exists (or is unreadable) for
     *         [language]
     */
    fun loadFor(context: Context, language: Language): Map<Char, String>? {
        val raw = readRaw(context, language) ?: return null
        return LetterHints.parse(raw.trim()).takeIf { it.isNotEmpty() }
    }
    
    /**
     * D-513: reads [language]'s own hints DELTA - the same file [loadFor] reads, parsed with removals
     * intact ([LetterHints.parseDelta]). A missing or unreadable file is an EMPTY delta (the compiled-in
     * base stands untouched for every key), deliberately NOT a null-with-fallback: a delta file that
     * carries only removals (English's own `a=;o=;u=;s=`) must never collapse into "no data", or the
     * §2.10 umlaut policy would silently lose its one mechanism.
     *
     * @param context any valid context
     * @param language the language whose own delta to load
     * @return the validated per-key delta (variant strings, null = explicit removal); possibly empty
     */
    fun loadDeltaFor(context: Context, language: Language): Map<Char, String?> {
        val raw = readRaw(context, language) ?: return emptyMap()
        return LetterHints.parseDelta(raw.trim())
    }
    
    private fun readRaw(context: Context, language: Language): String? {
        return if (language in DictionaryLoader.BUNDLED_LANGUAGES) {
            readAsset(context, assetName(language))
        } else {
            LanguagePackStorage.readHints(context, language)
        }
    }
    
    private fun readAsset(context: Context, name: String): String? {
        return runCatching {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}

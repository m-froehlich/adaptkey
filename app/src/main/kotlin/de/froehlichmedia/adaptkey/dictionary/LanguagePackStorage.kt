// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import android.content.Context
import de.froehlichmedia.adaptkey.language.Language
import java.io.File

/**
 * D-280: resolves the app-private directory that holds a user-installed language pack (an unigram + a
 * bigram TSV file per language, downloaded via the browser and imported through
 * [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]), mirroring
 * [de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelStorage]'s own "single source of truth shared by
 * the import screen and the reader" reasoning.
 *
 * D-310: each language gets its own subfolder (`<packDir>/<code>/`) holding fixed-name files (`dict.tsv`,
 * `bigram.tsv`, `hints.tsv`) - before this, all languages' files sat flat in one shared [packDir], each
 * disambiguated only by a `_<code>` filename suffix. The suffix is gone now that the folder itself already
 * scopes every file to one language, and the same fixed names are used for [DictionaryLoader]'s bundled-
 * English assets too (`assets/<code>/`), so there is exactly one naming convention across both sources.
 *
 * Lives under the app's internal `filesDir`, so it needs no storage permission and is removed on uninstall.
 * [DictionaryLoader] reads from here for every language outside its own [DictionaryLoader.BUNDLED_LANGUAGES].
 */
object LanguagePackStorage {
    
    private const val DIR_NAME = "language_packs"
    
    /**
     * @param context any valid context
     * @return the private language-pack directory (not guaranteed to exist yet)
     */
    fun packDir(context: Context): File = File(context.filesDir, DIR_NAME)
    
    /** @return [language]'s own subfolder under [packDir] (not guaranteed to exist yet) */
    fun languageDir(context: Context, language: Language): File = File(packDir(context), language.code)
    
    /** @return the installed unigram file for [language], whether or not it actually exists yet */
    fun wordsFile(context: Context, language: Language): File = File(languageDir(context, language), "dict.tsv")
    
    /** @return the installed bigram file for [language], whether or not it actually exists yet */
    fun bigramsFile(context: Context, language: Language): File = File(languageDir(context, language), "bigram.tsv")
    
    /**
     * D-281: the installed letter-hint default (L-05/C-08's per-language AltGr set) for [language], whether
     * or not it actually exists yet - optional, unlike the unigram file, since a language pack without one
     * simply falls back to [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout.DEFAULT_LETTER_HINTS].
     */
    fun hintsFile(context: Context, language: Language): File = File(languageDir(context, language), "hints.tsv")
    
    /**
     * D-434: the installed abbreviation-list default (§6's sentence-boundary detection) for [language],
     * whether or not it actually exists yet - optional, like [hintsFile]: a language pack without one falls
     * back to [de.froehlichmedia.adaptkey.capitalisation.Abbreviations.GERMAN].
     */
    fun abbreviationsFile(context: Context, language: Language): File =
        File(languageDir(context, language), "abbreviations.tsv")
    
    /**
     * D-436: the installed base-letter -> diacritic-variants table (D-387/D-435's [DataDiacriticFolding])
     * for [language], whether or not it actually exists yet - optional, like [hintsFile]: a language pack
     * without one simply falls back to [de.froehlichmedia.adaptkey.suggestion.NoOpDiacriticFolding].
     */
    fun diacriticsFile(context: Context, language: Language): File =
        File(languageDir(context, language), "diacritics.tsv")
    
    /**
     * @param context any valid context
     * @param language the language to check
     * @return true when a language pack is installed for [language] (its unigram file is present - a
     *         bigram file is optional, see [DictionaryLoader]'s own tolerance for a missing bigram source)
     */
    fun isInstalled(context: Context, language: Language): Boolean = wordsFile(context, language).isFile
    
    /** @return the installed unigram file's content, or null when not installed / unreadable */
    fun readWords(context: Context, language: Language): String? {
        return runCatching { wordsFile(context, language).readText(Charsets.UTF_8) }.getOrNull()
    }
    
    /** @return the installed bigram file's content, or null when not installed / unreadable */
    fun readBigrams(context: Context, language: Language): String? {
        return runCatching { bigramsFile(context, language).readText(Charsets.UTF_8) }.getOrNull()
    }
    
    /** @return the installed letter-hint file's content, or null when not installed / unreadable */
    fun readHints(context: Context, language: Language): String? {
        return runCatching { hintsFile(context, language).readText(Charsets.UTF_8) }.getOrNull()
    }
    
    /** @return the installed abbreviation-list file's content, or null when not installed / unreadable */
    fun readAbbreviations(context: Context, language: Language): String? {
        return runCatching { abbreviationsFile(context, language).readText(Charsets.UTF_8) }.getOrNull()
    }
    
    /** @return the installed diacritics-table file's content, or null when not installed / unreadable */
    fun readDiacritics(context: Context, language: Language): String? {
        return runCatching { diacriticsFile(context, language).readText(Charsets.UTF_8) }.getOrNull()
    }
    
    /**
     * Removes [language]'s installed pack files (not the dictionary database itself - the caller is
     * responsible for also deleting that, see [DictionaryLoader.databaseName]).
     *
     * @param context any valid context
     * @param language the language to remove
     */
    fun remove(context: Context, language: Language) {
        wordsFile(context, language).delete()
        bigramsFile(context, language).delete()
        hintsFile(context, language).delete()
        abbreviationsFile(context, language).delete()
        diacriticsFile(context, language).delete()
        // Best-effort: File.delete() on a directory only succeeds when it is already empty, exactly the
        // state the deletes above just left it in (barring a stray unrelated file, harmless either way).
        languageDir(context, language).delete()
    }
}

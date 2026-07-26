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
    
    /** @return the installed unigram file for [language], whether or not it actually exists yet */
    fun wordsFile(context: Context, language: Language): File = File(packDir(context), "dict_${language.code}.tsv")
    
    /** @return the installed bigram file for [language], whether or not it actually exists yet */
    fun bigramsFile(context: Context, language: Language): File = File(packDir(context), "bigram_${language.code}.tsv")
    
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
    }
}

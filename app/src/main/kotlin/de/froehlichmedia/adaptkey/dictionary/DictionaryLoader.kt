// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import android.content.Context
import de.froehlichmedia.adaptkey.language.Language

/**
 * Builds the per-language dictionary stores and, on first run, seeds each from its bundled asset
 * (real Wikipedia-derived lexicons: `dict_<code>.tsv` unigrams + `bigram_<code>.tsv` bigrams).
 *
 * One [SqliteDictionaryStore] per language, keyed by [Language]; the store keeps the same schema, so
 * all ranking / capitalisation logic is reused unchanged per language. Android-only glue; the parsing
 * itself is the pure, unit-tested [DictionaryAssetParser]. If a language's asset is missing the store
 * is left empty (and German additionally falls back to the small [SeedData] so it is never empty).
 */
object DictionaryLoader {
    
    /** The languages that ship with a bundled dictionary (German default, English auto, Greek via G-01). */
    val LANGUAGES = listOf(Language.GERMAN, Language.ENGLISH, Language.GREEK)
    
    private fun databaseName(language: Language): String = "adaptkey_dictionary_${language.code}.db"
    
    private fun wordsAsset(language: Language): String = "dict_${language.code}.tsv"
    
    private fun bigramsAsset(language: Language): String = "bigram_${language.code}.tsv"
    
    /**
     * @param context any valid context (the input method service)
     * @return a store per supported language, each seeded from its asset when first created
     */
    fun loadStores(context: Context): Map<Language, SqliteDictionaryStore> {
        return LANGUAGES.associateWith { language ->
            val store = SqliteDictionaryStore(context, databaseName(language))
            if (store.isEmpty()) {
                seed(context, language, store)
            }
            store
        }
    }
    
    private fun seed(context: Context, language: Language, store: SqliteDictionaryStore) {
        val words = readAsset(context, wordsAsset(language))?.let { DictionaryAssetParser.parseWords(it) } ?: emptyList()
        val bigrams = readAsset(context, bigramsAsset(language))?.let { DictionaryAssetParser.parseBigrams(it) } ?: emptyList()
        if (words.isNotEmpty()) {
            store.bulkImport(words, bigrams)
        } else if (language == Language.GERMAN) {
            // Never leave German empty, even if the asset is somehow unavailable.
            SeedData.seed(store)
        }
    }
    
    private fun readAsset(context: Context, name: String): String? {
        return runCatching {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}

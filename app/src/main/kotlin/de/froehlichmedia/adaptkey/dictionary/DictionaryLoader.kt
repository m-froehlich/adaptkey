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
 *
 * D-178: [BUNDLED_DICTIONARY_VERSION] tracks the bundled asset content, separately from the store's own
 * schema ({@code SqliteDictionaryStore}'s {@code DATABASE_VERSION}). Bumping it forces exactly one
 * [SqliteDictionaryStore.resetBundledWords] + reseed per store on the next load, without touching the
 * learned overlay, the blacklist, or the pending-blacklist marks - this release bumps it for the first
 * time, to flush out words (like "aks", D-172/D-177) that a pre-D-177 build had learned straight into the
 * bundled table, back before [SqliteDictionaryStore.learn] had a separate table of its own to write to,
 * and that would otherwise sit there forever, indistinguishable from a real dictionary entry.
 */
object DictionaryLoader {
    
    /** The languages that ship with a bundled dictionary (German default, English auto, Greek via G-01). */
    val LANGUAGES = listOf(Language.GERMAN, Language.ENGLISH, Language.GREEK)
    
    /**
     * D-178: bump to force a one-time reseed of the bundled tables on every existing install's next load.
     * D-232: bumped again - German gained 24 missing irregular/ablaut-verb conjugated forms (D-230's
     * "darfst"-class split regression) plus 14 IT/programming terms; English gained 5 IT/programming terms
     * German was missing (D-228's "Docker"-class regression, plus terms the user expects to type often).
     */
    private const val BUNDLED_DICTIONARY_VERSION = 2
    
    /**
     * D-186: bump to force a one-time [SqliteDictionaryStore.purgeBundledDuplicatesFromLearned] on every
     * existing install's next load - flushes bundled words ("die", "du", "immer", ...) that a pre-D-186
     * build wrote into the learned overlay on every single reinforcement, flooding the Learned Words
     * editor with plain vocabulary instead of just what was actually taught.
     */
    private const val LEARNED_CLEANUP_VERSION = 1
    
    /**
     * The SQLite database file backing [language]'s dictionary. Public so the C-05 blacklist editor can
     * open the very store the running keyboard uses for that language.
     *
     * @param language the dictionary language
     * @return the per-language database file name
     */
    fun databaseName(language: Language): String = "adaptkey_dictionary_${language.code}.db"
    
    private fun wordsAsset(language: Language): String = "dict_${language.code}.tsv"
    
    private fun bigramsAsset(language: Language): String = "bigram_${language.code}.tsv"
    
    /**
     * @param context any valid context (the input method service)
     * @return a store per supported language, each seeded from its asset when first created, and reseeded
     * exactly once whenever [BUNDLED_DICTIONARY_VERSION] is bumped past what the store already holds
     */
    fun loadStores(context: Context): Map<Language, SqliteDictionaryStore> {
        return LANGUAGES.associateWith { language ->
            val store = SqliteDictionaryStore(context, databaseName(language))
            if (store.isEmpty()) {
                seed(context, language, store)
                store.setBundledContentVersion(BUNDLED_DICTIONARY_VERSION)
            } else if (store.bundledContentVersion() < BUNDLED_DICTIONARY_VERSION) {
                store.resetBundledWords()
                seed(context, language, store)
                store.setBundledContentVersion(BUNDLED_DICTIONARY_VERSION)
            }
            if (store.learnedCleanupVersion() < LEARNED_CLEANUP_VERSION) {
                store.purgeBundledDuplicatesFromLearned()
                store.setLearnedCleanupVersion(LEARNED_CLEANUP_VERSION)
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

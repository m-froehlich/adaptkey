// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import android.content.Context
import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
import de.froehlichmedia.adaptkey.language.Language

/**
 * Builds the per-language dictionary stores: [BUNDLED_LANGUAGES] are seeded on first run from their asset
 * bundled inside the APK itself; every other, D-280-installed language is seeded from
 * [LanguagePackStorage]'s copy on private storage instead (both are real Wikipedia-derived lexicons of the
 * same shape - a per-language folder holding `dict.tsv` unigrams + `bigram.tsv` bigrams, D-310 - just from a
 * different source: `app/src/main/assets/<code>/` here, [LanguagePackStorage]'s own per-device directory
 * there).
 *
 * One [SqliteDictionaryStore] per language, keyed by [Language]; the store keeps the same schema, so
 * all ranking / capitalisation logic is reused unchanged per language. Android-only glue; the parsing
 * itself is the pure, unit-tested [DictionaryAssetParser]. If a language's source is missing the store is
 * simply left empty.
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
    
    /**
     * D-280: the only language whose dictionary ships inside the APK itself - every other language (German
     * and Greek included, both bundled through D-279) is now an optional, user-installed language pack (see
     * the language-contribution guide), kept out of the APK to keep its size down for users who only need
     * one or two languages. [loadStores] always includes this language, so onboarding/G-01 never start with
     * a genuinely empty dictionary even before the user installs anything.
     */
    val BUNDLED_LANGUAGES = listOf(Language.ENGLISH)
    
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
    
    // D-310: a per-language subfolder with fixed filenames, mirroring LanguagePackStorage's own layout for
    // an installed pack - see that object's own KDoc for why (a single shared naming/pathing convention
    // regardless of whether a language is bundled or downloaded).
    private fun wordsAsset(language: Language): String = "${language.code}/dict.tsv"
    
    private fun bigramsAsset(language: Language): String = "${language.code}/bigram.tsv"
    
    /**
     * @param context any valid context
     * @return [BUNDLED_LANGUAGES] plus every currently installed language ([InstalledLanguagesStore]), in
     *         that order - the set [loadStores] builds a real store for, and the set
     *         [de.froehlichmedia.adaptkey.AdaptKeyService] seeds its instant bootstrap in-memory stores
     *         from before the real load finishes
     */
    fun activeLanguages(context: Context): List<Language> {
        return (BUNDLED_LANGUAGES + InstalledLanguagesStore.load(context)).distinct()
    }
    
    /**
     * @param context any valid context (the input method service)
     * @return a store for each of [activeLanguages], each seeded from its source when first created; a
     *         bundled language is additionally reseeded whenever [BUNDLED_DICTIONARY_VERSION] is bumped
     *         past what the store already holds - an installed language's own content only ever changes
     *         through a fresh install (see [LanguagePackInstaller]), so that reseed does not apply to it
     */
    fun loadStores(context: Context): Map<Language, SqliteDictionaryStore> {
        return activeLanguages(context).associateWith { language ->
            val store = SqliteDictionaryStore(context, databaseName(language))
            if (store.isEmpty()) {
                seed(context, language, store)
                store.setBundledContentVersion(BUNDLED_DICTIONARY_VERSION)
            } else if (language in BUNDLED_LANGUAGES && store.bundledContentVersion() < BUNDLED_DICTIONARY_VERSION) {
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
        val wordsText: String?
        val bigramsText: String?
        if (language in BUNDLED_LANGUAGES) {
            wordsText = readAsset(context, wordsAsset(language))
            bigramsText = readAsset(context, bigramsAsset(language))
        } else {
            wordsText = LanguagePackStorage.readWords(context, language)
            bigramsText = LanguagePackStorage.readBigrams(context, language)
        }
        val words = wordsText?.let { DictionaryAssetParser.parseWords(it) } ?: emptyList()
        val bigrams = bigramsText?.let { DictionaryAssetParser.parseBigrams(it) } ?: emptyList()
        if (words.isNotEmpty()) {
            store.bulkImport(words, bigrams)
        }
    }
    
    private fun readAsset(context: Context, name: String): String? {
        return runCatching {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}

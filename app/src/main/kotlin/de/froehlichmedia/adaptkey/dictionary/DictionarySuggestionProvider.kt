// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.suggestion.EditDistance
import de.froehlichmedia.adaptkey.suggestion.Suggestion
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider
import de.froehlichmedia.adaptkey.suggestion.Umlaut

/**
 * Tier-1 suggestion provider over a {@link DictionaryStore}: personal n-gram completion with a
 * bigram context bonus, the blacklist (A-04) and the "valid words are not overwritten" rule (A-01).
 *
 * Depends only on the {@link DictionaryStore} abstraction, so the ranking and policy are unit-tested
 * with the in-memory store; the SQLite store supplies the same behaviour on device.
 *
 * @property store the backing dictionary store
 * @property maxCandidates the maximum number of suggestions returned
 */
class DictionarySuggestionProvider(
    private val store: DictionaryStore,
    private val maxCandidates: Int = 12
) : SuggestionProvider {
    
    override fun suggestionsFor(input: String, previousWord: String?): List<Suggestion> {
        val token = input.lowercase()
        // Keyed by canonical word so a word is never offered twice; insertion order is irrelevant since
        // the merged set is re-sorted by score before it is capped.
        val candidates = LinkedHashMap<String, Suggestion>()
        // Prefix completion, ranked by frequency + bigram context (shown from the very first letter, D-11).
        for (entry in store.unigramsByPrefix(token, maxCandidates * SCAN_FACTOR)) {
            if (store.isBlacklisted(entry.word)) {
                continue // A-04
            }
            candidates[entry.word] = Suggestion(entry.word, score(entry.word, entry.frequency, previousWord))
        }
        // D-12: also offer close real words - a single edit or an umlaut/ß variant - so a mistype or a
        // valid-but-wrong word still surfaces the intended one ("mut" -> "mit", "grun" -> "grün").
        for (word in fuzzyNeighbours(token)) {
            if (candidates.containsKey(word) || store.isBlacklisted(word)) {
                continue // A-04
            }
            candidates[word] = Suggestion(word, score(word, store.frequencyOf(word), previousWord))
        }
        return candidates.values
            .sortedByDescending { it.score }
            .take(maxCandidates)
    }
    
    /**
     * Close real-word neighbours of [token] for the suggestion bar (D-12): candidates within one edit of
     * the token once German umlauts / ß are folded on both sides, so a diacritic-less typing matches its
     * correct form. The token itself is excluded (S-02 handles the verbatim case). Uses the same bounded,
     * indexed candidate set as the autocorrect, so it stays cheap per keystroke.
     *
     * @param token the lower-cased composing token
     * @return the neighbouring known words in canonical case
     */
    private fun fuzzyNeighbours(token: String): List<String> {
        if (token.length < MIN_FUZZY_LENGTH) {
            return emptyList()
        }
        val folded = Umlaut.fold(token)
        return store.correctionCandidates(token).filter { candidate ->
            val lower = candidate.lowercase()
            lower != token && EditDistance.atMostOne(folded, Umlaut.fold(lower))
        }
    }
    
    override fun isKnownWord(word: String): Boolean {
        return store.isKnownWord(word) && !store.isBlacklisted(word) // A-04
    }
    
    override fun autocorrectFor(input: String, previousWord: String?): String? {
        val token = input.lowercase()
        if (token.length < MIN_AUTOCORRECT_LENGTH) {
            return null
        }
        // A-01: a valid word is never overwritten.
        if (isKnownWord(token)) {
            return null
        }
        // Only a bounded candidate set is scanned (not the whole lexicon). The cheap pure edit-distance
        // test runs before the per-candidate blacklist query, so the DB is touched only for real matches.
        // D-12: umlauts / ß are folded on both sides so a diacritic-less token ("grun") corrects to "grün".
        val folded = Umlaut.fold(token)
        return store.correctionCandidates(token)
            .filter { it.lowercase() != token && EditDistance.atMostOne(folded, Umlaut.fold(it.lowercase())) && !store.isBlacklisted(it) }
            .maxByOrNull { score(it, store.frequencyOf(it), previousWord) }
    }
    
    private fun score(word: String, frequency: Long, previousWord: String?): Double {
        val base = frequency.toDouble()
        if (previousWord == null) {
            return base
        }
        return base + store.bigramFrequency(previousWord, word).toDouble() * BIGRAM_WEIGHT
    }
    
    companion object {
        
        private const val MIN_AUTOCORRECT_LENGTH = 2
        // Fuzzy bar suggestions kick in from the third letter; on 1-2 letters the prefix completion alone
        // is both plenty and more precise (D-11 / D-12).
        private const val MIN_FUZZY_LENGTH = 3
        private const val SCAN_FACTOR = 2
        private const val BIGRAM_WEIGHT = 10.0
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.suggestion.EditDistance
import de.froehlichmedia.adaptkey.suggestion.Suggestion
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider

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
        return store.unigramsByPrefix(input.lowercase(), maxCandidates * SCAN_FACTOR)
            .filter { !store.isBlacklisted(it.word) } // A-04
            .map { Suggestion(it.word, score(it.word, it.frequency, previousWord)) }
            .sortedByDescending { it.score }
            .take(maxCandidates)
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
        return store.allKnownWords()
            .filter { it.lowercase() != token && !store.isBlacklisted(it) && EditDistance.atMostOne(token, it.lowercase()) }
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
        private const val SCAN_FACTOR = 2
        private const val BIGRAM_WEIGHT = 10.0
    }
}

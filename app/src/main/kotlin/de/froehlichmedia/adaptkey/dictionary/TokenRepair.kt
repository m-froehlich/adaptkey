// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * The two words a token was split into (A-05); both are returned in lower case, so the caller applies
 * the capitalisation hierarchy (§6) to each part.
 *
 * @property left the first word
 * @property right the second word
 */
data class SplitResult(
    val left: String,
    val right: String
)

/**
 * Retroactive token repair for the space/letter confusion bands (T-05): word split (A-05) and word
 * merge (A-06). Pure logic over the {@link DictionaryStore} abstraction, so it is unit-tested with the
 * in-memory store, mirroring {@link DictionarySuggestionProvider} and {@code CapitalisationEngine}.
 *
 * Both rules require a valid linguistic result, not mere spatial proximity: a split or merge is applied
 * only when the dictionary (or a high-probability bigram) confirms it. A token that is already a known
 * word is never touched (consistent with A-01).
 *
 * @property store the backing dictionary store
 */
class TokenRepair(private val store: DictionaryStore) {
    
    /**
     * Attempts to split [token] into two words (A-05).
     *
     * First the space-ambiguous tap positions are tried: the flagged character is dropped and replaced
     * by a space, so {@code "und<x>das"} with the {@code x} flagged becomes {@code "und" + "das"}. If no
     * flagged split validates, a fully missed space is tried by inserting a space at every position
     * without dropping a character (e.g. {@code "aberdas" -> "aber" + "das"}). In both modes each half
     * must be a known, non-blacklisted word; among the valid candidates the highest-scoring split wins.
     *
     * @param token the committed token (any case); a known word is never split
     * @param spaceAmbiguousIndices the indices flagged space-ambiguous by the T-05 bands
     * @param previousWord the word committed before the token, for bigram scoring; may be null
     * @return the split, or null when no valid linguistic split exists
     */
    fun trySplit(token: String, spaceAmbiguousIndices: Set<Int>, previousWord: String? = null): SplitResult? {
        val t = token.lowercase()
        if (t.length < 2 * MIN_PART || store.isKnownWord(t)) {
            return null
        }
        
        val flagged = spaceAmbiguousIndices
            .filter { it in MIN_PART..t.length - 1 - MIN_PART }
            .mapNotNull { i -> candidateAt(t.substring(0, i), t.substring(i + 1), previousWord) }
        if (flagged.isNotEmpty()) {
            return flagged.maxBy { it.second }.first
        }
        
        val missed = (MIN_PART..t.length - MIN_PART)
            .mapNotNull { k -> candidateAt(t.substring(0, k), t.substring(k), previousWord) }
        return missed.maxByOrNull { it.second }?.first
    }
    
    /**
     * Attempts to merge a spurious letter-ambiguous space back into [token] (A-06): prepends the
     * [inferredChar] from the tap's x-coordinate and tests whether the result is a valid word or a
     * high-probability continuation of [previousWord]. Only applies when [token] itself is not a valid
     * word.
     *
     * @param previousWord the word before the spurious space, for bigram confirmation; may be null
     * @param inferredChar the letter inferred from the letter-ambiguous tap
     * @param token the token following the spurious space (any case)
     * @return the reconstructed lower-case word, or null when no valid merge exists
     */
    fun tryMerge(previousWord: String?, inferredChar: Char, token: String): String? {
        val t = token.lowercase()
        if (t.isEmpty() || store.isKnownWord(t)) {
            return null
        }
        val candidate = inferredChar.lowercaseChar() + t
        if (store.isBlacklisted(candidate)) {
            return null
        }
        if (store.isKnownWord(candidate)) {
            return candidate
        }
        if (previousWord != null && store.bigramFrequency(previousWord, candidate) >= MIN_BIGRAM) {
            return candidate
        }
        return null
    }
    
    private fun candidateAt(left: String, right: String, previousWord: String?): Pair<SplitResult, Double>? {
        if (left.length < MIN_PART || right.length < MIN_PART) {
            return null
        }
        if (!isWord(left) || !isWord(right)) {
            return null
        }
        return SplitResult(left, right) to score(left, right, previousWord)
    }
    
    private fun isWord(word: String): Boolean {
        return store.isKnownWord(word) && !store.isBlacklisted(word)
    }
    
    private fun score(left: String, right: String, previousWord: String?): Double {
        val base = store.frequencyOf(left).toDouble() + store.frequencyOf(right).toDouble()
        val contextBonus = previousWord?.let { store.bigramFrequency(it, left).toDouble() * BIGRAM_WEIGHT } ?: 0.0
        return base + store.bigramFrequency(left, right).toDouble() * BIGRAM_WEIGHT + contextBonus
    }
    
    companion object {
        
        /** Minimum length of either part of a split or the token of a merge. */
        const val MIN_PART = 2
        
        /** Minimum bigram count accepted as a "high-probability" continuation (A-06). */
        const val MIN_BIGRAM = 3L
        
        private const val BIGRAM_WEIGHT = 10.0
    }
}

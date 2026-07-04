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
     * First a "hit a letter instead of space" mis-tap is tried by dropping one character and replacing it
     * with a space: the character must be either a T-05 space-ambiguous tap or a letter that physically
     * sits over the space bar ([OVER_SPACE_LETTERS], so it works even without touch calibration), e.g.
     * {@code "und<c>das" -> "und" + "das"}. Failing that, a fully missed space is tried by inserting a space
     * without dropping a character, but only where the two halves actually co-occur (a real bigram), so a
     * typo is not cut into two coincidental fragments (e.g. {@code "aberdas" -> "aber" + "das"} splits,
     * {@code "luste"} does not). In all modes each half must be a known, non-blacklisted word; among the
     * valid candidates the highest-scoring split wins.
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
        
        // Drop-a-character split: the removed character is either a T-05 space-ambiguous tap or a letter
        // that physically sits over the space bar (c/v/b/n/m on QWERTZ) — both are plausible "hit a letter
        // instead of space" mis-taps. The over-space set makes this work even without touch calibration,
        // where the T-05 flags are unreliable. Each half must still be a valid word.
        val dropIndices = (spaceAmbiguousIndices + t.indices.filter { t[it] in OVER_SPACE_LETTERS })
            .filter { it in MIN_PART..t.length - 1 - MIN_PART }
            .toSet()
        val dropped = dropIndices.mapNotNull { i -> candidateAt(t.substring(0, i), t.substring(i + 1), previousWord) }
        if (dropped.isNotEmpty()) {
            return dropped.maxBy { it.second }.first
        }
        
        // A fully missed space is only inserted when the two halves actually co-occur (a real bigram):
        // with a large, noisy dictionary almost any typo can be cut into two "known" fragments, so a mere
        // pair of dictionary words is not enough evidence — "aber das" (frequent bigram) splits, a typo
        // like "luste" -> "lu ste" (never co-occurs) does not.
        val missed = (MIN_PART..t.length - MIN_PART)
            .mapNotNull { k -> candidateAt(t.substring(0, k), t.substring(k), previousWord) }
            .filter { store.bigramFrequency(it.first.left, it.first.right) >= MIN_SPLIT_BIGRAM }
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
        
        /** Minimum bigram count required to accept a fully-missed-space split (A-05), so typos are not cut apart. */
        const val MIN_SPLIT_BIGRAM = 3L
        
        /** QWERTZ letters that physically sit over the space bar; a plausible letter-for-space mis-tap (A-05). */
        val OVER_SPACE_LETTERS = setOf('c', 'v', 'b', 'n', 'm')
        
        private const val BIGRAM_WEIGHT = 10.0
    }
}

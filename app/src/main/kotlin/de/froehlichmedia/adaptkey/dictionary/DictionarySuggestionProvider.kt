// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.suggestion.EditDistance
import de.froehlichmedia.adaptkey.suggestion.KeyboardProximity
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
        return store.correctionCandidates(token, candidateFirstChars(token)).filter { candidate ->
            val lower = candidate.lowercase()
            lower != token && isCloseMatch(folded, lower)
        }
    }
    
    /**
     * The initial letters to search for correction candidates of [token] (D-38): its own first character,
     * its keyboard neighbours (so a first-key typo like `eerden` -> `werden` is reachable) and its umlaut
     * variant when it starts with `a` / `o` / `u` (so `Uberblick` -> `Überblick`).
     *
     * @param token the lower-cased token
     * @return the set of initial letters to search
     */
    private fun candidateFirstChars(token: String): Set<Char> {
        val first = token.firstOrNull() ?: return emptySet()
        val result = HashSet<Char>()
        result.add(first)
        result.addAll(KeyboardProximity.neighboursOf(first))
        when (first) {
            'a' -> result.add('ä')
            'o' -> result.add('ö')
            'u' -> result.add('ü')
        }
        return result
    }
    
    /**
     * Whether [candidateLower] is a plausible correction of the folded token [foldedToken] (D-12 / D-28):
     * within the proximity-aware weighted edit budget - a single edit of any kind, or two edits that are
     * both cheap (a neighbouring-key substitution or an umlaut/ß fold), so `komplezz` reaches `komplett`
     * (two adjacent `z`→`t` slips) while two unrelated substitutions are rejected.
     *
     * @param foldedToken the umlaut-folded, lower-cased typed token
     * @param candidateLower the lower-cased candidate word
     * @return true when the candidate is within the correction budget
     */
    private fun isCloseMatch(foldedToken: String, candidateLower: String): Boolean {
        return correctionCost(foldedToken, candidateLower) <= MAX_CORRECTION_COST
    }
    
    /**
     * The proximity-aware weighted edit cost between the folded token and a candidate (D-28 / D-38): a
     * neighbouring-key substitution costs [ADJACENT_SUB_COST], any other substitution or an insert/delete
     * [SUB_COST] / [INDEL_COST]. Used both to gate candidates and to rank the autocorrect by lowest cost.
     *
     * @param foldedToken the umlaut-folded, lower-cased typed token
     * @param candidateLower the lower-cased candidate word
     * @return the total weighted edit cost
     */
    private fun correctionCost(foldedToken: String, candidateLower: String): Int {
        return EditDistance.weightedDistance(foldedToken, Umlaut.fold(candidateLower), INDEL_COST) { x, y ->
            when {
                x == y -> 0
                KeyboardProximity.adjacent(x, y) -> ADJACENT_SUB_COST
                else -> SUB_COST
            }
        }
    }
    
    /**
     * The known word [input] becomes by restoring only its German diacritics - a word whose umlaut/ß-folded
     * form equals the folded token but that carries the diacritics the user omitted (D-48: umlauts are
     * first-class characters): `konnen` → `können`, `russ` → `ruß`. Returns null when the token is already a
     * known word (A-01) or no pure-diacritic match exists.
     *
     * Such a restoration must take precedence over an A-05 split, so a real umlaut word (`konnen`) is
     * corrected to `können`, never cut into fragments (`ko nen`).
     *
     * @param input the composing token (any case)
     * @param previousWord the preceding word, for bigram tie-breaking among matches; may be null
     * @return the diacritic-restored known word in canonical case, or null
     */
    override fun diacriticRestoration(input: String, previousWord: String?): String? {
        val token = input.lowercase()
        if (token.length < MIN_FUZZY_LENGTH || isKnownWord(token)) {
            return null
        }
        val folded = Umlaut.fold(token)
        return store.correctionCandidates(token, candidateFirstChars(token))
            .asSequence()
            .filter { candidate ->
                val lower = candidate.lowercase()
                lower != token && !store.isBlacklisted(candidate) && Umlaut.fold(lower) == folded
            }
            .maxByOrNull { score(it, store.frequencyOf(it), previousWord) }
    }
    
    override fun nextWordSuggestions(previousWord: String): List<Suggestion> {
        if (previousWord.isBlank()) {
            return emptyList()
        }
        // The store already returns the successors ordered by bigram count; drop blacklisted words (A-04)
        // and carry the count as the score so the bar ranking is consistent with the other tiers.
        return store.nextWords(previousWord, maxCandidates)
            .asSequence()
            .filter { !store.isBlacklisted(it) }
            .map { word -> Suggestion(word, store.bigramFrequency(previousWord, word).toDouble()) }
            .toList()
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
        // Only a bounded candidate set is scanned (not the whole lexicon). The cheap pure distance test
        // runs before the per-candidate blacklist query, so the DB is touched only for real matches.
        // D-12 / D-28: umlauts / ß are folded and neighbouring-key typos are cheap, so "grun"→"grün" and
        // "komplezz"→"komplett". D-38: also search neighbour / umlaut first-char buckets, and rank by the
        // lowest edit cost first (frequency only breaks ties), so "dasy" corrects to "dass" (one adjacent
        // edit) rather than the more frequent "das" (a deletion).
        val folded = Umlaut.fold(token)
        return store.correctionCandidates(token, candidateFirstChars(token))
            .asSequence()
            .filter { it.lowercase() != token && !store.isBlacklisted(it) }
            .mapNotNull { candidate ->
                val cost = correctionCost(folded, candidate.lowercase())
                if (cost > MAX_CORRECTION_COST) null else CandidateCost(candidate, cost, score(candidate, store.frequencyOf(candidate), previousWord))
            }
            .minWithOrNull(compareBy({ it.cost }, { -it.score }))
            ?.candidate
    }
    
    /** A correction candidate with its edit cost and n-gram score, for the D-38 cost-first ranking. */
    private data class CandidateCost(val candidate: String, val cost: Int, val score: Double)
    
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
        
        // D-28 proximity-aware correction budget: a neighbouring-key substitution costs 1, any other
        // substitution or an insert/delete costs 2, and a candidate is accepted up to a total cost of 2 -
        // i.e. any single edit, or two edits that are both cheap (adjacent-key or umlaut-fold).
        private const val ADJACENT_SUB_COST = 1
        private const val SUB_COST = 2
        private const val INDEL_COST = 2
        private const val MAX_CORRECTION_COST = 2
    }
}

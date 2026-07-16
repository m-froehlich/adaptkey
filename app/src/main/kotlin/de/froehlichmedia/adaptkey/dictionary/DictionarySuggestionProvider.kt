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
 * @property minAutocorrectFrequency D-114: a correction candidate below this absolute frequency is never
 *           offered by [autocorrectFor]/[highConfidenceCorrection], however good its edit cost otherwise
 *           looks - defaults to 0 (no floor) so a plain `DictionarySuggestionProvider(store)` behaves
 *           exactly as before; production call sites pass a real, corpus-calibrated value (see
 *           [de.froehlichmedia.adaptkey.AdaptKeyService])
 */
class DictionarySuggestionProvider(
    private val store: DictionaryStore,
    private val maxCandidates: Int = 12,
    private val minAutocorrectFrequency: Long = 0
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
        // D-116: an unhyphenated compound whose exact form isn't itself in the dictionary but whose known
        // first part plus a resolvable rest reconstructs it - only attempted once prefix/fuzzy matching
        // found nothing at all, both because that is genuinely when it is needed (a real compound has no
        // single-word neighbour close enough for D-12 to find) and to keep it off the common keystroke path
        // entirely: it can run a handful of extra store lookups (D-138 is the standing reminder that
        // stacking several per-keystroke lookups is a real, previously-felt cost, not a theoretical one).
        if (candidates.isEmpty()) {
            compoundCandidate(token, previousWord)?.let { word ->
                candidates[word] = Suggestion(word, score(word, store.frequencyOf(word), previousWord))
            }
        }
        return candidates.values
            .sortedByDescending { it.score }
            .take(maxCandidates)
    }
    
    /**
     * D-116: recognises [token] as a plausible unhyphenated compound - a known noun (at least a handful of
     * characters) as the first part, followed by an optional Fugenelement and a remainder that is itself
     * known or a high-confidence (cost-1) correction of one, e.g. `beitragsjahreb` -> `Beitrag` + `s` +
     * `jahren` (the whole compound `Beitragsjahren` is too rare to be in the dictionary itself). Deliberately
     * **suggestion-only** - never wired into [autocorrectFor] / [highConfidenceCorrection] - because the
     * split point itself can be genuinely ambiguous between two equally valid readings (the classic German
     * compound-splitting counterexample "Wachstube" as "Wachs"+"tube" vs. "Wach"+"Stube"); offering a wrong
     * guess in the bar is harmless, silently committing one is not.
     *
     * @param token the lower-cased composing token
     * @param previousWord the preceding word, threaded through to the rest correction's own bigram scoring
     * @return the reconstructed compound in natural German casing (capitalised first part, lower-case
     *         rest), or null when no plausible split exists
     */
    private fun compoundCandidate(token: String, previousWord: String?): String? {
        if (isKnownWord(token)) {
            return null
        }
        val result = CompoundSplit.split(
            token,
            isKnownNoun = { candidate -> isKnownWord(candidate) && store.partsOfSpeech(candidate).contains(PartOfSpeech.NOUN) },
            resolveRest = { rest -> if (isKnownWord(rest)) rest else highConfidenceCorrection(rest, previousWord) }
        ) ?: return null
        return result.firstPart.replaceFirstChar { it.titlecase() } + result.fugenElement + result.rest
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
        return bestCorrection(input, previousWord, MAX_CORRECTION_COST)
    }
    
    /**
     * The autocorrection for [input], restricted to a low edit cost - a single neighbouring-key
     * substitution or better (D-67). Used to veto an A-05 split so a split never beats a much safer
     * whole-word correction, e.g. `kleiben` -> `kleinen` (a single adjacent `b`/`n` slip) must win over
     * `klei` + `en`.
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @return the high-confidence autocorrect replacement, or null when none qualifies
     */
    override fun highConfidenceCorrection(input: String, previousWord: String?): String? {
        return bestCorrection(input, previousWord, ADJACENT_SUB_COST)
    }
    
    /**
     * Shared candidate search behind [autocorrectFor] and [highConfidenceCorrection] (D-38 / D-67): only a
     * bounded candidate set is scanned (not the whole lexicon). The cheap pure distance test runs before
     * the per-candidate blacklist query, so the DB is touched only for real matches. D-12 / D-28: umlauts /
     * ß are folded and neighbouring-key typos are cheap, so "grun"→"grün" and "komplezz"→"komplett". D-38:
     * also search neighbour / umlaut first-char buckets, and rank by the lowest edit cost first (frequency
     * only breaks ties), so "dasy" corrects to "dass" (one adjacent edit) rather than the more frequent
     * "das" (a deletion).
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @param maxCost the inclusive edit-cost ceiling a candidate must stay within
     * @return the best-ranked correction within [maxCost], or null when none qualifies
     */
    private fun bestCorrection(input: String, previousWord: String?, maxCost: Int): String? {
        val token = input.lowercase()
        if (token.length < MIN_AUTOCORRECT_LENGTH) {
            return null
        }
        val folded = Umlaut.fold(token)
        val best = store.correctionCandidates(token, candidateFirstChars(token))
            .asSequence()
            .filter { it.lowercase() != token && !store.isBlacklisted(it) }
            .mapNotNull { candidate ->
                val cost = correctionCost(folded, candidate.lowercase())
                val frequency = store.frequencyOf(candidate)
                // D-114: a candidate too rare to be a trustworthy silent autocorrect target is dropped
                // outright, regardless of edit cost - see minAutocorrectFrequency.
                if (cost > maxCost || frequency < minAutocorrectFrequency) {
                    null
                } else {
                    CandidateCost(candidate, cost, score(candidate, frequency, previousWord))
                }
            }
            .minWithOrNull(compareBy({ it.cost }, { -it.score }))
            ?: return null
        // A-01: a valid word is never overwritten - except (§44/D-113) when the candidate is both a
        // single adjacent-key-level edit away (cost <= ADJACENT_SUB_COST, not the full two-edit
        // autocorrect budget) AND dramatically more frequent (shouldOverrideKnownWord()). D-113:
        // restricting the override to the strict single-adjacent-edit tier is what keeps it to genuine
        // cheap-typo cases ("due"->"die", "ddr"->"der", both cost 1) without also firing on two real,
        // unrelated words that merely happen to sit within the wider two-edit autocorrect budget
        // ("spreche" is a cost-2 edit from "Sprache" - e/a are not adjacent keys - so a common verb form
        // was losing to a far more frequent, but entirely different, noun).
        if (isKnownWord(token)) {
            if (!(best.cost <= ADJACENT_SUB_COST && shouldOverrideKnownWord(token, best.candidate))) {
                return null
            }
        } else if (RegularVerbInflection.isPlausibleInflection(token, ::isKnownWord)) {
            // D-115 / D-125: an unknown but regular ("weak") verb inflection of a known infinitive
            // ("beurteilst" of "beurteilen") is protected outright, with no ratio-override - unlike a
            // literal known word, it has no recorded frequency of its own to compare against a candidate's,
            // so §44's ratio check would always trivially fire (0 * ratio <= anything) if applied here.
            return null
        }
        return best.candidate
    }
    
    /**
     * §44: a candidate overrides A-01 when it is at least [KNOWN_WORD_OVERRIDE_RATIO] times more frequent
     * than [word] - deliberately extreme, so an ordinary pair of genuinely different, comparably common
     * words never gets remotely close to it and A-01 keeps protecting every normal known word exactly as
     * before. Without this, a stray adjacent-key slip that happens to also spell a real (but rare) word is
     * permanently protected from correction: "due" (a rare loanword, frequency 24 in the bundled corpus)
     * blocked any correction to "die" (frequency ~890000) outright, simply for existing in the dictionary
     * at all - regardless of how implausible it is that "due" was actually intended over "die".
     */
    override fun shouldOverrideKnownWord(word: String, candidate: String): Boolean {
        val wordFrequency = store.frequencyOf(word.lowercase())
        val candidateFrequency = store.frequencyOf(candidate.lowercase())
        return wordFrequency * KNOWN_WORD_OVERRIDE_RATIO <= candidateFrequency
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
        
        // §44: how many times more frequent a correction candidate must be than the typed word before A-01's
        // "known word" protection is set aside. Deliberately extreme - see bestCorrection().
        private const val KNOWN_WORD_OVERRIDE_RATIO = 50
    }
}

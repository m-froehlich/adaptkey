// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.suggestion.Umlaut

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
) {
    
    /**
     * §47: the two colour-span ranges within [token] this split corresponds to - contiguous for a
     * missed-space split, with a one-character gap at the dropped character for a drop-strategy split.
     * The strategy is not tracked explicitly; it is recovered from the length arithmetic alone, since a
     * drop split's halves are always exactly one character shorter than [token] combined, while a
     * missed-space split's halves add up to [token] exactly.
     *
     * @param token the exact composing token this result was computed from
     * @return the left and right span ranges, in that order
     */
    fun spanRanges(token: String): Pair<IntRange, IntRange> {
        val gap = token.length - left.length - right.length
        val rightStart = left.length + gap
        return (0 until left.length) to (rightStart until rightStart + right.length)
    }
}

/**
 * Retroactive token repair for the space/letter confusion bands (T-05): word split (A-05) and word merge
 * (A-06). Pure logic over the {@link DictionaryStore} abstraction, so it is unit-tested with the
 * in-memory store, mirroring {@link DictionarySuggestionProvider} and {@code CapitalisationEngine}.
 *
 * Both rules require a valid linguistic result, not mere spatial proximity: a split or merge is applied
 * only when the dictionary (or a high-probability bigram) confirms it. A token that is already a known
 * word is never touched (consistent with A-01).
 *
 * §128 (D-203): the split-candidate gate was redesigned after a real, data-confirmed finding - requiring
 * a prior bigram co-occurrence (the old `MIN_SPLIT_BIGRAM`) rejected *every* first-time-typed compound
 * typo by construction (a compound accidentally glued together has, by definition, never been recorded as
 * two separate co-occurring words), which is exactly why "der" + "Kinderarzt" split successfully during a
 * deliberate mid-word re-edit ([splitAtUnresolvedConnector], which never required co-occurrence) but not
 * at ordinary commit time ([trySplit], which did) - confirmed against the bundled corpus, not guessed: the
 * bigram table has zero co-occurrences for "der"+"kinderarzt" even though both halves are individually
 * well-known. The replacement, from a live design discussion: (1) a token that is already a known word,
 * *or a plausible regular-verb inflection of one* ([RegularVerbInflection]), is never split at all - closes
 * the historical "meinst" -> "mei"+"st" false positive at the source, the same way A-01 protects a literal
 * dictionary word; (2) each half must individually clear [MIN_SPLIT_HALF_FREQUENCY], not merely exist -
 * calibrated against the bundled dict_de.tsv (whose own floor is already 8, so this is a narrow trim of the
 * bottom tier, not a strong filter - most of the historical "mei"/"st"-class noise is closer to genuine
 * dictionary entries in frequency than initially assumed, see [isNoun]'s own note); (3) a pair where *both*
 * halves independently resolve to a noun is rejected - German does not ordinarily juxtapose two bare,
 * unlinked nouns as separate words (that is what a compound *word* is for), unlike an article/pronoun/
 * conjunction (tagged [PartOfSpeech.OTHER] in this dictionary) followed by a noun, which is an entirely
 * ordinary phrase shape; (4) bigram co-occurrence is no longer a gate at all - it still weights ranking via
 * [score] exactly as before, so a candidate *with* co-occurrence evidence still wins over one without, but
 * a plausible novel pairing is no longer rejected outright for lacking prior evidence.
 *
 * @property store the backing dictionary store
 */
class TokenRepair(private val store: DictionaryStore) {
    
    /**
     * Attempts to split [token] into two words (A-05).
     *
     * D-69 / §45: two split strategies are tried and the higher-scoring result wins overall - neither one
     * gets an unconditional priority over the other. A "hit a letter instead of space" mis-tap drops one
     * character and replaces it with a space: the character must be either a T-05 space-ambiguous tap or a
     * letter that physically sits over the space bar ([OVER_SPACE_LETTERS], so it works even without touch
     * calibration), e.g. {@code "und<c>das" -> "und" + "das"}. A fully missed space is tried by inserting a
     * space without dropping a character. Both strategies require each half to clear [candidateAt]'s own
     * gates (§128 / D-203) - not merely both individually be *any* known word, which alone let almost any
     * typo be cut into two "known" fragments if either half happened to be an obscure dictionary entry
     * nobody would plausibly have typed there. In all modes each half must also be non-blacklisted; among
     * every valid candidate from both strategies the highest-scoring split wins.
     *
     * D-216: polls [isCancelled] once per split position tried - each position costs several store
     * round-trips via [candidateAt] (D-214), so a token superseded partway through (this now runs on a
     * background thread, see [de.froehlichmedia.adaptkey.AdaptKeyService]'s own `composingPreviewExecutor`)
     * stops there instead of finishing every remaining position for a result nobody is waiting on any more.
     * Whatever was already found is still returned rather than discarded - harmless either way, since the
     * caller re-checks staleness again before ever applying it.
     *
     * @param token the committed token (any case); a known word (or plausible inflection of one) is never split
     * @param spaceAmbiguousIndices the indices flagged space-ambiguous by the T-05 bands
     * @param previousWord the word committed before the token, for bigram scoring; may be null
     * @param isCancelled polled once per split position; true stops trying further positions early
     * @return the split, or null when no valid linguistic split exists
     */
    fun trySplit(
        token: String,
        spaceAmbiguousIndices: Set<Int>,
        previousWord: String? = null,
        isCancelled: () -> Boolean = { false }
    ): SplitResult? {
        val t = token.lowercase()
        if (t.length < 2 * MIN_PART || isAlreadyRecognised(t)) {
            return null
        }
        
        // Drop-a-character split: the removed character is either a T-05 space-ambiguous tap or a letter
        // that physically sits over the space bar (c/v/b/n/m on QWERTZ) — both are plausible "hit a letter
        // instead of space" mis-taps. The over-space set makes this work even without touch calibration,
        // where the T-05 flags are unreliable. Each half must still clear candidateAt's own gates.
        val dropIndices = (spaceAmbiguousIndices + t.indices.filter { t[it] in OVER_SPACE_LETTERS })
            .filter { it in MIN_PART..t.length - 1 - MIN_PART }
            .toSet()
        val candidates = ArrayList<Pair<SplitResult, Double>>()
        for (i in dropIndices) {
            if (isCancelled()) {
                return candidates.maxByOrNull { it.second }?.first
            }
            candidateAt(t.substring(0, i), t.substring(i + 1), previousWord)?.let { candidates.add(it) }
        }
        for (k in MIN_PART..t.length - MIN_PART) {
            if (isCancelled()) {
                return candidates.maxByOrNull { it.second }?.first
            }
            candidateAt(t.substring(0, k), t.substring(k), previousWord)?.let { candidates.add(it) }
        }
        
        return candidates.maxByOrNull { it.second }?.first
    }
    
    /**
     * D-122: an unresolved [OVER_SPACE_LETTERS] connector split - unlike [trySplit], considers only the
     * connector-letter-drop strategy, never the missed-space one, and only ever consulted while the user is
     * actively re-editing an existing word mid-word ([AdaptKeyService.isEditingMidWord] - a much stronger
     * intent signal ("I came back to fix this specific word") than ordinary forward typing). Since §128 /
     * D-203, [candidateAt]'s own gates (frequency floor, not-both-nouns) apply here exactly as they do in
     * [trySplit] - there is no longer a *co-occurrence* difference between the two (bigram was never a gate
     * either function still needs), only a difference in which strategies/positions are tried. The caller is
     * responsible for treating the result as a suggestion only, never a silent autocorrect.
     *
     * @param token the composing token (any case); a known word (or plausible inflection of one) is never split
     * @param previousWord the word committed before the token, used only for ranking between multiple
     *        candidate connector positions (via the shared [score]), not as a gate; may be null
     * @return the highest-scoring connector split, or null when no candidate position yields two valid halves
     */
    fun splitAtUnresolvedConnector(token: String, previousWord: String? = null): SplitResult? {
        val t = token.lowercase()
        if (t.length < 2 * MIN_PART || isAlreadyRecognised(t)) {
            return null
        }
        return t.indices
            .filter { it in MIN_PART..t.length - 1 - MIN_PART && t[it] in OVER_SPACE_LETTERS }
            .mapNotNull { i -> candidateAt(t.substring(0, i), t.substring(i + 1), previousWord) }
            .maxByOrNull { it.second }
            ?.first
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
    
    /**
     * §128 / D-203: whether [t] is already recognised as a real word and must never be split - either
     * literally ([DictionaryStore.isKnownWord]) or as a plausible regular-verb inflection of a known
     * infinitive ([RegularVerbInflection]). The latter closes the historical "meinst" -> "mei" + "st" false
     * positive at the source: "meinst" is never itself in the dictionary, so the old `isKnownWord(t)`-only
     * guard let it fall through to split-candidate generation at all.
     */
    private fun isAlreadyRecognised(t: String): Boolean {
        return store.isKnownWord(t) || RegularVerbInflection.isPlausibleInflection(t, store::isKnownWord)
    }
    
    /**
     * §128 / D-203: the shared candidate gate behind both [trySplit] and [splitAtUnresolvedConnector].
     * Both [left] and [right] must resolve to a real, non-blacklisted word ([resolveWord] - umlaut/ß-fold
     * aware, so a half typed without its diacritic, e.g. "ueber", is still recognised via its real spelling
     * "über" for the purposes of this check and [isNoun], matching this project's own "umlauts are ordinary
     * characters" principle elsewhere), each must individually clear [MIN_SPLIT_HALF_FREQUENCY] (not merely
     * exist), and the pair must not be *both* nouns ([isNoun] - German does not ordinarily juxtapose two
     * bare, unlinked nouns as separate words, unlike a function word followed by a noun, an entirely
     * ordinary phrase shape). Deliberately does **not** require prior bigram co-occurrence (§128 / D-203) -
     * see the class-level KDoc for why that was the wrong gate. [SplitResult] still carries the literal
     * typed substrings ([left]/[right] as passed in, not the umlaut-restored form) so [SplitResult.spanRanges]
     * - which maps back onto the exact characters of the currently displayed composing text - stays correct;
     * only the *resolved* forms are used for the frequency/noun/score lookups below.
     *
     * @param left the left half exactly as typed (lower-cased)
     * @param right the right half exactly as typed (lower-cased)
     * @param previousWord the word committed before the token, for bigram scoring; may be null
     * @return the split candidate and its score, or null when either half fails a gate
     */
    private fun candidateAt(left: String, right: String, previousWord: String?): Pair<SplitResult, Double>? {
        if (left.length < MIN_PART || right.length < MIN_PART) {
            return null
        }
        val leftEntry = resolveWord(left) ?: return null
        val rightEntry = resolveWord(right) ?: return null
        if (leftEntry.frequency < MIN_SPLIT_HALF_FREQUENCY || rightEntry.frequency < MIN_SPLIT_HALF_FREQUENCY) {
            return null
        }
        // D-244: an acronym-shaped half (stored all-uppercase, e.g. "EBS") needs a much higher bar than an
        // ordinary word - confirmed against the real dict_de.tsv that 77% of all-caps entries sit below
        // frequency 50, i.e. they are disproportionately obscure abbreviations nobody would plausibly have
        // intended, unlike an ordinary lower/mixed-case half of the same low frequency. Reusing
        // MIN_AUTOCORRECT_CANDIDATE_FREQUENCY's own established "trustworthy" bar (300, D-114) rather than
        // inventing a new one.
        if (isAcronym(leftEntry) && leftEntry.frequency < MIN_SPLIT_ACRONYM_FREQUENCY) {
            return null
        }
        if (isAcronym(rightEntry) && rightEntry.frequency < MIN_SPLIT_ACRONYM_FREQUENCY) {
            return null
        }
        if (isNoun(leftEntry) && isNoun(rightEntry)) {
            return null
        }
        return SplitResult(left, right) to score(leftEntry, rightEntry, previousWord)
    }
    
    /**
     * §128 / D-203: [raw] itself first (the common case - nothing to unfold), then every plausible
     * umlaut/ß-restored spelling ([Umlaut.unfoldCandidates]), so a half typed without its diacritic still
     * resolves to its real dictionary entry.
     *
     * D-214: returns the resolved [WordEntry] itself, not just its word - [candidateAt] needs the
     * frequency and part-of-speech [isNoun]/[score] would otherwise each independently re-fetch from the
     * store for the very word this call just resolved.
     *
     * @param raw the lower-cased, literally-typed half
     * @return the matched, non-blacklisted entry, or null when no variant matches
     */
    private fun resolveWord(raw: String): WordEntry? {
        for (candidate in Umlaut.unfoldCandidates(raw)) {
            if (store.isBlacklisted(candidate)) {
                continue
            }
            val entry = store.entryOf(candidate)
            if (entry != null) {
                return entry
            }
        }
        return null
    }
    
    /**
     * D-244: whether [entry]'s own canonical stored spelling is all-uppercase (an acronym/abbreviation,
     * e.g. "EBS", "DDR") rather than an ordinary word or capitalised noun (e.g. "Dock", "Kinderarzt") - used
     * by [candidateAt] to apply [MIN_SPLIT_ACRONYM_FREQUENCY] on top of the ordinary [MIN_SPLIT_HALF_FREQUENCY].
     * A single-character entry is never flagged (nothing to distinguish "acronym" from "capital letter" at
     * that length).
     */
    private fun isAcronym(entry: WordEntry): Boolean {
        return entry.word.length >= MIN_PART && entry.word == entry.word.uppercase() && entry.word.any { it.isLetter() }
    }
    
    /**
     * §128 / D-203: whether [entry] resolves to a noun ([PartOfSpeech.NOUN] or [PartOfSpeech.PROPER_NOUN]),
     * used by [candidateAt] to reject a both-nouns pair. Calibrated against the bundled dict_de.tsv for the
     * motivating "meinst" -> "mei"+"st" case: both "Mei" (frequency 16, tagged NOUN+OTHER) and "St"
     * (frequency 5939, tagged NOUN) individually clear [MIN_SPLIT_HALF_FREQUENCY] comfortably - frequency
     * alone cannot distinguish them from a genuine compound half like "Kinderarzt" (frequency 14) - but both
     * being tagged as nouns is what actually sets this pair apart from an ordinary phrase like "der" (OTHER)
     * + "Kinderarzt" (NOUN) or "und" (OTHER) + "das" (OTHER).
     */
    private fun isNoun(entry: WordEntry): Boolean {
        return entry.partsOfSpeech.contains(PartOfSpeech.NOUN) || entry.partsOfSpeech.contains(PartOfSpeech.PROPER_NOUN)
    }
    
    /**
     * D-214: takes the already-resolved entries directly instead of re-fetching each half's frequency from
     * the store a second time (candidateAt's own callers - trySplit/splitAtUnresolvedConnector - already
     * paid for that lookup once via [resolveWord]). Only the bigram counts are genuinely new lookups here.
     */
    private fun score(left: WordEntry, right: WordEntry, previousWord: String?): Double {
        val base = left.frequency.toDouble() + right.frequency.toDouble()
        val contextBonus = previousWord?.let { store.bigramFrequency(it, left.word).toDouble() * BIGRAM_WEIGHT } ?: 0.0
        return base + store.bigramFrequency(left.word, right.word).toDouble() * BIGRAM_WEIGHT + contextBonus
    }
    
    companion object {
        
        /**
         * D-230 (reverted): a 2->3 bump was tried to close the `"Docker"`/`"darfst"` regressions (see history
         * §157/§158) but was wrong - it also blocks every genuine missed-space split ending in a real
         * 2-letter German function word (`"an"`, `"im"`, `"um"`, `"es"`, `"zu"`, ...), e.g. `"gehtes"` ->
         * `"geht"` + `"es"`, which this app must still be able to find. Reverted to 2; the actual fix for
         * `"Docker"`/`"darfst"` needs a different, more targeted strategy - see the open backlog discussion.
         */
        const val MIN_PART = 2
        
        /** Minimum bigram count accepted as a "high-probability" continuation (A-06). */
        const val MIN_BIGRAM = 3L
        
        /**
         * §128 / D-203: the minimum standalone frequency either half of a split must individually clear
         * (replaces the old `MIN_SPLIT_BIGRAM` co-occurrence gate - see the class-level KDoc). Calibrated
         * against the bundled dict_de.tsv, whose own minimum entry frequency is already 8 (the corpus was
         * pre-filtered at the source) - this is a narrow trim of the very bottom tier, not a strong filter;
         * most of the discrimination against implausible fragments comes from [isNoun]'s both-nouns rule
         * instead, not from frequency alone.
         */
        const val MIN_SPLIT_HALF_FREQUENCY = 10L
        
        /**
         * D-244: the minimum frequency an acronym-shaped half ([isAcronym]) must individually clear, on top
         * of [MIN_SPLIT_HALF_FREQUENCY] - reuses the same "trustworthy" bar
         * [de.froehlichmedia.adaptkey.AdaptKeyService]'s own `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY` (D-114)
         * already established, rather than inventing a new threshold.
         */
        const val MIN_SPLIT_ACRONYM_FREQUENCY = 300L
        
        /** QWERTZ letters that physically sit over the space bar; a plausible letter-for-space mis-tap (A-05). */
        val OVER_SPACE_LETTERS = setOf('c', 'v', 'b', 'n', 'm')
        
        private const val BIGRAM_WEIGHT = 10.0
    }
}

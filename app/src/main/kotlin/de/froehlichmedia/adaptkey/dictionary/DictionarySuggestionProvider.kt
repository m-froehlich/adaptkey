// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.suggestion.Correction
import de.froehlichmedia.adaptkey.suggestion.EditDistance
import de.froehlichmedia.adaptkey.suggestion.KeyboardProximity
import de.froehlichmedia.adaptkey.suggestion.Suggestion
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider
import de.froehlichmedia.adaptkey.suggestion.Umlaut
import kotlin.math.pow

/**
 * Tier-1 suggestion provider over a {@link DictionaryStore}: personal n-gram completion with a
 * bigram context bonus, the blacklist (A-04) and the "valid words are not overwritten" rule (A-01).
 *
 * Depends only on the {@link DictionaryStore} abstraction, so the ranking and policy are unit-tested
 * with the in-memory store; the SQLite store supplies the same behaviour on device.
 *
 * @property store the backing dictionary store
 * @property maxCandidates the maximum number of suggestions returned
 * @property minAutocorrectFrequency D-114/D-227: a correction candidate below this absolute frequency is
 *           never offered by [autocorrectFor]/[highConfidenceCorrection] - *unless* it is both a cost-1
 *           neighbouring-key substitution away ([ADJACENT_SUB_COST]) and not tagged [PartOfSpeech.NOUN]/
 *           [PartOfSpeech.PROPER_NOUN]. Cost alone is not enough to exempt a candidate - D-114's own
 *           original bug ("Virgin", frequency 62) was itself a cost-1 edit, but is also a noun-tagged
 *           Wikipedia-corpus artefact, unlike a genuinely common non-noun word the corpus simply
 *           under-counts (e.g. "übrigens", frequency 79). Defaults to 0 (no floor) so a plain
 *           `DictionarySuggestionProvider(store)` behaves exactly as before; production call sites pass a
 *           real, corpus-calibrated value (see [de.froehlichmedia.adaptkey.AdaptKeyService])
 */
class DictionarySuggestionProvider(
    private val store: DictionaryStore,
    private val maxCandidates: Int = 12,
    private val minAutocorrectFrequency: Long = 0
) : SuggestionProvider {
    
    override fun suggestionsFor(
        input: String,
        previousWord: String?,
        includeExpensiveFallbacks: Boolean,
        isCancelled: () -> Boolean
    ): List<Suggestion> {
        val token = input.lowercase()
        // Keyed by canonical word so a word is never offered twice; insertion order is irrelevant since
        // the merged set is re-sorted by score before it is capped.
        val candidates = LinkedHashMap<String, Suggestion>()
        // Prefix completion, ranked by frequency + bigram context (shown from the very first letter, D-11).
        // D-144: unigramsByPrefix is a literal/raw prefix match (both stores) - it alone would never find
        // "tatsächlich" for a typed "tatsachl", violating this app's own founding "umlauts are ordinary
        // characters" principle for the one feature it names explicitly (suggestions). Umlaut.unfoldCandidates
        // tries every plausible unfolded spelling of the typed prefix - the literal token first (the
        // overwhelmingly common case, with nothing to unfold, costs exactly the one query it always did).
        for (prefixVariant in Umlaut.unfoldCandidates(token)) {
            for (entry in store.unigramsByPrefix(prefixVariant, maxCandidates * SCAN_FACTOR)) {
                if (candidates.containsKey(entry.word) || store.isBlacklisted(entry.word)) {
                    continue // A-04
                }
                candidates[entry.word] = Suggestion(entry.word, score(entry.word, entry.frequency, previousWord))
            }
        }
        // D-12: also offer close real words - a single edit or an umlaut/ß variant - so a mistype or a
        // valid-but-wrong word still surfaces the intended one ("mut" -> "mit", "grun" -> "grün").
        // D-205: ranked by scoreWithCost, not score - a closer candidate generally outranks a farther,
        // merely more frequent one (see scoreWithCost's own KDoc).
        // D-208: moved behind includeExpensiveFallbacks (like D-116/D-117 below), unlike those NOT also
        // gated on candidates.isEmpty() - D-12's own basic behaviour ("mut" -> "mit") must still surface
        // even when prefix completion also found something for "mut" itself. Its own cost (a bucket scan
        // of up to ~2000 candidates plus a per-candidate edit-distance computation, §125/D-194's banding
        // notwithstanding) grows with the composing token's own length and was running on every keystroke
        // once the token reached MIN_FUZZY_LENGTH - a real, measured, felt slowdown mid-word on longer
        // words, distinct from D-160's own commit-adjacent empty-candidates escalation.
        if (includeExpensiveFallbacks) {
            for ((word, cost) in fuzzyNeighbours(token, isCancelled)) {
                if (candidates.containsKey(word) || store.isBlacklisted(word)) {
                    continue // A-04
                }
                candidates[word] = Suggestion(word, scoreWithCost(word, store.frequencyOf(word), previousWord, cost))
            }
        }
        // D-116: an unhyphenated compound whose exact form isn't itself in the dictionary but whose known
        // first part plus a resolvable rest reconstructs it - only attempted once prefix/fuzzy matching
        // found nothing at all, both because that is genuinely when it is needed (a real compound has no
        // single-word neighbour close enough for D-12 to find) and to keep it off the common keystroke path
        // entirely: it can run a handful of extra store lookups (D-138 is the standing reminder that
        // stacking several per-keystroke lookups is a real, previously-felt cost, not a theoretical one).
        // D-160: the empty-candidates gate alone proved insufficient - a long unknown compound satisfies it
        // on *every* keystroke, so exactly the worst-case token ran this (and the wide fuzzy below,
        // including this one's own inner highConfidenceCorrection pass) per keystroke, saturating the main
        // thread (spec §102). Both fallbacks now additionally honour includeExpensiveFallbacks: the hot
        // path passes false and re-runs with true in one deferred pass once the token has been stable.
        if (includeExpensiveFallbacks && candidates.isEmpty()) {
            compoundCandidate(token, previousWord)?.let { word ->
                candidates[word] = Suggestion(word, score(word, store.frequencyOf(word), previousWord))
            }
        }
        // D-117: a longer token garbled by more than D-28's ordinary two-edit budget ("erkamm" for
        // "erkannt") still gets one more, wider-budget try - suggestion-only (never autocorrect, unlike
        // every other candidate source above, this one is deliberately never trusted enough for that) and
        // only once every cheaper, tighter search above found nothing at all, for the same reason as D-116:
        // this is a rare fallback, not a general loosening of D-28's own budget, which stays exactly as
        // tight as before for the common case. Gated on includeExpensiveFallbacks like D-116 (D-160).
        if (includeExpensiveFallbacks && candidates.isEmpty()) {
            for ((word, cost) in wideFuzzyNeighbours(token, isCancelled)) {
                if (candidates.containsKey(word) || store.isBlacklisted(word)) {
                    continue // A-04
                }
                candidates[word] = Suggestion(word, scoreWithCost(word, store.frequencyOf(word), previousWord, cost))
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
    
    // D-202: reuses compoundCandidate() as-is (confirmed sufficient on its own) rather than a separate
    // recognition pass - context-free (no previousWord) since this only feeds a learning-throttle decision,
    // not a ranked suggestion, so the rest-correction's bigram tie-breaking among otherwise-equal candidates
    // does not matter here.
    override fun looksLikeUnsplitCompound(word: String): Boolean {
        return compoundCandidate(word.lowercase(), null) != null
    }
    
    /**
     * Close real-word neighbours of [token] for the suggestion bar (D-12): candidates within one edit of
     * the token once German umlauts / ß are folded on both sides, so a diacritic-less typing matches its
     * correct form. The token itself is excluded (S-02 handles the verbatim case). Uses the same bounded,
     * indexed candidate set as the autocorrect, so it stays cheap per keystroke.
     *
     * D-205: returns each candidate's own edit cost alongside it - [suggestionsFor] discounts [score] by it
     * ([scoreWithCost]) instead of ranking purely by frequency, so a candidate genuinely close to the typed
     * token generally outranks a farther one even when the farther one is far more frequent.
     *
     * D-211: polls [isCancelled] once per candidate - the search runs on a background thread now (D-208),
     * so a superseded call stops partway through the (potentially large, D-209-uncapped) candidate list
     * instead of finishing pointless work; whatever was already gathered is still returned rather than
     * discarded, since a spent cycle can at least contribute what it found, but the caller checks staleness
     * again before ever applying it (see [de.froehlichmedia.adaptkey.AdaptKeyService]'s own KDoc).
     *
     * @param token the lower-cased composing token
     * @param isCancelled polled once per candidate; true stops the scan early
     * @return the neighbouring known words in canonical case, each paired with its edit cost
     */
    private fun fuzzyNeighbours(token: String, isCancelled: () -> Boolean): List<Pair<String, Int>> {
        if (token.length < MIN_FUZZY_LENGTH) {
            return emptyList()
        }
        val folded = Umlaut.fold(token)
        val result = ArrayList<Pair<String, Int>>()
        for (candidate in store.correctionCandidates(token, candidateFirstChars(token))) {
            if (isCancelled()) {
                break
            }
            val lower = candidate.lowercase()
            if (lower == token) {
                continue
            }
            val cost = correctionCost(folded, lower, MAX_CORRECTION_COST)
            if (cost <= MAX_CORRECTION_COST) {
                result.add(candidate to cost)
            }
        }
        return result
    }
    
    /**
     * D-117: [fuzzyNeighbours]'s own wider-budget sibling ([WIDE_CORRECTION_COST] instead of
     * [MAX_CORRECTION_COST]) for a token multiple typos have garbled past the ordinary D-28 budget, e.g.
     * `erkamm` -> `erkannt` (two substitutions plus an insertion). Restricted to a longer minimum length
     * ([MIN_WIDE_FUZZY_LENGTH]) than the ordinary fuzzy search, since a wide edit-cost budget on a short
     * token would match almost anything - and, like [fuzzyNeighbours], still only searches the token's own
     * first-character bucket (its own letter or a keyboard neighbour), so a token whose very *first* letter
     * is also badly garbled is still out of reach; a genuinely open question (see D-117's own spec entry),
     * not attempted here.
     *
     * D-211: polls [isCancelled] once per candidate - see [fuzzyNeighbours]'s own KDoc for the reasoning.
     *
     * @param token the lower-cased composing token
     * @param isCancelled polled once per candidate; true stops the scan early
     * @return the neighbouring known words in canonical case, each paired with its edit cost (D-205)
     */
    private fun wideFuzzyNeighbours(token: String, isCancelled: () -> Boolean): List<Pair<String, Int>> {
        if (token.length < MIN_WIDE_FUZZY_LENGTH) {
            return emptyList()
        }
        val folded = Umlaut.fold(token)
        val result = ArrayList<Pair<String, Int>>()
        for (candidate in store.correctionCandidates(token, candidateFirstChars(token))) {
            if (isCancelled()) {
                break
            }
            val lower = candidate.lowercase()
            if (lower == token) {
                continue
            }
            val cost = correctionCost(folded, lower, WIDE_CORRECTION_COST)
            if (cost <= WIDE_CORRECTION_COST) {
                result.add(candidate to cost)
            }
        }
        return result
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
     * The proximity-aware weighted edit cost between the folded token and a candidate (D-28 / D-38): a
     * neighbouring-key substitution costs [ADJACENT_SUB_COST], any other substitution or an insert/delete
     * [SUB_COST] / [INDEL_COST]. Used both to gate candidates and to rank the autocorrect by lowest cost.
     *
     * §125 / D-194: [maxCost] is threaded straight into [EditDistance.weightedDistance]'s own banding -
     * every call site here only ever compares the result against a fixed ceiling anyway (see its own
     * KDoc), so passing that same ceiling in lets the DP stay within a band around it instead of scanning
     * the whole token/candidate pair, which is what actually mattered for the per-keystroke cost on long
     * tokens. Callers must pass their own real ceiling, not a stand-in - a narrower one here than the one
     * actually compared against downstream would wrongly clip candidates that should have qualified.
     *
     * @param foldedToken the umlaut-folded, lower-cased typed token
     * @param candidateLower the lower-cased candidate word
     * @param maxCost the same cost ceiling the caller will compare the result against
     * @return the total weighted edit cost, or a value guaranteed to exceed [maxCost] when the true cost does
     */
    private fun correctionCost(foldedToken: String, candidateLower: String, maxCost: Int): Int {
        return EditDistance.weightedDistance(foldedToken, Umlaut.fold(candidateLower), INDEL_COST, maxCost) { x, y ->
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
     * D-197: draws candidates from [DictionaryStore.diacriticCandidates], not [DictionaryStore.correctionCandidates]
     * - this is an exact fold-equality test, not a weighted edit-distance search, so it needs the *complete*
     * length/first-character window, not [correctionCandidates]' frequency-truncated one. A rare but
     * correctly-spelled diacritic word was previously crowded out of the bounded candidate set by hundreds of
     * more common same-bucket words before ever reaching the comparison below, e.g. "Gruße" failing to
     * restore to "Grüße" (frequency 18) while falling back to an unrelated fuzzy match instead.
     *
     * D-204: the fold-equality check itself now accepts either of [Umlaut.foldVariants]' variants for the
     * candidate side, not only [Umlaut.fold]'s own "ss" convention - so a token typed via this app's own
     * long-press-alternative convention (e.g. "gruse" for "Grüße", `ß` reached by long-pressing `s`) is
     * recognised as an equally exact match, not left to the edit-cost-budgeted/frequency-floored fuzzy path.
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
        return store.diacriticCandidates(token, candidateFirstChars(token))
            .asSequence()
            .filter { candidate ->
                val lower = candidate.lowercase()
                lower != token && !store.isBlacklisted(candidate) && Umlaut.foldVariants(lower).contains(folded)
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
        return bestCorrection(input, previousWord, MAX_CORRECTION_COST)?.candidate
    }
    
    /**
     * D-207: answers [autocorrectFor] and [highConfidenceCorrection] together from the single, wider
     * ([MAX_CORRECTION_COST]) search - a cost-1 candidate always wins that search's own cost-first ranking
     * whenever one exists (see [bestCorrection]'s own KDoc), so it is exactly the candidate
     * [highConfidenceCorrection]'s tighter budget would separately have found; re-running that narrower
     * search again on the same token would only ever rediscover the same winner. Replaces the two
     * independent `store.correctionCandidates()` searches [de.froehlichmedia.adaptkey.AdaptKeyService]'s
     * `finalizeAndCommit()` previously ran on every commit for this reason alone.
     */
    override fun bestCorrectionFor(input: String, previousWord: String?): Correction? {
        val best = bestCorrection(input, previousWord, MAX_CORRECTION_COST) ?: return null
        return Correction(best.candidate, best.cost <= ADJACENT_SUB_COST)
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
        return bestCorrection(input, previousWord, ADJACENT_SUB_COST)?.candidate
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
     * D-207: returns the winning candidate's own edit cost alongside it (not just the word) - lets
     * [bestCorrectionFor] answer the high-confidence question from this one search's own result, instead
     * of running a second, narrower search over the same candidates purely to re-derive it.
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @param maxCost the inclusive edit-cost ceiling a candidate must stay within
     * @return the best-ranked candidate within [maxCost] and its own cost, or null when none qualifies
     */
    private fun bestCorrection(input: String, previousWord: String?, maxCost: Int): CandidateCost? {
        val token = input.lowercase()
        if (token.length < MIN_AUTOCORRECT_LENGTH) {
            return null
        }
        val folded = Umlaut.fold(token)
        val best = store.correctionCandidates(token, candidateFirstChars(token))
            .asSequence()
            .filter { it.lowercase() != token && !store.isBlacklisted(it) }
            .mapNotNull { candidate ->
                val cost = correctionCost(folded, candidate.lowercase(), maxCost)
                // D-220: the cost check runs before frequencyOf() is ever called, not after - the token's
                // own first-character bucket is uncapped (D-209), so a common initial letter (e.g. H/G) can
                // hold hundreds of candidates most of which the cheap, pure-CPU cost check alone already
                // rejects; querying every one of them for its frequency regardless (the previous order) was
                // measured (via D-217's handleKey timing) as the actual dominant cost of every commit -
                // finalizeAndCommit()'s own new D-220 timing log showed bestCorrectionMs at 200-400ms for an
                // already-correct, common-initial-letter word (isKnownWord() alone does not short-circuit
                // this search - see the A-01 override check below - so the search always ran regardless).
                if (cost > maxCost) {
                    return@mapNotNull null
                }
                val entry = store.entryOf(candidate)
                val frequency = entry?.frequency ?: store.frequencyOf(candidate)
                // D-114/D-227: a candidate too rare to be a trustworthy silent autocorrect target is dropped
                // outright - unless it is both a cost-1 neighbouring-key substitution AND not noun-tagged.
                // A first attempt exempted every cost-1 candidate outright, but that reopened D-114's own
                // original bug: "Virgin" (frequency 62, tagged NOUN - a Wikipedia-corpus proper-noun
                // artefact) is *also* a cost-1 edit from "Virhin"/"vorhin", so cost alone cannot tell the two
                // cases apart. What does: "übrigens" (frequency 79, tagged OTHER - an entirely ordinary,
                // simply corpus-under-counted adverb) is not a noun. A rare NOUN/PROPER_NOUN in this
                // Wikipedia-derived corpus is disproportionately a foreign/proper-noun artefact - exactly
                // what this floor exists to filter - while a rare non-noun at cost-1 is far more likely a
                // genuine, common word that is simply outnumbered by hyper-frequent words like "der"/"die".
                val isNounLike = entry?.partsOfSpeech?.any {
                    it == PartOfSpeech.NOUN || it == PartOfSpeech.PROPER_NOUN
                } ?: true
                if (frequency < minAutocorrectFrequency && (cost > ADJACENT_SUB_COST || isNounLike)) {
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
        return best
    }
    
    /**
     * §44 / D-244: a candidate overrides A-01 when it is at least [KNOWN_WORD_OVERRIDE_RATIO] times more
     * frequent than [word] - deliberately extreme, so an ordinary pair of genuinely different, comparably
     * common words never gets remotely close to it and A-01 keeps protecting every normal known word
     * exactly as before. Without this, a stray adjacent-key slip that happens to also spell a real (but
     * rare) word is permanently protected from correction: "due" (a rare loanword, frequency 24 in the
     * bundled corpus) blocked any correction to "die" (frequency ~890000) outright, simply for existing in
     * the dictionary at all - regardless of how implausible it is that "due" was actually intended over
     * "die".
     *
     * D-244: raised from 50 to 100 after a real regression - "Ohren" (ears, frequency 170, an entirely
     * ordinary, unambiguous German word) was silently overridden to "Ihren" (frequency 11,907 - `o`/`i` are
     * QWERTZ-adjacent) purely because 170*50 <= 11,907. The original 50x bar assumed "a genuine word pair
     * never gets remotely close" - wrong here: 70x is not remotely close to the 37,000x+ ratios the real
     * blacklisted-confusable cases (`due`/`die`, `ddr`/`der`) actually sit at, confirmed against the real
     * corpus, not guessed. 100 keeps comfortable headroom below the smallest genuine case (`ddr`/`der`,
     * ~228x) while excluding the `Ohren`/`Ihren` case (70x) - a considered value, not device-tuned further.
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
    
    /**
     * D-205: [score], discounted by how far [word] actually is from the typed token - raised directly from
     * the user's own position that a candidate's *closeness* to the actual mistake should generally matter
     * more for the suggestion bar's ranking than how often it is used overall, mirroring [bestCorrection]'s
     * already-shipped cost-first autocorrect ranking (`compareBy({it.cost}, {-it.score})`) - but as a soft
     * preference here, not a hard rule, since this ranking also has to sit alongside candidate sources with
     * no cost concept at all (an ordinary prefix completion is cost 0 by construction). [FUZZY_COST_DECAY]
     * is applied once per cost step (`FUZZY_COST_DECAY^cost`), so an overwhelmingly more frequent but
     * farther candidate can still occasionally win - calibrated against the real bundled `dict_de.tsv`
     * frequency range (roughly 8 to 1,000,000): at 0.01, a cost-1 candidate needs ~100x the frequency of a
     * cost-0 one to outrank it, a cost-2 candidate ~10,000x - both achievable at the corpus's extremes, not
     * as a matter of course. A considered starting point, not yet device-tuned - easy to retune here alone,
     * no call site depends on its exact value.
     *
     * @param word the candidate word
     * @param frequency the candidate's dictionary frequency
     * @param previousWord the preceding word, for the same bigram bonus [score] applies
     * @param cost the candidate's edit cost from the typed token (0 for an exact/prefix match)
     * @return [score]'s own result, discounted by [FUZZY_COST_DECAY] raised to the power of [cost]
     */
    private fun scoreWithCost(word: String, frequency: Long, previousWord: String?, cost: Int): Double {
        return score(word, frequency, previousWord) * FUZZY_COST_DECAY.pow(cost)
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
        private const val KNOWN_WORD_OVERRIDE_RATIO = 100
        
        // D-117: a considered, not-yet-device-tuned starting point for the wider, suggestion-only fallback
        // budget - loose enough to reach "erkamm" -> "erkannt" (cost 4: two adjacent-key substitutions plus
        // an insertion), restricted to longer tokens ([MIN_WIDE_FUZZY_LENGTH]) so it stays meaningfully
        // scoped rather than a general loosening of D-28's own tight, autocorrect-grade budget.
        private const val WIDE_CORRECTION_COST = 4
        private const val MIN_WIDE_FUZZY_LENGTH = 6
        
        // D-205: see scoreWithCost()'s own KDoc for the calibration reasoning against the real bundled
        // dict_de.tsv frequency range.
        private const val FUZZY_COST_DECAY = 0.01
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.GermanRules
import de.froehlichmedia.adaptkey.language.LanguageRules
import de.froehlichmedia.adaptkey.suggestion.Acronym
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
 * @property aggressiveness D-353: how eagerly [bestCorrection]/[fuzzyNeighbours] trust a correction
 *           candidate - see [AutocorrectAggressiveness] and [CorrectionConfidence] for the full mechanism
 *           this replaces D-114/D-227's frequency floor and D-244's flat ratio bar with. Defaults to
 *           [AutocorrectAggressiveness.DEFAULT]; production call sites pass the user's own configured
 *           level (see [de.froehlichmedia.adaptkey.AdaptKeyService])
 * @property languageRules D-410: the active language's own compound-split/verb-inflection/adjective-
 *           inflection protections (see [LanguageRules]) - delegated rather than hardcoded so a non-German
 *           store is never subject to German-specific morphology. Defaults to [GermanRules] so every
 *           existing caller that does not pass one explicitly keeps this class's historical behaviour
 *           unchanged; [de.froehlichmedia.adaptkey.AdaptKeyService] is the one production caller that
 *           resolves and passes the value matching the actually active language.
 * @property now D-411/D-429: "now", for [LearnedFrequencyBoost]'s and [LearnedBigramBoost]'s own recency
 *           checks inside [score], [rankingBigramFrequency] and [nextWordSuggestions] - threaded through
 *           rather than read directly, so a caller's own tests stay deterministic (mirrors
 *           [InMemoryDictionaryStore]'s identical `clock` parameter). Defaults to the real wall clock.
 */
class DictionarySuggestionProvider(
    private val store: DictionaryStore,
    private val maxCandidates: Int = 12,
    private val aggressiveness: AutocorrectAggressiveness = AutocorrectAggressiveness.DEFAULT,
    private val languageRules: LanguageRules = GermanRules,
    private val now: () -> Long = { System.currentTimeMillis() }
) : SuggestionProvider {
    
    override fun suggestionsFor(
        input: String,
        previousWord: String?,
        previousPreviousWord: String?,
        includeExpensiveFallbacks: Boolean,
        isCancelled: () -> Boolean
    ): List<Suggestion> {
        val token = input.lowercase()
        val candidates = obviousCandidates(token, previousWord, previousPreviousWord, includeExpensiveFallbacks, isCancelled)
        // D-117: a longer token garbled by more than D-28's ordinary two-edit budget ("erkamm" for
        // "erkannt") still gets one more, wider-budget try - suggestion-only (never autocorrect, unlike
        // every other candidate source above, this one is deliberately never trusted enough for that) and
        // only once every cheaper, tighter search above found nothing at all, for the same reason as D-116:
        // this is a rare fallback, not a general loosening of D-28's own budget, which stays exactly as
        // tight as before for the common case. Gated on includeExpensiveFallbacks like D-116 (D-160).
        // D-431: deliberately not consulted by hasObviousCandidate() below - a match found only here is, by
        // this very method's own long-standing KDoc, "never trusted enough" even for a suggestion chip to
        // count as "something was found" for A-13's purposes.
        if (includeExpensiveFallbacks && candidates.isEmpty()) {
            for ((word, cost) in wideFuzzyNeighbours(token, isCancelled)) {
                if (candidates.containsKey(word) || store.isBlacklisted(word)) {
                    continue // A-04
                }
                candidates[word] = Suggestion(word, scoreWithCost(word, store.frequencyOf(word), previousWord, previousPreviousWord, cost))
            }
        }
        return candidates.values
            .sortedByDescending { it.score }
            .take(maxCandidates)
    }
    
    /**
     * D-431: whether [suggestionsFor] would find anything "obvious" for [input] without resorting to
     * [wideFuzzyNeighbours]'s own last-resort, never-trusted-even-for-a-chip fallback (D-117) - a real
     * prefix completion (including a D-144 umlaut-unfolded or D-328 neighbour-substituted-prefix variant),
     * a close ([MAX_CORRECTION_COST]) fuzzy match (D-12), or a recognised unhyphenated compound (D-116).
     *
     * Used by [de.froehlichmedia.adaptkey.AdaptKeyService]'s own A-13 gate
     * ([de.froehlichmedia.adaptkey.suggestion.MissedBackspaceRecovery], D-377): "every other repair
     * mechanism has already had its own chance and failed" was never meant to require the *whole*
     * suggestion bar to end up empty - something can almost always be suggested - only that nothing
     * *obvious* turned up first. A-13's own recovered word is then offered regardless of whether
     * [wideFuzzyNeighbours] separately, coincidentally, also found some unrelated real word (see D-431's
     * own history entry for the concrete case this was reported against: `"welxmche"` for `"welche"`
     * wrongly suppressed by a cheaper, but wrong, wide-fuzzy match on `"welsche"`).
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @param previousPreviousWord D-366: the word committed two positions before, or null when unknown
     * @return true when an ordinary (non-wide-fuzzy) candidate search already finds something
     */
    override fun hasObviousCandidate(input: String, previousWord: String?, previousPreviousWord: String?): Boolean {
        val token = input.lowercase()
        return obviousCandidates(token, previousWord, previousPreviousWord, includeExpensiveFallbacks = true) { false }.isNotEmpty()
    }
    
    /**
     * D-431: [suggestionsFor]'s own candidate search, minus [wideFuzzyNeighbours]'s last-resort fallback -
     * shared verbatim between [suggestionsFor] itself and [hasObviousCandidate], so the two can never drift
     * apart on what counts as "obvious".
     */
    private fun obviousCandidates(
        token: String,
        previousWord: String?,
        previousPreviousWord: String?,
        includeExpensiveFallbacks: Boolean,
        isCancelled: () -> Boolean
    ): LinkedHashMap<String, Suggestion> {
        // Keyed by canonical word so a word is never offered twice; insertion order is irrelevant since
        // the merged set is re-sorted by score before it is capped.
        val candidates = LinkedHashMap<String, Suggestion>()
        // Prefix completion, ranked by frequency + bigram context (shown from the very first letter, D-11).
        // D-144: unigramsByPrefix is a literal/raw prefix match (both stores) - it alone would never find
        // "tatsächlich" for a typed "tatsachl", violating this app's own founding "umlauts are ordinary
        // characters" principle for the one feature it names explicitly (suggestions). Umlaut.unfoldCandidates
        // tries every plausible unfolded spelling of the typed prefix - the literal token first (the
        // overwhelmingly common case, with nothing to unfold, costs exactly the one query it always did).
        // D-272: discounted by scoreWithPrefixDistance, not score - a candidate needing fewer additional
        // characters beyond what is already typed generally outranks one needing more, mirroring D-205's own
        // "closeness over raw frequency" principle for the fuzzy-match path (see scoreWithPrefixDistance's
        // own KDoc for why this needed its own, capped decay rather than reusing D-205's directly).
        for (prefixVariant in Umlaut.unfoldCandidates(token)) {
            for (entry in store.unigramsByPrefix(prefixVariant, maxCandidates * SCAN_FACTOR)) {
                if (candidates.containsKey(entry.word) || store.isBlacklisted(entry.word)) {
                    continue // A-04
                }
                val extraLength = entry.word.length - token.length
                candidates[entry.word] =
                    Suggestion(entry.word, scoreWithPrefixDistance(entry.word, entry.frequency, previousWord, previousPreviousWord, extraLength))
            }
        }
        // D-328: escalation - when the literal (and umlaut-unfolded) prefix found nothing at all, also try
        // prefix completions of keyboard-neighbour-substituted prefixes, so a typo early in a long word
        // ("vetmut..." -> "vermut...") still surfaces the intended completion mid-word rather than only once
        // the whole token is typed and the full-token edit-distance search (fuzzyNeighbours below) finally
        // comes within budget. Gated like D-116/D-117 on candidates being empty (the literal prefix already
        // had its chance) and on a longer minimum length (a neighbour substitution on a short token would
        // match far too much); capped like D-144's own unfold combinatorics so a long token cannot blow up
        // the number of indexed prefix scans. Each variant is fed through the same Umlaut.unfoldCandidates +
        // unigramsByPrefix loop as the literal token, so a typo plus a missing umlaut ("twtsach..." ->
        // "tatsächlich") is resolved in one pass. Suggestion-only by construction - it populates the same
        // candidates map, so S-02 (never the exact input) and A-04 (blacklist) apply unchanged.
        if (includeExpensiveFallbacks && candidates.isEmpty() && token.length >= MIN_NEIGHBOUR_PREFIX_LENGTH) {
            for (prefixVariant in neighbourPrefixVariants(token)) {
                if (isCancelled()) {
                    break
                }
                for (unfolded in Umlaut.unfoldCandidates(prefixVariant)) {
                    if (isCancelled()) {
                        break
                    }
                    for (entry in store.unigramsByPrefix(unfolded, maxCandidates)) {
                        if (candidates.containsKey(entry.word) || store.isBlacklisted(entry.word)) {
                            continue // A-04
                        }
                        val extraLength = entry.word.length - token.length
                        candidates[entry.word] =
                            Suggestion(entry.word, scoreWithPrefixDistance(entry.word, entry.frequency, previousWord, previousPreviousWord, extraLength))
                    }
                }
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
                candidates[word] = Suggestion(word, scoreWithCost(word, store.frequencyOf(word), previousWord, previousPreviousWord, cost))
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
                candidates[word] = Suggestion(word, score(word, store.frequencyOf(word), previousWord, previousPreviousWord))
            }
        }
        return candidates
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
        val result = languageRules.splitCompound(
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
            // D-353: a candidate must still clear the chip-offer confidence floor - see this class's own
            // companion KDoc / CorrectionConfidence for why this is the same formula bestCorrection() uses
            // for its own (higher) auto-apply decision, just compared against the lower of the two
            // thresholds - an implausible enough candidate (e.g. D-117's wider fallback deliberately stays
            // outside this, see its own KDoc) should not clutter the suggestion bar either, not just never
            // silently apply.
            if (cost <= MAX_CORRECTION_COST && candidateConfidence(token, candidate, lower, cost) >= aggressiveness.chipOfferThreshold) {
                result.add(candidate to cost)
            }
        }
        return result
    }
    
    /**
     * D-353: [CorrectionConfidence.forUnknownToken] for [candidate] as a replacement for [token], looking
     * up its frequency/noun-tag/prefix-shift signals from [store]. Shared between [fuzzyNeighbours]' own
     * chip-offer filter and [bestCorrection]'s auto-apply decision so both are always answering the exact
     * same question about the exact same candidate, just against different thresholds.
     *
     * @param token the lower-cased typed token
     * @param candidate the candidate in its canonical (dictionary) case
     * @param candidateLower [candidate], already lower-cased by the caller (avoids a repeat lowercase() call)
     * @param cost the candidate's edit cost from [token]
     * @return the confidence in `[0, 1]`
     */
    private fun candidateConfidence(token: String, candidate: String, candidateLower: String, cost: Int): Double {
        val entry = store.entryOf(candidate)
        val frequency = entry?.frequency ?: store.frequencyOf(candidate)
        val isNounLike = entry?.partsOfSpeech?.any {
            it == PartOfSpeech.NOUN || it == PartOfSpeech.PROPER_NOUN
        } ?: true
        // D-371: a digit-ending typed token (e.g. a house/model number glued onto a word, "Str12") caps the
        // resulting confidence so it can only ever auto-apply at AGGRESSIVE - see
        // CorrectionConfidence.DIGIT_SUFFIX_CONFIDENCE_CAP's own KDoc for the exact threshold placement.
        val endsInDigit = token.isNotEmpty() && token.last().isDigit()
        return CorrectionConfidence.forUnknownToken(
            cost,
            frequency,
            isNounLike,
            CorrectionConfidence.prefixShiftsAway(token, candidateLower),
            endsInDigit
        )
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
     * D-328: every single-position keyboard-neighbour substitution of [token] (e.g. "vetmut" -> "vermut"),
     * capped at [MAX_NEIGHBOUR_PREFIX_VARIANTS]. Each variant, once fed back through
     * [Umlaut.unfoldCandidates] and [DictionaryStore.unigramsByPrefix] by the caller, reaches completions
     * whose real spelling the user mistyped at exactly one position - the prefix-completion counterpart of
     * [candidateFirstChars]'s own first-character neighbour broadening, extended to every position. Used only
     * as an escalation when the literal prefix found nothing, so a correctly-typed word never pays for it.
     * 
     * @param token the lower-cased composing token (already length-checked by the caller)
     * @return the distinct single-substitution variants, never including [token] itself
     */
    private fun neighbourPrefixVariants(token: String): List<String> {
        val results = LinkedHashSet<String>()
        for (i in token.indices) {
            for (neighbour in KeyboardProximity.neighboursOf(token[i])) {
                if (!neighbour.isLetter()) {
                    continue // Digits are never a plausible word-initial letter.
                }
                if (results.size >= MAX_NEIGHBOUR_PREFIX_VARIANTS) {
                    return results.toList()
                }
                results.add(token.substring(0, i) + neighbour + token.substring(i + 1))
            }
        }
        return results.toList()
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
    
    /**
     * D-246: elevates the existing bigram baseline with the personal trigram table when
     * [previousPreviousWord] is known, via Stupid Backoff (Brants et al. 2007): a candidate with a real
     * trigram match scores by its trigram count (D-429: rescaled and recency-boosted via
     * [LearnedBigramBoost], the same as every other n-gram contribution in this class - previously the one
     * remaining place still scoring by the raw count directly); a candidate reached only through the
     * (bundled + personal) bigram signal scores by its bigram count discounted by [TRIGRAM_BACKOFF_WEIGHT]
     * - a soft preference, not a hard "trigram always wins" rule (mirrors [scoreWithCost]'s own soft
     * edit-cost discount), so an overwhelmingly more frequent bigram-only candidate can still outrank a
     * barely-seen trigram one. A word already scored via its trigram match is never re-added via the
     * bigram pass (the more specific signal always wins for that word, never blended with its own
     * less-specific estimate).
     *
     * @param previousWord the most recently committed word
     * @param previousPreviousWord the word committed two positions before, or null when unknown - falls
     *        back to the plain bigram ranking, exactly as before D-246
     * @return predicted next words, most likely first
     */
    override fun nextWordSuggestions(previousWord: String, previousPreviousWord: String?): List<Suggestion> {
        if (previousWord.isBlank()) {
            return emptyList()
        }
        val hasTrigramContext = !previousPreviousWord.isNullOrBlank()
        val scores = LinkedHashMap<String, Double>()
        if (hasTrigramContext) {
            store.nextWordsTrigram(previousPreviousWord!!, previousWord, maxCandidates)
                .asSequence()
                .filter { !store.isBlacklisted(it) }
                .forEach { word ->
                    val trigram = store.trigramWithTimestamp(previousPreviousWord, previousWord, word)
                    scores[word] = LearnedBigramBoost.boost(trigram?.count ?: 0L, trigram?.lastTouched ?: 0L, now())
                }
        }
        // The store already returns the successors ordered by bigram count; drop blacklisted words (A-04).
        // A candidate already scored via its trigram match keeps that score; every other candidate carries
        // its bigram count as the score - discounted by TRIGRAM_BACKOFF_WEIGHT only when two-word context
        // was actually available (a genuine backoff from a higher order that just didn't match this word),
        // never when previousPreviousWord is unknown - that case is plain bigram ranking, exactly as before
        // D-246, not a backoff from anything.
        store.nextWords(previousWord, maxCandidates)
            .asSequence()
            .filter { !store.isBlacklisted(it) && it !in scores }
            .forEach { word ->
                // D-365: the learned share is rescaled via LearnedBigramBoost - see rankingBigramFrequency's
                // own KDoc - instead of counted as a flat +1-per-use raw count that a bundled bigram
                // co-occurring for some other candidate could otherwise swamp outright.
                val bigramScore = rankingBigramFrequency(previousWord, word)
                scores[word] = if (hasTrigramContext) bigramScore * TRIGRAM_BACKOFF_WEIGHT else bigramScore
            }
        return scores.entries
            .sortedByDescending { it.value }
            .take(maxCandidates)
            .map { (word, value) -> Suggestion(word, value) }
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
        // D-404-followup: a word typed entirely in capitals ("ETF", "AVD") is never autocorrected away,
        // known or not, and regardless of how confident/frequent a candidate looks - typing in capitals is
        // itself the user's own explicit, deliberate signal that this is an acronym, not a typo. Checked
        // against [input] (the original typed casing), not [token] (already folded to lower-case above),
        // and ahead of every other branch below, including the isKnownWord/shouldOverrideKnownWord one -
        // this is an absolute veto, not merely a higher bar the way D-403's own learnedCasingOf exemption
        // is (that one only protects a word already learned; an acronym must never be corrected away even
        // on its very first, not-yet-learned typing - see AdaptKeyService.learnThresholdFor's own identical
        // Acronym.isAcronym check for the matching "promotes at the ordinary threshold" half of this).
        if (Acronym.isAcronym(input)) {
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
                val frequency = store.entryOf(candidate)?.frequency ?: store.frequencyOf(candidate)
                CandidateCost(candidate, cost, score(candidate, frequency, previousWord))
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
        } else if (languageRules.isPlausibleVerbInflection(token, ::isKnownWord)) {
            // D-115 / D-125: an unknown but regular ("weak") verb inflection of a known infinitive
            // ("beurteilst" of "beurteilen") is protected outright, with no ratio-override - unlike a
            // literal known word, it has no recorded frequency of its own to compare against a candidate's,
            // so §44's ratio check would always trivially fire (0 * ratio <= anything) if applied here.
            return null
        } else if (languageRules.isPlausibleAdjectiveComparative(token, ::isPlausiblePositiveStem)) {
            // D-252: the adjective counterpart of the verb-inflection protection above, same reasoning -
            // "zuversichtlicher" (unknown, no dictionary entry of its own) must not lose to some other,
            // cost-1-adjacent, more-frequent candidate the way "beurteilst" would without the verb check.
            return null
        } else if (candidateConfidence(token, best.candidate, best.candidate.lowercase(), best.cost) < aggressiveness.autoApplyThreshold) {
            // D-353: replaces D-114/D-227's own per-candidate frequency-floor rejection (previously applied
            // during the scan above, to every candidate) with a single confidence check on the winner - see
            // CorrectionConfidence/AutocorrectAggressiveness for the graduated measure and its calibration.
            return null
        }
        return best
    }
    
    /**
     * D-252: whether [stem] is a plausible positive (base) adjective for [AdjectiveInflection] - known
     * *and* not a noun, since German nouns take no comparative/superlative degree at all. Without the noun
     * exclusion, a bare known-word check would treat "docker" as a plausible comparative of "dock"
     * (`NOUN`), the same over-triggering [TokenRepair.isAlreadyRecognised] already had to guard against.
     * 
     * @param stem the candidate reconstructed positive adjective (already lower-cased)
     * @return true when [stem] is known and not tagged [PartOfSpeech.NOUN]/[PartOfSpeech.PROPER_NOUN]
     */
    private fun isPlausiblePositiveStem(stem: String): Boolean {
        val entry = store.entryOf(stem) ?: return false
        return !(entry.partsOfSpeech.contains(PartOfSpeech.NOUN) || entry.partsOfSpeech.contains(PartOfSpeech.PROPER_NOUN))
    }
    
    /**
     * §44 / D-244 / D-353: a candidate overrides A-01 when [CorrectionConfidence.forKnownWordOverride]'s
     * frequency-ratio score clears [AutocorrectAggressiveness.autoApplyThreshold] - deliberately extreme by
     * construction (see that object's own KDoc for the log-scaled ratio calibration), so an ordinary pair
     * of genuinely different, comparably common words never gets remotely close to it and A-01 keeps
     * protecting every normal known word exactly as before. Without this, a stray adjacent-key slip that
     * happens to also spell a real (but rare) word is permanently protected from correction: "due" (a rare
     * loanword, frequency 24 in the bundled corpus) blocked any correction to "die" (frequency ~890000)
     * outright, simply for existing in the dictionary at all - regardless of how implausible it is that
     * "due" was actually intended over "die".
     *
     * D-244 / D-353: originally a flat 100x bar, replaced by [CorrectionConfidence]'s log-scaled ratio
     * curve after a real regression - "Ohren" (ears, frequency 170, an entirely ordinary, unambiguous
     * German word) was silently overridden to "Ihren" (frequency 11,907 - `o`/`i` are QWERTZ-adjacent)
     * purely because 170*50 <= 11,907 under the original, even looser 50x bar. Confirmed against the real
     * corpus, not guessed: 70x (`Ohren`/`Ihren`) is not remotely close to the 37,000x+ ratios the real
     * blacklisted-confusable cases (`due`/`die`, `ddr`/`der`, ~228x) actually sit at. D-353 additionally
     * guarantees this floor holds at *every* [AutocorrectAggressiveness] level, not only the default one -
     * see that enum's own KDoc for the exact calibration.
     *
     * D-403: never overrides a word the user has personally taught the keyboard - checked via
     * [DictionaryStore.learnedCasingOf], true for both a fully self-taught word and a deliberately
     * different-cased override of an otherwise-bundled entry (D-264) alike, before the ratio is even
     * computed. This ratio was calibrated against *bundled* corpus rarity (a genuinely rare but real
     * dictionary word); a learned word's own frequency is a fundamentally different kind of number by
     * construction - [DictionaryStore.learn] sets it to exactly the word's own reinforcement count,
     * starting at 1 - so without this exemption, almost any freshly-promoted word (an acronym like
     * "kWp"/"AVD", an abbreviation, anything) stayed permanently defenceless against this override for any
     * ordinary, moderately common cost-1-adjacent word, no matter how many times it had already been
     * deliberately taught.
     *
     * D-404 Tier 2: never overrides a word with a candidate from the *same* D-412 word family (checked via
     * [sameWordFamily], before the ratio is computed) - e.g. a typed "Kugel" must never be silently swapped
     * for the far more frequent plural sibling "Kugeln". The ratio rule exists for coincidental dictionary
     * noise (a rare, unrelated entry that happens to collide, `due`/`ddr`); a same-family candidate is by
     * definition a genuine, currently-in-use word of the same family the user may simply have chosen
     * deliberately, so hierarchy rule 1 (§6: explicit input always wins) applies unconditionally, with no
     * ratio to weigh at all. Missing lemma data on either side (a word/language the D-412 linking passes
     * never reached) yields no family match, leaving the ratio check as the only signal, exactly as before.
     */
    override fun shouldOverrideKnownWord(word: String, candidate: String): Boolean {
        if (store.learnedCasingOf(word) != null) {
            return false
        }
        val wordLower = word.lowercase()
        val candidateLower = candidate.lowercase()
        if (sameWordFamily(wordLower, candidateLower)) {
            return false
        }
        val confidence = CorrectionConfidence.forKnownWordOverride(
            ADJACENT_SUB_COST,
            store.frequencyOf(wordLower),
            store.frequencyOf(candidateLower),
            CorrectionConfidence.prefixShiftsAway(wordLower, candidateLower)
        )
        return confidence >= aggressiveness.autoApplyThreshold
    }
    
    /**
     * D-404 Tier 2: whether [wordLower] and [candidateLower] resolve to the same D-412 word family - either
     * one is the other's [WordEntry.lemma] base form, or both share a common base (e.g. "lief"/"läuft", both
     * linked to "laufen"). Reuses [DictionaryStore.entryOf] rather than a dedicated lemma lookup, since a
     * bundled+learned merged entry is exactly what a family check should compare against (D-404's own
     * learned-word lemma links, D-323/D-324, count too). Family-key resolution mirrors
     * [LearnedWordExpirySweep]'s own `entry.lemma ?: entry.word` grouping, lower-cased on both sides since
     * [WordEntry.lemma] is stored in the base entry's own canonical case, not [wordLower]'s lower-case key.
     *
     * @param wordLower the known typed word, already lower-cased
     * @param candidateLower the correction candidate, already lower-cased
     * @return true when both resolve to the same family key; false when either is unknown or unlinked
     */
    private fun sameWordFamily(wordLower: String, candidateLower: String): Boolean {
        val wordFamily = store.entryOf(wordLower)?.let { (it.lemma ?: it.word).lowercase() } ?: return false
        val candidateFamily = store.entryOf(candidateLower)?.let { (it.lemma ?: it.word).lowercase() } ?: return false
        return wordFamily == candidateFamily
    }
    
    /** A correction candidate with its edit cost and n-gram score, for the D-38 cost-first ranking. */
    private data class CandidateCost(val candidate: String, val cost: Int, val score: Double)
    
    /**
     * D-411: [frequency] (the caller's own already-merged bundled+learned figure, e.g. from [DictionaryStore.
     * entryOf]/[DictionaryStore.unigramsByPrefix]/[DictionaryStore.frequencyOf]) with its learned component
     * replaced by [LearnedFrequencyBoost]'s scaled, ranking-only equivalent - see [rankingFrequency]'s own
     * KDoc for why this is safe to apply here, at the single shared root every ranking score passes through,
     * without ever touching a correctness-affecting decision.
     *
     * D-365/D-366: the bigram/trigram bonus below is the same shared root's other half - [previousWord]'s
     * own contribution rescaled via [rankingBigramFrequency], and, when [previousPreviousWord] is also known,
     * elevated further by a genuine personal trigram match, exactly like [nextWordSuggestions]'s own D-246
     * Stupid Backoff blend. Every ordinary suggestionsFor() call site threads [previousPreviousWord] through
     * from the very start, so this signal no longer disappears the moment the user starts typing.
     */
    private fun score(word: String, frequency: Long, previousWord: String?, previousPreviousWord: String? = null): Double {
        val base = rankingFrequency(word, frequency)
        if (previousWord == null) {
            return base
        }
        // D-366: the same Stupid Backoff preference [nextWordSuggestions] already applies (D-246) - a
        // genuine personal trigram match for this exact candidate elevates it beyond the plain bigram
        // signal, instead of the richer two-word-context signal only ever mattering for S-07's own
        // blank-slate prediction and vanishing the moment the user starts typing. [LearnedBigramBoost]
        // (D-365/D-429) scales the trigram's own raw count, and applies the same recency boost, the same
        // way it treats a learned bigram count - a small, personal-only reinforcement count needs the same
        // curve to compete meaningfully against a corpus-scale contribution, regardless of which n-gram
        // order it came from.
        if (previousPreviousWord != null) {
            val trigram = store.trigramWithTimestamp(previousPreviousWord, previousWord, word)
            if (trigram != null && trigram.count > 0L) {
                return base + LearnedBigramBoost.boost(trigram.count, trigram.lastTouched, now()) * BIGRAM_WEIGHT
            }
            return base + rankingBigramFrequency(previousWord, word) * BIGRAM_WEIGHT * TRIGRAM_BACKOFF_WEIGHT
        }
        return base + rankingBigramFrequency(previousWord, word) * BIGRAM_WEIGHT
    }
    
    /**
     * D-365: [DictionaryStore.bigramFrequency] with its learned contribution ([DictionaryStore.
     * learnedBigramFrequency]'s own raw, +1-per-use count) replaced by [LearnedBigramBoost]'s scaled,
     * recency-aware (D-429) equivalent - the bundled contribution passes through untouched. Ranking-only,
     * mirroring [rankingFrequency]'s own D-411 precedent exactly: [TokenRepair]'s `>= MIN_BIGRAM` merge
     * gate (A-06) keeps reading [DictionaryStore.bigramFrequency] directly, never through this.
     *
     * @param previousWord the preceding word
     * @param word the candidate word
     * @return the bundled bigram count plus the learned count's scaled, ranking-only equivalent
     */
    private fun rankingBigramFrequency(previousWord: String, word: String): Double {
        val total = store.bigramFrequency(previousWord, word)
        val learned = store.learnedBigramWithTimestamp(previousWord, word) ?: LearnedNgram(0L, 0L)
        val bundledOnly = total - learned.count
        return bundledOnly.toDouble() + LearnedBigramBoost.boost(learned.count, learned.lastTouched, now())
    }
    
    /**
     * D-411: [mergedFrequency] with its learned contribution ([DictionaryStore.learnedFrequencyOf]'s own raw
     * count) removed and replaced by [LearnedFrequencyBoost]'s scaled, recency-aware equivalent - the bundled
     * contribution (if any) passes through untouched. [score] is the *only* place this runs, and [score] is
     * used solely for ranking/ordering already-accepted candidates against each other (prefix completion,
     * fuzzy-suggestion ordering, and - only as a cost tiebreaker, never a gate - [bestCorrection]/
     * [diacriticRestoration]'s own candidate selection). Every correctness-affecting frequency read in this
     * class ([candidateConfidence], [shouldOverrideKnownWord], [isPlausiblePositiveStem]) calls
     * [DictionaryStore.entryOf]/[DictionaryStore.frequencyOf] directly instead, never through [score], so
     * this boost can never change what gets silently auto-applied - only how candidates already cleared for
     * display are ordered.
     *
     * @param word the candidate word
     * @param mergedFrequency the already-merged bundled+learned frequency the caller looked up
     * @return [mergedFrequency] as a [Double], with its learned share replaced by the boosted equivalent
     */
    private fun rankingFrequency(word: String, mergedFrequency: Long): Double {
        val learned = store.learnedFrequencyOf(word) ?: return mergedFrequency.toDouble()
        val bundledOnly = mergedFrequency - learned.frequency
        return bundledOnly + LearnedFrequencyBoost.boost(learned.frequency, learned.lastTouched, now())
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
     * @param previousPreviousWord the word before that, for the same trigram bonus [score] applies (D-366)
     * @param cost the candidate's edit cost from the typed token (0 for an exact/prefix match)
     * @return [score]'s own result, discounted by [FUZZY_COST_DECAY] raised to the power of [cost]
     */
    private fun scoreWithCost(word: String, frequency: Long, previousWord: String?, previousPreviousWord: String?, cost: Int): Double {
        return score(word, frequency, previousWord, previousPreviousWord) * FUZZY_COST_DECAY.pow(cost)
    }
    
    /**
     * D-272: [score], discounted by how many characters [word] still extends beyond the typed prefix - the
     * user's own position, directly stated, that within a shared-prefix "family" the completion closer to
     * what has actually been typed so far should generally win over a merely more frequent, longer one
     * (`"natürlich"` before `"natürliche"` for a typed `"natürl"`), mirroring [scoreWithCost]'s already-
     * shipped "closeness over raw frequency" principle for the fuzzy-match path - as a soft preference, not
     * a hard rule, for the same reason [scoreWithCost] is soft: an overwhelmingly more frequent candidate can
     * still win.
     * 
     * Deliberately **not** [scoreWithCost]'s own [FUZZY_COST_DECAY] reused verbatim: checked against the real
     * bundled `dict_de.tsv`, a flat per-character exponential decay strong enough to flip a close, 1-character
     * German inflection pair (`"wichtig"` 1342 vs. `"wichtige"` 4330, needing a ratio below ~0.31) compounds
     * far too aggressively over the 4-7 extra characters an entirely ordinary longer completion needs
     * (`"Informationen"` 1913 vs. `"informiert"` 312 for a typed `"inform"` - `"Informationen"`'s own, correct
     * frequency lead was getting *inverted*, near-erased behind a candidate needing 3 fewer characters).
     * [PREFIX_LENGTH_DECAY_CAP] resolves this: the decay differentiates real closeness only up to that many
     * extra characters: `"natürlich"`/`"natürliche"` (3 vs. 4, both under the cap) are still told apart, while
     * `"informiert"`/`"Informationen"` (4 vs. 7, both at-or-past the cap) receive the *same* factor once
     * capped and fall back to plain, undistorted frequency ranking against each other - exactly where the
     * "how many more characters" signal genuinely stops being a meaningful proxy for likelihood. A considered
     * starting point checked against several real word families, not exhaustively device-tuned - easy to
     * retune here alone, no call site depends on its exact value.
     * 
     * @param word the candidate word
     * @param frequency the candidate's dictionary frequency
     * @param previousWord the preceding word, for the same bigram bonus [score] applies
     * @param previousPreviousWord the word before that, for the same trigram bonus [score] applies (D-366)
     * @param extraLength how many characters longer [word] is than the typed prefix (0 for an exact/complete
     *        match); a negative value (an umlaut-unfolded search variant briefly shorter than the literal
     *        typed prefix) is treated as 0
     * @return [score]'s own result, discounted by [PREFIX_LENGTH_DECAY] raised to the power of [extraLength]
     *         clamped to [PREFIX_LENGTH_DECAY_CAP]
     */
    private fun scoreWithPrefixDistance(
        word: String,
        frequency: Long,
        previousWord: String?,
        previousPreviousWord: String?,
        extraLength: Int
    ): Double {
        return score(word, frequency, previousWord, previousPreviousWord) * PREFIX_LENGTH_DECAY.pow(extraLength.coerceIn(0, PREFIX_LENGTH_DECAY_CAP))
    }
    
    companion object {
        
        private const val MIN_AUTOCORRECT_LENGTH = 2
        // Fuzzy bar suggestions kick in from the third letter; on 1-2 letters the prefix completion alone
        // is both plenty and more precise (D-11 / D-12).
        private const val MIN_FUZZY_LENGTH = 3
        private const val SCAN_FACTOR = 2
        private const val BIGRAM_WEIGHT = 10.0
        
        // D-246: Stupid Backoff's own literature-established discount (Brants et al., "Large Language
        // Models in Machine Translation", 2007) - applied to a bigram-only candidate's score whenever a
        // trigram context was available but did not itself match that candidate, so a real (if sparse)
        // trigram match is preferred, not blindly required, over a merely more frequent bigram-only word.
        private const val TRIGRAM_BACKOFF_WEIGHT = 0.4
        
        // D-28 proximity-aware correction budget: a neighbouring-key substitution costs 1, any other
        // substitution or an insert/delete costs 2, and a candidate is accepted up to a total cost of 2 -
        // i.e. any single edit, or two edits that are both cheap (adjacent-key or umlaut-fold).
        private const val ADJACENT_SUB_COST = 1
        private const val SUB_COST = 2
        private const val INDEL_COST = 2
        private const val MAX_CORRECTION_COST = 2
        
        // D-117: a considered, not-yet-device-tuned starting point for the wider, suggestion-only fallback
        // budget - loose enough to reach "erkamm" -> "erkannt" (cost 4: two adjacent-key substitutions plus
        // an insertion), restricted to longer tokens ([MIN_WIDE_FUZZY_LENGTH]) so it stays meaningfully
        // scoped rather than a general loosening of D-28's own tight, autocorrect-grade budget.
        private const val WIDE_CORRECTION_COST = 4
        private const val MIN_WIDE_FUZZY_LENGTH = 6
        
        // D-205: see scoreWithCost()'s own KDoc for the calibration reasoning against the real bundled
        // dict_de.tsv frequency range.
        private const val FUZZY_COST_DECAY = 0.01
        
        // D-272: see scoreWithPrefixDistance()'s own KDoc for the calibration reasoning against the real
        // bundled dict_de.tsv - 0.3 flips every close, 1-character-gap inflection pair checked there, and the
        // cap of 4 keeps a genuinely longer completion needing several more characters from being crowded out
        // by a much rarer, merely-shorter one once both are past the point where "how many more characters"
        // stops being a meaningful signal.
        private const val PREFIX_LENGTH_DECAY = 0.3
        private const val PREFIX_LENGTH_DECAY_CAP = 4
        
        // D-328: neighbour-substituted prefix escalation - a typo early in a long word ("vetmut..." for
        // "vermut...") is invisible to both the literal prefix scan (no shared prefix) and the full-token
        // edit-distance search (too far while still partial). Only trying prefix completions of a corrected
        // prefix reaches it mid-word. L >= 5 keeps the neighbour substitution specific enough to be useful
        // (on a 2-3 letter token it would match far too much); the variant cap bounds the indexed scans.
        private const val MIN_NEIGHBOUR_PREFIX_LENGTH = 5
        private const val MAX_NEIGHBOUR_PREFIX_VARIANTS = 24
    }
}

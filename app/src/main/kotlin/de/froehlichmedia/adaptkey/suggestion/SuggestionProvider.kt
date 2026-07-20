// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Source of word suggestions and autocorrect decisions for the current input token.
 *
 * {@link StubSuggestionProvider} is a placeholder; the SQLite-backed
 * {@link de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider} is the real tier-1
 * predictor. The fastText language detection and the mini-LLM tiers from the specification slot in
 * later behind the same interface without changing the suggestion-bar wiring.
 */
interface SuggestionProvider {
    
    /**
     * Ranked candidates for the given input token.
     *
     * @param input the current composing token (never blank when called)
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @param includeExpensiveFallbacks D-160/D-208: whether the implementation may also run its costlier
     *        searches - D-12's own fuzzy-neighbour matching (cost grows with the token's own length,
     *        unconditionally once long enough to qualify) and, only once those also find nothing, the
     *        expensive last-resort searches (compound reconstruction, wide-budget fuzzy matching). The
     *        per-keystroke hot path passes false (prefix completion only); the default keeps the full
     *        behaviour for every other caller. Implementations without such tiers ignore it.
     * @param isCancelled D-211: polled between candidates during the costlier searches above - true once
     *        this call has been superseded by more recent input, so a background-thread search can stop
     *        partway through instead of finishing pointless work for a token nobody is waiting on any more.
     *        Checked cooperatively, not a hard interrupt (see [de.froehlichmedia.adaptkey.AdaptKeyService]'s
     *        own KDoc on why a forcible thread kill is neither safe nor, on current JDKs, reliably possible
     *        at all). The default never cancels, since a synchronous caller has nothing to poll against.
     * @return candidates sorted by descending [Suggestion.score]; may include or omit [input]
     *         (the controller enforces S-02)
     */
    fun suggestionsFor(
        input: String,
        previousWord: String?,
        includeExpensiveFallbacks: Boolean = true,
        isCancelled: () -> Boolean = { false }
    ): List<Suggestion>
    
    /**
     * @param word the word to check
     * @return true if [word] is a known, valid word (drives the S-05 confirmation highlight)
     */
    fun isKnownWord(word: String): Boolean
    
    /**
     * The replacement an autocorrect would apply to [input] on the next delimiter, if any (S-06).
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @return the proposed replacement, or null when the input is accepted as typed (A-01)
     */
    fun autocorrectFor(input: String, previousWord: String?): String?
    
    /**
     * The known word [input] becomes by restoring only its diacritics (D-48: umlauts / ß are first-class
     * characters). Used to veto an A-05 split so a real umlaut word (`konnen`) is corrected to `können`,
     * not cut into fragments. The default implementation reports no restoration.
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word, for tie-breaking; may be null
     * @return the diacritic-restored known word, or null when there is none
     */
    fun diacriticRestoration(input: String, previousWord: String?): String? = null
    
    /**
     * The autocorrection for [input], but only when it is high-confidence (D-67): a low edit cost, e.g. a
     * single neighbouring-key substitution or better. Used to veto an A-05 split so a split never beats a
     * much safer whole-word correction (`kleiben` -> `kleinen`, not `klei` + `en` - `b`/`n` are adjacent
     * keys). The default falls back to [autocorrectFor], since a placeholder provider has no cost tiers to
     * be more selective with.
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @return the high-confidence autocorrect replacement, or null when none qualifies
     */
    fun highConfidenceCorrection(input: String, previousWord: String?): String? = autocorrectFor(input, previousWord)
    
    /**
     * D-207: [autocorrectFor]'s own result, plus whether that exact candidate also qualifies as
     * [highConfidenceCorrection] - answered from a single search where possible, since autocorrectFor's
     * search space is already a superset of highConfidenceCorrection's tighter cost budget. Replaces the
     * call site that previously called both independently on the same input just to answer a yes/no cost
     * question about the very candidate [autocorrectFor] already found - a whole second candidate search
     * wasted on every commit. The default (no cost tiers to share) simply calls both separately.
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word for n-gram context, or null at a fresh start
     * @return the correction and whether it is high-confidence, or null when [autocorrectFor] is null
     */
    fun bestCorrectionFor(input: String, previousWord: String?): Correction? {
        val word = autocorrectFor(input, previousWord) ?: return null
        return Correction(word, highConfidenceCorrection(input, previousWord) == word)
    }
    
    /**
     * Next-word predictions to show once a word has been committed and no token is being composed yet
     * (D-43): the most likely words to follow [previousWord], by bigram probability. The default returns
     * none, so a non-predicting provider simply leaves the bar empty.
     *
     * @param previousWord the most recently committed word
     * @return predicted next words as ranked suggestions, most likely first
     */
    fun nextWordSuggestions(previousWord: String): List<Suggestion> = emptyList()
    
    /**
     * §44: whether a known [word] should still be treated as correctable in favour of [candidate] - true
     * only when [candidate] is dramatically more frequent, e.g. [word] is a rare dictionary entry that
     * coincidentally matches a typo of a far more common word (`due` blocking a correction to `die` purely
     * for existing in the dictionary at all, regardless of how implausible it is that `due` was intended).
     * Used by both [autocorrectFor]'s own A-01 guard and the raw-coordinate correction fallback, so they
     * agree. The default (no frequency data available) never overrides a known word.
     *
     * @param word the known word A-01 would otherwise protect
     * @param candidate the correction under consideration
     * @return true when [word]'s A-01 protection should be set aside for [candidate]
     */
    fun shouldOverrideKnownWord(word: String, candidate: String): Boolean = false
    
    /**
     * D-202: whether [word] looks like a plausible but incorrectly-unsplit compound (D-116's own noun +
     * Fugenelement + resolvable-rest recognition) - used only to slow down how eagerly such a token is
     * learned as if it were a genuine single word (see [de.froehlichmedia.adaptkey.AdaptKeyService]'s D-37
     * promotion threshold), never to auto-split or suggest anything itself. The default reports no opinion
     * (a placeholder provider has no compound recognition to consult).
     *
     * @param word the word being considered for learning (any case)
     * @return true when [word] resolves to a plausible compound reconstruction
     */
    fun looksLikeUnsplitCompound(word: String): Boolean = false
}

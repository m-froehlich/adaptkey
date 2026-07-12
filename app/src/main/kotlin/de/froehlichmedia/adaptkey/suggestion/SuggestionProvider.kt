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
     * @return candidates sorted by descending [Suggestion.score]; may include or omit [input]
     *         (the controller enforces S-02)
     */
    fun suggestionsFor(input: String, previousWord: String?): List<Suggestion>
    
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
     * Next-word predictions to show once a word has been committed and no token is being composed yet
     * (D-43): the most likely words to follow [previousWord], by bigram probability. The default returns
     * none, so a non-predicting provider simply leaves the bar empty.
     *
     * @param previousWord the most recently committed word
     * @return predicted next words as ranked suggestions, most likely first
     */
    fun nextWordSuggestions(previousWord: String): List<Suggestion> = emptyList()
}

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
}

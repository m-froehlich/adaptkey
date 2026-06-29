package de.froehlichmedia.adaptkey.suggestion

/**
 * Source of word suggestions and autocorrect decisions for the current input token.
 *
 * The scaffold ships only {@link StubSuggestionProvider}; the real three-tier predictor
 * (personal n-gram, fastText language detection, mini-LLM) from the specification replaces
 * this in the dictionary and prediction sessions without changing the suggestion-bar wiring.
 */
interface SuggestionProvider {
    
    /**
     * Ranked candidates for the given input token.
     *
     * @param input the current composing token (never blank when called)
     * @return candidates sorted by descending [Suggestion.score]; may include or omit [input]
     *         (the controller enforces S-02)
     */
    fun suggestionsFor(input: String): List<Suggestion>
    
    /**
     * @param word the word to check
     * @return true if [word] is a known, valid word (drives the S-05 confirmation highlight)
     */
    fun isKnownWord(word: String): Boolean
    
    /**
     * The replacement an autocorrect would apply to [input] on the next delimiter, if any (S-06).
     *
     * @param input the current composing token
     * @return the proposed replacement, or null when the input is accepted as typed
     */
    fun autocorrectFor(input: String): String?
}

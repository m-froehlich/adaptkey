package de.froehlichmedia.adaptkey.prediction

/**
 * A request handed to the tier-3 predictor (§9).
 *
 * Unlike the tier-1 n-gram, the mini-LLM is given the whole sentence and is therefore not subject to
 * the punctuation-resets-context limitation (A-02): [sentence] is the raw text leading up to the token
 * and is not truncated at a comma or other punctuation.
 *
 * @property input the current composing token the prediction is for
 * @property previousWord the most recently committed word, or null at a fresh start
 * @property sentence the full running context before the cursor plus the token (A-02, no reset at
 *           punctuation); the backend may use as much of it as it needs
 */
data class Tier3Request(
    val input: String,
    val previousWord: String?,
    val sentence: String
)

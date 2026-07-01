package de.froehlichmedia.adaptkey.prediction

/**
 * Pure adaptive-learning feedback policy (§9): decides when a tier-3 result should be fed back into the
 * tier-1 n-gram model as a learning signal.
 *
 * The spec's emergent behaviour is that LLM results teach the n-gram, so the LLM is needed less over
 * time. The signal fires only when the mini-LLM taught the n-gram something new: the user committed a
 * word that tier 3 suggested with sufficient confidence and that tier 1 did *not* already know. A word
 * tier 1 already knows needs no reinforcement here (it is learned through the normal commit path), and
 * a word tier 3 did not confidently propose is not attributed to the LLM.
 */
object AdaptiveLearning {
    
    /** The default minimum tier-3 confidence for a committed word to count as an LLM learning signal. */
    const val DEFAULT_MIN_CONFIDENCE = 0.6
    
    /**
     * @param committedWord the word the user actually committed
     * @param tier3 the tier-3 result that was shown for the token
     * @param tier1KnewWord whether the tier-1 n-gram already knew [committedWord] before the commit
     * @param minConfidence the minimum tier-3 confidence required (default [DEFAULT_MIN_CONFIDENCE])
     * @return the word to reinforce in the n-gram, or null when this commit is not an LLM learning signal
     */
    fun learningSignal(
        committedWord: String,
        tier3: Tier3Result,
        tier1KnewWord: Boolean,
        minConfidence: Double = DEFAULT_MIN_CONFIDENCE
    ): String? {
        if (committedWord.isBlank() || tier1KnewWord) {
            return null
        }
        val match = tier3.suggestions.firstOrNull { it.word.equals(committedWord, ignoreCase = true) } ?: return null
        if (match.confidence < minConfidence) {
            return null
        }
        return committedWord
    }
}

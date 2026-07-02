package de.froehlichmedia.adaptkey.prediction

/**
 * Pure parser that turns a raw tier-3 continuation string into ranked candidate words (§9).
 *
 * The mini-LLM produces free text continuing the prompt; this extracts usable word candidates from it:
 * if the continuation begins with letters it completes the current token (`"wor"` + `"ld is"` →
 * `"world"`), otherwise the current token is treated as finished and the continuation's words become
 * next-word predictions. Candidates are de-duplicated, capped, and given rank-descending confidences.
 *
 * The confidences are **rank-derived**, not true model probabilities — a text-only continuation carries
 * no token scores. A scored decode (top-k over the first-token logits) can supply real confidences later
 * without changing this contract. Kept Android-free and deterministic so it is fully unit-testable.
 */
object Tier3ResponseParser {
    
    /**
     * Parses a continuation into ranked candidates.
     *
     * @param input the current composing token the continuation follows
     * @param generated the raw text the model produced after the prompt
     * @param maxCandidates the maximum number of candidates to return (>= 0)
     * @return the candidate words, most confident first; empty when nothing usable was produced
     */
    fun parse(input: String, generated: String, maxCandidates: Int): List<Tier3Suggestion> {
        require(maxCandidates >= 0) { "maxCandidates must not be negative: $maxCandidates" }
        if (maxCandidates == 0) {
            return emptyList()
        }
        val words = ArrayList<String>()
        val firstBoundary = generated.indexOfFirst { !it.isLetter() }
        val head = if (firstBoundary < 0) generated else generated.substring(0, firstBoundary)
        if (head.isNotEmpty()) {
            // A letter-initial continuation completes the current token; otherwise it stands alone.
            words.add(if (input.isNotEmpty()) input + head else head)
        }
        val rest = if (firstBoundary < 0) "" else generated.substring(firstBoundary)
        for (word in rest.split(NON_LETTER)) {
            if (word.isNotEmpty()) {
                words.add(word)
            }
        }
        val unique = words.distinct().take(maxCandidates)
        return unique.mapIndexed { index, word -> Tier3Suggestion(word, rankConfidence(index, unique.size)) }
    }
    
    private fun rankConfidence(index: Int, size: Int): Double {
        return (size - index).toDouble() / size
    }
    
    private val NON_LETTER = Regex("[^\\p{L}]+")
}

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.suggestion.Suggestion

/**
 * Pure measure of how confident the tier-1 n-gram is about its top prediction (§9 / C-06).
 *
 * Confidence is the dominance of the leading candidate: the top score divided by the total score of
 * all candidates. One clearly-leading candidate approaches 1.0 (very confident), many similarly-scored
 * candidates share the mass and yield a low value (uncertain — a good moment to consult tier 3), and an
 * empty candidate list is 0.0 (no idea at all). This is the rank-margin signal the C-06 threshold gates
 * on; it depends only on the tier-1 scores, so it is fully unit-testable.
 */
object Tier1Confidence {
    
    /**
     * Computes the tier-1 confidence for a ranked candidate list.
     *
     * @param candidates the tier-1 candidates (any order; only their scores matter)
     * @return the dominance of the top candidate in [0, 1]; 0.0 when there are no positive scores
     */
    fun confidence(candidates: List<Suggestion>): Double {
        val scores = candidates.map { it.score }.filter { it > 0.0 }
        if (scores.isEmpty()) {
            return 0.0
        }
        val total = scores.sum()
        val top = scores.max()
        return (top / total).coerceIn(0.0, 1.0)
    }
}

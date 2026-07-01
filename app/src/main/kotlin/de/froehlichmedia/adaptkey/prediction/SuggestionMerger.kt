package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.suggestion.Suggestion

/**
 * Pure combination of tier-1 n-gram candidates with tier-3 mini-LLM candidates into a single ranked
 * suggestion list (§9).
 *
 * The two tiers score on different scales — tier 1 by raw frequency, tier 3 by a [0, 1] confidence — so
 * the tier-1 scores are normalised to [0, 1] against their own maximum before the two are added. A word
 * offered by both tiers accumulates both contributions, so agreement between the n-gram and the LLM
 * floats a word to the top; a word offered by only one tier keeps that tier's contribution alone.
 * Sorting is stable and the encounter order (tier-1 candidates first, then tier-3-only ones) breaks
 * ties deterministically, which keeps the bar predictable.
 */
object SuggestionMerger {
    
    /**
     * Merges the two candidate lists.
     *
     * @param tier1 the tier-1 candidates (frequency-scored)
     * @param tier3 the tier-3 candidates (confidence-scored, in [0, 1])
     * @param limit the maximum number of merged candidates to return
     * @param tier3Weight the relative weight given to the tier-3 confidence contribution (default 1.0)
     * @return the merged candidates, most probable first, capped to [limit]
     */
    fun merge(
        tier1: List<Suggestion>,
        tier3: List<Tier3Suggestion>,
        limit: Int,
        tier3Weight: Double = 1.0
    ): List<Suggestion> {
        if (limit <= 0) {
            return emptyList()
        }
        val maxTier1 = tier1.maxOfOrNull { it.score }?.takeIf { it > 0.0 } ?: 1.0
        // LinkedHashMap preserves encounter order: tier-1 words first, tier-3-only words appended.
        val combined = LinkedHashMap<String, Double>()
        for (candidate in tier1) {
            combined[candidate.word] = (combined[candidate.word] ?: 0.0) + candidate.score / maxTier1
        }
        for (candidate in tier3) {
            combined[candidate.word] = (combined[candidate.word] ?: 0.0) + candidate.confidence * tier3Weight
        }
        return combined.entries
            .map { Suggestion(it.key, it.value) }
            .sortedByDescending { it.score }
            .take(limit)
    }
}

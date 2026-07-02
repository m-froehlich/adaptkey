// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.suggestion.Suggestion

/**
 * The outcome of running the tier-3 orchestration for one token.
 *
 * @property suggestions the suggestions to display: the tier-1 list unchanged when tier 3 was not
 *           activated, otherwise the tier-1 + tier-3 merge
 * @property capitalisation the §6 rule-6 capitalisation proposal, or null when none was made
 * @property activated whether tier 3 was actually consulted
 * @property tier1Confidence the computed tier-1 confidence (the value the C-06 threshold was gated on)
 * @property tier3 the raw tier-3 result (empty when not activated), for the adaptive-learning feedback
 */
data class Tier3Outcome(
    val suggestions: List<Suggestion>,
    val capitalisation: CapitalisationProposal?,
    val activated: Boolean,
    val tier1Confidence: Double,
    val tier3: Tier3Result
)

/**
 * Pure orchestration of the three-tier prediction pipeline (§9), composing the tier-1 confidence
 * measure, the C-06 activation policy, the tier-3 backend and the tier-1/tier-3 merge.
 *
 * The Android service supplies the tier-1 candidates and the context; this class decides whether the
 * mini-LLM is worth waking (per the C-06 threshold and backend availability), consults it when so, and
 * returns the merged suggestions plus the §6 capitalisation proposal. With the default
 * [NoopTier3Provider] the backend is never available, so [predict] always returns the tier-1 list
 * untouched and no proposal — the pipeline is present but completely inert until a real backend is
 * installed.
 *
 * @property provider the tier-3 backend (defaults to the inert [NoopTier3Provider])
 */
class Tier3Orchestrator(private val provider: Tier3Provider = NoopTier3Provider) {
    
    /**
     * Runs the pipeline for one token.
     *
     * @param input the current composing token
     * @param previousWord the most recently committed word, or null
     * @param sentence the full-sentence context for tier 3 (A-02, no reset at punctuation)
     * @param tier1 the ranked tier-1 candidates
     * @param threshold the configured C-06 activation threshold
     * @param limit the maximum number of merged suggestions to return
     * @return the [Tier3Outcome] for this token
     */
    fun predict(
        input: String,
        previousWord: String?,
        sentence: String,
        tier1: List<Suggestion>,
        threshold: LlmActivationThreshold,
        limit: Int
    ): Tier3Outcome {
        val confidence = Tier1Confidence.confidence(tier1)
        if (!Tier3Activation.shouldActivate(confidence, threshold, provider.isAvailable)) {
            return Tier3Outcome(tier1, null, activated = false, tier1Confidence = confidence, tier3 = Tier3Result.EMPTY)
        }
        val result = provider.predict(Tier3Request(input, previousWord, sentence))
        val merged = SuggestionMerger.merge(tier1, result.suggestions, limit)
        return Tier3Outcome(merged, result.capitalisation, activated = true, tier1Confidence = confidence, tier3 = result)
    }
}

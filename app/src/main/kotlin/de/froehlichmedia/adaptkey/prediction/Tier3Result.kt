// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * A single tier-3 candidate word with the backend's confidence in it.
 *
 * @property word the predicted word
 * @property confidence the model confidence in [0, 1]; higher is more certain
 */
data class Tier3Suggestion(
    val word: String,
    val confidence: Double
)

/**
 * A high-certainty capitalisation proposal from tier 3 (§6 hierarchy rule 6).
 *
 * The mini-LLM may capitalise a word only when the sentence context is unambiguously nominal and its
 * confidence is high — never as a default. This carries the proposed cased form and that confidence;
 * whether it is actually applied is decided by the pure [HighCertaintyCapitalisation] policy.
 *
 * @property word the proposed cased form of the token
 * @property confidence the model confidence in [0, 1]
 */
data class CapitalisationProposal(
    val word: String,
    val confidence: Double
)

/**
 * The output of a tier-3 prediction: ranked candidate words and an optional high-certainty
 * capitalisation proposal (§6 rule 6).
 *
 * @property suggestions the candidate words, most confident first
 * @property capitalisation an optional §6 rule-6 capitalisation proposal for the requested token, or
 *           null when the backend makes none
 */
data class Tier3Result(
    val suggestions: List<Tier3Suggestion>,
    val capitalisation: CapitalisationProposal? = null
) {
    
    companion object {
        
        /** The empty result: no candidates and no capitalisation proposal. */
        val EMPTY = Tier3Result(emptyList(), null)
    }
}

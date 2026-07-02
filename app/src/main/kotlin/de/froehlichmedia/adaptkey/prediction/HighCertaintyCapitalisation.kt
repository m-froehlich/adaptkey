// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * Pure gate for the §6 hierarchy rule 6 (the "LLM exception"): the mini-LLM may capitalise a word only
 * when the sentence context is unambiguously nominal *and* its confidence is high — never as a default.
 *
 * This turns a tier-3 [CapitalisationProposal] into the single boolean the capitalisation engine needs
 * ("force this token's first letter upper-case"). It fires only when the proposal is confident enough,
 * actually capitalises the token, and refers to that very token — so it can lift an otherwise-lowercased
 * ambiguous or unknown word to upper-case, but it can never lower-case anything (the engine only reads
 * the true value in its upper-casing branch).
 */
object HighCertaintyCapitalisation {
    
    /** The default minimum confidence for the LLM to be allowed to capitalise (§6 rule 6). */
    const val DEFAULT_MIN_CONFIDENCE = 0.85
    
    /**
     * Decides whether the tier-3 proposal should force [word] to be capitalised.
     *
     * @param proposal the tier-3 capitalisation proposal, or null when there is none
     * @param word the word about to be cased (the autocorrected or typed form)
     * @param minConfidence the minimum confidence required (default [DEFAULT_MIN_CONFIDENCE])
     * @return true only when the proposal is confident, capitalises [word] and refers to [word]
     */
    fun forcesUpper(
        proposal: CapitalisationProposal?,
        word: String,
        minConfidence: Double = DEFAULT_MIN_CONFIDENCE
    ): Boolean {
        if (proposal == null || word.isEmpty()) {
            return false
        }
        if (proposal.confidence < minConfidence) {
            return false
        }
        val proposesUpper = proposal.word.firstOrNull()?.isUpperCase() == true
        return proposesUpper && proposal.word.equals(word, ignoreCase = true)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import kotlin.math.ln

/**
 * D-391: a single `[0, 1]` confidence score for a [TokenRepair.tryFuseAcrossSpace] fusion candidate -
 * [AutoMergeAggressiveness]'s own thresholds are compared against this.
 *
 * A genuinely different question from [CorrectionConfidence]: there is no edit cost here (a fusion is not a
 * substitution - two already-committed words are replaced by one, not one candidate chosen over another by
 * how close an edit it is), so the only real signal is *how frequent/well-established the fused candidate
 * itself is* - a rare fused match is far more likely to be two genuinely separate, deliberately-typed words
 * that merely happen to also spell something else when glued together (the false-positive this whole
 * mechanism must guard against), while a common one is real, everyday evidence of a swallowed connector
 * letter. Mirrors [CorrectionConfidence.frequencyFactor]'s own noun/non-noun reference-point split (D-227's
 * finding: a rare noun in this Wikipedia-derived corpus is disproportionately a proper-noun artefact) for
 * the same reason, but with its own, more conservative reference points - a wrong fusion rewrites
 * already-committed, finished text, a materially higher-stakes mistake than an ordinary same-token
 * autocorrect substitution.
 *
 * Not yet calibrated against any real regression corpus (unlike [CorrectionConfidence]'s own carefully
 * worked numbers) - both reference points below are a considered starting point, easy to retune once real
 * device feedback exists, the same status this project already accepts for its other freshly-introduced
 * thresholds.
 */
object MergeConfidence {
    
    /**
     * Confidence that [frequency] (the fused candidate's own dictionary/learned frequency) is genuine,
     * well-established evidence rather than a coincidental dictionary match.
     *
     * @param frequency the fused candidate's own frequency
     * @param isNounLike whether the fused candidate is tagged [PartOfSpeech.NOUN] / [PartOfSpeech.PROPER_NOUN]
     * @return the confidence in `[0, 1]`
     */
    fun forFusedCandidate(frequency: Long, isNounLike: Boolean): Double {
        val reference = if (isNounLike) NOUN_REFERENCE_FREQUENCY else REFERENCE_FREQUENCY
        return (ln(frequency.coerceAtLeast(1).toDouble()) / ln(reference)).coerceIn(0.0, 1.0)
    }
    
    /**
     * D-391: the non-noun fused-candidate frequency at which [forFusedCandidate] saturates to 1.0 - well
     * above [CorrectionConfidence]'s own equivalent [CorrectionConfidence]-internal reference (25), since a
     * fusion has no edit-cost signal at all to lean on alongside frequency, unlike an ordinary correction.
     */
    private const val REFERENCE_FREQUENCY = 300.0
    
    /**
     * D-391: the noun-tagged fused-candidate frequency at which [forFusedCandidate] saturates to 1.0 -
     * mirrors [CorrectionConfidence]'s own noun-vs-non-noun split (a rare noun in the bundled Wikipedia
     * corpus is disproportionately a proper-noun artefact), scaled up alongside [REFERENCE_FREQUENCY] for
     * the same added-conservatism reason.
     */
    private const val NOUN_REFERENCE_FREQUENCY = 8000.0
}

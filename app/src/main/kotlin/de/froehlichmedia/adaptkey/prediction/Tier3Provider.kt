// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * Tier-3 predictor abstraction (§9): the on-device mini-LLM that is consulted only when the tier-1
 * n-gram confidence falls below the C-06 activation threshold.
 *
 * This is a pluggable seam so the heavy, native, instrumented-only ONNX Runtime backend can be added
 * later as a thin Android layer without touching the pure orchestration. The default backend is
 * [NoopTier3Provider], which reports itself unavailable so tier 3 is never activated and the keyboard
 * behaves exactly as the tier-1-only build does.
 */
interface Tier3Provider {
    
    /**
     * @return true when a real model is loaded and ready; a false value short-circuits activation so
     *         no request is ever built or dispatched
     */
    val isAvailable: Boolean
    
    /**
     * Predicts continuations for the request. Only called when [isAvailable] is true and the tier-1
     * confidence is below the configured threshold.
     *
     * @param request the token, previous word and full-sentence context (A-02)
     * @return the ranked candidates and any high-certainty capitalisation proposal
     */
    fun predict(request: Tier3Request): Tier3Result
    
    /**
     * D-404: determines [Tier3FamilyRequest.word]'s category and its whole inflectional family, so the
     * caller can learn them all together, linked - the "with LLM, always learn the whole family" design.
     * Only ever called when [isAvailable] is true. The default implementation returns
     * [Tier3FamilyResult.EMPTY], so [NoopTier3Provider] and any other backend not yet implementing this
     * task need no override of their own.
     *
     * @param request the word (and optional sentence context) to analyse
     * @return the determined family, or [Tier3FamilyResult.EMPTY] when nothing usable was produced
     */
    fun predictFamily(request: Tier3FamilyRequest): Tier3FamilyResult = Tier3FamilyResult.EMPTY
}

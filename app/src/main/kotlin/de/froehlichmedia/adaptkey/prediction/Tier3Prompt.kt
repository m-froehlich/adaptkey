// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * Pure builder for the text prompt handed to the tier-3 mini-LLM (§9).
 *
 * The model is used as a plain continuation predictor: it is fed the running context as-is and asked to
 * continue it, so the "prompt" is simply the recent text ending at the cursor. Per A-02 the context is
 * the full running sentence (already assembled by the caller, not truncated at punctuation); this only
 * caps it to a trailing window so a very long field cannot blow up per-activation latency. Kept pure so
 * the windowing is unit-testable independently of any model.
 */
object Tier3Prompt {
    
    /** Default trailing-context window in characters (bounds per-activation compute). */
    const val DEFAULT_MAX_CONTEXT_CHARS = 256
    
    /**
     * Builds the continuation prompt from a request.
     *
     * @param request the tier-3 request (its [Tier3Request.sentence] is the full running context, A-02)
     * @param maxContextChars the trailing-context window (must be positive)
     * @return the context to feed the model, capped to the last [maxContextChars] characters
     */
    fun build(request: Tier3Request, maxContextChars: Int = DEFAULT_MAX_CONTEXT_CHARS): String {
        require(maxContextChars > 0) { "maxContextChars must be positive: $maxContextChars" }
        val sentence = request.sentence
        return if (sentence.length <= maxContextChars) sentence else sentence.takeLast(maxContextChars)
    }
}

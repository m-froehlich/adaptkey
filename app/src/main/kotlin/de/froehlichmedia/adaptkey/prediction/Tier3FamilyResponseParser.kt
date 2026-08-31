// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech

/**
 * Pure parser that turns a raw D-404 family-learning continuation into a [Tier3FamilyResult] (see
 * [Tier3FamilyPrompt] for the exact `KEY=value` answer shape the prompt asks for).
 *
 * Deliberately generous: a small (360M-parameter) instruction-tuned model will not always reproduce the
 * requested format exactly, so every field is extracted independently via its own regex rather than
 * requiring the whole line to parse as one unit - a missing/garbled `WORTART=`/`FORMEN=` field simply
 * leaves that part of the result empty/null instead of discarding an otherwise-usable lemma. Only the
 * lemma is load-bearing: without a plausible one, the whole result is discarded ([Tier3FamilyResult.EMPTY])
 * - matching this whole feature's stance that a partial/garbled generation must never become a partial/
 * garbled write (see [de.froehlichmedia.adaptkey.dictionary.LearnedLemmaLinking]'s own "never guess"
 * framing for the non-LLM path this mirrors in spirit, even though this path *is* generative).
 */
object Tier3FamilyResponseParser {
    
    /**
     * @param generated the raw text the model produced after [Tier3FamilyPrompt.build]'s own priming
     *        prefix (the prompt ends mid-answer, right after `"GRUNDFORM="`, so [generated] itself starts
     *        with the lemma's value, not the key)
     * @return the parsed result, or [Tier3FamilyResult.EMPTY] when no plausible lemma could be found
     */
    fun parse(generated: String): Tier3FamilyResult {
        // The prompt primes "GRUNDFORM=" before handing control to the model, so generated picks up right
        // after that key - prepend it back so every KEY=value field, lemma included, is found by the same
        // generic per-field pattern below.
        val text = "${Tier3FamilyPrompt.KEY_LEMMA}=$generated"
        val lemma = LEMMA_PATTERN.find(text)?.groupValues?.get(1)?.trim()
        if (lemma.isNullOrEmpty() || lemma.length < MIN_WORD_LENGTH || !lemma.all { it.isLetter() }) {
            return Tier3FamilyResult.EMPTY
        }
        val category = CATEGORY_PATTERN.find(text)?.groupValues?.get(1)?.let { token ->
            runCatching { PartOfSpeech.valueOf(token.trim().uppercase()) }.getOrNull()
        }
        val formsRaw = FORMS_PATTERN.find(text)?.groupValues?.get(1).orEmpty()
        val forms = formsRaw.split(',')
            .map { it.trim() }
            .filter { it.length >= MIN_WORD_LENGTH && it.all { c -> c.isLetter() } }
        val family = (listOf(lemma) + forms).distinctBy { it.lowercase() }.take(MAX_FAMILY_SIZE)
        return Tier3FamilyResult(category, lemma, family)
    }
    
    // A single letter is never a real word to learn (mirrors AdaptKeyService's own MIN_LEARN_LENGTH gate).
    private const val MIN_WORD_LENGTH = 2
    
    // Bounds a degenerate/repetitive generation from flooding the learned lexicon in one call.
    private const val MAX_FAMILY_SIZE = 16
    
    private val LEMMA_PATTERN = Regex("${Tier3FamilyPrompt.KEY_LEMMA}\\s*=\\s*([^\\n;,]+)", RegexOption.IGNORE_CASE)
    private val CATEGORY_PATTERN = Regex("${Tier3FamilyPrompt.KEY_CATEGORY}\\s*=\\s*(\\w+)", RegexOption.IGNORE_CASE)
    private val FORMS_PATTERN = Regex("${Tier3FamilyPrompt.KEY_FORMS}\\s*=\\s*([^\\n]*)", RegexOption.IGNORE_CASE)
}

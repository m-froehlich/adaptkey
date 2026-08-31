// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * Pure builder for the D-404 family-learning prompt (see [Tier3FamilyRequest]/[Tier3FamilyResult]).
 *
 * Unlike [Tier3Prompt] (a plain continuation prompt - the model just continues running text), this asks
 * an explicit question and requests a fixed, easy-to-parse answer shape, since [Tier3FamilyResponseParser]
 * needs to reliably pull a lemma, a category and a list of forms back out of free-generated text from a
 * small (360M-parameter) model. Kept deliberately rigid (one line, `KEY=value` pairs, `;`-separated) rather
 * than natural language or JSON - both are harder for a small model to reproduce exactly, and a strict
 * format fails obviously (nothing matches) rather than subtly (a plausible-looking but wrong parse).
 */
object Tier3FamilyPrompt {
    
    /**
     * Builds the prompt for [request].
     *
     * @param request the family request (word plus optional sentence context)
     * @return the prompt to feed the model
     */
    fun build(request: Tier3FamilyRequest): String {
        val context = request.sentence.trim()
        val contextLine = if (context.isNotEmpty() && context != request.word) {
            "Satz: $context\n"
        } else {
            ""
        }
        return "${contextLine}Wort: ${request.word}\n" +
            "Nenne die Grundform, die Wortart ($CATEGORY_TOKENS) und die wichtigsten Flexionsformen " +
            "des Wortes in genau diesem Format, ohne weiteren Text:\n" +
            "$KEY_LEMMA=<Grundform>; $KEY_CATEGORY=<Wortart>; $KEY_FORMS=<Form1>,<Form2>,...\n" +
            "$KEY_LEMMA="
    }
    
    /** The `KEY=value` key naming the answer's lemma field. */
    const val KEY_LEMMA = "GRUNDFORM"
    
    /** The `KEY=value` key naming the answer's category field. */
    const val KEY_CATEGORY = "WORTART"
    
    /** The `KEY=value` key naming the answer's family-forms field. */
    const val KEY_FORMS = "FORMEN"
    
    private const val CATEGORY_TOKENS = "NOUN, VERB, ADJECTIVE, PREPOSITION, PROPER_NOUN oder OTHER"
}

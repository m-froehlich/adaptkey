// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-404 (tier 3): extremely conservative, lookup-only base-form linking for the Learned Words list - never
 * guesses or writes a new "base form" row, only checks whether a plausible base-form candidate of a word
 * being learned already exists as its own entry in the learned lexicon (something the user genuinely typed
 * and had learned before). Mirrors [RegularVerbInflection]'s own "strip a closed set of endings, check the
 * dictionary" pattern, but returns the matching candidate instead of a boolean, and adds noun-style endings
 * of its own (an inflected noun form like a genitive/plural is exactly what D-404 exists to consolidate).
 *
 * Deliberately tries both noun- and verb-style endings unconditionally rather than branching on a suspected
 * part of speech first (a capitalised word tried only against noun endings, a lower-case one only against
 * verb endings) - a coincidental match against an unrelated already-learned word is possible in principle
 * (the same accepted trade-off [RegularVerbInflection] already documents for its own narrower case), but it
 * only ever links two words the user has genuinely typed and had learned already, never fabricates an entry
 * - so the low-probability cost of a stray link stays far cheaper than the complexity of a category-gated
 * branch, and covers the "NOUN,VERB-suspected" case the design calls for without any extra logic.
 *
 * [findLemma] is the forward direction (a newly-typed word is itself the inflected form, and an already-
 * learned base is being searched for); [candidateInflections] is the reverse (a newly-typed word is itself
 * the base, and any already-learned word matching one of its own inflected forms should be linked back to
 * it) - both directions are needed for the D-404 opening example: "Hundes" already learned standalone, then
 * "Hund" learned afterwards must still end up linked, even though "Hund" itself is the base, not the
 * inflection.
 */
object LearnedLemmaLinking {
    
    // Genitive/dative/plural-style endings glued onto a nominative singular stem. Order does not affect
    // correctness (each candidate is independently checked; a wrong split simply fails the lookup) - listed
    // longest-first purely for readability, same convention as RegularVerbInflection.ENDINGS.
    private val NOUN_ENDINGS = listOf("nen", "ern", "en", "es", "er", "e", "n", "s")
    
    /**
     * @param word the word being learned, in any case
     * @param isLearnedWord looks up whether a candidate base form already exists as its own entry in the
     *        learned lexicon (never the bundled dictionary - a word already known there is never learned at
     *        all, see [de.froehlichmedia.adaptkey.AdaptKeyService.learnWord]'s own D-264 skip logic)
     * @return the first matching already-learned base-form candidate, or null when none is found
     */
    fun findLemma(word: String, isLearnedWord: (String) -> Boolean): String? {
        val lower = word.lowercase()
        for (ending in NOUN_ENDINGS) {
            // D-404: an explicit endsWith check, not just a length guard - String.removeSuffix() is a no-op
            // (returns the string unchanged) when it does not actually end with the given suffix, which
            // without this check would silently turn the "stem" into the whole original word and risk a
            // false self-match (isLearnedWord(word) is often true here - see this method's own callers,
            // several of which check an already-learned word being reinforced).
            if (!lower.endsWith(ending) || lower.length <= ending.length) {
                continue
            }
            val stem = lower.removeSuffix(ending)
            if (isLearnedWord(stem)) {
                return stem
            }
        }
        // RegularVerbInflection.candidateInfinitives() has no equivalent risk: it always appends "en" after
        // stripping, so a candidate can never equal the original token even when a given ending fails to
        // match (a no-op strip + "en" still differs from the token by construction).
        return RegularVerbInflection.candidateInfinitives(lower).firstOrNull(isLearnedWord)
    }
    
    /**
     * The reverse of [findLemma]: every plausible inflected form of [base] - candidate noun forms
     * ([NOUN_ENDINGS] appended directly) plus, when [base] itself looks like an infinitive (ends in `en`),
     * candidate verb-personal forms (its own stem, per [RegularVerbInflection.ENDINGS]). The caller checks
     * each candidate against the already-learned lexicon and links only the ones that already exist there -
     * this method itself never performs any lookup or write, purely generates candidates.
     *
     * @param base the word just learned, suspected of being a base form, in any case
     * @return the candidate inflected forms, lower-cased
     */
    fun candidateInflections(base: String): List<String> {
        val lower = base.lowercase()
        val nounForms = NOUN_ENDINGS.map { lower + it }
        val verbForms = if (lower.length > 2 && lower.endsWith("en")) {
            val stem = lower.removeSuffix("en")
            RegularVerbInflection.ENDINGS.map { stem + it }
        } else {
            emptyList()
        }
        return nounForms + verbForms
    }
}

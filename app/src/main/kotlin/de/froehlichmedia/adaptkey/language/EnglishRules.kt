// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-number (this round): the English [LanguageRules] implementation - like
 * [de.froehlichmedia.adaptkey.language.FrenchRules], only [bundledConfusablesBlacklist] and, since
 * the english-dictionary-quality plan's pinning round, [pinnedContinuationStopwords] are populated;
 * the six German-specific compounding/inflection hooks stay the same documented no-op
 * [NoOpLanguageRules] gives every other language, and [decimalCommaGluesDigits]/[timeSuggestionWord] are
 * plain "does not apply" facts for English (period decimals, no S-08-style trailing time word).
 *
 * [bundledConfusablesBlacklist] became possible to build properly only once
 * [de.froehlichmedia.adaptkey.suggestion.KeyboardProximity] stopped being hardcoded to QWERTZ (see that
 * interface's own KDoc) - run against the real QWERTY grid
 * ([de.froehlichmedia.adaptkey.suggestion.KeyboardProximityQwerty]) via the same D-304/D-330-followup
 * method German's own list uses (`dictionaries/confusables_scan.py`, scoring every keyboard-adjacent word
 * pair with `CorrectionConfidence.forKnownWordOverride`'s own formula). The bundled `en/dict.tsv` scan
 * found ~1,384 candidate pairs clearing even the low chip-offer bar - the overwhelming majority are
 * genuine, if rare, real words (`fir`/`otter`/`nave`) whose own frequency is simply too low relative to a
 * very common QWERTY-adjacent neighbour (`for`/`other`/`have`); the correct fix for those is a frequency
 * correction (matching German's own D-330 `dein`/`sein` precedent), not blacklisting, and is deliberately
 * **not** attempted in this round - see `AdaptKey-Progress.md`'s own open TODO. This list is scoped
 * narrowly to the handful of score-1.0 pairs whose *risky* side is not, as far as this round's own review
 * could confirm, a real standalone English word at all (the same "corpus/tokeniser noise, not a genuine
 * cross-language leak this time" shape A-04's existing German list already covers for a different cause) -
 * a small, deliberately conservative first pass, not claimed exhaustive.
 */
object EnglishRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** English has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    override fun bundledConfusablesBlacklist(): Set<String> = BUNDLED_CONFUSABLES_BLACKLIST
    
    /** English writes decimals with a point ("3.14"), not a comma. */
    override fun decimalCommaGluesDigits(): Boolean = false
    
    /**
     * D-572: English's function words after which D-503's n-gram pinning is switched off entirely -
     * see [LanguageRules.pinnedContinuationStopwords] for the mechanism and the rationale.
     *
     * The English shape of the D-503-followup problem the plan (`english-dictionary-quality.md`)
     * documented: after `the`/`of`/`and`, the Wiktionary-corpus nouns pin the bar with corpus-generic
     * continuations - the same flood `GermanRules.PINNING_STOPWORDS` was built for, previously
     * unsolved for English because [EnglishRules] inherited the empty default.
     *
     * Scope, mirroring German's Variant B (the left-side gate - only the previous word decides):
     * articles, prepositions, conjunctions, the demonstrative determiners, and - deliberately
     * BROADER than German's own list - the pronouns/possessives and the auxiliary/copula/negation
     * family (`is`, `was`, `be`, `not`, ...). The plan's list names the pronouns explicitly: unlike
     * German, where "ich -> bin/habe" is a genuine personal signal worth pinning, the English
     * Wiktionary-corpus continuations after pronouns are equally corpus-generic ("they have been",
     * "it was the"), and pinning them floods the bar with encyclopedic-register nouns exactly like
     * an article does. The modals (`will`, `would`, `can`, `could`, `should`, `may`, `might`,
     * `must`) are deliberately left pinnable - mirroring the German pronoun rationale, their
     * continuations carry a real personal signal ("I'll be there" vs. the corpus's generic
     * "will be/have"). Stored lower-cased; the caller matches case-insensitively and folds
     * diacritics, so only genuinely lowercase English function words belong here.
     */
    override fun pinnedContinuationStopwords(): Set<String> = PINNING_CONTINUATION_STOPWORDS
    
    /** D-572: see [pinnedContinuationStopwords]. Stored lower-cased. */
    private val PINNING_CONTINUATION_STOPWORDS = setOf(
        // Articles.
        "the", "a", "an",
        // Coordinating and subordinating conjunctions.
        "and", "or", "but", "nor", "so", "yet", "if", "because", "while", "when", "than", "that",
        "whether",
        // Prepositions.
        "of", "in", "to", "for", "with", "on", "at", "by", "from", "as", "about", "into", "over",
        "after", "under", "between", "through", "during", "before", "against", "without", "within",
        "along", "across", "behind", "beyond", "off", "up", "down", "out", "near", "past", "since",
        "until", "till", "upon", "among", "around", "toward", "towards", "onto", "per", "via",
        "beneath", "beside", "besides", "despite", "except", "inside", "outside", "throughout",
        "underneath", "amid", "aboard", "atop", "lest",
        // Demonstrative determiners.
        "this", "that", "these", "those",
        // Pronouns and possessives (deliberately included, unlike German's own list - see KDoc).
        "it", "its", "itself", "they", "them", "their", "theirs", "we", "us", "our", "ours",
        "you", "your", "yours", "he", "him", "his", "himself", "she", "her", "hers", "herself",
        // Auxiliary / copula / negation family.
        "is", "are", "was", "were", "be", "been", "being", "am", "has", "have", "had", "does",
        "do", "did", "not",
    )
    
    private val BUNDLED_CONFUSABLES_BLACKLIST = setOf("ij", "iz", "iy", "ae", "ne")
}

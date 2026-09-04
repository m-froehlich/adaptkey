// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-446: the Italian [LanguageRules] implementation - built via the Language Contribution Guide's own §8
 * real-corpus/real-lexicon pipeline, directly to the method [FrenchRules]/[SpanishRules]/[PortugueseRules]
 * were built/rebuilt to. Like every other implemented language, only three of the nine hooks are naively
 * fillable without real Italian grammar-engineering work: [decimalCommaGluesDigits] and
 * [timeSuggestionWord] are plain locale facts, [bundledConfusablesBlacklist] would reuse a real confusables
 * scan (see that function's own KDoc for why it stays empty for now). The remaining six hooks encode
 * German-specific compounding/inflection grammar that has no Italian equivalent implemented yet - left as
 * the same "does not apply" no-op [NoOpLanguageRules] gives every other unimplemented language.
 */
object ItalianRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Italian has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-446: `dictionaries/confusables_scan.py dictionaries/it/dict.tsv qwerty` is directly runnable
     * (Italian already uses plain QWERTY, no dedicated layout prerequisite needed) but deliberately not
     * curated into a blacklist this round - the same "cannot confidently separate a genuine short Italian
     * word/abbreviation from real corpus noise without native fluency" reasoning [FrenchRules]/
     * [SpanishRules]/[PortugueseRules] document for their own scans applies here too - left for a
     * native-speaker-guided pass (see the Language Contribution Guide's own step 11) rather than guessed at.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Italian writes decimals with a comma ("3,14"), like German/French/Spanish/Portuguese. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

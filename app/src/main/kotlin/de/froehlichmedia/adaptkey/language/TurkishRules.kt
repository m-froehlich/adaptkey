// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-449: the Turkish [LanguageRules] implementation - built via the Language Contribution Guide's own §8
 * real-corpus/real-lexicon pipeline. Like every other implemented language, only three of the nine hooks
 * are naively fillable without real Turkish grammar-engineering work: [decimalCommaGluesDigits] and
 * [timeSuggestionWord] are plain locale facts, [bundledConfusablesBlacklist] would reuse a real confusables
 * scan (see that function's own KDoc for why it stays empty for now). The remaining six hooks encode
 * German-specific compounding/inflection grammar that has no Turkish equivalent implemented yet - left as
 * the same "does not apply" no-op [NoOpLanguageRules] gives every other unimplemented language.
 *
 * None of these nine hooks touch capitalisation - Turkish's own dotted/dotless İ/I distinction (originally
 * left as a still-open design question, see `AdaptKey-Progress.md`'s D-449 entry) is resolved separately, via
 * [de.froehlichmedia.adaptkey.capitalisation.TurkishCasingRules] (D-449-followup), a dedicated
 * [de.froehlichmedia.adaptkey.capitalisation.CasingRules] implementation - not in this object, since it is a
 * character-casing convention, not a compounding/inflection grammar rule.
 */
object TurkishRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Turkish has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-449: `dictionaries/confusables_scan.py dictionaries/tr/dict.tsv qwerty` is directly runnable
     * (Turkish already uses plain QWERTY - the dominant real-world "Turkish Q" convention, no dedicated
     * layout prerequisite needed) but deliberately not curated into a blacklist this round - the same
     * "cannot confidently separate a genuine short Turkish word/abbreviation from real corpus noise without
     * native fluency" reasoning every other implemented language documents for its own scan applies here
     * too - left for a native-speaker-guided pass (see the Language Contribution Guide's own step 11)
     * rather than guessed at.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Turkish writes decimals with a comma ("3,14"), like every other implemented language here. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

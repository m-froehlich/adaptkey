// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450: the Swedish [LanguageRules] implementation - built via the Language Contribution Guide's own §8
 * real-corpus/real-lexicon pipeline, from the English Wiktionary's own coverage of Swedish (no native
 * edition exists on kaikki.org, confirmed directly). Like every other implemented language, only three of
 * the nine hooks are naively fillable without real Swedish grammar-engineering work: [decimalCommaGluesDigits]
 * and [timeSuggestionWord] are plain locale facts, [bundledConfusablesBlacklist] would reuse a real
 * confusables scan (see that function's own KDoc for why it stays empty for now). The remaining six hooks
 * encode German-specific compounding/inflection grammar that has no Swedish equivalent implemented yet -
 * left as the same "does not apply" no-op [NoOpLanguageRules] gives every other unimplemented language, even
 * though Swedish (like German) is itself a genuinely compounding language - building real Swedish
 * compound-splitting grammar is a separate, not-yet-started effort, not something this naive-fill round
 * attempts.
 */
object SwedishRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Swedish has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450: `dictionaries/confusables_scan.py dictionaries/sv/dict.tsv qwerty` is directly runnable
     * (Swedish already uses plain QWERTY, no dedicated layout prerequisite needed) but deliberately not
     * curated into a blacklist this round - the same "cannot confidently separate a genuine short word from
     * real corpus noise without native fluency" reasoning every other implemented language documents for
     * its own scan applies here too - left for a native-speaker-guided pass (see the Language Contribution
     * Guide's own step 11) rather than guessed at.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Swedish writes decimals with a comma ("3,14"), like every other implemented language here. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

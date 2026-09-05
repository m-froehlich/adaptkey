// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450: the Finnish [LanguageRules] implementation - built via the Language Contribution Guide's own §8
 * pipeline, from the English Wiktionary's own coverage of Finnish (no native edition exists on kaikki.org,
 * confirmed directly - even though this particular fallback source turned out unusually rich, see
 * `AdaptKey-Progress.md`'s own D-450 entry). Like every other implemented language, only three of the nine
 * hooks are naively fillable: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain locale facts,
 * [bundledConfusablesBlacklist] would reuse a real confusables scan. The remaining six hooks encode
 * German-specific compounding/inflection grammar with no Finnish equivalent implemented yet - Finnish's own
 * real agglutinative case system is a much larger, separate undertaking, not attempted here.
 */
object FinnishRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Finnish has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450: deliberately not curated into a blacklist this round - the same "cannot confidently separate a
     * genuine short word from real corpus noise without native fluency" reasoning every other implemented
     * language documents for its own scan applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Finnish writes decimals with a comma ("3,14"), like every other implemented language here. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

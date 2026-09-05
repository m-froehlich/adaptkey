// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450: the Tagalog [LanguageRules] implementation - built via the Language Contribution Guide's own §8
 * pipeline, from the English Wiktionary's own coverage of Tagalog (no native edition exists on kaikki.org,
 * confirmed directly). Like every other implemented language, only three of the nine hooks are naively
 * fillable: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain locale facts,
 * [bundledConfusablesBlacklist] would reuse a real confusables scan. The remaining six hooks encode
 * German-specific compounding/inflection grammar with no Tagalog equivalent implemented yet - Tagalog's own
 * real focus/trigger verb-affixation system is a much larger, separate undertaking, not attempted here.
 */
object TagalogRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Tagalog has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450: deliberately not curated into a blacklist this round - the same "cannot confidently separate a
     * genuine short word from real corpus noise without native fluency" reasoning every other implemented
     * language documents for its own scan applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /**
     * D-450: like Malay and Swahili (see their own `*Rules` notes), the Philippines follows the American
     * period-decimal convention, NOT a comma (a direct consequence of decades of American administration and
     * ongoing close ties) - verified directly rather than assumed to match this project's own comma-heavy
     * default.
     */
    override fun decimalCommaGluesDigits(): Boolean = false
}

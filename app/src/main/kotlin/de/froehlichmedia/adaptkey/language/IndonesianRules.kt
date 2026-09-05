// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450: the Indonesian [LanguageRules] implementation - built via the Language Contribution Guide's own §8
 * pipeline, from Indonesian's own NATIVE Wiktionary edition (confirmed directly on kaikki.org). Like every
 * other implemented language, only three of the nine hooks are naively fillable: [decimalCommaGluesDigits]
 * and [timeSuggestionWord] are plain locale facts, [bundledConfusablesBlacklist] would reuse a real
 * confusables scan. The remaining six hooks encode German-specific compounding/inflection grammar with no
 * Indonesian equivalent implemented yet - Indonesian's own real reduplication-plural and rich voice/aspect
 * affixation verb morphology are a much smaller, honestly documented Wortfamilien scope this round (see
 * `AdaptKey-Progress.md`'s own D-450 entry), not attempted as dedicated grammar hooks here.
 */
object IndonesianRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Indonesian has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450: deliberately not curated into a blacklist this round - the same "cannot confidently separate a
     * genuine short word from real corpus noise without native fluency" reasoning every other implemented
     * language documents for its own scan applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /**
     * Indonesian writes decimals with a comma ("3,14") - unlike Malay (Malaysia), which follows the British/
     * American period convention instead (see `MalayRules`'s own note) - Indonesia's own convention follows
     * its Dutch colonial history, verified directly rather than assumed identical to its close linguistic
     * relative.
     */
    override fun decimalCommaGluesDigits(): Boolean = true
}

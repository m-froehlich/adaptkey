// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450-followup: the Serbian [LanguageRules] implementation - built via the Language Contribution Guide's
 * own §8 pipeline, from the shared "Serbo-Croatian" Wiktionary edition's own Cyrillic-tagged coverage (no
 * separate native Serbian edition exists - kaikki.org treats Croatian/Bosnian/Serbian as one shared language,
 * see `AdaptKey-Progress.md`'s own D-450/D-450-followup entries) plus a real, dedicated full Serbian
 * Wikipedia corpus extraction (unlike Croatian/Bosnian, script was a real, directly-measured decision here -
 * see `dictionaries/sr/extract_wiki_dump.py`'s own module docstring). Like every other implemented language,
 * only three of the nine hooks are naively fillable: [decimalCommaGluesDigits] and [timeSuggestionWord] are
 * plain locale facts, [bundledConfusablesBlacklist] would reuse a real confusables scan. The remaining six
 * hooks encode German-specific compounding/inflection grammar with no Serbian equivalent implemented yet.
 */
object SerbianRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Serbian has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450-followup: deliberately not curated into a blacklist this round - the same "cannot confidently
     * separate a genuine short word from real corpus noise without native fluency" reasoning every other
     * implemented language documents for its own scan applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Serbian writes decimals with a comma ("3,14"), like every other implemented language here. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450-followup: the Uzbek [LanguageRules] implementation - built via the Language Contribution Guide's own
 * §8 pipeline, from a full Uzbek Wikipedia corpus extraction plus the ENGLISH Wiktionary's own coverage of
 * Uzbek (kaikki.org has no native `uz` edition - see `dictionaries/uz/extract_wiktionary.py`'s own module
 * docstring for the live-verified 404). The thinnest Wiktionary source of any language pack this project has
 * built (4,465 total entries) - see that same module docstring for the mandatory disclosure this implies.
 * Like every other implemented language, only three of the nine hooks are naively fillable:
 * [decimalCommaGluesDigits] and [timeSuggestionWord] are plain locale facts, [bundledConfusablesBlacklist]
 * would reuse a real confusables scan. The remaining six hooks encode German-specific compounding/inflection
 * grammar with no Uzbek equivalent implemented yet.
 */
object UzbekRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Uzbek has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450-followup: deliberately not curated into a blacklist this round - `confusables_scan.py` (plain
     * `"qwerty"` row layout, since Uzbek needs no new keyboard geometry) found 1,693 candidate pairs,
     * overwhelmingly short 3-4-letter tokens risking autocorrect into a common everyday word (bir/yil/deb/
     * katta/kishi) - the same "cannot confidently separate a genuine short word from real corpus noise
     * without native fluency" reasoning every other non-German implemented language documents for its own
     * scan applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Uzbekistan writes decimals with a comma ("3,14"), like every other implemented language here. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

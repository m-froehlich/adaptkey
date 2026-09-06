// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450-followup: the Ukrainian [LanguageRules] implementation - built via the Language Contribution Guide's
 * own §8 pipeline, from a full Ukrainian Wikipedia corpus extraction plus the ENGLISH Wiktionary's own
 * coverage of Ukrainian (kaikki.org has no native `uk` edition - see `dictionaries/uk/extract_wiktionary.py`'s
 * own module docstring for the live-verified 404 and the mandatory disclosure this implies: this pack's
 * POS/Wortfamilien source is genuinely thinner than Russian's own native-sourced round and should be
 * expected to need more follow-up curation later). Like every other implemented language, only three of the
 * nine hooks are naively fillable: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain locale
 * facts, [bundledConfusablesBlacklist] would reuse a real confusables scan. The remaining six hooks encode
 * German-specific compounding/inflection grammar with no Ukrainian equivalent implemented yet.
 */
object UkrainianRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Ukrainian has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450-followup: deliberately not curated into a blacklist this round - `confusables_scan.py` (new
     * `"ukrainian_jcuken"` row layout, matching `JcukenLayout.kt`'s own `ROW_TOP_UK`/`ROW_MIDDLE_UK`/
     * `ROW_BOTTOM_UK` exactly) found 3,832 candidate pairs, overwhelmingly short 2-letter tokens risking
     * autocorrect into one of Ukrainian's own extremely common short prepositions/conjunctions (на/та/до/за/
     * що/не) - the same "cannot confidently separate a genuine short word from real corpus noise without
     * native fluency" reasoning every other non-German implemented language documents for its own scan
     * applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Ukraine writes decimals with a comma ("3,14"), the standard DSTU convention - verified, not assumed. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

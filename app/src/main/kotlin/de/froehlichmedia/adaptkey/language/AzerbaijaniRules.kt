// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450-followup: the Azerbaijani [LanguageRules] implementation - built via the Language Contribution
 * Guide's own §8 pipeline, from a full Azerbaijani Wikipedia corpus extraction plus the ENGLISH Wiktionary's
 * own coverage of Azerbaijani (kaikki.org has no native `az` edition - see
 * `dictionaries/az/extract_wiktionary.py`'s own module docstring for the live-verified 404 and the mandatory
 * disclosure this implies). Like every other implemented language, only three of the nine hooks are
 * naively fillable: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain locale facts,
 * [bundledConfusablesBlacklist] would reuse a real confusables scan. The remaining six hooks encode
 * German-specific compounding/inflection grammar with no Azerbaijani equivalent implemented yet.
 */
object AzerbaijaniRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Azerbaijani has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450-followup: deliberately not curated into a blacklist this round - `confusables_scan.py` (new
     * `"azerbaijani"` row layout, matching `AzerbaijaniLayout.kt`'s own `TOP_ROW`/`MIDDLE_ROW`/`THIRD_ROW`
     * exactly) found 4,409 candidate pairs, overwhelmingly short 2-letter tokens risking autocorrect into one
     * of Azerbaijani's own extremely common short conjunctions/suffix-words (və/ci/bu/də/cü/il/cı/ən/cu/öz) -
     * the same "cannot confidently separate a genuine short word from real corpus noise without native
     * fluency" reasoning every other non-German implemented language documents for its own scan applies here
     * too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Azerbaijan writes decimals with a comma ("3,14"), like Turkish and every other implemented language here. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

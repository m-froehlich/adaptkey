// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-450-followup: the Russian [LanguageRules] implementation - built via the Language Contribution Guide's
 * own §8 pipeline, from the native Russian Wiktionary edition (`ru.wiktionary.org`, kaikki.org's own
 * `downloads/ru/` extract - the richest native Wiktionary source this project has processed) plus a real,
 * dedicated full Russian Wikipedia corpus extraction (the largest single dump this project has processed,
 * ~5.99GB compressed - see `dictionaries/ru/extract_wiki_dump.py`'s own module docstring). Like every other
 * implemented language, only three of the nine hooks are naively fillable: [decimalCommaGluesDigits] and
 * [timeSuggestionWord] are plain locale facts, [bundledConfusablesBlacklist] would reuse a real confusables
 * scan. The remaining six hooks encode German-specific compounding/inflection grammar with no Russian
 * equivalent implemented yet.
 */
object RussianRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Russian has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-450-followup: deliberately not curated into a blacklist this round - `confusables_scan.py` (new
     * `"russian_jcuken"` row layout, matching `JcukenLayout.kt`'s own `ROW_TOP_RU`/`ROW_MIDDLE_RU`/
     * `ROW_BOTTOM_RU` exactly) found 1,166 candidate pairs, overwhelmingly short 2-3-letter tokens risking
     * autocorrect into one of Russian's own extremely common short prepositions/particles (на/по/из/не/от/
     * за/до/во/но/что/как/при/или) - the same "cannot confidently separate a genuine short word from real
     * corpus noise without native fluency" reasoning every other non-German implemented language documents
     * for its own scan applies here too.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** Russia writes decimals with a comma ("3,14"), the standard GOST 8.417 convention - verified, not assumed. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

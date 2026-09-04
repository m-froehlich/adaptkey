// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-443: the Spanish [LanguageRules] implementation - built directly to the real-corpus/real-lexicon
 * method [FrenchRules] was rebuilt to (D-441-followup, AdaptKey-History.md §415), not to the earlier,
 * explicitly-superseded first pass (a small OpenSubtitles-derived list plus suffix-heuristic POS
 * tagging). Like [FrenchRules]/[EnglishRules], only three of the nine hooks are naively fillable without
 * real Spanish grammar-engineering work: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain
 * locale facts, [bundledConfusablesBlacklist] would reuse a real confusables scan (see that function's
 * own KDoc for why it stays empty for now). The remaining six hooks encode German-specific
 * compounding/inflection grammar that has no Spanish equivalent implemented yet - left as the same
 * "does not apply" no-op [NoOpLanguageRules] gives every other unimplemented language.
 */
object SpanishRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** Spanish has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-443: `dictionaries/confusables_scan.py dictionaries/es/dict.tsv qwerty` is directly runnable
     * (Spanish already uses [de.froehlichmedia.adaptkey.suggestion.KeyboardProximityQwerty] - no AZERTY-
     * style geometry gap to unblock first, unlike French's own D-441/D-442 round) but was deliberately not
     * curated into a blacklist this round: the same "cannot confidently separate a genuine short Spanish
     * word/abbreviation from real corpus noise without native fluency" reasoning [FrenchRules] documents
     * for its own AZERTY scan applies here too - left for a native-speaker-guided pass (see the Language
     * Contribution Guide's own step 11) rather than guessed at. 625 candidates found originally, 650 after
     * D-444's own dictionary rebuild against the correct native Wiktionary source - same shape, not
     * re-reviewed word by word.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /**
     * The RAE/Spain convention this pack follows writes decimals with a comma ("3,14"), like German/
     * French - not every Spanish-speaking locale agrees (several Latin American countries use a point),
     * but this app has no per-region Spanish variant to target separately.
     */
    override fun decimalCommaGluesDigits(): Boolean = true
}

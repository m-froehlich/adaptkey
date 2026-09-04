// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-441: the French [LanguageRules] implementation - see the Language Contribution Guide's own §8 step 9
 * for why only three of the nine hooks are ever naively fillable without real French grammar-engineering
 * work: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain locale facts,
 * [bundledConfusablesBlacklist] reuses this round's own confusables scan (see that function's own KDoc).
 * The remaining six hooks encode German-specific compounding/inflection grammar
 * ([blocksAsSplitPrefix]/[blocksAsFeminineAgentException]/[blocksAsCompoundPrefix]/
 * [isPlausibleVerbInflection]/[isPlausibleAdjectiveComparative]/[splitCompound]) that has no French
 * equivalent implemented yet - left as the same "does not apply" no-op [NoOpLanguageRules] gives every
 * other unimplemented language, per that object's own documented, accepted degraded state, not silently
 * faked.
 */
object FrenchRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    
    /** French has no single-word equivalent to German's S-08 "Uhr" convention after a typed time. */
    override fun timeSuggestionWord(): String? = null
    
    /**
     * D-442-followup: `dictionaries/confusables_scan.py dictionaries/fr/dict.tsv azerty` (now runnable at
     * all only since [de.froehlichmedia.adaptkey.suggestion.KeyboardProximityAzerty] exists) found 994
     * real candidate pairs, the overwhelming majority short (2-3 letter) tokens - `ve`/`xe`/`re`/`ka`/`ma`/
     * `st`/... - risking autocorrect into a very common neighbouring function word (`de`/`la`/`le`/`et`).
     * Deliberately left empty rather than guessed at: several of these are genuine French abbreviations/
     * loanwords this round's own non-native French judgement cannot confidently separate from corpus
     * noise at this length (`dj`, `led`, `fn`, `lr`, `crs`, `onf` are all real; `ma` itself is an
     * extremely common real word, not noise, at risk only because it is short - the identical ambiguity
     * D-442's own English scan resolved for `fir`/`otter` via frequency correction, not blacklisting).
     * Curating this list correctly needs the same native-speaker review the guide's own step 11 already
     * asks for before publishing this pack as more than "pretty good" - not attempted here.
     */
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** French, like German, writes decimals with a comma ("3,14"), not a point. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

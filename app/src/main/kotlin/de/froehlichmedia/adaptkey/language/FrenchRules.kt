// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-number (this round): the French [LanguageRules] implementation - see the Language Contribution
 * Guide's own §8 step 9 for why only three of the nine hooks are ever naively fillable without real
 * French grammar-engineering work: [decimalCommaGluesDigits] and [timeSuggestionWord] are plain
 * locale facts, [bundledConfusablesBlacklist] reuses this round's own confusables scan (currently
 * empty - see the guide's step 7/this class's own KDoc for why). The remaining six hooks encode
 * German-specific compounding/inflection grammar ([blocksAsSplitPrefix]/
 * [blocksAsFeminineAgentException]/[blocksAsCompoundPrefix]/[isPlausibleVerbInflection]/
 * [isPlausibleAdjectiveComparative]/[splitCompound]) that has no French equivalent implemented yet -
 * left as the same "does not apply" no-op [NoOpLanguageRules] gives every other unimplemented
 * language, per that object's own documented, accepted degraded state, not silently faked.
 *
 * [bundledConfusablesBlacklist] is deliberately empty rather than a naive port of the German list:
 * [de.froehlichmedia.adaptkey.suggestion.KeyboardProximity] (the adjacency grid the guide's step 7
 * confusables method needs) is hardcoded to the QWERTZ row geometry and is not aware of
 * [de.froehlichmedia.adaptkey.keyboard.AzertyLayout] at all, so a real keyboard-adjacency scan
 * against AZERTY's actual physical layout is not possible without first making that class
 * layout-aware - a genuine, newly-found structural gap (see AdaptKey-Progress.md's own Open TODOs
 * for the tracking entry), not something this language-pack round attempts to fix.
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
    
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    
    /** French, like German, writes decimals with a comma ("3,14"), not a point. */
    override fun decimalCommaGluesDigits(): Boolean = true
}

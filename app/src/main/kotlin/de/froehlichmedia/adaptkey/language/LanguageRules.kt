// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.CompoundSplit

/**
 * D-410: the seam between language-agnostic core logic and the handful of genuinely language-specific
 * grammar/orthography rules this app implements - today exclusively German (A-05's inseparable-prefix and
 * feminine-agent-suffix split vetoes, D-116/D-115/D-125/D-252's compound/verb/adjective morphology
 * protections, S-08's "Uhr" time suggestion, A-04's bundled cross-language-confusables blacklist, A-12/
 * D-320's decimal-comma digit gluing). Before D-410 each of these lived as an unconditional global `object`
 * singleton, applied identically regardless of which language was actually active - see AdaptKey-History.md
 * D-410 for the full design discussion (including what was deliberately left out of this pass and why).
 *
 * [LanguageRulesRegistry.rulesFor] resolves the instance for a given [Language], mirroring
 * [de.froehlichmedia.adaptkey.keyboard.LayoutRegistry]'s existing `Map<Language, X>` pattern. A language
 * without its own real implementation gets [NoOpLanguageRules] - every check below defaults to "does not
 * apply" rather than silently inheriting another language's grammar.
 */
interface LanguageRules {
    
    /**
     * A-05 / D-249: whether [candidate] must be rejected outright as the left half of a word split - a
     * known inseparable verb prefix (or similar) that is not, itself, a genuinely common standalone word.
     *
     * @param candidate the lower-cased left half being considered
     * @param frequency [candidate]'s own standalone dictionary frequency, for the exemption check
     */
    fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean
    
    /**
     * A-05 / D-261: whether [rightHalf] must be rejected as the right half of a word split because
     * [leftHalf] is a plausible agent-noun stem whose genuine feminine form is [rightHalf] glued on, not a
     * real two-word split.
     *
     * @param rightHalf the lower-cased right half being considered
     * @param leftHalf the lower-cased left half being considered
     * @param leftIsNoun whether [leftHalf] itself resolves to a noun in the active dictionary
     */
    fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean
    
    /**
     * A-05 / D-404-followup: whether [candidate] must be rejected as the left half of a word split when
     * [rightIsNoun] - a known compound-forming particle/adverb (e.g. "schon" in "Schonfenster") that,
     * unlike [blocksAsSplitPrefix]'s inseparable verb prefixes, is also a perfectly ordinary standalone
     * word on its own, so it must not be blocked unconditionally - only when immediately glued to a noun,
     * the shape a genuine German compound takes ("schon gut" - "schon" followed by something that is not
     * a noun - must still split normally).
     *
     * @param candidate the lower-cased left half being considered
     * @param rightIsNoun whether the right half resolves to a noun in the active dictionary
     */
    fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean
    
    /**
     * D-115 / D-125: whether [token] is a plausible regular verb inflection of a known infinitive, so it
     * must never be silently corrected or split.
     *
     * @param isKnownWord looks up whether a candidate reconstructed infinitive is a known dictionary word
     */
    fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean
    
    /**
     * D-252: whether [token] is a plausible regular adjective comparative/superlative of a known positive,
     * so it must never be silently corrected or split.
     *
     * @param isPlausiblePositive whether a candidate reconstructed stem is a plausible positive adjective
     */
    fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean
    
    /**
     * D-116: attempts to recognise [token] as a compound-word split (suggestion only, A-08).
     *
     * @param isKnownNoun looks up whether a candidate first part is a known dictionary noun
     * @param resolveRest looks up whether a candidate remainder is itself acceptable
     */
    fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result?
    
    /**
     * S-08 / D-137: the word to suggest after a typed time (`HH:MM `).
     *
     * @return the suggestion word, or null when this language has no such convention
     */
    fun timeSuggestionWord(): String?
    
    /**
     * A-04 / D-176: the bundled cross-language-confusables/pre-reform-spelling blacklist seeded into this
     * language's own store on install.
     *
     * @return the words to seed, or empty when this language curates no such list (see
     *         AdaptKey-Progress.md's own open TODO: only German has one today)
     */
    fun bundledConfusablesBlacklist(): Set<String>
    
    /**
     * A-12 / D-320: whether a digit right after this language's own sentence-punctuation auto-space, where
     * the punctuation was a comma, glues onto it as a decimal separator rather than confirming the space -
     * true only for a language whose own decimal convention uses a comma.
     */
    fun decimalCommaGluesDigits(): Boolean
}

/** D-410: the default - no language-specific grammar/orthography rule applies. */
object NoOpLanguageRules : LanguageRules {
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean = false
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean = false
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean = false
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean = false
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean = false
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? = null
    override fun timeSuggestionWord(): String? = null
    override fun bundledConfusablesBlacklist(): Set<String> = emptySet()
    override fun decimalCommaGluesDigits(): Boolean = false
}

/**
 * D-410: resolves each [Language]'s own [LanguageRules] - mirrors
 * [de.froehlichmedia.adaptkey.keyboard.LayoutRegistry]'s identical `Map<Language, X>` shape.
 */
object LanguageRulesRegistry {
    
    private val RULES: Map<Language, LanguageRules> = mapOf(
        Language.GERMAN to GermanRules,
        Language.FRENCH to FrenchRules
    )
    
    /**
     * @param language a keyboard-typing language
     * @return its own [LanguageRules], or [NoOpLanguageRules] for a language without any real
     *         implementation yet
     */
    fun rulesFor(language: Language): LanguageRules = RULES[language] ?: NoOpLanguageRules
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import de.froehlichmedia.adaptkey.language.Language

/**
 * D-449-followup: the seam between [CapitalisationEngine]'s language-agnostic §6 hierarchy and a language's
 * own single-character case-mapping conventions - mirrors [de.froehlichmedia.adaptkey.language.LanguageRules]/
 * [de.froehlichmedia.adaptkey.suggestion.DiacriticFolding]'s identical D-410/D-435 shape ("delegate to the
 * active language, default to doing nothing special").
 *
 * A deliberately separate seam from [de.froehlichmedia.adaptkey.language.LanguageRules] itself, not a tenth
 * hook added there: every existing [de.froehlichmedia.adaptkey.language.LanguageRules] hook encodes German
 * compounding/inflection grammar with no bearing on how a single character upper/lower-cases, and
 * [CapitalisationEngine] is the only reader - keeping the two seams apart means a change to one can never
 * ripple through the other's unrelated call sites.
 *
 * [DefaultCasingRules] is Kotlin's own locale-invariant [Char.uppercaseChar]/[Char.lowercaseChar] - the exact
 * behaviour every language had before this seam existed, so every language other than Turkish is unaffected
 * by this seam's introduction.
 */
interface CasingRules {
    
    /**
     * Upper-cases the first character of [word] only, per this language's own convention. Mirrors
     * `word.replaceFirstChar { it.uppercaseChar() }`, the pre-D-449-followup behaviour every language shared.
     */
    fun uppercaseFirst(word: String): String
    
    /**
     * Lower-cases the first character of [word] only, per this language's own convention. Mirrors
     * `word.replaceFirstChar { it.lowercaseChar() }`, the pre-D-449-followup behaviour every language shared.
     */
    fun lowercaseFirst(word: String): String
    
    /**
     * Upper-cases every character of [word] (C-04 [de.froehlichmedia.adaptkey.capitalisation.CapsMode.CHARACTERS]).
     * Mirrors `word.uppercase()`, the pre-D-449-followup behaviour every language shared.
     */
    fun uppercaseAll(word: String): String
}

/** D-449-followup: the default - Kotlin's own locale-invariant single-character case mapping. */
object DefaultCasingRules : CasingRules {
    override fun uppercaseFirst(word: String): String = word.replaceFirstChar { it.uppercaseChar() }
    override fun lowercaseFirst(word: String): String = word.replaceFirstChar { it.lowercaseChar() }
    override fun uppercaseAll(word: String): String = word.uppercase()
}

/**
 * D-449-followup: resolves each [Language]'s own [CasingRules] - mirrors
 * [de.froehlichmedia.adaptkey.language.LanguageRulesRegistry]'s identical `Map<Language, X>` shape.
 */
object CasingRulesRegistry {
    
    private val RULES: Map<Language, CasingRules> = mapOf(
        Language.TURKISH to TurkishCasingRules
    )
    
    /**
     * @param language a keyboard-typing language
     * @return its own [CasingRules], or [DefaultCasingRules] for a language without a special-cased one
     */
    fun rulesFor(language: Language): CasingRules = RULES[language] ?: DefaultCasingRules
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.suggestion.DiacriticFolding
import de.froehlichmedia.adaptkey.suggestion.NoOpDiacriticFolding
import de.froehlichmedia.adaptkey.suggestion.Umlaut

/**
 * D-435: resolves each [Language]'s own [DiacriticFolding] - mirrors [LanguageRulesRegistry]'s identical
 * `Map<Language, X>` shape and its own "no real implementation yet -> a documented no-op default" philosophy.
 */
object DiacriticFoldingRegistry {
    
    private val FOLDINGS: Map<Language, DiacriticFolding> = mapOf(
        Language.GERMAN to Umlaut
    )
    
    /**
     * @param language a keyboard-typing language
     * @return its own [DiacriticFolding], or [NoOpDiacriticFolding] for a language without any real
     *         implementation yet
     */
    fun foldingFor(language: Language): DiacriticFolding = FOLDINGS[language] ?: NoOpDiacriticFolding
}

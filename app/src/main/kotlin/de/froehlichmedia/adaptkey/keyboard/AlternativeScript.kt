// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure helper for D-99's math-symbol popup: distinguishes a genuine language letter - which should extend
 * the composing word and follow the normal case rules, e.g. a G-01 Greek accented vowel while actually
 * typing Greek - from a letter borrowed from a different script purely as a symbol, e.g. π/α/β/γ/δ/λ/ω
 * picked from the Latin keyboard's `p` key as mathematical constants/variables, not language text. The
 * latter must never be silently uppercased by auto-capitalisation - case is semantically significant there
 * (Π and π are different symbols), unlike an ordinary capitalised word start.
 */
object AlternativeScript {
    
    /**
     * D-285: `ƒ` (U+0192, LATIN SMALL LETTER F WITH HOOK) passes [Char.isLetter] and is genuinely Latin
     * script, so the [Character.UnicodeScript] check below alone cannot exclude it the way it excludes π -
     * but in this app it is used exactly the same way, as `f`'s own borrowed function/currency-symbol corner
     * hint (D-90/§29), never a real alphabetic letter. Not a script-based exclusion, since none exists for
     * this case; a small, explicit, hand-curated set instead - extend it if a future per-language hint
     * (D-281) introduces another Latin-script "letter" used purely as a symbol.
     */
    private val LATIN_SYMBOL_LETTERS = setOf('ƒ')
    
    /**
     * @param symbol a long-press alternative
     * @param activeLanguageIsGreek whether the keyboard is currently in Greek input mode
     * @return true when [symbol] should extend the composing word like ordinary typed text
     */
    fun extendsWord(symbol: String, activeLanguageIsGreek: Boolean): Boolean {
        if (symbol.isEmpty() || !symbol.all { it.isLetter() } || symbol.any { it in LATIN_SYMBOL_LETTERS }) {
            return false
        }
        if (activeLanguageIsGreek) {
            return true
        }
        return symbol.none { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.GREEK }
    }
}

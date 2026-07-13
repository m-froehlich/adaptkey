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
     * @param symbol a long-press alternative
     * @param activeLanguageIsGreek whether the keyboard is currently in Greek input mode
     * @return true when [symbol] should extend the composing word like ordinary typed text
     */
    fun extendsWord(symbol: String, activeLanguageIsGreek: Boolean): Boolean {
        if (symbol.isEmpty() || !symbol.all { it.isLetter() }) {
            return false
        }
        if (activeLanguageIsGreek) {
            return true
        }
        return symbol.none { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.GREEK }
    }
}

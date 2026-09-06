// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * D-449-followup: Turkish's dotted/dotless İ/I pair, the "still-open design question" named in
 * `AdaptKey-Progress.md`'s D-449 entry - closed by adding this as a real [CasingRules] implementation rather
 * than guessed at. Confirmed against Unicode's own `SpecialCasing.txt` `tr`/`az` section: of its four
 * Turkish-specific case-mapping lines, only two actually differ from Kotlin's locale-invariant default
 * (verified directly, not assumed) - `'ı'.uppercaseChar()` already yields `'I'` and `'İ'.lowercaseChar()`
 * already yields `'i'` under the ordinary Unicode simple-case tables, so only the two genuinely asymmetric
 * pairs below need a real override:
 *
 * - upper-casing plain `'i'` (U+0069) must yield `'İ'` (U+0130, LATIN CAPITAL LETTER I WITH DOT ABOVE), not
 *   the ordinary `'I'` (U+0049) every other Latin-script language uses;
 * - lower-casing plain `'I'` (U+0049) must yield `'ı'` (U+0131, LATIN SMALL LETTER DOTLESS I), not the
 *   ordinary `'i'` (U+0069).
 *
 * Every other character defers to Kotlin's own [Char.uppercaseChar]/[Char.lowercaseChar] unchanged - this is
 * a two-character exception to the default, not a wholesale Turkish alphabet remap.
 */
object TurkishCasingRules : CasingRules {
    
    private const val DOTTED_CAPITAL_I = 'İ'
    private const val DOTLESS_SMALL_I = 'ı'
    
    override fun uppercaseFirst(word: String): String = word.replaceFirstChar(::turkishUppercaseChar)
    
    override fun lowercaseFirst(word: String): String = word.replaceFirstChar(::turkishLowercaseChar)
    
    override fun uppercaseAll(word: String): String = buildString(word.length) {
        for (char in word) {
            append(turkishUppercaseChar(char))
        }
    }
    
    private fun turkishUppercaseChar(char: Char): Char = if (char == 'i') DOTTED_CAPITAL_I else char.uppercaseChar()
    
    private fun turkishLowercaseChar(char: Char): Char = if (char == 'I') DOTLESS_SMALL_I else char.lowercaseChar()
}

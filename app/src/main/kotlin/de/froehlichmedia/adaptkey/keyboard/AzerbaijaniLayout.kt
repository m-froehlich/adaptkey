// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * Azerbaijani standard Latin key map, researched (not guessed) against the real Microsoft `KBDAZST`
 * "Azerbaijani (Standard)" layout (verified directly against `kbdlayout.info/KBDAZST`/
 * `learn.microsoft.com/en-us/globalization/keyboards/kbdazst.html`).
 *
 * A genuinely different Latin arrangement from QWERTY/QWERTZ, not merely a within-row letter swap the way
 * German's QWERTZ is (`y`/`z`) - a casual description found while researching this (informally summarised
 * as "QÜERTY", Ü replacing W) turned out to be a real oversimplification once the actual key-by-key table was
 * pulled: `q`/`w` do not even appear in this layout's own primary rows at their ordinary QWERTY positions at
 * all (`q` sits on the bottom row, `w` is dropped from the primary letter rows entirely) - the whole
 * arrangement is its own standard, mirroring [AzertyLayout]'s own "moves letters across rows entirely,
 * cannot be expressed as a variant flag" category (D-314) rather than [KeyboardLayout]'s QWERTZ/QWERTY
 * variant-flag one. Still genuinely Latin script, so - like [AzertyLayout] and unlike [GreekLayout]/
 * [SerbianLayout] - it keeps the L-05/C-08 [letterHints] AltGr overlay and reuses [KeyboardLayout.topRowKey]/
 * [KeyboardLayout.letterKey] so `p`/`o`/`a`/`e`/`n` keep their existing math-symbol popups (D-99/§29)
 * wherever those particular letters land in this layout's own rows - that lookup is keyed by character, not
 * row/column position, so it works unchanged here.
 *
 * All 32 letters of the Azerbaijani Latin alphabet are covered directly by the three primary rows (no AltGr
 * dependency for any base letter, matching the real standard) - `q`/`w`/`x` (present) map to their own real
 * Azerbaijani phonetic values (`q`=/g/, `x`=/x/ voiceless velar fricative) rather than being unused filler
 * the way Serbian Cyrillic's JUSCII layout leaves `q`/`w`/`x`/`y` unused; Azerbaijani Latin uses its own full
 * 32-letter alphabet across all three rows including the dotted/dotless İ/I pair (see
 * [de.froehlichmedia.adaptkey.capitalisation.TurkishCasingRules]'s own KDoc - the same Unicode
 * `SpecialCasing.txt` `tr`/`az` rule applies to Azerbaijani unchanged, so that casing seam needs only a
 * registry entry once this language is actually built, not new casing logic).
 */
object AzerbaijaniLayout {
    
    private const val TOP_ROW = "püşudbmyohxj"
    private const val MIDDLE_ROW = "gısarnəlicğ"
    private const val THIRD_ROW = "çfvtkeqzö"
    
    // L-06: German-QWERTZ shifted symbol per digit, shown as a corner hint (shared with the Latin layout).
    private val NUMBER_HINTS = mapOf(
        '1' to "!", '2' to "\"", '3' to "§", '4' to "$", '5' to "%",
        '6' to "&", '7' to "/", '8' to "(", '9' to ")", '0' to "="
    )
    
    // D-105: every digit's own superscript form, offered as a second D-01 popup alternative alongside its
    // existing shifted symbol above (shared with the Latin layout's identical treatment).
    private val NUMBER_SUPERSCRIPTS = mapOf(
        '1' to "¹", '2' to "²", '3' to "³", '4' to "⁴", '5' to "⁵",
        '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹", '0' to "⁰"
    )
    
    /**
     * Builds the Azerbaijani keyboard for the given configuration.
     *
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @param letterHints the per-letter secondary-symbol map (L-05 / C-08); defaults to
     *        [KeyboardLayout.DEFAULT_LETTER_HINTS] - a language pack's own `hints.tsv` (D-281) overrides it
     *        the same way it does for [KeyboardLayout] itself.
     * @param urlMode D-143: true for a recognised URL-entry field - replaces the bottom row's comma/space/
     *        period trio with [KeyboardLayout.urlBottomRow]; defaults to false.
     * @param emailMode D-158: true for a recognised email-address field - replaces the bottom row's
     *        comma/space/period trio with [KeyboardLayout.emailBottomRow]; defaults to false. Ignored when
     *        [urlMode] is also true.
     * @param locale D-143 / D-158: the system locale [KeyboardLayout.urlBottomRow]'s/[KeyboardLayout.
     *        emailBottomRow]'s period key resolves its TLD popup from ([UrlLocale]); only meaningful when
     *        [urlMode] or [emailMode] is true.
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true,
        letterHints: Map<Char, String> = KeyboardLayout.DEFAULT_LETTER_HINTS,
        urlMode: Boolean = false,
        emailMode: Boolean = false,
        locale: Locale = Locale.getDefault()
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            result.add("1234567890".map { c -> numberKey(c) })
        }
        
        result.add(TOP_ROW.map { c -> KeyboardLayout.topRowKey(c, letterHints) })
        result.add(MIDDLE_ROW.map { c -> KeyboardLayout.letterKey(c, letterHints) })
        
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(THIRD_ROW.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            THIRD_ROW.forEach { c -> add(KeyboardLayout.letterKey(c, letterHints, weight = thirdRowLetterWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(
            if (urlMode) {
                KeyboardLayout.urlBottomRow(proportions, locale)
            } else if (emailMode) {
                KeyboardLayout.emailBottomRow(proportions, locale)
            } else {
                buildList {
                    // L-03: combined emoji / numeric-layer key, identical to the Latin bottom row.
                    add(Key(label = "😊", code = KeyCode.SYMBOL, hint = "123", weight = proportions.symbolWeight))
                    // D-22: comma / full-stop carry the same long-press punctuation sets as the Latin layout.
                    add(charKey(',', alternatives = KeyboardLayout.COMMA_ALTERNATIVES, weight = proportions.commaWeight))
                    add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
                    add(charKey('.', alternatives = KeyboardLayout.PERIOD_ALTERNATIVES, weight = proportions.periodWeight))
                    add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
                }
            }
        )
        
        return result
    }
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
    
    /** D-105: see [KeyboardLayout.numberKey] - identical treatment. */
    private fun numberKey(c: Char): Key {
        val hint = NUMBER_HINTS[c]
        val superscript = NUMBER_SUPERSCRIPTS[c]
        val alternatives = if (hint != null && superscript != null) listOf(hint, superscript) else emptyList()
        return charKey(c, hint, alternatives = alternatives)
    }
}

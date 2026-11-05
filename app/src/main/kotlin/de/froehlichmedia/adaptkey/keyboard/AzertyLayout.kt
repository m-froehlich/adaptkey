// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * French AZERTY key map (D-314), the standard French physical-layout convention the language-
 * contribution guide has long called out as the obvious remaining Latin geometry beyond QWERTY/QWERTZ.
 * Unlike German's QWERTZ (only `y`/`z` swap position within the same row), AZERTY moves `q`/`w`/`a`/`z`/`m`
 * between rows entirely, so it cannot be expressed as a variant flag on [KeyboardLayout.rows] and instead
 * gets its own row geometry here, mirroring [GreekLayout]'s approach: the number row and the shared control
 * / bottom row are identical to the Latin layout, only the three letter rows differ. Unlike Greek (a
 * different alphabet), AZERTY is still Latin script, so it keeps the L-05/C-08 [letterHints] AltGr overlay
 * [KeyboardLayout] itself takes - a language pack's own `hints.tsv` (D-281) applies here exactly like it
 * does for [KeyboardLayout] - and reuses [KeyboardLayout.topRowKey] so `p`/`o` keep their existing math-
 * symbol/average-symbol popups (D-99/§29), since both letters sit at the same top-row positions they do in
 * QWERTY/QWERTZ.
 */
object AzertyLayout {
    
    private const val TOP_ROW = "azertyuiop"
    private const val MIDDLE_ROW = "qsdfghjklm"
    private const val THIRD_ROW = "wxcvbn"
    
    /**
     * Builds the AZERTY keyboard for the given configuration.
     * 
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @param letterHints the per-letter secondary-symbol map (L-05 / C-08); defaults to
     *        [KeyboardLayout.DEFAULT_LETTER_HINTS] - a language pack's own `hints.tsv` (D-281) overrides it
     *        the same way it does for [KeyboardLayout] itself.
     * @param urlMode D-143: true for a recognised URL-entry field - replaces the bottom row's comma/space/
     *        period trio with [KeyboardLayout.urlBottomRow] (URL entry doesn't depend on the active typing
     *        geometry, identical to [GreekLayout]'s own reasoning); defaults to false.
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
        locale: Locale = Locale.getDefault(),
        showApostropheKey: Boolean = true
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            // L-06 / D-498 / D-511: the shared number-row builder - the AZERTY number row is deliberately
            // the Latin one, byte-identical to every other layout's by construction (the reform's
            // dual-corner row rides along like it does for the non-Latin layouts, §2.12).
            result.add("1234567890".map { c -> KeyboardLayout.numberKey(c) })
        }
        
        result.add(TOP_ROW.map { c -> KeyboardLayout.topRowKey(c, letterHints, KeyboardLayout.BASE_LEFT_CORNERS) })
        // D-512 §2.2: the dedicated apostrophe key right of `m` on AZERTY (right of `l` on QWERTY/QWERTZ).
        // D-515: gated by [showApostropheKey] like the QWERTZ/QWERTY rows - default off for AZERTY too.
        val middleRow = MIDDLE_ROW.map { c -> KeyboardLayout.letterKey(c, letterHints, KeyboardLayout.BASE_LEFT_CORNERS) }
        result.add(if (showApostropheKey) middleRow + KeyboardLayout.apostropheKey() else middleRow)
        
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(THIRD_ROW.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            THIRD_ROW.forEachIndexed { index, c ->
                // D-512 §2.8: `w` (AZERTY's first third-row key) carries the `+` left corner.
                add(
                    KeyboardLayout.letterKey(
                        c,
                        letterHints,
                        leftCorners = KeyboardLayout.BASE_LEFT_CORNERS,
                        weight = thirdRowLetterWeight,
                        plusSlot = index == 0
                    )
                )
            }
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
}

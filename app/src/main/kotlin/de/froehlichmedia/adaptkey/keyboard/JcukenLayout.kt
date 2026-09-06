// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * The ЙЦУКЕН (JCUKEN) Cyrillic key map shared by Russian and Ukrainian, researched (not guessed) against the
 * real Microsoft standards - `KBDRU` ("Russian", `learn.microsoft.com/en-us/globalization/keyboards/
 * kbdru.html`) and `KBDUR` ("Ukrainian", `.../kbdur.html`).
 *
 * Unlike Serbian's own JUSCII-descended layout ([SerbianLayout], a completely different lineage phonetically
 * mapped onto the Latin QWERTZ grid), Russian and Ukrainian share ONE real historical keyboard standard with
 * only a handful of substituted/added positions - directly analogous to [KeyboardLayout]'s own QWERTY/QWERTZ
 * relationship (a variant flag on one shared implementation), not two unrelated layouts requiring their own
 * objects the way [SerbianLayout]/[AzerbaijaniLayout] do. Verified letter-by-letter before concluding this:
 * the two standards are identical across all three rows except -
 * - top row: the last letter is `ъ` (Russian, hard sign) vs. `ї` (Ukrainian);
 * - middle row: position 2 is `ы` (Russian) vs. `і` (Ukrainian), and the last letter is `э` (Russian) vs.
 *   `є` (Ukrainian);
 * - bottom row: Russian's own leading slot (the physical key to the left of "я") is a punctuation mark
 *   (backslash), while Ukrainian's is the letter `ґ` - Ukrainian's alphabet has one more letter (33) than
 *   Russian's own core layout letters (32, `ё` aside - see below), and this is where it goes.
 *
 * `ё` (Russian's own "yo", a real distinct letter) sits on its own dedicated physical key on a real desktop
 * keyboard (the one before "1", which this app's own mobile row shape has no slot for, the same practical
 * constraint every other layout in this app already has for a desktop's own extra keys) - offered instead as
 * a long-press secondary on `е`, mirroring [GreekLayout]'s own accent-on-vowel convention exactly (`е`'s
 * long-press secondary is itself a letter, so the service already appends it into the word rather than
 * treating it as a delimiter - no new mechanism needed). `ё` is not part of the Ukrainian alphabet at all, so
 * this secondary is Russian-only ([ukrainian] = false).
 */
object JcukenLayout {
    
    private const val ROW_TOP_RU = "йцукенгшщзхъ"
    private const val ROW_TOP_UK = "йцукенгшщзхї"
    private const val ROW_MIDDLE_RU = "фывапролджэ"
    private const val ROW_MIDDLE_UK = "фівапролджє"
    private const val ROW_BOTTOM_RU = "ячсмитьбю"
    private const val ROW_BOTTOM_UK = "ґячсмитьбю"
    
    /** Russian's own `е` -> `ё` long-press secondary (D-450-followup); not offered in Ukrainian mode. */
    private const val YO_HINT = "ё"
    
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
     * Builds the JCUKEN keyboard for the given configuration.
     *
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @param urlMode D-143: true for a recognised URL-entry field - replaces the bottom row's comma/space/
     *        period trio with [KeyboardLayout.urlBottomRow] (URL entry doesn't depend on the active typing
     *        alphabet, the same reasoning [GreekLayout]/[SerbianLayout] already document); defaults to false.
     * @param emailMode D-158: true for a recognised email-address field - replaces the bottom row's
     *        comma/space/period trio with [KeyboardLayout.emailBottomRow]; defaults to false. Ignored when
     *        [urlMode] is also true.
     * @param locale D-143 / D-158: the system locale [KeyboardLayout.urlBottomRow]'s/[KeyboardLayout.
     *        emailBottomRow]'s period key resolves its TLD popup from ([UrlLocale]); only meaningful when
     *        [urlMode] or [emailMode] is true.
     * @param ukrainian false (default) builds the Russian variant, true builds the Ukrainian variant - see
     *        this object's own KDoc for the exact, real letter-by-letter differences.
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true,
        urlMode: Boolean = false,
        emailMode: Boolean = false,
        locale: Locale = Locale.getDefault(),
        ukrainian: Boolean = false
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            result.add("1234567890".map { c -> numberKey(c) })
        }
        
        result.add((if (ukrainian) ROW_TOP_UK else ROW_TOP_RU).map { c -> letterKey(c, ukrainian) })
        result.add((if (ukrainian) ROW_MIDDLE_UK else ROW_MIDDLE_RU).map { c -> charKey(c) })
        
        val rowBottom = if (ukrainian) ROW_BOTTOM_UK else ROW_BOTTOM_RU
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(rowBottom.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            rowBottom.forEach { c -> add(charKey(c, weight = thirdRowLetterWeight)) }
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
    
    /** The middle row's own `е` key gets the `ё` long-press secondary in Russian mode only. */
    private fun letterKey(c: Char, ukrainian: Boolean): Key {
        val hint = if (!ukrainian && c == 'е') YO_HINT else null
        return charKey(c, hint = hint, alternatives = if (hint != null) listOf(hint) else emptyList())
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

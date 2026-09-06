// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * Serbian Cyrillic key map, the alternate alphabet reached via the G-01 language switch - D-450-followup:
 * Serbian's own real Cyrillic-keyboard-layout gap, deferred since the original 18-language round explicitly
 * pending this dedicated implementation (see `AdaptKey-Progress.md`'s D-450 entry).
 *
 * Row groupings match the real, established Serbian (Cyrillic) standard - Microsoft's own `KBDYCC` layout
 * (verified directly against `kbdlayout.info/KBDYCC`/`learn.microsoft.com/en-us/globalization/keyboards/
 * kbdycc.html`, the JUSCII-descended QWERTZ-based physical layout every Serbian Windows/Linux install ships)
 * - researched, not guessed. That desktop layout maps the 30-letter Vuk Karadžić alphabet onto the QWERTZ
 * key grid unevenly (13 letters on the top row, 11 on the middle row, only 6 - plus one historical filler,
 * see below - on the bottom row), because Serbian Cyrillic simply has more letters (30) than Latin QWERTY/
 * QWERTZ has convenient slots for (26) - this layout keeps that real, asymmetric grouping faithfully rather
 * than rebalancing it for a tidier-looking mobile row, since matching the genuine standard (so existing
 * muscle memory transfers) is the whole point, not an aesthetic rebalance.
 *
 * One real letter is deliberately dropped from the desktop standard: `Ѕ` (U+0405, CYRILLIC CAPITAL LETTER
 * DZE) sits at the desktop layout's own ISO-only extra key (the physical key to the left of `Z`, which
 * mobile keyboards - like every other layout in this app - simply do not have a slot for at all) purely as a
 * legacy JUSCII/Macedonian-typewriter-heritage filler; it is not part of the real 30-letter Serbian
 * (Vuk Karadžić) alphabet at all, so including it would be actively wrong for Serbian, not merely redundant.
 *
 * Unlike [GreekLayout], no long-press accent system is needed: Serbian Cyrillic's five letters not shared
 * with Russian/Bulgarian Cyrillic (Ђ/Љ/Њ/Ћ/Џ) are standalone code points, not diacritic composites of a
 * plainer base letter the way Latin `č`/`ć`/`đ`/`š`/`ž` are - so, like [GreekLayout] itself (a different but
 * also genuinely complete, self-contained alphabet), this layout takes no `letterHints` parameter and ships
 * no `hints.tsv`/`diacritics.tsv` for its pack (see that object's own KDoc for the identical reasoning).
 *
 * The number row and the control/bottom row mirror [KeyboardLayout] exactly (same [KeyProportions] wiring,
 * the same combined emoji/?123 key and enter), so switching alphabets only changes the letters.
 */
object SerbianLayout {
    
    private const val ROW_TOP = "љњертзуиопшђж"
    private const val ROW_MIDDLE = "асдфгхјклчћ"
    private const val ROW_BOTTOM = "џцвбнм"
    
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
     * Builds the Serbian Cyrillic keyboard for the given configuration.
     *
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @param urlMode D-143: true for a recognised URL-entry field - replaces the bottom row's comma/space/
     *        period trio with [KeyboardLayout.urlBottomRow] (URL entry doesn't depend on the active typing
     *        alphabet, so it's identical to the Latin layout's); defaults to false.
     * @param emailMode D-158: true for a recognised email-address field - replaces the bottom row's
     *        comma/space/period trio with [KeyboardLayout.emailBottomRow] (identical reasoning to
     *        [urlMode]); defaults to false. Ignored when [urlMode] is also true.
     * @param locale D-143 / D-158: the system locale [KeyboardLayout.urlBottomRow]'s/[KeyboardLayout.
     *        emailBottomRow]'s period key resolves its TLD popup from ([UrlLocale]); only meaningful when
     *        [urlMode] or [emailMode] is true.
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true,
        urlMode: Boolean = false,
        emailMode: Boolean = false,
        locale: Locale = Locale.getDefault()
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            result.add("1234567890".map { c -> numberKey(c) })
        }
        
        result.add(ROW_TOP.map { c -> charKey(c) })
        result.add(ROW_MIDDLE.map { c -> charKey(c) })
        
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(ROW_BOTTOM.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            ROW_BOTTOM.forEach { c -> add(charKey(c, weight = thirdRowLetterWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(
            if (urlMode) {
                // D-143: URL entry doesn't depend on the active typing alphabet - identical to the Latin
                // layout's own URL-mode bottom row.
                KeyboardLayout.urlBottomRow(proportions, locale)
            } else if (emailMode) {
                // D-158: same reasoning - identical to the Latin layout's own email-mode bottom row.
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
    
    /**
     * D-105: see [KeyboardLayout.numberKey] - identical treatment. D-282: the `0` reversal this KDoc used
     * to mention is gone here too, for the same reason - [AdaptKeyboardView.openPopup] now handles it
     * dynamically.
     */
    private fun numberKey(c: Char): Key {
        val hint = NUMBER_HINTS[c]
        val superscript = NUMBER_SUPERSCRIPTS[c]
        val alternatives = if (hint != null && superscript != null) listOf(hint, superscript) else emptyList()
        return charKey(c, hint, alternatives = alternatives)
    }
}

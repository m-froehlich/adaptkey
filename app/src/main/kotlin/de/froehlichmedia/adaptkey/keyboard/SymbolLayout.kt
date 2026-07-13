// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * D-92 / D-100: the two `?123` pages, reached by a long-press or upward swipe on the combined emoji /
 * `?123` key (L-03), or a D-19 full-field swipe. Page 1 is a genuine calculator layout (digit block,
 * arithmetic operators, currency, decimal separator). Page 2 (D-102) is the fallback place to reach
 * digits and their shifted symbols when the main page's number row is hidden (C-09) - plus the handful of
 * other leftover characters and the main page's letter alt-hints, distributed here since there's room.
 * D-100 removed the page-toggle key from both pages entirely - the D-19 swipe (or the `ABC` detour back to
 * the letters page) is now the only way to move between them. Mirrors the alpha layout's row shape
 * (proportions, weights) so the same [KeyProportions] configuration (C-01) applies unchanged.
 */
object SymbolLayout {
    
    // D-101 (corrected): ( and ) stay two separate keys, each with the matching bracket family of its own
    // kind (opening / closing) as its D-01 popup - not merged into one key. §29 follow-up: square brackets
    // before curly braces.
    private val OPEN_BRACKET_ALTERNATIVES = listOf("(", "[", "{", "<")
    private val CLOSE_BRACKET_ALTERNATIVES = listOf(")", "]", "}", ">")
    
    // Page 1 (calculator) row 1: everyday symbols not already reachable via a letter-page long-press hint.
    private const val CALC_ROW1_SYMBOLS = "()°√π~&|"
    
    // Page 1 (calculator): ×'s alt-popup is the asterisk and the German "Malpunkt" middle dot; ÷'s mirrors
    // it with the slash and colon; ='s offers the arrow, "approximately" and the German "Gleich mit Dach".
    private val MULTIPLY_ALTERNATIVES = listOf("*", "×", "·")
    private val DIVIDE_ALTERNATIVES = listOf("/", "÷", ":")
    private val EQUALS_ALTERNATIVES = listOf("=", "→", "≈", "≙")
    
    // Page 1: this page's own 2 / 3 keys (not the main number row) carry ² / ³ as a long-press hint.
    private const val SQUARED_HINT = "²"
    private const val CUBED_HINT = "³"
    
    // Page 1's own 0 key carries # as a long-press hint - needed for fields marked as a phone number.
    private const val HASH_HINT = "#"
    
    /**
     * §31: the calculator page's minus key (U+2212, not the ASCII hyphen). Exposed so
     * [KeyboardLayout.hasLongPressAction] and [de.froehlichmedia.adaptkey.AdaptKeyService]'s long-press-to-
     * flip-sign handling can recognise this specific key without duplicating the literal.
     */
    const val MINUS_SIGN = '−'
    
    // D-100: the digit block is narrower than before, freeing a right-hand column that runs the full
    // height of the page (backspace / space / currency / optional ABC / enter). Every cell down that
    // column from row 2 (space) to row 5 (enter) shares this weight - since every one of those rows has
    // the same number of other cells (4: three digits/operators + the column cell), a shared weight value
    // gives them all the same pixel width automatically (no per-row weight recalculation needed). Row 1
    // (backspace) is the one exception - it has a different cell count/composition, so it keeps
    // proportions.backspaceWeight (L-04) rather than trying to force-match this column.
    private const val CALC_COLUMN_WEIGHT = 1f
    
    // §29 follow-up: page 2's leftover characters, not reachable elsewhere, plus € (moved here from the
    // letter-hints row below - it now leads the row) and a handful of new additions: ® next to © (the
    // circled C/R pair), Ø ("Durchschnitt", the German shorthand for "average" - also added as an alt on
    // the letters page's o key), and ƒ (the function symbol, also newly added to the letters page's f key).
    // • moved ahead of @ (correction). The underscore that used to live here on its own is gone - now
    // redundant with the "-" key's own _ alt below. The apostrophe that used to live here moved to the
    // letter-hints row (still below); ± moved there too (correction - the plain + stays put, it was never
    // meant to move); the bracket family that used to live here moved to page 1 (D-101).
    private const val CATCHALL_ROW1_SYMBOLS = "€•@©®Øƒ"
    
    // D-102: a fixed digit row, independent of C-09 - the fallback for anyone who has hidden the main
    // page's number row.
    private const val CATCHALL_DIGITS = "1234567890"
    
    // D-102: the main number row's shifted symbols (L-06 / NUMBER_HINTS, in 1..0 order), directly
    // tappable here rather than hidden behind a long-press - the whole point is restoring easy access to
    // them when the main number row (and its long-press hints) are hidden.
    private const val CATCHALL_NUMBER_SYMBOLS = "!\"§$%&/()="
    
    // §29 follow-up: the main letter page's remaining alt-hint symbols, distributed here now that there's
    // room. € moved out to CATCHALL_ROW1_SYMBOLS above (its own first position there); the apostrophe moved
    // in right after #, so it roughly sits under row 3's own " above it; ± moved in at the very end
    // (correction - + was never meant to move, it stays in its original spot). ° moved ahead of + so the
    // arithmetic operators (- + × ÷) aren't split apart (correction). ^ inserted at position 3 to fill the
    // row out to 10 keys, matching rows 2 and 3's width (correction). / (v's hint, D-96) is still skipped -
    // already covered by CATCHALL_NUMBER_SYMBOLS (7's shifted symbol). π keeps its own Greek-letter popup
    // (D-99) on the letters page rather than being duplicated here. Still a first draft, per the user's own
    // "schauen wir, wie sich das ergibt und sortieren ggf. nochmal um".
    private const val CATCHALL_LETTER_HINTS = "#'^-°+×÷*±"
    
    // D-102 (correction): this page's € key (now the first key of row 1 above) also gets the common-currency
    // popup - redundant with page 1's dedicated currency key, by explicit request. It sits on the *left*
    // here, unlike page 1's, so the base glyph stays first (grows rightward), not reversed like
    // CalculatorLocale.COMMON_CURRENCY_SYMBOLS.
    private val CATCHALL_CURRENCY_ALTERNATIVES = listOf("€", "$", "£", "¥")
    
    // D-102 (correction): this page's distributed "-" key (m's hint) gets "_" as its single long-press alt -
    // which is also why the standalone underscore key in row 1 above could be dropped (§29 follow-up).
    private const val UNDERSCORE_HINT = "_"
    
    /**
     * Builds the symbol/numeric keyboard for [page]. Both pages' `ABC` key is always present in the
     * returned rows, regardless of whether the combined `?123` key (D-59) is enabled - its slot must stay
     * reserved (page 1 for the calculator column's proportions, D-100; page 2 so `space` doesn't grow to
     * fill the gap and look oversized). [AdaptKeyboardView] hides it visually and makes it inert when the
     * setting is off, the same way it already treats the disabled combined key.
     *
     * @param page the symbol page, 1 (calculator) or 2 (leftover catch-all)
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param locale the system locale the calculator page's currency key and decimal/thousands
     *        separators are resolved from ([CalculatorLocale]); defaults to the JVM default locale
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     * @throws IllegalArgumentException if [page] is not 1 or 2
     */
    fun rows(
        page: Int,
        proportions: KeyProportions = KeyProportions.DEFAULT,
        locale: Locale = Locale.getDefault()
    ): List<List<Key>> {
        requireValidPage(page)
        return if (page == 1) calculatorRows(proportions, locale) else catchAllRows(proportions)
    }
    
    private fun calculatorRows(proportions: KeyProportions, locale: Locale): List<List<Key>> {
        val format = CalculatorLocale.resolve(locale)
        val result = ArrayList<List<Key>>()
        
        // Row 1: symbols, then backspace - the top of the new right-hand column (D-100). No page-toggle
        // key any more.
        result.add(buildList {
            CALC_ROW1_SYMBOLS.forEach { c -> add(calcRow1Key(c)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        // Row 2 (corrected): 7 8 9 + | space. Operator column reordered top to bottom: + − × ÷.
        result.add(
            listOf(
                charKey('7'),
                charKey('8'),
                charKey('9'),
                charKey('+'),
                Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        // Row 3 (corrected): 4 5 6 − | currency.
        result.add(
            listOf(
                charKey('4'),
                charKey('5'),
                charKey('6'),
                charKey(MINUS_SIGN),
                charKey(format.currencyBase, alternatives = format.currencyAlternatives, weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        // Row 4 (corrected): 1 2 3 × | = ("=" always visible in the column, ABC tucked into the grid
        // instead, since ABC is the rarer action of the two).
        result.add(
            listOf(
                charKey('1'),
                charKey('2', hint = SQUARED_HINT),
                charKey('3', hint = CUBED_HINT),
                charKey('×', alternatives = MULTIPLY_ALTERNATIVES),
                charKey('=', alternatives = EQUALS_ALTERNATIVES, weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        // Row 5 (corrected): 0 (under 1, with a # long-press alt for phone-number-style fields), decimal
        // separator (under 2), ABC (under 3 - D-59/D-93-gated but always emitted here so the row keeps its
        // cell count; AdaptKeyboardView hides it when disabled), ÷ (under the operator column) | enter.
        result.add(
            listOf(
                charKey('0', hint = HASH_HINT),
                charKey(format.decimalSeparator, hint = format.thousandsSeparatorHint),
                Key(label = "ABC", code = KeyCode.LETTERS),
                charKey('÷', alternatives = DIVIDE_ALTERNATIVES),
                Key(label = "↵", code = KeyCode.ENTER, weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        return result
    }
    
    private fun catchAllRows(proportions: KeyProportions): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        // Row 1: leftover symbols (led by €, §29 follow-up), then backspace. No page-toggle key (D-100).
        result.add(buildList {
            CATCHALL_ROW1_SYMBOLS.forEach { c -> add(catchAllRow1Key(c)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        // Row 2 (D-102): the fixed digit row, independent of C-09.
        result.add(CATCHALL_DIGITS.map { c -> charKey(c) })
        
        // Row 3 (D-102): the main number row's shifted symbols, directly tappable. ( and ) also get their
        // bracket-family popups (correction), matching page 1's bracket keys.
        result.add(CATCHALL_NUMBER_SYMBOLS.map { c -> catchAllNumberSymbolKey(c) })
        
        // Row 4 (D-102): the main letter page's remaining alt-hint symbols, distributed here. - also gets
        // _ as its alt (correction), redundant with elsewhere by explicit request.
        result.add(CATCHALL_LETTER_HINTS.map { c -> catchAllLetterHintKey(c) })
        
        // Row 5 (corrected): ABC's slot next to space stays reserved when hidden (D-59/D-100-style, via
        // AdaptKeyboardView) rather than collapsing (D-93's old omit-and-grow) - so space doesn't grow to
        // fill the gap and end up looking oversized when the combined ?123 key is disabled.
        result.add(
            listOf(
                Key(label = "ABC", code = KeyCode.LETTERS, weight = proportions.symbolWeight),
                Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight),
                Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight)
            )
        )
        
        return result
    }
    
    private fun requireValidPage(page: Int) {
        require(page == 1 || page == 2) { "page must be 1 or 2: $page" }
    }
    
    /**
     * D-101 (corrected): gives `(` / `)` their own bracket-family popup. §29 follow-up: gives `π` its
     * Greek-letter popup too, reusing [KeyboardLayout.PI_ALTERNATIVES] so both π keys stay in sync. Every
     * other row-1 symbol is plain.
     */
    private fun calcRow1Key(c: Char): Key {
        return when (c) {
            '(' -> charKey(c, alternatives = OPEN_BRACKET_ALTERNATIVES)
            ')' -> charKey(c, alternatives = CLOSE_BRACKET_ALTERNATIVES)
            'π' -> charKey(c, alternatives = KeyboardLayout.PI_ALTERNATIVES)
            else -> charKey(c)
        }
    }
    
    /** D-102 (corrected): gives `(` / `)` in the shifted-symbols row their own bracket-family popup too. */
    private fun catchAllNumberSymbolKey(c: Char): Key {
        return when (c) {
            '(' -> charKey(c, alternatives = OPEN_BRACKET_ALTERNATIVES)
            ')' -> charKey(c, alternatives = CLOSE_BRACKET_ALTERNATIVES)
            else -> charKey(c)
        }
    }
    
    /** §29 follow-up: gives `€` (now leading row 1) the common-currency popup; the rest stay plain. */
    private fun catchAllRow1Key(c: Char): Key {
        return if (c == '€') charKey(c, alternatives = CATCHALL_CURRENCY_ALTERNATIVES) else charKey(c)
    }
    
    /** D-102 (corrected): gives `-` an `_` alt; the rest of this row stays plain. */
    private fun catchAllLetterHintKey(c: Char): Key {
        return when (c) {
            '-' -> charKey(c, hint = UNDERSCORE_HINT)
            else -> charKey(c)
        }
    }
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
}

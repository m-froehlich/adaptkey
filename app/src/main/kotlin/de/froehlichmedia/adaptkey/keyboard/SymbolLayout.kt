// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * D-92 / D-100: the two `?123` pages, reached by a long-press or upward swipe on the combined emoji /
 * `?123` key (L-03), or a D-19 full-field swipe. Page 1 is a genuine calculator layout (digit block,
 * arithmetic operators, currency, decimal separator); page 2 is a leftover/catch-all page for the handful
 * of characters not already reachable elsewhere (a main-page letter hint, the comma/period popups, or page
 * 1 itself). D-100 removed the page-toggle key from both pages entirely - the D-19 swipe (or the `ABC`
 * detour back to the letters page) is now the only way to move between them. Mirrors the alpha layout's
 * row shape (proportions, weights) so the same [KeyProportions] configuration (C-01) applies unchanged.
 */
object SymbolLayout {
    
    // D-101: the ( key's long-press popup absorbs the whole bracket family - ) plus the curly and angle
    // pairs - so page 1 needs only one bracket key, not two.
    private const val BRACKET_KEY = '('
    private val BRACKET_ALTERNATIVES = listOf("(", ")", "{", "}", "[", "]", "<", ">")
    
    // Page 1 (calculator) row 1: everyday symbols not already reachable via a letter-page long-press hint.
    private const val CALC_ROW1_SYMBOLS = "(°√π~&|"
    
    // Page 1 (calculator): ×'s alt-popup is the asterisk and the German "Malpunkt" middle dot; ÷'s mirrors
    // it with the slash and colon; ='s offers the arrow, "approximately" and the German "Gleich mit Dach".
    private val MULTIPLY_ALTERNATIVES = listOf("*", "×", "·")
    private val DIVIDE_ALTERNATIVES = listOf("/", "÷", ":")
    private val EQUALS_ALTERNATIVES = listOf("=", "→", "≈", "≙")
    
    // Page 1: this page's own 2 / 3 keys (not the main number row) carry ² / ³ as a long-press hint.
    private const val SQUARED_HINT = "²"
    private const val CUBED_HINT = "³"
    
    // D-100: the digit block is narrower than before, freeing a right-hand column that runs the full
    // height of the page (backspace / space / currency / optional ABC / enter). Every cell down that
    // column from row 2 (space) to row 5 (enter) shares this weight - since every one of those rows has
    // the same number of other cells (4: three digits/operators + the column cell), a shared weight value
    // gives them all the same pixel width automatically (no per-row weight recalculation needed). Row 1
    // (backspace) is the one exception - it has a different cell count/composition, so it keeps
    // proportions.backspaceWeight (L-04) rather than trying to force-match this column.
    private const val CALC_COLUMN_WEIGHT = 1f
    
    // Page 2 (catch-all) row 1: the German-locale audit's leftover characters, not reachable elsewhere.
    private const val CATCHALL_ROW1_SYMBOLS = "@_\"'•©±"
    
    // Page 2 row 2: the bracket family, useful for code/math outside the calculator page. No sentence
    // punctuation on this page by design - already well reachable on the main letter page.
    private const val CATCHALL_BRACKETS = "{}[]<>"
    
    /**
     * Builds the symbol/numeric keyboard for [page].
     *
     * @param page the symbol page, 1 (calculator) or 2 (leftover catch-all)
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param symbolKeyEnabled whether the combined `?123` key (D-59) is enabled; when it is off, the
     *        back-to-letters (`ABC`) key on this layer is redundant with the D-19 full-field swipe, so it
     *        is dropped and the remaining keys in its row grow to fill the freed space (D-93)
     * @param locale the system locale the calculator page's currency key and decimal/thousands
     *        separators are resolved from ([CalculatorLocale]); defaults to the JVM default locale
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     * @throws IllegalArgumentException if [page] is not 1 or 2
     */
    fun rows(
        page: Int,
        proportions: KeyProportions = KeyProportions.DEFAULT,
        symbolKeyEnabled: Boolean = true,
        locale: Locale = Locale.getDefault()
    ): List<List<Key>> {
        requireValidPage(page)
        return if (page == 1) calculatorRows(proportions, symbolKeyEnabled, locale) else catchAllRows(proportions, symbolKeyEnabled)
    }
    
    private fun calculatorRows(proportions: KeyProportions, symbolKeyEnabled: Boolean, locale: Locale): List<List<Key>> {
        val format = CalculatorLocale.resolve(locale)
        val result = ArrayList<List<Key>>()
        
        // Row 1: symbols, then backspace - the top of the new right-hand column (D-100). No page-toggle
        // key any more.
        result.add(buildList {
            CALC_ROW1_SYMBOLS.forEach { c -> add(calcRow1Key(c)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        // Row 2: 7 8 9 ÷ | space.
        result.add(
            listOf(
                charKey('7'),
                charKey('8'),
                charKey('9'),
                charKey('÷', alternatives = DIVIDE_ALTERNATIVES),
                Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        // Row 3: 4 5 6 × | currency.
        result.add(
            listOf(
                charKey('4'),
                charKey('5'),
                charKey('6'),
                charKey('×', alternatives = MULTIPLY_ALTERNATIVES),
                charKey(format.currencyBase, alternatives = format.currencyAlternatives, weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        // Row 4: 1 2 3 − | ABC (optional, D-59/D-93).
        result.add(buildList {
            add(charKey('1'))
            add(charKey('2', hint = SQUARED_HINT))
            add(charKey('3', hint = CUBED_HINT))
            add(charKey('−'))
            if (symbolKeyEnabled) {
                add(Key(label = "ABC", code = KeyCode.LETTERS, weight = CALC_COLUMN_WEIGHT))
            }
        })
        
        // Row 5: 0 (under 1), decimal separator (under 2), = (under 3), + (under the operator column) | enter.
        result.add(
            listOf(
                charKey('0'),
                charKey(format.decimalSeparator, hint = format.thousandsSeparatorHint),
                charKey('=', alternatives = EQUALS_ALTERNATIVES),
                charKey('+'),
                Key(label = "↵", code = KeyCode.ENTER, weight = CALC_COLUMN_WEIGHT)
            )
        )
        
        return result
    }
    
    private fun catchAllRows(proportions: KeyProportions, symbolKeyEnabled: Boolean): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        // Row 1: symbols, then backspace. No page-toggle key any more (D-100).
        result.add(buildList {
            CATCHALL_ROW1_SYMBOLS.forEach { c -> add(charKey(c)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(CATCHALL_BRACKETS.map { c -> charKey(c) })
        
        result.add(buildList {
            if (symbolKeyEnabled) {
                add(Key(label = "ABC", code = KeyCode.LETTERS, weight = proportions.symbolWeight))
            }
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
            add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
        })
        
        return result
    }
    
    private fun requireValidPage(page: Int) {
        require(page == 1 || page == 2) { "page must be 1 or 2: $page" }
    }
    
    /** D-101: gives [BRACKET_KEY] its bracket-family popup; every other row-1 symbol is a plain key. */
    private fun calcRow1Key(c: Char): Key {
        return if (c == BRACKET_KEY) charKey(c, alternatives = BRACKET_ALTERNATIVES) else charKey(c)
    }
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
}

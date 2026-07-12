// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-92: the two `?123` pages, reached by a long-press or upward swipe on the combined emoji / `?123` key
 * (L-03). Page 1 is a genuine calculator layout (digit block, arithmetic operators, currency, decimal
 * separator); page 2 is a leftover/catch-all page for the handful of characters not already reachable
 * elsewhere (a main-page letter hint, the comma/period popups, or page 1 itself). Mirrors the alpha
 * layout's row shape (proportions, weights) so the same [KeyProportions] configuration (C-01) applies
 * unchanged.
 */
object SymbolLayout {
    
    // Page 1 (calculator) row 1: everyday symbols not already reachable via a letter-page long-press hint.
    private const val CALC_ROW1_SYMBOLS = "()°√π~&|"
    private const val CALC_ROW1_SYMBOL_COUNT = 8
    
    // Page 1 (calculator): ×'s alt-popup is the asterisk and the German "Malpunkt" middle dot; ÷'s mirrors
    // it with the slash and colon; ='s offers the arrow, "approximately" and the German "Gleich mit Dach".
    private val MULTIPLY_ALTERNATIVES = listOf("*", "×", "·")
    private val DIVIDE_ALTERNATIVES = listOf("/", "÷", ":")
    private val EQUALS_ALTERNATIVES = listOf("=", "→", "≈", "≙")
    
    // Page 1: this page's own 2 / 3 keys (not the main number row) carry ² / ³ as a long-press hint.
    private const val SQUARED_HINT = "²"
    private const val CUBED_HINT = "³"
    
    // Page 1: one consolidated currency key - € is the base glyph (the only actively selectable input
    // alphabets, German and Greek, are both Eurozone currencies), with the other common currencies one
    // long-press away.
    private const val CURRENCY_BASE = '€'
    private val CURRENCY_ALTERNATIVES = listOf("$", "£", "€", "¥")
    
    // Page 1: the decimal separator - comma is what both actively selectable alphabets (German, Greek)
    // use - with the full stop as its long-press thousands-separator alternative.
    private const val DECIMAL_SEPARATOR = ','
    private const val THOUSANDS_SEPARATOR_HINT = "."
    
    // Page 1's bottom row is the most crowded row in the whole keyboard (D-92), so its space key is
    // deliberately smaller than the letter/page-2 space bar (C-01's spaceWeight), placed inline directly
    // left of Enter rather than the requested "stacked above Enter" (which would need a layout-engine
    // extension for partial-height rows - flagged in the spec as a follow-up, not built here).
    private const val CALC_SPACE_WEIGHT = 1.5f
    
    // Page 2 (catch-all) row 1: the German-locale audit's leftover characters, not reachable elsewhere.
    private const val CATCHALL_ROW1_SYMBOLS = "@_\"'•©±"
    private const val CATCHALL_ROW1_SYMBOL_COUNT = 7
    
    // Page 2 row 2: the bracket family, useful for code/math outside the calculator page. No sentence
    // punctuation on this page by design - already well reachable on the main letter page.
    private const val CATCHALL_BRACKETS = "{}[]<>"
    
    /**
     * Builds the symbol/numeric keyboard for [page].
     *
     * @param page the symbol page, 1 (calculator) or 2 (leftover catch-all)
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param symbolKeyEnabled whether the combined `?123` key (D-59) is enabled; when it is off, the
     *        dedicated page-toggle (`1/2`) and back-to-letters (`ABC`) keys on this layer are redundant
     *        with the D-19 full-field swipe, so they are dropped and the remaining keys in their rows
     *        grow to fill the freed space (D-93)
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     * @throws IllegalArgumentException if [page] is not 1 or 2
     */
    fun rows(page: Int, proportions: KeyProportions = KeyProportions.DEFAULT, symbolKeyEnabled: Boolean = true): List<List<Key>> {
        requireValidPage(page)
        return if (page == 1) calculatorRows(proportions, symbolKeyEnabled) else catchAllRows(proportions, symbolKeyEnabled)
    }
    
    private fun calculatorRows(proportions: KeyProportions, symbolKeyEnabled: Boolean): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        val symbolWeight = proportions.thirdRowLetterWeight(CALC_ROW1_SYMBOL_COUNT)
        
        result.add(buildList {
            CALC_ROW1_SYMBOLS.forEach { c -> add(charKey(c, weight = symbolWeight)) }
            if (symbolKeyEnabled) {
                add(Key(label = pageLabel(1), code = KeyCode.SYMBOL_PAGE, weight = proportions.shiftWeight))
            }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(listOf(charKey('7'), charKey('8'), charKey('9'), charKey('÷', alternatives = DIVIDE_ALTERNATIVES)))
        result.add(listOf(charKey('4'), charKey('5'), charKey('6'), charKey('×', alternatives = MULTIPLY_ALTERNATIVES)))
        result.add(
            listOf(
                charKey('1'),
                charKey('2', hint = SQUARED_HINT),
                charKey('3', hint = CUBED_HINT),
                charKey('−')
            )
        )
        
        result.add(buildList {
            if (symbolKeyEnabled) {
                add(Key(label = "ABC", code = KeyCode.LETTERS, weight = proportions.symbolWeight))
            }
            add(charKey(CURRENCY_BASE, alternatives = CURRENCY_ALTERNATIVES))
            add(charKey('0'))
            add(charKey(DECIMAL_SEPARATOR, hint = THOUSANDS_SEPARATOR_HINT, weight = proportions.commaWeight))
            add(charKey('+'))
            add(charKey('=', alternatives = EQUALS_ALTERNATIVES))
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = CALC_SPACE_WEIGHT))
            add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
        })
        
        return result
    }
    
    private fun catchAllRows(proportions: KeyProportions, symbolKeyEnabled: Boolean): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        val symbolWeight = proportions.thirdRowLetterWeight(CATCHALL_ROW1_SYMBOL_COUNT)
        
        result.add(buildList {
            CATCHALL_ROW1_SYMBOLS.forEach { c -> add(charKey(c, weight = symbolWeight)) }
            if (symbolKeyEnabled) {
                add(Key(label = pageLabel(2), code = KeyCode.SYMBOL_PAGE, weight = proportions.shiftWeight))
            }
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
    
    /**
     * Toggles between the two symbol pages (L-03).
     *
     * @param page the current page, 1 or 2
     * @return the other page
     * @throws IllegalArgumentException if [page] is not 1 or 2
     */
    fun togglePage(page: Int): Int {
        requireValidPage(page)
        return if (page == 1) 2 else 1
    }
    
    private fun requireValidPage(page: Int) {
        require(page == 1 || page == 2) { "page must be 1 or 2: $page" }
    }
    
    private fun pageLabel(page: Int): String {
        return if (page == 1) "1/2" else "2/2"
    }
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
}

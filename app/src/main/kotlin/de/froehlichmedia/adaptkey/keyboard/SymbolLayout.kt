// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Numeric / symbol key map (L-03 "?123"), reached by a long-press or upward swipe on the combined
 * emoji / ?123 key. Rarely needed - L-05 / L-06 already put the most common symbols on the letter
 * view - so it is kept to two pages. Mirrors the alpha layout's row shape (proportions, weights) so
 * the same [KeyProportions] configuration (C-01) applies unchanged.
 */
object SymbolLayout {
    
    private const val THIRD_ROW_SYMBOL_COUNT = 7
    
    private const val PAGE1_ROW_A = "@#€_&-+()/"
    private const val PAGE1_ROW_B = "*\"':;!?"
    private const val PAGE2_ROW_A = "~`|•√π÷×§±"
    private const val PAGE2_ROW_B = "£¥°={}©"
    
    /**
     * Builds the symbol keyboard for [page].
     *
     * @param page the symbol page, 1 or 2
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param symbolKeyEnabled whether the combined `?123` key (D-59) is enabled; when it is off, the
     *        dedicated page-toggle (`1/2`) and back-to-letters (`ABC`) keys on this layer are redundant
     *        with the D-19 full-field swipe, so they are dropped and the remaining keys in their rows
     *        grow to fill the freed space
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     * @throws IllegalArgumentException if [page] is not 1 or 2
     */
    fun rows(page: Int, proportions: KeyProportions = KeyProportions.DEFAULT, symbolKeyEnabled: Boolean = true): List<List<Key>> {
        requireValidPage(page)
        val rowA = if (page == 1) PAGE1_ROW_A else PAGE2_ROW_A
        val rowB = if (page == 1) PAGE1_ROW_B else PAGE2_ROW_B
        val result = ArrayList<List<Key>>()
        
        result.add("1234567890".map { c -> charKey(c) })
        result.add(rowA.map { c -> charKey(c) })
        
        val rowBWeight = proportions.thirdRowLetterWeight(THIRD_ROW_SYMBOL_COUNT)
        result.add(buildList {
            if (symbolKeyEnabled) {
                add(Key(label = pageLabel(page), code = KeyCode.SYMBOL_PAGE, weight = proportions.shiftWeight))
            }
            rowB.forEach { c -> add(charKey(c, weight = rowBWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(buildList {
            if (symbolKeyEnabled) {
                add(Key(label = "ABC", code = KeyCode.LETTERS, weight = proportions.symbolWeight))
            }
            add(charKey(',', weight = proportions.commaWeight))
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
            add(charKey('.', weight = proportions.periodWeight))
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
    
    private fun charKey(c: Char, weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, weight = weight)
    }
}

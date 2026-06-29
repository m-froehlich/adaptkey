package de.froehlichmedia.adaptkey.keyboard

/**
 * QWERTZ key map (L-01).
 *
 * The persistent number row (L-06) and the AltGr-style long-press hints (L-05) are present as
 * structure; the long-press behaviour itself is not wired up yet. Key proportions (L-02 narrower
 * space / wider comma & full stop, L-04 wider backspace) are driven by a [KeyProportions]
 * configuration (C-01) rather than hard-coded weights.
 */
object KeyboardLayout {
    
    private const val THIRD_ROW_LETTERS = "yxcvbnm"
    
    // L-06: German-QWERTZ shifted symbol per digit, shown as a corner hint.
    private val NUMBER_HINTS = mapOf(
        '1' to "!", '2' to "\"", '3' to "§", '4' to "$", '5' to "%",
        '6' to "&", '7' to "/", '8' to "(", '9' to ")", '0' to "="
    )
    
    // L-05: default AltGr-style secondary symbols on selected letters.
    private val LETTER_HINTS = mapOf(
        'q' to "@", 'e' to "€", 'h' to "#", 'm' to "-", 'n' to "+", 'd' to "°"
    )
    
    /**
     * Builds the keyboard for the given configuration.
     *
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            // L-06: persistent number row with shifted-symbol hints.
            result.add("1234567890".map { c -> charKey(c, NUMBER_HINTS[c]) })
        }
        
        result.add("qwertzuiop".map { c -> charKey(c, LETTER_HINTS[c]) })
        result.add("asdfghjkl".map { c -> charKey(c, LETTER_HINTS[c]) })
        
        // L-04: the backspace surcharge is taken evenly from the third-row letters.
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(THIRD_ROW_LETTERS.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            THIRD_ROW_LETTERS.forEach { c -> add(charKey(c, LETTER_HINTS[c], weight = thirdRowLetterWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(buildList {
            // L-03: combined emoji / numeric-layer key (no panel yet).
            add(Key(label = "?123", code = KeyCode.SYMBOL, weight = proportions.symbolWeight))
            // L-02: comma & full stop widened, space narrowed.
            add(charKey(',', weight = proportions.commaWeight))
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
            add(charKey('.', weight = proportions.periodWeight))
            add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
        })
        
        return result
    }
    
    private fun charKey(c: Char, hint: String? = null, weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, weight = weight)
    }
}

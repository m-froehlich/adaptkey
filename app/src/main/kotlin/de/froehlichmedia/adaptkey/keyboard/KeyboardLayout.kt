package de.froehlichmedia.adaptkey.keyboard

/**
 * Static QWERTZ key map for the scaffold (L-01).
 *
 * Includes structural stubs for the persistent number row (L-06), the AltGr-style
 * long-press hints (L-05) and the adjusted key proportions (L-02 narrower space /
 * wider comma & full stop, L-04 wider backspace). None of the long-press / proportion
 * tuning is wired to behaviour yet — only the structure is present.
 */
object KeyboardLayout {
    
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
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(): List<List<Key>> {
        val numberRow = "1234567890".map { c -> charKey(c, NUMBER_HINTS[c]) }
        val row1 = "qwertzuiop".map { c -> charKey(c, LETTER_HINTS[c]) }
        val row2 = "asdfghjkl".map { c -> charKey(c, LETTER_HINTS[c]) }
        
        val row3 = buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = 1.5f))
            "yxcvbnm".forEach { c -> add(charKey(c, LETTER_HINTS[c])) }
            // L-04: backspace widened relative to its neighbours.
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = 1.5f))
        }
        
        val bottomRow = buildList {
            // L-03: combined emoji / numeric-layer key (stub: no panel yet).
            add(Key(label = "?123", code = KeyCode.SYMBOL, weight = 1.5f))
            // L-02: comma & full stop widened, space narrowed.
            add(charKey(',', weight = 1.3f))
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = 3.5f))
            add(charKey('.', weight = 1.3f))
            add(Key(label = "↵", code = KeyCode.ENTER, weight = 1.5f))
        }
        
        return listOf(numberRow, row1, row2, row3, bottomRow)
    }
    
    private fun charKey(c: Char, hint: String? = null, weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, weight = weight)
    }
}

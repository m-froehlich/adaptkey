// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * QWERTZ key map (L-01).
 *
 * Each key carries its secondary long-press symbol as [Key.hint]: the persistent number row (L-06)
 * uses the German-QWERTZ shifted symbols and letters use the AltGr-style map (L-05 / C-08). The hint
 * is always populated so the long-press function works even when the corner glyphs are hidden; only
 * the drawing is gated by the view. Key proportions (L-02 narrower space / wider comma & full stop,
 * L-04 wider backspace) are driven by a [KeyProportions] configuration (C-01) rather than hard-coded
 * weights.
 */
object KeyboardLayout {
    
    private const val THIRD_ROW_LETTERS = "yxcvbnm"
    
    // L-03: the combined emoji / ?123 key - tap opens the emoji panel, long-press or an upward swipe
    // switches to the numeric/symbol layer (corner-hinted like the other secondary symbols).
    private const val SYMBOL_KEY_LABEL = "😊"
    private const val SYMBOL_KEY_HINT = "123"
    
    // L-06: German-QWERTZ shifted symbol per digit, shown as a corner hint.
    private val NUMBER_HINTS = mapOf(
        '1' to "!", '2' to "\"", '3' to "§", '4' to "$", '5' to "%",
        '6' to "&", '7' to "/", '8' to "(", '9' to ")", '0' to "="
    )
    
    /**
     * D-02 / D-22 / D-44 / D-51: the full-stop key's long-press set - the sentence terminators, laid out left
     * to right in the horizontal popup with the full stop centred (`! . ?`). The popup pre-selects the key's
     * own character (the full stop), so a straight-up release still types `.`.
     */
    val PERIOD_ALTERNATIVES = listOf("!", ".", "?")
    
    /**
     * D-22 / D-44: the comma key's long-press set - the remaining clause punctuation, laid out left to
     * right in the D-44 horizontal popup (`- , : ; / _`). The popup pre-selects the key's own character
     * (the comma), so a straight-up release still types `,`.
     */
    val COMMA_ALTERNATIVES = listOf("-", ",", ":", ";", "/", "_")
    
    /** D-90: the π key's own corner hint. */
    private const val PI_HINT = "π"
    
    /** D-99: the π key's long-press popup - the Greek letters, alongside π itself. */
    private val PI_ALTERNATIVES = listOf(PI_HINT, "α", "β", "γ", "δ", "λ", "ω")
    
    /**
     * L-05 / C-08: default AltGr-style secondary symbols on selected letters. Exposed so the settings
     * layer can offer it as the reset baseline for the configurable per-key map.
     */
    val DEFAULT_LETTER_HINTS = mapOf(
        'q' to "@", 'e' to "€", 'h' to "#", 'm' to "-", 'n' to "+", 'd' to "°",
        // D-90: the math-symbol hint on p, alongside the original AltGr-style set above.
        'p' to PI_HINT,
        // D-96: reorganised math-symbol hints - × moved to x, ÷ moved to c, v gets /, b gets *.
        'x' to "×", 'c' to "÷", 'v' to "/", 'b' to "*",
        'a' to "ä", 'o' to "ö", 'u' to "ü", 's' to "ß"
    )
    
    /**
     * Builds the keyboard for the given configuration.
     *
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @param letterHints the per-letter secondary-symbol map (L-05 / C-08); defaults to [DEFAULT_LETTER_HINTS]
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true,
        letterHints: Map<Char, String> = DEFAULT_LETTER_HINTS
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            // L-06: persistent number row with shifted-symbol hints.
            result.add("1234567890".map { c -> charKey(c, NUMBER_HINTS[c]) })
        }
        
        result.add("qwertzuiop".map { c -> topRowKey(c, letterHints) })
        result.add("asdfghjkl".map { c -> charKey(c, letterHints[c]) })
        
        // L-04: the backspace surcharge is taken evenly from the third-row letters.
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(THIRD_ROW_LETTERS.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            THIRD_ROW_LETTERS.forEach { c -> add(charKey(c, letterHints[c], weight = thirdRowLetterWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(buildList {
            // L-03: combined emoji / numeric-layer key - tap opens the emoji panel, long-press / swipe-up
            // switches to ?123.
            add(Key(label = SYMBOL_KEY_LABEL, code = KeyCode.SYMBOL, hint = SYMBOL_KEY_HINT, weight = proportions.symbolWeight))
            // L-02: comma & full stop widened, space narrowed.
            // D-22: the comma key carries the clause punctuation (; : - _ /) as its long-press popup.
            add(charKey(',', alternatives = COMMA_ALTERNATIVES, weight = proportions.commaWeight))
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
            // D-02 / D-22: the full-stop key carries the sentence terminators (. ! ?).
            add(charKey('.', alternatives = PERIOD_ALTERNATIVES, weight = proportions.periodWeight))
            add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
        })
        
        return result
    }
    
    /**
     * The symbol a long-press on [key] should emit (L-05 / L-06): a character key's secondary
     * [Key.hint], or null when the key has no secondary or is a control key.
     *
     * @param key the pressed key
     * @return the secondary symbol to commit on long-press, or null when there is none
     */
    fun longPressSymbol(key: Key): String? {
        return if (key.code == KeyCode.CHAR) key.hint else null
    }
    
    /**
     * Whether holding [key] past the long-press timeout has an action at all: a character key's single
     * secondary symbol (L-05 / L-06, see [longPressSymbol]), its multi-alternative popup (D-01, two or
     * more [Key.alternatives]), or the combined emoji / ?123 key switching to the numeric/symbol layer
     * (L-03).
     *
     * @param key the pressed key
     * @return true when a long-press on this key should be scheduled
     */
    fun hasLongPressAction(key: Key): Boolean {
        return (key.code == KeyCode.CHAR && (key.hint != null || key.alternatives.size >= 2)) || key.code == KeyCode.SYMBOL
    }
    
    /**
     * D-99: builds a top-row key, giving `p` its Greek-letter popup ([PI_ALTERNATIVES]) - but only while
     * it still carries the default π hint, so a user who has reassigned `p` via the C-08 editor keeps
     * their own single-symbol long-press instead of an unrelated Greek-letter popup.
     */
    private fun topRowKey(c: Char, letterHints: Map<Char, String>): Key {
        val hint = letterHints[c]
        return if (c == 'p' && hint == PI_HINT) charKey(c, hint, alternatives = PI_ALTERNATIVES) else charKey(c, hint)
    }
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
}

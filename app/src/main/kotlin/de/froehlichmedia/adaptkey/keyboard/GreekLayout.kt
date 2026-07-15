// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Modern Greek (monotonic) key map, the alternate alphabet reached via the G-01 language switch.
 *
 * The three letter rows follow the standard Greek touch layout; the number row and the control /
 * bottom row mirror [KeyboardLayout] exactly (same [KeyProportions] wiring, the same combined emoji /
 * ?123 key and enter), so switching alphabets only changes the letters. Stressed vowels carry their
 * tonos form as the [Key.hint] long-press secondary (α→ά, ε→έ, …); because the secondary is itself a
 * letter, the service appends it into the word rather than treating it as a delimiter. The diaeresis
 * forms (ϊ, ϋ) are intentionally left out for now - one long-press slot per key, tonos is far more
 * frequent.
 */
object GreekLayout {
    
    private const val ROW_TOP = "ςερτυθιοπ"
    private const val ROW_MIDDLE = "ασδφγηξκλ"
    private const val ROW_BOTTOM = "ζχψωβνμ"
    
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
    
    /** Tonos (stress-accent) secondary for each stressed vowel, offered as a long-press. */
    val ACCENTS = mapOf(
        'α' to "ά", 'ε' to "έ", 'η' to "ή", 'ι' to "ί", 'ο' to "ό", 'υ' to "ύ", 'ω' to "ώ"
    )
    
    /** Diaeresis (dialytika) secondary for the two vowels that take it (ι, υ); the second long-press slot. */
    val DIAERESIS = mapOf('ι' to "ϊ", 'υ' to "ϋ")
    
    /**
     * Builds the Greek keyboard for the given configuration.
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
            result.add("1234567890".map { c -> numberKey(c) })
        }
        
        result.add(ROW_TOP.map { c -> letterKey(c) })
        result.add(ROW_MIDDLE.map { c -> letterKey(c) })
        
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(ROW_BOTTOM.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            ROW_BOTTOM.forEach { c -> add(letterKey(c, thirdRowLetterWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(buildList {
            // L-03: combined emoji / numeric-layer key, identical to the Latin bottom row.
            add(Key(label = "😊", code = KeyCode.SYMBOL, hint = "123", weight = proportions.symbolWeight))
            // D-22: comma / full-stop carry the same long-press punctuation sets as the Latin layout.
            add(charKey(',', alternatives = KeyboardLayout.COMMA_ALTERNATIVES, weight = proportions.commaWeight))
            add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
            add(charKey('.', alternatives = KeyboardLayout.PERIOD_ALTERNATIVES, weight = proportions.periodWeight))
            add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
        })
        
        return result
    }
    
    /**
     * A Greek letter key with its accent long-press options: the tonos form as the primary (corner hint +
     * pre-selected popup cell), plus the diaeresis form as a second popup cell for ι / υ (D-23). Letters
     * with no accent get a plain key.
     *
     * @param c the Greek letter
     * @param weight the key's row weight
     * @return the key
     */
    private fun letterKey(c: Char, weight: Float = 1f): Key {
        val tonos = ACCENTS[c]
        val diaeresis = DIAERESIS[c]
        val alternatives = if (tonos != null && diaeresis != null) listOf(tonos, diaeresis) else emptyList()
        return charKey(c, hint = tonos, alternatives = alternatives, weight = weight)
    }
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
    
    /** D-105: see [KeyboardLayout.numberKey] - identical treatment, including the `0` reversal. */
    private fun numberKey(c: Char): Key {
        val hint = NUMBER_HINTS[c]
        val superscript = NUMBER_SUPERSCRIPTS[c]
        val alternatives = if (hint != null && superscript != null) listOf(hint, superscript) else emptyList()
        return charKey(c, hint, alternatives = if (c == '0') alternatives.reversed() else alternatives)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

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
     * @param urlMode D-143: true for a recognised URL-entry field - replaces the bottom row's comma/space/
     *        period trio with [KeyboardLayout.urlBottomRow] (URL entry doesn't depend on the active typing
     *        alphabet, so it's identical to the Latin layout's); defaults to false.
     * @param emailMode D-158: true for a recognised email-address field - replaces the bottom row's
     *        comma/space/period trio with [KeyboardLayout.emailBottomRow] (identical reasoning to
     *        [urlMode]); defaults to false. Ignored when [urlMode] is also true.
     * @param locale D-143 / D-158: the system locale [KeyboardLayout.urlBottomRow]'s/[KeyboardLayout.
     *        emailBottomRow]'s period key resolves its TLD popup from ([UrlLocale]); only meaningful when
     *        [urlMode] or [emailMode] is true.
     * @param showEmojiKey D-555: whether the dedicated bottom-row emoji key sits right of the combined
     *        `!?#` key ([KeyboardLayout.lettersBottomRow]; width funded from the L-02 widening). Defaults
     *        to false - every pre-D-555 call site unchanged; the runtime value is the resolved setting
     *        (default ON, hidden while C-27 puts the emoji mode in the combined key's own slot).
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true,
        urlMode: Boolean = false,
        emailMode: Boolean = false,
        locale: Locale = Locale.getDefault(),
        showEmojiKey: Boolean = false
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            // L-06 / D-498: the shared number-row builder - the Greek number row is deliberately the
            // Latin one (German-QWERTZ shifted symbols, D-105 popups), byte-identical by construction.
            result.add("1234567890".map { c -> KeyboardLayout.numberKey(c) })
        }
        
        result.add(ROW_TOP.map { c -> letterKey(c) })
        result.add(ROW_MIDDLE.map { c -> letterKey(c) })
        
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(ROW_BOTTOM.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            ROW_BOTTOM.forEach { c -> add(letterKey(c, thirdRowLetterWeight)) }
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
                // D-555: the shared bottom-row builder - Greek carries the dedicated emoji key
                // like every other geometry (universal, never per-layout gated).
                KeyboardLayout.lettersBottomRow(proportions, showEmojiKey)
            }
        )
        
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
}

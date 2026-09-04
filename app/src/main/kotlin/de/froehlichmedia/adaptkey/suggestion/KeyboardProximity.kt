// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import kotlin.math.abs

/**
 * Physical key adjacency for whichever row geometry ([de.froehlichmedia.adaptkey.keyboard.LayoutKind])
 * is actually being typed on, used to make a typo on a **neighbouring** key a cheap edit during
 * correction (D-28). Two characters are adjacent when their keys touch horizontally, vertically or
 * diagonally; e.g. on the QWERTZ implementation, `z` neighbours `t`, so `komplezz` is a cheap two-edit
 * hop from `komplett`.
 *
 * D-number (this round): this was a single hardcoded QWERTZ [Map] until now - found while trying to run
 * a real AZERTY keyboard-adjacency confusables scan for French's own D-441 language pack and discovering
 * every neighbour-typo correction signal (this interface's own callers, not merely that one scan method)
 * was silently checking German's own physical layout regardless of which layout was actually active. Now
 * a genuine seam, mirroring [de.froehlichmedia.adaptkey.language.LanguageRules]/
 * [de.froehlichmedia.adaptkey.suggestion.DiacriticFolding]'s identical "interface + per-variant object +
 * a registry keyed by the real geometry" shape - see [KeyboardProximityRegistry].
 */
interface KeyboardProximity {
    
    /**
     * Whether [a] and [b] are physically neighbouring keys (never true for the same character - equality
     * is handled separately by the caller's cost function).
     *
     * @param a the first letter
     * @param b the second letter
     * @return true when the two keys are adjacent on this layout
     */
    fun adjacent(a: Char, b: Char): Boolean
    
    /**
     * The keys physically neighbouring [c] on this layout (D-38): used to broaden the correction
     * candidate set so a typo on the very first key (e.g. `eerden` for `werden`) can still be found.
     *
     * @param c the letter
     * @return its neighbouring letters, or an empty set for a non-letter
     */
    fun neighboursOf(c: Char): Set<Char>
}

/**
 * Shared adjacency-map builder for every row-based [KeyboardProximity] implementation - each layout
 * differs only in which characters sit in which row/column, never in the adjacency rule itself (same
 * row or an immediately adjacent row, column index within 1 either way, a good approximation of the
 * on-screen stagger). D-41: the number row is included like any other row, so a digit is an ordinary
 * neighbour character rather than a special case.
 */
internal object RowKeyboardProximity {
    
    fun build(rows: List<String>): Map<Char, Set<Char>> {
        val positions = HashMap<Char, Pair<Int, Int>>()
        rows.forEachIndexed { row, keys ->
            keys.forEachIndexed { col, ch -> positions[ch] = row to col }
        }
        val result = HashMap<Char, Set<Char>>()
        for ((ch, pos) in positions) {
            val set = HashSet<Char>()
            for ((other, otherPos) in positions) {
                if (other != ch && abs(pos.first - otherPos.first) <= 1 && abs(pos.second - otherPos.second) <= 1) {
                    set.add(other)
                }
            }
            result[ch] = set
        }
        return result
    }
}

/** German's QWERTZ geometry ([de.froehlichmedia.adaptkey.keyboard.LayoutKind.LATIN_QWERTZ]). */
object KeyboardProximityQwertz : KeyboardProximity {
    
    private val ROWS = listOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm")
    private val neighbours: Map<Char, Set<Char>> = RowKeyboardProximity.build(ROWS)
    
    override fun adjacent(a: Char, b: Char): Boolean {
        if (a == b) {
            return false
        }
        return neighbours[a]?.contains(b) == true
    }
    
    override fun neighboursOf(c: Char): Set<Char> = neighbours[c] ?: emptySet()
}

/**
 * English's/every other unconfigured Latin language's QWERTY geometry
 * ([de.froehlichmedia.adaptkey.keyboard.LayoutKind.LATIN_QWERTY]) - identical to
 * [KeyboardProximityQwertz] except `y`/`z` swap position (top row vs. bottom row), matching
 * [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout]'s own `TOP_ROW_QWERTY`/`THIRD_ROW_LETTERS_QWERTY`.
 */
object KeyboardProximityQwerty : KeyboardProximity {
    
    private val ROWS = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val neighbours: Map<Char, Set<Char>> = RowKeyboardProximity.build(ROWS)
    
    override fun adjacent(a: Char, b: Char): Boolean {
        if (a == b) {
            return false
        }
        return neighbours[a]?.contains(b) == true
    }
    
    override fun neighboursOf(c: Char): Set<Char> = neighbours[c] ?: emptySet()
}

/**
 * French's AZERTY geometry ([de.froehlichmedia.adaptkey.keyboard.LayoutKind.LATIN_AZERTY]), matching
 * [de.froehlichmedia.adaptkey.keyboard.AzertyLayout]'s own `TOP_ROW`/`MIDDLE_ROW`/`THIRD_ROW` exactly
 * (`q`/`w`/`a`/`z`/`m` move between rows entirely, not merely within one - see that object's own KDoc).
 */
object KeyboardProximityAzerty : KeyboardProximity {
    
    private val ROWS = listOf("1234567890", "azertyuiop", "qsdfghjklm", "wxcvbn")
    private val neighbours: Map<Char, Set<Char>> = RowKeyboardProximity.build(ROWS)
    
    override fun adjacent(a: Char, b: Char): Boolean {
        if (a == b) {
            return false
        }
        return neighbours[a]?.contains(b) == true
    }
    
    override fun neighboursOf(c: Char): Set<Char> = neighbours[c] ?: emptySet()
}

/**
 * The Greek keyboard geometry ([de.froehlichmedia.adaptkey.keyboard.LayoutKind.GREEK]), matching
 * [de.froehlichmedia.adaptkey.keyboard.GreekLayout]'s own `ROW_TOP`/`ROW_MIDDLE`/`ROW_BOTTOM` exactly.
 */
object KeyboardProximityGreek : KeyboardProximity {
    
    private val ROWS = listOf("1234567890", "ςερτυθιοπ", "ασδφγηξκλ", "ζχψωβνμ")
    private val neighbours: Map<Char, Set<Char>> = RowKeyboardProximity.build(ROWS)
    
    override fun adjacent(a: Char, b: Char): Boolean {
        if (a == b) {
            return false
        }
        return neighbours[a]?.contains(b) == true
    }
    
    override fun neighboursOf(c: Char): Set<Char> = neighbours[c] ?: emptySet()
}

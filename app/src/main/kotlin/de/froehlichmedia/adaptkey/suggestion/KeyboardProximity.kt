// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import kotlin.math.abs

/**
 * Physical key adjacency on the QWERTZ layout, used to make a typo on a **neighbouring** key a cheap edit
 * during correction (D-28). Two characters are adjacent when their keys touch horizontally, vertically or
 * diagonally; e.g. `z` neighbours `t`, so `komplezz` is a cheap two-edit hop from `komplett`.
 *
 * D-41: the number row is part of the map, so a digit is an ordinary neighbour character rather than a
 * special case - the `8` key sits above the `i`/`o` gap, so `W8rt` is a cheap `8`→`o` slip from `Wort`.
 *
 * The map is derived from the four rows by column index (a good approximation of the on-screen stagger),
 * so it stays in sync with [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout] by construction.
 */
object KeyboardProximity {
    
    private val ROWS = listOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm")
    
    private val neighbours: Map<Char, Set<Char>> = buildNeighbours()
    
    /**
     * Whether [a] and [b] are physically neighbouring keys (never true for the same character - equality
     * is handled separately by the caller's cost function).
     *
     * @param a the first letter
     * @param b the second letter
     * @return true when the two keys are adjacent on the layout
     */
    fun adjacent(a: Char, b: Char): Boolean {
        if (a == b) {
            return false
        }
        return neighbours[a]?.contains(b) == true
    }
    
    /**
     * The keys physically neighbouring [c] on the layout (D-38): used to broaden the correction candidate
     * set so a typo on the very first key (e.g. `eerden` for `werden`) can still be found.
     *
     * @param c the letter
     * @return its neighbouring letters, or an empty set for a non-letter
     */
    fun neighboursOf(c: Char): Set<Char> {
        return neighbours[c] ?: emptySet()
    }
    
    private fun buildNeighbours(): Map<Char, Set<Char>> {
        val positions = HashMap<Char, Pair<Int, Int>>()
        ROWS.forEachIndexed { row, keys ->
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

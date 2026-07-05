// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import kotlin.math.abs

/**
 * Physical key adjacency on the QWERTZ letter layout, used to make a typo on a **neighbouring** key a
 * cheap edit during correction (D-28). Two letters are adjacent when their keys touch horizontally,
 * vertically or diagonally; e.g. `z` neighbours `t`, so `komplezz` is a cheap two-edit hop from
 * `komplett`.
 *
 * The map is derived from the three letter rows by column index (a good approximation of the on-screen
 * stagger), so it stays in sync with [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout] by construction.
 */
object KeyboardProximity {
    
    private val ROWS = listOf("qwertzuiop", "asdfghjkl", "yxcvbnm")
    
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

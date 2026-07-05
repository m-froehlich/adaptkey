// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Small string-distance helpers shared by the suggestion providers.
 */
object EditDistance {
    
    /**
     * Checks whether two strings differ by at most one single-character edit.
     *
     * @param a the first string
     * @param b the second string
     * @return true if [a] and [b] are equal or differ by exactly one insert, delete, or substitution
     */
    fun atMostOne(a: String, b: String): Boolean {
        val lengthDiff = a.length - b.length
        if (lengthDiff < -1 || lengthDiff > 1) {
            return false
        }
        if (a.length == b.length) {
            var mismatches = 0
            for (i in a.indices) {
                if (a[i] != b[i] && ++mismatches > 1) {
                    return false
                }
            }
            return true
        }
        // Lengths differ by exactly one: check for a single insert/delete.
        val shorter = if (a.length < b.length) a else b
        val longer = if (a.length < b.length) b else a
        var i = 0
        var j = 0
        var edited = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++
                j++
            } else {
                if (edited) {
                    return false
                }
                edited = true
                j++
            }
        }
        return true
    }
    
    /**
     * The minimum weighted edit distance (Levenshtein) between [a] and [b] with a caller-supplied
     * per-substitution cost and a fixed insert/delete cost (D-28). This lets a typo on a neighbouring key
     * count as a cheap edit, so a proximity-aware correction can accept two adjacent-key slips while still
     * rejecting two unrelated ones.
     *
     * @param a the first string
     * @param b the second string
     * @param indelCost the cost of a single insertion or deletion
     * @param substitutionCost the cost of substituting one character for another (should return 0 for equal characters)
     * @return the minimum total edit cost
     */
    fun weightedDistance(a: String, b: String, indelCost: Int, substitutionCost: (Char, Char) -> Int): Int {
        val m = b.length
        var prev = IntArray(m + 1) { it * indelCost }
        var cur = IntArray(m + 1)
        for (i in 1..a.length) {
            cur[0] = i * indelCost
            for (j in 1..m) {
                val substitute = prev[j - 1] + substitutionCost(a[i - 1], b[j - 1])
                val delete = prev[j] + indelCost
                val insert = cur[j - 1] + indelCost
                cur[j] = minOf(substitute, delete, insert)
            }
            val swap = prev
            prev = cur
            cur = swap
        }
        return prev[m]
    }
}

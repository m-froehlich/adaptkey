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
     * D-194 / §125: [maxCost] restricts the computation to a band around the diagonal (Ukkonen's banded
     * edit distance) - reconciling a length difference of `|i-j|` alone needs at least that many indels,
     * so any cell more than `maxCost / indelCost` off the diagonal cannot possibly resolve to a distance
     * `<= maxCost` (every [substitutionCost] in this codebase returns 0 for equal characters, never a
     * negative value, so this bound always holds). Cells outside the band are never evaluated at all,
     * turning the per-call cost from `O(a.length * b.length)` into `O(max(a.length, b.length) * band)` - a
     * constant-width band, not one that grows with the strings, which is what actually mattered for the
     * reported per-keystroke lag on long tokens. Every caller here only ever compares the result against a
     * fixed ceiling, so a value outside the band is safely reported as merely "more than [maxCost]", never
     * a false "close enough". Omitting [maxCost] computes the exact, unbounded distance, identical to the
     * pre-§125 behaviour.
     *
     * @param a the first string
     * @param b the second string
     * @param indelCost the cost of a single insertion or deletion
     * @param maxCost the inclusive cost ceiling the caller actually cares about; results above it are only
     *        guaranteed to exceed it, never exact - defaults to unbounded (exact distance, no banding)
     * @param substitutionCost the cost of substituting one character for another (should return 0 for equal characters)
     * @return the minimum total edit cost, or a value guaranteed to exceed [maxCost] when the true
     *         distance does
     */
    fun weightedDistance(a: String, b: String, indelCost: Int, maxCost: Int = UNREACHABLE, substitutionCost: (Char, Char) -> Int): Int {
        val n = a.length
        val m = b.length
        val band = if (maxCost >= UNREACHABLE || indelCost <= 0) maxOf(n, m) else maxCost / indelCost
        var prev = IntArray(m + 1) { j -> if (j <= band) j * indelCost else UNREACHABLE }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            val loJ = maxOf(0, i - band)
            val hiJ = minOf(m, i + band)
            for (j in 0..m) {
                cur[j] = when {
                    j < loJ || j > hiJ -> UNREACHABLE
                    j == 0 -> i * indelCost
                    else -> {
                        val substitute = prev[j - 1] + substitutionCost(a[i - 1], b[j - 1])
                        val delete = prev[j] + indelCost
                        val insert = cur[j - 1] + indelCost
                        minOf(substitute, delete, insert)
                    }
                }
            }
            val swap = prev
            prev = cur
            cur = swap
        }
        return prev[m]
    }
    
    // D-194: far larger than any realistic edit cost in this app (word lengths are at most a few dozen
    // characters, costs per edit at most a handful), yet with enough headroom below Int.MAX_VALUE that
    // summing it with a small real cost a few times over (see the `substitute`/`delete`/`insert` cells
    // above) can never overflow.
    private const val UNREACHABLE = 1 shl 20
}

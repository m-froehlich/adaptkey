// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

/**
 * A character n-gram frequency profile for one [language] (A-03): an ordered list of n-grams, most
 * frequent first, indexed by rank for the Cavnar-Trenkle out-of-place distance.
 *
 * @property language the language this profile describes
 */
class CharNgramProfile(val language: Language, orderedNgrams: List<String>) {
    
    private val ranks: Map<String, Int> = buildMap {
        orderedNgrams.forEachIndexed { index, ngram ->
            // A duplicate n-gram would be a builder bug; keep the first (best) rank.
            putIfAbsent(ngram, index)
        }
    }
    
    /** The number of ranked n-grams in this profile. */
    val size: Int
        get() = ranks.size
    
    /**
     * @param ngram the n-gram to look up
     * @return its 0-based rank in this profile, or null when the profile does not contain it
     */
    fun rankOf(ngram: String): Int? {
        return ranks[ngram]
    }
}

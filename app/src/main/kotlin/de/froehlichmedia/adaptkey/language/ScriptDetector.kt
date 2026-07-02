// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

/**
 * Detects the fraction of letters written in the Greek script (A-03).
 *
 * Greek uses a script disjoint from every Latin-based language, so it is separated by the character
 * set alone - far more reliably than by n-gram statistics. The classifier uses this as a fast path
 * before falling back to the Cavnar-Trenkle profile distance for the Latin-script languages.
 */
object ScriptDetector {
    
    /**
     * @param ch the character to test
     * @return true when [ch] is a Greek letter (Greek and Coptic, or Greek Extended block)
     */
    fun isGreekLetter(ch: Char): Boolean {
        return (ch in 'Ͱ'..'Ͽ' || ch in 'ἀ'..'῿') && ch.isLetter()
    }
    
    /**
     * The share of letters in [text] that belong to the Greek script.
     *
     * @param text the raw text
     * @return the Greek-letter fraction in `[0, 1]`, or 0 when [text] has no letters
     */
    fun greekFraction(text: String): Double {
        var letters = 0
        var greek = 0
        for (ch in text) {
            if (!ch.isLetter()) {
                continue
            }
            letters++
            if (isGreekLetter(ch)) {
                greek++
            }
        }
        return if (letters == 0) 0.0 else greek.toDouble() / letters
    }
}

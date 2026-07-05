// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Folds German umlauts and the sharp s to their base ASCII forms (ä→a, ö→o, ü→u, ß→ss), so a token
 * typed without the diacritic (e.g. "grun") matches its correct form ("grün") under the single-edit
 * distance (D-12). Applied to both sides before comparing, it makes an umlaut/ß variant a distance-0
 * match rather than an unreachable one; non-German text is left unchanged.
 */
object Umlaut {
    
    /**
     * Returns [text] with every umlaut / sharp-s folded to its ASCII base (uppercase umlauts fold to the
     * lowercase base). Characters without a folding are copied unchanged.
     *
     * @param text the text to fold
     * @return the folded text
     */
    fun fold(text: String): String {
        val builder = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                'ä', 'Ä' -> builder.append('a')
                'ö', 'Ö' -> builder.append('o')
                'ü', 'Ü' -> builder.append('u')
                'ß' -> builder.append("ss")
                else -> builder.append(c)
            }
        }
        return builder.toString()
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * Pure logic for the G-05 gesture: a double-tap on Shift toggles the case of the current word's
 * first character, then commits it immediately (verbatim, no autocorrect).
 * 
 * The case toggle ([flipFirst]) is its own inverse, so the service can both apply and revert it
 * with the same call — though the new immediate-commit design no longer uses a provisional state
 * that would need reverting.
 */
object WordEndShift {
    
    /**
     * Toggles the case of the first character of [word]. A non-letter first character (or an empty
     * string) is returned unchanged; the remaining characters are never touched.
     * 
     * @param word the word whose first character should be flipped
     * @return the word with its first character's case toggled
     */
    fun flipFirst(word: String): String {
        val first = word.firstOrNull() ?: return word
        if (!first.isLetter()) {
            return word
        }
        val flipped = if (first.isUpperCase()) first.lowercaseChar() else first.uppercaseChar()
        return flipped + word.substring(1)
    }
}

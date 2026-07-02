// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

/**
 * Pure helper for the G-02 "delete whole word" gesture: given the committed text before the cursor,
 * it computes how many trailing characters make up the previous word so the caller can delete them
 * in one step instead of a single character.
 *
 * A word is a maximal run of non-whitespace characters. The deletion span covers any trailing
 * whitespace plus that run, so deleting at "foo bar " removes " bar" and leaves "foo", and deleting
 * at "foo bar" removes "bar" and leaves "foo ".
 */
object WordBoundary {
    
    /**
     * Returns the number of characters at the end of [before] that form the previous word together
     * with the whitespace immediately preceding the cursor.
     *
     * @param before the committed text directly before the cursor
     * @return the count to delete; 0 when [before] is empty
     */
    fun wordDeleteLength(before: CharSequence): Int {
        var index = before.length
        // Consume the trailing whitespace run first (so a cursor sitting after a space still deletes
        // the word that precedes it).
        while (index > 0 && before[index - 1].isWhitespace()) {
            index--
        }
        // Then consume the word characters.
        while (index > 0 && !before[index - 1].isWhitespace()) {
            index--
        }
        return before.length - index
    }
}

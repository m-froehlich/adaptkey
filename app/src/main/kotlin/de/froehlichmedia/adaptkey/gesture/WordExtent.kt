// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

/**
 * Pure helper for D-62: when a new composing token starts with the caret sitting inside (or directly
 * against) an already-committed word, the autocorrect / suggestions must see the whole word, not just
 * whatever gets typed from here on. This locates the word-character run immediately touching the caret
 * on each side, so the caller can reclaim it into the composing token.
 *
 * A word character is a letter; a run stops at the first non-letter (or the string boundary).
 */
object WordExtent {
    
    /**
     * The word-character runs immediately touching the caret.
     *
     * @property before the trailing letters of the text before the caret; empty when the caret does not
     *           directly follow a letter
     * @property after the leading letters of the text after the caret; empty when the caret is not
     *           directly followed by a letter
     */
    data class Reclaim(val before: String, val after: String)
    
    /**
     * @param before the committed text directly before the caret
     * @param after the committed text directly after the caret
     * @return the reclaimable word fragments on each side; both empty when the caret touches no word
     */
    fun reclaim(before: CharSequence, after: CharSequence): Reclaim {
        var start = before.length
        while (start > 0 && before[start - 1].isLetter()) {
            start--
        }
        var end = 0
        while (end < after.length && after[end].isLetter()) {
            end++
        }
        return Reclaim(before.substring(start), after.substring(0, end))
    }
}

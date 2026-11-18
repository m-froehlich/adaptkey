// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

/**
 * Pure helper for D-62: when a new composing token starts with the caret sitting inside (or directly
 * against) an already-committed word, the autocorrect / suggestions must see the whole word, not just
 * whatever gets typed from here on. This locates the word-character run immediately touching the caret
 * on each side, so the caller can reclaim it into the composing token.
 *
 * A word character is a letter or the plain apostrophe (D-544: the dedicated apostrophe key's own
 * character is an ordinary word-forming character - it composes, is autocorrected and is learned as
 * part of its word, so a reclaim must keep "ist's"/"sieht's" whole exactly like any letter run);
 * a run stops at the first character that is neither (or the string boundary).
 */
object WordExtent {
    
    /**
     * D-544: the dedicated apostrophe key's own character - the one word-forming non-letter. The
     * single source of truth for the "is this a word character" decision, shared between the
     * reclaim computation below and the service's composing/learning gates (handleKey's
     * token-extension decision, deleteComposingChar's exposed-character check, isEditingMidWord,
     * learnWord/learnWordStrong), so the five can never drift apart on what still counts as the
     * same word. Only the plain ASCII apostrophe (U+0027) counts - the character the key actually
     * types; no dictionary in any language pack carries the typographic variants, and only this
     * one can ever enter a token through the letters surface's own composing path.
     */
    const val APOSTROPHE: Char = '\''
    
    /**
     * Whether [c] forms part of a word: a letter, or D-544's word-forming apostrophe.
     *
     * @param c the character to classify
     * @return true when a composing token / reclaim run may contain [c]
     */
    fun isWordChar(c: Char): Boolean = c.isLetter() || c == APOSTROPHE
    
    /**
     * The word-character runs immediately touching the caret.
     *
     * @property before the trailing word characters of the text before the caret; empty when the caret
     *           does not directly follow a word character
     * @property after the leading word characters of the text after the caret; empty when the caret is
     *           not directly followed by a word character
     */
    data class Reclaim(val before: String, val after: String)
    
    /**
     * @param before the committed text directly before the caret
     * @param after the committed text directly after the caret
     * @return the reclaimable word fragments on each side; both empty when the caret touches no word
     */
    fun reclaim(before: CharSequence, after: CharSequence): Reclaim {
        var start = before.length
        while (start > 0 && isWordChar(before[start - 1])) {
            start--
        }
        var end = 0
        while (end < after.length && isWordChar(after[end])) {
            end++
        }
        return Reclaim(before.substring(start), after.substring(0, end))
    }
}

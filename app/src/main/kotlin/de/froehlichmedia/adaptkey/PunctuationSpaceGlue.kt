// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

/**
 * Pure decision for D-320: whether a digit typed right after an A-12 sentence-punctuation auto-space
 * should glue directly onto the punctuation instead of confirming the auto-space as ordinary text - the
 * digit-vs-decimal-separator case (`3` `.` `1` `4` -> `3.14`, not `3. 14`), symmetric for `.` and `,` (the
 * German decimal separator) since D-320 also extended the auto-space itself to comma.
 *
 * Deliberately narrower than the full auto-space-arming punctuation set (`.!?,`) - `!`/`?` never carry
 * numeric meaning, so `3!4`/`3?4` are left as ordinary confirmed text followed by a new digit.
 */
object PunctuationSpaceGlue {
    
    private const val NUMERIC_PUNCTUATION = ".,"
    
    /**
     * @param textBeforeCursor the text immediately before the cursor, read fresh from the real document at
     *        decision time (mirrors A-07's own "verify against ground truth, never trust a stored flag"
     *        philosophy) - only its last three characters matter; anything shorter never qualifies
     * @return true when the last three characters are, in order, a digit, a `.`/`,`, and the auto-inserted space
     */
    fun gluesDigit(textBeforeCursor: String): Boolean {
        if (textBeforeCursor.length < 3) {
            return false
        }
        val digitBefore = textBeforeCursor[textBeforeCursor.length - 3]
        val punctuation = textBeforeCursor[textBeforeCursor.length - 2]
        val space = textBeforeCursor[textBeforeCursor.length - 1]
        return space == ' ' && punctuation in NUMERIC_PUNCTUATION && digitBefore.isDigit()
    }
}

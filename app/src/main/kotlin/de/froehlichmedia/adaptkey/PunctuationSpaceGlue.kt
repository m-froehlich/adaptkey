// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

/**
 * Pure decision for D-320: whether a digit typed right after an A-12 sentence-punctuation auto-space
 * should glue directly onto the punctuation instead of confirming the auto-space as ordinary text - the
 * digit-vs-decimal-separator case (`3` `.` `1` `4` -> `3.14`, not `3. 14`). `.` always qualifies; `,`
 * qualifies only for a language whose own decimal convention uses a comma ([includeComma], D-410 - see
 * [de.froehlichmedia.adaptkey.language.LanguageRules.decimalCommaGluesDigits]), since D-320 originally
 * extended the auto-space itself to comma specifically for German.
 *
 * Deliberately narrower than the full auto-space-arming punctuation set (`.!?,`) - `!`/`?` never carry
 * numeric meaning, so `3!4`/`3?4` are left as ordinary confirmed text followed by a new digit.
 */
object PunctuationSpaceGlue {
    
    private const val NUMERIC_PUNCTUATION = "."
    private const val COMMA = ','
    
    /**
     * @param textBeforeCursor the text immediately before the cursor, read fresh from the real document at
     *        decision time (mirrors A-07's own "verify against ground truth, never trust a stored flag"
     *        philosophy) - only its last three characters matter; anything shorter never qualifies
     * @param includeComma D-410: whether the active language's own decimal convention uses a comma too,
     *        not only a period; defaults to true (this app's historical, German-only behaviour) so every
     *        existing caller that does not pass a value keeps it unchanged
     * @return true when the last three characters are, in order, a digit, a qualifying punctuation mark,
     *         and the auto-inserted space
     */
    fun gluesDigit(textBeforeCursor: String, includeComma: Boolean = true): Boolean {
        if (textBeforeCursor.length < 3) {
            return false
        }
        val digitBefore = textBeforeCursor[textBeforeCursor.length - 3]
        val punctuation = textBeforeCursor[textBeforeCursor.length - 2]
        val space = textBeforeCursor[textBeforeCursor.length - 1]
        val qualifies = punctuation in NUMERIC_PUNCTUATION || (includeComma && punctuation == COMMA)
        return space == ' ' && qualifies && digitBefore.isDigit()
    }
}

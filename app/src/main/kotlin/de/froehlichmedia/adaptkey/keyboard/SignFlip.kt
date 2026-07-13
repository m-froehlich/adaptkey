// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure helper for §31: long-pressing the calculator page's minus key flips the sign of the number
 * directly before the caret, instead of committing text like every other long-press secondary.
 *
 * A "number" is the maximal trailing run of digits and decimal/thousands separators (`,`/`.` - either
 * one, regardless of which is the active locale's actual decimal separator, since only the number's
 * extent matters here, not its formatting) immediately before the caret.
 */
object SignFlip {
    
    /**
     * The edit to apply: delete [deleteLength] characters immediately before the caret, then insert
     * [insertText] in their place.
     */
    data class Result(val deleteLength: Int, val insertText: String)
    
    /**
     * Computes the sign-flip edit for the text [before] the caret.
     *
     * @param before the committed text directly before the caret
     * @return the edit to apply, or null when there is no number directly before the caret
     */
    fun resultFor(before: CharSequence): Result? {
        var start = before.length
        while (start > 0 && (before[start - 1].isDigit() || before[start - 1] == ',' || before[start - 1] == '.')) {
            start--
        }
        val digits = before.substring(start)
        if (digits.isEmpty()) {
            return null
        }
        return if (start > 0 && before[start - 1] == SymbolLayout.MINUS_SIGN) {
            Result(deleteLength = digits.length + 1, insertText = digits)
        } else {
            Result(deleteLength = digits.length, insertText = "${SymbolLayout.MINUS_SIGN}$digits")
        }
    }
}

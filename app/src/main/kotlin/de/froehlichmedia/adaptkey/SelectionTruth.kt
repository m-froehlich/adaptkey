// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

/**
 * Pure predicate for D-139's onUpdateSelection truth verification (spec §101).
 *
 * Selection-update callbacks are delivered asynchronously, one per edit, and routinely lag behind the
 * IME's own local state during fast typing or multi-step batch edits - a callback can describe an edit one
 * or two behind, or a transient intermediate state of a batch the target editor chose not to coalesce.
 * Comparing such a callback's positions against only the latest expected caret therefore misclassifies the
 * IME's own earlier edits as external caret moves. Synchronous InputConnection *reads*, by contrast, are
 * answered only after every previously-sent mutation has been applied, so they always see the editor's
 * true current state regardless of how far the callback queue lags.
 *
 * The same predicate is applied at both stages of the verification: first against the callback's own
 * reported positions (the cheap, overwhelmingly common in-sync case, no read needed), and - only when that
 * mismatches - against the ground-truth positions read synchronously via `getExtractedText()`. Truth at
 * the expected caret means the mismatching callback was a stale echo of the IME's own edit and must be
 * ignored; truth elsewhere means a genuinely external change (user tap, app-side edit) that the composing
 * state must react to.
 */
object SelectionTruth {
    
    /**
     * Whether a selection is a collapsed caret sitting exactly at the expected position.
     *
     * @param expected the absolute caret position the IME's own latest edit should have produced
     * @param selStart the selection start to test (reported by a callback, or read as ground truth)
     * @param selEnd the selection end to test
     * @return true when the selection is collapsed at exactly [expected]
     */
    fun isAtExpectedCaret(expected: Int, selStart: Int, selEnd: Int): Boolean {
        return selStart == selEnd && selStart == expected
    }
}

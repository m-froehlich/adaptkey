// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

/**
 * Pure decider for D-492's paste-shape fix (2026-10-30): whether a composing token the IME still owns
 * needs its editor-side span re-marked because the target editor silently killed it, and whether that
 * re-mark is safe to perform.
 * 
 * The captured device log (ordinary Chrome text field, "Besuch" pasted then reclaimed): the paste echo
 * armed the debounced D-62 caret-move reclaim, the reclaim took the pasted word's trailing "Besuch"
 * (anchor 9) via `setComposingRegion` + `setComposingText`, and `updateComposing()` scheduled the
 * cosmetic `composingPreviewRunnable` at +200 ms. ~170 ms after the reclaim the editor killed the
 * composing span (echo `candidates=[-1,-1]`) *at an unchanged, still-matching caret* - so
 * `onUpdateSelection`'s own caret check kept the now-stale model, and the text itself was still intact
 * at the anchor, so a content-only verification (the original fix-1 sketch) would have passed too. When
 * the preview runnable then re-sent `setComposingText("Besuch")`, the editor - having no live span -
 * *inserted* the text at the caret instead of replacing anything: "BesuchBesuch", committed by the
 * EXTERNAL branch's `finishComposingText()`.
 * 
 * The only signal that distinguishes this shape is the editor's own composing-span report (the
 * `candidates` pair of the most recent `onUpdateSelection` echo). Spec invariant 2 forbids deciding a
 * *tear-down* from that report (it is not guaranteed to arrive in sync with the selection stream), and
 * this decider honours that asymmetrically: it can only ever
 * 
 * - do nothing (no token owned, no anchor, or the editor last reported a live span),
 * - re-mark ownership in place via `setComposingRegion(start, end)` - never a text write, never a
 *   delete/reinsert (D-182/D-383 precedents) - but only after BOTH a synchronous ground-truth caret
 *   check ([SelectionTruth]) AND a content check confirming the model's own token still sits verbatim
 *   at the believed anchor, or
 * - report the situation unverifiable, in which case the caller must skip any pending *optional* write
 *   (the cosmetic rewrite) rather than risk re-sending composing text over a dead span. A false skip
 *   costs only a colour update; a false pass duplicates text.
 */
object ComposingSpanReMark {
    
    sealed class Decision {
        
        /**
         * Nothing to do: no token is owned, no anchor is known, or the editor's most recent echo still
         * reported a live composing span. The caller may write freely (an ordinary replacing
         * `setComposingText` is safe in the first and third case; in the second, nothing is owned and
         * the caller's normal rules apply).
         */
        object NothingToDo : Decision()
        
        /**
         * The editor reported no span while the model's own token is verified intact at the anchor with
         * the caret at the expected edit point - re-mark exactly this absolute document range as the
         * composing region. Never inserts or deletes anything.
         */
        data class ReMark(val start: Int, val end: Int) : Decision()
        
        /**
         * The no-span report could not be confirmed against ground truth (no extracted text available,
         * or caret/content mismatch). The caller must not re-send composing text over the possibly dead
         * span: an optional cosmetic rewrite is skipped, and no ownership change is attempted.
         */
        object Unverifiable : Decision()
    }
    
    /**
     * @param composing the model's current composing token text
     * @param composingAnchor the absolute document offset the token is believed to start at (-1 unknown)
     * @param expectedCaret the absolute caret the model expects ([composingAnchor] + composingCursor)
     * @param lastEchoSpanPresent whether the most recent onUpdateSelection echo reported a live
     *   composing span (candidatesStart/candidatesEnd both >= 0)
     * @param extractedStartOffset [android.view.inputmethod.ExtractedText.startOffset] of the
     *   synchronous ground-truth read, or 0 when none is available (pair with an empty
     *   [extractedText], which forces [Decision.Unverifiable] whenever a decision is actually needed)
     * @param extractedText the ground-truth document chunk (windowed extraction supported - positions
     *   are relative to [extractedStartOffset], mirroring [ComposingAnchor]'s D-87 rule)
     * @param extractedSelStart [android.view.inputmethod.ExtractedText.selectionStart]
     * @param extractedSelEnd [android.view.inputmethod.ExtractedText.selectionEnd]
     */
    fun decide(
        composing: CharSequence,
        composingAnchor: Int,
        expectedCaret: Int,
        lastEchoSpanPresent: Boolean,
        extractedStartOffset: Int,
        extractedText: CharSequence,
        extractedSelStart: Int,
        extractedSelEnd: Int
    ): Decision {
        if (composing.isEmpty() || composingAnchor < 0 || lastEchoSpanPresent) {
            return Decision.NothingToDo
        }
        if (extractedText.isEmpty()) {
            return Decision.Unverifiable
        }
        val actualStart = ComposingAnchor.resolve(extractedStartOffset, extractedSelStart, 0)
        val actualEnd = ComposingAnchor.resolve(extractedStartOffset, extractedSelEnd, 0)
        if (!SelectionTruth.isAtExpectedCaret(expectedCaret, actualStart, actualEnd)) {
            return Decision.Unverifiable
        }
        val relStart = composingAnchor - extractedStartOffset
        val relEnd = relStart + composing.length
        if (relStart < 0 || relEnd > extractedText.length ||
            extractedText.subSequence(relStart, relEnd).toString() != composing.toString()
        ) {
            return Decision.Unverifiable
        }
        return Decision.ReMark(composingAnchor, composingAnchor + composing.length)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

/**
 * Pure helper for D-62 / D-87: computes the absolute document position of the D-62 mid-word reclaim's
 * composing anchor - where the reclaimed "before" fragment (now the start of the composing token) sits in
 * the real document.
 *
 * [android.view.inputmethod.ExtractedText.selectionStart] is relative to the extracted chunk
 * ([android.view.inputmethod.ExtractedText.text]), not the whole document - it must be added to
 * [android.view.inputmethod.ExtractedText.startOffset] to get the true absolute position. Using
 * `selectionStart` alone (the D-87 bug) happens to work in short/simple fields, where `startOffset` is
 * always 0, but silently miscomputes the anchor - and with it every later "is this our own edit" check in
 * [de.froehlichmedia.adaptkey.AdaptKeyService.onUpdateSelection] - in any field long or paginated enough for
 * the framework to extract a windowed chunk instead of the whole document.
 */
object ComposingAnchor {
    
    /**
     * @param extractedStartOffset [android.view.inputmethod.ExtractedText.startOffset]
     * @param extractedSelectionStart [android.view.inputmethod.ExtractedText.selectionStart]
     * @param reclaimedBeforeLength the length of the D-62 reclaim's "before" fragment
     * @return the absolute document position the reclaimed "before" fragment starts at
     */
    fun resolve(extractedStartOffset: Int, extractedSelectionStart: Int, reclaimedBeforeLength: Int): Int {
        return extractedStartOffset + extractedSelectionStart - reclaimedBeforeLength
    }
}

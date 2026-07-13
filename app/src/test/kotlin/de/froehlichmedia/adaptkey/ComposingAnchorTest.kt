// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-87 fix: [ComposingAnchor] must add the extracted chunk's own start offset, not
 * treat [android.view.inputmethod.ExtractedText.selectionStart] as an absolute document position.
 */
class ComposingAnchorTest {
    
    @Test
    fun `a short field with no windowing (startOffset 0) resolves the same as the raw selection`() {
        // "Hallo W|rt" - the whole field is extracted as one chunk, so startOffset is 0.
        assertEquals(6, ComposingAnchor.resolve(extractedStartOffset = 0, extractedSelectionStart = 7, reclaimedBeforeLength = 1))
    }
    
    @Test
    fun `a long field where the framework only extracted a windowed chunk still resolves correctly`() {
        // The real caret sits at absolute document position 4007, but the extracted chunk only starts at
        // document position 4000, so selectionStart is 7 relative to that chunk - not 4007. The D-87 bug
        // used 7 directly as if it were absolute; the fix adds startOffset back in.
        assertEquals(
            4006,
            ComposingAnchor.resolve(extractedStartOffset = 4000, extractedSelectionStart = 7, reclaimedBeforeLength = 1)
        )
    }
    
    @Test
    fun `a longer reclaimed prefix shifts the anchor further back`() {
        assertEquals(
            4003,
            ComposingAnchor.resolve(extractedStartOffset = 4000, extractedSelectionStart = 7, reclaimedBeforeLength = 4)
        )
    }
}

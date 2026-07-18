// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the §101 (D-139) truth-verification predicate: [SelectionTruth.isAtExpectedCaret] must
 * accept exactly a collapsed caret at the expected position and nothing else - it gates both the cheap
 * reported-positions check and the decisive ground-truth check in `onUpdateSelection`.
 */
class SelectionTruthTest {
    
    @Test
    fun `collapsed caret exactly at the expected position matches`() {
        assertTrue(SelectionTruth.isAtExpectedCaret(expected = 10, selStart = 10, selEnd = 10))
    }
    
    @Test
    fun `collapsed caret at the field start matches when expected there`() {
        assertTrue(SelectionTruth.isAtExpectedCaret(expected = 0, selStart = 0, selEnd = 0))
    }
    
    @Test
    fun `collapsed caret one behind the expected position does not match`() {
        // The log-1 shape: a lagging echo of the previous word's commit, reported while the next token
        // is already composing one position further on.
        assertFalse(SelectionTruth.isAtExpectedCaret(expected = 10, selStart = 9, selEnd = 9))
    }
    
    @Test
    fun `collapsed caret one before the expected position does not match`() {
        // The log-2 shape: a batch edit's transient intermediate state (the reclaim's delete step),
        // reported separately by an editor that does not coalesce batches.
        assertFalse(SelectionTruth.isAtExpectedCaret(expected = 127, selStart = 126, selEnd = 126))
    }
    
    @Test
    fun `a real selection does not match even when it starts at the expected position`() {
        assertFalse(SelectionTruth.isAtExpectedCaret(expected = 10, selStart = 10, selEnd = 14))
    }
    
    @Test
    fun `a real selection does not match even when it ends at the expected position`() {
        assertFalse(SelectionTruth.isAtExpectedCaret(expected = 14, selStart = 10, selEnd = 14))
    }
    
    @Test
    fun `large absolute positions in a windowed long field match exactly`() {
        // Positions of this size occur in real fields (a 47k-char mail body was observed live) - the
        // predicate must stay a plain exact comparison with no scale-dependent behaviour.
        assertTrue(SelectionTruth.isAtExpectedCaret(expected = 47676, selStart = 47676, selEnd = 47676))
        assertFalse(SelectionTruth.isAtExpectedCaret(expected = 47676, selStart = 47675, selEnd = 47675))
    }
}

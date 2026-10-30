// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-492 paste-shape decider ([ComposingSpanReMark.decide]). The captured device repro
 * is the reference case throughout: a pasted "Besuch" reclaimed into composing (anchor 14), the editor
 * killing the span (echo candidates=[-1,-1]) at an unchanged, still-matching caret, and the deferred
 * cosmetic rewrite that would insert over the dead span. The decider may only order an in-place re-mark
 * after synchronous caret+content verification - never a tear-down from the remote span report (spec
 * invariant 2), never a text write (D-182/D-383).
 */
class ComposingSpanReMarkTest {
    
    // "Wir haben einen Besuch geplant. Und noch mehr Text danach, damit ein windowed extraction mit
    // Startversatz fern vom Token realistisch konstruierbar bleibt."
    private val prefix = "Wir haben einen "
    private val token = "Besuch"
    private val suffix = " geplant. Und noch mehr Text danach, damit ein windowed extraction mit " +
        "Startversatz fern vom Token realistisch konstruierbar bleibt."
    
    private val doc: String = prefix + token + suffix
    private val anchor = prefix.length
    private val caret = anchor + token.length
    
    @Test
    fun `the captured repro shape orders a re-mark of the verified anchor range`() {
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = caret,
            extractedSelEnd = caret
        )
        assertEquals(ComposingSpanReMark.Decision.ReMark(anchor, caret), decision)
    }
    
    @Test
    fun `a live-span echo is nothing to do even when everything else would match`() {
        // The conservative default: until an echo says the span died, no ownership action at all.
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = true,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = caret,
            extractedSelEnd = caret
        )
        assertEquals(ComposingSpanReMark.Decision.NothingToDo, decision)
    }
    
    @Test
    fun `an empty composing token is nothing to do`() {
        val decision = ComposingSpanReMark.decide(
            composing = "",
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = caret,
            extractedSelEnd = caret
        )
        assertEquals(ComposingSpanReMark.Decision.NothingToDo, decision)
    }
    
    @Test
    fun `an unknown anchor is nothing to do - nothing reliable to verify against`() {
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = -1,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = caret,
            extractedSelEnd = caret
        )
        assertEquals(ComposingSpanReMark.Decision.NothingToDo, decision)
    }
    
    @Test
    fun `windowed extraction maps through startOffset to the same absolute range`() {
        // A long field where the framework extracts only a window around the selection - the D-87 rule:
        // positions inside the chunk are relative to startOffset, the anchor stays absolute.
        val windowStart = 5
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = windowStart,
            extractedText = doc.substring(windowStart),
            extractedSelStart = caret - windowStart,
            extractedSelEnd = caret - windowStart
        )
        assertEquals(ComposingSpanReMark.Decision.ReMark(anchor, caret), decision)
    }
    
    @Test
    fun `a caret elsewhere is unverifiable even when the content matches`() {
        // The caret moved after the span died (or the echo lags) - not this mechanism's call.
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = 5,
            extractedSelEnd = 5
        )
        assertEquals(ComposingSpanReMark.Decision.Unverifiable, decision)
    }
    
    @Test
    fun `a real selection is unverifiable even when it starts at the expected caret`() {
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = caret,
            extractedSelEnd = caret + 4
        )
        assertEquals(ComposingSpanReMark.Decision.Unverifiable, decision)
    }
    
    @Test
    fun `the text having changed at the anchor is unverifiable`() {
        // The editor refilled or edited the region - the model's believed range is stale.
        val changed = prefix + "Buch" + suffix
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = changed,
            extractedSelStart = caret,
            extractedSelEnd = caret
        )
        assertEquals(ComposingSpanReMark.Decision.Unverifiable, decision)
    }
    
    @Test
    fun `missing ground truth is unverifiable`() {
        // getExtractedText() returned null: startOffset 0 with an empty chunk.
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = "",
            extractedSelStart = 0,
            extractedSelEnd = 0
        )
        assertEquals(ComposingSpanReMark.Decision.Unverifiable, decision)
    }
    
    @Test
    fun `an anchor before the extracted window is unverifiable`() {
        // The window starts inside the believed token, so its start cannot be verified.
        val windowStart = anchor + 2
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = caret,
            lastEchoSpanPresent = false,
            extractedStartOffset = windowStart,
            extractedText = doc.substring(windowStart),
            extractedSelStart = caret - windowStart,
            extractedSelEnd = caret - windowStart
        )
        assertEquals(ComposingSpanReMark.Decision.Unverifiable, decision)
    }
    
    @Test
    fun `a token reaching past the extracted window is unverifiable`() {
        // Defensive bounds guard: a short extraction window that ends inside the believed token
        // ("Besuc|") cannot verify the token's end, even though the caret (mid-word edit point) sits
        // inside it - the decider must not re-mark a partially verified range.
        val window = doc.substring(anchor, caret - 1)
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = anchor + 4,
            lastEchoSpanPresent = false,
            extractedStartOffset = anchor,
            extractedText = window,
            extractedSelStart = 4,
            extractedSelEnd = 4
        )
        assertEquals(ComposingSpanReMark.Decision.Unverifiable, decision)
    }
    
    @Test
    fun `a mid-word edit point still re-marks when the caret sits at the logical edit point`() {
        // D-62: while an "after" fragment follows, the caret deliberately sits at anchor + cursor with
        // cursor < length - the decider takes the expected caret as given and must not require end-of-word.
        val decision = ComposingSpanReMark.decide(
            composing = token,
            composingAnchor = anchor,
            expectedCaret = anchor + 4,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = doc,
            extractedSelStart = anchor + 4,
            extractedSelEnd = anchor + 4
        )
        assertEquals(ComposingSpanReMark.Decision.ReMark(anchor, caret), decision)
    }
    
    @Test
    fun `the re-mark range always covers exactly the composing token`() {
        // The captured log's doubled word: the reclaim found "BesuchBesuch" (12 chars) after the fact -
        // a longer token's range end is anchor + length, nothing more.
        val longToken = "Besuchbesuch"
        val longDoc = "Test: " + longToken + " rest"
        val decision = ComposingSpanReMark.decide(
            composing = longToken,
            composingAnchor = 6,
            expectedCaret = 18,
            lastEchoSpanPresent = false,
            extractedStartOffset = 0,
            extractedText = longDoc,
            extractedSelStart = 18,
            extractedSelEnd = 18
        )
        assertTrue(decision is ComposingSpanReMark.Decision.ReMark)
        assertEquals(6, (decision as ComposingSpanReMark.Decision.ReMark).start)
        assertEquals(18, decision.end)
    }
}

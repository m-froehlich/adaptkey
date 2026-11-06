// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the glide release's commit decision (D-517, plan §4.3/§8.3): resolved casing via
 * the caller's capitalise lambda exactly when the preview is composing (D-440's gate), the
 * unknown-word fallback committing the inferred sequence without learning, and the no-autocorrect
 * contract (the capitalise lambda is the caller's whole derivation - this policy never substitutes).
 */
class GlideCommitPolicyTest {

    @Test
    fun `a known candidate re-derives casing while the preview is composing`() {
        val decision = GlideCommitPolicy.resolve(
            topCandidate = "morgen",
            inferredSequence = "",
            composingText = "morgen",
            capitalise = { word -> word.replaceFirstChar { it.uppercaseChar() } }
        )
        assertEquals("Morgen", decision.word)
        assertTrue(decision.learn)
    }

    @Test
    fun `a known candidate commits verbatim when no preview ever composed`() {
        val decision = GlideCommitPolicy.resolve(
            topCandidate = "Grüße",
            inferredSequence = "",
            composingText = "",
            capitalise = { word -> throw AssertionError("capitalise must not run with composing empty") }
        )
        assertEquals("Grüße", decision.word)
        assertTrue(decision.learn)
    }

    @Test
    fun `the unknown-word fallback commits the inferred sequence and never learns`() {
        val decision = GlideCommitPolicy.resolve(
            topCandidate = null,
            inferredSequence = "xqz",
            composingText = "",
            capitalise = { word -> throw AssertionError("capitalise must not run with composing empty") }
        )
        assertEquals("xqz", decision.word)
        assertFalse(decision.learn, "an unknown glide word is never auto-learned (plan §8.3)")
    }

    @Test
    fun `the unknown-word fallback still derives casing from a live preview`() {
        // The preview showed a candidate that was later superseded by nothing (empty results at
        // release): composing still holds it, so the ordinary casing derivation runs, exactly like
        // a chip commit's D-440 gate.
        val decision = GlideCommitPolicy.resolve(
            topCandidate = null,
            inferredSequence = "ahj",
            composingText = "ahj",
            capitalise = { word -> word.uppercase() }
        )
        assertEquals("AHJ", decision.word)
        assertFalse(decision.learn)
    }
}

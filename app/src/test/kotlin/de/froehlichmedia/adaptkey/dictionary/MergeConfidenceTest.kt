// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-391 fusion confidence score.
 */
class MergeConfidenceTest {
    
    @Test
    fun `a non-noun candidate at or above the reference frequency saturates to 1_0`() {
        assertEquals(1.0, MergeConfidence.forFusedCandidate(300L, isNounLike = false), 0.0001)
        assertEquals(1.0, MergeConfidence.forFusedCandidate(10_000L, isNounLike = false), 0.0001)
    }
    
    @Test
    fun `a rare non-noun candidate scores well below 1_0`() {
        val score = MergeConfidence.forFusedCandidate(2L, isNounLike = false)
        assertTrue(score in 0.0..0.3)
    }
    
    @Test
    fun `a noun-tagged candidate needs a dramatically higher frequency for the same score`() {
        val nonNoun = MergeConfidence.forFusedCandidate(300L, isNounLike = false)
        val noun = MergeConfidence.forFusedCandidate(300L, isNounLike = true)
        assertTrue(noun < nonNoun)
    }
    
    @Test
    fun `a noun-tagged candidate at its own reference frequency saturates to 1_0`() {
        assertEquals(1.0, MergeConfidence.forFusedCandidate(8_000L, isNounLike = true), 0.0001)
    }
    
    @Test
    fun `frequency zero never produces a negative or NaN score`() {
        assertEquals(0.0, MergeConfidence.forFusedCandidate(0L, isNounLike = false), 0.0001)
        assertEquals(0.0, MergeConfidence.forFusedCandidate(0L, isNounLike = true), 0.0001)
    }
}

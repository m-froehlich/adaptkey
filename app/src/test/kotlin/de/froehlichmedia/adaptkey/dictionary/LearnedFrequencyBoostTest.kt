// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * D-411: unit tests for the log-scaled, recency-aware learned-frequency ranking boost. Calibration
 * (REFERENCE_FREQUENCY 5000, REFERENCE_COUNT 50, a 14-day recency window, 1.5x recency factor) is
 * confirmed directly with the user, not guessed - see [LearnedFrequencyBoost]'s own KDoc.
 */
class LearnedFrequencyBoostTest {
    
    private val day = 24L * 60 * 60 * 1000
    
    @Test
    fun `a never-learned word gets no boost`() {
        assertEquals(0.0, LearnedFrequencyBoost.boost(0L, 0L, 0L))
    }
    
    @Test
    fun `a word used exactly the reference count reaches exactly the reference frequency when not recent`() {
        val now = 100L * day
        val longAgo = 0L
        assertEquals(5000.0, LearnedFrequencyBoost.boost(50L, longAgo, now), 0.001)
    }
    
    @Test
    fun `a word used far fewer times than the reference count scores well below the reference frequency`() {
        val now = 100L * day
        val longAgo = 0L
        val boost = LearnedFrequencyBoost.boost(5L, longAgo, now)
        assertTrue(boost in 1.0..2500.0, "expected a modest boost well under the reference, was $boost")
    }
    
    @Test
    fun `growth tapers well past the reference count instead of scaling linearly`() {
        val now = 100L * day
        val longAgo = 0L
        val atReference = LearnedFrequencyBoost.boost(50L, longAgo, now)
        val fourTimesReference = LearnedFrequencyBoost.boost(200L, longAgo, now)
        // A linear scale would give exactly 4x; the log curve must land well under that.
        assertTrue(fourTimesReference < atReference * 2, "expected sub-linear growth, was $fourTimesReference vs $atReference")
    }
    
    @Test
    fun `a word touched right now is boosted by the recency factor`() {
        val now = 100L * day
        assertEquals(5000.0 * 1.5, LearnedFrequencyBoost.boost(50L, now, now), 0.001)
    }
    
    @Test
    fun `a word touched exactly at the recency window edge still counts as recent`() {
        val now = 100L * day
        val touchedFourteenDaysAgo = now - 14 * day
        assertEquals(5000.0 * 1.5, LearnedFrequencyBoost.boost(50L, touchedFourteenDaysAgo, now), 0.001)
    }
    
    @Test
    fun `a word touched just past the recency window is no longer boosted by it`() {
        val now = 100L * day
        val touchedJustOver = now - 14 * day - 1
        assertEquals(5000.0, LearnedFrequencyBoost.boost(50L, touchedJustOver, now), 0.001)
    }
}

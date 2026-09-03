// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * D-365/D-429: unit tests for the log-scaled, recency-aware learned-bigram/trigram ranking boost.
 * Calibration (REFERENCE_FREQUENCY 250, REFERENCE_COUNT 50) is derived directly from the real bundled
 * `dictionaries/de/bigram.tsv` distribution (median 48, 90th percentile ~193, 95th percentile ~340); the
 * recency window/factor (14 days, 1.5x) are [LearnedFrequencyBoost]'s own D-411 values, reused rather than
 * recalibrated - see [LearnedBigramBoost]'s own KDoc.
 */
class LearnedBigramBoostTest {
    
    private val day = 24L * 60 * 60 * 1000
    
    @Test
    fun `a never-learned bigram gets no boost`() {
        assertEquals(0.0, LearnedBigramBoost.boost(0L, 0L, 0L))
    }
    
    @Test
    fun `a negative count gets no boost`() {
        assertEquals(0.0, LearnedBigramBoost.boost(-1L, 0L, 0L))
    }
    
    @Test
    fun `a bigram used exactly the reference count reaches exactly the reference frequency when not recent`() {
        val now = 100L * day
        val longAgo = 0L
        assertEquals(250.0, LearnedBigramBoost.boost(50L, longAgo, now), 0.001)
    }
    
    @Test
    fun `a bigram used far fewer times than the reference count scores well below the reference frequency`() {
        val now = 100L * day
        val longAgo = 0L
        val boost = LearnedBigramBoost.boost(5L, longAgo, now)
        assertTrue(boost in 1.0..125.0, "expected a modest boost well under the reference, was $boost")
    }
    
    @Test
    fun `growth tapers well past the reference count instead of scaling linearly`() {
        val now = 100L * day
        val longAgo = 0L
        val atReference = LearnedBigramBoost.boost(50L, longAgo, now)
        val fourTimesReference = LearnedBigramBoost.boost(200L, longAgo, now)
        // A linear scale would give exactly 4x; the log curve must land well under that.
        assertTrue(fourTimesReference < atReference * 2, "expected sub-linear growth, was $fourTimesReference vs $atReference")
    }
    
    @Test
    fun `a bigram touched right now is boosted by the recency factor`() {
        val now = 100L * day
        assertEquals(250.0 * 1.5, LearnedBigramBoost.boost(50L, now, now), 0.001)
    }
    
    @Test
    fun `a bigram touched exactly at the recency window edge still counts as recent`() {
        val now = 100L * day
        val touchedFourteenDaysAgo = now - 14 * day
        assertEquals(250.0 * 1.5, LearnedBigramBoost.boost(50L, touchedFourteenDaysAgo, now), 0.001)
    }
    
    @Test
    fun `a bigram touched just past the recency window is no longer boosted by it`() {
        val now = 100L * day
        val touchedJustOver = now - 14 * day - 1
        assertEquals(250.0, LearnedBigramBoost.boost(50L, touchedJustOver, now), 0.001)
    }
}

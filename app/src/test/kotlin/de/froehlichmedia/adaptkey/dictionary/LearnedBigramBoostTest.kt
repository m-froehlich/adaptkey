// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * D-365: unit tests for the log-scaled learned-bigram ranking boost. Calibration (REFERENCE_FREQUENCY 250,
 * REFERENCE_COUNT 50) is derived directly from the real bundled `dictionaries/de/bigram.tsv` distribution
 * (median 48, 90th percentile ~193, 95th percentile ~340) - see [LearnedBigramBoost]'s own KDoc.
 */
class LearnedBigramBoostTest {
    
    @Test
    fun `a never-learned bigram gets no boost`() {
        assertEquals(0.0, LearnedBigramBoost.boost(0L))
    }
    
    @Test
    fun `a negative count gets no boost`() {
        assertEquals(0.0, LearnedBigramBoost.boost(-1L))
    }
    
    @Test
    fun `a bigram used exactly the reference count reaches exactly the reference frequency`() {
        assertEquals(250.0, LearnedBigramBoost.boost(50L), 0.001)
    }
    
    @Test
    fun `a bigram used far fewer times than the reference count scores well below the reference frequency`() {
        val boost = LearnedBigramBoost.boost(5L)
        assertTrue(boost in 1.0..125.0, "expected a modest boost well under the reference, was $boost")
    }
    
    @Test
    fun `growth tapers well past the reference count instead of scaling linearly`() {
        val atReference = LearnedBigramBoost.boost(50L)
        val fourTimesReference = LearnedBigramBoost.boost(200L)
        // A linear scale would give exactly 4x; the log curve must land well under that.
        assertTrue(fourTimesReference < atReference * 2, "expected sub-linear growth, was $fourTimesReference vs $atReference")
    }
}

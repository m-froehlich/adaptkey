// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-391 auto-merge aggressiveness enum and its stored-value resolution.
 */
class AutoMergeAggressivenessTest {
    
    @Test
    fun `the default level is medium`() {
        assertEquals(AutoMergeAggressiveness.MEDIUM, AutoMergeAggressiveness.DEFAULT)
    }
    
    @Test
    fun `known keys resolve regardless of case and surrounding space`() {
        assertEquals(AutoMergeAggressiveness.CAUTIOUS, AutoMergeAggressiveness.fromKey("cautious"))
        assertEquals(AutoMergeAggressiveness.MEDIUM, AutoMergeAggressiveness.fromKey("MEDIUM"))
        assertEquals(AutoMergeAggressiveness.AGGRESSIVE, AutoMergeAggressiveness.fromKey("  aggressive "))
    }
    
    @Test
    fun `null blank unknown and the off sentinel all fall back to the default`() {
        assertEquals(AutoMergeAggressiveness.DEFAULT, AutoMergeAggressiveness.fromKey(null))
        assertEquals(AutoMergeAggressiveness.DEFAULT, AutoMergeAggressiveness.fromKey("   "))
        assertEquals(AutoMergeAggressiveness.DEFAULT, AutoMergeAggressiveness.fromKey("bogus"))
        // "off" disables the mechanism entirely (AdaptSettings.autoMergeEnabled) - it is never a level of
        // its own, mirroring AutocorrectAggressiveness.OFF_KEY's identical shape.
        assertEquals(AutoMergeAggressiveness.DEFAULT, AutoMergeAggressiveness.fromKey(AutoMergeAggressiveness.OFF_KEY))
    }
    
    @Test
    fun `thresholds increase in strictness from aggressive to cautious`() {
        assertEquals(0.90, AutoMergeAggressiveness.CAUTIOUS.autoApplyThreshold)
        assertEquals(0.75, AutoMergeAggressiveness.MEDIUM.autoApplyThreshold)
        assertEquals(0.55, AutoMergeAggressiveness.AGGRESSIVE.autoApplyThreshold)
    }
}

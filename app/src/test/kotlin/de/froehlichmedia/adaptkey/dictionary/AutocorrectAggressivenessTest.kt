// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-353 autocorrect aggressiveness enum and its stored-value resolution.
 */
class AutocorrectAggressivenessTest {
    
    @Test
    fun `the default is medium`() {
        assertEquals(AutocorrectAggressiveness.MEDIUM, AutocorrectAggressiveness.DEFAULT)
    }
    
    @Test
    fun `known keys resolve regardless of case and surrounding space`() {
        assertEquals(AutocorrectAggressiveness.CAUTIOUS, AutocorrectAggressiveness.fromKey("cautious"))
        assertEquals(AutocorrectAggressiveness.MEDIUM, AutocorrectAggressiveness.fromKey("MEDIUM"))
        assertEquals(AutocorrectAggressiveness.AGGRESSIVE, AutocorrectAggressiveness.fromKey("  aggressive "))
    }
    
    @Test
    fun `null blank and unknown keys fall back to the default`() {
        assertEquals(AutocorrectAggressiveness.DEFAULT, AutocorrectAggressiveness.fromKey(null))
        assertEquals(AutocorrectAggressiveness.DEFAULT, AutocorrectAggressiveness.fromKey("   "))
        assertEquals(AutocorrectAggressiveness.DEFAULT, AutocorrectAggressiveness.fromKey("bogus"))
    }
    
    @Test
    fun `D-407 OFF_KEY is not a member of this enum - falls back to the default like any other key it does not recognise`() {
        assertEquals(AutocorrectAggressiveness.DEFAULT, AutocorrectAggressiveness.fromKey(AutocorrectAggressiveness.OFF_KEY))
    }
    
    @Test
    fun `levels are ordered from most to least cautious`() {
        assertTrue(AutocorrectAggressiveness.CAUTIOUS.autoApplyThreshold > AutocorrectAggressiveness.MEDIUM.autoApplyThreshold)
        assertTrue(AutocorrectAggressiveness.MEDIUM.autoApplyThreshold > AutocorrectAggressiveness.AGGRESSIVE.autoApplyThreshold)
        assertTrue(AutocorrectAggressiveness.CAUTIOUS.chipOfferThreshold > AutocorrectAggressiveness.MEDIUM.chipOfferThreshold)
        assertTrue(AutocorrectAggressiveness.MEDIUM.chipOfferThreshold > AutocorrectAggressiveness.AGGRESSIVE.chipOfferThreshold)
    }
    
    @Test
    fun `every level's chip threshold is below its own auto threshold`() {
        for (level in AutocorrectAggressiveness.entries) {
            assertTrue(level.chipOfferThreshold < level.autoApplyThreshold, "$level")
        }
    }
    
    @Test
    fun `D-353 no level ever reopens the Ohren to Ihren regression - the explicit user-required floor`() {
        // The confirmed-bad case (D-244): "Ohren" (170) silently overridden to "Ihren" (11,907), ratio 70x.
        // AGGRESSIVE is the most permissive level - if its own auto threshold cannot exclude this score,
        // no level's could. A future retuning of CorrectionConfidence's own constants that reopens this
        // must fail this test before it ever reaches a device.
        val score = CorrectionConfidence.forKnownWordOverride(1, 170L, 11_907L, false)
        assertTrue(
            score < AutocorrectAggressiveness.AGGRESSIVE.autoApplyThreshold,
            "Ohren/Ihren's score ($score) must stay below AGGRESSIVE's own auto threshold " +
                "(${AutocorrectAggressiveness.AGGRESSIVE.autoApplyThreshold})"
        )
    }
}

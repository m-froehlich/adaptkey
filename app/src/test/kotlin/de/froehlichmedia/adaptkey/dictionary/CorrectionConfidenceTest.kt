// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-353 confidence formula, calibrated against the app's own real regression corpus -
 * see [AutocorrectAggressiveness]'s own KDoc for the full worked numbers these mirror.
 */
class CorrectionConfidenceTest {
    
    @Test
    fun `forUnknownToken clamps to 1_0 for a frequency at or beyond the reference`() {
        assertEquals(1.0, CorrectionConfidence.forUnknownToken(1, 1_000_000L, false, false, false))
    }
    
    @Test
    fun `forUnknownToken a cost-2 candidate scores lower than an otherwise identical cost-1 one`() {
        val cost1 = CorrectionConfidence.forUnknownToken(1, 1_000L, false, false, false)
        val cost2 = CorrectionConfidence.forUnknownToken(2, 1_000L, false, false, false)
        assertTrue(cost2 < cost1)
    }
    
    @Test
    fun `forUnknownToken D-116 a common noun is not held to the non-noun bar - Jahren`() {
        // "jahreb" -> "Jahren" (cost 1, frequency 2,000, tagged NOUN): must clear MEDIUM's own auto
        // threshold (0.75) - a flat noun penalty would have wrongly punished this exactly like a rare
        // proper-noun corpus artefact would be.
        val score = CorrectionConfidence.forUnknownToken(1, 2_000L, true, false, false)
        assertTrue(score >= AutocorrectAggressiveness.MEDIUM.autoApplyThreshold, "expected >= 0.75, was $score")
    }
    
    @Test
    fun `forUnknownToken D-114 a rare noun stays below every aggressiveness level's auto threshold - Virgin`() {
        // "Virhin" -> "Virgin" (cost 1, frequency 62, tagged NOUN, D-114's own reported bug).
        val score = CorrectionConfidence.forUnknownToken(1, 62L, true, false, false)
        for (level in AutocorrectAggressiveness.entries) {
            assertTrue(score < level.autoApplyThreshold, "expected < ${level.autoApplyThreshold} at $level, was $score")
        }
    }
    
    @Test
    fun `forUnknownToken D-227 a rare non-noun clears MEDIUM's auto threshold - übrigens`() {
        // "übrigebs" -> "übrigens" (cost 1, frequency 79, tagged OTHER, D-227's own reported bug).
        val score = CorrectionConfidence.forUnknownToken(1, 79L, false, false, false)
        assertTrue(score >= AutocorrectAggressiveness.MEDIUM.autoApplyThreshold, "expected >= 0.75, was $score")
    }
    
    @Test
    fun `forUnknownToken D-28 a cost-2 correction still clears MEDIUM's auto threshold - komplezz to komplett`() {
        val score = CorrectionConfidence.forUnknownToken(2, 40L, false, false, false)
        assertTrue(score >= AutocorrectAggressiveness.MEDIUM.autoApplyThreshold, "expected >= 0.75, was $score")
    }
    
    @Test
    fun `forUnknownToken D-371 a digit-ending token clears AGGRESSIVE's auto threshold but not MEDIUM or CAUTIOUS`() {
        // An otherwise maximally-confident candidate (frequency at/beyond the reference, cost 1) for a
        // typed token ending in a digit (e.g. "Str12") - the digit-suffix cap must let AGGRESSIVE still
        // auto-apply while MEDIUM/CAUTIOUS are held back to a chip offer only.
        val score = CorrectionConfidence.forUnknownToken(1, 1_000_000L, false, false, true)
        assertTrue(
            score >= AutocorrectAggressiveness.AGGRESSIVE.autoApplyThreshold,
            "expected >= AGGRESSIVE (${AutocorrectAggressiveness.AGGRESSIVE.autoApplyThreshold}), was $score"
        )
        assertTrue(
            score < AutocorrectAggressiveness.MEDIUM.autoApplyThreshold,
            "expected < MEDIUM (${AutocorrectAggressiveness.MEDIUM.autoApplyThreshold}), was $score"
        )
        assertTrue(
            score < AutocorrectAggressiveness.CAUTIOUS.autoApplyThreshold,
            "expected < CAUTIOUS (${AutocorrectAggressiveness.CAUTIOUS.autoApplyThreshold}), was $score"
        )
        for (level in AutocorrectAggressiveness.entries) {
            assertTrue(score >= level.chipOfferThreshold, "expected >= ${level.chipOfferThreshold} at $level, was $score")
        }
    }
    
    @Test
    fun `forUnknownToken D-371 a low-confidence digit-ending token still fails to auto-apply at any level`() {
        // The digit-suffix cap only ever lowers a score, never raises one - a candidate that would not have
        // cleared MEDIUM anyway (D-114's own Virgin case) must not be pushed up to AGGRESSIVE's threshold
        // just because the typed token also happens to end in a digit.
        val score = CorrectionConfidence.forUnknownToken(1, 62L, true, false, true)
        for (level in AutocorrectAggressiveness.entries) {
            assertTrue(score < level.autoApplyThreshold, "expected < ${level.autoApplyThreshold} at $level, was $score")
        }
    }
    
    @Test
    fun `forKnownWordOverride never fires beyond a single adjacent edit - D-113`() {
        assertEquals(0.0, CorrectionConfidence.forKnownWordOverride(2, 49L, 7228L, false))
    }
    
    @Test
    fun `forKnownWordOverride is 0 when the candidate is not actually more frequent`() {
        assertEquals(0.0, CorrectionConfidence.forKnownWordOverride(1, 1000L, 500L, false))
        assertEquals(0.0, CorrectionConfidence.forKnownWordOverride(1, 1000L, 1000L, false))
    }
    
    @Test
    fun `forKnownWordOverride D-244 the Ohren to Ihren regression stays below every aggressiveness level`() {
        // Real bundled frequencies: "Ohren" 170, "Ihren" 11,907 - ratio ~70x.
        val score = CorrectionConfidence.forKnownWordOverride(1, 170L, 11_907L, false)
        for (level in AutocorrectAggressiveness.entries) {
            assertTrue(score < level.autoApplyThreshold, "expected < ${level.autoApplyThreshold} at $level, was $score")
        }
    }
    
    @Test
    fun `forKnownWordOverride D-244 ddr to der clears MEDIUM and AGGRESSIVE's auto thresholds`() {
        // Real bundled frequencies: "ddr" 4,405, "der" 1,004,234 - ratio ~228x. CAUTIOUS is deliberately
        // stricter than today's pre-D-353 behaviour (only MEDIUM has to reproduce it exactly) and may
        // reject this case in exchange for admitting fewer, more certain corrections overall - see
        // AutocorrectAggressiveness's own KDoc.
        val score = CorrectionConfidence.forKnownWordOverride(1, 4_405L, 1_004_234L, false)
        assertTrue(score >= AutocorrectAggressiveness.MEDIUM.autoApplyThreshold, "expected >= MEDIUM, was $score")
        assertTrue(score >= AutocorrectAggressiveness.AGGRESSIVE.autoApplyThreshold, "expected >= AGGRESSIVE, was $score")
    }
    
    @Test
    fun `forKnownWordOverride due to die clears every aggressiveness level at an extreme ratio`() {
        assertEquals(1.0, CorrectionConfidence.forKnownWordOverride(1, 24L, 889_897L, false))
    }
    
    @Test
    fun `prefixShiftsAway D-354 aberkennen to anerkennen changes the recognised prefix`() {
        assertTrue(CorrectionConfidence.prefixShiftsAway("aberkennen", "anerkennen"))
    }
    
    @Test
    fun `prefixShiftsAway is false when the correction keeps the same prefix`() {
        assertFalse(CorrectionConfidence.prefixShiftsAway("aberkennen", "aberkennung"))
    }
    
    @Test
    fun `prefixShiftsAway is false when the typed token matches no plausible prefix at all - übrigebs`() {
        assertFalse(CorrectionConfidence.prefixShiftsAway("übrigebs", "übrigens"))
    }
    
    @Test
    fun `prefixShiftsAway D-354 aberkennen never clears any auto threshold but always clears every chip threshold`() {
        // "aberkennen" (unknown, frequency-wise plausible: cost 1, no dictionary entry, corrected to the
        // far more frequent "anerkennen", frequency 165) - the exact motivating case for D-354. Must be
        // capped below every AutocorrectAggressiveness auto threshold, yet still clear every chip threshold.
        val score = CorrectionConfidence.forUnknownToken(1, 165L, false, true, false)
        for (level in AutocorrectAggressiveness.entries) {
            assertTrue(score < level.autoApplyThreshold, "expected < ${level.autoApplyThreshold} at $level, was $score")
            assertTrue(score >= level.chipOfferThreshold, "expected >= ${level.chipOfferThreshold} at $level, was $score")
        }
    }
}

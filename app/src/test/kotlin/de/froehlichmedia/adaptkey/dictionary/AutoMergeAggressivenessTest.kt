// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    
    private fun candidate(
        fragments: FusionClass,
        frequency: Long = 5_000L,
        oneLetter: Boolean = false,
        left: Long = 0L,
        right: Long = 0L,
        attested: Boolean = false
    ) = FusionCandidate("wort", frequency, fragments, oneLetter, left, right, attested)
    
    @Test
    fun `D-477 each level admits a wider set of fragment classes than the one before`() {
        assertEquals(setOf(FusionClass.BOTH_UNKNOWN), AutoMergeAggressiveness.CAUTIOUS.allowedClasses)
        assertEquals(setOf(FusionClass.BOTH_UNKNOWN, FusionClass.ONE_UNKNOWN), AutoMergeAggressiveness.MEDIUM.allowedClasses)
        assertEquals(FusionClass.entries.toSet(), AutoMergeAggressiveness.AGGRESSIVE.allowedClasses)
    }
    
    @Test
    fun `D-477 frequency floors are lower at each more aggressive level, one-letter floors higher than the plain ones`() {
        val levels = listOf(AutoMergeAggressiveness.CAUTIOUS, AutoMergeAggressiveness.MEDIUM, AutoMergeAggressiveness.AGGRESSIVE)
        assertTrue(levels.zipWithNext().all { (stricter, looser) -> stricter.minFrequency >= looser.minFrequency })
        assertTrue(levels.zipWithNext().all { (stricter, looser) -> stricter.minFrequencyOneLetter >= looser.minFrequencyOneLetter })
        assertTrue(levels.all { it.minFrequencyOneLetter > it.minFrequency })
    }
    
    @Test
    fun `D-477 two unknown fragments are accepted at every level`() {
        AutoMergeAggressiveness.entries.forEach {
            assertNull(it.rejection(candidate(FusionClass.BOTH_UNKNOWN, frequency = 1_000_000L)), it.name)
        }
    }
    
    @Test
    fun `D-477 exactly one unknown fragment is refused only by cautious`() {
        val c = candidate(FusionClass.ONE_UNKNOWN)
        assertNotNull(AutoMergeAggressiveness.CAUTIOUS.rejection(c))
        assertTrue(AutoMergeAggressiveness.MEDIUM.accepts(c))
        assertTrue(AutoMergeAggressiveness.AGGRESSIVE.accepts(c))
    }
    
    @Test
    fun `D-477 a fused word below the level's frequency floor is refused`() {
        val medium = AutoMergeAggressiveness.MEDIUM
        assertNotNull(medium.rejection(candidate(FusionClass.BOTH_UNKNOWN, frequency = medium.minFrequency - 1)))
        assertNull(medium.rejection(candidate(FusionClass.BOTH_UNKNOWN, frequency = medium.minFrequency)))
    }
    
    @Test
    fun `D-477 a one-letter fragment needs the higher floor`() {
        val medium = AutoMergeAggressiveness.MEDIUM
        val between = medium.minFrequencyOneLetter - 1
        assertNull(medium.rejection(candidate(FusionClass.ONE_UNKNOWN, frequency = between, oneLetter = false)))
        assertNotNull(medium.rejection(candidate(FusionClass.ONE_UNKNOWN, frequency = between, oneLetter = true)))
        assertNull(medium.rejection(candidate(FusionClass.ONE_UNKNOWN, frequency = medium.minFrequencyOneLetter, oneLetter = true)))
    }
    
    @Test
    fun `D-477 two known words are only fused at aggressive, and only when clearly more frequent than the rarer part`() {
        val bothKnown = candidate(FusionClass.BOTH_KNOWN, frequency = 500L, left = 400L, right = 90_000L)
        assertNotNull(AutoMergeAggressiveness.CAUTIOUS.rejection(bothKnown))
        assertNotNull(AutoMergeAggressiveness.MEDIUM.rejection(bothKnown))
        assertNull(AutoMergeAggressiveness.AGGRESSIVE.rejection(bothKnown))
        // "der er" -> "derber": both parts far more frequent than the fused word.
        assertNotNull(AutoMergeAggressiveness.AGGRESSIVE.rejection(candidate(FusionClass.BOTH_KNOWN, frequency = 50L, left = 800_000L, right = 60_000L)))
    }
    
    @Test
    fun `D-477 two known words that already occur as a bigram are never fused`() {
        val attested = candidate(FusionClass.BOTH_KNOWN, frequency = 500L, left = 400L, right = 90_000L, attested = true)
        assertNotNull(AutoMergeAggressiveness.AGGRESSIVE.rejection(attested))
    }
    
    @Test
    fun `D-477 two known words with no comparable part frequency are refused rather than assumed fine`() {
        val noFrequency = candidate(FusionClass.BOTH_KNOWN, frequency = 500L, left = 0L, right = 90_000L)
        assertNotNull(AutoMergeAggressiveness.AGGRESSIVE.rejection(noFrequency))
    }
}

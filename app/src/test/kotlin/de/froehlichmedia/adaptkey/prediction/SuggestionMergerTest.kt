// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.suggestion.Suggestion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-1 / tier-3 suggestion merge (§9).
 */
class SuggestionMergerTest {
    
    @Test
    fun `a non-positive limit yields nothing`() {
        val merged = SuggestionMerger.merge(listOf(Suggestion("a", 1.0)), emptyList(), limit = 0)
        assertTrue(merged.isEmpty())
    }
    
    @Test
    fun `an empty tier-3 list preserves the tier-1 order`() {
        val tier1 = listOf(Suggestion("der", 100.0), Suggestion("die", 50.0))
        val merged = SuggestionMerger.merge(tier1, emptyList(), limit = 5)
        assertEquals(listOf("der", "die"), merged.map { it.word })
    }
    
    @Test
    fun `agreement between both tiers floats a word to the top`() {
        val tier1 = listOf(Suggestion("der", 100.0), Suggestion("die", 50.0))
        val tier3 = listOf(Tier3Suggestion("die", 0.9), Tier3Suggestion("das", 0.8))
        val merged = SuggestionMerger.merge(tier1, tier3, limit = 5)
        // die = 0.5 (normalised) + 0.9 = 1.4 -> above der = 1.0 -> above das = 0.8.
        assertEquals(listOf("die", "der", "das"), merged.map { it.word })
    }
    
    @Test
    fun `a tier-3-only word is appended`() {
        val tier1 = listOf(Suggestion("der", 100.0))
        val tier3 = listOf(Tier3Suggestion("das", 0.4))
        val merged = SuggestionMerger.merge(tier1, tier3, limit = 5)
        assertEquals(listOf("der", "das"), merged.map { it.word })
    }
    
    @Test
    fun `the result is capped to the limit`() {
        val tier1 = listOf(Suggestion("a", 3.0), Suggestion("b", 2.0), Suggestion("c", 1.0))
        val merged = SuggestionMerger.merge(tier1, emptyList(), limit = 2)
        assertEquals(listOf("a", "b"), merged.map { it.word })
    }
    
    @Test
    fun `all-zero tier-1 scores do not break normalisation`() {
        val tier1 = listOf(Suggestion("x", 0.0))
        val tier3 = listOf(Tier3Suggestion("y", 0.5))
        val merged = SuggestionMerger.merge(tier1, tier3, limit = 5)
        assertEquals(listOf("y", "x"), merged.map { it.word })
    }
    
    @Test
    fun `D-503 pinned continuations keep their flag and their leading slots through the merge`() {
        // "guests" is pinned (score 40 of max 80 -> normalised 0.5); "lovely" scores higher raw but is
        // not pinned; the tier-3 boost on the pinned word must not dissolve the pinning, and the
        // tier-3-only "beautiful" must not overtake it either.
        val tier1 = listOf(
            Suggestion("guests", 40.0, pinned = true),
            Suggestion("lovely", 80.0)
        )
        val tier3 = listOf(
            Tier3Suggestion("guests", 0.9),
            Tier3Suggestion("beautiful", 0.8)
        )
        val merged = SuggestionMerger.merge(tier1, tier3, limit = 5)
        
        assertEquals(listOf("guests", "lovely", "beautiful"), merged.map { it.word })
        val guests = merged.first { it.word == "guests" }
        assertTrue(guests.pinned)
        // The tier-3 agreement still accumulates onto the pinned word's score (0.5 + 0.9).
        assertEquals(0.5 + 0.9, guests.score, 1e-9)
    }
}

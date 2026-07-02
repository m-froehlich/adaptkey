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
}

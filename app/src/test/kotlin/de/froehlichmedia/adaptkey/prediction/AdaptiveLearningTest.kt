package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for the §9 adaptive-learning feedback policy.
 */
class AdaptiveLearningTest {
    
    private val result = Tier3Result(listOf(Tier3Suggestion("Vorhersage", 0.8), Tier3Suggestion("weil", 0.3)))
    
    @Test
    fun `a blank committed word yields no signal`() {
        assertNull(AdaptiveLearning.learningSignal("  ", result, tier1KnewWord = false))
    }
    
    @Test
    fun `a word the n-gram already knew yields no signal`() {
        assertNull(AdaptiveLearning.learningSignal("Vorhersage", result, tier1KnewWord = true))
    }
    
    @Test
    fun `a word tier 3 did not propose yields no signal`() {
        assertNull(AdaptiveLearning.learningSignal("etwas", result, tier1KnewWord = false))
    }
    
    @Test
    fun `a low-confidence tier-3 word yields no signal`() {
        assertNull(AdaptiveLearning.learningSignal("weil", result, tier1KnewWord = false))
    }
    
    @Test
    fun `a confident LLM word the n-gram did not know is a learning signal`() {
        assertEquals("Vorhersage", AdaptiveLearning.learningSignal("Vorhersage", result, tier1KnewWord = false))
    }
    
    @Test
    fun `the tier-3 match is case-insensitive`() {
        assertEquals("vorhersage", AdaptiveLearning.learningSignal("vorhersage", result, tier1KnewWord = false))
    }
}

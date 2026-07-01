package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.suggestion.Suggestion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-1 confidence measure (§9 / C-06): top-candidate dominance.
 */
class Tier1ConfidenceTest {
    
    @Test
    fun `an empty candidate list is fully unconfident`() {
        assertEquals(0.0, Tier1Confidence.confidence(emptyList()))
    }
    
    @Test
    fun `candidates with no positive score are fully unconfident`() {
        val candidates = listOf(Suggestion("a", 0.0), Suggestion("b", 0.0))
        assertEquals(0.0, Tier1Confidence.confidence(candidates))
    }
    
    @Test
    fun `a single candidate is fully confident`() {
        assertEquals(1.0, Tier1Confidence.confidence(listOf(Suggestion("der", 42.0))))
    }
    
    @Test
    fun `two equal candidates split the confidence`() {
        val candidates = listOf(Suggestion("der", 10.0), Suggestion("die", 10.0))
        assertEquals(0.5, Tier1Confidence.confidence(candidates))
    }
    
    @Test
    fun `a dominant candidate yields high confidence`() {
        val candidates = listOf(Suggestion("der", 30.0), Suggestion("die", 10.0))
        assertEquals(0.75, Tier1Confidence.confidence(candidates))
    }
    
    @Test
    fun `many similar candidates share the mass`() {
        val candidates = listOf(Suggestion("a", 1.0), Suggestion("b", 1.0), Suggestion("c", 1.0), Suggestion("d", 1.0))
        assertEquals(0.25, Tier1Confidence.confidence(candidates))
    }
}

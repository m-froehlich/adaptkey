package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.suggestion.Suggestion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-3 orchestration (§9): confidence gating, activation and the merge.
 */
class Tier3OrchestratorTest {
    
    /** A controllable available backend returning a fixed result. */
    private class FakeTier3Provider(private val result: Tier3Result) : Tier3Provider {
        
        var lastRequest: Tier3Request? = null
        
        override val isAvailable: Boolean = true
        
        override fun predict(request: Tier3Request): Tier3Result {
            lastRequest = request
            return result
        }
    }
    
    @Test
    fun `the no-op backend leaves the tier-1 suggestions untouched and inert`() {
        val orchestrator = Tier3Orchestrator()
        val tier1 = listOf(Suggestion("der", 30.0), Suggestion("die", 10.0))
        val outcome = orchestrator.predict("d", null, "d", tier1, LlmActivationThreshold.HIGH, limit = 8)
        
        assertFalse(outcome.activated)
        assertSame(tier1, outcome.suggestions)
        assertNull(outcome.capitalisation)
        assertEquals(Tier3Result.EMPTY, outcome.tier3)
        assertEquals(0.75, outcome.tier1Confidence)
    }
    
    @Test
    fun `a confident tier-1 keeps an available backend dormant`() {
        val provider = FakeTier3Provider(Tier3Result(listOf(Tier3Suggestion("das", 0.9))))
        val orchestrator = Tier3Orchestrator(provider)
        // Confidence 1.0 (single candidate) stays above every threshold.
        val tier1 = listOf(Suggestion("der", 30.0))
        val outcome = orchestrator.predict("d", null, "d", tier1, LlmActivationThreshold.MEDIUM, limit = 8)
        
        assertFalse(outcome.activated)
        assertSame(tier1, outcome.suggestions)
        assertNull(provider.lastRequest)
    }
    
    @Test
    fun `an uncertain tier-1 activates the backend and merges its result`() {
        val proposal = CapitalisationProposal("Der", 0.9)
        val provider = FakeTier3Provider(Tier3Result(listOf(Tier3Suggestion("die", 0.9), Tier3Suggestion("das", 0.8)), proposal))
        val orchestrator = Tier3Orchestrator(provider)
        // Two equal candidates -> confidence 0.5, below the HIGH threshold (0.75).
        val tier1 = listOf(Suggestion("der", 10.0), Suggestion("die", 10.0))
        val outcome = orchestrator.predict("d", "und", "und d", tier1, LlmActivationThreshold.HIGH, limit = 8)
        
        assertTrue(outcome.activated)
        assertEquals(proposal, outcome.capitalisation)
        assertTrue(outcome.suggestions.any { it.word == "das" })
        // die is offered by both tiers, so it leads the merge.
        assertEquals("die", outcome.suggestions.first().word)
        assertEquals(Tier3Request("d", "und", "und d"), provider.lastRequest)
    }
}

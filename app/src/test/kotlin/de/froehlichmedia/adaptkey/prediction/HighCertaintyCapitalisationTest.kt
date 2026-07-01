package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the §6 rule-6 high-certainty capitalisation gate.
 */
class HighCertaintyCapitalisationTest {
    
    @Test
    fun `no proposal never forces upper`() {
        assertFalse(HighCertaintyCapitalisation.forcesUpper(null, "morgen"))
    }
    
    @Test
    fun `an empty word never forces upper`() {
        assertFalse(HighCertaintyCapitalisation.forcesUpper(CapitalisationProposal("Morgen", 0.99), ""))
    }
    
    @Test
    fun `a low-confidence proposal does not force upper`() {
        val proposal = CapitalisationProposal("Morgen", 0.5)
        assertFalse(HighCertaintyCapitalisation.forcesUpper(proposal, "morgen"))
    }
    
    @Test
    fun `a confident capitalising proposal for the word forces upper`() {
        val proposal = CapitalisationProposal("Morgen", 0.95)
        assertTrue(HighCertaintyCapitalisation.forcesUpper(proposal, "morgen"))
    }
    
    @Test
    fun `a proposal that does not capitalise is ignored`() {
        val proposal = CapitalisationProposal("morgen", 0.95)
        assertFalse(HighCertaintyCapitalisation.forcesUpper(proposal, "morgen"))
    }
    
    @Test
    fun `a proposal for a different word is ignored`() {
        val proposal = CapitalisationProposal("Abend", 0.95)
        assertFalse(HighCertaintyCapitalisation.forcesUpper(proposal, "morgen"))
    }
    
    @Test
    fun `the minimum confidence is configurable`() {
        val proposal = CapitalisationProposal("Morgen", 0.7)
        assertTrue(HighCertaintyCapitalisation.forcesUpper(proposal, "morgen", minConfidence = 0.6))
    }
}

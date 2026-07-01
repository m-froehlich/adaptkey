package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure C-06 activation policy.
 */
class Tier3ActivationTest {
    
    @Test
    fun `an unavailable backend never activates`() {
        assertFalse(Tier3Activation.shouldActivate(0.0, LlmActivationThreshold.HIGH, providerAvailable = false))
    }
    
    @Test
    fun `activates when confidence is below the threshold`() {
        assertTrue(Tier3Activation.shouldActivate(0.4, LlmActivationThreshold.MEDIUM, providerAvailable = true))
    }
    
    @Test
    fun `does not activate at the threshold (strict)`() {
        assertFalse(Tier3Activation.shouldActivate(0.5, LlmActivationThreshold.MEDIUM, providerAvailable = true))
    }
    
    @Test
    fun `does not activate above the threshold`() {
        assertFalse(Tier3Activation.shouldActivate(0.9, LlmActivationThreshold.MEDIUM, providerAvailable = true))
    }
    
    @Test
    fun `a higher threshold makes the LLM more eager`() {
        // The same fairly-confident tier 1 activates under HIGH but not under LOW.
        assertTrue(Tier3Activation.shouldActivate(0.6, LlmActivationThreshold.HIGH, providerAvailable = true))
        assertFalse(Tier3Activation.shouldActivate(0.6, LlmActivationThreshold.LOW, providerAvailable = true))
    }
}

package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the C-06 activation threshold enum and its stored-value resolution.
 */
class LlmActivationThresholdTest {
    
    @Test
    fun `the default is medium`() {
        assertEquals(LlmActivationThreshold.MEDIUM, LlmActivationThreshold.DEFAULT)
    }
    
    @Test
    fun `thresholds increase from low to high`() {
        assertTrue(LlmActivationThreshold.LOW.confidenceThreshold < LlmActivationThreshold.MEDIUM.confidenceThreshold)
        assertTrue(LlmActivationThreshold.MEDIUM.confidenceThreshold < LlmActivationThreshold.HIGH.confidenceThreshold)
    }
    
    @Test
    fun `known keys resolve regardless of case and surrounding space`() {
        assertEquals(LlmActivationThreshold.LOW, LlmActivationThreshold.fromKey("low"))
        assertEquals(LlmActivationThreshold.MEDIUM, LlmActivationThreshold.fromKey("MEDIUM"))
        assertEquals(LlmActivationThreshold.HIGH, LlmActivationThreshold.fromKey("  High "))
    }
    
    @Test
    fun `null blank and unknown keys fall back to the default`() {
        assertEquals(LlmActivationThreshold.DEFAULT, LlmActivationThreshold.fromKey(null))
        assertEquals(LlmActivationThreshold.DEFAULT, LlmActivationThreshold.fromKey("   "))
        assertEquals(LlmActivationThreshold.DEFAULT, LlmActivationThreshold.fromKey("bogus"))
    }
}

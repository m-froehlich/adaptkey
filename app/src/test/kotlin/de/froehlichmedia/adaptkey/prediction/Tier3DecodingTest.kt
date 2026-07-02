package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the validated tier-3 decoding parameters.
 */
class Tier3DecodingTest {
    
    @Test
    fun `defaults are tiny and valid`() {
        val decoding = Tier3Decoding.DEFAULT
        assertEquals(8, decoding.maxNewTokens)
        assertEquals(3, decoding.numCandidates)
    }
    
    @Test
    fun `maxNewTokens must be within range`() {
        assertThrows(IllegalArgumentException::class.java) { Tier3Decoding(maxNewTokens = 0) }
        assertThrows(IllegalArgumentException::class.java) { Tier3Decoding(maxNewTokens = Tier3Decoding.MAX_NEW_TOKENS + 1) }
    }
    
    @Test
    fun `numCandidates must be within range`() {
        assertThrows(IllegalArgumentException::class.java) { Tier3Decoding(numCandidates = 0) }
        assertThrows(IllegalArgumentException::class.java) { Tier3Decoding(numCandidates = Tier3Decoding.MAX_CANDIDATES + 1) }
    }
    
    @Test
    fun `in-range values are accepted`() {
        val decoding = Tier3Decoding(maxNewTokens = 16, numCandidates = 5)
        assertEquals(16, decoding.maxNewTokens)
        assertEquals(5, decoding.numCandidates)
    }
}

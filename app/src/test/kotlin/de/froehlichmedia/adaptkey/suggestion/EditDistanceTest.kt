package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the shared single-edit distance helper.
 */
class EditDistanceTest {
    
    @Test
    fun `equal strings are within one edit`() {
        assertTrue(EditDistance.atMostOne("der", "der"))
    }
    
    @Test
    fun `a single substitution is within one edit`() {
        assertTrue(EditDistance.atMostOne("der", "dor"))
    }
    
    @Test
    fun `a single insertion is within one edit`() {
        assertTrue(EditDistance.atMostOne("de", "der"))
    }
    
    @Test
    fun `a single deletion is within one edit`() {
        assertTrue(EditDistance.atMostOne("derb", "der"))
    }
    
    @Test
    fun `two edits are not within one edit`() {
        assertFalse(EditDistance.atMostOne("dxr", "den"))
    }
    
    @Test
    fun `a length difference of two is not within one edit`() {
        assertFalse(EditDistance.atMostOne("de", "derb"))
    }
}

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the Greek-script fast path used by the A-03 classifier.
 */
class ScriptDetectorTest {
    
    @Test
    fun `isGreekLetter recognises Greek letters and rejects others`() {
        assertTrue(ScriptDetector.isGreekLetter('γ'))
        assertTrue(ScriptDetector.isGreekLetter('Ω'))
        assertFalse(ScriptDetector.isGreekLetter('a'))
        assertFalse(ScriptDetector.isGreekLetter('ü'))
        assertFalse(ScriptDetector.isGreekLetter('1'))
    }
    
    @Test
    fun `greekFraction is one for pure Greek and zero for pure Latin`() {
        assertEquals(1.0, ScriptDetector.greekFraction("γεια σου"), 1e-9)
        assertEquals(0.0, ScriptDetector.greekFraction("hallo welt"), 1e-9)
    }
    
    @Test
    fun `greekFraction ignores non-letters when computing the share`() {
        // 2 Greek letters, 2 Latin letters, punctuation and digits ignored.
        assertEquals(0.5, ScriptDetector.greekFraction("γα, ab 123!"), 1e-9)
    }
    
    @Test
    fun `greekFraction of letter-free text is zero`() {
        assertEquals(0.0, ScriptDetector.greekFraction("12 34 !!"), 1e-9)
    }
}

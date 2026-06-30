package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure K-01 calibration flow state.
 */
class CalibrationSessionTest {
    
    private fun session(): CalibrationSession {
        return CalibrationSession(listOf("alpha", "beta", "gamma"))
    }
    
    @Test
    fun `constructor rejects an empty sentence list`() {
        val empty = emptyList<String>()
        assertThrows(IllegalArgumentException::class.java) { CalibrationSession(empty) }
    }
    
    @Test
    fun `constructor rejects a blank sentence`() {
        val withBlank = listOf("ok", "   ")
        assertThrows(IllegalArgumentException::class.java) { CalibrationSession(withBlank) }
    }
    
    @Test
    fun `exposes the current sentence and counters`() {
        val session = session()
        
        assertEquals("alpha", session.currentSentence())
        assertEquals(0, session.index)
        assertEquals(1, session.currentNumber)
        assertEquals(3, session.sentenceCount)
        assertFalse(session.isOnLastSentence)
    }
    
    @Test
    fun `append builds up the typed text`() {
        val session = session()
        session.append('h')
        session.append('i')
        
        assertEquals("hi", session.typedText())
    }
    
    @Test
    fun `backspace removes the last character`() {
        val session = session()
        session.append('a')
        session.append('b')
        session.backspace()
        
        assertEquals("a", session.typedText())
    }
    
    @Test
    fun `backspace on an empty buffer is a no-op`() {
        val session = session()
        session.backspace()
        
        assertEquals("", session.typedText())
    }
    
    @Test
    fun `advance moves to the next sentence and clears the buffer`() {
        val session = session()
        session.append('x')
        
        assertTrue(session.advance())
        assertEquals("beta", session.currentSentence())
        assertEquals(2, session.currentNumber)
        assertEquals("", session.typedText())
    }
    
    @Test
    fun `advance reports the last sentence and changes nothing there`() {
        val session = session()
        session.advance()
        session.advance()
        
        assertTrue(session.isOnLastSentence)
        assertEquals("gamma", session.currentSentence())
        session.append('z')
        assertFalse(session.advance())
        assertEquals("gamma", session.currentSentence())
        assertEquals("z", session.typedText())
    }
}

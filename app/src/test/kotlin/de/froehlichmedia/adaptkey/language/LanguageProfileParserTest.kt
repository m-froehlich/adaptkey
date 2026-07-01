package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the A-03 profile-asset parser.
 */
class LanguageProfileParserTest {
    
    @Test
    fun `parses codes into profiles preserving rank order`() {
        val profiles = LanguageProfileParser.parse("de\ter\nde\ten\nen\tth")
        
        assertEquals(0, profiles[Language.GERMAN]?.rankOf("er"))
        assertEquals(1, profiles[Language.GERMAN]?.rankOf("en"))
        assertEquals(0, profiles[Language.ENGLISH]?.rankOf("th"))
    }
    
    @Test
    fun `a trailing space in the ngram is significant and preserved`() {
        val profiles = LanguageProfileParser.parse("de\ter ")
        
        assertEquals(0, profiles[Language.GERMAN]?.rankOf("er "))
        assertNull(profiles[Language.GERMAN]?.rankOf("er"))
    }
    
    @Test
    fun `a stray carriage return is tolerated without corrupting the ngram`() {
        val profiles = LanguageProfileParser.parse("de\ter\r\nde\tench\r")
        
        assertEquals(0, profiles[Language.GERMAN]?.rankOf("er"))
        assertEquals(1, profiles[Language.GERMAN]?.rankOf("ench"))
    }
    
    @Test
    fun `unknown codes and blank lines are skipped`() {
        val profiles = LanguageProfileParser.parse("\nzz\tab\n\nde\ten\n")
        
        assertTrue(profiles.containsKey(Language.GERMAN))
        assertEquals(1, profiles.size)
    }
    
    @Test
    fun `an empty input yields no profiles`() {
        assertTrue(LanguageProfileParser.parse("").isEmpty())
    }
}

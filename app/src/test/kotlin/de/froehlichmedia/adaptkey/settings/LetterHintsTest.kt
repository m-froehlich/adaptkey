// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure C-08 per-key symbol core: encode/decode round-trip, validation, default
 * fallback, and the curated palette invariants.
 */
class LetterHintsTest {
    
    @Test
    fun `encode then parse is a lossless round-trip for a valid map`() {
        val map = mapOf('q' to "@", 'e' to "€", 'z' to "•")
        val restored = LetterHints.parse(LetterHints.encode(map))
        assertEquals(LetterHints.sanitize(map), restored)
    }
    
    @Test
    fun `the default mapping round-trips unchanged`() {
        val encoded = LetterHints.encode(KeyboardLayout.DEFAULT_LETTER_HINTS)
        assertEquals(LetterHints.sanitize(KeyboardLayout.DEFAULT_LETTER_HINTS), LetterHints.parse(encoded))
    }
    
    @Test
    fun `encode is deterministic and sorted by key`() {
        val map = mapOf('z' to "•", 'a' to "@", 'm' to "-")
        assertEquals("a=@;m=-;z=•", LetterHints.encode(map))
    }
    
    @Test
    fun `encode of an empty or fully invalid map is the empty string`() {
        assertEquals("", LetterHints.encode(emptyMap()))
        assertEquals("", LetterHints.encode(mapOf('1' to "@", 'q' to "")))
    }
    
    @Test
    fun `sanitize lower-cases keys and drops invalid entries`() {
        val map = mapOf(
            'Q' to "@",      // upper-case key -> lower-cased
            '1' to "!",      // non-letter key -> dropped
            'e' to "",       // empty symbol -> dropped
            'h' to "###",    // too long -> dropped
            'm' to "a;b",    // contains entry separator -> dropped
            'n' to "x=y",    // contains key/value separator -> dropped
            'd' to "°"       // valid
        )
        assertEquals(mapOf('q' to "@", 'd' to "°"), LetterHints.sanitize(map))
    }
    
    @Test
    fun `isValidEntry enforces the validation rules`() {
        assertTrue(LetterHints.isValidEntry('q', "@"))
        assertTrue(LetterHints.isValidEntry('Q', "@"))
        assertFalse(LetterHints.isValidEntry('1', "@"))
        assertFalse(LetterHints.isValidEntry('q', ""))
        assertFalse(LetterHints.isValidEntry('q', "abc"))
        assertFalse(LetterHints.isValidEntry('q', "a;"))
        assertFalse(LetterHints.isValidEntry('q', "a="))
    }
    
    @Test
    fun `parse discards malformed segments but keeps the valid ones`() {
        val parsed = LetterHints.parse("q=@;garbage;=x;e=€;h")
        assertEquals(mapOf('q' to "@", 'e' to "€"), parsed)
    }
    
    @Test
    fun `parse of null blank or fully invalid input is empty without fallback`() {
        assertTrue(LetterHints.parse(null).isEmpty())
        assertTrue(LetterHints.parse("   ").isEmpty())
        assertTrue(LetterHints.parse("1=!;=x").isEmpty())
    }
    
    @Test
    fun `decodeOrDefault falls back to the default mapping when empty`() {
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, LetterHints.decodeOrDefault(null))
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, LetterHints.decodeOrDefault(""))
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, LetterHints.decodeOrDefault("1=!"))
    }
    
    @Test
    fun `decodeOrDefault returns the parsed map when non-empty`() {
        assertEquals(mapOf('q' to "!"), LetterHints.decodeOrDefault("q=!"))
    }
    
    @Test
    fun `the curated palette is non-empty unique and fully valid`() {
        assertFalse(LetterHints.PALETTE.isEmpty())
        assertEquals(LetterHints.PALETTE.size, LetterHints.PALETTE.toSet().size)
        for (symbol in LetterHints.PALETTE) {
            assertTrue(LetterHints.isValidEntry('q', symbol), "palette symbol must be assignable: $symbol")
        }
    }
    
    @Test
    fun `every default symbol is offered by the palette`() {
        assertTrue(LetterHints.PALETTE.containsAll(KeyboardLayout.DEFAULT_LETTER_HINTS.values.toSet()))
    }
}

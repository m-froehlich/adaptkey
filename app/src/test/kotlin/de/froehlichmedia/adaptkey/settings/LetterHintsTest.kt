// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure C-08 per-key symbol core: parsing and validation of the `hints_<code>.tsv`
 * persisted format read by [LanguageLetterHintsLoader].
 */
class LetterHintsTest {
    
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
    fun `parse of null blank or fully invalid input is empty`() {
        assertTrue(LetterHints.parse(null).isEmpty())
        assertTrue(LetterHints.parse("   ").isEmpty())
        assertTrue(LetterHints.parse("1=!;=x").isEmpty())
    }
}

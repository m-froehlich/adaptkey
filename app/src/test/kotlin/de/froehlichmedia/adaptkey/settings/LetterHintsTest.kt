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
    fun `D-497 a comma-separated variant list validates per variant`() {
        assertTrue(LetterHints.isValidEntry('e', "ę,ė"))
        assertTrue(LetterHints.isValidEntry('u', "ų,ū"))
        // every variant individually subject to the same rules the old whole-value symbol had
        assertTrue(LetterHints.isValidEntry('e', "ę")) // single variant = pre-D-497 behaviour
        assertFalse(LetterHints.isValidEntry('e', "ę,")) // empty trailing variant
        assertFalse(LetterHints.isValidEntry('e', ",ė")) // empty leading variant
        assertFalse(LetterHints.isValidEntry('e', "ę,,ė")) // empty middle variant
        assertFalse(LetterHints.isValidEntry('e', "ę,abc")) // second variant too long
        assertFalse(LetterHints.isValidEntry('e', "ę,a;b")) // second variant carries the entry separator
        assertFalse(LetterHints.isValidEntry('e', "ę,a=b")) // second variant carries the key/value separator
    }
    
    @Test
    fun `D-497 parse keeps a multi-variant value verbatim - the popup split happens at key build time`() {
        assertEquals(mapOf('e' to "ę,ė", 'u' to "ų,ū"), LetterHints.parse("e=ę,ė;u=ų,ū"))
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

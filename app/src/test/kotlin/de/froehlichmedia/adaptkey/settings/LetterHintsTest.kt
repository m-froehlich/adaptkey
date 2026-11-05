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
    
    @Test
    fun `D-513 parseDelta reads variant entries and empty-value removals`() {
        val delta = LetterHints.parseDelta("n=ń;s=;e=ę,ė;q=")
        assertEquals("ń", delta['n'])
        assertEquals("ę,ė", delta['e'])
        // An empty value is an explicit removal, distinct from an absent key.
        assertEquals(null, delta['s'])
        assertTrue(delta.containsKey('s'))
        assertEquals(null, delta['q'])
        assertTrue(delta.containsKey('q'))
        assertFalse(delta.containsKey('z'))
    }
    
    @Test
    fun `D-513 parseDelta demotes a malformed variant value to a removal`() {
        val delta = LetterHints.parseDelta("a=á;e=ę,abc;b=x=y")
        // Dropping towards the base is the safe direction: a corrupt entry never resurrects base data.
        assertEquals(null, delta['e'])
        assertEquals(null, delta['b'])
        assertEquals("á", delta['a'])
    }
    
    @Test
    fun `D-513 parseDelta of null blank input is empty`() {
        assertTrue(LetterHints.parseDelta(null).isEmpty())
        assertTrue(LetterHints.parseDelta("   ").isEmpty())
    }
    
    @Test
    fun `D-513 unionMerge - a key only the base carries stays the base`() {
        val merged = LetterHints.unionMerge(mapOf('q' to "@", 'd' to "$"), emptyMap())
        assertEquals(mapOf('q' to "@", 'd' to "$"), merged)
    }
    
    @Test
    fun `D-513 unionMerge - an explicit removal drops the key entirely`() {
        val merged = LetterHints.unionMerge(
            mapOf('q' to "@", 'a' to "ä", 's' to "ß"),
            mapOf('a' to null, 's' to null, 'q' to "@")
        )
        // English's own delta (section 2.10): the umlaut keys vanish, the kept symbol survives.
        assertEquals(mapOf('q' to "@"), merged)
    }
    
    @Test
    fun `D-513 unionMerge - the base symbol appends after the language's own variants`() {
        // section 5's pinned French order: e=é,è,ê,ë merges to é,è,ê,ë,€ (letterKey's centred assembly
        // puts the first variant in the pre-selected first cell).
        val merged = LetterHints.unionMerge(mapOf('e' to "€"), LetterHints.parseDelta("e=é,è,ê,ë"))
        assertEquals(mapOf('e' to "é,è,ê,ë,€"), merged)
    }
    
    @Test
    fun `D-513 unionMerge - the universal tilde-n becomes the first extra under a language letter`() {
        // section 2.6: Polish owns ń for n; the merged variant string is what letterKey's dual-corner
        // assembly turns into the [( | ñ | ń ] straddle (left corner code-side, tilde-n middle,
        // the language letter the release-default last cell).
        val merged = LetterHints.unionMerge(mapOf('n' to "ñ"), LetterHints.parseDelta("n=ń"))
        assertEquals(mapOf('n' to "ń,ñ"), merged)
    }
    
    @Test
    fun `D-513 unionMerge - a German umlaut letter never appends to a key the language owns`() {
        // section 2.10's L-05 rationale: a set tuned for German is not what a language without those
        // characters needs - Czech a stays Czech, Danish o stays Danish, no silent base-letter extras.
        val merged = LetterHints.unionMerge(
            mapOf('a' to "ä", 'o' to "ö", 'u' to "ü", 's' to "ß"),
            LetterHints.parseDelta("a=á;o=ø;u=;s=š")
        )
        assertEquals(mapOf('a' to "á", 'o' to "ø", 's' to "š"), merged)
    }
    
    @Test
    fun `D-513 unionMerge - a duplicate base value is not appended twice`() {
        val merged = LetterHints.unionMerge(mapOf('o' to "ö", 'u' to "ü"), LetterHints.parseDelta("o=ö;u=ü"))
        assertEquals(mapOf('o' to "ö", 'u' to "ü"), merged)
    }
    
    @Test
    fun `D-513 unionMerge - a delta entry for a key the base does not own is taken verbatim`() {
        // The merge contract the non-union replace paths rely on as well: the language's own data always
        // wins its key (e.g. Uzbek's o=o-modifier or a pack's currency shorthand on t).
        val merged = LetterHints.unionMerge(emptyMap(), LetterHints.parseDelta("m=-;t=zł"))
        assertEquals(mapOf('m' to "-", 't' to "zł"), merged)
    }
}

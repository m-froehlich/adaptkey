// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the §6 sentence-start detection, including the comma-terminated-line rule (C-10)
 * and the abbreviation / enumerator rule.
 */
class SentenceBoundaryTest {
    
    private fun start(before: String, comma: Boolean = true): Boolean {
        return SentenceBoundary.isSentenceStart(before, comma)
    }
    
    @Test
    fun `the start of the field is a sentence start`() {
        assertTrue(start(""))
        assertTrue(start("   "))
    }
    
    @Test
    fun `after a full stop followed by a space is a sentence start`() {
        assertTrue(start("Erster Satz. "))
        assertTrue(start("Wirklich? "))
        assertTrue(start("Stop! "))
    }
    
    @Test
    fun `mid-line without a terminator is not a sentence start`() {
        assertFalse(start("ich gehe nach "))
        assertFalse(start("Hallo Max, "))
    }
    
    @Test
    fun `a period without following whitespace is not yet a boundary`() {
        assertFalse(start("foo."))
    }
    
    @Test
    fun `a new line begins a new sentence`() {
        assertTrue(start("Zeile eins\n"))
        assertTrue(start("Erster Satz.\n"))
    }
    
    @Test
    fun `a comma-terminated line suppresses the next line's sentence start when enabled`() {
        // The German e-mail salutation, blank line included.
        assertFalse(start("Hallo Max Mustermann,\n\n"))
        assertFalse(start("das ist gut,\naber"))
    }
    
    @Test
    fun `the comma-line rule can be disabled`() {
        assertTrue(start("Hallo Max Mustermann,\n\n", comma = false))
    }
    
    @Test
    fun `a period after a known abbreviation is not a sentence start`() {
        assertFalse(start("und so weiter usw. "))
        assertFalse(start("siehe Nr. "))
    }
    
    @Test
    fun `a period after an enumerator is not a sentence start`() {
        assertFalse(start("1. "))
        assertFalse(start("Liste 10. "))
    }
    
    @Test
    fun `an abbreviation at the end of a line still starts the new line`() {
        assertTrue(start("und so weiter usw.\n"))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace appends a virtual space after a bare terminator`() {
        assertTrue(start(SentenceBoundary.withPendingTerminatorSpace("Erster Satz.")))
        assertTrue(start(SentenceBoundary.withPendingTerminatorSpace("Wirklich?")))
        assertTrue(start(SentenceBoundary.withPendingTerminatorSpace("Stop!")))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace does not touch a comma`() {
        assertFalse(start(SentenceBoundary.withPendingTerminatorSpace("Hallo Max,")))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace still respects the abbreviation veto`() {
        assertFalse(start(SentenceBoundary.withPendingTerminatorSpace("und so weiter usw.")))
        assertFalse(start(SentenceBoundary.withPendingTerminatorSpace("siehe Nr.")))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace still respects the enumerator veto`() {
        assertFalse(start(SentenceBoundary.withPendingTerminatorSpace("1.")))
        assertFalse(start(SentenceBoundary.withPendingTerminatorSpace("Liste 10.")))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace leaves text with real trailing whitespace unchanged`() {
        assertEquals("Erster Satz. ", SentenceBoundary.withPendingTerminatorSpace("Erster Satz. "))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace leaves mid-word text unchanged`() {
        assertEquals("ich gehe nach", SentenceBoundary.withPendingTerminatorSpace("ich gehe nach"))
    }
    
    @Test
    fun `D-416 withPendingTerminatorSpace leaves empty text unchanged`() {
        assertEquals("", SentenceBoundary.withPendingTerminatorSpace(""))
    }
    
    @Test
    fun `D-373 previousHyphenSegment finds the segment right before the hyphen`() {
        // Nothing precedes "München" here, so this position is also, correctly, a sentence start - the
        // dedicated tests below cover the sentence-start flag itself in both directions.
        val result = SentenceBoundary.previousHyphenSegment("München-", suppressAfterCommaLine = true)
        assertEquals("München", result?.first)
        assertTrue(result?.second == true)
    }
    
    @Test
    fun `D-373 previousHyphenSegment finds the last segment of a longer chain`() {
        // "Main", not "Rhein" - the segment immediately before *this* hyphen.
        val result = SentenceBoundary.previousHyphenSegment("Rhein-Main-", suppressAfterCommaLine = true)
        assertEquals("Main", result?.first)
    }
    
    @Test
    fun `D-373 previousHyphenSegment reports the segment as a sentence start when it genuinely is one`() {
        val result = SentenceBoundary.previousHyphenSegment("Schnell-", suppressAfterCommaLine = true)
        assertEquals("Schnell", result?.first)
        assertTrue(result?.second == true)
    }
    
    @Test
    fun `D-373 previousHyphenSegment reports no sentence start mid-sentence`() {
        val result = SentenceBoundary.previousHyphenSegment("Ich fahre nach München-", suppressAfterCommaLine = true)
        assertEquals("München", result?.first)
        assertFalse(result?.second == true)
    }
    
    @Test
    fun `D-373 previousHyphenSegment is null when nothing precedes the hyphen`() {
        assertEquals(null, SentenceBoundary.previousHyphenSegment("-", suppressAfterCommaLine = true))
        assertEquals(null, SentenceBoundary.previousHyphenSegment("  -", suppressAfterCommaLine = true))
    }
    
    @Test
    fun `D-434 isSentenceStart defaults to the German abbreviation set when none is passed, matching the old behaviour`() {
        assertFalse(start("und so weiter usw. "))
    }
    
    @Test
    fun `D-434 isSentenceStart honours an explicitly passed-in, non-German abbreviation set`() {
        // "usw." is not in this made-up set, so its period genuinely terminates the sentence here.
        assertTrue(SentenceBoundary.isSentenceStart("und so weiter usw. ", true, setOf("etc.")))
        // "etc." now IS in the passed-in set, so it correctly suppresses the sentence start.
        assertFalse(SentenceBoundary.isSentenceStart("and so on etc. ", true, setOf("etc.")))
    }
    
    @Test
    fun `D-434 isSentenceStart with an empty abbreviation set never vetoes any period`() {
        assertTrue(SentenceBoundary.isSentenceStart("und so weiter usw. ", true, emptySet()))
    }
    
    @Test
    fun `D-434 previousHyphenSegment forwards the passed-in abbreviation set to the segment's own sentence-start check`() {
        // The segment is "Wichtig"; the context right before it ("usw. ") only counts as a sentence start
        // when "usw." is NOT in the forwarded abbreviation set (otherwise its period is vetoed as non-terminal).
        val withDefault = SentenceBoundary.previousHyphenSegment("usw. Wichtig-", suppressAfterCommaLine = true)
        assertEquals("Wichtig", withDefault?.first)
        assertFalse(withDefault?.second == true)
        
        val withEmptySet = SentenceBoundary.previousHyphenSegment(
            "usw. Wichtig-", suppressAfterCommaLine = true, abbreviations = emptySet()
        )
        assertTrue(withEmptySet?.second == true)
    }
}

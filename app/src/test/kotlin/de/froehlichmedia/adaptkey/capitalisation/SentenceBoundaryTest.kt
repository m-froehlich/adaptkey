// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

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
}

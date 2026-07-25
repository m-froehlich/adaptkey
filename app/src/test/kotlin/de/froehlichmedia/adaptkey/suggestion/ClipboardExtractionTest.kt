// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Unit tests for D-266's pure clipboard-extraction logic. */
class ClipboardExtractionTest {
    
    @Test
    fun `firstLine returns the first non-blank line, trimmed`() {
        assertEquals("Hallo Welt", ClipboardExtraction.firstLine("  Hallo Welt  \nZweite Zeile"))
    }
    
    @Test
    fun `firstLine skips leading blank lines`() {
        assertEquals("Erste echte Zeile", ClipboardExtraction.firstLine("\n\n   \nErste echte Zeile\nRest"))
    }
    
    @Test
    fun `firstLine is null for entirely blank text`() {
        assertNull(ClipboardExtraction.firstLine("   \n  \n"))
    }
    
    @Test
    fun `firstLine is null for empty text`() {
        assertNull(ClipboardExtraction.firstLine(""))
    }
    
    @Test
    fun `firstCode extracts a URL query-parameter value`() {
        // The motivating example from the user's own spec.
        assertEquals("SDF123rtert", ClipboardExtraction.firstCode("https://example.com/invite?code=SDF123rtert"))
    }
    
    @Test
    fun `firstCode extracts only the first query parameter's value when several are present`() {
        assertEquals("abc123", ClipboardExtraction.firstCode("https://example.com/x?code=abc123&ref=xyz"))
    }
    
    @Test
    fun `firstCode strips a trailing fragment from the query value`() {
        assertEquals("abc123", ClipboardExtraction.firstCode("https://example.com/x?code=abc123#section"))
    }
    
    @Test
    fun `firstCode falls back to the last path segment when there is no query string`() {
        assertEquals("invite42", ClipboardExtraction.firstCode("https://example.com/join/invite42"))
    }
    
    @Test
    fun `firstCode falls back to the generic alphanumeric run for ordinary pasted text`() {
        // The other motivating example from the user's own spec.
        assertEquals("code123", ClipboardExtraction.firstCode("<code123 foo"))
    }
    
    @Test
    fun `firstCode does not treat whitespace-containing text as URL-shaped`() {
        // Ordinary prose that merely happens to contain a "?" must not be routed through the query/path
        // parser - it falls straight through to the generic fallback instead.
        assertEquals("Is", ClipboardExtraction.firstCode("Is this ok? Thanks"))
    }
    
    @Test
    fun `firstCode returns null when there is no alphanumeric content at all`() {
        assertNull(ClipboardExtraction.firstCode("   !!! ---   "))
    }
    
    @Test
    fun `firstCode is null for empty text`() {
        assertNull(ClipboardExtraction.firstCode(""))
    }
}

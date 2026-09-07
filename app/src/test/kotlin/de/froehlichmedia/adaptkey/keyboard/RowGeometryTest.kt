// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RowGeometry] (D-397) - the shared row/column position model
 * [de.froehlichmedia.adaptkey.suggestion.KeyboardProximity] and [AdaptKeyboardView]'s own generic
 * vertical touch-drift cap both build on.
 */
class RowGeometryTest {
    
    @Test
    fun `rowIndexOf finds a top-row letter without the digit row`() {
        assertEquals(0, RowGeometry.rowIndexOf(LayoutKind.LATIN_QWERTZ, includeDigitRow = false, c = 'q'))
    }
    
    @Test
    fun `rowIndexOf shifts every row down by one once the digit row is included`() {
        assertEquals(0, RowGeometry.rowIndexOf(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = '1'))
        assertEquals(1, RowGeometry.rowIndexOf(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'q'))
        assertEquals(2, RowGeometry.rowIndexOf(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'a'))
        assertEquals(3, RowGeometry.rowIndexOf(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'y'))
    }
    
    @Test
    fun `rowIndexOf returns null for a character not part of this layout's own rows`() {
        assertNull(RowGeometry.rowIndexOf(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = '+'))
    }
    
    @Test
    fun `D-397 the reported case - q has a row below it, a does not have one above without the digit row`() {
        // The named repro: q's own downward drift reaching far enough to register as a.
        assertTrue(RowGeometry.hasRowBelow(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'q'))
        assertTrue(RowGeometry.hasRowAbove(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'a'))
    }
    
    @Test
    fun `the digit row counts exactly like any other row, per explicit user instruction`() {
        // With the number row shown: the top letter row has a row above it (the digits), and the digit row
        // itself has a row below it (the top letter row) - neither is a special case.
        assertTrue(RowGeometry.hasRowAbove(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'q'))
        assertFalse(RowGeometry.hasRowAbove(LayoutKind.LATIN_QWERTZ, includeDigitRow = false, c = 'q'))
        assertTrue(RowGeometry.hasRowBelow(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = '5'))
        assertFalse(RowGeometry.hasRowAbove(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = '5'))
    }
    
    @Test
    fun `the bottom-most letter row has no row below it in this model - that boundary keeps its own tighter override`() {
        // D-133's own space-bar override is deliberately not modelled here (see RowGeometry.hasRowBelow's
        // own KDoc) - this only confirms the generic mechanism stays silent for that direction, not that
        // nothing at all applies there.
        assertFalse(RowGeometry.hasRowBelow(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'm'))
        assertTrue(RowGeometry.hasRowAbove(LayoutKind.LATIN_QWERTZ, includeDigitRow = true, c = 'm'))
    }
    
    @Test
    fun `every layout's own letter rows are reachable, not only QWERTZ`() {
        assertTrue(RowGeometry.hasRowBelow(LayoutKind.LATIN_AZERTY, includeDigitRow = true, c = 'a'))
        assertTrue(RowGeometry.hasRowAbove(LayoutKind.GREEK, includeDigitRow = true, c = 'ς'))
        assertTrue(RowGeometry.hasRowBelow(LayoutKind.SERBIAN_CYRILLIC, includeDigitRow = true, c = 'љ'))
    }
    
    @Test
    fun `rowsFor prefixes the digit row only when requested`() {
        val withoutDigits = RowGeometry.rowsFor(LayoutKind.LATIN_QWERTY, includeDigitRow = false)
        val withDigits = RowGeometry.rowsFor(LayoutKind.LATIN_QWERTY, includeDigitRow = true)
        assertEquals(3, withoutDigits.size)
        assertEquals(4, withDigits.size)
        assertEquals(RowGeometry.DIGIT_ROW, withDigits.first())
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-137 typed-time recognition. */
class TimePatternTest {
    
    @Test
    fun `a two-digit hour and minute match`() {
        assertTrue(TimePattern.endsWithTime("Wir sehen uns um 14:30"))
    }
    
    @Test
    fun `a single-digit hour matches`() {
        assertTrue(TimePattern.endsWithTime("um 9:05"))
    }
    
    @Test
    fun `trailing whitespace after the time still matches`() {
        assertTrue(TimePattern.endsWithTime("um 14:30 "))
    }
    
    @Test
    fun `a single-digit minute does not match`() {
        assertFalse(TimePattern.endsWithTime("um 14:3"))
    }
    
    @Test
    fun `text with no time at all does not match`() {
        assertFalse(TimePattern.endsWithTime("Hallo wie geht es dir"))
    }
    
    @Test
    fun `a time not at the end of the text does not match`() {
        assertFalse(TimePattern.endsWithTime("14:30 Uhr ist ein guter Zeitpunkt"))
    }
    
    @Test
    fun `empty text does not match`() {
        assertFalse(TimePattern.endsWithTime(""))
    }
}

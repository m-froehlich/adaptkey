// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Unit tests for the D-376 "km/h" completion recognition. */
class SpeedUnitCompletionTest {
    
    @Test
    fun `composing exactly km offers the full unit`() {
        assertEquals("km/h", SpeedUnitCompletion.completionForComposing("km"))
    }
    
    @Test
    fun `composing km is case-insensitive`() {
        assertEquals("km/h", SpeedUnitCompletion.completionForComposing("KM"))
    }
    
    @Test
    fun `composing that is only a prefix of km does not offer the unit`() {
        assertNull(SpeedUnitCompletion.completionForComposing("k"))
    }
    
    @Test
    fun `composing longer than km does not offer the unit`() {
        assertNull(SpeedUnitCompletion.completionForComposing("kmh"))
    }
    
    @Test
    fun `empty composing does not offer the unit`() {
        assertNull(SpeedUnitCompletion.completionForComposing(""))
    }
    
    @Test
    fun `a word-bounded trailing km slash offers the glued suffix`() {
        assertEquals("h", SpeedUnitCompletion.suffixAfterSlash("5km/"))
    }
    
    @Test
    fun `km slash is case-insensitive`() {
        assertEquals("h", SpeedUnitCompletion.suffixAfterSlash("5KM/"))
    }
    
    @Test
    fun `km slash at the very start of the text still matches`() {
        assertEquals("h", SpeedUnitCompletion.suffixAfterSlash("km/"))
    }
    
    @Test
    fun `km slash not at the end of the text does not match`() {
        assertNull(SpeedUnitCompletion.suffixAfterSlash("km/h ist die Einheit"))
    }
    
    @Test
    fun `km not followed by a slash does not match`() {
        assertNull(SpeedUnitCompletion.suffixAfterSlash("5km"))
    }
    
    @Test
    fun `a slash not preceded by km does not match`() {
        assertNull(SpeedUnitCompletion.suffixAfterSlash("und/oder/"))
    }
    
    @Test
    fun `km glued onto a longer word does not match`() {
        assertNull(SpeedUnitCompletion.suffixAfterSlash("akm/"))
    }
    
    @Test
    fun `empty text does not match`() {
        assertNull(SpeedUnitCompletion.suffixAfterSlash(""))
    }
}

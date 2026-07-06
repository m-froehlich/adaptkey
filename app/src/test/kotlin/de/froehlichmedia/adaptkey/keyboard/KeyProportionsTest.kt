// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for the key-proportion configuration (C-01, L-02 / L-04).
 */
class KeyProportionsTest {
    
    @Test
    fun `backspace weight applies the L04 surcharge`() {
        val proportions = KeyProportions()
        assertEquals(1.65f, proportions.backspaceWeight, 1e-4f)
    }
    
    @Test
    fun `third row letter weight is reduced below the base letter weight`() {
        val proportions = KeyProportions()
        val reduced = proportions.thirdRowLetterWeight(7)
        
        assertEquals(1.0f - (1.5f * 0.10f) / 7f, reduced, 1e-4f)
        assertTrue(reduced < proportions.letterWeight)
    }
    
    @Test
    fun `third row letter weight falls back to base weight for zero letters`() {
        val proportions = KeyProportions()
        assertEquals(proportions.letterWeight, proportions.thirdRowLetterWeight(0), 1e-4f)
    }
    
    @Test
    fun `third row letter weight rejects a negative count`() {
        val proportions = KeyProportions()
        assertThrows<IllegalArgumentException> { proportions.thirdRowLetterWeight(-1) }
    }
    
    @Test
    fun `L04 preserves the third row total width`() {
        val proportions = KeyProportions()
        val letters = 7
        
        val tuned = proportions.shiftWeight +
            letters * proportions.thirdRowLetterWeight(letters) +
            proportions.backspaceWeight
        val baseline = proportions.shiftWeight +
            letters * proportions.letterWeight +
            proportions.backspaceBaseWeight
        
        assertEquals(baseline, tuned, 1e-4f)
    }
    
    @Test
    fun `L02 widens comma and full stop equally beyond a letter`() {
        val proportions = KeyProportions()
        assertEquals(proportions.commaWeight, proportions.periodWeight, 1e-4f)
        assertTrue(proportions.commaWeight > proportions.letterWeight)
    }
    
    @Test
    fun `L02 narrows the space bar relative to the Gboard baseline`() {
        val proportions = KeyProportions()
        // Still the widest single key in its row, but well below a Gboard-style ~5 unit space bar.
        assertTrue(proportions.spaceWeight > proportions.commaWeight)
        assertTrue(proportions.spaceWeight < 4.0f)
    }
    
    @Test
    fun `constructor rejects a non-positive space weight`() {
        assertThrows<IllegalArgumentException> { KeyProportions(spaceWeight = 0f) }
    }
    
    @Test
    fun `constructor rejects a negative backspace surcharge`() {
        assertThrows<IllegalArgumentException> { KeyProportions(backspaceExtra = -0.1f) }
    }
    
    @Test
    fun `D-16 shift weight applies the surcharge and is off by default`() {
        assertEquals(1.5f, KeyProportions().shiftWeight, 1e-4f)
        assertEquals(1.5f * 1.10f, KeyProportions(shiftExtra = 0.10f).shiftWeight, 1e-4f)
    }
    
    @Test
    fun `D-16 a shift surcharge is also taken from the third-row letters, preserving the row width`() {
        val proportions = KeyProportions(shiftExtra = 0.10f, backspaceExtra = 0f)
        val letters = 7
        
        val tuned = proportions.shiftWeight +
            letters * proportions.thirdRowLetterWeight(letters) +
            proportions.backspaceWeight
        val baseline = proportions.shiftBaseWeight +
            letters * proportions.letterWeight +
            proportions.backspaceBaseWeight
        
        assertEquals(baseline, tuned, 1e-4f)
    }
    
    @Test
    fun `constructor rejects a negative shift surcharge`() {
        assertThrows<IllegalArgumentException> { KeyProportions(shiftExtra = -0.1f) }
    }
}

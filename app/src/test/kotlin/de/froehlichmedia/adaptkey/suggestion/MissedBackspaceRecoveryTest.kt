// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.TapPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the pure D-377 missed-Backspace recovery. */
class MissedBackspaceRecoveryTest {
    
    // Backspace sits at x=100, one key-width (20) wide/tall in each half-direction.
    private val backspace = OffsetModel.Candidate("DELETE", centerX = 100f, centerY = 0f, halfWidth = 20f, halfHeight = 20f)
    
    // "welxmche" for "welche": positions 0-7 = w,e,l,x,m,c,h,e. The "m" (index 4) is the missed-Backspace tap,
    // landing on the Backspace key itself; every other tap sits far away, unambiguously on its own letter.
    private val welxmcheTaps = listOf(
        TapPoint(0f, 0f), TapPoint(0f, 0f), TapPoint(0f, 0f), TapPoint(0f, 0f),
        TapPoint(100f, 0f), TapPoint(0f, 0f), TapPoint(0f, 0f), TapPoint(0f, 0f)
    )
    
    @Test
    fun `a tap landing on Backspace collapses with its predecessor`() {
        val candidates = MissedBackspaceRecovery.recover("welxmche", welxmcheTaps, backspace)
        
        assertEquals(listOf("welche"), candidates)
    }
    
    @Test
    fun `a tap one key-width away from Backspace still counts`() {
        // Backspace's own half-width is 20 - a tap at x=100+40=140 sits exactly one more key-width out.
        val taps = welxmcheTaps.toMutableList().also { it[4] = TapPoint(140f, 0f) }
        
        assertEquals(listOf("welche"), MissedBackspaceRecovery.recover("welxmche", taps, backspace))
    }
    
    @Test
    fun `a tap more than one key-width away from Backspace does not count`() {
        val taps = welxmcheTaps.toMutableList().also { it[4] = TapPoint(141f, 0f) }
        
        assertTrue(MissedBackspaceRecovery.recover("welxmche", taps, backspace).isEmpty())
    }
    
    @Test
    fun `no tap near Backspace yields no candidates`() {
        val taps = List(8) { TapPoint(0f, 0f) }
        
        assertTrue(MissedBackspaceRecovery.recover("welxmche", taps, backspace).isEmpty())
    }
    
    @Test
    fun `the very first character is never removed - there is no predecessor to collapse it with`() {
        val taps = listOf(TapPoint(100f, 0f), TapPoint(0f, 0f))
        
        assertTrue(MissedBackspaceRecovery.recover("ab", taps, backspace).isEmpty())
    }
    
    @Test
    fun `multiple near-Backspace taps each produce their own candidate`() {
        // "abcd": index 1 ("b") and index 3 ("d") both land near Backspace - collapsing each with its own
        // predecessor gives two independent candidates, "cd" (removes a+b) and "ab" (removes c+d).
        val taps = listOf(TapPoint(0f, 0f), TapPoint(100f, 0f), TapPoint(0f, 0f), TapPoint(100f, 0f))
        
        assertEquals(listOf("cd", "ab"), MissedBackspaceRecovery.recover("abcd", taps, backspace))
    }
    
    @Test
    fun `a tap-count mismatch yields no candidates`() {
        assertTrue(MissedBackspaceRecovery.recover("welxmche", listOf(TapPoint(100f, 0f)), backspace).isEmpty())
    }
    
    @Test
    fun `a token shorter than two characters yields no candidates`() {
        assertTrue(MissedBackspaceRecovery.recover("w", listOf(TapPoint(100f, 0f)), backspace).isEmpty())
        assertTrue(MissedBackspaceRecovery.recover("", emptyList(), backspace).isEmpty())
    }
}

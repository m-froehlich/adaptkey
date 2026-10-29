// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the L-03 panel transition policy.
 */
class PanelNavigationTest {
    
    @Test
    fun `paragraph 49 a tap on the combined key toggles letters and symbols`() {
        assertEquals(InputSurface.SYMBOLS, PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS))
        assertEquals(InputSurface.LETTERS, PanelNavigation.onCombinedKeyTap(InputSurface.SYMBOLS))
    }
    
    @Test
    fun `paragraph 49 a tap from the emoji surface still falls back to letters`() {
        // Defensive only - the combined key is not reachable while the emoji panel is showing (its own
        // keyboard view is hidden), but the fallback stays correct regardless.
        assertEquals(InputSurface.LETTERS, PanelNavigation.onCombinedKeyTap(InputSurface.EMOJI))
    }
    
    @Test
    fun `long-press or swipe-up always switches to the symbol layer`() {
        assertEquals(InputSurface.SYMBOLS, PanelNavigation.onSwitchToSymbols())
    }
    
    @Test
    fun `D-507 with the emoji mode the combined key's tap opens the emoji panel`() {
        assertEquals(InputSurface.EMOJI, PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, CombinedKeyMode.EMOJI))
    }
    
    @Test
    fun `D-507 the symbol mode keeps the ordinary toggle, and the off mode falls through to it defensively`() {
        // "off" never dispatches (the key is hidden then); the function stays total regardless.
        assertEquals(InputSurface.SYMBOLS, PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, CombinedKeyMode.SYMBOL))
        assertEquals(InputSurface.SYMBOLS, PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, CombinedKeyMode.OFF))
    }
    
    @Test
    fun `D-507 CombinedKeyMode fromKey clamps unknown, blank and missing values to the default`() {
        assertEquals(CombinedKeyMode.SYMBOL, CombinedKeyMode.fromKey(null))
        assertEquals(CombinedKeyMode.SYMBOL, CombinedKeyMode.fromKey(""))
        assertEquals(CombinedKeyMode.SYMBOL, CombinedKeyMode.fromKey("garbage"))
        assertEquals(CombinedKeyMode.OFF, CombinedKeyMode.fromKey("off"))
        assertEquals(CombinedKeyMode.SYMBOL, CombinedKeyMode.fromKey(" SYMBOL "))
        assertEquals(CombinedKeyMode.EMOJI, CombinedKeyMode.fromKey("emoji"))
    }
    
    @Test
    fun `D-19 a forward swipe cycles letters to symbol page 1 to page 2 and wraps`() {
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 1), PanelNavigation.swipePage(InputSurface.LETTERS, 1, forward = true))
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 2), PanelNavigation.swipePage(InputSurface.SYMBOLS, 1, forward = true))
        assertEquals(PanelNavigation.Page(InputSurface.LETTERS, 1), PanelNavigation.swipePage(InputSurface.SYMBOLS, 2, forward = true))
    }
    
    @Test
    fun `D-19 a backward swipe reverses the cycle`() {
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 2), PanelNavigation.swipePage(InputSurface.LETTERS, 1, forward = false))
        assertEquals(PanelNavigation.Page(InputSurface.LETTERS, 1), PanelNavigation.swipePage(InputSurface.SYMBOLS, 1, forward = false))
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 1), PanelNavigation.swipePage(InputSurface.SYMBOLS, 2, forward = false))
    }
    
    @Test
    fun `D-19 the emoji surface is treated as the letter view in the swipe cycle`() {
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 1), PanelNavigation.swipePage(InputSurface.EMOJI, 1, forward = true))
    }
}

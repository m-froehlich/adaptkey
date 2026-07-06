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
    fun `a tap on the combined key opens the emoji panel from the letter view`() {
        assertEquals(InputSurface.EMOJI, PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS))
    }
    
    @Test
    fun `the same tap returns to letters from the symbol layer or the emoji panel`() {
        assertEquals(InputSurface.LETTERS, PanelNavigation.onCombinedKeyTap(InputSurface.SYMBOLS))
        assertEquals(InputSurface.LETTERS, PanelNavigation.onCombinedKeyTap(InputSurface.EMOJI))
    }
    
    @Test
    fun `long-press or swipe-up always switches to the symbol layer`() {
        assertEquals(InputSurface.SYMBOLS, PanelNavigation.onSwitchToSymbols())
    }
    
    @Test
    fun `D-18 with the emoji panel disabled the combined key toggles letters and symbols`() {
        assertEquals(InputSurface.SYMBOLS, PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, emojiEnabled = false))
        assertEquals(InputSurface.LETTERS, PanelNavigation.onCombinedKeyTap(InputSurface.SYMBOLS, emojiEnabled = false))
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

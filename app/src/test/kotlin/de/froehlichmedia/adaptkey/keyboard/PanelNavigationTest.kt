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
    fun `D-531 a tap on the combined key opens the special-characters page from letters`() {
        assertEquals(
            PanelNavigation.Page(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE),
            PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS)
        )
    }
    
    @Test
    fun `a tap from a symbol page falls back to letters (defensive - the key does not exist there)`() {
        assertEquals(PanelNavigation.Page(InputSurface.LETTERS, 1), PanelNavigation.onCombinedKeyTap(InputSurface.SYMBOLS))
        assertEquals(PanelNavigation.Page(InputSurface.LETTERS, 1), PanelNavigation.onCombinedKeyTap(InputSurface.EMOJI))
    }
    
    @Test
    fun `D-531 a long-press or swipe-up goes straight to the number layer (page 1)`() {
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 1), PanelNavigation.onSwitchToSymbols())
    }
    
    @Test
    fun `D-507 with the emoji mode the combined key's tap opens the emoji panel`() {
        assertEquals(PanelNavigation.Page(InputSurface.EMOJI, 1), PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, CombinedKeyMode.EMOJI))
    }
    
    @Test
    fun `D-507 the symbol mode keeps the special-characters toggle, and the off mode falls through to it defensively`() {
        // "off" never dispatches (the key is hidden then); the function stays total regardless.
        assertEquals(
            PanelNavigation.Page(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE),
            PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, CombinedKeyMode.SYMBOL)
        )
        assertEquals(
            PanelNavigation.Page(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE),
            PanelNavigation.onCombinedKeyTap(InputSurface.LETTERS, CombinedKeyMode.OFF)
        )
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
    fun `D-19 D-531 a forward swipe cycles letters to the special-characters page to the number layer and wraps`() {
        assertEquals(
            PanelNavigation.Page(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE),
            PanelNavigation.swipePage(InputSurface.LETTERS, 1, forward = true)
        )
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 1), PanelNavigation.swipePage(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE, forward = true))
        assertEquals(PanelNavigation.Page(InputSurface.LETTERS, 1), PanelNavigation.swipePage(InputSurface.SYMBOLS, 1, forward = true))
    }
    
    @Test
    fun `D-19 a backward swipe reverses the cycle`() {
        assertEquals(PanelNavigation.Page(InputSurface.SYMBOLS, 1), PanelNavigation.swipePage(InputSurface.LETTERS, 1, forward = false))
        assertEquals(
            PanelNavigation.Page(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE),
            PanelNavigation.swipePage(InputSurface.SYMBOLS, 1, forward = false)
        )
        assertEquals(PanelNavigation.Page(InputSurface.LETTERS, 1), PanelNavigation.swipePage(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE, forward = false))
    }
    
    @Test
    fun `D-19 the emoji surface is treated as the letter view in the swipe cycle`() {
        assertEquals(
            PanelNavigation.Page(InputSurface.SYMBOLS, PanelNavigation.SPECIAL_KEYS_PAGE),
            PanelNavigation.swipePage(InputSurface.EMOJI, 1, forward = true)
        )
    }
}

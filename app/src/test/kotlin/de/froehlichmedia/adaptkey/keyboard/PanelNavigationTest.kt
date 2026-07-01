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
}

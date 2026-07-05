// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Pure transition policy for the combined emoji / ?123 key and its surrounding "back" affordances
 * (L-03).
 */
object PanelNavigation {
    
    /**
     * A tap on the combined key opens the emoji panel from the letter view; the same action, routed
     * from the numeric/symbol layer's "ABC" key or the emoji panel's back button, returns to letters.
     *
     * @param current the currently shown surface
     * @return the surface to switch to
     */
    fun onCombinedKeyTap(current: InputSurface): InputSurface {
        return when (current) {
            InputSurface.LETTERS -> InputSurface.EMOJI
            InputSurface.SYMBOLS, InputSurface.EMOJI -> InputSurface.LETTERS
        }
    }
    
    /**
     * A long-press or upward swipe on the combined key always switches to the numeric/symbol layer,
     * regardless of the current surface.
     *
     * @return [InputSurface.SYMBOLS]
     */
    fun onSwitchToSymbols(): InputSurface = InputSurface.SYMBOLS
    
    /**
     * A visible surface/page in the D-19 horizontal-swipe cycle: the letter view, then the two
     * numeric/symbol pages. The emoji panel is deliberately not part of the swipe cycle (it is reached
     * by tapping the combined key).
     *
     * @property surface the input surface to show
     * @property symbolPage the numeric/symbol page (only meaningful when [surface] is [InputSurface.SYMBOLS])
     */
    data class Page(val surface: InputSurface, val symbolPage: Int)
    
    // D-19: letters -> symbols page 1 -> symbols page 2, cycled by a full-field horizontal swipe.
    private val SWIPE_CYCLE = listOf(
        Page(InputSurface.LETTERS, 1),
        Page(InputSurface.SYMBOLS, 1),
        Page(InputSurface.SYMBOLS, 2)
    )
    
    /**
     * The surface/page a D-19 horizontal swipe moves to. A right swipe ([forward] true) advances through
     * [letters → symbols-1 → symbols-2] and wraps; a left swipe goes back. The emoji surface is treated
     * as the letter view (its own tap affordance leaves it), so a swipe from emoji lands on symbols.
     *
     * @param current the currently shown surface
     * @param symbolPage the current numeric/symbol page (used only when [current] is [InputSurface.SYMBOLS])
     * @param forward true for a right swipe (next), false for a left swipe (previous)
     * @return the surface/page to switch to
     */
    fun swipePage(current: InputSurface, symbolPage: Int, forward: Boolean): Page {
        val normalised = if (current == InputSurface.EMOJI) InputSurface.LETTERS else current
        val index = SWIPE_CYCLE.indexOfFirst {
            it.surface == normalised && (normalised != InputSurface.SYMBOLS || it.symbolPage == symbolPage)
        }.coerceAtLeast(0)
        val size = SWIPE_CYCLE.size
        val next = if (forward) (index + 1) % size else (index - 1 + size) % size
        return SWIPE_CYCLE[next]
    }
}

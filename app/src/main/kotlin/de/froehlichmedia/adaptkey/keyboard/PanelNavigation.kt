// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-507: what the bottom-row combined key's slot carries - three mutually exclusive functions, chosen by
 * one setting (the user's own decision: a single enum instead of two coupled booleans, so the impossible
 * "both at once" state structurally cannot exist and no cross-setting dependency wiring is needed).
 * 
 * The stored values are `off` / `symbol` / `emoji` - keep them in sync with `arrays.xml`
 * (`d507_combined_key_values`). Declared with "off" first (the standing rule for pick-one settings),
 * default [SYMBOL] (the behaviour every install has always had - nobody sees a change until the setting
 * is touched).
 */
enum class CombinedKeyMode {
    
    /** The key is drawn blank and inert, its slot kept (D-59's original hidden treatment). */
    OFF,
    
    /** The key is the plain `?123` numeric/symbol-layer toggle - the default, and the only mode in which the symbol pages' `ABC` keys exist. */
    SYMBOL,
    
    /** The key opens the emoji panel (the same surface the §48 extra-row button opens; that button stays, deliberately redundant). */
    EMOJI;
    
    companion object {
        
        val DEFAULT = SYMBOL
        
        /**
         * Resolves a stored preference value, tolerating case and unknown/blank input (falls back to
         * [DEFAULT], so a corrupt stored value cannot leave the setting invalid).
         */
        fun fromKey(key: String?): CombinedKeyMode {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

/**
 * Pure transition policy for the combined emoji / ?123 key and its surrounding "back" affordances
 * (L-03).
 */
object PanelNavigation {
    
    /**
     * A tap on the combined key toggles between the letter view and the numeric/symbol layer - or, since
     * D-507, opens the emoji panel when the combined-key setting has put the emoji function into the key's
     * slot. The `[InputSurface.EMOJI]` return value goes through the ordinary [InputSurface] switch, so
     * `AdaptKeyService`'s one [InputSurface] choke point (panel visibility, the D-472 return button, and
     * D-507's extra-row auto-open) handles every entry point identically.
     * 
     * §49 (implementing §48): the combined key used to also open the emoji panel (D-18, gated on the now-
     * retired `emojiPanelEnabled` setting) - now that the emoji button has its own dedicated,
     * always-reachable home in the settings row, that dual purpose was gone; the combined key was always a
     * plain `?123` toggle, unconditionally. D-507 re-introduces the emoji function on this key, but as its
     * own dedicated setting mode rather than D-18's second gate, and the extra-row button deliberately
     * stays alongside it.
     * 
     * [CombinedKeyMode.OFF] never dispatches (the key is hidden then); it falls through to the ordinary
     * toggle purely so the function stays total.
     * 
     * @param current the currently shown surface
     * @param mode what the combined key's slot currently carries
     * @return the surface to switch to
     */
    fun onCombinedKeyTap(current: InputSurface, mode: CombinedKeyMode = CombinedKeyMode.SYMBOL): InputSurface {
        if (mode == CombinedKeyMode.EMOJI) {
            return InputSurface.EMOJI
        }
        return if (current == InputSurface.LETTERS) InputSurface.SYMBOLS else InputSurface.LETTERS
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

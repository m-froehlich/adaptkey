// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-507: what the bottom-row combined key's slot carries - three mutually exclusive functions, chosen by
 * one setting (the user's own decision: a single enum instead of two coupled booleans, so the impossible
 * "both at once" state structurally cannot exist and no cross-setting dependency wiring is needed).
 * 
 * D-531: the symbol mode's key is the `!?#` special-characters-page toggle now - the number layer itself
 * is only needed for genuinely numeric input (number/date/phone fields, which open it automatically),
 * so the letters page's own key leads to the special-characters page instead, and that page's own
 * `?123` key ([KeyCode.NUMERIC]) is the one way onto the number layer.
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
 * Pure transition policy for the combined emoji / special-characters key and its surrounding "back"
 * affordances (L-03). D-531: the page chain is letters -(!?#)-> special-characters page -(?123)->
 * number layer -(ABC)-> letters; the D-19 swipe cycle visits the same pages in the same order, so
 * the gesture is simply the fast-forward of the key path.
 */
object PanelNavigation {
    
    /** D-531: the special-characters page - the combined key's tap destination from the letters page. */
    const val SPECIAL_KEYS_PAGE = 2
    
    /**
     * A tap on the combined key. D-531: from the letters page it opens the special-characters page
     * (the reform's re-scoping: the number layer is reserved for genuinely numeric fields, which
     * open it themselves, so the letters page's own key leads to the special characters instead) -
     * the `[InputSurface.EMOJI]` return value goes through the ordinary [InputSurface] switch, so
     * `AdaptKeyService`'s one [InputSurface] choke point (panel visibility, the D-472 return button,
     * and D-507's extra-row auto-open) handles every entry point identically. From a symbol page the
     * key does not exist (that slot carries the `?123`/`ABC` keys there); the letters fallthrough is
     * defensive only.
     * 
     * [CombinedKeyMode.OFF] never dispatches (the key is hidden then); it falls through to the ordinary
     * toggle purely so the function stays total.
     * 
     * @param current the currently shown surface
     * @param mode what the combined key's slot currently carries
     * @return the surface/page to switch to
     */
    fun onCombinedKeyTap(current: InputSurface, mode: CombinedKeyMode = CombinedKeyMode.SYMBOL): Page {
        if (mode == CombinedKeyMode.EMOJI) {
            return Page(InputSurface.EMOJI, 1)
        }
        return if (current == InputSurface.LETTERS) {
            Page(InputSurface.SYMBOLS, SPECIAL_KEYS_PAGE)
        } else {
            Page(InputSurface.LETTERS, 1)
        }
    }
    
    /**
     * A long-press or upward swipe on the combined key always switches straight to the numeric layer -
     * the number page (D-531: the calculator page the reform reserved for genuinely numeric input, and
     * the one page no key leads to any more, so the shortcut is its only direct route from letters).
     * 
     * @return the [InputSurface.SYMBOLS] page 1
     */
    fun onSwitchToSymbols(): Page = Page(InputSurface.SYMBOLS, 1)
    
    /**
     * A visible surface/page in the D-19 horizontal-swipe cycle. The emoji panel is deliberately not
     * part of the swipe cycle (it is reached by tapping the combined key in its emoji mode, or the
     * extra-row button).
     * 
     * @property surface the input surface to show
     * @property symbolPage the numeric/symbol page (only meaningful when [surface] is [InputSurface.SYMBOLS])
     */
    data class Page(val surface: InputSurface, val symbolPage: Int)
    
    // D-19/D-531: letters -> special-characters page (2) -> number layer (1), cycled by a full-field
    // horizontal swipe - the same chain the keys build (letters `!?#` -> page 2's `?123` -> page 1's
    // `ABC`), so a swipe is simply the fast-forward of the button path.
    private val SWIPE_CYCLE = listOf(
        Page(InputSurface.LETTERS, 1),
        Page(InputSurface.SYMBOLS, SPECIAL_KEYS_PAGE),
        Page(InputSurface.SYMBOLS, 1)
    )
    
    /**
     * The surface/page a D-19 horizontal swipe moves to. A right swipe ([forward] true) advances through
     * [letters → special-characters page → number layer] and wraps; a left swipe goes back. The emoji
     * surface is treated as the letter view (its own tap affordance leaves it), so a swipe from emoji
     * lands on the special-characters page.
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

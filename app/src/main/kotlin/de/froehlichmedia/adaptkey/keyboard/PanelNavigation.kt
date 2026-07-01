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
}

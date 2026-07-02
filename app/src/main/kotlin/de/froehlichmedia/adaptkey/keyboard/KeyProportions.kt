// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * Relative key-proportion configuration (C-01).
 *
 * Weights are relative within a row, so only their ratios matter. The defaults encode the
 * AdaptKey intent: L-02 (a narrower space bar with wider comma and full-stop keys, so an
 * accidental space instead of a comma becomes structurally less likely) and L-04 (a backspace
 * roughly 10 % wider than the Gboard default, with the extra width taken evenly from its
 * third-row letter neighbours so the row's total width is preserved). Every value is intended
 * to be user-adjustable from the settings screen.
 *
 * @property letterWeight base weight of an ordinary letter key
 * @property shiftWeight weight of the shift key in the third row
 * @property backspaceBaseWeight the Gboard-like backspace weight before the L-04 surcharge
 * @property backspaceExtra fractional surcharge applied to [backspaceBaseWeight] (L-04, ~0.10 = 10 %)
 * @property symbolWeight weight of the combined emoji / numeric-layer key (L-03)
 * @property enterWeight weight of the enter key
 * @property commaWeight weight of the comma key (L-02, wider than a letter)
 * @property periodWeight weight of the full-stop key (L-02, wider than a letter)
 * @property spaceWeight weight of the space bar (L-02, narrower than the Gboard default)
 */
data class KeyProportions(
    val letterWeight: Float = 1.0f,
    val shiftWeight: Float = 1.5f,
    val backspaceBaseWeight: Float = 1.5f,
    val backspaceExtra: Float = 0.10f,
    val symbolWeight: Float = 1.3f,
    val enterWeight: Float = 1.3f,
    val commaWeight: Float = 1.4f,
    val periodWeight: Float = 1.4f,
    val spaceWeight: Float = 3.2f
) {
    
    init {
        require(letterWeight > 0f) { "letterWeight must be > 0" }
        require(shiftWeight > 0f) { "shiftWeight must be > 0" }
        require(backspaceBaseWeight > 0f) { "backspaceBaseWeight must be > 0" }
        require(backspaceExtra >= 0f) { "backspaceExtra must be >= 0" }
        require(symbolWeight > 0f) { "symbolWeight must be > 0" }
        require(enterWeight > 0f) { "enterWeight must be > 0" }
        require(commaWeight > 0f) { "commaWeight must be > 0" }
        require(periodWeight > 0f) { "periodWeight must be > 0" }
        require(spaceWeight > 0f) { "spaceWeight must be > 0" }
    }
    
    /**
     * The final backspace weight after applying the L-04 surcharge.
     *
     * @return [backspaceBaseWeight] scaled by `(1 + backspaceExtra)`
     */
    val backspaceWeight: Float
        get() = backspaceBaseWeight * (1f + backspaceExtra)
    
    /**
     * Per-letter weight in the third row after the backspace's extra width has been taken evenly
     * from the [letterCount] letter keys (L-04), keeping the row's total width unchanged.
     *
     * @param letterCount the number of letter keys sharing the surcharge; must be >= 0
     * @return the reduced per-letter weight, or [letterWeight] when [letterCount] is 0
     */
    fun thirdRowLetterWeight(letterCount: Int): Float {
        require(letterCount >= 0) { "letterCount must be >= 0" }
        if (letterCount == 0) {
            return letterWeight
        }
        val surcharge = backspaceBaseWeight * backspaceExtra
        return letterWeight - surcharge / letterCount
    }
    
    companion object {
        
        /** The default, AdaptKey-tuned proportions. */
        val DEFAULT = KeyProportions()
    }
}

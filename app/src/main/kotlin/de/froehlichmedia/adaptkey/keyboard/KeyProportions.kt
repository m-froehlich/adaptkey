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
 * @property shiftBaseWeight the Gboard-like shift weight before the D-16 surcharge
 * @property shiftExtra fractional surcharge applied to [shiftBaseWeight] (D-16, mirrors the backspace one)
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
    val shiftBaseWeight: Float = 1.5f,
    val shiftExtra: Float = 0.0f,
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
        require(shiftBaseWeight > 0f) { "shiftBaseWeight must be > 0" }
        require(shiftExtra >= 0f) { "shiftExtra must be >= 0" }
        require(backspaceBaseWeight > 0f) { "backspaceBaseWeight must be > 0" }
        require(backspaceExtra >= 0f) { "backspaceExtra must be >= 0" }
        require(symbolWeight > 0f) { "symbolWeight must be > 0" }
        require(enterWeight > 0f) { "enterWeight must be > 0" }
        require(commaWeight > 0f) { "commaWeight must be > 0" }
        require(periodWeight > 0f) { "periodWeight must be > 0" }
        require(spaceWeight > 0f) { "spaceWeight must be > 0" }
    }
    
    /**
     * The final shift weight after applying the D-16 surcharge (mirrors [backspaceWeight]).
     *
     * @return [shiftBaseWeight] scaled by `(1 + shiftExtra)`
     */
    val shiftWeight: Float
        get() = shiftBaseWeight * (1f + shiftExtra)
    
    /**
     * The final backspace weight after applying the L-04 surcharge.
     *
     * @return [backspaceBaseWeight] scaled by `(1 + backspaceExtra)`
     */
    val backspaceWeight: Float
        get() = backspaceBaseWeight * (1f + backspaceExtra)
    
    /**
     * D-555 (variant A): the letters-page bottom row's funded weights while the dedicated emoji
     * key is shown - comma and full stop give up exactly their surplus above the letter weight,
     * splitting the emoji key's own weight between them in proportion to that surplus. A key is
     * never pushed below its own configured value, never below the floor, and never widened; a
     * key already at (or below) the floor contributes nothing. When both keys together carry
     * less surplus than [BOTTOM_ROW_EMOJI_KEY_WEIGHT], the emoji key keeps its full weight and
     * the residual is left to the row's own total - every key of the row then shrinks by the
     * same tiny factor at draw time, the space bar never specifically touched.
     *
     * Deliberately a pure build-time overlay: the C-01 comma/period slider values themselves are
     * never rewritten (the D-301 lesson - no derived value ever becomes a second stored source
     * of truth), so switching the emoji key off rebuilds the row exactly as configured.
     *
     * @return the funded (comma, emoji key, period) weights
     */
    fun fundedBottomRow(): FundedBottomRow {
        val emojiWeight = BOTTOM_ROW_EMOJI_KEY_WEIGHT
        val surplusComma = (commaWeight - letterWeight).coerceAtLeast(0f)
        val surplusPeriod = (periodWeight - letterWeight).coerceAtLeast(0f)
        val surplus = surplusComma + surplusPeriod
        if (surplus <= 0f) {
            return FundedBottomRow(commaWeight, emojiWeight, periodWeight)
        }
        val funded = minOf(emojiWeight, surplus)
        return FundedBottomRow(
            commaWeight = commaWeight - surplusComma / surplus * funded,
            emojiWeight = emojiWeight,
            periodWeight = periodWeight - surplusPeriod / surplus * funded
        )
    }
    
    /**
     * Per-letter weight in the third row after the backspace's (L-04) and shift's (D-16) extra width has
     * been taken evenly from the [letterCount] letter keys, keeping the row's total width unchanged.
     *
     * @param letterCount the number of letter keys sharing the surcharge; must be >= 0
     * @return the reduced per-letter weight, or [letterWeight] when [letterCount] is 0
     */
    fun thirdRowLetterWeight(letterCount: Int): Float {
        require(letterCount >= 0) { "letterCount must be >= 0" }
        if (letterCount == 0) {
            return letterWeight
        }
        val surcharge = backspaceBaseWeight * backspaceExtra + shiftBaseWeight * shiftExtra
        return letterWeight - surcharge / letterCount
    }
    
    companion object {
        
        /** The default, AdaptKey-tuned proportions. */
        val DEFAULT = KeyProportions()
        
        /**
         * D-555: the dedicated bottom-row emoji key's weight, funded from the L-02 widening of
         * comma and full stop (see [fundedBottomRow]). A considered starting point (~60 % of a
         * letter key), not yet device-tuned - a single constant, easy to retune.
         */
        const val BOTTOM_ROW_EMOJI_KEY_WEIGHT = 0.6f
    }
}

/**
 * D-555: the funded (comma, emoji key, period) weights of the letters-page bottom row while the
 * dedicated emoji key is shown - see [KeyProportions.fundedBottomRow] for the rule (variant A)
 * and its invariants.
 *
 * @property commaWeight the comma key's funded weight
 * @property emojiWeight the emoji key's own weight (the constant [KeyProportions.BOTTOM_ROW_EMOJI_KEY_WEIGHT])
 * @property periodWeight the full-stop key's funded weight
 */
data class FundedBottomRow(
    val commaWeight: Float,
    val emojiWeight: Float,
    val periodWeight: Float
)

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import kotlin.math.abs

/**
 * D-68: derives sensible initial per-key touch zones (T-03) directly from an explicitly chosen typing
 * pattern, instead of guessing it from a short typing sample (the old T-04 auto-detection).
 *
 * The shape follows the pattern's physical reach: for a one-sided finger/thumb pattern, keys near that
 * hand's "home" third stay tight and precise; the zone widens and grows less certain the further a key is
 * from home, deliberately smearing into the neighbouring key's territory at the far edge. Thumb patterns
 * start less precise even at home (short lever, awkward angle) and widen further still, with extra
 * widening the closer a key is to the top row - a thumb's hardest reach is the far top corner, not just
 * sideways. Two-thumb typing has no left/right skew at all: a flat, moderately wide zone on every key.
 *
 * Only the systematic-offset *direction* is left at zero: whether a user tends to undershoot or overshoot
 * a far key is a real, personal habit this module has no evidence for, so it is left for real usage to
 * discover (the model keeps learning during normal typing, see [OffsetModel.record]). Widening the zone
 * (the *spread*), by contrast, is a low-risk, well-grounded prior directly from the pattern's geometry.
 */
object PatternSeed {
    
    /**
     * Seeds initial per-key statistics for [pattern] from [geometry] (typically
     * `AdaptKeyboardView.charKeyGeometry()`). [TypingPattern.UNKNOWN] seeds nothing, so an explicit
     * "not chosen" state leaves any existing model untouched.
     *
     * @param pattern the user's explicitly chosen typing pattern
     * @param geometry every char key's geometry (centre and half-size)
     * @return per-key seed statistics, ready for [OffsetModel.restore]; empty for [TypingPattern.UNKNOWN]
     *   or empty [geometry]
     */
    fun seed(pattern: TypingPattern, geometry: List<OffsetModel.Candidate>): Map<String, OffsetModel.Stat> {
        if (pattern == TypingPattern.UNKNOWN || geometry.isEmpty()) {
            return emptyMap()
        }
        val keyboardWidth = (geometry.maxOf { it.centerX + it.halfWidth } - geometry.minOf { it.centerX - it.halfWidth }).toDouble()
        val minY = geometry.minOf { it.centerY }
        val maxY = geometry.maxOf { it.centerY }
        val result = HashMap<String, OffsetModel.Stat>()
        for (c in geometry) {
            val rowFraction = if (maxY > minY) ((c.centerY - minY) / (maxY - minY)).toDouble() else 0.0
            val (stdDevX, stdDevY) = shapeFor(pattern, c, keyboardWidth, rowFraction)
            result[c.id] = OffsetModel.Stat(
                count = SEED_COUNT,
                meanDx = 0.0,
                meanDy = 0.0,
                m2Dx = stdDevX * stdDevX * (SEED_COUNT - 1L),
                m2Dy = stdDevY * stdDevY * (SEED_COUNT - 1L)
            )
        }
        return result
    }
    
    private fun shapeFor(pattern: TypingPattern, c: OffsetModel.Candidate, keyboardWidth: Double, rowFraction: Double): Pair<Double, Double> {
        if (pattern == TypingPattern.TWO_THUMBS) {
            return (TWO_THUMBS_STD_X * c.halfWidth) to (TWO_THUMBS_STD_Y * c.halfHeight)
        }
        val isThumb = pattern == TypingPattern.LEFT_THUMB || pattern == TypingPattern.RIGHT_THUMB
        val homeFraction = when (pattern) {
            TypingPattern.LEFT_INDEX_FINGER -> INDEX_HOME_FRACTION
            TypingPattern.RIGHT_INDEX_FINGER -> 1.0 - INDEX_HOME_FRACTION
            TypingPattern.LEFT_THUMB -> THUMB_HOME_FRACTION
            TypingPattern.RIGHT_THUMB -> 1.0 - THUMB_HOME_FRACTION
            else -> 0.5
        }
        val homeX = homeFraction * keyboardWidth
        val reach = (abs(c.centerX - homeX) / (keyboardWidth / 2.0)).coerceIn(0.0, 1.0)
        val baseX = if (isThumb) THUMB_BASE_STD else INDEX_BASE_STD
        val gainX = if (isThumb) THUMB_REACH_GAIN else INDEX_REACH_GAIN
        var stdDevXFactor = baseX + gainX * reach
        var stdDevYFactor = if (isThumb) THUMB_BASE_VERT_STD else INDEX_BASE_VERT_STD
        if (isThumb) {
            // Thumb top-row refinement: a thumb struggles more with the far/top row than with pure sideways
            // reach, so widen both axes further the closer a key is to the top row (1 = top row, 0 = bottom
            // letter row).
            val topRowPenalty = 1.0 - rowFraction
            stdDevYFactor += THUMB_TOP_ROW_VERT_GAIN * topRowPenalty
            stdDevXFactor += THUMB_TOP_ROW_HORIZ_GAIN * topRowPenalty
        }
        return (stdDevXFactor * c.halfWidth) to (stdDevYFactor * c.halfHeight)
    }
    
    // Seed sample count - just above OffsetModel.DEFAULT_WARMUP_SAMPLES (20), so resolve() uses the seeded
    // likelihood immediately, but a user's first few dozen real taps can still meaningfully pull it towards
    // their own behaviour.
    private const val SEED_COUNT = 25L
    
    // Normalised home position (fraction of keyboard width) for a left-hand pattern; the right-hand pattern
    // mirrors it as (1 - fraction).
    private const val INDEX_HOME_FRACTION = 1.0 / 6.0
    private const val THUMB_HOME_FRACTION = 0.20
    
    // Horizontal spread as a fraction of a key's half-width: base (at home) + gain * reach (0..1).
    private const val INDEX_BASE_STD = 0.35
    private const val INDEX_REACH_GAIN = 0.9
    private const val THUMB_BASE_STD = 0.55
    private const val THUMB_REACH_GAIN = 1.1
    
    // Vertical spread as a fraction of a key's half-height, flat (no reach dependency) for fingers.
    private const val INDEX_BASE_VERT_STD = 0.45
    private const val THUMB_BASE_VERT_STD = 0.5
    
    // Thumb-only extra widening the closer a key is to the top row.
    private const val THUMB_TOP_ROW_VERT_GAIN = 0.6
    private const val THUMB_TOP_ROW_HORIZ_GAIN = 0.3
    
    // Two-thumb typing: flat, moderately wide, no positional skew at all.
    private const val TWO_THUMBS_STD_X = 0.65
    private const val TWO_THUMBS_STD_Y = 0.55
}

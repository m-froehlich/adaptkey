// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-593/D-594: the space bar's language-pack update marker - the amber full fill plus the two-line
 * centred label (the language endonym raised, a localized "update available" line beneath it). Pure
 * policy and geometry, kept out of [AdaptKeyboardView] so both are unit-testable without an Android
 * dependency, the same way [SpaceSwipeHints] is.
 *
 * The marker answers a "must not be forgotten" gap: a stale language pack quietly degrades every
 * correction the keyboard makes, but the only existing "update available" hint lives in the language
 * packs screen, seen by whoever happens to open it. The marker therefore shows exactly where the
 * stale pack is the *active* one, and nowhere else:
 *
 * - It sits on the language label's own space key only - the letters surface's space bar. The symbol
 *   pages' smaller space keys, and the narrower URL-mode/email-mode space keys, show no language of
 *   their own (D-097), so an update hint there would dangle without its context.
 * - It follows the language label's own visibility: until the service has pushed a label, the space
 *   key is not in its language-labelled state at all.
 * - Whether an update actually exists is the service's own push ([AdaptKeyboardView.spaceUpdateAvailable],
 *   the exact `LanguagePacksActivity.buildRow()` predicate incl. D-334 suppression) - this object only
 *   decides *where* such a signal may be drawn.
 */
object SpaceUpdateMarker {
    
    /** The "update available" line's own text size in dp - the corner hint glyphs' 11dp and the swipe
     *  chevrons' 14dp bracket it; a secondary line must read at a glance without competing with the
     *  endonym above it. */
    const val SUB_TEXT_SIZE_DP = 12f
    
    /** The vertical gap between the endonym line and the "update available" line, in dp. */
    const val LINE_GAP_DP = 2.5f
    
    /** How much of the space key's own height the two-line block may occupy at most - above that
     *  (very short keys, landscape) the marker draws as fill + endonym only, never a clipped line. */
    private const val MAX_BLOCK_HEIGHT_FRACTION = 0.82f
    
    /**
     * @param surface the currently shown input surface
     * @param urlMode true while a URL-mode field is focused (the space key shows no language label)
     * @param emailMode true while an email-mode field is focused (same reasoning as [urlMode])
     * @param languageLabelShown true when the space key currently carries the language label (the service
     *        has pushed a non-empty [AdaptKeyboardView.spaceLabel])
     * @param updateAvailable true when the service pushed [AdaptKeyboardView.spaceUpdateAvailable]
     * @return whether the amber update marker belongs on the space key right now
     */
    fun visible(
        surface: InputSurface,
        urlMode: Boolean,
        emailMode: Boolean,
        languageLabelShown: Boolean,
        updateAvailable: Boolean
    ): Boolean {
        return updateAvailable &&
            languageLabelShown &&
            surface == InputSurface.LETTERS &&
            !urlMode &&
            !emailMode
    }
    
    /**
     * The two baselines that centre the label block on the key: the endonym raised above centre, the
     * "update available" line below it, the block as a whole vertically centred (the Nutzer-Call of
     * 2026-11-28 - the two lines must not look "pushed down").
     *
     * Ascent values follow [android.graphics.Paint.FontMetrics] conventions: ascent is negative,
     * descent positive, so each line's own height is `-ascent + descent`. Pure arithmetic over the
     * caller's metrics, no Paint dependency.
     *
     * @param centerY the space key's vertical centre, in px
     * @param mainAscent the label paint's ascent (negative) for the endonym line
     * @param mainDescent the label paint's descent (positive) for the endonym line
     * @param subAscent the marker line paint's ascent (negative)
     * @param subDescent the marker line paint's descent (positive)
     * @param lineGap the vertical gap between the two lines, in px ([LINE_GAP_DP] scaled)
     * @return both baselines, in px, ready for `Canvas.drawText`
     */
    fun twoLineBaselines(
        centerY: Float,
        mainAscent: Float,
        mainDescent: Float,
        subAscent: Float,
        subDescent: Float,
        lineGap: Float
    ): TwoLineBaselines {
        val mainHeight = -mainAscent + mainDescent
        val subHeight = -subAscent + subDescent
        val blockHeight = mainHeight + lineGap + subHeight
        val top = centerY - blockHeight / 2f
        return TwoLineBaselines(
            mainBaseline = top - mainAscent,
            subBaseline = top + mainHeight + lineGap - subAscent
        )
    }
    
    /**
     * Whether the space key is tall enough for the two-line block at all - [twoLineBaselines]' block
     * (its own heights plus [lineGap]) must fit inside [MAX_BLOCK_HEIGHT_FRACTION] of the key height.
     * When this returns false the marker draws as fill + endonym at the ordinary centred baseline,
     * never as a clipped second line.
     *
     * @param keyHeightPx the space key's own height, in px
     * @param mainAscentPx the label paint's ascent (negative), in px
     * @param mainDescentPx the label paint's descent (positive), in px
     * @param subAscentPx the marker line paint's ascent (negative), in px
     * @param subDescentPx the marker line paint's descent (positive), in px
     * @param lineGapPx the vertical gap between the two lines, in px
     * @return whether both lines fit
     */
    fun fits(
        keyHeightPx: Float,
        mainAscentPx: Float,
        mainDescentPx: Float,
        subAscentPx: Float,
        subDescentPx: Float,
        lineGapPx: Float
    ): Boolean {
        val blockHeight = (-mainAscentPx + mainDescentPx) + lineGapPx + (-subAscentPx + subDescentPx)
        return blockHeight <= keyHeightPx * MAX_BLOCK_HEIGHT_FRACTION
    }
    
    /** Both baselines [twoLineBaselines] computes, named for the two `drawText` call sites. */
    data class TwoLineBaselines(val mainBaseline: Float, val subBaseline: Float)
}

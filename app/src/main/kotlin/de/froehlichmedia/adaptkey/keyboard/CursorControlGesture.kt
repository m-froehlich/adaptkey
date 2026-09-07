// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import kotlin.math.roundToInt

/**
 * Pure timing/geometry policy for the D-401 space-bar cursor/selection-control gesture: long-pressing the
 * space bar arms a mode where dragging moves the text cursor (Stage 1), and holding still for
 * [HOLD_STILL_TO_SELECT_MS] switches to extending a text selection instead (Stage 2). Mirrors
 * [BackspaceRepeat]'s own split - this object holds only stateless math; the running "how far has the
 * finger travelled since the origin" and "which steps have already been applied" state lives on the
 * Android side ([de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView]), which owns the actual
 * [android.os.Handler] timers this needs (the 800 ms stillness check, the 1000 ms re-touch grace window).
 *
 * Character movement is computed directly from drag distance (used to call
 * [android.view.inputmethod.InputConnection.setSelection] at an absolute offset). Line movement is
 * deliberately *not* computed the same way - this app has no reliable way to know a target field's real
 * line height/wrap positions (the exact reliability gap already named for [android.view.inputmethod.
 * CursorAnchorInfo] elsewhere in this project), so a line step is meant to be applied as a synthetic
 * `KEYCODE_DPAD_UP`/`KEYCODE_DPAD_DOWN` key event instead, letting the target app's own text layout decide
 * what "one line up" actually means.
 */
object CursorControlGesture {
    
    /** Stage 1 switches to Stage 2 once the finger has held still for this long (D-401). */
    const val HOLD_STILL_TO_SELECT_MS = 800L
    
    /** After lifting the finger, the mode stays armed this long, waiting for a re-touch (D-401). */
    const val LIFT_GRACE_MS = 1000L
    
    /** The crosshair's own fade back to the space key's centre once the grace window above begins (D-401). */
    const val CROSSHAIR_FADE_MS = 250L
    
    /** Every key's opacity while the gesture is active (D-401). */
    const val DIMMED_KEY_ALPHA = 0.30f
    
    /**
     * Drag distance per character step (D-401) - a first-pass calibration carried over from Gboard's own
     * spacebar cursor glide, not yet tuned against this app's own device testing (the user's own explicit
     * starting point: "erstmal von GBoard übernehmen"). Expect this to change once tried for real.
     */
    const val DP_PER_CHARACTER_STEP = 12f
    
    /** Drag distance per line step (D-401) - same calibration status as [DP_PER_CHARACTER_STEP]. */
    const val DP_PER_LINE_STEP = 32f
    
    /** Which half of the gesture is currently active. */
    enum class Stage {
        /** Dragging moves the caret; holding still for [HOLD_STILL_TO_SELECT_MS] promotes to [SELECTION]. */
        CURSOR,
        
        /** Dragging extends a text selection from wherever [CURSOR] left the caret. */
        SELECTION
    }
    
    /** The total character/line offset [stepsFor] resolves a drag distance to. */
    data class Steps(val characters: Int, val lines: Int)
    
    /**
     * The total character/line steps implied by [dx]/[dy] - both measured from the gesture's own origin,
     * never a delta from a previous call. The caller owns its own "already applied" counters and derives
     * the actual delta to apply from two successive results, so a step is only ever applied once each
     * time the drag distance genuinely crosses its own next threshold.
     *
     * @param dx horizontal distance from the origin, in raw pixels
     * @param dy vertical distance from the origin, in raw pixels
     * @param density the display's own density scale ([android.util.DisplayMetrics.density]), converting
     *        the dp constants above into pixels
     * @return the total character/line steps [dx]/[dy] resolve to
     */
    fun stepsFor(dx: Float, dy: Float, density: Float): Steps {
        val charStepPx = DP_PER_CHARACTER_STEP * density
        val lineStepPx = DP_PER_LINE_STEP * density
        return Steps(
            characters = (dx / charStepPx).roundToInt(),
            lines = (dy / lineStepPx).roundToInt()
        )
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure timing/geometry policy for the D-401 space-bar cursor/selection-control gesture: long-pressing the
 * space bar arms a mode where dragging moves the text cursor (Stage 1), and holding still for
 * [HOLD_STILL_TO_SELECT_MS] switches to extending a text selection instead (Stage 2). Mirrors
 * [BackspaceRepeat]'s own split - this object holds only stateless math; the running "how far has the
 * finger travelled since the origin" and "which steps have already been applied" state lives on the
 * Android side ([de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView]/[de.froehlichmedia.adaptkey.
 * AdaptKeyService]), which own the actual [android.os.Handler] timers and [android.view.inputmethod.
 * InputConnection] calls this needs.
 *
 * D-401-followup: rearchitected around the user's own explicit correction after several rounds of
 * incremental-stepping bugs - this gesture positions the caret *directly and absolutely*, not by moving it
 * through the document's own text flow. Dragging right always means "as far right as this line allows,
 * however far that is" - [characters] is the *total* signed offset from the gesture's own origin, always
 * re-clamped fresh to whichever line is currently active, never accumulated as a running delta. A line
 * change is a separate, deliberate action that only ever happens from motion that is genuinely more
 * vertical than horizontal - see [stepsFor]'s own dominant-axis gate - so a rightward drag can never, by
 * itself, also change which line the caret is on, regardless of how much incidental vertical drift a long
 * horizontal drag naturally accumulates.
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
    
    /**
     * Drag distance per line step (D-401) - same starting-point status as [DP_PER_CHARACTER_STEP]. Raised
     * from an original 32f during earlier tuning; kept at 200f here even after the D-401-followup
     * rearchitecture below, since [stepsFor]'s own dominant-axis gate (not this constant alone) is what now
     * actually prevents an unintended line change during a mostly-horizontal drag - this constant still
     * needs to be large enough that a *genuinely* vertical drag does not register many lines' worth of
     * change from a small amount of intended motion.
     */
    const val DP_PER_LINE_STEP = 200f
    
    /** Which half of the gesture is currently active. */
    enum class Stage {
        /** Dragging moves the caret; holding still for [HOLD_STILL_TO_SELECT_MS] promotes to [SELECTION]. */
        CURSOR,
        
        /** Dragging extends a text selection from wherever [CURSOR] left the caret. */
        SELECTION
    }
    
    /** The total character/line offset [stepsFor] resolves a drag distance to, both from the gesture's own origin. */
    data class Steps(val characters: Int, val lines: Int)
    
    /**
     * The total character/line steps implied by [dx]/[dy], both measured from the gesture's own origin
     * (never a delta from a previous call - the caller re-derives whatever incremental work it needs from
     * two successive results, or - for [characters] specifically, since D-401-followup - applies the total
     * directly and absolutely each time, per this object's own class KDoc).
     *
     * D-401-followup: [lines] is forced to zero whenever [dx] is at least as large as [dy] in magnitude -
     * a real device log showed a line step firing from vertical drift that was small in absolute terms but
     * still large enough to cross [DP_PER_LINE_STEP]'s own threshold, in the middle of a drag that was
     * overwhelmingly horizontal (514px sideways against 42px of drift) - hand wobble that piles up over any
     * long horizontal drag, not deliberate vertical intent. Requiring the vertical distance to actually
     * *dominate* the horizontal one, on top of [DP_PER_LINE_STEP]'s own absolute threshold, ties a line
     * change to motion that is recognisably more "up/down" than "left/right" at the moment it fires -
     * matching the user's own explicit model: moving the finger right only ever repositions the caret
     * within the current line, never as a side effect changes which line it is on.
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
        val lines = if (abs(dy) > abs(dx)) (dy / lineStepPx).roundToInt() else 0
        return Steps(
            characters = (dx / charStepPx).roundToInt(),
            lines = lines
        )
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.TapPoint
import kotlin.math.abs

/**
 * D-377: recovers a badly garbled token where a Backspace was missed and a neighbouring key was hit instead
 * (e.g. `"welxmche"` for `"welche"` - an accidental `x` for `c`, followed by a Backspace attempt that landed
 * on `m` instead). Unlike a generic fuzzy-dictionary widen (considered and rejected - see AdaptKey-Progress.md
 * §367's own KDoc for the false-positive-risk discussion), this is deliberately evidence-gated at the input
 * level, mirroring [RawCoordinateCorrection]'s own "consult the real tap, not just the dictionary" reasoning:
 * a position is only ever considered when the actual raw tap that produced it geometrically supports the
 * "meant Backspace" story, not merely because deleting some characters happens to spell a known word.
 *
 * Only the character *at* the near-Backspace tap and its immediate predecessor are ever removed together
 * (mirroring what a real Backspace press would have deleted right there) - this is not a general deletion
 * search over arbitrary positions.
 */
object MissedBackspaceRecovery {
    
    /**
     * D-377: "a distance of one key" (the user's own calibration) - the key's own bounds expanded by exactly
     * one more of its own half-width/half-height in each direction, so an immediately-adjacent key's centre
     * (which tiles roughly `2 * halfWidth`/`2 * halfHeight` away) still counts, but nothing further does.
     * Resolution-independent by construction (scales with the key's own measured size), matching
     * [de.froehlichmedia.adaptkey.touch.AmbiguityBands]' own band-relative-to-key-size philosophy rather than
     * a fixed pixel radius.
     */
    private const val PROXIMITY_KEY_MULTIPLE = 2f
    
    /**
     * @param token the composing token as typed (original case preserved in the output)
     * @param taps the raw `ACTION_DOWN` tap for each character, same order and length as [token]; a length
     *        mismatch (e.g. a desync after an edit) yields no candidates rather than risk a wrong removal
     * @param backspace Backspace's own current on-screen geometry
     * @return candidate shorter tokens, one per tap position that geometrically looks like a missed Backspace
     *         (position 0 is never a candidate - there is no predecessor to collapse it with); the caller is
     *         expected to test each against the dictionary and use the first known word
     */
    fun recover(token: String, taps: List<TapPoint>, backspace: OffsetModel.Candidate): List<String> {
        if (token.length < 2 || token.length != taps.size) {
            return emptyList()
        }
        val candidates = ArrayList<String>()
        for (i in 1 until token.length) {
            if (isNearBackspace(taps[i], backspace)) {
                candidates += token.removeRange(i - 1, i + 1)
            }
        }
        return candidates
    }
    
    private fun isNearBackspace(tap: TapPoint, backspace: OffsetModel.Candidate): Boolean {
        return abs(tap.x - backspace.centerX) <= backspace.halfWidth * PROXIMITY_KEY_MULTIPLE &&
            abs(tap.y - backspace.centerY) <= backspace.halfHeight * PROXIMITY_KEY_MULTIPLE
    }
}

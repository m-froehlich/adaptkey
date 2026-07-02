// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import kotlin.math.abs

/**
 * Pure swipe classifier for the keyboard gestures (§4). It maps a raw displacement between the
 * initial contact point (T-01) and the release point onto a [SwipeDirection].
 *
 * A movement only counts as a swipe when its dominant axis travels at least [minDistancePx];
 * otherwise the result is [SwipeDirection.NONE] and the touch is left to behave as a tap. The
 * dominant axis decides the orientation, so a diagonal drag resolves to whichever of the
 * horizontal / vertical components is larger.
 */
object SwipeGesture {
    
    /**
     * Classifies a displacement into a swipe direction.
     *
     * @param dx horizontal displacement (release minus down), positive to the right
     * @param dy vertical displacement (release minus down), positive downward
     * @param minDistancePx the minimum travel on the dominant axis required to count as a swipe
     * @return the recognised direction, or [SwipeDirection.NONE] when below the threshold
     * @throws IllegalArgumentException if [minDistancePx] is negative
     */
    fun classify(dx: Float, dy: Float, minDistancePx: Float): SwipeDirection {
        require(minDistancePx >= 0f) { "minDistancePx must not be negative: $minDistancePx" }
        val absX = abs(dx)
        val absY = abs(dy)
        if (absX < minDistancePx && absY < minDistancePx) {
            return SwipeDirection.NONE
        }
        return if (absX >= absY) {
            if (dx < 0f) SwipeDirection.LEFT else SwipeDirection.RIGHT
        } else {
            if (dy < 0f) SwipeDirection.UP else SwipeDirection.DOWN
        }
    }
}

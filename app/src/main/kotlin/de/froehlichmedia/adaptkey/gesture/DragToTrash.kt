// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import kotlin.math.abs

/**
 * Pure decision logic for the G-04 drag-to-trash gesture: dragging a suggestion chip upward into a
 * trash area to blacklist it (A-04).
 *
 * The gesture is deliberately vertical to avoid clashing with the horizontal scroll of the
 * suggestion bar and with a plain tap: it counts only once the upward travel exceeds
 * [activateThresholdPx] and the upward component dominates the horizontal one. The same predicate
 * decides both whether the trash affordance is shown during the drag and whether a release commits
 * the blacklisting.
 */
object DragToTrash {
    
    /**
     * Whether the current displacement qualifies as an upward trash drag.
     *
     * @param dx horizontal displacement from the touch-down point (positive to the right)
     * @param dy vertical displacement from the touch-down point (positive downward, so an upward
     *        drag is negative)
     * @param activateThresholdPx the minimum upward travel required to arm the gesture
     * @return true when the drag is upward past the threshold and the vertical axis dominates
     * @throws IllegalArgumentException if [activateThresholdPx] is negative
     */
    fun isArmed(dx: Float, dy: Float, activateThresholdPx: Float): Boolean {
        require(activateThresholdPx >= 0f) { "activateThresholdPx must not be negative: $activateThresholdPx" }
        val up = -dy
        return up >= activateThresholdPx && up >= abs(dx)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import kotlin.math.pow

/**
 * Pure timing/policy for the accelerating backspace-on-hold behaviour (D-07).
 *
 * Holding the backspace key starts repeating after a short initial delay ([INITIAL_DELAY_MS]); the
 * repeat interval starts slow ([START_DELAY_MS]) and accelerates geometrically towards a floor
 * ([MIN_DELAY_MS]), so the user is not surprised into deleting too much. Once roughly three words'
 * worth of characters have been removed ([WORD_MODE_AFTER_CHARS]) the deletion switches from
 * character-wise to word-wise until the key is released.
 *
 * This class only computes the schedule and the deletion unit; the view drives the timer and the
 * service performs the actual deletion, so the whole policy stays Android-free and unit-testable.
 */
object BackspaceRepeat {
    
    /** Delay before the first repeat fires, giving a normal tap room to be just a single deletion. */
    const val INITIAL_DELAY_MS = 400L
    
    /** Interval before the first accelerated repeat; the curve decays from here. */
    const val START_DELAY_MS = 200L
    
    /** Fastest repeat interval the acceleration is clamped to. */
    const val MIN_DELAY_MS = 45L
    
    /** Geometric decay factor applied per repeat step (smaller = accelerates faster). */
    const val ACCELERATION = 0.82
    
    /** Characters removed during a hold before deletion switches from character-wise to word-wise. */
    const val WORD_MODE_AFTER_CHARS = 18
    
    /**
     * The delay before the repeat at [step] should fire (0-based, where step 0 is the first repeat
     * after [INITIAL_DELAY_MS]). Starts at [START_DELAY_MS] and decays geometrically towards
     * [MIN_DELAY_MS], which it never goes below.
     *
     * @param step the 0-based repeat index
     * @return the delay in milliseconds until this repeat fires
     * @throws IllegalArgumentException when [step] is negative
     */
    fun nextDelayMs(step: Int): Long {
        require(step >= 0) { "step must be >= 0" }
        val decayed = (START_DELAY_MS * ACCELERATION.pow(step)).toLong()
        return decayed.coerceAtLeast(MIN_DELAY_MS)
    }
    
    /**
     * Whether the deletion at this point in the hold should remove a whole word rather than a single
     * character (D-07): true once [charsDeleted] has reached [WORD_MODE_AFTER_CHARS].
     *
     * @param charsDeleted the number of characters already removed during the current hold
     * @return true when the next deletion should be word-wise
     */
    fun deletesWord(charsDeleted: Int): Boolean {
        return charsDeleted >= WORD_MODE_AFTER_CHARS
    }
}

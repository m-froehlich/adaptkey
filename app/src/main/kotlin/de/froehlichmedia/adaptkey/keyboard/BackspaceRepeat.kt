// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import kotlin.math.pow

/**
 * Pure timing/policy for the accelerating backspace-on-hold behaviour (D-07 / D-31).
 *
 * Holding backspace starts repeating after [INITIAL_DELAY_MS]. Character-wise deletion starts at
 * [CHAR_START_DELAY_MS] and accelerates only **moderately** ([CHAR_ACCELERATION]) towards a floor
 * ([CHAR_MIN_DELAY_MS]), so it never runs away. Once roughly [WORD_MODE_AFTER_CHARS] characters have gone
 * it switches to **word-wise** deletion at a clearly **slower**, fixed cadence ([WORD_DELAY_MS]) - so the
 * jump into word deletion is a visible slow-down the user can follow and stop at the right moment.
 *
 * The delay is a function of how many characters have already been removed, so the service (which knows
 * that count and drives the deletion) can return the next delay each tick; the view only owns the timer.
 * The whole policy stays Android-free and unit-testable.
 */
object BackspaceRepeat {
    
    /** Delay before the first repeat fires, giving a normal tap room to be just a single deletion. */
    const val INITIAL_DELAY_MS = 400L
    
    /** Character-wise starting interval; the moderate acceleration decays from here. */
    const val CHAR_START_DELAY_MS = 190L
    
    /** Fastest character-wise interval the acceleration is clamped to (kept moderate on purpose). */
    const val CHAR_MIN_DELAY_MS = 65L
    
    /** Geometric decay factor per removed character - close to 1 so acceleration stays gentle (D-31). */
    const val CHAR_ACCELERATION = 0.90
    
    /** Characters removed during a hold before deletion switches from character-wise to word-wise. */
    const val WORD_MODE_AFTER_CHARS = 16
    
    /** Word-wise deletion cadence: clearly slower than character-wise, so it is followable (D-31). */
    const val WORD_DELAY_MS = 330L
    
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
    
    /**
     * The delay before the next deletion, given how many characters have been removed so far: the moderate
     * character-wise decay while below the word threshold, and the slower fixed word-wise cadence at/after
     * it (D-31).
     *
     * @param charsDeleted the number of characters removed so far during the hold
     * @return the delay in milliseconds until the next deletion
     * @throws IllegalArgumentException when [charsDeleted] is negative
     */
    fun nextDelayMs(charsDeleted: Int): Long {
        require(charsDeleted >= 0) { "charsDeleted must be >= 0" }
        if (deletesWord(charsDeleted)) {
            return WORD_DELAY_MS
        }
        val decayed = (CHAR_START_DELAY_MS * CHAR_ACCELERATION.pow(charsDeleted)).toLong()
        return decayed.coerceAtLeast(CHAR_MIN_DELAY_MS)
    }
}

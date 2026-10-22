// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.dictionary.BlacklistOrigin

/**
 * D-488: one armed request to blacklist [word] - what a chip tap would write, and what rejecting or
 * ignoring it has to clean up. Built by the service at the three arming sites (a G-04 drag of a bundled
 * word, the W-03 "Verbieten" deep zone, and the D-177 recurrence escalation); carried by
 * [BlacklistConfirmation] until the user resolves it.
 *
 * @property word the word the chip names
 * @property origin the [BlacklistOrigin] a confirmed write carries (fixed at arming time - the trigger
 *           is known then, not at tap time)
 * @property clearsPendingMark true only for the escalation shape: rejecting or ignoring must also clear
 *           the D-177 pending mark, so the chip never re-fires on every subsequent typing of the word
 */
data class BlacklistConfirmationRequest(
    val word: String,
    val origin: BlacklistOrigin,
    val clearsPendingMark: Boolean
)

/** What one resolution step decided. The service maps this onto store writes and chip removal. */
enum class BlacklistConfirmationOutcome {
    
    /** Nothing to do - e.g. a commit that merely counts toward the yield threshold. */
    NONE,
    
    /** The chip was tapped: write the blacklist entry with the request's own origin. */
    CONFIRMED,
    
    /** The chip was rejected or ignored (drag-down, field change, or the two-commit yield): never
     * write; clear the pending mark when the request says so. */
    REJECTED
}

/**
 * The result of one [BlacklistConfirmation] resolution step: the outcome plus the request it consumed
 * (null when nothing was consumed and the caller has nothing to settle).
 */
data class BlacklistConfirmationResult(
    val outcome: BlacklistConfirmationOutcome,
    val request: BlacklistConfirmationRequest?
)

/**
 * D-488: the pure state machine behind the blacklist confirmation chip. Holds at most one armed
 * request; the chip pins the suggestion bar (full takeover) until it is resolved one of three ways:
 *
 * - **Tap = confirm** ([confirm]) - the only path that ever writes the blacklist entry.
 * - **Drag down to the keyboard = reject** ([reject]) - and a field change rejects silently, the same
 *   safe default.
 * - **Ignore = reject** ([onCommit]): the keyboard keeps working while the chip is up, and each
 *   subsequently committed word counts; the second one yields the bar. Ignoring therefore never
 *   blacklists - blacklisting is a rare action and must happen deliberately (user's own framing).
 *
 * Free of any Android dependency so it can be unit-tested on the JVM; the service owns rendering and
 * the actual store writes.
 */
class BlacklistConfirmation {
    
    /** The currently armed request, or null while no chip is up. */
    var pending: BlacklistConfirmationRequest? = null
        private set
    
    private var commitsSinceArmed = 0
    
    /**
     * Arms [request], unless a request is already pending - the first one wins (a word committed again
     * while its own chip is up re-enters the escalation branch and must not reset the yield counter).
     *
     * @return true when [request] was armed, false when an earlier request is still pending
     */
    fun arm(request: BlacklistConfirmationRequest): Boolean {
        if (pending != null) {
            return false
        }
        pending = request
        commitsSinceArmed = 0
        return true
    }
    
    /**
     * One subsequently committed word while the chip is up. The first commit keeps the chip up (the
     * user may simply be finishing the sentence they are in); the second yields the bar as an ignore.
     */
    fun onCommit(): BlacklistConfirmationResult {
        val request = pending ?: return BlacklistConfirmationResult(BlacklistConfirmationOutcome.NONE, null)
        commitsSinceArmed++
        if (commitsSinceArmed < YIELD_AFTER_COMMITS) {
            return BlacklistConfirmationResult(BlacklistConfirmationOutcome.NONE, null)
        }
        return rejected()
    }
    
    /** The chip was tapped: the request is consumed as an explicit confirmation. */
    fun confirm(): BlacklistConfirmationResult {
        val request = pending ?: return BlacklistConfirmationResult(BlacklistConfirmationOutcome.NONE, null)
        pending = null
        return BlacklistConfirmationResult(BlacklistConfirmationOutcome.CONFIRMED, request)
    }
    
    /**
     * The chip was rejected (drag-down) or ignored (field change, yield) - consumed without ever
     * writing. Both routes share the same safe default: no blacklist entry, pending mark cleared when
     * the request carries one.
     */
    fun reject(): BlacklistConfirmationResult {
        val request = pending ?: return BlacklistConfirmationResult(BlacklistConfirmationOutcome.NONE, null)
        return rejected()
    }
    
    private fun rejected(): BlacklistConfirmationResult {
        val request = pending
        pending = null
        return BlacklistConfirmationResult(BlacklistConfirmationOutcome.REJECTED, request)
    }
    
    companion object {
        
        /**
         * How many words committed while the chip is up count as "the user is ignoring it". Two: one
         * may still be part of the sentence that triggered the escalation; a second means the user has
         * typed on past the chip without acting.
         */
        const val YIELD_AFTER_COMMITS = 2
    }
}

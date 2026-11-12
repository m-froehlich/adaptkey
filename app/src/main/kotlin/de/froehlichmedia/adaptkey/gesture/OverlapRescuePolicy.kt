// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

/**
 * Pure policy deciding whether a live key press may be parked when a second pointer lands while it is
 * still down - the D-528 rescue of exactly the overlapped tap D-495 deliberately left lost (see
 * [TouchSequenceTracker]'s own KDoc). Parked, the press survives intact and commits as a plain tap at
 * its own finger's lift; refused, the sequence dies exactly as it always did under D-495, just
 * visibly (the caller logs the refusal).
 *
 * Parked presses commit tap-only and never classify as a swipe, so D-495's own fix ("two taps must
 * never compose into one swipe") is preserved by construction: swipe classification stays per-press,
 * and every state where a second finger's coordinates could contaminate a first finger's gesture
 * remains a refusal.
 *
 * Pure by design (no Android types): the call site resolves `MotionEvent` details itself and feeds
 * plain booleans. Unit-tested directly - [de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView]
 * stays in the project's established untested Android-glue layer.
 */
object OverlapRescuePolicy {
    
    /** Why the live press may not be parked; the D-495 kill applies and the loss is logged instead. */
    enum class Refusal {
        /** No live press to park (an unclaimed DOWN that resolved no key) - stream hygiene only. */
        NO_PRESS,
        /** One rescue depth only: a three-finger pile-up keeps today's kill for both live presses. */
        ALREADY_PARKED,
        /** The press's own long-press already fired (listener action or popup arm) - not a clean tap. */
        LONG_PRESS_FIRED,
        /** A long-press alternatives popup is open (D-01) - single-pointer by design. */
        POPUP_OPEN,
        /** The live press is the backspace key (D-07) - hold-to-repeat is a hold feature, not a tap. */
        BACKSPACE_HOLD,
        /** The glide stroke already claimed (D-516/G-09, plan §4.1.3) - a second finger kills the stroke. */
        GLIDE_CLAIMED
    }
    
    /**
     * @param activePressExists whether a press is live at all (the view's resolved `pressedKey`)
     * @param parkedAlreadyExists whether a previous rescue's press is already parked
     * @param longPressFired whether the live press's own long-press has fired
     * @param popupOpen whether a long-press alternatives popup is currently open
     * @param activePressIsDelete whether the live press is the backspace key
     * @param glideStrokeClaimed whether the live press's glide stroke (if any) has claimed
     * @return null when the live press may be parked, else the refusal reason
     */
    fun refusalFor(
        activePressExists: Boolean,
        parkedAlreadyExists: Boolean,
        longPressFired: Boolean,
        popupOpen: Boolean,
        activePressIsDelete: Boolean,
        glideStrokeClaimed: Boolean
    ): Refusal? {
        if (!activePressExists) {
            return Refusal.NO_PRESS
        }
        if (parkedAlreadyExists) {
            return Refusal.ALREADY_PARKED
        }
        if (longPressFired) {
            return Refusal.LONG_PRESS_FIRED
        }
        if (popupOpen) {
            return Refusal.POPUP_OPEN
        }
        if (activePressIsDelete) {
            return Refusal.BACKSPACE_HOLD
        }
        if (glideStrokeClaimed) {
            return Refusal.GLIDE_CLAIMED
        }
        return null
    }
}

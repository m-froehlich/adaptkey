// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-528 overlapped-tap rescue policy: a clean tap still live when a second pointer
 * lands is parked instead of lost, while every state that needs single-pointer exclusivity keeps the
 * D-495 kill. The park scenario is the community-reported repro class ("typing fast skips letters",
 * Pixel 9a log: `rawTap` present, key never processed).
 */
class OverlapRescuePolicyTest {
    
    private fun refuse(
        activePressExists: Boolean = true,
        parkedAlreadyExists: Boolean = false,
        longPressFired: Boolean = false,
        popupOpen: Boolean = false,
        activePressIsDelete: Boolean = false,
        glideStrokeClaimed: Boolean = false
    ): OverlapRescuePolicy.Refusal? {
        return OverlapRescuePolicy.refusalFor(
            activePressExists,
            parkedAlreadyExists,
            longPressFired,
            popupOpen,
            activePressIsDelete,
            glideStrokeClaimed
        )
    }
    
    @Test
    fun `the reported repro - a clean letter tap still down when the second finger lands - parks`() {
        // Finger 1 resolved 'a' cleanly (rawTap logged, no motion, no long-press, no popup), finger 2
        // lands mid-press: the first tap must survive instead of dying silently.
        assertNull(refuse())
    }
    
    @Test
    fun `an unclaimed glide stroke is still just a tap and parks`() {
        // Glide on: every letter DOWN begins an unclaimed stroke; it is not a refusal - the stroke dies
        // silently with the park and the tap commits.
        assertNull(refuse(glideStrokeClaimed = false))
    }
    
    @Test
    fun `a claimed glide stroke keeps the D-495 kill - a second finger never commits mid-stroke`() {
        assertEquals(OverlapRescuePolicy.Refusal.GLIDE_CLAIMED, refuse(glideStrokeClaimed = true))
    }
    
    @Test
    fun `a fired long-press is not a clean tap any more`() {
        assertEquals(OverlapRescuePolicy.Refusal.LONG_PRESS_FIRED, refuse(longPressFired = true))
    }
    
    @Test
    fun `an open alternatives popup stays single-pointer`() {
        assertEquals(OverlapRescuePolicy.Refusal.POPUP_OPEN, refuse(popupOpen = true))
    }
    
    @Test
    fun `a held backspace keeps its hold-to-repeat semantics`() {
        assertEquals(OverlapRescuePolicy.Refusal.BACKSPACE_HOLD, refuse(activePressIsDelete = true))
    }
    
    @Test
    fun `a live press with nothing to park is stream hygiene, not a lost tap`() {
        assertEquals(OverlapRescuePolicy.Refusal.NO_PRESS, refuse(activePressExists = false))
    }
    
    @Test
    fun `only one rescue depth - a third finger during a live park keeps today's kill`() {
        assertEquals(OverlapRescuePolicy.Refusal.ALREADY_PARKED, refuse(parkedAlreadyExists = true))
    }
}

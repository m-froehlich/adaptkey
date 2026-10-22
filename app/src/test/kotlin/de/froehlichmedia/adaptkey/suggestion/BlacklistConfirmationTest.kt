// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.dictionary.BlacklistOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BlacklistConfirmation], D-488's pure state machine: the chip is the only path that
 * ever writes a keyboard-surface blacklist entry, ignoring must never write, and the pending D-177
 * mark is cleared exactly for the escalation shape.
 */
class BlacklistConfirmationTest {
    
    private fun escalation(word: String = "foo") =
        BlacklistConfirmationRequest(word, BlacklistOrigin.ESCALATION, clearsPendingMark = true)
    
    private fun gesture(word: String = "foo") =
        BlacklistConfirmationRequest(word, BlacklistOrigin.GESTURE_G04, clearsPendingMark = false)
    
    @Test
    fun armThenConfirmProducesAConfirmedWriteWithTheArmingOrigin() {
        val confirmation = BlacklistConfirmation()
        assertTrue(confirmation.arm(gesture()))
        
        val result = confirmation.confirm()
        
        assertEquals(BlacklistConfirmationOutcome.CONFIRMED, result.outcome)
        assertEquals(BlacklistOrigin.GESTURE_G04, result.request?.origin)
        assertEquals("foo", result.request?.word)
        assertNull(confirmation.pending)
    }
    
    @Test
    fun aSecondArmWhileOneIsPendingIsRefusedAndKeepsTheFirst() {
        val confirmation = BlacklistConfirmation()
        assertTrue(confirmation.arm(gesture(word = "foo")))
        
        assertFalse(confirmation.arm(escalation(word = "bar")))
        
        assertEquals("foo", confirmation.pending?.word)
    }
    
    @Test
    fun rejectNeverConfirmsAndClearsTheChip() {
        val confirmation = BlacklistConfirmation()
        confirmation.arm(escalation())
        
        val result = confirmation.reject()
        
        assertEquals(BlacklistConfirmationOutcome.REJECTED, result.outcome)
        assertEquals(BlacklistOrigin.ESCALATION, result.request?.origin)
        assertNull(confirmation.pending)
    }
    
    @Test
    fun theFirstCommitKeepsTheChipAndTheSecondYieldsItAsIgnored() {
        val confirmation = BlacklistConfirmation()
        confirmation.arm(escalation())
        
        val first = confirmation.onCommit()
        assertEquals(BlacklistConfirmationOutcome.NONE, first.outcome)
        assertNull(first.request)
        
        val second = confirmation.onCommit()
        assertEquals(BlacklistConfirmationOutcome.REJECTED, second.outcome)
        assertEquals(BlacklistOrigin.ESCALATION, second.request?.origin)
        assertNull(confirmation.pending)
    }
    
    @Test
    fun confirmInsideTheYieldWindowStillWrites() {
        val confirmation = BlacklistConfirmation()
        confirmation.arm(gesture())
        confirmation.onCommit()
        
        val result = confirmation.confirm()
        
        assertEquals(BlacklistConfirmationOutcome.CONFIRMED, result.outcome)
    }
    
    @Test
    fun operationsWithoutAPendingRequestAreNoOps() {
        val confirmation = BlacklistConfirmation()
        
        assertEquals(BlacklistConfirmationOutcome.NONE, confirmation.confirm().outcome)
        assertEquals(BlacklistConfirmationOutcome.NONE, confirmation.reject().outcome)
        assertEquals(BlacklistConfirmationOutcome.NONE, confirmation.onCommit().outcome)
        assertNull(confirmation.confirm().request)
    }
    
    @Test
    fun aFreshArmResetsTheCommitCounter() {
        val confirmation = BlacklistConfirmation()
        confirmation.arm(gesture())
        confirmation.onCommit()
        confirmation.reject()
        
        // A new chip must again get the full two commits before yielding.
        confirmation.arm(escalation())
        assertEquals(BlacklistConfirmationOutcome.NONE, confirmation.onCommit().outcome)
        assertEquals(BlacklistConfirmationOutcome.REJECTED, confirmation.onCommit().outcome)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-142 login-field classification. */
class LoginFieldDetectorTest {
    
    @Test
    fun `email variation is classified as email`() {
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.classify(0x21))
    }
    
    @Test
    fun `web email variation is classified as email`() {
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.classify(0xd1))
    }
    
    @Test
    fun `password variation is classified as password`() {
        assertEquals(LoginFieldKind.PASSWORD, LoginFieldDetector.classify(0x81))
    }
    
    @Test
    fun `visible password variation is classified as password`() {
        assertEquals(LoginFieldKind.PASSWORD, LoginFieldDetector.classify(0x91))
    }
    
    @Test
    fun `web password variation is classified as password`() {
        assertEquals(LoginFieldKind.PASSWORD, LoginFieldDetector.classify(0xe1))
    }
    
    @Test
    fun `an ordinary text field with no signal is classified as none`() {
        assertEquals(LoginFieldKind.NONE, LoginFieldDetector.classify(0))
    }
    
    @Test
    fun `classify never produces username - InputType has no variation for it`() {
        // Every variation constant that exists must map to EMAIL, PASSWORD or NONE.
        assertFalse(listOf(0x21, 0xd1, 0x81, 0x91, 0xe1, 0, 0x01, 0x02, 0x03)
            .any { LoginFieldDetector.classify(it) == LoginFieldKind.USERNAME })
    }
    
    @Test
    fun `hasWeakUsernameSignal matches an English hint text`() {
        assertTrue(LoginFieldDetector.hasWeakUsernameSignal("Username", null))
    }
    
    @Test
    fun `hasWeakUsernameSignal matches a German hint text case-insensitively`() {
        assertTrue(LoginFieldDetector.hasWeakUsernameSignal("Bitte Benutzername eingeben", null))
    }
    
    @Test
    fun `hasWeakUsernameSignal matches the field name when hint text is absent`() {
        assertTrue(LoginFieldDetector.hasWeakUsernameSignal(null, "login_field"))
    }
    
    @Test
    fun `hasWeakUsernameSignal is false for an unrelated hint`() {
        assertFalse(LoginFieldDetector.hasWeakUsernameSignal("Nachricht eingeben", "message"))
    }
    
    @Test
    fun `hasWeakUsernameSignal is false when both signals are absent`() {
        assertFalse(LoginFieldDetector.hasWeakUsernameSignal(null, null))
    }
}

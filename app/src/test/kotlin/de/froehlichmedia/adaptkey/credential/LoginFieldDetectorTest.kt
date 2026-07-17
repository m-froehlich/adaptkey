// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-142 login-field classification. The variation literals below are the bare
 * (class-bit-free) `InputType` values `classify()` actually receives - verified via `javap` against the
 * real `android-35` `InputType.class` - see [LoginFieldDetector]'s own KDoc for the masking bug this
 * corrected (found while implementing D-143's analogous URI-variation check).
 */
class LoginFieldDetectorTest {
    
    @Test
    fun `email variation is classified as email`() {
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.classify(0x20))
    }
    
    @Test
    fun `web email variation is classified as email`() {
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.classify(0xd0))
    }
    
    @Test
    fun `password variation is classified as password`() {
        assertEquals(LoginFieldKind.PASSWORD, LoginFieldDetector.classify(0x80))
    }
    
    @Test
    fun `visible password variation is classified as password`() {
        assertEquals(LoginFieldKind.PASSWORD, LoginFieldDetector.classify(0x90))
    }
    
    @Test
    fun `web password variation is classified as password`() {
        assertEquals(LoginFieldKind.PASSWORD, LoginFieldDetector.classify(0xe0))
    }
    
    @Test
    fun `an ordinary text field with no signal is classified as none`() {
        assertEquals(LoginFieldKind.NONE, LoginFieldDetector.classify(0))
    }
    
    @Test
    fun `classify never produces username - InputType has no variation for it`() {
        // Every variation constant that exists must map to EMAIL, PASSWORD or NONE.
        assertFalse(listOf(0x20, 0xd0, 0x80, 0x90, 0xe0, 0, 0x01, 0x02, 0x03)
            .any { LoginFieldDetector.classify(it) == LoginFieldKind.USERNAME })
    }
    
    @Test
    fun `weakSignalKind matches an English username hint text`() {
        assertEquals(LoginFieldKind.USERNAME, LoginFieldDetector.weakSignalKind("Username", null))
    }
    
    @Test
    fun `weakSignalKind matches a German username hint text case-insensitively`() {
        assertEquals(LoginFieldKind.USERNAME, LoginFieldDetector.weakSignalKind("Bitte Benutzername eingeben", null))
    }
    
    @Test
    fun `weakSignalKind matches the field name when hint text is absent`() {
        assertEquals(LoginFieldKind.USERNAME, LoginFieldDetector.weakSignalKind(null, "login_field"))
    }
    
    @Test
    fun `weakSignalKind matches an English email hint text`() {
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.weakSignalKind("Email address", null))
    }
    
    @Test
    fun `weakSignalKind matches a German email hint text - the finanzen_net_zero case`() {
        // D-142 follow-up: a real device field labelled "E-Mail-Adresse" did not classify() as EMAIL at
        // all (InputType variation not set) - this is the fallback that should catch it instead.
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.weakSignalKind("E-Mail-Adresse", null))
    }
    
    @Test
    fun `weakSignalKind prefers email over username when both keywords are present`() {
        assertEquals(LoginFieldKind.EMAIL, LoginFieldDetector.weakSignalKind("Username or email", null))
    }
    
    @Test
    fun `weakSignalKind is none for an unrelated hint`() {
        assertEquals(LoginFieldKind.NONE, LoginFieldDetector.weakSignalKind("Nachricht eingeben", "message"))
    }
    
    @Test
    fun `weakSignalKind is none when both signals are absent`() {
        assertEquals(LoginFieldKind.NONE, LoginFieldDetector.weakSignalKind(null, null))
    }
}

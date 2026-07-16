// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-139 callback-burst circuit breaker. */
class CallbackBurstGuardTest {
    
    @Test
    fun `calls within the limit are never a burst`() {
        val guard = CallbackBurstGuard(windowMs = 200L, limit = 3)
        assertFalse(guard.isBurst(0L))
        assertFalse(guard.isBurst(10L))
        assertFalse(guard.isBurst(20L))
    }
    
    @Test
    fun `a call beyond the limit within the window is a burst`() {
        val guard = CallbackBurstGuard(windowMs = 200L, limit = 3)
        repeat(3) { i -> guard.isBurst(i.toLong()) }
        assertTrue(guard.isBurst(30L))
    }
    
    @Test
    fun `the window resets once it elapses, so a sparse sequence never trips it`() {
        val guard = CallbackBurstGuard(windowMs = 200L, limit = 3)
        repeat(3) { i -> guard.isBurst(i.toLong()) }
        // Well past the window - the count starts over instead of accumulating forever.
        assertFalse(guard.isBurst(1000L))
        assertFalse(guard.isBurst(1010L))
    }
    
    @Test
    fun `a sustained but legitimate rate, like a held backspace, stays under a wide-margin limit`() {
        // §59: backspace-hold repeats as fast as every 45ms - simulate 200ms of that (about 4-5 ticks).
        val guard = CallbackBurstGuard()
        var burst = false
        var t = 0L
        while (t < 200L) {
            burst = guard.isBurst(t) || burst
            t += 45L
        }
        assertFalse(burst)
    }
    
    @Test
    fun `a tight, unthrottled cascade well beyond any legitimate rate is detected`() {
        val guard = CallbackBurstGuard()
        var burst = false
        repeat(100) { i -> burst = guard.isBurst(i.toLong()) || burst }
        assertTrue(burst)
    }
}

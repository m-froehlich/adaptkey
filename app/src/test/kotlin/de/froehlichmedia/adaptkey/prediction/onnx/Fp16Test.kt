// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the fp16 → float conversion used when reading the tier-3 logits.
 */
class Fp16Test {
    
    private fun f(bits: Int): Float = Fp16.toFloat(bits.toShort())
    
    @Test
    fun `common values convert exactly`() {
        assertEquals(0.0f, f(0x0000))
        assertEquals(1.0f, f(0x3C00))
        assertEquals(2.0f, f(0x4000))
        assertEquals(-1.0f, f(0xBC00))
        assertEquals(-2.0f, f(0xC000))
        assertEquals(0.5f, f(0x3800))
        assertEquals(1.5f, f(0x3E00))
        assertEquals(10.0f, f(0x4900))
    }
    
    @Test
    fun `signed zero and infinities`() {
        assertEquals(-0.0f, f(0x8000))
        assertTrue(f(0x7C00).isInfinite() && f(0x7C00) > 0f)
        assertTrue(f(0xFC00).isInfinite() && f(0xFC00) < 0f)
        assertTrue(f(0x7E00).isNaN())
    }
    
    @Test
    fun `smallest subnormal converts`() {
        // 0x0001 is 2^-24.
        assertEquals(Math.pow(2.0, -24.0).toFloat(), f(0x0001))
    }
    
    @Test
    fun `ordering is preserved for argmax`() {
        assertTrue(f(0x4900) > f(0x3C00))
        assertTrue(f(0x3C00) > f(0x0000))
        assertTrue(f(0x0000) > f(0xBC00))
    }
}

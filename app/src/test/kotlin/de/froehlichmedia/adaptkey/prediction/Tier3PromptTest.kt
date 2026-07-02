// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-3 continuation prompt builder.
 */
class Tier3PromptTest {
    
    private fun request(sentence: String): Tier3Request {
        return Tier3Request("token", null, sentence)
    }
    
    @Test
    fun `a short context is used verbatim`() {
        assertEquals("Guten Morgen, wie", Tier3Prompt.build(request("Guten Morgen, wie")))
    }
    
    @Test
    fun `a long context is capped to the trailing window`() {
        val sentence = "x".repeat(300)
        val prompt = Tier3Prompt.build(request(sentence), maxContextChars = 256)
        assertEquals(256, prompt.length)
        assertEquals("x".repeat(256), prompt)
    }
    
    @Test
    fun `the trailing window keeps the most recent characters`() {
        val prompt = Tier3Prompt.build(request("abcdefgh"), maxContextChars = 3)
        assertEquals("fgh", prompt)
    }
    
    @Test
    fun `a non-positive window is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Tier3Prompt.build(request("hi"), maxContextChars = 0) }
    }
}

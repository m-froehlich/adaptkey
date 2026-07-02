// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the GPT-2 byte-level character mapping.
 */
class ByteLevelTest {
    
    @Test
    fun `printable ascii maps to itself`() {
        assertEquals("A", ByteLevel.encode("A"))
        assertEquals("hello!", ByteLevel.encode("hello!"))
    }
    
    @Test
    fun `space and newline map to the reserved characters`() {
        assertEquals("Ġ", ByteLevel.encode(" "))
        assertEquals("Ċ", ByteLevel.encode("\n"))
    }
    
    @Test
    fun `multi-byte utf-8 maps one character per byte`() {
        // ß is two UTF-8 bytes, so it becomes two byte-level characters.
        assertEquals(2, ByteLevel.encode("ß").length)
    }
    
    @Test
    fun `encode then decode round-trips`() {
        for (sample in listOf("Ich heiße Max", " leading", "a\nb", "Καλημέρα", "2024!")) {
            assertEquals(sample, ByteLevel.decode(ByteLevel.encode(sample)))
        }
    }
}

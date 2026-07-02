// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Algorithm-level unit tests for the byte-level BPE tokenizer on small, hand-built vocabularies.
 * Exact parity against the real SmolLM2 vocabulary is covered by [BpeTokenizerParityTest].
 */
class BpeTokenizerTest {
    
    @Test
    fun `merges are applied in rank order`() {
        val tokenizer = BpeTokenizer(
            tokenToId = mapOf("a" to 0, "b" to 1, "c" to 2, "ab" to 3, "abc" to 4),
            merges = listOf("a" to "b", "ab" to "c")
        )
        assertArrayEquals(intArrayOf(4), tokenizer.encode("abc"))
    }
    
    @Test
    fun `a leading space becomes the reserved byte-level character`() {
        val tokenizer = BpeTokenizer(
            tokenToId = mapOf("a" to 0, "Ġ" to 1, "Ġa" to 2),
            merges = listOf("Ġ" to "a")
        )
        assertArrayEquals(intArrayOf(2), tokenizer.encode(" a"))
    }
    
    @Test
    fun `digits are split individually`() {
        val tokenizer = BpeTokenizer(
            tokenToId = mapOf("1" to 0, "2" to 1),
            merges = emptyList()
        )
        assertArrayEquals(intArrayOf(0, 1), tokenizer.encode("12"))
    }
    
    @Test
    fun `encode then decode round-trips`() {
        val tokenizer = BpeTokenizer(
            tokenToId = mapOf("a" to 0, "b" to 1, "c" to 2, "ab" to 3, "abc" to 4),
            merges = listOf("a" to "b", "ab" to "c")
        )
        assertEquals("abc", tokenizer.decode(tokenizer.encode("abc")))
    }
    
    @Test
    fun `special tokens are isolated on encode and skippable on decode`() {
        val tokenizer = BpeTokenizer(
            tokenToId = mapOf("h" to 0, "i" to 1, "hi" to 2, "<|x|>" to 9),
            merges = listOf("h" to "i"),
            specialTokens = mapOf("<|x|>" to 9)
        )
        assertArrayEquals(intArrayOf(2, 9, 2), tokenizer.encode("hi<|x|>hi"))
        assertEquals("hihi", tokenizer.decode(intArrayOf(2, 9, 2)))
        assertEquals("hi<|x|>hi", tokenizer.decode(intArrayOf(2, 9, 2), skipSpecial = false))
    }
    
    @Test
    fun `the special token id is resolvable`() {
        val tokenizer = BpeTokenizer(
            tokenToId = mapOf("<|x|>" to 9),
            merges = emptyList(),
            specialTokens = mapOf("<|x|>" to 9)
        )
        assertEquals(9, tokenizer.specialTokenId("<|x|>"))
        assertEquals(null, tokenizer.specialTokenId("<|y|>"))
    }
}

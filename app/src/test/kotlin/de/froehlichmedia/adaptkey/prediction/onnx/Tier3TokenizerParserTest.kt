// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for assembling a [BpeTokenizer] from vocab.json + merges.txt text.
 */
class Tier3TokenizerParserTest {
    
    @Test
    fun `builds a working tokenizer from vocab and merges`() {
        val vocab = """{"h": 0, "i": 1, "hi": 2}"""
        val tokenizer = Tier3TokenizerParser.parse(vocab, "h i\n")
        assertArrayEquals(intArrayOf(2), tokenizer.encode("hi"))
    }
    
    @Test
    fun `resolves special-token ids from the vocabulary`() {
        val vocab = """{"h": 0, "i": 1, "hi": 2, "<|im_end|>": 3}"""
        val tokenizer = Tier3TokenizerParser.parse(vocab, "h i\n")
        assertEquals(3, tokenizer.specialTokenId("<|im_end|>"))
        assertArrayEquals(intArrayOf(2, 3), tokenizer.encode("hi<|im_end|>"))
    }
    
    @Test
    fun `parseMerges skips the version header and blank lines`() {
        val merges = Tier3TokenizerParser.parseMerges("#version: 0.2\nh i\n\nĠ w\n")
        assertEquals(listOf("h" to "i", "Ġ" to "w"), merges)
    }
    
    @Test
    fun `parseMerges tolerates carriage returns`() {
        assertEquals(listOf("a" to "b"), Tier3TokenizerParser.parseMerges("a b\r\n"))
    }
    
    @Test
    fun `an absent special content is simply omitted`() {
        val tokenizer = Tier3TokenizerParser.parse("""{"a": 0}""", "")
        assertEquals(null, tokenizer.specialTokenId("<|im_end|>"))
    }
}

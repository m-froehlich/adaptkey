// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure emoji-keyword search asset parser (L-03, D-317).
 */
class EmojiKeywordParserTest {
    
    @Test
    fun `parses emoji-terms lines, splitting the pipe-separated term list`() {
        val raw = "💩\tkothaufen|poop|pile of poo\n😀\tgrinning face"
        
        val index = EmojiKeywordParser.parse(raw)
        
        assertEquals(listOf("💩"), index.search("poop"))
        assertEquals(listOf("💩"), index.search("kothaufen"))
        assertEquals(listOf("😀"), index.search("grinning"))
    }
    
    @Test
    fun `blank lines and a line without a tab are skipped without failing`() {
        val raw = "\n😀\n😀\tgrinning face\n"
        
        val index = EmojiKeywordParser.parse(raw)
        
        assertEquals(listOf("😀"), index.search("grinning"))
    }
    
    @Test
    fun `a line with a tab but no terms after it is skipped`() {
        val raw = "😀\t"
        
        val index = EmojiKeywordParser.parse(raw)
        
        assertEquals(emptyList<String>(), index.search("grinning"))
    }
    
    @Test
    fun `an empty input yields an empty index`() {
        val index = EmojiKeywordParser.parse("")
        
        assertEquals(emptyList<String>(), index.search("anything"))
    }
}

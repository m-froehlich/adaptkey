// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure emoji-search index (L-03, D-317).
 */
class EmojiKeywordIndexTest {
    
    private val index = EmojiKeywordIndex(
        mapOf(
            "💩" to listOf("kothaufen", "poop", "pile of poo"),
            "😀" to listOf("grinning face"),
            "❤" to listOf("red heart")
        )
    )
    
    @Test
    fun `matches a substring anywhere within a term, not only a prefix`() {
        assertEquals(listOf("😀"), index.search("face"))
    }
    
    @Test
    fun `matching is case-insensitive`() {
        assertEquals(listOf("💩"), index.search("POOP"))
    }
    
    @Test
    fun `leading and trailing whitespace is trimmed before matching`() {
        assertEquals(listOf("❤"), index.search("  heart  "))
    }
    
    @Test
    fun `a blank query matches nothing`() {
        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("   ").isEmpty())
    }
    
    @Test
    fun `a query matching no term returns no results`() {
        assertTrue(index.search("xyz").isEmpty())
    }
    
    @Test
    fun `an emoji whose terms include a matching multi-word phrase is found by any word in it`() {
        assertEquals(listOf("💩"), index.search("pile"))
    }
    
    @Test
    fun `the empty index matches nothing`() {
        assertTrue(EmojiKeywordIndex.EMPTY.search("poop").isEmpty())
    }
}

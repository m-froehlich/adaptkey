// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure MRU policy behind the emoji panel's "recent" tab (L-03).
 */
class RecentEmojisTest {
    
    @Test
    fun `a fresh emoji is moved to the front`() {
        val current = listOf("😀", "🐻")
        
        assertEquals(listOf("🍕", "😀", "🐻"), RecentEmojis.recordUse(current, "🍕"))
    }
    
    @Test
    fun `re-using an emoji moves it to the front without duplicating it`() {
        val current = listOf("😀", "🐻", "🍕")
        
        assertEquals(listOf("🐻", "😀", "🍕"), RecentEmojis.recordUse(current, "🐻"))
    }
    
    @Test
    fun `the list is capped at maxSize`() {
        val current = listOf("a", "b", "c")
        
        assertEquals(listOf("x", "a", "b"), RecentEmojis.recordUse(current, "x", maxSize = 3))
    }
    
    @Test
    fun `re-using the emoji at the cap still fits after dedup`() {
        val current = listOf("a", "b", "c")
        
        assertEquals(listOf("c", "a", "b"), RecentEmojis.recordUse(current, "c", maxSize = 3))
    }
    
    @Test
    fun `an empty emoji is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { RecentEmojis.recordUse(emptyList(), "") }
    }
    
    @Test
    fun `a non-positive maxSize is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { RecentEmojis.recordUse(emptyList(), "😀", maxSize = 0) }
    }
}

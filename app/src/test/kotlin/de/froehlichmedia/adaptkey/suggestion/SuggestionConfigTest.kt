// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for the suggestion-bar configuration (C-02, C-03, C-04).
 */
class SuggestionConfigTest {
    
    @Test
    fun `defaults match the specification`() {
        val config = SuggestionConfig()
        assertEquals(8, config.maxSuggestions)
        assertEquals(300L, config.reSortDelayMs)
        assertFalse(config.highlightEnabled)
        assertEquals(SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR, config.highlightColor)
    }
    
    @Test
    fun `accepts the recommended suggestion-count bounds`() {
        assertEquals(6, SuggestionConfig(maxSuggestions = 6).maxSuggestions)
        assertEquals(10, SuggestionConfig(maxSuggestions = 10).maxSuggestions)
    }
    
    @Test
    fun `rejects a non-positive suggestion count`() {
        assertThrows<IllegalArgumentException> { SuggestionConfig(maxSuggestions = 0) }
    }
    
    @Test
    fun `rejects an out-of-range re-sort delay`() {
        assertThrows<IllegalArgumentException> { SuggestionConfig(reSortDelayMs = 601L) }
    }
    
    @Test
    fun `accepts the re-sort delay bounds`() {
        assertEquals(0L, SuggestionConfig(reSortDelayMs = 0L).reSortDelayMs)
        assertEquals(600L, SuggestionConfig(reSortDelayMs = 600L).reSortDelayMs)
    }
    
    @Test
    fun `rejects a negative re-sort delay`() {
        assertThrows<IllegalArgumentException> { SuggestionConfig(reSortDelayMs = -1L) }
    }
}

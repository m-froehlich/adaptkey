// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure direct-paste chip label formatting (D-36).
 */
class ClipboardPreviewTest {
    
    @Test
    fun `short text is shown as-is`() {
        assertEquals("hello", ClipboardPreview.label("hello", sensitive = false))
    }
    
    @Test
    fun `long text is truncated with an ellipsis`() {
        val label = ClipboardPreview.label("this is a rather long clipboard entry", sensitive = false)
        assertTrue(label!!.endsWith("…"))
        assertTrue(label.length <= 24)
    }
    
    @Test
    fun `whitespace is collapsed to a single line`() {
        assertEquals("a b c", ClipboardPreview.label("a\n b\t  c", sensitive = false))
    }
    
    @Test
    fun `sensitive content is masked, never shown`() {
        val label = ClipboardPreview.label("hunter2password", sensitive = true)
        assertTrue(label!!.all { it == '•' })
        assertTrue("hunter2password" !in label)
    }
    
    @Test
    fun `blank or null clipboard yields no chip`() {
        assertNull(ClipboardPreview.label(null, sensitive = false))
        assertNull(ClipboardPreview.label("   ", sensitive = false))
        assertNull(ClipboardPreview.label("\n\t", sensitive = true))
    }
    
    @Test
    fun `paragraph 40 a clip copied just now is fresh`() {
        assertTrue(ClipboardPreview.isFresh(clipTimestampMs = 1_000L, nowMs = 1_000L))
    }
    
    @Test
    fun `paragraph 40 a clip exactly at the age limit is still fresh`() {
        assertTrue(ClipboardPreview.isFresh(clipTimestampMs = 0L, nowMs = ClipboardPreview.MAX_AGE_MS))
    }
    
    @Test
    fun `paragraph 40 a clip past the age limit is no longer fresh`() {
        assertFalse(ClipboardPreview.isFresh(clipTimestampMs = 0L, nowMs = ClipboardPreview.MAX_AGE_MS + 1))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.diagnostics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-139/D-110 in-memory diagnostic ring buffer. */
class DiagnosticRingBufferTest {
    
    @Test
    fun `record keeps entries within the retention window`() {
        val buffer = DiagnosticRingBuffer(retentionMs = 5000L)
        buffer.record(1000L, "a")
        buffer.record(3000L, "b")
        
        assertEquals(listOf("a", "b"), buffer.entries().map { it.message })
    }
    
    @Test
    fun `record drops entries older than the retention window relative to the newest one`() {
        val buffer = DiagnosticRingBuffer(retentionMs = 5000L)
        buffer.record(1000L, "old")
        buffer.record(7000L, "new") // 1000 is now 6000ms old, past the 5000ms window
        
        assertEquals(listOf("new"), buffer.entries().map { it.message })
    }
    
    @Test
    fun `an entry exactly at the retention boundary is kept`() {
        val buffer = DiagnosticRingBuffer(retentionMs = 5000L)
        buffer.record(1000L, "boundary")
        buffer.record(6000L, "newest") // exactly 5000ms old - not yet over the window
        
        assertEquals(listOf("boundary", "newest"), buffer.entries().map { it.message })
    }
    
    @Test
    fun `several old entries are all pruned at once`() {
        val buffer = DiagnosticRingBuffer(retentionMs = 1000L)
        buffer.record(0L, "a")
        buffer.record(100L, "b")
        buffer.record(200L, "c")
        buffer.record(5000L, "d")
        
        assertEquals(listOf("d"), buffer.entries().map { it.message })
    }
    
    @Test
    fun `entries preserves insertion order`() {
        val buffer = DiagnosticRingBuffer(retentionMs = 10_000L)
        buffer.record(0L, "first")
        buffer.record(1L, "second")
        buffer.record(2L, "third")
        
        assertEquals(listOf("first", "second", "third"), buffer.entries().map { it.message })
    }
    
    @Test
    fun `clear discards every entry`() {
        val buffer = DiagnosticRingBuffer(retentionMs = 10_000L)
        buffer.record(0L, "a")
        buffer.record(1L, "b")
        buffer.clear()
        
        assertTrue(buffer.entries().isEmpty())
    }
    
    @Test
    fun `a fresh buffer starts empty`() {
        assertTrue(DiagnosticRingBuffer(retentionMs = 1000L).entries().isEmpty())
    }
}

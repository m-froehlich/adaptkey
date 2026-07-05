// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure D-09 raw-tap diagnostic collector.
 */
class RawTapRecorderTest {
    
    @Test
    fun `a fresh recorder is empty and yields a header-only table`() {
        val recorder = RawTapRecorder()
        assertTrue(recorder.isEmpty())
        assertEquals("index\texpected\tresolved\tkeyCenterX\tkeyCenterY\ttapX\ttapY\tdeviationX\tdeviationY\n", recorder.toTsv())
    }
    
    @Test
    fun `deviation is the contact minus the key centre`() {
        val tap = RawTap('a', "c:a", keyCenterX = 100f, keyCenterY = 200f, tapX = 92f, tapY = 205f)
        assertEquals(-8f, tap.deviationX)
        assertEquals(5f, tap.deviationY)
    }
    
    @Test
    fun `recorded taps are serialised as tab-separated rows with rounded coordinates`() {
        val recorder = RawTapRecorder()
        recorder.record('a', "c:a", 100f, 200f, 92.345f, 205.5f)
        
        assertFalse(recorder.isEmpty())
        assertEquals(1, recorder.entries.size)
        val lines = recorder.toTsv().trimEnd('\n').split('\n')
        assertEquals(2, lines.size)
        assertEquals("0\ta\tc:a\t100.0\t200.0\t92.35\t205.5\t-7.65\t5.5", lines[1])
    }
    
    @Test
    fun `a null expected char is blank and a space is labelled`() {
        val recorder = RawTapRecorder()
        recorder.record(null, "c:x", 0f, 0f, 1f, 1f)
        recorder.record(' ', "SPACE", 0f, 0f, 0f, 0f)
        val lines = recorder.toTsv().trimEnd('\n').split('\n')
        assertTrue(lines[1].startsWith("0\t\tc:x\t"), "null expected char is an empty field")
        assertTrue(lines[2].startsWith("1\tspace\tSPACE\t"), "a space is labelled 'space'")
    }
    
    @Test
    fun `clear discards all recorded taps`() {
        val recorder = RawTapRecorder()
        recorder.record('a', "c:a", 0f, 0f, 0f, 0f)
        recorder.clear()
        assertTrue(recorder.isEmpty())
    }
}

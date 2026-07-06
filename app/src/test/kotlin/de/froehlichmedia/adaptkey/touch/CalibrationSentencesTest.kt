// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the default K-01 calibration sentences.
 */
class CalibrationSentencesTest {
    
    @Test
    fun `default set offers a handful of sentences`() {
        val size = CalibrationSentences.DEFAULT.size
        assertTrue(size in 3..8, "expected 3-8 sentences, was $size")
    }
    
    @Test
    fun `no sentence is blank`() {
        assertTrue(CalibrationSentences.DEFAULT.none { it.isBlank() })
    }
    
    @Test
    fun `sentences contain only typeable letters and spaces`() {
        // The base QWERTZ layout (L-01) has no umlauts, no eszett and no digits, so a calibration
        // sentence must be expressible with letters and spaces alone.
        val offending = CalibrationSentences.DEFAULT
            .flatMap { it.toList() }
            .filterNot { it == ' ' || it in 'a'..'z' || it in 'A'..'Z' }
        assertTrue(offending.isEmpty(), "untypeable characters: $offending")
    }
    
    @Test
    fun `sentences together cover the whole alphabet`() {
        val covered = CalibrationSentences.DEFAULT
            .flatMap { it.lowercase().toList() }
            .filter { it in 'a'..'z' }
            .toSet()
        assertEquals(('a'..'z').toSet(), covered)
    }
}

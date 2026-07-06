// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the typing-pattern classifier (T-04).
 */
class TypingPatternClassifierTest {
    
    private val classifier = TypingPatternClassifier()
    
    private fun sample(
        normalizedX: Double,
        lateral: Double,
        vertical: Double = 0.0,
        area: Double? = null,
        count: Long = 30L
    ): KeySample {
        return KeySample(normalizedX, lateral, vertical, area, count)
    }
    
    @Test
    fun `unknown below the minimum sample count`() {
        val samples = listOf(sample(0.3, lateral = 0.5, count = 10L))
        
        assertEquals(TypingPattern.UNKNOWN, classifier.classify(samples))
    }
    
    @Test
    fun `unknown for empty input`() {
        assertEquals(TypingPattern.UNKNOWN, classifier.classify(emptyList()))
    }
    
    @Test
    fun `zero-count samples do not contribute`() {
        val samples = listOf(sample(0.3, lateral = 0.5, count = 0L))
        
        assertEquals(TypingPattern.UNKNOWN, classifier.classify(samples))
    }
    
    @Test
    fun `clear rightward bias is the left index finger`() {
        val samples = listOf(sample(0.3, lateral = 0.3), sample(0.7, lateral = 0.3))
        
        assertEquals(TypingPattern.LEFT_INDEX_FINGER, classifier.classify(samples))
    }
    
    @Test
    fun `clear leftward bias is the right index finger`() {
        val samples = listOf(sample(0.3, lateral = -0.3), sample(0.7, lateral = -0.3))
        
        assertEquals(TypingPattern.RIGHT_INDEX_FINGER, classifier.classify(samples))
    }
    
    @Test
    fun `right-half pronounced bias identifies the left index finger below the overall threshold`() {
        // Overall lateral 0.125 is below the threshold, but the right half alone clears it (T-04).
        val samples = listOf(sample(0.3, lateral = 0.0), sample(0.7, lateral = 0.25))
        
        assertEquals(TypingPattern.LEFT_INDEX_FINGER, classifier.classify(samples))
    }
    
    @Test
    fun `left-half pronounced bias identifies the right index finger below the overall threshold`() {
        val samples = listOf(sample(0.3, lateral = -0.25), sample(0.7, lateral = 0.0))
        
        assertEquals(TypingPattern.RIGHT_INDEX_FINGER, classifier.classify(samples))
    }
    
    @Test
    fun `low lateral with large centred contact area is a thumb`() {
        val samples = listOf(
            sample(0.3, lateral = 0.05, vertical = 0.1, area = 0.25),
            sample(0.7, lateral = -0.05, vertical = -0.1, area = 0.25)
        )
        
        assertEquals(TypingPattern.THUMB, classifier.classify(samples))
    }
    
    @Test
    fun `low lateral without contact-area evidence stays unknown`() {
        val samples = listOf(sample(0.3, lateral = 0.05), sample(0.7, lateral = -0.05))
        
        assertEquals(TypingPattern.UNKNOWN, classifier.classify(samples))
    }
    
    @Test
    fun `low lateral with a small contact area is not a thumb`() {
        val samples = listOf(
            sample(0.3, lateral = 0.05, vertical = 0.1, area = 0.08),
            sample(0.7, lateral = -0.05, vertical = -0.1, area = 0.08)
        )
        
        assertEquals(TypingPattern.UNKNOWN, classifier.classify(samples))
    }
    
    @Test
    fun `low lateral but vertically off-centre is not a thumb`() {
        val samples = listOf(
            sample(0.3, lateral = 0.05, vertical = 0.5, area = 0.25),
            sample(0.7, lateral = -0.05, vertical = 0.5, area = 0.25)
        )
        
        assertEquals(TypingPattern.UNKNOWN, classifier.classify(samples))
    }
    
    @Test
    fun `a strong lateral bias outweighs a large contact area`() {
        val samples = listOf(
            sample(0.3, lateral = 0.3, vertical = 0.1, area = 0.3),
            sample(0.7, lateral = 0.3, vertical = 0.1, area = 0.3)
        )
        
        assertEquals(TypingPattern.LEFT_INDEX_FINGER, classifier.classify(samples))
    }
    
    @Test
    fun `the calibration preset detects a modest bias the live default misses`() {
        // A gentle rightward bias of 0.08: below the live 0.18 threshold, above the calibration 0.05 one.
        val samples = listOf(sample(0.3, lateral = 0.08), sample(0.7, lateral = 0.08))
        
        assertEquals(TypingPattern.UNKNOWN, TypingPatternClassifier().classify(samples))
        assertEquals(TypingPattern.LEFT_INDEX_FINGER, TypingPatternClassifier.forCalibration().classify(samples))
    }
    
    @Test
    fun `the calibration preset still needs some data`() {
        val samples = listOf(sample(0.5, lateral = 0.2, count = 5L))
        
        assertEquals(TypingPattern.UNKNOWN, TypingPatternClassifier.forCalibration().classify(samples))
    }
}

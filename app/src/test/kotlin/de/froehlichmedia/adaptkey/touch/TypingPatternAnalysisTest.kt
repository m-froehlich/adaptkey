// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the offset-model-to-pattern bridge (T-04).
 */
class TypingPatternAnalysisTest {
    
    private fun candidate(id: String, centerX: Float): OffsetModel.Candidate {
        return OffsetModel.Candidate(id, centerX, centerY = 200f, halfWidth = 50f, halfHeight = 40f)
    }
    
    @Test
    fun `samples normalise position and deviations and carry contact area`() {
        val model = OffsetModel()
        // 30 taps 15px to the right of centre (100), with a contact area of 0.25.
        repeat(30) { model.record("c:a", 100f, 200f, 115f, 200f, size = 0.25f) }
        
        val geometry = listOf(candidate("c:a", 100f), candidate("c:untrained", 10f))
        val samples = TypingPatternAnalysis.samples(model, geometry, keyboardWidth = 200f)
        
        assertEquals(1, samples.size)
        val s = samples.first()
        assertEquals(0.5, s.normalizedX, 1e-9)
        assertEquals(0.3, s.lateralFraction, 1e-9)
        assertEquals(0.0, s.verticalFraction, 1e-9)
        assertEquals(0.25, s.contactArea ?: Double.NaN, 1e-9)
        assertEquals(30L, s.sampleCount)
    }
    
    @Test
    fun `samples report null contact area without sized taps`() {
        val model = OffsetModel()
        repeat(10) { model.record("c:a", 100f, 200f, 100f, 200f) }
        
        val samples = TypingPatternAnalysis.samples(model, listOf(candidate("c:a", 100f)), keyboardWidth = 200f)
        
        assertNull(samples.first().contactArea)
    }
    
    @Test
    fun `samples are empty for a non-positive keyboard width`() {
        val model = OffsetModel()
        repeat(10) { model.record("c:a", 100f, 200f, 110f, 200f) }
        
        assertTrue(TypingPatternAnalysis.samples(model, listOf(candidate("c:a", 100f)), keyboardWidth = 0f).isEmpty())
    }
    
    @Test
    fun `classify derives the pattern end to end`() {
        val model = OffsetModel()
        // A consistent rightward landing across two keys -> left index finger.
        repeat(30) { model.record("c:a", 100f, 200f, 115f, 200f) }
        repeat(30) { model.record("c:b", 300f, 200f, 315f, 200f) }
        
        val geometry = listOf(candidate("c:a", 100f), candidate("c:b", 300f))
        val pattern = TypingPatternAnalysis.classify(model, geometry, keyboardWidth = 400f)
        
        assertEquals(TypingPattern.LEFT_INDEX_FINGER, pattern)
    }
}

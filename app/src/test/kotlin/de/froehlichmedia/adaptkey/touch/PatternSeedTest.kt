// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

/**
 * Unit tests for the pure D-68 pattern-based touch-zone seeding.
 */
class PatternSeedTest {
    
    // A tiny 3-key row spanning the full keyboard width: left, centre, right.
    private fun row(): List<OffsetModel.Candidate> {
        return listOf(
            OffsetModel.Candidate("c:l", centerX = 20f, centerY = 0f, halfWidth = 20f, halfHeight = 20f),
            OffsetModel.Candidate("c:m", centerX = 60f, centerY = 0f, halfWidth = 20f, halfHeight = 20f),
            OffsetModel.Candidate("c:r", centerX = 100f, centerY = 0f, halfWidth = 20f, halfHeight = 20f)
        )
    }
    
    private fun stdDevX(stat: OffsetModel.Stat?): Double {
        return sqrt(stat!!.m2Dx / (stat.count - 1))
    }
    
    @Test
    fun `unknown pattern seeds nothing`() {
        assertTrue(PatternSeed.seed(TypingPattern.UNKNOWN, row()).isEmpty())
    }
    
    @Test
    fun `empty geometry seeds nothing`() {
        assertTrue(PatternSeed.seed(TypingPattern.LEFT_INDEX_FINGER, emptyList()).isEmpty())
    }
    
    @Test
    fun `left index finger is tight near home and widens toward the far side`() {
        val seed = PatternSeed.seed(TypingPattern.LEFT_INDEX_FINGER, row())
        
        val near = stdDevX(seed["c:l"])
        val far = stdDevX(seed["c:r"])
        assertTrue(far > near, "the far key ($far) must be wider than the home key ($near)")
    }
    
    @Test
    fun `right index finger mirrors the left`() {
        val left = PatternSeed.seed(TypingPattern.LEFT_INDEX_FINGER, row())
        val right = PatternSeed.seed(TypingPattern.RIGHT_INDEX_FINGER, row())
        
        assertEquals(stdDevX(left["c:l"]), stdDevX(right["c:r"]), 1e-9)
        assertEquals(stdDevX(left["c:r"]), stdDevX(right["c:l"]), 1e-9)
    }
    
    @Test
    fun `a thumb is less precise than an index finger even at home`() {
        val index = PatternSeed.seed(TypingPattern.LEFT_INDEX_FINGER, row())
        val thumb = PatternSeed.seed(TypingPattern.LEFT_THUMB, row())
        
        assertTrue(stdDevX(thumb["c:l"]) > stdDevX(index["c:l"]))
    }
    
    @Test
    fun `a thumb struggles more with the top row than the bottom row`() {
        val topRow = listOf(OffsetModel.Candidate("c:t", centerX = 20f, centerY = 0f, halfWidth = 20f, halfHeight = 20f))
        val bottomRow = listOf(OffsetModel.Candidate("c:b", centerX = 20f, centerY = 100f, halfWidth = 20f, halfHeight = 20f))
        val geometry = topRow + bottomRow
        
        val seed = PatternSeed.seed(TypingPattern.LEFT_THUMB, geometry)
        
        val topStd = sqrt(seed["c:t"]!!.m2Dy / (seed["c:t"]!!.count - 1))
        val bottomStd = sqrt(seed["c:b"]!!.m2Dy / (seed["c:b"]!!.count - 1))
        assertTrue(topStd > bottomStd, "the top-row key ($topStd) must be wider than the bottom-row key ($bottomStd)")
    }
    
    @Test
    fun `an index finger's vertical spread does not depend on the row`() {
        val topRow = listOf(OffsetModel.Candidate("c:t", centerX = 20f, centerY = 0f, halfWidth = 20f, halfHeight = 20f))
        val bottomRow = listOf(OffsetModel.Candidate("c:b", centerX = 20f, centerY = 100f, halfWidth = 20f, halfHeight = 20f))
        val geometry = topRow + bottomRow
        
        val seed = PatternSeed.seed(TypingPattern.LEFT_INDEX_FINGER, geometry)
        
        assertEquals(seed["c:t"]!!.m2Dy, seed["c:b"]!!.m2Dy, 1e-9)
    }
    
    @Test
    fun `two thumbs has no positional skew`() {
        val seed = PatternSeed.seed(TypingPattern.TWO_THUMBS, row())
        
        val near = stdDevX(seed["c:l"])
        val far = stdDevX(seed["c:r"])
        val middle = stdDevX(seed["c:m"])
        assertEquals(near, far, 1e-9)
        assertEquals(near, middle, 1e-9)
    }
    
    @Test
    fun `mean offset is always zero`() {
        for (pattern in TypingPattern.entries) {
            if (pattern == TypingPattern.UNKNOWN) {
                continue
            }
            for (stat in PatternSeed.seed(pattern, row()).values) {
                assertEquals(0.0, stat.meanDx, 1e-9)
                assertEquals(0.0, stat.meanDy, 1e-9)
            }
        }
    }
    
    @Test
    fun `the seed count exceeds the model's warmup threshold`() {
        val seed = PatternSeed.seed(TypingPattern.LEFT_INDEX_FINGER, row())
        
        assertTrue(seed.values.first().count > OffsetModel.DEFAULT_WARMUP_SAMPLES)
    }
}

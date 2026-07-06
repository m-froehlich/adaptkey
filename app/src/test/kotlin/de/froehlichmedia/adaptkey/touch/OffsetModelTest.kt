// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the personal offset model (T-03).
 */
class OffsetModelTest {
    
    private fun candidate(id: String, centerX: Float, centerY: Float): OffsetModel.Candidate {
        return OffsetModel.Candidate(id, centerX, centerY, halfWidth = 50f, halfHeight = 50f)
    }
    
    @Test
    fun `record accumulates mean deviation per key`() {
        val model = OffsetModel()
        model.record("c:k", centerX = 10f, centerY = 10f, x = 12f, y = 14f)
        model.record("c:k", centerX = 10f, centerY = 10f, x = 16f, y = 18f)
        
        val stat = model.statFor("c:k")
        assertEquals(2L, stat?.count)
        assertEquals(4.0, stat?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(6.0, stat?.meanDy ?: Double.NaN, 1e-9)
    }
    
    @Test
    fun `record accumulates mean contact area only for sized taps`() {
        val model = OffsetModel()
        model.record("c:k", 0f, 0f, 1f, 1f, size = 0.2f)
        model.record("c:k", 0f, 0f, 1f, 1f, size = 0.4f)
        // A zero-size tap (device reports no contact area) must not drag the mean towards 0.
        model.record("c:k", 0f, 0f, 1f, 1f, size = 0f)
        
        assertEquals(0.3, model.meanContactArea("c:k") ?: Double.NaN, 1e-6)
        assertEquals(3L, model.statFor("c:k")?.count)
        assertEquals(2L, model.statFor("c:k")?.sizeCount)
    }
    
    @Test
    fun `meanContactArea is null without any sized tap`() {
        val model = OffsetModel()
        model.record("c:k", 0f, 0f, 1f, 1f)
        
        assertNull(model.meanContactArea("c:k"))
    }
    
    @Test
    fun `meanContactArea is null for an untrained key`() {
        assertNull(OffsetModel().meanContactArea("c:x"))
    }
    
    @Test
    fun `totalSamples counts taps across all keys`() {
        val model = OffsetModel()
        model.record("c:a", 0f, 0f, 1f, 1f)
        model.record("c:b", 0f, 0f, 1f, 1f)
        model.record("c:b", 0f, 0f, 2f, 2f)
        
        assertEquals(3L, model.totalSamples)
    }
    
    @Test
    fun `statFor returns a defensive copy`() {
        val model = OffsetModel()
        model.record("c:a", 0f, 0f, 5f, 5f)
        
        val copy = model.statFor("c:a")
        copy?.count = 999L
        
        assertEquals(1L, model.statFor("c:a")?.count)
    }
    
    @Test
    fun `statFor is null for an untrained key`() {
        assertNull(OffsetModel().statFor("c:x"))
    }
    
    @Test
    fun `spreadFor reports the mean offset and per-axis standard deviation (D-24)`() {
        val model = OffsetModel()
        // Deviations (2,4) and (6,8): mean (4,6), sample variance 8 on each axis.
        model.record("c:k", 10f, 10f, 12f, 14f)
        model.record("c:k", 10f, 10f, 16f, 18f)
        
        val spread = model.spreadFor("c:k")
        assertEquals(4.0, spread?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(6.0, spread?.meanDy ?: Double.NaN, 1e-9)
        assertEquals(kotlin.math.sqrt(8.0), spread?.stdDevX ?: Double.NaN, 1e-9)
        assertEquals(kotlin.math.sqrt(8.0), spread?.stdDevY ?: Double.NaN, 1e-9)
        assertEquals(2L, spread?.count)
    }
    
    @Test
    fun `spreadFor is null for an untrained key`() {
        assertNull(OffsetModel().spreadFor("c:x"))
    }
    
    @Test
    fun `cappedMeanOffset clamps to the given bounds`() {
        val model = OffsetModel()
        model.record("c:k", 0f, 0f, 1000f, -1000f)
        
        val (dx, dy) = model.cappedMeanOffset("c:k", maxAbsX = 30.0, maxAbsY = 20.0)
        assertEquals(30.0, dx, 1e-9)
        assertEquals(-20.0, dy, 1e-9)
    }
    
    @Test
    fun `cappedMeanOffset is zero for an untrained key`() {
        val (dx, dy) = OffsetModel().cappedMeanOffset("c:k", 50.0, 50.0)
        assertEquals(0.0, dx, 1e-9)
        assertEquals(0.0, dy, 1e-9)
    }
    
    @Test
    fun `resolve returns null for empty candidates`() {
        assertNull(OffsetModel().resolve(emptyList(), 0f, 0f))
    }
    
    @Test
    fun `resolve uses geometric containment during warmup`() {
        val candidates = listOf(candidate("A", 50f, 50f), candidate("B", 150f, 50f))
        
        val chosen = OffsetModel().resolve(candidates, x = 80f, y = 50f)
        assertEquals("A", chosen?.id)
    }
    
    @Test
    fun `resolve falls back to nearest centre for a gap tap during warmup`() {
        val candidates = listOf(candidate("A", 50f, 50f), candidate("B", 150f, 50f))
        
        val chosen = OffsetModel().resolve(candidates, x = 250f, y = 50f)
        assertEquals("B", chosen?.id)
    }
    
    @Test
    fun `resolve compensates a systematic offset after warmup`() {
        val model = OffsetModel()
        val candidates = listOf(candidate("A", 50f, 50f), candidate("B", 150f, 50f))
        
        // The user systematically taps ~30px to the right of the intended key centre.
        repeat(10) { model.record("A", 50f, 50f, 80f, 50f) }
        repeat(10) { model.record("B", 150f, 50f, 180f, 50f) }
        
        // A tap at x=105 lands geometrically inside B, but the learned offset points back to A.
        val chosen = model.resolve(candidates, x = 105f, y = 50f)
        assertEquals("A", chosen?.id)
        assertTrue(model.totalSamples >= OffsetModel.DEFAULT_WARMUP_SAMPLES)
    }
    
    @Test
    fun `snapshot and restore round-trip the statistics`() {
        val source = OffsetModel()
        source.record("c:a", 0f, 0f, 3f, 4f, size = 0.25f)
        source.record("c:a", 0f, 0f, 5f, 6f, size = 0.35f)
        source.record("c:b", 0f, 0f, 1f, 1f)
        
        val target = OffsetModel()
        target.restore(source.snapshot())
        
        val a = target.statFor("c:a")
        assertEquals(2L, a?.count)
        assertEquals(source.statFor("c:a")?.meanDx ?: Double.NaN, a?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(source.statFor("c:a")?.meanDy ?: Double.NaN, a?.meanDy ?: Double.NaN, 1e-9)
        assertEquals(0.3, target.meanContactArea("c:a") ?: Double.NaN, 1e-6)
        assertEquals(3L, target.totalSamples)
    }
    
    @Test
    fun `restore replaces previous contents`() {
        val model = OffsetModel()
        model.record("c:old", 0f, 0f, 1f, 1f)
        
        model.restore(mapOf("c:new" to OffsetModel.Stat(count = 5L, meanDx = 2.0, meanDy = 3.0)))
        
        assertNull(model.statFor("c:old"))
        assertEquals(5L, model.statFor("c:new")?.count)
    }
    
    @Test
    fun `merge copies keys absent from the target`() {
        val target = OffsetModel()
        target.record("c:a", 0f, 0f, 2f, 3f)
        val source = OffsetModel()
        source.record("c:b", 0f, 0f, 4f, 5f)
        
        target.merge(source)
        
        assertEquals(1L, target.statFor("c:a")?.count)
        assertEquals(4.0, target.statFor("c:b")?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(5.0, target.statFor("c:b")?.meanDy ?: Double.NaN, 1e-9)
    }
    
    @Test
    fun `merge of split halves matches a single combined model`() {
        // The same four taps, once recorded into one model and once split across two merged models.
        val single = OffsetModel()
        single.record("c:k", 0f, 0f, 1f, 10f)
        single.record("c:k", 0f, 0f, 3f, 12f)
        single.record("c:k", 0f, 0f, 7f, 18f)
        single.record("c:k", 0f, 0f, 9f, 20f)
        
        val first = OffsetModel()
        first.record("c:k", 0f, 0f, 1f, 10f)
        first.record("c:k", 0f, 0f, 3f, 12f)
        val second = OffsetModel()
        second.record("c:k", 0f, 0f, 7f, 18f)
        second.record("c:k", 0f, 0f, 9f, 20f)
        first.merge(second)
        
        val expected = single.statFor("c:k")
        val merged = first.statFor("c:k")
        assertEquals(expected?.count, merged?.count)
        assertEquals(expected?.meanDx ?: Double.NaN, merged?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(expected?.meanDy ?: Double.NaN, merged?.meanDy ?: Double.NaN, 1e-9)
        assertEquals(expected?.m2Dx ?: Double.NaN, merged?.m2Dx ?: Double.NaN, 1e-9)
        assertEquals(expected?.m2Dy ?: Double.NaN, merged?.m2Dy ?: Double.NaN, 1e-9)
    }
    
    @Test
    fun `merge combines contact area as a sample-count-weighted mean`() {
        val target = OffsetModel()
        target.record("c:k", 0f, 0f, 0f, 0f, size = 0.2f)
        val source = OffsetModel()
        source.record("c:k", 0f, 0f, 0f, 0f, size = 0.4f)
        source.record("c:k", 0f, 0f, 0f, 0f, size = 0.6f)
        
        target.merge(source)
        
        // (0.2*1 + 0.5*2) / 3 = 0.4
        assertEquals(0.4, target.meanContactArea("c:k") ?: Double.NaN, 1e-6)
        assertEquals(3L, target.statFor("c:k")?.sizeCount)
    }
    
    @Test
    fun `merge leaves the source model unchanged`() {
        val target = OffsetModel()
        target.record("c:k", 0f, 0f, 2f, 2f)
        val source = OffsetModel()
        source.record("c:k", 0f, 0f, 4f, 4f)
        
        target.merge(source)
        
        assertEquals(1L, source.statFor("c:k")?.count)
        assertEquals(4.0, source.statFor("c:k")?.meanDx ?: Double.NaN, 1e-9)
    }
}

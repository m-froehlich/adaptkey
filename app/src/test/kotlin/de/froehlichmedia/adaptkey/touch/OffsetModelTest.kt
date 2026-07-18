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
    fun `D-159 the first sample always sets the mean exactly, before any weighting can apply`() {
        // weight/weightSum == 1 for a key's very first sample regardless of the computed weight value
        // (nothing to compare it against yet) - this is what keeps every single-sample test elsewhere in
        // this file (cappedMeanOffset, merge, etc.) exact even after D-159.
        val model = OffsetModel()
        model.record("c:k", centerX = 10f, centerY = 10f, x = 12f, y = 14f)
        
        val stat = model.statFor("c:k")
        assertEquals(1L, stat?.count)
        assertEquals(2.0, stat?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(4.0, stat?.meanDy ?: Double.NaN, 1e-9)
    }
    
    @Test
    fun `D-159 a wildly off second sample is downweighted - the mean moves less than a naive average would`() {
        val model = OffsetModel()
        model.record("c:k", centerX = 10f, centerY = 10f, x = 12f, y = 14f) // dx=2 -> mean exactly 2.0
        model.record("c:k", centerX = 10f, centerY = 10f, x = 500f, y = 10f) // dx=490, a wild outlier
        
        val meanDx = model.statFor("c:k")?.meanDx ?: Double.NaN
        val naiveAverage = (2.0 + 490.0) / 2.0
        assertTrue(meanDx < naiveAverage, "the outlier must be discounted, not averaged in at full weight: $meanDx")
        assertTrue(meanDx > 2.0, "the mean must still move somewhat towards the new sample, just less: $meanDx")
    }
    
    @Test
    fun `D-159 repeated identical deviations are never downweighted against themselves`() {
        // Only the very first sample can be discounted at all (compared against the fresh, still-zero
        // mean) - every sample after it exactly matches the already-established mean (r=0), so weight
        // stays exactly 1.0 for the rest, and the mean itself is unaffected regardless of the first
        // sample's own weight (see "the first sample always sets the mean exactly" above). Used
        // deliberately by several other tests in this file (resolve/rankedCandidates) to stay exact
        // under D-159.
        val model = OffsetModel()
        repeat(10) { model.record("c:k", centerX = 0f, centerY = 0f, x = 30f, y = 0f) }
        
        assertEquals(30.0, model.statFor("c:k")?.meanDx ?: Double.NaN, 1e-9)
        // 9 of the 10 samples are guaranteed weight 1.0 each (see above); only the very first can be
        // below that, so the sum must exceed 9.0 - loosely bounded rather than an exact literal, since
        // the first sample's own weight depends on MIN_SAMPLE_WEIGHT/DEFAULT_SIGMA_FACTOR's exact values.
        assertTrue((model.statFor("c:k")?.weightSum ?: 0.0) > 9.0)
    }
    
    @Test
    fun `D-159 a sustained run of similarly off-centre samples still pulls the mean there over time`() {
        val model = OffsetModel()
        model.record("c:k", centerX = 0f, centerY = 0f, x = 5f, y = 0f) // establishes a small initial mean
        repeat(200) { model.record("c:k", centerX = 0f, centerY = 0f, x = 200f, y = 0f) }
        
        // Even heavily discounted (the MIN_SAMPLE_WEIGHT floor), 200 consistent samples must eventually
        // dominate a single earlier one - persistent drift is still learnable, just more gradually.
        val meanDx = model.statFor("c:k")?.meanDx ?: Double.NaN
        assertTrue(meanDx > 150.0, "a long enough consistent run must still move the mean close to it: $meanDx")
    }
    
    @Test
    fun `unrecord reverses a single record call back to an untrained key`() {
        val model = OffsetModel()
        val weight = model.record("c:k", centerX = 10f, centerY = 10f, x = 12f, y = 14f)
        model.unrecord("c:k", centerX = 10f, centerY = 10f, x = 12f, y = 14f, weight = weight)
        
        assertNull(model.statFor("c:k"))
    }
    
    @Test
    fun `unrecord restores the exact prior mean and variance among several samples`() {
        val model = OffsetModel()
        model.record("c:k", 10f, 10f, 12f, 14f)
        model.record("c:k", 10f, 10f, 16f, 18f)
        val before = model.statFor("c:k")!!
        
        val weight3 = model.record("c:k", 10f, 10f, 9f, 20f)
        model.unrecord("c:k", 10f, 10f, 9f, 20f, weight = weight3)
        val after = model.statFor("c:k")!!
        
        assertEquals(before.count, after.count)
        assertEquals(before.weightSum, after.weightSum, 1e-9)
        assertEquals(before.meanDx, after.meanDx, 1e-9)
        assertEquals(before.meanDy, after.meanDy, 1e-9)
        assertEquals(before.m2Dx, after.m2Dx, 1e-9)
        assertEquals(before.m2Dy, after.m2Dy, 1e-9)
    }
    
    @Test
    fun `unrecord on an untrained key is a harmless no-op`() {
        val model = OffsetModel()
        model.unrecord("c:x", 0f, 0f, 1f, 1f, weight = 1.0)
        
        assertNull(model.statFor("c:x"))
    }
    
    @Test
    fun `unrecord also reverses the contact-area mean`() {
        val model = OffsetModel()
        model.record("c:k", 0f, 0f, 1f, 1f, size = 0.2f)
        // Same (dx, dy) as the first sample, so weight is exactly 1.0 (see the "repeated identical
        // deviations" test above) - kept exact deliberately, this test is about the contact-area mean.
        val weight2 = model.record("c:k", 0f, 0f, 1f, 1f, size = 0.4f)
        model.unrecord("c:k", 0f, 0f, 1f, 1f, weight = weight2, size = 0.4f)
        
        // Float->Double promotion of the size argument loses precision below 1e-6, same established
        // tolerance as the existing contact-area tests just below (see this project's own documented gotcha).
        assertEquals(0.2, model.meanContactArea("c:k") ?: Double.NaN, 1e-6)
        assertEquals(1L, model.statFor("c:k")?.count)
        assertEquals(1L, model.statFor("c:k")?.sizeCount)
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
    fun `spreadFor reports the mean offset and standard deviation for repeated identical taps (D-24)`() {
        val model = OffsetModel()
        // Every tap after the first exactly matches the already-established mean (weight stays 1.0
        // throughout, see the "D-159 repeated identical deviations" test above) - keeps this a precise,
        // hand-computable check even after D-159's weighting.
        repeat(3) { model.record("c:k", 10f, 10f, 12f, 14f) }
        
        val spread = model.spreadFor("c:k")
        assertEquals(2.0, spread?.meanDx ?: Double.NaN, 1e-9)
        assertEquals(4.0, spread?.meanDy ?: Double.NaN, 1e-9)
        assertEquals(0.0, spread?.stdDevX ?: Double.NaN, 1e-9)
        assertEquals(0.0, spread?.stdDevY ?: Double.NaN, 1e-9)
        assertEquals(3L, spread?.count)
    }
    
    @Test
    fun `D-159 spreadFor's standard deviation uses weightSum, not the plain count, as its divisor`() {
        val model = OffsetModel()
        model.record("c:k", 10f, 10f, 12f, 14f) // dx=2, dy=4 -> mean exactly (2,4)
        model.record("c:k", 10f, 10f, 16f, 18f) // dx=6, dy=8, downweighted relative to (2,4)
        
        val stat = model.statFor("c:k")!!
        val spread = model.spreadFor("c:k")!!
        // The stored m2/weightSum are exactly what spreadFor must divide - cross-checks the formula
        // directly against the model's own already-exposed state, not a hand-derived literal.
        assertEquals(kotlin.math.sqrt(stat.m2Dx / (stat.weightSum - 1.0)), spread.stdDevX, 1e-9)
        assertEquals(kotlin.math.sqrt(stat.m2Dy / (stat.weightSum - 1.0)), spread.stdDevY, 1e-9)
        assertTrue(stat.weightSum < 2.0, "the second sample must have been downweighted below full weight")
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
    fun `D-133 cappedMeanOffset applies a tighter downward bound when given one`() {
        val model = OffsetModel()
        model.record("c:k", 0f, 0f, 0f, 1000f)
        
        val (_, dyDefault) = model.cappedMeanOffset("c:k", maxAbsX = 10.0, maxAbsY = 20.0)
        assertEquals(20.0, dyDefault, 1e-9)
        
        val (_, dyDown) = model.cappedMeanOffset("c:k", maxAbsX = 10.0, maxAbsY = 20.0, maxAbsYDown = 5.0)
        assertEquals(5.0, dyDown, 1e-9)
    }
    
    @Test
    fun `D-133 cappedMeanOffset's downward-only bound does not affect an upward deviation`() {
        val model = OffsetModel()
        model.record("c:k", 0f, 0f, 0f, -1000f)
        
        val (_, dy) = model.cappedMeanOffset("c:k", maxAbsX = 10.0, maxAbsY = 20.0, maxAbsYDown = 5.0)
        assertEquals(-20.0, dy, 1e-9)
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
    fun `D-133 a candidate's own maxDownwardOffsetFactor tightens its downward cap independently of maxOffsetFactor`() {
        val model = OffsetModel()
        // A trains a large, consistent systematic downward drift (mean dy = 100) - well past either cap
        // considered below, so both scenarios clamp; only how much differs.
        repeat(30) { model.record("A", centerX = 0f, centerY = 0f, x = 0f, y = 100f) }
        val bareA = OffsetModel.Candidate("A", 0f, 0f, halfWidth = 20f, halfHeight = 20f)
        val cappedA = OffsetModel.Candidate("A", 0f, 0f, halfWidth = 20f, halfHeight = 20f, maxDownwardOffsetFactor = 0.05)
        val b = OffsetModel.Candidate("B", 0f, 200f, halfWidth = 20f, halfHeight = 20f)
        
        // Ordinary isotropic cap (0.5): A's predicted centre moves to y=10 - still close enough to beat
        // B's own (untrained, unmoved) home for a tap at y=50.
        assertEquals("A", model.resolve(listOf(bareA, b), x = 0f, y = 50f)?.id)
        // The tighter, direction-specific cap (0.05): A's predicted centre only reaches y=1, now further
        // from the tap than B's home - B must win instead.
        assertEquals("B", model.resolve(listOf(cappedA, b), x = 0f, y = 50f)?.id)
    }
    
    
    @Test
    fun `rankedCandidates orders by distance during warmup - D-39`() {
        val candidates = listOf(candidate("A", 50f, 50f), candidate("B", 150f, 50f), candidate("C", 250f, 50f))
        
        val ranked = OffsetModel().rankedCandidates(candidates, x = 80f, y = 50f)
        
        assertEquals(listOf("A", "B", "C"), ranked.map { it.first.id })
    }
    
    @Test
    fun `rankedCandidates orders by learned likelihood after warmup - D-39`() {
        val model = OffsetModel()
        val candidates = listOf(candidate("A", 50f, 50f), candidate("B", 150f, 50f))
        // The user systematically taps ~30px to the right of the intended key centre (same setup as the
        // "resolve compensates a systematic offset" test above).
        repeat(10) { model.record("A", 50f, 50f, 80f, 50f) }
        repeat(10) { model.record("B", 150f, 50f, 180f, 50f) }
        
        // A tap at x=105 lands geometrically inside B, but the runner-up under the learned model is still A.
        val ranked = model.rankedCandidates(candidates, x = 105f, y = 50f)
        
        assertEquals(listOf("A", "B"), ranked.map { it.first.id })
        // Scores are comparable and best-first.
        assertTrue(ranked[0].second >= ranked[1].second)
    }
    
    @Test
    fun `rankedCandidates handles an empty candidate list - D-39`() {
        assertTrue(OffsetModel().rankedCandidates(emptyList(), 0f, 0f).isEmpty())
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
    
    // D-159: the previous "merge of split halves matches a single combined model" test is gone, not
    // merely updated - that equivalence (recording a stream sequentially == splitting it and merging the
    // halves), exact under the old plain Welford update, no longer holds *at all* once per-sample
    // weighting depends on each side's own independent running state: each branch's own first sample is
    // bootstrapped against a fresh zero mean, so splitting a stream changes how many "first samples"
    // occur, even when every tap shares an identical deviation. This is an inherent, expected consequence
    // of introducing robust per-sample downweighting (a well-known property of recursive robust
    // estimators in general), not a bug - see spec.md §98 for the full writeup. combine() still performs
    // a correct weighted parallel combination of two already-computed Stat objects, verified directly
    // below.
    @Test
    fun `D-159 merge weighs each side by its own effective weightSum, not the plain unweighted mean`() {
        // Two independently-recorded single-sample stats with deliberately different weightSums (a's own
        // tap exactly matches its fresh zero mean, weight 1.0; b's tap does not, weight below 1.0) - a
        // naive unweighted average of the two means would differ measurably from the correct
        // weightSum-weighted one, so this genuinely exercises combine()'s own formula.
        val a = OffsetModel()
        a.record("c:k", 0f, 0f, 0f, 0f)
        val aStat = a.statFor("c:k")!!
        val b = OffsetModel()
        b.record("c:k", 0f, 0f, 10f, 0f)
        val bStat = b.statFor("c:k")!!
        assertTrue(bStat.weightSum < 1.0, "b's own tap must have been downweighted below full weight")
        
        a.merge(b)
        val merged = a.statFor("c:k")!!
        
        val expectedWeightSum = aStat.weightSum + bStat.weightSum
        val expectedMeanDx = (aStat.meanDx * aStat.weightSum + bStat.meanDx * bStat.weightSum) / expectedWeightSum
        assertEquals(expectedWeightSum, merged.weightSum, 1e-9)
        assertEquals(expectedMeanDx, merged.meanDx, 1e-9)
        assertEquals(aStat.count + bStat.count, merged.count)
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

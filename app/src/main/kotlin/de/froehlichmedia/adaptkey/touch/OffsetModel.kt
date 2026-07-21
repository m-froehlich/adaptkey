// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Personal 2D offset model (T-03) - typing-style agnostic.
 *
 * For every confirmed tap the model accumulates the deviation `(dx, dy) = (touch - keyCentre)`
 * per key as an incremental 2D Gaussian (weighted Welford mean and variance, D-159). The learned
 * per-key mean compensates for the user's systematic deviation; [resolve] then picks the candidate
 * whose compensated Gaussian best explains a new tap. The model makes no assumption about
 * handedness or finger - it learns purely from observed deviations and improves continuously.
 *
 * D-159: each recorded tap is downweighted by how far it falls from the key's *currently learned*
 * expected strike point, relative to the currently learned spread (the same Gaussian [logLikelihood]
 * itself already scores candidates with) - a single wildly-off but still-resolved tap can no longer
 * yank the mean as hard as an ordinary, consistent one, while a *sustained* run of off-centre taps
 * still pulls the zone there over time (each one still contributes at least [MIN_SAMPLE_WEIGHT]).
 * This is standard robust (Huber-style) downweighting, not a heuristic invention - and needs no
 * separate "not enough data yet" gate of its own: it reuses [variance]'s own existing fallback
 * (the learned/seeded spread once available, otherwise a geometric estimate from the key's own
 * size), so an initial K-01 calibration seed is trusted as "real" spread immediately, exactly as
 * [resolve] already treats it (the seed's sample count already clears [warmupSamples] on its own).
 *
 * While fewer than [warmupSamples] taps have been seen, [resolve] falls back to plain geometry
 * so early typing behaves predictably. The learned mean offset is capped to a fraction of the
 * key size ([maxOffsetFactor]) so the resolution cannot run away from the physical key.
 *
 * The class is intentionally free of Android dependencies so it can be unit-tested on the JVM;
 * persistence is delegated to {@link OffsetStore}.
 *
 * Note: until word-level confirmation (T-02) and the retroactive correction rules (A-05 / A-06)
 * exist, the "confirmed key" is simply the key this model resolved. Capping and the variance
 * floor keep that feedback bounded; a stronger confirmation signal will replace it later.
 *
 * @property maxOffsetFactor the learned mean offset is capped to this fraction of the key half-size
 * @property warmupSamples number of recorded taps below which [resolve] uses pure geometry
 */
class OffsetModel(
    val maxOffsetFactor: Double = DEFAULT_MAX_OFFSET_FACTOR,
    private val warmupSamples: Long = DEFAULT_WARMUP_SAMPLES
) {
    
    /**
     * Incremental sufficient statistics for one key's deviation distribution.
     *
     * @property weightSum D-159: the running sum of per-sample weights [record] actually applied -
     *        the true "effective sample size" the weighted [meanDx]/[meanDy]/[m2Dx]/[m2Dy] were
     *        accumulated against, distinct from [count] (the plain number of taps recorded, used for
     *        [warmupSamples]/readiness gating). Defaults to matching [count] - i.e. "every recorded
     *        sample had full weight" - the correct assumption for a [Stat] built by anything that
     *        does not think about weighting at all (older persisted data, hand-built seeds/tests).
     */
    data class Stat(
        var count: Long = 0L,
        var weightSum: Double = count.toDouble(),
        var meanDx: Double = 0.0,
        var meanDy: Double = 0.0,
        var m2Dx: Double = 0.0,
        var m2Dy: Double = 0.0,
        var sizeCount: Long = 0L,
        var meanSize: Double = 0.0
    )
    
    /**
     * A candidate key with the geometry needed to score and record a tap.
     *
     * @property maxDownwardOffsetFactor D-133: overrides [maxOffsetFactor] for this candidate's downward
     *           (+y) learned offset only, when set - e.g. the bottom letter row's own harder,
     *           direction-specific bound against drifting toward the space bar below it (D-109 already
     *           bounds every axis isotropically; this tightens one direction further, for these keys only).
     *           Upward and both horizontal bounds are unaffected. Null (the default) means "use
     *           [maxOffsetFactor], same as every other direction".
     * @property maxUpwardOffsetFactor D-231: the same idea, mirrored for the upward (-y) direction - e.g.
     *           the Enter key's own harder bound against drifting up into Backspace's row above it (the two
     *           sit directly on top of each other at the row's right edge). Downward and both horizontal
     *           bounds are unaffected. Null (the default) means "use [maxOffsetFactor]".
     */
    data class Candidate(
        val id: String,
        val centerX: Float,
        val centerY: Float,
        val halfWidth: Float,
        val halfHeight: Float,
        val maxDownwardOffsetFactor: Double? = null,
        val maxUpwardOffsetFactor: Double? = null
    )
    
    private val stats = HashMap<String, Stat>()
    
    /** Total number of taps recorded across all keys. */
    val totalSamples: Long
        get() = stats.values.sumOf { it.count }
    
    /**
     * @param id the key id
     * @return a defensive copy of the statistics for [id], or null if the key is untrained
     */
    fun statFor(id: String): Stat? {
        return stats[id]?.copy()
    }
    
    /**
     * Records a confirmed tap, updating the key's running mean and variance (weighted Welford, D-159).
     *
     * D-159: the sample is weighted by [weightFor] before being folded in - a tap far from the key's
     * *currently learned* expected strike point (relative to its currently learned spread) contributes
     * less than a tap close to it, so one wild outlier can no longer swing the mean as hard as an
     * ordinary one; a sustained run of similarly-off taps still pulls the mean there over time, just
     * more gradually. [halfWidth]/[halfHeight] feed only the geometric fallback spread used before any
     * real (or seeded) variance exists for this key - once one does, it is used instead, exactly as
     * [logLikelihood] already does at resolve time.
     *
     * The optional contact area ([size], from {@code MotionEvent.getSize()}) feeds a separate running
     * mean used by the typing-pattern detection (T-04). It is only accumulated when strictly positive,
     * so devices that report no contact size simply leave [Stat.sizeCount] at zero and the pattern
     * detection falls back gracefully. Contact area is never weighted - only the positional deviation.
     *
     * @param id the confirmed key's id
     * @param centerX the key centre x in view pixels
     * @param centerY the key centre y in view pixels
     * @param x the raw tap x (T-01 ACTION_DOWN)
     * @param y the raw tap y (T-01 ACTION_DOWN)
     * @param size the normalised contact area of the tap, or 0 when unavailable (T-04)
     * @param halfWidth the key's own half-width in view pixels, for the geometric fallback spread only
     * @param halfHeight the key's own half-height in view pixels, for the geometric fallback spread only
     * @return the weight actually applied to this sample (D-159/D-140) - callers that may later need to
     *         [unrecord] this exact sample must retain it, since it cannot be re-derived afterwards once
     *         further taps have moved the key's mean/variance on
     */
    fun record(
        id: String,
        centerX: Float,
        centerY: Float,
        x: Float,
        y: Float,
        size: Float = 0f,
        halfWidth: Float = DEFAULT_FALLBACK_HALF_SIZE,
        halfHeight: Float = DEFAULT_FALLBACK_HALF_SIZE
    ): Double {
        val stat = stats.getOrPut(id) { Stat() }
        val dx = (x - centerX).toDouble()
        val dy = (y - centerY).toDouble()
        val weight = weightFor(stat, dx, dy, halfWidth, halfHeight)
        stat.count += 1L
        stat.weightSum += weight
        val deltaX = dx - stat.meanDx
        stat.meanDx += (weight / stat.weightSum) * deltaX
        stat.m2Dx += weight * deltaX * (dx - stat.meanDx)
        val deltaY = dy - stat.meanDy
        stat.meanDy += (weight / stat.weightSum) * deltaY
        stat.m2Dy += weight * deltaY * (dy - stat.meanDy)
        if (size > 0f) {
            stat.sizeCount += 1L
            stat.meanSize += (size.toDouble() - stat.meanSize) / stat.sizeCount
        }
        return weight
    }
    
    /**
     * D-159: the robust downweighting kernel - the (unnormalised) Gaussian likelihood ratio of `(dx, dy)`
     * under the key's own currently learned deviation model, i.e. exactly the same statistical framework
     * [logLikelihood] already scores candidates with, reused here as "how surprising is this one sample
     * given what we already believe about this key". Floored at [MIN_SAMPLE_WEIGHT] so a persistent run
     * of similarly-placed taps still moves the mean there over time rather than being discounted forever.
     */
    private fun weightFor(stat: Stat, dx: Double, dy: Double, halfWidth: Float, halfHeight: Float): Double {
        val varX = variance(stat.m2Dx, stat.count, stat.weightSum, defaultVariance(halfWidth))
        val varY = variance(stat.m2Dy, stat.count, stat.weightSum, defaultVariance(halfHeight))
        val rx = dx - stat.meanDx
        val ry = dy - stat.meanDy
        val r2 = (rx * rx) / varX + (ry * ry) / varY
        return max(MIN_SAMPLE_WEIGHT, exp(-0.5 * r2))
    }
    
    /**
     * D-140 / D-159: reverses exactly one prior [record] call for [id] - the exact algebraic inverse of
     * the *weighted* Welford update, not a heuristic. Used to un-train the model for a tap that later
     * turned out to be a genuine touch-resolution mistake (D-39 raw-coordinate correction), so a rejected
     * correction does not leave the model permanently reinforced towards the very key that was actually
     * wrong. A no-op when [id] has no recorded samples at all. Must be called with the exact same
     * arguments the corresponding [record] call used, [weight] included - it is the caller's
     * responsibility to have retained them (neither the sample to remove nor the weight it was originally
     * given can be inferred afterwards, once further taps have moved the key's mean/variance on).
     *
     * @param id the key id whose most recent matching sample is reversed
     * @param centerX the key centre x in view pixels, as passed to the original [record] call
     * @param centerY the key centre y in view pixels, as passed to the original [record] call
     * @param x the raw tap x, as passed to the original [record] call
     * @param y the raw tap y, as passed to the original [record] call
     * @param weight D-159: the weight [record] returned for this exact sample
     * @param size the contact area, as passed to the original [record] call
     */
    fun unrecord(id: String, centerX: Float, centerY: Float, x: Float, y: Float, weight: Double, size: Float = 0f) {
        val stat = stats[id] ?: return
        if (stat.count <= 0L) {
            return
        }
        val dx = (x - centerX).toDouble()
        val dy = (y - centerY).toDouble()
        val n0 = stat.count - 1L
        if (n0 == 0L) {
            // The last remaining sample for this key is being removed - drop the entry entirely so
            // statFor()/isKnownWord-style callers see an untrained key again, indistinguishable from one
            // that was never recorded at all, rather than a lingering all-zero Stat.
            stats.remove(id)
            return
        }
        val weightSum1 = stat.weightSum
        val weightSum0 = weightSum1 - weight
        val mean1X = stat.meanDx
        val mean0X = (mean1X * weightSum1 - weight * dx) / weightSum0
        stat.m2Dx -= weight * (dx - mean0X) * (dx - mean1X)
        stat.meanDx = mean0X
        val mean1Y = stat.meanDy
        val mean0Y = (mean1Y * weightSum1 - weight * dy) / weightSum0
        stat.m2Dy -= weight * (dy - mean0Y) * (dy - mean1Y)
        stat.meanDy = mean0Y
        stat.count = n0
        stat.weightSum = weightSum0
        if (size > 0f && stat.sizeCount > 0L) {
            val n1Size = stat.sizeCount
            val n0Size = n1Size - 1L
            if (n0Size == 0L) {
                stat.sizeCount = 0L
                stat.meanSize = 0.0
            } else {
                stat.meanSize = (stat.meanSize * n1Size - size.toDouble()) / n0Size
                stat.sizeCount = n0Size
            }
        }
    }
    
    /** The learned strike spread for a key (D-24): mean offset from the key centre and its std deviation. */
    data class Spread(
        val meanDx: Double,
        val meanDy: Double,
        val stdDevX: Double,
        val stdDevY: Double,
        val count: Long
    )
    
    /**
     * The learned strike spread for a key (D-24 touch-pattern visualisation): where the user's taps land
     * relative to the key centre (mean offset) and how scattered they are (per-axis standard deviation).
     *
     * @param id the key id
     * @return the spread, or null when the key is untrained
     */
    fun spreadFor(id: String): Spread? {
        val stat = stats[id]
        if (stat == null || stat.count == 0L) {
            return null
        }
        // D-159: m2Dx/m2Dy are now weighted sums of squares, so weightSum (not the plain sample count) is
        // their correct normalising denominator.
        val stdX = if (stat.weightSum > 1.0) sqrt(stat.m2Dx / (stat.weightSum - 1.0)) else 0.0
        val stdY = if (stat.weightSum > 1.0) sqrt(stat.m2Dy / (stat.weightSum - 1.0)) else 0.0
        return Spread(stat.meanDx, stat.meanDy, stdX, stdY, stat.count)
    }
    
    /**
     * The mean contact area learned for a key (T-04), or null when no sized taps have been seen.
     *
     * @param id the key id
     * @return the mean {@code MotionEvent.getSize()} for the key, or null when unavailable
     */
    fun meanContactArea(id: String): Double? {
        val stat = stats[id]
        if (stat == null || stat.sizeCount == 0L) {
            return null
        }
        return stat.meanSize
    }
    
    /**
     * The learned mean offset for a key, clamped to the given absolute bounds.
     *
     * @param id the key id
     * @param maxAbsX maximum absolute x offset to return
     * @param maxAbsY maximum absolute y offset to return, in the upward (negative) direction
     * @param maxAbsYDown D-133: maximum absolute y offset in the downward (positive) direction, when a
     *        harder direction-specific bound applies (e.g. the bottom letter row); defaults to [maxAbsY],
     *        i.e. the ordinary symmetric bound, for every existing caller
     * @return the clamped (dx, dy) mean offset, or (0, 0) when the key is untrained
     */
    fun cappedMeanOffset(id: String, maxAbsX: Double, maxAbsY: Double, maxAbsYDown: Double = maxAbsY): Pair<Double, Double> {
        val stat = stats[id]
        if (stat == null || stat.count == 0L) {
            return 0.0 to 0.0
        }
        return stat.meanDx.coerceIn(-maxAbsX, maxAbsX) to stat.meanDy.coerceIn(-maxAbsY, maxAbsYDown)
    }
    
    /**
     * Resolves a tap to the most likely candidate.
     *
     * During warm-up (fewer than [warmupSamples] total taps) this returns the geometric result:
     * the containing candidate, or the nearest centre when the tap falls in a gap. Afterwards it
     * returns the candidate with the highest 2D-Gaussian log-likelihood under the compensated,
     * per-key model.
     *
     * @param candidates the keys eligible for this tap; must not be empty for a non-null result
     * @param x the raw tap x
     * @param y the raw tap y
     * @return the chosen candidate, or null if [candidates] is empty
     */
    fun resolve(candidates: List<Candidate>, x: Float, y: Float): Candidate? {
        if (candidates.isEmpty()) {
            return null
        }
        if (totalSamples < warmupSamples) {
            return geometric(candidates, x, y)
        }
        var best: Candidate? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (candidate in candidates) {
            val score = logLikelihood(candidate, x, y)
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best ?: geometric(candidates, x, y)
    }
    
    /**
     * Ranks every candidate for a tap, best (most likely) first, with a comparable score (D-39): the same
     * scoring [resolve] uses internally - the compensated 2D-Gaussian log-likelihood once the model is
     * warmed up, or the negated squared distance to the centre during warm-up (so "higher is better" holds
     * in both regimes). Unlike [resolve], which only returns the single best match, this exposes the full
     * ordering so a caller can find the *runner-up* - the key a tap could plausibly have meant instead of
     * the one actually chosen - e.g. for raw-coordinate correction of an unknown word.
     *
     * @param candidates the keys to rank; may be empty
     * @param x the raw tap x
     * @param y the raw tap y
     * @return [candidates] paired with their score, sorted best first
     */
    fun rankedCandidates(candidates: List<Candidate>, x: Float, y: Float): List<Pair<Candidate, Double>> {
        if (totalSamples < warmupSamples) {
            return candidates
                .map { c ->
                    val dx = (x - c.centerX).toDouble()
                    val dy = (y - c.centerY).toDouble()
                    c to -(dx * dx + dy * dy)
                }
                .sortedByDescending { it.second }
        }
        return candidates.map { it to logLikelihood(it, x, y) }.sortedByDescending { it.second }
    }
    
    /** @return a defensive copy of all per-key statistics, keyed by key id */
    fun snapshot(): Map<String, Stat> {
        return stats.mapValues { entry -> entry.value.copy() }
    }
    
    /**
     * Replaces the model's contents with [data] (used when loading persisted statistics).
     *
     * @param data per-key statistics to load; copied defensively
     */
    fun restore(data: Map<String, Stat>) {
        stats.clear()
        data.forEach { (id, stat) -> stats[id] = stat.copy() }
    }
    
    /**
     * Folds another model's statistics into this one (K-01 calibration seeding).
     *
     * Per key the sufficient statistics are combined with the parallel (Chan) form of Welford's
     * algorithm, so merging a freshly calibrated model into the persisted one is mathematically
     * identical to having recorded every tap in a single model - this lets a repeated calibration
     * add to the learned data instead of discarding it. Keys present only in [other] are copied in;
     * the contact-area means (T-04) are combined as sample-count-weighted averages.
     *
     * @param other the model whose statistics are folded in; it is left unchanged
     */
    fun merge(other: OffsetModel) {
        for ((id, incoming) in other.snapshot()) {
            val existing = stats[id]
            stats[id] = if (existing == null) incoming.copy() else combine(existing, incoming)
        }
    }
    
    private fun combine(a: Stat, b: Stat): Stat {
        val count = a.count + b.count
        if (count == 0L) {
            return Stat()
        }
        // D-159: the parallel merge weighs each side by its own weightSum (the true effective mass its
        // mean/m2 were accumulated against), not the plain sample count - the unweighted case (every
        // weightSum equal to its count) reduces to exactly the original Chan formula.
        val weightSum = a.weightSum + b.weightSum
        val deltaX = b.meanDx - a.meanDx
        val deltaY = b.meanDy - a.meanDy
        val meanDx = if (weightSum > 0.0) a.meanDx + deltaX * b.weightSum / weightSum else a.meanDx
        val meanDy = if (weightSum > 0.0) a.meanDy + deltaY * b.weightSum / weightSum else a.meanDy
        val m2Dx = a.m2Dx + b.m2Dx + if (weightSum > 0.0) deltaX * deltaX * a.weightSum * b.weightSum / weightSum else 0.0
        val m2Dy = a.m2Dy + b.m2Dy + if (weightSum > 0.0) deltaY * deltaY * a.weightSum * b.weightSum / weightSum else 0.0
        val sizeCount = a.sizeCount + b.sizeCount
        val meanSize = if (sizeCount == 0L) 0.0 else (a.meanSize * a.sizeCount + b.meanSize * b.sizeCount) / sizeCount
        return Stat(count, weightSum, meanDx, meanDy, m2Dx, m2Dy, sizeCount, meanSize)
    }
    
    private fun geometric(candidates: List<Candidate>, x: Float, y: Float): Candidate {
        val containing = candidates.firstOrNull { c ->
            x >= c.centerX - c.halfWidth && x <= c.centerX + c.halfWidth &&
                y >= c.centerY - c.halfHeight && y <= c.centerY + c.halfHeight
        }
        if (containing != null) {
            return containing
        }
        return candidates.minBy { c ->
            val dx = x - c.centerX
            val dy = y - c.centerY
            dx * dx + dy * dy
        }
    }
    
    private fun logLikelihood(candidate: Candidate, x: Float, y: Float): Double {
        val stat = stats[candidate.id]
        val capX = candidate.halfWidth * maxOffsetFactor
        // D-133/D-231: a candidate may declare a harder, direction-specific bound on its own upward or
        // downward drift.
        val capYUp = candidate.halfHeight * (candidate.maxUpwardOffsetFactor ?: maxOffsetFactor)
        val capYDown = candidate.halfHeight * (candidate.maxDownwardOffsetFactor ?: maxOffsetFactor)
        val offsetX = (stat?.meanDx ?: 0.0).coerceIn(-capX, capX)
        val offsetY = (stat?.meanDy ?: 0.0).coerceIn(-capYUp, capYDown)
        val predictedX = candidate.centerX + offsetX
        val predictedY = candidate.centerY + offsetY
        val varX = variance(stat?.m2Dx, stat?.count, stat?.weightSum, defaultVariance(candidate.halfWidth))
        val varY = variance(stat?.m2Dy, stat?.count, stat?.weightSum, defaultVariance(candidate.halfHeight))
        val ddx = x - predictedX
        val ddy = y - predictedY
        return -0.5 * (ddx * ddx / varX + ddy * ddy / varY) - 0.5 * (ln(varX) + ln(varY))
    }
    
    /**
     * @param weightSum D-159: the weighted effective sample size the M2 sum was actually accumulated
     *        against - the correct normalising denominator once trusted, distinct from [count] (which
     *        still gates *whether* to trust a computed variance at all, since that is a question of "have
     *        we observed enough real taps", not "how much effective weight do they carry").
     */
    private fun variance(m2: Double?, count: Long?, weightSum: Double?, default: Double): Double {
        if (m2 != null && count != null && count >= MIN_VARIANCE_SAMPLES && weightSum != null && weightSum > 1.0) {
            val sampleVariance = m2 / (weightSum - 1.0)
            return max(sampleVariance, default * VARIANCE_FLOOR_FRACTION)
        }
        return default
    }
    
    private fun defaultVariance(half: Float): Double {
        val sigma = max(half.toDouble(), 1.0) * DEFAULT_SIGMA_FACTOR
        return sigma * sigma
    }
    
    companion object {
        
        // D-109: tightened from 0.9 (a learned zone could sit within 10% of a key's edge, effectively
        // crowding into the neighbouring key's own territory) - a starting, more conservative point per
        // the reported J-key overdrift; not yet device-tuned further, easy to retune later (single
        // constant, same precedent as the various sound/gesture threshold tunings this project has had).
        /** Default cap on the learned mean offset, as a fraction of the key half-size. */
        const val DEFAULT_MAX_OFFSET_FACTOR = 0.5
        
        /** Default number of taps before the model overrides plain geometry. */
        const val DEFAULT_WARMUP_SAMPLES = 20L
        
        private const val MIN_VARIANCE_SAMPLES = 5L
        private const val VARIANCE_FLOOR_FRACTION = 0.1
        private const val DEFAULT_SIGMA_FACTOR = 0.6
        
        // D-159: a sample is never discounted below this fraction of full weight - a persistent run of
        // similarly-off taps still pulls the mean there over time (just more gradually than an unweighted
        // model would), rather than being discounted forever. Not yet device-tuned, easy to retune later
        // (same precedent as DEFAULT_MAX_OFFSET_FACTOR above).
        private const val MIN_SAMPLE_WEIGHT = 0.1
        
        // D-159: the geometric fallback half-size record() uses when a caller does not pass the key's
        // real halfWidth/halfHeight (every real AdaptKeyboardView call site does) - matches this test
        // suite's own long-standing convention for an ordinary key (see OffsetModelTest's candidate()).
        private const val DEFAULT_FALLBACK_HALF_SIZE = 50f
    }
}

package de.froehlichmedia.adaptkey.touch

import kotlin.math.ln
import kotlin.math.max

/**
 * Personal 2D offset model (T-03) - typing-style agnostic.
 *
 * For every confirmed tap the model accumulates the deviation `(dx, dy) = (touch - keyCentre)`
 * per key as an incremental 2D Gaussian (Welford mean and variance). The learned per-key mean
 * compensates for the user's systematic deviation; [resolve] then picks the candidate whose
 * compensated Gaussian best explains a new tap. The model makes no assumption about handedness
 * or finger - it learns purely from observed deviations and improves continuously.
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
    private val maxOffsetFactor: Double = DEFAULT_MAX_OFFSET_FACTOR,
    private val warmupSamples: Long = DEFAULT_WARMUP_SAMPLES
) {
    
    /** Incremental sufficient statistics for one key's deviation distribution. */
    data class Stat(
        var count: Long = 0L,
        var meanDx: Double = 0.0,
        var meanDy: Double = 0.0,
        var m2Dx: Double = 0.0,
        var m2Dy: Double = 0.0,
        var sizeCount: Long = 0L,
        var meanSize: Double = 0.0
    )
    
    /** A candidate key with the geometry needed to score and record a tap. */
    data class Candidate(
        val id: String,
        val centerX: Float,
        val centerY: Float,
        val halfWidth: Float,
        val halfHeight: Float
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
     * Records a confirmed tap, updating the key's running mean and variance (Welford).
     *
     * The optional contact area ([size], from {@code MotionEvent.getSize()}) feeds a separate running
     * mean used by the typing-pattern detection (T-04). It is only accumulated when strictly positive,
     * so devices that report no contact size simply leave [Stat.sizeCount] at zero and the pattern
     * detection falls back gracefully.
     *
     * @param id the confirmed key's id
     * @param centerX the key centre x in view pixels
     * @param centerY the key centre y in view pixels
     * @param x the raw tap x (T-01 ACTION_DOWN)
     * @param y the raw tap y (T-01 ACTION_DOWN)
     * @param size the normalised contact area of the tap, or 0 when unavailable (T-04)
     */
    fun record(id: String, centerX: Float, centerY: Float, x: Float, y: Float, size: Float = 0f) {
        val stat = stats.getOrPut(id) { Stat() }
        val dx = (x - centerX).toDouble()
        val dy = (y - centerY).toDouble()
        stat.count += 1L
        val deltaX = dx - stat.meanDx
        stat.meanDx += deltaX / stat.count
        stat.m2Dx += deltaX * (dx - stat.meanDx)
        val deltaY = dy - stat.meanDy
        stat.meanDy += deltaY / stat.count
        stat.m2Dy += deltaY * (dy - stat.meanDy)
        if (size > 0f) {
            stat.sizeCount += 1L
            stat.meanSize += (size.toDouble() - stat.meanSize) / stat.sizeCount
        }
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
     * @param maxAbsY maximum absolute y offset to return
     * @return the clamped (dx, dy) mean offset, or (0, 0) when the key is untrained
     */
    fun cappedMeanOffset(id: String, maxAbsX: Double, maxAbsY: Double): Pair<Double, Double> {
        val stat = stats[id]
        if (stat == null || stat.count == 0L) {
            return 0.0 to 0.0
        }
        return stat.meanDx.coerceIn(-maxAbsX, maxAbsX) to stat.meanDy.coerceIn(-maxAbsY, maxAbsY)
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
        val deltaX = b.meanDx - a.meanDx
        val deltaY = b.meanDy - a.meanDy
        val meanDx = a.meanDx + deltaX * b.count / count
        val meanDy = a.meanDy + deltaY * b.count / count
        val m2Dx = a.m2Dx + b.m2Dx + deltaX * deltaX * a.count * b.count / count
        val m2Dy = a.m2Dy + b.m2Dy + deltaY * deltaY * a.count * b.count / count
        val sizeCount = a.sizeCount + b.sizeCount
        val meanSize = if (sizeCount == 0L) 0.0 else (a.meanSize * a.sizeCount + b.meanSize * b.sizeCount) / sizeCount
        return Stat(count, meanDx, meanDy, m2Dx, m2Dy, sizeCount, meanSize)
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
        val capY = candidate.halfHeight * maxOffsetFactor
        val offsetX = (stat?.meanDx ?: 0.0).coerceIn(-capX, capX)
        val offsetY = (stat?.meanDy ?: 0.0).coerceIn(-capY, capY)
        val predictedX = candidate.centerX + offsetX
        val predictedY = candidate.centerY + offsetY
        val varX = variance(stat?.m2Dx, stat?.count, defaultVariance(candidate.halfWidth))
        val varY = variance(stat?.m2Dy, stat?.count, defaultVariance(candidate.halfHeight))
        val ddx = x - predictedX
        val ddy = y - predictedY
        return -0.5 * (ddx * ddx / varX + ddy * ddy / varY) - 0.5 * (ln(varX) + ln(varY))
    }
    
    private fun variance(m2: Double?, count: Long?, default: Double): Double {
        if (m2 != null && count != null && count >= MIN_VARIANCE_SAMPLES) {
            val sampleVariance = m2 / (count - 1L)
            return max(sampleVariance, default * VARIANCE_FLOOR_FRACTION)
        }
        return default
    }
    
    private fun defaultVariance(half: Float): Double {
        val sigma = max(half.toDouble(), 1.0) * DEFAULT_SIGMA_FACTOR
        return sigma * sigma
    }
    
    companion object {
        
        /** Default cap on the learned mean offset, as a fraction of the key half-size. */
        const val DEFAULT_MAX_OFFSET_FACTOR = 0.9
        
        /** Default number of taps before the model overrides plain geometry. */
        const val DEFAULT_WARMUP_SAMPLES = 20L
        
        private const val MIN_VARIANCE_SAMPLES = 5L
        private const val VARIANCE_FLOOR_FRACTION = 0.1
        private const val DEFAULT_SIGMA_FACTOR = 0.6
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import kotlin.math.abs

/**
 * One key's aggregated evidence for typing-pattern detection (T-04).
 *
 * The fractions are normalised against the key size so the classifier is independent of the physical
 * keyboard scale: a [lateralFraction] of {@code +0.3} means the user lands, on average, 30 % of the
 * key's half-width to the right of its centre.
 *
 * @property normalizedX the key centre's x position across the keyboard, in {@code [0, 1]} (0 = left edge)
 * @property lateralFraction mean horizontal deviation as a fraction of the key half-width (sign = direction)
 * @property verticalFraction mean vertical deviation as a fraction of the key half-height (sign = direction)
 * @property contactArea mean {@code MotionEvent.getSize()} for the key, or null when unavailable
 * @property sampleCount number of taps backing this key's statistics
 */
data class KeySample(
    val normalizedX: Double,
    val lateralFraction: Double,
    val verticalFraction: Double,
    val contactArea: Double?,
    val sampleCount: Long
)

/**
 * Derives the dominant [TypingPattern] from the accumulated per-key offsets (T-04).
 *
 * The decision is a pure function of the supplied [KeySample]s, weighted by each key's sample count:
 *
 * - A clear lateral bias - overall, or concentrated on the half where the spec expects it to be most
 *   pronounced - points to an index finger (right-bias -> left index, left-bias -> right index).
 * - Otherwise, a low lateral bias combined with a large contact area and vertically centred hits points
 *   to a thumb. Thumb detection requires contact-area evidence; without it the result stays
 *   [TypingPattern.UNKNOWN] rather than guessing.
 * - Below [minTotalSamples] taps, or when nothing dominates, the result is [TypingPattern.UNKNOWN].
 *
 * All thresholds are constructor parameters so the detection can be tuned and unit-tested precisely;
 * the defaults are deliberately conservative because the result is shown for transparency only.
 *
 * @property minTotalSamples minimum total taps before any pattern is reported
 * @property lateralThreshold lateral-fraction magnitude that, on its own, identifies an index finger
 * @property lowLateralThreshold lateral-fraction magnitude below which a tap counts as laterally neutral
 * @property centeredThreshold vertical-fraction magnitude below which hits count as vertically centred
 * @property thumbContactArea mean contact area at or above which a neutral, centred pattern reads as a thumb
 */
class TypingPatternClassifier(
    private val minTotalSamples: Long = DEFAULT_MIN_TOTAL_SAMPLES,
    private val lateralThreshold: Double = DEFAULT_LATERAL_THRESHOLD,
    private val lowLateralThreshold: Double = DEFAULT_LOW_LATERAL_THRESHOLD,
    private val centeredThreshold: Double = DEFAULT_CENTERED_THRESHOLD,
    private val thumbContactArea: Double = DEFAULT_THUMB_CONTACT_AREA
) {
    
    /**
     * Classifies the dominant typing pattern.
     *
     * @param samples the per-key evidence; keys with no samples are ignored
     * @return the detected pattern, or [TypingPattern.UNKNOWN] when the evidence is insufficient
     */
    fun classify(samples: List<KeySample>): TypingPattern {
        val used = samples.filter { it.sampleCount > 0L }
        val total = used.sumOf { it.sampleCount }
        if (total < minTotalSamples) {
            return TypingPattern.UNKNOWN
        }
        val overallLateral = weighted(used) { it.lateralFraction }
        val leftLateral = weighted(used.filter { it.normalizedX < HALF }) { it.lateralFraction }
        val rightLateral = weighted(used.filter { it.normalizedX >= HALF }) { it.lateralFraction }
        
        // Left index finger: rightward bias, expected to be strongest on the right half (T-04).
        if (overallLateral >= lateralThreshold || (overallLateral > 0.0 && rightLateral >= lateralThreshold)) {
            return TypingPattern.LEFT_INDEX_FINGER
        }
        // Right index finger: leftward bias, expected to be strongest on the left half (T-04).
        if (overallLateral <= -lateralThreshold || (overallLateral < 0.0 && leftLateral <= -lateralThreshold)) {
            return TypingPattern.RIGHT_INDEX_FINGER
        }
        
        // Thumb: low lateral bias, large contact area, vertically centred. Requires area evidence.
        val sized = used.filter { it.contactArea != null }
        if (sized.isNotEmpty() && abs(overallLateral) <= lowLateralThreshold) {
            val meanArea = weighted(sized) { it.contactArea ?: 0.0 }
            val meanVertical = weighted(used) { abs(it.verticalFraction) }
            if (meanArea >= thumbContactArea && meanVertical <= centeredThreshold) {
                return TypingPattern.THUMB
            }
        }
        return TypingPattern.UNKNOWN
    }
    
    private inline fun weighted(samples: List<KeySample>, value: (KeySample) -> Double): Double {
        val total = samples.sumOf { it.sampleCount }
        if (total == 0L) {
            return 0.0
        }
        return samples.sumOf { value(it) * it.sampleCount } / total
    }
    
    companion object {
        
        private const val HALF = 0.5
        
        /** Default minimum total taps before any pattern is reported. */
        const val DEFAULT_MIN_TOTAL_SAMPLES = 40L
        
        /** Default lateral-fraction magnitude that, on its own, identifies an index finger. */
        const val DEFAULT_LATERAL_THRESHOLD = 0.18
        
        /** Minimum taps for a calibration classification (T-04 / K-01); calibration data is deliberate. */
        const val CALIBRATION_MIN_TOTAL_SAMPLES = 20L
        
        /**
         * Lateral threshold for a calibration classification: much more sensitive than the live default,
         * so a real but modest hand bias (which left the conservative live detector at UNKNOWN) still
         * yields a concrete result the user can be told about. Calibration is a controlled, deliberate
         * session, so committing to the dominant direction is appropriate.
         */
        const val CALIBRATION_LATERAL_THRESHOLD = 0.05
        
        /**
         * A decisive classifier for the K-01 calibration result: it reports the dominant hand from a
         * smaller, more modest bias than the transparency-only live detector.
         *
         * @return a calibration-tuned classifier
         */
        fun forCalibration(): TypingPatternClassifier {
            return TypingPatternClassifier(
                minTotalSamples = CALIBRATION_MIN_TOTAL_SAMPLES,
                lateralThreshold = CALIBRATION_LATERAL_THRESHOLD
            )
        }
        
        /** Default lateral-fraction magnitude below which a tap counts as laterally neutral. */
        const val DEFAULT_LOW_LATERAL_THRESHOLD = 0.10
        
        /** Default vertical-fraction magnitude below which hits count as vertically centred. */
        const val DEFAULT_CENTERED_THRESHOLD = 0.35
        
        /** Default mean contact area at or above which a neutral, centred pattern reads as a thumb. */
        const val DEFAULT_THUMB_CONTACT_AREA = 0.18
    }
}

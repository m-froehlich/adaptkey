// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import kotlin.math.sqrt

/**
 * One sample point of a glide trace or of an ideal candidate path, in the same pixel space the
 * keyboard view renders its keys in (D-516: the recognizer is fed plain coordinates - no Android
 * types - so the whole glide/ package stays JVM-unit-testable per the project's pure-policy style).
 */
data class GlidePoint(val x: Double, val y: Double) {

    /** Euclidean distance to [other]. */
    fun distanceTo(other: GlidePoint): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * A glide stroke resampled to constant arc-length spacing (D-516, plan §4.2 step 1). The raw move
 * events arrive at whatever rate the touch hardware delivered them - spacing them out at a fixed
 * distance makes every downstream computation (alignment cost, length ratio) speed-invariant: the
 * same physical stroke resamples identically whether the finger moved fast or slow.
 *
 * Pure by design (no Android imports), like [de.froehlichmedia.adaptkey.gesture.TouchSequenceTracker]:
 * the view feeds raw points and consumes the result; this class owns only the geometry.
 */
class GlideTrace(val points: List<GlidePoint>) {

    /** Total polyline length of the resampled points, in pixels. */
    val arcLength: Double = run {
        var total = 0.0
        for (i in 1 until points.size) {
            total += points[i - 1].distanceTo(points[i])
        }
        total
    }

    companion object {

        /**
         * Resampling distance in pixels - a considered starting point (plan §4.1: glide constants are
         * hardcoded and calibrated in R3 on the device, deliberately NOT coupled to
         * `d508_field_swipe_scale`, which calibrates a different question). Roughly one fifth of a
         * key pitch, so a word's ideal path carries a handful of points per key.
         */
        const val DEFAULT_SPACING_PX = 8.0

        /**
         * Degenerate-stroke guard: fewer resampled points than this and the stroke has nothing for
         * the recognizer to score (plan §4.2: too-short strokes are never admitted to scoring at
         * all). The R2 gesture-classification arming gate (§4.1.2) is a separate, stricter
         * question - this is only the recognizer's own sanity bound.
         */
        const val MIN_POINTS = 6

        /**
         * Resamples [raw] move points to constant [spacing] along the stroke's own arc length.
         * The raw first and last points are always kept exactly (they anchor the endpoint
         * prefilter's start/end key). Duplicate consecutive raw points (a dwell) contribute no
         * length and are skipped - a doubled letter therefore cannot be told apart geometrically
         * from its single-letter sibling; that family is separated by frequency/context, exactly
         * the adversarial-pair design the plan's §6 tests pin.
         *
         * @return the resampled trace, or null when [raw] has fewer than two points, [spacing] is
         *         not positive, or the resampled stroke falls below [MIN_POINTS]
         */
        fun resample(raw: List<GlidePoint>, spacing: Double = DEFAULT_SPACING_PX): GlideTrace? {
            if (raw.size < 2 || spacing <= 0.0) {
                return null
            }
            val points = ArrayList<GlidePoint>(raw.size)
            points.add(raw.first())
            var carried = 0.0
            for (i in 1 until raw.size) {
                val a = raw[i - 1]
                val b = raw[i]
                val segmentLength = a.distanceTo(b)
                if (segmentLength == 0.0) {
                    continue
                }
                var position = spacing - carried
                while (position <= segmentLength) {
                    val t = position / segmentLength
                    points.add(GlidePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
                    position += spacing
                }
                carried = segmentLength - (position - spacing)
            }
            val last = raw.last()
            if (points.last() != last) {
                points.add(last)
            }
            return if (points.size >= MIN_POINTS) GlideTrace(points) else null
        }
    }
}

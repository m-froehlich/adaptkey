// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Unit tests for the D-516 stroke resampling: constant arc-length spacing, speed invariance,
 * endpoint anchoring, and the degenerate-stroke guard.
 */
class GlideTraceTest {

    private fun line(from: GlidePoint, to: GlidePoint, step: Double): List<GlidePoint> {
        val distance = from.distanceTo(to)
        val count = (distance / step).toInt().coerceAtLeast(1)
        return (0..count).map { i ->
            val t = i.toDouble() / count
            GlidePoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
        }
    }

    @Test
    fun `resampled points sit at constant spacing along the stroke`() {
        // An unevenly sampled stroke: raw gaps of 2-18 px as a jittering touch stream delivers them.
        val raw = ArrayList<GlidePoint>()
        val start = GlidePoint(0.0, 0.0)
        val end = GlidePoint(300.0, 0.0)
        raw.add(start)
        var gaps = intArrayOf(3, 17, 6, 12, 2, 9, 15, 4, 11, 8)
        var x = 0.0
        var i = 0
        while (x < 300.0) {
            x += gaps[i % gaps.size].toDouble()
            raw.add(GlidePoint(minOf(x, 300.0), 0.0))
            i++
        }
        raw[raw.size - 1] = end

        val trace = GlideTrace.resample(raw)!!

        assertEquals(start, trace.points.first(), "the raw first point anchors the trace")
        assertEquals(end, trace.points.last(), "the raw last point anchors the trace")
        for (j in 1 until trace.points.size - 1) {
            val d = trace.points[j - 1].distanceTo(trace.points[j])
            assertTrue(
                abs(d - GlideTrace.DEFAULT_SPACING_PX) < 1e-9,
                "interior resampled points must sit exactly one spacing apart, got $d"
            )
        }
    }

    @Test
    fun `resampling is speed-invariant - the same path resamples identically however unevenly sampled`() {
        val path = ArrayList<GlidePoint>()
        path.addAll(line(GlidePoint(0.0, 0.0), GlidePoint(100.0, 30.0), 5.0))
        path.addAll(line(GlidePoint(100.0, 30.0), GlidePoint(180.0, 90.0), 20.0))
        path.addAll(line(GlidePoint(180.0, 90.0), GlidePoint(260.0, 95.0), 3.0))

        val even = GlideTrace.resample(path)!!
        val shuffled = ArrayList<GlidePoint>()
        // Same polyline, walked at wildly different speeds (alternate tiny/huge raw gaps).
        for (segment in 0 until path.size - 1) {
            val a = path[segment]
            val b = path[segment + 1]
            val parts = if (segment % 2 == 0) 24 else 2
            for (k in 0 until parts) {
                val t = k.toDouble() / parts
                shuffled.add(GlidePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
            }
        }
        shuffled.add(path.last())
        val uneven = GlideTrace.resample(shuffled)!!

        assertEquals(even.points.size, uneven.points.size, "same arc length, same point count")
        assertEquals(even.arcLength, uneven.arcLength, 1e-6)
        for (j in even.points.indices) {
            assertEquals(even.points[j].x, uneven.points[j].x, 1e-6)
            assertEquals(even.points[j].y, uneven.points[j].y, 1e-6)
        }
    }

    @Test
    fun `a dwell contributes no length and is skipped`() {
        val raw = listOf(
            GlidePoint(0.0, 0.0),
            GlidePoint(40.0, 0.0),
            GlidePoint(40.0, 0.0), // stopped finger, duplicate report
            GlidePoint(80.0, 0.0),
            GlidePoint(120.0, 0.0),
            GlidePoint(160.0, 0.0),
            GlidePoint(200.0, 0.0)
        )
        val withDwell = GlideTrace.resample(raw)!!
        val without = GlideTrace.resample(raw.filterIndexed { i, _ -> i != 2 })!!

        assertEquals(without.arcLength, withDwell.arcLength, 1e-9)
        assertEquals(without.points.size, withDwell.points.size)
    }

    @Test
    fun `degenerate strokes are rejected`() {
        assertNull(GlideTrace.resample(emptyList()))
        assertNull(GlideTrace.resample(listOf(GlidePoint(5.0, 5.0))))
        assertNull(
            GlideTrace.resample(listOf(GlidePoint(0.0, 0.0), GlidePoint(2.0, 0.0))),
            "a two-pixel stroke resamples below MIN_POINTS and must be rejected"
        )
        assertNull(GlideTrace.resample(line(GlidePoint(0.0, 0.0), GlidePoint(500.0, 0.0), 10.0), -1.0))
    }

    @Test
    fun `a real-length stroke is accepted and reports its arc length`() {
        val raw = line(GlidePoint(0.0, 0.0), GlidePoint(400.0, 0.0), 10.0)
        val trace = GlideTrace.resample(raw)!!
        assertNotNull(trace)
        assertEquals(400.0, trace.arcLength, 1.0)
        assertNotEquals(0, trace.points.size)
        assertTrue(trace.points.size >= GlideTrace.MIN_POINTS)
    }
}

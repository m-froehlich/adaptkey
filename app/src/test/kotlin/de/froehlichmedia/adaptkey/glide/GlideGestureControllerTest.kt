// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-517 glide stroke classification (plan §4.1/§4.1.2): the three-part claim
 * rule, the update throttle, the end/reset lifecycle, and the unknown-word letter-sequence
 * inference. Same synthetic QWERTZ grid the recognizer tests use.
 */
class GlideGestureControllerTest {

    private val geometry = GlideKeyGeometry.fromRows(
        rows = listOf("qwertzuiop", "asdfghjkl", "yxcvbnm"),
        keyWidth = KEY_PITCH,
        keyHeight = ROW_PITCH
    )

    private fun trace(controller: GlideGestureController, points: List<GlidePoint>, startMs: Long = 1_000L, stepMs: Long = 16L): List<GlideGestureController.Decision> {
        controller.begin(geometry, KEY_PITCH.toFloat(), points.first().x.toFloat(), points.first().y.toFloat(), startMs)
        return points.drop(1).mapIndexed { i, p -> controller.move(p.x.toFloat(), p.y.toFloat(), startMs + (i + 1) * stepMs) }
    }

    /** A straight-ish glide from h through a to k: several keys, far more than one key width. */
    private fun hToKPoints(): List<GlidePoint> {
        val points = ArrayList<GlidePoint>()
        val h = geometry.centerOf('h')!!
        val a = geometry.centerOf('a')!!
        val k = geometry.centerOf('k')!!
        // h -> a -> k via fine interpolation
        for (t in 0..10) points.add(lerp(h, a, t / 10.0))
        for (t in 1..10) points.add(lerp(a, k, t / 10.0))
        return points
    }

    private fun lerp(from: GlidePoint, to: GlidePoint, t: Double): GlidePoint =
        GlidePoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)

    @Test
    fun `a multi-key stroke past key width and minimum time claims exactly once`() {
        val controller = GlideGestureController()
        val decisions = trace(controller, hToKPoints(), startMs = 1_000L, stepMs = 16L)

        assertTrue(controller.claimed)
        assertEquals(1, decisions.count { it == GlideGestureController.Decision.CLAIMED })
        assertFalse(decisions.first() == GlideGestureController.Decision.CLAIMED, "the very first move must not claim")
    }

    @Test
    fun `a fast two-key tap sweep never claims - the duration gate`() {
        val controller = GlideGestureController()
        // Same geometric path, but delivered in 60 ms total: faster than the fastest legitimate
        // tap sweep is allowed to be claimed over.
        val decisions = trace(controller, hToKPoints(), startMs = 1_000L, stepMs = 4L)
        // 20 moves x 4 ms = 80 ms total; the stroke spans two keys for most of that window but
        // the MIN_CLAIM_MS gate holds until 120 ms have passed - which never happens.
        assertFalse(controller.claimed, "got ${decisions.filter { it != GlideGestureController.Decision.NONE }}")
    }

    @Test
    fun `a sub-key-width stroke never claims even with time`() {
        val controller = GlideGestureController()
        // A small wobble around h only: path well under 1.5 key widths, and no second key centre
        // ever comes within the passing radius.
        val h = geometry.centerOf('h')!!
        val points = (0..15).map { i ->
            GlidePoint(
                h.x + (if (i % 2 == 0) -2.0 else 2.0),
                h.y + (if (i % 2 == 0) 1.5 else -1.5)
            )
        }
        trace(controller, points, startMs = 1_000L, stepMs = 40L)
        assertFalse(controller.claimed)
    }

    @Test
    fun `the update throttle fires at most once per window`() {
        val controller = GlideGestureController()
        val decisions = trace(controller, hToKPoints(), startMs = 1_000L, stepMs = 8L)
        // The stroke spans ~20 moves at 8 ms: far more moves than the 40 ms window admits.
        val updates = decisions.count { it == GlideGestureController.Decision.UPDATE }
        assertTrue(updates > 0, "a claimed stroke must request updates at all")
        assertTrue(updates <= (20 * 8L) / GlideGestureController.UPDATE_THROTTLE_MS + 1)
    }

    @Test
    fun `end resets fully so the next stroke starts clean`() {
        val controller = GlideGestureController()
        trace(controller, hToKPoints())
        assertTrue(controller.claimed)
        assertTrue(controller.hasStroke)
        controller.end()
        assertFalse(controller.hasStroke)
        assertFalse(controller.claimed)
        assertTrue(controller.points.isEmpty())
        assertEquals("", controller.inferredLetterSequence())
    }

    @Test
    fun `cancel before the claim leaves an ordinary tap candidate`() {
        val controller = GlideGestureController()
        val h = geometry.centerOf('h')!!
        val points = (0..5).map { i -> GlidePoint(h.x + 3.0 * i, h.y) }
        val decisions = trace(controller, points)
        assertFalse(controller.claimed)
        assertTrue(decisions.all { it == GlideGestureController.Decision.NONE })
        controller.end()
        assertFalse(controller.hasStroke)
    }

    @Test
    fun `the inferred letter sequence follows the clearly-visited keys`() {
        val controller = GlideGestureController()
        // A diagonal h -> u glide crosses no intermediate centre, so the sequence is exactly "hu":
        // points midway between centres name neither (a straight row sweep would legitimately read
        // as every centre it crossed, which is why this test uses a diagonal).
        val h = geometry.centerOf('h')!!
        val u = geometry.centerOf('u')!!
        val points = (0..20).map { i -> lerp(h, u, i / 20.0) }
        trace(controller, points)
        assertEquals("hu", controller.inferredLetterSequence())
    }

    @Test
    fun `the inference skips points far from any key centre`() {
        val controller = GlideGestureController()
        // From a, a detour out through the empty area left of the keyboard and back along the same
        // horizontal line: the far points sit near no centre at all, and the approach back to a
        // crosses neither q (above) nor y (below) - both sit exactly one row pitch off the line,
        // far outside the unambiguous neighbourhood.
        val a = geometry.centerOf('a')!!
        val points = ArrayList<GlidePoint>()
        points.add(a)
        points.add(GlidePoint(-30.0, a.y))
        points.add(GlidePoint(-30.0, a.y + 200.0))
        points.add(GlidePoint(-30.0, a.y))
        points.add(a)
        trace(controller, points)
        assertEquals("a", controller.inferredLetterSequence())
    }

    @Test
    fun `a move without a stroke is a safe no-op`() {
        val controller = GlideGestureController()
        assertEquals(GlideGestureController.Decision.NONE, controller.move(10.0f, 10.0f, 100L))
        assertFalse(controller.hasStroke)
        assertTrue(controller.points.isEmpty())
    }

    companion object {
        private const val KEY_PITCH = 64.0
        private const val ROW_PITCH = 72.0
    }
}

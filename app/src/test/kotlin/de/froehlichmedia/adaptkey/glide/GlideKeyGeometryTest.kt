// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the [GlideKeyGeometry] pitch scale (D-516/D-542): the default pairwise-minimum
 * computation and the D-542 override that keeps the pitch scale geometric when the centres are
 * offset-compensated personal strike points.
 */
class GlideKeyGeometryTest {

    @Test
    fun `the default pitch is the smallest pairwise centre distance`() {
        val geometry = GlideKeyGeometry.fromRows(rows = listOf("qwe", "asd"), keyWidth = 100.0, keyHeight = 120.0)

        assertEquals(100.0, geometry.minKeyPitch)
    }

    @Test
    fun `D-542 the pitch override wins over the compensated centres' own pairwise minimum`() {
        // Two centres drifted close together (a compensated layout's worst case): their pairwise
        // minimum would shrink the tolerances, so the geometric override must hold the scale.
        val drifted = mapOf(
            'a' to GlidePoint(0.0, 0.0),
            'b' to GlidePoint(30.0, 0.0),
            'c' to GlidePoint(200.0, 0.0)
        )
        val geometry = GlideKeyGeometry(drifted, minKeyPitchOverride = 93.0)

        assertEquals(93.0, geometry.minKeyPitch)
        assertTrue(geometry.centers.size == 3)
    }

    @Test
    fun `D-542 without an override the compensated centres still define the pitch`() {
        val drifted = mapOf(
            'a' to GlidePoint(0.0, 0.0),
            'b' to GlidePoint(30.0, 0.0)
        )

        assertEquals(30.0, GlideKeyGeometry(drifted).minKeyPitch)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-593/D-594 space-bar update marker's visibility policy and label geometry. */
class SpaceUpdateMarkerTest {
    
    // Paint-metric-shaped fixtures: ascent negative, descent positive, per FontMetrics convention.
    private val mainAscent = -18f
    private val mainDescent = 4f
    private val subAscent = -10f
    private val subDescent = 2f
    private val lineGap = 3f
    
    @Test
    fun `D-594 visible on the letters surface's labelled space key with a pushed update signal`() {
        assertTrue(
            SpaceUpdateMarker.visible(
                InputSurface.LETTERS,
                urlMode = false,
                emailMode = false,
                languageLabelShown = true,
                updateAvailable = true
            )
        )
    }
    
    @Test
    fun `D-097 nowhere the language label does not show - symbol page, url mode, email mode, unlabelled`() {
        val allTrue = { surface: InputSurface, url: Boolean, email: Boolean, label: Boolean ->
            SpaceUpdateMarker.visible(surface, url, email, label, updateAvailable = true)
        }
        assertFalse(allTrue(InputSurface.SYMBOLS, false, false, true))
        assertFalse(allTrue(InputSurface.EMOJI, false, false, true))
        assertFalse(allTrue(InputSurface.LETTERS, true, false, true))
        assertFalse(allTrue(InputSurface.LETTERS, false, true, true))
        assertFalse(allTrue(InputSurface.LETTERS, false, false, false))
    }
    
    @Test
    fun `D-593 invisible without the pushed update signal, whatever else is true`() {
        assertFalse(
            SpaceUpdateMarker.visible(
                InputSurface.LETTERS,
                urlMode = false,
                emailMode = false,
                languageLabelShown = true,
                updateAvailable = false
            )
        )
    }
    
    @Test
    fun `twoLineBaselines centres the whole block on the key`() {
        // mainHeight = 18 + 4 = 22, subHeight = 10 + 2 = 12, gap 3 -> block 37,
        // top = 100 - 37/2 = 81.5; mainBaseline = top - (-18); subBaseline = top + 22 + 3 - (-10).
        val baselines = SpaceUpdateMarker.twoLineBaselines(
            centerY = 100f,
            mainAscent = mainAscent,
            mainDescent = mainDescent,
            subAscent = subAscent,
            subDescent = subDescent,
            lineGap = lineGap
        )
        assertEquals(99.5f, baselines.mainBaseline)
        assertEquals(116.5f, baselines.subBaseline)
    }
    
    @Test
    fun `twoLineBaselines keeps the gap between the lines and orders them`() {
        val baselines = SpaceUpdateMarker.twoLineBaselines(
            centerY = 0f,
            mainAscent = mainAscent,
            mainDescent = mainDescent,
            subAscent = subAscent,
            subDescent = subDescent,
            lineGap = lineGap
        )
        // The endonym sits above, the update line below, separated by exactly the gap.
        val mainBottom = baselines.mainBaseline + mainDescent
        val subTop = baselines.subBaseline + subAscent
        assertEquals(lineGap, subTop - mainBottom)
        assertTrue(baselines.mainBaseline < baselines.subBaseline)
    }
    
    @Test
    fun `twoLineBaselines is symmetric around the centre - same block, mirrored centre, same shape`() {
        val above = SpaceUpdateMarker.twoLineBaselines(
            centerY = 90f,
            mainAscent = mainAscent,
            mainDescent = mainDescent,
            subAscent = subAscent,
            subDescent = subDescent,
            lineGap = lineGap
        )
        val below = SpaceUpdateMarker.twoLineBaselines(
            centerY = 110f,
            mainAscent = mainAscent,
            mainDescent = mainDescent,
            subAscent = subAscent,
            subDescent = subDescent,
            lineGap = lineGap
        )
        assertEquals(20f, below.mainBaseline - above.mainBaseline)
        assertEquals(20f, below.subBaseline - above.subBaseline)
    }
    
    @Test
    fun `fits accepts the block when the key is tall enough`() {
        // block = 22 + 3 + 12 = 37; 37 <= 50 * 0.82 = 41.
        assertTrue(
            SpaceUpdateMarker.fits(
                keyHeightPx = 50f,
                mainAscentPx = mainAscent,
                mainDescentPx = mainDescent,
                subAscentPx = subAscent,
                subDescentPx = subDescent,
                lineGapPx = lineGap
            )
        )
    }
    
    @Test
    fun `fits rejects a short key so the second line can never clip`() {
        // 37 > 44 * 0.82 = 36.08 - fall back to fill + endonym instead.
        assertFalse(
            SpaceUpdateMarker.fits(
                keyHeightPx = 44f,
                mainAscentPx = mainAscent,
                mainDescentPx = mainDescent,
                subAscentPx = subAscent,
                subDescentPx = subDescent,
                lineGapPx = lineGap
            )
        )
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) reproducing the exact CalibrationActivity -> TouchModelActivity
 * sequence reported as broken: switching the typing pattern must fully replace previously *learned* data
 * (real accumulated taps, not just a previous seed), not merely lay a new seed on top of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OffsetStoreRoboTest {
    
    @Test
    fun `switching pattern fully replaces previously learned data`() {
        val context = RuntimeEnvironment.getApplication()
        
        // Simulate real prior usage under an old pattern: many actual recorded taps, not just a seed.
        val learned = OffsetModel()
        repeat(500) { learned.record("c:a", centerX = 50f, centerY = 50f, x = 70f, y = 65f) }
        OffsetStore.save(context, learned)
        OffsetStore.saveDetectedPattern(context, TypingPattern.LEFT_INDEX_FINGER)
        
        // CalibrationActivity.persistPattern()'s exact sequence for a newly chosen pattern.
        val geometry = listOf(OffsetModel.Candidate("c:a", centerX = 50f, centerY = 50f, halfWidth = 20f, halfHeight = 20f))
        val fresh = OffsetModel()
        fresh.restore(PatternSeed.seed(TypingPattern.TWO_THUMBS, geometry))
        OffsetStore.save(context, fresh)
        OffsetStore.saveDetectedPattern(context, TypingPattern.TWO_THUMBS)
        
        // TouchModelActivity.onCreate()'s exact load, via a freshly obtained context reference.
        val reloaded = OffsetStore.load(RuntimeEnvironment.getApplication())
        val spread = reloaded.spreadFor("c:a")
        
        assertEquals(TypingPattern.TWO_THUMBS, OffsetStore.loadDetectedPattern(context))
        assertEquals("the old 500-sample learned data must be gone", 25L, spread!!.count)
        assertEquals("TWO_THUMBS seeds no directional bias", 0.0, spread.meanDx, 1e-9)
    }
}

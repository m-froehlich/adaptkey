// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.widget.Button
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.TypingPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) driving the real [CalibrationActivity] UI - not just its pure
 * helper logic - to rule out an Android-lifecycle-specific bug (layout timing, a stale keyboard instance,
 * etc.) that a plain unit test of the store layer alone cannot catch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalibrationActivityRoboTest {
    
    @Test
    fun `tapping a pattern button seeds the store, and TouchModelActivity picks it up on the very first try`() {
        val calibration = Robolectric.buildActivity(CalibrationActivity::class.java).setup().get()
        
        val button = calibration.findViewById<Button>(R.id.calibration_two_thumbs)
        assertNotNull("the Both Thumbs button must exist and be found", button)
        button.performClick()
        
        assertEquals(TypingPattern.TWO_THUMBS, OffsetStore.loadDetectedPattern(calibration))
        val seeded = OffsetStore.load(calibration)
        assertEquals(
            "TWO_THUMBS must have seeded every char key, including the number row (D-72)",
            true,
            seeded.totalSamples > 0L
        )
        val zeroKey = seeded.spreadFor("c:1")
        assertNotNull("the number row must be seeded too (D-72)", zeroKey)
        
        // TouchModelActivity, opened separately (as the user does via Settings -> "Trefferzonen
        // anzeigen"), must reflect the just-seeded model - not anything left over from before.
        val touchModel = Robolectric.buildActivity(TouchModelActivity::class.java).setup().get()
        val shown = touchModel.findViewById<de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView>(R.id.touch_model_keyboard)
        val shownModel = shown.offsetModel
        assertNotNull(shownModel)
        assertEquals(0.0, shownModel!!.spreadFor("c:1")!!.meanDx, 1e-9)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.TypingPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * Robolectric test (JVM, no emulator) driving the real [CalibrationActivity] UI - not just its pure
 * helper logic - to rule out an Android-lifecycle-specific bug (layout timing, a stale keyboard instance,
 * etc.) that a plain unit test of the store layer alone cannot catch.
 * 
 * D-237: [CalibrationActivity] merged the old separate `TouchModelActivity` in - a style button no longer
 * persists immediately, it previews the style live and asks for confirmation first (see
 * [CalibrationActivity.previewAndConfirm]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalibrationActivityRoboTest {
    
    @Test
    fun `tapping a pattern button previews it live but does not persist until confirmed`() {
        val calibration = Robolectric.buildActivity(CalibrationActivity::class.java).setup().get()
        val keyboard = calibration.findViewById<AdaptKeyboardView>(R.id.calibration_keyboard)
        
        val button = calibration.findViewById<Button>(R.id.calibration_two_thumbs)
        assertNotNull("the Both Thumbs button must exist and be found", button)
        button.performClick()
        
        // Previewed live on the same keyboard view - but not yet the actually stored pattern.
        assertNotNull("the preview must be shown immediately, before any confirmation", keyboard.offsetModel?.spreadFor("c:1"))
        assertEquals(TypingPattern.UNKNOWN, OffsetStore.loadDetectedPattern(calibration))
        
        confirmDialog()
        
        assertEquals(TypingPattern.TWO_THUMBS, OffsetStore.loadDetectedPattern(calibration))
        val seeded = OffsetStore.load(calibration)
        assertEquals(
            "TWO_THUMBS must have seeded every char key, including the number row (D-72)",
            true,
            seeded.totalSamples > 0L
        )
        assertNotNull("the number row must be seeded too (D-72)", seeded.spreadFor("c:1"))
        // The same activity's keyboard must reflect the now-persisted model directly - no second
        // activity/screen needed any more.
        assertEquals(0.0, keyboard.offsetModel!!.spreadFor("c:1")!!.meanDx, 1e-9)
    }
    
    @Test
    fun `cancelling the confirmation reverts the live preview back to the actual persisted model`() {
        val calibration = Robolectric.buildActivity(CalibrationActivity::class.java).setup().get()
        val keyboard = calibration.findViewById<AdaptKeyboardView>(R.id.calibration_keyboard)
        
        calibration.findViewById<Button>(R.id.calibration_right_thumb).performClick()
        cancelDialog()
        
        // Nothing was ever persisted, so the actual model is still empty.
        assertEquals(TypingPattern.UNKNOWN, OffsetStore.loadDetectedPattern(calibration))
        assertNull("the preview must be reverted once the switch is cancelled", keyboard.offsetModel?.spreadFor("c:1"))
    }
    
    @Test
    fun `opened from the settings there is only OK, never Skip`() {
        val calibration = Robolectric.buildActivity(CalibrationActivity::class.java).setup().get()
        
        assertEquals(View.GONE, calibration.findViewById<Button>(R.id.calibration_skip).visibility)
        assertEquals(View.VISIBLE, calibration.findViewById<Button>(R.id.calibration_done).visibility)
    }
    
    @Test
    fun `OK closes the screen without touching an existing style or its learned zones`() {
        val context = RuntimeEnvironment.getApplication()
        val learned = OffsetModel()
        learned.restore(mapOf("c:a" to OffsetModel.Stat(count = 50L, meanDx = 3.0)))
        OffsetStore.save(context, learned)
        OffsetStore.saveDetectedPattern(context, TypingPattern.RIGHT_THUMB)
        val calibration = Robolectric.buildActivity(CalibrationActivity::class.java).setup().get()
        
        calibration.findViewById<Button>(R.id.calibration_done).performClick()
        
        assertTrue(calibration.isFinishing)
        assertEquals(TypingPattern.RIGHT_THUMB, OffsetStore.loadDetectedPattern(calibration))
        assertEquals(3.0, OffsetStore.load(calibration).spreadFor("c:a")!!.meanDx, 1e-9)
    }
    
    @Test
    fun `onboarding offers Skip until a style is chosen, then OK instead`() {
        val calibration = Robolectric
            .buildActivity(CalibrationActivity::class.java, onboardingIntent())
            .setup()
            .get()
        
        assertEquals(View.VISIBLE, calibration.findViewById<Button>(R.id.calibration_skip).visibility)
        assertEquals(View.GONE, calibration.findViewById<Button>(R.id.calibration_done).visibility)
        
        calibration.findViewById<Button>(R.id.calibration_right_thumb).performClick()
        confirmDialog()
        
        assertEquals(View.GONE, calibration.findViewById<Button>(R.id.calibration_skip).visibility)
        assertEquals(View.VISIBLE, calibration.findViewById<Button>(R.id.calibration_done).visibility)
    }
    
    @Test
    fun `onboarding Skip applies the both-thumbs default when nothing was chosen`() {
        val calibration = Robolectric
            .buildActivity(CalibrationActivity::class.java, onboardingIntent())
            .setup()
            .get()
        
        calibration.findViewById<Button>(R.id.calibration_skip).performClick()
        
        assertTrue(calibration.isFinishing)
        assertEquals(TypingPattern.TWO_THUMBS, OffsetStore.loadDetectedPattern(calibration))
    }
    
    @Test
    fun `Skip never overwrites an already chosen style (D-485)`() {
        val context = RuntimeEnvironment.getApplication()
        OffsetStore.saveDetectedPattern(context, TypingPattern.LEFT_THUMB)
        val calibration = Robolectric
            .buildActivity(CalibrationActivity::class.java, onboardingIntent())
            .setup()
            .get()
        
        // Hidden already (a style exists), but the handler must hold up on its own too.
        calibration.findViewById<Button>(R.id.calibration_skip).performClick()
        
        assertEquals(TypingPattern.LEFT_THUMB, OffsetStore.loadDetectedPattern(calibration))
    }
    
    private fun onboardingIntent(): Intent {
        return Intent(RuntimeEnvironment.getApplication(), CalibrationActivity::class.java)
            .putExtra(CalibrationActivity.EXTRA_FROM_ONBOARDING, true)
    }
    
    private fun latestAlertDialog(): AlertDialog {
        return ShadowDialog.getLatestDialog() as AlertDialog
    }
    
    private fun confirmDialog() {
        latestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }
    
    private fun cancelDialog() {
        latestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }
}

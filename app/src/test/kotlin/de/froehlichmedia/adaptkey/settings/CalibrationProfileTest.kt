// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-508: the input-tempo calibration profiles' pure value mapping (see [CalibrationProfile]'s own
 * KDoc for the apply-once model and the reasoning). The apply half is one-line SharedPreferences glue
 * (established Android-glue convention) - the table itself is the testable contract.
 */
class CalibrationProfileTest {
    
    @Test
    fun `STANDARD writes today's defaults for all five keys`() {
        val values = CalibrationProfile.settingsFor(CalibrationProfile.STANDARD)
        assertEquals(5, values.size)
        assertEquals(320, values[SettingsStore.KEY_LONGPRESS_DELAY])
        assertEquals(400, values[SettingsStore.KEY_DOUBLE_TAP_DELAY])
        assertEquals(100, values[SettingsStore.KEY_SPACE_SWIPE_SCALE])
        assertEquals(100, values[SettingsStore.KEY_FIELD_SWIPE_SCALE])
        assertEquals(400, values[SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY])
    }
    
    @Test
    fun `DELIBERATE raises every hold window and every swipe distance`() {
        val values = CalibrationProfile.settingsFor(CalibrationProfile.DELIBERATE)
        assertEquals(500, values[SettingsStore.KEY_LONGPRESS_DELAY])
        assertEquals(550, values[SettingsStore.KEY_DOUBLE_TAP_DELAY])
        assertEquals(135, values[SettingsStore.KEY_SPACE_SWIPE_SCALE])
        assertEquals(130, values[SettingsStore.KEY_FIELD_SWIPE_SCALE])
        assertEquals(600, values[SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY])
    }
    
    @Test
    fun `QUICK shortens every hold window and every swipe distance`() {
        val values = CalibrationProfile.settingsFor(CalibrationProfile.QUICK)
        assertEquals(220, values[SettingsStore.KEY_LONGPRESS_DELAY])
        assertEquals(300, values[SettingsStore.KEY_DOUBLE_TAP_DELAY])
        assertEquals(85, values[SettingsStore.KEY_SPACE_SWIPE_SCALE])
        assertEquals(85, values[SettingsStore.KEY_FIELD_SWIPE_SCALE])
        assertEquals(350, values[SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY])
    }
    
    @Test
    fun `every profiled key is one of the store's known preference keys`() {
        for (profile in CalibrationProfile.values()) {
            for (key in CalibrationProfile.settingsFor(profile).keys) {
                assertTrue(
                    "unknown key $key in profile $profile",
                    key in setOf(
                        SettingsStore.KEY_LONGPRESS_DELAY,
                        SettingsStore.KEY_DOUBLE_TAP_DELAY,
                        SettingsStore.KEY_SPACE_SWIPE_SCALE,
                        SettingsStore.KEY_FIELD_SWIPE_SCALE,
                        SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY
                    )
                )
            }
        }
    }
    
    @Test
    fun `profiled values stay within their own settings ranges`() {
        for (profile in CalibrationProfile.values()) {
            val values = CalibrationProfile.settingsFor(profile)
            val longPress = values.getValue(SettingsStore.KEY_LONGPRESS_DELAY)
            assertTrue(longPress in SettingsMapper.MIN_LONGPRESS_DELAY_MS..SettingsMapper.MAX_LONGPRESS_DELAY_MS)
            val doubleTap = values.getValue(SettingsStore.KEY_DOUBLE_TAP_DELAY)
            assertTrue(doubleTap in SettingsMapper.MIN_DOUBLE_TAP_DELAY_MS..SettingsMapper.MAX_DOUBLE_TAP_DELAY_MS)
            val space = values.getValue(SettingsStore.KEY_SPACE_SWIPE_SCALE)
            assertTrue(space in SettingsMapper.MIN_SWIPE_SCALE_PERCENT..SettingsMapper.MAX_SWIPE_SCALE_PERCENT)
            val field = values.getValue(SettingsStore.KEY_FIELD_SWIPE_SCALE)
            assertTrue(field in SettingsMapper.MIN_SWIPE_SCALE_PERCENT..SettingsMapper.MAX_SWIPE_SCALE_PERCENT)
            val backspace = values.getValue(SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY)
            assertTrue(backspace in SettingsMapper.MIN_BACKSPACE_REPEAT_INITIAL_DELAY_MS..SettingsMapper.MAX_BACKSPACE_REPEAT_INITIAL_DELAY_MS)
        }
    }
}

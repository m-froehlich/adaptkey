// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import androidx.appcompat.app.ActionBar
import de.froehlichmedia.adaptkey.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * D-484: every settings sub-screen gets a toolbar with its title and an up arrow (the app theme has no
 * action bar of its own); the settings root shows the title but no arrow. Robolectric, JVM only - the visual
 * result and the edge-to-edge insets are not checked here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenActivityRoboTest {
    
    @Test
    fun `a sub-screen shows its title and an up arrow that closes it`() {
        val activity = Robolectric.buildActivity(FeatureOverviewActivity::class.java).setup().get()
        
        val bar = activity.supportActionBar
        assertNotNull("the sub-screen must have a support action bar", bar)
        assertEquals(activity.getString(R.string.d89_title), bar!!.title.toString())
        assertTrue(
            "the up arrow must be enabled",
            bar.displayOptions and ActionBar.DISPLAY_HOME_AS_UP != 0
        )
        
        assertTrue(activity.onSupportNavigateUp())
        assertTrue("tapping the up arrow must close the screen", activity.isFinishing)
    }
    
    @Test
    fun `the calibration screen also has the up arrow`() {
        val activity = Robolectric.buildActivity(CalibrationActivity::class.java).setup().get()
        
        assertTrue(activity.supportActionBar!!.displayOptions and ActionBar.DISPLAY_HOME_AS_UP != 0)
    }
    
    @Test
    fun `the settings root shows its title but no up arrow`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        
        val bar = activity.supportActionBar
        assertNotNull(bar)
        assertEquals(activity.getString(R.string.settings_title), bar!!.title.toString())
        assertEquals(0, bar.displayOptions and ActionBar.DISPLAY_HOME_AS_UP)
    }
}

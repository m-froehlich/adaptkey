// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.BottomInsetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * D-486: the "room below the keyboard" setting is a plain [ListPreference] like the mini-LLM threshold (the
 * inline [DropDownPreference] prototype was rejected on the device), listing "none" first, whose entry order
 * matches [BottomInsetMode]'s declaration, whose stored values [BottomInsetMode.fromKey] reads, and whose row
 * shows the current choice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BottomInsetPreferenceRoboTest {
    
    private fun preference(): ListPreference {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.settings_container) as PreferenceFragmentCompat
        val preference = fragment.findPreference<ListPreference>(SettingsStore.KEY_BOTTOM_INSET_MODE)
        assertNotNull("the setting must be a ListPreference", preference)
        return preference!!
    }
    
    @Test
    fun `it is a plain list preference, not the rejected inline dropdown`() {
        assertFalse(preference() is DropDownPreference)
    }
    
    @Test
    fun `the list has none first and its values follow the enum order`() {
        val preference = preference()
        
        assertEquals(BottomInsetMode.entries.map { it.name.lowercase() }, preference.entryValues.map { it.toString() })
        assertEquals("none", preference.entryValues.first().toString())
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.d486_mode_none), preference.entries.first().toString())
    }
    
    @Test
    fun `the default is automatic and the row shows it as the current value`() {
        val preference = preference()
        val application = RuntimeEnvironment.getApplication()
        
        assertEquals("auto", preference.value)
        val current = application.getString(R.string.pref_current_value, application.getString(R.string.d486_mode_auto))
        assertTrue(preference.summary.toString().endsWith(current))
    }
    
    @Test
    fun `every entry value resolves to the mode at the same position`() {
        val preference = preference()
        
        for ((index, value) in preference.entryValues.withIndex()) {
            assertEquals(BottomInsetMode.entries[index], BottomInsetMode.fromKey(value.toString()))
        }
    }
}

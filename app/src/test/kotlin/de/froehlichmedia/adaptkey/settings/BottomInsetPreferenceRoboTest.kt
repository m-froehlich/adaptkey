// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.BottomInsetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * D-486: the "room below the keyboard" setting is an inline dropdown (prototype), listing "none" first, whose
 * entry order matches [BottomInsetMode]'s declaration and whose stored values [BottomInsetMode.fromKey] reads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BottomInsetPreferenceRoboTest {
    
    private fun preference(): DropDownPreference {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.settings_container) as PreferenceFragmentCompat
        val preference = fragment.findPreference<DropDownPreference>(SettingsStore.KEY_BOTTOM_INSET_MODE)
        assertNotNull("the setting must be a DropDownPreference", preference)
        return preference!!
    }
    
    @Test
    fun `the dropdown lists none first and its values follow the enum order`() {
        val preference = preference()
        
        assertEquals(BottomInsetMode.entries.map { it.name.lowercase() }, preference.entryValues.map { it.toString() })
        assertEquals("none", preference.entryValues.first().toString())
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.d486_mode_none), preference.entries.first().toString())
    }
    
    @Test
    fun `the dropdown defaults to automatic`() {
        assertEquals("auto", preference().value)
    }
    
    @Test
    fun `every entry value resolves to the mode at the same position`() {
        val preference = preference()
        
        for ((index, value) in preference.entryValues.withIndex()) {
            assertEquals(BottomInsetMode.entries[index], BottomInsetMode.fromKey(value.toString()))
        }
    }
}

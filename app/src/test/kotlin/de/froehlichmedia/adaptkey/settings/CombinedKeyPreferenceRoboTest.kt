// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.CombinedKeyMode
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
 * D-507: the combined-key setting is a plain [ListPreference] like the mini-LLM threshold and D-486 (the
 * inline [DropDownPreference] prototype was rejected on the device), listing "off" first, whose entry
 * order matches [CombinedKeyMode]'s declaration, whose stored values [CombinedKeyMode.fromKey] reads,
 * and whose row shows the current choice (D-419).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CombinedKeyPreferenceRoboTest {
    
    private fun preference(): ListPreference {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.settings_container) as PreferenceFragmentCompat
        val preference = fragment.findPreference<ListPreference>(SettingsStore.KEY_COMBINED_KEY_MODE)
        assertNotNull("the setting must be a ListPreference", preference)
        return preference!!
    }
    
    @Test
    fun `it is a plain list preference, not the rejected inline dropdown`() {
        assertFalse(preference() is DropDownPreference)
    }
    
    @Test
    fun `the list has off first and its values follow the enum order`() {
        val preference = preference()
        
        assertEquals(CombinedKeyMode.entries.map { it.name.lowercase() }, preference.entryValues.map { it.toString() })
        assertEquals("off", preference.entryValues.first().toString())
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.d507_mode_off), preference.entries.first().toString())
    }
    
    @Test
    fun `the default is the symbol toggle and the row shows it as the current value`() {
        val preference = preference()
        val application = RuntimeEnvironment.getApplication()
        
        assertEquals("symbol", preference.value)
        val current = application.getString(R.string.pref_current_value, application.getString(R.string.d507_mode_symbols))
        assertTrue(preference.summary.toString().endsWith(current))
    }
    
    @Test
    fun `every entry value resolves to the mode at the same position`() {
        val preference = preference()
        
        for ((index, value) in preference.entryValues.withIndex()) {
            assertEquals(CombinedKeyMode.entries[index], CombinedKeyMode.fromKey(value.toString()))
        }
    }
}

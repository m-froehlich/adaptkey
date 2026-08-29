// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import de.froehlichmedia.adaptkey.R

/**
 * D-407: a discrete, string-valued slider - a small number of named positions (e.g. "Off" / "Cautious" /
 * "Medium" / "Aggressive"), each shown as text next to the thumb rather than a raw number. Persists a
 * String, exactly like [androidx.preference.ListPreference] does, via [values]' entry at the selected
 * index - not a plain [androidx.preference.SeekBarPreference] subclass, since that class owns its own
 * internal seek listener/state privately and offers no supported hook to swap its numeric value label for
 * text without fighting the library.
 *
 * [labels] and [values] are set right after inflation, from `SettingsFragment.onCreatePreferences()` (the
 * same place every other dynamically-configured preference in this screen is finished off) - later than
 * the framework's own [onSetInitialValue] call during inflation, which already needs [values] to resolve
 * the persisted string into a slider position. Setting [values] therefore re-resolves the position from
 * storage itself (its own property setter), rather than requiring the two to be set in a particular order.
 *
 * @property labels the localised text shown next to the thumb at each position, in slider order
 * @property values the value persisted at each position, in the same order as [labels]
 */
class LabeledSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    
    var labels: List<CharSequence> = emptyList()
    
    var values: List<String> = emptyList()
        set(value) {
            field = value
            resolveIndexFromStorage()
            refreshBoundViews()
        }
    
    private var xmlDefaultValue: String? = null
    private var selectedIndex = 0
    private var seekBar: SeekBar? = null
    private var valueView: TextView? = null
    
    init {
        layoutResource = R.layout.preference_labeled_seekbar
    }
    
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val bar = holder.findViewById(R.id.labeled_seekbar) as? SeekBar
        val value = holder.findViewById(R.id.labeled_seekbar_value) as? TextView
        seekBar = bar
        valueView = value
        refreshBoundViews()
        bar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    selectedIndex = progress
                    updateValueLabel()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            
            // Persisted only on release (matching this screen's other sliders' own convention), not on
            // every intermediate drag position - avoids re-applying settings dozens of times per drag.
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setIndex(selectedIndex, notify = true)
            }
        })
    }
    
    private fun updateValueLabel() {
        valueView?.text = labels.getOrNull(selectedIndex) ?: ""
    }
    
    private fun refreshBoundViews() {
        seekBar?.max = (values.size - 1).coerceAtLeast(0)
        seekBar?.progress = selectedIndex
        updateValueLabel()
    }
    
    /**
     * Moves to [index] programmatically - e.g. to reflect a value read fresh after the screen resumes.
     * Persists and notifies the change listener when [notify] is true; a plain UI sync (no side effect)
     * otherwise.
     */
    fun setIndex(index: Int, notify: Boolean = false) {
        val clamped = index.coerceIn(0, (values.size - 1).coerceAtLeast(0))
        if (notify) {
            val newValue = values.getOrNull(clamped) ?: return
            if (!callChangeListener(newValue)) {
                // A change listener vetoed the update - resync the visible thumb/label to the last
                // persisted value instead of leaving it showing the rejected one.
                seekBar?.progress = selectedIndex
                updateValueLabel()
                return
            }
            persistString(newValue)
        }
        selectedIndex = clamped
        seekBar?.progress = clamped
        updateValueLabel()
    }
    
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }
    
    // D-407: this is called once during inflation, typically before values (still empty at that point) is
    // ever set from SettingsFragment.onCreatePreferences() - only the XML default is captured here; the
    // actual index resolution is deferred to resolveIndexFromStorage(), re-run whenever values changes.
    override fun onSetInitialValue(defaultValue: Any?) {
        xmlDefaultValue = defaultValue as? String
        resolveIndexFromStorage()
    }
    
    private fun resolveIndexFromStorage() {
        val stored = getPersistedString(xmlDefaultValue ?: "")
        val index = values.indexOf(stored)
        selectedIndex = if (index >= 0) index else 0
    }
}

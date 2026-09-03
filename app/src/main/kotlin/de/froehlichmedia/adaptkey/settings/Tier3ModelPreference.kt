// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelStorage

/**
 * D-433-followup: the Mini-LLM settings row's own two independent actions - "Mehr erfahren" (shows
 * [Tier3ModelActivity.showDetailsDialog] right here, no navigation at all) and a status-aware setup button
 * (navigates to [Tier3ModelActivity] to actually install/manage the model, labelled [R.string.
 * c06_model_pref_ready] or [R.string.c06_model_pref_setup_now] depending on whether a model is currently
 * installed) - cannot be expressed by a plain [Preference]'s own single click-to-navigate behaviour (the
 * screen's former `<intent>`-on-the-whole-row XML declaration), so this overrides the row's own layout
 * ([R.layout.preference_tier3_model]) to host both as independent buttons instead. The row itself is
 * deliberately not selectable ([isSelectable]) - only the two buttons act.
 *
 * The setup button's own label is re-derived every time this preference's view is (re)bound, including from
 * [SettingsFragment.onResume]'s own explicit [refresh] call - the install state changes only via the
 * separate [Tier3ModelActivity], so returning from it must refresh this row exactly like [SettingsFragment]'s
 * own pre-existing K-01 calibration-summary refresh already does for the same reason.
 */
class Tier3ModelPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    
    init {
        layoutResource = R.layout.preference_tier3_model
        isSelectable = false
    }
    
    /** [notifyChanged] is protected on [Preference] itself - this is the public call site outside code needs. */
    fun refresh() {
        notifyChanged()
    }
    
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val installed = Tier3ModelStorage.isModelInstalled(context)
        val setupButton = holder.findViewById(R.id.tier3_pref_setup) as Button
        setupButton.setText(if (installed) R.string.c06_model_pref_ready else R.string.c06_model_pref_setup_now)
        setupButton.setOnClickListener {
            context.startActivity(Intent(context, Tier3ModelActivity::class.java))
        }
        val learnMoreButton = holder.findViewById(R.id.tier3_pref_learn_more) as Button
        learnMoreButton.setOnClickListener {
            Tier3ModelActivity.showDetailsDialog(context)
        }
    }
}

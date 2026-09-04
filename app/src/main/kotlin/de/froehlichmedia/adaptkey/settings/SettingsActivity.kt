// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.onboarding.OnboardingStore
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.TypingPattern

/**
 * Settings entry point for the configurable parameters (C-01 … C-09).
 *
 * Hosts a {@code androidx.preference} screen and is wired as both the launcher activity and the IME
 * settings activity (see {@code res/xml/method.xml}). The controls write to the default shared
 * preferences, which {@link SettingsStore} reads back and the running keyboard applies live. C-05
 * (blacklist) opens its own editor; C-06 (LLM threshold) is a plain list preference persisted here and
 * consumed by the tier-3 orchestration (inert while the mini-LLM backend is the no-op stub).
 *
 * D-191: also the only place the {@code READ_CONTACTS} runtime permission is ever requested - deliberately
 * not from the keyboard itself. An {@code InputMethodService} has no {@code Activity} of its own and cannot
 * show the system permission dialog; only an {@code Activity} (like this one) can, which is also why the
 * "ask the first time an email field is focused" convenience that was considered is not built - it would
 * mean launching this settings screen out from under whatever app the user is actually typing in, mid-edit.
 */
class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
    
    /** The preference screen itself. */
    class SettingsFragment : PreferenceFragmentCompat() {
        
        // D-191: must be registered unconditionally during Fragment initialisation (the documented
        // androidx.activity.result contract - registering it later, e.g. inside onCreatePreferences, would
        // risk missing the callback if the process is recreated mid-request).
        private val contactsPermissionLauncher: ActivityResultLauncher<String> =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val toggle = findPreference<SwitchPreferenceCompat>("d191_contacts_suggestions_enabled")
                if (granted) {
                    toggle?.isChecked = true
                } else {
                    toggle?.isChecked = false
                    Toast.makeText(requireContext(), R.string.d191_contacts_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            
            // D-361-followup: monochrome glyph icons (GlyphIconDrawable) for the settings screen's own
            // sub-screen entry points only, per the user's own explicit scoping - a plain preference with no
            // dedicated screen of its own stays without an icon. D-361-followup (v2): the original set drew
            // from the Mathematical Operators/Supplemental Arrows blocks, which read as thin, faint line-art
            // rather than proper icons once seen on a real device - replaced with bolder, more solidly-shaped
            // characters from the Miscellaneous Symbols/Dingbats blocks instead (alongside GlyphIconDrawable's
            // own larger size/textSize bump).
            mapOf(
                "k01_calibration" to "◎",
                "d280_language_packs" to "文",
                "c05_blacklist" to "✗",
                "d177_learned_words" to "✒",
                "d142_credentials" to "⚿",
                "d278_backup" to "☁",
                "d_diag_log" to "☰",
                "d89_feature_overview" to "ℹ"
            ).forEach { (key, glyph) ->
                findPreference<Preference>(key)?.icon = GlyphIconDrawable(requireContext(), glyph)
            }
            
            // D-407/D-408: labels/values aren't XML attributes on LabeledSeekBarPreference (no custom-
            // attribute declaration exists for it, unlike ListPreference's built-in android:entries/
            // entryValues) - set here instead, the same place info_version/d89_feature_overview's own
            // programmatic setup lives.
            setupLabeledSlider(
                SettingsStore.KEY_AUTOCORRECT_AGGRESSIVENESS,
                R.array.d353_autocorrect_aggressiveness_labels,
                R.array.d353_autocorrect_aggressiveness_values
            )
            setupLabeledSlider(
                SettingsStore.KEY_AUTO_SPLIT_MODE,
                R.array.d352_auto_split_mode_labels,
                R.array.d352_auto_split_mode_values
            )
            
            // Read live from the actual installed package rather than a hand-maintained string resource,
            // which would inevitably drift out of sync with the real versionName in app/build.gradle.kts.
            findPreference<Preference>("info_version")?.summary = runCatching {
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            }.getOrNull()
            
            // D-192: appends a coloured "Learn more" hint to the feature-overview entry's own summary, so
            // it visibly reads as a link rather than blending into the surrounding plain-text preferences -
            // set programmatically (like info_version above) since a plain XML summary string cannot carry
            // a partial colour span on its own.
            findPreference<Preference>("d89_feature_overview")?.let { preference ->
                val base = getString(R.string.d89_summary)
                val learnMore = getString(R.string.d89_learn_more)
                val full = "$base\n\n$learnMore"
                preference.summary = SpannableString(full).apply {
                    setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.link_text)),
                        base.length + 2,
                        full.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            
            findPreference<Preference>("onboarding_replay")?.setOnPreferenceClickListener {
                OnboardingStore.setCompleted(requireContext(), false)
                Toast.makeText(requireContext(), R.string.onboarding_replay_done, Toast.LENGTH_SHORT).show()
                true
            }
            
            updateCalibrationSummary()
            
            // D-302: the C-04 highlight-colour preference shows its own colour as the summary's text
            // colour (and, via [onDisplayPreferenceDialog], as each dialog entry's text colour too) - a
            // direct visual preview instead of just a colour name.
            updateHighlightColorSummary()
            findPreference<ListPreference>(SettingsStore.KEY_HIGHLIGHT_COLOR)?.setOnPreferenceChangeListener { _, newValue ->
                updateHighlightColorSummary(newValue as String)
                true
            }
            
            // D-419: every plain ListPreference must show its own currently selected entry in the main
            // list, not only once its dialog is opened - explicit user request. c04_highlight_color above
            // already shows its current value (its own bespoke coloured-summary mechanism); every other
            // plain ListPreference in this screen goes through the shared helper below instead.
            setupListPreferenceCurrentValueSummary(SettingsStore.KEY_LLM_THRESHOLD, R.string.c06_summary)
            setupListPreferenceCurrentValueSummary(SettingsStore.KEY_LEARNED_WORD_EXPIRY_WINDOW, R.string.d389_summary)
            
            // D-191: the runtime permission dialog only appears the moment the user actually opts in here,
            // never proactively - turning the toggle on with the permission not yet granted blocks the
            // immediate switch flip (returning false) and requests it instead; the launcher callback above
            // resolves the switch to its final state once Android answers. Already-granted (the ordinary
            // case once turned on before) and turning it off both go straight through - neither needs the
            // system dialog, and a previously-granted permission is never asked for again.
            findPreference<SwitchPreferenceCompat>("d191_contacts_suggestions_enabled")?.setOnPreferenceChangeListener { _, newValue ->
                val wantsOn = newValue as Boolean
                val alreadyGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
                if (wantsOn && !alreadyGranted) {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    false
                } else {
                    true
                }
            }
        }
        
        /**
         * D-407/D-408: wires a [LabeledSeekBarPreference]'s [LabeledSeekBarPreference.labels]/
         * [LabeledSeekBarPreference.values] from the given string-array resources - shared by every such
         * slider in this screen (C-22 autocorrect, D-352 word splitting) rather than duplicated per site.
         *
         * @param key the preference's own key
         * @param labelsRes the localised, displayed labels array (slider order)
         * @param valuesRes the persisted value array, same order as [labelsRes]
         */
        private fun setupLabeledSlider(key: String, labelsRes: Int, valuesRes: Int) {
            findPreference<LabeledSeekBarPreference>(key)?.apply {
                labels = resources.getStringArray(labelsRes).toList()
                values = resources.getStringArray(valuesRes).toList()
            }
        }
        
        /**
         * D-302: intercepts the C-04 highlight-colour preference's own dialog to show each entry in its
         * own colour instead of the default [ListPreference] dialog's plain-text list; every other
         * preference dialog is untouched.
         */
        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference.key == SettingsStore.KEY_HIGHLIGHT_COLOR && preference is ListPreference) {
                showHighlightColorDialog(preference)
                return
            }
            super.onDisplayPreferenceDialog(preference)
        }
        
        override fun onResume() {
            super.onResume()
            maybeOfferCalibration()
            // D-237: refreshes after returning from CalibrationActivity with a newly (or re-)chosen style -
            // onCreatePreferences() alone would only ever show whatever was current the moment the screen
            // was first built.
            updateCalibrationSummary()
            // D-433-followup: identical reasoning for Tier3ModelPreference's own status-aware setup button -
            // the install state only ever changes via the separate Tier3ModelActivity, so returning from it
            // must re-bind this row's view to pick up a fresh Tier3ModelStorage.isModelInstalled() read.
            findPreference<Tier3ModelPreference>("c06_model")?.refresh()
        }
        
        /**
         * Offers the K-01 calibration once, the first time the settings screen is shown. The "offered"
         * flag is set before the dialog appears, so the offer never repeats regardless of the choice;
         * calibration stays reachable any time via its permanent settings entry.
         */
        private fun maybeOfferCalibration() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            if (prefs.getBoolean(SettingsStore.KEY_CALIBRATION_OFFERED, false)) {
                return
            }
            prefs.edit().putBoolean(SettingsStore.KEY_CALIBRATION_OFFERED, true).apply()
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.k01_offer_title)
                .setMessage(R.string.k01_offer_message)
                .setPositiveButton(R.string.k01_offer_start) { _, _ ->
                    startActivity(Intent(requireContext(), CalibrationActivity::class.java))
                }
                .setNegativeButton(R.string.k01_offer_later, null)
                .show()
        }
        
        /**
         * D-237: appends the currently selected typing style to the `k01_calibration` entry's summary
         * (programmatic, like [onCreatePreferences]'s own `info_version`/`d89_feature_overview` handling
         * above - a plain XML summary string cannot carry a runtime-read value on its own).
         */
        private fun updateCalibrationSummary() {
            val pattern = OffsetStore.loadDetectedPattern(requireContext())
            val base = getString(R.string.k01_summary)
            val current = getString(R.string.k01_current_style, getString(patternLabelRes(pattern)))
            findPreference<Preference>("k01_calibration")?.summary = "$base\n$current"
        }
        
        private fun patternLabelRes(pattern: TypingPattern): Int {
            return when (pattern) {
                TypingPattern.LEFT_INDEX_FINGER -> R.string.t04_pattern_left_index
                TypingPattern.RIGHT_INDEX_FINGER -> R.string.t04_pattern_right_index
                TypingPattern.LEFT_THUMB -> R.string.t04_pattern_left_thumb
                TypingPattern.RIGHT_THUMB -> R.string.t04_pattern_right_thumb
                TypingPattern.TWO_THUMBS -> R.string.t04_pattern_two_thumbs
                TypingPattern.UNKNOWN -> R.string.t04_pattern_unknown
            }
        }
        
        /**
         * D-302: colours the C-04 preference row's own summary in the currently selected highlight
         * colour, so the label itself previews what it names (no colour for the "no highlighting"
         * sentinel, whose entry has nothing to preview).
         *
         * @param value the newly chosen stored value, or null to re-derive from the preference's current
         *        (already-persisted) value - used both from [onCreatePreferences] and from the preference's
         *        own change listener
         */
        private fun updateHighlightColorSummary(value: String? = null) {
            val preference = findPreference<ListPreference>(SettingsStore.KEY_HIGHLIGHT_COLOR) ?: return
            val current = value ?: preference.value ?: return
            val index = preference.findIndexOfValue(current)
            val label = if (index >= 0) preference.entries[index].toString() else current
            val color = colorForEntryValue(current)
            preference.summary = if (color != null) {
                SpannableString(label).apply {
                    setSpan(ForegroundColorSpan(color), 0, label.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                label
            }
        }
        
        /**
         * D-419: wires [key]'s summary to show its own static description plus the currently selected
         * entry's label - the initial render here, and a change listener that keeps it live afterwards,
         * mirroring [updateCalibrationSummary]'s own "base description + live current value" shape (that
         * one is a plain [Preference], not a [ListPreference], so it cannot share this exact helper).
         *
         * @param key the [ListPreference]'s own key
         * @param baseSummaryRes the setting's own static description string resource
         */
        private fun setupListPreferenceCurrentValueSummary(key: String, baseSummaryRes: Int) {
            updateListPreferenceCurrentValueSummary(key, baseSummaryRes)
            findPreference<ListPreference>(key)?.setOnPreferenceChangeListener { _, newValue ->
                updateListPreferenceCurrentValueSummary(key, baseSummaryRes, newValue as String)
                true
            }
        }
        
        /**
         * D-419-followup: the "Currently: X" line is rendered fully bold, so it visibly stands apart from
         * the plain-text description above it instead of blending in - explicit user request.
         *
         * @param key the [ListPreference]'s own key
         * @param baseSummaryRes the setting's own static description string resource
         * @param value the newly chosen stored value, or null to re-derive from the preference's current
         *        (already-persisted) value - the same parameter shape [updateHighlightColorSummary] uses
         */
        private fun updateListPreferenceCurrentValueSummary(key: String, baseSummaryRes: Int, value: String? = null) {
            val preference = findPreference<ListPreference>(key) ?: return
            val current = value ?: preference.value ?: return
            val index = preference.findIndexOfValue(current)
            val label = if (index >= 0) preference.entries[index] else current
            val base = getString(baseSummaryRes)
            val currentLine = getString(R.string.pref_current_value, label)
            val full = "$base\n$currentLine"
            preference.summary = SpannableString(full).apply {
                setSpan(StyleSpan(Typeface.BOLD), base.length + 1, full.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        /**
         * D-302: shows the C-04 colour list as a plain [AlertDialog] (bypassing the default
         * [ListPreference] dialog flow entirely) whose [android.widget.CheckedTextView] rows are tinted in
         * their own colour via a custom [ArrayAdapter]. Applying a choice goes through
         * [Preference.callChangeListener] / [ListPreference.setValue] exactly like the standard dialog
         * would, so persistence and [updateHighlightColorSummary] (wired as the change listener in
         * [onCreatePreferences]) both still fire normally.
         *
         * @param preference the C-04 list preference
         */
        private fun showHighlightColorDialog(preference: ListPreference) {
            val entries = preference.entries ?: return
            val entryValues = preference.entryValues ?: return
            val defaultTextColor = resolveDefaultListItemTextColor()
            val adapter = object : ArrayAdapter<CharSequence>(requireContext(), android.R.layout.simple_list_item_single_choice, entries) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as CheckedTextView
                    view.setTextColor(colorForEntryValue(entryValues[position].toString()) ?: defaultTextColor)
                    return view
                }
            }
            AlertDialog.Builder(requireContext())
                .setTitle(preference.title)
                .setSingleChoiceItems(adapter, preference.findIndexOfValue(preference.value)) { dialog, which ->
                    val value = entryValues[which].toString()
                    if (preference.callChangeListener(value)) {
                        preference.value = value
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        /**
         * @param value a C-04 stored entry value
         * @return the parsed ARGB colour, or null for the "no highlighting" sentinel / an unparseable value
         */
        private fun colorForEntryValue(value: String): Int? {
            if (value == SettingsStore.NO_HIGHLIGHT_VALUE) {
                return null
            }
            return runCatching { Color.parseColor(value) }.getOrNull()
        }
        
        /** @return the current theme's own default alert-dialog list-item text colour. */
        private fun resolveDefaultListItemTextColor(): Int {
            val typedValue = TypedValue()
            val resolved = requireContext().theme.resolveAttribute(android.R.attr.textColorAlertDialogListItem, typedValue, true)
            if (!resolved) {
                return ContextCompat.getColor(requireContext(), android.R.color.black)
            }
            return if (typedValue.resourceId != 0) ContextCompat.getColor(requireContext(), typedValue.resourceId) else typedValue.data
        }
        
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.onboarding.OnboardingStore
import de.froehlichmedia.adaptkey.touch.OffsetStore

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
    
    /** The preference screen itself, including the non-declarative C-08 reset action. */
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
            
            // Read live from the actual installed package rather than a hand-maintained string resource,
            // which would inevitably drift out of sync with the real versionName in app/build.gradle.kts.
            findPreference<Preference>("info_version")?.summary = runCatching {
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            }.getOrNull()
            
            findPreference<Preference>("c08_reset_hints")?.setOnPreferenceClickListener {
                SettingsStore.resetLetterHints(requireContext())
                Toast.makeText(requireContext(), R.string.c08_reset_done, Toast.LENGTH_SHORT).show()
                true
            }
            
            findPreference<Preference>("onboarding_replay")?.setOnPreferenceClickListener {
                OnboardingStore.setCompleted(requireContext(), false)
                Toast.makeText(requireContext(), R.string.onboarding_replay_done, Toast.LENGTH_SHORT).show()
                true
            }
            
            findPreference<Preference>("reset_learning")?.setOnPreferenceClickListener {
                confirmResetLearning()
                true
            }
            
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
        
        override fun onResume() {
            super.onResume()
            maybeOfferCalibration()
        }
        
        /**
         * Offers the K-01 calibration once, the first time the settings screen is shown. The "offered"
         * flag is set before the dialog appears, so the offer never repeats regardless of the choice;
         * calibration stays reachable any time via its permanent settings entry.
         */
        private fun maybeOfferCalibration() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            if (prefs.getBoolean(KEY_CALIBRATION_OFFERED, false)) {
                return
            }
            prefs.edit().putBoolean(KEY_CALIBRATION_OFFERED, true).apply()
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
         * Double-confirmed reset of the learned touch data (offset model T-03 + typing pattern T-04): a
         * first confirmation, then a final irreversible confirmation, then the actual reset. Kept behind two
         * dialogs so it can never be triggered by accident, while staying quick for deliberate test resets.
         */
        private fun confirmResetLearning() {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_learning_title)
                .setMessage(R.string.reset_learning_confirm1_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> confirmResetLearningFinal() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        private fun confirmResetLearningFinal() {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_learning_confirm2_title)
                .setMessage(R.string.reset_learning_confirm2_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    OffsetStore.clear(requireContext())
                    Toast.makeText(requireContext(), R.string.reset_learning_done, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        companion object {
            
            // Default-prefs flag marking that the one-time K-01 calibration offer has been shown.
            private const val KEY_CALIBRATION_OFFERED = "k01_calibration_offered"
        }
    }
}

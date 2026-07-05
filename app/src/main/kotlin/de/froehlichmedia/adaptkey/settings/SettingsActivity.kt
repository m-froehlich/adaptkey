// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
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
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            
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
        }
        
        override fun onResume() {
            super.onResume()
            // T-04 is re-derived by the running keyboard; reflect the latest detection on each return.
            val pattern = OffsetStore.loadDetectedPattern(requireContext())
            findPreference<Preference>("t04_detected")?.setSummary(patternLabel(pattern))
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
                    findPreference<Preference>("t04_detected")?.setSummary(patternLabel(OffsetStore.loadDetectedPattern(requireContext())))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        private fun patternLabel(pattern: TypingPattern): Int {
            return when (pattern) {
                TypingPattern.LEFT_INDEX_FINGER -> R.string.t04_pattern_left_index
                TypingPattern.RIGHT_INDEX_FINGER -> R.string.t04_pattern_right_index
                TypingPattern.THUMB -> R.string.t04_pattern_thumb
                TypingPattern.UNKNOWN -> R.string.t04_pattern_unknown
            }
        }
        
        companion object {
            
            // Default-prefs flag marking that the one-time K-01 calibration offer has been shown.
            private const val KEY_CALIBRATION_OFFERED = "k01_calibration_offered"
        }
    }
}

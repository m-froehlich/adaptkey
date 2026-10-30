// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context

/**
 * D-508: the input-tempo calibration profiles - a whole-solution answer to the community report that
 * the gesture/long-press calibration (320 ms long-press arm vs. 50.6 dp space swipe, D-32/D-57's
 * opposite tunings meeting through one shared constant) sits on the wrong side for deliberately slow
 * users, with the same problem carrying over to slow presses on ordinary keys.
 * 
 * **Apply-once model, deliberately no persistent profile state.** A profile is an action that writes
 * concrete values into the *existing* preference keys (the sliders stay the single source of truth);
 * nothing else is stored. This mirrors D-301's lesson - a second truth layer above defaults collided
 * with per-key customisation and was removed rather than fixed - and it keeps backup export/import,
 * the service's own reload path and any future migration untouched: after applying, the profiled
 * values are ordinary stored settings.
 * 
 * Proposed starting values (D-508 plan §5; device-tuning expected, the G-08 "calibration status"
 * precedent):
 * 
 * | Key | Standard | Deliberate | Quick |
 * |---|---|---|---|
 * | [SettingsStore.KEY_LONGPRESS_DELAY] | 320 | 500 | 220 |
 * | [SettingsStore.KEY_DOUBLE_TAP_DELAY] (shared G-05/D-348/D-361 window) | 400 | 550 | 300 |
 * | [SettingsStore.KEY_SPACE_SWIPE_SCALE] | 100 % | 135 % | 85 % |
 * | [SettingsStore.KEY_FIELD_SWIPE_SCALE] | 100 % | 130 % | 85 % |
 * | [SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY] (D-07) | 400 ms | 600 ms | 350 ms |
 * 
 * Deliberately NOT profiled: the D-401 toggle (stays its own switch), touch-zone learning (D-474),
 * the shift grace window (C-07).
 */
enum class CalibrationProfile {
    
    /** Today's defaults - the values every existing install already runs with. */
    STANDARD,
    
    /** For deliberately slow typists: longer hold windows, larger swipe travel. */
    DELIBERATE,
    
    /** For very quick typists: shorter hold windows, smaller swipe travel. */
    QUICK;
    
    companion object {
        
        /**
         * @param profile the profile to resolve
         * @return the exact preference keys and values [apply] would write, as a pure map (unit-tested)
         */
        fun settingsFor(profile: CalibrationProfile): Map<String, Int> {
            val (longPress, doubleTap, spaceScale, fieldScale, backspaceInitial) = when (profile) {
                STANDARD -> listOf(320, 400, 100, 100, 400)
                DELIBERATE -> listOf(500, 550, 135, 130, 600)
                QUICK -> listOf(220, 300, 85, 85, 350)
            }
            return mapOf(
                SettingsStore.KEY_LONGPRESS_DELAY to longPress,
                SettingsStore.KEY_DOUBLE_TAP_DELAY to doubleTap,
                SettingsStore.KEY_SPACE_SWIPE_SCALE to spaceScale,
                SettingsStore.KEY_FIELD_SWIPE_SCALE to fieldScale,
                SettingsStore.KEY_BACKSPACE_REPEAT_INITIAL_DELAY to backspaceInitial
            )
        }
        
        /**
         * Writes [profile]'s values into the shared preferences. The resident service picks the change
         * up live through its own [android.content.SharedPreferences.OnSharedPreferenceChangeListener]
         * (the ordinary settings-reload path) - no extra plumbing, and a later manual slider change
         * simply overrides the profiled value, exactly the apply-once contract above.
         * 
         * @param context any valid context
         * @param profile the profile to apply
         */
        fun apply(context: Context, profile: CalibrationProfile) {
            val editor = SettingsStore.prefs(context).edit()
            settingsFor(profile).forEach { (key, value) -> editor.putInt(key, value) }
            editor.apply()
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig
import de.froehlichmedia.adaptkey.touch.TypingPattern

/**
 * Thin storage layer for the configurable parameters (C-01 … C-09).
 *
 * Reads from the same default [SharedPreferences] file that the {@code androidx.preference} screen
 * writes to (via [PreferenceManager.getDefaultSharedPreferences]), so the UI and the running keyboard
 * stay in sync automatically and the service can observe changes with a single
 * [SharedPreferences.OnSharedPreferenceChangeListener]. The slider integers are decoded here and the
 * validated configuration is produced by the pure [SettingsMapper]; that mapping is what the unit tests
 * exercise, while this Android-facing IO is left to instrumented tests (like the other store layers).
 *
 * C-05 (the blacklist) is not stored here - it lives in the SQLite dictionary.
 */
object SettingsStore {
    
    // Preference keys; must match res/xml/settings_preferences.xml.
    const val KEY_SPACE_WEIGHT = "c01_space_weight"
    const val KEY_COMMA_WEIGHT = "c01_comma_weight"
    const val KEY_PERIOD_WEIGHT = "c01_period_weight"
    const val KEY_BACKSPACE_EXTRA = "c01_backspace_extra"
    const val KEY_SHIFT_EXTRA = "c01_shift_extra"
    const val KEY_RESORT_DELAY = "c02_resort_delay_ms"
    const val KEY_MAX_SUGGESTIONS = "c03_max_suggestions"
    const val KEY_HIGHLIGHT_ENABLED = "c04_highlight_enabled"
    const val KEY_HIGHLIGHT_COLOR = "c04_highlight_color"
    const val KEY_NUMBER_ROW = "c09_number_row"
    const val KEY_HINTS_ENABLED = "c08_hints_enabled"
    const val KEY_LETTER_HINTS = "c08_letter_hints"
    const val KEY_SHIFT_GRACE = "c07_shift_grace_ms"
    const val KEY_COMMA_LINE_NOT_SENTENCE_START = "c10_comma_line_not_sentence_start"
    const val KEY_LLM_THRESHOLD = "c06_llm_threshold"
    const val KEY_KEY_SOUND = "d05_key_sound"
    const val KEY_KEY_HAPTICS = "d06_key_haptics"
    const val KEY_LONGPRESS_DELAY = "d32_longpress_delay_ms"
    const val KEY_SPACE_BELOW_NUMBER_ROW = "d55_space_below_number_row"
    const val KEY_SPACE_ABOVE_SPACE_ROW = "d55_space_above_space_row"
    const val KEY_SYMBOL_KEY = "d59_symbol_key"
    
    // C-01 weights are stored as hundredths of the float weight (e.g. 3.20 -> 320) so they fit a SeekBar.
    const val WEIGHT_SCALE = 100f
    
    /** Default stored integer for the space-bar weight slider (3.20). */
    const val DEF_SPACE_WEIGHT = 320
    
    /** Default stored integer for the comma weight slider (1.40). */
    const val DEF_COMMA_WEIGHT = 140
    
    /** Default stored integer for the full-stop weight slider (1.40). */
    const val DEF_PERIOD_WEIGHT = 140
    
    /** Default stored integer for the backspace surcharge slider, in percent (10 % = L-04). */
    const val DEF_BACKSPACE_EXTRA = 10
    
    /** Default stored integer for the shift surcharge slider, in percent (D-16, off until calibration). */
    const val DEF_SHIFT_EXTRA = 0
    
    /** Default stored integer for the re-sort delay slider (C-02, 300 ms). */
    const val DEF_RESORT_DELAY = 300
    
    /** Default stored integer for the maximum-suggestions slider (C-03, 8). */
    const val DEF_MAX_SUGGESTIONS = 8
    
    /** Default stored integer for the shift grace window slider (C-07, 300 ms). */
    const val DEF_SHIFT_GRACE = 300
    
    /** Default stored integer for the long-press delay slider (D-32, 320 ms). */
    const val DEF_LONGPRESS_DELAY = 320
    
    /** Default stored integer for both D-55 extra-row-spacing sliders (7 dp). */
    const val DEF_EXTRA_SPACING = AdaptSettings.DEFAULT_EXTRA_SPACING_DP
    
    /**
     * @param context any valid context
     * @return the shared default preferences backing both the settings screen and this store
     */
    fun prefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    
    /**
     * Loads and validates the full configuration.
     *
     * @param context any valid context (the input method service)
     * @return the resolved [AdaptSettings]; missing or corrupt values fall back to the spec defaults
     */
    fun load(context: Context): AdaptSettings {
        val p = prefs(context)
        val raw = RawSettings(
            spaceWeight = p.getInt(KEY_SPACE_WEIGHT, DEF_SPACE_WEIGHT) / WEIGHT_SCALE,
            commaWeight = p.getInt(KEY_COMMA_WEIGHT, DEF_COMMA_WEIGHT) / WEIGHT_SCALE,
            periodWeight = p.getInt(KEY_PERIOD_WEIGHT, DEF_PERIOD_WEIGHT) / WEIGHT_SCALE,
            backspaceExtra = p.getInt(KEY_BACKSPACE_EXTRA, DEF_BACKSPACE_EXTRA) / WEIGHT_SCALE,
            shiftExtra = p.getInt(KEY_SHIFT_EXTRA, DEF_SHIFT_EXTRA) / WEIGHT_SCALE,
            maxSuggestions = p.getInt(KEY_MAX_SUGGESTIONS, DEF_MAX_SUGGESTIONS),
            reSortDelayMs = p.getInt(KEY_RESORT_DELAY, DEF_RESORT_DELAY).toLong(),
            highlightEnabled = p.getBoolean(KEY_HIGHLIGHT_ENABLED, true),
            highlightColor = parseColor(p.getString(KEY_HIGHLIGHT_COLOR, null)),
            showNumberRow = p.getBoolean(KEY_NUMBER_ROW, true),
            hintsEnabled = p.getBoolean(KEY_HINTS_ENABLED, true),
            letterHints = LetterHints.decodeOrDefault(p.getString(KEY_LETTER_HINTS, null)),
            shiftGraceWindowMs = p.getInt(KEY_SHIFT_GRACE, DEF_SHIFT_GRACE).toLong(),
            commaLineNotSentenceStart = p.getBoolean(KEY_COMMA_LINE_NOT_SENTENCE_START, true),
            llmThresholdKey = p.getString(KEY_LLM_THRESHOLD, null),
            keySoundEnabled = p.getBoolean(KEY_KEY_SOUND, false),
            keyHapticsEnabled = p.getBoolean(KEY_KEY_HAPTICS, false),
            longPressDelayMs = p.getInt(KEY_LONGPRESS_DELAY, DEF_LONGPRESS_DELAY).toLong(),
            extraSpaceBelowNumberRowDp = p.getInt(KEY_SPACE_BELOW_NUMBER_ROW, DEF_EXTRA_SPACING),
            extraSpaceAboveSpaceRowDp = p.getInt(KEY_SPACE_ABOVE_SPACE_ROW, DEF_EXTRA_SPACING),
            symbolKeyEnabled = p.getBoolean(KEY_SYMBOL_KEY, true)
        )
        return SettingsMapper.toAdaptSettings(raw)
    }
    
    /**
     * Loads the current C-08 per-key secondary-symbol map for the editor, applying the default
     * fallback. This is the map the running keyboard resolves to as well (see [load]).
     *
     * @param context any valid context
     * @return the validated per-key map, or the default mapping when none is stored
     */
    fun loadLetterHints(context: Context): Map<Char, String> {
        return LetterHints.decodeOrDefault(prefs(context).getString(KEY_LETTER_HINTS, null))
    }
    
    /**
     * Persists an edited C-08 per-key secondary-symbol map. The map is sanitised and encoded by
     * [LetterHints]; writing the [KEY_LETTER_HINTS] key triggers the service's preference listener, so
     * the change reaches the live keyboard (hints + long-press) on the next input view.
     *
     * @param context any valid context
     * @param hints the edited per-key map
     */
    fun saveLetterHints(context: Context, hints: Map<Char, String>) {
        prefs(context).edit().putString(KEY_LETTER_HINTS, LetterHints.encode(hints)).apply()
    }
    
    /**
     * Restores the C-08 per-key symbols to the default mapping (the settings "reset hints" action).
     *
     * @param context any valid context
     */
    fun resetLetterHints(context: Context) {
        prefs(context).edit().remove(KEY_LETTER_HINTS).apply()
    }
    
    /**
     * D-16: presets the default key enlargement from the chosen typing hand (T-04) - a left-hand typist
     * (left index finger or left thumb) gets the enlarged backspace (right side, the awkward reach), a
     * right-hand typist an enlarged shift (left side). Writing the preferences reaches the live keyboard via
     * the service's change listener; the user can still adjust both afterwards. TWO_THUMBS / UNKNOWN have no
     * natural asymmetry and leave the current settings untouched.
     *
     * @param context any valid context
     * @param pattern the chosen typing pattern
     */
    fun applyPatternEnlargement(context: Context, pattern: TypingPattern) {
        val editor = prefs(context).edit()
        when (pattern) {
            TypingPattern.LEFT_INDEX_FINGER, TypingPattern.LEFT_THUMB -> {
                editor.putInt(KEY_BACKSPACE_EXTRA, DEF_BACKSPACE_EXTRA)
                editor.putInt(KEY_SHIFT_EXTRA, DEF_SHIFT_EXTRA)
            }
            
            TypingPattern.RIGHT_INDEX_FINGER, TypingPattern.RIGHT_THUMB -> {
                editor.putInt(KEY_SHIFT_EXTRA, DEF_BACKSPACE_EXTRA)
                editor.putInt(KEY_BACKSPACE_EXTRA, DEF_SHIFT_EXTRA)
            }
            
            TypingPattern.TWO_THUMBS, TypingPattern.UNKNOWN -> return
        }
        editor.apply()
    }
    
    private fun parseColor(value: String?): Int {
        if (value.isNullOrBlank()) {
            return SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR
        }
        return runCatching { Color.parseColor(value) }.getOrDefault(SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR)
    }
}

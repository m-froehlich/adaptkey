package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig

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
 * C-05 (the blacklist) is not stored here - it lives in the SQLite dictionary. C-06 (LLM threshold) is
 * not stored at all because no LLM tier exists yet.
 */
object SettingsStore {
    
    // Preference keys; must match res/xml/settings_preferences.xml.
    const val KEY_SPACE_WEIGHT = "c01_space_weight"
    const val KEY_COMMA_WEIGHT = "c01_comma_weight"
    const val KEY_PERIOD_WEIGHT = "c01_period_weight"
    const val KEY_BACKSPACE_EXTRA = "c01_backspace_extra"
    const val KEY_RESORT_DELAY = "c02_resort_delay_ms"
    const val KEY_MAX_SUGGESTIONS = "c03_max_suggestions"
    const val KEY_HIGHLIGHT_ENABLED = "c04_highlight_enabled"
    const val KEY_HIGHLIGHT_COLOR = "c04_highlight_color"
    const val KEY_NUMBER_ROW = "c09_number_row"
    const val KEY_HINTS_ENABLED = "c08_hints_enabled"
    const val KEY_LETTER_HINTS = "c08_letter_hints"
    const val KEY_SHIFT_GRACE = "c07_shift_grace_ms"
    
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
    
    /** Default stored integer for the re-sort delay slider (C-02, 300 ms). */
    const val DEF_RESORT_DELAY = 300
    
    /** Default stored integer for the maximum-suggestions slider (C-03, 8). */
    const val DEF_MAX_SUGGESTIONS = 8
    
    /** Default stored integer for the shift grace window slider (C-07, 300 ms). */
    const val DEF_SHIFT_GRACE = 300
    
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
            maxSuggestions = p.getInt(KEY_MAX_SUGGESTIONS, DEF_MAX_SUGGESTIONS),
            reSortDelayMs = p.getInt(KEY_RESORT_DELAY, DEF_RESORT_DELAY).toLong(),
            highlightEnabled = p.getBoolean(KEY_HIGHLIGHT_ENABLED, false),
            highlightColor = parseColor(p.getString(KEY_HIGHLIGHT_COLOR, null)),
            showNumberRow = p.getBoolean(KEY_NUMBER_ROW, true),
            hintsEnabled = p.getBoolean(KEY_HINTS_ENABLED, true),
            letterHints = decodeHints(p.getString(KEY_LETTER_HINTS, null)),
            shiftGraceWindowMs = p.getInt(KEY_SHIFT_GRACE, DEF_SHIFT_GRACE).toLong()
        )
        return SettingsMapper.toAdaptSettings(raw)
    }
    
    /**
     * Restores the C-08 per-key symbols to the default mapping (the settings "reset hints" action).
     *
     * @param context any valid context
     */
    fun resetLetterHints(context: Context) {
        prefs(context).edit().remove(KEY_LETTER_HINTS).apply()
    }
    
    private fun parseColor(value: String?): Int {
        if (value.isNullOrBlank()) {
            return SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR
        }
        return runCatching { Color.parseColor(value) }.getOrDefault(SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR)
    }
    
    private fun decodeHints(value: String?): Map<Char, String> {
        if (value.isNullOrBlank()) {
            return KeyboardLayout.DEFAULT_LETTER_HINTS
        }
        val map = HashMap<Char, String>()
        for (pair in value.split(';')) {
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2 && parts[0].length == 1 && parts[1].isNotEmpty()) {
                map[parts[0][0]] = parts[1]
            }
        }
        return if (map.isEmpty()) KeyboardLayout.DEFAULT_LETTER_HINTS else map
    }
}

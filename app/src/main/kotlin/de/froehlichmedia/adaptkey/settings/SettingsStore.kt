// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import de.froehlichmedia.adaptkey.language.ActiveLanguageStore
import de.froehlichmedia.adaptkey.language.Language
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
    const val KEY_HIGHLIGHT_COLOR = "c04_highlight_color"
    const val KEY_NUMBER_ROW = "c09_number_row"
    const val KEY_SHIFT_GRACE = "c07_shift_grace_ms"
    const val KEY_COMMA_LINE_NOT_SENTENCE_START = "c10_comma_line_not_sentence_start"
    const val KEY_LLM_THRESHOLD = "c06_llm_threshold"
    const val KEY_KEY_SOUND = "d05_key_sound"
    const val KEY_KEY_HAPTICS = "d06_key_haptics"
    const val KEY_LONGPRESS_DELAY = "d32_longpress_delay_ms"
    const val KEY_DOUBLE_TAP_DELAY = "double_tap_delay_ms"
    const val KEY_CAPS_LOCK_HAPTICS = "caps_lock_haptics_enabled"
    const val KEY_SPACE_BELOW_NUMBER_ROW = "d55_space_below_number_row"
    const val KEY_SPACE_ABOVE_SPACE_ROW = "d55_space_above_space_row"
    const val KEY_SYMBOL_KEY = "d59_symbol_key"
    const val KEY_DIAGNOSTIC_LOG_ENABLED = "d_diag_enabled"
    const val KEY_PENDING_BLACKLIST_EXPIRY_DAYS = "d177_pending_blacklist_expiry_days"
    const val KEY_SAVE_CREDENTIALS = "d224_save_credentials"
    const val KEY_CONTACTS_SUGGESTIONS_ENABLED = "d191_contacts_suggestions_enabled"
    const val KEY_DOUBLE_TAP_BACKSPACE_UNDO = "d348_double_tap_backspace_undo"
    const val KEY_AUTO_SPLIT_MODE = "d352_auto_split_mode"
    // D-407: the former, separate D-234 "d234_autocorrect_enabled" toggle is gone - merged into this one
    // slider's own "Off" position (AutocorrectAggressiveness.OFF_KEY), no migration of any old stored value.
    const val KEY_AUTOCORRECT_AGGRESSIVENESS = "d353_autocorrect_aggressiveness"
    const val KEY_SUSTAINED_LANGUAGE_SWITCH_THRESHOLD = "d398_sustained_language_switch_threshold"
    const val KEY_LEARNED_WORD_EXPIRY_WINDOW = "d389_learned_word_expiry_window"
    
    /**
     * D-389: epoch millis the daily [de.froehlichmedia.adaptkey.dictionary.LearnedWordExpirySweep] last
     * actually ran - internal throttling bookkeeping, not a user-facing setting (no `settings_preferences.xml`
     * row), same shape as [KEY_CALIBRATION_OFFERED] below. Excluded from export for the same reason: a
     * restored device should re-derive its own sweep cadence from its own recent history, not inherit
     * another device's.
     */
    const val KEY_LEARNED_WORD_EXPIRY_LAST_SWEEP = "d389_learned_word_expiry_last_sweep"
    
    /**
     * D-304: the one-time K-01 calibration-offer flag ([SettingsActivity.SettingsFragment.maybeOfferCalibration]) -
     * not a `settings_preferences.xml` row at all, but still stored in this same default preferences file.
     * Moved here (from a private constant on that fragment) so [exportableSettings] can exclude it by name
     * instead of it silently falling through that function's own "any other stored key" fallback.
     */
    const val KEY_CALIBRATION_OFFERED = "k01_calibration_offered"
    
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
    
    /** Default stored integer for the double-tap Shift delay slider (G-05, 400 ms). */
    const val DEF_DOUBLE_TAP_DELAY = 400
    
    /** Default stored boolean for the Caps Lock haptic feedback (G-06, on). */
    const val DEF_CAPS_LOCK_HAPTICS = true
    
    /** Default stored integer for both D-55 extra-row-spacing sliders (7 dp). */
    const val DEF_EXTRA_SPACING = AdaptSettings.DEFAULT_EXTRA_SPACING_DP
    
    /** Default stored boolean for the D-348 double-tap Backspace undo toggle (off). */
    const val DEF_DOUBLE_TAP_BACKSPACE_UNDO = false
    
    /** Default stored integer for the D-177 pending-blacklist expiry slider (7 days). */
    const val DEF_PENDING_BLACKLIST_EXPIRY_DAYS = AdaptSettings.DEFAULT_PENDING_BLACKLIST_EXPIRY_DAYS
    
    /** Default stored integer for the D-398 sustained-language-switch-threshold slider (5 words). */
    const val DEF_SUSTAINED_LANGUAGE_SWITCH_THRESHOLD = AdaptSettings.DEFAULT_SUSTAINED_LANGUAGE_SWITCH_THRESHOLD
    
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
        val highlightColorRaw = p.getString(KEY_HIGHLIGHT_COLOR, null)
        val raw = RawSettings(
            spaceWeight = p.getInt(KEY_SPACE_WEIGHT, DEF_SPACE_WEIGHT) / WEIGHT_SCALE,
            commaWeight = p.getInt(KEY_COMMA_WEIGHT, DEF_COMMA_WEIGHT) / WEIGHT_SCALE,
            periodWeight = p.getInt(KEY_PERIOD_WEIGHT, DEF_PERIOD_WEIGHT) / WEIGHT_SCALE,
            backspaceExtra = p.getInt(KEY_BACKSPACE_EXTRA, DEF_BACKSPACE_EXTRA) / WEIGHT_SCALE,
            shiftExtra = p.getInt(KEY_SHIFT_EXTRA, DEF_SHIFT_EXTRA) / WEIGHT_SCALE,
            maxSuggestions = p.getInt(KEY_MAX_SUGGESTIONS, DEF_MAX_SUGGESTIONS),
            reSortDelayMs = p.getInt(KEY_RESORT_DELAY, DEF_RESORT_DELAY).toLong(),
            highlightEnabled = highlightColorRaw != NO_HIGHLIGHT_VALUE,
            highlightColor = parseColor(highlightColorRaw),
            showNumberRow = p.getBoolean(KEY_NUMBER_ROW, true),
            letterHints = loadLetterHints(context),
            shiftGraceWindowMs = p.getInt(KEY_SHIFT_GRACE, DEF_SHIFT_GRACE).toLong(),
            commaLineNotSentenceStart = p.getBoolean(KEY_COMMA_LINE_NOT_SENTENCE_START, true),
            llmThresholdKey = p.getString(KEY_LLM_THRESHOLD, null),
            keySoundEnabled = p.getBoolean(KEY_KEY_SOUND, false),
            keyHapticsEnabled = p.getBoolean(KEY_KEY_HAPTICS, false),
            longPressDelayMs = p.getInt(KEY_LONGPRESS_DELAY, DEF_LONGPRESS_DELAY).toLong(),
            doubleTapDelayMs = p.getInt(KEY_DOUBLE_TAP_DELAY, DEF_DOUBLE_TAP_DELAY).toLong(),
            capsLockHapticsEnabled = p.getBoolean(KEY_CAPS_LOCK_HAPTICS, DEF_CAPS_LOCK_HAPTICS),
            extraSpaceBelowNumberRowDp = p.getInt(KEY_SPACE_BELOW_NUMBER_ROW, DEF_EXTRA_SPACING),
            extraSpaceAboveSpaceRowDp = p.getInt(KEY_SPACE_ABOVE_SPACE_ROW, DEF_EXTRA_SPACING),
            symbolKeyEnabled = p.getBoolean(KEY_SYMBOL_KEY, true),
            diagnosticLogEnabled = p.getBoolean(KEY_DIAGNOSTIC_LOG_ENABLED, false),
            pendingBlacklistExpiryDays = p.getInt(KEY_PENDING_BLACKLIST_EXPIRY_DAYS, DEF_PENDING_BLACKLIST_EXPIRY_DAYS),
            saveCredentials = p.getBoolean(KEY_SAVE_CREDENTIALS, true),
            contactsSuggestionsEnabled = p.getBoolean(KEY_CONTACTS_SUGGESTIONS_ENABLED, false),
            doubleTapBackspaceUndo = p.getBoolean(KEY_DOUBLE_TAP_BACKSPACE_UNDO, DEF_DOUBLE_TAP_BACKSPACE_UNDO),
            autoSplitModeKey = p.getString(KEY_AUTO_SPLIT_MODE, null),
            autocorrectAggressivenessKey = p.getString(KEY_AUTOCORRECT_AGGRESSIVENESS, null),
            sustainedLanguageSwitchThreshold = p.getInt(
                KEY_SUSTAINED_LANGUAGE_SWITCH_THRESHOLD, DEF_SUSTAINED_LANGUAGE_SWITCH_THRESHOLD
            ),
            learnedWordExpiryWindowKey = p.getString(KEY_LEARNED_WORD_EXPIRY_WINDOW, null)
        )
        return SettingsMapper.toAdaptSettings(raw)
    }
    
    /**
     * Loads the C-08 per-key secondary-symbol map, always [language]'s own bundled default - no longer
     * user-configurable (the former per-key override editor combined incorrectly with a per-language
     * default, silently reintroducing cross-language hint bleed the moment any single symbol was
     * customised, so it was removed entirely rather than fixed; see D-281 for the per-language default
     * mechanism itself).
     * 
     * @param context any valid context
     * @param language the language whose own default hint set to resolve; defaults to whichever language
     *        is currently active ([ActiveLanguageStore])
     * @return [language]'s own default per-key map, or the compiled-in default when [language] has none
     */
    fun loadLetterHints(context: Context, language: Language = ActiveLanguageStore.load(context)): Map<Char, String> {
        return LanguageLetterHintsLoader.loadFor(context, language) ?: KeyboardLayout.DEFAULT_LETTER_HINTS
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
        if (value.isNullOrBlank() || value == NO_HIGHLIGHT_VALUE) {
            return SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR
        }
        return runCatching { Color.parseColor(value) }.getOrDefault(SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR)
    }
    
    /**
     * D-298: the C-04 "no highlighting" list entry's own stored value - folds [RawSettings.highlightEnabled]
     * into the single `c04_highlight_color` preference instead of a separate switch, since a colour picker
     * that already has to list every real choice can list "none" as one more of them just as easily.
     * 
     * Not private: [SettingsActivity.SettingsFragment] also needs it to recognise the sentinel when
     * colouring the C-04 preference row/dialog (D-302).
     */
    const val NO_HIGHLIGHT_VALUE = "none"
    
    /**
     * D-278/D-304: the settings screen's own current category/row order (`res/xml/settings_preferences.xml`),
     * used by [exportableSettings] so an exported backup file's settings section reads in the same order the
     * user actually sees on screen, not [SharedPreferences.getAll]'s own arbitrary hash order. Every key in
     * [EXPORT_EXCLUDED_KEYS] is deliberately absent - see that set's own KDoc for why.
     */
    private val EXPORT_SETTINGS_KEY_ORDER: List<String> = listOf(
        KEY_PENDING_BLACKLIST_EXPIRY_DAYS,
        KEY_LEARNED_WORD_EXPIRY_WINDOW,
        KEY_SAVE_CREDENTIALS,
        KEY_CONTACTS_SUGGESTIONS_ENABLED,
        KEY_AUTO_SPLIT_MODE,
        KEY_AUTOCORRECT_AGGRESSIVENESS,
        KEY_SUSTAINED_LANGUAGE_SWITCH_THRESHOLD,
        KEY_DOUBLE_TAP_BACKSPACE_UNDO,
        KEY_HIGHLIGHT_COLOR,
        KEY_RESORT_DELAY,
        KEY_MAX_SUGGESTIONS,
        KEY_COMMA_LINE_NOT_SENTENCE_START,
        KEY_SHIFT_GRACE,
        KEY_LLM_THRESHOLD,
        KEY_NUMBER_ROW,
        KEY_SYMBOL_KEY,
        KEY_SPACE_BELOW_NUMBER_ROW,
        KEY_SPACE_ABOVE_SPACE_ROW,
        KEY_LONGPRESS_DELAY,
        KEY_DOUBLE_TAP_DELAY,
        KEY_SPACE_WEIGHT,
        KEY_COMMA_WEIGHT,
        KEY_PERIOD_WEIGHT,
        KEY_BACKSPACE_EXTRA,
        KEY_SHIFT_EXTRA,
        KEY_KEY_SOUND,
        KEY_KEY_HAPTICS,
        KEY_CAPS_LOCK_HAPTICS
    )
    
    /**
     * D-304: stored keys in this default preferences file that must never leave this device via export -
     * either a per-device debugging aid ([KEY_DIAGNOSTIC_LOG_ENABLED]) or internal one-time-offer bookkeeping
     * ([KEY_CALIBRATION_OFFERED], whose own underlying data - [de.froehlichmedia.adaptkey.touch.OffsetStore] -
     * lives in a wholly separate, never-exported preferences file anyway; importing this flag alone would
     * only ever suppress the one-time K-01 offer on a device that has none of the calibration data behind it).
     * 
     * Not private: [de.froehlichmedia.adaptkey.backup.BackupImporter] also excludes these, for defence in
     * depth against a pre-D-304 export file or a hand-edited one that still carries either key.
     */
    val EXPORT_EXCLUDED_KEYS = setOf(KEY_DIAGNOSTIC_LOG_ENABLED, KEY_CALIBRATION_OFFERED, KEY_LEARNED_WORD_EXPIRY_LAST_SWEEP)
    
    /**
     * D-278/D-304: every currently-stored preference value meant for the export/import backup (§21), in
     * [EXPORT_SETTINGS_KEY_ORDER]'s own display order - [de.froehlichmedia.adaptkey.backup.BackupExporter]'s
     * source for the bundle's settings section, replacing a raw [SharedPreferences.getAll] dump (whose
     * iteration order is an implementation-detail hash order, not the settings screen's own order, and would
     * carry every one of [EXPORT_EXCLUDED_KEYS] along with everything else).
     * 
     * Any stored key this build's [EXPORT_SETTINGS_KEY_ORDER] does not (yet) list, and that is not one of
     * [EXPORT_EXCLUDED_KEYS] either - a future preference this list has not been updated for, or a stray
     * leftover from a removed one - is still included, appended afterwards in alphabetical order, so a gap in
     * that list can never silently drop data from an export.
     * 
     * @param context any valid context
     * @return the exportable settings, key-value pairs in display order
     */
    fun exportableSettings(context: Context): Map<String, Any> {
        val all = (prefs(context).all.filterValues { it != null }.mapValues { it.value as Any }) - EXPORT_EXCLUDED_KEYS
        val result = LinkedHashMap<String, Any>()
        EXPORT_SETTINGS_KEY_ORDER.forEach { key -> all[key]?.let { result[key] = it } }
        (all.keys - result.keys).sorted().forEach { key -> result[key] = all.getValue(key) }
        return result
    }
}

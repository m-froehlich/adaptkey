// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyProportions
import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import de.froehlichmedia.adaptkey.prediction.LlmActivationThreshold
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig

/**
 * Raw, primitive settings values exactly as read from persistent storage, before validation.
 *
 * Each field defaults to its spec default, so a freshly installed app (no stored values) resolves to
 * [AdaptSettings.DEFAULT]. The float weights are already decoded from their slider integer encoding by
 * the storage layer; clamping into the spec ranges is the job of [SettingsMapper], keeping this a plain
 * value holder.
 */
data class RawSettings(
    val spaceWeight: Float = KeyProportions.DEFAULT.spaceWeight,
    val commaWeight: Float = KeyProportions.DEFAULT.commaWeight,
    val periodWeight: Float = KeyProportions.DEFAULT.periodWeight,
    val backspaceExtra: Float = KeyProportions.DEFAULT.backspaceExtra,
    val shiftExtra: Float = KeyProportions.DEFAULT.shiftExtra,
    val maxSuggestions: Int = SuggestionConfig().maxSuggestions,
    val reSortDelayMs: Long = SuggestionConfig().reSortDelayMs,
    val highlightEnabled: Boolean = SuggestionConfig().highlightEnabled,
    val highlightColor: Int = SuggestionConfig.DEFAULT_HIGHLIGHT_COLOR,
    val showNumberRow: Boolean = true,
    val hintsEnabled: Boolean = true,
    val letterHints: Map<Char, String> = KeyboardLayout.DEFAULT_LETTER_HINTS,
    val shiftGraceWindowMs: Long = AdaptSettings.DEFAULT_SHIFT_GRACE_WINDOW_MS,
    val commaLineNotSentenceStart: Boolean = true,
    val llmThresholdKey: String? = null,
    val keySoundEnabled: Boolean = false,
    val keyHapticsEnabled: Boolean = false,
    val longPressDelayMs: Long = AdaptSettings.DEFAULT_LONGPRESS_DELAY_MS,
    val extraSpaceBelowNumberRowDp: Int = AdaptSettings.DEFAULT_EXTRA_SPACING_DP,
    val extraSpaceAboveSpaceRowDp: Int = AdaptSettings.DEFAULT_EXTRA_SPACING_DP,
    val symbolKeyEnabled: Boolean = true,
    val tier3Enabled: Boolean = true
)

/**
 * Pure, Android-free translation of persisted [RawSettings] into the validated [AdaptSettings] the
 * keyboard consumes. Every numeric value is clamped into the spec range (§10) before it reaches the
 * data-class init contracts, so an out-of-range or corrupt stored value degrades gracefully to the
 * nearest valid value instead of crashing. This object is the unit-tested core of the settings layer.
 */
object SettingsMapper {
    
    /** C-03 minimum maximum-suggestion count (§10 recommended range 6-10). */
    const val MIN_MAX_SUGGESTIONS = 6
    
    /** C-03 maximum maximum-suggestion count (§10 recommended range 6-10). */
    const val MAX_MAX_SUGGESTIONS = 10
    
    /** C-02 minimum re-sort delay in ms (§10). */
    const val MIN_RESORT_DELAY_MS = 0L
    
    /** C-02 maximum re-sort delay in ms (§10). */
    const val MAX_RESORT_DELAY_MS = 600L
    
    /** C-07 minimum shift grace window in ms (§10). */
    const val MIN_SHIFT_GRACE_MS = 0L
    
    /** C-07 maximum shift grace window in ms (§10). */
    const val MAX_SHIFT_GRACE_MS = 500L
    
    /** D-32 minimum long-press delay in ms. */
    const val MIN_LONGPRESS_DELAY_MS = 150L
    
    /** D-32 maximum long-press delay in ms. */
    const val MAX_LONGPRESS_DELAY_MS = 600L
    
    /** D-55 minimum extra row spacing in dp. */
    const val MIN_EXTRA_SPACING_DP = 0
    
    /** D-55 maximum extra row spacing in dp. */
    const val MAX_EXTRA_SPACING_DP = 25
    
    /** C-01 lower bound for the space-bar weight (must stay a usable, positive width). */
    const val MIN_SPACE_WEIGHT = 1.0f
    
    /** C-01 upper bound for the space-bar weight. */
    const val MAX_SPACE_WEIGHT = 6.0f
    
    /** C-01 lower bound for the comma / full-stop weights. */
    const val MIN_PUNCT_WEIGHT = 0.5f
    
    /** C-01 upper bound for the comma / full-stop weights. */
    const val MAX_PUNCT_WEIGHT = 3.0f
    
    /** C-01 lower bound for the backspace surcharge (L-04). */
    const val MIN_BACKSPACE_EXTRA = 0.0f
    
    /** C-01 upper bound for the backspace surcharge (L-04). */
    const val MAX_BACKSPACE_EXTRA = 0.5f
    
    /**
     * Maps the C-01 weights, clamping each into its keyboard-sane range. Only the four spec-listed
     * weights (space, comma, full stop, backspace surcharge) are configurable; the remaining
     * [KeyProportions] weights keep their defaults.
     *
     * @param raw the raw stored values
     * @return a valid [KeyProportions]
     */
    fun toKeyProportions(raw: RawSettings): KeyProportions {
        return KeyProportions(
            spaceWeight = raw.spaceWeight.coerceIn(MIN_SPACE_WEIGHT, MAX_SPACE_WEIGHT),
            commaWeight = raw.commaWeight.coerceIn(MIN_PUNCT_WEIGHT, MAX_PUNCT_WEIGHT),
            periodWeight = raw.periodWeight.coerceIn(MIN_PUNCT_WEIGHT, MAX_PUNCT_WEIGHT),
            backspaceExtra = raw.backspaceExtra.coerceIn(MIN_BACKSPACE_EXTRA, MAX_BACKSPACE_EXTRA),
            shiftExtra = raw.shiftExtra.coerceIn(MIN_BACKSPACE_EXTRA, MAX_BACKSPACE_EXTRA)
        )
    }
    
    /**
     * Maps the C-02 / C-03 / C-04 suggestion settings, clamping the count and the delay into their
     * spec ranges. Any int is a valid ARGB highlight colour, so it is passed through unchanged.
     *
     * @param raw the raw stored values
     * @return a valid [SuggestionConfig]
     */
    fun toSuggestionConfig(raw: RawSettings): SuggestionConfig {
        return SuggestionConfig(
            maxSuggestions = raw.maxSuggestions.coerceIn(MIN_MAX_SUGGESTIONS, MAX_MAX_SUGGESTIONS),
            reSortDelayMs = raw.reSortDelayMs.coerceIn(MIN_RESORT_DELAY_MS, MAX_RESORT_DELAY_MS),
            highlightEnabled = raw.highlightEnabled,
            highlightColor = raw.highlightColor
        )
    }
    
    /**
     * Clamps the C-07 shift grace window into its spec range (0-500 ms).
     *
     * @param raw the raw stored values
     * @return the clamped grace window in ms
     */
    fun shiftGraceWindowMs(raw: RawSettings): Long {
        return raw.shiftGraceWindowMs.coerceIn(MIN_SHIFT_GRACE_MS, MAX_SHIFT_GRACE_MS)
    }
    
    /**
     * Resolves the C-06 mini-LLM activation threshold, falling back to the spec default for an unknown,
     * blank or missing stored value (the validation point for this enum-valued setting; §9 / §10).
     *
     * @param raw the raw stored values
     * @return the resolved [LlmActivationThreshold]
     */
    fun toLlmActivationThreshold(raw: RawSettings): LlmActivationThreshold {
        return LlmActivationThreshold.fromKey(raw.llmThresholdKey)
    }
    
    /**
     * Resolves the full validated configuration. An empty per-key hint map falls back to the default
     * mapping so the keyboard never ends up with no secondary symbols at all.
     *
     * @param raw the raw stored values
     * @return the validated [AdaptSettings]
     */
    fun toAdaptSettings(raw: RawSettings): AdaptSettings {
        val hints = raw.letterHints.takeIf { it.isNotEmpty() } ?: KeyboardLayout.DEFAULT_LETTER_HINTS
        return AdaptSettings(
            keyProportions = toKeyProportions(raw),
            suggestionConfig = toSuggestionConfig(raw),
            showNumberRow = raw.showNumberRow,
            hintsEnabled = raw.hintsEnabled,
            letterHints = hints,
            shiftGraceWindowMs = shiftGraceWindowMs(raw),
            commaLineNotSentenceStart = raw.commaLineNotSentenceStart,
            llmActivationThreshold = toLlmActivationThreshold(raw),
            keySoundEnabled = raw.keySoundEnabled,
            keyHapticsEnabled = raw.keyHapticsEnabled,
            longPressDelayMs = raw.longPressDelayMs.coerceIn(MIN_LONGPRESS_DELAY_MS, MAX_LONGPRESS_DELAY_MS),
            extraSpaceBelowNumberRowDp = raw.extraSpaceBelowNumberRowDp.coerceIn(MIN_EXTRA_SPACING_DP, MAX_EXTRA_SPACING_DP),
            extraSpaceAboveSpaceRowDp = raw.extraSpaceAboveSpaceRowDp.coerceIn(MIN_EXTRA_SPACING_DP, MAX_EXTRA_SPACING_DP),
            symbolKeyEnabled = raw.symbolKeyEnabled,
            tier3Enabled = raw.tier3Enabled
        )
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyProportions
import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import de.froehlichmedia.adaptkey.prediction.LlmActivationThreshold
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Unit tests for the pure settings mapper (C-01 … C-09): validation, clamping and default fallbacks.
 */
class SettingsMapperTest {
    
    @Test
    fun `default raw settings resolve to the spec defaults`() {
        val resolved = SettingsMapper.toAdaptSettings(RawSettings())
        
        assertEquals(KeyProportions.DEFAULT, resolved.keyProportions)
        assertEquals(SuggestionConfig(), resolved.suggestionConfig)
        assertTrue(resolved.showNumberRow)
        assertTrue(resolved.hintsEnabled)
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, resolved.letterHints)
        assertEquals(AdaptSettings.DEFAULT_SHIFT_GRACE_WINDOW_MS, resolved.shiftGraceWindowMs)
        assertTrue(resolved.commaLineNotSentenceStart)
        assertEquals(LlmActivationThreshold.DEFAULT, resolved.llmActivationThreshold)
    }
    
    @Test
    fun `the C-06 threshold resolves from the stored key`() {
        assertEquals(LlmActivationThreshold.HIGH, SettingsMapper.toAdaptSettings(RawSettings(llmThresholdKey = "high")).llmActivationThreshold)
        assertEquals(LlmActivationThreshold.LOW, SettingsMapper.toLlmActivationThreshold(RawSettings(llmThresholdKey = "low")))
    }
    
    @Test
    fun `an unknown C-06 threshold key falls back to the default`() {
        assertEquals(LlmActivationThreshold.DEFAULT, SettingsMapper.toLlmActivationThreshold(RawSettings(llmThresholdKey = "bogus")))
    }
    
    @Test
    fun `the comma-line rule flag passes through (C-10)`() {
        assertFalse(SettingsMapper.toAdaptSettings(RawSettings(commaLineNotSentenceStart = false)).commaLineNotSentenceStart)
    }
    
    @Test
    fun `maxSuggestions is clamped into the recommended range`() {
        assertEquals(SettingsMapper.MIN_MAX_SUGGESTIONS, SettingsMapper.toSuggestionConfig(RawSettings(maxSuggestions = 0)).maxSuggestions)
        assertEquals(SettingsMapper.MAX_MAX_SUGGESTIONS, SettingsMapper.toSuggestionConfig(RawSettings(maxSuggestions = 99)).maxSuggestions)
        assertEquals(7, SettingsMapper.toSuggestionConfig(RawSettings(maxSuggestions = 7)).maxSuggestions)
    }
    
    @Test
    fun `reSortDelay is clamped into the spec range`() {
        assertEquals(SettingsMapper.MIN_RESORT_DELAY_MS, SettingsMapper.toSuggestionConfig(RawSettings(reSortDelayMs = -50L)).reSortDelayMs)
        assertEquals(SettingsMapper.MAX_RESORT_DELAY_MS, SettingsMapper.toSuggestionConfig(RawSettings(reSortDelayMs = 5_000L)).reSortDelayMs)
        assertEquals(250L, SettingsMapper.toSuggestionConfig(RawSettings(reSortDelayMs = 250L)).reSortDelayMs)
    }
    
    @Test
    fun `highlight flag and colour pass through unchanged`() {
        val config = SettingsMapper.toSuggestionConfig(RawSettings(highlightEnabled = true, highlightColor = 0x12345678))
        assertTrue(config.highlightEnabled)
        assertEquals(0x12345678, config.highlightColor)
    }
    
    @Test
    fun `D-55 extra row spacing is clamped into the 0-25 range`() {
        val low = SettingsMapper.toAdaptSettings(RawSettings(extraSpaceBelowNumberRowDp = -5, extraSpaceAboveSpaceRowDp = -1))
        assertEquals(SettingsMapper.MIN_EXTRA_SPACING_DP, low.extraSpaceBelowNumberRowDp)
        assertEquals(SettingsMapper.MIN_EXTRA_SPACING_DP, low.extraSpaceAboveSpaceRowDp)
        val high = SettingsMapper.toAdaptSettings(RawSettings(extraSpaceBelowNumberRowDp = 99, extraSpaceAboveSpaceRowDp = 40))
        assertEquals(SettingsMapper.MAX_EXTRA_SPACING_DP, high.extraSpaceBelowNumberRowDp)
        assertEquals(SettingsMapper.MAX_EXTRA_SPACING_DP, high.extraSpaceAboveSpaceRowDp)
        val ok = SettingsMapper.toAdaptSettings(RawSettings(extraSpaceBelowNumberRowDp = 12, extraSpaceAboveSpaceRowDp = 3))
        assertEquals(12, ok.extraSpaceBelowNumberRowDp)
        assertEquals(3, ok.extraSpaceAboveSpaceRowDp)
    }
    
    @Test
    fun `D-59 symbol-key flag passes through unchanged`() {
        assertFalse(SettingsMapper.toAdaptSettings(RawSettings(symbolKeyEnabled = false)).symbolKeyEnabled)
        assertTrue(SettingsMapper.toAdaptSettings(RawSettings(symbolKeyEnabled = true)).symbolKeyEnabled)
    }
    
    @Test
    fun `D-126 tier3Enabled flag passes through unchanged, defaulting to on`() {
        assertTrue(SettingsMapper.toAdaptSettings(RawSettings()).tier3Enabled)
        assertFalse(SettingsMapper.toAdaptSettings(RawSettings(tier3Enabled = false)).tier3Enabled)
    }
    
    @Test
    fun `D-139 diagnosticLogEnabled flag passes through unchanged, defaulting to off`() {
        assertFalse(SettingsMapper.toAdaptSettings(RawSettings()).diagnosticLogEnabled)
        assertTrue(SettingsMapper.toAdaptSettings(RawSettings(diagnosticLogEnabled = true)).diagnosticLogEnabled)
    }
    
    @Test
    fun `D-190 neverRecordCredentials flag passes through unchanged, defaulting to off`() {
        assertFalse(SettingsMapper.toAdaptSettings(RawSettings()).neverRecordCredentials)
        assertTrue(SettingsMapper.toAdaptSettings(RawSettings(neverRecordCredentials = true)).neverRecordCredentials)
    }
    
    
    @Test
    fun `shiftGraceWindow is clamped into the spec range`() {
        assertEquals(SettingsMapper.MIN_SHIFT_GRACE_MS, SettingsMapper.shiftGraceWindowMs(RawSettings(shiftGraceWindowMs = -1L)))
        assertEquals(SettingsMapper.MAX_SHIFT_GRACE_MS, SettingsMapper.shiftGraceWindowMs(RawSettings(shiftGraceWindowMs = 9_000L)))
        assertEquals(120L, SettingsMapper.shiftGraceWindowMs(RawSettings(shiftGraceWindowMs = 120L)))
    }
    
    @Test
    fun `out-of-range weights are clamped and never violate the KeyProportions contract`() {
        val raw = RawSettings(
            spaceWeight = 99f,
            commaWeight = -3f,
            periodWeight = 0f,
            backspaceExtra = 9f
        )
        val proportions = assertDoesNotThrow { SettingsMapper.toKeyProportions(raw) }
        
        assertEquals(SettingsMapper.MAX_SPACE_WEIGHT, proportions.spaceWeight)
        assertEquals(SettingsMapper.MIN_PUNCT_WEIGHT, proportions.commaWeight)
        assertEquals(SettingsMapper.MIN_PUNCT_WEIGHT, proportions.periodWeight)
        assertEquals(SettingsMapper.MAX_BACKSPACE_EXTRA, proportions.backspaceExtra)
    }
    
    @Test
    fun `in-range weights pass through unchanged`() {
        val raw = RawSettings(spaceWeight = 2.5f, commaWeight = 1.2f, periodWeight = 1.6f, backspaceExtra = 0.2f)
        val proportions = SettingsMapper.toKeyProportions(raw)
        
        assertEquals(2.5f, proportions.spaceWeight)
        assertEquals(1.2f, proportions.commaWeight)
        assertEquals(1.6f, proportions.periodWeight)
        assertEquals(0.2f, proportions.backspaceExtra)
    }
    
    @Test
    fun `an empty hint map falls back to the default mapping`() {
        val resolved = SettingsMapper.toAdaptSettings(RawSettings(letterHints = emptyMap()))
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, resolved.letterHints)
    }
    
    @Test
    fun `a custom hint map is preserved`() {
        val custom = mapOf('q' to "!", 'e' to "?")
        val resolved = SettingsMapper.toAdaptSettings(RawSettings(letterHints = custom))
        assertEquals(custom, resolved.letterHints)
    }
    
    @Test
    fun `number row and hints toggles pass through`() {
        val resolved = SettingsMapper.toAdaptSettings(RawSettings(showNumberRow = false, hintsEnabled = false))
        assertFalse(resolved.showNumberRow)
        assertFalse(resolved.hintsEnabled)
    }
    
    @Test
    fun `the feedback toggles default off and pass through when set (D-05, D-06)`() {
        val defaults = SettingsMapper.toAdaptSettings(RawSettings())
        assertFalse(defaults.keySoundEnabled)
        assertFalse(defaults.keyHapticsEnabled)
        
        val enabled = SettingsMapper.toAdaptSettings(RawSettings(keySoundEnabled = true, keyHapticsEnabled = true))
        assertTrue(enabled.keySoundEnabled)
        assertTrue(enabled.keyHapticsEnabled)
    }
}

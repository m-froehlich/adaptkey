// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import androidx.preference.PreferenceManager
import de.froehlichmedia.adaptkey.prediction.LlmActivationThreshold
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) for the Android-facing [SettingsStore]: it reads the same default
 * SharedPreferences the settings screen writes and resolves them through the pure mapper. Verifies the
 * IO wiring the pure [SettingsMapperTest] cannot cover.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsStoreRoboTest {
    
    @Test
    fun defaultsResolveWhenNothingStored() {
        val context = RuntimeEnvironment.getApplication()
        val settings = SettingsStore.load(context)
        assertEquals(LlmActivationThreshold.DEFAULT, settings.llmActivationThreshold)
        assertEquals(true, settings.commaLineNotSentenceStart)
    }
    
    @Test
    fun storedValuesAreReadAndClamped() {
        val context = RuntimeEnvironment.getApplication()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(SettingsStore.KEY_LLM_THRESHOLD, "high")
            .putInt(SettingsStore.KEY_MAX_SUGGESTIONS, 99) // out of range -> clamped
            .putBoolean(SettingsStore.KEY_COMMA_LINE_NOT_SENTENCE_START, false)
            .commit()
        
        val settings = SettingsStore.load(context)
        assertEquals(LlmActivationThreshold.HIGH, settings.llmActivationThreshold)
        assertEquals(SettingsMapper.MAX_MAX_SUGGESTIONS, settings.suggestionConfig.maxSuggestions)
        assertEquals(false, settings.commaLineNotSentenceStart)
    }
}

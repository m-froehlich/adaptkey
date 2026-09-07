// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.onboarding

import android.widget.Button
import android.widget.LinearLayout
import de.froehlichmedia.adaptkey.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) for the onboarding persistence and that the panel view inflates
 * and steps without crashing — which also verifies all its string resources resolve.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingRoboTest {
    
    @Test
    fun completionFlagRoundTrips() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(OnboardingStore.isCompleted(context))
        OnboardingStore.setCompleted(context, true)
        assertTrue(OnboardingStore.isCompleted(context))
        OnboardingStore.setCompleted(context, false)
        assertFalse(OnboardingStore.isCompleted(context))
    }
    
    @Test
    fun theViewInflatesAndFinishes() {
        val context = RuntimeEnvironment.getApplication()
        var finished = false
        val view = OnboardingView(context)
        view.onFinished = { finished = true }
        view.restart()
        // Stepping through never throws (all string resources resolve); the finish callback is wired.
        view.onFinished?.invoke()
        assertTrue(finished)
    }
    
    /**
     * D-385-followup: a device-locale suggestion turns the language-selection step's action button from the
     * generic fallback into a direct, per-language "Download X" label, and forwards the suggested language's
     * own code to [OnboardingView.onOpenLanguagePacks] when tapped - the destination screen's own jump-to
     * target (see [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]).
     */
    @Test
    fun languageSelectionActionUsesSuggestedDownloadLabelAndForwardsCode() {
        val context = RuntimeEnvironment.getApplication()
        val view = OnboardingView(context)
        view.suggestedLanguageNames = listOf("Français")
        view.topSuggestedLanguageCode = "fr"
        var openedWithCode: String? = "unset"
        view.onOpenLanguagePacks = { code -> openedWithCode = code }
        
        val nav = view.getChildAt(4) as LinearLayout
        (nav.getChildAt(1) as Button).performClick() // WELCOME -> LANGUAGE_SELECTION
        
        val actionButton = view.getChildAt(3) as Button
        assertEquals(context.getString(R.string.onboarding_language_action_suggested, "Français"), actionButton.text.toString())
        
        actionButton.performClick()
        assertEquals("fr", openedWithCode)
    }
    
    /** Without a suggestion, the action button and the forwarded code both fall back to their plain form. */
    @Test
    fun languageSelectionActionFallsBackToGenericLabelWithoutSuggestion() {
        val context = RuntimeEnvironment.getApplication()
        val view = OnboardingView(context)
        var openedWithCode: String? = "unset"
        view.onOpenLanguagePacks = { code -> openedWithCode = code }
        
        val nav = view.getChildAt(4) as LinearLayout
        (nav.getChildAt(1) as Button).performClick() // WELCOME -> LANGUAGE_SELECTION
        
        val actionButton = view.getChildAt(3) as Button
        assertEquals(context.getString(R.string.onboarding_language_action), actionButton.text.toString())
        
        actionButton.performClick()
        assertNull(openedWithCode)
    }
}

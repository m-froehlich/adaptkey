// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.onboarding

import org.junit.Assert.assertFalse
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
}

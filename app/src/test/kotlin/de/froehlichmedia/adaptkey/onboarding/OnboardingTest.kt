// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.onboarding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure onboarding step sequencing.
 */
class OnboardingTest {
    
    @Test
    fun `the flow starts at welcome and has four steps`() {
        assertEquals(OnboardingStep.WELCOME, Onboarding.first())
        assertEquals(4, Onboarding.count())
    }
    
    @Test
    fun `next advances through the steps and ends after the last`() {
        assertEquals(OnboardingStep.LANGUAGE_SELECTION, Onboarding.next(OnboardingStep.WELCOME))
        assertEquals(OnboardingStep.MODEL_IMPORT, Onboarding.next(OnboardingStep.LANGUAGE_SELECTION))
        assertEquals(OnboardingStep.CALIBRATION, Onboarding.next(OnboardingStep.MODEL_IMPORT))
        assertNull(Onboarding.next(OnboardingStep.CALIBRATION))
    }
    
    @Test
    fun `isLast is only true for the final step`() {
        assertFalse(Onboarding.isLast(OnboardingStep.WELCOME))
        assertFalse(Onboarding.isLast(OnboardingStep.LANGUAGE_SELECTION))
        assertTrue(Onboarding.isLast(OnboardingStep.CALIBRATION))
    }
    
    @Test
    fun `position is one-based`() {
        assertEquals(1, Onboarding.position(OnboardingStep.WELCOME))
        assertEquals(2, Onboarding.position(OnboardingStep.LANGUAGE_SELECTION))
        assertEquals(4, Onboarding.position(OnboardingStep.CALIBRATION))
    }
}

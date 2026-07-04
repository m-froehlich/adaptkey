// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.onboarding

/**
 * The first-run onboarding steps, shown above the keyboard on first use and reachable again later.
 *
 * The order is the guided flow the user asked for: the app and its core promises first, then how to
 * optionally add the mini-LLM, then the initial touch calibration.
 */
enum class OnboardingStep {
    WELCOME,
    MODEL_IMPORT,
    CALIBRATION
}

/**
 * Pure step-sequencing logic for the onboarding flow — Android-free and unit-testable; the view and the
 * persistence layer are thin wrappers around it.
 */
object Onboarding {
    
    /** The steps in display order. */
    val STEPS: List<OnboardingStep> = listOf(OnboardingStep.WELCOME, OnboardingStep.MODEL_IMPORT, OnboardingStep.CALIBRATION)
    
    /** @return the first step to show. */
    fun first(): OnboardingStep = STEPS.first()
    
    /**
     * @param step the current step
     * @return true when [step] is the final step (its primary button finishes the flow)
     */
    fun isLast(step: OnboardingStep): Boolean = step == STEPS.last()
    
    /**
     * @param step the current step
     * @return the next step, or null when the flow is finished
     */
    fun next(step: OnboardingStep): OnboardingStep? {
        val index = STEPS.indexOf(step)
        return if (index < 0 || index >= STEPS.lastIndex) null else STEPS[index + 1]
    }
    
    /**
     * @param step the current step
     * @return the 1-based position of [step] within the flow (for a "2 / 3" indicator)
     */
    fun position(step: OnboardingStep): Int = STEPS.indexOf(step) + 1
    
    /** @return the total number of steps. */
    fun count(): Int = STEPS.size
}

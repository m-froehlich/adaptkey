// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.onboarding

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Persists whether the first-run onboarding has been completed, in the default shared preferences (so
 * the settings screen can also reset it to re-show the introduction).
 */
object OnboardingStore {
    
    /** Default-prefs flag: the onboarding flow has been finished or skipped. */
    const val KEY_COMPLETED = "onboarding_completed"
    
    /**
     * @param context any valid context
     * @return true once the onboarding has been finished or skipped
     */
    fun isCompleted(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_COMPLETED, false)
    }
    
    /**
     * Records the onboarding as completed (or resets it to show again).
     *
     * @param context any valid context
     * @param completed whether the onboarding is now completed
     */
    fun setCompleted(context: Context, completed: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_COMPLETED, completed).apply()
    }
}

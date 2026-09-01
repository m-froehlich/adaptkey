// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-389: how long a learned word may go untouched before [LearnedWordExpirySweep] un-learns it - the
 * user's own suggested control, a coarse three-way choice ("früh"/"mittel"/"spät") rather than a raw
 * duration slider. "Untouched" means [DictionaryStore.learnedFrequencyOf]'s own `lastTouched` stamp, which
 * every [DictionaryStore.learn]/reinforcement write already refreshes (D-388/D-411) - a frequently-used
 * word's own window keeps extending itself for free, with no separate "usage count" tracking needed.
 *
 * @property days the retention window, in whole days (a coarse 30-day month approximation, matching this
 *           codebase's existing day-based expiry windows, e.g. [de.froehlichmedia.adaptkey.settings.
 *           AdaptSettings.pendingBlacklistExpiryDays] - calendar-accurate month arithmetic would be
 *           needless precision for a setting this coarse to begin with)
 */
enum class LearnedWordExpiryWindow(val days: Int) {
    
    /** ~3 months. */
    EARLY(90),
    
    /** ~6 months. */
    MEDIUM(180),
    
    /** ~12 months. */
    LATE(365);
    
    companion object {
        
        /** The spec default (D-389). */
        val DEFAULT = MEDIUM
        
        /**
         * Resolves a stored preference value to a window, tolerating case and unknown/blank input. This is
         * the validation/clamp point for the persisted setting: anything unrecognised falls back to
         * [DEFAULT] so a corrupt stored value can never leave the setting in an invalid state.
         *
         * @param key the stored value (e.g. "early" / "medium" / "late"), or null when unset
         * @return the matching window, or [DEFAULT] when [key] is null, blank or unrecognised
         */
        fun fromKey(key: String?): LearnedWordExpiryWindow {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

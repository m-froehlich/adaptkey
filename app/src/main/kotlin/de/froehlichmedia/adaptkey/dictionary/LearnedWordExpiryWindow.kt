// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-389: how long a learned word may go untouched before [LearnedWordExpirySweep] un-learns it.
 * "Untouched" means [DictionaryStore.learnedFrequencyOf]'s own `lastTouched` stamp, which every
 * [DictionaryStore.learn]/reinforcement write already refreshes (D-388/D-411) - a frequently-used word's
 * own window keeps extending itself for free, with no separate "usage count" tracking needed.
 *
 * D-389-followup (v3): shown to the user as concrete durations ("1 Monat"/"4 Monate"/"1 Jahr"/"Nie"), not
 * the original abstract "früh"/"mittel"/"spät" wording - explicit user request, so the settings row states
 * plainly how long an entry actually survives rather than leaving that to be inferred. [NEVER] is listed
 * last, after the three finite windows, matching that same concrete-durations-first ordering.
 *
 * @property days the retention window, in whole days (a coarse 30-day month approximation, matching this
 *           codebase's existing day-based expiry windows, e.g. [de.froehlichmedia.adaptkey.settings.
 *           AdaptSettings.pendingBlacklistExpiryDays] - calendar-accurate month arithmetic would be
 *           needless precision for a setting this coarse to begin with), or null for [NEVER] - expiry is a
 *           genuine opt-in, not merely a very long window, so there is no finite value that would mean the
 *           same thing; [LearnedWordExpirySweep] skips the sweep entirely rather than computing a cutoff
 *           against a sentinel.
 */
enum class LearnedWordExpiryWindow(val days: Int?) {
    
    /** 1 month. */
    ONE_MONTH(30),
    
    /** 4 months. */
    FOUR_MONTHS(120),
    
    /** 1 year. */
    ONE_YEAR(365),
    
    /** D-389-followup: learned words are never automatically un-learned - the spec default. */
    NEVER(null);
    
    companion object {
        
        /** D-389-followup: an automatic, silent deletion of the user's own accumulated vocabulary must be
         * opt-in, not opt-out. */
        val DEFAULT = NEVER
        
        /**
         * Resolves a stored preference value to a window, tolerating case and unknown/blank input. This is
         * the validation/clamp point for the persisted setting: anything unrecognised falls back to
         * [DEFAULT] so a corrupt stored value can never leave the setting in an invalid state.
         *
         * @param key the stored value (e.g. "one_month" / "four_months" / "one_year" / "never"), or null
         *        when unset
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

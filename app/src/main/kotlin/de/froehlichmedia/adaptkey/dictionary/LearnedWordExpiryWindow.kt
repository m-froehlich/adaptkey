// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-389: how long a learned word may go untouched before [LearnedWordExpirySweep] un-learns it.
 * "Untouched" means [DictionaryStore.learnedFrequencyOf]'s own `lastTouched` stamp, which every
 * [DictionaryStore.learn]/reinforcement write already refreshes (D-388/D-411) - a frequently-used word's
 * own window keeps extending itself for free, with no separate "usage count" tracking needed.
 *
 * D-389-followup (v5): the persisted preference value is this enum's own name (`early`/`medium`/`late`/
 * `never`), deliberately kept abstract - explicit user request. The *displayed* labels (each locale's own
 * `strings.xml`, the `d389_window_*` entries) show the concrete duration each level currently
 * means ("1 Monat"/"4 Monate"/"1 Jahr"), but that mapping is not baked into what is actually stored: a
 * device that already has `medium` saved automatically picks up whatever [days] `MEDIUM` is redefined to
 * mean in a later app update, with no migration needed. **Whoever changes a [days] value below must also
 * update that level's own `d389_window_*` label strings (all three languages) in the same change** - the
 * two are independent and nothing enforces they stay in sync automatically.
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
    
    /** Currently 1 month (`d389_window_early`) - see the class KDoc on keeping the two in sync. */
    EARLY(30),
    
    /** Currently 4 months (`d389_window_medium`) - see the class KDoc on keeping the two in sync. */
    MEDIUM(120),
    
    /** Currently 1 year (`d389_window_late`) - see the class KDoc on keeping the two in sync. */
    LATE(365),
    
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
         * @param key the stored value (e.g. "early" / "medium" / "late" / "never"), or null when unset
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

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-391: how eagerly [TokenRepair.tryFuseAcrossSpace]'s cross-word fusion (the reverse of A-05's own
 * missed-space split - two already-committed words become one, e.g. `"Ar eitstag"` -> `"Arbeitstag"`) may
 * silently apply, gated by [MergeConfidence]'s `[0, 1]` score - mirrors [AutocorrectAggressiveness]'s own
 * shape (a considered starting point per level, not yet device-tuned) but is deliberately its **own**,
 * separate setting rather than folded into C-22: this mechanism reaches backward into already-committed,
 * finished text - a materially different risk than an ordinary same-token autocorrect - and, unlike A-05's
 * own C-21 setting, has no realistic "offer as a chip" middle ground (the evidence a fusion needs only
 * exists once the *second* word has already committed, by which point a chip would already be too late) -
 * so there are only ever two outcomes, silently applied or nothing at all, never a suggestion.
 *
 * Deliberately defaults to **off** (see [OFF_KEY]/`AdaptSettings.autoMergeEnabled`) - unlike
 * [AutocorrectAggressiveness]'s own fail-open default, this is a brand-new, undevice-tested mechanism that
 * rewrites already-committed text, so a fresh install stays inert until the user opts in.
 */
enum class AutoMergeAggressiveness(val autoApplyThreshold: Double) {
    
    CAUTIOUS(autoApplyThreshold = 0.90),
    MEDIUM(autoApplyThreshold = 0.75),
    AGGRESSIVE(autoApplyThreshold = 0.55);
    
    companion object {
        
        /** The spec default level *when enabled* - see the class KDoc for why "off" is the actual default. */
        val DEFAULT = MEDIUM
        
        /**
         * The stored "Off" position - not a member of this enum (mirrors
         * [AutocorrectAggressiveness.OFF_KEY]'s identical shape), since it disables the mechanism entirely
         * rather than naming a confidence level.
         */
        const val OFF_KEY = "off"
        
        /**
         * Resolves a stored preference value to a level, tolerating case and unknown/blank input.
         *
         * @param key the stored value (e.g. "cautious" / "medium" / "aggressive"), or null when unset
         * @return the matching level, or [DEFAULT] when [key] is null, blank or unrecognised (including
         *         [OFF_KEY] - callers gate on `AdaptSettings.autoMergeEnabled` separately for that)
         */
        fun fromKey(key: String?): AutoMergeAggressiveness {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

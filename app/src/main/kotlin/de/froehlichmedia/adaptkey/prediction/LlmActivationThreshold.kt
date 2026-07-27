// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * The C-06 mini-LLM activation threshold: the tier-1 n-gram confidence at or below which tier 3 is
 * consulted (§9).
 *
 * The threshold is the confidence value the n-gram must *exceed* to keep tier 3 dormant, so a higher
 * setting makes the LLM more eager (it activates even for fairly confident tier-1 predictions) and a
 * lower setting more reluctant (only a near-blank tier-1 wakes it). The spec default is [MEDIUM].
 *
 * D-297: [DISABLED] folds in the former, separate "tier-3 enabled" toggle (D-126) - that toggle was
 * confirmed dead code before removal (`AdaptSettings.tier3Enabled` was persisted but never actually read
 * anywhere in the activation path). [DISABLED]'s own [confidenceThreshold] is deliberately outside the
 * valid `[0, 1]` confidence range, so [Tier3Activation.shouldActivate]'s existing `<` comparison already
 * never fires for it - no separate "is this disabled" branch needed there at all.
 *
 * @property confidenceThreshold the tier-1 confidence in [0, 1] below which tier 3 activates
 */
enum class LlmActivationThreshold(val confidenceThreshold: Double) {
    
    /** D-297: never activates - see this enum's own KDoc for why no special-casing is needed elsewhere. */
    DISABLED(-1.0),
    LOW(0.25),
    MEDIUM(0.50),
    HIGH(0.75);
    
    companion object {
        
        /** The spec default (C-06, "medium"). */
        val DEFAULT = MEDIUM
        
        /**
         * Resolves a stored preference value to a threshold, tolerating case and unknown/blank input.
         * This is the validation/clamp point for the persisted C-06 value: anything unrecognised falls
         * back to [DEFAULT] so a corrupt stored value can never leave the setting in an invalid state.
         *
         * @param key the stored value (e.g. "low" / "medium" / "high"), or null when unset
         * @return the matching threshold, or [DEFAULT] when [key] is null, blank or unrecognised
         */
        fun fromKey(key: String?): LlmActivationThreshold {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

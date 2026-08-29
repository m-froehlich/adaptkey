// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-352: how eagerly the A-05 retroactive word split may act. The user's own reported concern: A-05
 * "funktioniert zu oft falsch und zu oft nicht" (fires wrongly too often, and misses too often) - this
 * setting lets a split candidate still be found and offered without ever being silently applied, or turns
 * the mechanism off outright, rather than forcing an all-or-nothing choice.
 *
 * Deliberately scoped to A-05 (the missed-space/space-ambiguous split) alone - A-06 (merge onto a spurious
 * space) and D-122 (the mid-word connector-split suggestion, triggered by the user deliberately re-editing
 * an existing word, not by ordinary forward typing) are both untouched by this setting.
 */
enum class AutoSplitMode {
    
    /** The spec default: a high-confidence split still applies automatically at commit (unchanged A-05). */
    AUTOMATIC,
    
    /** A split candidate is only ever offered as a suggestion-bar chip - never silently applied. */
    CHIP_ONLY,
    
    /** A-05 is fully disabled - no automatic split, and no chip either. */
    OFF;
    
    companion object {
        
        /** The spec default (D-352). */
        val DEFAULT = AUTOMATIC
        
        /**
         * Resolves a stored preference value to a mode, tolerating case and unknown/blank input. This is
         * the validation/clamp point for the persisted setting: anything unrecognised falls back to
         * [DEFAULT] so a corrupt stored value can never leave the setting in an invalid state.
         *
         * @param key the stored value (e.g. "automatic" / "chip_only" / "off"), or null when unset
         * @return the matching mode, or [DEFAULT] when [key] is null, blank or unrecognised
         */
        fun fromKey(key: String?): AutoSplitMode {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

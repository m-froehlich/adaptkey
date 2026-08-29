// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-353: how eagerly {@link DictionarySuggestionProvider}'s dictionary/A-01-override autocorrect may act -
 * two thresholds per level, both compared against {@link CorrectionConfidence}'s `[0, 1]` score.
 *
 * **Calibration.** Every threshold below is chosen against the app's own existing regression corpus (the
 * real, previously-confirmed good and bad cases already covered by `DictionarySuggestionProviderTest`), not
 * guessed:
 *
 * - `komplett`/40 at cost 2 ("komplezz" -> "komplett", D-28): score 0.85 - must clear [MEDIUM]'s auto
 *   threshold (it always has).
 * - `Virgin`/62, noun-tagged (D-114): score ~0.54 - must stay below [MEDIUM]'s auto threshold.
 * - `Jahren`/2,000, noun-tagged (D-116's compound rest): score 1.0 - must clear [MEDIUM] easily; a rare
 *   noun and a common, correctly-recognised one must not be treated alike (see
 *   [CorrectionConfidence]'s own KDoc on [CorrectionConfidence].NOUN_REFERENCE_FREQUENCY).
 * - `ddr`/`der`, ratio 228x (D-244): score ~0.87 - must clear [MEDIUM].
 * - `due`/`die`, ratio 37,000x+ (§44): score 1.0 (clamped) - must clear every level.
 * - `Ohren`/`Ihren`, ratio 70x (D-244's own regression): score ~0.68 - must **never** clear any level's
 *   auto threshold, [AGGRESSIVE] included. This is the floor the user explicitly requires: a level may
 *   only ever admit more of the *untested* grey zone above this floor, never re-open a confirmed false
 *   positive purely because a more permissive level was chosen. [AGGRESSIVE]'s auto threshold (0.70) sits
 *   just above it, not freely chosen - see the design discussion this enum's introduction (D-353) grew out
 *   of for the reasoning.
 *
 * [MEDIUM]'s auto threshold (0.75) is therefore boxed in on both sides by the existing corpus (must exceed
 * ~0.68/Ohren and ~0.54/Virgin, must not exceed ~0.85/komplett or ~0.87/ddr-der) - it reproduces today's
 * exact behaviour, not a freely chosen number. [CAUTIOUS] and [AGGRESSIVE] are new; only the Ohren floor
 * constrains them, everything else is a considered starting point, not yet device-tuned.
 *
 * A single regression test (`AutocorrectAggressivenessTest`) asserts the Ohren/Ihren-shaped floor directly
 * against [AGGRESSIVE] (the most permissive level) so a future retuning of [CorrectionConfidence]'s own
 * constants cannot silently reopen it again without that test going red first.
 *
 * @property autoApplyThreshold confidence at or above which a candidate is confident enough to silently
 *           replace the typed token at commit
 * @property chipOfferThreshold confidence at or above which a candidate is still offered as an ordinary
 *           suggestion-bar entry ([DictionarySuggestionProvider]'s D-12 fuzzy match), even when it falls
 *           short of [autoApplyThreshold] - deliberately low, since an unwanted suggestion is merely
 *           ignorable, unlike an unwanted silent replacement
 */
enum class AutocorrectAggressiveness(val autoApplyThreshold: Double, val chipOfferThreshold: Double) {
    
    CAUTIOUS(autoApplyThreshold = 0.90, chipOfferThreshold = 0.40),
    MEDIUM(autoApplyThreshold = 0.75, chipOfferThreshold = 0.30),
    AGGRESSIVE(autoApplyThreshold = 0.70, chipOfferThreshold = 0.20);
    
    companion object {
        
        /** The spec default - reproduces the pre-D-353 behaviour exactly against the existing corpus. */
        val DEFAULT = MEDIUM
        
        /**
         * Resolves a stored preference value to a level, tolerating case and unknown/blank input.
         *
         * @param key the stored value (e.g. "cautious" / "medium" / "aggressive"), or null when unset
         * @return the matching level, or [DEFAULT] when [key] is null, blank or unrecognised
         */
        fun fromKey(key: String?): AutocorrectAggressiveness {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

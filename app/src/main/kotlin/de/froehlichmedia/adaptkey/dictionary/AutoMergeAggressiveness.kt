// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-391 / D-477: how eagerly [TokenRepair.tryFuseAcrossSpace]'s cross-word fusion (the reverse of A-05's own
 * missed-space split - two already-committed words become one, e.g. `"Na hbarn"` -> `"Nachbarn"`) may
 * silently apply. Its own setting, deliberately separate from C-21/C-22: this mechanism reaches backward into
 * already-committed, finished text - a materially different risk than an ordinary same-token autocorrect -
 * and has no realistic "offer as a chip" middle ground (the evidence only exists once the second word has
 * already committed) - so there are only ever two outcomes, silently applied or nothing at all.
 * 
 * D-477: the levels trade *precision for recall* along the one signal that actually separates a swallowed
 * connector letter from a coincidence - how many of the two tokens are recognised words ([FusionClass]) -
 * and use the fused word's frequency only as a per-level floor (a junk-row guard), not as a score:
 * 
 * - [CAUTIOUS]: both tokens must be unrecognised. No false merge in any measured set, at the price of missing
 *   every case where one fragment happens to be a real word (`"Na hbarn"` has the real word `"Na"`;
 *   `"au h"` has `"au"`) - about 18 % of the typed-word weight of realistic spurious-space errors is
 *   recovered.
 * - [MEDIUM]: at least one token unrecognised - about 64 % of that weight recovered, at roughly one
 *   coincidental fusion per 100,000 candidate pairs.
 * - [AGGRESSIVE]: additionally two recognised words, but only under [rejection]'s extra safeguard (the
 *   fused word at least as frequent as the rarer part, and the pair not already an attested bigram) -
 *   without it 0.1-0.2 % of real phrases (`der er`, `sei er`) would fuse. About 86 % recovered; a few
 *   more coincidental fusions are accepted. None of the levels fused any of 38,388 real attested phrases.
 * 
 * All figures are measured by `FusionEvaluationTest` against the real German dictionary (recall weighted by
 * how often each word is typed).
 * 
 * A fragment that is a single letter coincides with some word about a hundred times as often as a longer
 * one, so each level carries a higher [minFrequencyOneLetter] floor for it (`"au h"` -> `"auch"` clears it
 * easily; `"der e"` -> `"derbe"` does not).
 * 
 * Deliberately defaults to **off** (see [OFF_KEY]/`AdaptSettings.autoMergeEnabled`): it rewrites
 * already-committed text, so a fresh install stays inert until the user opts in.
 * 
 * @property allowedClasses which [FusionClass]es this level may fuse at all
 * @property minFrequency the fused word's frequency floor
 * @property minFrequencyOneLetter the (higher) floor when either original token is a single letter
 */
enum class AutoMergeAggressiveness(
    val allowedClasses: Set<FusionClass>,
    val minFrequency: Long,
    val minFrequencyOneLetter: Long
) {
    
    CAUTIOUS(setOf(FusionClass.BOTH_UNKNOWN), minFrequency = 30L, minFrequencyOneLetter = 1_000L),
    MEDIUM(setOf(FusionClass.BOTH_UNKNOWN, FusionClass.ONE_UNKNOWN), minFrequency = 30L, minFrequencyOneLetter = 1_000L),
    AGGRESSIVE(FusionClass.entries.toSet(), minFrequency = 10L, minFrequencyOneLetter = 300L);
    
    /**
     * @param candidate the evidence from [TokenRepair.tryFuseAcrossSpace]
     * @return null when this level accepts [candidate], otherwise a short human-readable reason (also what
     *         the in-app diagnostic log prints)
     */
    fun rejection(candidate: FusionCandidate): String? {
        if (candidate.fragments !in allowedClasses) {
            return "fragments=${candidate.fragments} not allowed at $name"
        }
        val floor = if (candidate.oneLetterFragment) minFrequencyOneLetter else minFrequency
        if (candidate.frequency < floor) {
            return "frequency ${candidate.frequency} below floor $floor at $name" +
                if (candidate.oneLetterFragment) " (one-letter fragment)" else ""
        }
        if (candidate.fragments == FusionClass.BOTH_KNOWN) {
            val rarerPart = minOf(candidate.leftFrequency, candidate.rightFrequency)
            if (rarerPart <= 0L) {
                return "both tokens known but a part has no dictionary frequency to compare against"
            }
            if (candidate.frequency < rarerPart) {
                return "both tokens known and the fused word (${candidate.frequency}) is rarer than the rarer part ($rarerPart)"
            }
            if (candidate.pairAttested) {
                return "both tokens known and already an attested word pair"
            }
        }
        return null
    }
    
    /** @return whether this level silently applies [candidate] */
    fun accepts(candidate: FusionCandidate): Boolean = rejection(candidate) == null
    
    companion object {
        
        /** The spec default level *when enabled* - see the class KDoc for why "off" is the actual default. */
        val DEFAULT = MEDIUM
        
        /**
         * The stored "Off" position - not a member of this enum (mirrors
         * [AutocorrectAggressiveness.OFF_KEY]'s identical shape), since it disables the mechanism entirely
         * rather than naming a level.
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

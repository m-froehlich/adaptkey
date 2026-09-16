// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import kotlin.math.ln

/**
 * D-353: a single `[0, 1]` confidence score for a correction candidate, replacing the pile of independent
 * gates {@link DictionarySuggestionProvider#bestCorrection} previously used (D-114/D-227's frequency floor
 * plus noun exemption, D-244's flat 100x ratio bar) with one calibrated, graduated measure that
 * {@link AutocorrectAggressiveness}'s two thresholds are compared against.
 * 
 * Two genuinely different scenarios get their own formula - they measure different things and do not
 * belong on one shared curve:
 * 
 * - {@link #forUnknownToken}: the typed token has no dictionary entry of its own - confidence rests on how
 *   close the edit is and how plausible the *candidate* is on its own merits (its frequency, weighed
 *   against a much higher bar when it is noun-tagged - D-227's own finding that a rare noun in this
 *   Wikipedia-derived corpus is disproportionately a proper-noun artefact, not an ordinary word, while a
 *   genuinely common noun like "Jahren" is not penalised once its own frequency clears that higher bar).
 * - {@link #forKnownWordOverride}: the typed token is itself a real dictionary word (A-01) - confidence
 *   instead rests on how much more frequent the candidate is *relative to the typed word itself* (D-244's
 *   own ratio idea), since a bare candidate frequency says nothing about whether overriding a valid word is
 *   ever justified.
 * 
 * D-354: {@link #prefixShiftsAway} is folded into both as a **cap**, not a multiplier - deliberately, so a
 * prefix-changing correction can still surface as an ordinary suggestion-bar candidate (confidence still
 * has to clear {@link AutocorrectAggressiveness#chipOfferThreshold} normally) while never being confident
 * enough to silently apply, at any {@link AutocorrectAggressiveness} level. See
 * [PREFIX_CONFIDENCE_CAP]'s own KDoc for why a cap, not a factor.
 * 
 * D-371: a typed token ending in a digit (e.g. a house/model number glued onto a word) gets the same
 * treatment in {@link #forUnknownToken}, via its own [DIGIT_SUFFIX_CONFIDENCE_CAP] - but this cap sits
 * strictly between [AutocorrectAggressiveness.MEDIUM]'s and [AutocorrectAggressiveness.AGGRESSIVE]'s own
 * auto-apply thresholds rather than below every level's like [PREFIX_CONFIDENCE_CAP], so an otherwise
 * high-confidence candidate can still auto-apply, but only at the most permissive level - see that
 * constant's own KDoc for the exact reasoning and numbers.
 * 
 * See {@link AutocorrectAggressiveness}'s own KDoc for the full worked calibration of every constant here
 * against the real regression corpus (`due`/`die`, `ddr`/`der`, `Ohren`/`Ihren`, `übrigens`, `Virgin`,
 * `komplezz`/`komplett`, `aberkennen`/`anerkennen`, ...).
 */
object CorrectionConfidence {
    
    /**
     * Confidence for correcting an unknown (not itself a dictionary word) typed token to [candidate].
     * 
     * @param cost the candidate's edit cost from the typed token (D-28)
     * @param candidateFrequency the candidate's own dictionary frequency
     * @param candidateIsNounLike whether the candidate is tagged [PartOfSpeech.NOUN] / [PartOfSpeech.PROPER_NOUN]
     * @param prefixShiftsAway [prefixShiftsAway]'s own result for this typed/candidate pair (D-354)
     * @param typedEndsInDigit D-371: whether the typed token itself ends in a digit (e.g. `"Str12"`) - see
     *        [DIGIT_SUFFIX_CONFIDENCE_CAP]'s own KDoc
     * @return the confidence in `[0, 1]`
     */
    fun forUnknownToken(
        cost: Int,
        candidateFrequency: Long,
        candidateIsNounLike: Boolean,
        prefixShiftsAway: Boolean,
        typedEndsInDigit: Boolean
    ): Double {
        val raw = costFactor(cost) * frequencyFactor(candidateFrequency, candidateIsNounLike)
        return capped(raw, prefixShiftsAway, typedEndsInDigit)
    }
    
    /**
     * Confidence for overriding A-01's protection of a typed token that is itself a known dictionary word.
     * 
     * D-113: never fires beyond a single adjacent-key edit ([cost] > 1 returns 0.0 outright) - a genuine
     * frequency-ratio gap between two entirely unrelated, real words (`spreche`/`Sprache`) is not itself
     * evidence that the rarer one was a typo, unlike a genuine cost-1 slip (`due`/`die`).
     * 
     * @param cost the candidate's edit cost from the typed token
     * @param typedFrequency the typed word's own dictionary frequency
     * @param candidateFrequency the candidate's own dictionary frequency
     * @param prefixShiftsAway [prefixShiftsAway]'s own result for this typed/candidate pair (D-354)
     * @return the confidence in `[0, 1]`
     */
    fun forKnownWordOverride(cost: Int, typedFrequency: Long, candidateFrequency: Long, prefixShiftsAway: Boolean): Double {
        if (cost > ADJACENT_COST) {
            return 0.0
        }
        val ratio = candidateFrequency.toDouble() / typedFrequency.coerceAtLeast(1L).toDouble()
        return capped(ratioFactor(ratio), prefixShiftsAway)
    }
    
    /**
     * D-473: confidence for an A-05 split of a token into [left]/[right], given each half's own resolved
     * dictionary entry - previously missing entirely: unlike every other correction mechanism in this file,
     * {@link de.froehlichmedia.adaptkey.dictionary.TokenRepair#trySplit}'s own gates (per-half frequency
     * floor, not-both-nouns, no protected split-prefix, ...) had no aggregate confidence of their own at
     * all, so once nothing vetoed it, any structurally-valid two-word pairing won unconditionally, however
     * coincidental - e.g. `"Schwimmtasche"` -> `"schwimmt"` + `"Asche"`, neither half individually rare, no
     * competing correction to prefer instead. Reuses {@link #frequencyFactor} - the same noun-aware curve
     * {@link #forUnknownToken} already applies to a single correction candidate - multiplicatively over both
     * halves, so a split is only as confident as its weakest half (D-227's "a rare noun is
     * disproportionately a Wikipedia extraction artefact" reasoning applies equally to either side of a
     * split): the historical `"der"` + `"Kinderarzt"` case (D-203) now scores ~0.35, since `"Kinderarzt"`
     * (frequency 14) is exactly the kind of rare noun {@link #NOUN_REFERENCE_FREQUENCY} already treats with
     * suspicion elsewhere - it still clears every {@link AutocorrectAggressiveness#chipOfferThreshold}, so
     * the split still surfaces, just no longer applies silently, consistent with the same "rare noun ->
     * chip, not silent" precedent {@link #NOUN_REFERENCE_FREQUENCY} was calibrated for in the first place.
     * 
     * D-473: this alone does not close every reported false-positive split - a coincidentally frequent
     * short fragment (e.g. `"trotz"` + `"de"`, both individually unremarkable) can still score high on pure
     * frequency while being the wrong reading when a much better single-word correction exists. That case is
     * closed separately, at the call site: [AdaptKeyService.finalizeAndCommit]'s own split-veto condition
     * now blocks a split whenever *any* confidence-cleared whole-word correction was found at all, not only
     * one within a single adjacent-key edit - this function is the backstop for when no such correction
     * exists to veto against.
     * 
     * @param leftFrequency the left half's own resolved dictionary frequency
     * @param leftIsNounLike whether the left half is tagged [PartOfSpeech.NOUN] / [PartOfSpeech.PROPER_NOUN]
     * @param rightFrequency the right half's own resolved dictionary frequency
     * @param rightIsNounLike whether the right half is tagged [PartOfSpeech.NOUN] / [PartOfSpeech.PROPER_NOUN]
     * @return the confidence in `[0, 1]`
     */
    fun forSplit(leftFrequency: Long, leftIsNounLike: Boolean, rightFrequency: Long, rightIsNounLike: Boolean): Double {
        return frequencyFactor(leftFrequency, leftIsNounLike) * frequencyFactor(rightFrequency, rightIsNounLike)
    }
    
    /**
     * D-473: confidence for a D-39 raw-coordinate-correction candidate (T-02) - previously missing entirely
     * for a typed token that is not itself a known dictionary word: [de.froehlichmedia.adaptkey.
     * AdaptKeyService.rawCoordinateCorrection] applied whatever [de.froehlichmedia.adaptkey.suggestion.
     * RawCoordinateCorrection.respellings] found unconditionally in that case, bypassing [prefixShiftsAway]'s
     * protection entirely - confirmed root cause of a real report (`"anspringen"`, absent from the
     * dictionary, silently corrected to `"abspringen"` - a genuine prefix-changing substitution the ordinary
     * edit-distance search already correctly declines via this exact mechanism). Deliberately simpler than
     * [forUnknownToken]: the candidate is already dictionary-verified by construction (unlike a free-form
     * edit-distance guess), so no cost/frequency factor is needed here - only the same prefix-shift caution
     * every other mechanism in this file already applies.
     * 
     * D-473: **not** consulted at all when the touch model's own top pick for the exact tap in question
     * already disagreed with what was actually resolved (a non-negative gap, see [de.froehlichmedia.
     * adaptkey.suggestion.RawCoordinateCorrection.Respelling]) - that is direct evidence about which key was
     * actually pressed, not a guess from spelling closeness, and is treated as strong enough to look past
     * this caution (and A-01's known-word ratio requirement alike) the same way a pure edit-distance guess
     * never could. See [de.froehlichmedia.adaptkey.AdaptKeyService.rawCoordinateCorrection] for exactly where
     * that short-circuit happens - kept there, not folded into this function, since it is a binary
     * "touch evidence is strong enough to skip this whole check" decision, not itself a graduated confidence.
     * 
     * @param prefixShiftsAway [prefixShiftsAway]'s own result for this typed/candidate pair (D-354)
     * @return the confidence in `[0, 1]`
     */
    fun forRawCoordinateCorrection(prefixShiftsAway: Boolean): Double {
        return if (prefixShiftsAway) PREFIX_CONFIDENCE_CAP else 1.0
    }
    
    /**
     * D-354: whether correcting [typed] to [candidate] changes a plausible German verb/word prefix at the
     * very start of the typed token - e.g. "aberkennen" ("ab-" + "erkennen") corrected to "anerkennen"
     * ("an-" + "erkennen"): the typed token, though not itself in the dictionary, may still be a genuine,
     * simply unlisted, prefixed word - a correction must never *silently* discard that possibility.
     * 
     * Deliberately broader than {@link TokenRepair#INSEPARABLE_PREFIXES} - that set exists to gate an A-05
     * *split*, where the Wechselpräfixe (über-/um-/durch-/unter-/voll-/hinter-/wieder-) are deliberately
     * excluded because each is also a common standalone word, so blocking them there would reject far more
     * genuine two-word splits than it would prevent. That reasoning does not apply here at all - this is
     * not a split gate, only a soft plausibility signal that caps how confident an outright substitution
     * may be, so the broader set is used deliberately, not merely reused without thought.
     * 
     * @param typed the lower-cased typed token
     * @param candidate the lower-cased correction candidate
     * @return true when the longest matching prefix of [typed] is not also a prefix of [candidate]
     */
    fun prefixShiftsAway(typed: String, candidate: String): Boolean {
        val prefix = PLAUSIBLE_GERMAN_PREFIXES.filter { typed.startsWith(it) }.maxByOrNull { it.length } ?: return false
        return !candidate.startsWith(prefix)
    }
    
    private fun costFactor(cost: Int): Double {
        return if (cost <= ADJACENT_COST) 1.0 else CORRECTION_COST_PENALTY_FACTOR
    }
    
    private fun frequencyFactor(frequency: Long, isNounLike: Boolean): Double {
        val reference = if (isNounLike) NOUN_REFERENCE_FREQUENCY else REFERENCE_FREQUENCY
        return (ln(frequency.coerceAtLeast(1).toDouble()) / ln(reference)).coerceIn(0.0, 1.0)
    }
    
    private fun ratioFactor(ratio: Double): Double {
        if (ratio <= 1.0) {
            return 0.0
        }
        return (ln(ratio) / ln(REQUIRED_OVERRIDE_RATIO)).coerceIn(0.0, 1.0)
    }
    
    private fun capped(raw: Double, prefixShiftsAway: Boolean, typedEndsInDigit: Boolean = false): Double {
        // D-371: the two caps are independent risk signals and combine via the lower of the two - a
        // candidate can be capped by both a prefix shift and a digit-ending typo at once, and either one
        // alone is already enough reason to withhold silent auto-apply below its own cap's own level.
        var result = raw
        if (prefixShiftsAway) {
            result = minOf(result, PREFIX_CONFIDENCE_CAP)
        }
        if (typedEndsInDigit) {
            result = minOf(result, DIGIT_SUFFIX_CONFIDENCE_CAP)
        }
        return result
    }
    
    private const val ADJACENT_COST = 1
    
    /** D-353: the discount applied to a candidate beyond a single adjacent-key edit - see this file's own class KDoc. */
    private const val CORRECTION_COST_PENALTY_FACTOR = 0.85
    
    /**
     * D-353: the non-noun candidate frequency at which [frequencyFactor] saturates to 1.0 - deliberately
     * low (comfortably below every existing cost-1/cost-2 regression case's own frequency, e.g. "komplett"
     * at 40, "dass" at 50) so an ordinary, unremarkable word is never held back by frequency alone; a
     * non-noun candidate this rare is already rare enough that no further discount is useful (D-227: it is
     * a noun-tagged rarity, not an ordinary rare word, that is the real risk signal).
     */
    private const val REFERENCE_FREQUENCY = 25.0
    
    /**
     * D-353/D-227: the noun-tagged candidate frequency at which [frequencyFactor] saturates to 1.0 -
     * dramatically higher than [REFERENCE_FREQUENCY], so a rare noun ("Virgin", frequency 62, a Wikipedia
     * proper-noun artefact) stays well short of any [AutocorrectAggressiveness] auto threshold while a
     * genuinely common, correctly-recognised noun ("Jahren", frequency 2,000) is not penalised once its own
     * frequency clears this bar - a flat "nouns always cost 0.5x" multiplier would have wrongly punished the
     * latter just as much as the former; see [AutocorrectAggressiveness]'s own KDoc for the worked numbers.
     */
    private const val NOUN_REFERENCE_FREQUENCY = 2000.0
    
    /**
     * D-353: replaces D-244's flat 100x bar with a log-scaled curve reaching 1.0 at this ratio - calibrated
     * so the confirmed-bad "Ohren"/"Ihren" case (70x) sits below every [AutocorrectAggressiveness] auto
     * threshold while the confirmed-good "ddr"/"der" (228x) and "due"/"die" (37,000x+) cases sit above it.
     * See [AutocorrectAggressiveness]'s own KDoc for the worked numbers.
     */
    private const val REQUIRED_OVERRIDE_RATIO = 500.0
    
    /**
     * D-354: the ceiling [prefixShiftsAway] clamps a score to - deliberately a cap, not a multiplier, and
     * deliberately chosen strictly between every [AutocorrectAggressiveness] level's chip and auto
     * thresholds, so a prefix-changing correction always still clears every chip-offer floor (never
     * silently hidden) while never clearing any auto-apply threshold (never silently applied) - see that
     * enum's own KDoc for the exact numbers this sits between.
     */
    private const val PREFIX_CONFIDENCE_CAP = 0.55
    
    /**
     * D-354: common German verb/word prefixes - see [prefixShiftsAway]'s own KDoc for why this is
     * deliberately broader than [TokenRepair.INSEPARABLE_PREFIXES].
     */
    private val PLAUSIBLE_GERMAN_PREFIXES = setOf(
        "ab", "an", "auf", "aus", "be", "bei", "durch", "ein", "emp", "ent", "er", "ge", "hinter", "los",
        "mit", "nach", "ob", "um", "unter", "ver", "vor", "weg", "wider", "zer", "zu", "über", "voll",
        "wieder", "miss", "ur"
    )
    
    /**
     * D-371: the ceiling [capped] clamps a digit-ending token's score to. Unlike [PREFIX_CONFIDENCE_CAP],
     * this one sits *above* [AutocorrectAggressiveness.AGGRESSIVE]'s own auto threshold (0.70) but *below*
     * [AutocorrectAggressiveness.MEDIUM]'s (0.75) - so a digit-ending token (e.g. `"Str12"`, a house/model
     * number glued onto a word) can still be silently auto-corrected, but only at the most permissive
     * [AutocorrectAggressiveness] level; [CAUTIOUS]/[MEDIUM] are left with only a chip offer (every chip
     * threshold sits well below 0.70, so the chip is never suppressed by this cap either). This is a
     * deliberately different placement than [PREFIX_CONFIDENCE_CAP] (which blocks auto-apply outright, at
     * every level) - here the risk is judged tolerable, just only for a user who has explicitly opted into
     * the most aggressive setting.
     */
    private const val DIGIT_SUFFIX_CONFIDENCE_CAP = 0.72
}

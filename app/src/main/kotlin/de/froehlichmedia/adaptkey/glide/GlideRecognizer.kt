// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.LearnedBigramBoost
import de.froehlichmedia.adaptkey.dictionary.LearnedNgram
import de.froehlichmedia.adaptkey.suggestion.DiacriticFolding
import de.froehlichmedia.adaptkey.suggestion.NoOpDiacriticFolding
import kotlin.math.abs
import kotlin.math.ln

/**
 * The glide-typing recognition core (D-516, plan §4.2): scores a resampled stroke against dictionary
 * candidates and returns the best words, ranked. This is the R1 recognition core only - deliberately
 * no UI connection (no view, no commit path, no gesture classification; those are R2/D-517). Nothing
 * here is observable with the feature switch off, and R2's `GlideGestureController` is the component
 * that decides whether a stroke is ever fed to this class.
 *
 * Mechanism, per the plan's literature-standard shape (SHARK2-lineage, the same conceptual design
 * AOSP LatinIME uses): the observed stroke (speed-invariantly resampled, see [GlideTrace]) is
 * aligned against each candidate's *ideal path* - the polyline through the key centres of the
 * candidate's folded spelling - with a simplified DTW; the mean aligned point distance plus a small
 * length-mismatch penalty form the geometric cost, which a log-scaled frequency prior (the same
 * family as D-411/D-365) and a personal bigram/trigram context bonus (the existing
 * [LearnedBigramBoost] rescaling) compete against. No kernel, no neural network, no precomputed
 * per-word tables: the only per-candidate work is building the ideal polyline (cheap) and one
 * bounded DTW alignment.
 *
 * Candidate pool: a shortlist, never a full scan - the plan's bill. The pool is the most frequent
 * words sharing the stroke's start key (plus the start key's diacritic variants, so a trace over the
 * `a` key reaches `ä`-initial words), then prefiltered cheaply by the plan's two endpoint rules
 * (first and last folded letter must be the stroke's own start/end keys, actually near them) and a
 * length-ratio window, before any alignment runs. This keeps the pool's own frequency truncation
 * (the ordinary prefix scan's established trade-off, D-65/D-209) the known recall bound for
 * very rare words - R3's calibration round revisits if device use shows it binding.
 *
 * Folding: geometry and candidates are computed entirely on folded forms ([DiacriticFolding.fold]),
 * so a stroke over `a` matches `ä` at zero geometric penalty - the "Umlauts Are Ordinary Characters"
 * principle, carried over from the tap pipeline unchanged. A doubled letter's two waypoints collapse
 * under arc-length resampling (a dwell has no length), so `Grüße`'s folded path and `Gruse`'s are
 * geometrically identical: that family is separated by frequency/context, exactly the adversarial
 * pairs the plan's §6 tests pin. Display/commit casing is the stored entry's canonical form.
 *
 * Pure by design: no Android imports. [store] is the same [DictionaryStore] abstraction the
 * suggestion pipeline uses; the R2 wiring feeds traces from the view on a background executor and
 * passes [isCancelled] so a superseded computation is thrown away between candidates - the
 * established cooperative-cancellation pattern of `DictionarySuggestionProvider`.
 */
class GlideRecognizer(
    private val store: DictionaryStore,
    private val layout: GlideKeyGeometry,
    private val folding: DiacriticFolding = NoOpDiacriticFolding,
    private val poolLimit: Int = DEFAULT_POOL_LIMIT,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Scores [trace] against the candidate pool and returns up to [maxCount] candidates, best
     * first. Empty (never throwing) for a degenerate trace, a stroke that starts or ends far from
     * any key, or an empty pool - the caller treats that as "no glide word found" and shows nothing.
     *
     * @param trace the stroke, resampled (see [GlideTrace.resample])
     * @param previousWord the word committed immediately before, for the context bonus - same
     *        shape as [de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider]'s own
     *        ranking, null at a fresh start
     * @param previousPreviousWord D-246: the word two positions before, or null when unknown
     * @param isCancelled cooperative cancellation - polled between candidates so a superseded
     *        live update is abandoned cheaply mid-loop
     * @param maxCount how many candidates to return (the pinned live chip plus the bar's
     *        alternates need only a handful)
     */
    fun candidates(
        trace: GlideTrace,
        previousWord: String? = null,
        previousPreviousWord: String? = null,
        isCancelled: () -> Boolean = { false },
        maxCount: Int = DEFAULT_MAX_CANDIDATES
    ): List<GlideCandidate> {
        if (trace.points.size < GlideTrace.MIN_POINTS) {
            return emptyList()
        }
        val startKey = layout.nearestKey(trace.points.first()) ?: return emptyList()
        val endKey = layout.nearestKey(trace.points.last()) ?: return emptyList()
        if (!isNearKey(trace.points.first(), startKey) || !isNearKey(trace.points.last(), endKey)) {
            return emptyList()
        }
        val startPrefixes = LinkedHashSet<Char>()
        startPrefixes.add(startKey)
        startPrefixes.addAll(folding.variantsOf(startKey))
        val pool = LinkedHashMap<String, Long>()
        for (prefix in startPrefixes) {
            for (entry in store.unigramsByPrefix(prefix.toString(), poolLimit)) {
                if (store.isBlacklisted(entry.word)) {
                    continue // A-04
                }
                pool.putIfAbsent(entry.word, entry.frequency)
            }
        }
        val traceSpacing = maxOf(
            GlideTrace.DEFAULT_SPACING_PX,
            trace.arcLength / MAX_PATH_POINTS
        )
        val results = ArrayList<GlideCandidate>(pool.size)
        for ((word, frequency) in pool) {
            if (isCancelled()) {
                break
            }
            val candidate = scoreCandidate(
                word, frequency, trace, startKey, endKey, traceSpacing, previousWord, previousPreviousWord
            )
            if (candidate != null) {
                results.add(candidate)
            }
        }
        return results.sortedByDescending { it.score }.take(maxCount)
    }

    /**
     * The full per-candidate pipeline: fold, endpoint prefilter, length-ratio window, alignment.
     * Returns null for every candidate the cheap gates reject - the plan's shortlist discipline
     * ("nur Kandidaten, die eine billige Vorfilterung überstehen").
     */
    private fun scoreCandidate(
        word: String,
        frequency: Long,
        trace: GlideTrace,
        startKey: Char,
        endKey: Char,
        traceSpacing: Double,
        previousWord: String?,
        previousPreviousWord: String?
    ): GlideCandidate? {
        val folded = folding.fold(word.lowercase())
        if (folded.length < MIN_FOLDED_LENGTH || folded.length > MAX_FOLDED_LENGTH) {
            return null
        }
        if (folded.first() != startKey || folded.last() != endKey) {
            return null
        }
        val waypoints = ArrayList<GlidePoint>(folded.length)
        for (c in folded) {
            val center = layout.centerOf(c) ?: return null // not typeable on this layout
            waypoints.add(center)
        }
        val idealArc = polylineLength(waypoints)
        if (idealArc <= 0.0) {
            return null
        }
        val ratio = idealArc / trace.arcLength
        if (ratio < MIN_ARC_RATIO || ratio > MAX_ARC_RATIO) {
            return null
        }
        val idealSpacing = maxOf(traceSpacing, idealArc / MAX_PATH_POINTS)
        val ideal = GlideTrace.resample(waypoints, idealSpacing) ?: return null
        val geometricCost = alignmentCost(trace.points, ideal.points)
        val lengthPenalty = LENGTH_PENALTY_WEIGHT * abs(ln(ratio)).coerceAtMost(MAX_LENGTH_PENALTY)
        val total = geometricCost + lengthPenalty - frequencyPrior(frequency) - contextBonus(
            word, previousWord, previousPreviousWord
        )
        return GlideCandidate(word = word, score = -total, geometricCost = geometricCost)
    }

    /** Endpoint proximity: the raw endpoint must actually sit near the key it resolves to. */
    private fun isNearKey(point: GlidePoint, key: Char): Boolean {
        val center = layout.centerOf(key) ?: return false
        return point.distanceTo(center) <= ENDPOINT_MAX_PITCHES * layout.minKeyPitch
    }

    /**
     * Simplified DTW between the observed and the ideal point lists: the mean aligned point
     * distance over the optimal monotone alignment path. Bounded by [MAX_PATH_POINTS] on both
     * sides (the spacings above keep each list within the cap), so one alignment is at most
     * [MAX_PATH_POINTS]² cells - trivial on modern hardware, per the plan's budget.
     */
    private fun alignmentCost(observed: List<GlidePoint>, ideal: List<GlidePoint>): Double {
        val n = observed.size
        val m = ideal.size
        var previousCost = DoubleArray(m)
        var previousCount = IntArray(m)
        var currentCost = DoubleArray(m)
        var currentCount = IntArray(m)
        previousCost[0] = observed[0].distanceTo(ideal[0])
        previousCount[0] = 1
        for (j in 1 until m) {
            previousCost[j] = previousCost[j - 1] + observed[0].distanceTo(ideal[j])
            previousCount[j] = previousCount[j - 1] + 1
        }
        for (i in 1 until n) {
            currentCost[0] = previousCost[0] + observed[i].distanceTo(ideal[0])
            currentCount[0] = previousCount[0] + 1
            for (j in 1 until m) {
                val diagonal = previousCost[j - 1]
                val up = previousCost[j]
                val left = currentCost[j - 1]
                val (bestCost, bestCount) = when {
                    diagonal <= up && diagonal <= left -> diagonal to previousCount[j - 1]
                    up <= left -> up to previousCount[j]
                    else -> left to currentCount[j - 1]
                }
                currentCost[j] = bestCost + observed[i].distanceTo(ideal[j])
                currentCount[j] = bestCount + 1
            }
            val swapCost = previousCost
            val swapCount = previousCount
            previousCost = currentCost
            previousCount = currentCount
            currentCost = swapCost
            currentCount = swapCount
        }
        return previousCost[m - 1] / previousCount[m - 1]
    }

    /**
     * Log-scaled frequency prior - the D-411/D-365 family: a word at [FREQUENCY_REFERENCE] gets the
     * full discount, below it the prior ramps up gradually, above it growth tapers. Ranking-only,
     * exactly like every other use of this curve family.
     */
    private fun frequencyPrior(frequency: Long): Double {
        if (frequency <= 0L) {
            return 0.0
        }
        return FREQUENCY_PRIOR_WEIGHT * ln(1.0 + frequency) / ln(1.0 + FREQUENCY_REFERENCE)
    }

    /**
     * Personal n-gram context bonus (plan §4.2 step 3), mirroring
     * [de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider]'s own ranking shape: a
     * genuine trigram match wins via the existing [LearnedBigramBoost] rescaling (D-366/D-429); a
     * bigram-only candidate is discounted by the same Stupid-Backoff factor; the bundled bigram
     * contribution is log-scaled against the same reference [LearnedBigramBoost] itself uses.
     * The whole bonus is expressed in pixel-cost equivalents ([CONTEXT_WEIGHT] translates the
     * n-gram curve's 0-to-several-hundred range into the geometric scale, so a near-tie flips on
     * context while a clear shape win stands) - ranking-only, nothing correctness-affecting reads
     * this. R3-calibrated.
     */
    private fun contextBonus(word: String, previousWord: String?, previousPreviousWord: String?): Double {
        if (previousWord == null) {
            return 0.0
        }
        if (previousPreviousWord != null) {
            val trigram = store.trigramWithTimestamp(previousPreviousWord, previousWord, word)
            if (trigram != null && trigram.count > 0L) {
                return CONTEXT_WEIGHT * LearnedBigramBoost.boost(trigram.count, trigram.lastTouched, now())
            }
        }
        val total = store.bigramFrequency(previousWord, word)
        if (total <= 0L) {
            return 0.0
        }
        val learned = store.learnedBigramWithTimestamp(previousWord, word) ?: LearnedNgram(0L, 0L)
        val bundledOnly = (total - learned.count).toDouble()
        val bundled = if (bundledOnly > 0.0) {
            ln(1.0 + bundledOnly) / ln(1.0 + BUNDLED_BIGRAM_REFERENCE)
        } else {
            0.0
        }
        val backoff = if (previousPreviousWord != null) TRIGRAM_BACKOFF_WEIGHT else 1.0
        val bonus = bundled + LearnedBigramBoost.boost(learned.count, learned.lastTouched, now())
        return CONTEXT_WEIGHT * backoff * bonus
    }

    private fun polylineLength(points: List<GlidePoint>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            total += points[i - 1].distanceTo(points[i])
        }
        return total
    }

    companion object {

        /**
         * Candidate-pool cut per start-key prefix, mirroring `SqliteDictionaryStore`'s own
         * CANDIDATE_LIMIT scale. Post-prefilter this lands at the plan's "a few hundred
         * alignments" target; the pool's frequency truncation is the accepted recall bound for
         * very rare words (see the class KDoc).
         */
        const val DEFAULT_POOL_LIMIT = 2000

        /** The pinned live chip plus the alternates the bar shows need only a handful. */
        const val DEFAULT_MAX_CANDIDATES = 8

        /** A glide word is at least two letters (a one-letter stroke is a tap, T-01 territory). */
        private const val MIN_FOLDED_LENGTH = 2

        /** Bound on silly-long entries reaching the aligner. */
        private const val MAX_FOLDED_LENGTH = 20

        /**
         * Both aligned paths are capped at this many points (the spacings adapt, see the callers),
         * bounding one DTW alignment to 160² cells regardless of stroke length.
         */
        private const val MAX_PATH_POINTS = 160

        /**
         * The ideal path may not be drastically longer or shorter than the stroke - the plan's
         * "Länge ±" prefilter plus the small length-mismatch penalty the cost formula names. A
         * ratio outside this window is rejected outright; inside it, the penalty ramps with
         * [LENGTH_PENALTY_WEIGHT]. Considered starting points, R3-calibrated - deliberately a
         * generous window, since this gate is a cheap shortlist cut, not a quality judgement (a
         * genuinely wrong-shape candidate inside the window loses at the alignment anyway).
         */
        private const val MIN_ARC_RATIO = 0.3
        private const val MAX_ARC_RATIO = 3.0
        private const val LENGTH_PENALTY_WEIGHT = 6.0
        private const val MAX_LENGTH_PENALTY = 10.0

        /**
         * The endpoint must sit within this many minimum key pitches of the key it resolves to -
         * "erste+letzte Taste in Endpunktnähe", resolution-independent by construction.
         */
        private const val ENDPOINT_MAX_PITCHES = 1.0

        /** See [frequencyPrior]. Corpus-scale reference, matching the D-411 family's range. */
        private const val FREQUENCY_PRIOR_WEIGHT = 6.0
        private const val FREQUENCY_REFERENCE = 100_000.0

        /** See [contextBonus]. Considered starting points, R3-calibrated. */
        private const val CONTEXT_WEIGHT = 0.1

        // The bundled bigram contribution's own log-scale reference - the same 250 figure
        // LearnedBigramBoost calibrates its curve against (the bundled bigram data's 90th-95th
        // percentile band), kept local since that constant is deliberately private to its own file.
        private const val BUNDLED_BIGRAM_REFERENCE = 250.0

        // D-246's established Stupid-Backoff discount, same role as the provider's own constant.
        private const val TRIGRAM_BACKOFF_WEIGHT = 0.4
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import kotlin.math.ln

/**
 * D-411: bridges a learned word's own raw reinforcement count (a personal-use tally - W-02's threshold of
 * 2-4 to even reach [DictionaryStore.learnedFrequencyOf], growing by exactly 1 per confirmed commit) into a
 * value that competes meaningfully against the bundled dictionary's own corpus-scale frequencies (up to
 * roughly 1,000,000) for suggestion-bar ranking purposes only - see
 * [DictionarySuggestionProvider]'s own KDoc for why this can never affect any correctness-affecting
 * decision (A-01's known-word-override check, D-353's `CorrectionConfidence` both read frequency directly
 * from the store, never through this boost).
 *
 * Log-scaled rather than a flat multiplier - mirrors `CorrectionConfidence`'s own log-scaled ratio (D-353)
 * for the analogous problem of bridging two very different frequency scales without a hard cliff: a word
 * used [REFERENCE_COUNT] times reaches exactly [REFERENCE_FREQUENCY]; below that it ramps up gradually
 * rather than jumping in all at once the moment some threshold is crossed, and above it growth continues
 * but tapers, so a word used thousands of times does not run away to an unbounded score. A recently-touched
 * word (within [RECENT_WINDOW_MILLIS]) is additionally multiplied by [RECENT_FACTOR] - once a word's
 * personal use goes quiet, it settles back to its plain scaled value rather than staying inflated forever
 * purely because it was heavily used a long time ago.
 *
 * Calibrated directly with the user, not guessed: [REFERENCE_FREQUENCY] sits comfortably above
 * `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY`'s own "trustworthy" bar (300) without approaching a genuinely
 * common word's real corpus frequency (tens of thousands and up), so a well-established personal word
 * competes with ordinary nouns without automatically outranking common function words - matching the
 * user's own explicit requirement that learned words must not be categorically preferred, only once they
 * have genuinely earned it.
 */
object LearnedFrequencyBoost {
    
    private const val REFERENCE_FREQUENCY = 5000.0
    private const val REFERENCE_COUNT = 50.0
    private const val RECENT_WINDOW_MILLIS = 14L * 24 * 60 * 60 * 1000
    private const val RECENT_FACTOR = 1.5
    
    /**
     * @param learnedFrequency the word's own raw reinforcement count (0 or less yields 0, no boost)
     * @param lastTouched epoch millis of the most recent write to this entry
     * @param now epoch millis "now" - threaded through rather than read directly inside this function, so
     *        callers (and their own tests) stay deterministic
     * @return the scaled, ranking-only frequency contribution
     */
    fun boost(learnedFrequency: Long, lastTouched: Long, now: Long): Double {
        if (learnedFrequency <= 0L) {
            return 0.0
        }
        val scaled = REFERENCE_FREQUENCY * ln(1.0 + learnedFrequency) / ln(1.0 + REFERENCE_COUNT)
        val recent = now - lastTouched in 0..RECENT_WINDOW_MILLIS
        return if (recent) scaled * RECENT_FACTOR else scaled
    }
}

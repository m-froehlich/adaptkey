// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import kotlin.math.ln

/**
 * D-365: bridges a self-learned bigram's own raw reinforcement count (+1 per confirmed commit, see
 * [SqliteDictionaryStore]'s own `TABLE_LEARNED_BIGRAMS`) into a value that competes meaningfully against
 * the bundled corpus's own bigram counts for ranking purposes only - the same problem
 * [LearnedFrequencyBoost] already solved for individual learned words (D-411), calibrated separately here
 * since the two scales are genuinely different: the bundled German bigram data
 * (`dictionaries/de/bigram.tsv`, 79,937 rows) ranges from 25 to 113,526, with a median of 48 and a 90th
 * percentile around 193 - nowhere near a word's own corpus-scale frequency (up to roughly 1,000,000).
 *
 * Log-scaled for the same reason as [LearnedFrequencyBoost]: a count of [REFERENCE_COUNT] reaches exactly
 * [REFERENCE_FREQUENCY]; below that it ramps up gradually rather than jumping in all at once, and above it
 * growth continues but tapers. [REFERENCE_COUNT] is kept the same as [LearnedFrequencyBoost]'s own -
 * "used 50 times" already means "well-established" consistently across the app, whether that use is a
 * word or a bigram. [REFERENCE_FREQUENCY] sits between the bundled bigram data's own 90th (193) and 95th
 * (340) percentile - solidly "well-established" without chasing the extreme tail (a handful of extremely
 * common pairs like "in der" at 113,526).
 *
 * No recency factor, unlike [LearnedFrequencyBoost] - `TABLE_LEARNED_BIGRAMS` carries no last-touched
 * timestamp; deliberately deferred to a later round rather than adding a schema migration for it now
 * (2026-09-01, user's own explicit call).
 *
 * D-366 reuses this same curve for a personal trigram count too (never for a bigram) - structurally the
 * same shape of quantity (a small, personal-only reinforcement count that needs to compete against a
 * corpus-scale contribution), even though the underlying calibration data above is bigram-specific.
 *
 * Ranking-only, mirroring [LearnedFrequencyBoost]'s own discipline exactly: never used for [TokenRepair]'s
 * own `>= MIN_BIGRAM` merge gate (A-06), which must keep reading [DictionaryStore.bigramFrequency] directly
 * - only [DictionarySuggestionProvider]'s own ranking functions use this.
 */
object LearnedBigramBoost {
    
    private const val REFERENCE_FREQUENCY = 250.0
    private const val REFERENCE_COUNT = 50.0
    
    /**
     * @param count the bigram's (or, per D-366, a trigram's) own raw reinforcement count (0 or less
     *        yields 0, no boost)
     * @return the scaled, ranking-only frequency contribution
     */
    fun boost(count: Long): Double {
        if (count <= 0L) {
            return 0.0
        }
        return REFERENCE_FREQUENCY * ln(1.0 + count) / ln(1.0 + REFERENCE_COUNT)
    }
}

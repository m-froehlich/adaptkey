// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-429: a learned bigram's or trigram's own raw reinforcement count and when it was last touched,
 * mirroring [LearnedFrequency]'s identical shape for individual learned words (D-411) - shared between
 * both n-gram orders since a trigram row carries no bundled counterpart to merge against either way (see
 * [SqliteDictionaryStore]'s own KDoc), so this is simply the row's full state rather than a "learned-only
 * slice" of something bigger.
 *
 * @property count the row's own reinforcement count
 * @property lastTouched epoch millis of the most recent write to this entry
 */
data class LearnedNgram(val count: Long, val lastTouched: Long)

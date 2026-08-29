// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-388: one row of [SqliteDictionaryStore.learnedWordsWithTimestamp] - a learned word together with when
 * it was last written (promoted, reinforced, re-cased, or restored from a backup), for the Learned Words
 * editor's own recency sort.
 *
 * @property word the learned word, in its own stored casing
 * @property frequency how many times it has been reinforced since promotion
 * @property lastTouched epoch millis of the most recent write to this entry
 */
data class LearnedWordEntry(val word: String, val frequency: Long, val lastTouched: Long)

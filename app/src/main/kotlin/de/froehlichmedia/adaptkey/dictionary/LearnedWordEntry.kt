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
 * @property partsOfSpeech D-404: the entry's own category tags, or empty when still undetermined
 *        ("unbekannt" - the editor's own asterisk marker)
 * @property lemma D-404: the base-form word this entry is linked to, or null when it is itself a base form
 *        (or has no recorded link) - the editor's own list-consolidation filter shows only entries with a
 *        null [lemma] as their own top-level row.
 */
data class LearnedWordEntry(
    val word: String,
    val frequency: Long,
    val lastTouched: Long,
    val partsOfSpeech: Set<PartOfSpeech> = emptySet(),
    val lemma: String? = null
)

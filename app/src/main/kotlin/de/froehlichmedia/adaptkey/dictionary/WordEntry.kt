// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * A dictionary unigram entry.
 *
 * @property word the word in its canonical case
 * @property frequency the learned frequency count (higher means more probable)
 * @property partsOfSpeech the part-of-speech tags for the capitalisation hierarchy (§6); may be empty
 * @property lemma D-412: for a bundled entry that is itself an inflected form of another bundled word
 *        (e.g. "ging" of "gehen"), that base form's own key, or null for a base form itself or any entry
 *        with no recorded link. Bundled-dictionary-only ({@link SqliteDictionaryStore}'s `TABLE_WORDS}) -
 *        never set on a learned entry. Not yet read anywhere; laid down as groundwork for a future
 *        inflection-aware feature (D-404).
 */
data class WordEntry(
    val word: String,
    val frequency: Long,
    val partsOfSpeech: Set<PartOfSpeech> = emptySet(),
    val lemma: String? = null
)

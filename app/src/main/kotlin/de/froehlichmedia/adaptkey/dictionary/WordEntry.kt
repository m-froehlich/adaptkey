// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * A dictionary unigram entry.
 *
 * @property word the word in its canonical case
 * @property frequency the learned frequency count (higher means more probable)
 * @property partsOfSpeech the part-of-speech tags for the capitalisation hierarchy (§6); may be empty
 * @property lemma D-412/D-404: for an entry that is itself an inflected form of another entry in the same
 *        table (e.g. "ging" of "gehen"), that base form's own key, or null for a base form itself or any
 *        entry with no recorded link. For a bundled entry ({@link SqliteDictionaryStore}'s `TABLE_WORDS`)
 *        this is generated at dictionary-build time (D-412) and is read-only at runtime. For a learned
 *        entry (`TABLE_LEARNED`) this is D-404's own conservative, lookup-only link - never a guessed
 *        write, only ever set when the linked word already exists as its own learned entry - and is what
 *        lets the Learned Words editor list only base forms, consolidating their inflected family behind
 *        them (see {@link LearnedLemmaLinking}).
 */
data class WordEntry(
    val word: String,
    val frequency: Long,
    val partsOfSpeech: Set<PartOfSpeech> = emptySet(),
    val lemma: String? = null
)

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * One learned two-word-context trigram row (S-07/D-246), as read back from [SqliteDictionaryStore] for the
 * D-278 backup/export feature - there is no bundled counterpart to merge against, unlike [WordEntry] or
 * [DictionaryAssetParser.Bigram], since the trigram table is personal-only (see [SqliteDictionaryStore]'s
 * own KDoc).
 *
 * @property previousPreviousWord the word two positions before [word] (lower-cased key, as stored)
 * @property previousWord the word immediately before [word] (lower-cased key, as stored)
 * @property word the predicted word (lower-cased key, as stored)
 * @property count the learned occurrence count
 */
data class TrigramEntry(
    val previousPreviousWord: String,
    val previousWord: String,
    val word: String,
    val count: Long
)

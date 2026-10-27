// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * A single suggestion candidate.
 * 
 * @property word the suggested word
 * @property score the relative probability/frequency; higher means more probable (S-01 sort key)
 * @property pinned D-503: true when this candidate is an n-gram continuation (bigram/trigram partner)
 *           of the previous word that case-insensitively starts with the typed prefix. Pinned
 *           candidates always occupy the suggestion bar's leading slots, in the order given, ahead of
 *           every ordinary score-ranked candidate - [SuggestionController] keeps them exempt from S-03
 *           position stabilisation and S-04's score-only re-sort. The score still matters within the
 *           pinned group (it is the same n-gram ranking `nextWordSuggestions()` uses, so the pinned
 *           order equals the blank-slate prediction order filtered down to prefix matches).
 */
data class Suggestion(
    val word: String,
    val score: Double,
    val pinned: Boolean = false
)

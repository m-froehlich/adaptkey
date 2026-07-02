// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * A single suggestion candidate.
 *
 * @property word the suggested word
 * @property score the relative probability/frequency; higher means more probable (S-01 sort key)
 */
data class Suggestion(
    val word: String,
    val score: Double
)

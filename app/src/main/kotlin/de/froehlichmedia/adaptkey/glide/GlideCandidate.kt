// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

/**
 * One scored glide candidate (D-516). [word] is the dictionary entry's canonical casing
 * (`unigramsByPrefix` already delivers it) - display and commit use it exactly as stored, never a
 * re-derived form. [score] follows the codebase's suggestion convention: higher is better. It folds
 * the geometric alignment cost, the length-mismatch penalty, the log-scaled frequency prior and the
 * personal n-gram context bonus into one comparable value. [geometricCost] (mean aligned point
 * distance, lower is better) is carried separately for tests and diagnostics - it is the part that
 * says how well the stroke's shape matches the word's ideal path, independent of how common the
 * word is.
 */
data class GlideCandidate(
    val word: String,
    val score: Double,
    val geometricCost: Double
)

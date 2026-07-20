// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-207: [SuggestionProvider.bestCorrectionFor]'s result - the best autocorrect candidate for a token,
 * plus whether it also qualifies as a high-confidence (D-67) correction, from a single search.
 *
 * @property word the proposed correction
 * @property highConfidence true when [word] would also qualify as [SuggestionProvider.highConfidenceCorrection]'s
 *           tighter cost budget
 */
data class Correction(
    val word: String,
    val highConfidence: Boolean
)

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

/**
 * A request handed to the tier-3 predictor's D-404 family-learning task: given [word], determine its
 * category and its whole inflectional family, so the caller can learn them all together, linked - the
 * "with LLM, always learn the whole family" half of D-404 (see [Tier3FamilyResult] and
 * [Tier3Provider.predictFamily]), distinct from [Tier3Request]'s own next-word-continuation task.
 *
 * @property word the word just learned (its own typed casing)
 * @property sentence the surrounding sentence context, when available (A-02-style, no punctuation
 *           truncation) - empty for a backfill reprocessing pass run long after the word was originally
 *           typed, where no original context was ever recorded to begin with.
 */
data class Tier3FamilyRequest(
    val word: String,
    val sentence: String = ""
)

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

/**
 * The glide release's commit decision, as one pure function (D-517, plan §4.3): what word commits
 * when the stroke releases, and whether it goes through the ordinary learning pipeline.
 *
 * - A dictionary candidate ([GlideCandidate]) commits with its resolved casing - the caller's
 *   [capitalise] lambda applies the exact `CapitalisationEngine.capitalise()` derivation the
 *   suggestion-chip path uses, keyed on the live composing text (D-440's rule: composing non-empty
 *   means re-derive; empty means the canonical form is already resolved and commits verbatim).
 *   No autocorrect ever runs - the word was explicitly chosen by the stroke.
 * - No candidate at all commits the stroke's inferred letter sequence as a (possibly misspelled)
 *   word (plan §8.3) - and never learns it: an unknown glide word is never auto-added to the
 *   personal dictionary, mirroring how other keyboards treat an out-of-vocabulary glide.
 */
object GlideCommitPolicy {

    /**
     * @param word the committed word (casing already resolved)
     * @param learn whether the ordinary commit-time learning (unigram/n-gram) runs for it
     */
    data class Decision(val word: String, val learn: Boolean)

    /**
     * @param topCandidate the stroke's best dictionary candidate, or null when none survived
     * @param inferredSequence the letter sequence the stroke visited (the unknown-word fallback)
     * @param composingText the live composing token (the preview), empty when none was ever shown
     * @param capitalise the caller's casing derivation - `CapitalisationEngine.capitalise(word,
     *        contextFor(composing))`, exactly what a suggestion-chip commit runs
     */
    fun resolve(
        topCandidate: String?,
        inferredSequence: String,
        composingText: String,
        capitalise: (String) -> String
    ): Decision {
        val raw = topCandidate ?: inferredSequence
        if (composingText.isEmpty()) {
            return Decision(raw, topCandidate != null)
        }
        return Decision(capitalise(raw), topCandidate != null)
    }
}

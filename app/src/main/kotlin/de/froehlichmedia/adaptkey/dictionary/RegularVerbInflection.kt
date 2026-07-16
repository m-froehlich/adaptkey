// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-115 / D-125: recognises a token as a plausible regular ("weak") German verb inflection of a known
 * infinitive, without needing every inflected form to exist in the dictionary itself. Deliberately
 * generic over the *pattern*, not any specific word: German weak verbs form their present tense and
 * preterite by a small, closed, highly productive set of personal endings on the infinitive's stem, so
 * stripping a candidate ending and re-appending the infinitive's own `-en` recovers the stem whenever the
 * token really is such an inflection - e.g. `beurteilst` (2nd person singular, not itself in the
 * dictionary) strips `-st` to `beurteil`, and `beurteil` + `en` = `beurteilen`, a known word.
 *
 * Only used to *protect* a token from being silently autocorrected to a different real word (the reported
 * bug: `beurteilst` -> `beurteilt`) - never to suggest or auto-apply anything, and never a claim that the
 * exact inflection is grammatically valid German (a coincidental match against an unrelated known word
 * ending in `-en` is possible in principle, accepted as a low-probability trade-off of a deliberately
 * simple, data-free heuristic). Covers the common present-tense and preterite personal endings; participles
 * (`ge-...-t`), strong/irregular ("ablaut") verbs and subjunctive forms are out of scope - see the
 * project's own backlog note on generating those algorithmically as a separate, later idea.
 */
object RegularVerbInflection {
    
    // Longest-first is not required for correctness (each candidate stem is independently checked against
    // the dictionary; a wrong, shorter split simply fails the lookup), but keeps the list readable in the
    // order these endings actually appear: preterite personal endings, then present-tense ones.
    private val ENDINGS = listOf(
        "test", "ten", "tet", "te", // preterite: du/wir+sie/ihr/ich+er
        "est", "st", "et", "t", "e" // present tense: du(+dental stem)/du/er+ihr(+dental stem)/er+ihr/ich
    )
    
    /**
     * @param token the composing token, in any case
     * @param isKnownWord looks up whether a candidate reconstructed infinitive is a known dictionary word
     * @return true when stripping some regular personal ending and appending `en` yields a known word
     */
    fun isPlausibleInflection(token: String, isKnownWord: (String) -> Boolean): Boolean {
        val lower = token.lowercase()
        for (ending in ENDINGS) {
            if (lower.length <= ending.length) {
                continue
            }
            val stem = lower.removeSuffix(ending)
            if (isKnownWord(stem + "en")) {
                return true
            }
        }
        return false
    }
}

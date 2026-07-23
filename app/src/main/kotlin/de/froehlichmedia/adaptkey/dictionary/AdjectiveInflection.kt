// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-252: recognises a token as a plausible regular German adjective comparative/superlative form of a
 * known positive (base) adjective, without needing every inflected form to exist in the dictionary itself
 * - the adjective counterpart of [RegularVerbInflection], same shape, one difference: a comparative/
 * superlative ending is stripped and the bare remaining stem is checked directly (nothing is re-appended,
 * unlike the verb case's own `+"en"` infinitive reconstruction, since the positive adjective carries no
 * suffix of its own). E.g. `zuversichtlicher` strips `-er` to `zuversichtlich`, a known word - so the whole
 * token is treated as already recognised and never offered to [TokenRepair]'s split-candidate search at
 * all, closing the reported `zuversichtlicher` -> `zuversichtlich er` false positive at the source (`er`,
 * the German 3rd-person pronoun, is an extremely frequent word - exactly why it kept winning as a split
 * half). Neither adjective stems nor `zuversichtlich` itself are tagged [PartOfSpeech.ADJECTIVE] anywhere
 * in the bundled dictionary (confirmed: zero entries carry that tag) - deliberately not gated on it, mirrors
 * [RegularVerbInflection]'s own POS-free, pure-lookup shape.
 *
 * Only used to *protect* a token from being wrongly split (or offered as a D-122 mid-word split
 * suggestion) - never to suggest or auto-apply a comparative/superlative form itself, and never a claim
 * that the exact inflection is grammatically valid German (a coincidental match against an unrelated known
 * word is possible in principle, the same accepted trade-off [RegularVerbInflection] already makes).
 * Deliberately scoped to the *regular* case only: an umlaut-mutating comparative/superlative (`alt` ->
 * `älter`, `groß` -> `größer`) is out of scope, mirroring [RegularVerbInflection]'s own exclusion of
 * strong/ablaut verbs (D-158) - the same "cover the common, regular case; leave the irregular one to the
 * dictionary/tier-3 to eventually absorb via learning" scoping precedent.
 *
 * D-252 (found by the existing test suite, not guessed): a plain "is the stem a known word" check
 * over-triggers on a real, already-established case - "Docker" (unknown) must still split into "dock"
 * (`NOUN`) + "er" (D-244's own regression test), but a bare known-word check would treat "docker" as a
 * plausible comparative of "dock" first and block the split entirely. German nouns do not take
 * comparative/superlative degree at all, so [isPlausiblePositive] is expected to also exclude a stem
 * resolving to a noun - [TokenRepair.isAlreadyRecognised] passes exactly that (known **and** not a noun),
 * not a bare [DictionaryStore.isKnownWord].
 */
object AdjectiveInflection {
    
    // Comparative (-er) and superlative (-st...) declension endings; order does not affect correctness
    // (each candidate stem is independently checked against the dictionary), grouped by degree for
    // readability.
    private val ENDINGS = listOf(
        // Comparative: er (schöner), and its declined attributive forms.
        "er", "ere", "eren", "erem", "erer", "eres",
        // Superlative: predicative "am ...sten", and its declined attributive forms.
        "ste", "sten", "stem", "ster", "stes"
    )
    
    /**
     * @param token the composing token, in any case
     * @param isPlausiblePositive whether a candidate reconstructed stem is a plausible positive adjective -
     *        expected to require both a known word *and* not a noun (see this object's own KDoc)
     * @return true when stripping some regular comparative/superlative ending yields a plausible positive
     */
    fun isPlausibleComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean {
        val lower = token.lowercase()
        for (ending in ENDINGS) {
            if (lower.length <= ending.length) {
                continue
            }
            val stem = lower.removeSuffix(ending)
            if (isPlausiblePositive(stem)) {
                return true
            }
        }
        return false
    }
}

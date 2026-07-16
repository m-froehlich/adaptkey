// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-116: recognises an unknown token as a plausible unhyphenated German compound - a known noun followed
 * by an optional Fugenelement (linking element) and a remainder that is itself a known word or a
 * high-confidence correction of one. Deliberately scoped down from full compound decomposition (B-01
 * already handles the *hyphenated* case): only a single split point is tried (first part vs. rest, no
 * recursion into further parts), and the first part must be a *known, unmodified* dictionary noun - a typo
 * inside the first part is out of scope, matching how the request was framed.
 *
 * The search tries the longest possible first part first, since a shorter one is more likely to be a
 * spurious coincidental match (the classic German compound-splitting ambiguity, e.g. "Wachstube" as
 * "Wachs"+"tube" vs. "Wach"+"Stube" - both individually valid words). This does not eliminate that class of
 * mistake, only reduces its likelihood; combined with [rest] having to independently resolve to something
 * plausible, a wrong split point usually just fails to produce any candidate at all rather than producing a
 * confidently wrong one.
 */
object CompoundSplit {
    
    // Common German Fugenelemente between compound parts, tried longest-first so a genuine "es"/"en" is not
    // pre-empted by a coincidental single-letter match; "" covers a direct concatenation with no linking
    // element at all (e.g. "Haus" + "schuh").
    private val FUGEN_ELEMENTS = listOf("es", "en", "er", "s", "n", "e", "")
    
    private const val MIN_FIRST_PART_LENGTH = 4
    private const val MIN_REST_LENGTH = 2
    
    /** A recognised split: [firstPart] and [rest] are both lower-case; [fugenElement] may be empty. */
    data class Result(val firstPart: String, val fugenElement: String, val rest: String)
    
    /**
     * @param token the composing token, in any case
     * @param isKnownNoun looks up whether a candidate first part is a known dictionary noun
     * @param resolveRest looks up whether a candidate remainder is itself acceptable (a known word, or
     *        correctable to one), returning the resolved lower-case form, or null when not acceptable
     * @return the longest plausible split, or null when none of the tried first-part lengths work out
     */
    fun split(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): Result? {
        val lower = token.lowercase()
        val maxFirstPartLength = lower.length - MIN_REST_LENGTH
        if (maxFirstPartLength < MIN_FIRST_PART_LENGTH) {
            return null
        }
        for (firstPartLength in maxFirstPartLength downTo MIN_FIRST_PART_LENGTH) {
            val firstPart = lower.substring(0, firstPartLength)
            if (!isKnownNoun(firstPart)) {
                continue
            }
            for (fugen in FUGEN_ELEMENTS) {
                if (!lower.startsWith(fugen, firstPartLength)) {
                    continue
                }
                val rawRest = lower.substring(firstPartLength + fugen.length)
                if (rawRest.length < MIN_REST_LENGTH) {
                    continue
                }
                val resolvedRest = resolveRest(rawRest) ?: continue
                return Result(firstPart, fugen, resolvedRest.lowercase())
            }
        }
        return null
    }
}

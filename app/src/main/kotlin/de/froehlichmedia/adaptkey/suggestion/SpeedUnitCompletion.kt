// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-376: recognises the two points at which a `"km/h"` completion is worth offering, mirroring
 * [TimePattern]'s own "just-typed shape" reasoning rather than relying on the bigram table to happen to know
 * `"km/h"` (which, containing `/`, is never itself a tokenisable dictionary word). Deliberately
 * language-agnostic, unlike [de.froehlichmedia.adaptkey.language.LanguageRules.timeSuggestionWord]'s own
 * German-only `"Uhr"` - `"km/h"` is the same SI notation in German and English alike, not a genuinely
 * language-specific word.
 *
 * - **Trigger 1** ([completionForComposing]): the composing token is exactly `"km"` - offered as a
 *   completion competing directly with the plain `"km"` candidate.
 * - **Trigger 2** ([suffixAfterSlash]): `"km/"` has just been committed (the user typed the `/` themselves
 *   instead of tapping the trigger-1 chip) - offered as a glued, no-leading-space completion for the
 *   remainder, `"h"`.
 */
object SpeedUnitCompletion {
    
    private const val PREFIX = "km"
    
    /** The full completion offered by [completionForComposing]. */
    const val UNIT = "km/h"
    
    /** The glued remainder offered by [suffixAfterSlash]. */
    const val SUFFIX = "h"
    
    // D-376: a plain \b word-boundary rejects the realistic "50km/" case too - \b requires a transition
    // between a \w and a non-\w character, and a digit is itself \w, so there is no boundary between "5" and
    // "k" at all. A negative lookbehind for a preceding Unicode letter is used instead: nothing immediately
    // before "km" (string start), a digit, whitespace or punctuation are all fine (e.g. "50km/", "(km/"), only
    // a preceding *letter* disqualifies it (rejects "akm/", where "km" is a suffix of a longer word, not its
    // own token).
    private val TRIGGER_2_PATTERN = Regex("""(?i)(?<![\p{L}])km/$""")
    
    /**
     * @param composing the current composing token
     * @return [UNIT] when [composing] is exactly `"km"` (case-insensitive), else null
     */
    fun completionForComposing(composing: String): String? {
        return if (composing.equals(PREFIX, ignoreCase = true)) UNIT else null
    }
    
    /**
     * @param textBeforeCursor the text immediately before the cursor, right after a delimiter commit
     * @return [SUFFIX] when [textBeforeCursor] ends with a `"km/"` not itself the tail of a longer word, else
     *         null
     */
    fun suffixAfterSlash(textBeforeCursor: CharSequence): String? {
        return if (TRIGGER_2_PATTERN.containsMatchIn(textBeforeCursor)) SUFFIX else null
    }
}

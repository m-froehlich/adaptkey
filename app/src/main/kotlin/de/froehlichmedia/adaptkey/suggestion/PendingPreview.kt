// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-557: what the S-06 pending preview may promise - the one bar element still gated by an A-03
 * foreign verdict.
 *
 * Background: A-03's own purpose is "leave the text as typed" - a confidently foreign context holds
 * autocorrect back at commit time. That verdict used to empty the whole suggestion bar as well, which
 * was disproportionate (chips are display-only, they can never rewrite anything) and, on the short
 * two-"word" contexts where the classifier is statistically noisy (measured with the 30-profile bundle:
 * 37% of word+first-letter contexts fire, e.g. "Brot m", "Teile m"), read as "sometimes no chips at
 * all". D-557 removed the bar-emptying, so the chips now flow normally in a foreign context. The
 * pending preview is the exception that stays gated: its S-06 pair ("the token as typed" -> the
 * replacement) explicitly promises a silent rewrite, so under a foreign verdict it must degrade to the
 * capitalisation-only preview - the one change the commit side still applies unconditionally in that
 * context (A-03 never gated §6 capitalisation) - never to the autocorrect replacement, or the bar
 * would promise a correction the commit side (whose own A-03 gate is untouched) will never make.
 *
 * Pure bar policy, deliberately free of any Android dependency so the contract is unit-tested on the
 * JVM (the AmbiguousCasingPlacement precedent).
 */
object PendingPreview {

    /**
     * Resolves the raw S-06 preview word for the current refresh.
     *
     * @param input the current composing token, exactly as typed
     * @param backgroundReplacement the deferred search's own pending autocorrect/diacritic replacement,
     *        or null when none was computed (or none qualified)
     * @param foreignContext true when the A-03 verdict held autocorrect back for this token's context
     * @param lowercaseCompetitor D-558: the competing lowercase reading the commit side's rule-3 gate
     *        knows about ([de.froehlichmedia.adaptkey.suggestion.SuggestionProvider.competingLowercaseWord]),
     *        or null. When one exists, the commit side withholds the noun-only force-capitalisation, so
     *        the bare token's capitalisation-only preview would promise a "Mut" the commit will never
     *        apply - and that preview is the entire reason the S-06 verbatim pair exists for such a
     *        token. Withheld only for the bare token itself: a real background replacement's own preview
     *        (D-112, "Fur" -> "Für") is a genuine correction the commit will still make, casing included.
     * @param capitalise the §6 capitalisation the commit side will apply (D-111/D-112's preview parity)
     * @return the (already capitalised) preview word, or null when it would not differ from [input] and
     *         so has nothing to preview
     */
    fun resolve(
        input: String,
        backgroundReplacement: String?,
        foreignContext: Boolean,
        lowercaseCompetitor: String? = null,
        capitalise: (String) -> String
    ): String? {
        val candidate = if (foreignContext) input else (backgroundReplacement ?: input)
        if (candidate == input && lowercaseCompetitor != null) {
            return null
        }
        return capitalise(candidate).takeIf { it != input }
    }
}

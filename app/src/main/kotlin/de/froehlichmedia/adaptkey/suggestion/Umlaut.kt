// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Folds German umlauts and the sharp s to their base ASCII forms (ä→a, ö→o, ü→u, ß→ss), so a token
 * typed without the diacritic (e.g. "grun") matches its correct form ("grün") under the single-edit
 * distance (D-12). Applied to both sides before comparing, it makes an umlaut/ß variant a distance-0
 * match rather than an unreachable one; non-German text is left unchanged.
 */
object Umlaut {
    
    /**
     * Returns [text] with every umlaut / sharp-s folded to its ASCII base (uppercase umlauts fold to the
     * lowercase base). Characters without a folding are copied unchanged.
     *
     * @param text the text to fold
     * @return the folded text
     */
    fun fold(text: String): String {
        val builder = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                'ä', 'Ä' -> builder.append('a')
                'ö', 'Ö' -> builder.append('o')
                'ü', 'Ü' -> builder.append('u')
                'ß' -> builder.append("ss")
                else -> builder.append(c)
            }
        }
        return builder.toString()
    }
    
    /**
     * D-144: the reverse of [fold] - every plausible spelling [text] could have had *before* folding, so a
     * prefix typed entirely without diacritics (skipping the umlaut/ß is a deliberate comfort shortcut, per
     * this project's own founding "umlauts are ordinary characters" principle) still reaches a dictionary
     * prefix search indexed on the *real*, unfolded spelling - e.g. "tatsachl" must still find "tatsächlich"
     * even though the two share no literal prefix at all. [text] itself is always the first entry (the
     * common case: nothing to unfold).
     *
     * Combinatorial in the number of fold-eligible positions (each `a`/`o`/`u` may or may not have
     * originally been `ä`/`ö`/`ü`; each `ss` run may or may not have originally been a single `ß`) - bounded
     * by [MAX_CANDIDATES] so a long, heavily-vowelled prefix can never blow this up; real typed prefixes are
     * short in practice (this exists specifically to help *early*, live completion, D-11), so the cap is
     * never expected to bind for a genuine typing session.
     *
     * @param text the lower-cased text to generate unfold candidates for (typically a composing prefix)
     * @return the candidates, [text] itself always first, each one otherwise unique
     */
    fun unfoldCandidates(text: String): List<String> {
        val results = LinkedHashSet<String>()
        results.add(text)
        unfold(text, 0, StringBuilder(), results)
        return results.toList()
    }
    
    private fun unfold(text: String, index: Int, current: StringBuilder, results: MutableSet<String>) {
        if (results.size >= MAX_CANDIDATES) {
            return
        }
        if (index >= text.length) {
            results.add(current.toString())
            return
        }
        val c = text[index]
        val umlaut = VOWEL_UNFOLD[c]
        when {
            umlaut != null -> {
                current.append(c)
                unfold(text, index + 1, current, results)
                current.setLength(current.length - 1)
                current.append(umlaut)
                unfold(text, index + 1, current, results)
                current.setLength(current.length - 1)
            }
            
            c == 's' && index + 1 < text.length && text[index + 1] == 's' -> {
                current.append("ss")
                unfold(text, index + 2, current, results)
                current.setLength(current.length - 2)
                current.append('ß')
                unfold(text, index + 2, current, results)
                current.setLength(current.length - 1)
            }
            
            else -> {
                current.append(c)
                unfold(text, index + 1, current, results)
                current.setLength(current.length - 1)
            }
        }
    }
    
    private val VOWEL_UNFOLD = mapOf('a' to 'ä', 'o' to 'ö', 'u' to 'ü')
    
    // A considered, generous-enough-for-any-real-prefix cap - see unfoldCandidates' own KDoc for why binding
    // it in practice would mean an unusually long, heavily-vowelled prefix, not ordinary typing.
    private const val MAX_CANDIDATES = 32
    
    /**
     * D-204: the alternate spelling [fold] itself does not produce - [ß] folded to a single, bare `s`
     * instead of [fold]'s own doubled `"ss"`. This app's own keyboard hosts [ß] as the long-press
     * alternative directly on the `s` key (L-05/G-01, mirroring how [ä]/[ö]/[ü] already sit on `a`/`o`/`u`),
     * so a lazily-tapped bare `s` is a direct stand-in for it - a different, real typing convention from
     * [fold]'s own formal-orthographic/Swiss "ss" substitution (`"straße"`/`"strasse"`, `"ruß"`/`"russ"`).
     * Every other umlaut already folds 1:1 onto its own hosting key, so only [ß] needs a second variant.
     *
     * @param text the text to fold
     * @return [text] with every umlaut folded as in [fold], but [ß] folded to a single `s`
     */
    fun foldToHostKey(text: String): String {
        val builder = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                'ä', 'Ä' -> builder.append('a')
                'ö', 'Ö' -> builder.append('o')
                'ü', 'Ü' -> builder.append('u')
                'ß' -> builder.append('s')
                else -> builder.append(c)
            }
        }
        return builder.toString()
    }
    
    /**
     * D-204: every distinct fold [text] plausibly came from - [fold]'s own result always first, plus
     * [foldToHostKey]'s result when it differs (i.e. [text] contains at least one [ß]). Used by
     * [de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider.diacriticRestoration] so both
     * typing conventions are recognised as an equally exact, cost-0 match - never used for [fold]'s own
     * ordinary fuzzy-matching role, so nothing about the existing "ss" convention changes.
     *
     * @param text the text to fold
     * @return the distinct fold variants of [text]
     */
    fun foldVariants(text: String): List<String> {
        val standard = fold(text)
        val hostKey = foldToHostKey(text)
        return if (hostKey == standard) listOf(standard) else listOf(standard, hostKey)
    }
}

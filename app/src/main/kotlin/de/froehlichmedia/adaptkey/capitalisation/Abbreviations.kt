// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * Recognises tokens whose trailing period is NOT a sentence terminator (§6): an active language's own
 * abbreviations that always end in a period, and ordinal / list enumerators such as `1.` (language-agnostic).
 *
 * D-434: the abbreviation set itself is now a parameter, not a hardcoded constant this object reaches for
 * internally - before this, [isAbbreviation]/[isNonTerminalPeriod] always checked against [GERMAN]
 * regardless of the field's actually active language, so every other language's sentence-boundary detection
 * silently ran against German abbreviations (harmless overlap for a few shared Latin abbreviations like
 * `etc.`, actively wrong for anything German-specific like `bzgl.`/`zzgl.` inside non-German text). [GERMAN]
 * survives as the compiled-in default a language without its own `abbreviations.tsv` falls back to (see
 * [de.froehlichmedia.adaptkey.settings.LanguageAbbreviationsLoader]) - mirroring
 * [de.froehlichmedia.adaptkey.keyboard.KeyboardLayout.DEFAULT_LETTER_HINTS]'s own "functional but not
 * tailored" fallback role for L-05/D-281.
 *
 * Multi-word abbreviations written with an internal space (e.g. `z. B.`) are only recognised in their
 * space-free form (`z.B.`); the spaced form and user-learned abbreviations are a later extension.
 */
object Abbreviations {
    
    private val ENUMERATOR = Regex("^\\d+\\.$")
    
    /** Common German abbreviations that always carry a trailing period (lower-cased). */
    val GERMAN: Set<String> = setOf(
        "usw.", "usf.", "etc.", "bzw.", "bzgl.", "ca.", "ggf.", "vgl.", "inkl.", "exkl.", "max.", "min.",
        "nr.", "stk.", "mio.", "mrd.", "tel.", "abs.", "art.", "str.", "geb.", "gest.", "verh.", "evtl.",
        "sog.", "bspw.", "einschl.", "zzgl.", "abzgl.", "dr.", "prof.", "ing.", "zit.", "s.",
        "z.b.", "d.h.", "u.a.", "o.ä.", "u.ä.", "i.d.r.", "z.t.", "u.u.", "u.v.m.", "m.a.w.", "n.chr.", "v.chr."
    )
    
    /**
     * @param token a whitespace-free token ending in a period (e.g. `usw.`, `1.`, `Nr.`)
     * @param abbreviations the active language's own abbreviation set (lower-cased, period included);
     *        defaults to [GERMAN] for callers with no language context of their own
     * @return true when the token is a known abbreviation
     */
    fun isAbbreviation(token: String, abbreviations: Set<String> = GERMAN): Boolean {
        return token.lowercase() in abbreviations
    }
    
    /**
     * @param token a whitespace-free token ending in a period
     * @return true when the token is an ordinal / list enumerator such as `1.` or `10.`
     */
    fun isEnumerator(token: String): Boolean {
        return ENUMERATOR.matches(token)
    }
    
    /**
     * @param token a whitespace-free token ending in a period
     * @param abbreviations the active language's own abbreviation set; see [isAbbreviation]
     * @return true when the trailing period does not terminate a sentence
     */
    fun isNonTerminalPeriod(token: String, abbreviations: Set<String> = GERMAN): Boolean {
        return isAbbreviation(token, abbreviations) || isEnumerator(token)
    }
    
    /**
     * Parses a language pack's optional `abbreviations.tsv` content (D-434): one lower-cased abbreviation
     * per line, period included (e.g. `usw.`); blank lines and lines starting with `#` are ignored. Mirrors
     * [de.froehlichmedia.adaptkey.settings.LetterHints.parse]'s role for `hints.tsv` - a pure, tolerant
     * parser with no hard failure mode, since a malformed line simply never matches anything at commit time.
     *
     * @param raw the file's raw text content
     * @return the parsed, lower-cased abbreviation set (possibly empty)
     */
    fun parse(raw: String): Set<String> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.lowercase() }
            .toSet()
    }
}

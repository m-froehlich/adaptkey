// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * Recognises tokens whose trailing period is NOT a sentence terminator (§6): common German
 * abbreviations that always end in a period, and ordinal / list enumerators such as `1.`.
 *
 * The abbreviation set is a curated built-in list (lower-cased, period included). Multi-word
 * abbreviations written with an internal space (e.g. `z. B.`) are only recognised in their
 * space-free form (`z.B.`); the spaced form and user-learned abbreviations are a later extension.
 */
object Abbreviations {
    
    private val ENUMERATOR = Regex("^\\d+\\.$")
    
    /** Common German abbreviations that always carry a trailing period (lower-cased). */
    val GERMAN: Set<String> = setOf(
        "usw.", "usf.", "etc.", "bzw.", "ca.", "ggf.", "vgl.", "inkl.", "exkl.", "max.", "min.",
        "nr.", "mio.", "mrd.", "tel.", "abs.", "art.", "str.", "geb.", "gest.", "verh.", "evtl.",
        "sog.", "bspw.", "einschl.", "zzgl.", "abzgl.", "dr.", "prof.", "ing.", "zit.", "s.",
        "z.b.", "d.h.", "u.a.", "o.ä.", "u.ä.", "i.d.r.", "z.t.", "u.u.", "u.v.m.", "m.a.w.", "n.chr.", "v.chr."
    )
    
    /**
     * @param token a whitespace-free token ending in a period (e.g. `usw.`, `1.`, `Nr.`)
     * @return true when the token is a known abbreviation
     */
    fun isAbbreviation(token: String): Boolean {
        return token.lowercase() in GERMAN
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
     * @return true when the trailing period does not terminate a sentence
     */
    fun isNonTerminalPeriod(token: String): Boolean {
        return isAbbreviation(token) || isEnumerator(token)
    }
}

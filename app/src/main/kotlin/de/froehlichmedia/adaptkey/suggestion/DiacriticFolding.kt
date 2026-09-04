// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-435: the seam between language-agnostic suggestion/repair logic and a language's own diacritic
 * conventions - mirrors [de.froehlichmedia.adaptkey.language.LanguageRules]' existing D-410 shape ("delegate
 * to the active language, default to doing nothing at all") for exactly the same reason: before this
 * interface existed, [DictionarySuggestionProvider]/[TokenRepair] called [Umlaut] directly and
 * unconditionally, so a non-German active language's own diacritics (if any) were silently never folded or
 * restored - not a feature merely missing for other languages, a genuine cross-language bug, since the class
 * doing the folding had no notion of "active language" at all.
 *
 * [Umlaut] is the only real implementation today (German's own ä/ö/ü/ß handling, including its D-204 dual
 * ASCII-convention special case); [NoOpDiacriticFolding] is the default for every language without one -
 * deliberately never [Umlaut] itself, since German's own map buys a language like Turkish or Polish nothing.
 */
interface DiacriticFolding {
    
    /**
     * Folds every diacritic character in [text] to its plain ASCII base, so a token typed with the diacritic
     * omitted matches under an edit-distance/prefix comparison against an already-folded counterpart.
     *
     * @param text the text to fold
     * @return the folded text
     */
    fun fold(text: String): String
    
    /**
     * The reverse of [fold]: every plausible spelling [text] could have had before folding, so a prefix
     * typed entirely without diacritics still reaches a dictionary prefix search indexed on the real,
     * unfolded spelling. [text] itself is always included (the common case: nothing to unfold).
     *
     * @param text the lower-cased text to generate unfold candidates for
     * @return the candidates, [text] itself always included, each one otherwise unique
     */
    fun unfoldCandidates(text: String): List<String>
    
    /**
     * Every distinct fold [text] plausibly came from - normally just [fold]'s own result, but a language may
     * have more than one legitimate ASCII substitution for the same diacritic (German's ß → "ss" formally,
     * or → "s" via this app's own long-press-alternative typing convention, D-204).
     *
     * @param text the text to fold
     * @return the distinct fold variants of [text], [fold]'s own result always included
     */
    fun foldVariants(text: String): List<String>
    
    /**
     * The known diacritic variants of the single base character [char] (e.g. `'o'` -> `{'ö'}` for German) -
     * generalises the first-character bucket broadening [DictionarySuggestionProvider.candidateFirstChars]
     * used to hardcode directly.
     *
     * @param char a single lower-case base character
     * @return the diacritic variants of [char] known to this language, or empty when it has none
     */
    fun variantsOf(char: Char): Set<Char>
}

/** D-435: the default - no language-specific diacritic handling applies. */
object NoOpDiacriticFolding : DiacriticFolding {
    override fun fold(text: String): String = text
    override fun unfoldCandidates(text: String): List<String> = listOf(text)
    override fun foldVariants(text: String): List<String> = listOf(text)
    override fun variantsOf(char: Char): Set<Char> = emptySet()
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

/**
 * Pure parser for the bundled `language_profiles.tsv` asset (A-03): one `<code>\t<ngram>` pair per
 * line, the lines for a language given in rank order (rank 0 first).
 *
 * The n-gram is everything after the first tab and is taken verbatim - a trailing space is
 * significant (e.g. the boundary n-gram `"er "`) and must not be trimmed. A stray carriage return is
 * tolerated. Blank lines and lines whose code names no known language are skipped, so a malformed
 * line loses only its own entry rather than the whole profile.
 */
object LanguageProfileParser {
    
    /**
     * Parses the raw asset text into one profile per language present.
     *
     * @param raw the asset content
     * @return a map from language to its [CharNgramProfile]; languages with no lines are absent
     */
    fun parse(raw: String): Map<Language, CharNgramProfile> {
        val ordered = LinkedHashMap<Language, MutableList<String>>()
        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            val tab = line.indexOf('\t')
            if (tab <= 0 || tab == line.length - 1) {
                return@forEach
            }
            val language = Language.fromCode(line.substring(0, tab)) ?: return@forEach
            ordered.getOrPut(language) { ArrayList() }.add(line.substring(tab + 1))
        }
        return ordered.mapValues { (language, ngrams) -> CharNgramProfile(language, ngrams) }
    }
}

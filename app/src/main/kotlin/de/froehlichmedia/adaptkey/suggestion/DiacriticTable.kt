// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-436: pure parser for a language pack's optional `diacritics.tsv` (see [DataDiacriticFolding]) - a
 * language's own base-letter-to-diacritic-variant table, the data half of D-387's generalisation of
 * [Umlaut]'s own mechanism beyond German.
 *
 * Format: one base letter per line, `baseLetter<TAB>variant1,variant2,...` (e.g. `e	é,è,ê,ë` for French,
 * `g	ğ` for Turkish) - only the *base -> known variants* direction is ever needed (see
 * [DataDiacriticFolding]'s own KDoc for why); blank lines and `#` comment lines are ignored. Deliberately
 * tolerant, like [Abbreviations.parse]/[de.froehlichmedia.adaptkey.settings.LetterHints.parse]: a malformed
 * line is simply skipped, never a hard failure.
 */
object DiacriticTable {
    
    /**
     * @param raw the file's raw text content
     * @return the parsed base-letter -> variants map (possibly empty); each base letter is lower-cased,
     *         each variant list contains only single, non-blank characters distinct from their own base
     */
    fun parse(raw: String): Map<Char, List<Char>> {
        val result = LinkedHashMap<Char, List<Char>>()
        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            val parts = trimmed.split('\t', limit = 2)
            if (parts.size != 2) {
                continue
            }
            val baseRaw = parts[0]
            if (baseRaw.length != 1 || !baseRaw[0].isLetter()) {
                continue
            }
            val base = baseRaw[0].lowercaseChar()
            val variants = parts[1].split(',')
                .map { it.trim() }
                .filter { it.length == 1 && it[0] != base }
                .map { it[0] }
                .distinct()
            if (variants.isNotEmpty()) {
                result[base] = variants
            }
        }
        return result
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

/**
 * Pure parser for the bundled emoji-keyword search asset (L-03, D-317): one
 * `<emoji>\t<term1>|<term2>|...` line per emoji. Blank lines and a line with no separator are
 * skipped, so a malformed line never crashes the panel - it just has no search terms.
 */
object EmojiKeywordParser {
    
    private const val FIELD_SEPARATOR = '\t'
    private const val TERM_SEPARATOR = '|'
    
    /**
     * @param raw the asset file content, one `<emoji>\t<term1>|<term2>|...` pair per line
     * @return a search index over every parsed emoji, in file order
     */
    fun parse(raw: String): EmojiKeywordIndex {
        val termsByEmoji = LinkedHashMap<String, List<String>>()
        raw.lineSequence().forEach { line -> parseLine(line, termsByEmoji) }
        return EmojiKeywordIndex(termsByEmoji)
    }
    
    private fun parseLine(line: String, termsByEmoji: MutableMap<String, List<String>>) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return
        }
        val separatorIndex = trimmed.indexOf(FIELD_SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == trimmed.length - 1) {
            return
        }
        val emoji = trimmed.substring(0, separatorIndex)
        val terms = trimmed.substring(separatorIndex + 1).split(TERM_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        if (terms.isNotEmpty()) {
            termsByEmoji[emoji] = terms
        }
    }
}

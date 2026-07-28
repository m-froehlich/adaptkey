// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

/**
 * Search index over the bundled CLDR-derived emoji keywords (L-03, D-317). German and English terms
 * are merged into one flat, lowercase list per emoji - search does not distinguish which language a
 * term came from, so an English colloquialism (e.g. "poop") and a German short name (e.g. "kothaufen")
 * both find the same emoji.
 *
 * @property termsByEmoji every bundled emoji's own search terms, in the dataset's own order
 */
data class EmojiKeywordIndex(private val termsByEmoji: Map<String, List<String>>) {
    
    /**
     * @param query the user's search text; matched case-insensitively as a substring against every
     *        bundled term
     * @return every matching emoji, in bundled dataset order; empty when [query] is blank
     */
    fun search(query: String): List<String> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) {
            return emptyList()
        }
        return termsByEmoji.filterValues { terms -> terms.any { it.contains(needle) } }.keys.toList()
    }
    
    companion object {
        
        /** An empty index, used when the bundled asset is missing or unparseable. */
        val EMPTY = EmojiKeywordIndex(emptyMap())
    }
}

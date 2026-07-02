// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

/**
 * The bundled emoji dataset (L-03), grouped by [EmojiCategory] in curation order.
 *
 * @property byCategory every [EmojiCategory] mapped to its emoji codepoints; a category with no
 *           entries maps to an empty list rather than being absent
 */
data class EmojiDataset(val byCategory: Map<EmojiCategory, List<String>>) {
    
    /**
     * @return true when every category is empty
     */
    fun isEmpty(): Boolean = byCategory.values.all { it.isEmpty() }
    
    companion object {
        
        /** An empty dataset, used when the bundled asset is missing or unparseable. */
        val EMPTY = EmojiDataset(emptyMap())
    }
}

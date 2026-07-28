// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

import android.content.Context

/**
 * Loads the bundled emoji-keyword search asset (L-03, D-317) and hands it to the pure
 * [EmojiKeywordParser]. Android-only glue; left to instrumented tests.
 */
object EmojiKeywordLoader {
    
    private const val ASSET_NAME = "emoji_keywords.tsv"
    
    /**
     * @param context any valid context (the input method service)
     * @return the parsed search index, or [EmojiKeywordIndex.EMPTY] when the asset is missing or unreadable
     */
    fun load(context: Context): EmojiKeywordIndex {
        return runCatching {
            val raw = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            EmojiKeywordParser.parse(raw)
        }.getOrDefault(EmojiKeywordIndex.EMPTY)
    }
}

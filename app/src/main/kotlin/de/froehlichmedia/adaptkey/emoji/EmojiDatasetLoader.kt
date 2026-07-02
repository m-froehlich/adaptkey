// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

import android.content.Context

/**
 * Loads the bundled emoji dataset asset (L-03) and hands it to the pure [EmojiDatasetParser].
 * Android-only glue; left to instrumented tests.
 */
object EmojiDatasetLoader {
    
    private const val ASSET_NAME = "emoji_dataset.tsv"
    
    /**
     * @param context any valid context (the input method service)
     * @return the parsed dataset, or [EmojiDataset.EMPTY] when the asset is missing or unreadable
     */
    fun load(context: Context): EmojiDataset {
        return runCatching {
            val raw = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            EmojiDatasetParser.parse(raw)
        }.getOrDefault(EmojiDataset.EMPTY)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import android.content.Context
import de.froehlichmedia.adaptkey.prediction.Tier3ModelFiles

/**
 * Thin Android loader that builds the tier-3 [BpeTokenizer] from the tokenizer files bundled in the
 * APK assets (§9).
 *
 * The tokenizer (`vocab.json` + `merges.txt`) is shipped in `assets/tier3/`, so it is always available
 * — only the model weights are user-provided. All this does is read the two asset texts and delegate to
 * the pure, unit-tested [Tier3TokenizerParser]; like the other asset loaders it is covered by
 * instrumented rather than unit tests.
 */
object Tier3TokenizerLoader {
    
    /**
     * @param context any valid context
     * @return the tokenizer built from the bundled assets
     */
    fun load(context: Context): BpeTokenizer {
        val vocab = readAsset(context, "${Tier3ModelFiles.ASSET_DIR}/${Tier3ModelFiles.VOCAB_FILE}")
        val merges = readAsset(context, "${Tier3ModelFiles.ASSET_DIR}/${Tier3ModelFiles.MERGES_FILE}")
        return Tier3TokenizerParser.parse(vocab, merges)
    }
    
    private fun readAsset(context: Context, path: String): String {
        return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

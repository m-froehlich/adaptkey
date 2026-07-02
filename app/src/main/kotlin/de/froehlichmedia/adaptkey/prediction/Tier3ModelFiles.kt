// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import java.io.File

/**
 * The on-device model layout the tier-3 ONNX backend expects, and detection of whether it is present.
 *
 * The mini-LLM **weights** ([MODEL_FILE], hundreds of MB) are far too large to ship in the APK, so they
 * are provided by the user into the app's private storage (the app has no internet permission; the
 * import is browser-download + a system file picker). The much smaller **tokenizer** files
 * ([VOCAB_FILE] / [MERGES_FILE], ~1.3 MB) *are* bundled in the APK assets, so the only file the user
 * must supply is the single big model graph. When it is absent the backend reports itself unavailable
 * and the keyboard runs tier-1-only.
 *
 * Kept over [File] so the presence check is unit-testable on the JVM with a temporary directory.
 */
object Tier3ModelFiles {
    
    /** The quantised ONNX model graph — the only user-provided file, in the private model directory. */
    const val MODEL_FILE = "model.onnx"
    
    /** The bundled byte-level BPE vocabulary asset base name (under [ASSET_DIR]). */
    const val VOCAB_FILE = "vocab.json"
    
    /** The bundled byte-level BPE merges asset base name (under [ASSET_DIR]). */
    const val MERGES_FILE = "merges.txt"
    
    /** The assets sub-directory holding the bundled tokenizer files. */
    const val ASSET_DIR = "tier3"
    
    /**
     * @param modelDir the private model directory
     * @return true when the user-provided model graph is present, so the backend can be loaded
     */
    fun isComplete(modelDir: File): Boolean {
        return File(modelDir, MODEL_FILE).isFile
    }
}

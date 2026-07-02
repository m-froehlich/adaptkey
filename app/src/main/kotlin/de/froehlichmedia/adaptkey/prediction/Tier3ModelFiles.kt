package de.froehlichmedia.adaptkey.prediction

import java.io.File

/**
 * The on-device model layout the tier-3 ONNX backend expects, and detection of whether it is present.
 *
 * The mini-LLM model is **not** shipped in the APK (it is far too large); it is provided into the app's
 * storage separately (the import UX is a later, deliberate step). A canonical, model-agnostic file
 * layout decouples the app from any particular download: whatever model is imported is placed under a
 * directory with these fixed names, so swapping models needs no code change. When the directory is
 * incomplete the backend simply reports itself unavailable and the keyboard runs tier-1-only.
 *
 * Kept over [File] so the presence check is unit-testable on the JVM with a temporary directory.
 */
object Tier3ModelFiles {
    
    /** The quantised ONNX model graph. */
    const val MODEL_FILE = "model.onnx"
    
    /** The byte-level BPE vocabulary (token→id). */
    const val VOCAB_FILE = "vocab.json"
    
    /** The byte-level BPE merges, in rank order. */
    const val MERGES_FILE = "merges.txt"
    
    /** Every file that must be present for the backend to load. */
    val REQUIRED = listOf(MODEL_FILE, VOCAB_FILE, MERGES_FILE)
    
    /**
     * @param dir the candidate model directory
     * @return the required file names missing from [dir]; all of them when [dir] is not a directory
     */
    fun missingFiles(dir: File): List<String> {
        if (!dir.isDirectory) {
            return REQUIRED
        }
        return REQUIRED.filter { !File(dir, it).isFile }
    }
    
    /**
     * @param dir the candidate model directory
     * @return true when every required file is present, so the backend can be loaded
     */
    fun isComplete(dir: File): Boolean {
        return missingFiles(dir).isEmpty()
    }
}

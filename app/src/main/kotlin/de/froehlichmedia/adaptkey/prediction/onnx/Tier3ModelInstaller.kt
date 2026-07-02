// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import de.froehlichmedia.adaptkey.prediction.Tier3ModelFiles
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Installs (and removes) the user-provided tier-3 model graph in the app's private model directory (§9).
 *
 * The Android layer opens an [InputStream] from the file the user picked (a system file picker / SAF Uri,
 * so no storage permission is needed) and hands it here. The copy goes to a temporary `.part` file first
 * and is only renamed into place on success, so an interrupted import can never leave a half-written file
 * that looks complete to [Tier3ModelFiles.isComplete]. Kept over [File] / [InputStream] so it is
 * unit-testable on the JVM.
 */
object Tier3ModelInstaller {
    
    private const val TEMP_SUFFIX = ".part"
    
    /**
     * Copies [source] into [modelDir] as the model graph, atomically via a temporary file.
     *
     * @param source the model bytes (closed by the caller)
     * @param modelDir the private model directory (created if absent)
     * @return the number of bytes written
     * @throws IOException when the directory cannot be created or the temporary file cannot be renamed
     */
    fun install(source: InputStream, modelDir: File): Long {
        if (!modelDir.isDirectory && !modelDir.mkdirs()) {
            throw IOException("cannot create model directory: $modelDir")
        }
        val temp = File(modelDir, Tier3ModelFiles.MODEL_FILE + TEMP_SUFFIX)
        val bytes = temp.outputStream().use { out -> source.copyTo(out) }
        val target = File(modelDir, Tier3ModelFiles.MODEL_FILE)
        target.delete()
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("cannot move imported model into place: $target")
        }
        return bytes
    }
    
    /**
     * Removes the installed model graph (and any leftover temporary file), disabling the backend.
     *
     * @param modelDir the private model directory
     * @return true when a model graph was present and deleted
     */
    fun clear(modelDir: File): Boolean {
        File(modelDir, Tier3ModelFiles.MODEL_FILE + TEMP_SUFFIX).delete()
        return File(modelDir, Tier3ModelFiles.MODEL_FILE).delete()
    }
}

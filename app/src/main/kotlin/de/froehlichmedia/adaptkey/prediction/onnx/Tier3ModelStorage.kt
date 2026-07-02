// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import android.content.Context
import de.froehlichmedia.adaptkey.prediction.Tier3ModelFiles
import java.io.File

/**
 * Resolves the app-private directory that holds the user-provided tier-3 model graph (§9).
 *
 * The directory lives under the app's internal `filesDir`, so it is private to the app, needs no storage
 * permission, and is removed on uninstall. It is the single source of truth shared by the import screen
 * (writes the model here) and the backend (reads it), so the location can never drift between them.
 */
object Tier3ModelStorage {
    
    private const val DIR_NAME = "tier3-model"
    
    /**
     * @param context any valid context
     * @return the private model directory (not guaranteed to exist yet)
     */
    fun modelDir(context: Context): File {
        return File(context.filesDir, DIR_NAME)
    }
    
    /**
     * @param context any valid context
     * @return true when the user-provided model graph is present
     */
    fun isModelInstalled(context: Context): Boolean {
        return Tier3ModelFiles.isComplete(modelDir(context))
    }
}

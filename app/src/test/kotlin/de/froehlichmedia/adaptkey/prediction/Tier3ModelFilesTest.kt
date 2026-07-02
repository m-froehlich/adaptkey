// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Unit tests for the tier-3 model-file presence detection.
 */
class Tier3ModelFilesTest {
    
    @Test
    fun `a non-directory is incomplete`(@TempDir dir: File) {
        assertFalse(Tier3ModelFiles.isComplete(File(dir, "does-not-exist")))
    }
    
    @Test
    fun `an empty directory is incomplete`(@TempDir dir: File) {
        assertFalse(Tier3ModelFiles.isComplete(dir))
    }
    
    @Test
    fun `a directory holding the model graph is complete`(@TempDir dir: File) {
        File(dir, Tier3ModelFiles.MODEL_FILE).writeText("x")
        assertTrue(Tier3ModelFiles.isComplete(dir))
    }
}

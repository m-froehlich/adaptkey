package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `a non-directory reports every required file missing`(@TempDir dir: File) {
        val absent = File(dir, "does-not-exist")
        assertEquals(Tier3ModelFiles.REQUIRED, Tier3ModelFiles.missingFiles(absent))
        assertFalse(Tier3ModelFiles.isComplete(absent))
    }
    
    @Test
    fun `an empty directory is incomplete`(@TempDir dir: File) {
        assertEquals(Tier3ModelFiles.REQUIRED, Tier3ModelFiles.missingFiles(dir))
        assertFalse(Tier3ModelFiles.isComplete(dir))
    }
    
    @Test
    fun `a directory missing one file lists exactly that file`(@TempDir dir: File) {
        File(dir, Tier3ModelFiles.MODEL_FILE).writeText("x")
        File(dir, Tier3ModelFiles.TOKENIZER_FILE).writeText("x")
        assertEquals(listOf(Tier3ModelFiles.CONFIG_FILE), Tier3ModelFiles.missingFiles(dir))
        assertFalse(Tier3ModelFiles.isComplete(dir))
    }
    
    @Test
    fun `a directory with every required file is complete`(@TempDir dir: File) {
        for (name in Tier3ModelFiles.REQUIRED) {
            File(dir, name).writeText("x")
        }
        assertTrue(Tier3ModelFiles.missingFiles(dir).isEmpty())
        assertTrue(Tier3ModelFiles.isComplete(dir))
    }
}

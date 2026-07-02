package de.froehlichmedia.adaptkey.prediction.onnx

import de.froehlichmedia.adaptkey.prediction.Tier3ModelFiles
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Unit tests for the tier-3 model installer.
 */
class Tier3ModelInstallerTest {
    
    @Test
    fun `install writes the model and reports it complete`(@TempDir dir: File) {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val bytes = Tier3ModelInstaller.install(ByteArrayInputStream(data), dir)
        
        assertEquals(5L, bytes)
        assertTrue(Tier3ModelFiles.isComplete(dir))
        assertArrayEquals(data, File(dir, Tier3ModelFiles.MODEL_FILE).readBytes())
    }
    
    @Test
    fun `install creates the directory when absent`(@TempDir parent: File) {
        val dir = File(parent, "tier3-model")
        Tier3ModelInstaller.install(ByteArrayInputStream(byteArrayOf(9)), dir)
        assertTrue(Tier3ModelFiles.isComplete(dir))
    }
    
    @Test
    fun `install replaces an existing model`(@TempDir dir: File) {
        Tier3ModelInstaller.install(ByteArrayInputStream(byteArrayOf(1, 1, 1)), dir)
        Tier3ModelInstaller.install(ByteArrayInputStream(byteArrayOf(7, 7)), dir)
        assertArrayEquals(byteArrayOf(7, 7), File(dir, Tier3ModelFiles.MODEL_FILE).readBytes())
    }
    
    @Test
    fun `install leaves no temporary part file behind`(@TempDir dir: File) {
        Tier3ModelInstaller.install(ByteArrayInputStream(byteArrayOf(1)), dir)
        assertFalse(File(dir, Tier3ModelFiles.MODEL_FILE + ".part").exists())
    }
    
    @Test
    fun `clear removes the installed model`(@TempDir dir: File) {
        Tier3ModelInstaller.install(ByteArrayInputStream(byteArrayOf(1)), dir)
        assertTrue(Tier3ModelInstaller.clear(dir))
        assertFalse(Tier3ModelFiles.isComplete(dir))
    }
    
    @Test
    fun `clear on an empty directory reports nothing removed`(@TempDir dir: File) {
        assertFalse(Tier3ModelInstaller.clear(dir))
    }
}

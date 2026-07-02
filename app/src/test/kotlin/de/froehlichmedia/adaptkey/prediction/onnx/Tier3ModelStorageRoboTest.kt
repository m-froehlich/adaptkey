// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Robolectric test (JVM, no emulator) for the model storage + install lifecycle against a real
 * app-private [android.content.Context.getFilesDir], exercising the wiring the pure installer test
 * cannot: the actual private directory resolution and the presence gate the backend keys off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier3ModelStorageRoboTest {
    
    @Test
    fun installThenClearFlipsInstalledState() {
        val context = RuntimeEnvironment.getApplication()
        val dir = Tier3ModelStorage.modelDir(context)
        
        assertFalse(Tier3ModelStorage.isModelInstalled(context))
        
        val data = byteArrayOf(4, 2, 4, 2)
        Tier3ModelInstaller.install(ByteArrayInputStream(data), dir)
        
        assertTrue(Tier3ModelStorage.isModelInstalled(context))
        assertArrayEquals(data, File(dir, "model.onnx").readBytes())
        
        assertTrue(Tier3ModelInstaller.clear(dir))
        assertFalse(Tier3ModelStorage.isModelInstalled(context))
    }
}

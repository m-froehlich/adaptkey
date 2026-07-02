// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (runs on the JVM, no emulator): verifies the tier-3 tokenizer bundled in the APK
 * assets loads and matches the reference SmolLM2 tokenization end-to-end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier3TokenizerLoaderRoboTest {
    
    @Test
    fun bundledAssetsBuildAReferenceMatchingTokenizer() {
        val context = RuntimeEnvironment.getApplication()
        val tokenizer = Tier3TokenizerLoader.load(context)
        
        // Golden ids from the reference Hugging Face tokenizer (see BpeTokenizerParityTest).
        assertArrayEquals(intArrayOf(19556, 905), tokenizer.encode("Hello world"))
        assertEquals("Hello world", tokenizer.decode(intArrayOf(19556, 905)))
        assertEquals(2, tokenizer.specialTokenId("<|im_end|>"))
    }
}

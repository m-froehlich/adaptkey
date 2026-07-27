// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Robolectric test (JVM, real filesystem) for [LanguagePackStorage] - in particular D-310's per-language
 * subfolder layout and the [LanguagePackStorage.cleanupLegacyFlatFiles] sweep for the pre-D-310 flat one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LanguagePackStorageTest {
    
    private fun zipOf(vararg entries: Pair<String, String>): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }
    
    @Test
    fun `each file path lives under the language's own subfolder`() {
        val context = RuntimeEnvironment.getApplication()
        val packDir = LanguagePackStorage.packDir(context)
        
        assertEquals(File(packDir, "de/dict.tsv"), LanguagePackStorage.wordsFile(context, Language.GERMAN))
        assertEquals(File(packDir, "de/bigram.tsv"), LanguagePackStorage.bigramsFile(context, Language.GERMAN))
        assertEquals(File(packDir, "de/hints.tsv"), LanguagePackStorage.hintsFile(context, Language.GERMAN))
    }
    
    @Test
    fun `isInstalled and read functions work off the newly written subfolder layout`() {
        val context = RuntimeEnvironment.getApplication()
        LanguagePackInstaller.install(
            zipOf("dict.tsv" to "der\t100\n", "bigram.tsv" to "a\tb\t1\n", "hints.tsv" to "a=ä"),
            LanguagePackStorage.packDir(context),
            Language.GERMAN
        )
        
        assertTrue(LanguagePackStorage.isInstalled(context, Language.GERMAN))
        assertEquals("der\t100\n", LanguagePackStorage.readWords(context, Language.GERMAN))
        assertEquals("a\tb\t1\n", LanguagePackStorage.readBigrams(context, Language.GERMAN))
        assertEquals("a=ä", LanguagePackStorage.readHints(context, Language.GERMAN))
    }
    
    @Test
    fun `remove deletes the language's own subfolder too, not just the files inside it`() {
        val context = RuntimeEnvironment.getApplication()
        LanguagePackInstaller.install(zipOf("dict.tsv" to "der\t100\n"), LanguagePackStorage.packDir(context), Language.GERMAN)
        
        LanguagePackStorage.remove(context, Language.GERMAN)
        
        assertFalse(LanguagePackStorage.isInstalled(context, Language.GERMAN))
        assertFalse(LanguagePackStorage.languageDir(context, Language.GERMAN).exists())
    }
    
    @Test
    fun `cleanupLegacyFlatFiles removes a pre-D-310 flat file sitting directly in packDir`() {
        val context = RuntimeEnvironment.getApplication()
        val packDir = LanguagePackStorage.packDir(context)
        packDir.mkdirs()
        val legacyDict = File(packDir, "dict_de.tsv")
        legacyDict.writeText("der\t100\n")
        
        LanguagePackStorage.cleanupLegacyFlatFiles(context)
        
        assertFalse(legacyDict.exists())
    }
    
    @Test
    fun `cleanupLegacyFlatFiles never touches the new per-language subfolder layout`() {
        val context = RuntimeEnvironment.getApplication()
        LanguagePackInstaller.install(zipOf("dict.tsv" to "der\t100\n"), LanguagePackStorage.packDir(context), Language.GERMAN)
        
        LanguagePackStorage.cleanupLegacyFlatFiles(context)
        
        assertTrue(LanguagePackStorage.isInstalled(context, Language.GERMAN))
    }
    
    @Test
    fun `cleanupLegacyFlatFiles is harmless when there is nothing to clean up`() {
        val context = RuntimeEnvironment.getApplication()
        LanguagePackStorage.cleanupLegacyFlatFiles(context)
    }
}

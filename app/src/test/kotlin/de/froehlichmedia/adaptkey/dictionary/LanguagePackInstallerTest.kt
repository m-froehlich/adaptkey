// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Unit tests for the D-280 language-pack installer. */
class LanguagePackInstallerTest {
    
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
    fun `install writes both files from a well-formed archive`(@TempDir dir: File) {
        val archive = zipOf("dict_fr.tsv" to "bonjour\t100\n", "bigram_fr.tsv" to "bonjour\tle monde\t5\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertEquals("bonjour\t100\n", File(dir, "dict_fr.tsv").readText())
        assertEquals("bonjour\tle monde\t5\n", File(dir, "bigram_fr.tsv").readText())
    }
    
    @Test
    fun `install accepts a words-only archive, bigrams stay absent`(@TempDir dir: File) {
        val archive = zipOf("dict_fr.tsv" to "bonjour\t100\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "dict_fr.tsv").isFile)
        assertFalse(File(dir, "bigram_fr.tsv").exists())
    }
    
    @Test
    fun `install rejects an archive missing the words entry`(@TempDir dir: File) {
        val archive = zipOf("bigram_fr.tsv" to "bonjour\tle monde\t5\n")
        
        assertThrows(IOException::class.java) { LanguagePackInstaller.install(archive, dir, Language.FRENCH) }
    }
    
    @Test
    fun `install ignores unrelated entries and entries for a different language`(@TempDir dir: File) {
        val archive = zipOf(
            "dict_fr.tsv" to "bonjour\t100\n",
            "dict_es.tsv" to "hola\t100\n",
            "README.md" to "not a dictionary"
        )
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "dict_fr.tsv").isFile)
        assertFalse(File(dir, "dict_es.tsv").exists())
        assertFalse(File(dir, "README.md").exists())
    }
    
    @Test
    fun `install creates the directory when absent`(@TempDir parent: File) {
        val dir = File(parent, "language_packs")
        LanguagePackInstaller.install(zipOf("dict_fr.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        assertTrue(File(dir, "dict_fr.tsv").isFile)
    }
    
    @Test
    fun `install replaces an existing pack`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf("dict_fr.tsv" to "old\t1\n"), dir, Language.FRENCH)
        LanguagePackInstaller.install(zipOf("dict_fr.tsv" to "new\t2\n"), dir, Language.FRENCH)
        assertEquals("new\t2\n", File(dir, "dict_fr.tsv").readText())
    }
    
    @Test
    fun `install leaves no temporary part file behind`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf("dict_fr.tsv" to "bonjour\t100\n", "bigram_fr.tsv" to "a\tb\t1\n"), dir, Language.FRENCH)
        assertFalse(File(dir, "dict_fr.tsv.part").exists())
        assertFalse(File(dir, "bigram_fr.tsv.part").exists())
    }
    
    @Test
    fun `clear removes an installed pack and reports true`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf("dict_fr.tsv" to "bonjour\t100\n", "bigram_fr.tsv" to "a\tb\t1\n"), dir, Language.FRENCH)
        
        assertTrue(LanguagePackInstaller.clear(dir, Language.FRENCH))
        
        assertFalse(File(dir, "dict_fr.tsv").exists())
        assertFalse(File(dir, "bigram_fr.tsv").exists())
    }
    
    @Test
    fun `clear on an absent pack reports nothing removed`(@TempDir dir: File) {
        assertFalse(LanguagePackInstaller.clear(dir, Language.FRENCH))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
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
    fun `install accepts a words-only archive, bigrams and hints stay absent`(@TempDir dir: File) {
        val archive = zipOf("dict_fr.tsv" to "bonjour\t100\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "dict_fr.tsv").isFile)
        assertFalse(File(dir, "bigram_fr.tsv").exists())
        assertFalse(File(dir, "hints_fr.tsv").exists())
    }
    
    @Test
    fun `install writes an optional hints file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf("dict_de.tsv" to "der\t100\n", "hints_de.tsv" to "a=ä;s=ß")
        
        LanguagePackInstaller.install(archive, dir, Language.GERMAN)
        
        assertEquals("a=ä;s=ß", File(dir, "hints_de.tsv").readText())
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
        LanguagePackInstaller.install(
            zipOf("dict_fr.tsv" to "bonjour\t100\n", "bigram_fr.tsv" to "a\tb\t1\n", "hints_fr.tsv" to "a=à"),
            dir,
            Language.FRENCH
        )
        assertFalse(File(dir, "dict_fr.tsv.part").exists())
        assertFalse(File(dir, "bigram_fr.tsv.part").exists())
        assertFalse(File(dir, "hints_fr.tsv.part").exists())
    }
    
    @Test
    fun `clear removes an installed pack including hints and reports true`(@TempDir dir: File) {
        LanguagePackInstaller.install(
            zipOf("dict_fr.tsv" to "bonjour\t100\n", "bigram_fr.tsv" to "a\tb\t1\n", "hints_fr.tsv" to "a=à"),
            dir,
            Language.FRENCH
        )
        
        assertTrue(LanguagePackInstaller.clear(dir, Language.FRENCH))
        
        assertFalse(File(dir, "dict_fr.tsv").exists())
        assertFalse(File(dir, "bigram_fr.tsv").exists())
        assertFalse(File(dir, "hints_fr.tsv").exists())
    }
    
    @Test
    fun `clear on an absent pack reports nothing removed`(@TempDir dir: File) {
        assertFalse(LanguagePackInstaller.clear(dir, Language.FRENCH))
    }
    
    @Test
    fun `D-308 install returns the archive's own version`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf("dict_fr.tsv" to "bonjour\t100\n", "version_fr.txt" to "3"),
            dir,
            Language.FRENCH
        )
        assertEquals(3, version)
    }
    
    @Test
    fun `D-308 install falls back to DEFAULT_VERSION when the archive has no version file`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(zipOf("dict_fr.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-308 install falls back to DEFAULT_VERSION when the version file is unparseable`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf("dict_fr.tsv" to "bonjour\t100\n", "version_fr.txt" to "not-a-number"),
            dir,
            Language.FRENCH
        )
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-308 a different language's version file is ignored`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf("dict_fr.tsv" to "bonjour\t100\n", "version_es.txt" to "9"),
            dir,
            Language.FRENCH
        )
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-308 parse reads the archive without writing anything to disk`(@TempDir dir: File) {
        val pack = LanguagePackInstaller.parse(zipOf("dict_fr.tsv" to "bonjour\t100\n", "version_fr.txt" to "5"), Language.FRENCH)
        
        assertEquals("bonjour\t100\n", String(pack.words, Charsets.UTF_8))
        assertEquals(5, pack.version)
        assertFalse(File(dir, "dict_fr.tsv").exists())
    }
    
    @Test
    fun `D-308 write applies an already-parsed pack to disk`(@TempDir dir: File) {
        val pack = LanguagePackInstaller.parse(zipOf("dict_fr.tsv" to "bonjour\t100\n", "bigram_fr.tsv" to "a\tb\t1\n"), Language.FRENCH)
        
        LanguagePackInstaller.write(dir, pack)
        
        assertEquals("bonjour\t100\n", File(dir, "dict_fr.tsv").readText())
        assertEquals("a\tb\t1\n", File(dir, "bigram_fr.tsv").readText())
    }
}

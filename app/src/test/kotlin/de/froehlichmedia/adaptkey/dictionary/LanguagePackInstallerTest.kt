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

/**
 * Unit tests for the D-280 language-pack installer. D-310: every archive entry is a fixed name
 * (`dict.tsv`/`bigram.tsv`/`hints.tsv`/`version.txt`, no `<code>` suffix), and [LanguagePackInstaller.write]
 * extracts into a per-language subfolder (`<dir>/<code>/`) rather than directly into [dir].
 */
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
    fun `install writes both files from a well-formed archive into the language's own subfolder`(@TempDir dir: File) {
        val archive = zipOf("dict.tsv" to "bonjour\t100\n", "bigram.tsv" to "bonjour\tle monde\t5\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertEquals("bonjour\t100\n", File(dir, "fr/dict.tsv").readText())
        assertEquals("bonjour\tle monde\t5\n", File(dir, "fr/bigram.tsv").readText())
    }
    
    @Test
    fun `install accepts a words-only archive, bigrams and hints stay absent`(@TempDir dir: File) {
        val archive = zipOf("dict.tsv" to "bonjour\t100\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "fr/dict.tsv").isFile)
        assertFalse(File(dir, "fr/bigram.tsv").exists())
        assertFalse(File(dir, "fr/hints.tsv").exists())
        assertFalse(File(dir, "fr/abbreviations.tsv").exists())
        assertFalse(File(dir, "fr/diacritics.tsv").exists())
    }
    
    @Test
    fun `install writes an optional hints file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf("dict.tsv" to "der\t100\n", "hints.tsv" to "a=ä;s=ß")
        
        LanguagePackInstaller.install(archive, dir, Language.GERMAN)
        
        assertEquals("a=ä;s=ß", File(dir, "de/hints.tsv").readText())
    }
    
    @Test
    fun `D-434 install writes an optional abbreviations file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf("dict.tsv" to "der\t100\n", "abbreviations.tsv" to "usw.\nbzgl.\n")
        
        LanguagePackInstaller.install(archive, dir, Language.GERMAN)
        
        assertEquals("usw.\nbzgl.\n", File(dir, "de/abbreviations.tsv").readText())
    }
    
    @Test
    fun `D-436 install writes an optional diacritics file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf("dict.tsv" to "bonjour\t100\n", "diacritics.tsv" to "e\té,è,ê,ë\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertEquals("e\té,è,ê,ë\n", File(dir, "fr/diacritics.tsv").readText())
    }
    
    @Test
    fun `install rejects an archive missing the words entry`(@TempDir dir: File) {
        val archive = zipOf("bigram.tsv" to "bonjour\tle monde\t5\n")
        
        assertThrows(IOException::class.java) { LanguagePackInstaller.install(archive, dir, Language.FRENCH) }
    }
    
    @Test
    fun `install ignores unrelated entries`(@TempDir dir: File) {
        val archive = zipOf("dict.tsv" to "bonjour\t100\n", "README.md" to "not a dictionary")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "fr/dict.tsv").isFile)
        assertFalse(File(dir, "README.md").exists())
        assertFalse(File(dir, "fr/README.md").exists())
    }
    
    @Test
    fun `install keeps two languages' packs fully separate under the same parent directory`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf("dict.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        LanguagePackInstaller.install(zipOf("dict.tsv" to "hola\t100\n"), dir, Language.SPANISH)
        
        assertEquals("bonjour\t100\n", File(dir, "fr/dict.tsv").readText())
        assertEquals("hola\t100\n", File(dir, "es/dict.tsv").readText())
    }
    
    @Test
    fun `install creates the directory tree when absent`(@TempDir parent: File) {
        val dir = File(parent, "language_packs")
        LanguagePackInstaller.install(zipOf("dict.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        assertTrue(File(dir, "fr/dict.tsv").isFile)
    }
    
    @Test
    fun `install replaces an existing pack`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf("dict.tsv" to "old\t1\n"), dir, Language.FRENCH)
        LanguagePackInstaller.install(zipOf("dict.tsv" to "new\t2\n"), dir, Language.FRENCH)
        assertEquals("new\t2\n", File(dir, "fr/dict.tsv").readText())
    }
    
    @Test
    fun `install leaves no temporary part file behind`(@TempDir dir: File) {
        LanguagePackInstaller.install(
            zipOf("dict.tsv" to "bonjour\t100\n", "bigram.tsv" to "a\tb\t1\n", "hints.tsv" to "a=à"),
            dir,
            Language.FRENCH
        )
        assertFalse(File(dir, "fr/dict.tsv.part").exists())
        assertFalse(File(dir, "fr/bigram.tsv.part").exists())
        assertFalse(File(dir, "fr/hints.tsv.part").exists())
    }
    
    @Test
    fun `clear removes an installed pack including hints and its own subfolder, reports true`(@TempDir dir: File) {
        LanguagePackInstaller.install(
            zipOf(
                "dict.tsv" to "bonjour\t100\n",
                "bigram.tsv" to "a\tb\t1\n",
                "hints.tsv" to "a=à",
                "abbreviations.tsv" to "etc.\n",
                "diacritics.tsv" to "e\té,è\n"
            ),
            dir,
            Language.FRENCH
        )
        
        assertTrue(LanguagePackInstaller.clear(dir, Language.FRENCH))
        
        assertFalse(File(dir, "fr/dict.tsv").exists())
        assertFalse(File(dir, "fr/bigram.tsv").exists())
        assertFalse(File(dir, "fr/hints.tsv").exists())
        assertFalse(File(dir, "fr/abbreviations.tsv").exists())
        assertFalse(File(dir, "fr/diacritics.tsv").exists())
        assertFalse(File(dir, "fr").exists())
    }
    
    @Test
    fun `clear on an absent pack reports nothing removed`(@TempDir dir: File) {
        assertFalse(LanguagePackInstaller.clear(dir, Language.FRENCH))
    }
    
    @Test
    fun `D-308 install returns the archive's own version`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "3"),
            dir,
            Language.FRENCH
        )
        assertEquals(3, version)
    }
    
    @Test
    fun `D-308 install falls back to DEFAULT_VERSION when the archive has no version file`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(zipOf("dict.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-308 install falls back to DEFAULT_VERSION when the version file is unparseable`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "not-a-number"),
            dir,
            Language.FRENCH
        )
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-310 the version entry is never written to disk, unlike dict bigram and hints`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "4"),
            dir,
            Language.FRENCH
        )
        assertEquals(4, version)
        assertFalse(File(dir, "version.txt").exists())
        assertFalse(File(dir, "fr/version.txt").exists())
    }
    
    @Test
    fun `D-308 parse reads the archive without writing anything to disk`(@TempDir dir: File) {
        val pack = LanguagePackInstaller.parse(zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "5"), Language.FRENCH)
        
        assertEquals("bonjour\t100\n", String(pack.words, Charsets.UTF_8))
        assertEquals(5, pack.version)
        assertFalse(File(dir, "fr/dict.tsv").exists())
    }
    
    @Test
    fun `D-308 write applies an already-parsed pack to disk`(@TempDir dir: File) {
        val pack = LanguagePackInstaller.parse(zipOf("dict.tsv" to "bonjour\t100\n", "bigram.tsv" to "a\tb\t1\n"), Language.FRENCH)
        
        LanguagePackInstaller.write(dir, pack)
        
        assertEquals("bonjour\t100\n", File(dir, "fr/dict.tsv").readText())
        assertEquals("a\tb\t1\n", File(dir, "fr/bigram.tsv").readText())
    }
    
    @Test
    fun `D-386-followup parse accepts a matching declared language code on version txt's second line`() {
        val pack = LanguagePackInstaller.parse(
            zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "3\nfr\n"),
            Language.FRENCH
        )
        assertEquals(3, pack.version)
    }
    
    @Test
    fun `D-386-followup parse rejects a mismatched declared language code`() {
        val exception = assertThrows(LanguagePackInstaller.LanguageMismatchException::class.java) {
            LanguagePackInstaller.parse(zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "3\nde\n"), Language.FRENCH)
        }
        assertEquals("de", exception.declaredCode)
        assertEquals("fr", exception.expectedCode)
    }
    
    @Test
    fun `D-386-followup parse is tolerant of a legacy archive with no declared language code at all`() {
        val pack = LanguagePackInstaller.parse(zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "3"), Language.FRENCH)
        assertEquals(3, pack.version)
    }
    
    @Test
    fun `D-386-followup parse matches the declared language code case-insensitively`() {
        val pack = LanguagePackInstaller.parse(
            zipOf("dict.tsv" to "bonjour\t100\n", "version.txt" to "3\nFR\n"),
            Language.FRENCH
        )
        assertEquals(3, pack.version)
    }
    
    @Test
    fun `D-386-followup compareVersions returns INSTALL when nothing is installed yet`() {
        assertEquals(LanguagePackInstaller.VersionCheck.INSTALL, LanguagePackInstaller.compareVersions(1, null))
    }
    
    @Test
    fun `D-386-followup compareVersions returns INSTALL when the pack is strictly newer`() {
        assertEquals(LanguagePackInstaller.VersionCheck.INSTALL, LanguagePackInstaller.compareVersions(5, 3))
    }
    
    @Test
    fun `D-386-followup compareVersions returns ALREADY_CURRENT when the versions match exactly`() {
        assertEquals(LanguagePackInstaller.VersionCheck.ALREADY_CURRENT, LanguagePackInstaller.compareVersions(3, 3))
    }
    
    @Test
    fun `D-386-followup compareVersions returns OLDER_THAN_INSTALLED when the pack is strictly older`() {
        assertEquals(LanguagePackInstaller.VersionCheck.OLDER_THAN_INSTALLED, LanguagePackInstaller.compareVersions(2, 3))
    }
}

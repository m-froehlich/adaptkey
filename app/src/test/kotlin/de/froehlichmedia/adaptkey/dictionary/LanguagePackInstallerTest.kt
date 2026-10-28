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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests for the D-280 language-pack installer. D-310: every archive entry is a fixed name
 * (`dict.tsv`/`bigram.tsv`/`hints.tsv`/`version.txt`, no `<code>` suffix), and [LanguagePackInstaller.write]
 * applies into a per-language subfolder (`<dir>/<code>/`) rather than directly into [dir]. D-505: the
 * installer streams - [LanguagePackInstaller.readVersion] touches only the version entry, [extract] stages
 * `.part` files without applying them, and [write]'s atomic rename applies them.
 */
class LanguagePackInstallerTest {
    
    /** Writes the entries as a real zip file - D-505: the installer's API takes [File]s, not streams. */
    private fun zipOf(dir: File, vararg entries: Pair<String, String>): File {
        val archive = File(dir, "pack.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return archive
    }
    
    private fun partFile(dir: File, code: String, name: String): File = File(dir, "$code/$name.part")
    
    @Test
    fun `install writes both files from a well-formed archive into the language's own subfolder`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "bigram.tsv" to "bonjour\tle monde\t5\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertEquals("bonjour\t100\n", File(dir, "fr/dict.tsv").readText())
        assertEquals("bonjour\tle monde\t5\n", File(dir, "fr/bigram.tsv").readText())
    }
    
    @Test
    fun `install accepts a words-only archive, bigrams and hints stay absent`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "fr/dict.tsv").isFile)
        assertFalse(File(dir, "fr/bigram.tsv").exists())
        assertFalse(File(dir, "fr/hints.tsv").exists())
        assertFalse(File(dir, "fr/abbreviations.tsv").exists())
        assertFalse(File(dir, "fr/diacritics.tsv").exists())
    }
    
    @Test
    fun `install writes an optional hints file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "der\t100\n", "hints.tsv" to "a=ä;s=ß")
        
        LanguagePackInstaller.install(archive, dir, Language.GERMAN)
        
        assertEquals("a=ä;s=ß", File(dir, "de/hints.tsv").readText())
    }
    
    @Test
    fun `D-434 install writes an optional abbreviations file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "der\t100\n", "abbreviations.tsv" to "usw.\nbzgl.\n")
        
        LanguagePackInstaller.install(archive, dir, Language.GERMAN)
        
        assertEquals("usw.\nbzgl.\n", File(dir, "de/abbreviations.tsv").readText())
    }
    
    @Test
    fun `D-436 install writes an optional diacritics file when the archive includes one`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "diacritics.tsv" to "e\té,è,ê,ë\n")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertEquals("e\té,è,ê,ë\n", File(dir, "fr/diacritics.tsv").readText())
    }
    
    @Test
    fun `install rejects an archive missing the words entry`(@TempDir dir: File) {
        val archive = zipOf(dir, "bigram.tsv" to "bonjour\tle monde\t5\n")
        
        assertThrows(IOException::class.java) { LanguagePackInstaller.install(archive, dir, Language.FRENCH) }
    }
    
    @Test
    fun `install ignores unrelated entries`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "README.md" to "not a dictionary")
        
        LanguagePackInstaller.install(archive, dir, Language.FRENCH)
        
        assertTrue(File(dir, "fr/dict.tsv").isFile)
        assertFalse(File(dir, "README.md").exists())
        assertFalse(File(dir, "fr/README.md").exists())
    }
    
    @Test
    fun `install keeps two languages' packs fully separate under the same parent directory`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf(dir, "dict.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        LanguagePackInstaller.install(zipOf(dir, "dict.tsv" to "hola\t100\n"), dir, Language.SPANISH)
        
        assertEquals("bonjour\t100\n", File(dir, "fr/dict.tsv").readText())
        assertEquals("hola\t100\n", File(dir, "es/dict.tsv").readText())
    }
    
    @Test
    fun `install creates the directory tree when absent`(@TempDir parent: File) {
        val dir = File(parent, "language_packs")
        // The staged archive must not live inside the not-yet-existing target directory (in production
        // it is a cache file), so it is written next to it.
        LanguagePackInstaller.install(zipOf(parent, "dict.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        assertTrue(File(dir, "fr/dict.tsv").isFile)
    }
    
    @Test
    fun `install replaces an existing pack`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf(dir, "dict.tsv" to "old\t1\n"), dir, Language.FRENCH)
        LanguagePackInstaller.install(zipOf(dir, "dict.tsv" to "new\t2\n"), dir, Language.FRENCH)
        assertEquals("new\t2\n", File(dir, "fr/dict.tsv").readText())
    }
    
    @Test
    fun `install leaves no temporary part file behind`(@TempDir dir: File) {
        LanguagePackInstaller.install(
            zipOf(dir, "dict.tsv" to "bonjour\t100\n", "bigram.tsv" to "a\tb\t1\n", "hints.tsv" to "a=à"),
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
                dir,
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
            zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "3"),
            dir,
            Language.FRENCH
        )
        assertEquals(3, version)
    }
    
    @Test
    fun `D-308 install falls back to DEFAULT_VERSION when the archive has no version file`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(zipOf(dir, "dict.tsv" to "bonjour\t100\n"), dir, Language.FRENCH)
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-308 install falls back to DEFAULT_VERSION when the version file is unparseable`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "not-a-number"),
            dir,
            Language.FRENCH
        )
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, version)
    }
    
    @Test
    fun `D-310 the version entry is never written to disk, unlike dict bigram and hints`(@TempDir dir: File) {
        val version = LanguagePackInstaller.install(
            zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "4"),
            dir,
            Language.FRENCH
        )
        assertEquals(4, version)
        assertFalse(File(dir, "version.txt").exists())
        assertFalse(File(dir, "fr/version.txt").exists())
    }
    
    @Test
    fun `D-505 readVersion reads only the version entry and extracts nothing`(@TempDir dir: File) {
        val archive = zipOf(
            dir,
            "dict.tsv" to "bonjour\t100\n",
            "bigram.tsv" to "a\tb\t1\n",
            "version.txt" to "5"
        )
        
        val version = LanguagePackInstaller.readVersion(archive, Language.FRENCH)
        
        assertEquals(5, version)
        // Nothing staged, nothing applied - the caller can still walk away at zero extraction cost.
        assertFalse(File(dir, "fr").exists())
    }
    
    @Test
    fun `D-505 readVersion falls back to DEFAULT_VERSION when the archive has no version file`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n")
        
        assertEquals(InstalledLanguagesStore.DEFAULT_VERSION, LanguagePackInstaller.readVersion(archive, Language.FRENCH))
    }
    
    @Test
    fun `D-386-followup readVersion accepts a matching declared language code on version txt's second line`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "3\nfr\n")
        
        assertEquals(3, LanguagePackInstaller.readVersion(archive, Language.FRENCH))
    }
    
    @Test
    fun `D-386-followup readVersion rejects a mismatched declared language code`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "3\nde\n")
        
        val exception = assertThrows(LanguagePackInstaller.LanguageMismatchException::class.java) {
            LanguagePackInstaller.readVersion(archive, Language.FRENCH)
        }
        assertEquals("de", exception.declaredCode)
        assertEquals("fr", exception.expectedCode)
    }
    
    @Test
    fun `D-386-followup readVersion is tolerant of a legacy archive with no declared language code at all`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "3")
        
        assertEquals(3, LanguagePackInstaller.readVersion(archive, Language.FRENCH))
    }
    
    @Test
    fun `D-386-followup readVersion matches the declared language code case-insensitively`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "version.txt" to "3\nFR\n")
        
        assertEquals(3, LanguagePackInstaller.readVersion(archive, Language.FRENCH))
    }
    
    @Test
    fun `D-505 extract stages part files without applying them and write renames them into place`(@TempDir dir: File) {
        val archive = zipOf(dir, "dict.tsv" to "bonjour\t100\n", "bigram.tsv" to "a\tb\t1\n", "version.txt" to "5")
        
        val pack = LanguagePackInstaller.extract(archive, Language.FRENCH, dir)
        
        assertEquals(5, pack.version)
        assertEquals("bonjour\t100\n", pack.words.readText())
        assertEquals("a\tb\t1\n", pack.bigrams?.readText())
        // Staged next to their final locations, but the real files do not exist yet - a failed import
        // can never have half-replaced the previously installed pack.
        assertTrue(partFile(dir, "fr", "dict.tsv").isFile)
        assertTrue(partFile(dir, "fr", "bigram.tsv").isFile)
        assertFalse(File(dir, "fr/dict.tsv").exists())
        assertFalse(File(dir, "fr/bigram.tsv").exists())
        
        LanguagePackInstaller.write(dir, pack)
        
        assertEquals("bonjour\t100\n", File(dir, "fr/dict.tsv").readText())
        assertEquals("a\tb\t1\n", File(dir, "fr/bigram.tsv").readText())
        assertFalse(partFile(dir, "fr", "dict.tsv").exists())
        assertFalse(partFile(dir, "fr", "bigram.tsv").exists())
    }
    
    @Test
    fun `D-505 extract cleans up staged part files when the words entry is missing`(@TempDir dir: File) {
        val archive = zipOf(dir, "bigram.tsv" to "bonjour\tle monde\t5\n")
        
        assertThrows(IOException::class.java) { LanguagePackInstaller.extract(archive, Language.FRENCH, dir) }
        
        // Rejected from the central directory up front - nothing staged, nothing applied, not even the
        // language's own subfolder created for a first import.
        assertFalse(File(dir, "fr").exists())
    }
    
    @Test
    fun `D-505 extract cleans up staged part files when an entry is corrupt midway`(@TempDir dir: File) {
        // A structurally valid zip whose deflated entry payload has been corrupted in place: the central
        // directory still opens, the words entry validates up front, but inflating fails mid-copy - the
        // case that exercises extract's own failure cleanup rather than an up-front rejection.
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("dict.tsv"))
            zip.write("bonjour\t100\n".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        val archive = File(dir, "pack.zip")
        val corrupted = bytes.toByteArray()
        // Local file header = 30 bytes + 8-char entry name, so the deflated payload starts at offset 38;
        // 13 bytes of ASCII input compress to ~15 bytes, so offset 45 sits safely inside the stream. Any
        // flipped byte in a raw-deflate stream makes the inflater or the entry CRC check fail.
        corrupted[45] = (corrupted[45].toInt() xor 0x55).toByte()
        archive.writeBytes(corrupted)
        
        assertThrows(IOException::class.java) { LanguagePackInstaller.extract(archive, Language.FRENCH, dir) }
        
        assertFalse(partFile(dir, "fr", "dict.tsv").exists())
        assertFalse(File(dir, "fr/dict.tsv").exists())
        assertFalse(File(dir, "fr").exists())
    }
    
    @Test
    fun `D-505 install replaces an installed pack while keeping the old files intact until write succeeds`(@TempDir dir: File) {
        LanguagePackInstaller.install(zipOf(dir, "dict.tsv" to "old\t1\n"), dir, Language.FRENCH)
        
        val archive = zipOf(dir, "dict.tsv" to "new\t2\n")
        val pack = LanguagePackInstaller.extract(archive, Language.FRENCH, dir)
        
        // Between extract and write the previously installed pack is still complete and valid.
        assertEquals("old\t1\n", File(dir, "fr/dict.tsv").readText())
        
        LanguagePackInstaller.write(dir, pack)
        assertEquals("new\t2\n", File(dir, "fr/dict.tsv").readText())
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

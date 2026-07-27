// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
import de.froehlichmedia.adaptkey.language.Language
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Installs (and removes) a user-provided language-pack archive in [LanguagePackStorage]'s private
 * directory (D-280).
 *
 * The archive is a plain zip bundling a language's unigram (`dict_<code>.tsv`) and, optionally, bigram
 * (`bigram_<code>.tsv`), letter-hint (`hints_<code>.tsv`, D-281), and version (`version_<code>.txt`, D-308)
 * files - the same files [DictionaryLoader] already reads for a bundled language, just zipped together into one
 * download/import step (the user's own choice over a multi-file picker). The Android layer opens an
 * [InputStream] from the file the user picked (a system file picker / SAF Uri, so no storage permission is
 * needed) and hands it here. Each entry is copied to a temporary `.part` file first and only renamed into
 * place once the whole archive has been read successfully, so an interrupted or malformed import can never
 * leave a half-written file that looks complete - mirroring
 * [de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelInstaller]'s own atomic-rename approach. Kept over
 * [File] / [InputStream] so it is unit-testable on the JVM.
 *
 * D-308: [parse] and [write] are split apart so a caller can inspect [ParsedPack.version] - read from the
 * archive itself, not from [LanguagePackCatalog]'s own compiled-in hint - before deciding whether to apply
 * it at all. [install] remains the simple "always apply" convenience wrapper the original D-280 flow (and
 * every existing test) already used.
 */
object LanguagePackInstaller {
    
    private const val TEMP_SUFFIX = ".part"
    
    /**
     * A fully-parsed archive, read entirely into memory - nothing has been written to disk yet.
     *
     * @property language the language this pack is for
     * @property words the unigram file's raw bytes (always present - [parse] throws otherwise)
     * @property bigrams the optional bigram file's raw bytes
     * @property hints the optional letter-hint file's raw bytes
     * @property version D-308: the pack's own version, read from its `version_<code>.txt` entry - falls back to
     *           [InstalledLanguagesStore.DEFAULT_VERSION] when the entry is absent or unparseable (an
     *           archive built before this convention existed, or a malformed one). The project is young
     *           enough today that there is no real "unknown/legacy pack" case in practice - every pack
     *           either already carries a real version or is fresh enough that version 1 is simply correct -
     *           so this fallback is a pragmatic default, not a placeholder for a case expected to matter.
     */
    class ParsedPack(
        val language: Language,
        val words: ByteArray,
        val bigrams: ByteArray?,
        val hints: ByteArray?,
        val version: Int
    )
    
    /**
     * Unzips [source] into [packDir] unconditionally, validating it contains at least [language]'s unigram
     * entry before committing anything. Equivalent to `write(packDir, parse(source, language))` - the
     * simple "this is a fresh install, or an update the caller already decided to apply" path.
     *
     * @param source the archive bytes (closed by the caller)
     * @param packDir the private language-pack directory (created if absent)
     * @param language the language this archive is expected to contain
     * @return the archive's own [ParsedPack.version]
     * @throws IOException when the directory cannot be created, the archive is missing the unigram entry,
     *         or a temporary file cannot be renamed into place
     */
    fun install(source: InputStream, packDir: File, language: Language): Int {
        val pack = parse(source, language)
        write(packDir, pack)
        return pack.version
    }
    
    /**
     * D-308: reads [source] fully into memory without writing anything to disk - lets the caller (D-308's
     * own "only overwrite an already-installed pack when the picked archive is actually newer" check in
     * [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]) inspect [ParsedPack.version] before
     * deciding whether [write] should ever run at all.
     *
     * @param source the archive bytes (closed by this call)
     * @param language the language this archive is expected to contain
     * @return the parsed pack, not yet written anywhere
     * @throws IOException when the archive is missing the unigram entry
     */
    fun parse(source: InputStream, language: Language): ParsedPack {
        val wordsName = "dict_${language.code}.tsv"
        val bigramsName = "bigram_${language.code}.tsv"
        val hintsName = "hints_${language.code}.tsv"
        val versionName = "version_${language.code}.txt"
        var words: ByteArray? = null
        var bigrams: ByteArray? = null
        var hints: ByteArray? = null
        var versionBytes: ByteArray? = null
        ZipInputStream(source).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    wordsName -> words = zip.readBytes()
                    bigramsName -> bigrams = zip.readBytes()
                    hintsName -> hints = zip.readBytes()
                    versionName -> versionBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val wordsBytes = words ?: throw IOException("archive is missing $wordsName")
        val version = versionBytes
            ?.let { String(it, Charsets.UTF_8).trim().toIntOrNull() }
            ?: InstalledLanguagesStore.DEFAULT_VERSION
        return ParsedPack(language, wordsBytes, bigrams, hints, version)
    }
    
    /**
     * Writes an already-[parse]d pack to disk, unconditionally overwriting whatever was previously
     * installed for [ParsedPack.language].
     *
     * @param packDir the private language-pack directory (created if absent)
     * @param pack the parsed pack to write
     * @throws IOException when the directory cannot be created or a temporary file cannot be renamed into
     *         place
     */
    fun write(packDir: File, pack: ParsedPack) {
        if (!packDir.isDirectory && !packDir.mkdirs()) {
            throw IOException("cannot create language pack directory: $packDir")
        }
        writeAtomically(File(packDir, "dict_${pack.language.code}.tsv"), pack.words)
        pack.bigrams?.let { writeAtomically(File(packDir, "bigram_${pack.language.code}.tsv"), it) }
        pack.hints?.let { writeAtomically(File(packDir, "hints_${pack.language.code}.tsv"), it) }
    }
    
    /**
     * Removes [language]'s installed pack files (not the dictionary database itself - the caller is
     * responsible for also deleting that, see [DictionaryLoader.databaseName]).
     *
     * @param packDir the private language-pack directory
     * @param language the language to remove
     * @return true when a unigram file was present and deleted
     */
    fun clear(packDir: File, language: Language): Boolean {
        File(packDir, "dict_${language.code}.tsv" + TEMP_SUFFIX).delete()
        File(packDir, "bigram_${language.code}.tsv" + TEMP_SUFFIX).delete()
        File(packDir, "hints_${language.code}.tsv" + TEMP_SUFFIX).delete()
        File(packDir, "bigram_${language.code}.tsv").delete()
        File(packDir, "hints_${language.code}.tsv").delete()
        return File(packDir, "dict_${language.code}.tsv").delete()
    }
    
    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(target.parentFile, target.name + TEMP_SUFFIX)
        temp.writeBytes(bytes)
        target.delete()
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("cannot move imported file into place: $target")
        }
    }
}

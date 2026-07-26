// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

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
 * (`bigram_<code>.tsv`) file - the same two files [DictionaryLoader] already reads for a bundled language,
 * just zipped together into one download/import step (the user's own choice over a two-file picker). The
 * Android layer opens an [InputStream] from the file the user picked (a system file picker / SAF Uri, so
 * no storage permission is needed) and hands it here. Each entry is copied to a temporary `.part` file
 * first and only renamed into place once the whole archive has been read successfully, so an interrupted
 * or malformed import can never leave a half-written file that looks complete - mirroring
 * [de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelInstaller]'s own atomic-rename approach. Kept over
 * [File] / [InputStream] so it is unit-testable on the JVM.
 */
object LanguagePackInstaller {
    
    private const val TEMP_SUFFIX = ".part"
    
    /**
     * Unzips [source] into [packDir], validating it contains at least [language]'s unigram entry before
     * committing anything.
     *
     * @param source the archive bytes (closed by the caller)
     * @param packDir the private language-pack directory (created if absent)
     * @param language the language this archive is expected to contain
     * @throws IOException when the directory cannot be created, the archive is missing the unigram entry,
     *         or a temporary file cannot be renamed into place
     */
    fun install(source: InputStream, packDir: File, language: Language) {
        if (!packDir.isDirectory && !packDir.mkdirs()) {
            throw IOException("cannot create language pack directory: $packDir")
        }
        val wordsName = "dict_${language.code}.tsv"
        val bigramsName = "bigram_${language.code}.tsv"
        var words: ByteArray? = null
        var bigrams: ByteArray? = null
        ZipInputStream(source).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    wordsName -> words = zip.readBytes()
                    bigramsName -> bigrams = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val wordsBytes = words ?: throw IOException("archive is missing $wordsName")
        writeAtomically(File(packDir, wordsName), wordsBytes)
        bigrams?.let { writeAtomically(File(packDir, bigramsName), it) }
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
        File(packDir, "bigram_${language.code}.tsv").delete()
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

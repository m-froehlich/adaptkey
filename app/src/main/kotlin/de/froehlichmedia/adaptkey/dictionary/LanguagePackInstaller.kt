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
 * The archive is a plain zip bundling a language's unigram (`dict.tsv`) and, optionally, bigram
 * (`bigram.tsv`), letter-hint (`hints.tsv`, D-281), and version (`version.txt`, D-308) files, all at the
 * archive's own root with fixed names - D-310: no `<code>` suffix on any of them any more, since the
 * archive itself is already scoped to exactly one language, and [write] now extracts into that language's
 * own subfolder under [packDir] rather than a shared flat directory (see [LanguagePackStorage]'s own KDoc
 * for the full reasoning). The Android layer opens an [InputStream] from the file the user picked (a system
 * file picker / SAF Uri, so no storage permission is needed) and hands it here. Each entry is copied to a
 * temporary `.part` file first and only renamed into place once the whole archive has been read
 * successfully, so an interrupted or malformed import can never leave a half-written file that looks
 * complete - mirroring [de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelInstaller]'s own atomic-rename
 * approach. Kept over [File] / [InputStream] so it is unit-testable on the JVM.
 *
 * D-308: [parse] and [write] are split apart so a caller can inspect [ParsedPack.version] - read from the
 * archive itself, not from [LanguagePackCatalog]'s own compiled-in hint - before deciding whether to apply
 * it at all. [install] remains the simple "always apply" convenience wrapper the original D-280 flow (and
 * every existing test) already used.
 */
object LanguagePackInstaller {
    
    private const val TEMP_SUFFIX = ".part"
    private const val WORDS_ENTRY_NAME = "dict.tsv"
    private const val BIGRAMS_ENTRY_NAME = "bigram.tsv"
    private const val HINTS_ENTRY_NAME = "hints.tsv"
    private const val VERSION_ENTRY_NAME = "version.txt"
    
    /**
     * A fully-parsed archive, read entirely into memory - nothing has been written to disk yet.
     *
     * @property language the language this pack is for
     * @property words the unigram file's raw bytes (always present - [parse] throws otherwise)
     * @property bigrams the optional bigram file's raw bytes
     * @property hints the optional letter-hint file's raw bytes
     * @property version D-308: the pack's own version, read from its `version.txt` entry - falls back to
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
     * D-386-followup: the three possible outcomes of comparing a freshly-parsed archive's own
     * [ParsedPack.version] against whatever is already installed - distinguishing "genuinely the same
     * version, nothing to do" from "the found archive is actually *older*" matters on its own: a stale or
     * wrong file the user picked by mistake (e.g. an old cached copy still sitting in Downloads) must never
     * be reported the same way as an intentional, already-up-to-date re-check, or the user has no way to
     * notice they picked up an outdated file at all.
     */
    enum class VersionCheck {
        /** No installed version to compare against, or the archive is strictly newer - apply it. */
        INSTALL,
        
        /** Exactly the same version as what is already installed - nothing to apply. */
        ALREADY_CURRENT,
        
        /** Strictly older than what is already installed - nothing applied; worth surfacing distinctly. */
        OLDER_THAN_INSTALLED
    }
    
    /**
     * Pure version comparison, split out of [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]'s
     * own import flow so it is directly unit-testable independent of the SAF/Activity glue around it.
     *
     * @param packVersion the freshly-parsed archive's own [ParsedPack.version]
     * @param installedVersion the currently installed version, or null when the language is not installed
     *        at all yet (there is nothing to compare against)
     * @return the outcome - see [VersionCheck]'s own cases
     */
    fun compareVersions(packVersion: Int, installedVersion: Int?): VersionCheck {
        return when {
            installedVersion == null || packVersion > installedVersion -> VersionCheck.INSTALL
            packVersion == installedVersion -> VersionCheck.ALREADY_CURRENT
            else -> VersionCheck.OLDER_THAN_INSTALLED
        }
    }
    
    /**
     * D-386-followup: thrown by [parse] when the archive's own `version.txt` declares a language code (its
     * optional second line) that does not match the [language] the caller expected to import - a genuine
     * content-level check against picking the wrong archive by mistake (e.g. a similarly-named file from an
     * earlier download), not merely trusting the file the user handed in. An archive predating this
     * convention (no second line at all) is never rejected on this basis - see [parse]'s own KDoc.
     *
     * @property declaredCode the language code the archive itself declares
     * @property expectedCode the language the caller expected to import
     */
    class LanguageMismatchException(val declaredCode: String, val expectedCode: String) :
        IOException("archive declares language '$declaredCode', expected '$expectedCode'")
    
    /**
     * D-308: reads [source] fully into memory without writing anything to disk - lets the caller (D-308's
     * own "only overwrite an already-installed pack when the picked archive is actually newer" check in
     * [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]) inspect [ParsedPack.version] before
     * deciding whether [write] should ever run at all.
     *
     * D-386-followup: `version.txt` gained an optional second line - the pack's own declared language code
     * (`"de"`, `"el"`, ...), cross-checked against [language] here. A genuine, archive-internal identity
     * check kept even after D-413 reverted back to a manual single-file picker - the user visually
     * confirming a file name in the picker is still no guarantee it is the *content* they meant (a stale
     * same-named file downloaded earlier, or the wrong language's archive). Deliberately tolerant of an
     * archive with no second line at all (every archive built before this convention existed) - only an
     * actual, present mismatch is rejected, never a missing declaration.
     *
     * @param source the archive bytes (closed by this call)
     * @param language the language this archive is expected to contain
     * @return the parsed pack, not yet written anywhere
     * @throws IOException when the archive is missing the unigram entry
     * @throws LanguageMismatchException when `version.txt`'s own declared language code is present and does
     *         not match [language]
     */
    fun parse(source: InputStream, language: Language): ParsedPack {
        var words: ByteArray? = null
        var bigrams: ByteArray? = null
        var hints: ByteArray? = null
        var versionBytes: ByteArray? = null
        ZipInputStream(source).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    WORDS_ENTRY_NAME -> words = zip.readBytes()
                    BIGRAMS_ENTRY_NAME -> bigrams = zip.readBytes()
                    HINTS_ENTRY_NAME -> hints = zip.readBytes()
                    VERSION_ENTRY_NAME -> versionBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val wordsBytes = words ?: throw IOException("archive is missing $WORDS_ENTRY_NAME")
        val versionLines = versionBytes
            ?.let { String(it, Charsets.UTF_8).lines().map { line -> line.trim() }.filter { line -> line.isNotEmpty() } }
            ?: emptyList()
        val version = versionLines.getOrNull(0)?.toIntOrNull() ?: InstalledLanguagesStore.DEFAULT_VERSION
        val declaredCode = versionLines.getOrNull(1)
        if (declaredCode != null && !declaredCode.equals(language.code, ignoreCase = true)) {
            throw LanguageMismatchException(declaredCode, language.code)
        }
        return ParsedPack(language, wordsBytes, bigrams, hints, version)
    }
    
    /**
     * Writes an already-[parse]d pack to disk, unconditionally overwriting whatever was previously
     * installed for [ParsedPack.language] - D-310: into that language's own subfolder
     * (`<packDir>/<language.code>/`), not directly into [packDir] itself.
     *
     * @param packDir the private language-pack directory (its per-language subfolder is created if absent)
     * @param pack the parsed pack to write
     * @throws IOException when the directory cannot be created or a temporary file cannot be renamed into
     *         place
     */
    fun write(packDir: File, pack: ParsedPack) {
        val languageDir = File(packDir, pack.language.code)
        if (!languageDir.isDirectory && !languageDir.mkdirs()) {
            throw IOException("cannot create language pack directory: $languageDir")
        }
        writeAtomically(File(languageDir, WORDS_ENTRY_NAME), pack.words)
        pack.bigrams?.let { writeAtomically(File(languageDir, BIGRAMS_ENTRY_NAME), it) }
        pack.hints?.let { writeAtomically(File(languageDir, HINTS_ENTRY_NAME), it) }
    }
    
    /**
     * Removes [language]'s installed pack files (not the dictionary database itself - the caller is
     * responsible for also deleting that, see [DictionaryLoader.databaseName]) - D-310: including its own
     * now-empty subfolder under [packDir].
     *
     * @param packDir the private language-pack directory
     * @param language the language to remove
     * @return true when a unigram file was present and deleted
     */
    fun clear(packDir: File, language: Language): Boolean {
        val languageDir = File(packDir, language.code)
        File(languageDir, WORDS_ENTRY_NAME + TEMP_SUFFIX).delete()
        File(languageDir, BIGRAMS_ENTRY_NAME + TEMP_SUFFIX).delete()
        File(languageDir, HINTS_ENTRY_NAME + TEMP_SUFFIX).delete()
        File(languageDir, BIGRAMS_ENTRY_NAME).delete()
        File(languageDir, HINTS_ENTRY_NAME).delete()
        val removed = File(languageDir, WORDS_ENTRY_NAME).delete()
        // Best-effort: only actually deletes once the directory is empty, exactly the state the deletes
        // above just left it in (barring a stray unrelated file, harmless either way).
        languageDir.delete()
        return removed
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

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
import de.froehlichmedia.adaptkey.language.Language
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Installs (and removes) a user-provided language-pack archive in [LanguagePackStorage]'s private
 * directory (D-280).
 * 
 * The archive is a plain zip bundling a language's unigram (`dict.tsv`) and, optionally, bigram
 * (`bigram.tsv`), letter-hint (`hints.tsv`, D-281), abbreviation-list (`abbreviations.tsv`, D-434),
 * diacritics-table (`diacritics.tsv`, D-436), and version (`version.txt`, D-308) files, all at the archive's
 * own root with fixed names - D-310: no `<code>` suffix on any of them any more, since the
 * archive itself is already scoped to exactly one language, and [write] now extracts into that language's
 * own subfolder under [packDir] rather than a shared flat directory (see [LanguagePackStorage]'s own KDoc
 * for the full reasoning). The Android layer stages the archive the user picked (a system file picker /
 * SAF Uri, so no storage permission is needed) into a private cache file and hands that [File] here.
 * 
 * D-505: the whole pipeline streams - nothing is ever fully materialised in memory. The original D-280
 * implementation read every zip entry into a `ByteArray` (`ZipInputStream.readBytes()`) and held them all
 * in [ParsedPack] until [write] copied them to disk, which put the largest packs' *unpacked* content
 * (the Russian pack: 73 MB `dict.tsv` + 141 MB `bigram.tsv` - 214 MB simultaneously, against a ~256 MiB
 * app heap) straight into `OutOfMemoryError` territory; a community report confirmed exactly that
 * ("Import failed" on the Russian pack, while the much smaller Serbian pack imported fine). The pipeline
 * is now three narrow steps instead: [readVersion] opens the archive with `java.util.zip.ZipFile`
 * (random access, so the version/language check touches only the tiny `version.txt` entry and never
 * inflates the large ones), [extract] streams every wanted entry straight into the target language's own
 * subfolder as `.part` files, and [write] atomically renames those into place. Interrupted or malformed
 * imports can never leave a half-written file that looks complete - the `.part`-rename atomicity of the
 * original design is kept verbatim, and [extract] additionally deletes any half-extracted `.part` files
 * when it fails mid-stream. Kept over [File] so it is unit-testable on the JVM.
 * 
 * D-308: [readVersion] is split from [extract] so a caller can check [ParsedPack.version]'s source of
 * truth - read from the archive itself, not from [LanguagePackCatalog]'s own compiled-in hint - *before*
 * deciding whether the (potentially large) extraction should run at all. On the "already up to date" /
 * "older than installed" paths nothing is extracted any more, where the pre-D-505 flow inflated the whole
 * archive into memory just to read its version. [install] remains the simple "always apply" convenience
 * wrapper the original D-280 flow (and every existing test) already used.
 */
object LanguagePackInstaller {
    
    private const val TEMP_SUFFIX = ".part"
    private const val COPY_BUFFER_SIZE = 64 * 1024
    private const val WORDS_ENTRY_NAME = "dict.tsv"
    private const val BIGRAMS_ENTRY_NAME = "bigram.tsv"
    private const val HINTS_ENTRY_NAME = "hints.tsv"
    private const val ABBREVIATIONS_ENTRY_NAME = "abbreviations.tsv"
    private const val DIACRITICS_ENTRY_NAME = "diacritics.tsv"
    private const val VERSION_ENTRY_NAME = "version.txt"
    
    /**
     * An archive fully extracted into its target language's own subfolder as `.part` files - staged on
     * disk, not yet applied (no final file has been renamed over anything).
     * 
     * D-505: every property is the staged `.part` [File] itself rather than the entry's raw bytes - the
     * point of the whole rework is that a pack's content never has to fit into memory at once. The files
     * live next to their final locations (created by [extract]), so [write] is a pure atomic rename per
     * file.
     * 
     * @property language the language this pack is for
     * @property words the staged unigram file (always present - [extract] throws otherwise)
     * @property bigrams the staged optional bigram file, or null when the archive had none
     * @property hints the staged optional letter-hint file, or null when the archive had none
     * @property abbreviations the staged optional abbreviation-list file (D-434, §6 sentence-boundary
     *           detection), or null - a language pack without one simply falls back to
     *           [de.froehlichmedia.adaptkey.capitalisation.Abbreviations.GERMAN]
     * @property diacritics the staged optional diacritics-table file (D-436, D-387's own base-letter ->
     *           variants generalisation of [de.froehlichmedia.adaptkey.suggestion.Umlaut]), or null - a
     *           language pack without one simply falls back to
     *           [de.froehlichmedia.adaptkey.suggestion.NoOpDiacriticFolding]
     * @property version D-308: the pack's own version, read from its `version.txt` entry - falls back to
     *           [InstalledLanguagesStore.DEFAULT_VERSION] when the entry is absent or unparseable (an
     *           archive built before this convention existed, or a malformed one). The project is young
     *           enough today that there is no real "unknown/legacy pack" case in practice - every pack
     *           either already carries a real version or is fresh enough that version 1 is simply correct -
     *           so this fallback is a pragmatic default, not a placeholder for a case expected to matter.
     */
    class ParsedPack(
        val language: Language,
        val words: File,
        val bigrams: File?,
        val hints: File?,
        val abbreviations: File?,
        val diacritics: File?,
        val version: Int
    )
    
    /**
     * Unzips [archive] into [packDir] unconditionally, validating it contains at least [language]'s unigram
     * entry before committing anything. Equivalent to `write(packDir, extract(archive, language, packDir))`
     * - the simple "this is a fresh install, or an update the caller already decided to apply" path.
     * 
     * @param archive the staged archive file (not deleted by this call - the caller owns its lifecycle)
     * @param packDir the private language-pack directory (created if absent)
     * @param language the language this archive is expected to contain
     * @return the archive's own version (see [ParsedPack.version])
     * @throws IOException when the directory cannot be created, the archive is missing the unigram entry,
     *         or a temporary file cannot be renamed into place
     * @throws LanguageMismatchException when `version.txt`'s own declared language code is present and does
     *         not match [language]
     */
    fun install(archive: File, packDir: File, language: Language): Int {
        val pack = extract(archive, language, packDir)
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
     * D-386-followup: thrown by [readVersion]/[extract] when the archive's own `version.txt` declares a
     * language code (its optional second line) that does not match the [language] the caller expected to
     * import - a genuine content-level check against picking the wrong archive by mistake (e.g. a
     * similarly-named file from an earlier download), not merely trusting the file the user handed in. An
     * archive predating this convention (no second line at all) is never rejected on this basis - see
     * [readVersion]'s own KDoc.
     * 
     * @property declaredCode the language code the archive itself declares
     * @property expectedCode the language the caller expected to import
     */
    class LanguageMismatchException(val declaredCode: String, val expectedCode: String) :
        IOException("archive declares language '$declaredCode', expected '$expectedCode'")
    
    /**
     * D-308/D-505: reads [archive]'s own `version.txt` entry - and nothing else. ZipFile's random access
     * means the check costs one small entry read, never the inflation of the archive's large members, so
     * the caller can decide whether [extract] should run at all *before* any large work happens (the
     * pre-D-505 flow read the whole archive into memory just to learn its version).
     * 
     * D-386-followup: `version.txt` may carry an optional second line - the pack's own declared language
     * code (`"de"`, `"el"`, ...), cross-checked against [language] here. A genuine, archive-internal
     * identity check kept even after D-413 reverted back to a manual single-file picker - the user visually
     * confirming a file name in the picker is still no guarantee it is the *content* they meant (a stale
     * same-named file downloaded earlier, or the wrong language's archive). Deliberately tolerant of an
     * archive with no second line at all (every archive built before this convention existed) - only an
     * actual, present mismatch is rejected, never a missing declaration.
     * 
     * @param archive the staged archive file
     * @param language the language this archive is expected to contain
     * @return the archive's own version (see [ParsedPack.version])
     * @throws IOException when the archive cannot be opened as a zip at all
     * @throws LanguageMismatchException when `version.txt`'s own declared language code is present and does
     *         not match [language]
     */
    fun readVersion(archive: File, language: Language): Int {
        ZipFile(archive).use { zip ->
            return readVersion(zip, language)
        }
    }
    
    /**
     * D-505: streams every wanted entry of [archive] into [language]'s own subfolder under [packDir] as
     * `.part` files - one small copy buffer at a time, no entry is ever fully materialised in memory.
     * Applies nothing yet: the final [de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage] filenames
     * are only written by [write]'s atomic renames, so a failed extraction (a truncated archive, a missing
     * unigram entry) leaves the previously installed pack completely untouched - and this call cleans its
     * own half-extracted `.part` files up before rethrowing.
     * 
     * Two passes over the staged file, each doing the one job it is best at: a `java.util.zip.ZipFile`
     * preflight validates the unigram entry's presence and reads [ParsedPack.version] from the central
     * directory *without inflating the large members* (so a rejection or a skip can never trigger the
     * extraction work at all), then a sequential `java.util.zip.ZipInputStream` copy loop streams the
     * entries. The ZipInputStream pass deliberately keeps the original D-280 implementation's per-entry
     * CRC verification - ZipFile's own entry streams would read corrupted payload silently, and a
     * half-downloaded archive must fail the import, not import garbage.
     * 
     * @param archive the staged archive file
     * @param language the language this archive is expected to contain
     * @param packDir the private language-pack directory (its per-language subfolder is created if absent)
     * @return the extracted pack, staged as `.part` files ready for [write]
     * @throws IOException when the directory cannot be created, the archive is missing the unigram entry,
     *         the archive cannot be read as a zip, an entry fails its CRC check, or a `.part` file cannot
     *         be written
     * @throws LanguageMismatchException when `version.txt`'s own declared language code is present and does
     *         not match [language]
     */
    fun extract(archive: File, language: Language, packDir: File): ParsedPack {
        // Preflight: central-directory validation and version/language check, no large entry inflated yet.
        val version = ZipFile(archive).use { zip ->
            if (zip.getEntry(WORDS_ENTRY_NAME) == null) {
                throw IOException("archive is missing $WORDS_ENTRY_NAME")
            }
            readVersion(zip, language)
        }
        val languageDir = File(packDir, language.code)
        if (!languageDir.isDirectory && !languageDir.mkdirs()) {
            throw IOException("cannot create language pack directory: $languageDir")
        }
        var words: File? = null
        var bigrams: File? = null
        var hints: File? = null
        var abbreviations: File? = null
        var diacritics: File? = null
        try {
            // Copy loop: sequential streaming with per-entry CRC verification (ZipInputStream checks each
            // entry's CRC as it is drained - a corrupt payload fails here, never silently imports).
            ZipInputStream(archive.inputStream().buffered(COPY_BUFFER_SIZE)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val staged: File? = when (entry.name) {
                        WORDS_ENTRY_NAME -> File(languageDir, WORDS_ENTRY_NAME + TEMP_SUFFIX).also { words = it }
                        BIGRAMS_ENTRY_NAME -> File(languageDir, BIGRAMS_ENTRY_NAME + TEMP_SUFFIX).also { bigrams = it }
                        HINTS_ENTRY_NAME -> File(languageDir, HINTS_ENTRY_NAME + TEMP_SUFFIX).also { hints = it }
                        ABBREVIATIONS_ENTRY_NAME -> File(languageDir, ABBREVIATIONS_ENTRY_NAME + TEMP_SUFFIX).also { abbreviations = it }
                        DIACRITICS_ENTRY_NAME -> File(languageDir, DIACRITICS_ENTRY_NAME + TEMP_SUFFIX).also { diacritics = it }
                        else -> null
                    }
                    if (staged != null) {
                        staged.outputStream().use { output ->
                            zip.copyTo(output, COPY_BUFFER_SIZE)
                        }
                    }
                    // Drains and CRC-verifies the current entry - wanted ones are drained by the copy
                    // above, unrelated ones (and every wanted one's tail) by this.
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (t: Throwable) {
            // Half-extracted .part files only - no final file has been renamed over anything yet.
            // Best-effort directory cleanup: a fresh first-import failure leaves the just-created
            // subfolder empty at this point, so it goes away with the .part files (an update's
            // subfolder still holds the previously installed files and correctly survives).
            listOfNotNull(words, bigrams, hints, abbreviations, diacritics).forEach { it.delete() }
            languageDir.delete()
            throw t
        }
        return ParsedPack(language, requireNotNull(words), bigrams, hints, abbreviations, diacritics, version)
    }
    
    /**
     * Applies an already-[extract]ed pack to disk, unconditionally overwriting whatever was previously
     * installed for [ParsedPack.language] - D-310: into that language's own subfolder
     * (`<packDir>/<language.code>/`), not directly into [packDir] itself.
     * 
     * D-505: this is now a pure rename per staged `.part` file (created by [extract] next to their final
     * locations) instead of a byte copy - the atomic "only rename into place once everything succeeded"
     * contract of the original design is kept verbatim.
     * 
     * @param packDir the private language-pack directory (its per-language subfolder is created if absent)
     * @param pack the extracted pack to apply
     * @throws IOException when the directory cannot be created or a temporary file cannot be renamed into
     *         place
     */
    fun write(packDir: File, pack: ParsedPack) {
        val languageDir = File(packDir, pack.language.code)
        if (!languageDir.isDirectory && !languageDir.mkdirs()) {
            throw IOException("cannot create language pack directory: $languageDir")
        }
        moveToFinal(File(languageDir, WORDS_ENTRY_NAME), pack.words)
        pack.bigrams?.let { moveToFinal(File(languageDir, BIGRAMS_ENTRY_NAME), it) }
        pack.hints?.let { moveToFinal(File(languageDir, HINTS_ENTRY_NAME), it) }
        pack.abbreviations?.let { moveToFinal(File(languageDir, ABBREVIATIONS_ENTRY_NAME), it) }
        pack.diacritics?.let { moveToFinal(File(languageDir, DIACRITICS_ENTRY_NAME), it) }
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
        File(languageDir, ABBREVIATIONS_ENTRY_NAME + TEMP_SUFFIX).delete()
        File(languageDir, DIACRITICS_ENTRY_NAME + TEMP_SUFFIX).delete()
        File(languageDir, BIGRAMS_ENTRY_NAME).delete()
        File(languageDir, HINTS_ENTRY_NAME).delete()
        File(languageDir, ABBREVIATIONS_ENTRY_NAME).delete()
        File(languageDir, DIACRITICS_ENTRY_NAME).delete()
        val removed = File(languageDir, WORDS_ENTRY_NAME).delete()
        // Best-effort: only actually deletes once the directory is empty, exactly the state the deletes
        // above just left it in (barring a stray unrelated file, harmless either way).
        languageDir.delete()
        return removed
    }
    
    /** D-308: the version/language check over an already-open zip, shared by [readVersion] and [extract]. */
    private fun readVersion(zip: ZipFile, language: Language): Int {
        val versionEntry = zip.getEntry(VERSION_ENTRY_NAME)
            ?: return InstalledLanguagesStore.DEFAULT_VERSION
        val versionBytes = zip.getInputStream(versionEntry).use { it.readBytes() }
        val versionLines = String(versionBytes, Charsets.UTF_8)
            .lines()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
        val version = versionLines.getOrNull(0)?.toIntOrNull() ?: InstalledLanguagesStore.DEFAULT_VERSION
        val declaredCode = versionLines.getOrNull(1)
        if (declaredCode != null && !declaredCode.equals(language.code, ignoreCase = true)) {
            throw LanguageMismatchException(declaredCode, language.code)
        }
        return version
    }
    
    /** D-505: the per-file atomic apply - rename the staged `.part` over its final location, or nothing. */
    private fun moveToFinal(target: File, staged: File) {
        target.delete()
        if (!staged.renameTo(target)) {
            staged.delete()
            throw IOException("cannot move imported file into place: $target")
        }
    }
}

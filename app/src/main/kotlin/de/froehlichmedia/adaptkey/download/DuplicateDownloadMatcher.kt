// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.download

/**
 * D-386: a file discovered in the user-granted download-folder tree, with just enough metadata
 * ([DuplicateDownloadMatcher] needs no more) to pick the right one among possible browser-created
 * duplicates.
 *
 * @property documentId the SAF document ID within the tree (opaque to this object, passed straight through
 *           to [android.provider.DocumentsContract.buildDocumentUriUsingTree] by the caller)
 * @property name the file's own display name, exactly as the browser saved it
 * @property lastModifiedMillis the file's own last-modified time (epoch millis)
 */
data class DownloadCandidate(
    val documentId: String,
    val name: String,
    val lastModifiedMillis: Long
)

/**
 * D-386: finds the most recently modified file matching an expected download name, tolerating the
 * `" (1)"`/`" (2)"`/... suffix a browser inserts when a same-named file already exists in the destination
 * folder (the near-universal convention across Chrome/Edge/Firefox on Android, and the concrete complaint
 * that prompted this: Samsung One UI's own download sandboxing routinely leaves several stale, differently-
 * suffixed copies behind). Pure string/list logic, deliberately free of any SAF/`DocumentsContract`
 * dependency, so it is unit-testable without an Android runtime -
 * [de.froehlichmedia.adaptkey.download.DownloadFolderResolver] supplies the real candidate list from a
 * granted folder tree.
 */
object DuplicateDownloadMatcher {
    
    /**
     * @param candidates every file found in the granted folder (any order)
     * @param expectedFileName the plain file name the app itself expects (e.g. `"adaptkey-lang-de.zip"`,
     *        `"model_q4f16.onnx"`) - matched exactly, or with a `" (N)"` suffix inserted directly before the
     *        extension for any non-negative integer `N`
     * @return the matching candidate with the highest [DownloadCandidate.lastModifiedMillis], or null when
     *         no candidate matches at all
     */
    fun findNewest(candidates: List<DownloadCandidate>, expectedFileName: String): DownloadCandidate? {
        val pattern = duplicatePattern(expectedFileName)
        return candidates
            .filter { pattern.matches(it.name) }
            .maxByOrNull { it.lastModifiedMillis }
    }
    
    /**
     * @param expectedFileName the plain file name to build a duplicate-tolerant matcher for
     * @return a regex matching [expectedFileName] itself or any of its `" (N)"`-suffixed duplicates
     */
    private fun duplicatePattern(expectedFileName: String): Regex {
        val dot = expectedFileName.lastIndexOf('.')
        val stem = if (dot > 0) expectedFileName.substring(0, dot) else expectedFileName
        val extension = if (dot > 0) expectedFileName.substring(dot) else ""
        return Regex("^${Regex.escape(stem)}( \\(\\d+\\))?${Regex.escape(extension)}$")
    }
}

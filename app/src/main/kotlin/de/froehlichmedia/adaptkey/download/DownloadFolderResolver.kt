// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * D-386: the SAF-facing half of the duplicate-download-file fix - resolves
 * [de.froehlichmedia.adaptkey.download.DownloadFolderStore]'s granted tree URI into an actual file to
 * import, and cleans up after a successful import. Android/SAF glue, like every other Android-facing layer
 * in this project - covered by instrumented rather than unit tests; the actual matching decision is pure
 * and unit-tested via [DuplicateDownloadMatcher].
 */
object DownloadFolderResolver {
    
    /**
     * Lists every file directly inside [treeUri] and returns the newest one matching [expectedFileName]
     * (see [DuplicateDownloadMatcher]), resolved to a document URI ready to open/read.
     *
     * @param context any valid context
     * @param treeUri a previously granted `ACTION_OPEN_DOCUMENT_TREE` URI ([DownloadFolderStore.treeUri])
     * @param expectedFileName the plain file name the caller expects (e.g. `"adaptkey-lang-de.zip"`)
     * @return the resolved document URI of the newest match, or null when none was found
     */
    fun findNewestMatch(context: Context, treeUri: Uri, expectedFileName: String): Uri? {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)
        val candidates = ArrayList<DownloadCandidate>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                candidates.add(DownloadCandidate(cursor.getString(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        val winner = DuplicateDownloadMatcher.findNewest(candidates, expectedFileName) ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, winner.documentId)
    }
    
    /**
     * D-386's second half: deletes [documentUri] when it is no older than [maxAgeMillis] - the just-imported
     * file cleanup, so a *future* download of the same name never needs a `" (N)"` suffix to begin with,
     * keeping [findNewestMatch]'s own job trivial on every later import. Silently does nothing when the
     * provider does not support deleting the document, or the age can no longer be determined (both treated
     * as harmless - the file simply stays behind, exactly as it would have without this cleanup at all).
     *
     * @param context any valid context
     * @param documentUri the just-imported file's own document URI (typically [findNewestMatch]'s result)
     * @param maxAgeMillis the inclusive age ceiling for deletion
     * @param now "now", for deterministic testing of the pure age comparison below - defaults to the real
     *        wall clock
     */
    fun deleteIfRecentlyCreated(
        context: Context,
        documentUri: Uri,
        maxAgeMillis: Long,
        now: Long = System.currentTimeMillis()
    ) {
        val lastModified = context.contentResolver.query(
            documentUri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null } ?: return
        if (isRecentlyCreated(lastModified, maxAgeMillis, now)) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, documentUri) }
        }
    }
    
    /**
     * The pure age comparison [deleteIfRecentlyCreated] gates its deletion on, split out so it is directly
     * unit-testable without a `ContentResolver`.
     *
     * @param lastModifiedMillis the document's own last-modified time (epoch millis)
     * @param maxAgeMillis the inclusive age ceiling
     * @param now "now" (epoch millis)
     * @return true when the document is no older than [maxAgeMillis]
     */
    fun isRecentlyCreated(lastModifiedMillis: Long, maxAgeMillis: Long, now: Long): Boolean {
        return now - lastModifiedMillis <= maxAgeMillis
    }
    
    /** D-386: the age ceiling for post-import deletion, per the user's own explicit request. */
    const val DELETE_MAX_AGE_MILLIS = 60_000L
    
    /**
     * D-386: a best-effort `content://` URI for the device's own Downloads folder under the AOSP
     * `ExternalStorageProvider`, for [android.content.Intent.EXTRA_INITIAL_URI]'s own picker-navigation
     * hint - never
     * assumed to resolve; a picker that does not recognise this exact document ID shape (some OEM pickers)
     * simply falls back to its own default location, never an error. Shared by both
     * [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity] and [de.froehlichmedia.adaptkey.settings.
     * Tier3ModelActivity], the two `ACTION_OPEN_DOCUMENT_TREE` call sites.
     *
     * @return the hint URI to pass as `EXTRA_INITIAL_URI`
     */
    fun downloadsInitialUriHint(): Uri {
        return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download")
    }
}

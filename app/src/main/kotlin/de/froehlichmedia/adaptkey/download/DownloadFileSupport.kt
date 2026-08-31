// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * D-413: small SAF-facing helpers shared by both `ACTION_OPEN_DOCUMENT` import flows
 * ([de.froehlichmedia.adaptkey.settings.LanguagePacksActivity], [de.froehlichmedia.adaptkey.settings.
 * Tier3ModelActivity]) - a best-effort hint to open the picker directly in Downloads, and post-import
 * cleanup of the just-picked file. Android/SAF glue, like every other Android-facing layer in this project -
 * covered by instrumented rather than unit tests; [isRecentlyCreated] is the one pure decision, unit-tested
 * directly.
 *
 * D-413 is a straight revert of D-344/D-386's own `ACTION_OPEN_DOCUMENT_TREE` folder-grant mechanism: on a
 * real device (Samsung One UI, a recent Android version) the system picker refuses to grant the Downloads
 * folder itself at all ("Dieser Ordner kann nicht verwendet werden. Zum Schutz deiner Daten einen anderen
 * Ordner auswählen.") - a platform-level SAF restriction on exactly the folder this whole mechanism was built
 * around, not an app bug. [de.froehlichmedia.adaptkey.download.DownloadFolderStore] (the persisted tree
 * grant) and [de.froehlichmedia.adaptkey.download.DuplicateDownloadMatcher] (automatic `" (N)"`-suffix
 * duplicate resolution within a granted folder) are both gone with it - a single `ACTION_OPEN_DOCUMENT` pick
 * needs neither; the user chooses the exact file again, every time, the way the app worked before D-386.
 * [downloadsInitialUriHint] and the post-import [deleteIfRecentlyCreated] cleanup both still apply unchanged
 * to a single picked document URI - see D-386-followup (still live in [de.froehlichmedia.adaptkey.dictionary.
 * LanguagePackInstaller]) for the content-level staleness/language checks that remain as a safety net even
 * with the user picking the file directly again.
 */
object DownloadFileSupport {
    
    /**
     * Deletes [documentUri] when it is no older than [maxAgeMillis] - the just-imported file cleanup, kept
     * from D-386 at the user's own explicit request even after reverting the folder-grant mechanism around
     * it. Silently does nothing when the provider does not support deleting the document, or the age can no
     * longer be determined (both treated as harmless - the file simply stays behind, exactly as it would
     * have without this cleanup at all).
     *
     * @param context any valid context
     * @param documentUri the just-imported file's own document URI (the `ACTION_OPEN_DOCUMENT` result)
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
     * A best-effort `content://` URI for the device's own Downloads folder under the AOSP
     * `ExternalStorageProvider`, for [android.content.Intent.EXTRA_INITIAL_URI]'s own picker-navigation
     * hint - never assumed to resolve; a picker that does not recognise this exact document ID shape (some
     * OEM pickers) simply falls back to its own default location, never an error. Shared by both
     * [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity] and [de.froehlichmedia.adaptkey.settings.
     * Tier3ModelActivity], the two `ACTION_OPEN_DOCUMENT` call sites.
     *
     * @return the hint URI to pass as `EXTRA_INITIAL_URI`
     */
    fun downloadsInitialUriHint(): Uri {
        return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download")
    }
}

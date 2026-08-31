// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.download

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * D-386: persists the one folder tree (typically the browser's own Downloads directory) the user has
 * granted AdaptKey access to via `ACTION_OPEN_DOCUMENT_TREE`, shared by every SAF-based import flow
 * ([de.froehlichmedia.adaptkey.settings.LanguagePacksActivity], [de.froehlichmedia.adaptkey.settings.
 * Tier3ModelActivity]) - granted once, reused for every future import, no matter which of the two triggers
 * the grant first.
 *
 * `ACTION_OPEN_DOCUMENT_TREE` needs no manifest-declared permission at all (same "user-driven system
 * picker, no `<uses-permission>`" model `ACTION_OPEN_DOCUMENT` already used) - persisting the grant here is
 * what lets it survive across app/device restarts (`takePersistableUriPermission`), so the user is asked
 * only the one time.
 */
object DownloadFolderStore {
    
    private const val PREFS = "adaptkey_download_folder"
    private const val KEY_TREE_URI = "tree_uri"
    
    private const val PERSIST_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    
    /**
     * @param context any valid context
     * @return the previously granted tree URI, or null if none has been granted yet
     */
    fun treeUri(context: Context): Uri? {
        val stored = prefs(context).getString(KEY_TREE_URI, null) ?: return null
        return Uri.parse(stored)
    }
    
    /**
     * Records [uri] (the result of a just-completed `ACTION_OPEN_DOCUMENT_TREE` pick) as the granted
     * download folder, taking a persistable permission so it survives beyond this process's lifetime.
     * Replaces any previously granted folder.
     *
     * @param context any valid context
     * @param uri the tree URI the user just granted
     */
    fun save(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, PERSIST_FLAGS)
        prefs(context).edit().putString(KEY_TREE_URI, uri.toString()).apply()
    }
    
    /**
     * Forgets the granted download folder (releasing the persisted permission when possible), so the next
     * import falls back to asking the user to grant one again - used when [DownloadFolderResolver] fails to
     * find an expected file in it, on the assumption the user downloads to a different folder than before.
     *
     * @param context any valid context
     */
    fun clear(context: Context) {
        treeUri(context)?.let { uri ->
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, PERSIST_FLAGS) }
        }
        prefs(context).edit().remove(KEY_TREE_URI).apply()
    }
    
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

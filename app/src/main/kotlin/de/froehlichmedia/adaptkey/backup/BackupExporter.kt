// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.backup

import android.content.Context
import de.froehlichmedia.adaptkey.credential.CredentialStore
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.settings.SettingsStore

/**
 * D-278: gathers the current device's [BackupBundle] (§21) - the Android-facing counterpart to the pure
 * [BackupJsonCodec]. Reads every currently active language's store via [DictionaryLoader.loadStores], the
 * same set the running keyboard itself uses, so an export always reflects exactly what is genuinely on this
 * device (see [SqliteDictionaryStore.userBlacklistedWords] for why bundled data is excluded).
 */
object BackupExporter {
    
    /**
     * @param context any valid context (the input method service, or a settings [android.app.Activity])
     * @return the full backup bundle for this device's current state
     */
    fun export(context: Context): BackupBundle {
        val stores = DictionaryLoader.loadStores(context)
        val languages = stores.mapValues { (_, store) ->
            LanguageSection(
                learnedWords = store.learnedWords(),
                learnedBigrams = store.learnedBigramEntries(),
                learnedTrigrams = store.learnedTrigramEntries(),
                blacklist = store.userBlacklistedWords(),
                pendingBlacklist = store.pendingBlacklistEntries()
            )
        }
        return BackupBundle(
            formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
            appVersionName = currentAppVersionName(context),
            exportedAtEpochMillis = System.currentTimeMillis(),
            settings = SettingsStore.exportableSettings(context),
            credentials = CredentialStore.all(context),
            languages = languages
        )
    }
    
    // Read live from the actual installed package rather than a hand-maintained constant - mirrors
    // SettingsActivity's own "info_version" resolution.
    private fun currentAppVersionName(context: Context): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }
}

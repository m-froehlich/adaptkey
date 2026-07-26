// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.backup

import de.froehlichmedia.adaptkey.credential.CredentialEntry
import de.froehlichmedia.adaptkey.dictionary.DictionaryAssetParser
import de.froehlichmedia.adaptkey.dictionary.PendingBlacklistEntry
import de.froehlichmedia.adaptkey.dictionary.TrigramEntry
import de.froehlichmedia.adaptkey.dictionary.WordEntry
import de.froehlichmedia.adaptkey.language.Language

/**
 * D-278: the full export/import bundle (§21) - everything AdaptKey has personally learned or the user has
 * configured, carryable to a second device. Deliberately excludes the bundled dictionary/bigram data and
 * bundled blacklist entries, since those already ship with the app or a language pack and would only bloat
 * the file; only the genuinely personal overlay travels (see [SqliteDictionaryStore.userBlacklistedWords]'s
 * own KDoc for the same reasoning applied to the blacklist specifically).
 *
 * A [languages] entry only ever exists for a language this device actually has data for at export time -
 * [BackupImporter] separately decides, at import time, whether the *target* device can accept each one.
 *
 * @property formatVersion this bundle's own schema version - bumped only when the JSON shape itself changes,
 *           independently of [appVersionName]
 * @property appVersionName the exporting app's `versionName`, carried purely for diagnostic purposes; import
 *           logic does not currently branch on it
 * @property exportedAtEpochMillis when this bundle was created ([System.currentTimeMillis] at export time)
 * @property settings every raw preference key/value pair from [de.froehlichmedia.adaptkey.settings.SettingsStore]'s
 *           own shared-preferences file
 * @property credentials every learned username/email entry ([de.froehlichmedia.adaptkey.credential.CredentialStore])
 * @property languages per-language personal data, keyed by the language it belongs to
 */
data class BackupBundle(
    val formatVersion: Int,
    val appVersionName: String,
    val exportedAtEpochMillis: Long,
    val settings: Map<String, Any>,
    val credentials: List<CredentialEntry>,
    val languages: Map<Language, LanguageSection>
) {
    
    companion object {
        
        /** The schema version this build writes and can read; see [formatVersion]'s own KDoc. */
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/**
 * One language's personal data within a [BackupBundle] - the learned overlay, the user blacklist, and any
 * still-pending blacklist marks, read directly from that language's own
 * [de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore].
 *
 * @property learnedWords every row of the learned-word overlay (W-01)
 * @property learnedBigrams every learned bigram row
 * @property learnedTrigrams every learned trigram row (S-07)
 * @property blacklist every [de.froehlichmedia.adaptkey.dictionary.BlacklistCategory.USER] blacklist word
 * @property pendingBlacklist every still-pending provisional-blacklist mark (G-04/W-01)
 */
data class LanguageSection(
    val learnedWords: List<WordEntry>,
    val learnedBigrams: List<DictionaryAssetParser.Bigram>,
    val learnedTrigrams: List<TrigramEntry>,
    val blacklist: List<String>,
    val pendingBlacklist: List<PendingBlacklistEntry>
)

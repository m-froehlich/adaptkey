// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * One provisional-pending-blacklist mark (G-04/W-01), as read back from [SqliteDictionaryStore] for the
 * D-278 backup/export feature.
 *
 * @property word the marked word (lower-cased key, as stored)
 * @property timestampMillis when the mark was recorded ([System.currentTimeMillis] at mark time)
 */
data class PendingBlacklistEntry(
    val word: String,
    val timestampMillis: Long
)

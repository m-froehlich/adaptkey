// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

/**
 * D-142: one learned credential-style value (a username or email address the user has typed into a
 * recognised login field), kept entirely separate from the ordinary word dictionary.
 *
 * @property value the credential exactly as typed (canonical case preserved)
 * @property kind [LoginFieldKind.USERNAME] or [LoginFieldKind.EMAIL] - never [LoginFieldKind.PASSWORD]
 *           or [LoginFieldKind.NONE], which are never stored at all
 * @property frequency how many times this exact value has been observed
 */
data class CredentialEntry(
    val value: String,
    val kind: LoginFieldKind,
    val frequency: Long
)

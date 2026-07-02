// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * The positional and field context used to decide a token's capitalisation (§6).
 *
 * @property explicitFirstUpper the user explicitly typed/shifted the first letter as uppercase;
 *           an explicit uppercase is never lowercased (hierarchy rule 1)
 * @property sentenceStart the token starts a sentence (field start, or after `.`/`!`/`?` + space)
 * @property capsMode the editor-mandated capitalisation, ranking directly below explicit input
 * @property afterHyphen the token is the segment following a hyphen (B-02: lowercase unless proper)
 */
data class CapitalisationContext(
    val explicitFirstUpper: Boolean,
    val sentenceStart: Boolean,
    val capsMode: CapsMode,
    val afterHyphen: Boolean
)

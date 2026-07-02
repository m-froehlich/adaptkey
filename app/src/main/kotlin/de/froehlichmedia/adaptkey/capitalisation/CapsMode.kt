// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * Editor-mandated capitalisation, derived from the target field's {@code EditorInfo} input-type
 * flags (§6 "Editor-Mandated Capitalisation").
 */
enum class CapsMode {
    
    /** No field-mandated capitalisation. */
    NONE,
    
    /** {@code TYPE_TEXT_FLAG_CAP_SENTENCES}: capitalise the first word of each sentence. */
    SENTENCES,
    
    /** {@code TYPE_TEXT_FLAG_CAP_WORDS}: capitalise the first letter of every word. */
    WORDS,
    
    /** {@code TYPE_TEXT_FLAG_CAP_CHARACTERS}: capitalise every character. */
    CHARACTERS
}

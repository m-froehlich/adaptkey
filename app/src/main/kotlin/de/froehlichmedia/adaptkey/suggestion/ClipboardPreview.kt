// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Pure formatting of the clipboard direct-paste chip label (D-36): a short single-line preview of the
 * clipboard text, or a masked run of bullets when the content is sensitive (e.g. a password), so nothing
 * secret is shown on the keyboard. Kept Android-free so the masking / truncation is unit-testable; the
 * Android layer supplies the raw text and the sensitivity flag.
 */
object ClipboardPreview {
    
    private const val MAX_LENGTH = 24
    private const val ELLIPSIS = "…"
    private const val BULLET = '•'
    private const val MASK_LENGTH = 6
    
    /**
     * Formats the chip label for [text].
     *
     * @param text the clipboard text
     * @param sensitive whether the clipboard content is sensitive (mask it, e.g. a password)
     * @return the label to show, or null when there is nothing worth offering (blank text)
     */
    fun label(text: CharSequence?, sensitive: Boolean): String? {
        if (text.isNullOrBlank()) {
            return null
        }
        if (sensitive) {
            return BULLET.toString().repeat(MASK_LENGTH)
        }
        val collapsed = text.toString().replace(Regex("\\s+"), " ").trim()
        if (collapsed.isEmpty()) {
            return null
        }
        return if (collapsed.length > MAX_LENGTH) {
            collapsed.take(MAX_LENGTH - 1) + ELLIPSIS
        } else {
            collapsed
        }
    }
}

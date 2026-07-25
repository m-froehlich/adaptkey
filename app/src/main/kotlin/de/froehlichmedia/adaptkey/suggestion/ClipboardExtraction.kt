// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-266: pure extraction logic for two clipboard-paste chip variants beyond V-01's own whole-clipboard
 * native paste - "Erste Zeile" (the clipboard's first line) and "Erster Code" (the first plausible "code"
 * token). [firstCode] is explicitly an iterative, first-pass feature per the user's own framing ("wird
 * anfangs oft danebenliegen, muss in späteren Runden nachgeschärft werden") - a chain of specialised
 * parsers, tried in order, each free to fail (return null) and fall through to the next; the generic
 * alphanumeric-run fallback deliberately runs last, not first, per the user's own proposed architecture.
 */
object ClipboardExtraction {
    
    /**
     * @param text the raw clipboard text
     * @return the first non-blank line, trimmed, or null when [text] has none
     */
    fun firstLine(text: CharSequence): String? {
        return text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.takeIf { it.isNotEmpty() }
    }
    
    /**
     * @param text the raw clipboard text
     * @return the extracted code token, or null when [text] is blank
     */
    fun firstCode(text: CharSequence): String? {
        val trimmed = text.toString().trim()
        if (trimmed.isEmpty()) {
            return null
        }
        return urlQueryOrPathSegment(trimmed) ?: firstAlphanumericRun(trimmed)
    }
    
    /**
     * The URL-aware parser: a query-string parameter's value (e.g. `"...?code=SDF123rtert"` ->
     * `"SDF123rtert"`) takes priority over a trailing path segment, since a query value is far more often
     * the actual "code" being shared (an invite code, a tracking token) than the last path segment is.
     * Gated on [text] containing no whitespace at all - a genuine URL/URI is always one contiguous token,
     * so this cheaply avoids misfiring a URL-shaped parse onto ordinary prose that merely happens to
     * contain a `?`.
     */
    private fun urlQueryOrPathSegment(text: String): String? {
        if (text.any { it.isWhitespace() }) {
            return null
        }
        val queryStart = text.indexOf('?')
        if (queryStart >= 0 && queryStart + 1 < text.length) {
            val query = text.substring(queryStart + 1).substringBefore('#')
            val value = query.substringBefore('&').substringAfter('=', missingDelimiterValue = "")
            firstAlphanumericRun(value)?.let { return it }
        }
        val pathEnd = text.indexOfFirst { it == '?' || it == '#' }.let { if (it < 0) text.length else it }
        val lastSegment = text.substring(0, pathEnd).substringAfterLast('/')
        return firstAlphanumericRun(lastSegment)
    }
    
    /** The generic fallback: the first maximal run of letters/digits anywhere in [text]. */
    private fun firstAlphanumericRun(text: String): String? {
        val start = text.indexOfFirst { it.isLetterOrDigit() }
        if (start < 0) {
            return null
        }
        var end = start
        while (end < text.length && text[end].isLetterOrDigit()) {
            end++
        }
        return text.substring(start, end)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-137: recognises a just-typed time (`14:30`, `9:05`) so "Uhr" can always be offered as a next-word
 * prediction, regardless of what the bigram table happens to know about that exact, effectively unique
 * digit token. Deliberately a plain shape check (1-2 digit hour, `:`, 2-digit minute) rather than validating
 * real time ranges (`0-23`/`0-59`) - a shape-only false positive (e.g. `99:99`) is harmless (it just offers
 * a plausible word that happens not to apply), while range-validating would add complexity for no real gain.
 */
object TimePattern {
    
    private val PATTERN = Regex("""\b\d{1,2}:\d{2}\s*$""")
    
    /**
     * @param text the text immediately before the cursor
     * @return true when [text] ends with a typed time, optionally followed by trailing whitespace
     */
    fun endsWithTime(text: CharSequence): Boolean {
        return PATTERN.containsMatchIn(text)
    }
}

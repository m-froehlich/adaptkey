// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

/**
 * Pure decision of whether the position after [before] (the text before the cursor) begins a new
 * sentence, i.e. whether the next word should be auto-capitalised (§6). Used for both the
 * capitalisation context and the Shift auto-arming.
 *
 * Rules:
 * - the very start of the field (only whitespace before) is a sentence start;
 * - a `.`, `!` or `?` that is actually followed by whitespace ends a sentence — UNLESS the period
 *   belongs to a known abbreviation or an enumerator (`usw.`, `1.`), which is not a terminator;
 * - a new line (the text before ends with a run of whitespace containing a line break) begins a new
 *   sentence too, EXCEPT when the preceding content line ended with a comma (the German e-mail
 *   salutation: `Hallo Max Mustermann,` ⏎⏎ `danke …`), which is suppressed when
 *   [suppressAfterCommaLine] is set (C-10, default on);
 * - anything else (mid-line, no terminator) is not a sentence start.
 *
 * Blank lines between content lines are skipped implicitly because they are part of the trailing
 * whitespace run.
 */
object SentenceBoundary {
    
    /**
     * @param before the text before the cursor (e.g. from `getTextBeforeCursor`)
     * @param suppressAfterCommaLine whether a comma-terminated line suppresses the next line's
     *        sentence start (C-10)
     * @return true when the next word starts a new sentence and should be auto-capitalised
     */
    fun isSentenceStart(before: String, suppressAfterCommaLine: Boolean): Boolean {
        val trailingWhitespace = before.takeLastWhile { it.isWhitespace() }
        val trimmed = before.substring(0, before.length - trailingWhitespace.length)
        if (trimmed.isEmpty()) {
            // Start of the field (possibly after leading blank lines).
            return true
        }
        if (trailingWhitespace.isEmpty()) {
            // Still inside a token / line — no delimiter yet.
            return false
        }
        val lastChar = trimmed.last()
        val lineStart = trailingWhitespace.any { it == '\n' || it == '\r' }
        if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
            if (lastChar == '.' && Abbreviations.isNonTerminalPeriod(lastToken(trimmed))) {
                // e.g. "… usw. x" / "1. x": the period is not a terminator; only a real new line starts over.
                return lineStart
            }
            return true
        }
        if (lineStart) {
            // A new line is a fresh sentence, except right after a comma-terminated line (salutation).
            return !(suppressAfterCommaLine && lastChar == ',')
        }
        return false
    }
    
    private fun lastToken(text: String): String {
        return text.takeLastWhile { !it.isWhitespace() }
    }
    
    /**
     * D-373: the segment immediately preceding a trailing hyphen in [before] (e.g. `"Rhein-Main-"` ->
     * `"Main"`), paired with whether *that* segment was itself at a sentence start - used to decide whether
     * a hyphen chain's own capitalisation should propagate onto the segment now starting (B-02/D-373):
     * a previous segment that was itself a sentence start needs its own dictionary part-of-speech check
     * before propagating (a sentence-initial capital says nothing about the word's own grammatical status),
     * while a previous segment capitalised anywhere else in the chain is trusted directly.
     *
     * @param before the text before the cursor; must already be known to end in `"-"`
     * @param suppressAfterCommaLine forwarded to [isSentenceStart] for the previous segment's own context
     * @return the previous segment's own text (real casing preserved) and whether it was a sentence start,
     *         or null when [before] ends in a bare hyphen with no letters before it at all
     */
    fun previousHyphenSegment(before: String, suppressAfterCommaLine: Boolean): Pair<String, Boolean>? {
        val beforeHyphen = before.dropLast(1)
        val segment = beforeHyphen.takeLastWhile { it.isLetter() }
        if (segment.isEmpty()) {
            return null
        }
        val contextBeforeSegment = beforeHyphen.substring(0, beforeHyphen.length - segment.length)
        return segment to isSentenceStart(contextBeforeSegment, suppressAfterCommaLine)
    }
    
    private const val BARE_TERMINATORS = ".!?"
    
    /**
     * D-416: appends a virtual, never-written trailing space to [before] when it ends directly in a bare
     * sentence-terminating mark (`.`/`!`/`?`) with nothing after it - the deferred auto-space model arms
     * capitalisation for the next word immediately on committing such a mark, before any physical space
     * exists to satisfy [isSentenceStart]'s own "requires real trailing whitespace" rule above. Comma is
     * deliberately excluded - it was never one of this object's own terminator characters (A-12's
     * space-arming punctuation set `.!?,` is broader than this one), matching the existing rule that a
     * comma never arms capitalisation by itself.
     *
     * @param before the text before the cursor, exactly as passed to [isSentenceStart]
     * @return [before] unchanged, or with one virtual trailing space appended when it ends bare in `.`/`!`/`?`
     */
    fun withPendingTerminatorSpace(before: String): String {
        return if (before.isNotEmpty() && before.last() in BARE_TERMINATORS) "$before " else before
    }
}

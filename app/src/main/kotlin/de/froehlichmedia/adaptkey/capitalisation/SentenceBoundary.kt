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
}

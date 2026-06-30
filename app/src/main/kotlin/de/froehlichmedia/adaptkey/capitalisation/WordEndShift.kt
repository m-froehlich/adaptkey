package de.froehlichmedia.adaptkey.capitalisation

/**
 * Pure logic for the G-05 gesture: pressing Shift at the end of a fully typed word.
 *
 * Pressing Shift provisionally toggles the case of the word's first character. The final outcome
 * depends on the very next key:
 *
 * - **a space, punctuation or line break** keeps the toggle ([Resolution.KEEP]);
 * - **a letter** discards the toggle and instead inserts that letter as an uppercase character
 *   inside the word ([Resolution.CAMEL_CASE], i.e. camelCase / PascalCase);
 * - **another Shift** re-toggles the first character ([Resolution.RETOGGLE]);
 * - **anything else** (e.g. backspace) cancels the pending gesture ([Resolution.CANCEL]).
 *
 * The case toggle ([flipFirst]) is its own inverse, so the service can both apply and revert it with
 * the same call.
 */
object WordEndShift {
    
    /** The category of the key pressed directly after the word-end Shift. */
    enum class NextKey {
        LETTER,
        DELIMITER,
        SHIFT,
        OTHER
    }
    
    /** What to do with the provisional first-character toggle once the next key is known. */
    enum class Resolution {
        CAMEL_CASE,
        KEEP,
        RETOGGLE,
        CANCEL
    }
    
    /**
     * Resolves the pending word-end Shift against the next key.
     *
     * @param next the category of the next key
     * @return the resolution to apply
     */
    fun resolveNextKey(next: NextKey): Resolution {
        return when (next) {
            NextKey.LETTER -> Resolution.CAMEL_CASE
            NextKey.DELIMITER -> Resolution.KEEP
            NextKey.SHIFT -> Resolution.RETOGGLE
            NextKey.OTHER -> Resolution.CANCEL
        }
    }
    
    /**
     * Toggles the case of the first character of [word]. A non-letter first character (or an empty
     * string) is returned unchanged; the remaining characters are never touched.
     *
     * @param word the word whose first character should be flipped
     * @return the word with its first character's case toggled
     */
    fun flipFirst(word: String): String {
        val first = word.firstOrNull() ?: return word
        if (!first.isLetter()) {
            return word
        }
        val flipped = if (first.isUpperCase()) first.lowercaseChar() else first.uppercaseChar()
        return flipped + word.substring(1)
    }
}

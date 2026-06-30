package de.froehlichmedia.adaptkey.capitalisation

/**
 * Pure decision logic for the "delayed Shift against surprising field capitalisation" guard (§6, C-07).
 *
 * Background: with {@code TYPE_TEXT_FLAG_CAP_WORDS} or {@code TYPE_TEXT_FLAG_CAP_CHARACTERS} every word
 * start is auto-armed to uppercase, including mid-sentence words. That is surprising relative to normal
 * typing: a user who reflexively presses Shift to capitalise the next word would instead toggle the
 * already-armed uppercase back to lowercase. The guard ignores such a Shift press during a short grace
 * window after the word start; once it elapses Shift toggles normally, so a deliberate lowercase
 * override remains possible. The guard applies only to a field mandate outside a regular sentence start
 * - ordinary sentence-start capitalisation is unaffected.
 *
 * This object owns only the decisions; the arming, the monotonic timing and the actual key state live
 * in the input method service, so the window length (C-07) and the elapsed time are passed in as plain
 * values and this logic stays Android-free and unit-tested.
 */
object ShiftGrace {
    
    /**
     * Whether a fresh word start should be auto-armed to uppercase by the field mandate.
     *
     * @param capsMode the editor-mandated capitalisation
     * @param sentenceStart whether the upcoming word starts a sentence
     * @return true when the next letter should default to uppercase
     */
    fun autoArmAtWordStart(capsMode: CapsMode, sentenceStart: Boolean): Boolean {
        return when (capsMode) {
            CapsMode.WORDS, CapsMode.CHARACTERS -> true
            CapsMode.SENTENCES -> sentenceStart
            CapsMode.NONE -> false
        }
    }
    
    /**
     * Whether the arm at this word start is the "surprising" field mandate the grace guard protects:
     * a per-word field mandate ([CapsMode.WORDS] / [CapsMode.CHARACTERS]) outside a regular sentence
     * start. Ordinary sentence-start capitalisation is never guarded.
     *
     * @param capsMode the editor-mandated capitalisation
     * @param sentenceStart whether the upcoming word starts a sentence
     * @return true when a Shift press against this arm is subject to the grace window
     */
    fun isGuardedArm(capsMode: CapsMode, sentenceStart: Boolean): Boolean {
        return (capsMode == CapsMode.WORDS || capsMode == CapsMode.CHARACTERS) && !sentenceStart
    }
    
    /**
     * Whether a Shift press should be suppressed (ignored, leaving the field-mandated uppercase armed).
     *
     * @param guardedArm whether the current word start is a guarded field-mandated arm ([isGuardedArm])
     * @param currentlyUpper whether Shift is currently armed to uppercase (the press would lower it)
     * @param windowMs the configured grace window in ms (C-07); 0 disables the guard entirely
     * @param elapsedMs time elapsed since the word start was armed, in ms
     * @return true when the press falls inside the grace window and must be ignored
     */
    fun suppressesShiftPress(guardedArm: Boolean, currentlyUpper: Boolean, windowMs: Long, elapsedMs: Long): Boolean {
        if (windowMs <= 0L) {
            return false
        }
        if (!guardedArm || !currentlyUpper) {
            return false
        }
        return elapsedMs in 0L until windowMs
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

/**
 * Tracks the text state of the K-01 calibration flow, free of Android dependencies.
 *
 * The session holds the provided sentences (T-03 raw-coordinate seeding works regardless of what is
 * typed - the value lies in the raw touch points, not in matching the target exactly), the index of
 * the current sentence and the buffer typed for it. The Android layer feeds key events into [append] /
 * [backspace] and renders the exposed state; advancing to the next sentence ([advance]) clears the
 * buffer. Kept pure so the progression logic can be unit-tested on the JVM.
 *
 * @property sentences the calibration sentences in order; must be non-empty with no blank entries
 */
class CalibrationSession(private val sentences: List<String>) {
    
    init {
        require(sentences.isNotEmpty()) { "sentences must not be empty" }
        require(sentences.none { it.isBlank() }) { "sentences must not be blank" }
    }
    
    private val typed = StringBuilder()
    
    /** Zero-based index of the current sentence. */
    var index: Int = 0
        private set
    
    /** Total number of calibration sentences. */
    val sentenceCount: Int
        get() = sentences.size
    
    /** One-based number of the current sentence, for display ("sentence 2 of 3"). */
    val currentNumber: Int
        get() = index + 1
    
    /** Whether the current sentence is the last one (its completion finishes the flow). */
    val isOnLastSentence: Boolean
        get() = index == sentences.size - 1
    
    /** @return the sentence the user is currently asked to type */
    fun currentSentence(): String {
        return sentences[index]
    }
    
    /** @return the text typed so far for the current sentence */
    fun typedText(): String {
        return typed.toString()
    }
    
    /**
     * Appends a typed character to the current sentence's buffer.
     *
     * @param ch the character to append
     */
    fun append(ch: Char) {
        typed.append(ch)
    }
    
    /** Removes the last typed character of the current sentence, if any. */
    fun backspace() {
        if (typed.isNotEmpty()) {
            typed.setLength(typed.length - 1)
        }
    }
    
    /**
     * Advances to the next sentence, clearing the typed buffer.
     *
     * @return true if the session moved on to a further sentence; false if it was already on the last
     *         one (in which case nothing changes and the caller should finish the calibration)
     */
    fun advance(): Boolean {
        if (isOnLastSentence) {
            return false
        }
        index += 1
        typed.setLength(0)
        return true
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Placeholder suggestion source backed by a tiny built-in frequency list of common German words.
 *
 * This exists only to exercise the suggestion bar end to end; it is NOT the dictionary
 * infrastructure from the specification. The SQLite-backed
 * {@link de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider} is the real
 * implementation behind the same {@link SuggestionProvider} interface. It ignores the n-gram
 * context argument.
 */
class StubSuggestionProvider : SuggestionProvider {
    
    // Roughly frequency-ordered; the score is derived from the position (earlier = more frequent).
    private val words = listOf(
        "der", "die", "und", "in", "den", "von", "zu", "das", "mit", "sich",
        "des", "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine", "als",
        "auch", "es", "an", "werden", "aus", "er", "hat", "dass", "sie", "nach",
        "wird", "bei", "einer", "um", "am", "sind", "noch", "wie", "einem", "über",
        "einen", "so", "zum", "haben", "nur", "oder", "aber", "vor", "zur", "bis"
    )
    
    private val scoreByWord: Map<String, Double> =
        words.mapIndexed { index, word -> word to (words.size - index).toDouble() }.toMap()
    
    private val knownWords: Set<String> = words.toHashSet()
    
    override fun suggestionsFor(input: String, previousWord: String?): List<Suggestion> {
        val prefix = input.lowercase()
        return words
            .filter { it.startsWith(prefix) }
            .map { Suggestion(it, scoreByWord.getValue(it)) }
            .sortedByDescending { it.score }
            .take(MAX_CANDIDATES)
    }
    
    override fun isKnownWord(word: String): Boolean {
        return knownWords.contains(word.lowercase())
    }
    
    override fun autocorrectFor(input: String, previousWord: String?): String? {
        val token = input.lowercase()
        if (token.length < 2 || isKnownWord(token)) {
            return null
        }
        return words
            .filter { it != token && EditDistance.atMostOne(token, it) }
            .maxByOrNull { scoreByWord.getValue(it) }
    }
    
    companion object {
        
        private const val MAX_CANDIDATES = 12
    }
}

package de.froehlichmedia.adaptkey.suggestion

/**
 * Placeholder suggestion source backed by a tiny built-in frequency list of common German words.
 *
 * This exists only to exercise the suggestion bar end to end; it is NOT the dictionary
 * infrastructure from the specification. The real personal n-gram model, blacklist and
 * part-of-speech data (SQLite) replace this in a later session behind the same
 * {@link SuggestionProvider} interface.
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
    
    override fun suggestionsFor(input: String): List<Suggestion> {
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
    
    override fun autocorrectFor(input: String): String? {
        val token = input.lowercase()
        if (token.length < 2 || isKnownWord(token)) {
            return null
        }
        return words
            .filter { levenshteinAtMostOne(token, it) }
            .maxByOrNull { scoreByWord.getValue(it) }
    }
    
    /**
     * @return true if [a] and [b] differ by at most one single-character edit (insert, delete, or substitute)
     */
    private fun levenshteinAtMostOne(a: String, b: String): Boolean {
        val lengthDiff = a.length - b.length
        if (lengthDiff < -1 || lengthDiff > 1) {
            return false
        }
        if (a.length == b.length) {
            var mismatches = 0
            for (i in a.indices) {
                if (a[i] != b[i] && ++mismatches > 1) {
                    return false
                }
            }
            return mismatches == 1
        }
        // Lengths differ by exactly one: check for a single insert/delete.
        val shorter = if (a.length < b.length) a else b
        val longer = if (a.length < b.length) b else a
        var i = 0
        var j = 0
        var edited = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++
                j++
            } else {
                if (edited) {
                    return false
                }
                edited = true
                j++
            }
        }
        return true
    }
    
    companion object {
        
        private const val MAX_CANDIDATES = 12
    }
}

package de.froehlichmedia.adaptkey.language

import kotlin.math.min

/**
 * Character n-gram extraction for language detection (A-03).
 *
 * The normalization here MUST stay byte-for-byte equivalent to the profile builder that generated
 * the `language_profiles.tsv` asset, otherwise the runtime n-grams would not line up with the stored
 * profiles. The contract is:
 *
 * 1. lowercase (Unicode default),
 * 2. every non-letter character becomes a single space (runs collapse),
 * 3. the result is trimmed and wrapped with one leading and one trailing space, so word-boundary
 *    n-grams (e.g. `" de"`, `"er "`) are captured.
 *
 * Profiles use bigrams and trigrams; a profile is the top-N n-grams by frequency, in rank order
 * (rank 0 = most frequent, ties broken by the n-gram ascending for determinism).
 */
object CharNgrams {
    
    private val NGRAM_SIZES = intArrayOf(2, 3)
    
    /**
     * Normalizes [text] per the contract above.
     *
     * @param text the raw text
     * @return the normalized string, always at least a single space
     */
    fun normalize(text: String): String {
        val builder = StringBuilder(text.length + 2)
        var prevSpace = false
        for (ch in text.lowercase()) {
            if (ch.isLetter()) {
                builder.append(ch)
                prevSpace = false
            } else if (!prevSpace) {
                builder.append(' ')
                prevSpace = true
            }
        }
        val trimmed = builder.toString().trim()
        return if (trimmed.isEmpty()) " " else " $trimmed "
    }
    
    /**
     * Counts every bigram and trigram of the normalized form of [text].
     *
     * @param text the raw text
     * @return a map from n-gram to occurrence count
     */
    fun counts(text: String): Map<String, Int> {
        val norm = normalize(text)
        val counts = HashMap<String, Int>()
        for (n in NGRAM_SIZES) {
            var i = 0
            val last = norm.length - n
            while (i <= last) {
                val gram = norm.substring(i, i + n)
                counts[gram] = (counts[gram] ?: 0) + 1
                i++
            }
        }
        return counts
    }
    
    /**
     * Builds the ranked n-gram profile of [text]: the most frequent n-grams first, capped at [size].
     * Ties are broken by the n-gram ascending, matching the profile builder.
     *
     * @param text the raw text
     * @param size the maximum number of n-grams to keep
     * @return the ranked n-grams, most frequent first
     */
    fun rankedProfile(text: String, size: Int): List<String> {
        val counts = counts(text)
        val sorted = counts.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        )
        return sorted.subList(0, min(size, sorted.size)).map { it.key }
    }
}

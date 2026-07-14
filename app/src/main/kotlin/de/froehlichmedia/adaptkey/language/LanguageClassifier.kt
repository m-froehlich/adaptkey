// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import kotlin.math.abs

/**
 * Pure on-device language classifier (A-03), tier 2 of the prediction stack.
 *
 * Greek is decided by script (see [ScriptDetector]); the Latin-script languages are ranked by the
 * classic Cavnar-Trenkle "out-of-place" distance between the input's ranked n-gram profile and each
 * bundled [CharNgramProfile]. The winner is the smallest distance; a [Result] also carries a
 * confidence derived from the relative gap to the runner-up, so callers can stay conservative on
 * short or ambiguous input.
 *
 * §43: a single word's own n-gram sample is too noisy to trust for this out-of-place comparison,
 * *regardless of how many n-grams that one word happens to produce* - closely-related Latin-script
 * languages share most common bigrams/trigrams, so one word's idiosyncratic ranking easily nudges the
 * distance past [minNgrams] while still being statistically meaningless. Confirmed empirically: with
 * the bundled profiles, isolated common German words like "Bitte", "heute", "Auto", "werden" and
 * "trinkst" - none of them short by [minNgrams]'s standard - classified as foreign on their own, but
 * every one resolved correctly the moment a second word joined them. [minWords] guards against this
 * directly, on top of (not instead of) [minNgrams].
 *
 * Everything here is deterministic and free of Android, so it is unit-tested directly and validated
 * against a held-out corpus in the evaluation suite. Detection never rewrites text; it only informs
 * whether German autocorrect should apply (the A-03 guard) and, later, per-language filtering.
 *
 * @param profiles the per-language n-gram profiles, typically parsed from the bundled asset
 * @param minNgrams the minimum number of input n-grams required to attempt a Latin-script decision;
 *        below this the input is too short and [Language.UNKNOWN] is returned
 * @param minWords the minimum number of whitespace-separated words required to attempt a Latin-script
 *        decision, independent of [minNgrams] - a single word is never enough (see above)
 * @param profileSize the profile length, also the per-n-gram penalty for an out-of-place miss
 */
class LanguageClassifier(
    private val profiles: Map<Language, CharNgramProfile>,
    private val minNgrams: Int = 8,
    private val minWords: Int = 2,
    private val greekThreshold: Double = 0.5,
    private val profileSize: Int = 200
) {
    
    /**
     * The outcome of a classification.
     *
     * @property language the most probable language, or [Language.UNKNOWN] when undecidable
     * @property confidence a `[0, 1]` separation measure: high when the winner clearly beat the rest,
     *           near zero when the top languages were almost tied
     */
    data class Result(val language: Language, val confidence: Double)
    
    /**
     * Classifies [text] into a language.
     *
     * @param text the text to classify
     * @return the detection result
     */
    fun classify(text: String): Result {
        if (ScriptDetector.greekFraction(text) >= greekThreshold) {
            return Result(Language.GREEK, 1.0)
        }
        if (profiles.isEmpty() || wordCount(text) < minWords) {
            return Result(Language.UNKNOWN, 0.0)
        }
        val ranked = CharNgrams.rankedProfile(text, profileSize)
        if (ranked.size < minNgrams) {
            return Result(Language.UNKNOWN, 0.0)
        }
        val distances = sortedDistances(ranked)
        val best = distances[0]
        val second = distances.getOrNull(1)
            ?: return Result(best.first, 1.0)
        // Confidence = how much better the winner is than the runner-up, relative to the runner-up.
        val confidence = ((second.second - best.second) / second.second).coerceIn(0.0, 1.0)
        return Result(best.first, confidence)
    }
    
    /**
     * Classifies only the last [wordWindow] words of [context] (A-03 works from the last 3-5 words),
     * so the decision follows the current writing rather than the whole field's history.
     *
     * @param context the text before (and optionally including) the current token
     * @param wordWindow how many trailing words to consider
     * @return the detection result for the recent context
     */
    fun classifyRecent(context: String, wordWindow: Int = 5): Result {
        return classify(lastWords(context, wordWindow))
    }
    
    /**
     * Whether [context] is confidently written in a language other than [Language.GERMAN] - the A-03
     * guard signal that German autocorrect should be held back. Deliberately conservative: unknown or
     * borderline input is treated as "not foreign", so autocorrect keeps working by default.
     *
     * Unlike a general confidence, this asks specifically whether German is a *poor* fit: it fires
     * only when some other language wins and German's out-of-place distance is at least [germanMargin]
     * worse than the winner's. That deliberately ignores how close the winner is to a related language
     * (e.g. Portuguese vs. Spanish) - irrelevant to the sole question "is this German?".
     *
     * §43: a lone word - even a longer one clearing [minNgrams] on its own - is never enough to fire
     * this; see [minWords] and the class doc.
     *
     * @param context the recent text
     * @param germanMargin how much worse German's distance must be than the winner's, as a fraction
     * @param wordWindow how many trailing words to consider
     * @return true only when the context is clearly not German
     */
    fun isForeign(context: String, germanMargin: Double = 0.15, wordWindow: Int = 5): Boolean {
        val text = lastWords(context, wordWindow)
        if (ScriptDetector.greekFraction(text) >= greekThreshold) {
            return true
        }
        if (wordCount(text) < minWords) {
            return false
        }
        val ranked = CharNgrams.rankedProfile(text, profileSize)
        if (profiles.isEmpty() || ranked.size < minNgrams) {
            return false
        }
        val distances = sortedDistances(ranked)
        val best = distances[0]
        if (best.first == Language.GERMAN) {
            return false
        }
        // Without a German profile there is nothing to compare against; stay conservative.
        val germanDistance = distances.firstOrNull { it.first == Language.GERMAN }?.second ?: return false
        return (germanDistance - best.second) / best.second >= germanMargin
    }
    
    private fun sortedDistances(ranked: List<String>): List<Pair<Language, Double>> {
        return profiles.entries
            .map { (language, profile) -> language to distance(ranked, profile) }
            .sortedBy { it.second }
    }
    
    private fun distance(ranked: List<String>, profile: CharNgramProfile): Double {
        var sum = 0L
        ranked.forEachIndexed { index, ngram ->
            val rank = profile.rankOf(ngram)
            sum += if (rank != null) abs(index - rank).toLong() else profileSize.toLong()
        }
        return sum.toDouble() / ranked.size
    }
    
    /** §43: the number of whitespace-separated words in [text], for the [minWords] guard. */
    private fun wordCount(text: String): Int {
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }
    
    companion object {
        
        /**
         * Extracts the last [count] whitespace-separated words of [text].
         *
         * @param text the source text
         * @param count how many trailing words to keep; a non-positive value yields an empty string
         * @return the trailing words rejoined with single spaces
         */
        fun lastWords(text: String, count: Int): String {
            if (count <= 0) {
                return ""
            }
            val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                return ""
            }
            return words.subList(maxOf(0, words.size - count), words.size).joinToString(" ")
        }
    }
}

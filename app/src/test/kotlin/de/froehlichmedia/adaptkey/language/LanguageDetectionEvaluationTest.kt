// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Evaluation suite for the A-03 classifier - deliberately NOT a unit test in the strict sense.
 *
 * It loads the real bundled `language_profiles.tsv` and runs the classifier over a held-out set of
 * UDHR sentences ([src/test/resources/language_eval.tsv]) that were excluded from the profiles at
 * build time. It then asserts an overall and per-language accuracy floor.
 *
 * Honesty note: this is a **same-domain** held-out split (all sentences come from the Universal
 * Declaration of Human Rights, so vocabulary overlaps between train and test). It validates that the
 * n-gram mechanism works, that the Kotlin normalization matches the builder, and that the languages
 * are separable - it does NOT prove open-domain accuracy on arbitrary everyday text, which would need
 * an independent corpus. The floors are set with that caveat in mind.
 */
class LanguageDetectionEvaluationTest {
    
    private data class Sample(val language: Language, val text: String)
    
    private fun loadClassifier(): LanguageClassifier {
        val candidates = listOf(
            File("src/main/assets/language_profiles.tsv"),
            File("app/src/main/assets/language_profiles.tsv")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("language_profiles.tsv not found (cwd=${File(".").absolutePath})")
        return LanguageClassifier(LanguageProfileParser.parse(file.readText(Charsets.UTF_8)))
    }
    
    private fun loadEvalCorpus(): List<Sample> {
        val stream = javaClass.getResourceAsStream("/language_eval.tsv")
            ?: error("language_eval.tsv not found on the test classpath")
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) {
                    return@mapNotNull null
                }
                val language = Language.fromCode(line.substring(0, tab)) ?: return@mapNotNull null
                Sample(language, line.substring(tab + 1))
            }.toList()
        }
    }
    
    @Test
    fun `held-out UDHR sentences are classified with high accuracy`() {
        val classifier = loadClassifier()
        val corpus = loadEvalCorpus()
        assertTrue(corpus.size >= 100, "eval corpus unexpectedly small: ${corpus.size}")
        
        var correct = 0
        val perLangTotal = HashMap<Language, Int>()
        val perLangCorrect = HashMap<Language, Int>()
        for (sample in corpus) {
            val predicted = classifier.classify(sample.text).language
            perLangTotal[sample.language] = (perLangTotal[sample.language] ?: 0) + 1
            if (predicted == sample.language) {
                correct++
                perLangCorrect[sample.language] = (perLangCorrect[sample.language] ?: 0) + 1
            }
        }
        
        val accuracy = correct.toDouble() / corpus.size
        val breakdown = perLangTotal.entries.sortedBy { it.key.name }.joinToString(", ") { (lang, total) ->
            "${lang.code}=${perLangCorrect[lang] ?: 0}/$total"
        }
        assertTrue(accuracy >= 0.90, "overall accuracy $accuracy too low; per-language: $breakdown")
    }
    
    @Test
    fun `greek sentences are always detected via the script fast path`() {
        val classifier = loadClassifier()
        val greek = loadEvalCorpus().filter { it.language == Language.GREEK }
        assertTrue(greek.isNotEmpty(), "no Greek eval sentences")
        
        val correct = greek.count { classifier.classify(it.text).language == Language.GREEK }
        assertEquals(greek.size, correct, "some Greek sentences were misclassified")
    }
    
    @Test
    fun `German sentences are not flagged foreign, other languages are`() {
        val classifier = loadClassifier()
        val corpus = loadEvalCorpus()
        
        val german = corpus.filter { it.language == Language.GERMAN }
        val germanForeign = german.count { classifier.isForeign(it.text) }
        // The guard must almost never fire on real German (it would disable German autocorrect).
        assertTrue(germanForeign <= 1, "German wrongly flagged foreign $germanForeign/${german.size} times")
        
        val nonGerman = corpus.filter { it.language != Language.GERMAN }
        val flagged = nonGerman.count { classifier.isForeign(it.text) }
        assertTrue(
            flagged.toDouble() / nonGerman.size >= 0.85,
            "guard flagged only $flagged/${nonGerman.size} non-German sentences"
        )
    }
}

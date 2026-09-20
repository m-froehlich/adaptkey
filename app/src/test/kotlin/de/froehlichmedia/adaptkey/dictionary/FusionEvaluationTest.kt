// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.GermanRules
import de.froehlichmedia.adaptkey.suggestion.Umlaut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

/**
 * D-477: evaluation of the cross-word fusion levels ([AutoMergeAggressiveness]) against the *real* bundled
 * German dictionary - deliberately not a unit test in the strict sense (compare
 * `LanguageDetectionEvaluationTest`). It answers the two questions the level constants exist to trade off:
 * how much of a realistic error population is recovered (recall, weighted by how often each word is typed -
 * a real spurious space hits a word roughly in proportion to its usage), and how often a fusion is applied
 * where it must not be (real two-word phrases, and unrelated token pairs that only coincidentally spell a
 * word once a letter is inserted).
 * 
 * The constants and the level design were chosen from exactly these measurements (see `AdaptKey-Progress.md`
 * D-477); the assertions below pin the *shape* with headroom, not the exact numbers, so a dictionary rebuild
 * does not fail them spuriously but a change that makes a level markedly unsafe or useless does. Skipped when
 * the `dictionaries/` tree is not reachable from the test's working directory.
 */
class FusionEvaluationTest {
    
    companion object {
        
        private const val CONNECTORS = "yxcvbnm"
        
        private lateinit var repair: TokenRepair
        private lateinit var store: InMemoryDictionaryStore
        private lateinit var words: List<WordEntry>
        private lateinit var common: List<String>
        private lateinit var foreign: List<String>
        private lateinit var realPhrases: List<Pair<String, String>>
        
        private fun find(vararg candidates: String): File? = candidates.map { File(it) }.firstOrNull { it.exists() }
        
        @JvmStatic
        @BeforeAll
        fun load() {
            val dict = find("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv")
            val bigram = find("../dictionaries/de/bigram.tsv", "dictionaries/de/bigram.tsv")
            assumeTrue(dict != null && bigram != null, "dictionaries/de not reachable from ${File(".").absolutePath}")
            store = InMemoryDictionaryStore()
            words = DictionaryAssetParser.parseWords(dict!!.readText(Charsets.UTF_8))
            words.forEach { store.putWord(it) }
            val bigrams = DictionaryAssetParser.parseBigrams(bigram!!.readText(Charsets.UTF_8))
            bigrams.forEach { store.putBigram(it.previousWord, it.word, it.count) }
            repair = TokenRepair(store, GermanRules, Umlaut)
            common = words.filter { it.word.length >= 2 && it.word.all { c -> c.isLetter() } }.take(300).map { it.word.lowercase() }
            val collected = ArrayList<String>()
            for (path in listOf("../dictionaries/fr/dict.tsv", "../dictionaries/es/dict.tsv", "src/main/assets/en/dict.tsv")) {
                val file = find(path, path.removePrefix("../")) ?: continue
                DictionaryAssetParser.parseWords(file.readText(Charsets.UTF_8))
                    .filter { it.frequency >= 30 && it.word.length in 3..9 && it.word.all { c -> c.isLetter() } }
                    .map { it.word.lowercase() }
                    .filter { !store.isKnownWord(it) }
                    .take(800)
                    .forEach { collected.add(it) }
            }
            foreign = collected
            realPhrases = bigrams
                .filter { it.count >= 50 && it.previousWord.all { c -> c.isLetter() } && it.word.all { c -> c.isLetter() } }
                .filter { store.isKnownWord(it.previousWord.lowercase()) && store.isKnownWord(it.word.lowercase()) }
                .map { it.previousWord.lowercase() to it.word.lowercase() }
        }
    }
    
    private val letters = ('a'..'z').map { it.toString() } + listOf("ä", "ö", "ü")
    
    /** How many pairs of [pairs] each level would silently fuse. */
    private fun accepted(pairs: Sequence<Pair<String, String>>): Map<AutoMergeAggressiveness, Int> {
        val counts = AutoMergeAggressiveness.entries.associateWith { 0 }.toMutableMap()
        for ((l, r) in pairs) {
            val candidate = repair.tryFuseAcrossSpace(l, r) ?: continue
            AutoMergeAggressiveness.entries.forEach { level -> if (level.accepts(candidate)) counts[level] = counts.getValue(level) + 1 }
        }
        return counts
    }
    
    private fun cross(left: List<String>, right: List<String>): Sequence<Pair<String, String>> =
        left.asSequence().flatMap { l -> right.asSequence().map { r -> l to r } }
    
    @Test
    fun `recall by usage weight grows with the level and the levels keep their promise`() {
        val recovered = AutoMergeAggressiveness.entries.associateWith { 0.0 }.toMutableMap()
        var total = 0.0
        for (w in words) {
            if (w.frequency < 10 || w.word.length < 4 || w.word.any { !it.isLetter() }) continue
            val lower = w.word.lowercase()
            val positions = (1..lower.length - 2).filter { lower[it] in CONNECTORS }
            if (positions.isEmpty()) continue
            val weight = w.frequency.toDouble() / positions.size
            for (i in positions) {
                val candidate = repair.tryFuseAcrossSpace(lower.substring(0, i), lower.substring(i + 1)) ?: continue
                if (candidate.fused != lower) continue
                total += weight
                AutoMergeAggressiveness.entries.forEach { level -> if (level.accepts(candidate)) recovered[level] = recovered.getValue(level) + weight }
            }
        }
        val recall = recovered.mapValues { it.value / total }
        println("D-477 usage-weighted recall: $recall")
        val cautious = recall.getValue(AutoMergeAggressiveness.CAUTIOUS)
        val medium = recall.getValue(AutoMergeAggressiveness.MEDIUM)
        val aggressive = recall.getValue(AutoMergeAggressiveness.AGGRESSIVE)
        assertTrue(cautious < medium && medium < aggressive, "recall must grow with the level: $recall")
        assertTrue(cautious >= 0.10, "cautious recovers too little: $cautious")
        assertTrue(medium >= 0.55, "medium recovers too little: $medium")
        assertTrue(aggressive >= 0.75, "aggressive recovers too little: $aggressive")
    }
    
    @Test
    fun `no level ever fuses a real, attested two-word phrase or a random pair of common words`() {
        val attested = accepted(realPhrases.asSequence())
        val randomCommon = accepted(cross(common, common))
        println("D-477 real attested phrases (${realPhrases.size}): $attested; random common pairs: $randomCommon")
        AutoMergeAggressiveness.entries.forEach { level ->
            assertEquals(0, attested.getValue(level), "$level fused a real attested phrase")
            assertEquals(0, randomCommon.getValue(level), "$level fused a random pair of common words")
        }
    }
    
    @Test
    fun `an unknown token next to a word, or two unknown tokens, almost never coincidentally fuse`() {
        val knownUnknown = accepted(cross(common.take(30), foreign))
        val unknownKnown = accepted(cross(foreign.take(500), common))
        val shuffled = foreign.shuffled(java.util.Random(7))
        val unknownUnknown = accepted(cross(shuffled.take(150), shuffled))
        println("D-477 coincidental fusions (known x unknown, ${30 * foreign.size} pairs): $knownUnknown; " +
            "(unknown x known, ${500 * common.size}): $unknownKnown; (unknown x unknown, ${150 * shuffled.size}): $unknownUnknown")
        val cautious = AutoMergeAggressiveness.CAUTIOUS
        assertEquals(0, knownUnknown.getValue(cautious) + unknownKnown.getValue(cautious) + unknownUnknown.getValue(cautious))
        val pairs = 30 * foreign.size + 500 * common.size + 150 * shuffled.size
        val mediumFalse = knownUnknown.getValue(AutoMergeAggressiveness.MEDIUM) + unknownKnown.getValue(AutoMergeAggressiveness.MEDIUM) +
            unknownUnknown.getValue(AutoMergeAggressiveness.MEDIUM)
        assertTrue(mediumFalse.toDouble() / pairs < 0.00005, "medium false-merge rate too high: $mediumFalse of $pairs")
        val aggressiveFalse = knownUnknown.getValue(AutoMergeAggressiveness.AGGRESSIVE) + unknownKnown.getValue(AutoMergeAggressiveness.AGGRESSIVE) +
            unknownUnknown.getValue(AutoMergeAggressiveness.AGGRESSIVE)
        assertTrue(aggressiveFalse.toDouble() / pairs < 0.0005, "aggressive false-merge rate too high: $aggressiveFalse of $pairs")
    }
    
    @Test
    fun `a single-letter token next to a word is the riskiest case and stays within its measured bound`() {
        val sets = listOf(cross(common, letters), cross(letters, common), cross(foreign.take(500), letters), cross(letters, foreign.take(500)))
        val results = sets.map { accepted(it) }
        val pairs = 2 * common.size * letters.size + 2 * 500 * letters.size
        val total = AutoMergeAggressiveness.entries.associateWith { level -> results.sumOf { it.getValue(level) } }
        println("D-477 single-letter coincidences over $pairs pairs: $total")
        assertTrue(total.getValue(AutoMergeAggressiveness.CAUTIOUS).toDouble() / pairs < 0.001, "cautious: $total")
        assertTrue(total.getValue(AutoMergeAggressiveness.MEDIUM).toDouble() / pairs < 0.005, "medium: $total")
        assertTrue(total.getValue(AutoMergeAggressiveness.AGGRESSIVE).toDouble() / pairs < 0.01, "aggressive: $total")
    }
    
    @Test
    fun `the reported case is merged at medium and aggressive, and at cautious only where neither part is a word`() {
        val candidate = repair.tryFuseAcrossSpace("Na", "hbarn")
        assertEquals("nachbarn", candidate?.fused)
        // "Na" is a real word, "hbarn" is not: exactly one unknown fragment.
        assertEquals(FusionClass.ONE_UNKNOWN, candidate?.fragments)
        assertTrue(AutoMergeAggressiveness.MEDIUM.accepts(candidate!!), AutoMergeAggressiveness.MEDIUM.rejection(candidate) ?: "")
        assertTrue(AutoMergeAggressiveness.AGGRESSIVE.accepts(candidate))
        assertTrue(!AutoMergeAggressiveness.CAUTIOUS.accepts(candidate))
    }
    
    @Test
    fun `au h becomes auch at medium - a one-letter fragment with a very frequent fused word`() {
        val candidate = repair.tryFuseAcrossSpace("au", "h")
        assertEquals("auch", candidate?.fused)
        assertTrue(candidate!!.oneLetterFragment)
        assertTrue(AutoMergeAggressiveness.MEDIUM.accepts(candidate), AutoMergeAggressiveness.MEDIUM.rejection(candidate) ?: "")
    }
}

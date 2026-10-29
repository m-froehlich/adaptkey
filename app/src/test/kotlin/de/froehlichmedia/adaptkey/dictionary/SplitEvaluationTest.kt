// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.GermanRules
import de.froehlichmedia.adaptkey.suggestion.Umlaut
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

/**
 * D-504: evaluation of the A-05 silent-split policy against the *real* bundled German dictionary -
 * deliberately not a unit test in the strict sense (compare [FusionEvaluationTest]). It answers the two
 * questions the D-504 constants exist to trade off: how much of a realistic missed-space population
 * (attested two-word phrases typed as one token) is still recovered silently, and how often an unknown
 * token that merely coincidentally concatenates two known words would be split silently where it must
 * not be.
 * 
 * The D-504 constants ([CorrectionConfidence.SPLIT_SILENT_HALF_FREQUENCY] = 300,
 * [CorrectionConfidence.SPLIT_AUTO_APPLY_CONFIDENCE] = 0.90) were chosen from the app's own precedent
 * bars (D-114's "trustworthy" frequency, CAUTIOUS's auto threshold), then validated here; the assertions
 * pin the *shape* with headroom, not the exact numbers, so a dictionary rebuild does not fail them
 * spuriously but a change that makes the policy markedly unsafe or useless does. Skipped when the
 * `dictionaries/` tree is not reachable from the test's working directory.
 */
class SplitEvaluationTest {
    
    companion object {
        
        private lateinit var repair: TokenRepair
        private lateinit var store: InMemoryDictionaryStore
        private lateinit var realPhrases: List<Triple<String, String, Long>>
        
        private fun find(vararg candidates: String): File? = candidates.map { File(it) }.firstOrNull { it.exists() }
        
        @JvmStatic
        @BeforeAll
        fun load() {
            val dict = find("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv")
            val bigram = find("../dictionaries/de/bigram.tsv", "dictionaries/de/bigram.tsv")
            assumeTrue(dict != null && bigram != null, "dictionaries/de not reachable from ${File(".").absolutePath}")
            store = InMemoryDictionaryStore()
            DictionaryAssetParser.parseWords(dict!!.readText(Charsets.UTF_8)).forEach { store.putWord(it) }
            val bigrams = DictionaryAssetParser.parseBigrams(bigram!!.readText(Charsets.UTF_8))
            bigrams.forEach { store.putBigram(it.previousWord, it.word, it.count) }
            repair = TokenRepair(store, GermanRules, Umlaut)
            realPhrases = bigrams
                .filter { it.count >= 50 }
                .filter { it.previousWord.length >= TokenRepair.MIN_PART && it.word.length >= TokenRepair.MIN_PART }
                .filter { it.previousWord.all { c -> c.isLetter() } && it.word.all { c -> c.isLetter() } }
                .filter { store.isKnownWord(it.previousWord.lowercase()) && store.isKnownWord(it.word.lowercase()) }
                .map { Triple(it.previousWord.lowercase(), it.word.lowercase(), it.count) }
        }
    }
    
    @Test
    fun `usage-weighted silent recall over joined attested phrases stays usable`() {
        var total = 0.0
        var silentAtTruePosition = 0.0
        var silentAtWrongPosition = 0.0
        var excludedAlreadyWords = 0
        for ((left, right, count) in realPhrases) {
            val joined = left + right
            // A join that is itself a dictionary word is never split (isAlreadyRecognised) - correctly
            // out of the mechanism's reach, and excluded from the population rather than counted as a miss.
            if (store.isKnownWord(joined)) {
                excludedAlreadyWords++
                continue
            }
            total += count
            val found = repair.trySplit(joined, emptySet()) ?: continue
            if (found.confidence < CorrectionConfidence.SPLIT_AUTO_APPLY_CONFIDENCE) continue
            if (found.left == left && found.right == right) {
                silentAtTruePosition += count
            } else {
                silentAtWrongPosition += count
            }
        }
        val recall = silentAtTruePosition / total
        println(
            "D-504 joined-attested-phrase population: ${realPhrases.size} phrases, total weight $total, " +
                "$excludedAlreadyWords joins are themselves words; usage-weighted silent recall $recall, " +
                "wrong-position silent share ${(silentAtWrongPosition / total)}"
        )
        assertTrue(recall >= 0.60, "usage-weighted silent recall too low: $recall")
        assertTrue(
            silentAtWrongPosition < silentAtTruePosition,
            "wrong-position silent splits must not dominate the true-position ones"
        )
    }
    
    @Test
    fun `on a broad-frequency random-pair population the silent gate visibly bites`() {
        // Population design note: pairs of the TOP-300 most frequent words are lexically INDISTINGUISHABLE
        // from a genuine missed space and are split silently by design (D-203) at a rate that D-504
        // deliberately does not touch - their halves all clear the floor, and A-07's undo is the recourse.
        // What D-504 changes is the broad-frequency population: unknown tokens whose coincidental halves
        // sit below the silent floor stay structurally splittable (chip/preview) but lose silent
        // application. This test measures exactly that bite: the silent subset must be strictly smaller
        // than the structural subset, and every silent split's halves must clear the floor.
        val sample = DictionaryAssetParser.parseWords(find("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv")!!.readText(Charsets.UTF_8))
            .filter { it.frequency >= 10 && it.word.length >= 2 && it.word.all { c -> c.isLetter() } }
            .map { it.word.lowercase() }
            .shuffled(java.util.Random(13))
            .take(1500)
        val rnd = java.util.Random(17)
        var checked = 0
        var structural = 0
        var silent = 0
        while (checked < 60_000) {
            val l = sample[rnd.nextInt(sample.size)]
            val r = sample[rnd.nextInt(sample.size)]
            if (l == r) continue
            val joined = l + r
            if (store.isKnownWord(joined)) continue
            checked++
            val found = repair.trySplit(joined, emptySet()) ?: continue
            structural++
            if (found.confidence >= CorrectionConfidence.SPLIT_AUTO_APPLY_CONFIDENCE) {
                silent++
                // The floor binds on the RESOLVED dictionary spellings (D-268) - those are what
                // forSplit scored; the typed half may differ ("fur" -> "für") and has no row of its own.
                val minHalf = minOf(store.frequencyOf(found.resolvedLeft), store.frequencyOf(found.resolvedRight))
                assertTrue(
                    minHalf >= CorrectionConfidence.SPLIT_SILENT_HALF_FREQUENCY,
                    "a silent split must have both halves at or above the floor, was $minHalf ($found)"
                )
            }
        }
        println(
            "D-504 broad random-pair population: $checked unknown joins, $structural split structurally, " +
                "$silent of those would apply silently (the rest are chip/preview only)"
        )
        assertTrue(structural > 0, "population produced no structural splits at all")
        assertTrue(silent < structural, "the silent floor must visibly bite on the broad population: $silent of $structural")
    }
    
    @Test
    fun `the reported D-493 junk-half case never applies silently`() {
        // "schmeißen" (absent from the dictionary) structurally splits into "sch" (84, OTHER - the junk
        // row the data round removes) + "meißen" (resolving to "Meißen", 352, NOUN). Both sit below
        // SPLIT_SILENT_HALF_FREQUENCY, so the confidence is exactly 0.0 and the split can only ever
        // surface as a chip/live preview - never a silent rewrite, at any aggressiveness level.
        val found = repair.trySplit("schmeißen", emptySet())
        assertTrue(
            found == null || found.confidence < CorrectionConfidence.SPLIT_AUTO_APPLY_CONFIDENCE,
            "the sch/Meißen-shaped split must never reach silent application, was ${found?.confidence}"
        )
        assertTrue(
            found == null || found.confidence == 0.0,
            "expected the floor to zero the confidence outright, was ${found?.confidence}"
        )
    }
}

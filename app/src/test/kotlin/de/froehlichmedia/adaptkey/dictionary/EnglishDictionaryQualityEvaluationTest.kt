// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.capitalisation.CapitalisationContext
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationEngine
import de.froehlichmedia.adaptkey.capitalisation.CapsMode
import de.froehlichmedia.adaptkey.language.EnglishRules
import de.froehlichmedia.adaptkey.language.GermanRules
import de.froehlichmedia.adaptkey.suggestion.KeyboardProximityQwerty
import de.froehlichmedia.adaptkey.suggestion.Umlaut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

/**
 * D-567/D-568/D-569/D-570/D-571/D-573/D-574 (the english-dictionary-quality plan, M9): evaluation of
 * the plan's outcomes against the *real* bundled English dictionary and the real German dictionary -
 * deliberately an evaluation in the `SplitEvaluationTest`/`FusionEvaluationTest`/
 * `JetztRankingEvaluationTest` sense, not strict unit tests. Each assertion pins an outcome that a
 * dictionary rebuild or a future data edit could silently undo, and whose undoing is exactly what
 * the plan's rounds were for. Skipped when the data trees are not reachable from the test's working
 * directory.
 */
class EnglishDictionaryQualityEvaluationTest {

    companion object {

        private lateinit var enStore: InMemoryDictionaryStore
        private lateinit var enProvider: DictionarySuggestionProvider
        private lateinit var deProvider: DictionarySuggestionProvider
        private lateinit var enFreq: Map<String, Long>
        private lateinit var deFreq: Map<String, Long>
        private lateinit var engine: CapitalisationEngine

        /**
         * D-569: the reviewed homograph whitelist - genuine English words kept on purpose (for them
         * the D-106 veto's suppression is its correct behaviour for English loans inside German
         * text). Lives in `dictionaries/en/veto_victims_whitelist.txt` as the single source of
         * truth, shared with `dictionaries/en/build_en_dict.py`'s verify. The victim census may
         * only ever contain whitelist words: any new victim is EN Rauschen deadening the veto
         * again and must fail here.
         */
        private var vetoVictimWhitelist: Set<String> = emptySet()

        /** D-568: the clean function-word top 25 the artefact sweep produced - pinned exactly. */
        private val PINNED_TOP_25 = listOf(
            "the", "of", "and", "in", "to", "is", "as", "was", "for", "by", "that", "with", "on",
            "from", "his", "are", "it", "an", "at", "he", "or", "which", "be", "were", "this",
        )

        private fun find(vararg candidates: String): File? =
            candidates.map { File(it) }.firstOrNull { it.exists() }

        @JvmStatic
        @BeforeAll
        fun load() {
            val enDict = find(
                "../app/src/main/assets/en/dict.tsv",
                "app/src/main/assets/en/dict.tsv",
                "src/main/assets/en/dict.tsv",
            )
            val deDict = find("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv")
            val deBigram = find("../dictionaries/de/bigram.tsv", "dictionaries/de/bigram.tsv")
            val whitelist = find(
                "../dictionaries/en/veto_victims_whitelist.txt",
                "dictionaries/en/veto_victims_whitelist.txt",
            )
            assumeTrue(
                enDict != null && deDict != null && deBigram != null && whitelist != null,
                "dictionary/whitelist data not reachable from ${File(".").absolutePath}",
            )
            vetoVictimWhitelist = whitelist!!
                .readText(Charsets.UTF_8)
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            enStore = InMemoryDictionaryStore()
            val enEntries = mutableListOf<WordEntry>()
            DictionaryAssetParser.parseWords(enDict!!.readText(Charsets.UTF_8)).forEach {
                enStore.putWord(it)
                enEntries.add(it)
            }
            enFreq = enEntries.associate { it.word to it.frequency }
            enProvider = DictionarySuggestionProvider(
                enStore,
                languageRules = EnglishRules,
                keyboardProximity = KeyboardProximityQwerty,
            )
            engine = CapitalisationEngine(enStore)

            val deStore = InMemoryDictionaryStore()
            DictionaryAssetParser.parseWords(deDict!!.readText(Charsets.UTF_8)).forEach {
                deStore.putWord(it)
            }
            // Lower-cased key map, mirroring the D-569 review generator exactly (de surfaces are
            // stored in their canonical casing, e.g. "Dur"/"mit").
            deFreq = DictionaryAssetParser.parseWords(deDict.readText(Charsets.UTF_8))
                .associate { it.word.lowercase() to it.frequency }
            DictionaryAssetParser.parseBigrams(deBigram!!.readText(Charsets.UTF_8))
                .forEach { deStore.putBigram(it.previousWord, it.word, it.count) }
            deProvider = DictionarySuggestionProvider(
                deStore,
                diacriticFolding = Umlaut,
                languageRules = GermanRules,
            )
        }

        private fun ctx(): CapitalisationContext = CapitalisationContext(
            explicitFirstUpper = false,
            sentenceStart = false,
            capsMode = CapsMode.NONE,
            afterHyphen = false,
        )
    }

    @Test
    fun `D-567 the two veto blockers stay gone from the English dictionary`() {
        assertFalse(enProvider.isKnownWord("mut"), "mut must stay removed (D-567)")
        assertFalse(enProvider.isKnownWord("dur"), "dur must stay removed (D-567)")
        assertFalse(enProvider.isKnownWord("muts"), "muts must stay removed (D-567)")
        // ... while the German correction targets stay intact on the German side.
        assertTrue(deFreq.containsKey("dir") && deFreq.containsKey("mit"))
    }

    @Test
    fun `D-567 the silent corrections fire on the German side - mut to mit, dur to dir`() {
        assertEquals("mit", deProvider.autocorrectFor("mut", null), "mut -> mit")
        assertEquals("dir", deProvider.autocorrectFor("dur", null), "dur -> dir")
    }

    @Test
    fun `D-568 the top 25 is the clean function-word list`() {
        val top = enFreq.entries.sortedByDescending { it.value }.take(25).map { it.key }
        assertEquals(PINNED_TOP_25, top, "top-25 drifted - check for artefact rows")
    }

    @Test
    fun `D-569 the veto victim census stays inside the reviewed whitelist`() {
        // D-569 removed every EN Rauschen victim; D-573's raises have meanwhile lifted 23 whitelist
        // members above the <= 20 band entirely (better than whitelist-equality - they are no
        // longer victims at all). The durable invariant: the census may only ever contain reviewed
        // genuine English words, never new Rauschen.
        val victims = enFreq.filter { (word, freq) ->
            freq <= 20 && deFreq[word.lowercase()]?.let { it >= 300 } == true
        }.keys
        assertTrue(
            victims.all { it in vetoVictimWhitelist },
            "new EN Rauschen victims appeared (not on the reviewed whitelist): "
                + victims.filterNot { it in vetoVictimWhitelist },
        )
    }

    @Test
    fun `D-571 dont reaches don't through the D-544 suggestion path`() {
        // The apostrophe insertion is an INDEL (cost 2), so the silent-autocorrect bar stays closed
        // by the cost model - exactly D-544's promise is the SUGGESTION path: "dont" reaches
        // "don't" as the bar's first chip, and "don" prefix-completes to it.
        val chips = enProvider.suggestionsFor(
            "dont", null, includeExpensiveFallbacks = true,
        ).map { it.word }
        assertEquals("don't", chips.firstOrNull(), "dont must chip to don't: $chips")
        val completions = enProvider.suggestionsFor(
            "don", null, includeExpensiveFallbacks = true,
        ).map { it.word }
        assertTrue("don't" in completions, "'don' must complete to don't: $completions")
    }

    @Test
    fun `D-573 the headline confusable pairs stay healed`() {
        // ratio <= 75 keeps every level's known-word override silent (AGGRESSIVE auto = 0.70).
        for ((rare, safe) in listOf("fir" to "for", "tic" to "tis", "wad" to "was")) {
            val rareFreq = enFreq[rare] ?: error("$rare missing from en/dict.tsv")
            val safeFreq = enFreq[safe] ?: error("$safe missing from en/dict.tsv")
            assertTrue(
                rareFreq >= safeFreq.toDouble() / 75.0,
                "$rare($rareFreq) drifted below $safe($safeFreq)/75 - the silent override re-opens",
            )
        }
    }

    @Test
    fun `D-574 china ottoman and sir stay lowercase while american and germans force-cap`() {
        assertEquals("china", engine.capitalise("china", ctx()), "the porcelain stays small")
        assertEquals("ottoman", engine.capitalise("ottoman", ctx()), "the furniture stays small")
        assertEquals("sir", engine.capitalise("sir", ctx()), "'Dear sir' stays small")
        assertEquals("American", engine.capitalise("american", ctx()), "the demonym forces up")
        assertEquals("Germans", engine.capitalise("germans", ctx()), "the demonym plural forces up")
    }
}

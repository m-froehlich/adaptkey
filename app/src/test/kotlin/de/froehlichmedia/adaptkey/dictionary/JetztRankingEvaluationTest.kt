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
 * D-545: evaluation of the user's own reported requirement ("Spätestens bei 'jetz' MUSS 'jetzt' da
 * stehen") against the *real* bundled German dictionary - deliberately not a unit test in the strict
 * sense (compare `SplitEvaluationTest`/`FusionEvaluationTest`). The word is the single most frequent
 * "jet*" entry by a wide margin, so the assertion pins a shape, not a tuned constant: as long as the
 * dictionary data or the prefix/fuzzy ranking never demotes it below the bar's first slot for its own
 * prefixes, this holds regardless of dictionary rebuilds; a regression that does demote it is exactly
 * what the user reported and must fail here. Skipped when the `dictionaries/` tree is not reachable from
 * the test's working directory.
 */
class JetztRankingEvaluationTest {

    companion object {

        private lateinit var store: InMemoryDictionaryStore
        private lateinit var provider: DictionarySuggestionProvider

        private fun find(vararg candidates: String): File? = candidates.map { File(it) }.firstOrNull { it.exists() }

        @JvmStatic
        @BeforeAll
        fun load() {
            val dict = find("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv")
            val bigram = find("../dictionaries/de/bigram.tsv", "dictionaries/de/bigram.tsv")
            assumeTrue(dict != null && bigram != null, "dictionaries/de not reachable from ${File(".").absolutePath}")
            store = InMemoryDictionaryStore()
            DictionaryAssetParser.parseWords(dict!!.readText(Charsets.UTF_8)).forEach { store.putWord(it) }
            DictionaryAssetParser.parseBigrams(bigram!!.readText(Charsets.UTF_8))
                .forEach { store.putBigram(it.previousWord, it.word, it.count) }
            provider = DictionarySuggestionProvider(store, diacriticFolding = Umlaut, languageRules = GermanRules)
        }

        private fun bar(input: String, previousWord: String? = null): List<String> =
            provider.suggestionsFor(input, previousWord, null, true) { false }.map { it.word }
    }

    @Test
    fun `jetzt is the first chip once the prefix identifies it - the user's MUSS requirement`() {
        // At "j"/"je" the wider je-family legitimately competes ("je" itself leads its own prefix), but
        // from the first unambiguous letter on the word must lead its own prefixes (the report's
        // "Spätestens bei 'jetz' MUSS 'jetzt' da stehen").
        for (prefix in listOf("jet", "jetz")) {
            assertEquals("jetzt", bar(prefix).first(), "first chip for \"$prefix\"")
        }
        // Earlier prefixes keep it prominently reachable instead.
        assertTrue("jetzt" in bar("je"), "\"jetzt\" stays in the bar for \"je\": " + bar("je"))
        assertTrue("jetzt" in bar("j"), "\"jetzt\" stays in the bar for \"j\": " + bar("j"))
    }

    @Test
    fun `jetzt keeps the first chip under real bigram context`() {
        // "jetzt in"/"bis jetzt"/"ist jetzt" are the bundled corpus's own strongest partners (64/54/48).
        for (previous in listOf("in", "bis", "ist", "auch", "genau")) {
            assertEquals("jetzt", bar("jet", previous).first(), "first chip for \"jet\" after \"$previous\"")
            assertEquals("jetzt", bar("jetz", previous).first(), "first chip for \"jetz\" after \"$previous\"")
        }
    }

    @Test
    fun `D-264 a learned Jetzt casing override never loses the lowercase word entirely`() {
        // The device state the report described: a personally promoted differently-cased "Jetzt" makes the
        // merged entry carry the learned casing everywhere (D-264), so the leading chip shows "Jetzt" - the
        // requirement is only that the lowercase spelling stays reachable (it returns through the fuzzy
        // path, whose INDEL_COST reach of "jetz" -> "jetzt" keeps the bundled entry in the candidate pool).
        repeat(40) { store.learn("Jetzt", null, null) }
        val bar = bar("jetz")
        assertEquals("Jetzt", bar.first(), "the learned casing wins the leading slot (D-264)")
        assertTrue("jetzt" in bar, "lowercase \"jetzt\" stays offered somewhere in the bar: $bar")
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.suggestion.Umlaut
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * D-306: verifies the actual bundled/repo `dict.tsv` data files (German's under `dictionaries/de/`,
 * English's under `assets/en/`, D-310) - not just the pure
 * parsing/split logic they are read through - so a data-quality regression (a missing/malformed
 * part-of-speech tag able to defeat [TokenRepair]'s own "not both nouns" split gate, exactly how the
 * mistagged "til" entry produced the reported "Tippstil" -> "Tipp"/"til" false-positive split) is caught
 * here rather than only rediscovered on a real device.
 */
class BundledDictionaryDataTest {
    
    private fun read(vararg candidates: String): String {
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("none of $candidates found (cwd=${File(".").absolutePath})")
        return file.readText(Charsets.UTF_8)
    }
    
    @Test
    fun `dict_de has no entries with a missing or malformed part-of-speech tag`() {
        val entries = DictionaryAssetParser.parseWords(read("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv"))
        val untagged = entries.filter { it.partsOfSpeech.isEmpty() }
        assertTrue(untagged.isEmpty(), "untagged entries slipped back in: ${untagged.take(10)}")
    }
    
    @Test
    fun `dict_en has no entries with a missing or malformed part-of-speech tag`() {
        val entries = DictionaryAssetParser.parseWords(read("src/main/assets/en/dict.tsv", "app/src/main/assets/en/dict.tsv"))
        val untagged = entries.filter { it.partsOfSpeech.isEmpty() }
        assertTrue(untagged.isEmpty(), "untagged entries slipped back in: ${untagged.take(10)}")
    }
    
    @Test
    fun `D-306 typing Tippstil no longer splits into Tipp and til against the real bundled German dictionary`() {
        val store = InMemoryDictionaryStore()
        DictionaryAssetParser.parseWords(read("../dictionaries/de/dict.tsv", "dictionaries/de/dict.tsv"))
            .forEach { store.putWord(it) }
        val repair = TokenRepair(store, diacriticFolding = Umlaut)
        
        // Index 4 is the 's' between "Tipp" and "til" - flagged space-ambiguous in the real device report.
        assertNull(repair.trySplit("Tippstil", setOf(4)))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * D-281: verifies the actual bundled/repo `hints_<code>.tsv` data files - not just the pure
 * [LetterHints] parsing core they are read through - so a transcription mistake in the data itself
 * (not the code) is caught here rather than only on a real device.
 */
class LanguageLetterHintsDataTest {
    
    private fun read(vararg candidates: String): String {
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("none of $candidates found (cwd=${File(".").absolutePath})")
        return file.readText(Charsets.UTF_8).trim()
    }
    
    @Test
    fun `hints_en matches the default German set minus the umlauts and ss`() {
        val parsed = LetterHints.parse(read("src/main/assets/en/hints.tsv", "app/src/main/assets/en/hints.tsv"))
        val expected = KeyboardLayout.DEFAULT_LETTER_HINTS - setOf('a', 'o', 'u', 's')
        assertEquals(expected, parsed)
    }
    
    @Test
    fun `hints_de matches the compiled-in default German set exactly`() {
        val parsed = LetterHints.parse(read("../dictionaries/de/hints.tsv", "dictionaries/de/hints.tsv"))
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, parsed)
    }
    
    @Test
    fun `D-497 hints_lt carries the reported e and u variants as multi-variant values`() {
        val parsed = LetterHints.parse(read("../dictionaries/lt/hints.tsv", "dictionaries/lt/hints.tsv"))
        // the community request: e pops up e/ę/ė and u pops up u/ų/ū - the base letter is the plain tap,
        // so the data carries both variants; everything else stays single-variant
        assertEquals("ę,ė", parsed['e'])
        assertEquals("ų,ū", parsed['u'])
    }
    
    @Test
    fun `D-497-followup every shipped pack hints file parses and carries its confirmed variant pairs`() {
        // the D-497-followup round touched 16 packs at once (lt + the 15 confirmed in the review table) -
        // one walk over ALL shipped hints.tsv catches a transcription/encoding mistake in any of them at
        // build time, not on a real device; the variant pairs below pin the round's actual content.
        val dictDir = File("../dictionaries").takeIf { it.isDirectory }
            ?: File("dictionaries").also { check(it.isDirectory) { "dictionaries dir not found" } }
        val files = dictDir.listFiles { f -> f.isDirectory && File(f, "hints.tsv").exists() }!!.sortedBy { it.name }
        check(files.isNotEmpty()) { "no hints.tsv found anywhere under ${dictDir.absolutePath}" }
        for (dir in files) {
            val parsed = LetterHints.parse(dir.resolve("hints.tsv").readText(Charsets.UTF_8).trim())
            check(parsed.isNotEmpty()) { "${dir.name}/hints.tsv parsed empty" }
            for ((key, value) in parsed) {
                check(LetterHints.isValidEntry(key, value)) { "${dir.name}: invalid entry $key=$value" }
            }
        }
        val expected = mapOf(
            "lt" to mapOf('e' to "ę,ė", 'u' to "ų,ū"),
            "cs" to mapOf('e' to "é,ě", 'u' to "ú,ů"),
            "sk" to mapOf('a' to "á,ä", 'o' to "ó,ô", 'l' to "ľ,ĺ"),
            "hr" to mapOf('c' to "č,ć"),
            "bs" to mapOf('c' to "č,ć"),
            "pl" to mapOf('z' to "ż,ź"),
            "et" to mapOf('o' to "õ,ö"),
            "hu" to mapOf('o' to "ó,ö,ő", 'u' to "ú,ü,ű"),
            "ro" to mapOf('a' to "ă,â"),
            "da" to mapOf('a' to "å,æ"),
            "nb" to mapOf('a' to "å,æ"),
            "sv" to mapOf('a' to "ä,å"),
            "fi" to mapOf('a' to "ä,å"),
            "pt" to mapOf('a' to "á,ã,â,à", 'e' to "é,ê", 'o' to "ó,õ,ô"),
            "it" to mapOf('e' to "è,é", 'o' to "ò,ó", 'u' to "ù,ú"),
            "fr" to mapOf('a' to "à,â", 'e' to "é,è,ê,ë", 'i' to "î,ï", 'u' to "ù,û")
        )
        for ((code, pairs) in expected) {
            val dir = files.firstOrNull { it.name == code }
                ?: error("expected pack '$code' missing from dictionaries/")
            val parsed = LetterHints.parse(dir.resolve("hints.tsv").readText(Charsets.UTF_8).trim())
            for ((key, value) in pairs) {
                assertEquals(value, parsed[key], "$code: variant pair for '$key'")
            }
        }
    }
}

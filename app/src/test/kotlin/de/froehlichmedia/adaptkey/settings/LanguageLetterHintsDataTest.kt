// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `hints_en is the removal delta that keeps every German umlaut out (section 210 policy)`() {
        val delta = LetterHints.parseDelta(read("src/main/assets/en/hints.tsv", "app/src/main/assets/en/hints.tsv"))
        // D-513: English's whole delta is four explicit removals - the base stands otherwise (d $, n tilde-n
        // with its paren left corner, m's paren, s's star), and the umlauts/ß never reach the English page.
        assertEquals(setOf('a', 'o', 'u', 's'), delta.keys)
        assertTrue(delta.values.all { it == null })
    }
    
    @Test
    fun `hints_de matches the reformed compiled-in base map exactly`() {
        val parsed = LetterHints.parse(read("../dictionaries/de/hints.tsv", "dictionaries/de/hints.tsv"))
        // D-512/D-513: the curated German reference pack IS the reform's base map (d $, n tilde-n, c/v/b/m
        // freed - their corners live in code-side BASE_LEFT_CORNERS now, not in the hints data).
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, parsed)
    }
    
    @Test
    fun `D-513 union merge over the real packs reproduces section 5's verified table`() {
        val base = KeyboardLayout.DEFAULT_LETTER_HINTS
        fun merged(code: String): Map<Char, String> =
            LetterHints.unionMerge(base, LetterHints.parseDelta(read("../dictionaries/$code/hints.tsv", "dictionaries/$code/hints.tsv")))
        
        // Scandinavian primary-in-corner stays exactly as today (section 5, first bullet).
        assertEquals("ä,å", merged("sv")['a'])
        assertEquals("ö", merged("sv")['o'])
        assertEquals("ä,å", merged("fi")['a'])
        assertEquals("ö", merged("fi")['o'])
        assertEquals("å,æ", merged("da")['a'])
        assertEquals("ø", merged("da")['o'])
        assertEquals("å,æ", merged("nb")['a'])
        assertEquals("ø", merged("nb")['o'])
        // Turkish keeps every own letter; the German umlauts never append over an owned key.
        assertEquals("ı", merged("tr")['i'])
        assertEquals("ö", merged("tr")['o'])
        assertEquals("ü", merged("tr")['u'])
        assertEquals("ç", merged("tr")['c'])
        assertEquals("ğ", merged("tr")['g'])
        assertEquals("ş", merged("tr")['s'])
        // hu/pt lead variants kept exactly as the D-497-followup round fixed them.
        assertEquals("ó,ö,ő", merged("hu")['o'])
        assertEquals("ú,ü,ű", merged("hu")['u'])
        assertEquals("á,ã,â,à", merged("pt")['a'])
        assertEquals("ó,õ,ô", merged("pt")['o'])
        // section 5's two pinned merged orders.
        assertEquals("é,è,ê,ë,€", merged("fr")['e'])
        assertEquals("ń,ñ", merged("pl")['n'])
        assertEquals("ň,ñ", merged("cs")['n'])
        assertEquals("ņ,ñ", merged("lv")['n'])
        // The n-owners' letters stay the release default; tilde-n is the first extra.
        assertEquals("ń", merged("pl")['n']!!.split(',')[0])
        assertEquals("ň", merged("cs")['n']!!.split(',')[0])
        assertEquals("ņ", merged("lv")['n']!!.split(',')[0])
    }
    
    @Test
    fun `D-513 the reform's retirement drops are gone from the merged maps`() {
        val base = KeyboardLayout.DEFAULT_LETTER_HINTS
        fun merged(code: String): Map<Char, String> =
            LetterHints.unionMerge(base, LetterHints.parseDelta(read("../dictionaries/$code/hints.tsv", "dictionaries/$code/hints.tsv")))
        
        // The tilde now lives on 1-left (section 3b) - no pack may resurrect it on l.
        listOf("da", "nb", "sv", "fi", "cs", "hr", "hu", "id", "lt", "ms", "ro", "sw", "tl").forEach { code ->
            assertNull(merged(code)['l'], "$code still carries an l entry")
        }
        // es's l=tilde-n oddity died; fr's w=scatter-hint retired (section 2.8).
        assertNull(merged("es")['l'])
        assertNull(merged("fr")['w'])
        // The freed keys carry no right corner from any pack (their left corners are code-side).
        listOf("da", "fi", "fr", "hu", "pl", "sk", "tr", "cs").forEach { code ->
            assertNull(merged(code)['m'], "$code still carries an m entry")
            assertNull(merged(code)['v'], "$code still carries a v entry")
        }
    }
    
    @Test
    fun `D-513 the union merge resolves the bottom-row slot collisions per the plan's slot rule`() {
        val base = KeyboardLayout.DEFAULT_LETTER_HINTS
        fun merged(code: String): Map<Char, String> =
            LetterHints.unionMerge(base, LetterHints.parseDelta(read("../dictionaries/$code/hints.tsv", "dictionaries/$code/hints.tsv")))
        
        // The + slot keys (QWERTY z / QWERTZ y) keep their language letter as the right corner.
        assertEquals("ż,ź", merged("pl")['z'])
        assertEquals("ž", merged("cs")['z'])
        assertEquals("ž", merged("sk")['z'])
        assertEquals("±", merged("tr")['z'])
        assertEquals("¶", merged("da")['z'])
        assertEquals("ý", merged("cs")['y'])
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

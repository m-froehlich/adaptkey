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
        val parsed = LetterHints.parse(read("src/main/assets/hints_en.tsv", "app/src/main/assets/hints_en.tsv"))
        val expected = KeyboardLayout.DEFAULT_LETTER_HINTS - setOf('a', 'o', 'u', 's')
        assertEquals(expected, parsed)
    }
    
    @Test
    fun `hints_de matches the compiled-in default German set exactly`() {
        val parsed = LetterHints.parse(read("../dictionaries/de/hints_de.tsv", "dictionaries/de/hints_de.tsv"))
        assertEquals(KeyboardLayout.DEFAULT_LETTER_HINTS, parsed)
    }
}

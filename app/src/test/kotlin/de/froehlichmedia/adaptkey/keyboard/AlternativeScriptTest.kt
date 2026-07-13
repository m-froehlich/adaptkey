// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlternativeScriptTest {
    
    @Test
    fun `a German umlaut extends the word outside Greek mode`() {
        assertTrue(AlternativeScript.extendsWord("ä", activeLanguageIsGreek = false))
    }
    
    @Test
    fun `pi picked from the Latin keyboard does not extend the word`() {
        assertFalse(AlternativeScript.extendsWord("π", activeLanguageIsGreek = false))
    }
    
    @Test
    fun `every math-symbol popup letter is rejected outside Greek mode`() {
        for (symbol in listOf("π", "α", "β", "γ", "δ", "λ", "ω")) {
            assertFalse(AlternativeScript.extendsWord(symbol, activeLanguageIsGreek = false))
        }
    }
    
    @Test
    fun `a Greek accented vowel extends the word while actually in Greek mode`() {
        assertTrue(AlternativeScript.extendsWord("ά", activeLanguageIsGreek = true))
    }
    
    @Test
    fun `pi would even extend the word in Greek mode, since it is then genuine Greek text`() {
        assertTrue(AlternativeScript.extendsWord("π", activeLanguageIsGreek = true))
    }
    
    @Test
    fun `a punctuation mark never extends the word`() {
        assertFalse(AlternativeScript.extendsWord(".", activeLanguageIsGreek = false))
        assertFalse(AlternativeScript.extendsWord(".", activeLanguageIsGreek = true))
    }
    
    @Test
    fun `an empty symbol never extends the word`() {
        assertFalse(AlternativeScript.extendsWord("", activeLanguageIsGreek = false))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for D-320: [PunctuationSpaceGlue] decides whether a digit typed right after an A-12
 * sentence-punctuation auto-space should glue onto the punctuation (a decimal number) instead of
 * confirming the auto-space as ordinary text.
 */
class PunctuationSpaceGlueTest {
    
    @Test
    fun `a digit after a period-space that itself follows a digit glues`() {
        assertTrue(PunctuationSpaceGlue.gluesDigit("3. "))
    }
    
    @Test
    fun `a digit after a comma-space that itself follows a digit glues`() {
        assertTrue(PunctuationSpaceGlue.gluesDigit("3, "))
    }
    
    @Test
    fun `a longer preceding context still only looks at the last three characters`() {
        assertTrue(PunctuationSpaceGlue.gluesDigit("Kapitel 3. "))
    }
    
    @Test
    fun `a period following a letter does not glue`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit("Hallo. "))
    }
    
    @Test
    fun `an exclamation mark never glues even after a digit`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit("3! "))
    }
    
    @Test
    fun `a question mark never glues even after a digit`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit("3? "))
    }
    
    @Test
    fun `missing trailing space does not glue`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit("3.5"))
    }
    
    @Test
    fun `text shorter than three characters never glues`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit(". "))
    }
    
    @Test
    fun `empty text never glues`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit(""))
    }
    
    @Test
    fun `D-410 a comma still glues by default, matching this app's historical German-only behaviour`() {
        assertTrue(PunctuationSpaceGlue.gluesDigit("3, "))
    }
    
    @Test
    fun `D-410 a comma does not glue when includeComma is false`() {
        assertFalse(PunctuationSpaceGlue.gluesDigit("3, ", includeComma = false))
    }
    
    @Test
    fun `D-410 a period still glues when includeComma is false`() {
        assertTrue(PunctuationSpaceGlue.gluesDigit("3. ", includeComma = false))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-3 continuation parser.
 */
class Tier3ResponseParserTest {
    
    @Test
    fun `a letter-initial continuation completes the current token`() {
        val result = Tier3ResponseParser.parse("wor", "ld is round", maxCandidates = 3)
        assertEquals("world", result.first().word)
    }
    
    @Test
    fun `a boundary-initial continuation treats the token as finished`() {
        // The current token "wie" is complete; the model predicts the next words.
        val result = Tier3ResponseParser.parse("wie", " geht es", maxCandidates = 3)
        assertEquals(listOf("geht", "es"), result.map { it.word })
    }
    
    @Test
    fun `candidates are ranked by descending confidence`() {
        val result = Tier3ResponseParser.parse("", "eins zwei drei", maxCandidates = 3)
        assertEquals(listOf("eins", "zwei", "drei"), result.map { it.word })
        assertTrue(result[0].confidence > result[1].confidence)
        assertTrue(result[1].confidence > result[2].confidence)
    }
    
    @Test
    fun `candidates are de-duplicated and capped`() {
        val result = Tier3ResponseParser.parse("", "und und oder und", maxCandidates = 5)
        assertEquals(listOf("und", "oder"), result.map { it.word })
    }
    
    @Test
    fun `the candidate count is capped to maxCandidates`() {
        val result = Tier3ResponseParser.parse("", "a b c d e", maxCandidates = 2)
        assertEquals(listOf("a", "b"), result.map { it.word })
    }
    
    @Test
    fun `zero maxCandidates yields nothing`() {
        assertTrue(Tier3ResponseParser.parse("wor", "ld", maxCandidates = 0).isEmpty())
    }
    
    @Test
    fun `a negative maxCandidates is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Tier3ResponseParser.parse("x", "y", maxCandidates = -1) }
    }
}

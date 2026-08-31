// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-404 family-learning prompt builder. */
class Tier3FamilyPromptTest {
    
    @Test
    fun `prompt names the requested word`() {
        val prompt = Tier3FamilyPrompt.build(Tier3FamilyRequest("Hund"))
        assertTrue(prompt.contains("Hund"))
    }
    
    @Test
    fun `prompt primes the answer with the lemma key, ready for the model to continue`() {
        val prompt = Tier3FamilyPrompt.build(Tier3FamilyRequest("Hund"))
        assertTrue(prompt.trimEnd('\n').endsWith("${Tier3FamilyPrompt.KEY_LEMMA}="))
    }
    
    @Test
    fun `prompt mentions every category token so the model has a closed set to answer with`() {
        val prompt = Tier3FamilyPrompt.build(Tier3FamilyRequest("Hund"))
        for (token in listOf("NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER")) {
            assertTrue(prompt.contains(token), "expected prompt to mention $token")
        }
    }
    
    @Test
    fun `a given sentence context distinct from the bare word is included`() {
        val prompt = Tier3FamilyPrompt.build(Tier3FamilyRequest("Hund", sentence = "Der Hund bellt laut"))
        assertTrue(prompt.contains("Der Hund bellt laut"))
    }
    
    @Test
    fun `an empty sentence context is omitted, not rendered as an empty context line`() {
        val prompt = Tier3FamilyPrompt.build(Tier3FamilyRequest("Hund", sentence = ""))
        assertFalse(prompt.contains("Satz:"))
    }
    
    @Test
    fun `a sentence context identical to the bare word alone is omitted as uninformative`() {
        val prompt = Tier3FamilyPrompt.build(Tier3FamilyRequest("Hund", sentence = "Hund"))
        assertFalse(prompt.contains("Satz:"))
    }
}

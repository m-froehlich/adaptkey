// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-115 / D-125 regular-verb-inflection recognition. */
class RegularVerbInflectionTest {
    
    private fun knownInfinitives(vararg words: String): (String) -> Boolean {
        val set = words.toSet()
        return { word -> set.contains(word) }
    }
    
    @Test
    fun `du-form present tense is recognised via the -st ending`() {
        assertTrue(RegularVerbInflection.isPlausibleInflection("beurteilst", knownInfinitives("beurteilen")))
    }
    
    @Test
    fun `ich-form present tense is recognised via the -e ending`() {
        assertTrue(RegularVerbInflection.isPlausibleInflection("kaufe", knownInfinitives("kaufen")))
    }
    
    @Test
    fun `er-ihr present tense is recognised via the -t ending`() {
        assertTrue(RegularVerbInflection.isPlausibleInflection("kauft", knownInfinitives("kaufen")))
    }
    
    @Test
    fun `preterite forms are recognised via -te -test -ten -tet`() {
        val known = knownInfinitives("kaufen")
        assertTrue(RegularVerbInflection.isPlausibleInflection("kaufte", known))
        assertTrue(RegularVerbInflection.isPlausibleInflection("kauftest", known))
        assertTrue(RegularVerbInflection.isPlausibleInflection("kauften", known))
        assertTrue(RegularVerbInflection.isPlausibleInflection("kauftet", known))
    }
    
    @Test
    fun `a dental stem's inserted -e is recognised via -est and -et`() {
        assertTrue(RegularVerbInflection.isPlausibleInflection("redest", knownInfinitives("reden")))
        assertTrue(RegularVerbInflection.isPlausibleInflection("redet", knownInfinitives("reden")))
    }
    
    @Test
    fun `no match when no ending reconstructs a known infinitive`() {
        assertFalse(RegularVerbInflection.isPlausibleInflection("xyzst", knownInfinitives("beurteilen")))
    }
    
    @Test
    fun `a token no longer than the ending itself never matches`() {
        // "st" alone would strip to an empty stem ("" + "en" = "en") - must not match even if "en" itself
        // happens to be known.
        assertFalse(RegularVerbInflection.isPlausibleInflection("st", knownInfinitives("en")))
    }
}

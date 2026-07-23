// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-252 regular-adjective-comparative/superlative recognition. */
class AdjectiveInflectionTest {
    
    private fun knownPositives(vararg words: String): (String) -> Boolean {
        val set = words.toSet()
        return { word -> set.contains(word) }
    }
    
    @Test
    fun `the reported regression - comparative -er is recognised via a known positive`() {
        assertTrue(AdjectiveInflection.isPlausibleComparative("zuversichtlicher", knownPositives("zuversichtlich")))
    }
    
    @Test
    fun `declined comparative forms are recognised via -ere -eren -erem -erer -eres`() {
        val known = knownPositives("schön")
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönere", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schöneren", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönerem", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönerer", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schöneres", known))
    }
    
    @Test
    fun `superlative forms are recognised via -ste -sten -stem -ster -stes`() {
        val known = knownPositives("schön")
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönste", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönsten", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönstem", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönster", known))
        assertTrue(AdjectiveInflection.isPlausibleComparative("schönstes", known))
    }
    
    @Test
    fun `no match when no ending reconstructs a known positive`() {
        assertFalse(AdjectiveInflection.isPlausibleComparative("xyzer", knownPositives("zuversichtlich")))
    }
    
    @Test
    fun `an umlaut-mutating comparative is deliberately out of scope`() {
        // "älter" would need "alt" (umlaut-mutated), not "ält" - the regular, non-mutating case only.
        assertFalse(AdjectiveInflection.isPlausibleComparative("älter", knownPositives("alt")))
    }
    
    @Test
    fun `a token no longer than the ending itself never matches`() {
        // "er" alone would strip to an empty stem - must not match even if "" itself happens to be known.
        assertFalse(AdjectiveInflection.isPlausibleComparative("er", knownPositives("")))
    }
}

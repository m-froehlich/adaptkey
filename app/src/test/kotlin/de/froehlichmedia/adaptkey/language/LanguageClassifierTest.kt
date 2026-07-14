// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the deterministic mechanics of the A-03 classifier, using small profiles derived
 * from disjoint sample text so the argmin, the UNKNOWN fallbacks and the guard can be checked without
 * depending on the bundled asset (that is exercised end-to-end by the evaluation suite).
 */
class LanguageClassifierTest {
    
    private fun profileFrom(language: Language, sample: String): CharNgramProfile {
        return CharNgramProfile(language, CharNgrams.rankedProfile(sample, 200))
    }
    
    private fun twoLanguageClassifier(): LanguageClassifier {
        val german = profileFrom(Language.GERMAN, "der die das und ein eine ist haben werden auch")
        val english = profileFrom(Language.ENGLISH, "the quick brown fox jumps over lazy dog with love")
        return LanguageClassifier(mapOf(Language.GERMAN to german, Language.ENGLISH to english))
    }
    
    @Test
    fun `greek script wins via the fast path even without profiles`() {
        val classifier = LanguageClassifier(emptyMap())
        
        val result = classifier.classify("Καλημέρα, τι κάνεις σήμερα")
        
        assertEquals(Language.GREEK, result.language)
        assertEquals(1.0, result.confidence, 1e-9)
    }
    
    @Test
    fun `empty profiles yield UNKNOWN for latin text`() {
        val classifier = LanguageClassifier(emptyMap())
        
        assertEquals(Language.UNKNOWN, classifier.classify("hello there friend").language)
    }
    
    @Test
    fun `text below the minimum ngram count is UNKNOWN`() {
        assertEquals(Language.UNKNOWN, twoLanguageClassifier().classify("a").language)
    }
    
    @Test
    fun `paragraph 43 a single word is never enough to decide foreign, even one that would otherwise clearly win`() {
        val permissive = LanguageClassifier(
            mapOf(
                Language.GERMAN to profileFrom(Language.GERMAN, "der die das und ein eine ist haben werden auch"),
                Language.ENGLISH to profileFrom(Language.ENGLISH, "the quick brown fox jumps over lazy dog with love")
            ),
            minWords = 1
        )
        // With the old single-word-permitting gate, "over" (literally in the English training text) confidently
        // wins English and fires isForeign - proving the underlying risk a lone word poses is real, not
        // hypothetical: the same statistical noise misclassified genuinely German words in production (§43).
        assertEquals(Language.ENGLISH, permissive.classify("over").language)
        assertTrue(permissive.isForeign("over"))
        
        // The default minWords = 2 blocks the exact same single word outright.
        val guarded = twoLanguageClassifier()
        assertEquals(Language.UNKNOWN, guarded.classify("over").language)
        assertFalse(guarded.isForeign("over"))
    }
    
    @Test
    fun `paragraph 43 a second word is enough to lift the minWords guard`() {
        assertTrue(twoLanguageClassifier().isForeign("quick over"))
    }
    
    @Test
    fun `the closest profile wins`() {
        val classifier = twoLanguageClassifier()
        
        assertEquals(Language.GERMAN, classifier.classify("der die das und ein ist haben").language)
        assertEquals(Language.ENGLISH, classifier.classify("the quick brown fox jumps over").language)
    }
    
    @Test
    fun `isForeign flags a clearly non-German context and spares German`() {
        val classifier = twoLanguageClassifier()
        
        assertTrue(classifier.isForeign("the quick brown fox jumps over"))
        assertFalse(classifier.isForeign("der die das und ein ist haben"))
    }
    
    @Test
    fun `isForeign treats Greek as foreign and short input as native`() {
        val classifier = twoLanguageClassifier()
        
        assertTrue(classifier.isForeign("Καλημέρα τι κάνεις"))
        assertFalse(classifier.isForeign("das"))
    }
    
    @Test
    fun `classifyRecent judges only the trailing words of the context`() {
        val classifier = twoLanguageClassifier()
        
        // A long German lead-in followed by an English tail: the recent window decides English.
        val context = "der die das und ein ist haben the quick brown fox jumps over"
        assertEquals(Language.ENGLISH, classifier.classifyRecent(context, wordWindow = 6).language)
    }
    
    @Test
    fun `lastWords keeps only the trailing words`() {
        assertEquals("d e f", LanguageClassifier.lastWords("a b c d e f", 3))
        assertEquals("x", LanguageClassifier.lastWords("x", 5))
        assertEquals("", LanguageClassifier.lastWords("   ", 5))
        assertEquals("", LanguageClassifier.lastWords("a b c", 0))
    }
}

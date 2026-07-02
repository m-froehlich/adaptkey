// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the real-dictionary asset parser.
 */
class DictionaryAssetParserTest {
    
    @Test
    fun `parses words with frequency and POS in file order`() {
        val raw = "Haus\t400\tNOUN\ndas\t970\tOTHER\nmorgen\t320\tNOUN,OTHER"
        
        val words = DictionaryAssetParser.parseWords(raw)
        
        assertEquals(3, words.size)
        assertEquals(WordEntry("Haus", 400, setOf(PartOfSpeech.NOUN)), words[0])
        assertEquals(WordEntry("das", 970, setOf(PartOfSpeech.OTHER)), words[1])
        assertEquals(setOf(PartOfSpeech.NOUN, PartOfSpeech.OTHER), words[2].partsOfSpeech)
    }
    
    @Test
    fun `an empty POS field yields no tags`() {
        val words = DictionaryAssetParser.parseWords("wort\t12\t")
        
        assertEquals(1, words.size)
        assertTrue(words[0].partsOfSpeech.isEmpty())
    }
    
    @Test
    fun `a word line without a POS column is allowed`() {
        val words = DictionaryAssetParser.parseWords("wort\t12")
        
        assertEquals(WordEntry("wort", 12, emptySet()), words[0])
    }
    
    @Test
    fun `blank lines, bad frequencies and unknown POS names are skipped`() {
        val raw = "\ngut\tNOTANUMBER\tADJECTIVE\nHaus\t400\tNOUN,BOGUS\r"
        
        val words = DictionaryAssetParser.parseWords(raw)
        
        assertEquals(1, words.size)
        assertEquals(setOf(PartOfSpeech.NOUN), words[0].partsOfSpeech)
    }
    
    @Test
    fun `parses bigrams`() {
        val bigrams = DictionaryAssetParser.parseBigrams("der\tHund\t50\ndie\tKatze\t45\r")
        
        assertEquals(2, bigrams.size)
        assertEquals(DictionaryAssetParser.Bigram("der", "Hund", 50), bigrams[0])
        assertEquals(DictionaryAssetParser.Bigram("die", "Katze", 45), bigrams[1])
    }
    
    @Test
    fun `malformed bigram lines are skipped`() {
        val bigrams = DictionaryAssetParser.parseBigrams("der\tHund\nonlyone\ndie\tKatze\tNAN\nab\tcd\t7")
        
        assertEquals(1, bigrams.size)
        assertEquals(DictionaryAssetParser.Bigram("ab", "cd", 7), bigrams[0])
    }
    
    @Test
    fun `empty input yields empty lists`() {
        assertTrue(DictionaryAssetParser.parseWords("").isEmpty())
        assertTrue(DictionaryAssetParser.parseBigrams("").isEmpty())
    }
}

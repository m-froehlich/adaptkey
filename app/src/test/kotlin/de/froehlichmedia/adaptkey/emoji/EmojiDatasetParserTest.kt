// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.emoji

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure emoji dataset asset parser (L-03).
 */
class EmojiDatasetParserTest {
    
    @Test
    fun `parses category-tab-emoji lines into the dataset in file order`() {
        val raw = "SMILEYS_PEOPLE\t😀\nSMILEYS_PEOPLE\t😄\nANIMALS_NATURE\t🐻"
        
        val dataset = EmojiDatasetParser.parse(raw)
        
        assertEquals(listOf("😀", "😄"), dataset.byCategory[EmojiCategory.SMILEYS_PEOPLE])
        assertEquals(listOf("🐻"), dataset.byCategory[EmojiCategory.ANIMALS_NATURE])
    }
    
    @Test
    fun `every category is present even without an entry`() {
        val dataset = EmojiDatasetParser.parse("SMILEYS_PEOPLE\t😀")
        
        assertTrue(dataset.byCategory.containsKey(EmojiCategory.SYMBOLS))
        assertEquals(emptyList<String>(), dataset.byCategory[EmojiCategory.SYMBOLS])
    }
    
    @Test
    fun `blank lines and unknown categories are skipped without failing`() {
        val raw = "\nNOT_A_CATEGORY\t🙂\n\nSMILEYS_PEOPLE\t😀\n"
        
        val dataset = EmojiDatasetParser.parse(raw)
        
        assertEquals(listOf("😀"), dataset.byCategory[EmojiCategory.SMILEYS_PEOPLE])
    }
    
    @Test
    fun `a line without a tab or without an emoji is skipped`() {
        val raw = "SMILEYS_PEOPLE\nSMILEYS_PEOPLE\t"
        
        val dataset = EmojiDatasetParser.parse(raw)
        
        assertEquals(emptyList<String>(), dataset.byCategory[EmojiCategory.SMILEYS_PEOPLE])
    }
    
    @Test
    fun `an empty input yields an empty dataset`() {
        val dataset = EmojiDatasetParser.parse("")
        
        assertTrue(dataset.isEmpty())
    }
}

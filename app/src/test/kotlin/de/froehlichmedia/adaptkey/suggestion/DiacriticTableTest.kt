// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-436 `diacritics.tsv` parser.
 */
class DiacriticTableTest {
    
    @Test
    fun `parses a single base letter with a single variant`() {
        assertEquals(mapOf('g' to listOf('ğ')), DiacriticTable.parse("g\tğ"))
    }
    
    @Test
    fun `parses a base letter with several comma-separated variants`() {
        assertEquals(mapOf('e' to listOf('é', 'è', 'ê', 'ë')), DiacriticTable.parse("e\té,è,ê,ë"))
    }
    
    @Test
    fun `parses several base letters, one per line`() {
        val parsed = DiacriticTable.parse("g\tğ\ns\tş\ni\tı\n")
        assertEquals(mapOf('g' to listOf('ğ'), 's' to listOf('ş'), 'i' to listOf('ı')), parsed)
    }
    
    @Test
    fun `an upper-case base letter is lower-cased`() {
        assertEquals(mapOf('e' to listOf('é')), DiacriticTable.parse("E\té"))
    }
    
    @Test
    fun `blank lines and comment lines are ignored`() {
        assertEquals(mapOf('g' to listOf('ğ')), DiacriticTable.parse("\n# a comment\ng\tğ\n   \n"))
    }
    
    @Test
    fun `a line with no tab separator is skipped`() {
        assertEquals(emptyMap<Char, List<Char>>(), DiacriticTable.parse("g ğ"))
    }
    
    @Test
    fun `a multi-character base is skipped`() {
        assertEquals(emptyMap<Char, List<Char>>(), DiacriticTable.parse("gg\tğ"))
    }
    
    @Test
    fun `a multi-character or blank variant is dropped, not the whole line`() {
        assertEquals(mapOf('e' to listOf('é')), DiacriticTable.parse("e\tee,é,"))
    }
    
    @Test
    fun `a variant equal to its own base is dropped`() {
        assertEquals(mapOf('e' to listOf('é')), DiacriticTable.parse("e\te,é"))
    }
    
    @Test
    fun `duplicate variants on the same line are collapsed`() {
        assertEquals(mapOf('e' to listOf('é')), DiacriticTable.parse("e\té,é"))
    }
    
    @Test
    fun `a base letter with no valid variants at all is dropped entirely`() {
        assertEquals(emptyMap<Char, List<Char>>(), DiacriticTable.parse("e\te,ee"))
    }
    
    @Test
    fun `empty content yields an empty map`() {
        assertEquals(emptyMap<Char, List<Char>>(), DiacriticTable.parse(""))
    }
}

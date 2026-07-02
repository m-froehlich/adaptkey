// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure G-05 word-end Shift logic: the next-key resolution table and the
 * first-character case toggle.
 */
class WordEndShiftTest {
    
    @Test
    fun `a following letter triggers camelCase`() {
        assertEquals(WordEndShift.Resolution.CAMEL_CASE, WordEndShift.resolveNextKey(WordEndShift.NextKey.LETTER))
    }
    
    @Test
    fun `a following delimiter keeps the toggle`() {
        assertEquals(WordEndShift.Resolution.KEEP, WordEndShift.resolveNextKey(WordEndShift.NextKey.DELIMITER))
    }
    
    @Test
    fun `a following shift re-toggles`() {
        assertEquals(WordEndShift.Resolution.RETOGGLE, WordEndShift.resolveNextKey(WordEndShift.NextKey.SHIFT))
    }
    
    @Test
    fun `any other following key cancels`() {
        assertEquals(WordEndShift.Resolution.CANCEL, WordEndShift.resolveNextKey(WordEndShift.NextKey.OTHER))
    }
    
    @Test
    fun `flipFirst lowercases an uppercase first letter`() {
        assertEquals("upper", WordEndShift.flipFirst("Upper"))
    }
    
    @Test
    fun `flipFirst uppercases a lowercase first letter`() {
        assertEquals("Lower", WordEndShift.flipFirst("lower"))
    }
    
    @Test
    fun `flipFirst leaves the remaining characters untouched`() {
        assertEquals("camelCase", WordEndShift.flipFirst("CamelCase"))
    }
    
    @Test
    fun `flipFirst is its own inverse`() {
        assertEquals("Haus", WordEndShift.flipFirst(WordEndShift.flipFirst("Haus")))
    }
    
    @Test
    fun `flipFirst returns an empty string unchanged`() {
        assertEquals("", WordEndShift.flipFirst(""))
    }
    
    @Test
    fun `flipFirst leaves a non-letter first character unchanged`() {
        assertEquals("3er", WordEndShift.flipFirst("3er"))
    }
    
    @Test
    fun `flipFirst toggles an umlaut first letter`() {
        assertEquals("Über", WordEndShift.flipFirst("über"))
        assertEquals("über", WordEndShift.flipFirst("Über"))
    }
}

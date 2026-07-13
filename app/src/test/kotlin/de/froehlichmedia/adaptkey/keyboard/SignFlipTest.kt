// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for §31's calculator minus-key long-press sign flip.
 */
class SignFlipTest {
    
    @Test
    fun `a plain number gets a leading minus inserted`() {
        val result = SignFlip.resultFor("123")
        
        assertEquals(SignFlip.Result(deleteLength = 3, insertText = "${SymbolLayout.MINUS_SIGN}123"), result)
    }
    
    @Test
    fun `an already-negative number has its minus removed`() {
        val result = SignFlip.resultFor("${SymbolLayout.MINUS_SIGN}123")
        
        assertEquals(SignFlip.Result(deleteLength = 4, insertText = "123"), result)
    }
    
    @Test
    fun `a decimal number is flipped as a whole`() {
        val result = SignFlip.resultFor("12,5")
        
        assertEquals(SignFlip.Result(deleteLength = 4, insertText = "${SymbolLayout.MINUS_SIGN}12,5"), result)
    }
    
    @Test
    fun `only the trailing number is affected, text before it is left alone`() {
        val result = SignFlip.resultFor("Preis: 42")
        
        assertEquals(SignFlip.Result(deleteLength = 2, insertText = "${SymbolLayout.MINUS_SIGN}42"), result)
    }
    
    @Test
    fun `no number directly before the caret yields no edit`() {
        assertNull(SignFlip.resultFor(""))
        assertNull(SignFlip.resultFor("Hallo"))
        assertNull(SignFlip.resultFor("42 "))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * D-509: the corner model's own pure decisions - [Key.defaultCornerHint]'s right-corner-first rule
 * (left-corner fallback) and the two long-press gates ([KeyboardLayout.longPressSymbol] /
 * [KeyboardLayout.hasLongPressAction]) that consume it. The drawing half (both corners painted, the
 * corner-aware D-98 triangle) is view glue and lives outside the unit-tested layer.
 */
class KeyCornerTest {
    
    @Test
    fun `D-509 the right corner is the default while present`() {
        val key = Key(label = "d", code = KeyCode.CHAR, char = 'd', hint = "$", leftHint = "°")
        assertEquals("$", key.defaultCornerHint)
    }
    
    @Test
    fun `D-509 a lone left corner becomes the default`() {
        val key = Key(label = "m", code = KeyCode.CHAR, char = 'm', leftHint = ")")
        assertEquals(")", key.defaultCornerHint)
    }
    
    @Test
    fun `D-509 a right-corner-only key keeps the pre-reform behaviour`() {
        val key = Key(label = "s", code = KeyCode.CHAR, char = 's', hint = "ß")
        assertNull(key.leftHint)
        assertEquals("ß", key.defaultCornerHint)
    }
    
    @Test
    fun `D-509 a corner-less key has no default`() {
        val key = Key(label = "l", code = KeyCode.CHAR, char = 'l')
        assertNull(key.defaultCornerHint)
    }
    
    @Test
    fun `D-509 longPressSymbol honours the corner-default rule`() {
        assertEquals(
            "$",
            KeyboardLayout.longPressSymbol(Key(label = "d", code = KeyCode.CHAR, char = 'd', hint = "$", leftHint = "°"))
        )
        assertEquals(")", KeyboardLayout.longPressSymbol(Key(label = "m", code = KeyCode.CHAR, char = 'm', leftHint = ")")))
        assertNull(KeyboardLayout.longPressSymbol(Key(label = "l", code = KeyCode.CHAR, char = 'l')))
        assertNull(KeyboardLayout.longPressSymbol(Key(label = "⇧", code = KeyCode.SHIFT)))
    }
    
    @Test
    fun `D-509 hasLongPressAction honours the corner-default rule`() {
        assertTrue(KeyboardLayout.hasLongPressAction(Key(label = "d", code = KeyCode.CHAR, char = 'd', hint = "$", leftHint = "°")))
        assertTrue(KeyboardLayout.hasLongPressAction(Key(label = "m", code = KeyCode.CHAR, char = 'm', leftHint = ")")))
        assertFalse(KeyboardLayout.hasLongPressAction(Key(label = "l", code = KeyCode.CHAR, char = 'l')))
    }
}

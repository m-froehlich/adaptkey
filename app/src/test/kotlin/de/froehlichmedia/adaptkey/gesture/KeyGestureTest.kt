// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import de.froehlichmedia.adaptkey.keyboard.KeyCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure swipe-to-action policy (§4 G-01 … G-03).
 */
class KeyGestureTest {
    
    @Test
    fun `a downward swipe dismisses the keyboard from any key`() {
        assertEquals(GestureAction.DISMISS_KEYBOARD, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.DOWN))
        assertEquals(GestureAction.DISMISS_KEYBOARD, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.DOWN))
        assertEquals(GestureAction.DISMISS_KEYBOARD, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.DOWN))
    }
    
    @Test
    fun `swipe left on backspace deletes a word`() {
        assertEquals(GestureAction.DELETE_WORD, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.LEFT))
    }
    
    @Test
    fun `other swipes on backspace carry no action`() {
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.RIGHT))
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.UP))
    }
    
    @Test
    fun `horizontal swipes on the space bar switch language`() {
        assertEquals(GestureAction.LANGUAGE_PREV, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT))
        assertEquals(GestureAction.LANGUAGE_NEXT, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.RIGHT))
    }
    
    @Test
    fun `an upward swipe on the space bar carries no action`() {
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.UP))
    }
    
    @Test
    fun `D-19 a horizontal swipe on a letter switches surface`() {
        assertEquals(GestureAction.SWITCH_SURFACE_PREV, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.LEFT))
        assertEquals(GestureAction.SWITCH_SURFACE_NEXT, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.RIGHT))
    }
    
    @Test
    fun `an upward swipe on a letter carries no action`() {
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.UP))
    }
    
    @Test
    fun `a NONE direction never carries an action`() {
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.NONE))
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.NONE))
    }
    
    @Test
    fun `an upward swipe on the combined key switches to the symbol layer`() {
        assertEquals(GestureAction.OPEN_SYMBOL_LAYER, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.UP))
    }
    
    @Test
    fun `horizontal swipes on the combined key switch surface (D-19), NONE carries no action`() {
        assertEquals(GestureAction.SWITCH_SURFACE_PREV, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.LEFT))
        assertEquals(GestureAction.SWITCH_SURFACE_NEXT, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.RIGHT))
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.NONE))
    }
    
    @Test
    fun `a downward swipe on the combined key still dismisses the keyboard`() {
        assertEquals(GestureAction.DISMISS_KEYBOARD, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.DOWN))
    }
}

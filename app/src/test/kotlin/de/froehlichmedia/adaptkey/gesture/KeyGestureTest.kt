// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import de.froehlichmedia.adaptkey.keyboard.InputSurface
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
    fun `other swipes on backspace carry no action, except up which opens the settings row`() {
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.RIGHT))
        // §48: an upward swipe is not backspace-specific - it opens the settings row from anywhere.
        assertEquals(GestureAction.OPEN_SETTINGS_ROW, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.UP))
    }
    
    @Test
    fun `horizontal swipes on the space bar switch language`() {
        assertEquals(GestureAction.LANGUAGE_PREV, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT))
        assertEquals(GestureAction.LANGUAGE_NEXT, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.RIGHT))
    }
    
    @Test
    fun `an upward swipe on the space bar opens the settings row (paragraph 48)`() {
        assertEquals(GestureAction.OPEN_SETTINGS_ROW, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.UP))
    }
    
    @Test
    fun `D-92 horizontal swipes on the space key only switch language on the letters surface`() {
        assertEquals(
            GestureAction.LANGUAGE_PREV,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT, InputSurface.LETTERS)
        )
        assertEquals(
            GestureAction.SWITCH_SURFACE_NEXT,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT, InputSurface.SYMBOLS)
        )
        assertEquals(
            GestureAction.SWITCH_SURFACE_PREV,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.RIGHT, InputSurface.SYMBOLS)
        )
    }
    
    @Test
    fun `D-143 a horizontal swipe on the URL-mode space key switches surface, not language`() {
        assertEquals(
            GestureAction.SWITCH_SURFACE_NEXT,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT, InputSurface.LETTERS, urlMode = true)
        )
        assertEquals(
            GestureAction.SWITCH_SURFACE_PREV,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.RIGHT, InputSurface.LETTERS, urlMode = true)
        )
        // Confirms the default (urlMode = false) is unaffected - still the ordinary language swipe.
        assertEquals(
            GestureAction.LANGUAGE_PREV,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT, InputSurface.LETTERS)
        )
    }
    
    @Test
    fun `D-158 a horizontal swipe on the email-mode space key switches surface, not language`() {
        assertEquals(
            GestureAction.SWITCH_SURFACE_NEXT,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.LEFT, InputSurface.LETTERS, emailMode = true)
        )
        assertEquals(
            GestureAction.SWITCH_SURFACE_PREV,
            KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.RIGHT, InputSurface.LETTERS, emailMode = true)
        )
    }
    
    
    @Test
    fun `D-19 and D-91 a horizontal swipe on a letter switches surface`() {
        assertEquals(GestureAction.SWITCH_SURFACE_NEXT, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.LEFT))
        assertEquals(GestureAction.SWITCH_SURFACE_PREV, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.RIGHT))
    }
    
    @Test
    fun `an upward swipe on a letter opens the settings row (paragraph 48)`() {
        assertEquals(GestureAction.OPEN_SETTINGS_ROW, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.UP))
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
    fun `horizontal swipes on the combined key switch surface (D-19 and D-91), NONE carries no action`() {
        assertEquals(GestureAction.SWITCH_SURFACE_NEXT, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.LEFT))
        assertEquals(GestureAction.SWITCH_SURFACE_PREV, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.RIGHT))
        assertEquals(GestureAction.NONE, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.NONE))
    }
    
    @Test
    fun `a downward swipe on the combined key still dismisses the keyboard`() {
        assertEquals(GestureAction.DISMISS_KEYBOARD, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.DOWN))
    }
    
    @Test
    fun `paragraph 48 an upward swipe opens the settings row from any key except the combined key`() {
        assertEquals(GestureAction.OPEN_SETTINGS_ROW, KeyGesture.resolve(KeyCode.CHAR, SwipeDirection.UP))
        assertEquals(GestureAction.OPEN_SETTINGS_ROW, KeyGesture.resolve(KeyCode.SPACE, SwipeDirection.UP))
        assertEquals(GestureAction.OPEN_SETTINGS_ROW, KeyGesture.resolve(KeyCode.DELETE, SwipeDirection.UP))
        // The combined key keeps its own L-03 upward gesture instead.
        assertEquals(GestureAction.OPEN_SYMBOL_LAYER, KeyGesture.resolve(KeyCode.SYMBOL, SwipeDirection.UP))
    }
}

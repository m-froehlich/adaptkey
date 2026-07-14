// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.gesture

import de.froehlichmedia.adaptkey.keyboard.InputSurface
import de.froehlichmedia.adaptkey.keyboard.KeyCode

/**
 * Pure policy mapping a swipe (the key it started on, plus its [SwipeDirection]) onto a
 * [GestureAction] (§4 G-01 … G-03, the L-03 upward swipe on the combined emoji / ?123 key, and §48's
 * upward-swipe settings row).
 *
 * A downward swipe dismisses the keyboard regardless of the key underneath (G-03); an upward swipe
 * reveals the settings row regardless of the key underneath too (§48), except on the combined key,
 * which keeps its own upward gesture. A horizontal swipe is key-specific: left on backspace deletes a
 * word (G-02), left / right on the space bar switches language (G-01) - but only on the letters surface
 * (D-92: the smaller space keys on the `?123` pages have no language of their own to switch, so a
 * horizontal swipe there is an ordinary surface swipe like everywhere else on those pages) -, up on the
 * combined key switches to the numeric/symbol layer (L-03), and left / right anywhere else on the key
 * field switches the surface/page (D-19). Everything else carries no gesture and resolves to
 * [GestureAction.NONE], leaving the touch to be handled as a tap.
 */
object KeyGesture {
    
    /**
     * Resolves a swipe into its action.
     *
     * @param keyCode the functional category of the key the swipe started on (T-01 contact point)
     * @param direction the recognised swipe direction
     * @param surface the surface the swipe happened on; defaults to [InputSurface.LETTERS], since G-01's
     *        language swipe only applies there (D-92)
     * @return the mapped action, or [GestureAction.NONE] when the swipe has no meaning here
     */
    fun resolve(keyCode: KeyCode, direction: SwipeDirection, surface: InputSurface = InputSurface.LETTERS): GestureAction {
        // G-03: a downward swipe dismisses the keyboard from anywhere (§48: AdaptKeyService.handleSwipe()
        // re-routes this to closing the settings row first when it is open, before it ever reaches here -
        // this resolver has no row-open state to gate on, so it always reports the plain dismiss).
        if (direction == SwipeDirection.DOWN) {
            return GestureAction.DISMISS_KEYBOARD
        }
        // §48: an upward swipe reveals the settings row from anywhere, mirroring G-03 above - except the
        // combined emoji/?123 key, which keeps its own L-03 upward gesture (switch to the symbol layer).
        if (direction == SwipeDirection.UP && keyCode != KeyCode.SYMBOL) {
            return GestureAction.OPEN_SETTINGS_ROW
        }
        return when (keyCode) {
            // G-02: swipe left on backspace deletes the whole previous word (right is not a surface swipe
            // here, to keep the backspace key unambiguous).
            KeyCode.DELETE -> if (direction == SwipeDirection.LEFT) GestureAction.DELETE_WORD else GestureAction.NONE
            // G-01: swipe left / right on the letters surface's space bar switches the input language
            // (never a surface swipe there). D-92: on any other surface, the space key has no language of
            // its own, so it falls through to an ordinary surface swipe (D-19) like every other key there.
            KeyCode.SPACE -> if (surface == InputSurface.LETTERS) {
                when (direction) {
                    SwipeDirection.LEFT -> GestureAction.LANGUAGE_PREV
                    SwipeDirection.RIGHT -> GestureAction.LANGUAGE_NEXT
                    else -> GestureAction.NONE
                }
            } else {
                surfaceSwipe(direction)
            }
            
            // L-03: swipe up on the combined emoji / ?123 key switches to the numeric/symbol layer; a
            // horizontal swipe there is an ordinary surface swipe (D-19).
            KeyCode.SYMBOL -> if (direction == SwipeDirection.UP) GestureAction.OPEN_SYMBOL_LAYER else surfaceSwipe(direction)
            
            // D-19: left / right anywhere else on the key field switches the surface/page.
            else -> surfaceSwipe(direction)
        }
    }
    
    /**
     * Maps a horizontal swipe on an ordinary field key to a surface/page switch (D-19); a non-horizontal
     * direction carries no surface action.
     *
     * D-91: a **left** swipe advances forward through the letters → symbols-page-1 → symbols-page-2 cycle,
     * and **right** goes back - reversed from the original D-19 mapping per user request.
     *
     * @param direction the recognised swipe direction
     * @return the surface-switch action, or [GestureAction.NONE]
     */
    private fun surfaceSwipe(direction: SwipeDirection): GestureAction {
        return when (direction) {
            SwipeDirection.LEFT -> GestureAction.SWITCH_SURFACE_NEXT
            SwipeDirection.RIGHT -> GestureAction.SWITCH_SURFACE_PREV
            else -> GestureAction.NONE
        }
    }
}

package de.froehlichmedia.adaptkey.gesture

import de.froehlichmedia.adaptkey.keyboard.KeyCode

/**
 * Pure policy mapping a swipe (the key it started on, plus its [SwipeDirection]) onto a
 * [GestureAction] (§4 G-01 … G-03, plus the L-03 upward swipe on the combined emoji / ?123 key).
 *
 * A downward swipe dismisses the keyboard regardless of the key underneath (G-03). A horizontal
 * swipe is key-specific: left on backspace deletes a word (G-02), left / right on the space bar
 * switches language (G-01), up on the combined key switches to the numeric/symbol layer (L-03).
 * Everything else carries no gesture and resolves to [GestureAction.NONE], leaving the touch to be
 * handled as a tap.
 */
object KeyGesture {
    
    /**
     * Resolves a swipe into its action.
     *
     * @param keyCode the functional category of the key the swipe started on (T-01 contact point)
     * @param direction the recognised swipe direction
     * @return the mapped action, or [GestureAction.NONE] when the swipe has no meaning here
     */
    fun resolve(keyCode: KeyCode, direction: SwipeDirection): GestureAction {
        // G-03: a downward swipe dismisses the keyboard from anywhere.
        if (direction == SwipeDirection.DOWN) {
            return GestureAction.DISMISS_KEYBOARD
        }
        return when (keyCode) {
            // G-02: swipe left on backspace deletes the whole previous word.
            KeyCode.DELETE -> if (direction == SwipeDirection.LEFT) GestureAction.DELETE_WORD else GestureAction.NONE
            // G-01: swipe left / right on the space bar switches the input language.
            KeyCode.SPACE -> when (direction) {
                SwipeDirection.LEFT -> GestureAction.LANGUAGE_PREV
                SwipeDirection.RIGHT -> GestureAction.LANGUAGE_NEXT
                else -> GestureAction.NONE
            }
            
            // L-03: swipe up on the combined emoji / ?123 key switches to the numeric/symbol layer.
            KeyCode.SYMBOL -> if (direction == SwipeDirection.UP) GestureAction.OPEN_SYMBOL_LAYER else GestureAction.NONE
            
            else -> GestureAction.NONE
        }
    }
}

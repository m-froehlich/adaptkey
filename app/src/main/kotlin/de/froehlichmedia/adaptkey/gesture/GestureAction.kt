package de.froehlichmedia.adaptkey.gesture

/**
 * The action a recognised keyboard swipe maps to (§4). [NONE] means the swipe carries no behaviour
 * for the key it started on and the touch should be ignored as a gesture.
 */
enum class GestureAction {
    NONE,
    
    /** G-02: a left swipe on the backspace key deletes the whole previous word. */
    DELETE_WORD,
    
    /** G-03: a downward swipe anywhere on the keyboard dismisses it. */
    DISMISS_KEYBOARD,
    
    /** G-01: a left swipe on the space bar switches to the previous input language. */
    LANGUAGE_PREV,
    
    /** G-01: a right swipe on the space bar switches to the next input language. */
    LANGUAGE_NEXT,
    
    /** L-03: an upward swipe on the combined emoji / ?123 key switches to the numeric/symbol layer. */
    OPEN_SYMBOL_LAYER
}

package de.froehlichmedia.adaptkey.gesture

/**
 * The direction of a recognised swipe gesture, or [NONE] when the movement stayed below the
 * gesture distance threshold and should be treated as a plain tap (T-01).
 */
enum class SwipeDirection {
    NONE,
    LEFT,
    RIGHT,
    UP,
    DOWN
}

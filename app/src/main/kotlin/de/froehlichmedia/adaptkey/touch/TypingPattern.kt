// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

/**
 * The user's explicitly chosen typing pattern (T-04).
 *
 * D-68: no longer auto-detected from a short typing sample - a few sentences could not reliably classify
 * it, and a wrong guess seeded some key zones badly, taking a very long time for real typing to correct
 * (the offset model has no forgetting mechanism). Instead the pattern is asked directly (calibration
 * screen / onboarding) and used to seed sensible initial per-key touch zones (T-03), see [PatternSeed].
 */
enum class TypingPattern {
    
    /** Typing with the left index finger only. */
    LEFT_INDEX_FINGER,
    
    /** Typing with the right index finger only. */
    RIGHT_INDEX_FINGER,
    
    /** One-handed typing with the left thumb (phone held in the left hand). */
    LEFT_THUMB,
    
    /** One-handed typing with the right thumb (phone held in the right hand). */
    RIGHT_THUMB,
    
    /** Two-handed typing with both thumbs. */
    TWO_THUMBS,
    
    /** Not chosen yet. */
    UNKNOWN
}

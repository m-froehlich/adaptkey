// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

/**
 * The dominant typing pattern derived from the personal offset model (T-04).
 *
 * The classification is informational only - it never constrains input - and can be overridden
 * manually in the settings. See [TypingPatternClassifier] for how it is inferred from the
 * accumulated per-key deviations.
 */
enum class TypingPattern {
    
    /** Systematic offset to the right, more pronounced on the right half; narrow contact area. */
    LEFT_INDEX_FINGER,
    
    /** Systematic offset to the left, more pronounced on the left half; narrow contact area. */
    RIGHT_INDEX_FINGER,
    
    /** Low lateral offset, large contact area, vertically centred hits. */
    THUMB,
    
    /** Not enough evidence yet, or the deviations match no single pattern. */
    UNKNOWN
}

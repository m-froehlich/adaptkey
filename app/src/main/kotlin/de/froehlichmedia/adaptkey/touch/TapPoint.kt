// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

/**
 * A single retained raw tap coordinate (T-02): the `ACTION_DOWN` position for one composing-token
 * character, in view pixels. Kept alongside the composing token so an unknown word can later be corrected
 * from where the taps actually landed (D-39), not just from the committed spelling.
 *
 * @property x the raw tap x
 * @property y the raw tap y
 */
data class TapPoint(val x: Float, val y: Float)

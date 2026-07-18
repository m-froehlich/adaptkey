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
 * @property weight D-159: the weight [OffsetModel.record] actually applied for this tap - retained so a
 *        later [OffsetModel.unrecord] (D-140) can reverse the exact same weighted update, not a heuristic
 *        re-derivation from whatever the model's current state happens to be by then. Defaults to `1.0`
 *        (an ordinary, undownweighted sample) for call sites with no real weight to report - the
 *        long-press-letter path, which has never tracked taps at all (a pre-existing, documented gap).
 */
data class TapPoint(val x: Float, val y: Float, val weight: Double = 1.0)

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Context
import android.widget.Button
import android.widget.LinearLayout

/**
 * D-296: shared between [LearnedWordsActivity]'s and [BlacklistActivity]'s own per-entry dialogs, both of
 * which show a word next to a compact copy button in the same layout shape.
 * 
 * @receiver used only for [Context.getResources]' own density, to convert [value] to real pixels
 */
fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

/**
 * D-296: a compact, square (width == height) [Button] for a glyph-only action - the ordinary [Button]
 * style's own minimum width/generous padding is exactly what made a Copy/Save pair needlessly wide; still a
 * real [Button] (not a hand-rolled touch target), so [Button.isEnabled] still greys the glyph out
 * automatically when the caller wants that.
 * 
 * @param glyph the button's own label (an emoji glyph)
 * @param description the accessible name ([android.view.View.setContentDescription]), since the visible
 *        label itself is a glyph, not descriptive text
 * @return the configured button, side length [SQUARE_ICON_BUTTON_SIZE_DP], not yet added to any parent
 */
fun Context.squareIconButton(glyph: String, description: String): Button {
    return Button(this, null, android.R.attr.borderlessButtonStyle).apply {
        text = glyph
        contentDescription = description
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(dp(SQUARE_ICON_BUTTON_SIZE_DP), dp(SQUARE_ICON_BUTTON_SIZE_DP))
    }
}

const val COPY_GLYPH = "📋"

// D-296: a standard-sized touch target (matches the Android accessibility guideline minimum), just square
// instead of the ordinary Button's own wide/padded shape.
private const val SQUARE_ICON_BUTTON_SIZE_DP = 48

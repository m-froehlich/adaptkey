// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * Suggestion-bar configuration (C-02, C-03, C-04).
 *
 * @property maxSuggestions maximum number of stabilised suggestions shown (C-03, recommended 6-10, default 8)
 * @property reSortDelayMs pause with no input after which the bar re-sorts (C-02 / S-04, 0-600 ms, default 300)
 * @property highlightEnabled whether a recognised, complete word is highlighted while composing (C-04 / S-05)
 * @property highlightColor the highlight colour as an ARGB int (C-04 / S-05)
 */
data class SuggestionConfig(
    val maxSuggestions: Int = 8,
    val reSortDelayMs: Long = 300L,
    val highlightEnabled: Boolean = true,
    val highlightColor: Int = DEFAULT_HIGHLIGHT_COLOR
) {
    
    init {
        require(maxSuggestions in 1..50) { "maxSuggestions out of range: $maxSuggestions" }
        require(reSortDelayMs in 0L..600L) { "reSortDelayMs out of range: $reSortDelayMs" }
    }
    
    companion object {
        
        /**
         * Default recognised-word text colour (D-25). D-240: moved from a dark, muted green (`#2E7D32`,
         * Material Green 800) to a brighter, more saturated one (Material Green A700) - contrast against
         * the light background was never the issue, but a *dark* green sits too close in lightness to
         * ordinary black/dark-grey body text for the human eye to reliably tell apart at a glance; hue
         * discrimination degrades sharply as lightness approaches either extreme. A brighter, more vivid
         * colour reads unambiguously as "this word changed colour" instead.
         */
        const val DEFAULT_HIGHLIGHT_COLOR: Int = 0xFF00C853.toInt()
    }
}

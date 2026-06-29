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
    val highlightEnabled: Boolean = false,
    val highlightColor: Int = DEFAULT_HIGHLIGHT_COLOR
) {
    
    init {
        require(maxSuggestions in 1..50) { "maxSuggestions out of range: $maxSuggestions" }
        require(reSortDelayMs in 0L..600L) { "reSortDelayMs out of range: $reSortDelayMs" }
    }
    
    companion object {
        
        /** Default highlight colour (a muted green), used when C-04 is enabled. */
        const val DEFAULT_HIGHLIGHT_COLOR: Int = 0xFF2E7D32.toInt()
    }
}

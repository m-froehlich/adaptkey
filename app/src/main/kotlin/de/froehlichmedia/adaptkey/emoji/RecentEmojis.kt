package de.froehlichmedia.adaptkey.emoji

/**
 * Pure most-recently-used policy for the emoji panel's "recent" tab (L-03).
 */
object RecentEmojis {
    
    /** Default cap on the recent-emoji list. */
    const val DEFAULT_MAX_SIZE = 30
    
    /**
     * Records a use of [emoji], moving it to the front of [current] (deduplicated) and capping the
     * result at [maxSize].
     *
     * @param current the current recent-emoji list, most recent first
     * @param emoji the emoji that was just committed
     * @param maxSize the maximum list length to keep
     * @return the updated list, most recent first
     * @throws IllegalArgumentException if [emoji] is empty or [maxSize] is not positive
     */
    fun recordUse(current: List<String>, emoji: String, maxSize: Int = DEFAULT_MAX_SIZE): List<String> {
        require(emoji.isNotEmpty()) { "emoji must not be empty" }
        require(maxSize > 0) { "maxSize must be positive: $maxSize" }
        val result = ArrayList<String>(minOf(current.size + 1, maxSize))
        result.add(emoji)
        for (existing in current) {
            if (result.size >= maxSize) {
                break
            }
            if (existing != emoji) {
                result.add(existing)
            }
        }
        return result
    }
}

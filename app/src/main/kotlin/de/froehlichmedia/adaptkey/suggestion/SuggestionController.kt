package de.froehlichmedia.adaptkey.suggestion

/**
 * Applies the suggestion-bar policy from the specification, free of any Android dependency so it
 * can be unit-tested on the JVM:
 *
 * - S-01: at most [SuggestionConfig.maxSuggestions] entries, sorted by descending score; the most
 *   probable appears at the far left.
 * - S-02: the word exactly as typed is never offered (the only exception is the S-06 verbatim chip).
 * - S-03: while the user is actively typing ([update]), already-visible entries keep their position
 *   and newly appearing candidates are appended at the right; no reordering happens.
 * - S-04: reordering by probability happens only on [resort], which the host calls after the
 *   configured input pause.
 * - S-06: when an autocorrect is pending, a verbatim "keep as typed" chip is pinned at the far left,
 *   exempt from stabilisation, followed by the proposed replacement.
 */
class SuggestionController(private val config: SuggestionConfig) {
    
    /** The display category of a bar entry. */
    enum class Kind {
        NORMAL,
        VERBATIM
    }
    
    /**
     * A renderable bar entry.
     *
     * @property text the label to display
     * @property kind whether this is an ordinary suggestion or the verbatim chip (S-06)
     * @property word the word committed when the entry is tapped (the literal typed word for [Kind.VERBATIM])
     */
    data class DisplayItem(
        val text: String,
        val kind: Kind,
        val word: String
    )
    
    private var input: String = ""
    private var candidatesByWord: Map<String, Suggestion> = emptyMap()
    private var stableOrder: List<String> = emptyList()
    private var pendingReplacement: String? = null
    
    /**
     * Feeds a fresh keystroke result, preserving the positions of already-visible entries (S-03)
     * and dropping the verbatim duplicate of the input (S-02).
     *
     * @param input the current composing token
     * @param candidates the raw ranked candidates for [input]
     * @param pendingAutocorrect the replacement an autocorrect would apply on the next delimiter (S-06), or null
     */
    fun update(input: String, candidates: List<Suggestion>, pendingAutocorrect: String?) {
        this.input = input
        this.pendingReplacement = pendingAutocorrect?.takeIf { it != input }
        
        // S-02: never offer the word exactly as typed as an ordinary suggestion.
        val filtered = candidates.filter { it.word != input }
        candidatesByWord = filtered.associateBy { it.word }
        
        // S-03: keep visible entries in place, append newly appearing ones (sorted) at the right.
        val kept = stableOrder.filter { candidatesByWord.containsKey(it) }
        val keptSet = kept.toHashSet()
        val appended = filtered
            .filter { it.word !in keptSet }
            .sortedByDescending { it.score }
            .map { it.word }
        stableOrder = (kept + appended).take(config.maxSuggestions)
    }
    
    /**
     * Re-sorts the stabilised entries strictly by descending score (S-01 / S-04). Called by the host
     * after the configured input pause.
     */
    fun resort() {
        stableOrder = candidatesByWord.values
            .sortedByDescending { it.score }
            .map { it.word }
            .take(config.maxSuggestions)
    }
    
    /**
     * Drops the pending autocorrect (used when the user keeps the verbatim input), leaving the
     * ordinary suggestions untouched.
     */
    fun declineAutocorrect() {
        pendingReplacement = null
    }
    
    /** Clears all state, e.g. after a token has been committed. */
    fun clear() {
        input = ""
        candidatesByWord = emptyMap()
        stableOrder = emptyList()
        pendingReplacement = null
    }
    
    /**
     * @return the entries to render, left to right: the verbatim chip and its proposed replacement
     *         first when an autocorrect is pending (S-06), then the stabilised suggestions
     */
    fun displayed(): List<DisplayItem> {
        val items = ArrayList<DisplayItem>()
        val replacement = pendingReplacement
        if (replacement != null) {
            items.add(DisplayItem(text = "\"$input\"", kind = Kind.VERBATIM, word = input))
            items.add(DisplayItem(text = replacement, kind = Kind.NORMAL, word = replacement))
        }
        val alreadyShown = items.mapTo(HashSet()) { it.word }
        for (word in stableOrder) {
            if (word !in alreadyShown) {
                items.add(DisplayItem(text = word, kind = Kind.NORMAL, word = word))
            }
        }
        return items
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

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
        VERBATIM,
        
        /** D-36: a direct-paste chip showing (a preview of) the clipboard; tapping it pastes. */
        CLIPBOARD,
        
        /** D-266: a preview of just the clipboard's first line; tapping it commits that line only,
         * directly (not the native paste action [CLIPBOARD] uses, which cannot paste a mere substring). */
        CLIPBOARD_FIRST_LINE,
        
        /** D-266: a preview of the first plausible "code" token found in the clipboard (via
         * [de.froehlichmedia.adaptkey.suggestion.ClipboardExtraction.firstCode]); tapping it commits that
         * token only, directly, exactly like [CLIPBOARD_FIRST_LINE]. */
        CLIPBOARD_FIRST_CODE,
        
        /** D-142: a saved username/email/domain-completion value in a recognised login field; tapping it
         * commits [DisplayItem.word] verbatim (never §6-capitalised) and reinforces it in the credential
         * store, never the ordinary dictionary. Built and pushed directly to the suggestion bar, bypassing
         * this class's own [update]/[displayed] (S-03 position stabilisation is for smoothing prose-typing
         * suggestion flicker; a short, freshly-ranked credential list has no need for it), mirroring how
         * [CLIPBOARD] is already handled. */
        CREDENTIAL,
        
        /** D-247: a "Gelernt: X" confirmation, shown once right after [DisplayItem.word] was just promoted
         * to the learned dictionary (D-37) - purely informational plus a drag target (see
         * `SuggestionBarView`'s own two-zone "Vergessen"/"Verbieten" drag), never committed by a tap. Built
         * and pushed directly to the suggestion bar, pinned ahead of the ordinary predictions, bypassing
         * this class's own [update]/[displayed] the same way [CLIPBOARD]/[CREDENTIAL] already do. */
        LEARNED,
        
        /** B-03/D-289: a previously-learned hyphen-joined compound (e.g. "Trogata-Team") matching the
         * current composing prefix; tapping it commits [DisplayItem.word] verbatim (already correctly
         * cased, exactly as learned - never re-run through the ordinary §6 pipeline, which assumes a
         * single, plain word). Built and pushed directly to the suggestion bar, pinned ahead of every
         * ordinary suggestion (never ranked by score against them - see the design discussion this
         * implements for why), the same "built outside SuggestionController" shape [CREDENTIAL]/[LEARNED]
         * already use. */
        COMPOUND,
        
        /** D-317: a live emoji-search match (L-03) - [DisplayItem.text] and [DisplayItem.word] are both
         * the emoji itself; tapping it commits the emoji and leaves search mode. Built and pushed directly
         * to the suggestion bar from the local search buffer, bypassing this class's own [update]/
         * [displayed] entirely, the same "built outside SuggestionController" shape [CREDENTIAL]/[LEARNED]
         * already use - there is no composing token to rank these against. */
        EMOJI_SEARCH_RESULT,
        
        /** D-318: pinned first, ahead of every [EMOJI_SEARCH_RESULT] - shows the emoji-search query typed
         * so far (D-317's capture buffer has no on-screen representation of its own otherwise, reported as
         * "typing into nothing"). Purely informational, like [LEARNED]; a tap does nothing. */
        EMOJI_SEARCH_QUERY,
        
        /** D-346: a placeholder "…" shown when the bar would otherwise be empty and a deferred/background
         * fuzzy search is still in flight, so the user knows the keyboard is still looking. Purely
         * informational; a tap does nothing. Replaced by real results (or an empty bar) once the search
         * completes. */
        LOADING,
        
        /** D-414-followup: a magnet chip shown when the bar would otherwise be empty and a word genuinely
         * touches the caret (composing empty) that could be reclaimed into composing (D-62) - migrated from
         * the extra row's own dedicated button, which required an extra swipe-up to reach and only ever
         * reflected its own enabled/disabled state, not a true visibility toggle. Tapping it performs the
         * same unconditional, suppression-bypassing, immediate reclaim the button used to. Built and pushed
         * directly to the suggestion bar, the same "built outside SuggestionController" shape [LOADING]/
         * [COMPOUND] already use. */
        RECLAIM,
        
        /** D-404-followup: one casing variant of a §6-rule-5-ambiguous noun (`CapitalisationEngine.
         * isAmbiguousCasing` - "Weg" `NOUN,OTHER`), e.g. "Weg"/"weg". Built and pushed directly to the
         * suggestion bar - never routed through [update]/[displayed]'s own capitalisation-at-render step
         * ([DisplayItem.word] already carries its own deliberately forced casing; re-running it through
         * `capitalise()` would always collapse both variants back to the same, ambiguous-default-lowercase
         * result, since that function derives purely from typing context, never from what a candidate word
         * already carries). Tapping it commits [DisplayItem.word] verbatim, exactly as shown - never
         * re-capitalised, the same "already correctly cased" contract [COMPOUND] uses. */
        AMBIGUOUS_CASE
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
        // D-364: compared case-insensitively - the caller pre-capitalises `replacement` (D-111/D-112's own
        // "preview the eventual committed casing, including a case-only change" reason - see
        // AdaptKeyService's own pending-computation KDoc), while stableOrder holds raw, uncapitalised
        // canonical dictionary words throughout, per this class's own "capitalising earlier would break S-02/
        // S-03 identity" design. A case-sensitive comparison here let the very same underlying word slip
        // through as two separate chips - e.g. "text" still sitting in stableOrder was never recognised as
        // the same word as the already-shown, already-capitalised "Text" replacement chip, so both rendered.
        val alreadyShown = items.mapTo(HashSet()) { it.word.lowercase() }
        for (word in stableOrder) {
            if (word.lowercase() !in alreadyShown) {
                items.add(DisplayItem(text = word, kind = Kind.NORMAL, word = word))
            }
        }
        return items
    }
}

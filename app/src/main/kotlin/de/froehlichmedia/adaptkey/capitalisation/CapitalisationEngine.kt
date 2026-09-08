// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import de.froehlichmedia.adaptkey.suggestion.Acronym

/**
 * Applies the capitalisation hierarchy (§6) to a single token, using the part-of-speech tags in
 * the dictionary. Pure logic over the {@link DictionaryStore} abstraction, so it is unit-tested
 * with the in-memory store.
 *
 * Hierarchy (highest priority first):
 * 1. Explicit user input — the token's own first character, as it actually stands right now, is never
 *    changed by any rule below. This is symmetric (D-405): an explicit uppercase is never lowercased, and
 *    an explicit lowercase is never uppercased either — sentence/line-start capitalisation is a *live*
 *    typing aid only (see [de.froehlichmedia.adaptkey.AdaptKeyService.armShiftForNextWord]/[ShiftGrace]),
 *    never a commit-time correction; by the time a token reaches [capitalise], whatever casing it carries
 *    already reflects the user's own choice, whether that is the pre-armed suggestion or a deliberate
 *    override.
 * 2. Editor-mandated capitalisation ([CapsMode]); linguistic rules never lowercase a field-required
 *    capital.
 * 3. Proper nouns and pure nouns (a word that exists only as a noun) are capitalised.
 * 4. Ambiguous words (noun and another part of speech) are left unchanged — no auto-capitalisation.
 *
 * B-02 (segment after a hyphen is lowercase unless it is a proper noun) is applied below the field
 * mandate but above the remaining linguistic rules. The high-confidence LLM exception from §6 (rule 6)
 * is applied through the optional [llmForcesUpper] flag, which the caller derives from a tier-3
 * proposal via {@link de.froehlichmedia.adaptkey.prediction.HighCertaintyCapitalisation}; it can lift
 * an otherwise-lowercased word to upper-case but never lowercases anything.
 *
 * @property casing D-449-followup: the active language's own single-character case-mapping convention (see
 *           [CasingRules]) - delegated rather than hardcoded so a non-Turkish store is never subject to (or,
 *           before this, silently starved of) Turkish's dotted/dotless İ/I distinction. Defaults to
 *           [DefaultCasingRules] so every existing caller/test that does not pass one explicitly keeps this
 *           class's historical behaviour unchanged; [de.froehlichmedia.adaptkey.AdaptKeyService] is the one
 *           production caller that resolves and passes the value matching the actually active language.
 */
class CapitalisationEngine(private val store: DictionaryStore, private val casing: CasingRules = DefaultCasingRules) {
    
    /**
     * Returns [word] with its first character cased according to the hierarchy. Characters beyond
     * the first are left untouched, except in [CapsMode.CHARACTERS], which upper-cases the whole word.
     *
     * @param word the token to case (the already-applied autocorrect form, or the typed word)
     * @param context the positional and field context
     * @param llmForcesUpper the §6 rule-6 LLM exception: when true, a high-certainty nominal context
     *        capitalises a word the linguistic rules would otherwise leave lowercase (never the reverse)
     * @return the cased word; an empty input is returned unchanged
     */
    fun capitalise(word: String, context: CapitalisationContext, llmForcesUpper: Boolean = false): String {
        if (word.isEmpty()) {
            return word
        }
        if (context.capsMode == CapsMode.CHARACTERS) {
            return casing.uppercaseAll(word)
        }
        // D-457-followup: [word] may be a differently-cased spelling of an already-known acronym (e.g. the
        // literal typed "llm" for a learned "LLM") rather than the acronym's own canonical spelling itself -
        // confirmed from a real device log that this genuinely happens (typing the full word "llm" lower-
        // case, not just its prefix). Every branch below can only ever adjust word's own first character
        // (uppercaseFirst/lowercaseFirst), so none of them could ever reconstruct "LLM" from "llm" even once
        // correctly deciding "uppercase" for it (isPureNoun was already true here - the result was still
        // only "Llm"). [store.entryOf] resolves case-insensitively and returns the entry's own real casing,
        // so this is checked directly rather than guessed at. Skipped when [word] already carries the
        // acronym's own shape (the two branches inside the `when` below already handle that exactly) and
        // when the caller's own first character was an explicit, deliberate choice (rule 1 - context.
        // explicitFirstUpper - still wins outright, unchanged; this is a narrower, later-priority fallback
        // for the reported all-lowercase-typing case specifically, not a new standing exception to rule 1).
        if (!context.explicitFirstUpper && !Acronym.isAcronym(word)) {
            val canonical = store.entryOf(word)?.word
            if (canonical != null && Acronym.isAcronym(canonical)) {
                return canonical
            }
        }
        
        val pos = store.partsOfSpeech(word)
        val isProper = pos.contains(PartOfSpeech.PROPER_NOUN)
        // D-461: rules 3 and 4 collapsed into one condition - a word is force-capitalised exactly when it
        // has no reading at all beyond noun/proper noun. Before this, `isProper` was its own independent
        // branch ranking *above* the pure-noun check, so a PROPER_NOUN tag silently overrode an otherwise
        // correct ambiguity: "Weg" (NOUN,VERB,PROPER_NOUN) was force-capitalised despite D-368 having
        // retagged it precisely so that "weg sein" would not be, and "wir waren" committed as "wir Waren".
        // A proper noun that is also a verb (or adjective, or anything else) is exactly as ambiguous as a
        // common noun that is - so it gets rule 5's treatment, not rule 4's.
        val isNounOnly = isNounOnly(pos)
        val isAmbiguousNoun = (pos.contains(PartOfSpeech.NOUN) || isProper) && !isNounOnly
        
        // D-405: context.sentenceStart deliberately never appears in this decision any more. Before this
        // change, a token starting a sentence/line was force-capitalised here unconditionally, regardless of
        // what was actually typed - overriding a user who had explicitly Shift-disarmed the pre-armed
        // capital and typed lower-case on purpose (confirmed from a real device log: composing stayed "test"
        // throughout, yet finalWord came out "Test"). Sentence-start capitalisation is now *only* a live
        // typing aid (armShiftForNextWord/ShiftGrace pre-arm Shift before the word is typed, so the ordinary
        // "forgot to capitalise" case is already handled by the time the very first keystroke lands) - by
        // commit time, explicitFirstUpper already reflects reality either way, with no second, independent
        // re-derivation needed or wanted. If a future report shows a genuine sentence start committing
        // lower-case despite the user never touching Shift, the fix is almost certainly a gap in the live
        // arming path (see AdaptKeyService.armShiftForNextWord/sentenceStartBefore, and this project's own
        // prior bugs in that exact area: D-45, D-313, D-335) - trace and fix the live arming, don't just
        // reinstate `context.sentenceStart -> true` here, since that would silently reopen this exact issue
        // for every deliberate lower-case override again. CapitalisationContext.sentenceStart is kept on the
        // data class (still populated by every caller) specifically so a narrower, context-aware
        // reintroduction stays a small, local change if one is ever genuinely needed.
        val upper = when {
            context.explicitFirstUpper -> true
            context.capsMode == CapsMode.WORDS -> true
            // B-02 / D-373. D-461: still the proper-noun exception specifically, never `isNounOnly` on its
            // own - B-02's default after a hyphen is lower-case for an ordinary noun too, so widening this
            // to every noun-only word would capitalise the second half of any plain compound ("Haus-tür").
            context.afterHyphen -> (isProper && isNounOnly) || previousSegmentPropagates(context)
            isNounOnly -> true // §6 rules 3+4 (D-461)
            llmForcesUpper -> true // §6 rule 6: high-certainty LLM nominal exception
            isAmbiguousNoun -> false
            else -> false
        }
        
        return when {
            upper -> casing.uppercaseFirst(word)
            // D-457: a deliberately all-caps acronym (e.g. a learned "LLM" with no noun/proper-noun tag of
            // its own, reached while its lowercase prefix is still being typed) must never have its casing
            // touched at all here - lowercaseFirst() only ever handles the first character, so applying it
            // to an acronym produces a nonsensical hybrid ("lLM") instead of leaving the deliberate all-caps
            // spelling intact. Checked only in this branch: the upper branch above already returns the
            // correct, unchanged acronym as a harmless no-op (uppercaseFirst("LLM") == "LLM").
            Acronym.isAcronym(word) -> word
            // Never lowercases an explicit uppercase: that path returns above via upper == true.
            else -> casing.lowercaseFirst(word)
        }
    }
    
    /**
     * D-373: B-02's own `isProper`-only exception missed a common, real case - a hyphen chain whose second
     * (or later) segment is an ordinary word with no independent proper-noun tag of its own (`"Nord"` in
     * `"München-Nord"`), even though the segment right before the hyphen genuinely was capitalised.
     * Deliberately two different checks depending on *why* that previous segment was capitalised, per the
     * user's own explicit hybrid design:
     *
     * - if the previous segment was itself at a sentence start, a bare capital there proves nothing about
     *   its own grammatical status (any word can open a sentence) - only propagate when it is independently
     *   a known noun/proper noun in the dictionary, the same signal [isNounOnly] already uses for
     *   the *current* word;
     * - otherwise, the previous segment's capital is trusted directly (whatever put it there - B-02's own
     *   proper-noun exception, an earlier D-373 propagation further back in the same chain, or an explicit
     *   user choice - already answered the "should this be capitalised" question once; a hyphen chain reads
     *   as one unit, so the next segment should agree).
     *
     * @param context the current word's own context; only [CapitalisationContext.previousHyphenSegment] and
     *        [CapitalisationContext.previousHyphenSegmentAtSentenceStart] are read here
     * @return true when the previous segment's own capitalisation should propagate onto this one
     */
    private fun previousSegmentPropagates(context: CapitalisationContext): Boolean {
        val previous = context.previousHyphenSegment ?: return false
        if (context.previousHyphenSegmentAtSentenceStart) {
            val pos = store.partsOfSpeech(previous)
            return pos.contains(PartOfSpeech.NOUN) || pos.contains(PartOfSpeech.PROPER_NOUN)
        }
        return previous.firstOrNull()?.isUpperCase() == true
    }
    
    companion object {
        
        /**
         * D-461: whether [pos] carries no reading at all beyond noun/proper noun - the single condition
         * §6's rules 3 and 4 now share. An empty tag set (an unknown word) is deliberately false: nothing
         * is known about it, so nothing may force a capital.
         *
         * @param pos the word's own dictionary tags
         * @return true when the word is a noun and/or proper noun and nothing else
         */
        fun isNounOnly(pos: Set<PartOfSpeech>): Boolean {
            return pos.isNotEmpty() &&
                pos.all { it == PartOfSpeech.NOUN || it == PartOfSpeech.PROPER_NOUN }
        }
        
        /**
         * D-404-followup: whether [pos] is genuinely ambiguous under §6 rule 5 - a noun or proper-noun tag
         * alongside at least one other reading ("Weg" `NOUN,VERB,PROPER_NOUN`), so [capitalise] forces no
         * casing and both spellings are offered as their own chips (S-11) instead. Mirrors [capitalise]'s
         * own `isAmbiguousNoun` computation exactly, deliberately kept in sync rather than re-derived.
         *
         * D-461: a `PROPER_NOUN` tag no longer suppresses this. It used to, on the reasoning that a place
         * name which also happens to be a common noun must still always force upper-case - but that reading
         * is what silently defeated D-368's own retags ("Weg", "waren", "Arbeit"), since the corpus hands a
         * proper-noun tag to almost any word that ever appeared as a surname or place. A proper noun with a
         * second real reading is ambiguous in exactly the way a common noun with one is.
         *
         * @param pos the word's own dictionary tags
         * @return true when neither rule 3 nor rule 4 would force a casing, but a nominal reading exists
         */
        fun isAmbiguousCasing(pos: Set<PartOfSpeech>): Boolean {
            val hasNominal = pos.contains(PartOfSpeech.NOUN) || pos.contains(PartOfSpeech.PROPER_NOUN)
            return hasNominal && !isNounOnly(pos)
        }
    }
}

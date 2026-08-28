// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech

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
 */
class CapitalisationEngine(private val store: DictionaryStore) {
    
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
            return word.uppercase()
        }
        
        val pos = store.partsOfSpeech(word)
        val isProper = pos.contains(PartOfSpeech.PROPER_NOUN)
        val hasNoun = pos.contains(PartOfSpeech.NOUN)
        val isPureNoun = hasNoun && pos.all { it == PartOfSpeech.NOUN || it == PartOfSpeech.PROPER_NOUN }
        val isAmbiguousNoun = hasNoun && !isPureNoun
        
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
            context.afterHyphen -> isProper // B-02
            isProper -> true
            isPureNoun -> true
            llmForcesUpper -> true // §6 rule 6: high-certainty LLM nominal exception
            isAmbiguousNoun -> false
            else -> false
        }
        
        return if (upper) {
            word.replaceFirstChar { it.uppercaseChar() }
        } else {
            // Never lowercases an explicit uppercase: that path returns above via upper == true.
            word.replaceFirstChar { it.lowercaseChar() }
        }
    }
}

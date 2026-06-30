package de.froehlichmedia.adaptkey.capitalisation

import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech

/**
 * Applies the capitalisation hierarchy (§6) to a single token, using the part-of-speech tags in
 * the dictionary. Pure logic over the {@link DictionaryStore} abstraction, so it is unit-tested
 * with the in-memory store.
 *
 * Hierarchy (highest priority first):
 * 1. Explicit user input — an explicit uppercase first letter is never lowercased.
 * 2. Editor-mandated capitalisation ([CapsMode]); linguistic rules never lowercase a field-required
 *    capital.
 * 3. Sentence start.
 * 4. Proper nouns and pure nouns (a word that exists only as a noun) are capitalised.
 * 5. Ambiguous words (noun and another part of speech) are left unchanged — no auto-capitalisation.
 *
 * B-02 (segment after a hyphen is lowercase unless it is a proper noun) is applied below the field
 * mandate but above the remaining linguistic rules. The high-confidence LLM exception from §6 is not
 * part of this stage.
 */
class CapitalisationEngine(private val store: DictionaryStore) {
    
    /**
     * Returns [word] with its first character cased according to the hierarchy. Characters beyond
     * the first are left untouched, except in [CapsMode.CHARACTERS], which upper-cases the whole word.
     *
     * @param word the token to case (the already-applied autocorrect form, or the typed word)
     * @param context the positional and field context
     * @return the cased word; an empty input is returned unchanged
     */
    fun capitalise(word: String, context: CapitalisationContext): String {
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
        
        val upper = when {
            context.explicitFirstUpper -> true
            context.capsMode == CapsMode.WORDS -> true
            context.afterHyphen -> isProper // B-02
            context.sentenceStart -> true
            isProper -> true
            isPureNoun -> true
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

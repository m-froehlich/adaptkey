// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * Data-access abstraction for the personal dictionary (tier-1 n-gram), the blacklist (A-04) and
 * part-of-speech annotations (§6).
 *
 * All word arguments are matched case-insensitively; stored entries keep their canonical case.
 * Two implementations exist: {@link InMemoryDictionaryStore} (pure Kotlin, used for unit tests)
 * and {@link SqliteDictionaryStore} (the persistent on-device store). Ranking and policy live in
 * {@link DictionarySuggestionProvider}, not here, so this layer stays a thin store.
 */
interface DictionaryStore {
    
    /**
     * Inserts or replaces a unigram entry (used for seeding and imports).
     *
     * @param entry the entry to store
     */
    fun putWord(entry: WordEntry)
    
    /**
     * Inserts or replaces a bigram count.
     *
     * @param previousWord the preceding word
     * @param word the following word
     * @param count the co-occurrence count
     */
    fun putBigram(previousWord: String, word: String, count: Long)
    
    /**
     * Records one observation of [word] (adaptive learning): increments its unigram frequency and,
     * when [previousWord] is given, the corresponding bigram count. Unknown words are created.
     *
     * @param word the observed word
     * @param previousWord the word committed immediately before, or null
     */
    fun learn(word: String, previousWord: String?)
    
    /**
     * Reverses exactly one prior [learn] observation of [word] (A-07 undo of a rejected
     * autocorrect/split): decrements its unigram frequency and, when [previousWord] is given, the
     * corresponding bigram count, each by exactly one - removing the entry entirely once its count
     * reaches zero, so a word [learn] had just created (frequency 1) is fully un-learned again rather
     * than left behind as a zero-frequency ghost [isKnownWord] would still report as known. Never
     * called on its own; always paired with the exact prior [learn] call it undoes, so the count can
     * never go negative in practice. Only ever touches the learned lexicon (D-177) - a bundled word's
     * own frequency is never affected, since [learn] never wrote there in the first place.
     *
     * @param word the word to reverse
     * @param previousWord the word committed immediately before, or null
     */
    fun unlearn(word: String, previousWord: String?)
    
    /**
     * D-177: permanently, irreversibly removes [word] from the learned lexicon regardless of its
     * accumulated frequency - unlike [unlearn], which only reverses exactly one prior observation and
     * leaves the rest intact. Never touches the bundled dictionary (nothing to remove there in the
     * first place - see [isBundledWord]). Used when the user rejects a self-taught word outright (the
     * G-04 drag-to-trash gesture, or the learned-words editor), not to undo a single commit.
     *
     * @param word the word to forget entirely
     */
    fun forget(word: String)
    
    /**
     * D-177: whether [word] is part of the bundled dictionary asset, independent of any learning.
     * Distinguishes a real dictionary word - which must be permanently blacklisted to ever be
     * suppressed, since there is nothing to "forget" - from a purely self-taught one, which [forget]
     * can simply remove outright. A word can be both (e.g. a common bundled word the user has also
     * personally reinforced); this reports true regardless, since the bundled fact alone is what
     * matters for that decision.
     *
     * @param word the word to check
     * @return true when [word] exists in the bundled dictionary
     */
    fun isBundledWord(word: String): Boolean
    
    /**
     * D-177: every word currently in the learned lexicon (never the bundled dictionary), in canonical
     * case with its learned frequency, for the learned-words editor - including a word that could never
     * be reached any other way (e.g. one matching the current input, which S-02 always excludes from
     * the suggestion bar, so G-04's drag-to-trash structurally can never reach it either).
     *
     * @return the learned words, sorted by descending frequency
     */
    fun learnedWords(): List<WordEntry>
    
    /**
     * D-177: marks [word] as provisionally forgotten, recorded with [timestampMillis] but never
     * surfaced in the visible blacklist ([blacklistedWords]) - the "naively unlearn it, but remember
     * for a while in case it comes back" refinement: if [word] gets learned again before the mark
     * expires, that is treated as a genuinely recurring mistake (not a one-off typo/test word) and the
     * caller promotes it to a real, permanent blacklist entry instead of learning it again. Replaces
     * any existing mark for the same word (a fresh forget resets the expiry window).
     *
     * @param word the word to mark
     * @param timestampMillis when it was marked (epoch millis)
     */
    fun markPendingBlacklist(word: String, timestampMillis: Long)
    
    /**
     * @param word the word to check
     * @return the epoch-millis timestamp [word] was marked at via [markPendingBlacklist], or null if it
     *         was never marked (or the mark has since been cleared) - the caller applies the expiry
     *         window, not this store
     */
    fun pendingBlacklistedSince(word: String): Long?
    
    /**
     * Clears a pending-blacklist mark for [word] - either because it expired unused, or because it was
     * just promoted to a real, permanent blacklist entry.
     *
     * @param word the word to clear
     */
    fun clearPendingBlacklist(word: String)
    
    /**
     * @param prefix the (case-insensitive) prefix to match
     * @param limit the maximum number of entries to return
     * @return matching unigrams sorted by descending frequency
     */
    fun unigramsByPrefix(prefix: String, limit: Int): List<WordEntry>
    
    /**
     * @param previousWord the preceding word
     * @param word the following word
     * @return the stored bigram count, or 0 when unknown
     */
    fun bigramFrequency(previousWord: String, word: String): Long
    
    /**
     * The most frequent successor words of [previousWord] by bigram count (D-43 next-word prediction), in
     * canonical case. The default returns none; both concrete stores override it with a bigram lookup.
     *
     * @param previousWord the preceding word (case-insensitive)
     * @param limit the maximum number of successors to return
     * @return the successor words ordered by descending bigram count, in canonical case
     */
    fun nextWords(previousWord: String, limit: Int): List<String> = emptyList()
    
    /**
     * @param word the word to look up
     * @return the unigram frequency, or 0 when unknown
     */
    fun frequencyOf(word: String): Long
    
    /**
     * @param word the word to check
     * @return true if the word exists as a unigram
     */
    fun isKnownWord(word: String): Boolean
    
    /**
     * @param word the word to look up
     * @return the part-of-speech tags, or an empty set when unknown/unannotated
     */
    fun partsOfSpeech(word: String): Set<PartOfSpeech>
    
    /**
     * @return every known word in canonical case (used by the single-edit autocorrect search)
     */
    fun allKnownWords(): List<String>
    
    /**
     * A bounded candidate set for the single-edit autocorrect of [token], so callers never scan the
     * whole lexicon on every keystroke. An edit-distance-1 match shares [token]'s first character and
     * differs in length by at most one, so a store may return just those; the caller still applies the
     * exact edit-distance test. The default implementation returns the whole lexicon (fine for the small
     * in-memory store used in tests); the SQLite store overrides it with an indexed query.
     *
     * @param token the (case-insensitive) typed token being corrected
     * @return a superset of the edit-distance-1 words, in canonical case
     */
    fun correctionCandidates(token: String): List<String> = allKnownWords()
    
    /**
     * A bounded candidate set for the correction of [token] that also searches the additional first-character
     * buckets in [firstChars] (D-38): the token's keyboard-neighbour and umlaut-variant initial letters, so a
     * first-key typo (`eerden` -> `werden`) or a missing initial umlaut (`Uberblick` -> `Überblick`) can be
     * found. The default ignores [firstChars] (fine for the small in-memory store, which returns the whole
     * lexicon); the SQLite store overrides it with one indexed query per bucket.
     *
     * @param token the (case-insensitive) typed token being corrected
     * @param firstChars the initial letters to search (should include the token's own first character)
     * @return a superset of the in-budget correction candidates, in canonical case
     */
    fun correctionCandidates(token: String, firstChars: Set<Char>): List<String> = correctionCandidates(token)
    
    /**
     * Adds [word] to the blacklist under [category] (A-04).
     *
     * @param word the word to exclude
     * @param category the blacklist category
     */
    fun blacklist(word: String, category: BlacklistCategory)
    
    /**
     * Removes [word] from the blacklist if present.
     *
     * @param word the word to re-admit
     */
    fun unblacklist(word: String)
    
    /**
     * @param word the word to check
     * @return true if the word is blacklisted
     */
    fun isBlacklisted(word: String): Boolean
    
    /**
     * @param word the word to look up
     * @return the blacklist category, or null when the word is not blacklisted
     */
    fun blacklistCategory(word: String): BlacklistCategory?
    
    /**
     * Lists all currently blacklisted words (A-04 / C-05), e.g. for the settings editor.
     *
     * @return the blacklisted words in their stored (lower-cased) key form, sorted alphabetically
     */
    fun blacklistedWords(): List<String>
    
    /**
     * @return true if no unigrams are stored yet (used to decide whether to seed)
     */
    fun isEmpty(): Boolean
}

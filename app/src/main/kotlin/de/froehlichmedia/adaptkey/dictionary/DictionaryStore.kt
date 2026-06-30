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

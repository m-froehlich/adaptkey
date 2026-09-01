// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * In-memory {@link DictionaryStore}, free of Android dependencies for unit testing and as a simple
 * reference implementation. Lookups are case-insensitive; entries keep their canonical case.
 * 
 * D-177: mirrors {@link SqliteDictionaryStore}'s split between the bundled dictionary ([unigrams] /
 * [bigrams], written by [putWord] / [putBigram]) and the user's own learned vocabulary ([learned] /
 * [learnedBigrams], written only by [learn] / [unlearn] / [forget]) - see that class's own KDoc for the
 * full reasoning. Every "does the keyboard know this word" read merges both sources.
 *
 * @param clock D-411: how [learnedTouch] stamps "now" - defaults to the real wall clock; tests inject a
 *        fixed/controllable value instead, mirroring [DictionarySuggestionProvider]'s own `now` parameter,
 *        so [LearnedFrequencyBoost]'s recency behaviour stays deterministic under test.
 */
class InMemoryDictionaryStore(private val clock: () -> Long = { System.currentTimeMillis() }) : DictionaryStore {
    
    private val unigrams = HashMap<String, WordEntry>()
    private val bigrams = HashMap<String, Long>()
    private val learned = HashMap<String, WordEntry>()
    private val learnedBigrams = HashMap<String, Long>()
    // D-246: personal-only, no bundled counterpart - unlike bigrams, no seeded "trigrams" map exists.
    private val learnedTrigrams = HashMap<String, Long>()
    private val blacklist = HashMap<String, BlacklistCategory>()
    private val pendingBlacklist = HashMap<String, Long>()
    // D-411: last-touched epoch millis per learned entry, mirroring SqliteDictionaryStore's own
    // last_touched column - stamped by every learned/unlearn write, uniformly, same reasoning as there.
    private val learnedTouch = HashMap<String, Long>()
    
    override fun putWord(entry: WordEntry) {
        unigrams[entry.word.lowercase()] = entry
    }
    
    override fun putBigram(previousWord: String, word: String, count: Long) {
        bigrams[bigramKey(previousWord, word)] = count
    }
    
    override fun learn(
        word: String,
        previousWord: String?,
        previousPreviousWord: String?,
        seedFrequency: Long,
        categoryHint: PartOfSpeech?
    ) {
        val key = word.lowercase()
        val existing = learned[key]
        // D-264: a fresh learned entry uses the casing actually typed/committed, not the bundled entry's
        // own (if any) - letting a deliberately different-cased spelling (e.g. a preferred all-caps
        // acronym) become the one that wins in entryOf()/unigramsByPrefix() merges, instead of being
        // silently discarded in favour of whatever the bundled asset happens to store.
        val canonical = existing?.word ?: word
        // D-404: a category is only ever set once, from whichever categoryHint call first supplies one -
        // never overridden afterwards, and never defaulted to anything when no hint is given (stays the
        // empty set, read by the editor as "unbekannt").
        val pos = when {
            existing != null && existing.partsOfSpeech.isNotEmpty() -> existing.partsOfSpeech
            categoryHint != null -> setOf(categoryHint)
            else -> existing?.partsOfSpeech ?: emptySet()
        }
        // D-404: the base-form link is resolved once, the first time this entry's own lemma is still
        // unset - lookup-only, see LearnedLemmaLinking's own KDoc.
        val lemma = existing?.lemma ?: LearnedLemmaLinking.findLemma(key) { candidate -> learned.containsKey(candidate) }
        // D-388: a genuinely new entry starts at seedFrequency, not always 1 - see the interface's own KDoc.
        learned[key] = if (existing != null) {
            existing.copy(frequency = existing.frequency + 1L, partsOfSpeech = pos, lemma = lemma)
        } else {
            WordEntry(word = canonical, frequency = seedFrequency, partsOfSpeech = pos, lemma = lemma)
        }
        if (existing?.lemma == null && lemma == null) {
            // D-404: word itself may be the base of an already-learned inflected form still missing its
            // own link - the reverse direction, needed for the opening example ("Hundes" already learned,
            // "Hund" learned afterwards).
            linkExistingInflectionsTo(key)
        }
        learnedTouch[key] = clock()
        if (previousWord != null) {
            val bigramKey = bigramKey(previousWord, word)
            learnedBigrams[bigramKey] = (learnedBigrams[bigramKey] ?: 0L) + 1L
            if (previousPreviousWord != null) {
                val trigramKey = trigramKey(previousPreviousWord, previousWord, word)
                learnedTrigrams[trigramKey] = (learnedTrigrams[trigramKey] ?: 0L) + 1L
            }
        }
    }
    
    /**
     * D-404: links every already-learned word matching one of [baseKey]'s own candidate inflected forms
     * ([LearnedLemmaLinking.candidateInflections]) back to it, unless that word already carries its own
     * lemma link. Lookup-only - never creates a new entry, only fills in the `lemma` field of one already
     * present.
     */
    private fun linkExistingInflectionsTo(baseKey: String) {
        for (candidate in LearnedLemmaLinking.candidateInflections(baseKey)) {
            val row = learned[candidate] ?: continue
            if (row.lemma == null) {
                learned[candidate] = row.copy(lemma = baseKey)
            }
        }
    }
    
    override fun learnContext(word: String, previousWord: String?, previousPreviousWord: String?) {
        // D-327: only the n-gram context, never the unigram - see the interface KDoc.
        if (previousWord != null) {
            val bigramKey = bigramKey(previousWord, word)
            learnedBigrams[bigramKey] = (learnedBigrams[bigramKey] ?: 0L) + 1L
            if (previousPreviousWord != null) {
                val trigramKey = trigramKey(previousPreviousWord, previousWord, word)
                learnedTrigrams[trigramKey] = (learnedTrigrams[trigramKey] ?: 0L) + 1L
            }
        }
    }
    
    override fun unlearn(word: String, previousWord: String?, previousPreviousWord: String?) {
        val key = word.lowercase()
        val existing = learned[key]
        if (existing != null) {
            val frequency = existing.frequency - 1L
            if (frequency <= 0L) {
                learned.remove(key)
                learnedTouch.remove(key)
            } else {
                learned[key] = existing.copy(frequency = frequency)
                learnedTouch[key] = clock()
            }
        }
        if (previousWord != null) {
            val bigramKey = bigramKey(previousWord, word)
            val count = (learnedBigrams[bigramKey] ?: 0L) - 1L
            if (count <= 0L) {
                learnedBigrams.remove(bigramKey)
            } else {
                learnedBigrams[bigramKey] = count
            }
            if (previousPreviousWord != null) {
                val trigramKey = trigramKey(previousPreviousWord, previousWord, word)
                val trigramCount = (learnedTrigrams[trigramKey] ?: 0L) - 1L
                if (trigramCount <= 0L) {
                    learnedTrigrams.remove(trigramKey)
                } else {
                    learnedTrigrams[trigramKey] = trigramCount
                }
            }
        }
    }
    
    override fun forget(word: String) {
        val key = word.lowercase()
        learned.remove(key)
        learnedTouch.remove(key)
    }
    
    override fun isBundledWord(word: String): Boolean {
        return unigrams.containsKey(word.lowercase())
    }
    
    override fun bundledCasingOf(word: String): String? {
        return unigrams[word.lowercase()]?.word
    }
    
    override fun learnedCasingOf(word: String): String? {
        return learned[word.lowercase()]?.word
    }
    
    override fun learnedFrequencyOf(word: String): LearnedFrequency? {
        val key = word.lowercase()
        val entry = learned[key] ?: return null
        return LearnedFrequency(entry.frequency, learnedTouch[key] ?: 0L)
    }
    
    
    override fun setLearnedLemma(word: String, lemma: String?) {
        val key = word.lowercase()
        val existing = learned[key] ?: return
        learned[key] = existing.copy(lemma = lemma)
    }
    
    override fun learnedWords(): List<WordEntry> {
        return learned.values.sortedWith(compareByDescending<WordEntry> { it.frequency }.thenBy { it.word })
    }
    
    override fun markPendingBlacklist(word: String, timestampMillis: Long) {
        pendingBlacklist[word.lowercase()] = timestampMillis
    }
    
    override fun pendingBlacklistedSince(word: String): Long? {
        return pendingBlacklist[word.lowercase()]
    }
    
    override fun clearPendingBlacklist(word: String) {
        pendingBlacklist.remove(word.lowercase())
    }
    
    override fun unigramsByPrefix(prefix: String, limit: Int): List<WordEntry> {
        val normalized = prefix.lowercase()
        val merged = LinkedHashMap<String, WordEntry>()
        unigrams.filterKeys { it.startsWith(normalized) }.forEach { (key, entry) -> merged[key] = entry }
        learned.filterKeys { it.startsWith(normalized) }.forEach { (key, entry) ->
            val existing = merged[key]
            // D-264: the learned entry's own casing wins over a bundled one when both exist for the same
            // key (see learn()'s own note). D-404: the learned entry's own lemma link (if any) wins too -
            // it is more specific, user-established data - falling back to the bundled one otherwise.
            merged[key] = if (existing != null) {
                entry.copy(frequency = existing.frequency + entry.frequency, lemma = entry.lemma ?: existing.lemma)
            } else {
                entry
            }
        }
        return merged.values.sortedByDescending { it.frequency }.take(limit)
    }
    
    override fun bigramFrequency(previousWord: String, word: String): Long {
        val key = bigramKey(previousWord, word)
        return (bigrams[key] ?: 0L) + (learnedBigrams[key] ?: 0L)
    }
    
    override fun learnedBigramFrequency(previousWord: String, word: String): Long {
        return learnedBigrams[bigramKey(previousWord, word)] ?: 0L
    }
    
    override fun nextWords(previousWord: String, limit: Int): List<String> {
        if (previousWord.isEmpty() || limit <= 0) {
            return emptyList()
        }
        // The prefix must use the same separator as bigramKey(), so successors of previousWord are matched.
        val prefix = previousWord.lowercase() + BIGRAM_SEPARATOR
        val counts = LinkedHashMap<String, Long>()
        bigrams.forEach { (key, count) -> if (count > 0L && key.startsWith(prefix)) counts[key] = (counts[key] ?: 0L) + count }
        learnedBigrams.forEach { (key, count) -> if (count > 0L && key.startsWith(prefix)) counts[key] = (counts[key] ?: 0L) + count }
        return counts.entries
            .sortedByDescending { it.value }
            .map { it.key.substring(prefix.length) }
            .map { successor -> learned[successor]?.word ?: unigrams[successor]?.word ?: successor }
            .take(limit)
    }
    
    override fun trigramFrequency(previousPreviousWord: String, previousWord: String, word: String): Long {
        return learnedTrigrams[trigramKey(previousPreviousWord, previousWord, word)] ?: 0L
    }
    
    override fun nextWordsTrigram(previousPreviousWord: String, previousWord: String, limit: Int): List<String> {
        if (previousPreviousWord.isEmpty() || previousWord.isEmpty() || limit <= 0) {
            return emptyList()
        }
        val prefix = previousPreviousWord.lowercase() + BIGRAM_SEPARATOR + previousWord.lowercase() + BIGRAM_SEPARATOR
        return learnedTrigrams.entries
            .filter { it.value > 0L && it.key.startsWith(prefix) }
            .sortedByDescending { it.value }
            .map { it.key.substring(prefix.length) }
            .map { successor -> learned[successor]?.word ?: unigrams[successor]?.word ?: successor }
            .take(limit)
    }
    
    override fun frequencyOf(word: String): Long {
        return entryOf(word)?.frequency ?: 0L
    }
    
    override fun isKnownWord(word: String): Boolean {
        return entryOf(word) != null
    }
    
    override fun partsOfSpeech(word: String): Set<PartOfSpeech> {
        return entryOf(word)?.partsOfSpeech ?: emptySet()
    }
    
    override fun allKnownWords(): List<String> {
        val result = LinkedHashSet<String>()
        unigrams.values.mapTo(result) { it.word }
        learned.values.mapTo(result) { it.word }
        return result.toList()
    }
    
    override fun blacklist(word: String, category: BlacklistCategory) {
        blacklist[word.lowercase()] = category
    }
    
    override fun unblacklist(word: String) {
        blacklist.remove(word.lowercase())
    }
    
    override fun isBlacklisted(word: String): Boolean {
        return blacklist.containsKey(word.lowercase())
    }
    
    override fun blacklistCategory(word: String): BlacklistCategory? {
        return blacklist[word.lowercase()]
    }
    
    override fun blacklistedWords(): List<String> {
        return blacklist.keys.sorted()
    }
    
    override fun isEmpty(): Boolean {
        return unigrams.isEmpty()
    }
    
    override fun entryOf(word: String): WordEntry? {
        val key = word.lowercase()
        val bundled = unigrams[key]
        val personal = learned[key]
        return when {
            bundled == null -> personal
            personal == null -> bundled
            // D-264: the learned entry's own casing wins when both exist for the same key. D-404: the
            // learned entry's own lemma link (if any) wins too, falling back to the bundled one otherwise.
            else -> WordEntry(
                personal.word,
                bundled.frequency + personal.frequency,
                bundled.partsOfSpeech + personal.partsOfSpeech,
                lemma = personal.lemma ?: bundled.lemma
            )
        }
    }
    
    private fun bigramKey(previousWord: String, word: String): String {
        return previousWord.lowercase() + BIGRAM_SEPARATOR + word.lowercase()
    }
    
    private fun trigramKey(previousPreviousWord: String, previousWord: String, word: String): String {
        return previousPreviousWord.lowercase() + BIGRAM_SEPARATOR + previousWord.lowercase() + BIGRAM_SEPARATOR + word.lowercase()
    }
    
    companion object {
        
        /** Separator between the two halves of a bigram key; a space cannot occur inside a single word. */
        private const val BIGRAM_SEPARATOR = " "
    }
}

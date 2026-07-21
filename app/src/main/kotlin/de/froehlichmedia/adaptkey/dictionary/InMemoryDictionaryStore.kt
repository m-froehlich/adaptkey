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
 */
class InMemoryDictionaryStore : DictionaryStore {
    
    private val unigrams = HashMap<String, WordEntry>()
    private val bigrams = HashMap<String, Long>()
    private val learned = HashMap<String, WordEntry>()
    private val learnedBigrams = HashMap<String, Long>()
    private val blacklist = HashMap<String, BlacklistCategory>()
    private val pendingBlacklist = HashMap<String, Long>()
    
    override fun putWord(entry: WordEntry) {
        unigrams[entry.word.lowercase()] = entry
    }
    
    override fun putBigram(previousWord: String, word: String, count: Long) {
        bigrams[bigramKey(previousWord, word)] = count
    }
    
    override fun learn(word: String, previousWord: String?) {
        val key = word.lowercase()
        val existing = learned[key]
        val canonical = existing?.word ?: unigrams[key]?.word ?: word
        learned[key] = if (existing != null) {
            existing.copy(frequency = existing.frequency + 1L)
        } else {
            WordEntry(word = canonical, frequency = 1L)
        }
        if (previousWord != null) {
            val bigramKey = bigramKey(previousWord, word)
            learnedBigrams[bigramKey] = (learnedBigrams[bigramKey] ?: 0L) + 1L
        }
    }
    
    override fun unlearn(word: String, previousWord: String?) {
        val key = word.lowercase()
        val existing = learned[key]
        if (existing != null) {
            val frequency = existing.frequency - 1L
            if (frequency <= 0L) {
                learned.remove(key)
            } else {
                learned[key] = existing.copy(frequency = frequency)
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
        }
    }
    
    override fun forget(word: String) {
        learned.remove(word.lowercase())
    }
    
    override fun isBundledWord(word: String): Boolean {
        return unigrams.containsKey(word.lowercase())
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
            merged[key] = if (existing != null) existing.copy(frequency = existing.frequency + entry.frequency) else entry
        }
        return merged.values.sortedByDescending { it.frequency }.take(limit)
    }
    
    override fun bigramFrequency(previousWord: String, word: String): Long {
        val key = bigramKey(previousWord, word)
        return (bigrams[key] ?: 0L) + (learnedBigrams[key] ?: 0L)
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
            else -> WordEntry(bundled.word, bundled.frequency + personal.frequency, bundled.partsOfSpeech + personal.partsOfSpeech)
        }
    }
    
    private fun bigramKey(previousWord: String, word: String): String {
        return previousWord.lowercase() + BIGRAM_SEPARATOR + word.lowercase()
    }
    
    companion object {
        
        /** Separator between the two halves of a bigram key; a space cannot occur inside a single word. */
        private const val BIGRAM_SEPARATOR = " "
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * In-memory {@link DictionaryStore}, free of Android dependencies for unit testing and as a simple
 * reference implementation. Lookups are case-insensitive; entries keep their canonical case.
 */
class InMemoryDictionaryStore : DictionaryStore {
    
    private val unigrams = HashMap<String, WordEntry>()
    private val bigrams = HashMap<String, Long>()
    private val blacklist = HashMap<String, BlacklistCategory>()
    
    override fun putWord(entry: WordEntry) {
        unigrams[entry.word.lowercase()] = entry
    }
    
    override fun putBigram(previousWord: String, word: String, count: Long) {
        bigrams[bigramKey(previousWord, word)] = count
    }
    
    override fun learn(word: String, previousWord: String?) {
        val key = word.lowercase()
        val existing = unigrams[key]
        unigrams[key] = if (existing != null) {
            existing.copy(frequency = existing.frequency + 1L)
        } else {
            WordEntry(word = word, frequency = 1L)
        }
        if (previousWord != null) {
            val bigramKey = bigramKey(previousWord, word)
            bigrams[bigramKey] = (bigrams[bigramKey] ?: 0L) + 1L
        }
    }
    
    override fun unlearn(word: String, previousWord: String?) {
        val key = word.lowercase()
        val existing = unigrams[key]
        if (existing != null) {
            val frequency = existing.frequency - 1L
            if (frequency <= 0L) {
                unigrams.remove(key)
            } else {
                unigrams[key] = existing.copy(frequency = frequency)
            }
        }
        if (previousWord != null) {
            val bigramKey = bigramKey(previousWord, word)
            val count = (bigrams[bigramKey] ?: 0L) - 1L
            if (count <= 0L) {
                bigrams.remove(bigramKey)
            } else {
                bigrams[bigramKey] = count
            }
        }
    }
    
    override fun unigramsByPrefix(prefix: String, limit: Int): List<WordEntry> {
        val normalized = prefix.lowercase()
        return unigrams
            .filter { (key, _) -> key.startsWith(normalized) }
            .map { (_, entry) -> entry }
            .sortedByDescending { it.frequency }
            .take(limit)
    }
    
    override fun bigramFrequency(previousWord: String, word: String): Long {
        return bigrams[bigramKey(previousWord, word)] ?: 0L
    }
    
    override fun nextWords(previousWord: String, limit: Int): List<String> {
        if (previousWord.isEmpty() || limit <= 0) {
            return emptyList()
        }
        // The prefix must use the same separator as bigramKey(), so successors of previousWord are matched.
        val prefix = previousWord.lowercase() + BIGRAM_SEPARATOR
        return bigrams.entries
            .filter { it.value > 0L && it.key.startsWith(prefix) }
            .sortedByDescending { it.value }
            .map { it.key.substring(prefix.length) }
            .map { successor -> unigrams[successor]?.word ?: successor }
            .take(limit)
    }
    
    override fun frequencyOf(word: String): Long {
        return unigrams[word.lowercase()]?.frequency ?: 0L
    }
    
    override fun isKnownWord(word: String): Boolean {
        return unigrams.containsKey(word.lowercase())
    }
    
    override fun partsOfSpeech(word: String): Set<PartOfSpeech> {
        return unigrams[word.lowercase()]?.partsOfSpeech ?: emptySet()
    }
    
    override fun allKnownWords(): List<String> {
        return unigrams.values.map { it.word }
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
    
    private fun bigramKey(previousWord: String, word: String): String {
        return previousWord.lowercase() + BIGRAM_SEPARATOR + word.lowercase()
    }
    
    companion object {
        
        /** Separator between the two halves of a bigram key; a space cannot occur inside a single word. */
        private const val BIGRAM_SEPARATOR = " "
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * Pure parser for the bundled per-language dictionary assets (real Wikipedia-derived lexicons).
 *
 * Word lines are `<word>\t<frequency>\t<pos,comma,separated>` (the POS field may be empty); bigram
 * lines are `<previousWord>\t<word>\t<count>`. Blank lines, malformed lines and unknown POS names are
 * skipped so one bad line never aborts the import. A stray carriage return is tolerated. Only parsing
 * lives here; the actual bulk insert into SQLite is done by {@link SqliteDictionaryStore}.
 */
object DictionaryAssetParser {
    
    /** A parsed bigram row. */
    data class Bigram(val previousWord: String, val word: String, val count: Long)
    
    /**
     * Parses word entries from the raw asset text.
     *
     * @param raw the `dict_<lang>.tsv` content
     * @return the parsed [WordEntry] list in file order (most frequent first, as emitted by the builder)
     */
    fun parseWords(raw: String): List<WordEntry> {
        val result = ArrayList<WordEntry>()
        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isBlank()) {
                return@forEach
            }
            val parts = line.split('\t')
            if (parts.size < 2 || parts[0].isEmpty()) {
                return@forEach
            }
            val frequency = parts[1].toLongOrNull() ?: return@forEach
            val pos = if (parts.size >= 3) parsePos(parts[2]) else emptySet()
            result.add(WordEntry(parts[0], frequency, pos))
        }
        return result
    }
    
    /**
     * Parses bigram rows from the raw asset text.
     *
     * @param raw the `bigram_<lang>.tsv` content
     * @return the parsed [Bigram] list in file order
     */
    fun parseBigrams(raw: String): List<Bigram> {
        val result = ArrayList<Bigram>()
        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isBlank()) {
                return@forEach
            }
            val parts = line.split('\t')
            if (parts.size < 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
                return@forEach
            }
            val count = parts[2].toLongOrNull() ?: return@forEach
            result.add(Bigram(parts[0], parts[1], count))
        }
        return result
    }
    
    private fun parsePos(raw: String): Set<PartOfSpeech> {
        if (raw.isBlank()) {
            return emptySet()
        }
        return raw.split(',')
            .mapNotNull { name -> runCatching { PartOfSpeech.valueOf(name.trim()) }.getOrNull() }
            .toSet()
    }
}

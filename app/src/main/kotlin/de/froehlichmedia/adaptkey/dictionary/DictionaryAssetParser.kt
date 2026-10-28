// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import java.io.Reader

/**
 * Pure parser for the bundled per-language dictionary assets (real Wikipedia-derived lexicons).
 * 
 * Word lines are `<word>\t<frequency>\t<pos,comma,separated>\t<lemma>` (the POS field may be empty; the
 * lemma field, D-412, is optional and omitted from most rows - see {@link WordEntry#lemma}); bigram
 * lines are `<previousWord>\t<word>\t<count>`. Blank lines, malformed lines and unknown POS names are
 * skipped so one bad line never aborts the import. A stray carriage return is tolerated. Only parsing
 * lives here; the actual bulk insert into SQLite is done by {@link SqliteDictionaryStore}.
 * 
 * D-505: beyond the original whole-`String` entry points there is a streaming one
 * ([forEachWordChunk]/[forEachBigramChunk]): the largest packs' text files (Russian: 73 MB `dict.tsv` +
 * 141 MB `bigram.tsv`) no longer fit a phone's app heap when materialised - as a `String` (UTF-16 doubles
 * the bytes) plus a fully-built entry list they need several hundred megabytes at once. Both shapes share
 * the exact same per-line logic ([parseWordLine]/[parseBigramLine]), so there is one parser, not two
 * divergent ones; the whole-`String` functions remain the convenient, fully-tested shape for the small
 * bundled assets and the test suite.
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
        return raw.lineSequence().mapNotNull(::parseWordLine).toList()
    }
    
    /**
     * Parses bigram rows from the raw asset text.
     * 
     * @param raw the `bigram_<lang>.tsv` content
     * @return the parsed [Bigram] list in file order
     */
    fun parseBigrams(raw: String): List<Bigram> {
        return raw.lineSequence().mapNotNull(::parseBigramLine).toList()
    }
    
    /**
     * D-505: streams [reader]'s word lines into [chunkSize]-sized batches, invoking [action] per complete
     * batch - so a caller can import a multi-hundred-megabyte lexicon into SQLite without ever holding
     * more than one chunk of entries in memory (a trailing partial chunk is delivered too). Uses the exact
     * same per-line logic as [parseWords]; the two entry points can never diverge. Closes [reader].
     * 
     * @param reader the UTF-8-decoded `dict.tsv` stream (closed by this call)
     * @param chunkSize how many parsed entries to accumulate per [action] invocation
     * @param action called once per complete chunk (plus a final partial one), in file order
     */
    fun forEachWordChunk(reader: Reader, chunkSize: Int, action: (List<WordEntry>) -> Unit) {
        reader.use { r ->
            r.useLines { lines ->
                lines.mapNotNull(::parseWordLine).chunked(chunkSize).forEach(action)
            }
        }
    }
    
    /**
     * D-505: the bigram counterpart of [forEachWordChunk] - streams [reader]'s bigram lines into
     * [chunkSize]-sized batches, invoking [action] per batch. Uses the exact same per-line logic as
     * [parseBigrams]. Closes [reader].
     * 
     * @param reader the UTF-8-decoded `bigram.tsv` stream (closed by this call)
     * @param chunkSize how many parsed rows to accumulate per [action] invocation
     * @param action called once per complete chunk (plus a final partial one), in file order
     */
    fun forEachBigramChunk(reader: Reader, chunkSize: Int, action: (List<Bigram>) -> Unit) {
        reader.use { r ->
            r.useLines { lines ->
                lines.mapNotNull(::parseBigramLine).chunked(chunkSize).forEach(action)
            }
        }
    }
    
    /** One dict.tsv line -> [WordEntry], or null for a blank/malformed line (never aborts the import). */
    private fun parseWordLine(rawLine: String): WordEntry? {
        val line = rawLine.trimEnd('\r')
        if (line.isBlank()) {
            return null
        }
        val parts = line.split('\t')
        if (parts.size < 2 || parts[0].isEmpty()) {
            return null
        }
        val frequency = parts[1].toLongOrNull() ?: return null
        val pos = if (parts.size >= 3) parsePos(parts[2]) else emptySet()
        val lemma = if (parts.size >= 4) parts[3].ifEmpty { null } else null
        return WordEntry(parts[0], frequency, pos, lemma)
    }
    
    /** One bigram.tsv line -> [Bigram], or null for a blank/malformed line (never aborts the import). */
    private fun parseBigramLine(rawLine: String): Bigram? {
        val line = rawLine.trimEnd('\r')
        if (line.isBlank()) {
            return null
        }
        val parts = line.split('\t')
        if (parts.size < 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return null
        }
        val count = parts[2].toLongOrNull() ?: return null
        return Bigram(parts[0], parts[1], count)
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

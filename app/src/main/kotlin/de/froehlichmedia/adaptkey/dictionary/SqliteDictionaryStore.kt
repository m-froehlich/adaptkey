// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Persistent {@link DictionaryStore} backed by SQLite (A-04: survives app updates).
 *
 * A thin data-access layer: all ranking and policy live in {@link DictionarySuggestionProvider}.
 * Case-insensitive matching uses an explicit lower-cased key column (rather than {@code COLLATE
 * NOCASE}, which does not fold German umlauts). This class is exercised by instrumented tests; the
 * store-independent logic is unit-tested through {@link InMemoryDictionaryStore}.
 */
class SqliteDictionaryStore(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION), DictionaryStore {
    
    private val db: SQLiteDatabase
        get() = writableDatabase
    
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE $TABLE_WORDS (wkey TEXT PRIMARY KEY, word TEXT NOT NULL, freq INTEGER NOT NULL, pos TEXT NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE $TABLE_BIGRAMS (prevkey TEXT NOT NULL, wkey TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY (prevkey, wkey))"
        )
        database.execSQL(
            "CREATE TABLE $TABLE_BLACKLIST (wkey TEXT PRIMARY KEY, category TEXT NOT NULL)"
        )
    }
    
    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_BIGRAMS")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_BLACKLIST")
        onCreate(database)
    }
    
    override fun putWord(entry: WordEntry) {
        putWordInternal(entry.word, entry.frequency, entry.partsOfSpeech)
    }
    
    /**
     * Bulk-imports a real dictionary (unigrams + bigrams) in a single transaction, for the one-time
     * first-run seeding from the bundled asset. Far faster than individual [putWord] / [putBigram]
     * calls for the ~100k-entry lexicons.
     *
     * @param words the unigram entries to insert
     * @param bigrams the bigram rows to insert
     */
    fun bulkImport(words: List<WordEntry>, bigrams: List<DictionaryAssetParser.Bigram>) {
        val database = db
        database.beginTransaction()
        try {
            words.forEach { putWordInternal(it.word, it.frequency, it.partsOfSpeech) }
            bigrams.forEach { putBigram(it.previousWord, it.word, it.count) }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
    
    override fun putBigram(previousWord: String, word: String, count: Long) {
        val values = ContentValues().apply {
            put("prevkey", previousWord.lowercase())
            put("wkey", word.lowercase())
            put("count", count)
        }
        db.insertWithOnConflict(TABLE_BIGRAMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun learn(word: String, previousWord: String?) {
        val existing = entryOf(word)
        val canonical = existing?.word ?: word
        val frequency = (existing?.frequency ?: 0L) + 1L
        val pos = existing?.partsOfSpeech ?: emptySet()
        putWordInternal(canonical, frequency, pos)
        if (previousWord != null) {
            putBigram(previousWord, word, bigramFrequency(previousWord, word) + 1L)
        }
    }
    
    override fun unigramsByPrefix(prefix: String, limit: Int): List<WordEntry> {
        val result = ArrayList<WordEntry>()
        db.rawQuery(
            "SELECT word, freq, pos FROM $TABLE_WORDS WHERE wkey LIKE ? ORDER BY freq DESC LIMIT ?",
            arrayOf(prefix.lowercase() + "%", limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2))))
            }
        }
        return result
    }
    
    override fun bigramFrequency(previousWord: String, word: String): Long {
        db.rawQuery(
            "SELECT count FROM $TABLE_BIGRAMS WHERE prevkey = ? AND wkey = ?",
            arrayOf(previousWord.lowercase(), word.lowercase())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }
    
    override fun nextWords(previousWord: String, limit: Int): List<String> {
        if (previousWord.isEmpty() || limit <= 0) {
            return emptyList()
        }
        // The LIMIT is inlined as a derived int (injection-safe). The join maps each successor key back to
        // its canonical-case word; the (prevkey, wkey) primary key makes the prevkey lookup fast.
        val result = ArrayList<String>()
        db.rawQuery(
            "SELECT w.word FROM $TABLE_BIGRAMS b JOIN $TABLE_WORDS w ON b.wkey = w.wkey WHERE b.prevkey = ? ORDER BY b.count DESC LIMIT $limit",
            arrayOf(previousWord.lowercase())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result
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
        val result = ArrayList<String>()
        db.rawQuery("SELECT word FROM $TABLE_WORDS", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result
    }
    
    /**
     * Bounded candidate set for the single-edit autocorrect: words whose lower-cased key starts with the
     * token's first character (an edit-distance-1 match keeps the first character, save the rare first-char
     * typo) and whose length is within one of the token's. The `wkey` primary-key index makes the
     * first-character range scan fast, so this replaces the whole-lexicon scan that made per-keystroke
     * autocorrect unusable with the real ~120k-word dictionaries.
     */
    override fun correctionCandidates(token: String): List<String> {
        val key = token.lowercase()
        if (key.isEmpty()) {
            return emptyList()
        }
        return correctionCandidates(token, setOf(key[0]))
    }
    
    override fun correctionCandidates(token: String, firstChars: Set<Char>): List<String> {
        val key = token.lowercase()
        if (key.isEmpty() || firstChars.isEmpty()) {
            return emptyList()
        }
        // The length bounds and LIMIT are inlined as integers: bound as text they would sort below any
        // integer length() and the BETWEEN would never match. They are derived ints, so this is injection-safe.
        val minLen = key.length - 1
        val maxLen = key.length + 1
        // D-38: search each candidate first-character bucket (the token's own char plus its keyboard
        // neighbours / umlaut variant), so a first-key typo or a missing initial umlaut can still be found.
        // Each bucket is an indexed first-char range scan, so the total stays bounded and cheap.
        // D-65 / D-63: order each bucket by descending frequency before the LIMIT cut. Without it the rows
        // come back in wkey order, and German umlaut letters (ö = U+00F6 etc.) sort after all of a-z, so a
        // common umlaut word like "können" fell past the LIMIT while a rare same-shape word ("kannen") stayed
        // - and "konnen" mis-corrected to "kannen". Frequency order keeps the umlaut words reachable.
        val perBucketLimit = maxOf(1, CANDIDATE_LIMIT / firstChars.size)
        val result = ArrayList<String>()
        for (firstChar in firstChars) {
            val codePoint = firstChar.code
            val lower = String(Character.toChars(codePoint))
            val upper = String(Character.toChars(codePoint + 1))
            db.rawQuery(
                "SELECT word FROM $TABLE_WORDS WHERE wkey >= ? AND wkey < ? AND length(wkey) BETWEEN $minLen AND $maxLen ORDER BY freq DESC LIMIT $perBucketLimit",
                arrayOf(lower, upper)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(0))
                }
            }
        }
        return result
    }
    
    override fun blacklist(word: String, category: BlacklistCategory) {
        val values = ContentValues().apply {
            put("wkey", word.lowercase())
            put("category", category.name)
        }
        db.insertWithOnConflict(TABLE_BLACKLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun unblacklist(word: String) {
        db.delete(TABLE_BLACKLIST, "wkey = ?", arrayOf(word.lowercase()))
    }
    
    override fun isBlacklisted(word: String): Boolean {
        return blacklistCategory(word) != null
    }
    
    override fun blacklistCategory(word: String): BlacklistCategory? {
        db.rawQuery(
            "SELECT category FROM $TABLE_BLACKLIST WHERE wkey = ?",
            arrayOf(word.lowercase())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            return runCatching { BlacklistCategory.valueOf(cursor.getString(0)) }.getOrNull()
        }
    }
    
    override fun blacklistedWords(): List<String> {
        val result = ArrayList<String>()
        db.rawQuery("SELECT wkey FROM $TABLE_BLACKLIST ORDER BY wkey ASC", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result
    }
    
    override fun isEmpty(): Boolean {
        db.rawQuery("SELECT 1 FROM $TABLE_WORDS LIMIT 1", null).use { cursor ->
            return !cursor.moveToFirst()
        }
    }
    
    private fun entryOf(word: String): WordEntry? {
        db.rawQuery(
            "SELECT word, freq, pos FROM $TABLE_WORDS WHERE wkey = ?",
            arrayOf(word.lowercase())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            return WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2)))
        }
    }
    
    private fun putWordInternal(word: String, frequency: Long, pos: Set<PartOfSpeech>) {
        val values = ContentValues().apply {
            put("wkey", word.lowercase())
            put("word", word)
            put("freq", frequency)
            put("pos", pos.joinToString(",") { it.name })
        }
        db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    private fun parsePos(raw: String): Set<PartOfSpeech> {
        if (raw.isBlank()) {
            return emptySet()
        }
        return raw.split(",")
            .mapNotNull { name -> runCatching { PartOfSpeech.valueOf(name) }.getOrNull() }
            .toSet()
    }
    
    companion object {
        
        private const val DATABASE_NAME = "adaptkey_dictionary.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_WORDS = "words"
        private const val TABLE_BIGRAMS = "bigrams"
        private const val TABLE_BLACKLIST = "blacklist"
        
        // Upper bound on autocorrect candidates scanned per keystroke (bounds worst-case latency).
        private const val CANDIDATE_LIMIT = 2000
    }
}

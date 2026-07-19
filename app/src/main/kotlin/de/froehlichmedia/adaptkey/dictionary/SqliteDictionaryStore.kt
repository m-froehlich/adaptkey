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
 *
 * D-177: the bundled dictionary ({@link #TABLE_WORDS} / {@link #TABLE_BIGRAMS}, seeded once from the
 * asset) and the user's own learned vocabulary ({@link #TABLE_LEARNED} / {@link #TABLE_LEARNED_BIGRAMS},
 * written only by {@link #learn}/{@link #unlearn}/{@link #forget}) are kept in entirely separate
 * tables, by direct instruction - the bundled tables must stay untouched by adaptive learning, so a
 * future dictionary-asset update can replace them cleanly without resetting anything the user has
 * personally taught the keyboard. Every read that represents "does the keyboard know this word"
 * (frequency, known-ness, part of speech, prefix/fuzzy candidate search, bigram successors) merges both
 * sources; only {@link #isBundledWord} and {@link #learnedWords} look at a single source deliberately,
 * since callers need exactly that distinction (a real dictionary word must be permanently blacklisted to
 * be suppressed; a purely self-taught one can simply be forgotten - see {@link #forget}).
 */
class SqliteDictionaryStore(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION), DictionaryStore {
    
    private val db: SQLiteDatabase
        get() = writableDatabase
    
    init {
        // D-177/D-178: additive and idempotent (CREATE TABLE IF NOT EXISTS), so it reaches an
        // already-installed device's existing database - whose onCreate() ran long before these tables
        // existed - without any destructive DATABASE_VERSION bump/reimport, exactly like §107's
        // bundled-blacklist seeding. Also covers the ordinary fresh-install path, where onCreate() below
        // already created everything and this is a harmless no-op.
        ensureAdditiveSchema(db)
    }
    
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
        ensureAdditiveSchema(database)
    }
    
    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_BIGRAMS")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_BLACKLIST")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNED")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNED_BIGRAMS")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_BLACKLIST")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_META")
        onCreate(database)
    }
    
    private fun ensureAdditiveSchema(database: SQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_LEARNED (wkey TEXT PRIMARY KEY, word TEXT NOT NULL, freq INTEGER NOT NULL, pos TEXT NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_LEARNED_BIGRAMS (prevkey TEXT NOT NULL, wkey TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY (prevkey, wkey))"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_PENDING_BLACKLIST (wkey TEXT PRIMARY KEY, ts INTEGER NOT NULL)"
        )
        // D-178: tracks the bundled dictionary content version so DictionaryLoader can tell an
        // already-seeded store apart from one still holding pre-D-177 words that were learned straight into
        // TABLE_WORDS, back when learn() had no separate table to write to yet.
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_META (key TEXT PRIMARY KEY, value TEXT NOT NULL)"
        )
    }
    
    override fun putWord(entry: WordEntry) {
        putWordInternal(TABLE_WORDS, entry.word, entry.frequency, entry.partsOfSpeech)
    }
    
    /**
     * Bulk-imports a real dictionary (unigrams + bigrams) in a single transaction, for the one-time
     * first-run seeding from the bundled asset. Far faster than individual [putWord] / [putBigram]
     * calls for the ~100k-entry lexicons. Always the bundled tables - never the learned ones.
     *
     * @param words the unigram entries to insert
     * @param bigrams the bigram rows to insert
     */
    fun bulkImport(words: List<WordEntry>, bigrams: List<DictionaryAssetParser.Bigram>) {
        val database = db
        database.beginTransaction()
        try {
            words.forEach { putWordInternal(TABLE_WORDS, it.word, it.frequency, it.partsOfSpeech) }
            bigrams.forEach { putBigramInternal(TABLE_BIGRAMS, it.previousWord, it.word, it.count) }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
    
    /**
     * D-178: wipes only the bundled tables ([TABLE_WORDS] / [TABLE_BIGRAMS]), leaving the learned overlay,
     * the blacklist, and the pending-blacklist marks untouched, so [DictionaryLoader] can reseed a clean
     * copy of the bundled asset - flushing out any word that was learned straight into [TABLE_WORDS] by a
     * pre-D-177 build, back before [learn] had a separate table of its own to write to, and that would
     * otherwise sit there forever, indistinguishable from a real dictionary entry.
     */
    fun resetBundledWords() {
        val database = db
        database.beginTransaction()
        try {
            database.execSQL("DELETE FROM $TABLE_WORDS")
            database.execSQL("DELETE FROM $TABLE_BIGRAMS")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
    
    /**
     * D-178: the bundled dictionary content version last seeded into this store, or 0 if never recorded
     * (every store that predates this mechanism).
     *
     * @return the recorded version, or 0 if none is recorded yet
     */
    fun bundledContentVersion(): Int {
        db.rawQuery("SELECT value FROM $TABLE_META WHERE key = ?", arrayOf(META_KEY_BUNDLED_VERSION)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0).toIntOrNull() ?: 0 else 0
        }
    }
    
    /**
     * Records the bundled dictionary content version this store now holds, so a later
     * [DictionaryLoader.loadStores] call does not reseed it again until the constant is bumped further.
     *
     * @param version the version to record
     */
    fun setBundledContentVersion(version: Int) {
        val values = ContentValues().apply {
            put("key", META_KEY_BUNDLED_VERSION)
            put("value", version.toString())
        }
        db.insertWithOnConflict(TABLE_META, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    /**
     * D-186: removes every [TABLE_LEARNED] / [TABLE_LEARNED_BIGRAMS] row whose key also exists in
     * [TABLE_WORDS] - a one-time flush for installs that accumulated bundled-word duplicates in the
     * learned overlay before [learn] stopped writing them there at all (see [learn]'s own KDoc). Never
     * touches [TABLE_WORDS]/[TABLE_BIGRAMS], the blacklist, or a genuinely self-taught word (one with no
     * bundled counterpart).
     */
    fun purgeBundledDuplicatesFromLearned() {
        val database = db
        database.beginTransaction()
        try {
            database.execSQL("DELETE FROM $TABLE_LEARNED WHERE wkey IN (SELECT wkey FROM $TABLE_WORDS)")
            database.execSQL("DELETE FROM $TABLE_LEARNED_BIGRAMS WHERE wkey IN (SELECT wkey FROM $TABLE_WORDS)")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
    
    /**
     * D-186: the learned-overlay cleanup version last applied to this store, or 0 if never recorded -
     * mirrors [bundledContentVersion]'s own versioning scheme, but for [purgeBundledDuplicatesFromLearned]
     * instead of a bundled reseed.
     *
     * @return the recorded version, or 0 if none is recorded yet
     */
    fun learnedCleanupVersion(): Int {
        db.rawQuery("SELECT value FROM $TABLE_META WHERE key = ?", arrayOf(META_KEY_LEARNED_CLEANUP_VERSION)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0).toIntOrNull() ?: 0 else 0
        }
    }
    
    /**
     * Records the learned-overlay cleanup version this store now holds, so a later
     * [DictionaryLoader.loadStores] call does not run [purgeBundledDuplicatesFromLearned] again until the
     * constant is bumped further.
     *
     * @param version the version to record
     */
    fun setLearnedCleanupVersion(version: Int) {
        val values = ContentValues().apply {
            put("key", META_KEY_LEARNED_CLEANUP_VERSION)
            put("value", version.toString())
        }
        db.insertWithOnConflict(TABLE_META, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun putBigram(previousWord: String, word: String, count: Long) {
        putBigramInternal(TABLE_BIGRAMS, previousWord, word, count)
    }
    
    override fun learn(word: String, previousWord: String?) {
        // D-177: always the learned table, regardless of whether word is also a bundled entry - reinforcing
        // an already-bundled word (e.g. "der") adds/updates a small personal overlay here rather than ever
        // touching TABLE_WORDS, so the bundled asset stays swappable. frequencyOf()/isKnownWord() etc. sum
        // both sources back together for ranking, so personalisation still works exactly as before from the
        // caller's perspective.
        val existing = learnedEntryOf(word)
        val canonical = existing?.word ?: bundledEntryOf(word)?.word ?: word
        val frequency = (existing?.frequency ?: 0L) + 1L
        val pos = existing?.partsOfSpeech ?: emptySet()
        putWordInternal(TABLE_LEARNED, canonical, frequency, pos)
        if (previousWord != null) {
            putBigramInternal(TABLE_LEARNED_BIGRAMS, previousWord, word, learnedBigramFrequency(previousWord, word) + 1L)
        }
    }
    
    override fun unlearn(word: String, previousWord: String?) {
        val existing = learnedEntryOf(word)
        if (existing != null) {
            val frequency = existing.frequency - 1L
            if (frequency <= 0L) {
                db.delete(TABLE_LEARNED, "wkey = ?", arrayOf(word.lowercase()))
            } else {
                putWordInternal(TABLE_LEARNED, existing.word, frequency, existing.partsOfSpeech)
            }
        }
        if (previousWord != null) {
            val count = learnedBigramFrequency(previousWord, word) - 1L
            if (count <= 0L) {
                db.delete(TABLE_LEARNED_BIGRAMS, "prevkey = ? AND wkey = ?", arrayOf(previousWord.lowercase(), word.lowercase()))
            } else {
                putBigramInternal(TABLE_LEARNED_BIGRAMS, previousWord, word, count)
            }
        }
    }
    
    override fun forget(word: String) {
        db.delete(TABLE_LEARNED, "wkey = ?", arrayOf(word.lowercase()))
    }
    
    override fun isBundledWord(word: String): Boolean {
        return bundledEntryOf(word) != null
    }
    
    override fun learnedWords(): List<WordEntry> {
        val result = ArrayList<WordEntry>()
        db.rawQuery("SELECT word, freq, pos FROM $TABLE_LEARNED ORDER BY freq DESC, word ASC", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2))))
            }
        }
        return result
    }
    
    override fun markPendingBlacklist(word: String, timestampMillis: Long) {
        val values = ContentValues().apply {
            put("wkey", word.lowercase())
            put("ts", timestampMillis)
        }
        db.insertWithOnConflict(TABLE_PENDING_BLACKLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun pendingBlacklistedSince(word: String): Long? {
        db.rawQuery("SELECT ts FROM $TABLE_PENDING_BLACKLIST WHERE wkey = ?", arrayOf(word.lowercase())).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }
    
    override fun clearPendingBlacklist(word: String) {
        db.delete(TABLE_PENDING_BLACKLIST, "wkey = ?", arrayOf(word.lowercase()))
    }
    
    override fun unigramsByPrefix(prefix: String, limit: Int): List<WordEntry> {
        val merged = LinkedHashMap<String, WordEntry>()
        queryByPrefix(TABLE_WORDS, prefix, limit).forEach { merged[it.word.lowercase()] = it }
        queryByPrefix(TABLE_LEARNED, prefix, limit).forEach { entry ->
            val key = entry.word.lowercase()
            val existing = merged[key]
            merged[key] = if (existing != null) existing.copy(frequency = existing.frequency + entry.frequency) else entry
        }
        return merged.values.sortedByDescending { it.frequency }.take(limit)
    }
    
    private fun queryByPrefix(table: String, prefix: String, limit: Int): List<WordEntry> {
        val result = ArrayList<WordEntry>()
        db.rawQuery(
            "SELECT word, freq, pos FROM $table WHERE wkey LIKE ? ORDER BY freq DESC LIMIT ?",
            arrayOf(prefix.lowercase() + "%", limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2))))
            }
        }
        return result
    }
    
    override fun bigramFrequency(previousWord: String, word: String): Long {
        return bundledBigramFrequency(previousWord, word) + learnedBigramFrequency(previousWord, word)
    }
    
    private fun bundledBigramFrequency(previousWord: String, word: String): Long {
        return bigramFrequencyIn(TABLE_BIGRAMS, previousWord, word)
    }
    
    private fun learnedBigramFrequency(previousWord: String, word: String): Long {
        return bigramFrequencyIn(TABLE_LEARNED_BIGRAMS, previousWord, word)
    }
    
    private fun bigramFrequencyIn(table: String, previousWord: String, word: String): Long {
        db.rawQuery(
            "SELECT count FROM $table WHERE prevkey = ? AND wkey = ?",
            arrayOf(previousWord.lowercase(), word.lowercase())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }
    
    override fun nextWords(previousWord: String, limit: Int): List<String> {
        if (previousWord.isEmpty() || limit <= 0) {
            return emptyList()
        }
        val counts = LinkedHashMap<String, Long>()
        for (table in listOf(TABLE_BIGRAMS, TABLE_LEARNED_BIGRAMS)) {
            db.rawQuery("SELECT wkey, count FROM $table WHERE prevkey = ?", arrayOf(previousWord.lowercase())).use { cursor ->
                while (cursor.moveToNext()) {
                    val wkey = cursor.getString(0)
                    counts[wkey] = (counts[wkey] ?: 0L) + cursor.getLong(1)
                }
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (wkey, _) -> canonicalWordFor(wkey) }
    }
    
    private fun canonicalWordFor(wkey: String): String {
        return learnedEntryOf(wkey)?.word ?: bundledEntryOf(wkey)?.word ?: wkey
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
        db.rawQuery("SELECT word FROM $TABLE_WORDS", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        db.rawQuery("SELECT word FROM $TABLE_LEARNED", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result.toList()
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
        // D-177: also searched against the (small) learned table, deduplicated by exact canonical form - a
        // word reinforced in both tables shares the same canonical case (learn() resolves it from whichever
        // already exists), so a plain set is enough; frequencyOf() further downstream already merges both
        // sources' frequency for ranking regardless of which bucket a given candidate came from here.
        val result = LinkedHashSet<String>()
        for (firstChar in firstChars) {
            val codePoint = firstChar.code
            val lower = String(Character.toChars(codePoint))
            val upper = String(Character.toChars(codePoint + 1))
            result.addAll(bucketQuery(TABLE_WORDS, lower, upper, minLen, maxLen, perBucketLimit))
            result.addAll(bucketQuery(TABLE_LEARNED, lower, upper, minLen, maxLen, perBucketLimit))
        }
        return result.toList()
    }
    
    private fun bucketQuery(table: String, lower: String, upper: String, minLen: Int, maxLen: Int, limit: Int): List<String> {
        val result = ArrayList<String>()
        db.rawQuery(
            "SELECT word FROM $table WHERE wkey >= ? AND wkey < ? AND length(wkey) BETWEEN $minLen AND $maxLen ORDER BY freq DESC LIMIT $limit",
            arrayOf(lower, upper)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
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
        val bundled = bundledEntryOf(word)
        val learned = learnedEntryOf(word)
        return when {
            bundled == null -> learned
            learned == null -> bundled
            else -> WordEntry(bundled.word, bundled.frequency + learned.frequency, bundled.partsOfSpeech + learned.partsOfSpeech)
        }
    }
    
    private fun bundledEntryOf(word: String): WordEntry? {
        return entryOfIn(TABLE_WORDS, word)
    }
    
    private fun learnedEntryOf(word: String): WordEntry? {
        return entryOfIn(TABLE_LEARNED, word)
    }
    
    private fun entryOfIn(table: String, word: String): WordEntry? {
        db.rawQuery(
            "SELECT word, freq, pos FROM $table WHERE wkey = ?",
            arrayOf(word.lowercase())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            return WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2)))
        }
    }
    
    private fun putWordInternal(table: String, word: String, frequency: Long, pos: Set<PartOfSpeech>) {
        val values = ContentValues().apply {
            put("wkey", word.lowercase())
            put("word", word)
            put("freq", frequency)
            put("pos", pos.joinToString(",") { it.name })
        }
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    private fun putBigramInternal(table: String, previousWord: String, word: String, count: Long) {
        val values = ContentValues().apply {
            put("prevkey", previousWord.lowercase())
            put("wkey", word.lowercase())
            put("count", count)
        }
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
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
        private const val TABLE_LEARNED = "learned"
        private const val TABLE_LEARNED_BIGRAMS = "learned_bigrams"
        private const val TABLE_PENDING_BLACKLIST = "pending_blacklist"
        private const val TABLE_META = "meta"
        private const val META_KEY_BUNDLED_VERSION = "bundled_version"
        private const val META_KEY_LEARNED_CLEANUP_VERSION = "learned_cleanup_version"
        
        // Upper bound on autocorrect candidates scanned per keystroke (bounds worst-case latency).
        private const val CANDIDATE_LIMIT = 2000
    }
}

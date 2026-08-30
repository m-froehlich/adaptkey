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
        ensureLastTouchedColumn(db)
        ensureLemmaColumn(db)
    }
    
    /**
     * D-212: enables write-ahead logging - without it, SQLite's default rollback-journal mode serialises
     * *every* access to this connection across threads, so [de.froehlichmedia.adaptkey.AdaptKeyService]'s
     * D-211 background suggestion search (reading via [correctionCandidates]) would still block the main
     * thread's own per-keystroke prefix-completion read ([unigramsByPrefix]) behind the same lock -
     * confirmed as the likely cause after D-211 alone showed no measured improvement (a fresh device log
     * looked no better, in places worse, right after D-209 made the background read itself larger). WAL
     * allows genuine concurrent readers, which is exactly this situation: one thread reading in the
     * background while another reads (and occasionally writes, e.g. [learn]) on the main thread.
     */
    override fun onConfigure(database: SQLiteDatabase) {
        super.onConfigure(database)
        database.enableWriteAheadLogging()
    }
    
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE $TABLE_WORDS (wkey TEXT PRIMARY KEY, word TEXT NOT NULL, freq INTEGER NOT NULL, pos TEXT NOT NULL, lemma TEXT)"
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
        database.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNED_TRIGRAMS")
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
        // D-246: S-07 trigram support - personal-only (no bundled counterpart, unlike the bigram tables),
        // so there is only ever a single "learned" table here, not a bundled/learned pair. Additive/
        // idempotent like every other table in this method, reaching an already-installed device without a
        // DATABASE_VERSION bump.
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_LEARNED_TRIGRAMS " +
                "(w1key TEXT NOT NULL, w2key TEXT NOT NULL, wkey TEXT NOT NULL, count INTEGER NOT NULL, " +
                "PRIMARY KEY (w1key, w2key, wkey))"
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
    
    /**
     * D-388: adds [TABLE_LEARNED]'s `last_touched` column (epoch millis, stamped on every write - see
     * [putWordInternal]) when this database predates it - guarded, not blindly re-run every open, since
     * SQLite's own `ALTER TABLE ADD COLUMN` fails outright if the column already exists (unlike
     * [ensureAdditiveSchema]'s `CREATE TABLE IF NOT EXISTS` calls, which are naturally idempotent).
     * Existing rows are seeded with strictly increasing timestamps, one second apart, in alphabetical
     * order - not the arbitrary order SQLite would otherwise leave them in - so a fresh "most recently
     * used" view over already-existing data reads as a stable, alphabetically-ordered block (the
     * oldest-looking entries) rather than looking shuffled. Any word actually learned after this
     * migration gets a real [System.currentTimeMillis] timestamp, astronomically larger than this seed
     * range, so it naturally sorts above every seeded row without any further work.
     */
    private fun ensureLastTouchedColumn(database: SQLiteDatabase) {
        val hasColumn = database.rawQuery("PRAGMA table_info($TABLE_LEARNED)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }.any { it == "last_touched" }
        }
        if (hasColumn) {
            return
        }
        database.execSQL("ALTER TABLE $TABLE_LEARNED ADD COLUMN last_touched INTEGER NOT NULL DEFAULT 0")
        val wkeys = ArrayList<String>()
        database.rawQuery("SELECT wkey FROM $TABLE_LEARNED", null).use { cursor ->
            while (cursor.moveToNext()) {
                wkeys.add(cursor.getString(0))
            }
        }
        database.beginTransaction()
        try {
            wkeys.sorted().forEachIndexed { index, wkey ->
                database.execSQL(
                    "UPDATE $TABLE_LEARNED SET last_touched = ? WHERE wkey = ?",
                    arrayOf(index.toLong() * SEED_TIMESTAMP_STEP_MS, wkey)
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
    
    /**
     * D-412: adds [TABLE_WORDS]'s `lemma` column when this database predates it - same guarded-`ALTER
     * TABLE` pattern as [ensureLastTouchedColumn] (`PRAGMA table_info` presence check, since `ADD COLUMN`
     * fails outright if the column already exists). Unlike that migration, no existing-row backfill is
     * needed: [TABLE_WORDS] is entirely reseedable from the language-pack asset ([resetBundledWords] +
     * [bulkImport]), so a `NULL` default for every pre-existing row is simply correct until the next
     * reimport populates the real links - never [TABLE_LEARNED], which has no `lemma` column at all and
     * must never be touched by this bundled-only migration.
     */
    private fun ensureLemmaColumn(database: SQLiteDatabase) {
        val hasColumn = database.rawQuery("PRAGMA table_info($TABLE_WORDS)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }.any { it == "lemma" }
        }
        if (hasColumn) {
            return
        }
        database.execSQL("ALTER TABLE $TABLE_WORDS ADD COLUMN lemma TEXT")
    }
    
    override fun putWord(entry: WordEntry) {
        putWordInternal(TABLE_WORDS, entry.word, entry.frequency, entry.partsOfSpeech, lemma = entry.lemma)
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
            words.forEach { putWordInternal(TABLE_WORDS, it.word, it.frequency, it.partsOfSpeech, lemma = it.lemma) }
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
     * D-334: the version of the installed language-pack content last seeded into this store's
     * [TABLE_WORDS]/[TABLE_BIGRAMS], or 0 if never recorded (every store that predates this mechanism, or
     * a bundled-language store that is never tracked this way). Mirrors [bundledContentVersion]'s own
     * scheme, but for a D-280-installed pack rather than a bundled asset - lets [DictionaryLoader] reseed
     * only the seeded tables (leaving the learned overlay intact) when a newer pack was imported, instead of
     * the previous [android.content.Context.deleteDatabase] wipe that destroyed every learned word.
     * 
     * @return the recorded installed-pack version, or 0 if none is recorded yet
     */
    fun installedPackVersion(): Int {
        db.rawQuery("SELECT value FROM $TABLE_META WHERE key = ?", arrayOf(META_KEY_INSTALLED_PACK_VERSION)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0).toIntOrNull() ?: 0 else 0
        }
    }
    
    /**
     * Records the installed language-pack version this store now holds, so a later
     * [DictionaryLoader.loadStores] call does not reseed it again until [InstalledLanguagesStore.
     * installedVersion] moves further.
     * 
     * @param version the version to record
     */
    fun setInstalledPackVersion(version: Int) {
        val values = ContentValues().apply {
            put("key", META_KEY_INSTALLED_PACK_VERSION)
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
    
    /**
     * D-327: removes the [previousWord] -> [word] bigram from BOTH the bundled table ([TABLE_BIGRAMS]) and
     * the personal learned table ([TABLE_LEARNED_BIGRAMS]) - a one-time, versioned purge for a specific
     * bundled bigram row that should never have shipped (e.g. "mein" -> "kampf", a Wikipedia-corpus
     * extraction artefact, not anything a user typed). Idempotent and a harmless no-op for a store that
     * never held the row, so it runs uniformly across every language store without a per-language guard.
     * Never touches unigrams, trigrams, the blacklist, or any other bigram.
     * 
     * @param previousWord the bigram's context word (any case)
     * @param word the predicted word (any case)
     */
    fun purgeBigram(previousWord: String, word: String) {
        val database = db
        database.beginTransaction()
        try {
            database.execSQL(
                "DELETE FROM $TABLE_BIGRAMS WHERE prevkey = ? AND wkey = ?",
                arrayOf(previousWord.lowercase(), word.lowercase())
            )
            database.execSQL(
                "DELETE FROM $TABLE_LEARNED_BIGRAMS WHERE prevkey = ? AND wkey = ?",
                arrayOf(previousWord.lowercase(), word.lowercase())
            )
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
    
    /**
     * D-327: the bigram-purge cleanup version last applied to this store, or 0 if never recorded -
     * mirrors [bundledContentVersion]'s own versioning scheme, but for [purgeBigram] instead of a bundled
     * reseed.
     * 
     * @return the recorded version, or 0 if none is recorded yet
     */
    fun bigramCleanupVersion(): Int {
        db.rawQuery("SELECT value FROM $TABLE_META WHERE key = ?", arrayOf(META_KEY_BIGRAM_CLEANUP_VERSION)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0).toIntOrNull() ?: 0 else 0
        }
    }
    
    /**
     * Records the bigram-purge cleanup version this store now holds, so a later
     * [DictionaryLoader.loadStores] call does not run [purgeBigram] again until the constant is bumped
     * further.
     * 
     * @param version the version to record
     */
    fun setBigramCleanupVersion(version: Int) {
        val values = ContentValues().apply {
            put("key", META_KEY_BIGRAM_CLEANUP_VERSION)
            put("value", version.toString())
        }
        db.insertWithOnConflict(TABLE_META, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    override fun putBigram(previousWord: String, word: String, count: Long) {
        putBigramInternal(TABLE_BIGRAMS, previousWord, word, count)
    }
    
    override fun learn(word: String, previousWord: String?, previousPreviousWord: String?, seedFrequency: Long) {
        // D-177: always the learned table, regardless of whether word is also a bundled entry - reinforcing
        // an already-bundled word (e.g. "der") adds/updates a small personal overlay here rather than ever
        // touching TABLE_WORDS, so the bundled asset stays swappable. frequencyOf()/isKnownWord() etc. sum
        // both sources back together for ranking, so personalisation still works exactly as before from the
        // caller's perspective.
        val existing = learnedEntryOf(word)
        // D-264: a fresh learned entry uses the casing actually typed/committed, not the bundled entry's
        // own (if any) - letting a deliberately different-cased spelling (e.g. a preferred all-caps
        // acronym) become the one that wins in entryOf()/unigramsByPrefix() merges, instead of being
        // silently discarded in favour of whatever the bundled asset happens to store.
        val canonical = existing?.word ?: word
        // D-388: a genuinely new entry starts at seedFrequency (the caller's own choice - e.g. the pending
        // count already accumulated before promotion, see AdaptKeyService.learnWord()) rather than always
        // 1; an already-existing entry is reinforced exactly as before, ignoring seedFrequency entirely -
        // it only ever seeds a fresh row, never overrides an ongoing count.
        val frequency = existing?.let { it.frequency + 1L } ?: seedFrequency
        val pos = existing?.partsOfSpeech ?: emptySet()
        putWordInternal(TABLE_LEARNED, canonical, frequency, pos, lastTouched = System.currentTimeMillis())
        if (previousWord != null) {
            putBigramInternal(TABLE_LEARNED_BIGRAMS, previousWord, word, learnedBigramFrequency(previousWord, word) + 1L)
            // D-246: S-07 trigram support - personal-only, so only ever written here, never seeded.
            if (previousPreviousWord != null) {
                putTrigramInternal(
                    previousPreviousWord, previousWord, word,
                    trigramFrequency(previousPreviousWord, previousWord, word) + 1L
                )
            }
        }
    }
    
    override fun learnContext(word: String, previousWord: String?, previousPreviousWord: String?) {
        // D-327: only the n-gram context, never the unigram - mirrors learn()'s own n-gram block exactly,
        // omitting only the putWordInternal(TABLE_LEARNED, ...) above. See the interface KDoc.
        if (previousWord != null) {
            putBigramInternal(TABLE_LEARNED_BIGRAMS, previousWord, word, learnedBigramFrequency(previousWord, word) + 1L)
            if (previousPreviousWord != null) {
                putTrigramInternal(
                    previousPreviousWord, previousWord, word,
                    trigramFrequency(previousPreviousWord, previousWord, word) + 1L
                )
            }
        }
    }
    
    override fun unlearn(word: String, previousWord: String?, previousPreviousWord: String?) {
        val existing = learnedEntryOf(word)
        if (existing != null) {
            val frequency = existing.frequency - 1L
            if (frequency <= 0L) {
                db.delete(TABLE_LEARNED, "wkey = ?", arrayOf(word.lowercase()))
            } else {
                putWordInternal(TABLE_LEARNED, existing.word, frequency, existing.partsOfSpeech, lastTouched = System.currentTimeMillis())
            }
        }
        if (previousWord != null) {
            val count = learnedBigramFrequency(previousWord, word) - 1L
            if (count <= 0L) {
                db.delete(TABLE_LEARNED_BIGRAMS, "prevkey = ? AND wkey = ?", arrayOf(previousWord.lowercase(), word.lowercase()))
            } else {
                putBigramInternal(TABLE_LEARNED_BIGRAMS, previousWord, word, count)
            }
            if (previousPreviousWord != null) {
                val trigramCount = trigramFrequency(previousPreviousWord, previousWord, word) - 1L
                if (trigramCount <= 0L) {
                    db.delete(
                        TABLE_LEARNED_TRIGRAMS, "w1key = ? AND w2key = ? AND wkey = ?",
                        arrayOf(previousPreviousWord.lowercase(), previousWord.lowercase(), word.lowercase())
                    )
                } else {
                    putTrigramInternal(previousPreviousWord, previousWord, word, trigramCount)
                }
            }
        }
    }
    
    override fun forget(word: String) {
        db.delete(TABLE_LEARNED, "wkey = ?", arrayOf(word.lowercase()))
    }
    
    override fun isBundledWord(word: String): Boolean {
        return bundledEntryOf(word) != null
    }
    
    override fun bundledCasingOf(word: String): String? {
        return bundledEntryOf(word)?.word
    }
    
    override fun learnedCasingOf(word: String): String? {
        return learnedEntryOf(word)?.word
    }
    
    override fun learnedFrequencyOf(word: String): LearnedFrequency? {
        db.rawQuery(
            "SELECT freq, last_touched FROM $TABLE_LEARNED WHERE wkey = ?",
            arrayOf(word.lowercase())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            return LearnedFrequency(cursor.getLong(0), cursor.getLong(1))
        }
    }
    
    /**
     * D-292: updates an already-learned word's own stored casing, keeping its frequency and part-of-speech
     * tags exactly as they were - the Learned Words editor's own "fix only the casing" action. A no-op when
     * [word] is not currently a learned entry at all. The caller is responsible for ensuring [newCasing] is
     * case-insensitively identical to [word] before calling - this method itself does not re-check that,
     * matching every other store method's "caller enforces intent" contract; since both share the same
     * lower-cased key, the underlying row is updated in place rather than creating a second entry.
     * 
     * @param word the learned word to re-case (any case)
     * @param newCasing the corrected spelling to store instead
     */
    fun recaseLearnedWord(word: String, newCasing: String) {
        val existing = learnedEntryOf(word) ?: return
        putWordInternal(TABLE_LEARNED, newCasing, existing.frequency, existing.partsOfSpeech, lastTouched = System.currentTimeMillis())
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
    
    /**
     * D-388: [learnedWords] plus each entry's own `last_touched` stamp, for [de.froehlichmedia.adaptkey.
     * settings.LearnedWordsActivity]'s own sortable view - not part of the shared [DictionaryStore]
     * interface (frequency-only ordering, via [learnedWords], is all every other caller needs, e.g.
     * [de.froehlichmedia.adaptkey.backup.BackupExporter]), and not added onto [WordEntry] itself, which is
     * shared far too widely across the suggestion/correction engine to carry a field only this one screen
     * cares about. Unordered - the caller decides the actual display order (alphabetical or by recency).
     *
     * @return every learned entry with its own last-touched timestamp (epoch millis)
     */
    fun learnedWordsWithTimestamp(): List<LearnedWordEntry> {
        val result = ArrayList<LearnedWordEntry>()
        db.rawQuery("SELECT word, freq, last_touched FROM $TABLE_LEARNED", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(LearnedWordEntry(cursor.getString(0), cursor.getLong(1), cursor.getLong(2)))
            }
        }
        return result
    }
    
    
    /**
     * B-03/D-289: every learned hyphen-joined compound (e.g. "Trogata-Team") whose key starts with [prefix] -
     * the store side of the proactive compound-completion chip. A compound only ever exists in
     * [TABLE_LEARNED] once it has actually been promoted past [de.froehlichmedia.adaptkey.AdaptKeyService]'s
     * own compound threshold ([learn] is only ever called on promotion, never while merely pending), so no
     * separate "is this actually promoted" filter is needed here - the row's mere presence already means it
     * is.
     * 
     * @param prefix the current composing token (any case)
     * @param limit the maximum number of rows to return
     * @return matching compound entries, ordered by descending frequency
     */
    override fun learnedHyphenCompoundsByPrefix(prefix: String, limit: Int): List<WordEntry> {
        val result = ArrayList<WordEntry>()
        db.rawQuery(
            "SELECT word, freq, pos FROM $TABLE_LEARNED WHERE wkey LIKE ? AND wkey LIKE '%-%' ORDER BY freq DESC LIMIT ?",
            arrayOf(prefix.lowercase() + "%", limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2))))
            }
        }
        return result
    }
    
    /**
     * D-278: every learned bigram row, for the backup/export feature (§21). Keys are already lower-cased,
     * exactly as [nextWords] itself reads them - canonical casing is resolved elsewhere, not carried by the
     * bigram tables.
     * 
     * @return every row of [TABLE_LEARNED_BIGRAMS], in no particular order
     */
    fun learnedBigramEntries(): List<DictionaryAssetParser.Bigram> {
        val result = ArrayList<DictionaryAssetParser.Bigram>()
        db.rawQuery("SELECT prevkey, wkey, count FROM $TABLE_LEARNED_BIGRAMS", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(DictionaryAssetParser.Bigram(cursor.getString(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        return result
    }
    
    /**
     * D-278: every learned trigram row (S-07), for the backup/export feature (§21).
     * 
     * @return every row of [TABLE_LEARNED_TRIGRAMS], in no particular order
     */
    fun learnedTrigramEntries(): List<TrigramEntry> {
        val result = ArrayList<TrigramEntry>()
        db.rawQuery("SELECT w1key, w2key, wkey, count FROM $TABLE_LEARNED_TRIGRAMS", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(TrigramEntry(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3)))
            }
        }
        return result
    }
    
    /**
     * D-278: every [BlacklistCategory.USER] blacklist entry, for the backup/export feature (§21) - a bundled
     * category is deliberately excluded, since [de.froehlichmedia.adaptkey.AdaptKeyService] already reseeds
     * it idempotently on every service start (see [BlacklistCategory.BUNDLED]'s own KDoc), so it would only
     * bloat the exported file with data the target device already has.
     * 
     * @return every user-added blacklist word (lower-cased key, as stored), ordered alphabetically
     */
    fun userBlacklistedWords(): List<String> {
        val result = ArrayList<String>()
        db.rawQuery(
            "SELECT wkey FROM $TABLE_BLACKLIST WHERE category = ? ORDER BY wkey ASC",
            arrayOf(BlacklistCategory.USER.name)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result
    }
    
    /**
     * D-278: every provisional-pending-blacklist mark (G-04/W-01), for the backup/export feature (§21).
     * 
     * @return every row of [TABLE_PENDING_BLACKLIST], in no particular order
     */
    fun pendingBlacklistEntries(): List<PendingBlacklistEntry> {
        val result = ArrayList<PendingBlacklistEntry>()
        db.rawQuery("SELECT wkey, ts FROM $TABLE_PENDING_BLACKLIST", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(PendingBlacklistEntry(cursor.getString(0), cursor.getLong(1)))
            }
        }
        return result
    }
    
    /**
     * D-278: additive merge of one imported learned-word row (backup/export, §21) - adds [frequencyDelta] to
     * whatever this store already holds for [word] (0 if not yet known), the same resolution [learn] itself
     * uses, rather than overwriting outright. This is what lets two devices' independently-learned counts
     * combine on import instead of one clobbering the other.
     * 
     * @param word the word exactly as exported (canonical case)
     * @param frequencyDelta the exported frequency to add
     * @param partsOfSpeech the exported tags, merged into whatever this store already has (in practice always
     *        empty - [learn] never sets a tag on a learned entry - kept for fidelity with the exported row)
     */
    fun restoreLearnedWord(word: String, frequencyDelta: Long, partsOfSpeech: Set<PartOfSpeech>) {
        val existing = learnedEntryOf(word)
        val canonical = existing?.word ?: word
        val frequency = (existing?.frequency ?: 0L) + frequencyDelta
        val mergedPos = (existing?.partsOfSpeech ?: emptySet()) + partsOfSpeech
        putWordInternal(TABLE_LEARNED, canonical, frequency, mergedPos, lastTouched = System.currentTimeMillis())
    }
    
    /**
     * D-278: additive merge of one imported learned-bigram row (backup/export, §21), mirroring
     * [restoreLearnedWord]'s own delta-merge resolution.
     * 
     * @param previousWord the bigram's context word, exactly as exported
     * @param word the predicted word, exactly as exported
     * @param countDelta the exported count to add
     */
    fun restoreLearnedBigram(previousWord: String, word: String, countDelta: Long) {
        val count = learnedBigramFrequency(previousWord, word) + countDelta
        putBigramInternal(TABLE_LEARNED_BIGRAMS, previousWord, word, count)
    }
    
    /**
     * D-278: additive merge of one imported learned-trigram row (S-07, backup/export §21), mirroring
     * [restoreLearnedWord]'s own delta-merge resolution.
     * 
     * @param previousPreviousWord the trigram's own first context word, exactly as exported
     * @param previousWord the trigram's own second context word, exactly as exported
     * @param word the predicted word, exactly as exported
     * @param countDelta the exported count to add
     */
    fun restoreLearnedTrigram(previousPreviousWord: String, previousWord: String, word: String, countDelta: Long) {
        val count = trigramFrequency(previousPreviousWord, previousWord, word) + countDelta
        putTrigramInternal(previousPreviousWord, previousWord, word, count)
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
            // D-264: the learned entry's own casing wins over a bundled one when both exist for the same
            // key (see learn()'s own note) - e.g. a preferred all-caps acronym spelling must be what the
            // suggestion bar actually offers, not a differently-cased bundled entry. D-412: lemma is
            // bundled-only, so the bundled entry's own value (if any) is kept rather than lost to the
            // learned entry's always-null one.
            merged[key] = if (existing != null) {
                entry.copy(frequency = existing.frequency + entry.frequency, lemma = existing.lemma)
            } else {
                entry
            }
        }
        return merged.values.sortedByDescending { it.frequency }.take(limit)
    }
    
    private fun queryByPrefix(table: String, prefix: String, limit: Int): List<WordEntry> {
        val hasLemma = table == TABLE_WORDS
        val columns = if (hasLemma) "word, freq, pos, lemma" else "word, freq, pos"
        val result = ArrayList<WordEntry>()
        db.rawQuery(
            "SELECT $columns FROM $table WHERE wkey LIKE ? ORDER BY freq DESC LIMIT ?",
            arrayOf(prefix.lowercase() + "%", limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val lemma = if (hasLemma) cursor.getString(3) else null
                result.add(WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2)), lemma))
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
    
    override fun trigramFrequency(previousPreviousWord: String, previousWord: String, word: String): Long {
        db.rawQuery(
            "SELECT count FROM $TABLE_LEARNED_TRIGRAMS WHERE w1key = ? AND w2key = ? AND wkey = ?",
            arrayOf(previousPreviousWord.lowercase(), previousWord.lowercase(), word.lowercase())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }
    
    override fun nextWordsTrigram(previousPreviousWord: String, previousWord: String, limit: Int): List<String> {
        if (previousPreviousWord.isEmpty() || previousWord.isEmpty() || limit <= 0) {
            return emptyList()
        }
        val result = ArrayList<String>()
        db.rawQuery(
            "SELECT wkey FROM $TABLE_LEARNED_TRIGRAMS WHERE w1key = ? AND w2key = ? ORDER BY count DESC LIMIT ?",
            arrayOf(previousPreviousWord.lowercase(), previousWord.lowercase(), limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(canonicalWordFor(cursor.getString(0)))
            }
        }
        return result
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
        // D-65 / D-63: order each bucket by descending frequency before the LIMIT cut. Without it the rows
        // come back in wkey order, and German umlaut letters (ö = U+00F6 etc.) sort after all of a-z, so a
        // common umlaut word like "können" fell past the LIMIT while a rare same-shape word ("kannen") stayed
        // - and "konnen" mis-corrected to "kannen". Frequency order keeps the umlaut words reachable.
        val perBucketLimit = maxOf(1, CANDIDATE_LIMIT / firstChars.size)
        // D-209: the token's own first-character bucket is never capped, unlike the keyboard-neighbour
        // buckets (reached only for a first-key typo, a much rarer case) - see correctionCandidatesInternal's
        // own KDoc for the reasoning and the real repro ("Kita", frequency 17, crowded out by 389 more
        // frequent same-letter/same-length-window words before ever reaching the edit-distance comparison).
        return correctionCandidatesInternal(token, firstChars, perBucketLimit, uncappedChars = setOfNotNull(token.lowercase().firstOrNull()))
    }
    
    /**
     * D-197: [correctionCandidates]' identical bucket search, but with two buckets left uncapped instead of
     * one - see [DictionaryStore.diacriticCandidates]'s own KDoc for why a frequency-truncated search
     * silently starved out a rare but correctly-spelled diacritic candidate (e.g. "Grüße", frequency 18,
     * behind hundreds of more common same-bucket words) before diacritic restoration ever got to compare it
     * against the token.
     * 
     * D-221: [correctionCandidatesInternal]'s own KDoc explains why the token's own literal first-character
     * bucket stays uncapped, exactly as for [correctionCandidates]. The *umlaut-variant* bucket (ä/ö/ü for a
     * token starting with a/o/u) is uncapped here too, though, unlike for [correctionCandidates] - that
     * variant bucket is not a rare edge case for diacritic restoration the way a keyboard-neighbour typo is:
     * it is the *expected*, ordinary path, since a user typing the ASCII-folded form of an umlaut word (e.g.
     * "uber" for "über") has their token classified under the plain letter while the real word lives under
     * its own umlaut. Capping that bucket would silently reintroduce the exact "Grüße" bug this function
     * exists to prevent, just reached via the umlaut-variant path instead of the primary-bucket path. The
     * ordinary keyboard-neighbour buckets stay capped, same as [correctionCandidates] - the double
     * coincidence of a first-key typo *and* that word also needing umlaut restoration is rarer still than a
     * first-key typo alone.
     */
    override fun diacriticCandidates(token: String, firstChars: Set<Char>): List<String> {
        val key = token.lowercase()
        val ownChar = key.firstOrNull()
        // Mirrors DictionarySuggestionProvider.candidateFirstChars()'s own a/o/u -> ä/ö/ü mapping - kept
        // local rather than shared, since threading it through the DictionaryStore interface for three fixed
        // character pairs would be a larger change than the mapping itself.
        val umlautVariant = when (ownChar) {
            'a' -> 'ä'
            'o' -> 'ö'
            'u' -> 'ü'
            else -> null
        }
        val perBucketLimit = maxOf(1, CANDIDATE_LIMIT / firstChars.size)
        return correctionCandidatesInternal(token, firstChars, perBucketLimit, uncappedChars = setOfNotNull(ownChar, umlautVariant))
    }
    
    /**
     * D-209 / D-221: every char in [uncappedChars] is searched without [perBucketLimit] at all - reserved
     * for the bucket(s) where silently dropping a rare-but-correct candidate would be most costly (see
     * [correctionCandidates]'s and [diacriticCandidates]'s own call sites for which chars each passes and
     * why). Deliberately scoped to only those buckets, not every searched first-char - uncapping every
     * keyboard-neighbour bucket too would reopen the exact per-keystroke cost concern D-160/D-208 already
     * address, for candidates that only ever matter for a first-key typo, a rarer case than an ordinary
     * same-letter one.
     */
    private fun correctionCandidatesInternal(
        token: String,
        firstChars: Set<Char>,
        perBucketLimit: Int,
        uncappedChars: Set<Char> = emptySet()
    ): List<String> {
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
        // D-177: also searched against the (small) learned table, deduplicated by exact canonical form - a
        // word reinforced in both tables shares the same canonical case (learn() resolves it from whichever
        // already exists), so a plain set is enough; frequencyOf() further downstream already merges both
        // sources' frequency for ranking regardless of which bucket a given candidate came from here.
        val result = LinkedHashSet<String>()
        for (firstChar in firstChars) {
            val codePoint = firstChar.code
            val lower = String(Character.toChars(codePoint))
            val upper = String(Character.toChars(codePoint + 1))
            val limit = if (firstChar in uncappedChars) Int.MAX_VALUE else perBucketLimit
            result.addAll(bucketQuery(TABLE_WORDS, lower, upper, minLen, maxLen, limit))
            result.addAll(bucketQuery(TABLE_LEARNED, lower, upper, minLen, maxLen, limit))
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
    
    override fun entryOf(word: String): WordEntry? {
        val bundled = bundledEntryOf(word)
        val learned = learnedEntryOf(word)
        return when {
            bundled == null -> learned
            learned == null -> bundled
            // D-264: the learned entry's own casing wins when both exist for the same key. D-412: lemma is
            // bundled-only (TABLE_LEARNED has no such column, so learned.lemma is always null here anyway).
            else -> WordEntry(
                learned.word,
                bundled.frequency + learned.frequency,
                bundled.partsOfSpeech + learned.partsOfSpeech,
                lemma = bundled.lemma
            )
        }
    }
    
    private fun bundledEntryOf(word: String): WordEntry? {
        return entryOfIn(TABLE_WORDS, word)
    }
    
    private fun learnedEntryOf(word: String): WordEntry? {
        return entryOfIn(TABLE_LEARNED, word)
    }
    
    /**
     * D-412: [TABLE_WORDS] alone carries a `lemma` column - [TABLE_LEARNED] has none, so selecting it
     * there would fail outright.
     */
    private fun entryOfIn(table: String, word: String): WordEntry? {
        val columns = if (table == TABLE_WORDS) "word, freq, pos, lemma" else "word, freq, pos"
        db.rawQuery(
            "SELECT $columns FROM $table WHERE wkey = ?",
            arrayOf(word.lowercase())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            val lemma = if (table == TABLE_WORDS) cursor.getString(3) else null
            return WordEntry(cursor.getString(0), cursor.getLong(1), parsePos(cursor.getString(2)), lemma)
        }
    }
    
    /**
     * @param lastTouched D-388: epoch millis to stamp into [TABLE_LEARNED]'s own `last_touched` column -
     *        null for [TABLE_WORDS] (bundled), which has no such column at all. Every [TABLE_LEARNED]
     *        write is a full `INSERT OR REPLACE`, so this must always be passed explicitly for that table
     *        - an omitted value would silently reset the column back to its schema default instead of
     *        leaving it untouched.
     * @param lemma D-412: the base-form key to stamp into [TABLE_WORDS]'s own `lemma` column - ignored
     *        for any other table, which has no such column at all (mirrors [lastTouched] above).
     */
    private fun putWordInternal(
        table: String,
        word: String,
        frequency: Long,
        pos: Set<PartOfSpeech>,
        lastTouched: Long? = null,
        lemma: String? = null
    ) {
        val values = ContentValues().apply {
            put("wkey", word.lowercase())
            put("word", word)
            put("freq", frequency)
            put("pos", pos.joinToString(",") { it.name })
            if (lastTouched != null) {
                put("last_touched", lastTouched)
            }
            if (table == TABLE_WORDS) {
                put("lemma", lemma)
            }
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
    
    private fun putTrigramInternal(previousPreviousWord: String, previousWord: String, word: String, count: Long) {
        val values = ContentValues().apply {
            put("w1key", previousPreviousWord.lowercase())
            put("w2key", previousWord.lowercase())
            put("wkey", word.lowercase())
            put("count", count)
        }
        db.insertWithOnConflict(TABLE_LEARNED_TRIGRAMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
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
        
        // D-388: the gap between consecutive migration-seeded last_touched values - see
        // ensureLastTouchedColumn's own KDoc.
        private const val SEED_TIMESTAMP_STEP_MS = 1000L
        private const val TABLE_WORDS = "words"
        private const val TABLE_BIGRAMS = "bigrams"
        private const val TABLE_BLACKLIST = "blacklist"
        private const val TABLE_LEARNED = "learned"
        private const val TABLE_LEARNED_BIGRAMS = "learned_bigrams"
        private const val TABLE_LEARNED_TRIGRAMS = "learned_trigrams"
        private const val TABLE_PENDING_BLACKLIST = "pending_blacklist"
        private const val TABLE_META = "meta"
        private const val META_KEY_BUNDLED_VERSION = "bundled_version"
        private const val META_KEY_INSTALLED_PACK_VERSION = "installed_pack_version"
        private const val META_KEY_LEARNED_CLEANUP_VERSION = "learned_cleanup_version"
        private const val META_KEY_BIGRAM_CLEANUP_VERSION = "bigram_cleanup_version"
        
        // Upper bound on autocorrect candidates scanned per keystroke (bounds worst-case latency).
        private const val CANDIDATE_LIMIT = 2000
    }
}

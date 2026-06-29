package de.froehlichmedia.adaptkey.dictionary

/**
 * Built-in seed for a fresh dictionary.
 *
 * This is a placeholder so the keyboard is useful before any learning has happened; it is NOT the
 * real lexicon. The full word list, frequencies and part-of-speech distribution are imported from
 * German lexicon dumps (DWDS / Wiktionary) at install time in a later session, replacing this seed.
 */
object SeedData {
    
    /** A bigram seed triple. */
    data class BigramSeed(
        val previousWord: String,
        val word: String,
        val count: Long
    )
    
    val words: List<WordEntry> = listOf(
        entry("der", 1000, PartOfSpeech.OTHER),
        entry("die", 990, PartOfSpeech.OTHER),
        entry("und", 980, PartOfSpeech.OTHER),
        entry("das", 970, PartOfSpeech.OTHER),
        entry("ist", 960, PartOfSpeech.VERB),
        entry("nicht", 950, PartOfSpeech.OTHER),
        entry("ein", 940, PartOfSpeech.OTHER),
        entry("eine", 930, PartOfSpeech.OTHER),
        entry("ich", 920, PartOfSpeech.OTHER),
        entry("mit", 910, PartOfSpeech.PREPOSITION, PartOfSpeech.OTHER),
        entry("für", 900, PartOfSpeech.PREPOSITION),
        entry("über", 880, PartOfSpeech.PREPOSITION),
        entry("unter", 860, PartOfSpeech.PREPOSITION),
        entry("haben", 840, PartOfSpeech.VERB),
        entry("gehen", 820, PartOfSpeech.VERB),
        entry("machen", 800, PartOfSpeech.VERB),
        entry("sehen", 780, PartOfSpeech.VERB),
        entry("kommen", 760, PartOfSpeech.VERB),
        entry("gut", 740, PartOfSpeech.ADJECTIVE),
        entry("schön", 720, PartOfSpeech.ADJECTIVE),
        entry("groß", 700, PartOfSpeech.ADJECTIVE),
        entry("Haus", 400, PartOfSpeech.NOUN),
        entry("Hund", 380, PartOfSpeech.NOUN),
        entry("Katze", 370, PartOfSpeech.NOUN),
        entry("Auto", 360, PartOfSpeech.NOUN),
        entry("Stadt", 350, PartOfSpeech.NOUN),
        entry("Tag", 340, PartOfSpeech.NOUN),
        entry("Welt", 330, PartOfSpeech.NOUN),
        // Ambiguous: noun and another part of speech -> never auto-capitalised (hierarchy rule 5).
        entry("morgen", 320, PartOfSpeech.NOUN, PartOfSpeech.OTHER),
        entry("gegenüber", 300, PartOfSpeech.NOUN, PartOfSpeech.PREPOSITION),
        entry("Berlin", 280, PartOfSpeech.PROPER_NOUN),
        entry("Deutschland", 260, PartOfSpeech.PROPER_NOUN),
        entry("Anna", 200, PartOfSpeech.PROPER_NOUN)
    )
    
    val bigrams: List<BigramSeed> = listOf(
        BigramSeed("der", "Hund", 50),
        BigramSeed("die", "Katze", 45),
        BigramSeed("das", "Haus", 45),
        BigramSeed("ein", "Auto", 35),
        BigramSeed("über", "Berlin", 20)
    )
    
    /**
     * Writes the seed into an empty store.
     *
     * @param store the store to populate
     */
    fun seed(store: DictionaryStore) {
        words.forEach { store.putWord(it) }
        bigrams.forEach { store.putBigram(it.previousWord, it.word, it.count) }
    }
    
    private fun entry(word: String, frequency: Long, vararg pos: PartOfSpeech): WordEntry {
        return WordEntry(word, frequency, pos.toSet())
    }
}

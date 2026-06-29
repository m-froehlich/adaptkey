package de.froehlichmedia.adaptkey.dictionary

/**
 * A dictionary unigram entry.
 *
 * @property word the word in its canonical case
 * @property frequency the learned frequency count (higher means more probable)
 * @property partsOfSpeech the part-of-speech tags for the capitalisation hierarchy (§6); may be empty
 */
data class WordEntry(
    val word: String,
    val frequency: Long,
    val partsOfSpeech: Set<PartOfSpeech> = emptySet()
)

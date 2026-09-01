// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import de.froehlichmedia.adaptkey.dictionary.InMemoryDictionaryStore
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import de.froehlichmedia.adaptkey.dictionary.WordEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the capitalisation hierarchy (§6).
 */
class CapitalisationEngineTest {
    
    private val store = InMemoryDictionaryStore().apply {
        putWord(WordEntry("Haus", 10L, setOf(PartOfSpeech.NOUN)))
        putWord(WordEntry("morgen", 10L, setOf(PartOfSpeech.NOUN, PartOfSpeech.OTHER)))
        putWord(WordEntry("gegenüber", 10L, setOf(PartOfSpeech.NOUN, PartOfSpeech.PREPOSITION)))
        putWord(WordEntry("Berlin", 10L, setOf(PartOfSpeech.PROPER_NOUN)))
        putWord(WordEntry("gehen", 10L, setOf(PartOfSpeech.VERB)))
    }
    private val engine = CapitalisationEngine(store)
    
    private fun ctx(
        explicit: Boolean = false,
        sentenceStart: Boolean = false,
        caps: CapsMode = CapsMode.NONE,
        afterHyphen: Boolean = false,
        previousHyphenSegment: String? = null,
        previousHyphenSegmentAtSentenceStart: Boolean = false
    ): CapitalisationContext {
        return CapitalisationContext(
            explicit,
            sentenceStart,
            caps,
            afterHyphen,
            previousHyphenSegment,
            previousHyphenSegmentAtSentenceStart
        )
    }
    
    @Test
    fun `pure noun is capitalised mid-sentence`() {
        assertEquals("Haus", engine.capitalise("haus", ctx()))
    }
    
    @Test
    fun `ambiguous noun is left lowercase`() {
        assertEquals("morgen", engine.capitalise("morgen", ctx()))
        assertEquals("gegenüber", engine.capitalise("gegenüber", ctx()))
    }
    
    @Test
    fun `proper noun is capitalised`() {
        assertEquals("Berlin", engine.capitalise("berlin", ctx()))
    }
    
    @Test
    fun `a verb is not capitalised mid-sentence`() {
        assertEquals("gehen", engine.capitalise("gehen", ctx()))
    }
    
    @Test
    fun `an unknown lowercase word stays lowercase`() {
        assertEquals("xyz", engine.capitalise("xyz", ctx()))
    }
    
    @Test
    fun `D-405 sentence start alone no longer capitalises at commit - only live Shift-arming does that now`() {
        assertEquals("gehen", engine.capitalise("gehen", ctx(sentenceStart = true)))
    }
    
    @Test
    fun `explicit uppercase is never lowercased`() {
        // "morgen" is ambiguous and would normally stay lowercase, but the user typed it uppercase.
        assertEquals("Morgen", engine.capitalise("Morgen", ctx(explicit = true)))
    }
    
    @Test
    fun `a non-explicit stray capital is normalised down`() {
        assertEquals("gehen", engine.capitalise("Gehen", ctx()))
    }
    
    @Test
    fun `B-02 lowercases a common noun after a hyphen`() {
        assertEquals("haus", engine.capitalise("Haus", ctx(afterHyphen = true)))
    }
    
    @Test
    fun `B-02 keeps a proper noun capitalised after a hyphen`() {
        assertEquals("Berlin", engine.capitalise("berlin", ctx(afterHyphen = true)))
    }
    
    @Test
    fun `caps mode CHARACTERS upper-cases the whole word`() {
        assertEquals("HAUS", engine.capitalise("haus", ctx(caps = CapsMode.CHARACTERS)))
    }
    
    @Test
    fun `caps mode WORDS capitalises even an ambiguous word`() {
        assertEquals("Morgen", engine.capitalise("morgen", ctx(caps = CapsMode.WORDS)))
    }
    
    @Test
    fun `field-mandated WORDS outranks the after-hyphen rule`() {
        assertEquals("Haus", engine.capitalise("haus", ctx(caps = CapsMode.WORDS, afterHyphen = true)))
    }
    
    @Test
    fun `empty input is returned unchanged`() {
        assertEquals("", engine.capitalise("", ctx(sentenceStart = true)))
    }
    
    @Test
    fun `§6 rule 6 LLM exception lifts an ambiguous noun to upper-case`() {
        assertEquals("Morgen", engine.capitalise("morgen", ctx(), llmForcesUpper = true))
    }
    
    @Test
    fun `§6 rule 6 LLM exception lifts an unknown word to upper-case`() {
        assertEquals("Xyz", engine.capitalise("xyz", ctx(), llmForcesUpper = true))
    }
    
    @Test
    fun `§6 rule 6 LLM exception does not override the B-02 after-hyphen rule`() {
        assertEquals("haus", engine.capitalise("Haus", ctx(afterHyphen = true), llmForcesUpper = true))
    }
    
    @Test
    fun `D-373 not at sentence start - a capitalised previous segment propagates directly - Muenchen-Nord`() {
        // "nord" itself is unknown/not a proper noun - B-02 alone would lowercase it.
        val context = ctx(afterHyphen = true, previousHyphenSegment = "München", previousHyphenSegmentAtSentenceStart = false)
        assertEquals("Nord", engine.capitalise("nord", context))
    }
    
    @Test
    fun `D-373 not at sentence start - a lowercase previous segment does not propagate`() {
        val context = ctx(afterHyphen = true, previousHyphenSegment = "anteil", previousHyphenSegmentAtSentenceStart = false)
        assertEquals("teil", engine.capitalise("Teil", context))
    }
    
    @Test
    fun `D-373 at sentence start - a capitalised non-noun previous segment does not propagate`() {
        // "Schnell" was only capitalised because it opened the sentence, not because of its own grammar -
        // must not drag an unrelated word after the hyphen up with it.
        val context = ctx(afterHyphen = true, previousHyphenSegment = "Schnell", previousHyphenSegmentAtSentenceStart = true)
        assertEquals("gehen", engine.capitalise("Gehen", context))
    }
    
    @Test
    fun `D-373 at sentence start - a genuine proper noun previous segment does propagate`() {
        val context = ctx(afterHyphen = true, previousHyphenSegment = "Berlin", previousHyphenSegmentAtSentenceStart = true)
        assertEquals("Mitte", engine.capitalise("mitte", context))
    }
    
    @Test
    fun `D-373 at sentence start - a genuine pure-noun previous segment does propagate`() {
        val context = ctx(afterHyphen = true, previousHyphenSegment = "Haus", previousHyphenSegmentAtSentenceStart = true)
        assertEquals("Tuer", engine.capitalise("tuer", context))
    }
    
    @Test
    fun `D-373 no previous segment recorded falls back to plain B-02`() {
        assertEquals("teil", engine.capitalise("Teil", ctx(afterHyphen = true)))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure retroactive token repair: word split (A-05) and word merge (A-06), each
 * requiring a valid linguistic result rather than mere spatial proximity.
 */
class TokenRepairTest {
    
    private lateinit var store: InMemoryDictionaryStore
    private lateinit var repair: TokenRepair
    
    @BeforeEach
    fun setUp() {
        store = InMemoryDictionaryStore()
        listOf("und", "das", "aber", "bald", "ist", "ich").forEach { word ->
            store.putWord(WordEntry(word, frequency = 10L))
        }
        // §45: every split candidate now requires the two halves to co-occur (a real bigram), whichever
        // strategy found it.
        store.putBigram("aber", "das", TokenRepair.MIN_SPLIT_BIGRAM)
        store.putBigram("und", "das", TokenRepair.MIN_SPLIT_BIGRAM)
        repair = TokenRepair(store)
    }
    
    @Test
    fun `a flagged space-ambiguous character is split into two words`() {
        // "undxdas" with the 'x' at index 3 flagged -> drop it and split.
        val result = repair.trySplit("undxdas", setOf(3))
        assertEquals(SplitResult("und", "das"), result)
    }
    
    @Test
    fun `a fully missed space is split by inserting a space`() {
        val result = repair.trySplit("aberdas", emptySet())
        assertEquals(SplitResult("aber", "das"), result)
    }
    
    @Test
    fun `a token with no valid split is left alone`() {
        assertNull(repair.trySplit("abcdef", setOf(3)))
    }
    
    @Test
    fun `a missed-space split is rejected when the halves do not co-occur`() {
        // "und" and "bald" are both known words, but without a bigram this is a typo, not a missed space.
        assertNull(repair.trySplit("undbald", emptySet()))
    }
    
    @Test
    fun `an over-space letter is dropped as a likely space mis-tap even without a flag`() {
        // 'c' sits over the space bar on QWERTZ, so "undcdas" is treated as "und" + "das".
        assertEquals(SplitResult("und", "das"), repair.trySplit("undcdas", emptySet()))
    }
    
    @Test
    fun `a non-over-space interior letter is not dropped without a flag`() {
        // 'x' is not over the space bar and is not flagged, so "undxdas" is left alone.
        assertNull(repair.trySplit("undxdas", emptySet()))
    }
    
    @Test
    fun `a known word is never split`() {
        assertNull(repair.trySplit("aber", setOf(2)))
    }
    
    @Test
    fun `paragraph 45 a drop candidate with no co-occurrence evidence is rejected, even when both halves are known words`() {
        // Reproduces the reported bug: "mei" and "st" are each individually real (if obscure) dictionary
        // entries, and 'n' sits over the space bar, so the drop strategy alone used to accept "mei" + "st"
        // with zero evidence the two are ever used together - "meinst" -> "mei St".
        store.putWord(WordEntry("mei", frequency = 16L))
        store.putWord(WordEntry("st", frequency = 5939L))
        
        assertNull(repair.trySplit("meinst", emptySet()))
    }
    
    @Test
    fun `a split is rejected when a half is blacklisted`() {
        store.blacklist("das", BlacklistCategory.USER)
        assertNull(repair.trySplit("aberdas", emptySet()))
    }
    
    @Test
    fun `a missed-space split outscores a lower-quality drop candidate (immernoch to immer noch)`() {
        store.putWord(WordEntry("immer", frequency = 500L))
        store.putWord(WordEntry("noch", frequency = 500L))
        // "och" is also a known word, so the drop-a-character path ("immer" + "och", dropping the
        // over-space-letter 'n') is a technically valid candidate too - but it must lose to the missed-space
        // candidate ("immer" + "noch") once both are compared, not win merely by being found first.
        store.putWord(WordEntry("och", frequency = 5L))
        store.putBigram("immer", "noch", 200L)
        
        assertEquals(SplitResult("immer", "noch"), repair.trySplit("immernoch", emptySet()))
    }
    
    @Test
    fun `a spurious space is merged back into a known word`() {
        val merged = repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "ald")
        assertEquals("bald", merged)
    }
    
    @Test
    fun `a merge yielding no known word is rejected`() {
        assertNull(repair.tryMerge(previousWord = "ich", inferredChar = 'q', token = "xyz"))
    }
    
    @Test
    fun `a merge is rejected when the following token is already a word`() {
        assertNull(repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "und"))
    }
    
    @Test
    fun `a high-probability bigram justifies a merge even without a known word`() {
        // "bold" is not a unigram, but follows "ich" frequently enough.
        store.putBigram("ich", "bold", TokenRepair.MIN_BIGRAM)
        val merged = repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "old")
        assertEquals("bold", merged)
    }
    
    @Test
    fun `a blacklisted merge candidate is rejected`() {
        store.blacklist("bald", BlacklistCategory.USER)
        assertNull(repair.tryMerge(previousWord = "ich", inferredChar = 'b', token = "ald"))
    }
    
    @Test
    fun `paragraph 47 a missed-space split's span ranges are contiguous, with no gap`() {
        // "aberdas" -> "aber" + "das": the halves add up to the token exactly, so there is no dropped
        // character between them.
        val ranges = SplitResult("aber", "das").spanRanges("aberdas")
        assertEquals(0..3, ranges.first)
        assertEquals(4..6, ranges.second)
    }
    
    @Test
    fun `paragraph 47 a drop-strategy split's span ranges leave a one-character gap`() {
        // "undcdas" -> "und" + "das": the halves are one character shorter than the token combined, so the
        // dropped 'c' at index 3 sits uncoloured between the two spans.
        val ranges = SplitResult("und", "das").spanRanges("undcdas")
        assertEquals(0..2, ranges.first)
        assertEquals(4..6, ranges.second)
    }
    
    @Test
    fun `D-122 an unresolved connector is split even with no bigram co-occurrence at all`() {
        // Reproduces the reported case: "testvwort" ("test" + accidental "v" + "wort") - "test" and "wort"
        // never co-occur in the corpus, so trySplit() itself would (correctly, for its own broader use)
        // find nothing here; splitAtUnresolvedConnector() must still find it.
        store.putWord(WordEntry("test", frequency = 500L))
        store.putWord(WordEntry("wort", frequency = 4_084L))
        
        assertNull(repair.trySplit("testvwort", emptySet()), "trySplit() itself must stay bigram-gated")
        assertEquals(SplitResult("test", "wort"), repair.splitAtUnresolvedConnector("testvwort"))
    }
    
    @Test
    fun `D-122 a non-over-space letter is never treated as a connector`() {
        store.putWord(WordEntry("test", frequency = 500L))
        store.putWord(WordEntry("wort", frequency = 4_084L))
        // 'x' is not one of TokenRepair.OVER_SPACE_LETTERS, unlike 'v'.
        assertNull(repair.splitAtUnresolvedConnector("testxwort"))
    }
    
    @Test
    fun `D-122 only the connector position that yields two known words on both sides is picked`() {
        // "undcdasnist": dropping the 'c' (index 3) would leave "und" + "dasnist" - "dasnist" is not a
        // known word, so that position is rejected; only dropping the 'n' (index 7) yields two genuinely
        // known words on both sides.
        store.putWord(WordEntry("undcdas", frequency = 1L)) // an unlikely but present "word" for this test
        store.putWord(WordEntry("ist", frequency = 500L))
        
        assertEquals(SplitResult("undcdas", "ist"), repair.splitAtUnresolvedConnector("undcdasnist"))
    }
    
    @Test
    fun `D-122 no split when neither half is a known word`() {
        assertNull(repair.splitAtUnresolvedConnector("xyzvabc"))
    }
    
    @Test
    fun `D-122 a known word is never split`() {
        assertNull(repair.splitAtUnresolvedConnector("aber"))
    }
    
    @Test
    fun `D-122 a blacklisted half is rejected`() {
        store.putWord(WordEntry("test", frequency = 500L))
        store.putWord(WordEntry("wort", frequency = 4_084L))
        store.blacklist("wort", BlacklistCategory.USER)
        assertNull(repair.splitAtUnresolvedConnector("testvwort"))
    }
}

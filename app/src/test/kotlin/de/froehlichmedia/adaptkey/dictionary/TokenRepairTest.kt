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
 *
 * §128 / D-203: the split-candidate gate no longer requires prior bigram co-occurrence (see
 * [TokenRepair]'s own class-level KDoc for why that gate was replaced) - these tests set up frequencies at
 * or above [TokenRepair.MIN_SPLIT_HALF_FREQUENCY] for every word meant to be a valid split half.
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
    fun `§128 a missed-space split succeeds even without any prior bigram co-occurrence`() {
        // "und" and "bald" are both known words and neither is a noun - no bigram between them was ever
        // recorded, and none is needed any more (D-203): a first-time-typed compound typo has, by
        // definition, never co-occurred as two separate words before, so requiring it rejected exactly the
        // cases this mechanism exists to catch.
        assertEquals(SplitResult("und", "bald"), repair.trySplit("undbald", emptySet()))
    }
    
    @Test
    fun `D-216 trySplit stops early and finds nothing once isCancelled returns true immediately`() {
        // Same "undbald" -> "und" + "bald" split as the §128 test above, but cancelled on the very first
        // poll: trySplit must give up before evaluating any split position at all, rather than still
        // returning the split it would otherwise have found.
        assertNull(repair.trySplit("undbald", emptySet()) { true })
    }
    
    @Test
    fun `D-216 an isCancelled that never returns true leaves the result unchanged`() {
        assertEquals(SplitResult("und", "bald"), repair.trySplit("undbald", emptySet()) { false })
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
    fun `§128 a regular-verb inflection of a known infinitive is never split`() {
        // Reproduces the reported bug at its actual source: "meinst" is not itself in the dictionary, but
        // strips its "st" ending to "mein" + "en" = "meinen", a known infinitive - RegularVerbInflection
        // recognises it as a plausible inflection, so it is treated exactly like a literal known word and
        // never even reaches split-candidate generation. "mei" is deliberately *not* tagged as a noun here
        // (unlike the real dictionary's actual "Mei" entry) so this pair would otherwise pass the
        // not-both-nouns check and split successfully if the inflection guard did not exist - isolating
        // that the guard alone is what blocks it here (the separate "both nouns" test below covers the
        // real-world "Mei"/"St" tagging, an independent mechanism that closes the same historical bug).
        store.putWord(WordEntry("meinen", frequency = 500L))
        store.putWord(WordEntry("mei", frequency = 16L))
        store.putWord(WordEntry("st", frequency = 5_939L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertNull(repair.trySplit("meinst", emptySet()))
    }
    
    @Test
    fun `D-252 an adjective comparative of a known positive is never split`() {
        // Reproduces the reported bug at its actual source: "zuversichtlicher" is not itself in the
        // dictionary, but strips its "er" ending to "zuversichtlich", a known positive adjective -
        // AdjectiveInflection recognises it as a plausible comparative, so it is treated exactly like a
        // literal known word and never even reaches split-candidate generation. Neither half is tagged as a
        // noun here (matching the real dictionary's own OTHER tag for both) so this pair would otherwise
        // pass the not-both-nouns check and split successfully - "er" (the German pronoun) is real,
        // extremely frequent, and exactly why it kept winning as a split half - isolating that the
        // comparative-inflection guard alone is what blocks it here.
        store.putWord(WordEntry("zuversichtlich", frequency = 17L))
        store.putWord(WordEntry("er", frequency = 120_975L))
        
        assertNull(repair.trySplit("zuversichtlicher", emptySet()))
    }
    
    @Test
    fun `§128 a drop candidate where both halves are nouns is rejected, even when both are known words`() {
        // Reproduces the reported bug as it would appear without a covering verb inflection: "mei" and "st"
        // are each individually real (if obscure) dictionary entries, individually well above the
        // frequency floor, and 'n' sits over the space bar - but both resolve to a noun, unlike an ordinary
        // phrase (an article/pronoun/conjunction, tagged OTHER, followed by a noun). Frequencies mirror the
        // real bundled dict_de.tsv values for "Mei" (16) and "St" (5939) - deliberately not obviously "too
        // rare", since frequency alone cannot distinguish this pair from a genuine compound half.
        store.putWord(WordEntry("mei", frequency = 16L, partsOfSpeech = setOf(PartOfSpeech.NOUN, PartOfSpeech.OTHER)))
        store.putWord(WordEntry("st", frequency = 5_939L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertNull(repair.trySplit("meinst", emptySet()))
    }
    
    @Test
    fun `§128 a function word followed by a noun is a valid split, unlike two nouns`() {
        // "der" (untagged here, matching the real dictionary's OTHER-only article entries) + "kinderarzt"
        // (NOUN) is an entirely ordinary German phrase shape and must still split - only a *both-nouns*
        // pairing is rejected, not "any pairing involving a noun".
        store.putWord(WordEntry("der", frequency = 1_004_234L))
        store.putWord(WordEntry("kinderarzt", frequency = 14L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertEquals(SplitResult("der", "kinderarzt"), repair.trySplit("derkinderarzt", emptySet()))
    }
    
    @Test
    fun `D-230 reverted - a missed space before a genuine 2-letter function word must still split`() {
        // A MIN_PART 2->3 bump was tried to close the "Docker"/"darfst" regressions but was itself a
        // regression: it would also block this entirely ordinary case (e.g. "Wie geht es?" typed as
        // "wiegehtes" losing the second space) - "es" is a real, common 2-letter German word, not a corpus
        // artefact. MIN_PART stays 2; the "Docker"/"darfst" class needs a different, more targeted fix.
        store.putWord(WordEntry("geht", frequency = 500L))
        store.putWord(WordEntry("es", frequency = 400_000L))
        
        assertEquals(SplitResult("geht", "es"), repair.trySplit("gehtes", emptySet()))
    }
    
    @Test
    fun `D-244 an acronym-shaped half needs a much higher frequency floor than an ordinary word`() {
        // "übrigebs" -> "übrig"+"Ebs": "Ebs"/"EBS" (an acronym, stored all-uppercase) sits right at
        // MIN_SPLIT_HALF_FREQUENCY's own floor (10) - a real dictionary entry nobody would plausibly have
        // intended here. An ordinary word of the same frequency (not all-uppercase) must still split fine.
        store.putWord(WordEntry("übrig", frequency = 325L))
        store.putWord(WordEntry("EBS", frequency = TokenRepair.MIN_SPLIT_HALF_FREQUENCY, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertNull(repair.trySplit("übrigebs", emptySet()))
    }
    
    @Test
    fun `D-244 an acronym-shaped half is accepted once it clears the higher acronym floor`() {
        store.putWord(WordEntry("übrig", frequency = 325L))
        store.putWord(WordEntry("EBS", frequency = TokenRepair.MIN_SPLIT_ACRONYM_FREQUENCY, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertEquals(SplitResult("übrig", "ebs"), repair.trySplit("übrigebs", emptySet()))
    }
    
    @Test
    fun `D-244 an ordinary mixed-case word of the same low frequency is unaffected by the acronym floor`() {
        // "Dock" is capitalised (an ordinary noun), not all-uppercase - the acronym floor must not apply to it.
        store.putWord(WordEntry("Dock", frequency = TokenRepair.MIN_SPLIT_HALF_FREQUENCY, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("er", frequency = 120_975L))
        
        assertEquals(SplitResult("dock", "er"), repair.trySplit("docker", emptySet()))
    }
    
    @Test
    fun `§128 a half below the frequency floor is rejected even though it is a known word`() {
        store.putWord(WordEntry("mini", frequency = TokenRepair.MIN_SPLIT_HALF_FREQUENCY - 1))
        store.putWord(WordEntry("wort", frequency = 500L))
        
        assertNull(repair.trySplit("miniwort", emptySet()))
        
        // The identical pair clears the floor once "mini" is frequent enough - confirming the frequency
        // check, not some other gate, was what rejected it above.
        store.putWord(WordEntry("mini", frequency = TokenRepair.MIN_SPLIT_HALF_FREQUENCY))
        assertEquals(SplitResult("mini", "wort"), repair.trySplit("miniwort", emptySet()))
    }
    
    @Test
    fun `§128 a half typed without its umlaut still resolves via the real diacritic spelling`() {
        // "uberwort" ("uber" typed without the umlaut + "wort") must still be recognised as "über" + "wort"
        // (D-48: umlauts are ordinary characters) - the split's own halves stay the literal typed substrings
        // (so §47's span-colouring math over the still-displayed composing text stays correct), but
        // eligibility/frequency/noun-pair checks resolve through the real spelling.
        store.putWord(WordEntry("über", frequency = 500L, partsOfSpeech = setOf(PartOfSpeech.PREPOSITION)))
        store.putWord(WordEntry("wort", frequency = 4_084L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertEquals(SplitResult("uber", "wort"), repair.trySplit("uberwort", emptySet()))
    }
    
    @Test
    fun `D-249 the negation prefix un- is never accepted as a split's left half`() {
        // "unglücklich" -> "un"+"glücklich": "un" is not itself a dictionary entry (confirmed against the
        // real dict_de.tsv), but the block must be unconditional - checked before dictionary resolution, not
        // merely a side effect of "un" happening to fail resolveWord - so a future dictionary addition could
        // not silently reopen this.
        store.putWord(WordEntry("glücklich", frequency = 150L))
        assertNull(repair.trySplit("unglücklich", emptySet()))
    }
    
    @Test
    fun `D-249 the inseparable prefix wider- is rejected even though it is itself a rare dictionary word`() {
        // "widersagen" -> "wider"+"sagen": both halves are real, individually well above the frequency
        // floor (mirroring the real dict_de.tsv values - "wider" 598, "sagen" 775) and neither is a noun, so
        // candidateAt's pre-existing gates alone would accept this split without the new prefix guard.
        store.putWord(WordEntry("wider", frequency = 598L))
        store.putWord(WordEntry("sagen", frequency = 775L))
        assertNull(repair.trySplit("widersagen", emptySet()))
    }
    
    @Test
    fun `D-249 the pronoun er is exempt from the prefix guard since it is itself an extremely common word`() {
        // Unlike "wider", "er" (frequency 120,975, the real dict_de.tsv value) sits far above
        // PREFIX_COMMON_WORD_FREQUENCY_CEILING - the guard must not block it, or a genuine missed-space case
        // like "erkommt" -> "er kommt" would regress.
        store.putWord(WordEntry("er", frequency = 120_975L))
        store.putWord(WordEntry("kommt", frequency = 500L))
        assertEquals(SplitResult("er", "kommt"), repair.trySplit("erkommt", emptySet()))
    }
    
    @Test
    fun `D-249 a prefix-shaped left half is blocked right at the frequency ceiling and freed just above it`() {
        store.putWord(WordEntry("wider", frequency = TokenRepair.PREFIX_COMMON_WORD_FREQUENCY_CEILING))
        store.putWord(WordEntry("sagen", frequency = 775L))
        assertNull(repair.trySplit("widersagen", emptySet()))
        
        store.putWord(WordEntry("wider", frequency = TokenRepair.PREFIX_COMMON_WORD_FREQUENCY_CEILING + 1))
        assertEquals(SplitResult("wider", "sagen"), repair.trySplit("widersagen", emptySet()))
    }
    
    @Test
    fun `D-249 an inseparable prefix is never accepted via the D-122 connector-split path either`() {
        // "entvwort" dropping the over-space-letter 'v' at index 3 -> "ent"+"wort": the shared candidateAt
        // gate must block this exactly as it does for trySplit.
        store.putWord(WordEntry("wort", frequency = 4_084L))
        assertNull(repair.splitAtUnresolvedConnector("entvwort"))
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
        // "och" is also a known word, well above the frequency floor, so the drop-a-character path
        // ("immer" + "och", dropping the over-space-letter 'n') is a technically valid candidate too - but
        // it must lose to the missed-space candidate ("immer" + "noch") once both are compared by score,
        // not win merely by being found first, and not be excluded by the frequency floor either (this
        // test is specifically about score ranking, not gating).
        store.putWord(WordEntry("och", frequency = 15L))
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
    fun `D-122 an unresolved connector is found even without any prior bigram co-occurrence`() {
        store.putWord(WordEntry("test", frequency = 500L))
        store.putWord(WordEntry("wort", frequency = 4_084L))
        
        assertEquals(SplitResult("test", "wort"), repair.splitAtUnresolvedConnector("testvwort"))
    }
    
    @Test
    fun `D-122 only tries the connector-drop strategy, unlike trySplit's own missed-space strategy too`() {
        // "testwort" has no over-space-letter connector position at all - splitAtUnresolvedConnector can
        // never find a candidate here, while trySplit's own missed-space strategy does. This is the actual
        // remaining difference between the two functions now that neither requires bigram co-occurrence
        // (§128 / D-203) - scope of strategies tried, not evidence required.
        store.putWord(WordEntry("test", frequency = 500L))
        store.putWord(WordEntry("wort", frequency = 4_084L))
        
        assertNull(repair.splitAtUnresolvedConnector("testwort"))
        assertEquals(SplitResult("test", "wort"), repair.trySplit("testwort", emptySet()))
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
        // known words on both sides, both above the frequency floor.
        store.putWord(WordEntry("undcdas", frequency = 15L)) // an unlikely but present "word" for this test
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
    
    @Test
    fun `D-122 both-nouns pairing is rejected here too, mirroring trySplit`() {
        store.putWord(WordEntry("mei", frequency = 16L, partsOfSpeech = setOf(PartOfSpeech.NOUN, PartOfSpeech.OTHER)))
        store.putWord(WordEntry("st", frequency = 5_939L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertNull(repair.splitAtUnresolvedConnector("meinst"))
    }
}

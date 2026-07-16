// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the tier-1 dictionary suggestion provider (n-gram ranking, A-01, A-04).
 */
class DictionarySuggestionProviderTest {
    
    private val store = InMemoryDictionaryStore()
    private val provider = DictionarySuggestionProvider(store)
    
    @Test
    fun `suggestionsFor ranks prefix matches by frequency`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 100L))
        
        val words = provider.suggestionsFor("h", null).map { it.word }
        assertEquals(listOf("Haus", "Hund"), words)
    }
    
    @Test
    fun `bigram context can outrank a more frequent unigram`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 100L))
        store.putBigram("der", "Hund", 50L)
        
        assertEquals("Haus", provider.suggestionsFor("h", null).first().word)
        assertEquals("Hund", provider.suggestionsFor("h", "der").first().word)
    }
    
    @Test
    fun `A-04 blacklisted words are excluded from suggestions`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 100L))
        store.blacklist("Haus", BlacklistCategory.USER)
        
        val words = provider.suggestionsFor("h", null).map { it.word }
        assertEquals(listOf("Hund"), words)
    }
    
    @Test
    fun `A-04 a blacklisted word is not reported as known`() {
        store.putWord(WordEntry("Haus", 100L))
        store.blacklist("Haus", BlacklistCategory.USER)
        
        assertFalse(provider.isKnownWord("Haus"))
    }
    
    @Test
    fun `isKnownWord is true for a known non-blacklisted word`() {
        store.putWord(WordEntry("Haus", 100L))
        assertTrue(provider.isKnownWord("haus"))
    }
    
    @Test
    fun `D-43 nextWordSuggestions ranks successors by bigram count`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Hut", 10L))
        store.putBigram("der", "Hund", 40L)
        store.putBigram("der", "Hut", 5L)
        
        val words = provider.nextWordSuggestions("der").map { it.word }
        assertEquals(listOf("Hund", "Hut"), words)
    }
    
    @Test
    fun `D-43 nextWordSuggestions omits blacklisted successors and unknown context`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putBigram("der", "Hund", 40L)
        store.blacklist("Hund", BlacklistCategory.USER)
        assertTrue(provider.nextWordSuggestions("der").isEmpty())
        // No bigrams for an unseen context word.
        assertTrue(provider.nextWordSuggestions("völlig").isEmpty())
        assertTrue(provider.nextWordSuggestions("").isEmpty())
    }
    
    @Test
    fun `A-01 a known word is never autocorrected`() {
        store.putWord(WordEntry("Haus", 100L))
        assertNull(provider.autocorrectFor("haus", null))
    }
    
    @Test
    fun `paragraph 44 A-01 is set aside for a dramatically rarer known word next to a much more common neighbour`() {
        // Reproduces the reported bug: "due" is itself a rare but real dictionary entry, so A-01 used to
        // block any correction to "die" outright - regardless of "die" being nearly 40000x more frequent.
        store.putWord(WordEntry("due", 24L))
        store.putWord(WordEntry("die", 889_897L))
        
        assertEquals("die", provider.autocorrectFor("due", null))
    }
    
    @Test
    fun `paragraph 44 A-01 still protects a known word whose frequency is only modestly lower than a neighbour's`() {
        store.putWord(WordEntry("Bad", 100L))
        store.putWord(WordEntry("Bat", 200L))
        
        assertNull(provider.autocorrectFor("Bad", null))
    }
    
    @Test
    fun `paragraph 44 shouldOverrideKnownWord matches autocorrectFor's own threshold`() {
        store.putWord(WordEntry("ddr", 4_405L))
        store.putWord(WordEntry("der", 1_004_234L))
        
        assertTrue(provider.shouldOverrideKnownWord("ddr", "der"))
        assertFalse(provider.shouldOverrideKnownWord("der", "ddr"))
    }
    
    @Test
    fun `D-113 the ratio override does not fire on a cost-2 edit even at an extreme frequency ratio`() {
        // "spreche" (a common verb form) and "Sprache" (a noun) differ by "e"->"a" - not adjacent keys on
        // QWERTZ, so this is a cost-2 edit, not the cost-1 typo class §44 was built for ("due"->"die",
        // "ddr"->"der"). A huge frequency ratio alone must not be enough to override A-01 here.
        store.putWord(WordEntry("spreche", 49L))
        store.putWord(WordEntry("Sprache", 7228L))
        
        // The ratio check alone (unchanged) still says yes - it's the new cost-2 exclusion in
        // bestCorrection() that actually blocks the override, not a change to the ratio rule itself.
        assertTrue(provider.shouldOverrideKnownWord("spreche", "Sprache"))
        assertNull(provider.autocorrectFor("spreche", null))
    }
    
    @Test
    fun `D-113 the ratio override still fires for a genuine cost-1 adjacent-key typo`() {
        store.putWord(WordEntry("due", 24L))
        store.putWord(WordEntry("die", 889_897L))
        
        // Unchanged from the original §44 behaviour: a single adjacent-key slip still overrides A-01.
        assertEquals("die", provider.autocorrectFor("due", null))
    }
    
    @Test
    fun `D-114 minAutocorrectFrequency defaults to no floor - unaffected by the reported case's fix`() {
        // The shared no-floor provider (default constructor) still finds a low-frequency candidate, exactly
        // as before D-114 - the floor is opt-in via the constructor, not a blanket behaviour change.
        store.putWord(WordEntry("Virgin", 62L))
        
        assertEquals("Virgin", provider.autocorrectFor("Virhin", null))
    }
    
    @Test
    fun `D-114 a candidate below minAutocorrectFrequency is never offered, however good its edit cost`() {
        // Reproduces the reported bug: "vorhin" is missing from the dictionary entirely, and "Virgin" (an
        // English-proper-noun artefact of the German Wikipedia corpus) was the only candidate within the
        // edit-cost budget - low-confidence, but nothing stopped it from winning by default.
        val guardedStore = InMemoryDictionaryStore()
        val guardedProvider = DictionarySuggestionProvider(guardedStore, minAutocorrectFrequency = 300)
        guardedStore.putWord(WordEntry("Virgin", 62L))
        guardedStore.putWord(WordEntry("Vorhinein", 11L))
        
        assertNull(guardedProvider.autocorrectFor("Virhin", null))
        // A candidate at or above the floor is unaffected.
        guardedStore.putWord(WordEntry("Wort", 300L))
        assertEquals("Wort", guardedProvider.autocorrectFor("W8rt", null))
    }
    
    @Test
    fun `D-115 D-125 an unknown regular verb inflection is protected, not corrected to a different known word`() {
        // Reproduces the reported bug: "beurteilst" (2nd person singular, not itself in the dictionary)
        // must not autocorrect to "beurteilt" (a different real inflected form) just because the latter is
        // known and within edit-cost budget.
        store.putWord(WordEntry("beurteilen", 139L))
        store.putWord(WordEntry("beurteilt", 306L))
        
        assertNull(provider.autocorrectFor("beurteilst", null))
    }
    
    @Test
    fun `autocorrectFor returns null for a too-short token`() {
        store.putWord(WordEntry("der", 100L))
        assertNull(provider.autocorrectFor("d", null))
    }
    
    @Test
    fun `D-48 diacriticRestoration restores omitted umlauts`() {
        store.putWord(WordEntry("können", 100L))
        
        assertEquals("können", provider.diacriticRestoration("konnen", null))
        // ß is a diacritic too: a typed "russ" restores to the sharp-s spelling.
        store.putWord(WordEntry("ruß", 30L))
        assertEquals("ruß", provider.diacriticRestoration("russ", null))
    }
    
    @Test
    fun `D-48 diacriticRestoration returns null for a valid word or a real typo`() {
        store.putWord(WordEntry("können", 100L))
        store.putWord(WordEntry("Masse", 100L))
        // A-01: an already-known word is left alone.
        assertNull(provider.diacriticRestoration("masse", null))
        // A pure typo that is not merely a missing diacritic has no restoration.
        assertNull(provider.diacriticRestoration("kannan", null))
    }
    
    @Test
    fun `autocorrectFor proposes the most frequent single-edit neighbour`() {
        store.putWord(WordEntry("der", 100L))
        store.putWord(WordEntry("den", 50L))
        
        assertEquals("der", provider.autocorrectFor("dee", null))
    }
    
    @Test
    fun `A-04 a blacklisted neighbour is not proposed as an autocorrection`() {
        store.putWord(WordEntry("der", 100L))
        store.putWord(WordEntry("den", 50L))
        store.blacklist("der", BlacklistCategory.USER)
        
        assertEquals("den", provider.autocorrectFor("dee", null))
    }
    
    @Test
    fun `D-12 a close neighbour is offered even when the typed word is itself valid`() {
        store.putWord(WordEntry("Mut", 50L))
        store.putWord(WordEntry("mit", 100L))
        
        val words = provider.suggestionsFor("mut", null).map { it.word }
        assertTrue(words.contains("mit"), "the intended \"mit\" must still be offered for a valid \"mut\"")
    }
    
    @Test
    fun `D-12 an umlaut variant is offered for a diacritic-less prefix`() {
        store.putWord(WordEntry("grün", 80L))
        
        val words = provider.suggestionsFor("grun", null).map { it.word }
        assertTrue(words.contains("grün"), "\"grun\" must surface \"grün\"")
    }
    
    @Test
    fun `D-12 a single mistype surfaces the intended word`() {
        store.putWord(WordEntry("Default", 30L))
        
        val words = provider.suggestionsFor("defaukt", null).map { it.word }
        assertTrue(words.contains("Default"), "\"defaukt\" must surface \"Default\"")
    }
    
    @Test
    fun `D-12 autocorrect is umlaut-aware`() {
        store.putWord(WordEntry("grün", 80L))
        
        assertEquals("grün", provider.autocorrectFor("grun", null))
    }
    
    @Test
    fun `fuzzy neighbours are not offered for a one or two letter token`() {
        store.putWord(WordEntry("mit", 100L))
        
        // "mu" must not fuzzy-match "mit"; only prefix completion applies at this length.
        assertFalse(provider.suggestionsFor("mu", null).map { it.word }.contains("mit"))
    }
    
    @Test
    fun `D-28 two adjacent-key typos are corrected (komplezz to komplett)`() {
        store.putWord(WordEntry("komplett", 40L))
        
        assertTrue(provider.suggestionsFor("komplezz", null).map { it.word }.contains("komplett"))
        assertEquals("komplett", provider.autocorrectFor("komplezz", null))
    }
    
    @Test
    fun `D-28 two unrelated (non-adjacent) edits are not corrected`() {
        store.putWord(WordEntry("haus", 100L))
        
        // "hoos" is two non-adjacent substitutions from "haus" - beyond the correction budget.
        assertNull(provider.autocorrectFor("hoos", null))
        assertFalse(provider.suggestionsFor("hoos", null).map { it.word }.contains("haus"))
    }
    
    @Test
    fun `D-38 the lower-edit-cost correction wins over a more frequent one (dasy to dass)`() {
        store.putWord(WordEntry("das", 1000L))
        store.putWord(WordEntry("dass", 50L))
        
        // "das" is far more frequent, but "dass" is one adjacent edit (y->s) vs a deletion for "das".
        assertEquals("dass", provider.autocorrectFor("dasy", null))
    }
    
    @Test
    fun `D-38 a first-key typo is corrected (eerden to werden)`() {
        store.putWord(WordEntry("werden", 500L))
        
        assertEquals("werden", provider.autocorrectFor("eerden", null))
    }
    
    @Test
    fun `D-38 a missing initial umlaut is corrected (Uberblick to Überblick)`() {
        store.putWord(WordEntry("Überblick", 80L))
        
        assertEquals("Überblick", provider.autocorrectFor("Uberblick", null))
    }
    
    @Test
    fun `D-40 a token with a stray digit corrects to the intended word (W8rt to Wort)`() {
        store.putWord(WordEntry("Wort", 300L))
        
        // The service keeps a mid-word digit in the token; the digit is one substitution from the letter.
        assertEquals("Wort", provider.autocorrectFor("W8rt", null))
    }
    
    @Test
    fun `D-67 highConfidenceCorrection accepts a single adjacent-key slip (kleiben to kleinen)`() {
        store.putWord(WordEntry("kleinen", 200L))
        
        // "b" and "n" are adjacent QWERTZ keys, so this is a cost-1 correction.
        assertEquals("kleinen", provider.highConfidenceCorrection("kleiben", null))
    }
    
    @Test
    fun `D-67 highConfidenceCorrection rejects a cost-2 correction that autocorrectFor still accepts`() {
        store.putWord(WordEntry("komplett", 40L))
        
        // "komplezz" is two adjacent-key substitutions from "komplett" (cost 2, two ADJACENT_SUB_COST edits):
        // still within the ordinary autocorrect budget (D-28), but above the single-adjacent-edit
        // high-confidence ceiling, so it must not veto a split.
        assertEquals("komplett", provider.autocorrectFor("komplezz", null))
        assertNull(provider.highConfidenceCorrection("komplezz", null))
    }
    
    @Test
    fun `A-01 highConfidenceCorrection never overrides an already-known word`() {
        store.putWord(WordEntry("haus", 100L))
        assertNull(provider.highConfidenceCorrection("haus", null))
    }
}

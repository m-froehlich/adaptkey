// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.NoOpLanguageRules
import de.froehlichmedia.adaptkey.suggestion.Umlaut
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
    private val provider = DictionarySuggestionProvider(store, diacriticFolding = Umlaut)
    
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
    fun `D-365 a well-established learned bigram now competes fairly against a merely-bundled one`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Haus", 10L))
        // An ordinary, unrelated bundled bigram - ranked purely on frequency, "Haus" would win a raw,
        // unscaled +1-per-use comparison against anything less than 100 personal repetitions.
        store.putBigram("mein", "Haus", 100L)
        // LearnedBigramBoost's own reference point (50 uses = "well-established") reached via a real,
        // personally-typed context, not a seeded one.
        repeat(50) { store.learnContext("Hund", "mein") }
        
        assertEquals("Hund", provider.suggestionsFor("h", "mein").first().word)
    }
    
    @Test
    fun `D-366 a personal trigram match elevates a candidate even while typing a prefix, not only at a blank slate`() {
        store.putWord(WordEntry("Schatz", 10L))
        store.putWord(WordEntry("Schuh", 10L))
        // A solidly common bundled bigram for the same "mein " context - wins the ordinary, context-free
        // comparison against a merely-learned-bigram "Schatz".
        store.putBigram("mein", "Schuh", 150L)
        // A sparse but real personal trigram: "Hallo mein Schatz", typed a handful of times.
        repeat(3) { store.learnContext("Schatz", "mein", "Hallo") }
        
        // Without the two-word context, ordinary bigram ranking alone still favours the bundled "Schuh" -
        // the exact behaviour D-366 reported as already correct at a blank slate.
        assertEquals("Schuh", provider.suggestionsFor("s", "mein").first().word)
        
        // With the very same context genuinely typed before ("Hallo mein"), the sparse personal trigram
        // match now elevates "Schatz" instead - this is the signal D-366 reported as vanishing the moment
        // the user starts typing a prefix; it must not vanish any more.
        assertEquals("Schatz", provider.suggestionsFor("s", "mein", "Hallo").first().word)
    }
    
    @Test
    fun `D-144 a prefix typed without its umlaut still reaches the correctly-spelled word`() {
        store.putWord(WordEntry("tatsächlich", 500L))
        
        val words = provider.suggestionsFor("tatsachl", null).map { it.word }
        assertTrue(words.contains("tatsächlich"))
    }
    
    @Test
    fun `D-144 a prefix typed with ss instead of ß still reaches the correctly-spelled word`() {
        store.putWord(WordEntry("Straße", 500L))
        
        // "strass" is not a literal prefix of "straße" at all - only unfolding the trailing "ss" to "ß"
        // (Umlaut.unfoldCandidates) makes "straß" reachable as a real dictionary prefix.
        val words = provider.suggestionsFor("strass", null).map { it.word }
        assertTrue(words.contains("Straße"))
    }
    
    @Test
    fun `D-328 a single-position keyboard-neighbour typo still surfaces the completion mid-word`() {
        store.putWord(WordEntry("vermutlich", 500L))
        store.putWord(WordEntry("vermuten", 300L))
        
        // "vetmut" (6 chars, typo at position 3: t for r) shares no literal prefix with "vermut..." at all,
        // so the ordinary prefix scan finds nothing; the full-token edit-distance search is still far out of
        // budget while the word is only partially typed. The neighbour-prefix escalation reaches it.
        val words = provider.suggestionsFor("vetmut", null).map { it.word }
        assertTrue(words.contains("vermutlich"))
    }
    
    @Test
    fun `D-328 a neighbour typo plus a missing umlaut are resolved together in one pass`() {
        store.putWord(WordEntry("tatsächlich", 500L))
        
        // "twtsachl": typo at position 2 (w for a) AND the ä omitted. The neighbour-substituted prefix
        // "tatsachl" fed back through Umlaut.unfoldCandidates reaches "tatsächlich".
        val words = provider.suggestionsFor("twtsachl", null).map { it.word }
        assertTrue(words.contains("tatsächlich"))
    }
    
    @Test
    fun `D-328 the neighbour-prefix escalation does not fire on a short token below the minimum length`() {
        store.putWord(WordEntry("werden", 500L))
        
        // L = 3 ("wqr") is below MIN_NEIGHBOUR_PREFIX_LENGTH (5), so the escalation is skipped; a neighbour
        // substitution on a 3-letter token would match far too much. The ordinary fuzzy search still handles
        // short tokens at the full-word stage.
        val words = provider.suggestionsFor("wqr", null).map { it.word }
        assertFalse(words.contains("werden"))
    }
    
    @Test
    fun `D-328 the escalation fires at the minimum length boundary of 5`() {
        store.putWord(WordEntry("vermutlich", 500L))
        store.putWord(WordEntry("vermuten", 300L))
        
        // L = 5 is exactly the inclusive lower bound; the neighbour variant "vermu" (t->r at position 2)
        // reaches both completions.
        val words = provider.suggestionsFor("vetmu", null).map { it.word }
        assertTrue(words.contains("vermutlich"))
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
    fun `D-246,D-429 nextWordSuggestions ranks a trigram match by its rescaled, recency-boosted count`() {
        store.putWord(WordEntry("Nachbar", 3L))
        store.putWord(WordEntry("Auto", 3L))
        repeat(5) { store.learn("Nachbar", "der", "ist") }
        // Bigram-only competitor: no trigram entry at all for the "ist der" -> ? context.
        store.putBigram("der", "Auto", 10L)
        
        val results = provider.nextWordSuggestions("der", "ist")
        assertEquals(listOf("Nachbar", "Auto"), results.map { it.word })
        // D-429: the trigram's own raw count (5) is no longer used directly - it is rescaled through
        // LearnedBigramBoost exactly like every other n-gram contribution in this class, and (since it was
        // just touched) recency-boosted too. lastTouched==now==0 forces the same "recent" branch the real
        // call above hits (it was touched moments before this assertion runs), without depending on the
        // test's own wall-clock timing.
        assertEquals(LearnedBigramBoost.boost(5L, 0L, 0L), results.first { it.word == "Nachbar" }.score, 0.001)
        // The bigram-only word is discounted (D-246: TRIGRAM_BACKOFF_WEIGHT = 0.4) since two-word context
        // was available - 10 * 0.4 = 4, still well below the trigram match's now much larger boosted score.
        assertEquals(4.0, results.first { it.word == "Auto" }.score)
    }
    
    @Test
    fun `D-246 the trigram preference is soft - an overwhelmingly frequent bigram-only word can still outrank it`() {
        store.putWord(WordEntry("Nachbar", 3L))
        store.putWord(WordEntry("Haus", 3L))
        store.learn("Nachbar", "der", "ist")
        store.putBigram("der", "Haus", 1000L)
        
        val words = provider.nextWordSuggestions("der", "ist").map { it.word }
        assertEquals(listOf("Haus", "Nachbar"), words)
    }
    
    @Test
    fun `D-246 without previousPreviousWord, ranking falls back to plain undiscounted bigram counts`() {
        store.putWord(WordEntry("Hund", 10L))
        store.putWord(WordEntry("Hut", 10L))
        store.putBigram("der", "Hund", 40L)
        store.putBigram("der", "Hut", 5L)
        
        val results = provider.nextWordSuggestions("der", null)
        assertEquals(listOf("Hund", "Hut"), results.map { it.word })
        // Undiscounted - not multiplied by TRIGRAM_BACKOFF_WEIGHT, since there is no context to back off from.
        assertEquals(40.0, results.first { it.word == "Hund" }.score)
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
    fun `D-244 the raised ratio still fires for the real ddr to der case, at 228x`() {
        store.putWord(WordEntry("ddr", 4_405L))
        store.putWord(WordEntry("der", 1_004_234L))
        
        assertEquals("der", provider.autocorrectFor("ddr", null))
    }
    
    @Test
    fun `D-244 a modest 70x ratio no longer overrides A-01 (Ohren to Ihren regression)`() {
        // Real bundled frequencies: "Ohren" (ears, an entirely ordinary German word) was silently
        // autocorrected to "Ihren" ("o"/"i" are QWERTZ-adjacent, cost 1) purely because the old 50x bar was
        // too permissive for a genuine, unrelated word pair with a merely modest frequency gap.
        store.putWord(WordEntry("Ohren", 170L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Ihren", 11_907L))
        
        assertNull(provider.autocorrectFor("Ohren", null))
    }
    
    @Test
    fun `D-403 a learned word is never overridden by the ratio check, however low its own frequency`() {
        // A freshly-learned word's own frequency equals exactly its reinforcement count (learn() sets it to
        // 1 on first promotion) - well within a common word's 100x reach on its own, which is exactly why
        // this exemption exists: the ratio rule was calibrated against bundled corpus rarity, not a
        // personally-taught word's own necessarily tiny count.
        store.learn("kwp", null, null)
        store.putWord(WordEntry("Kap", 500L))
        
        assertFalse(provider.shouldOverrideKnownWord("kwp", "Kap"))
    }
    
    @Test
    fun `D-403 a learned word's own autocorrect protection survives an extreme frequency gap`() {
        store.learn("avd", null, null)
        // "d"/"s" are QWERTZ-adjacent (home row), so "avs" is a genuine cost-1 rival for the learned "avd" -
        // exercises the real end-to-end autocorrectFor() path, not just the ratio check in isolation.
        store.putWord(WordEntry("avs", 50_000L))
        
        assertNull(provider.autocorrectFor("avd", null))
    }
    
    @Test
    fun `D-403 the exemption is scoped to learned words only - a bundled word at the same low frequency is still overridden`() {
        // Regression guard: confirms the new learnedCasingOf() check does not accidentally widen the
        // existing bundled-word ratio override (D-244's own "due"/"ddr" cases already cover this
        // structurally, via store.putWord alone, never store.learn - this test makes the distinction
        // explicit rather than relying on that being noticed incidentally).
        store.putWord(WordEntry("due", 1L))
        store.putWord(WordEntry("die", 500L))
        
        assertTrue(provider.shouldOverrideKnownWord("due", "die"))
    }
    
    @Test
    fun `D-404-followup reproduces the real reported bug - an unlearned acronym must not be autocorrected away`() {
        // Confirmed against the real bundled German dictionary before this fix: typing "etf" (never yet
        // learned, so not protected by D-403's own learnedCasingOf() check at all) was silently
        // autocorrected to "etc" on every single attempt - a cost-1 QWERTZ-adjacent, far more frequent real
        // word - which meant "ETF" could never even accumulate a pending count through ordinary typing, let
        // alone reach W-02's promotion threshold. Reproduced here with a small, synthetic pair so the fix is
        // pinned independently of the live dictionary's own contents ever changing.
        store.putWord(WordEntry("etc", 50_000L))
        
        assertNull(provider.autocorrectFor("ETF", null))
    }
    
    @Test
    fun `D-404-followup an acronym is never autocorrected away even when typed lower-case`() {
        // The acronym veto is keyed off the original typed casing, not the lower-cased lookup token -
        // still applies as long as the whole token was typed in capitals.
        store.putWord(WordEntry("avs", 50_000L))
        
        assertNull(provider.autocorrectFor("AVD", null))
    }
    
    @Test
    fun `D-404-followup the acronym veto does not suppress correction of an ordinary lower-case typo`() {
        // Regression guard: confirms the new Acronym.isAcronym(input) check is scoped to fully-uppercase
        // input only - an ordinary lower-case typo (D-38's own "dasy" -> "dass" case) must still
        // autocorrect exactly as before.
        store.putWord(WordEntry("das", 1000L))
        store.putWord(WordEntry("dass", 50L))
        
        assertEquals("dass", provider.autocorrectFor("dasy", null))
    }
    
    @Test
    fun `D-404 Tier 2 a family sibling is never overridden however extreme the frequency ratio - Kugel to Kugeln`() {
        // The exact motivating case: the far more frequent plural must never silently replace a correctly
        // typed singular purely for being more common in the corpus - the two are the same D-412 word
        // family, not the coincidental collision the ratio rule exists to catch.
        store.putWord(WordEntry("Kugel", 50L))
        store.putWord(WordEntry("Kugeln", 500_000L, lemma = "Kugel"))
        
        assertFalse(provider.shouldOverrideKnownWord("Kugel", "Kugeln"))
    }
    
    @Test
    fun `D-404 Tier 2 two siblings of a common base never override each other - lief to lauft`() {
        // Neither side is the other's own lemma - both link to a shared third base ("laufen") - still the
        // same family and still exempt from the ratio override.
        store.putWord(WordEntry("lief", 100L, lemma = "laufen"))
        store.putWord(WordEntry("läuft", 200_000L, lemma = "laufen"))
        
        assertFalse(provider.shouldOverrideKnownWord("lief", "läuft"))
    }
    
    @Test
    fun `D-404 Tier 2 does not affect an unrelated pair with no lemma link, even at a similarly extreme ratio`() {
        // Regression guard: sameWordFamily() only ever suppresses a genuine family match - an ordinary
        // unrelated pair (D-244's own "ddr"/"der", neither lemma-linked) still overrides exactly as before.
        store.putWord(WordEntry("ddr", 4_405L))
        store.putWord(WordEntry("der", 1_004_234L))
        
        assertTrue(provider.shouldOverrideKnownWord("ddr", "der"))
    }
    
    @Test
    fun `D-114 D-353 a noun-tagged candidate at a low confidence is never offered, however good its edit cost`() {
        // Reproduces the reported bug: "vorhin" is missing from the dictionary entirely, and "Virgin" (an
        // English-proper-noun artefact of the German Wikipedia corpus, tagged NOUN like the real
        // dict_de.tsv entry) was the only candidate within the edit-cost budget - low-confidence, but
        // nothing stopped it from winning by default. D-353: the confidence gate is no longer opt-in via a
        // constructor parameter - every provider now applies it, at the default (MEDIUM) aggressiveness.
        store.putWord(WordEntry("Virgin", 62L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Vorhinein", 11L))
        
        assertNull(provider.autocorrectFor("Virhin", null))
        // A common, non-noun candidate is unaffected.
        store.putWord(WordEntry("Wort", 300L))
        assertEquals("Wort", provider.autocorrectFor("W8rt", null))
    }
    
    @Test
    fun `D-227 D-353 a non-noun cost-1 candidate still wins at a frequency where a noun-tagged one would not`() {
        // Reproduces the reported bug: "übrigebs" (a keyboard-adjacent b/n typo of "übrigens", tagged OTHER
        // like the real dict_de.tsv entry) has no dictionary entry itself, and "übrigens" (frequency 79) sat
        // below the old 300 floor purely because the corpus under-counts it relative to hyper-frequent
        // words - letting a low-quality A-05 split ("übrig"+"Ebs") win instead of this cost-1 correction.
        // Contrasted directly against the D-114 case above: same cost tier and frequency range, but
        // "übrigens" is tagged OTHER (an ordinary adverb), not NOUN, so [CorrectionConfidence] holds it to
        // a far lower frequency bar than a noun.
        store.putWord(WordEntry("übrigens", 79L, partsOfSpeech = setOf(PartOfSpeech.OTHER)))
        
        assertEquals("übrigens", provider.autocorrectFor("übrigebs", null))
    }
    
    @Test
    fun `D-116 D-353 a common, correctly-tagged noun is not held to the same bar as a rare one`() {
        // The compound-reconstruction path (D-116) routes its own "rest" correction through the exact same
        // bestCorrection() confidence gate ("jahreb" -> "Jahren", cost 1, tagged NOUN) - a flat noun penalty
        // would have wrongly punished this, ordinary, correctly-recognised noun exactly as much as a rare
        // proper-noun corpus artefact like "Virgin" above; CorrectionConfidence instead holds nouns to a
        // much higher (not unreachable) frequency bar - see its own KDoc.
        store.putWord(WordEntry("Beitrag", 500L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Jahren", 2_000L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        
        assertTrue(provider.suggestionsFor("beitragsjahreb", null).map { it.word }.contains("Beitragsjahren"))
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
    fun `D-410 the verb-inflection protection above is German-specific - a non-German provider does not apply it`() {
        // Same data/token as the D-115/D-125 test above, but built with the no-op language rules a
        // non-German store now resolves to (LanguageRulesRegistry) - the protection must not fire, so the
        // token is free to autocorrect to the closer, more frequent known word exactly as any other
        // unrecognised token would.
        val noOpProvider = DictionarySuggestionProvider(store, languageRules = NoOpLanguageRules)
        store.putWord(WordEntry("beurteilen", 139L))
        store.putWord(WordEntry("beurteilt", 306L))
        
        assertEquals("beurteilt", noOpProvider.autocorrectFor("beurteilst", null))
    }
    
    @Test
    fun `D-252 an unknown adjective comparative is protected, not corrected to a different known word`() {
        // The adjective counterpart of the D-115/D-125 verb-inflection test above: "zuversichtlicher"
        // (comparative, not itself in the dictionary) must not autocorrect to a different known word just
        // because it is within edit-cost budget and far more frequent.
        store.putWord(WordEntry("zuversichtlich", 17L))
        store.putWord(WordEntry("zuversichtliche", 50_000L))
        
        assertNull(provider.autocorrectFor("zuversichtlicher", null))
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
    fun `D-435 a language with no DiacriticFolding of its own never restores German umlauts`() {
        // The exact fix for the cross-language leak: before D-435, Umlaut.* was called unconditionally
        // regardless of which language's own provider this was, so a non-German language got German's
        // restoration behaviour "for free". A provider left on the NoOpDiacriticFolding default must not.
        val noOpProvider = DictionarySuggestionProvider(store)
        store.putWord(WordEntry("können", 100L))
        
        assertEquals(null, noOpProvider.diacriticRestoration("konnen", null))
    }
    
    @Test
    fun `D-154 D-155 diacriticRestoration restores uber and fur regardless of other-language homographs`() {
        // "fur" is coincidentally a real English word and "uber" a foreign-looking ASCII token - neither
        // is a reason for the German dictionary's own umlaut restoration to stay silent (AdaptKeyService's
        // finalizeAndCommit consults this provider directly, independent of cross-language suppression).
        store.putWord(WordEntry("über", 60204L))
        store.putWord(WordEntry("für", 158762L))
        
        assertEquals("über", provider.diacriticRestoration("uber", null))
        assertEquals("für", provider.diacriticRestoration("fur", null))
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
    fun `D-204 diacriticRestoration recognises this app's own long-press-alternative typing convention`() {
        // "Grüße" (frequency 18) is far below the production autocorrect frequency floor (300) and 2
        // edits away from "große"/"größe" under the ordinary ss-fold convention - but "gruse" is this
        // app's own lazy long-press-alternative shorthand (ü on u, ß on s, both reached by long-pressing
        // the shown base key), so it must be recognised as an exact diacritic match, not a fuzzy one.
        store.putWord(WordEntry("Grüße", 18L))
        store.putWord(WordEntry("große", 11204L))
        store.putWord(WordEntry("Größe", 2769L))
        
        assertEquals("Grüße", provider.diacriticRestoration("gruse", null))
    }
    
    @Test
    fun `D-204 diacriticRestoration still restores the formal ss convention for sharp s`() {
        // The pre-existing "ruß"/"russ" convention (D-48) must keep working unchanged alongside D-204's
        // new bare-s convention - neither fold variant may crowd out the other.
        store.putWord(WordEntry("ruß", 30L))
        
        assertEquals("ruß", provider.diacriticRestoration("russ", null))
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
    fun `D-205 a closer fuzzy candidate generally outranks a farther, more frequent one`() {
        // "vein" is one adjacent-key substitution from "bein" (v neighbours b); "sein" is a
        // non-adjacent substitution (cost 2). Despite "sein" being 20x more frequent, the ranking
        // must still favour the closer candidate - the suggestion bar's own analogue of D-38's
        // already-shipped cost-first autocorrect ranking.
        store.putWord(WordEntry("vein", 1000L))
        store.putWord(WordEntry("sein", 20000L))
        
        val words = provider.suggestionsFor("bein", null).map { it.word }
        assertEquals(listOf("vein", "sein"), words.take(2))
    }
    
    @Test
    fun `D-205 an overwhelmingly more frequent farther candidate can still win - a soft preference`() {
        // Same shape as above, but "sein" is now frequent enough (near the bundled corpus's own
        // realistic ceiling) to overcome the cost discount against a "vein" at the corpus's own
        // frequency floor - proving FUZZY_COST_DECAY is a soft preference, not a hard cost-first
        // rule like bestCorrection()'s own autocorrect ranking.
        store.putWord(WordEntry("vein", 8L))
        store.putWord(WordEntry("sein", 1_000_000L))
        
        assertEquals("sein", provider.suggestionsFor("bein", null).first().word)
    }
    
    @Test
    fun `D-272 a closer prefix completion generally outranks a farther, more frequent one from the same family`() {
        // Real bundled dict_de.tsv frequencies: "natürliche" (the adjective inflection) outscores
        // "natürlich" (the adverb) on raw corpus frequency alone - a Wikipedia-register artefact ("natürliche
        // Ressourcen" etc. is far more common in encyclopedic text than the everyday discourse-marker
        // adverb) - but "natürlich" needs one fewer additional character beyond the typed "natürl"; the
        // closer completion must still win.
        store.putWord(WordEntry("natürlich", 707L))
        store.putWord(WordEntry("natürliche", 1313L))
        
        assertEquals("natürlich", provider.suggestionsFor("natürl", null).first().word)
    }
    
    @Test
    fun `D-272 an overwhelmingly more frequent farther completion can still win - a soft preference`() {
        // Same one-character gap as above, but the farther candidate is now frequent enough to overcome the
        // decay - proving PREFIX_LENGTH_DECAY is a soft preference, not a hard "shorter always wins" rule,
        // mirroring D-205's own soft-preference shape for the fuzzy-match path.
        store.putWord(WordEntry("natürlich", 8L))
        store.putWord(WordEntry("natürliche", 1_000_000L))
        
        assertEquals("natürliche", provider.suggestionsFor("natürl", null).first().word)
    }
    
    @Test
    fun `D-272 a much longer but far more frequent completion is not crowded out once both exceed the length cap`() {
        // Real bundled dict_de.tsv frequencies for a typed "inform": "informiert" needs 4 more characters,
        // "Informationen" needs 7 - both already past PREFIX_LENGTH_DECAY_CAP, so the decay must not keep
        // compounding between them once capped. "Informationen" is both the far more frequent and, in
        // practice, the far more plausible completion - it must not fall behind purely for needing 3 more
        // characters than the cap already stops distinguishing.
        store.putWord(WordEntry("informiert", 312L))
        store.putWord(WordEntry("Informationen", 1913L))
        
        assertEquals("Informationen", provider.suggestionsFor("inform", null).first().word)
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
    
    @Test
    fun `D-207 bestCorrectionFor reports a cost-1 correction as high-confidence`() {
        store.putWord(WordEntry("kleinen", 200L))
        
        val result = provider.bestCorrectionFor("kleiben", null)
        assertEquals("kleinen", result?.word)
        assertTrue(result?.highConfidence == true)
    }
    
    @Test
    fun `D-207 bestCorrectionFor reports a cost-2-only correction as not high-confidence`() {
        store.putWord(WordEntry("komplett", 40L))
        
        // Same word pair as D-67's own two-call test above - bestCorrectionFor must agree with what
        // autocorrectFor()/highConfidenceCorrection() would separately have concluded, from one search.
        val result = provider.bestCorrectionFor("komplezz", null)
        assertEquals("komplett", result?.word)
        assertFalse(result?.highConfidence == true)
    }
    
    @Test
    fun `D-207 bestCorrectionFor returns null exactly when autocorrectFor does`() {
        store.putWord(WordEntry("haus", 100L))
        assertNull(provider.bestCorrectionFor("haus", null))
    }
    
    @Test
    fun `D-116 an unlisted compound is reconstructed from a known noun first part and a correctable rest`() {
        // Reproduces the reported case: "Beitragsjahren" itself is too rare for the dictionary, but its
        // first part ("Beitrag") is known and its typo'd rest ("jahreb") is a cost-1 slip (b/n are adjacent
        // QWERTZ keys) of the known "Jahren".
        store.putWord(WordEntry("Beitrag", 500L, setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Jahren", 2_000L, setOf(PartOfSpeech.NOUN)))
        
        assertTrue(provider.suggestionsFor("beitragsjahreb", null).map { it.word }.contains("Beitragsjahren"))
    }
    
    @Test
    fun `D-116 is suggestion-only - never silently autocorrected`() {
        store.putWord(WordEntry("Beitrag", 500L, setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Jahren", 2_000L, setOf(PartOfSpeech.NOUN)))
        
        // The split point can be genuinely ambiguous (the classic German compound-splitting problem), so
        // this must only ever appear as a suggestion, never fire through the autocorrect/high-confidence path.
        assertNull(provider.autocorrectFor("beitragsjahreb", null))
        assertNull(provider.highConfidenceCorrection("beitragsjahreb", null))
    }
    
    @Test
    fun `D-116 a known word that is not tagged as a noun is not accepted as a compound first part`() {
        // "kaufen" is a real, known word here, but only as a verb - starting the compound search from a
        // non-noun first part is explicitly out of scope for this round.
        store.putWord(WordEntry("Kaufen", 500L, setOf(PartOfSpeech.VERB)))
        store.putWord(WordEntry("Haus", 100L, setOf(PartOfSpeech.NOUN)))
        
        assertTrue(provider.suggestionsFor("kaufenhaus", null).isEmpty())
    }
    
    @Test
    fun `D-202 looksLikeUnsplitCompound is true for a token compoundCandidate can reconstruct`() {
        store.putWord(WordEntry("Beitrag", 500L, setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Jahren", 2_000L, setOf(PartOfSpeech.NOUN)))
        
        assertTrue(provider.looksLikeUnsplitCompound("beitragsjahreb"))
    }
    
    @Test
    fun `D-202 looksLikeUnsplitCompound is false when no compound reconstruction exists`() {
        store.putWord(WordEntry("Kaufen", 500L, setOf(PartOfSpeech.VERB)))
        store.putWord(WordEntry("Haus", 100L, setOf(PartOfSpeech.NOUN)))
        
        assertFalse(provider.looksLikeUnsplitCompound("kaufenhaus"))
    }
    
    @Test
    fun `D-202 looksLikeUnsplitCompound is false for an already-known word`() {
        store.putWord(WordEntry("haus", 100L, setOf(PartOfSpeech.NOUN)))
        
        assertFalse(provider.looksLikeUnsplitCompound("haus"))
    }
    
    @Test
    fun `D-117 a multi-typo word beyond the ordinary D-28 budget still surfaces as a suggestion`() {
        // "erkamm" -> "erkannt": two adjacent-key substitutions (m/n) plus an insertion (cost 4), beyond
        // the ordinary two-edit MAX_CORRECTION_COST but within the wider, suggestion-only D-117 fallback.
        store.putWord(WordEntry("erkannt", 300L))
        
        assertTrue(provider.suggestionsFor("erkamm", null).map { it.word }.contains("erkannt"))
    }
    
    @Test
    fun `D-117 is suggestion-only - never silently autocorrected`() {
        store.putWord(WordEntry("erkannt", 300L))
        
        assertNull(provider.autocorrectFor("erkamm", null))
        assertNull(provider.highConfidenceCorrection("erkamm", null))
    }
    
    @Test
    fun `D-117 does not fire once ordinary prefix or fuzzy matching already found something`() {
        store.putWord(WordEntry("erkannt", 300L))
        // "erk" is itself a prefix match for "erkannt" - the wider D-117 fallback must not additionally
        // kick in and start pulling in unrelated far-off candidates once there is already something.
        val words = provider.suggestionsFor("erk", null).map { it.word }
        assertEquals(listOf("erkannt"), words)
    }
    
    @Test
    fun `D-117 is not attempted for a token shorter than the minimum wide-fuzzy length`() {
        // "kamm" is short enough that a cost-4 budget would be far too loose to mean anything.
        store.putWord(WordEntry("kannst", 300L))
        assertTrue(provider.suggestionsFor("kamm", null).isEmpty())
    }
    
    @Test
    fun `D-431 hasObviousCandidate is true when an ordinary prefix or fuzzy match exists`() {
        store.putWord(WordEntry("Haus", 100L))
        assertTrue(provider.hasObviousCandidate("Hau", null, null))
    }
    
    @Test
    fun `D-431 hasObviousCandidate is false when nothing at all is found`() {
        assertFalse(provider.hasObviousCandidate("qxzvbklm", null, null))
    }
    
    @Test
    fun `D-431 hasObviousCandidate is false when only the D-117 wide-fuzzy fallback finds something`() {
        // "erkannt" is only reachable from "erkamm" via the D-117 wide-fuzzy fallback (see the test above) -
        // hasObviousCandidate must not count it, since A-13's own gate must not be blocked by it.
        store.putWord(WordEntry("erkannt", 300L))
        assertFalse(provider.hasObviousCandidate("erkamm", null, null))
        // Yet suggestionsFor() itself still finds it - the two must not silently drift apart on what
        // "found something" means for their own, deliberately different purposes.
        assertTrue(provider.suggestionsFor("erkamm", null).map { it.word }.contains("erkannt"))
    }
    
    @Test
    fun `D-431 the real reported repro - welxmche must not be blocked by a coincidental welsche wide-fuzzy match`() {
        // The user's own worked example (D-377): "welxmche" for "welche" (x mistyped, then a Backspace
        // attempt that landed on m instead). "welsche" is a genuine, if rare, German word that
        // coincidentally sits at a *cheaper* edit distance from "welxmche" (delete "m", substitute the
        // adjacent-key x->s) than the actually-intended two-deletion reconstruction "welche" - confirmed
        // real from a device report, not a hypothetical: before D-431, this alone silently blocked A-13's
        // own gate every time.
        store.putWord(WordEntry("welche", 500_000L))
        store.putWord(WordEntry("welsche", 50L))
        
        // The wide-fuzzy fallback genuinely does surface "welsche" (confirming the reported symptom's own
        // root cause, not just asserting the fix in isolation).
        assertTrue(provider.suggestionsFor("welxmche", null).map { it.word }.contains("welsche"))
        // But hasObviousCandidate must not count that last-resort match - A-13 still deserves its own
        // chance regardless.
        assertFalse(provider.hasObviousCandidate("welxmche", null, null))
    }
    
    @Test
    fun `D-160 the hot path with expensive fallbacks off skips the D-116 compound reconstruction`() {
        store.putWord(WordEntry("Beitrag", 500L, setOf(PartOfSpeech.NOUN)))
        store.putWord(WordEntry("Jahren", 2_000L, setOf(PartOfSpeech.NOUN)))
        
        assertTrue(provider.suggestionsFor("beitragsjahreb", null, includeExpensiveFallbacks = false).isEmpty())
    }
    
    @Test
    fun `D-160 the hot path with expensive fallbacks off skips the D-117 wide fuzzy search`() {
        store.putWord(WordEntry("erkannt", 300L))
        
        assertTrue(provider.suggestionsFor("erkamm", null, includeExpensiveFallbacks = false).isEmpty())
    }
    
    @Test
    fun `D-160 a plain prefix match is unaffected by the expensive-fallbacks flag`() {
        store.putWord(WordEntry("erkannt", 300L))
        
        // "erk" is itself a literal prefix of "erkannt" - reached via prefix completion, not fuzzy
        // matching, so this must not depend on includeExpensiveFallbacks either way.
        assertEquals(listOf("erkannt"), provider.suggestionsFor("erk", null, includeExpensiveFallbacks = false).map { it.word })
        assertEquals(listOf("erkannt"), provider.suggestionsFor("erk", null, includeExpensiveFallbacks = true).map { it.word })
    }
    
    @Test
    fun `D-208 the hot path with expensive fallbacks off skips D-12 fuzzy matching too`() {
        store.putWord(WordEntry("mit", 100L))
        
        // "mut" is a genuine fuzzy (not prefix) match for "mit" - D-208 moved this off the instant hot
        // path (its cost grows with the composing token's own length), unlike the plain-prefix case above.
        assertTrue(provider.suggestionsFor("mut", null, includeExpensiveFallbacks = false).isEmpty())
        assertEquals(listOf("mit"), provider.suggestionsFor("mut", null, includeExpensiveFallbacks = true).map { it.word })
    }
    
    @Test
    fun `D-208 fuzzy matching still applies in the deferred pass even when the hot path already found something`() {
        store.putWord(WordEntry("mut", 50L))
        store.putWord(WordEntry("mit", 100L))
        
        // Unlike D-116/D-117 (only attempted when candidates.isEmpty()), D-12 fuzzy matching must still
        // surface "mit" for "mut" even though "mut" itself is a real word with its own prefix completion.
        val words = provider.suggestionsFor("mut", null, includeExpensiveFallbacks = true).map { it.word }
        assertTrue(words.contains("mut"))
        assertTrue(words.contains("mit"))
    }
    
    @Test
    fun `D-211 the fuzzy scan stops early once isCancelled returns true`() {
        // Three fuzzy matches for "bein" (v/n/f all keyboard-neighbour "b" substitutions) - a real scan
        // would find all three; cancelling on the very first poll must stop it after at most one.
        store.putWord(WordEntry("vein", 100L))
        store.putWord(WordEntry("nein", 90L))
        store.putWord(WordEntry("fein", 80L))
        
        var calls = 0
        val words = provider.suggestionsFor("bein", null, includeExpensiveFallbacks = true) {
            calls++
            calls > 1
        }.map { it.word }
        
        assertTrue(words.size <= 1)
    }
    
    @Test
    fun `D-211 isCancelled that never returns true has no effect on the result`() {
        store.putWord(WordEntry("vein", 100L))
        store.putWord(WordEntry("nein", 90L))
        store.putWord(WordEntry("fein", 80L))
        
        val words = provider.suggestionsFor("bein", null, includeExpensiveFallbacks = true) { false }.map { it.word }
        
        assertEquals(setOf("vein", "nein", "fein"), words.toSet())
    }
    
    @Test
    fun `D-354 a prefix-changing correction is never silently applied, but still offered as a suggestion`() {
        // "aberkennen" ("ab-" + "erkennen") is unknown to the dictionary; "anerkennen" ("an-" + "erkennen")
        // is a real, far more frequent cost-1 neighbour. The typed token may itself be a genuine, simply
        // unlisted, prefixed word - never silently discarded, but still surfaced as an ordinary suggestion.
        store.putWord(WordEntry("anerkennen", 165L, partsOfSpeech = setOf(PartOfSpeech.OTHER)))
        
        assertNull(provider.autocorrectFor("aberkennen", null))
        assertTrue(provider.suggestionsFor("aberkennen", null).map { it.word }.contains("anerkennen"))
    }
    
    @Test
    fun `D-353 the Ohren to Ihren regression stays blocked end-to-end even at the most permissive aggressiveness`() {
        val aggressiveStore = InMemoryDictionaryStore()
        val aggressiveProvider = DictionarySuggestionProvider(aggressiveStore, aggressiveness = AutocorrectAggressiveness.AGGRESSIVE)
        aggressiveStore.putWord(WordEntry("Ohren", 170L, partsOfSpeech = setOf(PartOfSpeech.NOUN)))
        aggressiveStore.putWord(WordEntry("Ihren", 11_907L))
        
        assertNull(aggressiveProvider.autocorrectFor("Ohren", null))
    }
    
    @Test
    fun `D-353 a higher aggressiveness level still autocorrects a case the default one already did`() {
        val aggressiveStore = InMemoryDictionaryStore()
        val aggressiveProvider = DictionarySuggestionProvider(aggressiveStore, aggressiveness = AutocorrectAggressiveness.AGGRESSIVE)
        aggressiveStore.putWord(WordEntry("ddr", 4_405L))
        aggressiveStore.putWord(WordEntry("der", 1_004_234L))
        
        assertEquals("der", aggressiveProvider.autocorrectFor("ddr", null))
    }
    
    @Test
    fun `D-371 a digit-ending token is never silently corrected at MEDIUM, only offered as a suggestion`() {
        // A trailing digit accidentally left on an otherwise unambiguous word (e.g. a stray autocomplete/
        // stuck-key artefact, "hilfe9" for "hilfe") - the default (MEDIUM) level must not silently strip it.
        store.putWord(WordEntry("hilfe", 1_000_000L, partsOfSpeech = setOf(PartOfSpeech.OTHER)))
        
        assertNull(provider.autocorrectFor("hilfe9", null))
        assertTrue(provider.suggestionsFor("hilfe9", null).map { it.word }.contains("hilfe"))
    }
    
    @Test
    fun `D-371 the same digit-ending token auto-corrects at AGGRESSIVE`() {
        val aggressiveStore = InMemoryDictionaryStore()
        val aggressiveProvider = DictionarySuggestionProvider(aggressiveStore, aggressiveness = AutocorrectAggressiveness.AGGRESSIVE)
        aggressiveStore.putWord(WordEntry("hilfe", 1_000_000L, partsOfSpeech = setOf(PartOfSpeech.OTHER)))
        
        assertEquals("hilfe", aggressiveProvider.autocorrectFor("hilfe9", null))
    }
    
    @Test
    fun `D-411 a heavily and recently used personal word now outranks a moderately common bundled word`() {
        val now = 1_000_000_000L
        val boostStore = InMemoryDictionaryStore(clock = { now })
        val boostProvider = DictionarySuggestionProvider(boostStore, now = { now })
        boostStore.putWord(WordEntry("an", 2_000L))
        boostStore.learn("az", null, seedFrequency = 50L)
        
        val words = boostProvider.suggestionsFor("a", null).map { it.word }
        assertTrue(words.indexOf("az") < words.indexOf("an"))
    }
    
    @Test
    fun `D-411 a rarely used, long-untouched personal word still does not outrank a common bundled word`() {
        // now is well past the 14-day recency window from the touched-at-0 seed below.
        val now = 100L * 24 * 60 * 60 * 1000
        val boostStore = InMemoryDictionaryStore(clock = { 0L })
        val boostProvider = DictionarySuggestionProvider(boostStore, now = { now })
        boostStore.putWord(WordEntry("an", 2_000L))
        boostStore.learn("ay", null, seedFrequency = 2L)
        
        val words = boostProvider.suggestionsFor("a", null).map { it.word }
        assertTrue(words.indexOf("an") < words.indexOf("ay"))
    }
    
    @Test
    fun `D-429 a heavily and recently used personal bigram now outranks a moderately common bundled one`() {
        val now = 1_000_000_000L
        val boostStore = InMemoryDictionaryStore(clock = { now })
        val boostProvider = DictionarySuggestionProvider(boostStore, now = { now })
        boostStore.putWord(WordEntry("Hund", 10L))
        boostStore.putWord(WordEntry("Haus", 10L))
        // Unboosted, LearnedBigramBoost(50) = 250 - just below this bundled bigram's own 300 count, so the
        // recency multiplier is what must actually decide the ranking here, not the base scale alone.
        boostStore.putBigram("mein", "Haus", 300L)
        repeat(50) { boostStore.learnContext("Hund", "mein") }
        
        assertEquals("Hund", boostProvider.suggestionsFor("h", "mein").first().word)
    }
    
    @Test
    fun `D-429 the same well-established personal bigram loses that recency edge once long untouched`() {
        // now is well past the 14-day recency window from the touched-at-0 seed below.
        val now = 100L * 24 * 60 * 60 * 1000
        val boostStore = InMemoryDictionaryStore(clock = { 0L })
        val boostProvider = DictionarySuggestionProvider(boostStore, now = { now })
        boostStore.putWord(WordEntry("Hund", 10L))
        boostStore.putWord(WordEntry("Haus", 10L))
        boostStore.putBigram("mein", "Haus", 300L)
        repeat(50) { boostStore.learnContext("Hund", "mein") }
        
        assertEquals("Haus", boostProvider.suggestionsFor("h", "mein").first().word)
    }
    
    @Test
    fun `D-429 nextWordSuggestions rescales a raw personal trigram count instead of using it directly`() {
        // Before D-429, this was the one place in the ranking code still scoring a trigram match by its
        // literal reinforcement count - everywhere else already went through LearnedBigramBoost.
        val now = 1_000_000_000L
        val boostStore = InMemoryDictionaryStore(clock = { now })
        val boostProvider = DictionarySuggestionProvider(boostStore, now = { now })
        boostStore.putWord(WordEntry("Schatz", 10L))
        repeat(5) { boostStore.learnContext("Schatz", "mein", "Hallo") }
        
        val suggestion = boostProvider.nextWordSuggestions("mein", "Hallo").first { it.word == "Schatz" }
        assertTrue(suggestion.score > 5.0, "expected a boosted score above the raw count, was ${suggestion.score}")
    }
}

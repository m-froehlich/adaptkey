// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, real SQLite, no emulator) for the SQLite store's bounded autocorrect candidate
 * query — the fix for the per-keystroke whole-lexicon scan that made typing laggy with the real
 * dictionaries — and the end-to-end single-edit autocorrect over the SQLite store.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SqliteDictionaryStoreRoboTest {
    
    private fun store(name: String): SqliteDictionaryStore {
        return SqliteDictionaryStore(RuntimeEnvironment.getApplication(), name)
    }
    
    @Test
    fun unlearnReversesALearnThatCreatedABrandNewWord() {
        val store = store("unlearn-new.db")
        store.learn("neu", "ganz")
        store.unlearn("neu", "ganz")
        
        assertFalse(store.isKnownWord("neu"))
        assertEquals(0L, store.bigramFrequency("ganz", "neu"))
        store.close()
    }
    
    @Test
    fun unlearnReversesALearnThatReinforcedAnAlreadyKnownWord() {
        val store = store("unlearn-reinforce.db")
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", "der")
        store.unlearn("hund", "der")
        
        assertTrue(store.isKnownWord("hund"))
        assertEquals(3L, store.frequencyOf("hund"))
        assertEquals(0L, store.bigramFrequency("der", "hund"))
        store.close()
    }
    
    @Test
    fun correctionCandidatesAreBoundedByFirstCharAndLength() {
        val store = store("cand.db")
        listOf("haus", "hase", "maus", "h", "hausboot").forEach { store.putWord(WordEntry(it, 10L, emptySet())) }
        
        val candidates = store.correctionCandidates("haus")
        
        assertTrue(candidates.contains("haus"))
        assertTrue(candidates.contains("hase")) // same first char, length within one
        assertFalse(candidates.contains("maus")) // different first char
        assertFalse(candidates.contains("hausboot")) // length differs by more than one
        assertFalse(candidates.contains("h")) // length differs by more than one
        store.close()
    }
    
    @Test
    fun diacriticCandidatesAreNotBoundedByFrequencyUnlikeCorrectionCandidatesInANeighbourBucket() {
        val store = store("diacritic-cand.db")
        // Six same-bucket ("h", length 4-6) words at descending frequency; "hobel" (the lowest) plays the
        // role of a rare-but-correctly-spelled diacritic candidate (D-197 / spec §126) that
        // correctionCandidates' frequency-truncated LIMIT would starve out before diacriticRestoration ever
        // gets a chance to compare it against the token.
        // D-209: the token itself ("zobel", not a real word) starts with 'z', not 'h' - so the 'h' bucket
        // here is reached only as a synthetic keyboard-neighbour char, exercising the still-capped case
        // (D-209 lifts the cap only for the token's own literal first-character bucket, not every bucket
        // correctionCandidates searches - see correctionCandidatesAreNotBoundedByFrequencyInTheTokenSOwnBucket
        // below for that other case).
        listOf("haupt" to 500L, "hafen" to 400L, "hallo" to 300L, "hemde" to 200L, "hitze" to 100L, "hobel" to 1L)
            .forEach { (word, freq) -> store.putWord(WordEntry(word, freq, emptySet())) }
        // A large synthetic firstChars set (only 'h' actually matches any seeded word; the other 399 are
        // disjoint Unicode code points no real word ever starts with) forces perBucketLimit down to
        // CANDIDATE_LIMIT(2000)/400 = 5 - fewer than the six seeded words - without needing to seed
        // thousands of real words to reach the real per-bucket cap in a fast unit test.
        val firstChars = (setOf('h') + (1..399).map { offset -> Char(8000 + offset) }).toSet()
        
        val bounded = store.correctionCandidates("zobel", firstChars)
        val unbounded = store.diacriticCandidates("zobel", firstChars)
        
        assertFalse(bounded.contains("hobel"))
        assertTrue(unbounded.containsAll(listOf("haupt", "hafen", "hallo", "hemde", "hitze", "hobel")))
        store.close()
    }
    
    @Test
    fun correctionCandidatesAreNotBoundedByFrequencyInTheTokenSOwnBucket() {
        val store = store("cand-primary.db")
        // D-209: the real "Kita"-shaped repro - a rare-but-correct word (frequency 17 in the real bundled
        // dictionary; "hobel" here at frequency 1) crowded out of its own first-letter bucket by hundreds
        // of more frequent same-letter/same-length-window words, before ever reaching the edit-distance
        // comparison. Same six-word "h" bucket as the neighbour-bucket comparison test above, but the token
        // itself ("hobel") now shares that bucket's own first character - so, unlike a neighbour bucket,
        // the frequency cap must not apply here at all.
        listOf("haupt" to 500L, "hafen" to 400L, "hallo" to 300L, "hemde" to 200L, "hitze" to 100L, "hobel" to 1L)
            .forEach { (word, freq) -> store.putWord(WordEntry(word, freq, emptySet())) }
        val firstChars = (setOf('h') + (1..399).map { offset -> Char(8000 + offset) }).toSet()
        
        val candidates = store.correctionCandidates("hobel", firstChars)
        
        assertTrue(candidates.containsAll(listOf("haupt", "hafen", "hallo", "hemde", "hitze", "hobel")))
        store.close()
    }
    
    @Test
    fun autocorrectSuggestsTheSingleEditNeighbour() {
        val store = store("auto.db")
        store.putWord(WordEntry("haus", 100L, emptySet()))
        val provider = DictionarySuggestionProvider(store)
        
        assertEquals("haus", provider.autocorrectFor("hous", null))
        assertEquals(null, provider.autocorrectFor("haus", null)) // A-01: a known word is not corrected
        store.close()
    }
    
    @Test
    fun missingUmlautCorrectsToTheUmlautWordNotAShapeAlike() {
        // D-65 / D-63: "konnen" must correct to "können" (missing umlaut, fold-distance 0), never to the
        // real-but-wrong "kannen" (Kannen, distance 2). This reproduces the device condition: enough plain
        // ASCII "k…" words to overflow the per-bucket candidate LIMIT. Because ö (U+00F6) sorts after every
        // ASCII letter, "können" is the very last "k" word in wkey order and was dropped past the LIMIT -
        // only the frequency-ordered scan (high-frequency "können" first) keeps it reachable.
        val store = store("umlaut.db")
        store.putWord(WordEntry("können", 5000L, emptySet()))
        store.putWord(WordEntry("kannen", 3L, emptySet()))
        store.putWord(WordEntry("müssen", 4000L, emptySet()))
        // Filler: 400 distinct length-6 ASCII "k" words (all sort before "kö…"), more than the per-bucket
        // LIMIT, each rarer than "können", so without ORDER BY freq the umlaut word falls off the end.
        val letters = "abcdefghijklmnopqrstuvwxy"
        var seeded = 0
        for (a in letters) {
            for (b in letters) {
                if (seeded >= 400) {
                    break
                }
                store.putWord(WordEntry("k" + a + b + "xyz", 1L, emptySet()))
                seeded++
            }
        }
        val provider = DictionarySuggestionProvider(store)
        
        assertEquals("können", provider.autocorrectFor("konnen", null))
        assertEquals("müssen", provider.autocorrectFor("mussen", null))
        store.close()
    }
    
    @Test
    fun ambiguousUmlautWordIsSuggestedButNotAutoCorrected() {
        // D-63: "konnten" is itself a valid word, so it must NOT be auto-corrected (A-01), but "könnten"
        // must still be offered in the suggestions.
        val store = store("umlaut-ambiguous.db")
        store.putWord(WordEntry("konnten", 200L, emptySet()))
        store.putWord(WordEntry("könnten", 900L, emptySet()))
        val provider = DictionarySuggestionProvider(store)
        
        assertEquals(null, provider.autocorrectFor("konnten", null))
        assertTrue(provider.suggestionsFor("konnten", null).any { it.word == "könnten" })
        store.close()
    }
    
    @Test
    fun nextWordsReturnsCanonicalSuccessorsByCount() {
        val store = store("next.db")
        store.putWord(WordEntry("Hund", 10L, emptySet()))
        store.putWord(WordEntry("Hut", 10L, emptySet()))
        store.putBigram("der", "Hund", 40L)
        store.putBigram("der", "Hut", 5L)
        
        assertEquals(listOf("Hund", "Hut"), store.nextWords("der", 10))
        assertEquals(listOf("Hund"), store.nextWords("der", 1))
        assertTrue(store.nextWords("unbekannt", 10).isEmpty())
        store.close()
    }
    
    @Test
    fun isBundledWordDistinguishesBundledFromLearnedOnly() {
        val store = store("bundled-flag.db")
        store.putWord(WordEntry("hund", 3L))
        store.learn("aks", null)
        
        assertTrue(store.isBundledWord("hund"))
        assertFalse(store.isBundledWord("aks"))
        store.close()
    }
    
    @Test
    fun forgetRemovesALearnedWordRegardlessOfAccumulatedFrequency() {
        val store = store("forget.db")
        store.learn("aks", null)
        store.learn("aks", null)
        store.learn("aks", null)
        
        store.forget("aks")
        
        assertFalse(store.isKnownWord("aks"))
        store.close()
    }
    
    @Test
    fun forgetOnABundledWordLeavesItsBundledFrequencyUntouched() {
        val store = store("forget-bundled.db")
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", null)
        
        store.forget("hund")
        
        assertTrue(store.isKnownWord("hund"))
        assertEquals(3L, store.frequencyOf("hund"))
        store.close()
    }
    
    @Test
    fun learnedWordsReturnsOnlyLearnedEntriesSortedByDescendingFrequency() {
        val store = store("learned-words.db")
        store.putWord(WordEntry("hund", 100L))
        store.learn("aks", null)
        store.learn("neu", null)
        store.learn("neu", null)
        
        assertEquals(listOf("neu", "aks"), store.learnedWords().map { it.word })
        store.close()
    }
    
    @Test
    fun pendingBlacklistMarkCheckAndClearRoundTrip() {
        val store = store("pending-blacklist.db")
        
        assertEquals(null, store.pendingBlacklistedSince("aks"))
        
        store.markPendingBlacklist("aks", 12345L)
        assertEquals(12345L, store.pendingBlacklistedSince("aks"))
        
        store.clearPendingBlacklist("aks")
        assertEquals(null, store.pendingBlacklistedSince("aks"))
        store.close()
    }
    
    @Test
    fun aWordThatIsBothBundledAndLearnedReportsTheSummedFrequency() {
        val store = store("merged-frequency.db")
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", null)
        store.learn("hund", null)
        
        assertEquals(5L, store.frequencyOf("hund"))
        store.close()
    }
    
    @Test
    fun bundledContentVersionDefaultsToZeroForAStoreThatNeverRecordedOne() {
        val store = store("version-default.db")
        
        assertEquals(0, store.bundledContentVersion())
        store.close()
    }
    
    @Test
    fun setBundledContentVersionAndBundledContentVersionRoundTrip() {
        val store = store("version-roundtrip.db")
        
        store.setBundledContentVersion(3)
        assertEquals(3, store.bundledContentVersion())
        
        store.setBundledContentVersion(4)
        assertEquals(4, store.bundledContentVersion())
        store.close()
    }
    
    @Test
    fun resetBundledWordsWipesOnlyTheBundledTablesLeavingLearnedWordsIntact() {
        val store = store("reset-bundled.db")
        store.putWord(WordEntry("hund", 3L))
        store.putBigram("der", "hund", 10L)
        store.learn("aks", "der")
        
        store.resetBundledWords()
        
        assertFalse(store.isKnownWord("hund"))
        assertEquals(0L, store.bigramFrequency("der", "hund"))
        assertTrue(store.isKnownWord("aks"))
        assertEquals(1L, store.bigramFrequency("der", "aks"))
        store.close()
    }
    
    @Test
    fun learnedCleanupVersionDefaultsToZeroForAStoreThatNeverRecordedOne() {
        val store = store("cleanup-version-default.db")
        
        assertEquals(0, store.learnedCleanupVersion())
        store.close()
    }
    
    @Test
    fun setLearnedCleanupVersionAndLearnedCleanupVersionRoundTrip() {
        val store = store("cleanup-version-roundtrip.db")
        
        store.setLearnedCleanupVersion(1)
        assertEquals(1, store.learnedCleanupVersion())
        
        store.setLearnedCleanupVersion(2)
        assertEquals(2, store.learnedCleanupVersion())
        store.close()
    }
    
    @Test
    fun purgeBundledDuplicatesFromLearnedRemovesOnlyWordsAlsoInTheBundledDictionary() {
        val store = store("purge-bundled-duplicates.db")
        store.putWord(WordEntry("hund", 3L))
        store.learn("hund", "der")
        store.learn("flauschbeere", "die")
        
        store.purgeBundledDuplicatesFromLearned()
        
        assertFalse(store.learnedWords().any { it.word == "hund" })
        assertTrue(store.learnedWords().any { it.word == "flauschbeere" })
        // "hund" is still known - it comes from the untouched bundled table, not the purged overlay.
        assertTrue(store.isKnownWord("hund"))
        store.close()
    }
}

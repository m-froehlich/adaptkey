// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.glide

import de.froehlichmedia.adaptkey.dictionary.InMemoryDictionaryStore
import de.froehlichmedia.adaptkey.dictionary.WordEntry
import de.froehlichmedia.adaptkey.suggestion.Umlaut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * D-516 synthetic-trace tests for the glide recognition core (plan §6 "Erkenner"): the ideal path of
 * a word is walked with controlled, seeded noise and uneven sampling, and the recognizer must return
 * that word first. The adversarial pairs (identical ideal-line cuts) pin the frequency and context
 * weights, exactly as the plan demands. Deterministic throughout (fixed seeds, no wall clock).
 */
class GlideRecognizerTest {

    private val geometry = GlideKeyGeometry.fromRows(
        rows = listOf("qwertzuiop", "asdfghjkl", "yxcvbnm"),
        keyWidth = KEY_PITCH,
        keyHeight = ROW_PITCH
    )

    private fun store(vararg words: Pair<String, Long>): InMemoryDictionaryStore {
        val store = InMemoryDictionaryStore()
        for ((word, frequency) in words) {
            store.putWord(WordEntry(word, frequency))
        }
        return store
    }

    /**
     * Walks the folded word's ideal polyline through the test geometry's key centres as a
     * realistically wobbly finger stroke: the waypoints and their subdivision midpoints carry the
     * seeded Gaussian offsets, the points between are interpolated straight - correlated, smooth
     * deviation like a real digitizer trace produces, not per-report static that would absurdly
     * inflate the stroke's arc length. (The resampler's speed invariance itself is covered
     * exhaustively by [GlideTraceTest]; these traces only need realistic geometry.)
     */
    private fun noisyTrace(word: String, sigma: Double, seed: Long): GlideTrace {
        val folded = Umlaut.fold(word.lowercase())
        val centers = folded.map { geometry.centerOf(it)!! }
        val random = Random(seed)
        val raw = ArrayList<GlidePoint>()
        var previous = jitter(centers.first(), random, sigma)
        raw.add(previous)
        for (i in 1 until centers.size) {
            val a = previous
            val b = centers[i]
            val length = a.distanceTo(b)
            val steps = maxOf(1, (length / 20.0).roundToInt())
            for (k in 1..steps) {
                val t = k.toDouble() / steps
                val point = GlidePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                previous = if (k < steps) jitter(point, random, sigma) else point
                raw.add(previous)
            }
            previous = jitter(b, random, sigma)
            raw[raw.size - 1] = previous
        }
        return GlideTrace.resample(raw)!!
    }

    private fun jitter(p: GlidePoint, random: Random, sigma: Double): GlidePoint {
        val offset = if (sigma == 0.0) {
            GlidePoint(0.0, 0.0)
        } else {
            GlidePoint(random.nextGaussian() * sigma, random.nextGaussian() * sigma)
        }
        return GlidePoint(p.x + offset.x, p.y + offset.y)
    }

    @Test
    fun `everyday words are recognised at position 1 from noisy synthetic traces`() {
        val store = store(
            "hallo" to 5200L, "danke" to 3400L, "guten" to 8000L, "morgen" to 4100L,
            "schule" to 2900L, "wasser" to 3500L, "haus" to 4200L, "arbeit" to 3100L,
            // same-first-letter distractors; every h...o / w...r / d...e / a...t / h...s one below
            // deliberately shares start AND end key with a target, so the alignment really decides
            "hafen" to 1800L, "halle" to 2100L, "hand" to 4600L, "hase" to 900L, "hat" to 5200L,
            "halo" to 200L, "homo" to 600L, "haushalt" to 900L, "haubitze" to 40L, "hans" to 400L,
            "dank" to 2200L, "denken" to 3900L, "dienst" to 1700L, "dorf" to 1400L,
            "dame" to 900L, "dose" to 700L,
            "gute" to 6100L, "gesagt" to 4400L, "gast" to 1500L, "ganz" to 5900L, "gern" to 2000L,
            "morgens" to 1200L, "miete" to 1000L, "musik" to 2600L, "mutter" to 3200L,
            "schuler" to 300L, "schuh" to 800L, "schnee" to 1600L, "schreiben" to 2700L,
            "wasserstoff" to 60L, "waschen" to 700L, "warte" to 400L, "weber" to 1300L,
            "arbeiten" to 2400L, "arzt" to 1300L, "apfel" to 1100L, "abend" to 2800L, "achat" to 200L
        )
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val words = listOf("hallo", "danke", "guten", "morgen", "schule", "wasser", "haus", "arbeit")
        for ((index, word) in words.withIndex()) {
            val trace = noisyTrace(word, sigma = 8.0, seed = 1_000L + index)
            val result = recognizer.candidates(trace, maxCount = 5)
            assertTrue(result.isNotEmpty(), "$word: the recognizer must find candidates at all")
            assertEquals(
                word, result[0].word,
                "$word: expected at position 1, got ${result.map { it.word }}"
            )
        }
    }

    @Test
    fun `heavier noise still keeps the right word at position 1 for a long word`() {
        val store = store(
            "vermutlich" to 2900L, "vermuten" to 2200L, "vermieten" to 900L,
            "vergessen" to 3100L, "verkaufen" to 2000L, "verstanden" to 2600L
        )
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("vermutlich", sigma = 12.0, seed = 7L)
        val result = recognizer.candidates(trace, maxCount = 6)
        assertEquals(
            "vermutlich", result[0].word,
            "got ${result.map { it.word }}"
        )
    }

    @Test
    fun `a trace over a matches an umlaut word - folding is ordinary, casing stays canonical`() {
        val store = store("Grüße" to 1200L, "grusse" to 5L, "gruse" to 5L, "greise" to 200L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        // Walk the folded sequence grusse (g r u s s e) - what a user tracing "Grüße" produces.
        val trace = noisyTrace("grusse", sigma = 6.0, seed = 11L)
        val result = recognizer.candidates(trace, maxCount = 5)
        assertEquals(
            "Grüße", result[0].word,
            "the folded match must win and stay in the entry's canonical casing, got ${result.map { it.word }}"
        )
    }

    @Test
    fun `the host-key shorthand trace gruse also finds Grüße`() {
        val store = store("Grüße" to 1200L, "greise" to 200L, "grosser" to 900L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        // D-204's lazily-tapped bare s convention: the trace skips the doubled s entirely.
        val trace = noisyTrace("gruse", sigma = 6.0, seed = 13L)
        val result = recognizer.candidates(trace, maxCount = 5)
        assertEquals("Grüße", result[0].word, "got ${result.map { it.word }}")
    }

    @Test
    fun `an identical ideal-line pair is separated by frequency, not geometry`() {
        // messe and mese share the same folded key path (the doubled s collapses under arc
        // resampling), so their geometric costs are near-identical by construction - the plan's
        // adversarial-pair class. With the rarer word given the higher frequency, the ranking must
        // follow frequency alone, pinning FREQUENCY_PRIOR_WEIGHT's direction and strength.
        val store = store("messe" to 3950L, "mese" to 4000L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("messe", sigma = 5.0, seed = 17L)
        val result = recognizer.candidates(trace, maxCount = 2)
        assertEquals(2, result.size)
        assertEquals("mese", result[0].word, "with equal shapes the more frequent word must win")
        assertTrue(
            abs(result[0].geometricCost - result[1].geometricCost) < 2.0,
            "the pair's geometric costs must be near-identical for this test to mean anything " +
                "(got ${result[0].geometricCost} vs ${result[1].geometricCost})"
        )
    }

    @Test
    fun `a clear shape win defeats a much more frequent word`() {
        // The frequency prior must not overwhelm the geometry: a well-matched rare word beats a
        // badly-matched very frequent one. "hallo" traced; "h..." frequent distractors whose folded
        // paths reach the same end key but bend far away mid-stroke.
        val store = store("hallo" to 800L, "hullo" to 90_000L, "hillo" to 80_000L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("hallo", sigma = 6.0, seed = 19L)
        val result = recognizer.candidates(trace, maxCount = 3)
        assertEquals(
            "hallo", result[0].word,
            "a 100x more frequent but worse-shaped word must lose, got ${result.map { it.word }}"
        )
    }

    @Test
    fun `a personal bigram context flips a near-tie`() {
        // Two same-shape words (the messe/mese folded pair) at near-equal frequency: without context
        // the slightly more frequent one wins; with a personal bigram on the other word, that one
        // wins. This pins CONTEXT_WEIGHT's direction against the frequency prior.
        val store = store("messe" to 3950L, "mese" to 4000L)
        store.learnContext("messe", previousWord = "die")
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("messe", sigma = 5.0, seed = 23L)

        val withoutContext = recognizer.candidates(trace, previousWord = null, maxCount = 2)
        assertEquals("mese", withoutContext[0].word, "the frequency order without context")

        val withContext = recognizer.candidates(trace, previousWord = "die", maxCount = 2)
        assertEquals(
            "messe", withContext[0].word,
            "the personal bigram must flip the near-tie, got ${withContext.map { it.word }}"
        )
    }

    @Test
    fun `context never overrides a clear shape loss`() {
        // The same messe/mese pair, but the trace is mese's own path variant... identical shapes,
        // so instead use two differently-shaped words: a clear shape winner against a context-boosted
        // clear shape loser - the context bonus must stay far below the shape difference.
        val store = store("morgen" to 900L, "mohn" to 90_000L)
        store.learnContext("mohn", previousWord = "guten")
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("morgen", sigma = 6.0, seed = 41L)
        val result = recognizer.candidates(trace, previousWord = "guten", maxCount = 2)
        assertEquals(
            "morgen", result[0].word,
            "a 100x more frequent, bigram-boosted but wrong-shaped word must lose, got ${result.map { it.word }}"
        )
    }

    @Test
    fun `a blacklisted word is never a candidate`() {
        val store = store("hallo" to 5200L, "hullo" to 300L)
        store.blacklist("hallo", de.froehlichmedia.adaptkey.dictionary.BlacklistCategory.USER)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("hallo", sigma = 6.0, seed = 29L)
        val result = recognizer.candidates(trace, maxCount = 5)
        assertTrue(result.none { it.word == "hallo" }, "A-04 must filter the blacklisted word")
        assertEquals("hullo", result[0].word)
    }

    @Test
    fun `words with letters outside the layout are skipped without crashing`() {
        val store = store("danke" to 3400L, "dβma" to 500L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("danke", sigma = 6.0, seed = 31L)
        val result = recognizer.candidates(trace, maxCount = 5)
        assertTrue(result.all { it.word == "danke" }, "got ${result.map { it.word }}")
    }

    @Test
    fun `a stroke far from any key yields nothing`() {
        val store = store("hallo" to 5200L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        // A stroke over the empty area right of the last key column, one full pitch away from the
        // nearest centre: the endpoint proximity gate must reject it outright.
        val x0 = 10 * KEY_PITCH + 2.0 * KEY_PITCH
        val raw = ArrayList<GlidePoint>()
        var x = x0
        var y = 20.0
        for (i in 0 until 20) {
            raw.add(GlidePoint(x, y))
            x += 8.0
            y += 1.0
        }
        val trace = GlideTrace.resample(raw)!!
        assertTrue(recognizer.candidates(trace).isEmpty())
    }

    @Test
    fun `a degenerate stroke - tight wobble around one key - yields nothing`() {
        val store = store("hallo" to 5200L, "heilig" to 800L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        // A circle of ~20 px radius around the h key's centre: enough arc length to resample
        // past MIN_POINTS, but it starts and ends on the same key, so no candidate's folded
        // spelling can match both endpoints.
        val h = geometry.centerOf('h')!!
        val raw = ArrayList<GlidePoint>()
        for (i in 0 until 16) {
            val angle = 2 * Math.PI * i / 16
            raw.add(GlidePoint(h.x + 20 * Math.cos(angle), h.y + 20 * Math.sin(angle)))
        }
        val trace = GlideTrace.resample(raw)!!
        assertTrue(recognizer.candidates(trace).isEmpty())
    }

    @Test
    fun `cooperative cancellation stops the scoring loop`() {
        val store = store("hallo" to 5200L, "danke" to 3400L, "guten" to 8000L)
        val recognizer = GlideRecognizer(store, geometry, Umlaut)
        val trace = noisyTrace("hallo", sigma = 6.0, seed = 37L)
        val result = recognizer.candidates(trace, isCancelled = { true }, maxCount = 5)
        assertTrue(result.isEmpty(), "a cancelled pass must return nothing")
    }

    companion object {
        private const val KEY_PITCH = 64.0
        private const val ROW_PITCH = 72.0
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.TapPoint

/**
 * D-39: raw-coordinate per-character correction.
 *
 * Correction should not work from the committed characters alone but from the retained raw tap coordinates
 * per character (T-02 / T-05). For an unknown word, this walks it character by character and, using each
 * tap's raw position and the personal offset model (T-03), considers the geometrically next-most-plausible
 * key - not the static QWERTZ adjacency map used elsewhere (D-28 / D-38 / D-41), but the actual runner-up
 * under the user's own learned strike distribution at that exact tap. This recovers cases where the original
 * per-tap key resolution ([OffsetModel.resolve]) picked the wrong key out of two genuinely close candidates -
 * something the static adjacency map cannot see, since it never looks at where the tap actually landed.
 *
 * Only a single-position substitution is attempted per candidate spelling, since most single-key slips are
 * one position off; heavily garbled tokens (many typos at once) are left to the tier-3 mini-LLM, which tends
 * to recognise the intended word from context instead.
 */
object RawCoordinateCorrection {
    
    /**
     * Candidate respellings of [token], one per character position, each substituting that position with the
     * geometrically next-most-plausible key under [offsetModel] - ordered best (most confident substitution)
     * first, by how close the runner-up key came to beating the key that was actually chosen. The caller is
     * expected to test each spelling against the dictionary and use the first known word.
     *
     * @param token the composing token as typed (original case preserved in the output)
     * @param taps the raw `ACTION_DOWN` tap for each character, same order and length as [token]; a length
     *   mismatch (e.g. a desync after an edit) yields no candidates rather than risk a wrong substitution
     * @param keyCandidates every char key's geometry (id `"c:<char>"`), from the layout active while typing
     * @param offsetModel the personal offset model (T-03) used to score candidates; an untrained model still
     *   works (falls back to plain geometry, see [OffsetModel.rankedCandidates])
     * @return respellings ordered best-first; empty when the input is too short or inconsistent
     */
    fun respellings(
        token: String,
        taps: List<TapPoint>,
        keyCandidates: List<OffsetModel.Candidate>,
        offsetModel: OffsetModel
    ): List<String> {
        if (token.isEmpty() || token.length != taps.size || keyCandidates.isEmpty()) {
            return emptyList()
        }
        val scored = ArrayList<Pair<String, Double>>()
        for (i in token.indices) {
            val original = token[i]
            val actualId = keyId(original)
            val ranked = offsetModel.rankedCandidates(keyCandidates, taps[i].x, taps[i].y)
            val actualScore = ranked.firstOrNull { it.first.id == actualId }?.second ?: continue
            val (runnerUp, runnerUpScore) = ranked.firstOrNull { it.first.id != actualId } ?: continue
            val altChar = charOf(runnerUp.id) ?: continue
            val substituted = if (original.isUpperCase()) altChar.uppercaseChar() else altChar
            if (substituted == original) {
                continue
            }
            val respelling = token.substring(0, i) + substituted + token.substring(i + 1)
            // The gap is <= 0 in the normal case (the actual key was resolved as the best match); the closer
            // to zero, the more genuinely ambiguous the tap was between the two keys.
            scored.add(respelling to (runnerUpScore - actualScore))
        }
        return scored.sortedByDescending { it.second }.map { it.first }
    }
    
    private fun keyId(c: Char): String {
        return "c:${c.lowercaseChar()}"
    }
    
    private fun charOf(id: String): Char? {
        return id.removePrefix("c:").singleOrNull()
    }
}

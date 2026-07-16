// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-116 compound-split recognition. */
class CompoundSplitTest {
    
    private fun knownNouns(vararg words: String): (String) -> Boolean {
        val set = words.toSet()
        return { word -> set.contains(word) }
    }
    
    @Test
    fun `splits a compound at a known noun plus an s-Fugenelement`() {
        // "beitragsjahreb" -> "beitrag" + "s" + "jahreb", rest resolved to "jahren" by the caller.
        val result = CompoundSplit.split(
            "beitragsjahreb",
            knownNouns("beitrag"),
            resolveRest = { rest -> if (rest == "jahreb") "jahren" else null }
        )
        assertEquals(CompoundSplit.Result("beitrag", "s", "jahren"), result)
    }
    
    @Test
    fun `splits a direct concatenation with no Fugenelement at all`() {
        val result = CompoundSplit.split(
            "hausschuh",
            knownNouns("haus"),
            resolveRest = { rest -> if (rest == "schuh") "schuh" else null }
        )
        assertEquals(CompoundSplit.Result("haus", "", "schuh"), result)
    }
    
    @Test
    fun `prefers the longest known-noun first part`() {
        // Both "beitrag" and "beitragsjahr" are known nouns here; the longer one must win, so the shorter
        // one's own split is never even attempted.
        val result = CompoundSplit.split(
            "beitragsjahrstand",
            knownNouns("beitrag", "beitragsjahr"),
            resolveRest = { rest -> if (rest == "stand") "stand" else null }
        )
        assertEquals(CompoundSplit.Result("beitragsjahr", "", "stand"), result)
    }
    
    @Test
    fun `no known-noun first part yields no split`() {
        assertNull(CompoundSplit.split("xyzabcdef", knownNouns("haus"), resolveRest = { it }))
    }
    
    @Test
    fun `a first part that is known but whose rest never resolves yields no split`() {
        assertNull(CompoundSplit.split("hausxyz", knownNouns("haus"), resolveRest = { null }))
    }
    
    @Test
    fun `first part shorter than the minimum length is never tried`() {
        // "Ei" (2 chars) would otherwise be a plausible first part of "eischnee", but is too short.
        assertNull(CompoundSplit.split("eischnee", knownNouns("ei"), resolveRest = { it }))
    }
    
    @Test
    fun `a Fugenelement candidate leaving too short a rest is skipped, not passed to resolveRest`() {
        // "beitrag" + "s" would leave only "x" (1 char) - too short, so that candidate must be skipped
        // without ever calling resolveRest, even though a later, longer-rest candidate (here the
        // no-Fugenelement "sx") still exists and is tried.
        val seenRests = mutableListOf<String>()
        CompoundSplit.split("beitragsx", knownNouns("beitrag"), resolveRest = { rest -> seenRests.add(rest); null })
        assertTrue(seenRests.none { it.length < 2 })
    }
    
    @Test
    fun `matching is case-insensitive and the result is lower-case`() {
        val result = CompoundSplit.split(
            "Hausschuh",
            knownNouns("haus"),
            resolveRest = { rest -> if (rest == "schuh") "SCHUH" else null }
        )
        assertEquals(CompoundSplit.Result("haus", "", "schuh"), result)
    }
}

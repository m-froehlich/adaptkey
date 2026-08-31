// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Unit tests for the D-386 duplicate-download-file matching. */
class DuplicateDownloadMatcherTest {
    
    @Test
    fun `matches the exact expected file name`() {
        val candidates = listOf(DownloadCandidate("1", "adaptkey-lang-de.zip", 1000L))
        assertEquals(candidates.first(), DuplicateDownloadMatcher.findNewest(candidates, "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `matches a browser-renamed duplicate with a numbered suffix`() {
        val candidates = listOf(DownloadCandidate("1", "adaptkey-lang-de (1).zip", 1000L))
        assertEquals(candidates.first(), DuplicateDownloadMatcher.findNewest(candidates, "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `prefers the newest match when several duplicates exist`() {
        val older = DownloadCandidate("1", "adaptkey-lang-de.zip", 1000L)
        val newer = DownloadCandidate("2", "adaptkey-lang-de (1).zip", 5000L)
        val newest = DownloadCandidate("3", "adaptkey-lang-de (2).zip", 9000L)
        assertEquals(newest, DuplicateDownloadMatcher.findNewest(listOf(older, newer, newest), "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `does not match an unrelated file with a similar prefix`() {
        val candidates = listOf(DownloadCandidate("1", "adaptkey-lang-de-old.zip", 1000L))
        assertNull(DuplicateDownloadMatcher.findNewest(candidates, "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `does not match a file with a different extension`() {
        val candidates = listOf(DownloadCandidate("1", "adaptkey-lang-de.zip.part", 1000L))
        assertNull(DuplicateDownloadMatcher.findNewest(candidates, "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `does not match a differently-named file entirely`() {
        val candidates = listOf(DownloadCandidate("1", "model_q4f16.onnx", 1000L))
        assertNull(DuplicateDownloadMatcher.findNewest(candidates, "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `returns null when there are no candidates at all`() {
        assertNull(DuplicateDownloadMatcher.findNewest(emptyList(), "adaptkey-lang-de.zip"))
    }
    
    @Test
    fun `matches a file with no extension the same way`() {
        val candidates = listOf(
            DownloadCandidate("1", "README", 1000L),
            DownloadCandidate("2", "README (1)", 2000L)
        )
        assertEquals(candidates[1], DuplicateDownloadMatcher.findNewest(candidates, "README"))
    }
    
    @Test
    fun `does not treat a double-digit or multi-digit suffix as a mismatch`() {
        val candidates = listOf(DownloadCandidate("1", "model_q4f16 (12).onnx", 1000L))
        assertEquals(candidates.first(), DuplicateDownloadMatcher.findNewest(candidates, "model_q4f16.onnx"))
    }
    
    @Test
    fun `is case-sensitive on the file name, matching real filesystem semantics`() {
        val candidates = listOf(DownloadCandidate("1", "ADAPTKEY-LANG-DE.ZIP", 1000L))
        assertNull(DuplicateDownloadMatcher.findNewest(candidates, "adaptkey-lang-de.zip"))
    }
}

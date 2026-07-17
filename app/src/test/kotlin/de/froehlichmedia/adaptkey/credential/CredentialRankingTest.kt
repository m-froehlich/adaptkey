// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-142 credential-suggestion ranking policy. */
class CredentialRankingTest {
    
    @Test
    fun `suggestionsFor matches by case-insensitive prefix, most frequent first`() {
        val entries = listOf(
            CredentialEntry("peter.mueller", LoginFieldKind.USERNAME, 3L),
            CredentialEntry("peter1980", LoginFieldKind.USERNAME, 7L),
            CredentialEntry("anna.schmidt", LoginFieldKind.USERNAME, 5L)
        )
        
        assertEquals(listOf("peter1980", "peter.mueller"), CredentialRanking.suggestionsFor(entries, "Peter", 10))
    }
    
    @Test
    fun `suggestionsFor with an empty prefix returns everything ranked by frequency`() {
        val entries = listOf(
            CredentialEntry("a", LoginFieldKind.USERNAME, 1L),
            CredentialEntry("b", LoginFieldKind.USERNAME, 9L)
        )
        
        assertEquals(listOf("b", "a"), CredentialRanking.suggestionsFor(entries, "", 10))
    }
    
    @Test
    fun `suggestionsFor honours the limit`() {
        val entries = listOf(
            CredentialEntry("a", LoginFieldKind.USERNAME, 3L),
            CredentialEntry("b", LoginFieldKind.USERNAME, 2L),
            CredentialEntry("c", LoginFieldKind.USERNAME, 1L)
        )
        
        assertEquals(2, CredentialRanking.suggestionsFor(entries, "", 2).size)
    }
    
    @Test
    fun `suggestionsFor with no match returns empty`() {
        val entries = listOf(CredentialEntry("hans", LoginFieldKind.USERNAME, 1L))
        
        assertTrue(CredentialRanking.suggestionsFor(entries, "x", 10).isEmpty())
    }
    
    @Test
    fun `emailDomainsFor sums frequency across several aliases on the same domain`() {
        val entries = listOf(
            CredentialEntry("privat@example.com", LoginFieldKind.EMAIL, 6L),
            CredentialEntry("arbeit@example.com", LoginFieldKind.EMAIL, 5L),
            CredentialEntry("noreply@other.org", LoginFieldKind.EMAIL, 10L)
        )
        
        // example.com's two aliases (6 + 5 = 11) outrank other.org's single, individually more frequent one.
        assertEquals(listOf("example.com", "other.org"), CredentialRanking.emailDomainsFor(entries, "", 10))
    }
    
    @Test
    fun `emailDomainsFor filters by domain prefix`() {
        val entries = listOf(
            CredentialEntry("a@example.com", LoginFieldKind.EMAIL, 1L),
            CredentialEntry("b@example.org", LoginFieldKind.EMAIL, 1L),
            CredentialEntry("c@other.net", LoginFieldKind.EMAIL, 1L)
        )
        
        assertEquals(listOf("example.com", "example.org"), CredentialRanking.emailDomainsFor(entries, "ex", 10).sorted())
    }
    
    @Test
    fun `emailDomainsFor ignores username entries`() {
        val entries = listOf(
            CredentialEntry("plainuser", LoginFieldKind.USERNAME, 100L),
            CredentialEntry("a@example.com", LoginFieldKind.EMAIL, 1L)
        )
        
        assertEquals(listOf("example.com"), CredentialRanking.emailDomainsFor(entries, "", 10))
    }
    
    @Test
    fun `emailDomainsFor honours the limit`() {
        val entries = listOf(
            CredentialEntry("a@one.com", LoginFieldKind.EMAIL, 3L),
            CredentialEntry("b@two.com", LoginFieldKind.EMAIL, 2L),
            CredentialEntry("c@three.com", LoginFieldKind.EMAIL, 1L)
        )
        
        assertEquals(2, CredentialRanking.emailDomainsFor(entries, "", 2).size)
    }
}

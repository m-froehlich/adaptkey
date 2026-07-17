// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

/**
 * D-142: pure ranking/policy over a list of learned [CredentialEntry] values - free of any Android
 * dependency, matching this project's usual split between pure policy and thin Android storage
 * ([CredentialStore]).
 */
object CredentialRanking {
    
    /**
     * The saved credential values (any [LoginFieldKind.USERNAME] or [LoginFieldKind.EMAIL] entry) whose
     * value starts with [prefix] (case-insensitive; an empty prefix matches everything, so a freshly
     * opened login field can offer the user's most-used identifiers before they type anything at all),
     * most-frequently-used first.
     *
     * @param entries every learned credential entry
     * @param prefix the text typed so far in the field
     * @param limit the maximum number of values to return
     * @return matching values in their own canonical case, ranked by descending frequency
     */
    fun suggestionsFor(entries: List<CredentialEntry>, prefix: String, limit: Int): List<String> {
        if (limit <= 0) {
            return emptyList()
        }
        return entries
            .filter { it.value.startsWith(prefix, ignoreCase = true) }
            .sortedWith(compareByDescending<CredentialEntry> { it.frequency }.thenBy { it.value.lowercase() })
            .map { it.value }
            .take(limit)
    }
    
    /**
     * The domains of every learned [LoginFieldKind.EMAIL] entry whose domain starts with [domainPrefix]
     * (case-insensitive), most-frequently-used first - the frequency of a domain is the sum of the
     * frequencies of every saved email address on it, so a domain used by several different, individually
     * less-frequent aliases still ranks correctly high. Directly answers the "which of my own domains do I
     * mean" question the moment `@` is typed, without the user needing to have typed that exact address
     * before.
     *
     * @param entries every learned credential entry (non-email entries are ignored)
     * @param domainPrefix the text typed so far after `@`
     * @param limit the maximum number of domains to return
     * @return matching domains, ranked by descending summed frequency
     */
    fun emailDomainsFor(entries: List<CredentialEntry>, domainPrefix: String, limit: Int): List<String> {
        if (limit <= 0) {
            return emptyList()
        }
        return entries
            .filter { it.kind == LoginFieldKind.EMAIL && it.value.contains('@') }
            .groupingBy { it.value.substringAfterLast('@') }
            .fold(0L) { total, entry -> total + entry.frequency }
            .filterKeys { it.startsWith(domainPrefix, ignoreCase = true) }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key.lowercase() })
            .map { it.key }
            .take(limit)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * D-143: resolves the URL-mode period key's long-press TLD popup from a [Locale], mirroring
 * [CalculatorLocale]'s JDK-locale-driven approach rather than a large hand-maintained worldwide table.
 *
 * English-language locales lead with the two generic top-level domains (`.com`, `.org`) - the
 * overwhelmingly common case for an English speaker - followed by the locale's own country-code TLD, when
 * one is idiomatically used for casual addresses; every other locale leads with its own country-code TLD
 * instead, then `.com` / `.org`. [CCTLD_OVERRIDES] corrects the handful of countries where the plain
 * lower-cased ISO code either isn't the customary form (Great Britain uses `.co.uk` for ordinary
 * commercial/personal sites, not the bare `.uk`) or has no everyday casual TLD at all (the United States -
 * `.us` is not how Americans think of their own sites, unlike `.de` for Germans) - not an attempt at an
 * exhaustive worldwide table, just the two cases this reasoning actually calls for; more can be added later
 * if a real device round surfaces one.
 */
object UrlLocale {
    
    // Maps an ISO 3166-1 country code to its idiomatic ccTLD text (without the leading dot), or to null when
    // the country has no everyday casual ccTLD worth offering at all. A country absent from this map falls
    // back to its own lower-cased code, which is correct for the overwhelming majority of countries (DE, FR,
    // GR, IT, ES, NL, ...).
    private val CCTLD_OVERRIDES: Map<String, String?> = mapOf(
        "GB" to "co.uk",
        "US" to null
    )
    
    /**
     * The period key's URL-mode long-press alternatives for [locale]: the plain full stop itself (so a
     * straight release still types `.`), followed by up to three TLD suggestions.
     *
     * @param locale the system locale to resolve from
     * @return the ordered alternatives list, always starting with `.`
     */
    fun periodAlternatives(locale: Locale): List<String> {
        val ccTld = ccTldFor(locale)
        val english = locale.language.equals("en", ignoreCase = true)
        val tlds = if (english) {
            listOfNotNull(".com", ".org", ccTld?.let { ".$it" })
        } else {
            listOfNotNull(ccTld?.let { ".$it" }, ".com", ".org")
        }
        return listOf(".") + tlds
    }
    
    private fun ccTldFor(locale: Locale): String? {
        val country = locale.country
        if (country.isBlank()) {
            return null
        }
        return if (CCTLD_OVERRIDES.containsKey(country)) CCTLD_OVERRIDES.getValue(country) else country.lowercase()
    }
}

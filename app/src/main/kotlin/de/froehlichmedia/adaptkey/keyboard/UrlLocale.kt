// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * D-143: resolves the URL-mode period key's long-press TLD popup from a [Locale], mirroring
 * [CalculatorLocale]'s JDK-locale-driven approach rather than a large hand-maintained worldwide table.
 *
 * D-145 correction: the locale's own country-code TLD, when it has one worth offering, **always** leads the
 * list, regardless of language - D-01's popup centres its *pre-selected* cell directly over the stem (the
 * one entry reachable by a straight release with no finger movement at all, see
 * [AdaptKeyboardView.preSelectedIndexFor]/[HorizontalLongPressPopup.rowLeft]), so the single most personally
 * relevant suggestion for this exact locale must be the one placed there - not merely whichever of two
 * equally-generic top-level domains happened to be listed first. A locale with no idiomatic ccTLD at all
 * (see [CCTLD_OVERRIDES]) falls back to leading with `.com`. [CCTLD_OVERRIDES] corrects the handful of
 * countries where the plain lower-cased ISO code either isn't the customary form (Great Britain uses
 * `.co.uk` for ordinary commercial/personal sites, not the bare `.uk`) or has no everyday casual TLD at all
 * (the United States - `.us` is not how Americans think of their own sites, unlike `.de` for Germans) - not
 * an attempt at an exhaustive worldwide table, just the two cases this reasoning actually calls for; more
 * can be added later if a real device round surfaces one.
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
     * The period key's URL-mode long-press alternatives for [locale]: up to three TLD suggestions, **not**
     * including the plain full stop itself (D-144 correction - a plain tap already types `.`, so repeating
     * it in its own popup would be a redundant entry offering nothing new). The locale's own ccTLD, when it
     * has one, always leads (D-145 correction - see this class's own KDoc for why the *first* entry matters
     * far more than mere reading order); otherwise `.com` leads.
     *
     * @param locale the system locale to resolve from
     * @return the ordered TLD suggestions, never including the bare `.`
     */
    fun periodAlternatives(locale: Locale): List<String> {
        val ccTld = ccTldFor(locale)
        return listOfNotNull(ccTld?.let { ".$it" }, ".com", ".org")
    }
    
    private fun ccTldFor(locale: Locale): String? {
        val country = locale.country
        if (country.isBlank()) {
            return null
        }
        return if (CCTLD_OVERRIDES.containsKey(country)) CCTLD_OVERRIDES.getValue(country) else country.lowercase()
    }
}

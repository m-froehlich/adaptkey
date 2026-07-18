// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * D-143: resolves the URL-mode period key's long-press TLD popup from a [Locale], mirroring
 * [CalculatorLocale]'s JDK-locale-driven approach rather than a large hand-maintained worldwide table.
 *
 * D-145 correction (second pass): the locale's own country-code TLD, when it has one worth offering, is
 * placed at the *middle* of the list and is the one [AdaptKeyboardView.preSelectedIndexFor] pre-selects for
 * this key - inheriting this key's own original, pre-URL-mode convention (the plain "! . ?" sentence-
 * terminator popup already put "." at the visual centre, flanked by "!"/"?"), rather than the newer
 * lead-with-self convention some other multi-alternative keys use (`https://`, `=`). D-01's popup always
 * centres whichever cell is pre-selected directly over the stem (the one entry reachable by a straight
 * release with no finger movement at all, see [HorizontalLongPressPopup.rowLeft]), so the single most
 * personally relevant suggestion for this exact locale must be that one - not merely whichever of two
 * equally-generic top-level domains happened to be listed first. A locale with no idiomatic ccTLD at all
 * (see [CCTLD_OVERRIDES]) falls back to a plain two-entry list led by `.com`. [CCTLD_OVERRIDES] corrects the
 * handful of countries where the plain lower-cased ISO code either isn't the customary form (Great Britain
 * uses `.co.uk` for ordinary commercial/personal sites, not the bare `.uk`) or has no everyday casual TLD at
 * all (the United States - `.us` is not how Americans think of their own sites, unlike `.de` for Germans) -
 * not an attempt at an exhaustive worldwide table, just the two cases this reasoning actually calls for; more
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
     * has one, sits at the middle of the list, flanked by the two generic TLDs (D-145 correction - see this
     * class's own KDoc for why the *pre-selected* position matters far more than mere reading order);
     * otherwise the list is just the two generic TLDs, `.com` leading.
     *
     * @param locale the system locale to resolve from
     * @return the ordered TLD suggestions, never including the bare `.`
     */
    fun periodAlternatives(locale: Locale): List<String> {
        val ccTld = ccTldFor(locale)?.let { ".$it" }
        return if (ccTld != null) listOf(".com", ccTld, ".org") else listOf(".com", ".org")
    }
    
    /**
     * The period key's email-mode long-press alternatives for [locale]: exactly [periodAlternatives], with
     * `.net` inserted directly before `.org` - explicitly requested as an email-only addition, not shared
     * with [periodAlternatives]/URL mode. The locale's own ccTLD, when present, still precedes `.net` in the
     * list, so [AdaptKeyboardView.preSelectedIndexFor]'s "neither `.com` nor `.org`" ccTLD lookup keeps
     * finding it first regardless; that lookup is additionally hardened to also exclude `.net` itself, so a
     * locale with no ccTLD (where `.net` would otherwise be the first non-`.com`/`.org` entry) still falls
     * back to pre-selecting `.com`, exactly as before.
     *
     * @param locale the system locale to resolve from
     * @return the ordered TLD suggestions, never including the bare `.`
     */
    fun emailPeriodAlternatives(locale: Locale): List<String> {
        val ccTld = ccTldFor(locale)?.let { ".$it" }
        return if (ccTld != null) listOf(".com", ccTld, ".net", ".org") else listOf(".com", ".net", ".org")
    }
    
    private fun ccTldFor(locale: Locale): String? {
        val country = locale.country
        if (country.isBlank()) {
            return null
        }
        return if (CCTLD_OVERRIDES.containsKey(country)) CCTLD_OVERRIDES.getValue(country) else country.lowercase()
    }
}

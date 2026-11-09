// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Currency
import java.util.Locale

/**
 * D-520: resolves the letters page's currency slot from a [Locale] - the system region decides the
 * currency, the same device-level decision the calculator page's currency key has made since D-92
 * ("bei Tastatur-sprachlicher Gleichheit auf das System als Entscheidung zurückfallen"). The slot
 * lives on the d key's right corner (the Alt-Belegungs-Reform's former `$` release default) and must
 * always show the currency of the country the device is configured for, so the resolution asks the
 * locale's REGION component (`Locale.getCountry()`) first - a language alone (e.g. `de` without a
 * country) decides nothing here - and then maps it to a short, corner-drawable symbol.
 *
 * The mapping is deliberately a curated table, not a raw `Currency.getSymbol()` pass-through: the
 * JDK's own locale data answers the top regions correctly (`$`, `€`, `£`, `¥`, `₹`...), but it fails
 * the corner glyph's length budget exactly where a region has no dedicated Unicode sign (Switzerland
 * and Liechtenstein resolve to the three-letter ISO code "CHF"). The table pins the top-10 regions
 * deterministically across JDK locale-data vintages, the JDK answer passes through where it is
 * already a usable one or two glyph symbol (the long tail: `kr`, `zł`, `R$`, ...), a currency-code
 * fallback catches remaining multi-letter codes, and a region-less locale falls back to the euro -
 * [CalculatorLocale]'s own D-92 fallback precedent.
 *
 * Pure and Android-free, unit-tested like [CalculatorLocale]/[UrlLocale] before it.
 */
object CurrencyLocale {
    
    /**
     * The world's most relevant currencies, in global relevance order - the D-520 popup's content,
     * minus whichever of them the key itself carries as its primary (the region's own currency is
     * never repeated in the popup). Curated by global relevance (reserve-currency status, trading
     * volume, GDP of the issuing economies), de-duplicated by GLYPH because two cells spelling the
     * same character can never be told apart by sliding: `¥` covers the Chinese Yuan and the
     * Japanese Yen alike, `$` covers the US dollar and every other dollar-pegged sign the long-tail
     * resolution can produce. `Fr` and `R$` are the two genuine two-glyph symbols of the set - both
     * within the letters-page corner budget (the reform already draws `kr`/`zł` pack hints), and
     * `Fr` is listed in [AlternativeScript]'s symbol-word exclusion so the Shift/Caps case machinery
     * never uppercases it into word text.
     */
    val RELEVANCE_ORDER = listOf("$", "€", "¥", "£", "₹", "₩", "₽", "₺", "Fr", "R$")
    
    /** The corner glyph's own length budget - the reform's established two-glyph limit (`LetterHints.MAX_SYMBOL_LENGTH`). */
    private const val MAX_SYMBOL_LENGTH = 2
    
    /** The D-92 precedent's fallback, kept for the region-less-locale case here too. */
    private const val DEFAULT_SYMBOL = "€"
    
    /**
     * The top-10 regions' deterministic answers, plus the eurozone's own members (whose JDK answer
     * is already `€` - the table pins it against locale-data drift) and the Swiss/Liechtenstein gap
     * where the JDK gives the unusable three-letter "CHF". Country codes, not language codes.
     */
    private val COUNTRY_SYMBOLS = mapOf(
        "US" to "$", "GB" to "£", "CN" to "¥", "JP" to "¥", "IN" to "₹", "KR" to "₩",
        "RU" to "₽", "TR" to "₺", "BR" to "R$", "CH" to "Fr", "LI" to "Fr",
        "AT" to "€", "BE" to "€", "HR" to "€", "CY" to "€", "EE" to "€", "FI" to "€",
        "FR" to "€", "DE" to "€", "GR" to "€", "IE" to "€", "IT" to "€", "LV" to "€",
        "LT" to "€", "LU" to "€", "MT" to "€", "NL" to "€", "PT" to "€", "SK" to "€",
        "SI" to "€", "ES" to "€",
        // The euro's de-facto users without eurozone membership.
        "AD" to "€", "ME" to "€", "MC" to "€", "SM" to "€", "VA" to "€", "XK" to "€"
    )
    
    /**
     * Currency-code fallback for a region whose JDK symbol exceeds the corner budget - the same
     * short forms the language packs already standardised (`t=kr`, `zł`).
     */
    private val CODE_SYMBOLS = mapOf(
        "CHF" to "Fr", "BRL" to "R$",
        "SEK" to "kr", "NOK" to "kr", "DKK" to "kr", "ISK" to "kr",
        "PLN" to "zł"
    )
    
    /**
     * The region's own currency symbol for the slot's primary - the glyph the key displays as its
     * corner and a plain long-press release commits.
     *
     * @param locale the device's system locale; its REGION component decides, never the language
     * @return a one- or two-glyph symbol, never null or blank
     */
    fun primarySymbol(locale: Locale): String {
        COUNTRY_SYMBOLS[locale.country]?.let { return it }
        val currency = runCatching { Currency.getInstance(locale) }.getOrNull() ?: return DEFAULT_SYMBOL
        val symbol = runCatching { currency.getSymbol(locale) }.getOrNull()
        if (symbol != null && symbol.isNotEmpty() && symbol.length <= MAX_SYMBOL_LENGTH) {
            return symbol
        }
        return CODE_SYMBOLS[currency.currencyCode] ?: DEFAULT_SYMBOL
    }
    
    /**
     * The slot's popup content for a resolved primary: the world's most relevant currencies in
     * [RELEVANCE_ORDER], minus the primary itself (the key's own currency is never repeated in the
     * popup). A primary outside the top-10 (a long-tail `kr`/`zł` device) leaves the full ten in
     * the popup - the key plus popup then cover eleven currencies, which is the same "your currency
     * lives on the key, the world's ten live above it" contract.
     *
     * @param primary the resolved primary symbol
     * @return the popup cells in relevance order, alternating around the host key at display time
     */
    fun popupSymbols(primary: String): List<String> {
        return RELEVANCE_ORDER.filter { it != primary }
    }
}

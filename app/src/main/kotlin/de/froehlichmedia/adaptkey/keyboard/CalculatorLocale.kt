// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * D-92: resolves the calculator page's currency key and decimal/thousands separators from a [Locale].
 *
 * AdaptKey's own selectable keyboard alphabets (German, Greek) always agree with each other on both
 * points - a comma decimal separator and the euro - so that choice never actually decides anything here.
 * Per the user's own framing: "bei Tastatur-sprachlicher Gleichheit (egal) auf das System als
 * Entscheidung zurückfallen" (when the keyboard language is a tie, fall back to the system for the
 * decision). This resolves purely from the device's system locale (e.g. a US system locale yields `$`
 * and a full stop, even while typing in German) using the JDK's own locale data
 * ([DecimalFormatSymbols], [Currency]) rather than a hand-maintained table, so it is correct for
 * locales well beyond the app's own DE/EN/EL dictionaries.
 */
object CalculatorLocale {
    
    /**
     * The resolved calculator-page format for a locale.
     *
     * @property currencyBase the currency key's own glyph (e.g. `€`, `$`, `£`)
     * @property currencyAlternatives the currency key's long-press popup, always including [currencyBase]
     * @property decimalSeparator the decimal separator (e.g. `,` in German/Greek, `.` in English)
     * @property thousandsSeparatorHint the decimal separator key's long-press alternative
     */
    data class Format(
        val currencyBase: Char,
        val currencyAlternatives: List<String>,
        val decimalSeparator: Char,
        val thousandsSeparatorHint: String
    )
    
    /** Fallback when a locale's currency can't be resolved to a single glyph (e.g. a 3-letter ISO code). */
    private const val DEFAULT_CURRENCY = '€'
    
    // D-100: the currency key now sits in the calculator page's right-hand column, at the screen's right
    // edge. The D-44 popup keeps its pre-selected cell (the key's own char) centred under the stem, so a
    // pre-selected cell near the *start* of the list forces the popup to grow almost entirely rightward -
    // straight into the screen edge, where AdaptKeyboardView.openPopup()'s clamp then has to shove the
    // whole row far to the left, away from the stem key. Ordering the common currencies so the base glyph
    // tends to land near the *end* of the list instead makes the popup grow leftward, which is where the
    // room actually is.
    private val COMMON_CURRENCY_SYMBOLS = listOf("¥", "£", "$", "€")
    
    /**
     * Resolves the calculator format for [locale].
     *
     * @param locale the system locale to resolve from
     * @return the resolved currency and separator format
     */
    fun resolve(locale: Locale): Format {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val currencyBase = resolveCurrencyGlyph(locale)
        val currencyAlternatives = if (currencyBase.toString() in COMMON_CURRENCY_SYMBOLS) {
            COMMON_CURRENCY_SYMBOLS
        } else {
            COMMON_CURRENCY_SYMBOLS + listOf(currencyBase.toString())
        }
        return Format(
            currencyBase = currencyBase,
            currencyAlternatives = currencyAlternatives,
            decimalSeparator = symbols.decimalSeparator,
            thousandsSeparatorHint = symbols.groupingSeparator.toString()
        )
    }
    
    /**
     * [Key.char] is a single [Char], but not every locale's currency symbol is one glyph (e.g. a bare
     * ISO code such as "CHF" for a locale without a proper symbol, or a locale without a resolvable
     * country at all) - such cases fall back to [DEFAULT_CURRENCY] rather than truncating or crashing.
     */
    private fun resolveCurrencyGlyph(locale: Locale): Char {
        val symbol = runCatching { Currency.getInstance(locale).getSymbol(locale) }.getOrNull()
        return if (symbol?.length == 1) symbol[0] else DEFAULT_CURRENCY
    }
}

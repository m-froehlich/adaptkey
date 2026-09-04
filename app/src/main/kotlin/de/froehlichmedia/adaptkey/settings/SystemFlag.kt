// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import java.util.Locale

/**
 * D-361-followup (v4): resolves a country-flag emoji from a [Locale]'s own ISO 3166-1 alpha-2 country code,
 * for the settings screen's own language-related glyph icons (Language packs, the automatic-language-switch
 * threshold). User's own explicit call, after weighing a neutral/generic flag against this: "abhängig von
 * der Systemsprache die passende auswählen" - the icon should reflect whichever country/language the device
 * itself is actually set to, not a fixed choice baked into the app.
 *
 * Purely algorithmic - a country code's two letters each map onto Unicode's Regional Indicator Symbol block
 * (`A` -> `U+1F1E6`, ..., `Z` -> `U+1F1FF`; a flag emoji is simply the pair for that code, e.g. `DE` ->
 * `U+1F1E9 U+1F1EA`), so this needs no bundled flag-image lookup table for the ~250 possible codes - it
 * generalises to any locale's country automatically, including ones this app has no dedicated language pack
 * for at all.
 */
object SystemFlag {
    
    /** Shown when [locale] carries no usable two-letter country code (e.g. a bare language-only locale). */
    const val FALLBACK = "🇩🇪"
    
    private const val REGIONAL_INDICATOR_BASE = 0x1F1E6
    
    /**
     * @param locale the locale to read a country code from - callers pass the device's actual current
     *        configuration locale (e.g. `resources.configuration.locales.get(0)`, the same source
     *        [de.froehlichmedia.adaptkey.AdaptKeyService.applySettings]'s own D-92 `systemLocale` already
     *        reads), not necessarily [Locale.getDefault] (which per-app language overrides can decouple
     *        from the device's own system locale on newer Android versions)
     * @return the two-codepoint flag emoji for [locale]'s own country, or [FALLBACK] if it has none
     */
    fun glyph(locale: Locale): String {
        val country = locale.country
        if (country.length != 2) {
            return FALLBACK
        }
        val chars = country.uppercase(Locale.ROOT)
        if (chars.any { it !in 'A'..'Z' }) {
            return FALLBACK
        }
        return chars.map { String(Character.toChars(REGIONAL_INDICATOR_BASE + (it - 'A'))) }.joinToString("")
    }
}

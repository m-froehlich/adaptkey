// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import java.util.Locale

/**
 * D-280: suggests which installable language packs are worth offering during onboarding, purely from the
 * device's own configured locales - entirely offline, since the app already knows in code (via
 * [de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog]) which languages it can even type in; no
 * network call or permission is needed to make this suggestion.
 */
object SuggestedLanguages {
    
    /**
     * @param locales the device's configured locales, most-preferred first (e.g. from
     *        `Resources.getConfiguration().locales`, converted to a plain list by the Android caller)
     * @param available the languages that can actually be suggested (defaults to every language with a
     *        real pack, [de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog.ENTRIES])
     * @return [available] entries whose [Language.code] matches one of [locales]' own ISO 639 language
     *         code, in [locales]' own preference order, without duplicates
     */
    fun from(locales: List<Locale>, available: List<Language>): List<Language> {
        val result = LinkedHashSet<Language>()
        for (locale in locales) {
            available.firstOrNull { it.code == locale.language }?.let { result.add(it) }
        }
        return result.toList()
    }
}

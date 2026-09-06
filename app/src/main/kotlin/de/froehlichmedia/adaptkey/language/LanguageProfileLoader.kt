// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import android.content.Context

/**
 * Loads the bundled `language_profiles.tsv` asset (A-03) and hands it to the pure
 * [LanguageProfileParser]. Android-only glue; left to instrumented tests.
 */
object LanguageProfileLoader {
    
    private const val ASSET_NAME = "language_profiles.tsv"
    
    /**
     * @param context any valid context (the input method service)
     * @return the parsed profiles; empty when the asset is missing or unreadable. Exposed separately from
     *         [loadClassifier] (D-450-followup) so a caller can build a second [LanguageClassifier] scoped
     *         to a subset of languages - e.g. `AdaptKeyService`'s own same-script Cyrillic-sibling
     *         classifier, built from `profiles.filterKeys { it in LayoutRegistry.CYRILLIC_LANGUAGES }` -
     *         without re-reading and re-parsing the asset a second time.
     */
    fun loadProfiles(context: Context): Map<Language, CharNgramProfile> {
        return runCatching {
            val raw = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            LanguageProfileParser.parse(raw)
        }.getOrDefault(emptyMap())
    }
    
    /**
     * @param context any valid context (the input method service)
     * @return a classifier over every parsed profile; over an empty profile set (so every result is
     *         [Language.UNKNOWN], a safe no-op guard) when the asset is missing or unreadable
     */
    fun loadClassifier(context: Context): LanguageClassifier = LanguageClassifier(loadProfiles(context))
}

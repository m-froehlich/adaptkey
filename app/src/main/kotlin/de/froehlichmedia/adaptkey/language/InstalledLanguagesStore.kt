// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import android.content.Context
import android.content.SharedPreferences

/**
 * D-280: which languages beyond the always-bundled [Language.ENGLISH] (see
 * [de.froehlichmedia.adaptkey.dictionary.DictionaryLoader.BUNDLED_LANGUAGES]) currently have an installed
 * language pack ([de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage]) - the single source of truth
 * [LanguageCycle] (G-01) and [de.froehlichmedia.adaptkey.dictionary.DictionaryLoader] both read.
 *
 * Kept in its own private SharedPreferences file, like [ActiveLanguageStore], so a change here can be
 * observed with its own listener ([de.froehlichmedia.adaptkey.AdaptKeyService] reloads the dictionary
 * stores when it fires) without also reacting to every ordinary settings change.
 */
object InstalledLanguagesStore {
    
    private const val PREFS = "adaptkey_installed_languages"
    private const val KEY_INSTALLED = "installed"
    
    /**
     * @param context any valid context
     * @return the private preferences file backing this store, so a caller can register/unregister its own
     *         [SharedPreferences.OnSharedPreferenceChangeListener]
     */
    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    
    /**
     * @param context any valid context
     * @return the installed languages (never includes [Language.ENGLISH], which is always bundled and never
     *         tracked here as a separate install)
     */
    fun load(context: Context): Set<Language> {
        val stored = prefs(context).getStringSet(KEY_INSTALLED, emptySet()) ?: emptySet()
        return stored.mapNotNull { name -> Language.entries.firstOrNull { it.name == name } }
            .filter { it != Language.ENGLISH }
            .toSet()
    }
    
    /**
     * Marks [language] as installed.
     *
     * @param context any valid context
     * @param language the newly installed language
     */
    fun add(context: Context, language: Language) {
        save(context, load(context) + language)
    }
    
    /**
     * Marks [language] as no longer installed.
     *
     * @param context any valid context
     * @param language the removed language
     */
    fun remove(context: Context, language: Language) {
        save(context, load(context) - language)
    }
    
    private fun save(context: Context, languages: Set<Language>) {
        prefs(context).edit()
            .putStringSet(KEY_INSTALLED, languages.map { it.name }.toSet())
            .apply()
    }
}

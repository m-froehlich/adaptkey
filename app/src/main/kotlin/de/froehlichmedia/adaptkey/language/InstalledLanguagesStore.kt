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
    private const val KEY_VERSION_PREFIX = "version_"
    
    /**
     * D-307: the default version assumed for an install with no version recorded at all - every language
     * pack in [de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog] started at version 1 before this
     * mechanism existed, so an existing install (from before this feature shipped) correctly reads as
     * "version 1" rather than as a spurious "unknown" state, and is offered an update the moment the
     * catalog's own version for that language moves past 1.
     */
    const val DEFAULT_VERSION = 1
    
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
     * Marks [language] as installed at [version] - the caller (the D-280 import flow) passes the catalog
     * entry's own current [de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog.Entry.version], since
     * whatever archive was just imported is, by construction, that version.
     *
     * @param context any valid context
     * @param language the newly installed language
     * @param version D-307: the installed pack's own version; defaults to [DEFAULT_VERSION] for call sites
     *        that do not care about version tracking (e.g. test fixtures)
     */
    fun add(context: Context, language: Language, version: Int = DEFAULT_VERSION) {
        save(context, load(context) + language)
        prefs(context).edit().putInt(KEY_VERSION_PREFIX + language.name, version).apply()
    }
    
    /**
     * D-307: the version of [language]'s installed pack, as recorded by the [add] call that installed it -
     * [DEFAULT_VERSION] for an install predating this mechanism, or for a language that is not installed at
     * all (the caller is expected to check [load] first if that distinction matters).
     *
     * @param context any valid context
     * @param language the language to check
     * @return the recorded installed version
     */
    fun installedVersion(context: Context, language: Language): Int {
        return prefs(context).getInt(KEY_VERSION_PREFIX + language.name, DEFAULT_VERSION)
    }
    
    /**
     * Marks [language] as no longer installed.
     *
     * @param context any valid context
     * @param language the removed language
     */
    fun remove(context: Context, language: Language) {
        save(context, load(context) - language)
        prefs(context).edit().remove(KEY_VERSION_PREFIX + language.name).apply()
    }
    
    private fun save(context: Context, languages: Set<Language>) {
        prefs(context).edit()
            .putStringSet(KEY_INSTALLED, languages.map { it.name }.toSet())
            .apply()
    }
}

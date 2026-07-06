// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import android.content.Context

/**
 * Persists the active input language / alphabet (G-01) across service restarts, in a small private
 * SharedPreferences file of its own (kept out of the settings prefs so it does not trigger the settings
 * change listener). Only the two user-selectable alphabets are stored; anything unrecognised loads as
 * German.
 */
object ActiveLanguageStore {
    
    private const val PREFS = "adaptkey_state"
    private const val KEY_ACTIVE_LANGUAGE = "active_language"
    
    /**
     * Persists the active input language.
     *
     * @param context any valid context
     * @param language the active input language
     */
    fun save(context: Context, language: Language) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_LANGUAGE, language.name)
            .apply()
    }
    
    /**
     * Loads the persisted active input language.
     *
     * @param context any valid context
     * @return the stored language, or [Language.GERMAN] when none is stored or it is unrecognised
     */
    fun load(context: Context): Language {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_LANGUAGE, null)
        return Language.entries.firstOrNull { it.name == stored } ?: Language.GERMAN
    }
}

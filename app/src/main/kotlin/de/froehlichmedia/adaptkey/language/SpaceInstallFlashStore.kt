// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlichmedia

package de.froehlichmedia.adaptkey.language

import android.content.Context
import android.content.SharedPreferences

/**
 * D-596: the one-shot "a language pack was just installed - flash the space bar green on its next
 * keyboard appearance" flag. Set by [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]'s own
 * import flow the moment an archive actually installed, consumed by the resident keyboard service the
 * next time a field focus shows the letters surface's space key.
 *
 * A prefs file of its own (like [InstalledLanguagesStore] and [ActiveLanguageStore]) rather than a
 * service field so the confirmation survives the process dying between install and the next keyboard
 * appearance - but exactly once: [clear] runs the moment the flash has been drawn, and an unconsumed
 * flag is deliberately *not* part of the backup export (the exporter enumerates its sources
 * explicitly), so a freshly imported backup cannot re-fire a confirmation for an install the new
 * device never performed.
 */
object SpaceInstallFlashStore {
    
    private const val PREFS = "adaptkey_space_install_flash"
    private const val KEY_PENDING = "pending"
    
    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    
    /** Marks that exactly one green space-bar confirmation flash is owed. */
    fun markPending(context: Context) {
        prefs(context).edit().putBoolean(KEY_PENDING, true).apply()
    }
    
    /** Whether a flash is still owed - read-only, so a caller on the wrong surface can leave it armed. */
    fun isPending(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PENDING, false)
    }
    
    /** Clears the flag - the caller flashes only after this returns true for `isPending`. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_PENDING).apply()
    }
}

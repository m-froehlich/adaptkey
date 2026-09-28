// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.content.Context
import android.provider.Settings

/**
 * D-486: reads the navigation mode the phone reports (AOSP's `Settings.Secure` `navigation_mode`, plus
 * Xiaomi's `force_fsg_nav_bar` switch, which its full-screen gestures use). Both are plain string keys any
 * app may read; every failure (missing key, a restricting ROM) yields [NavigationMode.UNKNOWN], which
 * [BottomInsetPolicy] treats like a gesture phone - i.e. the pre-D-486 behaviour.
 * 
 * As an Android-facing layer it is left to instrumented tests; the decision logic is the pure
 * [NavigationMode.from] and [BottomInsetPolicy.resolve].
 */
object NavigationModeReader {
    
    private const val KEY_NAVIGATION_MODE = "navigation_mode"
    private const val KEY_XIAOMI_FULL_SCREEN_GESTURES = "force_fsg_nav_bar"
    
    /** Raw values for the diagnostic log; null when the key is absent or unreadable. */
    data class Raw(val navigationMode: Int?, val vendorFullScreenGestures: Boolean)
    
    fun readRaw(context: Context): Raw {
        val resolver = context.contentResolver
        val secure = runCatching { Settings.Secure.getInt(resolver, KEY_NAVIGATION_MODE) }.getOrNull()
        val vendor = runCatching { Settings.Global.getInt(resolver, KEY_XIAOMI_FULL_SCREEN_GESTURES, 0) == 1 }
            .getOrDefault(false)
        return Raw(secure, vendor)
    }
    
    fun read(context: Context): NavigationMode {
        val raw = readRaw(context)
        return NavigationMode.from(raw.navigationMode, raw.vendorFullScreenGestures)
    }
}

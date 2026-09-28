// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-486: how much room the keyboard keeps free at the bottom of the screen. Android 15 draws the input view
 * edge-to-edge, so the service pads it up by the bottom system-bar/gesture inset (D-136, D-260). On a phone
 * without any gesture bar (seen on a Chinese ROM) that reserved a gesture-bar-high strip for nothing.
 * 
 * The stored values are `auto` / `nav_bar_only` / `none` - keep them in sync with `arrays.xml`
 * (`d486_bottom_inset_values`).
 */
enum class BottomInsetMode {
    
    /** Follows the navigation mode the phone reports - see [BottomInsetPolicy.resolve]. The default. */
    AUTO,
    
    /** Only the navigation bar itself is kept free, never the (invisible) gesture-recognition strip. */
    NAV_BAR_ONLY,
    
    /** Nothing is kept free: the keyboard extends to the very bottom edge. */
    NONE;
    
    companion object {
        
        val DEFAULT = AUTO
        
        /**
         * Resolves a stored preference value, tolerating case and unknown/blank input (falls back to
         * [DEFAULT], so a corrupt stored value cannot leave the setting invalid).
         */
        fun fromKey(key: String?): BottomInsetMode {
            if (key.isNullOrBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }
}

/** D-486: the navigation mode the phone reports; [UNKNOWN] whenever it cannot be read. */
enum class NavigationMode {
    THREE_BUTTON,
    TWO_BUTTON,
    GESTURAL,
    UNKNOWN;
    
    companion object {
        
        /**
         * Maps AOSP's `Settings.Secure` `navigation_mode` (0 = three-button, 1 = two-button, 2 = gestural).
         * A phone that keeps its own gestures switched on outside that setting (Xiaomi's `force_fsg_nav_bar`)
         * counts as gestural whatever the value says.
         * 
         * @param secureValue the raw `navigation_mode`, or null when it is not set/readable
         * @param vendorFullScreenGestures whether such a vendor switch reports gestures as on
         */
        fun from(secureValue: Int?, vendorFullScreenGestures: Boolean): NavigationMode {
            if (vendorFullScreenGestures) {
                return GESTURAL
            }
            return when (secureValue) {
                0 -> THREE_BUTTON
                1 -> TWO_BUTTON
                2 -> GESTURAL
                else -> UNKNOWN
            }
        }
    }
}

/**
 * D-486: the pure bottom-inset computation shared by the service's insets listener and its recheck.
 */
object BottomInsetPolicy {
    
    /**
     * @property totalPx how far the whole input view is kept clear of the bottom screen edge
     * @property reclaimableGestureZonePx the part of [totalPx] that is only the gesture-recognition strip
     *           beyond the navigation bar itself - what D-260's space-key touch extension may reclaim
     */
    data class Result(val totalPx: Int, val reclaimableGestureZonePx: Int)
    
    /**
     * @param mode the user's choice
     * @param navigationBarPx the reported navigation-bar inset (the drawn bar or pill)
     * @param gestureInsetPx the reported system-gesture inset (bar plus its recognition strip)
     * @param navigationMode what the phone reports; only consulted for [BottomInsetMode.AUTO]
     * @return in [BottomInsetMode.AUTO] exactly the pre-D-486 behaviour (`max(bar, gesture)`, with the strip
     *         beyond the bar reclaimable), except when the phone explicitly reports three-button navigation: then
     *         there is no gesture bar, so only the navigation bar is kept free
     */
    fun resolve(mode: BottomInsetMode, navigationBarPx: Int, gestureInsetPx: Int, navigationMode: NavigationMode): Result {
        val bar = navigationBarPx.coerceAtLeast(0)
        val gesture = gestureInsetPx.coerceAtLeast(0)
        return when (mode) {
            BottomInsetMode.NONE -> Result(0, 0)
            BottomInsetMode.NAV_BAR_ONLY -> Result(bar, 0)
            BottomInsetMode.AUTO -> {
                // Two-button navigation still has a swipe-up home pill, so it keeps the gesture strip.
                if (navigationMode == NavigationMode.THREE_BUTTON) {
                    Result(bar, 0)
                } else {
                    Result(maxOf(bar, gesture), (gesture - bar).coerceAtLeast(0))
                }
            }
        }
    }
}

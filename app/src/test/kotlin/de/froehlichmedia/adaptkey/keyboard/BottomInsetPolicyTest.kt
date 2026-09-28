// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * D-486: the pure bottom-inset decision - the user's "room below the keyboard" choice against the reported
 * insets and the reported navigation mode.
 */
class BottomInsetPolicyTest {
    
    private fun resolve(mode: BottomInsetMode, bar: Int, gesture: Int, navigation: NavigationMode) =
        BottomInsetPolicy.resolve(mode, bar, gesture, navigation)
    
    @Test
    fun `automatic on a gesture phone keeps the whole inset and offers the strip beyond the bar`() {
        // Pill 63 px, recognition strip up to 100 px: the pre-D-486 behaviour, unchanged.
        val result = resolve(BottomInsetMode.AUTO, 63, 100, NavigationMode.GESTURAL)
        assertEquals(BottomInsetPolicy.Result(100, 37), result)
    }
    
    @Test
    fun `automatic with an unreadable navigation mode behaves like a gesture phone`() {
        assertEquals(BottomInsetPolicy.Result(100, 37), resolve(BottomInsetMode.AUTO, 63, 100, NavigationMode.UNKNOWN))
    }
    
    @Test
    fun `automatic keeps the gesture strip on two-button navigation, which still has a home pill`() {
        assertEquals(BottomInsetPolicy.Result(100, 37), resolve(BottomInsetMode.AUTO, 63, 100, NavigationMode.TWO_BUTTON))
    }
    
    @Test
    fun `automatic on three-button navigation ignores a reported gesture strip`() {
        // The phone from the report: no gesture bar, yet a gesture inset is reported.
        assertEquals(BottomInsetPolicy.Result(0, 0), resolve(BottomInsetMode.AUTO, 0, 63, NavigationMode.THREE_BUTTON))
        // With a real button bar only the bar is kept free.
        assertEquals(BottomInsetPolicy.Result(126, 0), resolve(BottomInsetMode.AUTO, 126, 150, NavigationMode.THREE_BUTTON))
    }
    
    @Test
    fun `automatic never reserves less than the bar when the bar is the larger inset`() {
        assertEquals(BottomInsetPolicy.Result(126, 0), resolve(BottomInsetMode.AUTO, 126, 0, NavigationMode.UNKNOWN))
        assertEquals(BottomInsetPolicy.Result(126, 0), resolve(BottomInsetMode.AUTO, 126, 126, NavigationMode.GESTURAL))
    }
    
    @Test
    fun `navigation bar only never reserves the gesture strip whatever the phone reports`() {
        assertEquals(BottomInsetPolicy.Result(63, 0), resolve(BottomInsetMode.NAV_BAR_ONLY, 63, 100, NavigationMode.GESTURAL))
        assertEquals(BottomInsetPolicy.Result(0, 0), resolve(BottomInsetMode.NAV_BAR_ONLY, 0, 63, NavigationMode.UNKNOWN))
    }
    
    @Test
    fun `none reserves nothing at all`() {
        assertEquals(BottomInsetPolicy.Result(0, 0), resolve(BottomInsetMode.NONE, 126, 150, NavigationMode.THREE_BUTTON))
        assertEquals(BottomInsetPolicy.Result(0, 0), resolve(BottomInsetMode.NONE, 63, 100, NavigationMode.GESTURAL))
    }
    
    @Test
    fun `negative reported insets are treated as zero`() {
        assertEquals(BottomInsetPolicy.Result(0, 0), resolve(BottomInsetMode.AUTO, -5, -3, NavigationMode.UNKNOWN))
    }
    
    @Test
    fun `the navigation mode maps AOSP values and a vendor gesture switch wins`() {
        assertEquals(NavigationMode.THREE_BUTTON, NavigationMode.from(0, vendorFullScreenGestures = false))
        assertEquals(NavigationMode.TWO_BUTTON, NavigationMode.from(1, vendorFullScreenGestures = false))
        assertEquals(NavigationMode.GESTURAL, NavigationMode.from(2, vendorFullScreenGestures = false))
        assertEquals(NavigationMode.UNKNOWN, NavigationMode.from(null, vendorFullScreenGestures = false))
        assertEquals(NavigationMode.UNKNOWN, NavigationMode.from(7, vendorFullScreenGestures = false))
        // A phone whose own full-screen gestures are on is gestural even when navigation_mode says buttons.
        assertEquals(NavigationMode.GESTURAL, NavigationMode.from(0, vendorFullScreenGestures = true))
    }
    
    @Test
    fun `the stored mode resolves case-insensitively and falls back to automatic`() {
        assertEquals(BottomInsetMode.AUTO, BottomInsetMode.fromKey(null))
        assertEquals(BottomInsetMode.AUTO, BottomInsetMode.fromKey(""))
        assertEquals(BottomInsetMode.AUTO, BottomInsetMode.fromKey("garbage"))
        assertEquals(BottomInsetMode.NONE, BottomInsetMode.fromKey("none"))
        assertEquals(BottomInsetMode.NAV_BAR_ONLY, BottomInsetMode.fromKey("nav_bar_only"))
        assertEquals(BottomInsetMode.NAV_BAR_ONLY, BottomInsetMode.fromKey(" NAV_BAR_ONLY "))
        assertEquals(BottomInsetMode.AUTO, BottomInsetMode.fromKey("auto"))
    }
}

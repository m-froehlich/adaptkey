// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-352 auto-split mode enum and its stored-value resolution.
 */
class AutoSplitModeTest {
    
    @Test
    fun `the default is automatic`() {
        assertEquals(AutoSplitMode.AUTOMATIC, AutoSplitMode.DEFAULT)
    }
    
    @Test
    fun `known keys resolve regardless of case and surrounding space`() {
        assertEquals(AutoSplitMode.AUTOMATIC, AutoSplitMode.fromKey("automatic"))
        assertEquals(AutoSplitMode.CHIP_ONLY, AutoSplitMode.fromKey("CHIP_ONLY"))
        assertEquals(AutoSplitMode.OFF, AutoSplitMode.fromKey("  off "))
    }
    
    @Test
    fun `null blank and unknown keys fall back to the default`() {
        assertEquals(AutoSplitMode.DEFAULT, AutoSplitMode.fromKey(null))
        assertEquals(AutoSplitMode.DEFAULT, AutoSplitMode.fromKey("   "))
        assertEquals(AutoSplitMode.DEFAULT, AutoSplitMode.fromKey("bogus"))
    }
}

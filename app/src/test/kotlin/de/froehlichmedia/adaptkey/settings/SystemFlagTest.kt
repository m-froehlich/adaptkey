// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SystemFlag] - pure [Locale]-to-flag-emoji resolution, independent of the Android glue
 * ([SettingsActivity]) that reads the actual device locale to feed it.
 */
class SystemFlagTest {
    
    @Test
    fun `resolves a real two-letter country code to its flag emoji`() {
        assertEquals("🇩🇪", SystemFlag.glyph(Locale.GERMANY))
        assertEquals("🇬🇧", SystemFlag.glyph(Locale.UK))
        assertEquals("🇺🇸", SystemFlag.glyph(Locale.US))
    }
    
    @Test
    fun `lower-case country codes resolve the same as upper-case ones`() {
        assertEquals(SystemFlag.glyph(Locale.GERMANY), SystemFlag.glyph(Locale("de", "de")))
    }
    
    @Test
    fun `a language-only locale with no country falls back`() {
        assertEquals(SystemFlag.FALLBACK, SystemFlag.glyph(Locale("de")))
    }
    
    @Test
    fun `an empty locale falls back`() {
        assertEquals(SystemFlag.FALLBACK, SystemFlag.glyph(Locale("")))
    }
    
    @Test
    fun `a malformed country code falls back rather than resolving nonsense`() {
        assertEquals(SystemFlag.FALLBACK, SystemFlag.glyph(Locale("de", "1"))) // too short
        assertEquals(SystemFlag.FALLBACK, SystemFlag.glyph(Locale("es", "419"))) // too long (UN M49 region)
        assertEquals(SystemFlag.FALLBACK, SystemFlag.glyph(Locale("de", "12"))) // right length, not letters
    }
}

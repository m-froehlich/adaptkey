// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the D-143/D-144/D-145 URL-mode period-key TLD resolution:
 * [UrlLocale.periodAlternatives] never includes the plain full stop itself (D-144 - a straight tap already
 * types `.`), and the locale's own ccTLD, when it has one, sits at the *middle* of the list, flanked by the
 * two generic TLDs (D-145 - `AdaptKeyboardView.preSelectedIndexFor` pre-selects this key's one entry that is
 * neither `.com` nor `.org`, and D-01's popup centres whichever entry is pre-selected directly over the
 * stem, so the single most personally relevant suggestion must be reachable there, without any finger
 * movement, regardless of language).
 */
class UrlLocaleTest {
    
    @Test
    fun `a German locale's ccTLD sits at the middle, flanked by the generic TLDs`() {
        assertEquals(listOf(".com", ".de", ".org"), UrlLocale.periodAlternatives(Locale.GERMANY))
    }
    
    @Test
    fun `a Greek locale's ccTLD sits at the middle too`() {
        assertEquals(listOf(".com", ".gr", ".org"), UrlLocale.periodAlternatives(Locale("el", "GR")))
    }
    
    @Test
    fun `D-145 a UK locale's middle entry is co uk (not the bare uk)`() {
        assertEquals(listOf(".com", ".co.uk", ".org"), UrlLocale.periodAlternatives(Locale.UK))
    }
    
    @Test
    fun `a US locale has no idiomatic ccTLD - just the two generic TLDs, com leading`() {
        assertEquals(listOf(".com", ".org"), UrlLocale.periodAlternatives(Locale.US))
    }
    
    @Test
    fun `a locale with no country falls back to the generic TLDs alone`() {
        assertEquals(listOf(".com", ".org"), UrlLocale.periodAlternatives(Locale("de")))
    }
    
    @Test
    fun `D-144 the bare full stop is never among the alternatives`() {
        assertFalse(UrlLocale.periodAlternatives(Locale.GERMANY).contains("."))
        assertFalse(UrlLocale.periodAlternatives(Locale.US).contains("."))
    }
    
    @Test
    fun `D-145 the ccTLD, when present, is always the one entry that is neither com nor org`() {
        val germany = UrlLocale.periodAlternatives(Locale.GERMANY)
        assertEquals(".de", germany.first { it != ".com" && it != ".org" })
        
        val uk = UrlLocale.periodAlternatives(Locale.UK)
        assertEquals(".co.uk", uk.first { it != ".com" && it != ".org" })
        
        // No ccTLD for this locale - every entry is one of the two generic TLDs.
        val us = UrlLocale.periodAlternatives(Locale.US)
        assertTrue(us.all { it == ".com" || it == ".org" })
    }
}

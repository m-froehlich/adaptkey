// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the D-143/D-144/D-145 URL-mode period-key TLD resolution:
 * [UrlLocale.periodAlternatives] never includes the plain full stop itself (D-144 - a straight tap already
 * types `.`), and the locale's own ccTLD always leads when it has one (D-145 - the D-01 popup centres its
 * *pre-selected* (first) entry directly over the stem, so the single most personally relevant suggestion
 * must be the one reachable without any finger movement, regardless of language).
 */
class UrlLocaleTest {
    
    @Test
    fun `a German locale leads with its own ccTLD, then the generic TLDs`() {
        assertEquals(listOf(".de", ".com", ".org"), UrlLocale.periodAlternatives(Locale.GERMANY))
    }
    
    @Test
    fun `a Greek locale leads with its own ccTLD too`() {
        assertEquals(listOf(".gr", ".com", ".org"), UrlLocale.periodAlternatives(Locale("el", "GR")))
    }
    
    @Test
    fun `D-145 a UK locale leads with co uk (not the bare uk), even though English is generic-first elsewhere`() {
        assertEquals(listOf(".co.uk", ".com", ".org"), UrlLocale.periodAlternatives(Locale.UK))
    }
    
    @Test
    fun `a US locale is English with no idiomatic ccTLD - leads with the generic com instead`() {
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
    fun `D-145 the ccTLD, when present, is always the first (pre-selected) entry`() {
        assertEquals(".de", UrlLocale.periodAlternatives(Locale.GERMANY).first())
        assertEquals(".co.uk", UrlLocale.periodAlternatives(Locale.UK).first())
        // No ccTLD for this locale - .com is the sensible first entry instead.
        assertEquals(".com", UrlLocale.periodAlternatives(Locale.US).first())
    }
}

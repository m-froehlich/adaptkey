// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Unit tests for the D-143/D-144 URL-mode period-key TLD resolution: [UrlLocale.periodAlternatives]
 * orders TLD suggestions per [UrlLocale]'s own doc comment - D-144 correction: never includes the plain
 * full stop itself, since a straight tap already types `.` and repeating it in its own popup is redundant.
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
    fun `a UK locale is English - leads with the generic TLDs, then co uk (not the bare uk)`() {
        assertEquals(listOf(".com", ".org", ".co.uk"), UrlLocale.periodAlternatives(Locale.UK))
    }
    
    @Test
    fun `a US locale is English with no idiomatic ccTLD - only the generic TLDs`() {
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
}

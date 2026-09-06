// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.capitalisation

import de.froehlichmedia.adaptkey.language.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Unit tests for D-449-followup's [CasingRules] seam: [DefaultCasingRules] and [CasingRulesRegistry].
 */
class CasingRulesTest {
    
    @Test
    fun `DefaultCasingRules mirrors Kotlin's own locale-invariant single-character mapping`() {
        assertEquals("Haus", DefaultCasingRules.uppercaseFirst("haus"))
        assertEquals("haus", DefaultCasingRules.lowercaseFirst("Haus"))
        assertEquals("HAUS", DefaultCasingRules.uppercaseAll("haus"))
    }
    
    @Test
    fun `DefaultCasingRules does not apply Turkish dotting to plain i`() {
        assertEquals("Istanbul", DefaultCasingRules.uppercaseFirst("istanbul"))
    }
    
    @Test
    fun `Turkish resolves to TurkishCasingRules`() {
        assertSame(TurkishCasingRules, CasingRulesRegistry.rulesFor(Language.TURKISH))
    }
    
    @Test
    fun `Azerbaijani resolves to TurkishCasingRules too - the shared tr slash az dotted-I rule`() {
        assertSame(TurkishCasingRules, CasingRulesRegistry.rulesFor(Language.AZERBAIJANI))
    }
    
    @Test
    fun `every other language resolves to DefaultCasingRules`() {
        assertSame(DefaultCasingRules, CasingRulesRegistry.rulesFor(Language.GERMAN))
        assertSame(DefaultCasingRules, CasingRulesRegistry.rulesFor(Language.ENGLISH))
        assertSame(DefaultCasingRules, CasingRulesRegistry.rulesFor(Language.UNKNOWN))
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * D-410: [LanguageRulesRegistry] resolves each [Language]'s own [LanguageRules] - German and (D-441)
 * French get real implementations, every other (bundled or not) language gets [NoOpLanguageRules], so a
 * German- or French-specific grammar/orthography rule can never silently apply while a different
 * language is active.
 */
class LanguageRulesTest {
    
    @Test
    fun `German resolves to GermanRules`() {
        assertSame(GermanRules, LanguageRulesRegistry.rulesFor(Language.GERMAN))
    }
    
    @Test
    fun `French resolves to FrenchRules`() {
        assertSame(FrenchRules, LanguageRulesRegistry.rulesFor(Language.FRENCH))
    }
    
    @Test
    fun `every other bundled language resolves to the no-op default`() {
        assertSame(NoOpLanguageRules, LanguageRulesRegistry.rulesFor(Language.ENGLISH))
        assertSame(NoOpLanguageRules, LanguageRulesRegistry.rulesFor(Language.GREEK))
        assertSame(NoOpLanguageRules, LanguageRulesRegistry.rulesFor(Language.UNKNOWN))
    }
    
    @Test
    fun `the no-op default never blocks a split prefix`() {
        assertFalse(NoOpLanguageRules.blocksAsSplitPrefix("un", 0L))
    }
    
    @Test
    fun `the no-op default never blocks a feminine agent exception`() {
        assertFalse(NoOpLanguageRules.blocksAsFeminineAgentException("in", "lehrer", true))
    }
    
    @Test
    fun `the no-op default never recognises a verb inflection`() {
        assertFalse(NoOpLanguageRules.isPlausibleVerbInflection("beurteilst") { true })
    }
    
    @Test
    fun `the no-op default never recognises an adjective comparative`() {
        assertFalse(NoOpLanguageRules.isPlausibleAdjectiveComparative("schoener") { true })
    }
    
    @Test
    fun `the no-op default never splits a compound`() {
        assertNull(NoOpLanguageRules.splitCompound("beitragsjahren", { true }) { it })
    }
    
    @Test
    fun `the no-op default has no time-suggestion word`() {
        assertNull(NoOpLanguageRules.timeSuggestionWord())
    }
    
    @Test
    fun `the no-op default curates no bundled blacklist`() {
        assertTrue(NoOpLanguageRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `the no-op default never glues a decimal comma`() {
        assertFalse(NoOpLanguageRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `German suggests Uhr after a time`() {
        assertEquals("Uhr", GermanRules.timeSuggestionWord())
    }
    
    @Test
    fun `German glues a decimal comma`() {
        assertTrue(GermanRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `German curates a non-empty bundled blacklist`() {
        assertTrue(GermanRules.bundledConfusablesBlacklist().containsAll(setOf("due", "sue", "ddr", "aks")))
    }
    
    @Test
    fun `German blocks an inseparable prefix below the frequency ceiling`() {
        assertTrue(GermanRules.blocksAsSplitPrefix("wider", GermanRules.PREFIX_COMMON_WORD_FREQUENCY_CEILING))
        assertFalse(GermanRules.blocksAsSplitPrefix("wider", GermanRules.PREFIX_COMMON_WORD_FREQUENCY_CEILING + 1))
    }
    
    @Test
    fun `German does not block a candidate that is not an inseparable prefix`() {
        assertFalse(GermanRules.blocksAsSplitPrefix("und", 0L))
    }
    
    @Test
    fun `German blocks the feminine agent exception for a known -er noun stem`() {
        assertTrue(GermanRules.blocksAsFeminineAgentException("in", "lehrer", true))
    }
    
    @Test
    fun `German blocks the feminine agent exception for a curated non--er stem`() {
        assertTrue(GermanRules.blocksAsFeminineAgentException("in", "arzt", true))
    }
    
    @Test
    fun `German does not block the feminine agent exception when the left half is not a noun`() {
        assertFalse(GermanRules.blocksAsFeminineAgentException("in", "lehrer", false))
    }
    
    @Test
    fun `German does not block the feminine agent exception for a right half other than in`() {
        assertFalse(GermanRules.blocksAsFeminineAgentException("en", "lehrer", true))
    }
    
    @Test
    fun `German recognises a plausible weak-verb inflection`() {
        assertTrue(GermanRules.isPlausibleVerbInflection("beurteilst") { it == "beurteilen" })
    }
    
    @Test
    fun `German recognises a plausible adjective comparative`() {
        assertTrue(GermanRules.isPlausibleAdjectiveComparative("zuversichtlicher") { it == "zuversichtlich" })
    }
    
    @Test
    fun `German finds a compound split when the callbacks recognise it`() {
        val result = GermanRules.splitCompound(
            "beitragsjahren",
            isKnownNoun = { it == "beitrag" }
        ) { rest -> if (rest == "jahren") rest else null }
        assertEquals("beitrag", result?.firstPart)
        assertEquals("jahren", result?.rest)
    }
    
    @Test
    fun `French glues a decimal comma`() {
        assertTrue(FrenchRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `French has no time-suggestion word`() {
        assertNull(FrenchRules.timeSuggestionWord())
    }
    
    @Test
    fun `French curates no bundled blacklist yet`() {
        assertTrue(FrenchRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `French leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(FrenchRules.blocksAsSplitPrefix("un", 0L))
        assertFalse(FrenchRules.blocksAsFeminineAgentException("in", "lehrer", true))
        assertFalse(FrenchRules.blocksAsCompoundPrefix("schon", true))
        assertFalse(FrenchRules.isPlausibleVerbInflection("parlons") { true })
        assertFalse(FrenchRules.isPlausibleAdjectiveComparative("plus grand") { true })
        assertNull(FrenchRules.splitCompound("motclé", { true }) { it })
    }
}

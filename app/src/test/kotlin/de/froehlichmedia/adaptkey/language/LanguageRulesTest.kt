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
 * D-410: [LanguageRulesRegistry] resolves each [Language]'s own [LanguageRules] - German, English
 * (D-number), French (D-441) and Spanish (D-443) get real implementations, every other (bundled or not)
 * language gets [NoOpLanguageRules], so a language-specific grammar/orthography rule can never silently
 * apply while a different language is active.
 */
class LanguageRulesTest {
    
    @Test
    fun `German resolves to GermanRules`() {
        assertSame(GermanRules, LanguageRulesRegistry.rulesFor(Language.GERMAN))
    }
    
    @Test
    fun `English resolves to EnglishRules`() {
        assertSame(EnglishRules, LanguageRulesRegistry.rulesFor(Language.ENGLISH))
    }
    
    @Test
    fun `French resolves to FrenchRules`() {
        assertSame(FrenchRules, LanguageRulesRegistry.rulesFor(Language.FRENCH))
    }
    
    @Test
    fun `Spanish resolves to SpanishRules`() {
        assertSame(SpanishRules, LanguageRulesRegistry.rulesFor(Language.SPANISH))
    }
    
    @Test
    fun `Portuguese resolves to PortugueseRules`() {
        assertSame(PortugueseRules, LanguageRulesRegistry.rulesFor(Language.PORTUGUESE))
    }
    
    @Test
    fun `Italian resolves to ItalianRules`() {
        assertSame(ItalianRules, LanguageRulesRegistry.rulesFor(Language.ITALIAN))
    }
    
    @Test
    fun `Dutch resolves to DutchRules`() {
        assertSame(DutchRules, LanguageRulesRegistry.rulesFor(Language.DUTCH))
    }
    
    @Test
    fun `Polish resolves to PolishRules`() {
        assertSame(PolishRules, LanguageRulesRegistry.rulesFor(Language.POLISH))
    }
    
    @Test
    fun `Turkish resolves to TurkishRules`() {
        assertSame(TurkishRules, LanguageRulesRegistry.rulesFor(Language.TURKISH))
    }
    
    @Test
    fun `every other bundled language resolves to the no-op default`() {
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
    
    @Test
    fun `Spanish glues a decimal comma`() {
        assertTrue(SpanishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Spanish has no time-suggestion word`() {
        assertNull(SpanishRules.timeSuggestionWord())
    }
    
    @Test
    fun `Spanish curates no bundled blacklist yet`() {
        assertTrue(SpanishRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Spanish leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(SpanishRules.blocksAsSplitPrefix("un", 0L))
        assertFalse(SpanishRules.blocksAsFeminineAgentException("in", "maestro", true))
        assertFalse(SpanishRules.blocksAsCompoundPrefix("bien", true))
        assertFalse(SpanishRules.isPlausibleVerbInflection("hablamos") { true })
        assertFalse(SpanishRules.isPlausibleAdjectiveComparative("más grande") { true })
        assertNull(SpanishRules.splitCompound("palabraclave", { true }) { it })
    }
    
    @Test
    fun `English does not glue a decimal comma`() {
        assertFalse(EnglishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `English has no time-suggestion word`() {
        assertNull(EnglishRules.timeSuggestionWord())
    }
    
    @Test
    fun `English curates a small, real bundled blacklist`() {
        assertTrue(EnglishRules.bundledConfusablesBlacklist().containsAll(setOf("ij", "iz", "iy", "ae", "ne")))
    }
    
    @Test
    fun `English leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(EnglishRules.blocksAsSplitPrefix("un", 0L))
        assertFalse(EnglishRules.blocksAsFeminineAgentException("in", "teacher", true))
        assertFalse(EnglishRules.blocksAsCompoundPrefix("some", true))
        assertFalse(EnglishRules.isPlausibleVerbInflection("walks") { true })
        assertFalse(EnglishRules.isPlausibleAdjectiveComparative("faster") { true })
        assertNull(EnglishRules.splitCompound("keyword", { true }) { it })
    }
    
    @Test
    fun `Portuguese glues a decimal comma`() {
        assertTrue(PortugueseRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Portuguese has no time-suggestion word`() {
        assertNull(PortugueseRules.timeSuggestionWord())
    }
    
    @Test
    fun `Portuguese curates no bundled blacklist yet`() {
        assertTrue(PortugueseRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Portuguese leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(PortugueseRules.blocksAsSplitPrefix("um", 0L))
        assertFalse(PortugueseRules.blocksAsFeminineAgentException("a", "professor", true))
        assertFalse(PortugueseRules.blocksAsCompoundPrefix("bem", true))
        assertFalse(PortugueseRules.isPlausibleVerbInflection("falamos") { true })
        assertFalse(PortugueseRules.isPlausibleAdjectiveComparative("maior") { true })
        assertNull(PortugueseRules.splitCompound("palavrachave", { true }) { it })
    }
    
    @Test
    fun `Italian glues a decimal comma`() {
        assertTrue(ItalianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Italian has no time-suggestion word`() {
        assertNull(ItalianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Italian curates no bundled blacklist yet`() {
        assertTrue(ItalianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Italian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(ItalianRules.blocksAsSplitPrefix("un", 0L))
        assertFalse(ItalianRules.blocksAsFeminineAgentException("a", "maestro", true))
        assertFalse(ItalianRules.blocksAsCompoundPrefix("bene", true))
        assertFalse(ItalianRules.isPlausibleVerbInflection("parliamo") { true })
        assertFalse(ItalianRules.isPlausibleAdjectiveComparative("più grande") { true })
        assertNull(ItalianRules.splitCompound("parolachiave", { true }) { it })
    }
    
    @Test
    fun `Dutch glues a decimal comma`() {
        assertTrue(DutchRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Dutch suggests uur after a time`() {
        assertEquals("uur", DutchRules.timeSuggestionWord())
    }
    
    @Test
    fun `Dutch curates no bundled blacklist yet`() {
        assertTrue(DutchRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Dutch leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(DutchRules.blocksAsSplitPrefix("een", 0L))
        assertFalse(DutchRules.blocksAsFeminineAgentException("in", "leraar", true))
        assertFalse(DutchRules.blocksAsCompoundPrefix("goed", true))
        assertFalse(DutchRules.isPlausibleVerbInflection("praten") { true })
        assertFalse(DutchRules.isPlausibleAdjectiveComparative("groter") { true })
        assertNull(DutchRules.splitCompound("sleutelwoord", { true }) { it })
    }
    
    @Test
    fun `Polish glues a decimal comma`() {
        assertTrue(PolishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Polish has no time-suggestion word`() {
        assertNull(PolishRules.timeSuggestionWord())
    }
    
    @Test
    fun `Polish curates no bundled blacklist yet`() {
        assertTrue(PolishRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Polish leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(PolishRules.blocksAsSplitPrefix("nie", 0L))
        assertFalse(PolishRules.blocksAsFeminineAgentException("ka", "nauczyciel", true))
        assertFalse(PolishRules.blocksAsCompoundPrefix("dobrze", true))
        assertFalse(PolishRules.isPlausibleVerbInflection("czytamy") { true })
        assertFalse(PolishRules.isPlausibleAdjectiveComparative("większy") { true })
        assertNull(PolishRules.splitCompound("słowokluczowe", { true }) { it })
    }
    
    @Test
    fun `Turkish glues a decimal comma`() {
        assertTrue(TurkishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Turkish has no time-suggestion word`() {
        assertNull(TurkishRules.timeSuggestionWord())
    }
    
    @Test
    fun `Turkish curates no bundled blacklist yet`() {
        assertTrue(TurkishRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Turkish leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(TurkishRules.blocksAsSplitPrefix("hayir", 0L))
        assertFalse(TurkishRules.blocksAsFeminineAgentException("ci", "ogretmen", true))
        assertFalse(TurkishRules.blocksAsCompoundPrefix("iyi", true))
        assertFalse(TurkishRules.isPlausibleVerbInflection("okuyoruz") { true })
        assertFalse(TurkishRules.isPlausibleAdjectiveComparative("daha buyuk") { true })
        assertNull(TurkishRules.splitCompound("anahtarkelime", { true }) { it })
    }
    
    @Test
    fun `Swedish resolves to SwedishRules`() {
        assertSame(SwedishRules, LanguageRulesRegistry.rulesFor(Language.SWEDISH))
    }
    
    @Test
    fun `Swedish glues a decimal comma`() {
        assertTrue(SwedishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Swedish has no time-suggestion word`() {
        assertNull(SwedishRules.timeSuggestionWord())
    }
    
    @Test
    fun `Swedish curates no bundled blacklist yet`() {
        assertTrue(SwedishRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Swedish leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(SwedishRules.blocksAsSplitPrefix("nej", 0L))
        assertFalse(SwedishRules.blocksAsFeminineAgentException("inna", "larare", true))
        assertFalse(SwedishRules.blocksAsCompoundPrefix("bra", true))
        assertFalse(SwedishRules.isPlausibleVerbInflection("laser") { true })
        assertFalse(SwedishRules.isPlausibleAdjectiveComparative("storre") { true })
        assertNull(SwedishRules.splitCompound("nyckelord", { true }) { it })
    }
    
    @Test
    fun `Norwegian resolves to NorwegianRules`() {
        assertSame(NorwegianRules, LanguageRulesRegistry.rulesFor(Language.NORWEGIAN))
    }
    
    @Test
    fun `Norwegian glues a decimal comma`() {
        assertTrue(NorwegianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Norwegian has no time-suggestion word`() {
        assertNull(NorwegianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Norwegian curates no bundled blacklist yet`() {
        assertTrue(NorwegianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Norwegian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(NorwegianRules.blocksAsSplitPrefix("nei", 0L))
        assertFalse(NorwegianRules.blocksAsFeminineAgentException("inne", "larer", true))
        assertFalse(NorwegianRules.blocksAsCompoundPrefix("bra", true))
        assertFalse(NorwegianRules.isPlausibleVerbInflection("leser") { true })
        assertFalse(NorwegianRules.isPlausibleAdjectiveComparative("storre") { true })
        assertNull(NorwegianRules.splitCompound("nokkelord", { true }) { it })
    }
    
    @Test
    fun `Danish resolves to DanishRules`() {
        assertSame(DanishRules, LanguageRulesRegistry.rulesFor(Language.DANISH))
    }
    
    @Test
    fun `Danish glues a decimal comma`() {
        assertTrue(DanishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Danish has no time-suggestion word`() {
        assertNull(DanishRules.timeSuggestionWord())
    }
    
    @Test
    fun `Danish curates no bundled blacklist yet`() {
        assertTrue(DanishRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Danish leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(DanishRules.blocksAsSplitPrefix("nej", 0L))
        assertFalse(DanishRules.blocksAsFeminineAgentException("inde", "laerer", true))
        assertFalse(DanishRules.blocksAsCompoundPrefix("godt", true))
        assertFalse(DanishRules.isPlausibleVerbInflection("laeser") { true })
        assertFalse(DanishRules.isPlausibleAdjectiveComparative("storre") { true })
        assertNull(DanishRules.splitCompound("noegleord", { true }) { it })
    }
    
    @Test
    fun `Finnish resolves to FinnishRules`() {
        assertSame(FinnishRules, LanguageRulesRegistry.rulesFor(Language.FINNISH))
    }
    
    @Test
    fun `Finnish glues a decimal comma`() {
        assertTrue(FinnishRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Finnish has no time-suggestion word`() {
        assertNull(FinnishRules.timeSuggestionWord())
    }
    
    @Test
    fun `Finnish curates no bundled blacklist yet`() {
        assertTrue(FinnishRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Finnish leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(FinnishRules.blocksAsSplitPrefix("ei", 0L))
        assertFalse(FinnishRules.blocksAsFeminineAgentException("tar", "opettaja", true))
        assertFalse(FinnishRules.blocksAsCompoundPrefix("hyvin", true))
        assertFalse(FinnishRules.isPlausibleVerbInflection("luemme") { true })
        assertFalse(FinnishRules.isPlausibleAdjectiveComparative("suurempi") { true })
        assertNull(FinnishRules.splitCompound("avainsana", { true }) { it })
    }
    
    @Test
    fun `Czech resolves to CzechRules`() {
        assertSame(CzechRules, LanguageRulesRegistry.rulesFor(Language.CZECH))
    }
    
    @Test
    fun `Czech glues a decimal comma`() {
        assertTrue(CzechRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Czech has no time-suggestion word`() {
        assertNull(CzechRules.timeSuggestionWord())
    }
    
    @Test
    fun `Czech curates no bundled blacklist yet`() {
        assertTrue(CzechRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Czech leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(CzechRules.blocksAsSplitPrefix("ne", 0L))
        assertFalse(CzechRules.blocksAsFeminineAgentException("ka", "ucitel", true))
        assertFalse(CzechRules.blocksAsCompoundPrefix("dobre", true))
        assertFalse(CzechRules.isPlausibleVerbInflection("cteme") { true })
        assertFalse(CzechRules.isPlausibleAdjectiveComparative("vetsi") { true })
        assertNull(CzechRules.splitCompound("klicove_slovo", { true }) { it })
    }
    
    @Test
    fun `Slovak resolves to SlovakRules`() {
        assertSame(SlovakRules, LanguageRulesRegistry.rulesFor(Language.SLOVAK))
    }
    
    @Test
    fun `Slovak glues a decimal comma`() {
        assertTrue(SlovakRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Slovak has no time-suggestion word`() {
        assertNull(SlovakRules.timeSuggestionWord())
    }
    
    @Test
    fun `Slovak curates no bundled blacklist yet`() {
        assertTrue(SlovakRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Slovak leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(SlovakRules.blocksAsSplitPrefix("nie", 0L))
        assertFalse(SlovakRules.blocksAsFeminineAgentException("ka", "ucitel", true))
        assertFalse(SlovakRules.blocksAsCompoundPrefix("dobre", true))
        assertFalse(SlovakRules.isPlausibleVerbInflection("citame") { true })
        assertFalse(SlovakRules.isPlausibleAdjectiveComparative("vacsi") { true })
        assertNull(SlovakRules.splitCompound("klucove_slovo", { true }) { it })
    }
    
    @Test
    fun `Hungarian resolves to HungarianRules`() {
        assertSame(HungarianRules, LanguageRulesRegistry.rulesFor(Language.HUNGARIAN))
    }
    
    @Test
    fun `Hungarian glues a decimal comma`() {
        assertTrue(HungarianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Hungarian has no time-suggestion word`() {
        assertNull(HungarianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Hungarian curates no bundled blacklist yet`() {
        assertTrue(HungarianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Hungarian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(HungarianRules.blocksAsSplitPrefix("nem", 0L))
        assertFalse(HungarianRules.blocksAsFeminineAgentException("no", "tanito", true))
        assertFalse(HungarianRules.blocksAsCompoundPrefix("jol", true))
        assertFalse(HungarianRules.isPlausibleVerbInflection("olvasunk") { true })
        assertFalse(HungarianRules.isPlausibleAdjectiveComparative("nagyobb") { true })
        assertNull(HungarianRules.splitCompound("kulcsszo", { true }) { it })
    }
    
    @Test
    fun `Romanian resolves to RomanianRules`() {
        assertSame(RomanianRules, LanguageRulesRegistry.rulesFor(Language.ROMANIAN))
    }
    
    @Test
    fun `Romanian glues a decimal comma`() {
        assertTrue(RomanianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Romanian has no time-suggestion word`() {
        assertNull(RomanianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Romanian curates no bundled blacklist yet`() {
        assertTrue(RomanianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Romanian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(RomanianRules.blocksAsSplitPrefix("nu", 0L))
        assertFalse(RomanianRules.blocksAsFeminineAgentException("a", "profesor", true))
        assertFalse(RomanianRules.blocksAsCompoundPrefix("bine", true))
        assertFalse(RomanianRules.isPlausibleVerbInflection("citim") { true })
        assertFalse(RomanianRules.isPlausibleAdjectiveComparative("mai_mare") { true })
        assertNull(RomanianRules.splitCompound("cuvant_cheie", { true }) { it })
    }
    
    @Test
    fun `Croatian resolves to CroatianRules`() {
        assertSame(CroatianRules, LanguageRulesRegistry.rulesFor(Language.CROATIAN))
    }
    
    @Test
    fun `Croatian glues a decimal comma`() {
        assertTrue(CroatianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Croatian has no time-suggestion word`() {
        assertNull(CroatianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Croatian curates no bundled blacklist yet`() {
        assertTrue(CroatianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Croatian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(CroatianRules.blocksAsSplitPrefix("ne", 0L))
        assertFalse(CroatianRules.blocksAsFeminineAgentException("ica", "ucitelj", true))
        assertFalse(CroatianRules.blocksAsCompoundPrefix("dobro", true))
        assertFalse(CroatianRules.isPlausibleVerbInflection("citamo") { true })
        assertFalse(CroatianRules.isPlausibleAdjectiveComparative("veci") { true })
        assertNull(CroatianRules.splitCompound("kljucna_rijec", { true }) { it })
    }
    
    @Test
    fun `Bosnian resolves to BosnianRules`() {
        assertSame(BosnianRules, LanguageRulesRegistry.rulesFor(Language.BOSNIAN))
    }
    
    @Test
    fun `Bosnian glues a decimal comma`() {
        assertTrue(BosnianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Bosnian has no time-suggestion word`() {
        assertNull(BosnianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Bosnian curates no bundled blacklist yet`() {
        assertTrue(BosnianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Bosnian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(BosnianRules.blocksAsSplitPrefix("ne", 0L))
        assertFalse(BosnianRules.blocksAsFeminineAgentException("ica", "ucitelj", true))
        assertFalse(BosnianRules.blocksAsCompoundPrefix("dobro", true))
        assertFalse(BosnianRules.isPlausibleVerbInflection("citamo") { true })
        assertFalse(BosnianRules.isPlausibleAdjectiveComparative("veci") { true })
        assertNull(BosnianRules.splitCompound("kljucna_rijec", { true }) { it })
    }
    
    @Test
    fun `Serbian resolves to SerbianRules`() {
        assertSame(SerbianRules, LanguageRulesRegistry.rulesFor(Language.SERBIAN))
    }
    
    @Test
    fun `Serbian glues a decimal comma`() {
        assertTrue(SerbianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Serbian has no time-suggestion word`() {
        assertNull(SerbianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Serbian curates no bundled blacklist yet`() {
        assertTrue(SerbianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Serbian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(SerbianRules.blocksAsSplitPrefix("не", 0L))
        assertFalse(SerbianRules.blocksAsFeminineAgentException("ица", "учитељ", true))
        assertFalse(SerbianRules.blocksAsCompoundPrefix("добро", true))
        assertFalse(SerbianRules.isPlausibleVerbInflection("читамо") { true })
        assertFalse(SerbianRules.isPlausibleAdjectiveComparative("већи") { true })
        assertNull(SerbianRules.splitCompound("кључна_реч", { true }) { it })
    }
    
    @Test
    fun `Estonian resolves to EstonianRules`() {
        assertSame(EstonianRules, LanguageRulesRegistry.rulesFor(Language.ESTONIAN))
    }
    
    @Test
    fun `Estonian glues a decimal comma`() {
        assertTrue(EstonianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Estonian has no time-suggestion word`() {
        assertNull(EstonianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Estonian curates no bundled blacklist yet`() {
        assertTrue(EstonianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Estonian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(EstonianRules.blocksAsSplitPrefix("ei", 0L))
        assertFalse(EstonianRules.blocksAsFeminineAgentException("tar", "opetaja", true))
        assertFalse(EstonianRules.blocksAsCompoundPrefix("hasti", true))
        assertFalse(EstonianRules.isPlausibleVerbInflection("loeme") { true })
        assertFalse(EstonianRules.isPlausibleAdjectiveComparative("suurem") { true })
        assertNull(EstonianRules.splitCompound("votmesona", { true }) { it })
    }
    
    @Test
    fun `Latvian resolves to LatvianRules`() {
        assertSame(LatvianRules, LanguageRulesRegistry.rulesFor(Language.LATVIAN))
    }
    
    @Test
    fun `Latvian glues a decimal comma`() {
        assertTrue(LatvianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Latvian has no time-suggestion word`() {
        assertNull(LatvianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Latvian curates no bundled blacklist yet`() {
        assertTrue(LatvianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Latvian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(LatvianRules.blocksAsSplitPrefix("ne", 0L))
        assertFalse(LatvianRules.blocksAsFeminineAgentException("iece", "skolotajs", true))
        assertFalse(LatvianRules.blocksAsCompoundPrefix("labi", true))
        assertFalse(LatvianRules.isPlausibleVerbInflection("lasam") { true })
        assertFalse(LatvianRules.isPlausibleAdjectiveComparative("lielaks") { true })
        assertNull(LatvianRules.splitCompound("atslegvards", { true }) { it })
    }
    
    @Test
    fun `Lithuanian resolves to LithuanianRules`() {
        assertSame(LithuanianRules, LanguageRulesRegistry.rulesFor(Language.LITHUANIAN))
    }
    
    @Test
    fun `Lithuanian glues a decimal comma`() {
        assertTrue(LithuanianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Lithuanian has no time-suggestion word`() {
        assertNull(LithuanianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Lithuanian curates no bundled blacklist yet`() {
        assertTrue(LithuanianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Lithuanian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(LithuanianRules.blocksAsSplitPrefix("ne", 0L))
        assertFalse(LithuanianRules.blocksAsFeminineAgentException("ge", "mokytojas", true))
        assertFalse(LithuanianRules.blocksAsCompoundPrefix("gerai", true))
        assertFalse(LithuanianRules.isPlausibleVerbInflection("skaitome") { true })
        assertFalse(LithuanianRules.isPlausibleAdjectiveComparative("didesnis") { true })
        assertNull(LithuanianRules.splitCompound("raktazodis", { true }) { it })
    }
    
    @Test
    fun `Malay resolves to MalayRules`() {
        assertSame(MalayRules, LanguageRulesRegistry.rulesFor(Language.MALAY))
    }
    
    @Test
    fun `Malay does not glue a decimal comma`() {
        assertFalse(MalayRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Malay has no time-suggestion word`() {
        assertNull(MalayRules.timeSuggestionWord())
    }
    
    @Test
    fun `Malay curates no bundled blacklist yet`() {
        assertTrue(MalayRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Malay leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(MalayRules.blocksAsSplitPrefix("tidak", 0L))
        assertFalse(MalayRules.blocksAsFeminineAgentException("wati", "guru", true))
        assertFalse(MalayRules.blocksAsCompoundPrefix("baik", true))
        assertFalse(MalayRules.isPlausibleVerbInflection("membaca") { true })
        assertFalse(MalayRules.isPlausibleAdjectiveComparative("lebih_besar") { true })
        assertNull(MalayRules.splitCompound("kata_kunci", { true }) { it })
    }
    
    @Test
    fun `Indonesian resolves to IndonesianRules`() {
        assertSame(IndonesianRules, LanguageRulesRegistry.rulesFor(Language.INDONESIAN))
    }
    
    @Test
    fun `Indonesian glues a decimal comma`() {
        assertTrue(IndonesianRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Indonesian has no time-suggestion word`() {
        assertNull(IndonesianRules.timeSuggestionWord())
    }
    
    @Test
    fun `Indonesian curates no bundled blacklist yet`() {
        assertTrue(IndonesianRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Indonesian leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(IndonesianRules.blocksAsSplitPrefix("tidak", 0L))
        assertFalse(IndonesianRules.blocksAsFeminineAgentException("wati", "guru", true))
        assertFalse(IndonesianRules.blocksAsCompoundPrefix("baik", true))
        assertFalse(IndonesianRules.isPlausibleVerbInflection("membaca") { true })
        assertFalse(IndonesianRules.isPlausibleAdjectiveComparative("lebih_besar") { true })
        assertNull(IndonesianRules.splitCompound("kata_kunci", { true }) { it })
    }
    
    @Test
    fun `Swahili resolves to SwahiliRules`() {
        assertSame(SwahiliRules, LanguageRulesRegistry.rulesFor(Language.SWAHILI))
    }
    
    @Test
    fun `Swahili does not glue a decimal comma`() {
        assertFalse(SwahiliRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Swahili has no time-suggestion word`() {
        assertNull(SwahiliRules.timeSuggestionWord())
    }
    
    @Test
    fun `Swahili curates no bundled blacklist yet`() {
        assertTrue(SwahiliRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Swahili leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(SwahiliRules.blocksAsSplitPrefix("hapana", 0L))
        assertFalse(SwahiliRules.blocksAsFeminineAgentException("kazi", "mwalimu", true))
        assertFalse(SwahiliRules.blocksAsCompoundPrefix("vizuri", true))
        assertFalse(SwahiliRules.isPlausibleVerbInflection("tunasoma") { true })
        assertFalse(SwahiliRules.isPlausibleAdjectiveComparative("kubwa_zaidi") { true })
        assertNull(SwahiliRules.splitCompound("neno_muhimu", { true }) { it })
    }
    
    @Test
    fun `Tagalog resolves to TagalogRules`() {
        assertSame(TagalogRules, LanguageRulesRegistry.rulesFor(Language.TAGALOG))
    }
    
    @Test
    fun `Tagalog does not glue a decimal comma`() {
        assertFalse(TagalogRules.decimalCommaGluesDigits())
    }
    
    @Test
    fun `Tagalog has no time-suggestion word`() {
        assertNull(TagalogRules.timeSuggestionWord())
    }
    
    @Test
    fun `Tagalog curates no bundled blacklist yet`() {
        assertTrue(TagalogRules.bundledConfusablesBlacklist().isEmpty())
    }
    
    @Test
    fun `Tagalog leaves every German-specific compounding-grammar hook a no-op`() {
        assertFalse(TagalogRules.blocksAsSplitPrefix("hindi", 0L))
        assertFalse(TagalogRules.blocksAsFeminineAgentException("guro", "titser", true))
        assertFalse(TagalogRules.blocksAsCompoundPrefix("mabuti", true))
        assertFalse(TagalogRules.isPlausibleVerbInflection("babasahin") { true })
        assertFalse(TagalogRules.isPlausibleAdjectiveComparative("mas_malaki") { true })
        assertNull(TagalogRules.splitCompound("susing_salita", { true }) { it })
    }
}

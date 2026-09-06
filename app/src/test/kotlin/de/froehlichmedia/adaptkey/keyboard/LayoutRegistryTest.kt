// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import de.froehlichmedia.adaptkey.language.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** Unit tests for [LayoutRegistry]. */
class LayoutRegistryTest {
    
    @Test
    fun `kindFor a language alone returns its own compiled-in layout, QWERTY for everything else`() {
        assertEquals(LayoutKind.LATIN_QWERTZ, LayoutRegistry.kindFor(Language.GERMAN))
        assertEquals(LayoutKind.LATIN_AZERTY, LayoutRegistry.kindFor(Language.FRENCH))
        assertEquals(LayoutKind.LATIN_AZERBAIJANI, LayoutRegistry.kindFor(Language.AZERBAIJANI))
        assertEquals(LayoutKind.GREEK, LayoutRegistry.kindFor(Language.GREEK))
        assertEquals(LayoutKind.SERBIAN_CYRILLIC, LayoutRegistry.kindFor(Language.SERBIAN))
        assertEquals(LayoutKind.RUSSIAN_CYRILLIC, LayoutRegistry.kindFor(Language.RUSSIAN))
        assertEquals(LayoutKind.UKRAINIAN_CYRILLIC, LayoutRegistry.kindFor(Language.UKRAINIAN))
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Language.ENGLISH))
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Language.SPANISH))
        // Uzbek needs no new layout at all - its own real standard is Latin-QWERTY-compatible (confirmed via
        // research, not assumed - see the D-450-followup session note in AdaptKey-Progress.md).
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Language.UZBEK))
    }
    
    @Test
    fun `scriptFor groups layouts by writing system, not by exact physical arrangement`() {
        assertEquals(Script.LATIN, LayoutRegistry.scriptFor(Language.GERMAN))
        assertEquals(Script.LATIN, LayoutRegistry.scriptFor(Language.FRENCH))
        assertEquals(Script.LATIN, LayoutRegistry.scriptFor(Language.AZERBAIJANI))
        assertEquals(Script.GREEK, LayoutRegistry.scriptFor(Language.GREEK))
        // Serbian, Russian, and Ukrainian each have their own distinct LayoutKind (real, different physical
        // layouts - see JcukenLayout's own KDoc for why Serbian is NOT a variant of the other two) but all
        // three share one Script.
        assertEquals(Script.CYRILLIC, LayoutRegistry.scriptFor(Language.SERBIAN))
        assertEquals(Script.CYRILLIC, LayoutRegistry.scriptFor(Language.RUSSIAN))
        assertEquals(Script.CYRILLIC, LayoutRegistry.scriptFor(Language.UKRAINIAN))
    }
    
    @Test
    fun `NON_LATIN_LANGUAGES contains every non-Latin script`() {
        assertEquals(
            setOf(Language.GREEK, Language.SERBIAN, Language.RUSSIAN, Language.UKRAINIAN),
            LayoutRegistry.NON_LATIN_LANGUAGES
        )
    }
    
    @Test
    fun `CYRILLIC_LANGUAGES is exactly the Script CYRILLIC subset of NON_LATIN_LANGUAGES`() {
        assertEquals(setOf(Language.SERBIAN, Language.RUSSIAN, Language.UKRAINIAN), LayoutRegistry.CYRILLIC_LANGUAGES)
        assertEquals(
            LayoutRegistry.NON_LATIN_LANGUAGES.filter { LayoutRegistry.scriptFor(it) == Script.CYRILLIC }.toSet(),
            LayoutRegistry.CYRILLIC_LANGUAGES
        )
    }
    
    @Test
    fun `D-450-followup resolved - the Cyrillic-sibling gap this canary used to document now has real data behind it`() {
        // This test replaces the original gap-documenting canary (it asserted the three Cyrillic siblings'
        // shared Script with a comment explaining WHY AdaptKeyService.resolveDict() could not yet tell them
        // apart: no per-language Cyrillic language_profiles.tsv trigram data existed for any of them). That
        // gap is now closed - Russian's and Ukrainian's own real dict.tsv-derived profiles landed with their
        // language packs (§442/§443), and Serbian's own profile (deliberately skipped when its pack shipped,
        // §440) was built the same way once the classifier this test set out to unblock actually needed it.
        // AdaptKeyService.resolveDict() now runs its own Cyrillic-scoped LanguageClassifier instance (built
        // from exactly CYRILLIC_LANGUAGES's own profiles) instead of trusting the active language
        // unconditionally - see that function's own KDoc for the real mechanism. Nothing here can directly
        // exercise resolveDict() itself (Android Service internals stay instrumented-test territory, per this
        // project's own established policy), but this file's own LayoutRegistry surface - CYRILLIC_LANGUAGES
        // existing at all - is the real, pure prerequisite that test once waited on, so asserting it here
        // keeps this the one place a future reader finds the whole story.
        assertEquals(3, LayoutRegistry.CYRILLIC_LANGUAGES.size)
    }
    
    @Test
    fun `D-400 the layout follows the system language, not the active dictionary language`() {
        // System German, active English (D-130 auto-switch or a manual G-01 swipe) - stays QWERTZ.
        assertEquals(LayoutKind.LATIN_QWERTZ, LayoutRegistry.kindFor(Locale.GERMANY, Language.ENGLISH))
        // System German, active French - still QWERTZ, not AZERTY: the system language always wins for an
        // ordinary Latin-to-Latin switch.
        assertEquals(LayoutKind.LATIN_QWERTZ, LayoutRegistry.kindFor(Locale.GERMANY, Language.FRENCH))
        // System French, active German - AZERTY, matching the system language.
        assertEquals(LayoutKind.LATIN_AZERTY, LayoutRegistry.kindFor(Locale.FRANCE, Language.GERMAN))
        // System English (no dedicated entry), active German - falls back to the shared QWERTY default.
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Locale.US, Language.GERMAN))
    }
    
    @Test
    fun `D-400 an explicit switch into a non-Latin active language always wins, regardless of system language`() {
        assertEquals(LayoutKind.GREEK, LayoutRegistry.kindFor(Locale.GERMANY, Language.GREEK))
        assertEquals(LayoutKind.GREEK, LayoutRegistry.kindFor(Locale("el"), Language.GREEK))
        assertEquals(LayoutKind.SERBIAN_CYRILLIC, LayoutRegistry.kindFor(Locale.GERMANY, Language.SERBIAN))
        assertEquals(LayoutKind.SERBIAN_CYRILLIC, LayoutRegistry.kindFor(Locale("sr"), Language.SERBIAN))
    }
    
    @Test
    fun `D-400 a non-Latin or unrecognised system language falls back to the active language's own layout`() {
        // System Greek, explicitly switched (G-01) to German or French - lands on that language's own real
        // layout directly, since "follow the system language" has no sensible Latin answer to give here.
        assertEquals(LayoutKind.LATIN_QWERTZ, LayoutRegistry.kindFor(Locale("el"), Language.GERMAN))
        assertEquals(LayoutKind.LATIN_AZERTY, LayoutRegistry.kindFor(Locale("el"), Language.FRENCH))
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Locale("el"), Language.ENGLISH))
        // A system language this app has no Language entry for at all behaves the same way.
        assertEquals(LayoutKind.LATIN_QWERTZ, LayoutRegistry.kindFor(Locale("ja"), Language.GERMAN))
    }
}

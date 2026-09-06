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
    fun `D-450-followup real, deliberate gap - Cyrillic siblings share a script but resolveDict() cannot yet tell them apart`() {
        // The original canary here asserted no two NON_LATIN_LANGUAGES shared a LayoutKind, to force a
        // conscious decision the day a second same-script language arrived. That day is this one: Serbian,
        // Russian, and Ukrainian are all genuinely Script.CYRILLIC (confirmed above), each with its own real,
        // distinct LayoutKind. The decision, made here rather than silently: AdaptKeyService.resolveDict()
        // still trusts whichever one is active UNCONDITIONALLY - not because the gap doesn't exist, but
        // because there is genuinely nothing to distinguish them WITH yet (no per-language Cyrillic
        // language_profiles.tsv trigram data exists for any of the three - that is dictionary-pipeline work,
        // deliberately out of scope for this layouts-only round, see AdaptKey-Progress.md's own Open TODOs).
        // This test documents the real invariant that decision rests on - it fails the moment it stops being
        // true, which is exactly when a real same-script classifier becomes buildable and resolveDict()'s
        // own shortcut should be revisited for real.
        val cyrillic = LayoutRegistry.NON_LATIN_LANGUAGES.filter { LayoutRegistry.scriptFor(it) == Script.CYRILLIC }
        assertEquals(setOf(Language.SERBIAN, Language.RUSSIAN, Language.UKRAINIAN), cyrillic.toSet())
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

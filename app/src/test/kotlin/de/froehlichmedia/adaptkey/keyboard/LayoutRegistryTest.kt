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
        assertEquals(LayoutKind.GREEK, LayoutRegistry.kindFor(Language.GREEK))
        assertEquals(LayoutKind.SERBIAN_CYRILLIC, LayoutRegistry.kindFor(Language.SERBIAN))
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Language.ENGLISH))
        assertEquals(LayoutKind.LATIN_QWERTY, LayoutRegistry.kindFor(Language.SPANISH))
    }
    
    @Test
    fun `NON_LATIN_LANGUAGES contains every non-Latin script`() {
        assertEquals(setOf(Language.GREEK, Language.SERBIAN), LayoutRegistry.NON_LATIN_LANGUAGES)
    }
    
    @Test
    fun `D-450-followup no two languages share a non-Latin layout kind yet`() {
        // AdaptKeyService.resolveDict() trusts any NON_LATIN_LANGUAGES member's own dictionary
        // unconditionally, precisely BECAUSE no two of them share a LayoutKind today - there is nothing a
        // same-script classifier could distinguish between yet (see that function's own D-450-followup
        // KDoc). This test is the canary: it fails the moment a second language shares an existing non-Latin
        // LayoutKind (e.g. a second Cyrillic language joining Serbian), which is exactly when resolveDict's
        // own unconditional-trust shortcut must be revisited and given a real same-script classifier instead.
        val kinds = LayoutRegistry.NON_LATIN_LANGUAGES.map { LayoutRegistry.kindFor(it) }
        assertEquals(
            kinds.size, kinds.toSet().size,
            "a second language now shares a non-Latin LayoutKind - AdaptKeyService.resolveDict()'s " +
                "unconditional non-Latin trust must be revisited with a real same-script classifier"
        )
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

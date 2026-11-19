// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.suggestion.SuggestionController.DisplayItem
import de.froehlichmedia.adaptkey.suggestion.SuggestionController.Kind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * D-546: the typing-time placement of the S-11 dual-casing chips. The reported regression ("jetzt"
 * komplett aus den Chips verdrängt, weil das gelernte "Jetzt" das zusammengeführte Wort §6-regel-5-
 * ambig macht und die Chips ans Listenende verbannt wurden, wo der Slot-Budget-Schnitt sie abschneidet)
 * pinnt die neue Form: die Chips nehmen den eigenen Rangplatz ihres Kandidaten ein, klein zuerst; nur
 * ein Kandidat, der die Leiste nie erreicht hat, behält die historische Rückfallform hintsan.
 */
class AmbiguousCasingPlacementTest {

    private fun item(word: String, kind: Kind = Kind.NORMAL, pinned: Boolean = false) =
        DisplayItem(text = word, kind = kind, word = word, pinned = pinned)

    private fun chip(word: String) = DisplayItem(text = word, kind = Kind.AMBIGUOUS_CASE, word = word)

    @Test
    fun `prefix chips take their candidate's own rank, lower first`() {
        val items = listOf(item("Jet"), item("Jetzt"), item("Jets"), item("Netz"))
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("Jet", "jetzt", "Jetzt", "Jets", "Netz"), out.map { it.text })
        assertEquals(Kind.AMBIGUOUS_CASE, out[1].kind)
        assertEquals(Kind.AMBIGUOUS_CASE, out[2].kind)
    }

    @Test
    fun `a fuzzy twin of the same key is covered by the one chip pair, never duplicated`() {
        // Both casings can rank as separate candidates (the bundled lowercase via the fuzzy path, the
        // merged learned-cased entry via the prefix scan) - the pair replaces both.
        val items = listOf(item("Jetzt"), item("jetzt"), item("Netz"))
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("jetzt", "Jetzt", "Netz"), out.map { it.text })
    }

    @Test
    fun `an exact-match alternate inserts in place when its candidate is displayed`() {
        // Typed "jetzt" in full: the merged candidate "Jetzt" survives S-02 (case-sensitively different),
        // and the single alternate chip takes its slot - the typed spelling itself is never re-offered.
        val items = listOf(item("Jet"), item("Jetzt"), item("Jets"))
        val chips = listOf(chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("Jet", "Jetzt", "Jets"), out.map { it.text })
        assertEquals(Kind.AMBIGUOUS_CASE, out[1].kind)
    }

    @Test
    fun `an alternate whose candidate was never displayed keeps the back-append shape`() {
        // Typed "Jetzt" exactly: S-02 removed the candidate as the typed spelling itself, so the
        // alternate has no slot to take and falls to the back, exactly as before D-546.
        val items = listOf(item("Jet"), item("Jets"))
        val chips = listOf(chip("jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("Jet", "Jets", "jetzt"), out.map { it.text })
    }

    @Test
    fun `a pinned ambiguous continuation expands at its own leading slot - D-548`() {
        // Supersedes the v1.2.85 single-casing exemption: since D-546 the pair takes its candidate's own
        // slot, and for a pinned continuation that slot is the bar's FRONT. Letting the pinned item pass
        // through single-cased while the pair lands later duplicated it (the reported "Jetzt" an
        // Position 2 UND 6).
        val items = listOf(item("Grüße", pinned = true), item("anderes"))
        val chips = listOf(chip("grüße"), chip("Grüße"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("grüße", "Grüße", "anderes"), out.map { it.text })
        assertEquals(Kind.AMBIGUOUS_CASE, out[0].kind)
        assertEquals(Kind.AMBIGUOUS_CASE, out[1].kind)
    }

    @Test
    fun `the pinned casing and a later twin collapse into one pair, never duplicated`() {
        // The reported shape: the pinned merged candidate ("Jetzt", from the bigram continuation path)
        // sat unreplaced at slot 2 while the pair built from its non-pinned fuzzy twin landed at 5-6 -
        // "Jetzt" twice, lowercase late. The pair now takes the pinned slot and the twin is swallowed.
        val items = listOf(item("Jetzt", pinned = true), item("Netz"), item("jetzige"), item("jetzt"))
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("jetzt", "Jetzt", "Netz", "jetzige"), out.map { it.text })
    }

    @Test
    fun `non-normal kinds are never replaced`() {
        val items = listOf(item("jetzt", kind = Kind.CLIPBOARD), item("anderes"))
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("jetzt", "anderes", "Jetzt"), out.map { it.text })
        assertEquals(Kind.CLIPBOARD, out[0].kind)
    }

    @Test
    fun `the pending-correction replacement is replaced by the casing pair at its slot`() {
        // D-546 residual, pinned: while typing "jetz" the tier-1 autocorrect to the merged "Jetzt"/
        // "jetzt" word is PENDING (MEDIUM), so controller.displayed() spends its S-06 verbatim +
        // replacement chips on it and the D-364 dedupe removes the ranked candidate - the only bar item
        // carrying the word is the replacement preview, and the casing pair must take ITS slot, not fall
        // to the back where the slot budget cut them (the reported "komplett verdrängt" shape).
        val items = listOf(
            DisplayItem(text = "\"jetz\"", kind = Kind.VERBATIM, word = "jetz"),
            DisplayItem(text = "jetzt", kind = Kind.NORMAL, word = "jetzt"),
            item("Jet"), item("Jets"), item("ist")
        )
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("\"jetz\"", "jetzt", "Jetzt", "Jet", "Jets", "ist"), out.map { it.text })
        assertEquals(Kind.AMBIGUOUS_CASE, out[1].kind)
        assertEquals(Kind.AMBIGUOUS_CASE, out[2].kind)
    }

    @Test
    fun `at a sentence start the replacement's casing stays first and the lowercase twin follows`() {
        // Same S-06 shape with the shift-armed spelling: the replacement preview carries "Jetzt"
        // (explicitFirstUpper, rule 1), the alternate lowercase chip follows it directly.
        val items = listOf(
            DisplayItem(text = "\"Jetz\"", kind = Kind.VERBATIM, word = "Jetz"),
            DisplayItem(text = "Jetzt", kind = Kind.NORMAL, word = "Jetzt"),
            item("Jet"), item("Jets")
        )
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 8)

        assertEquals(listOf("\"Jetz\"", "jetzt", "Jetzt", "Jet", "Jets"), out.map { it.text })
    }

    @Test
    fun `the bar stays capped at the slot budget`() {
        val items = listOf(item("Jet"), item("Jetzt"), item("Jets"), item("Netz"))
        val chips = listOf(chip("jetzt"), chip("Jetzt"))

        val out = AmbiguousCasingPlacement.insertInPlace(items, chips, maxSuggestions = 3)

        assertEquals(listOf("Jet", "jetzt", "Jetzt"), out.map { it.text })
    }

    @Test
    fun `no chips means no change at all`() {
        val items = listOf(item("Jet"), item("Jetzt"))

        assertEquals(items, AmbiguousCasingPlacement.insertInPlace(items, emptyList(), maxSuggestions = 8))
    }
}

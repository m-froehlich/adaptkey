// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * D-557: the S-06 pending preview's contract under an A-03 foreign verdict - the one bar element that
 * stays gated now that the verdict no longer empties the bar. The preview may only ever promise what
 * the commit side will actually apply: the capitalisation-only change in a foreign context (A-03 never
 * gated §6 capitalisation), the autocorrect replacement only in an ordinary one.
 */
class PendingPreviewTest {

    private val identity: (String) -> String = { it }

    private val capitaliseFirst: (String) -> String = { it.replaceFirstChar { c -> c.uppercaseChar() } }

    @Test
    fun `ordinary context previews the background replacement`() {
        assertEquals("mit", PendingPreview.resolve("mitx", "mit", foreignContext = false, capitalise = identity))
    }

    @Test
    fun `ordinary context without a replacement previews the capitalised input`() {
        assertEquals("Brot", PendingPreview.resolve("brot", null, foreignContext = false, capitalise = capitaliseFirst))
    }

    @Test
    fun `foreign context ignores the background replacement - it may never promise a rewrite commit will not apply`() {
        assertEquals("Brot", PendingPreview.resolve("brot", "Brett", foreignContext = true, capitalise = capitaliseFirst))
    }

    @Test
    fun `foreign context keeps the capitalisation-only preview - commit applies it unconditionally`() {
        assertEquals("Brot", PendingPreview.resolve("brot", null, foreignContext = true, capitalise = capitaliseFirst))
    }

    @Test
    fun `a preview identical to the typed token is not offered`() {
        assertNull(PendingPreview.resolve("brot", "brot", foreignContext = false, capitalise = identity))
        assertNull(PendingPreview.resolve("brot", null, foreignContext = true, capitalise = identity))
    }

    @Test
    fun `D-558 a competing lowercase reading withholds the bare token's capitalisation-only preview`() {
        // The commit side withholds rule 3's capital for such a token (CapitalisationEngine's
        // lowercaseCompetitor gate), so the S-06 pair - the verbatim chip exists only for a pending
        // replacement - must not promise a "Mut" the commit will never apply.
        assertNull(
            PendingPreview.resolve(
                "mut", null, foreignContext = false, lowercaseCompetitor = "mit", capitalise = capitaliseFirst
            )
        )
    }

    @Test
    fun `D-558 without a competitor the bare token's capitalisation-only preview is unchanged`() {
        assertEquals(
            "Mut",
            PendingPreview.resolve("mut", null, foreignContext = false, lowercaseCompetitor = null, capitalise = capitaliseFirst)
        )
    }

    @Test
    fun `D-558 a real replacement's preview is not withheld by the competitor gate`() {
        // The competitor concerns the bare token's own case-only preview; a genuine correction the
        // commit will apply is previewed with its casing, exactly as D-112 promises.
        assertEquals(
            "Mutter",
            PendingPreview.resolve(
                "mutterx", "mutter", foreignContext = false, lowercaseCompetitor = "mit", capitalise = capitaliseFirst
            )
        )
    }
}

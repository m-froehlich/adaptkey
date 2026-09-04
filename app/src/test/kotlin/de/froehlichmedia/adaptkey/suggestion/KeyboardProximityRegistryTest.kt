// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.keyboard.LayoutKind
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** Unit tests for [KeyboardProximityRegistry] - every [LayoutKind] must resolve its own real implementation. */
class KeyboardProximityRegistryTest {
    
    @Test
    fun `every LayoutKind resolves its own real implementation`() {
        assertSame(KeyboardProximityQwertz, KeyboardProximityRegistry.forLayoutKind(LayoutKind.LATIN_QWERTZ))
        assertSame(KeyboardProximityQwerty, KeyboardProximityRegistry.forLayoutKind(LayoutKind.LATIN_QWERTY))
        assertSame(KeyboardProximityAzerty, KeyboardProximityRegistry.forLayoutKind(LayoutKind.LATIN_AZERTY))
        assertSame(KeyboardProximityGreek, KeyboardProximityRegistry.forLayoutKind(LayoutKind.GREEK))
    }
}

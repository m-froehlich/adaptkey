// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * Unit tests for the inert default tier-3 backend.
 */
class NoopTier3ProviderTest {
    
    @Test
    fun `is never available`() {
        assertFalse(NoopTier3Provider.isAvailable)
    }
    
    @Test
    fun `always predicts the empty result`() {
        val result = NoopTier3Provider.predict(Tier3Request("wor", null, "das wor"))
        assertEquals(Tier3Result.EMPTY, result)
    }
}

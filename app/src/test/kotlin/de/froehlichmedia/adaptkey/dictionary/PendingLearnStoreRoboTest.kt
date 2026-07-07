// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) for the Android-facing [PendingLearnStore] IO (D-37): counting a
 * new word up, down and clearing it, backed by SharedPreferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingLearnStoreRoboTest {
    
    @Test
    fun incrementReturnsRisingCounts() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(1, PendingLearnStore.increment(context, "Backspace"))
        assertEquals(2, PendingLearnStore.increment(context, "backspace"))
        assertEquals(2, PendingLearnStore.count(context, "BACKSPACE"))
    }
    
    @Test
    fun decrementRemovesAtZero() {
        val context = RuntimeEnvironment.getApplication()
        PendingLearnStore.increment(context, "wort")
        PendingLearnStore.decrement(context, "wort")
        assertEquals(0, PendingLearnStore.count(context, "wort"))
        // Decrementing again stays at zero, no negative counts.
        PendingLearnStore.decrement(context, "wort")
        assertEquals(0, PendingLearnStore.count(context, "wort"))
    }
    
    @Test
    fun clearResetsTheCount() {
        val context = RuntimeEnvironment.getApplication()
        PendingLearnStore.increment(context, "haus")
        PendingLearnStore.increment(context, "haus")
        PendingLearnStore.clear(context, "haus")
        assertEquals(0, PendingLearnStore.count(context, "haus"))
    }
    
    @Test
    fun unknownWordCountsZero() {
        assertEquals(0, PendingLearnStore.count(RuntimeEnvironment.getApplication(), "nichts"))
    }
}

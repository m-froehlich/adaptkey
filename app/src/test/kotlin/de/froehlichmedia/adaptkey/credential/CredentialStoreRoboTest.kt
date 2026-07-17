// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) for the Android-facing [CredentialStore] IO (D-142): learning,
 * persistence, case-insensitive identity and clearing, backed by SharedPreferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CredentialStoreRoboTest {
    
    @Test
    fun learnCreatesAnEntryWithFrequencyOne() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "peter.mueller", LoginFieldKind.USERNAME)
        
        val entries = CredentialStore.all(context)
        assertEquals(1, entries.size)
        assertEquals("peter.mueller", entries[0].value)
        assertEquals(LoginFieldKind.USERNAME, entries[0].kind)
        assertEquals(1L, entries[0].frequency)
    }
    
    @Test
    fun learnAgainReinforcesTheSameEntryCaseInsensitively() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "Peter@example.com", LoginFieldKind.EMAIL)
        CredentialStore.learn(context, "peter@example.com", LoginFieldKind.EMAIL)
        
        val entries = CredentialStore.all(context)
        assertEquals(1, entries.size)
        assertEquals(2L, entries[0].frequency)
    }
    
    @Test
    fun aRepeatObservationUpdatesTheKind() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "peter", LoginFieldKind.USERNAME)
        CredentialStore.learn(context, "peter", LoginFieldKind.EMAIL)
        
        assertEquals(LoginFieldKind.EMAIL, CredentialStore.all(context).single().kind)
    }
    
    @Test
    fun passwordAndNoneAreNeverStored() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "hunter2", LoginFieldKind.PASSWORD)
        CredentialStore.learn(context, "irrelevant", LoginFieldKind.NONE)
        
        assertTrue(CredentialStore.isEmpty(context))
    }
    
    @Test
    fun blankValueIsNeverStored() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "   ", LoginFieldKind.USERNAME)
        
        assertTrue(CredentialStore.isEmpty(context))
    }
    
    @Test
    fun clearRemovesEveryEntry() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "a", LoginFieldKind.USERNAME)
        CredentialStore.learn(context, "b@example.com", LoginFieldKind.EMAIL)
        CredentialStore.clear(context)
        
        assertTrue(CredentialStore.isEmpty(context))
        assertTrue(CredentialStore.all(context).isEmpty())
    }
    
    @Test
    fun entriesSurviveARoundTripThroughPersistence() {
        val context = RuntimeEnvironment.getApplication()
        CredentialStore.learn(context, "user1", LoginFieldKind.USERNAME)
        CredentialStore.learn(context, "user2@example.com", LoginFieldKind.EMAIL)
        CredentialStore.learn(context, "user2@example.com", LoginFieldKind.EMAIL)
        
        val entries = CredentialStore.all(context).associateBy { it.value }
        assertEquals(2, entries.size)
        assertEquals(1L, entries.getValue("user1").frequency)
        assertEquals(2L, entries.getValue("user2@example.com").frequency)
        assertEquals(LoginFieldKind.EMAIL, entries.getValue("user2@example.com").kind)
    }
}

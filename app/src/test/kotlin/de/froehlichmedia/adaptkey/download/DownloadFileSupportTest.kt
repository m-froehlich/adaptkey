// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.download

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for [DownloadFileSupport]'s own pure age comparison. */
class DownloadFileSupportTest {
    
    @Test
    fun `a file created just now is recently created`() {
        assertTrue(DownloadFileSupport.isRecentlyCreated(lastModifiedMillis = 1000L, maxAgeMillis = 60_000L, now = 1000L))
    }
    
    @Test
    fun `a file exactly at the age ceiling counts as recently created (inclusive)`() {
        assertTrue(DownloadFileSupport.isRecentlyCreated(lastModifiedMillis = 0L, maxAgeMillis = 60_000L, now = 60_000L))
    }
    
    @Test
    fun `a file one millisecond past the age ceiling is not recently created`() {
        assertFalse(DownloadFileSupport.isRecentlyCreated(lastModifiedMillis = 0L, maxAgeMillis = 60_000L, now = 60_001L))
    }
    
    @Test
    fun `a file far older than the ceiling is not recently created`() {
        assertFalse(DownloadFileSupport.isRecentlyCreated(lastModifiedMillis = 0L, maxAgeMillis = 60_000L, now = 3_600_000L))
    }
}

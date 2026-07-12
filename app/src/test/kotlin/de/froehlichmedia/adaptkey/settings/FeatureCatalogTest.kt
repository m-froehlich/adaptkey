// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the D-89 feature catalog data itself, independent of the Android glue that renders it
 * ([FeatureOverviewActivity], covered separately by Robolectric).
 */
class FeatureCatalogTest {
    
    @Test
    fun `the catalog is not empty and every entry has a title and a description`() {
        assertTrue(FeatureCatalog.ENTRIES.isNotEmpty())
        FeatureCatalog.ENTRIES.forEach { entry ->
            assertTrue(entry.titleResId != 0)
            assertTrue(entry.descriptionResId != 0)
        }
    }
    
    @Test
    fun `no entry repeats the same title or description resource`() {
        val titleIds = FeatureCatalog.ENTRIES.map { it.titleResId }
        val descriptionIds = FeatureCatalog.ENTRIES.map { it.descriptionResId }
        
        assertEquals(titleIds.size, titleIds.toSet().size)
        assertEquals(descriptionIds.size, descriptionIds.toSet().size)
    }
}

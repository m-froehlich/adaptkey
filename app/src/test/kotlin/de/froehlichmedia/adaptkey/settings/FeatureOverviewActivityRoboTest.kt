// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.widget.LinearLayout
import de.froehlichmedia.adaptkey.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric test (JVM, no emulator) driving the real [FeatureOverviewActivity] UI, to catch a lifecycle-
 * or inflation-specific bug that [FeatureCatalogTest]'s pure-data checks cannot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeatureOverviewActivityRoboTest {
    
    @Test
    fun `renders a title and description view for every catalog entry`() {
        val activity = Robolectric.buildActivity(FeatureOverviewActivity::class.java).setup().get()
        
        val list = activity.findViewById<LinearLayout>(R.id.feature_overview_list)
        assertEquals(FeatureCatalog.ENTRIES.size, list.childCount)
        val firstEntry = list.getChildAt(0) as LinearLayout
        assertEquals(2, firstEntry.childCount)
    }
}

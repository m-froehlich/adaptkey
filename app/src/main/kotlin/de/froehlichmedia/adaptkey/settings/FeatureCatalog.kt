// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.R

/**
 * D-89: the full list of user-facing AdaptKey features, shown on [FeatureOverviewActivity]. Kept as plain
 * data (rather than built inline in the activity) so the catalog itself - its size and the pairing of each
 * title with its description - is unit-testable without Robolectric.
 */
object FeatureCatalog {
    
    /**
     * One entry in the overview: a short feature name and its one- or two-sentence explanation.
     *
     * @property titleResId string resource id of the feature's name
     * @property descriptionResId string resource id of the feature's explanation
     */
    data class Entry(val titleResId: Int, val descriptionResId: Int)
    
    /** All features, in display order. */
    val ENTRIES: List<Entry> = listOf(
        Entry(R.string.d89_f01_title, R.string.d89_f01_desc),
        Entry(R.string.d89_f02_title, R.string.d89_f02_desc),
        Entry(R.string.d89_f03_title, R.string.d89_f03_desc),
        Entry(R.string.d89_f04_title, R.string.d89_f04_desc),
        Entry(R.string.d89_f05_title, R.string.d89_f05_desc),
        Entry(R.string.d89_f06_title, R.string.d89_f06_desc),
        Entry(R.string.d89_f07_title, R.string.d89_f07_desc),
        Entry(R.string.d89_f08_title, R.string.d89_f08_desc),
        Entry(R.string.d89_f09_title, R.string.d89_f09_desc),
        Entry(R.string.d89_f10_title, R.string.d89_f10_desc),
        Entry(R.string.d89_f11_title, R.string.d89_f11_desc),
        Entry(R.string.d89_f12_title, R.string.d89_f12_desc),
        Entry(R.string.d89_f13_title, R.string.d89_f13_desc),
        Entry(R.string.d89_f14_title, R.string.d89_f14_desc),
        Entry(R.string.d89_f15_title, R.string.d89_f15_desc),
        Entry(R.string.d89_f16_title, R.string.d89_f16_desc),
        Entry(R.string.d89_f17_title, R.string.d89_f17_desc),
        Entry(R.string.d89_f18_title, R.string.d89_f18_desc)
    )
}

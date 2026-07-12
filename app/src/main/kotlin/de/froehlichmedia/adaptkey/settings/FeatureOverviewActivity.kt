// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R

/**
 * D-89: a full, scrollable overview of every AdaptKey feature (see [FeatureCatalog]), so a feature a user
 * never happened to discover on their own does not simply stay hidden. Launched from the settings screen's
 * "Info & Privacy" category.
 */
class FeatureOverviewActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_overview)
        title = getString(R.string.d89_title)
        
        val list = findViewById<LinearLayout>(R.id.feature_overview_list)
        val topMarginPx = (ENTRY_SPACING_DP * resources.displayMetrics.density).toInt()
        FeatureCatalog.ENTRIES.forEach { entry -> list.addView(buildEntryView(entry, topMarginPx)) }
    }
    
    private fun buildEntryView(entry: FeatureCatalog.Entry, topMarginPx: Int): LinearLayout {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = topMarginPx }
        
        val titleView = TextView(this)
        titleView.setText(entry.titleResId)
        titleView.setTypeface(titleView.typeface, Typeface.BOLD)
        
        val descriptionView = TextView(this)
        descriptionView.setText(entry.descriptionResId)
        
        container.addView(titleView)
        container.addView(descriptionView)
        return container
    }
    
    companion object {
        
        private const val ENTRY_SPACING_DP = 16f
    }
}

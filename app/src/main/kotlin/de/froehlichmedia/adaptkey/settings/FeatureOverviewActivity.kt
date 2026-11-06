// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R

/**
 * D-89: a full, scrollable overview of every AdaptKey feature (see [FeatureCatalog]), so a feature a user
 * never happened to discover on their own does not simply stay hidden. Launched from the settings screen's
 * "Info & Privacy" category.
 */
class FeatureOverviewActivity : SettingsScreenActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_overview)
        title = getString(R.string.d89_title)
        
        // D-478 (found while reviewing the app's UI): the only sub-screen without any inset handling - with
        // targetSdk 35's enforced edge-to-edge its heading slid under the status bar / display cutout. Same fix
        // as LanguagePacksActivity's own D-188 one: the root's own padding plus the system-bar/cutout insets.
        val root = findViewById<View>(R.id.feature_overview_root)
        val basePadding = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            v.setPadding(
                basePadding,
                basePadding,
                basePadding,
                basePadding + maxOf(navBars.bottom, gestures.bottom)
            )
            insets
        }
        
        val list = findViewById<LinearLayout>(R.id.feature_overview_list)
        val topMarginPx = (ENTRY_SPACING_DP * resources.displayMetrics.density).toInt()
        FeatureCatalog.ENTRIES.forEach { entry -> list.addView(buildEntryView(entry, topMarginPx)) }
        // D-518: the trailing note (a deliberate afterword, not a numbered feature entry) - since the
        // Alt-Belegungs-Reform the letters page itself carries practically every relevant character,
        // so everyday typing never needs the special-character pages. Italic, extra spacing: reads as
        // an editorial note, clearly apart from the numbered entries above.
        val note = TextView(this)
        note.setText(R.string.d89_footer_note)
        note.setTypeface(note.typeface, Typeface.ITALIC)
        val noteParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (NOTE_SPACING_DP * resources.displayMetrics.density).toInt() }
        note.layoutParams = noteParams
        list.addView(note)
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
        
        // D-518: the trailing note's extra separation from the last numbered entry.
        private const val NOTE_SPACING_DP = 24f
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

/**
 * D-484: common base of every settings screen (the settings root and its nine sub-screens). The app theme
 * is `NoActionBar`, so none of these screens had any visible title or way back other than the system back
 * gesture - users found that odd. This class wraps whatever layout the subclass passes to [setContentView]
 * in a vertical container with a [MaterialToolbar] on top, registered as the support action bar so the
 * activity's own `title` is shown automatically and the up arrow (with AppCompat's own, already localised
 * "Navigate up" description) comes for free.
 * 
 * The toolbar also owns the top system-bar/cutout inset, which is why the sub-screens' own edge-to-edge
 * listeners (D-188/D-151/D-80) no longer pad their content for the status bar - otherwise the gap would
 * appear twice. Their bottom (navigation bar/gesture) handling is unchanged. The insets are passed on
 * unmodified, so those listeners keep receiving the full values.
 * 
 * The settings root shows the title only ([showUpButton] = false): back there leaves the app.
 */
abstract class SettingsScreenActivity : AppCompatActivity() {
    
    /** Whether the toolbar shows an up arrow; only the settings root turns it off. */
    protected open val showUpButton: Boolean
        get() = true
    
    override fun setContentView(layoutResID: Int) {
        val content = layoutInflater.inflate(layoutResID, null)
        val toolbar = MaterialToolbar(this)
        val barHeight = actionBarHeightPx()
        
        val screen = LinearLayout(this)
        screen.orientation = LinearLayout.VERTICAL
        screen.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight))
        screen.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        
        // The toolbar grows by the status bar/cutout height and pads its own top by the same amount, so its
        // background also fills the area behind the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v: View, insets: WindowInsetsCompat ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val top = maxOf(statusBars.top, cutout.top)
            v.setPadding(v.paddingLeft, top, v.paddingRight, 0)
            v.layoutParams = v.layoutParams.apply { height = barHeight + top }
            insets
        }
        
        super.setContentView(screen)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showUpButton)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    private fun actionBarHeightPx(): Int {
        val value = TypedValue()
        return if (theme.resolveAttribute(android.R.attr.actionBarSize, value, true)) {
            TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
        } else {
            (56 * resources.displayMetrics.density).toInt()
        }
    }
}

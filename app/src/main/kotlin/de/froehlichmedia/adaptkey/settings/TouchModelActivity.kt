// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.touch.OffsetStore

/**
 * Touch-pattern visualisation (D-24): shows the keyboard with a coloured spread ellipse and a strike-point
 * dot over each trained key, so the user can see how their personal offset model (T-03) has learned to
 * compensate for where they actually tap. Reached from the settings screen and from a calibration result.
 *
 * As an Android-facing screen it is left to instrumented tests; the drawing maths live in the pure
 * [de.froehlichmedia.adaptkey.touch.OffsetModel.spreadFor] and the view.
 */
class TouchModelActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch_model)
        title = getString(R.string.d24_title)
        
        // Match the live keyboard's edge-to-edge bottom inset so the keys clear the gesture bar. D-80: the
        // top also needs the status bar / display cutout inset - without it, edge-to-edge drawing lets the
        // intro text start right under a front camera cutout.
        val root = findViewById<View>(R.id.touch_model_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0, maxOf(statusBars.top, cutout.top), 0, maxOf(bars.bottom, gestures.bottom))
            insets
        }
        
        val model = OffsetStore.load(this)
        val keyboard = findViewById<AdaptKeyboardView>(R.id.touch_model_keyboard)
        keyboard.offsetModel = model
        keyboard.showTouchModel = true
        
        // Hint the user to calibrate / keep typing when there is nothing learned yet.
        if (model.totalSamples == 0L) {
            findViewById<TextView>(R.id.touch_model_empty).visibility = View.VISIBLE
        }
    }
}

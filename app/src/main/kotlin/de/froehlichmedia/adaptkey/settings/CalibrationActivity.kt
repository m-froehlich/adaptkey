// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.PatternSeed
import de.froehlichmedia.adaptkey.touch.TypingPattern

/**
 * Typing-pattern picker (K-01, skippable), reachable from onboarding and any time later from settings.
 *
 * D-68: this used to be a three-sentence typing exercise whose result then had to be auto-classified into
 * a typing pattern (T-04) - too little data to classify reliably, and a wrong guess could seed some key
 * zones badly, taking a very long time for real typing to correct (the offset model has no forgetting
 * mechanism). Asking the pattern directly and deriving sensible initial per-key touch zones from it (see
 * [PatternSeed]) is both simpler and more reliable; ordinary typing then keeps refining those zones (T-03),
 * exactly as it already did before.
 *
 * A genuine pattern change **replaces** the whole personal offset model - the previous zones described a
 * different hand/finger geometry, so keeping them would only fight the new one.
 *
 * As an Android-facing layer it is left to instrumented tests; the testable logic lives in the pure
 * [PatternSeed].
 */
class CalibrationActivity : AppCompatActivity() {
    
    // Only used to obtain the laid-out char-key geometry that seeds the model - nothing is typed on it.
    private lateinit var keyboard: AdaptKeyboardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        title = getString(R.string.k01_activity_title)
        
        // §13 / K-01 fix: Android 15 (targetSdk 35) draws the activity edge-to-edge, so the embedded
        // keyboard's bottom row would sit under the gesture pill / navigation bar. Pad the whole screen up
        // by the bottom system-bar + gesture inset, exactly like the live keyboard's input view does.
        val root = findViewById<View>(R.id.calibration_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            v.setPadding(0, v.paddingTop, 0, maxOf(bars.bottom, gestures.bottom))
            insets
        }
        
        keyboard = findViewById(R.id.calibration_keyboard)
        // D-72: the number row must be present here too, or its keys are simply missing from
        // charKeyGeometry() below and PatternSeed never seeds them at all - the live keyboard shows the
        // number row by default (C-09), so most users would otherwise get zero digit-row calibration.
        keyboard.showNumberRow = true
        
        // D-73: ordered by real-world prevalence, matching the layout - both thumbs leads and is the
        // skip default.
        bindPatternButton(R.id.calibration_two_thumbs, TypingPattern.TWO_THUMBS)
        bindPatternButton(R.id.calibration_right_thumb, TypingPattern.RIGHT_THUMB)
        bindPatternButton(R.id.calibration_left_thumb, TypingPattern.LEFT_THUMB)
        bindPatternButton(R.id.calibration_right_index, TypingPattern.RIGHT_INDEX_FINGER)
        bindPatternButton(R.id.calibration_left_index, TypingPattern.LEFT_INDEX_FINGER)
        // D-73: skipping is not "do nothing" - by far the most common pattern (both thumbs) is applied
        // quietly (no feedback dialog, unlike an explicit choice) so an undecided user still ends up with
        // sensible touch zones instead of an unseeded, purely geometric model.
        findViewById<Button>(R.id.calibration_skip).setOnClickListener {
            persistPattern(TypingPattern.TWO_THUMBS)
            finish()
        }
    }
    
    private fun bindPatternButton(id: Int, pattern: TypingPattern) {
        findViewById<Button>(id).setOnClickListener { applyPattern(pattern) }
    }
    
    /**
     * D-68: seeds sensible initial touch zones straight from [pattern] and shows the confirmation dialog.
     * See [persistPattern] for the actual seeding.
     */
    private fun applyPattern(pattern: TypingPattern) {
        persistPattern(pattern)
        showFeedback(pattern)
    }
    
    /**
     * D-68: seeds sensible initial touch zones straight from the chosen pattern, replacing (not merging
     * with) any previously learned zones - a genuine pattern change describes a different hand/finger
     * geometry, so old data would only fight the new one. Also presets the D-16 key-enlargement default.
     * Split out from [applyPattern] so D-73's silent skip-default can reuse it without the feedback dialog.
     */
    private fun persistPattern(pattern: TypingPattern) {
        val geometry = keyboard.charKeyGeometry()
        val model = OffsetModel()
        model.restore(PatternSeed.seed(pattern, geometry))
        OffsetStore.save(this, model)
        OffsetStore.saveDetectedPattern(this, pattern)
        // D-16: preset the default key enlargement from the chosen hand (backspace for a left typist, shift
        // for a right typist); the user can still adjust it in the settings.
        SettingsStore.applyPatternEnlargement(this, pattern)
    }
    
    private fun showFeedback(pattern: TypingPattern) {
        AlertDialog.Builder(this)
            .setTitle(R.string.k01_done_title)
            .setMessage(feedbackText(pattern))
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setNeutralButton(R.string.d24_show_button) { _, _ -> showTouchModel() }
            .setOnCancelListener { finish() }
            .show()
    }
    
    /**
     * D-24: opens the touch-pattern visualisation so the seeded zones can be inspected right away, then
     * ends this screen.
     */
    private fun showTouchModel() {
        startActivity(Intent(this, TouchModelActivity::class.java))
        finish()
    }
    
    private fun feedbackText(pattern: TypingPattern): Int {
        return when (pattern) {
            TypingPattern.LEFT_INDEX_FINGER -> R.string.k01_feedback_left_index
            TypingPattern.RIGHT_INDEX_FINGER -> R.string.k01_feedback_right_index
            TypingPattern.LEFT_THUMB -> R.string.k01_feedback_left_thumb
            TypingPattern.RIGHT_THUMB -> R.string.k01_feedback_right_thumb
            TypingPattern.TWO_THUMBS -> R.string.k01_feedback_two_thumbs
            TypingPattern.UNKNOWN -> R.string.k01_feedback_unknown
        }
    }
}

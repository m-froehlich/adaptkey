// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.PatternSeed
import de.froehlichmedia.adaptkey.touch.TypingPattern

/**
 * Typing-pattern picker (K-01, skippable) and touch-zone visualisation (D-24), merged into one screen
 * (D-237) - reachable from onboarding and any time later from settings.
 *
 * D-68: this used to be a three-sentence typing exercise whose result then had to be auto-classified into
 * a typing pattern (T-04) - too little data to classify reliably, and a wrong guess could seed some key
 * zones badly, taking a very long time for real typing to correct (the offset model has no forgetting
 * mechanism). Asking the pattern directly and deriving sensible initial per-key touch zones from it (see
 * [PatternSeed]) is both simpler and more reliable; ordinary typing then keeps refining those zones (T-03),
 * exactly as it already did before.
 *
 * D-237: the currently selected pattern's button is highlighted, and the embedded keyboard always shows a
 * live D-24 zone overlay for the *actual persisted* model - there is no longer a separate "show touch
 * pattern" screen or setting. Tapping a style button previews that style's fresh seed live and asks for
 * confirmation before actually applying it (a genuine pattern change replaces the whole personal offset
 * model, since the previous zones described a different hand/finger geometry); cancelling reverts the
 * preview back to the actual persisted model. A separate, spatially distinct "reset" action re-seeds the
 * *currently selected* style's calibration back to its defaults without touching the style choice itself
 * (D-235's own reasoning: the style the user picked is not the thing that needs resetting when only the
 * learned drift has gone stale).
 *
 * As an Android-facing layer it is left to instrumented tests; the testable logic lives in the pure
 * [PatternSeed].
 */
class CalibrationActivity : AppCompatActivity() {
    
    // Also the live D-24 preview surface - shows the actual persisted model by default, or a fresh
    // (not-yet-applied) seed while a style switch is being confirmed.
    private lateinit var keyboard: AdaptKeyboardView
    private lateinit var emptyText: TextView
    private val patternButtons = HashMap<TypingPattern, Button>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        title = getString(R.string.k01_activity_title)
        
        // §13 / K-01 fix: Android 15 (targetSdk 35) draws the activity edge-to-edge, so the embedded
        // keyboard's bottom row would sit under the gesture pill / navigation bar. Pad the whole screen up
        // by the bottom system-bar + gesture inset, exactly like the live keyboard's input view does. D-80:
        // the top also needs the status bar / display cutout inset, or the intro text starts right under a
        // front camera cutout.
        val root = findViewById<View>(R.id.calibration_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0, maxOf(statusBars.top, cutout.top), 0, maxOf(bars.bottom, gestures.bottom))
            insets
        }
        
        keyboard = findViewById(R.id.calibration_keyboard)
        emptyText = findViewById(R.id.calibration_empty)
        // D-72: the number row must be present here too, or its keys are simply missing from
        // charKeyGeometry() below and PatternSeed never seeds them at all - the live keyboard shows the
        // number row by default (C-09), so most users would otherwise get zero digit-row calibration.
        keyboard.showNumberRow = true
        keyboard.showTouchModel = true
        
        patternButtons[TypingPattern.TWO_THUMBS] = findViewById(R.id.calibration_two_thumbs)
        patternButtons[TypingPattern.RIGHT_THUMB] = findViewById(R.id.calibration_right_thumb)
        patternButtons[TypingPattern.LEFT_THUMB] = findViewById(R.id.calibration_left_thumb)
        patternButtons[TypingPattern.RIGHT_INDEX_FINGER] = findViewById(R.id.calibration_right_index)
        patternButtons[TypingPattern.LEFT_INDEX_FINGER] = findViewById(R.id.calibration_left_index)
        // D-73: ordered by real-world prevalence, matching the layout - both thumbs leads and is the
        // skip default.
        for ((pattern, button) in patternButtons) {
            button.setOnClickListener { previewAndConfirm(pattern) }
        }
        // D-73: skipping is not "do nothing" - by far the most common pattern (both thumbs) is applied
        // quietly (no feedback dialog, unlike an explicit choice) so an undecided user still ends up with
        // sensible touch zones instead of an unseeded, purely geometric model.
        findViewById<Button>(R.id.calibration_skip).setOnClickListener {
            persistPattern(TypingPattern.TWO_THUMBS)
            finish()
        }
        findViewById<Button>(R.id.calibration_reset).setOnClickListener { confirmReset() }
        
        showActualModel()
    }
    
    /** Loads the real persisted model onto the live keyboard and highlights the actually-selected style. */
    private fun showActualModel() {
        val model = OffsetStore.load(this)
        keyboard.offsetModel = model
        emptyText.visibility = if (model.totalSamples == 0L) View.VISIBLE else View.GONE
        highlightButtonFor(OffsetStore.loadDetectedPattern(this))
        keyboard.invalidate()
    }
    
    private fun highlightButtonFor(pattern: TypingPattern) {
        val selectedTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.link_text))
        for ((candidate, button) in patternButtons) {
            button.backgroundTintList = if (candidate == pattern) selectedTint else null
        }
    }
    
    /**
     * D-237: previews [pattern]'s fresh seed live (without persisting it yet) and asks for confirmation
     * before actually applying it. Cancelling (either button or a back-press/outside-tap dismissal) reverts
     * the preview back to the actually persisted model - see [showActualModel].
     */
    private fun previewAndConfirm(pattern: TypingPattern) {
        val preview = OffsetModel()
        preview.restore(PatternSeed.seed(pattern, keyboard.charKeyGeometry()))
        keyboard.offsetModel = preview
        emptyText.visibility = View.GONE
        highlightButtonFor(pattern)
        keyboard.invalidate()
        AlertDialog.Builder(this)
            .setTitle(R.string.k01_switch_confirm_title)
            .setMessage(getString(R.string.k01_switch_confirm_message, patternLabel(pattern)))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                persistPattern(pattern)
                Toast.makeText(this, R.string.k01_switch_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> showActualModel() }
            .setOnCancelListener { showActualModel() }
            .show()
    }
    
    /**
     * D-68: seeds sensible initial touch zones straight from the chosen pattern, replacing (not merging
     * with) any previously learned zones - a genuine pattern change describes a different hand/finger
     * geometry, so old data would only fight the new one. Also presets the D-16 key-enlargement default.
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
        keyboard.offsetModel = model
        highlightButtonFor(pattern)
        keyboard.invalidate()
    }
    
    /**
     * D-237: double-confirmed reset of the *currently selected* style's own learned touch zones back to
     * its fresh [PatternSeed] defaults - deliberately never touches which style is selected, unlike the old
     * top-level "reset learning &amp; calibration" action this replaces (see history for the reasoning:
     * resetting stale drift is a completely different concern from re-choosing a typing style). Kept behind
     * two dialogs so it can never be triggered by accident, while staying quick for a deliberate reset.
     */
    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_learning_title)
            .setMessage(R.string.reset_learning_confirm1_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> confirmResetFinal() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun confirmResetFinal() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_learning_confirm2_title)
            .setMessage(R.string.reset_learning_confirm2_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val pattern = OffsetStore.loadDetectedPattern(this)
                val model = OffsetModel()
                model.restore(PatternSeed.seed(pattern, keyboard.charKeyGeometry()))
                OffsetStore.save(this, model)
                Toast.makeText(this, R.string.reset_learning_done, Toast.LENGTH_SHORT).show()
                showActualModel()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun patternLabel(pattern: TypingPattern): String {
        return getString(
            when (pattern) {
                TypingPattern.LEFT_INDEX_FINGER -> R.string.t04_pattern_left_index
                TypingPattern.RIGHT_INDEX_FINGER -> R.string.t04_pattern_right_index
                TypingPattern.LEFT_THUMB -> R.string.t04_pattern_left_thumb
                TypingPattern.RIGHT_THUMB -> R.string.t04_pattern_right_thumb
                TypingPattern.TWO_THUMBS -> R.string.t04_pattern_two_thumbs
                TypingPattern.UNKNOWN -> R.string.t04_pattern_unknown
            }
        )
    }
}

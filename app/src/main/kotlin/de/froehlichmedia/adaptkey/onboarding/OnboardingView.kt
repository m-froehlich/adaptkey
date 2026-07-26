// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.onboarding

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import de.froehlichmedia.adaptkey.R

/**
 * First-run onboarding panel shown above the keyboard (§2 / §9). It steps through the app + its core
 * promises, the optional mini-LLM import, and the initial calibration, using the pure [Onboarding]
 * sequencing. Thin Android glue over string resources and callbacks; instrumented-test territory.
 */
class OnboardingView(context: Context) : LinearLayout(context) {
    
    /** Invoked when the flow is finished or skipped (the host hides the panel and records completion). */
    var onFinished: (() -> Unit)? = null
    
    /** Invoked when the user opens the mini-LLM model import from the model step. */
    var onOpenModelImport: (() -> Unit)? = null
    
    /** D-280: invoked when the user opens the language-pack screen from the language-selection step. */
    var onOpenLanguagePacks: (() -> Unit)? = null
    
    /** Invoked when the user starts the calibration from the calibration step. */
    var onOpenCalibration: (() -> Unit)? = null
    
    /**
     * D-280: the device-locale-suggested languages ([de.froehlichmedia.adaptkey.language.
     * SuggestedLanguages]), already resolved to their own [de.froehlichmedia.adaptkey.language.Language.
     * endonym] display names by the caller (a plain Android [Context] concern, kept out of this view).
     * Appended to the language-selection step's body when non-empty; re-renders immediately if set after
     * that step is already showing.
     */
    var suggestedLanguageNames: List<String> = emptyList()
        set(value) {
            field = value
            if (step == OnboardingStep.LANGUAGE_SELECTION) {
                render()
            }
        }
    
    private var step = Onboarding.first()
    
    private val titleView = TextView(context)
    private val indicatorView = TextView(context)
    private val bodyView = TextView(context)
    private val actionButton = Button(context)
    private val skipButton = Button(context)
    private val nextButton = Button(context)
    private val footerView = TextView(context)
    
    init {
        orientation = VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(16))
        
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        titleView.setTypeface(titleView.typeface, android.graphics.Typeface.BOLD)
        addView(titleView)
        
        indicatorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        indicatorView.setPadding(0, dp(4), 0, dp(12))
        addView(indicatorView)
        
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        bodyView.setLineSpacing(dp(4).toFloat(), 1f)
        val scroll = ScrollView(context)
        scroll.addView(bodyView)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        
        actionButton.setOnClickListener { onAction() }
        addView(actionButton, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        
        val nav = LinearLayout(context)
        nav.orientation = HORIZONTAL
        nav.gravity = Gravity.END
        skipButton.setText(R.string.onboarding_skip)
        skipButton.setOnClickListener { finish() }
        nextButton.setOnClickListener { onNext() }
        nav.addView(skipButton)
        nav.addView(nextButton)
        addView(nav, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        
        footerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        footerView.setText(R.string.onboarding_footer_hint)
        footerView.setPadding(0, dp(10), 0, 0)
        addView(footerView)
        
        render()
    }
    
    /** Resets the flow to the first step (for re-showing the introduction later). */
    fun restart() {
        step = Onboarding.first()
        render()
    }
    
    private fun render() {
        indicatorView.text = context.getString(R.string.onboarding_step_indicator, Onboarding.position(step), Onboarding.count())
        when (step) {
            OnboardingStep.WELCOME -> {
                titleView.setText(R.string.onboarding_welcome_title)
                bodyView.setText(R.string.onboarding_welcome_body)
                actionButton.visibility = View.GONE
            }
            
            OnboardingStep.LANGUAGE_SELECTION -> {
                titleView.setText(R.string.onboarding_language_title)
                bodyView.text = if (suggestedLanguageNames.isEmpty()) {
                    context.getString(R.string.onboarding_language_body)
                } else {
                    context.getString(R.string.onboarding_language_body) + "\n\n" +
                        context.getString(R.string.onboarding_language_suggested, suggestedLanguageNames.joinToString(", "))
                }
                actionButton.setText(R.string.onboarding_language_action)
                actionButton.visibility = View.VISIBLE
            }
            
            OnboardingStep.MODEL_IMPORT -> {
                titleView.setText(R.string.onboarding_model_title)
                bodyView.setText(R.string.onboarding_model_body)
                actionButton.setText(R.string.onboarding_model_action)
                actionButton.visibility = View.VISIBLE
            }
            
            OnboardingStep.CALIBRATION -> {
                titleView.setText(R.string.onboarding_calibration_title)
                bodyView.setText(R.string.onboarding_calibration_body)
                actionButton.setText(R.string.onboarding_calibration_action)
                actionButton.visibility = View.VISIBLE
            }
        }
        nextButton.setText(if (Onboarding.isLast(step)) R.string.onboarding_finish else R.string.onboarding_next)
    }
    
    private fun onAction() {
        when (step) {
            OnboardingStep.LANGUAGE_SELECTION -> onOpenLanguagePacks?.invoke()
            OnboardingStep.MODEL_IMPORT -> onOpenModelImport?.invoke()
            OnboardingStep.CALIBRATION -> onOpenCalibration?.invoke()
            OnboardingStep.WELCOME -> Unit
        }
    }
    
    private fun onNext() {
        val next = Onboarding.next(step)
        if (next == null) {
            finish()
        } else {
            step = next
            render()
        }
    }
    
    private fun finish() {
        onFinished?.invoke()
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

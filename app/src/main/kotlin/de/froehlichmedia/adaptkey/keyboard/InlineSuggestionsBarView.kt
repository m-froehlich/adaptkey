// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

/**
 * D-135: hosts the platform-rendered Autofill "inline suggestion" views (a saved username/password from
 * whichever autofill service the device has configured - Google Password Manager by default, but equally
 * Bitwarden/1Password/etc.) in the same row the ordinary suggestion bar occupies.
 *
 * Deliberately a plain [ViewGroup][android.view.ViewGroup] container, not a custom-drawn
 * [android.graphics.Canvas] view like [AdaptKeyboardView]/`SuggestionBarView`: each suggestion is an
 * opaque `InlineContentView` the platform itself inflates and owns (see
 * [android.view.inputmethod.InlineSuggestion.inflate]) - the IME can only place it as a real child view,
 * never draw or restyle its content, since it never receives the underlying credential data at all.
 * `AdaptKeyService` shows this row instead of the ordinary suggestion bar whenever it holds at least one
 * inflated suggestion, and reverts to the ordinary bar once cleared.
 */
class InlineSuggestionsBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
    
    init {
        isHorizontalScrollBarEnabled = false
        addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
    }
    
    /** @return true once at least one suggestion view is currently shown */
    val hasSuggestions: Boolean
        get() = content.childCount > 0
    
    /** Adds one inflated suggestion view at the end of the row. */
    fun addSuggestion(view: View) {
        content.addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
    }
    
    /** Removes every suggestion view, leaving the row empty ([hasSuggestions] false). */
    fun clearSuggestions() {
        content.removeAllViews()
    }
}

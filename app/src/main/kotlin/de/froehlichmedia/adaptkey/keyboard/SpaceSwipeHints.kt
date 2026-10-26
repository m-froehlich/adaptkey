// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-502: whether the space key should display the G-01 language-swipe chevrons - the subtle, visible
 * left/right glyphs signalling that a horizontal swipe on the space bar cycles the input language (G-01).
 * Pure policy, kept out of [AdaptKeyboardView] so it is unit-testable without an Android dependency, the
 * same way [KeyGesture] is.
 *
 * The chevrons answer a genuine discoverability gap ("Die Benutzer sehen nicht, dass man die Sprache
 * wechseln kann") and are therefore shown exactly where the gesture itself exists, and nowhere else:
 *
 * - The gesture is bound to the letters surface's own space key only ([KeyGesture]'s own G-01 resolution)
 *   - the symbol pages' smaller space keys, and the narrower URL-mode/email-mode space keys, have no
 *   language of their own (their horizontal swipes fall through to the page switch), so they never show
 *   chevrons.
 * - A swipe can only ever reach a *different* language when at least one language pack beyond the
 *   always-bundled English is installed ([de.froehlichmedia.adaptkey.language.LanguageCycle.
 *   alternativeExists]) - with a one-entry cycle the chevrons would advertise a gesture that does nothing.
 * - The chevrons flank the language label, so they follow its own visibility: until the service has pushed
 *   a label, the space key is not yet in its language-labelled state at all.
 */
object SpaceSwipeHints {
    
    /**
     * @param surface the currently shown input surface
     * @param urlMode true while a URL-mode field is focused (the space key's swipe is not a language swipe)
     * @param emailMode true while an email-mode field is focused (same reasoning as [urlMode])
     * @param languageLabelShown true when the space key currently carries the language label (the service
     *        has pushed a non-empty [AdaptKeyboardView.spaceLabel])
     * @param alternativeLanguageExists true when at least one language pack beyond English is installed
     * @return whether the left/right swipe chevrons belong on the space key right now
     */
    fun visible(
        surface: InputSurface,
        urlMode: Boolean,
        emailMode: Boolean,
        languageLabelShown: Boolean,
        alternativeLanguageExists: Boolean
    ): Boolean {
        return alternativeLanguageExists &&
            languageLabelShown &&
            surface == InputSurface.LETTERS &&
            !urlMode &&
            !emailMode
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the D-502 space-key language-swipe chevron visibility policy. */
class SpaceSwipeHintsTest {
    
    @Test
    fun `visible on the letters surface with a label and an alternative language`() {
        assertTrue(
            SpaceSwipeHints.visible(
                surface = InputSurface.LETTERS,
                urlMode = false,
                emailMode = false,
                languageLabelShown = true,
                alternativeLanguageExists = true
            )
        )
    }
    
    @Test
    fun `never visible with no other language installed - the gesture would do nothing`() {
        assertFalse(
            SpaceSwipeHints.visible(
                surface = InputSurface.LETTERS,
                urlMode = false,
                emailMode = false,
                languageLabelShown = true,
                alternativeLanguageExists = false
            )
        )
    }
    
    @Test
    fun `never visible before the service has pushed a language label`() {
        assertFalse(
            SpaceSwipeHints.visible(
                surface = InputSurface.LETTERS,
                urlMode = false,
                emailMode = false,
                languageLabelShown = false,
                alternativeLanguageExists = true
            )
        )
    }
    
    @Test
    fun `never visible on the symbol surface - its space swipe is the page switch`() {
        assertFalse(
            SpaceSwipeHints.visible(
                surface = InputSurface.SYMBOLS,
                urlMode = false,
                emailMode = false,
                languageLabelShown = true,
                alternativeLanguageExists = true
            )
        )
    }
    
    @Test
    fun `never visible on the emoji surface`() {
        assertFalse(
            SpaceSwipeHints.visible(
                surface = InputSurface.EMOJI,
                urlMode = false,
                emailMode = false,
                languageLabelShown = true,
                alternativeLanguageExists = true
            )
        )
    }
    
    @Test
    fun `never visible in url mode even on the letters surface`() {
        assertFalse(
            SpaceSwipeHints.visible(
                surface = InputSurface.LETTERS,
                urlMode = true,
                emailMode = false,
                languageLabelShown = true,
                alternativeLanguageExists = true
            )
        )
    }
    
    @Test
    fun `never visible in email mode even on the letters surface`() {
        assertFalse(
            SpaceSwipeHints.visible(
                surface = InputSurface.LETTERS,
                urlMode = false,
                emailMode = true,
                languageLabelShown = true,
                alternativeLanguageExists = true
            )
        )
    }
}

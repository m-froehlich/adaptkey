// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

/**
 * D-106 stage 1: the ordered set of keyboard languages reachable via the G-01 space-bar swipe cycle -
 * German, English, Greek, in that fixed order (English promoted from an A-03-only auto-detected
 * fallback to a real, explicitly selectable active language, alongside German and Greek). Pure stepping
 * logic, kept separate from [de.froehlichmedia.adaptkey.AdaptKeyService] so it is unit-tested without an
 * Android dependency.
 */
object LanguageCycle {
    
    val LANGUAGES = listOf(Language.GERMAN, Language.ENGLISH, Language.GREEK)
    
    /**
     * @param current the currently active language
     * @return the next language in the cycle, wrapping from the last back to the first
     */
    fun next(current: Language): Language {
        val index = LANGUAGES.indexOf(current)
        return LANGUAGES[(index + 1) % LANGUAGES.size]
    }
    
    /**
     * @param current the currently active language
     * @return the previous language in the cycle, wrapping from the first back to the last
     */
    fun previous(current: Language): Language {
        val index = LANGUAGES.indexOf(current)
        return LANGUAGES[(index - 1 + LANGUAGES.size) % LANGUAGES.size]
    }
}

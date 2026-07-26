// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

/**
 * D-106 stage 1 / D-280: the set of keyboard languages reachable via the G-01 space-bar swipe cycle -
 * [Language.ENGLISH] (always bundled) plus whichever further languages are currently installed (D-280's
 * language packs), in [Language] declaration order so the cycle is deterministic regardless of install
 * order. Pure stepping logic, kept separate from [de.froehlichmedia.adaptkey.AdaptKeyService] so it is
 * unit-tested without an Android dependency.
 */
object LanguageCycle {
    
    /**
     * @param installed the currently installed languages beyond English (see
     *        [de.froehlichmedia.adaptkey.language.InstalledLanguagesStore])
     * @return the full cycle - English first, then every installed language in [Language] declaration order
     */
    fun languages(installed: Set<Language>): List<Language> {
        return listOf(Language.ENGLISH) + Language.entries.filter { it in installed && it != Language.ENGLISH }
    }
    
    /**
     * @param current the currently active language
     * @param installed the currently installed languages beyond English
     * @return the next language in the cycle, wrapping from the last back to the first; [current] itself
     *         when it is the only language in the cycle
     */
    fun next(current: Language, installed: Set<Language>): Language {
        val cycle = languages(installed)
        val index = cycle.indexOf(current)
        if (index < 0) {
            return cycle.first()
        }
        return cycle[(index + 1) % cycle.size]
    }
    
    /**
     * @param current the currently active language
     * @param installed the currently installed languages beyond English
     * @return the previous language in the cycle, wrapping from the first back to the last; [current] itself
     *         when it is the only language in the cycle
     */
    fun previous(current: Language, installed: Set<Language>): Language {
        val cycle = languages(installed)
        val index = cycle.indexOf(current)
        if (index < 0) {
            return cycle.first()
        }
        return cycle[(index - 1 + cycle.size) % cycle.size]
    }
}

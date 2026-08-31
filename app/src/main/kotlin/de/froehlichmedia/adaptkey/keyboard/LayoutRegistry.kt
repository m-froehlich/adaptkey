// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import de.froehlichmedia.adaptkey.language.Language
import java.util.Locale

/**
 * D-280: which compiled-in letters-surface layout a [Language] uses - the single source of truth
 * [de.froehlichmedia.adaptkey.AdaptKeyService] consults instead of scattering `activeLanguage ==
 * Language.GREEK`/`== Language.ENGLISH` comparisons across every call site that picks the keyboard view's
 * [de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView.layoutKind].
 *
 * Only a language with its own strong, expected physical-layout convention distinct from plain QWERTY needs
 * its own entry here: a genuinely different alphabet (Greek today), or a Latin arrangement that reorders
 * letters across rows rather than just swapping two within a row (French's AZERTY, D-314 - unlike German's
 * QWERTZ, it cannot be expressed as a variant flag on [KeyboardLayout.rows] since Q/W/A/Z/M actually move
 * between rows, not just within one). Every other Latin-script language, including one added later purely
 * via a downloadable dictionary (see the language-contribution guide), is already fully typeable through the
 * ordinary [KeyboardLayout] ([LayoutKind.LATIN_QWERTY]) with no new layout code at all.
 */
enum class LayoutKind {
    LATIN_QWERTZ,
    LATIN_QWERTY,
    LATIN_AZERTY,
    GREEK
}

object LayoutRegistry {
    
    private val KINDS: Map<Language, LayoutKind> = mapOf(
        Language.GERMAN to LayoutKind.LATIN_QWERTZ,
        Language.FRENCH to LayoutKind.LATIN_AZERTY,
        Language.GREEK to LayoutKind.GREEK
    )
    
    /**
     * @param language a keyboard-typing language
     * @return its compiled-in layout kind - [LayoutKind.LATIN_QWERTY] for every language without its own
     *         entry above, since that is the safe, already-typeable default for an ordinary Latin-script
     *         language
     */
    fun kindFor(language: Language): LayoutKind = KINDS[language] ?: LayoutKind.LATIN_QWERTY
    
    /** Languages with a genuinely different (non-Latin) alphabet, requiring dedicated layout code
     *  ([GreekLayout]) rather than reusing [KeyboardLayout]. */
    val NON_LATIN_LANGUAGES: Set<Language> = KINDS.filterValues { it == LayoutKind.GREEK }.keys
    
    /**
     * D-400: the actual layout shown day to day - deliberately independent of [activeLanguage] (the
     * dictionary/suggestion language, changed freely by both the manual G-01 swipe and D-130's automatic
     * sustained-English promotion) in the ordinary case, pinned instead to the device's own system
     * language, so a language switch alone - whichever of the two triggers it - never rearranges physical
     * keys the user did not ask to rearrange ("niemand will plötzlich von QWERTZ auf QWERTY wechseln").
     * Two cases override that default, in order:
     * 1. [activeLanguage] itself has a non-Latin layout ([NON_LATIN_LANGUAGES], Greek today) - always wins,
     *    since every other Latin layout is otherwise physically incapable of typing it at all. In practice
     *    this can only ever be reached via an explicit G-01 swipe *into* that language, never D-130's
     *    automatic promotion (which only ever targets English, and can only ever fire from an already-Latin
     *    active language to begin with - Greek's own layout has no Latin key positions to type the English
     *    words that would trigger it from).
     * 2. The system language itself has no sensible Latin layout to offer (it resolves to a non-Latin
     *    language - Greek - or to nothing this app recognises at all) while [activeLanguage] is a genuine,
     *    explicit choice with its own real layout convention - falls back to [activeLanguage]'s own layout
     *    directly in that case (the user's own reasoning: a system-language-Greek device explicitly
     *    switching to German or French is *already* a clear, deliberate switch, so it may as well land on
     *    that language's own correct layout rather than an arbitrary QWERTY default).
     *
     * @param systemLocale the device's own system locale (e.g. [Locale.getDefault])
     * @param activeLanguage the currently active dictionary/suggestion language (G-01)
     * @return the layout kind to actually show
     */
    fun kindFor(systemLocale: Locale, activeLanguage: Language): LayoutKind {
        if (activeLanguage in NON_LATIN_LANGUAGES) {
            return kindFor(activeLanguage)
        }
        val systemLanguage = Language.fromCode(systemLocale.language)
        if (systemLanguage != null && systemLanguage !in NON_LATIN_LANGUAGES) {
            return kindFor(systemLanguage)
        }
        return kindFor(activeLanguage)
    }
}

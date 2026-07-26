// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import de.froehlichmedia.adaptkey.language.Language

/**
 * D-280: which compiled-in letters-surface layout a [Language] uses - the single source of truth
 * [de.froehlichmedia.adaptkey.AdaptKeyService] consults instead of scattering `activeLanguage ==
 * Language.GREEK`/`== Language.ENGLISH` comparisons across every call site that picks the keyboard view's
 * `greek`/`qwerty` flags.
 *
 * Only a genuinely different alphabet needs its own entry here (Greek today): every Latin-script language,
 * including one added later purely via a downloadable dictionary (see the language-contribution guide), is
 * already fully typeable through the ordinary [KeyboardLayout] ([LayoutKind.LATIN_QWERTY]) with no new layout
 * code at all - only German keeps its own [LayoutKind.LATIN_QWERTZ] entry, the one Latin exception.
 */
enum class LayoutKind {
    LATIN_QWERTZ,
    LATIN_QWERTY,
    GREEK
}

object LayoutRegistry {
    
    private val KINDS: Map<Language, LayoutKind> = mapOf(
        Language.GERMAN to LayoutKind.LATIN_QWERTZ,
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
}

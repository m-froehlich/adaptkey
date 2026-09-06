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
    LATIN_AZERBAIJANI,
    GREEK,
    SERBIAN_CYRILLIC,
    RUSSIAN_CYRILLIC,
    UKRAINIAN_CYRILLIC
}

/**
 * The writing system a [LayoutKind] uses - a coarser grouping than [LayoutKind] itself, since several
 * distinct non-Latin `LayoutKind`s can share one script despite each having its own real, distinct physical
 * layout (D-450-followup: Russian/Ukrainian/Serbian are all [CYRILLIC] even though [LayoutRegistry.KINDS]
 * correctly gives each its own `LayoutKind`). This is exactly the grouping [LayoutRegistry.CYRILLIC_LANGUAGES]
 * scopes `AdaptKeyService`'s own Cyrillic-sibling classifier to - a shared [Script] but not a shared
 * `LayoutKind` is precisely the case that needs a real same-script classifier, not two languages that happen
 * to never collide.
 */
enum class Script { LATIN, GREEK, CYRILLIC }

object LayoutRegistry {
    
    private val KINDS: Map<Language, LayoutKind> = mapOf(
        Language.GERMAN to LayoutKind.LATIN_QWERTZ,
        Language.FRENCH to LayoutKind.LATIN_AZERTY,
        Language.AZERBAIJANI to LayoutKind.LATIN_AZERBAIJANI,
        Language.GREEK to LayoutKind.GREEK,
        Language.SERBIAN to LayoutKind.SERBIAN_CYRILLIC,
        Language.RUSSIAN to LayoutKind.RUSSIAN_CYRILLIC,
        Language.UKRAINIAN to LayoutKind.UKRAINIAN_CYRILLIC
    )
    
    /** Every [LayoutKind]'s own [Script] - see that enum's own KDoc for why this is a separate mapping from
     *  [KINDS] rather than folding "is it Latin" into [LayoutKind] itself. */
    private val SCRIPTS: Map<LayoutKind, Script> = mapOf(
        LayoutKind.LATIN_QWERTZ to Script.LATIN,
        LayoutKind.LATIN_QWERTY to Script.LATIN,
        LayoutKind.LATIN_AZERTY to Script.LATIN,
        LayoutKind.LATIN_AZERBAIJANI to Script.LATIN,
        LayoutKind.GREEK to Script.GREEK,
        LayoutKind.SERBIAN_CYRILLIC to Script.CYRILLIC,
        LayoutKind.RUSSIAN_CYRILLIC to Script.CYRILLIC,
        LayoutKind.UKRAINIAN_CYRILLIC to Script.CYRILLIC
    )
    
    /**
     * @param language a keyboard-typing language
     * @return its compiled-in layout kind - [LayoutKind.LATIN_QWERTY] for every language without its own
     *         entry above, since that is the safe, already-typeable default for an ordinary Latin-script
     *         language
     */
    fun kindFor(language: Language): LayoutKind = KINDS[language] ?: LayoutKind.LATIN_QWERTY
    
    /**
     * @param language a keyboard-typing language
     * @return its [Script] - [Script.LATIN] for every language without its own [KINDS] entry, mirroring
     *         [kindFor]'s own default
     */
    fun scriptFor(language: Language): Script = SCRIPTS[kindFor(language)] ?: Script.LATIN
    
    /**
     * Languages with a genuinely different (non-Latin) alphabet, requiring dedicated layout code
     * ([GreekLayout], [SerbianLayout], [JcukenLayout]) rather than reusing [KeyboardLayout] - generalised
     * (D-450-followup) from a Greek-only set to any non-[Script.LATIN] entry, so a second script sharing this
     * same "always trust the active language, no QWERTY/QWERTZ/AZERTY fallback makes sense" treatment needs
     * only its own `KINDS`/`SCRIPTS` entry above, not a change here too.
     */
    val NON_LATIN_LANGUAGES: Set<Language> = KINDS.keys.filterTo(HashSet()) { scriptFor(it) != Script.LATIN }
    
    /**
     * D-450-followup: the [Script.CYRILLIC] subset of [NON_LATIN_LANGUAGES] - Serbian, Russian, and
     * Ukrainian, each with its own distinct [LayoutKind]/real physical layout but sharing one script. Named
     * separately (rather than inline-filtered at each call site, as the original gap-documenting canary test
     * in `LayoutRegistryTest` did) now that a real use exists for it: `AdaptKeyService.resolveDict()`'s own
     * Cyrillic-sibling classifier scopes its comparison to exactly this set, once real `language_profiles.tsv`
     * trigram data exists for all three (it now does, as of this round - see that function's own KDoc).
     */
    val CYRILLIC_LANGUAGES: Set<Language> = KINDS.keys.filterTo(HashSet()) { scriptFor(it) == Script.CYRILLIC }
    
    /**
     * D-400: the actual layout shown day to day - deliberately independent of [activeLanguage] (the
     * dictionary/suggestion language, changed freely by the manual G-01 swipe and D-130's automatic
     * sustained-language promotion) in the ordinary case, pinned instead to the device's own system
     * language, so a language switch alone - whichever of the two triggers it - never rearranges physical
     * keys the user did not ask to rearrange ("niemand will plötzlich von QWERTZ auf QWERTY wechseln").
     * Two cases override that default, in order:
     * 1. [activeLanguage] itself has a non-Latin layout ([NON_LATIN_LANGUAGES], Greek or any
     *    [Script.CYRILLIC] language) - always wins, since every other layout is otherwise physically
     *    incapable of typing it at all. Reachable either via an explicit G-01 swipe *into* that language, or
     *    - D-450-followup, once the Cyrillic-sibling classifier existed to drive it - D-130's own automatic
     *    promotion firing FROM one [Script.CYRILLIC] language TO another (Russian typed while Ukrainian is
     *    active, say): unlike the English case, this really does need a genuine layout change (`JcukenLayout`'s
     *    own `ukrainian` flag differs), which is exactly why the promotion path calls
     *    `AdaptKeyService.applyActiveLanguageToView()` rather than only updating the space-bar label. Greek
     *    itself still cannot be an automatic-promotion target either way - it has no sibling script to be
     *    promoted FROM, and no Latin layout has Greek key positions to type the words that would trigger a
     *    promotion INTO it from a Latin language to begin with.
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

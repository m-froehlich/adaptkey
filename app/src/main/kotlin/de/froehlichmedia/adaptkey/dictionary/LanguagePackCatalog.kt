// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import de.froehlichmedia.adaptkey.language.Language

/**
 * D-280: the languages with a real, hosted language-pack archive today, and where to download each one
 * from - kept as plain data (rather than built inline in
 * [de.froehlichmedia.adaptkey.settings.LanguagePacksActivity]) so the list itself is unit-testable, and so
 * that adding a language contributed later (see the language-contribution guide) is a one-line addition
 * here rather than a UI change.
 *
 * Deliberately does **not** list every [Language] enum value - [de.froehlichmedia.adaptkey.language.
 * Language.FRENCH]/`SPANISH`/`ITALIAN`/`DUTCH`/`PORTUGUESE` are already fully typeable (the ordinary Latin
 * [de.froehlichmedia.adaptkey.keyboard.LayoutRegistry] default needs no new layout code for any of them),
 * but none has an actual dictionary built and hosted yet - offering a download button with nothing behind
 * it would be worse than not listing them at all.
 */
object LanguagePackCatalog {
    
    /**
     * One installable language pack.
     *
     * @property language the language this pack seeds a dictionary for
     * @property downloadUrl a stable, public URL for the zipped pack (see the language-contribution guide
     *           for the expected archive shape - a `dict_<code>.tsv` entry, optionally a `bigram_<code>.tsv`
     *           one)
     */
    data class Entry(val language: Language, val downloadUrl: String)
    
    /** All available language packs, in display order. */
    val ENTRIES: List<Entry> = listOf(
        Entry(
            Language.GERMAN,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-de.zip"
        ),
        Entry(
            Language.GREEK,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-el.zip"
        )
    )
}

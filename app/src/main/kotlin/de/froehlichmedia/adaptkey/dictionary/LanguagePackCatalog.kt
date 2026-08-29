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
     * @property version D-307: this pack's own version, bumped by hand whenever its *hosted* content
     *           actually changes (a dictionary-data fix like D-306, a new/updated bigram set, ...) -
     *           compared against [de.froehlichmedia.adaptkey.language.InstalledLanguagesStore.installedVersion]
     *           to tell an already-installed device an update exists. Deliberately not tied to the app's own
     *           `versionCode`/`versionName` - a language pack can be revised (or not) independently of any
     *           given app release, and this project's own workflow already bumps *something* on every round
     *           regardless, so a manual per-pack counter is simplest.
     */
    data class Entry(val language: Language, val downloadUrl: String, val version: Int)
    
    /** All available language packs, in display order. */
    val ENTRIES: List<Entry> = listOf(
        Entry(
            Language.GERMAN,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-de.zip",
            // D-306: dict_de.tsv cleaned of 43 confirmed Wikipedia-extraction-noise entries.
            // D-329: bigram_de.tsv rebuilt without the "mein" -> "kampf" row (D-327's own fix had only
            // corrected the repo source file and the app-side runtime purge, not the hosted archive itself).
            // D-330: "deine" frequency corrected 160 -> 600 (was letting A-01's cost-1/100x override
            // auto-correct it to "seine" every time) and retagged NOUN,OTHER -> OTHER; same fix extended to
            // "deiner"/"deinen"/"deinem"/"deines" against their "seinX" counterparts.
            // D-368: "Weg"/"Stelle"/"Sage" retagged NOUN,VERB (was NOUN / NOUN,OTHER) - these are genuine
            // noun/verb homographs ("ich stelle", "ich sage", "weg sein"), and CapitalisationEngine's own
            // isPureNoun/isAmbiguousNoun split already routes a NOUN,VERB entry to §6 rule 5 (S-06 chip
            // only, never forced) with no code change needed - confirmed directly, not assumed.
            // D-368 (round 2): 27 further weak-verb/noun homographs retagged NOUN,VERB (singular:
            // Frage/Fall/Ende/Liebe/Reise/Suche/Pflege/Sorge/Kauf/Lauf/Glaube/Klage/Schlag/Wache/Wette/Lese;
            // plural: Preise/Ziele/Kämpfe/Spiele/Male/Reize/Rufe/Grüße/Küsse/Schreie; the nominalised
            // infinitive Lachen) plus "dank" gaining the NOUN tag alongside its existing OTHER (the
            // "Dank"/gratitude reading had no tag at all before, unlike the others where only the verb
            // reading was missing).
            version = 7
        ),
        Entry(
            Language.GREEK,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-el.zip",
            version = 1
        )
    )
}

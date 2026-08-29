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
            // D-368 (round 3): a systematic scan of every NOUN-tagged entry against a hypothesised weak-verb
            // infinitive (own value + "n"/"en") found 10,013 mechanical hits across the whole dictionary -
            // the overwhelming majority explained by a noun's own regular dative-plural form, not a real
            // verb (the German dictionary carried zero VERB tags before D-368, so nothing existing could be
            // cross-checked against). Restricted to the 204 hits at or above CorrectionConfidence's own live
            // NOUN_REFERENCE_FREQUENCY (2000) and reviewed individually; 30 confirmed real and retagged
            // NOUN,VERB - ordinary 1st-person-present/imperative collisions (Teil/Form/Land/Rolle/Zahl/Krieg/
            // Reich/Film/Folge/text/Arbeit/Teile/Sitz/Buch/Bau/Nähe/Bad/Druck/Strecke/Lehre/Mal/Ziel), one
            // 3rd-person-present collision ("Macht" the noun vs "macht" = "er/sie/es macht"), and a newly
            // surfaced class, preterite-form collisions (Stand/Begriff/Schloss/Betrieb/Band/Unterschied/
            // Widerstand - e.g. "Band" vs "band" = preterite of "binden"). Everything below frequency 2000
            // remains open - see AdaptKey-Progress.md's own Open TODOs.
            // D-368 (round 4/5): continued the same systematic scan down through frequency 300 (717 hits in
            // 500-1999, 524 in 300-499) - the false-positive rate held steady (~2%, not falling) since the
            // dominant noise (a noun's own dative plural) is a grammar property, not a frequency one. 26 more
            // confirmed real and retagged NOUN,VERB (86 total now): Park/Rat/Spiel/Rede/Falle/Koch/Ruf/Wende/
            // Grab/Erbe/Boot/Antwort/Gestalt/Wein/Tanz/Gewinn/Ruhe/Heirat/Salz/Pass (1st-person/imperative),
            // Schnitt/Verband/Klang/zwang (preterite - e.g. "Klang" vs "klang" = preterite of "klingen"), and
            // Bitte/vergleiche (both already NOUN,OTHER, upgraded to the specific NOUN,VERB now that the
            // verb reading - "bitten"/"ich vergleiche" - is confirmed). Everything below frequency 300
            // remains open.
            // D-368 (round 6): continued into the 200-299 band (501 hits) - hit rate held at 11/501 (~2.2%),
            // confirming the noise floor is flat rather than tapering off. 11 more confirmed real and
            // retagged NOUN,VERB (97 total now): Rauch/Rate/Box/Ernte/Brauch/Heil/Fülle/Mach/Leid/Decke
            // (1st-person/imperative - "Mach"/"mach" being the colloquial imperative of "machen"), and Drang
            // (preterite - "Drang" the noun vs "drang" = preterite of "dringen"; Drang was already
            // NOUN,OTHER, now NOUN,OTHER,VERB). Everything below frequency 200 remains open.
            // D-368 (round 7): the whole 50-199 band (2,803 hits) reviewed in one sitting per explicit user
            // request. Hit rate ~1.4% (39/2803), slightly below the ~2% seen at higher frequencies but still
            // the same flat-noise-floor shape, not a resumed decline. 39 more confirmed real and retagged
            // NOUN,VERB (136 total now) - almost entirely 1st-person-present/imperative collisions: Halt/
            // Schlaf/Hass/Schau/Schleife/Schmelze/Zeuge/Besuche/Senke/Sing/Weide/Deck/Buche/Wachs/Stoß/
            // Stütze/Tank/Fang/Warte/Schreib/Spende/Pack/Bremse/Kürze/Dreh/Wiege/Scheide/Lade/Blase/Stopp/
            // Schenk/Funke/Bade/Schraube/Hexe, plus four already-NOUN,OTHER entries upgraded to the specific
            // NOUN,OTHER,VERB (Back/backen, Schütze/schützen, Schätze/schätzen, Geh/gehen). Everything below
            // frequency 50 remains open.
            // D-368 (round 8): the entire remaining mechanical candidate pool (5,264 hits across 20-49,
            // 10-19, and 5-9 - the scan found none below frequency 5) reviewed in one sitting per explicit
            // user request to finish the rest at once. Hit rate held flat at ~1.4% (74/5264: 38/2743, 23/2017,
            // 13/504) - the noise floor never fell further down to the lowest frequencies either, confirming
            // this is a permanent property of the dataset, not something later rounds would have made
            // cheaper by waiting. 74 more confirmed real and retagged NOUN,VERB or NOUN,OTHER,VERB (210 total
            // now, all six-plus rounds combined) - the same two shapes as every prior round (1st-person-
            // present on `-e`-nouns, informal dropped-`-e` imperative on stem nouns), including several very
            // common everyday imperatives (Zieh/Hau/Hör/Lach/Wasch/Renn/Spring/Steh/Trink) and a number of
            // dictionary entries that were themselves already lowercase inflected forms mistagged as nouns
            // (danke/wachse/zeichne/störe/schenke/singe/bring/bleib/rette/bezahle - each simply gained the
            // VERB tag alongside its existing noun tag). This completes the systematic D-368 homograph scan
            // that began in round 3: the entire ~120k-row dictionary has now been checked against the weak-
            // verb-infinitive hypothesis end to end.
            version = 12
        ),
        Entry(
            Language.GREEK,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-el.zip",
            version = 1
        )
    )
}

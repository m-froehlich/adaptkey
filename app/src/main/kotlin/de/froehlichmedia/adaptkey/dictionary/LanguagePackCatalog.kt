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
            // D-402/D-306-followup/D-345 (garbage cleanup): 348 confirmed Wikipedia-extraction-noise entries
            // removed outright from dict.tsv - not retagged, deleted. Covers the entries already named in
            // spec/history (Mur, BDI, Dee outright removed - no English pack exists to blacklist against
            // instead; en/ell/lich/ische, the four confirmed corpus-tokeniser split artefacts from §277;
            // fir, the confirmed "fir"->"dir" autocorrect-noise entry) plus a systematic probe of the whole
            // dictionary for every short (<=5 chars), low-frequency (<=100), pure-OTHER-tagged entry (1,061
            // candidates - OTHER being the catch-all tag where noise concentrates, unlike a specific POS tag)
            // reviewed individually with real linguistic judgement, not a mechanical filter. The large
            // majority of that probe turned out to be genuine German word-forms simply missing a specific
            // POS tag (conjugated verbs, declined adjectives, colloquial contractions, unit abbreviations) -
            // kept untouched. Confirmed removed: literal LaTeX/math markup command names leaked from
            // Wikipedia's math rendering (cfrac/hline/bigl/bigr/nabla/wedge/qquad/bmod/pmod/dotsb/dotsm/
            // vdots/oplus/vdash/sdot/hbar/sinh/cot/cosh/sgn/notin/binom and more), programming/Unix keyword
            // and command-name leaks (void/bool/const/sort/grep/chmod/gzip/xmlns/args/attr/obj/ptr and more),
            // Latin citation-fragment leaks from academic/legal footnotes (iure/quem/sunt/omnes/rebus/causa/
            // civis/bovis and more), other-language function-word leaks (French/Dutch/Scandinavian/Slavic:
            // aux/qui/dans/avec/vous/degli/sopra/dla and more), and Arabic/Sanskrit/other transliteration
            // and IPA-phonetic fragments (ʿAbd/ʿUmar/ḥaqq/kartī/īl and more) - the latter category located
            // programmatically via a "contains a character outside the German alphabet plus recognised unit
            // symbols (µ, ², ³)" filter rather than hand-transcribed, to avoid Unicode transcription errors.
            // Explicitly kept despite superficially looking dubious: heiße (182, OTHER) - genuinely the real
            // word "ich heiße", confirmed real back in §277, not touched. `git diff --stat` confirmed exactly
            // 348 lines removed and nothing else changed.
            // D-402/D-367 (missing-word additions and frequency corrections): 14 genuinely missing words
            // added (drüber/drunter/neulich/vertan/ah/Oh/erstaunlicherweise/aberkennen, plus agentisch and
            // its five regular declined forms) with frequencies calibrated against comparable existing
            // entries, not guessed blind. Five already-present-but-too-rare entries had their frequency
            // raised to escape CorrectionConfidence's log-scaled known-word-override ratio against a much
            // more frequent cost-1 neighbour (the same register-skew shape D-330 first fixed for "dein"):
            // vorm (30->200, vs "Form" 10141), tue (48->250, vs "The" 7983 - German-Wikipedia band/title
            // noise, not itself removed since it is plausibly genuine extraction content), wessen (20->90,
            // no confirmed live collision but far too low for how common the word actually is), aggressiv
            // (80->300, was letting its own inflected form "aggressive" outrank it), and natürlich (707->2500,
            // was letting its own inflected forms "natürliche"/"natürlichen" outrank it). "Nature" (148,
            // NOUN,OTHER - the English-loanword misparse, not the German adverb family) removed outright per
            // the explicit "should not appear at all" instruction; "Natura" (131, the real Natura-2000 nature-
            // reserve term) left alone, now safely outranked by the corrected "natürlich".
            // D-402 (remaining items): "Stk" (350, OTHER) added as its own word - the "Stk." abbreviation
            // itself is a code fix, not a dictionary one (see Abbreviations.kt's own GERMAN set), but the
            // bare word without a trailing period was also missing from the dictionary entirely and is now
            // added alongside it. "Wegerecht" (20, NOUN) added directly, resolving the reported "Wegerecht"
            // -> "We"+"gerecht" split by giving the whole compound its own entry rather than touching "We"
            // (203, NOUN,OTHER, of uncertain but not confirmed-noise status - left alone). Confirmed the
            // other three reported bad splits (Robotische/Scheiße/Traditionell/Beugungen) are now structurally
            // impossible: "Robot"/"Tradition"/"Beugung" remain genuine words, but their other halves
            // ("ische"/"Sc"/"ell"/"en") no longer resolve at all - "ische"/"ell"/"en" removed outright in
            // §301, and "Sc" was never a real dictionary entry.
            // D-330-followup (the full dein-/sein-/mein-/unser-/ihr-/euer- audit): computed every real
            // keyboard-adjacent single-substitution collision across all six German possessive determiners'
            // full declension paradigms (36 forms), using this project's own exact QWERTZ adjacency grid
            // (KeyboardProximity.kt) and CorrectionConfidence's live log-scaled ratio formula, not guessed.
            // Only one genuine risk remained: bare "dein" (139) vs "sein" (28942), score ~0.86, clearing
            // MEDIUM's 0.75 auto-apply threshold - the suffixed deine/deinen/deinem/deiner/deines forms
            // D-330 already fixed all score 0.64-0.68, safely below every level. No other pronoun pair
            // (mein-/ihr-/unser-/euer- against each other or against dein-/sein-) scored above 0.12 anywhere
            // in the full 36x36 comparison. Fixed the one real case the same way as every prior round: "dein"
            // 139 -> 550 (score now ~0.64, matching "deine"'s own already-fixed margin).
            // §306 (v1.0.59): D-412's own sibling tagging round - the ≥2000-frequency band of the
            // German verb-in-OTHER retagging project (73 words OTHER -> VERB, 5 genuine dual-meaning
            // words - sein/einigen/sieben/gleichen/bestimmten - OTHER -> OTHER,VERB, individually
            // reviewed against the real dictionary, D-368-style). 78 rows changed.
            // §307 (v1.0.60): round 2, the 500-1999 band (209 OTHER -> VERB, 18 genuine dual-meaning
            // words OTHER -> VERB,OTHER) - and a tag-order correction, applied retroactively to
            // round 1's 5 dual words too: OTHER always sorts last against VERB (VERB,OTHER, not
            // OTHER,VERB), per explicit user instruction. 227 + 5 rows changed.
            // §308 (v1.0.61): round 3, the 200-499 band (360 OTHER -> VERB, 63 OTHER -> VERB,OTHER) -
            // plus the same tag-order fix retroactively applied to D-368's own 42 pre-existing
            // NOUN,OTHER,VERB entries (-> NOUN,VERB,OTHER), per explicit user request. 423 + 42 rows
            // changed.
            version = 19
        ),
        Entry(
            Language.GREEK,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-el.zip",
            version = 1
        )
    )
}

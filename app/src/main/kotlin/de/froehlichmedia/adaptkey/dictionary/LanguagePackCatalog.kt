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
 * Language.ITALIAN]/`DUTCH`/`PORTUGUESE` are already fully typeable (the ordinary Latin
 * [de.froehlichmedia.adaptkey.keyboard.LayoutRegistry] default needs no new layout code for any of them),
 * but none has an actual dictionary built and hosted yet - offering a download button with nothing behind
 * it would be worse than not listing them at all. `FRENCH` (D-441) and `SPANISH` (D-443) have already
 * moved from that state to a real, listed entry below.
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
            // §309 (v1.0.62): round 4, the 50-199 band (774 OTHER -> VERB, 190 OTHER -> VERB,OTHER),
            // 2290 candidates individually reviewed. 964 rows changed.
            // §310 (v1.0.63): round 5a, the 30-49 sub-band of 10-49 (528 OTHER -> VERB, 100 OTHER ->
            // VERB,OTHER), 1454 candidates individually reviewed. 628 rows changed.
            // §311 (v1.0.64): round 5b, the 20-29 sub-band of 10-49 (532 OTHER -> VERB, 81 OTHER ->
            // VERB,OTHER), 1459 candidates individually reviewed. 613 rows changed.
            // §312 (v1.0.65): round 5c, the 15-19 sub-band of 10-19 (315 OTHER -> VERB, 80 OTHER ->
            // VERB,OTHER), 1165 candidates individually reviewed. 395 rows changed.
            // §313 (v1.0.66): round 5d, the 12-14 sub-band of 10-14 (340 OTHER -> VERB, 53 OTHER ->
            // VERB,OTHER), 1110 candidates individually reviewed. 393 rows changed.
            // §314 (v1.0.67): round 5e, the 10-11 sub-band of 10-14 (315 OTHER -> VERB, 42 OTHER ->
            // VERB,OTHER), 949 candidates individually reviewed - closes out the entire 10-49 band.
            // 357 rows changed.
            // §315 (v1.0.68): round 6, the final <10 band (327 OTHER -> VERB, 53 OTHER -> VERB,OTHER),
            // 1073 candidates individually reviewed - closes out the entire German verb-in-OTHER
            // retagging sweep across all bands. 380 rows changed.
            // §316 (v1.0.69): closed the deferred Learned-Words-inflection-gap and haptisch-family
            // backlog (28 missing words added, 3 mistagged existing entries fixed) plus the confirmed
            // LaTeX-noise backlog (7 rows removed from dict.tsv, 15 stale bigram.tsv rows removed).
            // §317 (v1.0.70): removed the second LaTeX-noise cluster flagged in §316 (7 more rows,
            // 47 stale bigram.tsv rows) and closed a fresh user-supplied word-family list (19 missing
            // words added, 2 mistagged existing entries fixed).
            // §318 (v1.0.71): "text" NOUN,VERB -> NOUN - a D-368 mechanical-scan false positive (bare
            // "text" is not itself a valid form of the real verb "texten"; that verb's own inflected
            // forms would be texte/textest/textet).
            // §319 (v1.0.72): added "texten" ("to text/message") with its finite forms (texten/texte/
            // textest/textet), flagged as missing in §318; its participle getextet already existed
            // (OTHER) and gained the VERB tag too.
            // §320 (v1.0.73): D-412's lemma column populated for the first time - noun-inflection
            // project round 1, the >=2000 frequency band (124 mechanical candidates, 79 individually
            // confirmed and linked to their base form).
            // §321 (v1.0.74): the noun-inflection-linking project completed end to end - every
            // remaining frequency band (500-1999, 200-499, 50-199, 20-49, 10-19, 5-9) worked through
            // round by round per explicit user instruction to continue autonomously without stopping
            // for interim builds. ~20,024 mechanical candidates individually reviewed across the whole
            // sweep (124 in round 1 already counted in §320); 14,976 lemma links confirmed in total
            // (79 from §320 plus 14,897 in this closing pass), the remainder rejected using the same
            // taxonomy established in §320: derivational demonyms (Berliner/Schweizer-style place-to-
            // person forms), agent-noun -er derivations (Politiker/Lehrer-style "one who does X"),
            // short-stem/coincidental collisions with an unrelated real word, cross-category pairs
            // (a nominalised-infinitive vs. a plain noun sharing a root), foreign/English-spelled
            // plurals, and proper-noun genitives that are themselves standalone surnames. A handful of
            // established person-noun plural exceptions from §320 (Franzose/Grieche/Russe/Chinese/
            // Este/Lette/Ire/Apache) were extended case by case where a genuinely already-accepted
            // person-noun's own plural came up again in a later band. Chain candidates continue to
            // resolve to the true deepest root rather than an intermediate mechanical hop, with several
            // dozen manual corrections per round where the mechanical chain landed on a spurious
            // fragment, a false umlaut-reversal, or a semantically unrelated intermediate word instead
            // of the real lemma. `git diff --stat`/`--numstat` confirmed each round's exact line count
            // before every commit; the write script's fail-loud asserts (target is `NOUN`-tagged,
            // currently has an empty `lemma`, chosen base exists as its own row) never fired across any
            // of the 27 rounds after round 5c's script fix. `dictionaries/de/version.txt` 31 -> 32, pack
            // rebuilt, `LanguagePackCatalog` version 31 -> 32. No new tests (data-only; `lemma` still
            // has zero code readers - this remains groundwork for D-404 Tier 1). `versionCode` 377 ->
            // 378, `versionName` `"1.0.73"` -> `"1.0.74"`. Not yet device-confirmed.
            //
            // §322 (v1.0.75): D-404 Tier 1, the "Wortfamilien" project - complete German noun/verb
            // paradigms (Genitiv/Dativ/Akkusativ Singular, Plural, Dativ Plural for nouns; Präsens x6/
            // Präteritum x6/Partizip II/Imperativ Sg+Pl for verbs), extending §320/§321's lemma-linking
            // groundwork from "link what already exists" to "generate and add what's missing". First
            // attempt was a from-scratch rule engine (genus.py: article-cooccurrence heuristic against
            // `bigram.tsv`; deklination.py/plural.py: strong/weak-declension + plural-class rules;
            // konjugation.py: strong/weak conjugation + a curated per-verb override table for the
            // genuinely ambiguous durch-/um-/über-/unter-/voll-/hinter-/wieder- separable-or-not
            // prefixes, `praefix_overrides.py`) - each component went through several real-bug-found-
            // and-fixed rounds against live corpus data (documented in-line in the scripts themselves),
            // but plural-class assignment in particular kept surfacing new exception classes faster than
            // rules could be added (German plural choice is often lexical, not derivable from spelling),
            // so on explicit user instruction the approach pivoted: `wiktextract`'s German Wiktionary
            // extract (kaikki.org, MIT-licensed tool / CC BY-SA-licensed content, same licence family as
            // this project's existing Wikipedia-derived `dict.tsv`/`bigram.tsv`) is now the *primary*
            // source for both nouns (`extract_wiktionary_nouns.py` -> `wiktionary_nomen.tsv`, 119,779
            // nouns with genus+genitiv+plural) and verbs (`extract_wiktionary_verbs.py` ->
            // `wiktionary_verben.tsv`, 14,412 verbs with full conjugation) - `nomen.py`/`verben.py` each
            // check the Wiktionary table first and fall back to the hand-built rule engine only for
            // words missing there. Explicitly rejected: bulk-importing the ~91,559 Wiktionary nouns
            // entirely absent from `dict.tsv` - only 2 of them have any `bigram.tsv` occurrence at all,
            // meaning essentially none of that pool has a real frequency signal or corpus relevance:
            // this project stayed scoped to completing paradigms of already-present lemmas, not growing
            // the vocabulary itself. New-row frequency uses a lemma-frequency ratio calibrated from each
            // POS's own already-linked pairs (nouns: median 0.355 from 14,976 pairs; verbs: median 0.417
            // from 191 pairs); collision rule: never write a form that already exists anywhere in
            // `dict.tsv` under any POS. The verb write surfaced a real class of candidate-list
            // contamination missed by the earlier validation passes - preterite-plural/participle/
            // Konjunktiv-II forms of already-known strong verbs (`wurden`/`waren`/`worden`,
            // `misslangen`, `gestünden`) and zu-infinitives of separable verbs being mistaken for their
            // own base infinitives - caught and fixed over six write-verify-revert rounds (a generalised
            // "does this word end in a long enough known-strong-verb inflected form" suffix check,
            // replacing an earlier fixed-prefix-list approach that missed compound/double-prefix cases
            // like `nachvollzogen`); final random-sample spot check (70 rows) came back error-free. Also
            // added 8 more strong verbs missing from the original hand-curated table along the way
            // (schlingen/misslingen/heben/empfinden/schleißen/trügen/gebären/schreiten), each confirmed
            // against real `dict.tsv` forms before adding, not guessed. Net result: `dict.tsv` 119,701 ->
            // 158,073 rows (+33,390 noun forms, +4,982 verb forms, all with a `lemma` link), 153,091 of
            // them lemma-linked. `dictionaries/de/version.txt` 32 -> 33, pack rebuilt and verified by
            // unzipping it back and byte-comparing `dict.tsv`, `LanguagePackCatalog` version 32 -> 33.
            // No new tests (data-only; `lemma` still has zero code readers - remains groundwork).
            // `versionCode` 378 -> 379, `versionName` `"1.0.74"` -> `"1.0.75"`. Not yet device-confirmed.
            // D-386-followup: `version.txt` gained a second line, the pack's own declared language code
            // ("de") - `LanguagePackInstaller.parse()` now cross-checks it against the language being
            // imported, so a resolved-by-filename-alone archive (D-386's own automatic folder resolution)
            // can never be silently accepted for the wrong language. `dictionaries/de/version.txt` 33 -> 34
            // (dict.tsv/bigram.tsv/hints.tsv themselves unchanged - verified byte-identical after rebuild),
            // `LanguagePackCatalog` version 33 -> 34.
            // D-404 Tier 1, adjective paradigms (see AdaptKey-Plan-Adjektive.md / History.md): completes
            // full declension (4 cases x singular/plural x strong/weak/mixed) x 3 degrees (Positiv/
            // Komparativ/Superlativ) for every adjective lemma already in `dict.tsv`, same "complete
            // existing lemmas only, no vocabulary growth" scope as the noun/verb round - verified: only
            // 274 of 27,957 not-yet-present Wiktionary adjective lemmas have any `bigram.tsv` occurrence at
            // all (~1%, same near-zero signal that ruled out bulk noun import), so the same call was made
            // here too. Primary source: `wiktionary_adjektive.tsv` (17,061 lemmas with a full, real
            // Flexion table extracted directly from Wiktionary - unlike nouns/verbs, no rule engine was
            // even needed to *generate* these, only to extract them; 3,786 of them are in scope). A real,
            // confirmed lexical irregularity (`hoch` -> `hoher`/`hohe`, a stem change even in the Positiv)
            // is exactly why full attested forms were taken directly rather than derived from a generic
            // suffix rule. The small remainder (20 in-scope lemmas with only bare Positiv/Komparativ/
            // Superlativ stems, no full table) went through a new, cross-verified rule module
            // (`adjektiv_deklination.py`: e-elision for `-el`/`-er` stems - "dunkel"->"dunkler", confirmed
            // never applied before the superlative `-st`/`-est` marker itself - "dunkelste", not
            // "dunklste"; the dental/sibilant superlative extension after s/ß/z/x/d/t/sch - "heiß"->
            // "heißeste", "bunt"->"bunteste") - deliberately excludes the closed umlaut-mutation class
            // (alt/kurz/groß/...), only ever taking an umlauted form when Wiktionary itself attests it,
            // same scoping precedent as the noun/verb round's strong-verb table. Frequency: new lemma
            // frequency x 0.5, a real median (not borrowed from nouns/verbs) computed from 8,837 already-
            // existing dict.tsv form/lemma frequency pairs matched via the Wiktionary form list. Existing
            // lemma rows also gained the `ADJECTIVE` tag alongside their existing tag (e.g. `schön` is now
            // `OTHER,ADJECTIVE`) - previously essentially unused (6 of 158,073 rows). Collision rule
            // unchanged: never write a form already present under any POS. `viel`/`wenig` are not reachable
            // under Wiktionary's `pos=="adj"` (tagged `adv`/`pron` there) - deliberately excluded rather
            // than special-cased for two words. Net result: `dict.tsv` 158,073 -> 189,267 rows (+31,194
            // adjective forms), 3,804 existing lemma rows re-tagged. `dictionaries/de/version.txt` 34 -> 35,
            // pack rebuilt and verified byte-identical after unzip, `LanguagePackCatalog` version 34 -> 35.
            // No new tests (data-only). Not yet device-confirmed.
            // D-404-followup: user caught a real regression from the round above - new multi-tag rows landed
            // as `OTHER,ADJECTIVE` (OTHER first), breaking the established convention (visible in every
            // pre-existing multi-tag row: `NOUN,OTHER`, `VERB,OTHER`, `NOUN,VERB,OTHER` - OTHER always last,
            // i.e. tags follow `PartOfSpeech`'s own enum declaration order). Fixed by re-sorting every
            // multi-tag row's POS field into canonical enum order (3,646 rows corrected, e.g. `schön` is now
            // `ADJECTIVE,OTHER`). Bundled with two more small, low-risk taggings while already touching this
            // file, both previously discussed and confirmed cheap (closed classes, no inflection, no
            // generation needed): 109 already-present German prepositions tagged `PREPOSITION` (155 total in
            // Wiktionary's `pos=="prep"`, 46 not yet in `dict.tsv` deliberately left out, same "complete
            // existing words only" scope as every other round); 6,049 already-`NOUN`-tagged proper nouns
            // (Wiktionary `pos=="name"`, 15,808 total) additionally tagged `PROPER_NOUN` - verified safe
            // first: `CapitalisationEngine`'s own `isProper` check forces capitalisation ahead of the
            // "ambiguous, leave alone" rule, so this was only ever risk-free for words *already* tagged
            // `NOUN` (capitalisation unchanged either way); 3 words matching Wiktionary's name list but not
            // already `NOUN` (`iPhone`/`iPad`/`eBay`, all `OTHER`) were deliberately excluded - forcing
            // first-letter capitalisation would have produced `Iphone` for a lower-case-typed `iphone`, wrong
            // for a brand name with its own internal-capitalisation convention. `dictionaries/de/version.txt`
            // 35 -> 36, pack rebuilt and verified byte-identical after unzip, `LanguagePackCatalog` version
            // 35 -> 36. No new tests (data-only). Not yet device-confirmed.
            // D-423: explicit user instruction - "daß" and the Swiss ss-spelling "Strasse"/"Strassen" must
            // never be suggested/autocorrected to, full stop. A blacklist-based fix (mirroring D-206's
            // existing pre-1996-reform-relic mechanism, which already covered "daß") was proposed first but
            // explicitly declined by the user in favour of outright dictionary removal - "daß" 868, "Strasse"
            // 92, "Strassen" 49 (plural, lemma-linked to "Strasse") removed outright, 3 rows. "daß" also
            // removed from `GermanRules.BUNDLED_CONFUSABLES_BLACKLIST` (D-206) as redundant once gone from
            // the dictionary entirely - blacklisting is no longer the mechanism for this word. The unrelated
            // surname "Strasser" (and its own "Strassers"/"Strassern" inflections) is deliberately untouched
            // - a real person's name is not an error to correct, same proper-noun exclusion D-206's own
            // rationale already establishes; "Strassenbahn"/"Strassenverkehr" (2 further Swiss-spelling
            // compounds found in the same scan) were also left untouched, out of the scope of what was
            // actually asked. `dictionaries/de/version.txt` 36 -> 37, pack rebuilt and verified byte-identical
            // after unzip, `LanguagePackCatalog` version 36 -> 37. No new tests (data-only). Not yet
            // device-confirmed.
            version = 37
        ),
        Entry(
            Language.GREEK,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-el.zip",
            // D-386-followup: same `version.txt` language-code addition as German above ("el").
            // `dictionaries/el/version.txt` 1 -> 2 (dict.tsv/bigram.tsv themselves unchanged - verified
            // byte-identical after rebuild), `LanguagePackCatalog` version 1 -> 2.
            //
            // D-424: the same "Wortfamilien" parity project as D-422 (English) - real POS tags,
            // Wiktionary-sourced inflection forms, lemma-linking. `dict_el.tsv` started from the identical
            // crude baseline (only `OTHER`/`PROPER_NOUN`, no `lemma` column, zero real POS tags). Unlike
            // English, kaikki.org does have its own small Greek-target extract (`el-extract.jsonl.gz`,
            // 106MB compressed - same shape as German's own extract, not English's full 2.6GB dump).
            // Genuinely more complex morphology than German or English, confirmed by real data, not assumed:
            // Greek nouns decline 4 cases x 2 numbers (up to 8 forms, though case syncretism collapses many
            // to the same surface form); adjectives additionally decline by gender (up to 24+ forms) plus
            // comparative/superlative; verbs carry an aspect-based conjugation table (present/imperfect
            // tenses x person x number x active/passive voice, ~20 forms per verb on average, up to 90 for
            // the most richly-documented ones) too large and irregular to hand-name slot by slot the way
            // German's Präsens x6/Präteritum x6 or English's 4-slot verb model did. `extract_wiktionary.py`
            // is deliberately generic instead: every distinct, grammatically-tagged form differing from the
            // lemma itself is extracted as its own row (`word\tform`, many rows per lemma) rather than fixed
            // named columns - periphrastic constructions (e.g. future "θα γράφω", subjunctive "να γράφω",
            // both multi-word) are excluded for free by the same "form contains a space" filter already used
            // for German/English, no Greek-specific handling needed. `merge_wiktionary.py` groups rows back
            // per lemma at merge time; otherwise identical scope discipline, collision rule, and case-match
            // guard as D-422's own English round (see that file's own KDoc for the "went"/"Gan" bug story
            // the guard exists for).
            //
            // Genuinely surprising, verified-not-assumed finding: unlike German/English, Greek's generated-
            // form calibration ratio came out *above* 1.0 (verb 1.40, adjective 1.39, vs. noun's more
            // expected 0.48) - Greek's own citation convention (1st-person-singular-present for verbs,
            // masculine-singular-nominative for adjectives) is a grammatically *rarer* form in real
            // encyclopedic prose than the forms being generated, confirmed directly against real dict.tsv
            // rows: "γράφει" ("he/she/it writes", freq 1661) vastly outranks its own lemma "γράφω" ("I
            // write", freq 61); "μεγάλη" (feminine "big", freq 10601) outranks the masculine citation form
            // "μεγάλος" (freq 1566). The ratio-from-already-matched-pairs calibration handled this correctly
            // without any special-casing, exactly because it measures the real corpus rather than assuming
            // "the lemma is the most common form" the way a naive fixed discount would have.
            //
            // Net result: `dict.tsv` 120,000 -> 154,387 rows (+34,387: 21,210 noun forms, 6,500 verb forms,
            // 6,677 adjective forms), 13,958 lemmas tagged with a real POS, 33,493 already-present forms
            // linked to their lemma, 42 prepositions tagged. `dictionaries/el/version.txt` 2 -> 3, pack
            // rebuilt (dict.tsv + bigram.tsv, no hints.tsv - Greek never had one) and verified byte-identical
            // after unzip, `LanguagePackCatalog` version 2 -> 3. No new tests (data-only, same as D-422).
            // Not yet device-confirmed.
            //
            // D-425 (follow-up, same day): the 5,359 empty-POS rows noted above, fixed on explicit user
            // request. Not individually reviewed with native-fluency linguistic judgement the way the
            // German/English rounds' own noise cleanups were (this session has no Greek fluency to do so
            // with the same confidence) - a structural check instead: every one of the 5,359 words consists
            // purely of Greek-script characters except 6, confirmed genuine extraction noise, not guessed -
            // 5 of them ("ισοτιµίας"/"τοµέα"/"ισοτιµία"/"τιµή"/"εφαρµογή") are Unicode-confusable duplicates
            // of an already-present, correctly-spelled, far-higher-frequency word, using U+00B5 MICRO SIGN
            // where the correct spelling uses U+03BC GREEK SMALL LETTER MU (visually near-identical,
            // different codepoint) - confirmed by direct lookup, e.g. "τιμή" (correct mu) already exists at
            // freq 2307, `NOUN`, vs. the confusable "τιµή" (micro sign) at freq 4 with no tag at all; removed
            // outright, same treatment as D-402's own confirmed-noise removals for German. The remaining
            // 5,354 - all frequency 4-24, the very bottom of the corpus, matching D-402's own finding that
            // low-frequency `OTHER`-adjacent rows are where noise concentrates, but the overwhelming majority
            // read as genuine rare Greek word-forms and proper nouns/surnames on inspection - tagged `OTHER`
            // outright rather than individually classified, matching the same default every other
            // not-yet-specifically-tagged word in this dictionary already carries; a future Wortfamilien-
            // style pass could still upgrade any of them to a real POS the same way D-424 already did for
            // the rest of the dictionary. `dictionaries/el/version.txt` 3 -> 4, pack rebuilt and verified
            // byte-identical after unzip, `LanguagePackCatalog` version 3 -> 4. No new tests (data-only).
            // Not yet device-confirmed.
            //
            // D-426 (bugfix, found via a follow-up "check for markup/noise" request, same day as D-424/
            // D-425): D-424's own generic form-extraction ("every grammatically-tagged form, however many")
            // turned out too permissive for a handful of real Wiktionary data shapes that are not themselves
            // standalone words - found via post-ship spot-check, not caught before the first run. Confirmed
            // examples, not guessed: a bare declension-table ending documented with a leading hyphen
            // (`"-ῶνος"` - "this class's genitive plural ends in -ῶνος", not itself a word), a footnote-
            // number artifact glued onto a form (`"απεδέχθη3ο"`), a cross-referenced Latin-script synonym or
            // transliteration mistaken for a Greek form (`"Urticaria"`, `"Korinthios"`), and genuine alternate
            // spellings joined by a `/` or `\` separator into one unparsed string (`"άρκεσε/ήρκεσε"` - the
            // augmented and unaugmented aorist, both genuinely valid, never split apart). Fixed at the root:
            // `extract_wiktionary.py` now splits on `/`/`\` first (recovering both real forms instead of
            // losing both) and then requires every accepted form to consist purely of Greek-script
            // characters - a single check that subsumes the digit/punctuation/Latin-letter/leading-hyphen
            // cases individually, since none of those shapes is ever a genuine Modern Greek word form.
            // Separately, the same follow-up request also surfaced 24 more instances of §371/D-425's own
            // Unicode-confusable-mu bug (U+00B5 MICRO SIGN vs U+03BC GREEK SMALL LETTER MU) that the D-425
            // fix had missed - that round only scanned the empty-POS subset, not the whole file; this round's
            // full-file scan found and removed all 24, each individually confirmed against an existing,
            // correctly-spelled, far-higher-frequency counterpart first, not assumed (e.g. confusable `"µε"`
            // vs. the correct, already-`PREPOSITION`-tagged `"με"`, freq 274401). Three genuine unit-symbol
            // rows (`"χλμ²"`/`"χμ²"`/`"μ²"`) and the ordinal marker `"Βʹ"` were confirmed NOT to be confusable
            // duplicates (no micro-sign present) and left untouched, matching this project's own established
            // precedent of keeping legitimate unit symbols.
            //
            // Re-derived from the pre-D424 original (120,000 rows, retrieved from git history) with the fixed
            // extraction script, rather than patching the already-corrupted merged file, for full internal
            // consistency. `dict.tsv` 120,000 -> 154,338 rows this time (vs. D-424's own first, now-superseded
            // 154,387 - the difference is entirely the ~30 corrupted rows this round prevents from ever being
            // generated, plus the 24 mu-duplicates now caught up front). Every check from D-424/D-425 re-run
            // clean: 0 case-insensitive duplicates, 0 empty POS fields, 0 non-positive frequencies, 0 orphaned
            // lemma links, all 14 distinct tag combinations in canonical enum order (verified in Python, not
            // shell `sort -u` - Git Bash's own `sort` does not collate Greek UTF-8 correctly on this machine
            // and printed visually-duplicate-looking lines for genuinely-identical combos, a false alarm
            // caught and re-verified, not a real data issue). `dictionaries/el/version.txt` 4 -> 5, pack
            // rebuilt and verified byte-identical after unzip, `LanguagePackCatalog` version 4 -> 5. No new
            // tests (data-only). Not yet device-confirmed.
            //
            // D-444: bare-NOUN capitalisation-safety fix - 46,608 rows carried the tag set exactly `{NOUN}`
            // with no `OTHER` paired in, found while building French's/Spanish's own Wortfamilien completion
            // (the Language Contribution Guide's own step 4 "mandatory bare-noun safety check", added this
            // round). `CapitalisationEngine`'s rule 3 (`isPureNoun -> true`) reads only the tag set, not
            // frequency or how central a sense actually is - a genuine but rare/archaic Wiktionary noun sense
            // for an otherwise non-nominal word was enough to wrongly force capitalisation every time that
            // word was typed, same root cause as English's own D-444 fix (see that entry above). Mechanical,
            // unconditional fix (`dictionaries/el/fix_bare_noun.py`): every row whose tag set is exactly
            // `{NOUN}` gets `OTHER` added; word/frequency columns and every other tag combination (including
            // every `PROPER_NOUN` row) untouched - confirmed via diff, not assumed. `dictionaries/el/
            // version.txt` 5 -> 6, pack rebuilt (new `dictionaries/el/build_zip.py`, this pack never had one
            // checked in before), `LanguagePackCatalog` version 5 -> 6. No new tests (data-only). Not yet
            // device-confirmed.
            version = 6
        ),
        Entry(
            Language.FRENCH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-fr.zip",
            // D-441: first French language pack, built following the Language Contribution Guide's own
            // §8 "one-shot pipeline" end to end (D-314's AZERTY geometry already existed; this round is
            // the dictionary/hints/diacritics/abbreviations/rules half).
            //
            // D-441-followup, same day: the first pass (dict.tsv from a 12,000-word OpenSubtitles-derived
            // list) was explicitly rejected by the user as too thin and not matching the guide's own
            // pipeline - redone properly against a real corpus. dict.tsv: 208,204 words - real frequency
            // counts from an actual French Wikipedia XML dump (frwiki-latest-pages-articles1.xml-
            // p1p306134.bz2, the official first split, capped at its own first 80,000 articles for this
            // machine's real memory headroom - 16GB total, ~4.5GB free - not the full ~306,000-article
            // part; 136.8M real tokens processed), rescaled to German's own frequency magnitude (rank-1 ->
            // ~1,000,000). POS tags are real, not guessed: merged against kaikki.org's French Wiktionary
            // extract (wiktextract, MIT tool / CC BY-SA content - same licence family as German's/Greek's
            // own Wiktionary-derived data, 402,395 entries, 385,932 distinct word strings) - a word found
            // there gets kaikki's own real part of speech; one not found is still kept, tagged OTHER, once
            // its own real corpus count clears 20 occurrences (low enough to keep genuine words Wiktionary
            // has no page for, high enough that one-off tokeniser noise mostly does not), *unless* it is
            // also a common word (frequency >= 100) in this project's own bundled en/dict.tsv - 11,086
            // rows removed this way, confirmed by hand-sampling to catch real English-quote contamination
            // ("countries"/"reviews"/"genocide"/"tourist") while leaving genuine rare French vocabulary
            // ("écobuage"/"hémiptères"/"inexactitudes") alone. A French common noun is deliberately tagged
            // NOUN,OTHER rather than a bare NOUN (verified: zero bare-NOUN rows in the final file):
            // CapitalisationEngine's own §6 rule 3 ("a pure noun is capitalised automatically") is
            // unconditional, not gated by Language, and French - unlike German - does not capitalise
            // common nouns; pairing with OTHER keeps the NOUN signal for A-05's split-safety gate while
            // correctly landing on rule 5's "ambiguous, no automatic correction" outcome instead. A
            // PROPER_NOUN tag is dropped whenever kaikki also saw the same string used as anything else
            // (`pierre`/`jean` are real common nouns and real first names; PROPER_NOUN forces
            // capitalisation unconditionally ahead of every other rule, so keeping it for either would
            // have wrongly capitalised the far more frequent common-noun reading every time). bigram.tsv:
            // 984,792 rows (>=10 real occurrences each) from the same real dump corpus - not the
            // OpenSubtitles source, a genuinely different, much larger sample (3,323,663 distinct pairs at
            // >=3 before this round's own >=10 cutoff). hints.tsv/diacritics.tsv/abbreviations.tsv
            // unchanged from the first pass - French's own AltGr set (a=à, c=ç, e=é, g=œ, i=î, j=ï, k=ë,
            // l=«, o=ô, r=», s=€, t=ê, u=ù, w=è, y=â, z=û, plus German's own 10 language-neutral
            // math/typography assignments kept as-is), the full diacritic-variant table, and a hand-
            // curated sentence-boundary abbreviation list. FrenchRules (LanguageRulesRegistry):
            // decimalCommaGluesDigits=true, timeSuggestionWord=null, bundledConfusablesBlacklist=empty -
            // D-442 made KeyboardProximity layout-aware and unblocked a real AZERTY confusables scan
            // (994 candidates found, mostly short 2-3-letter tokens - `ve`/`ka`/`st`/... - risking
            // autocorrect into a common neighbour), but several are genuine French abbreviations/loanwords
            // (`dj`/`led`/`fn`/`lr`/`crs`/`onf`) this round's own non-native French judgement could not
            // confidently separate from corpus noise at this length - left for native review rather than
            // guessed at (see FrenchRules's own KDoc). Not device-confirmed, and - per the guide's own
            // mandatory step 11 - not yet reviewed by a native French speaker: this is a "pretty good",
            // pipeline-built pack (now built from real corpus data at real scale, not a small proxy
            // source), not native-reviewed quality.
            //
            // D-444: dict.tsv's own POS source was itself the wrong kaikki file - found only when the user
            // directly asked "why is the French one so small?" while a completely separate task (Spanish
            // Wortfamilien completion) was underway. `kaikki.org/dictionary/French/` (used above, 56.5MB)
            // is the *English* Wiktionary edition's own coverage of French; `kaikki.org/dictionary/
            // downloads/fr/fr-extract.jsonl.gz` (714.6MB - a 12x difference) is French Wiktionnaire's own,
            // genuinely native edition - the correct source, used for German/Greek from the start but never
            // for French/Spanish until now. Rebuilt dict.tsv's POS tagging and, new this round, its full
            // Wortfamilien lemma-linking (D-412, previously entirely absent for French - `dict.tsv` never
            // carried a 4th `lemma` column at all) from this corrected source, keeping the existing real
            // Wikipedia-dump frequency numbers unchanged (those were never wrong - only the POS source was).
            // `dictionaries/fr/extract_wiktionary.py`/`merge_wiktionary.py` (new, modelled directly on
            // `dictionaries/el/extract_wiktionary.py`'s own generic per-form extraction, not English's
            // fixed-slot one - French's conjugation has too many combinations for named slots, the identical
            // reasoning Greek's own module comment already gives). Two real, found-the-hard-way bugs fixed
            // in the same round, both documented in `AdaptKey-History.md`'s own D-444 entry in full:
            // (1) a whole class of entries in this edition are individually-paged conjugated *forms* of
            // another lemma (`senses[].form_of`, not the top-level `tags` field English/Greek's own scripts
            // check) - unfiltered, this inflated the raw "verb" entry count 30x (1,265,901 raw vs. 40,263
            // genuine lemmas); (2) this edition writes an ambiguous-subject slot as one string, "il/elle/on
            // mange" (the pronoun alternatives joined by "/", the real verb glued on after a space only on
            // the last one) - naively splitting on "/" first produced bogus bare-pronoun "forms" ("il"/
            // "elle"), which poisoned the frequency-ratio calibration (verb ratio briefly computed as
            // ~163x, an impossible value for a real form/lemma relationship) before a sanity-check of the
            // calibration's own top outliers caught it; fixed by taking the last whitespace-separated token
            // first, only then splitting for a genuine word-level alternation.
            //
            // Also new this round: proper-noun tagging now checks the word's own *full* kaikki pos
            // vocabulary (a new `wiktionary_allpos.tsv`), not just whether the existing dict.tsv row already
            // carried a non-`OTHER` tag - the row-based check missed a real collision (French "les", a
            // pronoun, also has a rare Wiktionary "name"/surname entry; its dict.tsv row's prior tag was
            // bare `OTHER`, which the old check treated as "no collision"), confirmed and fixed the same
            // session the bare-noun safety check below was written. And the mandatory bare-noun safety check
            // itself (Language Contribution Guide's own revised step 4, written this round after the same
            // gap was found - independently - in English's and Greek's own already-shipped dictionaries,
            // see those Entries' own D-444 notes): any tag set that becomes exactly `{NOUN}` gets `OTHER`
            // added automatically, verified zero remaining bare-`NOUN` rows in the final file.
            //
            // Net result: `dict.tsv` 208,204 -> 373,700 rows (+165,496: 74,006 existing lemmas newly tagged
            // via the correct source, 100,205 existing rows linked to a lemma, 165,496 missing inflected
            // forms generated - noun ratio 0.50, verb ratio 0.25, adjective ratio 0.53, each calibrated from
            // real already-matched pairs). Re-verified clean: 0 case-insensitive duplicates, 0 non-positive
            // frequencies, 0 orphaned lemma links, 0 bare-NOUN rows, `pierre`/`jean` still correctly
            // `NOUN,OTHER` not `PROPER_NOUN`. `dictionaries/confusables_scan.py` re-run against the much
            // larger dictionary: 1,050 candidates (was 994) - same shape as before, left uncurated for the
            // same native-review reason. `dictionaries/fr/version.txt` 1 -> 2, pack rebuilt (7.7MB -> 8.7MB).
            // No new tests (data-only). Not device-confirmed. Still not native-speaker reviewed (guide step
            // 11) - the correction this round is a real quality improvement, not a substitute for that gate.
            version = 2
        ),
        Entry(
            Language.SPANISH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-es.zip",
            // D-443: first Spanish language pack, built directly to the real-corpus/real-lexicon method
            // D-441-followup (French, AdaptKey-History.md §415) ended up on - not the earlier, explicitly-
            // superseded first pass (a small OpenSubtitles-derived list, heuristic suffix-based POS tagging,
            // live-random-article bigram sampling). No intermediate/heuristic step was taken for Spanish at
            // all, per explicit user instruction. Spanish already uses the ordinary QWERTY row geometry
            // (LayoutRegistry.kindFor -> LayoutKind.LATIN_QWERTY, no dedicated layout code needed, unlike
            // French's own D-314 AZERTY work) and already had a real character-trigram profile
            // (language_profiles.tsv, "es", since D-280) - both prerequisites already satisfied.
            //
            // dict.tsv: 233,636 words - real frequency counts from an actual Spanish Wikipedia XML dump
            // (eswiki-latest-pages-articles1.xml-p1p159400.bz2, the official first split - the entire
            // ns=0/non-redirect content of that split was processed, 47,669 pages, well under the extractor's
            // own PAGE_CAP=100,000 safety cap; the machine's real free RAM was checked live before starting
            // (~6.3GB, more headroom than French's own ~4.5GB) and the cap was raised only modestly, not
            // scaled up blindly, following the French round's own confirmed-safe 80,000-page/~3.3GB-resident
            // data point). 91.97M real tokens processed, rescaled to German's own frequency magnitude
            // (rank-1 -> ~1,000,000). POS tags are real, not guessed: merged against kaikki.org's Spanish
            // Wiktionary extract (wiktextract, MIT tool / CC BY-SA content - same licence family as German's/
            // Greek's/French's own Wiktionary-derived data, 809,603 entries, 769,370 distinct word strings) -
            // a word found there gets kaikki's own real part of speech; one not found is still kept, tagged
            // OTHER, once its own real corpus count clears 20 occurrences, *unless* it is also a common word
            // (frequency >= 100) in this project's own bundled en/dict.tsv - 12,049 rows removed this way,
            // the same targeted noise signal French's own round used. A Spanish common noun is deliberately
            // tagged NOUN,OTHER rather than a bare NOUN (verified: zero bare-NOUN rows in the final file) -
            // CapitalisationEngine's own §6 rule 3 is unconditional, not gated by Language, and Spanish -
            // like French/English, unlike German - does not capitalise common nouns. A PROPER_NOUN tag is
            // dropped whenever kaikki also saw the same string used as anything else - actively checked for
            // Spanish's own real name/common-noun collisions (per explicit user instruction, not merely
            // reusing the French pierre/jean precedent): "sol"/"paz"/"victoria"/"luz"/"estrella"/"blanca"/
            // "clara"/"esperanza"/"flor"/"dolores"/"pilar"/"mercedes"/"rosario"/"milagros"/"amparo"/
            // "remedios"/"consuelo" - every one a real Spanish first name and a real common noun - all
            // correctly landed as NOUN,OTHER/NOUN,ADJECTIVE/NOUN,VERB, never PROPER_NOUN, confirmed by direct
            // inspection of the final dict.tsv rather than assumed. bigram.tsv: 732,856 rows (>=10 real
            // occurrences each, the same final cutoff French's own published pack uses) from the identical
            // real dump corpus (2,489,688 distinct pairs at >=3 before this cutoff). hints.tsv/diacritics.tsv/
            // abbreviations.tsv are Spanish's own, not reused from German/French: hints.tsv keeps German's 10
            // language-neutral math/typography assignments (b=*, d=°, f=ƒ, h=#, m=-, n=+, p=π, q=@, v=/, x=×)
            // and gives the remaining 16 letters Spanish's own accents/punctuation - a=á, e=é, i=í, o=ó, u=ú,
            // w=ü (u is already spoken for by ú, so the diaeresis - needed for "güe"/"güi" words like
            // "pingüino" - sits on the rare loanword-only letter w instead), l=ñ (mirroring the real Spanish
            // hardware keyboard's own Ñ-next-to-L placement, per explicit user instruction that ñ must not
            // sit on n - n already carries the language-neutral "+"), c=¿, j=¡ (the two Spanish-iconic
            // inverted punctuation marks), g=«, r=» (Spanish's own standard quotation-mark convention), s=€,
            // t=º, y=ª (the masculine/feminine ordinal indicators - a genuinely Spanish-specific typographic
            // convention, the same "°" role German's own set already fills for degree), k/z filled with
            // generically useful remaining typography (—, …). diacritics.tsv: a→á, e→é, i→í, o→ó, u→ú,ü, and
            // n→ñ - included even though ñ is not a genuine alternate-ASCII-spelling convention the way
            // German's ß/ss is (there is no legitimate "write ñ as nn" tradition), but the underlying
            // base-letter-to-real-variant shape is otherwise identical to every other entry here, so the
            // ordinary DataDiacriticFolding mechanism (D-436) already handles it correctly with no dedicated
            // hardcoded special case needed, unlike German's own Umlaut object. abbreviations.tsv: a
            // hand-curated ~26-entry Spanish sentence-boundary abbreviation list (sr./sra./dr./ud./pág./núm./
            // etc.).
            //
            // SpanishRules (LanguageRulesRegistry): decimalCommaGluesDigits=true (the RAE/Spain convention;
            // several Latin American locales use a point instead, not representable separately in this app's
            // current per-language, not per-region, model), timeSuggestionWord=null,
            // bundledConfusablesBlacklist=empty. `dictionaries/confusables_scan.py dictionaries/es/dict.tsv
            // qwerty 30` ran cleanly (Spanish needed no KeyboardProximity work first, unlike French's own
            // D-442 AZERTY prerequisite) and found 625 candidate pairs, the overwhelming majority short (2-3
            // letter) tokens risking autocorrect into a common neighbouring function word - reviewed by hand,
            // same as French's own AZERTY scan, and left deliberately uncurated: several are genuine Spanish
            // words/contractions this round's own non-native judgement confirmed real (fe/ve/re/pa/eh - faith/
            // "goes or sees"/musical note or colloquial intensifier/colloquial "para"/interjection) sitting
            // right alongside genuinely ambiguous short fragments (we/ce/dd/sn/rn) this round could not
            // confidently rule out as pure noise rather than an abbreviation-with-its-period-stripped
            // artefact or a foreign name fragment - left for native review rather than guessed at.
            //
            // Not device-confirmed, and - per the guide's own mandatory step 11 - deliberately NOT claimed to
            // have passed a native-Spanish-speaker review: real corpus scale from the start this time (no
            // superseded first pass, unlike French's own D-441/D-441-followup history), but still a "pretty
            // good" pipeline-built pack, not native-reviewed quality.
            //
            // D-444: same wrong-kaikki-source bug as French's own D-444 entry (see that Entry above for the
            // full story of how it was found: the user asking "why is the French one so small?" mid-way
            // through this pack's own Wortfamilien completion). `kaikki.org/dictionary/Spanish/` (used
            // above, 91MB) is the English Wiktionary edition's own coverage of Spanish; `kaikki.org/
            // dictionary/downloads/es/es-extract.jsonl.gz` (99.3MB - a smaller, easy-to-miss gap than
            // French's own 12x one, but the wrong file regardless) is Spanish Wiktionary's own genuinely
            // native edition. Rebuilt dict.tsv's POS tagging and added its first-ever Wortfamilien
            // lemma-linking (D-412 - `dict.tsv` never carried a `lemma` column before this round) from the
            // corrected source, keeping the existing real Wikipedia-dump frequency numbers unchanged.
            // `dictionaries/es/extract_wiktionary.py`/`merge_wiktionary.py` (new, same generic per-form
            // shape as `dictionaries/fr/extract_wiktionary.py`'s own D-444 round, built the same session -
            // Spanish's real verb count (647,802 raw "verb" entries, only ~19,000 genuine lemmas with their
            // own conjugation table) shows the identical individually-paged-conjugated-form shape French's
            // own round found, so the same `senses[].form_of` filter applies here too; Spanish's own output
            // was checked directly and does *not* have French's second bug (the "il/elle/on mange" combined-
            // pronoun slot notation - every generated form was already a clean single word), but the same
            // defensive fix was ported over anyway so both language scripts share one robust extraction rule
            // rather than relying on this edition happening not to need it.
            //
            // Also new this round: proper-noun tagging now checks the word's own *full* kaikki pos
            // vocabulary (a new `wiktionary_allpos.tsv`), not just whether the existing dict.tsv row already
            // carried a non-`OTHER` tag - the row-based check missed a real collision (Spanish "les", a
            // pronoun, also has a rare Wiktionary "name"/surname entry; its dict.tsv row's prior tag was
            // bare `OTHER`, which the old check treated as "no collision" - confirmed via direct grep before
            // the fix, and confirmed correctly `OTHER` again afterward). And the mandatory bare-noun safety
            // check itself (Language Contribution Guide's own revised step 4, written this round after the
            // same gap was found independently in English's and Greek's own already-shipped dictionaries,
            // see those Entries' own D-444 notes): any tag set that becomes exactly `{NOUN}` gets `OTHER`
            // added automatically - re-verified the `sol`/`paz`/`victoria`/... name/common-noun collisions
            // this pack's own first pass already found stay correctly `NOUN,OTHER`, never `PROPER_NOUN`,
            // after this round's retagging too.
            //
            // Net result: `dict.tsv` 233,636 -> 419,571 rows (+185,935: 51,200 existing lemmas newly tagged
            // via the correct source, 101,620 existing rows linked to a lemma, 185,935 missing inflected
            // forms generated - noun ratio 0.47, verb ratio 0.33, adjective ratio 0.60, each calibrated from
            // real already-matched pairs). Re-verified clean: 0 case-insensitive duplicates, 0 non-positive
            // frequencies, 0 orphaned lemma links, 0 bare-NOUN rows. `dictionaries/confusables_scan.py`
            // re-run against the much larger dictionary: 650 candidates (was 625) - same shape as before,
            // left uncurated for the same native-review reason. `dictionaries/es/version.txt` 1 -> 2, pack
            // rebuilt (6.1MB -> 7.1MB). No new tests (data-only). Not device-confirmed. Still not
            // native-speaker reviewed (guide step 11) - the correction this round is a real quality
            // improvement, not a substitute for that gate.
            version = 2
        ),
        Entry(
            Language.PORTUGUESE,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-pt.zip",
            // D-445: first Portuguese language pack, built via the Language Contribution Guide's own §8
            // real-corpus/real-lexicon pipeline in one continuous autonomous run together with Italian
            // (D-446) and Dutch (D-447), per explicit user instruction - see those Entries below for their
            // own numbers. Portuguese already used ordinary QWERTY (no dedicated layout code needed) and
            // already had a real character-trigram profile (`language_profiles.tsv`, "pt", since D-280).
            //
            // **Explicit user correction mid-round, worth recording**: the first attempt used a single
            // capped Wikipedia dump split (105,695 pages, the French/Spanish D-441/D-443 precedent) - the
            // user judged this "obviously too small" and required the COMPLETE Wikipedia dump be processed
            // for all three languages, not a page-capped first split. Re-done against
            // `ptwiki-latest-pages-articles.xml.bz2` (the single combined full dump, 2.72GB compressed) -
            // `dictionaries/pt/extract_wiki_dump.py` gained two real additions over the French/Spanish
            // reference to make full-dump-scale processing tractable in one night: multiprocessing across
            // this machine's 6 physical/12 logical cores for the CPU-heavy per-page cleaning/tokenising
            // step, and periodic hapax pruning (dropping count==1 entries once a Counter grows past a size
            // threshold) to keep memory bounded over a full-scale run instead of a capped one. Result: the
            // ENTIRE dump processed - 1,181,337 real (ns=0, non-redirect) pages, 488,957,696 real tokens -
            // an order of magnitude more real corpus data than any capped-dump language this project has
            // built so far.
            //
            // **A second real, confirmed structural finding, unrelated to the dump-size correction above**:
            // Portuguese's own native Wiktionary edition (`kaikki.org/dictionary/downloads/pt/
            // pt-extract.jsonl.gz`, 35.4MB compressed) is the correct, mandatory source per the guide's own
            // unconditional rule - but is actually SMALLER than the wrong English-Wiktionary-coverage file
            // (54.2MB) - the reverse of French's/Spanish's own pattern (native always bigger there). Still
            // used per the guide's rule regardless of size. More consequentially: this native edition
            // documents Portuguese VERB conjugation richly (79,453 raw "verb" entries, 72,710 senses[].
            // form_of references to another lemma - the same individually-paged-conjugated-form shape every
            // other native edition shows - leaving ~6,700 real verb lemmas with full tables) but essentially
            // does NOT document regular noun/adjective inflection as forms[] data at all: only 137 of 52,477
            // noun lemmas and 70 of 18,413 adjective lemmas have any real form beyond a non-word hyphenation
            // marker ("grego" -> "gre.go", tagged "canonical", excluded). Confirmed by direct inspection of
            // real entries, not assumed, and discussed with the user before proceeding: Portuguese Wortfamilien
            // completion is therefore real and complete for VERBS only this round (noun ratio n=29 pairs,
            // adjective ratio n=16 pairs - both far too sparse to mean anything, versus verb ratio n=28,416)
            // - nouns/adjectives keep whatever the base dict.tsv/POS-tagging pass already produced, with only
            // 129 generated noun forms and 36 generated adjective forms (essentially incidental alternate-
            // spelling/superlative entries, not systematic paradigm completion). This is a genuine, confirmed
            // source limitation - not a bug in `extract_wiktionary.py`/`merge_wiktionary.py` - documented
            // honestly rather than silently passed off as parity with French/Spanish/Italian/Dutch.
            //
            // `dict.tsv`: 535,869 rows (325,579 from the initial Wikipedia-frequency + kaikki-POS merge,
            // +210,290 from Wortfamilien completion - 210,125 of those generated verb forms alone). POS
            // tagging: a word found in kaikki gets its real part of speech; one not found is kept, tagged
            // OTHER, once its own real corpus count clears 20 occurrences (274,565 kept this way, 2,684,749
            // below-floor rows dropped), unless it is also a common word (frequency >= 100) in this
            // project's own bundled `en/dict.tsv` (14,968 rows removed this way - the same targeted noise
            // signal French/Spanish/Italian/Dutch all use). Mandatory bare-noun safety check (Guide step 4):
            // 0 bare-NOUN rows in the final file. `bigram.tsv`: 2,814,934 rows (>=10 real occurrences,
            // matching French's/Spanish's own final cutoff) from 6,561,082 rows at the raw >=3
            // extraction-time floor.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Portuguese's own, not reused: hints.tsv
            // keeps German's 10 language-neutral math/typography assignments (b=*, d=°, f=ƒ, h=#, m=-, n=+,
            // p=π, q=@, v=/, x=×) and gives the remaining 16 letters Portuguese-specific content - a=ã, c=ç,
            // e=é, i=í, o=õ, u=ú (the six accented/cedilla base letters - one representative variant per
            // key, `diacritics.tsv` keeps the full set), g=«/r=» (quotation marks), s=€ (currency), t=º/y=ª
            // (masculine/feminine ordinal indicators, the same role German's own ° fills for degree),
            // j/k/l/w/z filled with generically useful remaining typography (—, …, &, §, •). `diacritics.tsv`:
            // a→ã,á,à,â; c→ç; e→é,ê; i→í; o→ó,ô,õ; u→ú. `abbreviations.tsv`: a hand-curated ~30-entry
            // Portuguese sentence-boundary abbreviation list (sr./dr./prof./av./etc./...).
            //
            // `PortugueseRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true (both European and
            // Brazilian Portuguese), `timeSuggestionWord`=null (no single-word "Uhr"-style convention),
            // `bundledConfusablesBlacklist`=empty. `dictionaries/confusables_scan.py dictionaries/pt/dict.tsv
            // qwerty 30` found 901 candidate pairs - left deliberately uncurated, the same "cannot confidently
            // separate a genuine short Portuguese word/abbreviation from real corpus noise without native
            // fluency" reasoning French/Spanish/Italian/Dutch all document; a spot-check during this round
            // directly confirmed the risk of guessing - "sei" (176, a real, common word: "eu sei" = "I know")
            // appeared as a candidate risking autocorrect toward "seu" (46,532), exactly the kind of call this
            // round's own non-native judgement cannot safely make.
            //
            // Quality gate (Guide §8): 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned
            // lemma links, 0 bare-NOUN rows - all re-verified directly against the final file, not assumed.
            // New tests: `LanguageRulesTest` gained a `Portuguese resolves to PortugueseRules` case plus its
            // own `PortugueseRules`-mirroring test block, matching `FrenchRules`/`SpanishRules`'s existing
            // shape. `LanguagePackCatalogTest` needed no change - already fully generic over `ENTRIES`.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been
            // reviewed by anyone who actually speaks Portuguese. Real, full-dump corpus scale (488.96M real
            // tokens) and a real lexicon, but still "pretty good" in the guide's own sense - and, unlike
            // French/Spanish/Italian/Dutch, genuinely incomplete for noun/adjective word-family data
            // specifically (a confirmed source limitation, not merely "not yet reviewed"). Not
            // device-confirmed either.
            //
            // D-445-followup (`dictionaries/pt/generate_plurals.py`, new): a conservative, rule-based noun/
            // adjective PLURAL generator, built on explicit user request to close part of the gap above,
            // scoped deliberately narrowly to only mechanical, low-risk suffix rules - vowel-ending -> +s;
            // -m -> -ns; -r -> +es; -z -> +es (skipped when preceded by "ui"/"ai" - the juiz/raiz-style
            // hiatus-accent risk); -al/-el/-ol/-ul -> -ais/-éis/-óis/-uis (skipped when the word already
            // carries an earlier accent, a proparoxytone signal like cônsul). Every genuinely ambiguous class
            // is explicitly skipped rather than guessed at, per direct instruction not to manufacture data:
            // -s-ending words (lápis/vírus-style invariable vs. mês/país-style oxytone -es, not reliably
            // distinguishable by spelling), -il-ending words (the identical stress ambiguity), -ão-ending
            // words (three genuinely competing patterns - -ões/-ães/-ãos - with real dialectal variation even
            // in reference grammars for some members), and adjective gender-pair (-o/-a) generation (no
            // reliable per-word signal for which adjectives even take this alternation). Calibration ratio
            // (0.4474) measured empirically from 17,223 real already-matched pairs (the predicted plural
            // already existing as its own real dict.tsv entry), the same technique `merge_wiktionary.py`
            // already uses, not guessed. Result: `dict.tsv` 535,869 -> 558,148 rows (+22,279 generated
            // plurals; 17,225 further existing words linked to their singular via `lemma`; 6,736 eligible
            // words correctly matched no safe rule and were left untouched). Quality gate re-verified: 0
            // duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows. `dictionaries/
            // pt/version.txt` 1 -> 2, pack rebuilt. Verbs/adjectives' own Wortfamilien scope from D-445 itself
            // is otherwise unchanged - this round only adds plurals, nothing else.
            version = 2
        ),
        Entry(
            Language.ITALIAN,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-it.zip",
            // D-446: first Italian language pack - see the Portuguese Entry (D-445) directly above for the
            // shared context (the same one-shot autonomous pt/it/nl round, full-dump-only per the user's own
            // mid-round correction, the multiprocessing/hapax-pruning extractor). Italian already used
            // ordinary QWERTY and already had a real character-trigram profile (`language_profiles.tsv`,
            // "it", since D-280).
            //
            // Unlike Portuguese, Italian's own native Wiktionary edition (`kaikki.org/dictionary/downloads/
            // it/it-extract.jsonl.gz`, 40.0MB - again smaller than the wrong English-Wiktionary-coverage
            // file, 74.2MB, the same reversed-from-French/Spanish size pattern Portuguese's own round found,
            // still the correct mandatory choice regardless) documents noun/adjective inflection RICHLY, not
            // sparsely - confirmed directly before assuming Portuguese's own scope limitation would repeat
            // here: 22,738 of 37,208 real noun lemmas have real plural/gender forms, 12,072 of 14,972
            // adjective lemmas have real plural/gender/superlative forms (both far above Portuguese's own
            // 137/52,477 and 70/18,413). Verbs show the identical individually-paged-conjugated-form shape
            // every native edition this project has processed shows (462,027 raw "verb" entries, 454,157
            // `senses[].form_of` references - ~7,870 real lemmas). Full three-category Wortfamilien
            // completion therefore applies here, matching French's/Spanish's own parity rather than
            // Portuguese's own verb-only scope.
            //
            // The entire `itwiki-latest-pages-articles.xml.bz2` (4.24GB compressed, the largest dump this
            // project has processed) was processed via the same multiprocessing/hapax-pruning extractor
            // D-445 built - 1,984,914 real pages, 785,078,705 real tokens, the largest raw-token count of any
            // language pack this project has built so far, completed well within the same overnight run.
            //
            // `dict.tsv`: 613,988 rows (455,757 from the initial Wikipedia-frequency + kaikki-POS merge,
            // +158,231 from full Wortfamilien completion - 8,862 noun, 143,232 verb, 6,137 adjective
            // generated forms; calibration ratios noun=0.4340 (n=16,685 pairs), verb=0.5000 (n=36,090),
            // adjective=0.5000 (n=20,583) - all three well-populated, unlike Portuguese's own sparse noun/
            // adjective pair counts). POS tagging: 403,958 words kept unrecognised-by-kaikki (corpus count
            // >=20), 3,522,925 dropped below that floor, 15,053 removed as common-English-word contamination.
            // Proper-noun handling: 3,842 tagged, 595 skipped as real-word collisions (the same `all_pos`
            // broad-collision check French's/Spanish's own D-444 round added). Mandatory bare-noun safety
            // check: 0 bare-NOUN rows. `bigram.tsv`: 4,289,075 rows (>=10 cutoff) from 9,161,636 rows at the
            // raw >=3 extraction floor. Quality gate (`dictionaries/quality_gate.py`): 0 case-insensitive
            // duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Italian's own: `hints.tsv` keeps German's
            // 10 language-neutral assignments and gives the remaining 16 letters Italian content - a=à, e=è,
            // i=ì, o=ò, u=ù (Italian's five accented vowels - `diacritics.tsv` keeps the fuller set: a→à;
            // e→è,é; i→ì; o→ò,ó; u→ù), g=«/r=» (quotes), s=€ (currency), t=º/y=ª (ordinal indicators, also a
            // genuine Italian convention - "3ª edizione"), c=§ (section sign, legal/formal use), and
            // j/k/l/w/z filled with generically useful remaining typography (—, …, &, ₤ - a nod to the
            // historical lira, •). `abbreviations.tsv`: a hand-curated ~27-entry Italian sentence-boundary
            // list (sig./dott./prof./avv./ecc./...).
            //
            // `ItalianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null (no single-word "Uhr"-style convention), `bundledConfusablesBlacklist`=empty -
            // `confusables_scan.py dictionaries/it/dict.tsv qwerty 30` found 2,026 candidate pairs, left
            // deliberately uncurated for the same "cannot confidently separate a genuine short word from
            // real corpus noise without native fluency" reasoning every non-German round documents.
            //
            // New tests: `LanguageRulesTest`'s `Italian resolves to ItalianRules` case and its own
            // `ItalianRules`-mirroring test block were already added alongside Portuguese's own commit (both
            // are language-neutral, zero dictionary-file dependency) - only this `Entry`'s own real numbers
            // were pending until this round's data pipeline actually finished. `LanguagePackCatalogTest`
            // needed no change.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been
            // reviewed by anyone who actually speaks Italian. Real, full-dump corpus scale (785.08M real
            // tokens, the largest of any language pack this project has built) and a complete, real lexicon
            // with full noun/verb/adjective Wortfamilien parity - but still "pretty good" in the guide's own
            // sense, not native-reviewed quality. Not device-confirmed either.
            version = 1
        ),
        Entry(
            Language.DUTCH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-nl.zip",
            // D-447: first Dutch language pack, third and final of the pt/it/nl overnight one-shot round -
            // see the Portuguese Entry (D-445) directly above for the shared context (full-dump-only per the
            // user's own mid-round correction, the multiprocessing/hapax-pruning extractor). Dutch already
            // used ordinary QWERTY and already had a real character-trigram profile
            // (`language_profiles.tsv`, "nl", since D-280).
            //
            // Like Italian, Dutch's own native Wiktionary edition (`kaikki.org/dictionary/downloads/nl/
            // nl-extract.jsonl.gz`, 127.8MB - here the native file IS the much bigger one, ~4.4x the wrong
            // English-Wiktionary-coverage file's 29.2MB, matching French's/Spanish's own original "native
            // always bigger" pattern rather than Portuguese's/Italian's own reversed one - confirmed both
            // patterns are real, language-dependent, not a single rule) documents noun/adjective inflection
            // very richly: 130,196 of 149,010 real noun lemmas have real forms (plural, and a genuine
            // Dutch-specific "diminutive" form, e.g. huis -> huisje, kept as a real generated NOUN form since
            // it is a genuine everyday word); 15,706 of 19,440 adjective lemmas have real forms (inflected/
            // comparative/superlative/partitive). Full three-category Wortfamilien completion applies here.
            //
            // **A real, serious bug found and fixed before trusting this round's own output - the identical
            // class of "impossible calibration ratio" French's own D-444-followup already taught this
            // project to watch for, caught the same way (a sanity-check of the calibration's own top
            // outliers, not just checking the pipeline "ran").** The first pass computed a verb calibration
            // ratio of ~173x (n=35,225 pairs) - physically impossible for a real inflected-form/lemma
            // relationship. Root cause, confirmed by direct inspection of real entries: Dutch's own
            // conjugation tables are full periphrastic-tense tables (e.g. "ingebakerd zullen hebben" -
            // future perfect infinitive - or, for separable verbs, a two-word single-clause form like
            // "baker in", verb stem first, separable particle last) - the OPPOSITE shape from French's own
            // "il/elle/on mange" pattern (pronoun-prefix(es) first, real verb last) that the shared
            // `last_token()` recovery helper was built for. Applied here, it silently extracted the wrong
            // half every time - `last_token("baker in")` -> `"in"`; `last_token("ingebakerd zullen
            // hebben")` -> `"hebben"` - wrongly linking 18,025 rows across the whole file to a bare `"in"`/
            // `"hebben"`/`"worden"`/`"zijn"`, each an extremely common, semantically unrelated standalone
            // Dutch word (`"in"` alone: 491,058) whose own astronomical frequency poisoned the ratio.
            // **Fixed at the root** (`dictionaries/nl/extract_wiktionary.py`): no `last_token()` recovery for
            // Dutch at all - any raw form containing whitespace is rejected outright, the same "multi-word
            // forms are excluded" convention every language already applies, simply enforced before any
            // attempt to recover a trailing word rather than after. Dutch's own genuinely useful single-word
            // forms (subordinate-clause forms, both participles) were already present as their own
            // single-word entries, so nothing real was lost - re-run after the fix: verb ratio corrected to
            // a real 0.6667 (n=14,648 pairs), contaminated rows dropped from 18,025 to 5.
            //
            // The entire `nlwiki-latest-pages-articles.xml.bz2` (2.04GB compressed) was processed via the
            // same multiprocessing/hapax-pruning extractor - 2,225,912 real pages, 391,313,011 real tokens.
            //
            // **Net result** (after the fix above): `dict.tsv` 715,368 rows (463,009 from the initial
            // Wikipedia-frequency + kaikki-POS merge, +252,359 from full Wortfamilien completion - 174,646
            // noun, 27,951 verb, 49,762 adjective generated forms; calibration ratios noun=0.5000 (n=24,477
            // pairs), verb=0.6667 (n=14,648, post-fix), adjective=1.0000 (n=7,845)). POS tagging: 331,809
            // words kept unrecognised-by-kaikki (corpus count >=20), 2,058,610 dropped below that floor,
            // 12,614 removed as common-English-word contamination. Proper-noun handling: 6,108 tagged, 917
            // skipped as real-word collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows.
            // `bigram.tsv`: 2,361,683 rows (>=10 cutoff) from 6,046,978 rows at the raw >=3 extraction floor.
            // Quality gate (`dictionaries/quality_gate.py`): 0 case-insensitive duplicates, 0 non-positive
            // frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Dutch's own: `hints.tsv` keeps German's 10
            // language-neutral assignments and gives the remaining 16 letters Dutch content - e=ë, i=ï, o=ö,
            // u=ü (Dutch's trema/diaeresis marks, e.g. coördinatie/geïnteresseerd - `diacritics.tsv` keeps
            // the fuller set: e→ë,é,è; i→ï; o→ö; u→ü), g=„/r=" (Dutch low-quote convention), s=€ (currency),
            // t=— (em dash), c=§ (section sign), a=† (a genuinely Dutch obituary/genealogy convention -
            // "Jan Jansen †1990"), j/k/l/w/y/z filled with generically useful remaining typography (…, &, %,
            // ~, •, ±). `abbreviations.tsv`: a hand-curated ~27-entry Dutch sentence-boundary list (dhr./
            // mevr./dr./prof./bv./enz./...).
            //
            // `DutchRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true,
            // `timeSuggestionWord`="uur" - unlike French/Spanish/Portuguese/Italian, Dutch DOES have a real
            // single-word S-08-style convention after a typed time ("om 14.30 uur"), a genuine locale fact
            // naively fillable rather than left null. `bundledConfusablesBlacklist`=empty -
            // `confusables_scan.py` found 1,364 candidate pairs, left deliberately uncurated for the same
            // "cannot confidently separate a genuine short word from real corpus noise without native
            // fluency" reasoning every non-German round documents.
            //
            // `DutchRules`'s registry/test wiring was already committed alongside Portuguese's own round
            // (§418) - language-neutral, zero dictionary-file dependency. `LanguagePackCatalogTest` needed no
            // change.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been
            // reviewed by anyone who actually speaks Dutch. Real, full-dump corpus scale (391.31M real
            // tokens) and a complete, real lexicon with full noun/verb/adjective Wortfamilien parity (once
            // the periphrastic-form bug above was found and fixed) - but still "pretty good" in the guide's
            // own sense, not native-reviewed quality. Not device-confirmed either. This closes the
            // three-language (pt/it/nl) overnight one-shot round.
            version = 1
        ),
        Entry(
            Language.POLISH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-pl.zip",
            // D-448: first Polish language pack, built via the Language Contribution Guide's own §8
            // real-corpus/real-lexicon pipeline, first of a second two-language (pl/tr) autonomous round
            // (following directly after the pt/it/nl round above and D-445-followup) on explicit user
            // request, after first checking that both languages had everything the pipeline needs (native
            // Wiktionary edition, a real Wikipedia dump, no missing prerequisite). Polish already used
            // ordinary QWERTY (the standard "Polish programmers" layout, diacritics via AltGr - no dedicated
            // layout code needed) but was not yet in the `Language` enum or `language_profiles.tsv` at all -
            // added both this round.
            //
            // Polish's own native Wiktionary edition (`kaikki.org/dictionary/downloads/pl/pl-extract.jsonl.gz`,
            // 130.7MB, correctly bigger than the wrong English-Wiktionary-coverage file's 65.1MB) documents
            // noun/verb/adjective inflection EXTREMELY richly - the richest of any language this project has
            // built: 50,380 of 67,607 real noun lemmas have real forms (the full 7-case declension -
            // nominative/genitive/dative/accusative/instrumental/locative/vocative - singular+plural);
            // 19,060 of 22,126 adjective lemmas (the same 7 cases x gender - masculine/feminine/neuter/
            // nonvirile - plus comparative/superlative). Verbs show a genuinely different shape from every
            // language built before this round: only 801 of 12,402 raw "verb" entries are `senses[].form_of`
            // references (6.5%, vs. 70-95% for French/Spanish/Italian/Dutch/Portuguese) - most Polish verb
            // lemmas carry their own complete conjugation table directly on the one entry, not spread across
            // individually-paged forms.
            //
            // **A real multi-word-form shape check, per the Guide's own D-447 hardening** (never assume the
            // Dutch fix's own "reject any whitespace-containing form outright" order transfers unmodified):
            // Polish DOES have multi-word forms, but of a different, benign shape - a slash-separated
            // alternative where one side is a real word and the other a bare grammatical-note shorthand,
            // e.g. `"jestem / -(e)m"` (the enclitic contraction note for "być"'s own present tense). Splitting
            // on "/" FIRST (the OPPOSITE order from Dutch's own fix) correctly recovers the real word
            // (`"jestem"`) while the shorthand half (`"-(e)m"`) is separately rejected by `VALID_FORM_RE`;
            // genuine two-word reflexive constructions (Polish `się` is always its own separate word, e.g.
            // `"mieć się"`) are still correctly rejected, since splitting on "/" does nothing to a string with
            // no "/" in it, leaving the whitespace check to catch them as before. A separate, small (9-row)
            // real data quirk was also found and fixed: `"nie"` ("no") turns up as a literal placeholder value
            // in a handful of conjugation-table cells meaning "this form does not exist" (the same role
            // `"-"`/`"—"` already play) - confirmed directly against one real raw entry (`tyć`'s own passive
            // participle slot) before excluding it, not guessed.
            //
            // The entire `plwiki-latest-pages-articles.xml.bz2` (2.73GB compressed) was processed via the
            // same multiprocessing/hapax-pruning extractor D-445 built - 1,706,354 real pages, 383,934,279
            // real tokens.
            //
            // **Net result**: `dict.tsv` 884,720 rows (491,933 from the initial Wikipedia-frequency +
            // kaikki-POS merge, +392,787 from full Wortfamilien completion - 147,229 noun, 153,306 verb,
            // 92,252 adjective generated forms; calibration ratios noun=0.3684 (n=64,346 pairs), verb=0.9474
            // (n=26,141), adjective=0.6667 (n=47,068) - all three sane, re-verified against the Guide's own
            // new mandatory calibration-ratio sanity check before trusting them). POS tagging: 425,849 words
            // kept unrecognised-by-kaikki (corpus count >=20), 3,487,618 dropped below that floor, 14,510
            // removed as common-English-word contamination. Proper-noun handling: 6,203 tagged, 756 skipped
            // as real-word collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`:
            // 2,855,627 rows (>=10 cutoff) from 7,167,156 rows at the raw >=3 extraction floor. Quality gate:
            // 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN
            // rows - PASS.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Polish's own: `hints.tsv` largely keeps
            // German's 10 language-neutral assignments (`n` was reassigned to `ń`, since Polish's own
            // diacritic set is systematic enough - unlike Spanish's single `ñ` - to deserve its natural base
            // letter over the neutral reuse; `+` moved to `j` instead) and gives the remaining letters real
            // Polish content: `a=ą, c=ć, e=ę, l=ł, n=ń, o=ó, s=ś, z=ż` (the eight base letters with a real
            // diacritic - `diacritics.tsv` keeps the fuller set, `z`->`ź,ż`, the only base letter with two
            // genuine variants), `g=„`/`r="` (the Polish low-quote convention, matching German's own), `t=zł`
            // (the złoty currency symbol, a genuine two-character glyph - `LetterHints.MAX_SYMBOL_LENGTH` is
            // 2, so it fits), `k=—` (Polish's own em-dash dialogue convention), `w=§` (common in Polish legal/
            // administrative text), `i/u/y` filled with generically useful remaining typography (`…`, `&`,
            // `•`). `abbreviations.tsv`: a hand-curated ~25-entry Polish sentence-boundary list (`p.`/`np.`/
            // `tzn.`/`dr.`/`prof.`/...).
            //
            // `PolishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,198 candidate pairs,
            // left deliberately uncurated for the same reasoning every non-German round documents.
            //
            // New tests: `LanguageRulesTest` gained a `Polish resolves to PolishRules` case plus its own
            // `PolishRules`-mirroring test block. `LanguagePackCatalogTest` needed no change.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been
            // reviewed by anyone who actually speaks Polish. Real, full-dump corpus scale (383.93M real
            // tokens) and a complete, real lexicon with full noun/verb/adjective Wortfamilien parity (the
            // richest inflectional data of any language this project has built) - but still "pretty good" in
            // the guide's own sense, not native-reviewed quality. Not device-confirmed either.
            version = 1
        ),
        Entry(
            Language.TURKISH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-tr.zip",
            // D-449: first Turkish language pack, second and final of the pl/tr autonomous round - see the
            // Polish Entry (D-448) directly above for the shared context. Turkish already used ordinary
            // QWERTY (the dominant real-world "Turkish Q" convention, not the historical, now largely
            // superseded "Turkish F" arrangement) and was not yet in the `Language` enum at all - added
            // (`"tr"`, endonym `"Türkçe"`) this round, alongside Polish.
            //
            // Turkish's own native Wiktionary edition (`kaikki.org/dictionary/downloads/tr/tr-extract.jsonl.gz`,
            // 43.3MB, correctly bigger than the wrong English-Wiktionary-coverage file's 34.1MB) documents
            // noun/adjective inflection richly - 55,687 of 91,839 real noun lemmas have real forms (the full
            // 6-case system: nominative/genitive/dative/accusative/locative/ablative, singular+plural+
            // possessive-person paradigms); 11,009 of 12,081 adjective lemmas (comparative/superlative plus
            // the same case system used predicatively). Verbs show the same "most lemmas carry their own
            // full forms[] table directly" shape Polish's own D-448 round found (only 6,621 of 77,658 raw
            // "verb" entries, 8.5%, are `senses[].form_of` references).
            //
            // **Two real, structural Turkish-specific findings, both confirmed by direct inspection before
            // trusting any output, not assumed:**
            // 1. **Turkish is a postpositional, not prepositional, language, and this Wiktionary edition has
            //    no dedicated closed-class tag for its own postpositions** - checked directly: 0 entries
            //    tagged `prep`/`prep_phrase` anywhere in the whole file. Real Turkish postpositions
            //    (`için`/`gibi`/`kadar`/`göre`/`ile`/...) are instead tagged inconsistently across
            //    `particle`/`adv`/`conj`/`noun`/`adj` depending on the specific word's own other senses -
            //    several of the best-known ones (`sonra`/`doğru`/`karşı`/`dolayı`) already carry real,
            //    competing noun/adjective/adverb senses in this same data. A small hand-curated exception
            //    list was considered and deliberately not built - confidently deciding which of these should
            //    ALSO gain `PREPOSITION` without native Turkish fluency was judged too risky, the same
            //    reasoning that ruled out a curated list for Portuguese's own "-ão" ambiguity. `dict.tsv`
            //    therefore has 0 `PREPOSITION`-tagged rows for Turkish - a genuine, structural finding, not
            //    an oversight (see the Language Contribution Guide's own preposition-tagging step, which now
            //    names this exact exception).
            // 2. **A real ~3,000x-ratio calibration bug, the identical symptom-class Dutch's own D-447 bug
            //    and French's own D-444-followup bug share, caught the same way** (the mandatory
            //    calibration-ratio sanity check the Guide now requires after both): the first adjective
            //    calibration pass found two pairs with ratios of 3105x/3014x - `"obez"` ("obese", freq 34)
            //    linked to bare `"en"` (105,577) and bare `"daha"` (102,492), the analytic comparative/
            //    superlative marker words Turkish ordinarily writes attached to the adjective ("daha X"/
            //    "en X" - see finding 1's own sibling note in `extract_wiktionary.py`'s module docstring).
            //    Confirmed via the real raw JSON entry: `"obez"`'s own `forms[]` documents these two markers
            //    completely bare, without the adjective attached, unlike every other sampled adjective
            //    (`aralık`/`gri`/`Fransızca`/`Türkçe`/`Almanca`, all correctly `"daha X"`/`"en X"`) - a
            //    genuine, isolated source annotation inconsistency (confirmed: only this one word, 2 of
            //    21,994 comparative/superlative forms in the entire file), not a systemic pattern needing a
            //    structural fix. Fixed with a narrow, evidence-based exclusion (bare `"daha"`/`"en"` values
            //    specifically, only when tagged comparative/superlative) - re-run confirmed 0 remaining
            //    contamination, adjective ratio corrected from the impossible 3105x down to a real 0.0303
            //    (n=195).
            //
            // **A third, genuinely different kind of finding - a real casing bug in this project's OWN
            // extraction code, found and fixed before it could silently corrupt the dictionary content
            // itself** (unrelated to the still-open, deliberately-deferred question of how the *app itself*
            // should capitalise Turkish text, see below): ordinary Python `str.lower()` gets the Turkish
            // dotted/dotless I pair wrong - Unicode's default casefolding maps ASCII `"I"` to dotted `"i"`,
            // but Turkish orthography requires `"I"` -> dotless `"ı"` and `"İ"` (dotted capital) -> `"i"`
            // (not the 2-character combining-dot sequence Unicode's own default casefolding of U+0130
            // actually produces). Confirmed the real, silent-corruption risk directly: plain Python
            // `"IŞIK".lower()` produces `"işik"` - a *different, still-plausible-looking* Turkish word, not
            // an error, so this would have corrupted frequency data silently rather than failed loudly. A
            // small `turkish_lower()` helper (swap İ/I to their correct Turkish lower-case forms first, then
            // defer to ordinary Unicode rules for ç/ğ/ö/ş/ü, which have no such quirk) is used throughout
            // both `extract_wiki_dump.py`'s own tokenisation and `extract_wiktionary.py`'s own word/form
            // normalisation - spot-checked directly against the real corpus output before trusting it
            // (`"ışık"` correctly appears at real frequency 15,401; the wrong spelling `"işik"` appears only
            // twice, consistent with organic corpus noise rather than a systematic casing failure). This is
            // pure data-correctness work (getting Turkish *spellings* right in the dictionary itself), fully
            // separate from and not a substitute for the still-open, deliberately-deferred design question
            // named below.
            //
            // The entire `trwiki-latest-pages-articles.xml.bz2` (1.05GB compressed) was processed via the
            // same multiprocessing/hapax-pruning extractor - 698,557 real pages, 136,688,360 real tokens.
            //
            // **Net result**: `dict.tsv` 706,502 rows (257,407 from the initial Wikipedia-frequency +
            // kaikki-POS merge, +449,095 from full noun/verb/adjective Wortfamilien completion - calibration
            // ratios noun=0.0667 (n=33,751 pairs), verb=0.2797 (n=8,090), adjective=0.0303 (n=195, post-fix) -
            // all three re-checked against the Guide's own mandatory sanity check before being trusted).
            // POS tagging: 182,351 words kept unrecognised-by-kaikki (corpus count >=20), 1,909,033 dropped
            // below that floor, 13,477 removed as common-English-word contamination. Proper-noun handling:
            // 21,575 tagged, 3,956 skipped as real-word collisions. Mandatory bare-noun safety check: 0
            // bare-NOUN rows. `bigram.tsv`: 1,122,647 rows (>=10 cutoff) from 3,536,280 rows at the raw >=3
            // extraction floor. Quality gate clean.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Turkish's own: the real, closed 29-letter
            // Turkish alphabet (deliberately excludes q/w/x, not native letters) has six real diacritic
            // letters, each with exactly one variant - `c=ç, g=ğ, i=ı, o=ö, s=ş, u=ü` (`diacritics.tsv` keeps
            // the same one-to-one set, no base letter here has more than one real variant). German's 10
            // language-neutral assignments fit without conflict this time (unlike Polish's own `n`/`ń`
            // clash) - `b=*, d=°, f=ƒ, h=#, m=-, n=+, p=π, q=@, v=/, x=×`. Remaining letters: `t=₺` (the
            // Turkish lira symbol, a real dedicated Unicode currency character), `l=«`/`r=»` (quotation
            // marks), `a/e/j/k/w/y/z` filled with generically useful remaining typography (`&`, `…`, `—`,
            // `%`, `§`, `•`, `±`). `abbreviations.tsv`: a hand-curated ~24-entry Turkish sentence-boundary
            // list (`dr.`/`prof.`/`doç.`/`vb.`/`vs.`/...).
            //
            // `TurkishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 4,461 candidate pairs
            // (the largest count of any language scan so far), left deliberately uncurated for the same
            // reasoning every non-German round documents. None of `TurkishRules`'s nine hooks touch
            // capitalisation at all - deliberately, see below.
            //
            // New tests: `LanguageRulesTest` gained a `Turkish resolves to TurkishRules` case plus its own
            // mirroring test block. `language_profiles.tsv` (A-03 trigram data) NOT built for Turkish this
            // round either, same accepted, named gap as Polish's own entry above.
            //
            // **Deliberately NOT attempted this round, per explicit user instruction: Turkish's own dotted/
            // dotless İ/I capitalisation behaviour.** Unlike every other implemented language, Turkish maps
            // "I" (capital) -> "ı" (dotless) and "i" (lower) -> "İ" (dotted capital) when capitalising -
            // genuinely different from the ASCII i/I pair every other language here treats as the same
            // letter. This project's own capitalisation code was checked and confirmed to use Kotlin's
            // locale-invariant `.lowercase()`/`.uppercase()` everywhere (`toLowerCase()`/`toUpperCase()`,
            // the locale-*default*-dependent legacy calls, are not used anywhere in this codebase) - so no
            // other language's own capitalisation is put at risk by Turkish's presence, but genuinely
            // correct Turkish capitalisation ("istanbul" -> "İstanbul") is simply not implemented anywhere
            // yet. This is a real design question for `CapitalisationEngine` itself, explicitly deferred for
            // a dedicated discussion once this round's own pure data work was done - not decided, not
            // guessed at, and not silently left "probably fine".
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been
            // reviewed by anyone who actually speaks Turkish. Real, full-dump corpus scale (136.69M real
            // tokens) and a real lexicon with full noun/verb Wortfamilien coverage - but still "pretty good"
            // in the guide's own sense, and, unlike every other implemented language, genuinely incomplete
            // for prepositions specifically (a confirmed structural/linguistic finding, not an oversight -
            // see above). Not device-confirmed either. This closes the two-language (pl/tr) autonomous round.
            version = 1
        ),
        Entry(
            Language.SWEDISH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-sv.zip",
            // D-450: first of a large (18-language) autonomous round - see AdaptKey-Progress.md's own D-450
            // entry for the full structural context (which of the 18 have a native kaikki.org Wiktionary
            // edition and which do not, confirmed directly against `kaikki.org/dictionary/rawdata.html` before
            // starting, not assumed). Swedish was NOT yet in the `Language` enum - added (`"sv"`, endonym
            // `"Svenska"`) this round, alongside all 17 other new languages added in the same batch.
            //
            // **No native Swedish Wiktionary edition exists on kaikki.org.** This pack was built from the
            // English Wiktionary's own coverage of Swedish instead (`kaikki.org/dictionary/Swedish/kaikki.org-
            // dictionary-Swedish.jsonl.gz`, 32.3MB) - the same accepted fallback already used for Dutch/
            // Polish's own predecessors where applicable, now explicitly named in the Guide as the standing
            // approach for any language without a native edition (see the Guide's own newly-added fallback-
            // source requirement below). Languages with a real native edition on this project so far: German,
            // English, Greek, French, Spanish, Portuguese, Italian, Dutch, Polish, Turkish, and (within this
            // same 18-language round) Czech, Indonesian, Malay.
            //
            // The entire `svwiki-latest-pages-articles.xml.bz2` was processed via the same multiprocessing/
            // hapax-pruning extractor built for every prior round - 2,627,827 real pages, 268,255,177 real
            // tokens, 3,950,407 distinct words, 5,501,462 raw (>=3) bigram rows.
            //
            // **A real multi-word-form shape check, per the Guide's own D-447 hardening**: confirmed directly
            // that Swedish's own analytic comparative/superlative construction is marker-FIRST ("mer X"/
            // "mest X"), the same shape already found for Dutch's periphrastic verbs and Turkish's own "daha
            // X"/"en X" - so the same "reject any whitespace-containing form outright, no last_token recovery"
            // rule applies here too (unlike Polish's own marker-LAST "jestem / -(e)m" shape, which needed the
            // opposite fix). `EXCLUDE_FORM_TAGS` gained a new `"error-unrecognized-form"` entry, a genuine new
            // wiktextract diagnostic tag found in this data specifically.
            //
            // **Net result**: `dict.tsv` 521,823 rows (374,316 from the initial Wikipedia-frequency + kaikki-
            // POS merge, +147,507 from Wortfamilien completion - 111,218 noun + 20,546 verb-delta + 15,743
            // adjective-delta generated forms; calibration ratios noun=0.2857 (n=22,225 pairs), verb=0.6667
            // (n=15,249), adjective=0.9000 (n=8,223) - all three sane, checked against the Guide's own
            // mandatory calibration-ratio sanity check before being trusted). POS tagging: 328,566 words kept
            // unrecognised-by-kaikki (corpus count >=20, tagged `OTHER` only), 3,561,716 dropped below that
            // floor, 14,375 removed as common-English-word contamination. Wiktionary matching: 44,084 lemmas
            // tagged with real grammatical info, 3,446 unmatched; 46,461 existing forms linked to their lemma,
            // 147,507 forms generated. Proper-noun handling: 2,287 tagged, 22 unmatched, 190 skipped as real-
            // word collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,689,546
            // rows (>=10 cutoff) from the 5,501,462-row raw floor. Quality gate: 0 case-insensitive
            // duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
            //
            // **What exactly is thinner here, and its concrete effect on this pack's own app behaviour** (per
            // explicit user instruction - not a generic disclaimer): of `dict.tsv`'s 521,823 rows, only 44,084
            // lemmas (plus 2,287 proper nouns) - about 8.4% of the pre-Wortfamilien 374,316 base entries -
            // carry a real kaikki-derived POS tag and `lemma`/form link at all. The remaining 328,566 rows
            // (tagged `OTHER` only) are real Swedish words by Wikipedia-corpus frequency, but this thin English-
            // Wiktionary-of-Swedish coverage simply does not document what part of speech they are. This has
            // two concrete, mechanism-level consequences, not just "less complete":
            // 1. **A-05's split-safety gate** only vetoes a wrong compound split when it can see a genuine
            //    `NOUN` tag on the candidate word. A real Swedish noun that landed in the untagged/`OTHER`-only
            //    328,566 bucket (because this fallback source never covered it) has no such tag, so this
            //    protection silently does not apply to it - a real Swedish compound built from such a noun
            //    could be wrongly offered as two separate shorter words where a native-edition language (e.g.
            //    Polish/Turkish) would have been protected.
            // 2. **D-404 Tier 2's family-match ratio override** only fires between forms connected by a real
            //    `lemma` link - exactly the 44,084+46,461+147,507 rows this round's Wiktionary matching
            //    produced. A correct-but-rarer Swedish word among the 328,566 untagged rows cannot benefit from
            //    this override at all: if a more frequent, merely-related word's inflected form competes with
            //    it in a suggestion, the untagged-but-correct word can be wrongly out-ranked, whereas a linked
            //    pair would have been protected by the ratio check. In practice: expect noticeably more manual
            //    curation need for Swedish (and every other fallback-sourced language this round) than for
            //    Polish or Turkish's own native-edition packs.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Swedish's own: `a=ä, o=ö` (Swedish's two
            // umlauted vowels), `t=kr` (the krona currency abbreviation, a genuine two-character symbol, fits
            // `LetterHints.MAX_SYMBOL_LENGTH`=2), `g=«`/`r=»` (Swedish's own guillemet quotation convention).
            // `abbreviations.tsv`: a hand-curated 22-entry Swedish sentence-boundary list (`t.ex.`/`dvs.`/
            // `bl.a.`/`m.fl.`/...).
            //
            // `SwedishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,733 candidate pairs,
            // left deliberately uncurated for the same reasoning every non-German round documents (no native
            // Swedish fluency available to safely separate real short words from corpus noise).
            //
            // New tests: `LanguageRulesTest` gained a `Swedish resolves to SwedishRules` case plus its own
            // mirroring test block. `language_profiles.tsv` (A-03 trigram data) not built for Swedish either,
            // same accepted, named gap as every prior non-trigram round.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed
            // by anyone who actually speaks Swedish. Real, full-dump corpus scale (268.26M real tokens) - but
            // thinner Wortfamilien/POS coverage than every native-edition language built so far (see above,
            // concrete numbers and mechanism-level impact, not a vague caveat). Not device-confirmed either.
            version = 1
        ),
        Entry(
            Language.NORWEGIAN,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-nb.zip",
            // D-450 (continued): second language of the same 18-language round, right after Swedish - see the
            // Swedish Entry directly above for the shared structural context (the fallback-source decision,
            // and the mandatory "what's thinner + concrete app impact" documentation requirement this entry
            // follows the same way). Added `Language.NORWEGIAN` (`"nb"`, endonym `"Norsk bokmål"`) to the
            // enum - deliberately Bokmål only, per explicit user decision: kaikki.org splits Norwegian into
            // Bokmål and Nynorsk, and only Bokmål has real, usable Wiktionary/Wikipedia coverage for this
            // pipeline; Nynorsk was not built this round.
            //
            // **No native Norwegian Bokmål Wiktionary edition exists on kaikki.org.** Built from the English
            // Wiktionary's own coverage instead (`kaikki.org/dictionary/.../nb-extract.jsonl.gz`, 9.68MB - the
            // smallest fallback source of this round so far).
            //
            // The entire `nowiki-latest-pages-articles.xml.bz2` (831MB compressed) was processed via the same
            // multiprocessing/hapax-pruning extractor - 689,377 real pages, 150,939,253 real tokens,
            // 2,902,707 distinct words, 3,546,745 raw (>=3) bigram rows.
            //
            // **Multi-word-form shape, verified directly, not assumed to carry over from Swedish**: Norwegian
            // Bokmål shows the identical marker-first analytic comparative/superlative pattern ("mer X"/
            // "mest X") already confirmed for Swedish/Dutch/Turkish - the same "reject any whitespace-
            // containing form outright" rule applies unmodified.
            //
            // **Net result**: `dict.tsv` 250,821 rows (220,865 initial Wikipedia-frequency + kaikki-POS merge,
            // +29,956 from Wortfamilien completion - 22,568 noun + 5,593 verb-delta + 1,795 adjective-delta
            // generated forms; calibration ratios noun=0.3571 (n=16,394 pairs), verb=1.0000 (n=7,283),
            // adjective=0.7846 (n=3,271) - all three sane). POS tagging: 201,380 words kept unrecognised-by-
            // kaikki (tagged `OTHER` only), 2,666,944 dropped below the count->=20 floor, 14,898 removed as
            // common-English-word contamination. Wiktionary matching: 18,794 lemmas tagged with real
            // grammatical info (12,955 noun + 2,444 verb-delta + 3,395 adjective-delta), 775 unmatched;
            // 27,253 existing forms linked, 29,956 generated. Prepositions: 78 tagged, 1 unmatched.
            // Proper-noun handling: 1,138 tagged, 17 unmatched, 41 skipped as real-word collisions. Mandatory
            // bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,169,388 rows (>=10 cutoff) from the
            // 3,546,745-row raw floor. Quality gate: 0 case-insensitive duplicates, 0 non-positive
            // frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
            //
            // **What exactly is thinner here, and its concrete app-level effect** (the same mandatory
            // documentation Swedish's own entry above introduced, repeated here with Norwegian's own real
            // numbers, not reused verbatim): of `dict.tsv`'s 250,821 rows, only 18,794 lemmas plus 1,138
            // proper nouns - about 9.0% of the 220,865 pre-Wortfamilien base entries - carry a real kaikki-
            // derived POS tag and `lemma`/form link. The remaining 201,380 rows are real Norwegian words by
            // Wikipedia-corpus frequency alone, undocumented for part of speech by this thin fallback source.
            // The same two concrete mechanisms are weakened as a direct result: (1) **A-05's split-safety
            // gate** cannot veto a wrong compound split built from any of these 201,380 untagged words, since
            // it has no `NOUN` tag to check - a real Norwegian compound could be wrongly offered as two
            // separate words where a native-edition language would have been protected. (2) **D-404 Tier 2's
            // family-match ratio override** only fires on the 18,794+27,253+29,956 rows this round's
            // Wiktionary matching actually reached - a correct-but-rarer word among the 201,380 untagged rows
            // cannot benefit from it, so a more frequent, merely-related inflected sibling could wrongly out-
            // rank it in a suggestion. Expect the same, real curation need this fallback-sourced pack shares
            // with Swedish's own.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Norwegian Bokmål's own: `a=å, o=ø` (the
            // same base-letter diacritic pattern as Swedish, `diacritics.tsv` additionally keeps `a -> å,æ`
            // for the real second variant), `t=kr` (the same krone currency abbreviation Swedish and Danish
            // share, a genuine two-character glyph), `g=«`/`r=»` (the same guillemet convention as Swedish).
            // `abbreviations.tsv`: a hand-curated 20-entry Norwegian sentence-boundary list.
            //
            // `NorwegianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,896 candidate pairs,
            // left deliberately uncurated for the same reasoning every non-German round documents.
            //
            // New tests: `LanguageRulesTest` gained a `Norwegian resolves to NorwegianRules` case plus its own
            // mirroring test block. `language_profiles.tsv` not built for Norwegian either, same accepted gap.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed
            // by anyone who actually speaks Norwegian. Real, full-dump corpus scale (150.94M real tokens) -
            // but thinner Wortfamilien/POS coverage than every native-edition language (see above, concrete
            // numbers and mechanism-level impact). Not device-confirmed either. Danish and Finnish continue
            // next in the same round.
            version = 1
        ),
        Entry(
            Language.DANISH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-da.zip",
            // D-450 (continued): third language of the same 18-language round, right after Norwegian Bokmål -
            // see the Swedish/Norwegian Entries directly above for the shared structural context. Added
            // `Language.DANISH` (`"da"`, endonym `"Dansk"`) to the enum.
            //
            // **No native Danish Wiktionary edition exists on kaikki.org.** Built from the English
            // Wiktionary's own coverage instead (`kaikki.org/dictionary/.../da-extract.jsonl.gz`, 11.0MB).
            //
            // The entire `dawiki-latest-pages-articles.xml.bz2` (455MB compressed) was processed via the same
            // multiprocessing/hapax-pruning extractor - 315,686 real pages, 80,803,420 real tokens,
            // 1,846,772 distinct words, 2,355,076 raw (>=3) bigram rows.
            //
            // **Multi-word-form shape, verified directly, not assumed to carry over from Swedish/Norwegian**:
            // Danish shows the same marker-first analytic comparative/superlative pattern ("mere X"/"mest X")
            // - the same reject-whitespace-outright rule applies unmodified. `EXCLUDE_FORM_TAGS` gained a new
            // `"error-unknown-tag"` entry, a genuine new wiktextract diagnostic tag found in Danish's own
            // adjective data specifically (not previously seen in sv/nb).
            //
            // **Net result**: `dict.tsv` 208,383 rows (138,330 initial Wikipedia-frequency + kaikki-POS
            // merge, +70,053 from Wortfamilien completion - 58,310 noun + 9,138 verb-delta + 2,605 adjective-
            // delta generated forms; calibration ratios noun=0.2604 (n=13,575 pairs), verb=0.9031 (n=7,282),
            // adjective=0.9791 (n=2,761) - all three sane). POS tagging: 118,631 words kept unrecognised-by-
            // kaikki (tagged `OTHER` only), 1,694,172 dropped below the count->=20 floor, 14,270 removed as
            // common-English-word contamination. Wiktionary matching: 18,715 lemmas tagged with real
            // grammatical info (12,816 noun + 2,547 verb-delta + 3,352 adjective-delta), 1,080 unmatched;
            // 24,402 existing forms linked, 70,053 generated. Prepositions: 60 tagged, 1 unmatched.
            // Proper-noun handling: 1,553 tagged, 15 unmatched, 103 skipped as real-word collisions.
            // Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 702,019 rows (>=10 cutoff)
            // from the 2,355,076-row raw floor. Quality gate: 0 case-insensitive duplicates, 0 non-positive
            // frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
            //
            // **What exactly is thinner here, and its concrete app-level effect** (the same mandatory
            // documentation Swedish's own entry introduced, Danish's own real numbers): of `dict.tsv`'s
            // 208,383 rows, only 18,715 lemmas plus 1,553 proper nouns - about 14.7% of the 138,330 pre-
            // Wortfamilien base entries, the richest ratio of the three Nordic languages so far - carry a
            // real kaikki-derived POS tag and `lemma`/form link. The remaining 118,631 rows are real Danish
            // words by Wikipedia-corpus frequency alone, undocumented for part of speech. The same two
            // mechanisms are weakened: (1) A-05's split-safety gate cannot veto a wrong compound split built
            // from any of these 118,631 untagged words, since it has no `NOUN` tag to check - a real Danish
            // compound could be wrongly offered as two separate words. (2) D-404 Tier 2's family-match ratio
            // override cannot fire for a correct-but-rarer word among the 118,631 untagged rows, so a more
            // frequent, merely-related inflected sibling could wrongly out-rank it in a suggestion.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Danish's own: `a=å, o=ø` (the same base-
            // letter diacritic pattern as Swedish/Norwegian), `t=kr` (the same krone abbreviation), `g=„`/
            // `r="` (Danish's own German-style low-quote convention, unlike Swedish/Norwegian's guillemets -
            // verified directly, not assumed to match its Nordic neighbours). `abbreviations.tsv`: a hand-
            // curated 20-entry Danish sentence-boundary list.
            //
            // `DanishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 2,544 candidate pairs,
            // left deliberately uncurated for the same reasoning every non-German round documents.
            //
            // New tests: `LanguageRulesTest` gained a `Danish resolves to DanishRules` case plus its own
            // mirroring test block. `language_profiles.tsv` not built for Danish either, same accepted gap.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed
            // by anyone who actually speaks Danish. Real, full-dump corpus scale (80.80M real tokens) - but
            // thinner Wortfamilien/POS coverage than every native-edition language (see above, concrete
            // numbers and mechanism-level impact). Not device-confirmed either. Finnish continues next, the
            // last of the four-language Nordic batch, before the remaining 14 languages of this round.
            version = 1
        ),
        Entry(
            Language.FINNISH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-fi.zip",
            // D-450 (continued): fourth and final language of the Nordic batch within the same 18-language
            // round, right after Danish - see the Swedish/Norwegian/Danish Entries directly above for the
            // shared structural context. Added `Language.FINNISH` (`"fi"`, endonym `"Suomi"`) to the enum.
            //
            // **No native Finnish Wiktionary edition exists on kaikki.org.** Built from the English
            // Wiktionary's own coverage instead (`kaikki.org/dictionary/.../fi-extract.jsonl.gz`, 268.7MB -
            // by far the largest fallback source checked so far, and unusually rich for one: 125,244 of
            // 125,814 real noun lemmas have real forms, near-total coverage of the full Finnish case system).
            //
            // The entire `fiwiki-latest-pages-articles.xml.bz2` (983MB compressed, the largest dump of this
            // round) was processed via the same multiprocessing/hapax-pruning extractor - 624,121 real pages,
            // 129,787,537 real tokens, 2,794,769 distinct words, 3,518,226 raw (>=3) bigram rows. Finnish's
            // own postposition tag (`postp`) is clean and unambiguous (unlike Turkish's own structural gap,
            // D-449) - mapped directly to `PREPOSITION`, 256 tagged.
            //
            // **A genuine, large structural finding requiring a real decision, surfaced to the user rather
            // than guessed at**: an unfiltered first pass generated 13.46 MILLION noun/adjective family
            // forms - Finnish's own real morphology produces ~158 forms per noun lemma (the full case x
            // number paradigm PLUS six possessive-suffix combinations: person x number, e.g. "talo" ("house")
            // has 28 real case+number forms like "talossa"/"taloon" plus ~130 further real possessive-suffix
            // forms like "taloni"/"talollani", confirmed directly against the raw JSON, not assumed) - a
            // dict.tsv that would have been 570MB raw, 20-40x bigger than any other language pack in this
            // project (which range 5-32MB). Presented to the user via `AskUserQuestion` as a genuine
            // structural fork, not decided unilaterally: the user chose to cap generation to the core case x
            // number paradigm only, dropping the possessive-suffix forms. These are unambiguously identified
            // by the `"possessive"`/`"singular-possessive"`/`"plural-possessive"` tags - confirmed these never
            // co-occur with ordinary verb personal-conjugation forms (which use bare `"first-person"`/
            // `"second-person"`/`"third-person"` tags with no `"possessive"` tag alongside), so this exclusion
            // cannot accidentally strip real verb conjugation data, only the genuinely-possessive-suffixed
            // nominal/participial forms (including a handful of Finnish's own possessive-suffixed non-finite
            // verb forms - the 3rd infinitive/temporal converb paradigm - the same class of bloat, dropped for
            // the same reason). Re-run after the cap: 2.83M noun form rows (down from 18.46M), calibration
            // ratios re-checked and still sane.
            //
            // **Multi-word-form shape, verified directly**: a genuine noise pattern was found and confirmed -
            // a literal English-language metadata note, `"no gradation"` (a real grammatical fact about
            // Finnish consonant gradation, but expressed as English prose, not a Finnish word form), tagged
            // `["class"]` - caught for free by the same whitespace-rejection rule every other language in this
            // round already uses, no special-casing needed. `EXCLUDE_FORM_TAGS` gained this `"class"` entry.
            //
            // **Net result (after the paradigm cap)**: `dict.tsv` 2,537,125 rows (380,516 initial Wikipedia-
            // frequency + kaikki-POS merge, +2,156,609 from capped Wortfamilien completion - 1,703,533 noun +
            // 214,675 verb-delta + 238,401 adjective-delta generated forms; calibration ratios noun=0.3846
            // (n=93,590 pairs), verb=0.4438 (n=19,875), adjective=0.2105 (n=23,512) - all three sane). POS
            // tagging: 285,052 words kept unrecognised-by-kaikki (tagged `OTHER` only), 2,400,125 dropped
            // below the count->=20 floor, 14,128 removed as common-English-word contamination. Wiktionary
            // matching: 89,509 lemmas tagged with real grammatical info, 51,437 unmatched; 169,408 existing
            // forms linked, 2,156,609 generated. Proper-noun handling: 7,260 tagged, 196 unmatched, 1,316
            // skipped as real-word collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows.
            // `bigram.tsv`: 1,049,602 rows (>=10 cutoff) from the 3,518,226-row raw floor. Quality gate: 0
            // case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN
            // rows - PASS. Even after the cap, this pack's own `dict.tsv` (94MB raw, 18MB zipped) remains
            // noticeably larger than every other language pack in this project - a genuine, honestly
            // documented consequence of Finnish's own real morphological richness, not an extraction defect.
            //
            // **What exactly is thinner here, and its concrete app-level effect** (the same mandatory
            // documentation Swedish's own entry introduced): of `dict.tsv`'s 2,537,125 rows, only 89,509
            // lemmas plus 7,260 proper nouns - about 25.5% of the 380,516 pre-Wortfamilien base entries, the
            // richest tagged-lemma ratio of any fallback-sourced language this round, reflecting this
            // source's own unusual richness - carry a real kaikki-derived POS tag and `lemma`/form link. The
            // remaining 285,052 rows are real Finnish words by Wikipedia-corpus frequency alone, undocumented
            // for part of speech. The same two mechanisms are weakened: (1) A-05's split-safety gate cannot
            // veto a wrong compound split built from any of these 285,052 untagged words, since it has no
            // `NOUN` tag to check - and Finnish is a genuinely compound-forming language, so this gap is
            // practically relevant. (2) D-404 Tier 2's family-match ratio override cannot fire for a correct-
            // but-rarer word among the 285,052 untagged rows, so a more frequent, merely-related inflected
            // sibling could wrongly out-rank it in a suggestion. A second, separate limitation from the
            // deliberate paradigm cap above: even for the 89,509 tagged lemmas, the family-match override
            // will not recognise a real possessive-suffixed form (e.g. typing "taloni") as belonging to
            // "talo"'s own family, since those forms were deliberately not generated - a real, bounded scope
            // limit, not a bug.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Finnish's own: `a=ä, o=ö` (the two
            // umlauted vowels, matching Swedish), `s=€` (Finland is the only Eurozone country of this
            // Nordic batch, so this differs from Sweden/Norway/Denmark's own `t=kr` krona/krone assignment),
            // `g=«`/`r=»` (Finnish's own guillemet convention, matching Swedish/Norwegian). `abbreviations.tsv`:
            // a hand-curated 15-entry Finnish sentence-boundary list.
            //
            // `FinnishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 2,466 candidate pairs,
            // left deliberately uncurated for the same reasoning every non-German round documents.
            //
            // New tests: `LanguageRulesTest` gained a `Finnish resolves to FinnishRules` case plus its own
            // mirroring test block. `language_profiles.tsv` not built for Finnish either, same accepted gap.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed
            // by anyone who actually speaks Finnish. Real, full-dump corpus scale (129.79M real tokens) and
            // an unusually rich fallback source for its class - but still thinner POS/lemma coverage than a
            // native-edition language (see above, concrete numbers and mechanism-level impact), and a
            // deliberate, user-approved paradigm-size cap that leaves possessive-suffixed forms outside the
            // family-match protection even for tagged lemmas. Not device-confirmed either. **This closes the
            // four-language Nordic batch (Swedish/Norwegian/Danish/Finnish) of the larger 18-language round**
            // - Czech, Slovak, Hungarian, Romanian, Croatian, Bosnian, Serbian, Estonian, Latvian, Lithuanian,
            // Indonesian, Malay, Swahili, and Tagalog remain.
            version = 1
        ),
        Entry(
            Language.CZECH,
            "https://raw.githubusercontent.com/m-froehlich/adaptkey/main/language-packs/adaptkey-lang-cs.zip",
            // D-450 (continued): fifth language of the same 18-language round, first of the cs/sk/hu/ro
            // group - see the Swedish Entry earlier in this file for the shared structural context (the
            // fallback-source decision, and the mandatory "what's thinner + concrete app impact"
            // documentation requirement, applied only where relevant - see below). Added `Language.CZECH`
            // (`"cs"`, endonym `"Čeština"`) to the enum.
            //
            // **Czech DOES have a native Wiktionary edition on kaikki.org** (Wikislovník) - confirmed
            // directly against `kaikki.org/dictionary/rawdata.html` before starting, one of only three
            // languages in this 18-language round with real native coverage (alongside Indonesian and Malay,
            // still to come). `kaikki.org/dictionary/downloads/cs/cs-extract.jsonl.gz` (38.4MB, correctly
            // bigger than the wrong English-Wiktionary-coverage file's 19.7MB) was used, per the Guide's own
            // mandatory pre-flight size check.
            //
            // **A real, honestly documented source characteristic found by direct inspection, not a bug**:
            // this native edition's own proper-noun coverage is unusually thin - only 7 raw "name" entries in
            // the WHOLE file (3 survived to real tagged rows after the usual word-validity/collision checks).
            // Czech Wikislovník evidently does not catalogue proper nouns the way the English Wiktionary's
            // own broader fallback coverage does for other languages this project has built - a real,
            // confirmed gap specific to this one native source, not something a native-edition language
            // should otherwise be expected to have.
            //
            // The entire `cswiki-latest-pages-articles.xml.bz2` (1.30GB compressed) was processed via the
            // same multiprocessing/hapax-pruning extractor - 597,792 real pages, 229,425,091 real tokens,
            // 3,247,211 distinct words, 5,450,935 raw (>=3) bigram rows.
            //
            // **Multi-word-form shape, verified directly**: checked a real verb's own conjugation table
            // ("dělat"/"to do", 28 forms) - no space-containing forms found at all; Czech's own periphrastic
            // past tense ("dělal jsem") is apparently not documented as a single multi-word forms[] entry in
            // this edition, so the standard whitespace-rejection rule (kept regardless, as the universal
            // safety net every language in this project carries) had nothing to catch here specifically.
            //
            // **Net result**: `dict.tsv` 521,940 rows (353,591 initial Wikipedia-frequency + kaikki-POS
            // merge, +168,349 from full Wortfamilien completion - 93,109 noun + 34,943 verb-delta + 40,297
            // adjective-delta generated forms; calibration ratios noun=0.3019 (n=46,061 pairs), verb=0.5556
            // (n=13,467), adjective=0.4478 (n=27,947) - all three sane). POS tagging: 321,624 words kept
            // unrecognised-by-kaikki (tagged `OTHER` only), 2,879,028 dropped below the count->=20 floor,
            // 14,592 removed as common-English-word contamination. Wiktionary matching: 32,140 lemmas tagged
            // with real grammatical info, 4,667 unmatched; 89,122 existing forms linked, 168,349 generated.
            // Proper-noun handling: 3 tagged, 0 unmatched, 0 skipped (the thin native proper-noun coverage
            // noted above). Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,974,855 rows
            // (>=10 cutoff) from the 5,450,935-row raw floor. Quality gate: 0 case-insensitive duplicates, 0
            // non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
            //
            // Being a native-edition language, Czech does NOT carry the "what exactly is thinner" mandatory
            // documentation Swedish/Norwegian/Danish/Finnish's own entries above require for fallback-sourced
            // languages - 32,140 lemmas + 3 proper nouns of 353,591 pre-Wortfamilien base entries (~9.1%)
            // carry a real POS/lemma link, broadly comparable to the Nordic fallback languages' own ratios
            // despite being native-sourced, since Czech Wikislovník's own coverage breadth (not richness per
            // documented word) is the limiting factor here rather than source-type thinness.
            //
            // `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Czech's own: 13 base letters carry a real
            // diacritic (`a=á, c=č, d=ď, e=é/ě, i=í, n=ň, o=ó, r=ř, s=š, t=ť, u=ú/ů, y=ý, z=ž`) - the most of
            // any language this project has built so far - filling all 26 letter slots without room left for
            // a dedicated currency symbol (Czech koruna "Kč" was considered and deliberately dropped in favour
            // of the diacritic coverage). `g=„`/`h=""` (Czech's own low-quote convention).
            // `abbreviations.tsv`: a hand-curated 23-entry Czech sentence-boundary list (`např.`/`tj.`/
            // `tzv.`/`atd.`/...).
            //
            // `CzechRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`
            // =null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,897 candidate pairs,
            // left deliberately uncurated for the same reasoning every non-German round documents.
            //
            // New tests: `LanguageRulesTest` gained a `Czech resolves to CzechRules` case plus its own
            // mirroring test block. `language_profiles.tsv` not built for Czech either, same accepted gap.
            //
            // **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed
            // by anyone who actually speaks Czech. Real, full-dump corpus scale (229.43M real tokens) and a
            // genuinely native-sourced Wiktionary edition - but still "pretty good" in the guide's own sense,
            // not native-reviewed quality, and with a confirmed, honestly documented proper-noun coverage gap
            // specific to this source. Not device-confirmed either. Slovak, Hungarian, and Romanian continue
            // next in the same round.
            version = 1
        )
    )
}

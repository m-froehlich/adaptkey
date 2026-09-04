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
            version = 5
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
            version = 1
        )
    )
}


## §529 - D-504: auto-split tuning - the split gets its own, drastically stricter silent bar, and the German dictionary loses its junk (v1.2.92)

User report: "Am meisten stört das automatische Splitten. Die Split-Wahrscheinlichkeit muss drastisch
sinken. Das passiert einfach zu häufig." Root causes measured, not guessed: (1) the non-noun frequency
curve saturates at 25, so practically every dictionary word scores 1.0 as a split half and the D-473
product structure protects nothing - one reasonably frequent noun half carried any partner silently
(D-493's "sch" 84 × "Meißen" 352 = 0.77 ≥ MEDIUM's 0.75); (2) the structural half floor was 10 -
noise-level; (3) the dictionary itself carried the junk the rules worked on.

Code half: `CorrectionConfidence.forSplit` returns exactly 0.0 when either half's resolved frequency is
below `SPLIT_SILENT_HALF_FREQUENCY = 300` (the established D-114 "trustworthy" bar), and both silent
call sites compare against the split-specific `SPLIT_AUTO_APPLY_CONFIDENCE = 0.90` (CAUTIOUS's own auto
threshold) instead of the aggressiveness level's - decoupled from C-22 by design, C-21 still governs
the mode. Chips/live preview untouched (TokenRepair never filters on confidence). `MIN_SPLIT_HALF_FREQUENCY`
stays 10 deliberately: chips are ignorable. New `SplitEvaluationTest` (real dict.tsv + bigram.tsv,
FusionEvaluationTest's shape): usage-weighted silent recall over joined attested bigrams 84.7% after
cleanup (84.2% before - the cleanup slightly improved it; wrong-position silent share 2.7%); broad
random population 35,280 structural splits, 139 still silent (0.4%); D-493's shape pinned at exactly
0.0. One test bug found and fixed along the way: the floor assertion must look up the RESOLVED spellings
(D-268), not the typed halves ("fur" has no row; "für" does).

Data half: new `dictionaries/clean_dict.py` (rules R1-R5, report/apply, full decision log in
`scratchpad/d504-de-cleanup-*`). German applied: 5,709 decisions - 3,802 uncommon proper nouns dropped
(PROPER_NOUN, freq < 500, unshielded), 190 explicit drops ("Meißen" 352 per user decision + the
187-row 300-499 band hand-reviewed row by row; that band's 6 mis-tagged common nouns retagged
instead), 1,638 retags (lowercase sentence-case artefacts + the Wiktionary-noun real-common-noun
shield - German surnames ARE common nouns, so word-vs-name is decided by curated ground truth, never
by frequency alone - + the compound shield for Marktplatz-class words), 28 vowel-less fragments
dropped (prefix evidence only, so units/abbreviations like bzw/km/cm survive by construction plus an
explicit keep-list), 13 zero-doubt English strays dropped. dict.tsv 193,814 -> 189,781 rows; spot
checks: "sch"/"Meißen" absent, "Marktplatz"/"Achsel"/"Gala"/"Ebbe" kept as NOUN, "Berlin"/"bzw"
untouched. `dictionaries/de/version.txt` 48 -> 49, pack rebuilt via the new
`dictionaries/de/build_zip.py` and verified byte-identical after re-unzip, `LanguagePackCatalog`
German version 48 -> 49 with the round's changelog comment.

`:app:assembleRelease` + full suite green (1,780 tests). Spec A-05 gained the D-504 addendum; the
cleanup workflow moved into the contribution guide for native-speaker pack maintainers (D-506's
§530 below); cross-language report-only scan of all 30 other packs in
`scratchpad/d504-crosslang-report.txt` (R2 drops nothing without a per-language noun shield; the
first scan's Cyrillic/Greek R4 miscounts led to the vowel set covering Cyrillic and Greek). Rides
versionCode 588 / 1.2.92 together with D-506 (1.2.91/587 had been consumed by D-505's streaming
round in between). **Not yet device-confirmed** - the §7 checklist of
`AdaptKey-Plan-Auto-Split-Tuning.md` closes the round.

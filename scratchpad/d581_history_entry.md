## §581 - D-581: the markup-artefact sweep lands in BOTH dictionaries - nbsp (de rank 21 at 134,624, en rank 75 at 23,034) and the entity fragments out, the 333 non-typable rows out, typeability + markup-denylist anchored in the gate language-neutrally, de pack 54 -> 55 pushed WITH the round (the D-580 lesson applied in-round), bundled-EN reseed forced 3 -> 4

The dictionary-quality-next-level plan's M1, the plan's first round (executed 2026-11-28). Review-list
discipline as planned: survey (scratchpad/d581_survey.py - 17 de / 24 en case-insensitive entity-name
candidates), a lemma-dependency probe before anything was removed, the review list
(scratchpad/d581_review.md) presented and approved in the session chip with exactly one open question:
EN `amp` (142) STAYS - the plan's removal letter was wrong on the data, amp/amping/amped are a genuine
Wiktionary verb family ("to amp up"), and §4's own philosophy ("denylist only the true markup class")
decides Option A. The gate's first EN run then caught `apos` 2 - the 24th survey hit, overlooked in the
review list - removed as d581_apply2.py (the D-570-apply2 precedent); `apo` 5 (Greek transliteration,
not markup) deliberately stays.

DE (189,781 -> 189,430 rows): nbsp 134,624, thinsp 179, lt 175, reg 131, amp 65, para 60 (survey find
inside the named class), copy 35, dagger 18, deg 14, Trade 197, Bull 157 + Bulls 11 (the dependency
probe: Bulls' lemma pointed at Bull - removing Bull alone would have orphaned it, the D-577 gate),
Divide 19; all 333 non-typable rows (embedded space/paren: "des Nord" 916, "(des) Französisch" 463, ...,
333x NOUN, no compound restoration per the plan); the archaic rest (aßt/buk/wob/gor) REMOVED + denylisted,
because the D-206 blacklist mechanism itself was superseded by the D-473-followup (blacklisted-but-present
rows still count as typeable dictionary words - the modern "blacklist" is the gate denylist, and
re-introduction now fails the gate); til 26 (EN leakage, lowercase NOUN). Casing renames: text 5,076 ->
Text (no twin existed, no lemma dependents; typed "text" stays served through the case-fold, and §6
auto-caps now lands on the correct surface), teleskopmasten 25 -> Teleskopmasten. Keep-lowered per the
approved §4 decision: de 13,799 -> 200, jpg 14,825 -> 200, www stays 153; HTTP -> http (140) and
HTTPS -> https (43) retagged NOUN -> OTHER with lowercased surfaces (a German NOUN tag was actively
force-capping typed "http"). Deliberately kept: Euro/Cent/Yen/Times (real readings); Pound 65 named as
English leakage, NOT markup - accepted residual for a future leakage round.

EN (88,745 -> 88,732 rows, bundled): nbsp 23,034 + nbsps 9,384 (plural artefact, lemma nbsp - caught by
the dependency probe), GT 265, ndash 341, Lt 186, mdash 180, thinsp 61, Reg 36, deg 17 + degs 7,
middot 5 + middots 2, apos 2. Kept as real English words colliding with entity names:
times/trade/copy/euro/cent/divide/bull/pound/sect/dagger/para/shy/yen - which is exactly why the denylist
stays per-language curated on top of the neutral base set. Keep-lowered: jpg 8,428 -> 200 (NOUN,OTHER ->
OTHER), de 10,314 -> 200 (a real multi-POS entry - tags kept, documented deviation from the OTHER-tag
letter), HTTP 230 -> 200 and HTTPS 63 retagged PROPER_NOUN -> OTHER with surfaces left as-is in EN (only
DE lowercased, where the tag was actively wrong). Bigram consequence: de -544 rows (79,937 -> 79,393),
en -161 rows (80,000 -> 79,839; the survey's 333 estimate had counted rows touching kept real words).

The gate is M1's own deliverable (dictionaries/quality_gate.py): TYPEABILITY (base, always on - no
space/paren/U+00A0 in the surface, apostrophe explicitly allowed, so the 333-class can never re-enter)
and the MARKUP-ARTEFACT base denylist (the nbsp family, dash/quote entities, deg/GT/Lt/Trade/Divide -
tokens that are words in no served language) run for EVERY language; under --capitalises-nouns (the
capitalising languages today being German - the D-464 flag-keying precedent) the LOWERCASE BARE-NOUN
check (the text-class casing violation) and the German artefact denylist (lt/amp/reg/copy/para/dagger -
genuine English words, hence not in the neutral set - plus the archaic four). The D-575 EN machinery
untouched. Both mandated invocations PASS: de --capitalises-nouns (189,430 rows), en --lang en (88,732
rows).

The one behavioural interaction, chased down and pinned rather than papered over: FusionEvaluationTest's
D-477 random-common-pairs pin (0 fusions at every level) failed 0 -> 4 AGGRESSIVE after the sweep. Exact
replication (scratchpad/d581_fusion_probe2.py): so+de->sonde (230), de+nach->demnach (1,781),
de+noch->dennoch (2,931), de+gegenüber->demgegenüber (418) now pass AGGRESSIVE's both-parts-known
safeguard (fused word >= rarer part) ONLY because de is now 200 - at the old 13,799 every one was
rejected (rarer part 13,799). The approved keep-lowered decision itself flips the safeguard for
fragment+word pairs, and no frequency can satisfy both sides (lowering de makes acceptance easier;
raising it above 2,931 contradicts <= 200). Fix at the test's population level, with the reasoning in a
KDoc: the keep-lowered fragment class (de/jpg/www/http/https) is excluded from FusionEvaluationTest's
`common` - they sit in the top-300 rows by file position but are not common words; the two boundary
newcomers (erhalten, lässt) were probed: 0 fusions. The old-frequency replication confirms 0 accepted
pairs before the sweep, so this pins the D-477 promise for real common words, unchanged in strength.

Deployment, with the D-580 lesson applied INSIDE the round: de version.txt 54 -> 55 (a PowerShell
BOM+CRLF write was caught and stripped - the file is byte-convention "55\nde\n" now,
Integer.parseInt-safe), zip rebuilt with all four files byte-compared against their sources,
LanguagePackCatalog GERMAN 54 -> 55 (round story in its comment), and origin/main pushed as part of the
round - the hosted raw-URL archive reaches every existing install only through the push, which is exactly
what D-580 spent a device round proving. EN bundled: BUNDLED_DICTIONARY_VERSION 3 -> 4 in
DictionaryLoader.kt (the D-578 shape; existing installs reseed the bundled tables on next load, learned
data untouched). dictionaries/en/version.txt stays 1 (the EN source-stamp precedent of D-576/D-578).
build_en_dict.py BUILD VERIFY: PASS (dict.tsv sha256
341a53f019356ea5d5a33b22a92da15f2f73c86a73b162d64e35d706d393ee19; veto census 34/57 unchanged - M1
touches no veto surface). Full suite 2,015 green. Plan note: M1 done; M2 (D-582, the Golden-Set harness)
follows and makes Vorher/Nachher numbers mandatory for every round after it.

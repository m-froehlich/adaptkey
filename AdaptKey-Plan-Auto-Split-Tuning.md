# AdaptKey-Plan-Auto-Split-Tuning.md

**Status:** §3 (code) IMPLEMENTED + evaluated 2026-10-28; §4 (German data half) IMPLEMENTED
2026-10-29 via `dictionaries/clean_dict.py` - 5,709 decisions, dict.tsv 193,814 -> 189,781 rows,
version.txt 48 -> 49, pack rebuilt + byte-verified, `LanguagePackCatalog` 48 -> 49; cross-language
report-only scan of all 30 other packs done (`scratchpad/d504-crosslang-report.txt`; nothing
modified outside de). Full suite green (1,780 tests). Open: §6 spec addendum, §5 version bump
(1.2.90 -> 1.2.91 / 587), §7 device checklist.
**Backlog item:** D-504 (AdaptKey-Progress.md, "Open TODOs"). Request: the automatic A-05 word split
fires far too often; the split probability must drop drastically.

---

## 1. Current state (measured against the code as of v1.2.90 / versionCode 586, re-locate before trusting)

A silent A-05 split today needs:

1. Token unknown, not already recognised, not a plausible verb/adjective inflection, length >= 4.
2. A split point at a T-05 space-ambiguous tap, a space-row letter (character dropped), or any position
   (fully missed space, character kept). `TokenRepair.candidateAt` gates: both halves resolve as real,
   non-blacklisted words, each individually >= `MIN_SPLIT_HALF_FREQUENCY` = **10**, acronym halves >= 300,
   not both nouns, German-only prefix veto lists (D-410/D-404-followup sets).
3. `CorrectionConfidence.forSplit` = `frequencyFactor(left) x frequencyFactor(right)` (product; the split is
   only as confident as its weakest half), compared against `settings.autocorrectAggressiveness.autoApplyThreshold`
   (CAUTIOUS 0.90 / MEDIUM 0.75 / AGGRESSIVE 0.70) at the two silent call sites
   (`AdaptKeyService.finalizeAndCommit` ~line 5125 and the case-locked commit path ~line 4924).

**Why it fires too often — the measured numbers:**

- The non-noun frequency curve saturates at **25** (`CorrectionConfidence.REFERENCE_FREQUENCY`), so practically
  every dictionary word used as a split half scores **1.0** — the product structure protects nothing, because
  one half is almost always a saturating non-noun ("sch", "die", "und", "eh", ...).
- Only the noun half carries real weight (noun curve saturates at 2000). Worked case D-493:
  `sch` (84, OTHER -> 1.0) x `Meißen` (352, NOUN -> ln352/ln2000 = 0.77) = **0.77 >= 0.75 (MEDIUM) -> silent
  split**. A noun half from frequency ~900 upward carries ANY partner half silently at MEDIUM (0.9 at ~1365).
- `MIN_SPLIT_HALF_FREQUENCY = 10` trims nothing: the bundled dict's own floor is 8, and the junk that matters
  (`sch 84 OTHER`, dict.tsv line 20609) sits far above it.

**Data findings (dict.tsv, measured):**

- `sch 84 OTHER` exists (line 20609) — the D-493 split violated no rule; the rules worked on junk data.
- Junk is a class: 3,310 rows of length <= 3 total (function words included; a classified scan is needed).
  Verified junk samples: `jpg`, `svg`, `png`, `shy`, `vec`, `de`, `ca`, `et`, `ha`, `di`, `al`, `to`, `at`,
  `and`, `of`, `for`, `van`.
- `Meißen 352 NOUN,PROPER_NOUN` exists; 6,047 rows carry `PROPER_NOUN` (bands: 206 >= 1000, 217 in 500-999,
  301 in 300-499, 331 in 200-299, 749 in 100-199, 4,243 < 100).
- **Tag pollution discovery:** the top PROPER_NOUN rows include ordinary words mis-tagged as proper nouns by
  the Wikipedia sentence-case extraction: `waren` (31549), `Zeit` (24833), `Welt`, `Art`, `Rolle`, `Mitte`,
  `König`, `August`, `April` (month/name legitimately ambiguous). Any policy keyed on the PROPER_NOUN tag
  must first separate real proper nouns from these artefacts, or it would delete common words.
- `schmeißen` family and `Lebensmüde`: 0 rows — confirmed absent, as documented in D-493/D-469.
- Encoding caveat for all future scans: read with `-Encoding UTF8` (PS 5.1 mis-decoded umlauts above).

---

## 2. Agreed direction (backlog D-504, discussion 2026-10-28)

1. **AGREED:** decouple the split's silent bar from `d353_autocorrect_aggressiveness`; give the split its own
   drastically stricter bar.
2. **AGREED:** absolute, drastically raised frequency floors for the split halves — "sinnbefreit" must bind.
3. **AGREED:** junk-fragment cleanup round (D-306/§301 successor; user: "wenn es das tut, ist das zu
   entfernender Müll").
4. **AGREED:** `Meißen` leaves the dictionary; the round defines the proper-noun class policy.
5. **REJECTED:** a proper-noun right-half veto ("nur eine Mini-Symptom-Bekämpfung").
6. **OPEN (user's call, not this plan's scope):** C-21 default flip to "Chip only".

---

## 3. Code change (one round, pure policy + two call sites)

### 3.1 A split-specific silent bar with its own half floor, inside `CorrectionConfidence.forSplit`

- New constant `SPLIT_SILENT_HALF_FREQUENCY = 300L` — the project's established "trustworthy" bar
  (D-114's `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY`, reused by `MIN_SPLIT_ACRONYM_FREQUENCY`); deliberate
  precedent, not a free number. When **either** half's frequency is below it, `forSplit` returns **0.0**:
  the split can never apply silently, at any aggressiveness level — it still surfaces as chip/live preview
  (TokenRepair itself never filters on confidence, unchanged).
- New constant `SPLIT_AUTO_APPLY_CONFIDENCE = 0.90` — CAUTIOUS's own auto threshold, i.e. the most
  conservative bar the app already uses for silent single-word corrections. Deliberately **not** a per-level
  scale: C-21 (Automatic / Chip only / Off) already governs whether the split may act at all, and a split is
  a bigger intervention than a respelling, so it does not scale with `d353_autocorrect_aggressiveness`.

Worked outcomes of the pair (300, 0.90):

- `sch Meißen` -> 0.0 (84 < 300) — **never silent again**, chip only until the junk row itself is removed.
- `schwimmt Asche` (D-473) -> 0.0 for any sub-300 half; with both halves >= 300 it still needs >= 0.90.
- `der Kinderarzt` (14) -> never silent (rare-noun -> chip, the D-473 precedent, now structural for splits).
- Genuine high-quality splits (`gehört es`, `eh nicht`, `zum Beispiel` — both halves common) -> 1.0, silent.

### 3.2 Call sites

- `finalizeAndCommit`'s split backstop and the case-locked commit path compare against
  `CorrectionConfidence.SPLIT_AUTO_APPLY_CONFIDENCE` instead of `settings.autocorrectAggressiveness.autoApplyThreshold`.
  C-21's mode gate (`autoSplitMode != AUTOMATIC -> no silent split`) stays exactly as it is.

### 3.3 What deliberately does NOT change

- `MIN_SPLIT_HALF_FREQUENCY = 10` (the structural, chip/preview-side floor) stays: chips are ignorable, and
  the junk they would offer disappears with the data rounds below. Revisit after the data rounds land.
- The veto chain (any confidence-cleared correction, raw-coordinate correction result, prefix lists,
  not-both-nouns, umlaut unfolding) is untouched.
- A-06/D-391 fusion is untouched (it already has §499's per-level floors and was tuned separately).

### 3.4 Evaluation and tests (§499's method, permanent)

- New `SplitEvaluationTest` (real bundled dictionary, JVM): 
  - **Recall, usage-weighted:** attested bigrams from `bigram.tsv` joined without a space (the true
    "missed space" population), fed through the split pipeline; recall reported per silent-policy variant,
    weighted by bigram count — the number the threshold choice must be justified against, not vibes.
  - **Coincidence check:** random known-word pairs joined into non-words (plus the historical real cases)
    must never split silently under the new policy.
  - **Pinned real cases:** `schmeißen` never splits (silent or otherwise once the junk row is gone);
    `Schwimmtasche`/`Trotzde`-class stay silent-blocked; a curated positives set still applies silently.
- Run the full suite (`:app:testDebugUnitTest`) + `:app:assembleRelease`; 1767+ tests green.

---

## 4. Data rounds (separate commits, per-candidate confirmation per the standing convention)

**Method note:** all scans with UTF-8-safe tooling; candidates written to `scratchpad/d504-*.txt` lists for
the user's review; nothing is deleted without the explicit OK.

### 4.1 Junk fragments (dict.tsv, then every affected language pack re-checked for the same class)

- Mechanical scan: rows of length <= 3 (3,310 in German) classified into (a) obvious junk (file extensions,
  units, foreign strays, meaningless fragments), (b) legitimate short words (function words, `ab`, `ob`,
  `um`, `ei`?...), (c) needs-decision. The user reviews the list; deletions land with dict version bump +
  pack rebuild/republish per the D-280 contribution pipeline.
- Acceptance rule the user stated: a fragment like `sch` must never be able to carry a split half again.

### 4.2 Proper nouns (German first; the class policy generalises to other packs)

- `Meißen` removed explicitly (user decision).
- Policy proposal for the class — **DECISION POINT for the user:** a frequency floor alone is blunt
  (Wikipedia inflates historical/geographic names; `Meißen` at 352 would survive any floor that keeps e.g.
  mid-size towns, and dies under a ~400 floor that also removes ~200 borderline rows). Proposed combination:
  1. Tag-artefact pass first: PROPER_NOUN rows whose word shape is not a real proper noun (attested as an
     ordinary common word — `Zeit`, `waren`, `Art`, ...) get their `PROPER_NOUN` tag dropped (retag, not
     delete) — fixes the noun-curve/`isNoun` skew too.
  2. For the real proper nouns: remove below a frequency floor (proposal: < 300, the same "trustworthy"
     bar — removes ~5,530 of 6,047) PLUS the explicit user-named removals regardless of frequency
     (`Meißen`, 352, goes this way). Alternative: floor 500 (keeps 423). User picks.

---

## 5. Version / release

- One bump per the version-bump rule, concluding commit after build + tests green: `1.2.90 -> 1.2.91`
  (versionCode 586 -> 587). Data rounds ride in the same feature if the user wants one release, or land as
  their own pack-only round (packs have their own `version.txt` bump + republish, no app bump needed).

## 6. Spec + docs on closure

- Spec A-05 gains the D-504 addendum (split-specific bar, floors, junk/proper-noun cleanup); Progress.md's
  D-504 entry trimmed to the standard RESOLVED stub pointing at the round's §-entry.

## 7. Device-confirmation checklist (closes the round)

1. Type `schmeißen` (and `Lebensmüde`): stays whole as typed — no split, no "sch Meißen".
2. Type a genuine missed-space glue (`gehortes` -> "gehört es", `ehvnicht` -> "eh nicht"): still splits
   silently at the default settings.
3. A mid-quality split (`der Kinderarzt` shape) still offers its chip but never applies silently.
4. Aggressiveness slider (C-22) visibly no longer changes split eagerness (only respelling eagerness).
5. C-21 Chip only / Off still behave as before.

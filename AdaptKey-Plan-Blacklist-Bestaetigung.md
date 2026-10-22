# AdaptKey — Plan: Blacklist Confirmation & Provenance (proposed D-488)

**Status:** IMPLEMENTED end to end (2026-10-22, one session; see §9 for the exact state). Build + all
unit tests green (1719 total, 0 failures; `:app:assembleRelease`/`:app:testDebugUnitTest`), shipped as
§511 / v1.2.71 (versionCode 567). **Not device-confirmed** - R3's surface (chip takeover, reject-drag,
two-commit yield, G-04/W-03 confirmation flow) needs the device; existing mystery blacklist entries need
one manual user review (their provenance reads "unknown").

## 1. Motivation — the verified causal chain

User report: completely normal words stop being accepted / get corrected to something else; the words turn
out to be on the Blacklist, with no memory of when, how or why they got there. The user explicitly ruled out
ever having dragged any of these words away ("nie bewusst weg gewischt"), and the autocorrect-revert paths
were verified clean (A-07's `unlearnWord()` / D-348 / D-248 never write blacklist entries — confirmed in
code and in history §"A-11's backspace-triggered unlearn must not put the word on the blacklist").

The actual chain, traced through code:

1. A bundled word typed in a non-canonical casing (e.g. lower-case mid-sentence) is learned as a casing
   variant — W-04 behaviour, by design.
2. The user cleans the variant up via the Learned Words editor's "Vergessen" button — which unconditionally
   calls `forget()` + `markPendingBlacklist()` ([LearnedWordsActivity.kt:288-289], mirroring G-04's learned
   branch per D-177).
3. The user types the word again, normally. The canonical spelling short-circuits before the escalation
   check in `learnWord()`, but the *variant* spelling does not: no learned entry remains (forgotten), a
   pending mark exists → the D-177 recurrence escalation fires **silently**: `forget()` + permanent
   `blacklist(word, USER)`. The blacklist is keyed case-insensitively, so the whole word form is suppressed
   across casings.
4. Days or weeks later: the word "no longer works". No UI ever appeared at any point in this chain.

This exact trap is on record: the D-253/D-254 repro quoted in `forgetSelfTaughtWord()`'s KDoc
([AdaptKeyService.kt:1504-1512]) — "dragging 'Vergessen' then retyping the word … silently left it
permanently blacklisted". D-254 only defused the W-03 chip's shallow zone; the editor route and the G-04
drag route still work exactly this way today.

**Underlying wrong premise found while tracing** (must be rewritten during implementation): the D-177
comment at the Learned Words editor site claims "learnedWords() itself never returns a bundled one" — false.
`isBundledWord()`/`bundledCasingOf()` resolve case-insensitively (`word.lowercase()`), so the learned list
does contain words that are bundled under a different casing (every casing variant).

## 2. Agreed decisions

1. **Invariant:** no permanent `BlacklistCategory.USER` write ever happens without an explicit chip tap.
   Ignoring or rejecting the chip never blacklists.
2. The confirmation chip covers **all three "surprising" write paths**: the silent D-177 escalation
   (`learnWord()` and `learnHyphenCompound()`), the G-04 drag on a bundled word, and the W-03 "Verbieten"
   deep zone. The explicit UI paths (BlacklistActivity manual add, BackupImporter) stay as they are.
3. On escalation, the word **stays learned** until the chip is confirmed (not forgotten-upfront). Reject or
   ignore **clears the pending mark** (so the chip does not re-fire on every subsequent typing of the word).
4. The D-177 pending window itself stays in place (chip as an additional layer, not a replacement).
5. **New rule (the root-cause fix):** the Learned Words editor's "Vergessen" must **never** set the pending
   mark when `bundledCasingOf(word) != null` — i.e. when the word exists in the bundled dictionary in *any*
   casing. For such words the escalation is semantically wrong: the "recurrence" is just the bundled
   dictionary (or a legitimate W-04 casing variant) doing its job. After the rule, escalation can only ever
   fire for genuinely self-taught words. No migration needed: existing pending marks on bundled-casing words
   expire naturally within the window (default 7 days).
6. Blacklisting is expected to be rare and deliberately slow. If the chip appears unexpectedly often, that
   visibility is itself a diagnostic signal (user's own framing).
7. **Per-entry provenance** is stored persistently with each blacklist entry (origin + timestamp), shown in
   the Blacklist editor, extended through backups. Existing entries migrate as "unknown".
8. Diag lines at every blacklist write path (plus chip-armed events) — not for the user to "notice", but so
   a jointly captured log (§451/§477/§491 convention) names the writing path.
9. Toast after a mark-setting "Vergessen" honestly names the consequence (only reachable for genuinely
   self-taught words after decision 5): "Vergessen. Tippt du es wieder, fragt AdaptKey nach." (draft).
10. A down-drag that **starts on a suggestion chip** is never a G-03 dismiss-down (collision contract, see
    §3.9).
11. Confirming a blacklist never removes already-committed text from the document; the blacklist only
    affects suggestions and the learning pipeline.

## 3. Design

### 3.1 The confirmation chip

- Appearance: red exclamation mark + `Blacklist: <word>` (draft strings; red as a new signal colour —
  precedent for a dedicated chip colour: the loading chip's amber, D-362). Full suggestion-bar takeover
  while up (S-01's established alternate-content slot).
- **Tap = confirm** → write `blacklist(word, USER)` with provenance → refresh bar.
- **Drag down into the keyboard area = reject** → no write; on the escalation shape also clears the
  pending mark. Reuses the DragToTrash machinery, mirrored direction, drop-target model (the W-03 chip's
  shallow/deep *zone* model is a second precedent; here a drop target reads better since rejection is the
  "away" action).
- **Ignore = reject** (safe default): field change defensively clears the chip (D-317's emoji-search
  precedent); while typing in the same field, the chip yields as "ignore" after the **second subsequent
  commit** — the one tunable detail to watch in device feedback. Ignoring never writes.
- Two entry shapes, same visuals: (a) *confirmation of a just-performed gesture* (G-04 drag ending in the
  trash area; W-03 deep zone) — the chip appears instead of the immediate write; (b) *uninvited warning*
  (escalation) — the chip appears during ordinary typing because the system was about to decide on its own.
  The state machine is the same; the trigger differs.
- The chip is pinned state that survives keystrokes and ordinary suggestion refreshes (W-03/clipboard-chip
  precedent), anchored to the decision, not to the composing token.

### 3.2 Escalation rework (`learnWord()` / `learnHyphenCompound()`)

Today: `forget()` + `blacklist()` + `clearPendingBlacklist()` with no UI
([AdaptKeyService.kt:7018-7026] and [7143-7147]). New: arm the chip (reason = escalation), write nothing,
and let the ordinary learn path proceed (the word was just typed; it re-learns normally). On chip confirm:
`forget()` + `blacklist(..., origin = escalation)` + `clearPendingBlacklist()`. On reject/ignore:
`clearPendingBlacklist()` only.

### 3.3 G-04 and W-03

- G-04 drag on a **bundled** word ([onBlacklistWord]'s bundled branch): drag no longer writes; the chip
  appears (reason = user gesture). Confirm → blacklist. Reject/ignore → nothing at all (no mark exists).
- G-04 on a **self-taught** word: unchanged (forget + mark, no chip) — it does not blacklist, and D-177's
  window keeps guarding it. Note: thanks to case-insensitive `isBundledWord()`, a dragged casing *variant*
  of a bundled word routes to the bundled branch — after the chip, blacklisting it is a deliberate,
  confirmed act; the case-insensitive keying means the whole word form is then suppressed. Documented, not
  changed.
- W-03 "Verbieten" deep zone ([onForbidLearnedWord]): the deep drag no longer writes; the chip appears.
  Confirm → blacklist. Reject/ignore → the word simply stays learned. (Decision: uniform invariant, no
  exception for this path — user's call.)

### 3.4 The no-mark rule (root-cause fix)

In `LearnedWordsActivity`'s forget branch: if `bundledCasingOf(entry.word) != null` → plain `forget()`, no
`markPendingBlacklist()`, no warning toast. Otherwise → `forget()` + mark + the honest toast (decision 9).
The D-177 comment's wrong premise (§1) is rewritten in the same change. Optional defense-in-depth: the same
guard in `forgetSelfTaughtWord()` (its remaining caller already routes bundled words away; document, don't
rely on it).

### 3.5 Provenance schema

- `TABLE_BLACKLIST` gains additive columns `origin TEXT` and `added_at INTEGER` (ALTER TABLE, the D-388
  `last_touched` precedent for adding structure without discarding data). `InMemoryDictionaryStore` mirrors.
- Origin values (draft): `gesture-g04` (confirmed drag), `escalation` (confirmed recurrence after a
  mark), `w03-forbid` (confirmed Verbieten), `editor-manual` (BlacklistActivity add), `backup-import`,
  `bundled-seed` (the D-278 idempotently-seeded confusables), `unknown` (migration).
- Existing rows migrate with `origin = unknown`, displayed as "Unbekannt (vor dieser Version)". No
  retroactive provenance exists for them — the user reviews existing mystery entries manually once.
- Backup: `BackupBundle`'s blacklist section extends from `List<String>` to carry optional origin/added_at
  per word; the importer defaults missing fields to `unknown`; old backups keep importing unchanged; the
  exporter writes the new fields.

### 3.6 Provenance UI

The Blacklist entry dialog keeps its §495/§496 shape untouched. A second `squareIconButton()` ("ⓘ") next to
the existing copy button in the word row opens a small popup: title = word, two info lines (origin,
timestamp, localised), buttons "Kopieren" (copies word + origin + timestamp as text — the workflow is
pasting it into a session for joint diagnosis) and "Schließen".

### 3.7 Diagnostics

One `diag()` line per write site and per chip-armed event: word, origin, trigger
(`blacklist-write: word=... origin=... trigger=...`). Password-field suppression comes free via D-150's
choke point.

### 3.8 Strings

All new user-facing strings (chip label, toast, origin labels, popup, buttons) go into all 32 `values*`
directories as drafts (standing D-478 rule; generator-checked placeholders/escapes like §503).

### 3.9 G-03 collision contract

G-03 (dismiss-down) is recognised on the suggestion bar itself. A down-drag **starting on a chip** (the
rejection gesture) is never a dismiss-down — gesture origin is the chip view, not the bar. State this
explicitly in spec G-03/G-04 so it is a rule, not an implementation accident (the D-357 class of gap).

## 4. Affected code sites

- `AdaptKeyService.kt`: `onBlacklistWord()` (chip instead of immediate bundled-branch write),
  `onForbidLearnedWord()` (same), `learnWord()`'s escalation branch (~7018), `learnHyphenCompound()`'s
  escalation branch (~7143), chip state + suggestion-bar takeover wiring.
- `LearnedWordsActivity.kt`: forget branch (~284-292) — the no-mark rule + honest toast + comment rewrite.
- `BlacklistActivity.kt`: entry dialog info button + provenance popup.
- `DictionaryStore` / `SqliteDictionaryStore` / `InMemoryDictionaryStore`: schema columns, migration,
  provenance accessors; `blacklist()` gains the origin/added_at parameters.
- `BackupBundle` / `BackupImporter`: extended blacklist section.
- `SuggestionBar`/chip infrastructure: new chip kind with confirm/drag-down/ignore lifecycle (red colour
  resource).
- Strings ×32; spec updates (§7 below).

## 5. Rounds (suggested implementation order)

1. **R1 — no-mark rule + honest toast** (tiny, data-flow only): immediately stops new false entries; no UI
   beyond the toast. Can ship alone.
2. **R2 — provenance** (schema + migration + backup + editor info popup + diag lines): full traceability
   for every future entry, independent of the chip.
3. **R3 — the chip** (all three paths + escalation rework + G-03 contract + red colour + strings): the
   largest piece; depends on R2 for the origin values it writes.

## 6. Tests

Unit-testable (project conventions): the no-mark rule (store/helper level: bundled-casing variant → no
mark), escalation rework (mark + recurrence → no write, chip armed, ordinary learn proceeds; reject → mark
cleared), chip state machine if extracted pure (confirm/reject/ignore/field-change/yield-after-2-commits),
schema migration (Robolectric: old DB gains columns, existing rows → unknown), backup round-trip, provenance
formatting. Accepted untested Android-glue layers (per project convention): chip rendering/drag in the bar,
dialog/popups, activity wiring.

## 7. Spec touchpoints

G-04 (chip confirmation replaces the immediate bundled write; the two chip shapes), W-03 (deep zone
confirmed; toast), S-01 (new alternate slot content kind), the D-177 section (editor no-mark rule + comment
premise correction), G-03 (chip-origin down-drag never dismisses), §13/§38 as needed (editor, backup),
N-01 standing strings rule. Requirement IDs for the individual pieces get assigned from D-488 at
implementation.

## 8. Explicit non-goals

- No retroactive provenance for entries that exist today (shown as "unknown"; manual review).
- No removal of committed document text on confirm.
- No change to D-177's window duration or its setting (C-2x, default 7 days).
- BackupImporter / BlacklistActivity writes stay unchipped (explicit user action / explicit restore).

## 9. Implementation status (snapshot 2026-10-22, mid-session)

All three rounds' CODE is in and `:app:compileDebugKotlin` is green:

- R1 (no-mark rule): `LearnedWordsActivity` forget branch — `bundledCasingOf(word) != null` → plain
  `forget()` + plain toast; else mark + the honest `learned_words_removed_pending` toast; the wrong
  D-177 comment premise rewritten in place.
- R2 (provenance): new `dictionary/BlacklistOrigin.kt` (`BlacklistOrigin` + `BlacklistEntry`);
  `DictionaryStore.blacklist(word, category, origin, addedAtMillis)` + `blacklistEntry()` +
  `blacklistedEntries()` (existing rows KEEP their provenance on re-write - seed-proof); SQLite
  `ensureBlacklistProvenanceColumns` (guarded ALTER, no backfill -> UNKNOWN/0) + `onCreate` DDL with
  columns; `InMemory` parallel maps; backup v2 (`CURRENT_FORMAT_VERSION = 2`, blacklist entries as
  objects {word, origin, addedAtMillis}, v1 string arrays still decode -> UNKNOWN, importer maps
  UNKNOWN -> BACKUP_IMPORT + now); `userBlacklistEntries()` replaces `userBlacklistedWords()`;
  BlacklistActivity manual add records EDITOR_MANUAL; info popup via new square "ⓘ" button next to
  the copy button (`showBlacklistEntryInfo`, copy = word+origin+when).
- R3 (chip): new `suggestion/BlacklistConfirmation.kt` (pure state machine, YIELD_AFTER_COMMITS = 2);
  `SuggestionController.Kind.BLACKLIST_CONFIRM`; `SuggestionBarView` red bold chip colour
  (`suggestion_blacklist_confirm_text` #C62828), reject drag DOWN starting on the chip (claimed ahead
  of D-144 swipe-down, green "Keep" overlay = forgetPaint + `suggestion_keep_label`), G-03 collision
  contract enforced; service: `blacklistConfirmation` state + `blacklistConfirmArmedDuringCommit`
  flag, pinning override in `setSuggestionBarItems` (full takeover), tap = `confirmBlacklistConfirmation`
  (the only keyboard-surface write), reject listener, field-change ignore in `onStartInput`
  (refreshBar = false), commit counter at `learnWord`'s tail (`noteCommitForBlacklistConfirm`,
  arming commit skipped once), G-04 bundled branch + W-03 deep zone arm instead of write, D-177
  escalation branches (learnWord + learnHyphenCompound) arm via `armBlacklistConfirmDuringCommit` and
  fall through to the shared `promoteOrCountPending` tail, seed records BUNDLED_SEED,
  `diag("AdaptKeyBlacklist", ...)` on arm/write/reject.
- Strings: 15 new keys in values + all 31 locales via `scratchpad/d488_add_strings.ps1` (drafts,
  apostrophes escaped as \', UTF-8 no BOM verified).

STILL OPEN at snapshot: the new/extended unit tests (BlacklistConfirmationTest, InMemory provenance
cases, BackupJsonCodec v1/v2 decode + round-trip, SqliteDictionaryStoreRoboTest migration/provenance),
the full `:app:assembleRelease :app:testDebugUnitTest` run, version bump 566->567 / 1.2.70->1.2.71,
and the docs (Progress.md SS511 entry, spec G-04/W-03/G-03 addenda, this file's status line).

All of the above since landed: +15 tests (BlacklistConfirmationTest 7, InMemory +4, SQLite +2 incl. the
hand-built pre-D-488 database, codec +2, exporter/importer extended), 1719 total 0 failures, version
bumped, Progress §511 written, spec addenda in G-04/G-03/W-03, this status updated. Two test-round
lessons worth keeping: a hand-built "old database" in a Robolectric test must set `user_version` to the
current DATABASE_VERSION (otherwise the framework treats it as brand-new and calls onCreate, whose
CREATE TABLE collides), and must contain every other table in their old shapes (the other guarded
migrations run unconditionally on open).

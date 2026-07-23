# AdaptKey — Implementation Progress

Living status of the AdaptKey IME implementation, kept next to
[`AdaptKey-Spec.md`](AdaptKey-Spec.md) (the current, crystallised feature set) and
[`AdaptKey-History.md`](AdaptKey-History.md) (the append-only device-feedback/design-decision journal).
Requirement IDs (`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`, plus the newer `E-`/`U-`/`P-`/`W-`/`R-`/`V-`/`N-`/`X-`
domains) refer to the spec. Update this file whenever a component lands so it does not have to be restated
in every prompt.

## Project / Stack

- Directory: `D:\workspace-ai\adapt-key` (workspace `D:\workspace-ai`).
- Kotlin, `InputMethodService`, package `de.froehlichmedia.adaptkey`.
- minSdk 26, compile/target 35; AGP 8.7.3 / Kotlin 2.0.21; Gradle wrapper 8.11.1;
  `androidx.preference:preference-ktx` 1.2.1.
- Self-drawn `AdaptKeyboardView` (NOT the deprecated `android.inputmethodservice.KeyboardView`)
  because the spec needs raw `ACTION_DOWN` coordinates and free key proportions.

## Build (machine specifics)

- The only system JDK is Corretto 25, which the older Gradle dislikes → builds MUST
  use the Android Studio bundled JBR (JDK 21) as `JAVA_HOME`.
- Android SDK at `D:\Android\Sdk` (in `local.properties`, gitignored).
- Command (Git Bash, from the project dir) - **`:app:assembleRelease` since D-223** (produces the real,
  day-to-day install artifact, `app/build/outputs/apk/release/AdaptKey.apk`; `:app:testDebugUnitTest` still
  targets the debug variant regardless - the Kotlin source itself is identical between variants, there is no
  separate "release" source set to test, so this is simply the established, no-signing-required unit-test
  task and needs no change):
  ```
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME=/d/Android/Sdk ./gradlew :app:assembleRelease :app:testDebugUnitTest
  ```
- A post-write hook normalises blank lines / CRLF and may reformat files after edits
  (do not fight it).
- **D-223: `:app:assembleRelease` is now the default build target** (was `:app:assembleDebug` through D-222) -
  signed, `debuggable=false`, `isMinifyEnabled=false` (deliberately no code shrinking/obfuscation). Needs
  `keystore.properties` at the project root (gitignored, not checked in - holds
  `storeFile`/`storePassword`/`keyAlias`/`keyPassword` for the equally gitignored `release.keystore`, also at
  the project root); `:app:assembleRelease` fails without it, everything else configures fine regardless.
  **The release keystore is load-bearing once it has signed an installed build - back it up.** Losing it
  means every future version needs an uninstall + reinstall, wiping the learned dictionary/settings, since
  Android requires the same signing key to update in place. `app-debug.apk` (`:app:assembleDebug`) still
  exists unchanged for actual on-device debugging if that is ever needed again, but is no longer built by
  default.

## Guardrail - Read Before Touching `onUpdateSelection` / Composing State

D-139 (§99-§101 in the spec) took three real device-log tracing rounds to actually fix. Any change to
`AdaptKeyService.onUpdateSelection()`, `reclaimSurroundingWord()`, `composingAnchor`, or the batch-edit
sequencing around them must keep spec §99-§101's three stated invariants intact - see that section's
"Guiding Principle" note before touching this area, not a full re-audit every time.

## Reserve Ideas (Deferred, Not Forgotten)

Design ideas that were seriously considered, confirmed technically workable, but deliberately not implemented
because the problem they would solve turned out not to be the actual bottleneck. Kept here (not just buried in
History.md's append-only log) so they are not lost if the situation that would justify them ever recurs.

- **`android.os.CancellationSignal` for a genuine mid-query SQLite abort** (discussed during the D-212/WAL
  round, see history §136). Confirmed technically real - Android wires it to `sqlite3_progress_handler`, a
  true mid-query interrupt, not just a pre-check - and confirmed it would NOT need to break
  `DictionarySuggestionProvider`/`DictionaryStore`'s deliberately Android-free testable abstraction: since all
  SQL work already funnels through one single-threaded executor, the cancel state could live entirely local to
  `SqliteDictionaryStore` itself via a narrow, additional (non-interface) method. **Status: not needed.** The
  D-207-D-221 investigation (closed, history §145) found the real cost was synchronous computation itself
  (redundant per-candidate queries, unbounded bucket scans, unconditional per-keystroke searches) - not
  discarded/wasted background work a mid-query abort would have addressed; the existing cooperative
  `isCancelled: () -> Boolean` polling (D-211/D-216) already covers the "stop a superseded background search
  between candidates" case adequately. Revisit only if a future profiling round again points at wasted,
  actually-superseded query time (not raw computation cost) as the dominant remaining factor.

## Current State

- **§176 (v0.8.133): D-249 - the German inseparable verb/negation prefixes (`ver-`/`zer-`/`ent-`/`emp-`/**
  **`be-`/`ge-`/`miss-`/`er-`/`un-`/`ur-`/`wider-`) are now protected from A-05/D-122 splitting, per the**
  **user's own tiered candidate list agreed beforehand (Tier 4 - `über-`/`um-`/`durch-`/`unter-`/`voll-`/**
  **`hinter-`/`wieder-` - correctly excluded).** Root-caused against the real `dict_de.tsv` first: a concrete
  live false positive confirmed for `"widersagen"` -> `"wider"`+`"sagen"` (neither tagged a noun, both above
  the frequency floor). New `TokenRepair.INSEPARABLE_PREFIXES` set and `PREFIX_COMMON_WORD_FREQUENCY_CEILING`
  (5,000) gate inside the shared `candidateAt()` (covers both `trySplit`/A-05 and
  `splitAtUnresolvedConnector`/D-122), checked before dictionary resolution so it also covers the seven
  prefixes not themselves in the dictionary. `"er"` (frequency 120,975, the pronoun) is exempted from the
  block via the frequency ceiling, since blocking it unconditionally would break genuine splits like
  `"erkommt"` -> `"er kommt"` - the other three dictionary-entry prefixes (`"ver"` 131, `"ge"` 250, `"wider"`
  598) sit well below the ceiling and are blocked normally. 5 new tests (`TokenRepairTest`). 813 unit tests
  (808 + 5). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history
  §176, spec A-05.
- **§175 (v0.8.132): D-250 - D-161's WindowInsets recheck retuned from a single one-shot check to five,**
  **500ms apart, per the concrete spec §173 already captured (no further design round needed).**
  `windowInsetsRecheckRunnable` now reschedules itself (self-referencing lambda) up to a new
  `WINDOW_INSETS_RECHECK_MAX_ATTEMPTS = 5` times, `WINDOW_INSETS_RECHECK_DELAY_MS` (500ms) apart; a new
  `windowInsetsRecheckAttempt` counter is reset in `onStartInputView()` alongside the existing
  `removeCallbacks`/`postDelayed` pair, so a fast field/app switch still only leaves the most recent run of
  retries pending. The check itself is unchanged (still a silent no-op once the padding is already correct)
  and the chain deliberately does not stop early on a successful correction - matches the user's own "costs
  nothing worth worrying about" framing. No new tests (same established Android view/window-inset glue gap
  as §104's original landing). 808 unit tests (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. Not yet device-confirmed. See history §175.
- **§174 (v0.8.131): D-252 - "zuversichtlicher" -> "zuversichtlich er" closed via a new `AdjectiveInflection`**
  **check, mirroring the existing `RegularVerbInflection` protection (design agreed with the user first,**
  **see history §174 for the full root-cause + a real regression caught by the existing test suite).** New
  `AdjectiveInflection.isPlausibleComparative()` strips a regular comparative/superlative ending and checks
  the bare stem, wired into both `TokenRepair.isAlreadyRecognised()` (A-05 split protection) and
  `DictionarySuggestionProvider.bestCorrection()` (A-01 autocorrect-override protection, mirroring
  `RegularVerbInflection`'s own dual wiring). **Caught mid-implementation**: a bare known-word check broke
  the existing D-244 "Docker" -> "dock"+"er" regression test outright (over-protected it as a plausible
  comparative of "dock") - fixed by excluding a noun stem (German nouns take no comparative/superlative
  degree), not by special-casing the one word. 8 new tests (`AdjectiveInflectionTest` 6, `TokenRepairTest` 1,
  `DictionarySuggestionProviderTest` 1). 808 unit tests (800 + 8). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. Not yet device-confirmed. See history §174, spec A-01/A-05.
- **§173 CAPTURED (still v0.8.130, no code change): four backlog items noted, not designed/implemented -**
  **D-248 (D-247 extension: never learn an implausible line-end word, strategy still open), D-249 (`un-`/
  `ent-` prefixes should never be split off by A-05/D-122), D-250 (retune D-161's WindowInsets recheck to 5x
  @ 500ms, already concrete/ready to build), D-251 (a rough plan: a word-end reclaim should not reopen the
  full suggestion pipeline - needs real device logs before any design, touches the spec §1 guardrail area).**
  800 unit tests (unchanged). See history §173.
- **§172 (v0.8.130): D-247 - a "Gelernt: X" promotion confirmation chip with its own two-zone drag**
  **("Vergessen"/"Verbieten"), closing the silent-nonsense-word-learning gap after a two-round design**
  **discussion (see history §172 for the full back-and-forth, including two user-driven corrections).**
  New `MIN_LEARN_LENGTH = 2` excludes single letters from learning outright (the unambiguous case - a
  fragment from an unintended Enter mid-word). Everything else relies on the new confirmation: `LearnOutcome`
  gained `PROMOTED` (distinct from reinforcement-`LEARNED`), firing the chip exactly once, the commit a word
  first crosses W-02's threshold. A plain tap dismisses it; dragging it upward arms a two-zone gesture
  scoped **only** to this chip (ordinary G-04 suggestions keep their original single-zone behaviour
  unchanged) - shallow (48dp, reuses G-04's own arm distance) = "Vergessen" (unlearn only, green, fixed
  colour independent of C-04/S-05), deep (96dp) = "Verbieten" (immediate permanent blacklist, reuses G-04's
  red styling). Both zones map onto `onBlacklistWord()`'s two pre-existing outcomes, now reachable directly
  instead of only via origin (a freshly-promoted word is always self-taught, so the old origin check could
  never reach the blacklist outcome for it). `SuggestionBarView`'s single `trashArmed` boolean generalised
  into a three-state `DragZone`; new `Kind.LEARNED`, built and pushed directly to the bar (bypasses
  `SuggestionController`'s own ranking, mirrors `CLIPBOARD`/`CREDENTIAL`). No new tests (established
  `AdaptKeyService`/View glue gap, matching G-04/D-122/D-238's own precedent). 800 unit tests (unchanged).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed - needs a real word
  promotion to trigger at all. See history §172, spec §13 (W-02/W-03) and §4 (G-04).
- **§171 (v0.8.129): D-246 - S-07 next-word prediction elevated by a personal, self-growing trigram table,**
  **implemented after a design discussion (Stupid Backoff λ=0.4, personal-only `learned_trigrams` table, no**
  **new tier - see spec S-07 and history §171 for the full design/trade-off writeup).** `DictionaryStore.
  learn()`/`unlearn()` gained an optional `previousPreviousWord` parameter (default null, every existing call
  site/test unchanged); new `trigramFrequency()`/`nextWordsTrigram()` mirror the existing bigram methods in
  both `InMemoryDictionaryStore` and `SqliteDictionaryStore` (new additive table, no `DATABASE_VERSION` bump).
  `AdaptKeyService` grew a second context slot (`previousPreviousWord`), shifted/reset in lockstep with the
  existing `previousWord` everywhere it already changes; `LearnRecord`/A-07 undo reverse the trigram
  symmetrically, with one explicit fix-up in `performAutocorrectUndo()` for a self-referential-context
  subtlety in that one path (see history §171). `DictionarySuggestionProvider.nextWordSuggestions()` blends
  trigram (raw count) and bigram (discounted by `TRIGRAM_BACKOFF_WEIGHT=0.4`, only when two-word context was
  actually available) candidates - a soft preference, not an absolute rule, matching this project's existing
  S-01 philosophy. 11 new tests (4 `InMemoryDictionaryStoreTest`, 3 `DictionarySuggestionProviderTest`, 4
  `SqliteDictionaryStoreRoboTest`). 800 unit tests (789 + 11). `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. Not yet device-confirmed - needs real accumulated two-word-context typing before its effect is even
  visible, so this will take longer than usual to judge. See history §171.
- **§170 (v0.8.128): D-245 - the D-122 mid-word split chip's own trailing space now survives correctly for**
  **D-29's punctuation-eating rule.** Root-caused via code tracing (no device this round): `applySplit()` did
  two unbatched `InputConnection` edits (`setComposingText("")` then, later, `commitText()`) instead of the
  ordinary chip path's single `commitText()` - an editor that reports these as two separate
  `onUpdateSelection` callbacks (the same un-coalesced-batch-edit shape spec §1/D-87 already names) lets the
  first, spurious callback consume the D-123 single-shot `suppressNextReclaimSpaceReset` guard, leaving the
  second, real callback to hit the unguarded reset and silently clear `pendingSuggestionSpace` before the
  next keystroke (a period right after the chip) ever saw it armed. The flag-arming logic itself was already
  correct - fixed by wrapping `applySplit()`'s edit sequence in `ic.beginBatchEdit()`/`endBatchEdit()`,
  mirroring the D-87 precedent exactly; `finalizeAndCommit()`'s own automatic A-05 split (the other caller)
  gets the same fix for free. Honestly flagged as code-traced/mechanism-confirmed, not device-confirmed - no
  test harness exists for this `InputConnection` glue and no device was available this round (mirrors the
  D-229/§156 precedent for a reasoned-but-unobserved fix). **Noted, not fixed**: `applyMerge()` has the same
  unbatched-multi-edit shape (worse - three discrete edits) but never arms `pendingSuggestionSpace`, so this
  exact symptom can't manifest there; not otherwise investigated this round. No new tests (established
  `AdaptKeyService`/`InputConnection` glue gap, matching D-87/D-114/D-182's own precedent). 789 unit tests
  (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history
  §170.
- **§169 (v0.8.127): D-243 - raw touch coordinates now feed the X-01 diagnostic log; D-244 - "Ohren" ->**
  **"Ihren" root-caused to an overly permissive known-word override, and acronym-shaped split halves now**
  **need a higher bar.** D-243: new `AdaptKeyboardView.logTouch()` (mirrors `logHaptics()`'s D-193
  dual-output shape, no password guard needed - a raw tap/resolved key is never typed content) logs a
  `rawTap: ...` line for every `ACTION_DOWN` (key, raw x/y, resolved key centre, T-05 ambiguity/inferred
  char) - lets the user analyse missed space-bar taps directly from Settings -> Diagnostics. D-244 (two
  independent fixes from the same round): (1) `"Ohren"` (freq 170, a perfectly ordinary word) was silently
  overridden to `"Ihren"` (freq 11,907, `o`/`i` QWERTZ-adjacent) since 170*50 <= 11,907 - the old
  `KNOWN_WORD_OVERRIDE_RATIO=50` bar's own assumption ("a genuine pair never gets remotely close") was wrong
  for this 70x-ratio pair; raised to 100, comfortably below the smallest genuine case (`ddr`/`der`, ~228x).
  (2) Revisited the recurring "Ebs"-class nonsense split - confirmed `MIN_SPLIT_HALF_FREQUENCY` is still the
  primary gate (D-227's own POS refinement touched a different, unrelated mechanism); confirmed against real
  data that 77% of all-uppercase (acronym-shaped) dictionary entries sit below frequency 50. New
  `TokenRepair.isAcronym()` + `MIN_SPLIT_ACRONYM_FREQUENCY=300` (reusing D-114's own established bar) closes
  it without touching ordinary capitalised-noun halves like "Dock". 5 new tests (2 + 3). 789 unit tests
  (784 + 5). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history
  §169.
- **§168 (v0.8.126): D-241 - settled on a more subdued green after confirming the vivid one worked; D-242 -**
  **the S-05 highlight no longer flickers black between keystrokes on the same word.** D-241: confirmed
  `#00C853` (A700) was clearly distinguishable, then settled on Material Green 600 (`#43A047`) as the new
  default - only Green changed, the other 4 presets stay at the untested A700 tier. D-242: the brighter
  colour made a pre-existing flicker newly obvious - typing "Test" slowly flashed green then black then
  green per letter, since `updateComposing()`'s highlight only showed green on an *exact* match between the
  debounced background computation (D-194/D-213, ~200ms) and the current text, which is false during
  essentially all of a typing burst. Confirmed against real data that `"te"`/`"tes"` are themselves genuine,
  if marginal, dictionary entries, so every intermediate prefix of "Test" independently qualified, making the
  cycling especially visible. Fixed after discussing the trade-off with the user first: a new
  `previewForSameToken` check (current text and the cached preview's own text are prefixes of one another)
  replaces the exact-match requirement for the *plain* whole-word highlight only - the highlight now persists
  across keystrokes on the same token instead of resetting to black, transitioning directly from old decision
  to new. The §47 split-preview branch keeps its strict exact-match requirement (a stale split's span ranges
  would be a real out-of-bounds risk on a since-changed-length token, not just a stale display). Accepted
  trade-off: a token can stay shown as highlighted for up to one debounce cycle after it has actually stopped
  being a real word. No new tests (private `AdaptKeyService`/`InputConnection` rendering glue). 784 unit
  tests (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See
  history §168.
- **§167 (v0.8.125): D-240 - the S-05 highlight colour moved from a dark, muted tone to a brighter, more**
  **saturated one.** User's real complaint (after an initial back-and-forth): not contrast against the page
  background, but against ordinary black/dark-grey body text - a dark highlight colour clusters too close in
  lightness to black to read as "changed" at a glance. Darkening further (the first idea explored) would have
  made it worse; bold was rejected too (most fonts have wider bold glyphs, risking reflow/jitter on every
  highlight toggle in whatever app is being typed into, and weight doesn't fix colour contrast anyway); a
  background-colour highlight was ruled out without implementing it, since it would reverse D-25's own
  already-confirmed "colour the text, not the background" decision. Fixed by moving from the muted Material
  700/800 tier to the brighter, more saturated Accent (A700) tier - default green `#2E7D32` -> `#00C853`,
  the user's own pick after comparing it against a more subdued alternative; the other 4 C-04 presets moved
  to the same tier for consistency (Orange's own Accent ceiling flagged as inherently less vivid than the
  others - a colorimetric limit of the hue itself). No new tests (pure colour-constant/resource values). 784
  unit tests (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed -
  a visual-perception change the user wants to judge on-device directly. See history §167.
- **§166 (v0.8.124): D-239 - a touch-calibration reset from `CalibrationActivity` never reliably reached**
  **the already-resident keyboard service.** Root-caused: `AdaptKeyService` (a classic long-lived
  `InputMethodService`, never restarted just because Settings was opened/closed) only pulled a fresh
  `OffsetModel` in `onCreate()` and in `onStartInputView()` when `restarting == false` - but returning to
  typing after a Settings visit is frequently reported as `restarting = true`, skipping the reload entirely.
  Compounding it, `persistOffsetModel()`'s own guard only detects a *different* stored pattern, so a
  same-pattern "Reset" was invisible to it and the service's stale in-memory model got saved right back over
  the fresh reset on the next save. No push/notify channel existed between the two screens at all - just two
  on-disk `SharedPreferences` files with no listener on the touch-calibration one. Fixed generally: new
  `OffsetStore.prefs()` (mirrors `SettingsStore.prefs()`) exposes the file; `AdaptKeyService` registers a new
  `offsetModelPrefsListener` that calls `reloadOffsetModel()` on any write to it, independent of the
  `onStartInputView`/`restarting` nuance entirely - also neutralising the `persistOffsetModel()` clobber risk
  as a side effect, since the in-memory model is already back in sync by the time any later save runs. No new
  tests (`AdaptKeyService` lifecycle/`SharedPreferences` glue, the established gap). 784 unit tests
  (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history
  §166.
- **§165 (v0.8.123): D-238 - a position-1 suggestion chip closes D-234's own disclosed gap for A-05**
  **split/A-06 merge under a disabled autocorrect toggle.** Key simplification found while designing: when
  `settings.autocorrectEnabled` is false, `suppressAutocorrect` is unconditionally true, which also forces
  `diacriticWord`/`bestCorrection` to null in that state - so a live split preview needs no extra veto logic
  of its own, a plain `trySplit()` call already matches what `finalizeAndCommit()` would have decided.
  Reused rather than duplicated: `composingPreviewRunnable` (D-125/D-213's debounced background split
  computation, previously gated on `config.highlightEnabled` alone) now also runs when autocorrect is
  disabled, and `refreshSuggestions()` reads its cached `composingPreview.split` directly into a
  `Suggestion("$left $right", MAX_PRIORITY_SUGGESTION_SCORE)` chip, mirroring D-122's own shape exactly -
  tapping it needs zero new click-handling, since D-122's existing `item.word.contains(' ')` branch in
  `onSuggestionClicked()` already routes any multi-word chip to `applySplit()`. A-06's merge chip is
  single-word, so it can't be told apart from an ordinary completion by shape - computed cheaply and directly
  from `pendingMergeChar`/`previousWord` via the existing `tokenRepair.tryMerge()`, and on tap re-derived
  fresh and compared against the exact tapped text before routing to the existing `applyMerge()`; clears
  `pendingMergeChar` so it can't double-apply later. No new tests (private service/`InputConnection` glue,
  the same established gap D-122's own original implementation left untested). 784 unit tests (unchanged).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §165.
- **§164 (v0.8.122): D-237 - `CalibrationActivity`/`TouchModelActivity` merged; the reset action no longer**
  **touches the chosen typing style.** Confirmed the reported bug first: the old top-level "reset learning &
  calibration" action called `OffsetStore.clear()`, wiping *both* the learned drift stats and the stored
  typing-style choice together, leaving the pattern at `UNKNOWN` with no re-seed or nudge back to K-01.
  Reorganised per the user's spec: `cat_calibration` now has exactly one top-level entry (`k01_calibration`);
  `TouchModelActivity` is deleted, fully absorbed into `CalibrationActivity`, which now always shows a live
  D-24 zone overlay (for the actual persisted model by default) and highlights the currently selected style
  button. Tapping a style now previews its fresh seed live and asks for confirmation before persisting
  (cancel reverts to the actual model); a new, spatially separated "Reset" button re-seeds only the
  *currently selected* style's own defaults via `OffsetStore.save()` - never touching the style choice or
  calling the now-deleted `OffsetStore.clear()`, and deliberately skipping `applyPatternEnlargement()` too
  (re-seeding isn't a style change, so a manually-tuned C-01 slider must survive it). `k01_calibration`'s
  summary now shows the currently selected style, refreshed in both `onCreatePreferences()` and `onResume()`.
  Updated the existing `CalibrationActivityRoboTest` (real Robolectric activity-lifecycle test) for the new
  preview-then-confirm flow, split into confirm/cancel cases. 784 unit tests (783 - 1 + 2).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §164.
- **§163 (v0.8.121): D-235 - the K-01 seeded touch zone no longer scales against a widened key's own**
  **size; D-236 - T-06's visualisation no longer hides non-CHAR keys.** D-235: `PatternSeed` was scaling
  every seeded horizontal spread/shift by the candidate key's *own* half-width - a deliberately widened key
  (L-02's period/comma) got a proportionally *bigger* absolute tolerance zone for free, cancelling out the
  reason it was widened. Fixed: a shared `referenceHalfWidth` (median across the geometry list) replaces
  each candidate's own half-width in the horizontal formulas; vertical spread untouched (no comparable
  height-widening exists to correct for). 1 new test proves a widened key now gets the same absolute
  `stdDevX` as an ordinary one. D-236 (found while investigating): `drawTouchModel()` filtered to
  `KeyCode.CHAR` only, silently hiding Shift/Backspace/Enter/Space and the calculator's `sin`/`deg` keys even
  though `OffsetModel.record()` trains all of them identically - made D-231's/D-233's own fixes impossible to
  actually see. Filter removed (the existing `spreadFor() ?: continue` already handles untrained keys
  correctly); `capX`/`capY` now also consult the leftward/upward per-key overrides for full consistency with
  D-109/D-133/D-231/D-233. No new tests for D-236 (View drawing glue). 783 unit tests (782 + 1).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Neither confirmed on device yet. See history §163.
- **§162 (v0.8.120): D-233 - `m`'s learned touch zone capped against Backspace (horizontal axis); D-234 -**
  **user-facing autocorrect toggle, suggestions stay live, silent substitution does not.** D-233:
  `OffsetModel.Candidate` gained `maxRightwardOffsetFactor`/`maxLeftwardOffsetFactor` (mirroring D-231's
  vertical pair exactly); `m` (end of the bottom letter row, both QWERTY/QWERTZ) capped rightward toward
  Backspace, Backspace capped leftward toward `m`, both via a new `M_BACKSPACE_OFFSET_FACTOR = 0.25`; T-06's
  visualisation updated too since (unlike Enter/Backspace) `m` is a real `KeyCode.CHAR` key it actually draws.
  D-234: new `AdaptSettings.autocorrectEnabled` (default on) threaded through the usual settings pipeline
  (`cat_suggestions`, all three locales); wired into `finalizeAndCommit()` by folding
  `!settings.autocorrectEnabled` into the *existing* `suppressAutocorrect` flag (already gates
  bestCorrection/rawCoordinateCorrection/trySplit since D-106 stage 2/D-226) plus explicit guards on
  `diacriticWord` and the A-06 merge branch (both deliberately independent of `suppressAutocorrect`
  otherwise). `refreshSuggestions()`/the S-05 highlight are untouched by design - both already decoupled from
  `finalizeAndCommit()`'s own commit-time decision, so suggestions/highlighting keep working exactly as
  before while the toggle is off. **Known, disclosed gap**: A-05 split/A-06 merge have no suggestion-bar
  alternative outside a mid-word re-edit (D-122's own narrow scope, confirmed by reading
  `midWordConnectorSplitSuggestion()`) - with autocorrect off, a missed/spurious space commits as typed with
  no tappable alternative, flagged in the setting's own KDoc rather than silently built or hidden. 4 new
  tests (3 `OffsetModelTest`, 1 `SettingsMapperTest`). 782 unit tests (778 + 4).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Neither confirmed on device yet. See history §162.
- **§161 (still v0.8.119, no code change): D-230/D-228 device-confirmed** (`"darfst"`, `"Docker"`, both via
  D-232's dictionary additions). See history §161.
- **§160 (v0.8.119): D-232 - D-230/D-228 closed via dictionary data instead of algorithm, a user-proposed**
  **pragmatic alternative to §158's design deadlock.** Root cause of both regressions was simply "a real word
  missing from the dictionary" - added the missing words instead of fighting the split algorithm.
  **24 genuinely-missing irregular/ablaut-verb forms** added to `dict_de.tsv` (verified against the real file
  first - most present-tense/preterite forms already existed; `darfst`/`gibst`/`nimmst`/`sprichst`/`fahre`/
  `fährst`/`läufst`/`schlafe`/`schläfst`/`trägst`/`hältst`/`fällst`/`rätst`/`wäschst`/`empfehle`/`empfiehlst`/
  `hilfst`/`stirbst`/`wirfst`/`triffst`/`brichst`/`stehle`/`stiehlst`/`warst` were the real gaps), tagged
  `OTHER`, frequencies derived from each verb's own sibling form via the corpus's own observed ratio pattern.
  **IT/programming terms**: architecture question answered first - stores are one full SQLite file per
  language, not a shared table, so a third "#it" pseudo-dictionary would need a whole new store/provider and
  still wouldn't be suggested while German is active (only the active language's provider serves ordinary
  suggestions). User's call: add the missing pieces to **both** `dict_de.tsv` (14: `Kubernetes`/`Docker`/
  `Microservice`/`Commit`/`Deployment`/`Endpoint`/`Refactoring`/`Gradle`/`Kotlin`/`Workflow`/`Codebase`/
  `Bugfix`/`Changelog`/`Snapshot`) and `dict_en.tsv` (5: `Kubernetes`/`microservice`/`bugfix`/`changelog`/
  `rollback`) - most candidate terms already existed in both files with real corpus frequencies. **Correction
  to §152/§157/§158**: those entries claimed `"Docker"` has no entry in either bundled language - wrong, only
  `dict_de.tsv` was ever checked; `dict_en.tsv` already had `Docker 13 OTHER`, meaning D-226 (already shipped
  and device-confirmed) should already have protected it via `knownInOtherLanguage()` - never re-tested on
  device since. `BUNDLED_DICTIONARY_VERSION` bumped 1->2 so this reaches the existing install (D-178's
  mechanism); no `wkey` collisions introduced (verified). No new tests (pure data + one version constant,
  matching the D-114/D-132 precedent for data-only rounds). 778 unit tests (unchanged).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed - in particular whether
  `"Docker"` was already fixed by D-226 alone, independent of this round. See history §160.
- **§159 (v0.8.118): D-231 - Enter's learned touch zone capped against drifting into Backspace, mirroring**
  **D-109/D-133's bottom-row-vs-space-bar precedent.** User reported Enter's T-03 zone bleeding upward into
  Backspace, asking for a cap on both keys. Confirmed the geometry: Backspace sits at the right end of the
  third row, Enter directly below it at the right end of the bottom row - the same vertically-adjacent shape
  as the bottom letter row above the space bar. Implemented as a direct generalisation, not a new mechanism:
  `OffsetModel.Candidate` gained `maxUpwardOffsetFactor` (mirrors the existing `maxDownwardOffsetFactor`),
  `logLikelihood()`'s upward cap consults it the same way; `AdaptKeyboardView.downwardOffsetFactorFor()` now
  also covers `KeyCode.DELETE` (Backspace, capped downward toward Enter), new `upwardOffsetFactorFor()` covers
  `KeyCode.ENTER` (capped upward toward Backspace) - both share the same `ENTER_BACKSPACE_OFFSET_FACTOR = 0.25`
  as `BOTTOM_ROW_DOWNWARD_OFFSET_FACTOR`. T-06's visualisation needed no change (it only ever draws
  `KeyCode.CHAR` keys, so Enter/Backspace were never shown there). 1 new test (`OffsetModelTest`, mirrors
  D-133's own `resolve()`-level proof, direction reversed). 778 unit tests total (777 + 1).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §159.
- **§158 (v0.8.117): D-230 reverted - `MIN_PART=3` was itself a regression; D-228/D-230 reopened.** User
  caught it immediately: raising `MIN_PART` also blocks any genuine missed-space split ending in a real
  2-letter German word (`"an"`/`"im"`/`"um"`/`"es"`/...) - `"gehtes"` -> `"geht"`+`"es"` would never be found
  again. §157's own verification only checked existing tests, none of which happened to exercise a 2-letter
  right half. Reverted to `MIN_PART=2`; new test proves `"gehtes"` still splits; the 2 now-wrong
  `"darfst"`/`"Docker"` tests removed. **D-228/D-230 both reopened** - unfixed again. The real difficulty,
  laid out for the next design round (see history §158's table): the wanted case (`der`+`Kinderarzt`,
  `geht`+`es`) and the unwanted case (`darf`+`St`, `Dock`+`er`) have an *identical* surface shape by every
  signal this app can cheaply compute (frequency, both-nouns, bigram co-occurrence all zero either way) - the
  actual distinguishing feature is word-order/grammatical-role plausibility (determiner-noun / verb-particle
  vs. modal-verb-then-abbreviation / noun-then-bare-pronoun), which this dictionary's `OTHER` tag cannot
  represent (articles/pronouns/prepositions/conjunctions all lumped together). Not implemented - left for the
  user's own design call. 777 unit tests (778 - 2 + 1). `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  See history §158.
- **§157 (v0.8.116): D-229 device-confirmed; D-230 fixed - "darfst" -> "darf St" was the same class of**
  **regression as D-228's "Docker", closed via `MIN_PART` (which also retroactively closes D-228).** Root-
  caused: `"darfst"` (irregular/ablaut modal verb, `dürfen`/`darf`/`darfst`) has no dictionary entry and is
  explicitly out of `RegularVerbInflection`'s documented scope (strong/ablaut verbs). Without that
  protection, `"darf"` (OTHER) + `"St"` (NOUN) passes the both-nouns rule (only one side is a noun) and both
  clear the frequency floor - identical shape to D-228's `"Dock"`+`"er"`. §152 had already identified
  `MIN_PART` (2->3) as a clean fix for this exact shape but deferred it per the user's own "accept the
  general case" direction at the time; a second independent real-world occurrence changed that calculus.
  Fixed: `MIN_PART` raised to 3 - every currently-valid split already has both halves at 3+ characters
  (confirmed by the full existing suite passing unmodified), so nothing regressed. **D-228 is now closed** as
  a direct consequence (same 2-letter-half shape) - its own backspace-restore-partial-learning-credit idea
  remains open on its own merits. 2 new tests (`TokenRepairTest`). 778 unit tests total (776 + 2).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §157.
- **§156 (v0.8.115): D-229 fixed - the composing-text underline flicker root-caused to D-194's own**
  **debounced S-05 highlight, not a deliberate cue.** User reported a per-keystroke underline flash while
  typing, asking since when and to what purpose. Root-caused against the actual pre-D-194 (§125, v0.8.89)
  code via `git show`: before D-194, the highlight decision ran synchronously inside the same
  `setComposingText()` call, so colour state was always resolved and applied together - no intermediate state
  ever rendered. D-194 (a performance fix) split this into an immediate *plain* branch (fires on every
  keystroke while the ~200ms debounce hasn't caught up yet) and a later *styled* (green) branch. The plain
  branch sent a bare unstyled `String`, unlike every other branch, which always sends a `Spanned` - standard
  Android `Editor` behaviour draws its own default composing-underline decoration specifically when the IME
  hasn't supplied explicit character styling, and suppresses it once the IME has. Confirmed against real data
  that this isn't limited to genuinely complete words either: `"te"`/`"tes"` are themselves marginal
  dictionary entries (freq 302/197 and 11/21), so every intermediate prefix of `"Test"` can earn its own brief
  green flash once the debounce catches up, before the next keystroke resets it to plain/underlined again.
  Fixed: the plain branch now wraps the text in an unstyled `SpannableString` instead of a bare `String` -
  identical rendering otherwise, but always `Spanned`, so the editor's own underline decoration is never
  triggered. Does not touch `composingAnchor`/`onUpdateSelection`/`reclaimSurroundingWord`/batch-edit
  sequencing - the §1 guardrail is unaffected. No new tests (private `AdaptKeyService` rendering glue). 776
  unit tests (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest` green. **Hypothesis-based, honestly
  flagged**: the underlying platform-behaviour claim is standard/well-documented but unobservable in this
  no-emulator environment - awaits device confirmation. See history §156.
- **§155 (v0.8.114): D-168 fixed - a key's long-press popup alternatives now respect Shift/Caps Lock,**
  **scoped to genuine word-forming letters only.** Small tuck-in per direct request. Root cause was already
  on record (§105/§106): `drawPopupCell()` draws every alternative's raw text unchanged, unlike `labelFor()`'s
  own main-key case handling. The scoping this was deliberately deferred for (`Key.alternatives`/`hint` mixes
  real letters with punctuation/currency/corner-hint glyphs, Greek-as-math-symbol picks, and §53 `TEXT`-key
  multi-char labels) is resolved by reusing `commitLongPressSymbol()`'s own `AlternativeScript.extendsWord()`
  predicate for the display decision too - new `popupDisplayTextFor()` uppercases only when
  `(shifted || capsLock)`, the key isn't a `TEXT` key (excludes `"cos"`/`"tan"`/`"log"`/`"rad"`, which
  `extendsWord()` alone would wrongly accept), and `extendsWord()` itself agrees. No new tests (private View
  drawing glue, `AlternativeScript` itself unmodified and already tested). 776 unit tests (unchanged).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §155.
- **§154 (v0.8.113): D-225 implemented - `_` is now word-extending, and a token containing one is fully**
  **shielded from correction/suggestion/learning.** Picked up next per explicit request, following §149's own
  captured design. `commitLongPressSymbol()` (the only reachable path to `_`, which has no primary key
  anywhere) now treats `symbol == "_"` as word-extending directly, alongside the pre-existing
  `AlternativeScript` script check - `_` isn't genuine script text, so it was never routed through that check
  itself, just given its own explicit condition. `finalizeAndCommit()` now short-circuits on `'_' in typed`
  before any dictionary search runs, committing via a new `commitVerbatim(ic, delimiter, learn = false)`
  parameter (default `true`, existing G-05 call site unaffected) - deliberately stronger than G-05's own
  verbatim commit, which still learns the word; a `_`-containing token never does, per the user's own explicit
  correction ("nichts korrigieren, nichts lernen"). `refreshSuggestions()` mirrors the existing `urlMode`
  bypass for `'_' in input`, clearing the bar entirely. The live §47 split-preview needed no separate guard -
  `_` can never fold to a real dictionary word, so `trySplit()` already can't find a candidate through it.
  Recorded as new requirement `B-04` (§8) - genuinely new settled behaviour, unlike the D-210/D-226/D-227 bug
  fixes. No new tests (private `AdaptKeyService`/`InputConnection` glue, the same established gap as every
  other fix in this area). 776 unit tests (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  Not yet device-confirmed. See history §154.
- **§153 (still v0.8.112, no code change): D-222, D-223, D-224, D-226, D-227 device-confirmed** in one round
  (URL key merge, release build, save-credentials toggle, "commits", "übrigens"). See history §153.
- **§152 CAPTURED (still v0.8.112, no code change): D-228 - "Docker" -> "Dock er" accepted as unfixable in**
  **general; backspace-restore-as-partial-learning-credit captured for later.** Third real-world A-05 split
  example from the same feedback round as D-210/D-226/D-227, but structurally different: neither dictionary
  has ever heard of `"Docker"` at all (unlike `"übrigens"`/`"commits"`, real words gated incorrectly). `"Dock"`
  (freq 20, NOUN) + `"er"` (freq 120,975, OTHER - one of the single most frequent tokens in the whole corpus)
  clears every existing gate with zero bigram co-occurrence and wins purely because nothing else competes.
  Investigated and explicitly not pursued: a co-occurrence gate would also break D-203's own motivating
  `"der"+"Kinderarzt"` case (zero co-occurrence there too, confirmed against `bigram_de.tsv`); an absolute
  score floor can't discriminate either (same "hyper-frequent glue word + rare noun" shape, same magnitude);
  the one real distinguishing feature (word order/grammatical role - determiner-leads-noun vs.
  pronoun-trails-noun) isn't cheaply available from this dictionary's `OTHER`-lumped tagging. Raising
  `MIN_PART` (2->3) would cleanly close this specific 2-letter-suffix shape without touching D-203, but
  doesn't cover `"übrigebs"` and wasn't pursued in isolation per the user's own direction to accept the
  general case. **User's decision**: accept, let the personal dictionary (and later, tier-3) absorb it -
  deliberately not started until base dictionary-only autocorrect is judged solid. **Real gap surfaced,
  captured only**: the wrong split applies silently, so `"Docker"` never reaches D-37's learn threshold via
  ordinary confirmed use; A-07's backspace-undo restores the typed text but doesn't advance any pending-learn
  counter. User's own proposed direction (explicitly deferred, not decided): backspace-restore should earn
  *partial* learn credit, weaker than a full S-06-chip accept - illustrative starting numbers floated by the
  user (chip-accept +2, backspace-restore +1), possibly needing the promotion threshold itself to grow if
  partial credit is introduced. 776 unit tests (unchanged - documentation only, no version bump per this
  project's own convention). See history §152.
- **§151 (v0.8.112): D-227 fixed - the D-114 frequency floor was cost-blind; now also consults**
  **part-of-speech.** User asked directly why a keyboard-neighbourhood check didn't already rescue
  `"übrigebs"` (D-210's own split-regression report). Root-caused: `"übrigebs"` is a genuine cost-1
  QWERTZ-adjacent typo (`b`/`n`) of `"übrigens"` - `bestCorrection()` does find it, but `dict_de.tsv` lists it
  at frequency 79, below the production `minAutocorrectFrequency=300` floor (D-114), so it's dropped
  regardless of its perfect edit cost, `highConfidence` never becomes true, and the low-quality A-05 split
  wins by default. **First fix attempt (blanket cost-1 exemption, mirroring D-113's `shouldOverrideKnownWord`
  cost distinction) was disproven immediately by the existing test suite**: D-114's own original bug
  (`"Virhin"`->`"Virgin"`, freq 62) is *also* cost-1 (`h`/`g` adjacent) - cost alone can't tell the two cases
  apart. Checked against the real corpus instead: `"Virgin"` is tagged `NOUN` (a Wikipedia-corpus proper-noun
  artefact - exactly D-114's target), `"übrigens"` is tagged `OTHER` (an ordinary, merely corpus-under-counted
  adverb). Fixed: `bestCorrection()` now fetches the full `WordEntry` (`entryOf()`, D-214-style single
  round-trip) and only applies the frequency floor when the candidate is `NOUN`/`PROPER_NOUN`-tagged **or**
  its cost exceeds `ADJACENT_SUB_COST` - a cost-1 non-noun candidate is exempt regardless of frequency. The
  existing D-114 test had never actually set any part-of-speech on its synthetic `"Virgin"` entry (passed the
  old cost-only check by coincidence, not by modelling the real bug) - corrected to add the real dictionary's
  own `NOUN` tag rather than weakened; a new test proves the intended asymmetry (`"übrigens"`, `OTHER`-tagged,
  still wins; `"Virgin"`, `NOUN`-tagged, still correctly rejected). 776 unit tests (775 + 1 net).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §151.
- **§150 (v0.8.111): D-226 fixed - trySplit() never respected suppressAutocorrect/knownInOtherLanguage, so
  a real English loanword ("Commits") got torn apart against the German dictionary.** Second real A-05-split
  regression from the same feedback round as D-210. Root-caused, not guessed: `"commits"` is a genuine
  `dict_en.tsv` entry (freq 60) - exactly what `knownInOtherLanguage()` (D-106 stage 2) exists to protect - but
  is absent from `dict_de.tsv` (only coincidental fragments `"com"`/`"its"` exist there). `finalizeAndCommit()`'s
  `suppressAutocorrect` flag already correctly gates `bestCorrectionFor()`/`rawCoordinateCorrection()`, but the
  `trySplit()` veto condition never checked it at all - a plain oversight, not a deliberate design choice - so
  nothing stopped `"com"`+`"its"` from qualifying as a valid German split (same shape as D-210, but here an
  existing protection mechanism simply wasn't wired in). Fixed with one added condition, mirroring the other
  two call sites exactly. No new tests (private `AdaptKeyService` gating logic, no pure-function seam - the
  established gap for this class of fix). 775 unit tests (unchanged). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. Not yet device-confirmed. See history §150.
- **§149 CAPTURED (still v0.8.110, no code change): D-225 - `_` must stop acting as a delimiter; a token**
  **containing one must be fully shielded from correction/suggestion/learning.** User-flagged, confirmed
  against the code before capturing: `_` has no primary key anywhere in the app (long-press-only, on the
  comma key's popup and symbol page 2's distributed `-` key), and every one of those paths runs through
  `commitLongPressSymbol()` -> `AlternativeScript.extendsWord()` returns `false` for it (not a letter) ->
  `finalizeAndCommit()`, i.e. `_` behaves exactly like ordinary punctuation today - inherited, not designed,
  from D-35's generic "non-letter symbol ends the token" rule. An identifier like `MEINE_VARIABLE` is
  therefore torn into three independent commits (`MEINE`, `_`, `VARIABLE`), each half individually
  autocorrected/capitalised/learned as if it were an ordinary word. **User's direction (agreed, deferred until
  after D-210, this round captures only):** (1) `_` becomes word-extending; (2) stronger than first proposed -
  once a token contains `_`, the *entire* pipeline is disabled for it (no autocorrect, no suggestions, no §6
  capitalisation, no D-37 learning at all - "nichts korrigieren, nichts lernen"), not merely the automatic
  correction. Open questions for the implementation round: where exactly to gate ("contains `_`" detection
  point vs. one shared short-circuit mirroring the existing login/URL-mode verbatim path); whether such a
  token should ever be personally learned at all (leaning no). 775 unit tests (unchanged - documentation only,
  no version bump per this project's own convention). See history §149.
- **§147 (v0.8.109): D-223 - a real, signed release build.** User asked whether sideloading `app-debug.apk`
  still made sense given the USB/JDWP debugger it enables is never actually used - correctly distinguished
  from native "debug symbols" (this project never generates any regardless, debug or release). Generated a
  dedicated release keystore, wired signing through a new gitignored `keystore.properties` (mirrors
  `local.properties`'s own placement/reasoning), renamed the release output to `AdaptKey.apk`. Deliberately
  left `isMinifyEnabled=false` - code shrinking/obfuscation is a separate, riskier decision not asked for.
  `:app:assembleRelease` verified signed (`apksigner verify`) and installable; `:app:assembleDebug` verified
  unchanged. No Kotlin source touched - pure build-script/signing configuration. 775 unit tests (unchanged).
  See history §147 for the "must back up the keystore" consequence and the build-doc section above for the
  new command.

- **§148 (v0.8.110): D-224 - the inverted "never save credentials" toggle flipped to a positively-phrased
  "save credentials".** User: it reads backwards next to every other settings-screen toggle, where ON means
  the feature is active - here ON meant it was suppressed. Renamed throughout:
  `AdaptSettings.neverRecordCredentials: Boolean = false` -> `saveCredentials: Boolean = true` (same in
  `SettingsMapper.RawSettings`); `SettingsStore.KEY_NEVER_RECORD_CREDENTIALS` ("d190_never_record_credentials")
  -> `KEY_SAVE_CREDENTIALS` ("d224_save_credentials"), `defaultValue` flipped to `true` in
  `settings_preferences.xml`; new `d224_save_credentials_title`/`_summary` strings (EN/DE/EL) replace the old
  D-190 ones, same factual content reworded positively. `AdaptKeyService`'s two gate conditions inverted to
  match. **No migration** - user's own call, since exactly one install of this app exists; an old stored value
  under the legacy key is simply orphaned, the setting resets to its new default (saved = on) and can be
  re-toggled if needed. One test renamed/repolarised. 775 unit tests (unchanged count).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See history §148.

- **§146 (v0.8.108): D-222 - the URL-mode `https://` and `www.` keys merged into one, restoring the space
  key to full width.** User: on a phone the URL/address bar is used for search queries far more often than
  real URLs, so D-143's own drastically-shrunk URL-mode space key (weight 1, vs the ordinary row's 3.2) is a
  real annoyance in the common case. Proposed fix, implemented as-is: merge the two keys into one, labelled
  `www.` (protocol rarely needs typing explicitly), long-press popup = the old protocol key's exact
  alternative list (`https://`/`http://`/`ftp://`/`file://`), `https://` first so it's the pre-selected cell
  (via the existing generic `preSelectedIndexFor()` index-0 fallback, no special-casing needed).
  `urlBottomRow()` now returns 6 keys instead of 7. Freed width goes to space: mirrors D-158's own
  "conserve the row's total weight" pattern (not D-143's own "grow the row") - `URL_WWW_KEY_WEIGHT = 1.8f` +
  `proportions.spaceWeight` (now the ordinary, un-shrunk value) sum to the same 5.0 the old three keys did,
  so symbol/slash/period/enter render at the exact same width as before. `GreekLayout` needed no change
  (shares `urlBottomRow()` verbatim). Two tests renamed/rewritten to match the merged 6-key row. 775 unit
  tests (unchanged count). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
  See history §146.

- **§144 (v0.8.107): D-220 device-confirmed (bestCorrectionMs 186-404ms -> 68-90ms, the predicted 3-5x win);
  D-221 fixed - diacriticRestoration() uncapped every searched bucket, not just the ones correctness actually
  needed.** User's own observation from the same log: badly-mistyped, common-initial-letter words (e.g.
  "Glückwusf", "Geburtstsf") still showed `diacriticMs` at 176-213ms. Cause: `diacriticRestoration()`'s
  `isKnownWord()` early exit skips correctly-typed words (why D-220's fix alone didn't touch this), but for an
  actual typo `store.diacriticCandidates()` runs its deliberately unbounded (D-197) bucket scan - and unlike
  `correctionCandidates()` (D-209: only the token's own literal first-char bucket stays uncapped),
  `diacriticCandidates()` uncapped *every* bucket (own char + keyboard neighbours + umlaut variant) via a
  blanket `perBucketLimit = Int.MAX_VALUE`. First fix idea (cap neighbour buckets like correctionCandidates())
  was disproven by an existing test that deliberately asserted the opposite - the umlaut-variant bucket (ä/ö/ü
  for a/o/u) is diacritic restoration's *expected* path, not a rare edge case (a user typing "uber" has their
  token classified under 'u' while the real word "über" lives under 'ü') - capping it would reintroduce the
  exact "Grüße" bug D-197 prevents, via the umlaut path instead of the primary-bucket path. Fixed correctly:
  `correctionCandidatesInternal()`'s single `uncappedChar` became `uncappedChars: Set<Char>`;
  `diacriticCandidates()` now uncaps the token's own char *and* its umlaut variant, while ordinary
  keyboard-neighbour buckets stay capped like `correctionCandidates()`. Old neighbour-bucket test (asserting
  the removed blanket-uncap) replaced by two tests covering each bucket kind separately. 775 unit tests
  (774 - 1 + 2). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed** (see §145). See
  history §144.

- **§145: D-207-D-221 sluggishness investigation device-confirmed closed (no code change, confirmation only,
  no version bump).** User's own verdict on a fresh log covering all three test words plus a badly-mistyped
  "Geburtstsg" -> "Geburtstag": "Das sieht jetzt schon richtig gut aus... Beim vertippten 'Geburtstag' gab es
  einen kleinen Lag. Aber das ist vollkommen vertretbar." Numbers: correctly-typed common-initial-letter words
  now commit in bestCorrectionMs 87-105ms (down from the original 186-404ms); the worst remaining case, a
  three-typo "Geburtstsg" needing the full autocorrect search, cost diacriticMs=77 + bestCorrectionMs=87 for a
  183ms `handleKey` total (down from 638ms/500ms+ before D-220/D-221) - user-judged acceptable. Flash latency
  stayed at 1-17ms throughout, fully decoupled from processing time (D-219). This closes the investigation arc
  that started with the first "still sluggish" report and ran through D-207-D-219 (candidate search off the
  main thread, WAL, composing-preview split highlight off the main thread, redundant-query elimination,
  debounce restored, flash decoupled/shortened) and D-220/D-221 (finalizeAndCommit()'s own commit-time search
  chain). D-210 (the deferred A-05 split regression, `"übrigebs"` -> `"übrig Ebs"`) remains the next open item,
  explicitly set aside by the user until this investigation concluded.

- **§143 (v0.8.106): D-220 fixed - bestCorrection() queried frequencyOf() for every same-bucket candidate
  before even checking whether its edit cost qualified.** The D-220 timing log paid off immediately: fresh
  logs showed `bestCorrectionMs` alone dominating every slow commit (186-404ms for already-correct,
  common-initial-letter words like "Herzlichen"/"Glückwunsch"/"Geburtstag"; 0-19ms for an uncommon initial
  letter or a suppressAutocorrect-skipped token) while diacriticMs/splitMs/rawCorrectedMs stayed near 0.
  Root cause: unlike `diacriticRestoration()` (which checks `isKnownWord(token)` up front and returns
  immediately), `bestCorrection()` only consults `isKnownWord(token)` *after* its full search already ran -
  and that search called `store.frequencyOf(candidate)` for every candidate in the token's own (D-209,
  deliberately uncapped) first-character bucket *before* checking whether the cheap, pure-CPU
  `correctionCost()` check would already reject it. A common initial letter (H, G) can hold hundreds of
  same-bucket entries - hundreds of wasted SQLite round-trips per commit of an already-correct word. Fixed:
  the cost check now runs first, `frequencyOf()` only queried for candidates already within budget - a pure
  reordering (cost and frequency never depended on each other), confirmed behaviour-preserving by every
  existing bestCorrection/autocorrectFor/bestCorrectionFor test passing unmodified. No new tests. 774 unit
  tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. D-220's timing log stays in place
  for the next repro to confirm `bestCorrectionMs` actually dropped. See history §143.

- **§142 (v0.8.105): D-220 - diagnostics for the last remaining slow spot, device-confirmed to be
  finalizeAndCommit()'s own commit-time search chain.** D-217-D-219 confirmed a clear win ("Das Flashing ist
  super knackig und die Tastatur wirkt kaum noch träge", ordinary letters now 17-55ms, flash ~1 frame). The
  user's own "hier und da nochmal minimal" pointed at every remaining `handleKey` spike sitting exactly on
  SPACE/punctuation (100-685ms) - i.e. `finalizeAndCommit()`, never an ordinary letter. Located, not yet
  root-caused: that function runs up to four real searches synchronously on every commit
  (`diacriticRestoration()`, `bestCorrectionFor()`, conditionally `trySplit()`, conditionally
  `rawCoordinateCorrection()`), none deferrable (a commit's correction must resolve before the delimiter
  commits) and none touched by D-207-D-219. The 685ms outlier was a nonsense token forcing all four to run to
  exhaustion. Per this project's own root-cause-before-fix rule, added per-stage timing only (no fix yet) -
  two new `AdaptKeyHaptics` diag lines (`finalizeAndCommit: timing diacriticMs=... bestCorrectionMs=...
  splitMs=... rawCorrectedMs=...`), one for the split-found early return, one for the ordinary path. No new
  tests (pure diagnostic, no behaviour change). 774 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Awaits the next on-device repro to see the real
  per-stage breakdown. See history §142.

- **§141 (v0.8.104): D-219 - the flash effect decoupled from key processing and shortened, at the user's own
  explicit request.** Two asks: (1) the flash must paint independently of processing time, not just "faster
  processing eventually makes it faster too"; (2) it still felt too long - wants GBoard's near-instant popup
  snappiness. Fixed: `AdaptKeyboardView.onTouchEvent()`'s `ACTION_UP` now dispatches `onKeyListener?.onKey(...)`
  via `longPressHandler.post { }` instead of calling it inline right after `flash(key)` - `invalidate()`'s own
  sync barrier guarantees the pending frame paints before any regular `Handler` message posted afterwards, so
  the flash is now genuinely independent of `handleKey()`'s own duration (Android's standard `post()`-after-
  `invalidate()` pattern). `downX`/`downY`/`pendingAmbiguity`/`pendingRecordWeight` captured into locals before
  posting (mutable view fields the next `ACTION_DOWN` could otherwise overwrite first); ordering between
  keystrokes preserved (same serial `Handler`). `flashDurationMs` 28ms -> 16ms (one frame @ 60Hz). D-217's
  `flash: ... visible after Nms` log is the number to watch next - should now read ~1 frame regardless of
  `handleKey`'s own cost, unlike before this round where the two tracked each other almost exactly. No new
  tests (pure View glue). 774 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
  See history §141.

- **§140 (v0.8.103): D-218 - the real remaining sluggishness cost found, via D-217's own new timing log:
  the impending-autocorrect/diacritic preview ran unconditionally on every keystroke, never touched by
  D-207-D-217.** Fresh logs (fast and slow typing, same pattern either way) showed `handleKey` processing
  time growing with token length regardless of typing speed (e.g. `"Glücjwunsvh"`: 218->615ms), and `flash`
  visibility latency tracking it almost exactly - proving the flash lag is a direct consequence of the same
  main-thread cost, not a separate rendering issue. Root cause: `refreshSuggestions()` (called synchronously
  from every `handleKey()`) unconditionally computed the S-06 "impending autocorrect" chip via
  `diacriticRestoration()`/`autocorrectFor()` - both real, uncached store searches (full bucket scan +
  per-candidate edit distance + `frequencyOf()` query each) that D-207-D-217 never touched, since that whole
  investigation focused on the suggestion-bar candidate search and the composing-preview split highlight.
  Fixed by deferring the expensive half into the existing D-211/D-215 background/200ms-debounce pipeline: new
  `pendingCorrectionCandidate()` runs inside `dispatchExpensiveSuggestionSearch()`'s existing background block
  alongside its `suggestionsFor()` call; the hot/immediate path now shows only the cheap capitalisation-only
  preview (sentence-start capital) computed directly from the input, with the expensive whole-word
  replacement appearing once the token has been stable. `knownInOtherLanguage()` gained an explicit
  `activeLang` parameter (default unchanged) so the background call passes a main-thread-snapshotted value
  instead of racing a later keystroke's field reassignment. Side effect: also removes `pending` being
  redundantly recomputed a second time when the deferred pass lands. No new tests (Android service glue).
  774 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
  See history §140.

- **§139 (v0.8.102): D-217 - diagnostics now measure actual processing time and actual flash-paint latency,
  not just typing cadence.** User: existing logs only show *when* calls happen, not how long a tap actually
  took to process or how long the flash acknowledgement took to reach the screen. Added: (1)
  `AdaptKeyService.handleKey()` wraps its whole body (previously several independent early returns) in
  `try`/`finally`, logging `handleKey: key=<code> '<char>' t=<start> processed in <n>ms` unconditionally on
  every exit, tag `AdaptKeyHaptics`. (2) `AdaptKeyboardView`: `flash()` stamps a request time
  (`flashRequestedAt`/`flashTimingPending`); `drawKeys()`, at the point it selects `pressedKeyPaint` for
  `flashKey`, logs `flash: flash painted key=<id> requested at <t>, visible after <n>ms` the first frame that
  actually paints it (once per flash, not per frame). `logHaptics()` gained an optional `prefix` param
  (default unchanged) so this new line isn't mislabelled `"playKeyFeedback: "`. Lets a long handleKey number
  (main-thread computation) be told apart from a long flash-visible number with a short handleKey number
  (main thread busy with something else - a queued message, GC). Temporary D-139/D-193-style diagnostics, no
  behavioural change, no new tests (Android glue). 774 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. See history §139.

- **§138 (v0.8.101): D-214/D-215/D-216 - the real cost was O(n×m) computation, not stale requests.** Fifth
  repeat of the test, this time typing slowly (so no backlog of stale work could exist for D-211-D-213's
  cancellation to discard) - same per-character slowdown. User correctly diagnosed the whole investigation's
  framing as wrong: "Das eigentliche Problem sind nicht veraltete Anfragen... der Aufwand [ist] vermutlich
  O(n*m)." Three fixes, all user-proposed and confirmed ("Ja, bitte. Genau so."): **(a)** promoted the
  concrete stores' existing private `entryOf(word): WordEntry?` merge helper to a public `DictionaryStore`
  method; `TokenRepair.resolveWord()`/`candidateAt()`/`isNoun()`/`score()` rewritten to fetch a word's entry
  once and reuse it instead of re-querying `isKnownWord()`/`frequencyOf()`/`partsOfSpeech()` separately -
  ~8 reads per split position down to 2 (+2 genuine `bigramFrequency()` calls). **(b)** `EXPENSIVE_SUGGESTION_
  DELAY_MS` (200ms) restored for both `expensiveSuggestionRunnable` and `composingPreviewRunnable`, dropped in
  D-211/D-213 on the (correct but incomplete) reasoning that background threads protect the main thread - true,
  but a debounce also exists to avoid burning real background-thread computation on a result a fast subsequent
  keystroke will supersede before it's ever shown; that purpose was never actually solved by backgrounding the
  work. `clearComposing()` now cancels both runnables (previously only one). **(c)** `trySplit()` gained an
  `isCancelled: () -> Boolean = { false }` parameter (default no-op, no existing call site or pure-logic test
  needed changes); rewritten from `.mapNotNull` chains to explicit loops checking `isCancelled()` before each
  split position, returning the best candidate found so far when cancelled - mirrors D-211's cooperative-
  cancellation shape. `composingPreviewRunnable` passes the shared `expensiveSuggestionSeq` staleness check in.
  Two new tests in `TokenRepairTest.kt` (cancel-on-first-poll -> null; always-false isCancelled -> unchanged
  result). 774 unit tests (772 + 2). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-confirmed - awaits the user's next repeat of the same on-device test. See history §138.

- **§137 (v0.8.100): D-213 - the actual dominant cost found: trySplit()'s own main-thread dictionary
  reads, a completely different path from everything D-207-D-212 addressed.** Third repeat of the same
  test against the D-212 (WAL) build: "Leider war es das noch gar nicht... Es ist eigentlich schlimmer denn
  je" - "Geburtstag" still ~300-450ms/char. Root-caused: `TokenRepair.candidateAt()` (called once per split
  position `trySplit()` tries) costs up to ~8 SQLite reads each (`resolveWord()` x2, `frequencyOf()` x3 -
  one redundant, `partsOfSpeech()` x2, `bigramFrequency()`) - a 10-character token can cost 50+ reads in one
  `trySplit()` call. This runs from `composingPreviewRunnable` (the S-05/§47 live highlight/split-colour
  preview), a completely different code path from the suggestion-bar search D-207 through D-212 all
  addressed - §125/D-194's own original "cheap enough to leave on the main thread" reasoning was never
  actually measured and turned out to be wrong, only exposed as the dominant cost once the other path got
  fixed. Fixed identically to D-211/D-212's own shape: new `composingPreviewExecutor` (kept separate from
  `expensiveSuggestionExecutor` so both run concurrently under WAL instead of queueing on one thread),
  reuses `expensiveSuggestionSeq` as a shared staleness signal. `composingPreviewRunnable` now does only the
  cheap `InputConnection`-dependent part (`isEditingMidWord`) on the main thread, snapshots the mutable
  `composingTaps`/`composingFlags`-derived state (a real data-race risk otherwise, not just staleness) before
  dispatching `trySplit()`/`isKnownWord()` to the background executor, and re-validates freshness (including
  a freshly re-read `currentInputConnection`) before ever applying the result. The artificial
  `EXPENSIVE_SUGGESTION_DELAY_MS` debounce is gone too (dispatches immediately via `handler.post`), same
  reasoning as D-211. Dead code removed: `shouldHighlightComposing()`/`splitPreview()` (superseded),
  `EXPENSIVE_SUGGESTION_DELAY_MS` (now unused). No new tests (Android threading/InputConnection glue,
  established gap; `TokenRepair.trySplit()`'s own pure logic unchanged, already covered). 772 unit tests
  (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - fourth round
  of the same investigation. See history §137.
- **§136 (v0.8.99): D-212 - WAL enabled; D-211 alone confirmed insufficient, root-caused to
  connection-level lock contention.** User re-ran §135's own test against the D-211 build: "Das hat
  leider noch so gar nicht funktioniert" - measured, "Geburtstag" still ~450ms/char, no better than before
  D-211. Root-caused: `SqliteDictionaryStore` never enabled write-ahead logging, so SQLite's default
  rollback-journal mode serialises *all* connection access across threads (even pure reads) - D-211's
  background search and the main thread's own still-synchronous prefix-completion query were contending for
  the same lock, so the main thread was still getting blocked *indirectly*, and D-209's now-larger primary-
  bucket query made it worse, not better. Fixed: `SqliteDictionaryStore.onConfigure()` now calls
  `enableWriteAheadLogging()` - additive, reaches every existing install automatically. Also discussed:
  whether a running query can be aborted mid-flight via `android.os.CancellationSignal` (confirmed genuinely
  possible - a real progress-handler interrupt, not just a pre-check) but deliberately deferred, kept in
  reserve only if a fresh log after WAL still shows a problem - wiring it through would touch the
  deliberately Android-free `DictionarySuggestionProvider`/`DictionaryStore`, though the user's own instinct
  is this is solvable without actually breaking that testable abstraction, worth designing properly if it
  comes to that rather than bolting it on now. Also confirmed: the background executor is single-threaded,
  so "many parallel queries" was never actually possible - only one in-flight query's own duration is ever
  wasted when superseded, a small bounded cost distinct from the lock-contention problem WAL fixes. No new
  tests (SQLite configuration, exercised indirectly by the existing Robolectric store tests). 772 unit
  tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - awaits
  a repeat of the same test to see what, if anything, remains. See history §136.
- **§135 (v0.8.98): D-211 - the expensive suggestion search moves off the main thread entirely,
  closing out the sluggishness investigation.** User's own follow-up idea after §134: instead of D-160's
  main-thread debounce (still blocks the UI when it fires), run the search on a background thread and
  discard the result if superseded - correctly anticipating both real pitfalls themselves (a forced thread
  kill is unsafe/`Thread.stop()` is deprecated since Java 1.2; needs cooperative "recognise yourself as
  stale" checks, including for a callback that lands late). Confirmed as the better fix, not just an
  addition: `handler` is bound to the main `Looper`, so D-160/D-208's debounce only ever delayed *when* the
  search ran on the main thread, never moved it off - a working precedent for the proposed shape already
  existed in `tier3Executor` (background thread + sequence-number staleness check before/after), just
  missing a mid-search check the fuzzy/compound/wide-fuzzy candidate-bucket scan actually needs (unlike
  tier3's single bounded inference call). Implemented: `expensiveSuggestionToken`/`expensiveSuggestionRunnable`
  (`Handler.postDelayed`) replaced by `expensiveSuggestionExecutor` (mirrors `tier3Executor`) + `AtomicInteger
  expensiveSuggestionSeq`; no artificial delay at all now - a fast typing burst just produces near-instant
  bail-outs, a genuine pause gets its result the moment the search finishes. New `SuggestionProvider.
  suggestionsFor(isCancelled)` parameter (default no-op); `fuzzyNeighbours()`/`wideFuzzyNeighbours()` (the
  actual expensive loops per D-208/D-209) now poll it once per candidate and break early - checked before
  starting, during, and a third time on the main thread before ever applying the result, so a late callback
  is recognised as stale and discarded rather than overwriting newer state. 2 new tests
  (`DictionarySuggestionProviderTest`: early-stop-on-cancel, and a never-cancels control case). 772 unit
  tests total (770 + 2). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
  See history §135.
- **§134 (v0.8.97): D-207/D-208/D-209 - a real sluggishness investigation, traced to three distinct
  causes from real device logs, all fixed; D-210 captured (split-regression, separate round needed).**
  User supplied real `AdaptKeyJitter`/`AdaptKeyHaptics` logs from repeated typing sessions. **D-207**:
  commit-time ~400ms stall on longer/harder words (`"Kiza"`->`"Liga"`, `"mitnehme"`), precisely measured via
  raw log timestamps - traced to `finalizeAndCommit()` calling `highConfidenceCorrection()` AND
  `autocorrectFor()` independently on the same token, each running its own full `correctionCandidates()`
  search, purely to answer a yes/no split-veto question the wider search's own result already answers.
  Fixed: `bestCorrection()` now returns cost alongside the word; new `SuggestionProvider.bestCorrectionFor()`
  answers both from one search. **D-208**: mid-word sluggishness (fluid for 1-2 chars, then consistently
  300-500ms/keystroke) - traced to `fuzzyNeighbours()` running unconditionally on every keystroke once the
  token reaches length 3, cost scaling with token length, never covered by D-160's existing debounce (unlike
  D-116/D-117/D-131). Two red herrings ruled out first (a deliberate long-press for `ü`; a suggestion-bar
  tap completing "Herzlichen") - both confirmed as expected behaviour, not bugs. Fixed: fuzzy matching moved
  behind `includeExpensiveFallbacks` (but NOT also gated on `candidates.isEmpty()`, unlike D-116/D-117 -
  D-12's "mut"->"mit" must still surface alongside "mut"'s own prefix completions); the deferred-pass
  scheduling in `refreshSuggestions()` now always fires (not just when empty) so fuzzy gets its own
  200ms-debounced chance regardless of what prefix completion found. **D-209**: separately, "Kita" (freq 17)
  found not to autocorrect at all for "Kiza" - confirmed against real data that its own first-letter bucket
  (636 same-length 'k'-words, 389 more frequent) exceeds the ~285 per-bucket cap, starving it out before the
  edit-distance comparison - the same class of bug D-197 already fixed for `diacriticCandidates()`. Fixed
  narrowly: only the token's own literal first-character bucket is now uncapped (not the neighbour-key
  buckets) - safe specifically because D-208 already moved this search off the per-keystroke hot path.
  6 new tests (`DictionarySuggestionProviderTest` 5, `SqliteDictionaryStoreRoboTest` 1 new + 1 adjusted).
  770 unit tests total (764 + 6). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-confirmed - this was explicitly an "all registers" fix per the user's own request given how
  unacceptable the lag had become. **D-210 captured, not designed**: a real A-05 split regression
  (`"übrigebs"` -> `"übrig Ebs"`, nonsense) the user flagged as recurring more often lately and wants
  addressed as its own dedicated round - deliberately not traced this round to avoid distracting from the
  performance work. See history §134.
- **§133 (v0.8.96): D-205 designed and implemented - suggestion-bar ranking now discounts by edit cost,
  not just frequency.** User's position: for a fuzzy/correction candidate, closeness to the actual mistake
  should generally outweigh raw frequency - `bestCorrection()` (autocorrect) already ranks cost-first, but
  `suggestionsFor()` (the bar) ranked purely by frequency. Two scope levels discussed: (1) rank the existing
  static-adjacency edit cost first in the bar too (small, self-contained); (2) weigh the actual recorded
  touch coordinates (T-02) against T-03's personalised model (bigger, needs raw taps threaded into the
  Android/touch-free provider). **User chose (1) first**, explicitly deferring (2) over performance concerns
  (this app has fought typing sluggishness multiple times before - D-153/D-160/D-194/§102/§125 - and didn't
  want that risk reopened); (2) stays captured for later with that constraint attached. Also chose a **soft,
  weighted blend** over `bestCorrection()`'s own hard cost-first rule - an overwhelmingly more frequent
  farther candidate can still occasionally win. Implemented: `fuzzyNeighbours()`/`wideFuzzyNeighbours()` now
  surface each candidate's edit cost; new `scoreWithCost()` discounts `score()` by
  `FUZZY_COST_DECAY^cost` (`FUZZY_COST_DECAY = 0.01`, calibrated against the real `dict_de.tsv` frequency
  range 8-1,000,000 - cost-1 needs ~100x the frequency to win, cost-2 ~10,000x, both reachable at the
  corpus's extremes). Prefix completions (cost 0 by construction) and `bestCorrection()`'s own autocorrect
  ranking are both unaffected. 2 new tests (`DictionarySuggestionProviderTest`: a realistic closer-wins case,
  and an extreme-frequency case proving the rule is soft, not absolute). 764 unit tests total (762 + 2).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - `FUZZY_COST_DECAY` is a
  starting point, easy to retune in isolation. See history §133.
- **§132 (v0.8.95): D-206 designed and implemented - archaic-spelling relics blacklisted, not purged;
  BlacklistActivity now defaults to user-only entries.** Grounded in real data before choosing a direction:
  of 1648 ß-containing `dict_de.tsv` entries, only 40 show the actual pre-1996-reform-relic frequency
  signature (`"daß"` 868 vs. `"dass"` 61892) - the other 74 run the opposite way and are genuinely modern,
  correctly-spelled ß words with a rare Swiss-spelling counterpart (`"große"`/`"grosse"`), never to be
  touched. Even within the 40, 2 are coincidental word collisions (`"Maße"`≠`"Masse"`, `"Buße"`≠`"Busse"`)
  and several are proper nouns/surnames (`"Keßler"`, `"Reuß"`, `"Elsaß"`, ...) excluded from any automated
  action. User confirmed the much-smaller-than-assumed scope and approved reusing the existing A-04 BUNDLED
  blacklist (same mechanism as `due`/`sue`/`ddr`/`aks`) over purging the dictionary or inventing a new
  "derank" concept. A curated 25-word list (`daß`, `muß`, `mußte`, `Fluß`, `Rußland`, `Kongreß`, `häßlich`,
  ...) seeded into `BUNDLED_GERMAN_BLACKLIST` - stays typeable/known, never surfaces as its own suggestion
  again, still silently autocorrects to the modern form via the pre-existing §44 known-word-override (cost-0
  under the pre-existing `ß`->`"ss"` fold). Also, per the user's own follow-up: `BlacklistActivity` now
  defaults to showing only `USER`-category entries (a `BUNDLED` entry is rarely of interest and should
  rarely be removed - "da muss man sich schon sehr sicher sein"), with a new unchecked-by-default
  `blacklist_show_bundled` checkbox (all 3 locales) to reveal bundled entries when genuinely needed. No new
  tests (Android-facing glue, established gap - the underlying blacklist mechanics were already covered when
  first built). 762 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-confirmed. See history §132.
- **§131 (v0.8.94): D-204 - a second, app-specific ß fold convention ("gruse" for "Grüße"); D-205/D-206
  captured for later design rounds.** "vieie gruse" never autocorrected to "viele Grüße"; separately "viele
  Gruße" restored correctly at commit but the bar showed "große"/"größe" far ahead of "Grüße". Root cause,
  corrected mid-round by the user's own precise pushback: `"gruse"` isn't a 2-edit typo of `"Grüße"` at all -
  this app hosts `ß` as `s`'s long-press alternative (mirroring `ä`/`ö`/`ü` on `a`/`o`/`u`), so a bare `s` tap
  is a direct, position-preserving stand-in for it, lining up 1:1 with no missing/extra character - an exact
  diacritic match, not a fuzzy one, so it should never have reached the frequency-floored (`MIN_AUTOCORRECT_
  CANDIDATE_FREQUENCY=300L`, D-114) `bestCorrection()` path at all. Fixed: new `Umlaut.foldToHostKey()`/
  `foldVariants()` (ß -> single `s`, alongside the existing, still-tested ß -> `"ss"` convention - neither
  crowds out the other); `DictionarySuggestionProvider.diacriticRestoration()` now accepts either variant.
  Also fixed the position-1 requirement: `refreshSuggestions()`'s S-06 `pending` chip previously consulted
  only `autocorrectFor()` (which *does* apply the D-114 floor unconditionally) - a D-111/D-112 comment had
  wrongly assumed a diacritic-fold case was "already covered" there; now consults `diacriticRestoration()`
  first, mirroring `finalizeAndCommit()`'s own existing precedence, so the correction is pinned via the
  existing S-06 mechanism instead of buried in the frequency-sorted list. 6 new tests (`UmlautTest` 4,
  `DictionarySuggestionProviderTest` 2, incl. a `"russ"`->`"ruß"` regression check). 762 unit tests total
  (756 + 6). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed. **D-205/D-206
  captured, not designed** (user explicitly deferred both, "dazu später"): D-205 - suggestion-bar ranking
  should weigh edit/touch-proximity to the actual mistake, not just raw frequency (today's `score()` has no
  cost term at all outside `bestCorrection()`'s own autocorrect-only cost-first ranking); D-206 - the bundled
  Wikipedia-derived dictionary still contains pre-1996-reform spellings (e.g. `"daß"`) that inflate it and get
  suggested back to modern users - user wants a plan (purge vs. flag-and-derank) before any fix, noted as
  overlapping D-204's own ß/"ss" territory. See history §131.
- **§130 (still v0.8.93, no code change): D-107 closed - S-05 highlight semantics confirmed settled.**
  User confirmed the current meaning (green = safe, no correction planned) is correct as-is; the previously
  floated reversal ("green = about to be auto-corrected") is not adopted. Spec §S-05's open-design-question
  callout replaced with a confirmed-settled note. 756 unit tests (unchanged). See history §130.
- **§129 addendum (still v0.8.93, no code change): D-203 CONFIRMED WORKING on device.** User confirmed the
  redesigned split gate (`MIN_SPLIT_HALF_FREQUENCY`/both-nouns-rejection/no-bigram-gate) now correctly
  handles the "der"+"Kinderarzt"-class case on a real device. D-203 closed. 756 unit tests (unchanged).
- **§129 (v0.8.93): D-203 - the MIN_SPLIT_BIGRAM split gate replaced after a live design
  discussion, grown out of D-167.** Asked directly why mid-word re-edit (`splitAtUnresolvedConnector`,
  D-122) finds "der"+"Kinderarzt" but ordinary commit-time typing (`trySplit`) does not - confirmed against
  the bundled corpus (not guessed): `bigram_de.tsv` has zero co-occurrences for "der"+"kinderarzt" even
  though "Kinderarzt" is a well-known noun. `MIN_SPLIT_BIGRAM` required exactly that prior co-occurrence,
  which a first-time-typed compound typo can never have by definition - this reframed D-167 entirely (not
  a confidence problem, a structurally-too-strict gate). Redesigned from a live brainstorm, calibrated
  against real data before implementing: a naive "frequency floor alone" doesn't separate "Mei"(16)/"St"(5939)
  from "Kinderarzt"(14) - the bundled dictionary is already pre-filtered at freq>=8; a naive "one half must
  be a noun" rule would have broken this project's own existing valid-split tests ("und"+"das", "aber"+"das",
  no noun on either side) - refined to reject only when *both* halves are nouns instead. Implemented (shared
  by both `trySplit()`/`splitAtUnresolvedConnector()`): (1) `RegularVerbInflection.isPlausibleInflection`
  now also guards `TokenRepair` (never previously consulted there) - closes "meinst"->"mei"+"st" at the
  source; (2) `MIN_SPLIT_HALF_FREQUENCY=10` per half; (3) both-nouns pairs rejected; (4) `MIN_SPLIT_BIGRAM`
  removed as a gate entirely (still feeds `score()`'s ranking). Also connected to the umlaut/ß-fold strategy
  per direct instruction: `resolveWord()` tries `Umlaut.unfoldCandidates()`, so a half typed without its
  diacritic ("uber") still resolves via "über" for eligibility/POS purposes - `SplitResult` itself still
  carries the literal typed substrings (keeps §47 span-colouring correct; restoring the diacritic in the
  committed text too would be a separate future feature). 6 new tests, every pre-existing bigram-dependent
  test rewritten to match the new behaviour (`TokenRepairTest`, 23 -> 29). 756 unit tests total (750 + 6).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed. D-167's own
  embedded-capital-confidence idea stays captured for a possible later round. See spec §129.
- **§128 (v0.8.92): D-202 implemented - higher learn threshold for a suspected unsplit compound.**
  Follow-up to §127. User decided: D-116's `compoundCandidate()`/`CompoundSplit` recognition (heuristic a)
  is sufficient alone (a false positive only delays learning by a couple more repetitions, no real harm);
  a cheap embedded-mid-word-capital pre-check (heuristic b) runs first since it needs no dictionary lookup.
  Scope confirmed as proposed: only the not-yet-learned `PendingLearnStore` path, not already-learned-word
  reinforcement. New `SuggestionProvider.looksLikeUnsplitCompound()` (implemented in
  `DictionarySuggestionProvider` by reusing `compoundCandidate()` unchanged); `AdaptKeyService` gained
  `hasEmbeddedCapital()` + `learnThresholdFor()`, now consulted at the one D-37 promotion site instead of
  the hard-coded `LEARN_THRESHOLD` - selects the new `COMPOUND_LEARN_THRESHOLD = 4` over the ordinary `2`.
  Both checks re-run fresh every time, nothing cached, so a later code change reclassifies an already-pending
  word retroactively with no migration. The counter-decrement-on-undo half needed no change - already wired
  into A-07 undo via `PendingLearnStore.decrement()`. Explicitly independent of D-167 (still undecided, live
  split-*confidence* while typing) - this only throttles learning. 3 new tests
  (`DictionarySuggestionProviderTest`). 750 unit tests total (747 + 3). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. Not yet device-confirmed. See spec §128.
- **§127 (v0.8.91): D-201 fixed (doubled space on a mid-word connector-split chip); D-202
  captured.** D-201: applying the D-122 `"der Kinderarzt"` connector-split chip while re-editing mid-word
  into an already-space-terminated `"dervKinderarzt "` doubled the space - exactly the gap §117/D-183 had
  already flagged as present-but-unreproduced in `applyMidWordSplitSuggestion()`/`applySplit()`, now
  reproduced and fixed by hoisting the existing D-144/D-183 "don't double an already-present space" check
  above the D-122 branch split in `onSuggestionClicked()` and threading the computed delimiter through
  instead of `applySplit()`'s previous hardcoded `" "`. No new tests (established `AdaptKeyService` glue
  gap). 747 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-confirmed. D-202 (captured, not implemented): a suspected incorrectly-unsplit compound should need
  4 uncorrected occurrences instead of the ordinary `LEARN_THRESHOLD = 2` before D-37 promotes it - the
  counter-decrement-on-undo half of this **already exists** (`PendingLearnStore.decrement()`, already wired
  into A-07 undo); only the threshold itself needs to become a live, per-word decision instead of a fixed
  constant (architecturally small). Open question needing the user's choice: the detection heuristic for
  "vermutlich zusammengesetzt" - reuse D-116's `compoundCandidate()`/`CompoundSplit`, the D-167
  embedded-capital signal, or a combination - plus confirming scope stays limited to the not-yet-learned
  `PendingLearnStore` path. See spec §127.
- **§126 (v0.8.90): D-196/D-197/D-198 fixed; D-167/D-199/B-03(D-200) captured as design discussions.**
  Batch of seven items from one feedback round. **Fixed**: D-196 - suggestion-bar chips showed the raw
  dictionary-stored case while the commit path (`onSuggestionClicked`) already recomputed the full §6
  hierarchy - `AdaptKeyService.showSuggestions()` now capitalises every `Kind.NORMAL` item's *display* text
  with the exact same `capitalisation.capitalise(item.word, contextFor(...))` call the commit path uses, so
  the two can no longer diverge (`SuggestionController` itself untouched, stays Android/capitalisation-free).
  D-197 - "Gruße" wasn't restored to "Grüße" (suggested "Große" instead): fold/cost math was already correct
  (both fold to "grusse", cost 0) - the real bug was `correctionCandidates()`'s frequency-truncated SQL LIMIT
  (§125's own bounding, meant for the *edit-distance* search) silently starving a rare-but-correct diacritic
  candidate before `diacriticRestoration()`'s cheap exact-match check ever saw it. New
  `DictionaryStore.diacriticCandidates()` (SQLite override: same bucket query, no per-bucket cap - safe since
  the per-candidate check here is O(1), not a DP) fixes it. D-198 - email keyboard's period-key popup
  pre-selected `.net` instead of the locale's `.de`: traced to the popup row's own edge-clamping (email's
  4-entry TLD list is wider than URL mode's 3-entry one, same key position near the right edge) combined with
  `ACTION_MOVE` re-deriving the popup selection from raw pointer x on every move with no threshold - fixed
  generally (not patched to this one popup) by only updating selection past the system touch slop from the
  original touch-down, mirroring the existing D-108 long-press-smear pattern. 1 new test
  (`SqliteDictionaryStoreRoboTest`, D-197's regression). 747 unit tests total (746 + 1). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. None of the three device-confirmed yet. **Captured, not implemented**: D-167
  (embedded-capital split-confidence boost - two directions discussed, score-bonus-only vs. relaxing the §45
  bigram gate for a noun-shaped right half, awaiting the user's choice); D-199 (autocorrect-reapply-after-reject
  strategy, explicitly requested as a proposal only - extend the existing single-slot A-07 undo state into a
  one-shot "just rejected, commit verbatim once" suppression instead of clearing it immediately, scoped to
  expire on the next *different* word committed rather than a time constant); B-03/D-200 (hyphenated-compound
  learning, e.g. "E-Mail-Adresse" - researched first: hyphen bigram/next-word data is already collected
  identically to space-delimited words, so both of the user's proposed strategies are about the *suggestion*
  side, not new recording; strategy 1 chains next-word predictions across hyphens into one multi-part chip,
  strategy 2 (the original spec's B-03) promotes a repeatedly-confirmed chain into a learned whole-phrase unit,
  generalising D-37/D-110's existing single-word precedents - neither implemented, open questions on hop
  depth/promotion threshold/commit mechanics await the user's direction). See spec §126.
- **§125 (v0.8.89): D-194 - typing/backspace-hold sluggishness traced to three unthrottled
  per-keystroke lookups, root-caused and fixed after a design discussion.** Reported: typing and
  held-Backspace both feel sluggish, worsening with the composing token's length, worst for a long token
  that never resolves to a real word - also floated as the likely real explanation for D-184's "flash" tuning
  having no visible effect. Traced to three gaps, none covered by D-160's (§103) existing debounce: (1)
  `updateComposing()`'s S-05/§47 split-preview/highlight colouring ran `TokenRepair.trySplit()` synchronously
  on every keystroke - an O(token length) scan of split points, never short-circuiting for a token that never
  becomes a known word; (2) `DictionarySuggestionProvider`'s base `fuzzyNeighbours()` (D-12) ran an uncapped
  Levenshtein DP per candidate, every keystroke; (3) `refreshSuggestions()`'s `duringRepeat` branch (D-138/
  D-153) still called `provider.autocorrectFor()` unconditionally during backspace-hold, missed when that
  gate was introduced. Design discussed before implementing (per this project's own convention): debounce the
  colouring like D-160 already does for suggestions (chosen), a hard token-length cutoff (rejected - real
  three-part compounds get long fast), and a banded/early-exit edit distance (chosen, pure optimisation, no
  behaviour change). A fourth idea - caching which token prefixes are already-confirmed compound-component
  candidates - was raised but deliberately deferred to its own round (new per-token mutable state with real
  invalidation rules, not something to fold in speculatively next to the §99-§101 guardrail's territory).
  Fixed: `EditDistance.weightedDistance()` gained an optional banded `maxCost` (Ukkonen's banded edit
  distance - `O(a.length*b.length)` to `O(max(a,b).length * band)`), threaded through every
  `DictionarySuggestionProvider` call site with its own real ceiling so no candidate that used to qualify
  stops qualifying; a new `ComposingPreview` cache (mirrors `expensiveSuggestionToken`'s shape) makes
  `updateComposing()` read a debounced, cached split/highlight result instead of computing it inline -
  continuous typing renders uncoloured until a genuine pause, coloured 200ms later, matching the trade-off
  the user explicitly accepted; and `duringRepeat`'s `pending` autocorrect lookup was dropped outright (no
  trade-off - already-established "unreadable mid-repeat" reasoning). 5 new tests (`EditDistanceTest`:
  banded-vs-unbounded agreement, band-too-narrow signalling, length-difference and longer-token regression
  cases). 746 unit tests total (741 + 5). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-confirmed - D-184 stays open pending the same session, expected to improve as a side effect rather
  than needing its own fix. **D-195** (the fourth idea, a compound-prefix confirmation cache spanning both
  `trySplit()` and D-116's `compoundCandidate()`) was shelved after discussion, not implemented: besides the
  new-mutable-state/invalidation risk already named, the user raised a real objection - the actual best split
  boundary can shift a character or two later as more of the token is typed, which a naive "confirmed forever"
  cache would fight against rather than accommodate. Captured for its own round if it comes up again. See
  spec §125.
- **§124 (v0.8.88): D-193 - key vibration finally closed.** Diagnostic logging
  (`AdaptKeyboardView.logHaptics()`, mirrors `AdaptKeyService.diag()`'s dual logcat + in-app-`DiagnosticLog`
  output) added after D-06/D-34/D-66/D-75 failed three device rounds with the root cause never pinned down
  from an actual log. The repro log showed `vibrate()` succeeding with `USAGE_TOUCH` on every keystroke, no
  exception, no `hasVibrator()` failure - proving the app's own code was correct all along. **Confirmed by
  the user: the actual cause was a central system setting on the device, outside the app's control.**
  D-06/D-34/D-66/D-75/D-193 are all closed. The diagnostic logging is left in place (low-risk, proven
  useful). 741 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. See
  spec §124.
- **§123 (v0.8.87): D-192 - feature catalog refreshed + polish.** `FeatureCatalog.kt` was untouched since
  D-89 (v0.7.43); confirmed stale via `git log` and grew from 18 to 26 entries (`d89_f19`…`d89_f26`, all
  three locales) covering everything genuinely new and user-facing since: credential memory (D-142/D-180/
  D-190, merged into one entry), contact-derived email suggestions (D-191), the URL/email dedicated
  keyboards (D-143/D-158/D-185, merged), the learned-words editor (D-177), password-manager autofill
  (D-135), unhyphenated-compound recognition (D-116), the connector-split suggestion (D-122), and the extra
  row itself (never catalogued before). Also added: a coloured "Learn more" hint on the feature-overview
  settings entry (new `R.color.link_text`, a `SpannableString` built in `SettingsActivity`, matching
  `info_version`'s own established programmatic-summary pattern) - and trimmed the D-191 setting's own
  summary text (the "redundant with your address book" clause was meant to explain the design choice while
  building it, not for the end user - struck per feedback, `AdaptSettings`'s own KDoc keeps the full
  reasoning). 741 unit tests total (unchanged - resource-only + `FeatureCatalogTest`'s existing assertions
  already cover the growth). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed
  (2026-07-19): works as intended, closed.** See spec §123.
- **§122 (v0.8.86): D-191 - opt-in contact-derived email suggestions.** `READ_CONTACTS` added to the
  manifest (first permission beyond `VIBRATE`; no `INTERNET` either way). New
  `AdaptSettings.contactsSuggestionsEnabled` (default off) threaded through the usual settings pipeline;
  `SettingsActivity` (not the keyboard service - only an `Activity` can show a runtime permission dialog)
  requests `READ_CONTACTS` exactly when the toggle is switched on via `registerForActivityResult`, blocking
  the immediate flip until the OS answers; already-granted/switching-off skip the dialog. `AdaptKeyService.
  loadContactEmailsAsync()` queries `ContactsContract` on a new dedicated `contactsExecutor` once per EMAIL
  field focus (never per keystroke, mirroring D-160's hot-path reasoning), capped at 2000 rows, applied to
  `contactEmailCache` on the main thread. `showCredentialSuggestions()` now ranks via a new
  `mergedCredentialEntries()` - cached contact addresses wrapped as never-persisted, frequency-0
  `CredentialEntry` values, deduplicated against real stored entries - flowing through the existing
  `CredentialRanking` unchanged, so `@`-domain-completion picks up contact domains too for free. 1 new test
  (`SettingsMapperTest`). 741 unit tests total (740 + 1). `:app:assembleDebug`/`:app:testDebugUnitTest`
  green. **Device-confirmed (2026-07-19): works as intended, including the permission-dialog flow and a
  real address-book read. Closed.** See spec §122.
- **§121 (v0.8.85): D-190 - "Never save credentials" setting.** New `AdaptSettings.neverRecordCredentials`
  (default off), threaded through `RawSettings`/`SettingsMapper`/`SettingsStore` like `tier3Enabled`/
  `diagnosticLogEnabled`; a `SwitchPreferenceCompat` in `cat_dictionary` (DE/EN/EL), title+summary spelling
  out that passwords are never affected (already unconditional in the code) and already-saved values keep
  being suggested - only *new* recording is gated. Gates `captureCredentialIfLoginField()` and the
  `credentialSnapshot` accumulation in `commitVerbatimFieldFragment()`; `showCredentialSuggestions()`
  untouched by design. Came with a Contacts-permission question (recommended against building an
  auto-request-on-first-email-field flow - a runtime permission dialog needs an `Activity`, the IME service
  can't show one itself; a Settings-only opt-in toggle would be the right shape if ever built, not built this
  round). 1 new test (`SettingsMapperTest`). 740 unit tests total (739 + 1). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19): works as intended, closed.** See spec §121.
- **§120 (v0.8.84): D-189.** `SettingsRowView` renamed to `ExtraRowView` throughout (class/file, the
  `extraRow` field, every `AdaptKeyService` handler - `openExtraRow`/`closeExtraRow`/
  `dismissKeyboardOrCloseExtraRow`/every `...FromExtraRow()` button handler; `openSettingsAppFromExtraRow()`
  keeps "Settings" only for the gear's own Settings-app action) - requested directly, the row long ago
  outgrew being about the settings button alone. Bundled with a reveal-bug fix found while testing D-174:
  entering a *reliably* classified EMAIL/PASSWORD field set `credentialModeActive` silently, never opening
  the row to show it - only the *weak-signal* nudge path did. Now opens for both (mutually exclusive by
  construction, never double-opens), and the credential button flashes on both paths too, deferred via a new
  `ExtraRowView.open(onOpened)` callback so it fires once the row's slide-in animation has actually finished,
  not while still mid-animation. 739 unit tests total (unchanged). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19): works as intended, closed.** See spec §120.
- **§119 (v0.8.83): D-174 closed.** Root cause confirmed from a real K9 Mail device log: the recipient
  field's `inputType` correctly classified as EMAIL (`loginFieldKind` was never the bug), but K9 converts
  the typed address into a recipient "chip" and tears the field's `InputConnection` down the instant focus
  leaves it - faster than `onFinishInput()`'s own fallback read of it (`getTextBeforeCursor()` came back
  null, confirmed in the log by a burst of "ground truth unavailable" `onUpdateSelection` callbacks
  immediately before it). Fixed with a new in-memory `credentialSnapshot` (`AdaptKeyService`) that mirrors
  every fragment `commitVerbatimFieldFragment()` actually commits into a login-relevant field, used only as
  a fallback when the `InputConnection` read fails - the ordinary case (connection survives) is unchanged.
  739 unit tests total (unchanged - `InputConnection` timing glue, not reproducible without the exact
  app/timing that produced the log). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed
  (2026-07-19): works as intended, closed.** See spec §119.
- **§118 (v0.8.82): D-186/D-187/D-188 - device-confirmed, all three closed.** D-186: `learnWord()`/`learnWordStrong()` now skip
  `dictionaryStore.isBundledWord(word)` entirely (no write to the learned overlay) instead of reinforcing
  every already-bundled word there - the D-177 design that caused it ("die"/"du"/"immer" flooding the
  Learned Words editor) is reversed by direct instruction. A new `LEARNED_CLEANUP_VERSION`-gated
  `purgeBundledDuplicatesFromLearned()` (mirrors D-178's `BUNDLED_DICTIONARY_VERSION` precedent) flushes
  existing installs' accumulated duplicates once. D-187: none of the six settings-row button handlers
  auto-closes the row anymore (was `closeSettingsRow { action }` for every one) - by direct instruction, at
  minimum for the toggle buttons, for now across the board; `closeSettingsRow()` itself still serves the
  swipe-down gesture and the fresh-field reset. D-188: `BlacklistActivity`/`LearnedWordsActivity`/
  `CredentialsActivity` gained the same `WindowInsets` edge-to-edge padding fix `CalibrationActivity` already
  had (K-01/§13) - their controls were sliding under the status bar/cutout/gesture-nav, same root cause,
  same fix, applied to all three even though only two were named in the report. 3 new tests
  (`SqliteDictionaryStoreRoboTest`: learned-cleanup version + purge). 739 unit tests total (736 + 3).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19): all three work as
  intended, closed.** See spec §118.
- **§117 (v0.8.81): D-183 fixed.** Root cause confirmed from code, not the editor (initial Google Keep
  logs were a different, already-tracked D-139-class symptom - no suggestion tap in them at all; a fourth,
  Signal-based log isolated the real mechanism). `onSuggestionClicked()`'s existing D-144 "don't double a
  trailing space" guard (`getTextAfterCursor(1, 0)`) only worked when the real cursor sits at the *end* of
  the composing token - true for ordinary end-of-typing suggestion acceptance, but not for a D-62 mid-word
  reclaim, where the cursor sits *inside* the token (`composingCursor` = tap offset) and the "next character"
  is the token's own remaining text, not the real document text following it. Fixed by skipping past the
  token's own remaining characters (`composing.length - composingCursor`) before checking - reduces to the
  exact previous check for the ordinary (non-mid-word) case, no behaviour change there.
  `applyMidWordSplitSuggestion()`/`applySplit()` (the separate D-122 connector-split path) have the same
  class of gap but were not touched - never reproduced, different code path. 736 unit tests total
  (unchanged - `InputConnection` glue, established gap). `:app:assembleDebug`/`:app:testDebugUnitTest`
  green. **Device-confirmed (2026-07-19): works as intended, closed.** See spec §117.
- **§116 (v0.8.80): D-172/D-181 device-confirmed; D-184/D-185 implemented.** "aks"→"als" confirmed working
  problem-free on device - D-172/D-181 (§113) fully closed. D-184: root cause was structural, not the
  duration - `ACTION_DOWN` already cancelled the previous flash but never rendered a genuine "off" frame
  before the new press's highlight, so a same-key double-tap always read as one continuous flash regardless
  of how short `flashDurationMs` (28ms, already shortened twice in §13/§14) was; fixed with a new
  `paintSuppressedKey` one-frame (16ms) repress gap in `AdaptKeyboardView` that only masks the *paint*, never
  the underlying `pressedKey`/keystroke logic - no dropped-keystroke risk. D-185: the settings row gained a
  🌐 toggle button (second slot, right after the credential-mode button, `emojiButton` dynamically shifted
  one slot right while it's shown) that flips the existing `AdaptKeyService.urlMode` flag directly - every
  consumer already reading that flag (bottom-row swap, verbatim-commit, empty-suggestion-bar) respects a
  manual toggle for free. Button visibility tracks `isUrlField(info)` (the field's fixed type, stays
  reachable after toggling off); the row auto-opens on every URL-field entry, mirroring the existing
  `weakSignalKind` precedent. Resets to default-on every fresh field focus via the pre-existing
  `urlMode = isUrlField(info)` in `onStartInput` - no new reset logic needed. 736 unit tests total
  (unchanged - Android View/Service glue, same established untested-glue gap). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19): D-185 works as intended, closed. D-184
  showed no visible effect yet - left open for a future round rather than re-patched speculatively.** See
  spec §116.
- **§115 (v0.8.79): D-183/D-184/D-185 - three items captured to the backlog, not implemented,
  no code changed.** D-183: a mid-text suggestion-bar tap still inserts a superfluous space, including a
  wrong space *before* a period when applied right before one - not yet traced (needs a real device log with
  the exact suggestion/caret position/surrounding text). D-184: the typing "flash" highlight effect needs a
  shorter duration - feels sluggish and visually swallows double-taps - exact animation/constant responsible
  not yet identified. D-185: the existing automatic `urlMode` (D-142/D-143) needs to become togglable - a
  new settings-row button (shown only while `urlMode` is active, positioned second from the left right after
  the existing credential-mode button), defaulting on when a URL field is entered, switching to the ordinary
  letter keyboard when toggled off (motivation: a browser's URL bar, e.g. Chrome, can also be used to type an
  ordinary search query). 736 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest`
  green (unaffected, no code touched). See spec §115 for full detail on each.
- **§114 (v0.8.78): D-182 - `reclaimSurroundingWord()` stops deleting text it only needs to
  recolour.** Follow-up to §113: the "word deleted" symptom was already gone on the current build, but a
  related cursor-jump-to-start-of-field remained during the same reclaim. Root cause: `reclaimSurroundingWord()`
  deleted the committed text then `updateComposing()` re-inserted it moments later as styled composing text
  - briefly leaving the reclaimed span empty, with nowhere for the cursor to sit but the reclaim's start
  offset. A second, more serious device log from the same morning showed this same transient state actually
  causing data loss (not just a flash): a third, differently-stale `onUpdateSelection` echo's own
  ground-truth read agreed with the transient collapsed-at-anchor position, fooling §101's verification into
  tearing down a correctly-rebuilt reclaimed token entirely. Fixed by switching to
  `InputConnection.setComposingRegion(start, end)` - marks the already-existing text as composing in place,
  never removing a character, so there is no empty state left for a misbehaving editor (Gemini's search
  field, observed live in both logs) to echo or have ground truth agree with. Falls back to the old
  delete/reinsert only when the anchor is unresolvable (`anchor < 0`). Also confirmed (no code change): "aks"
  showing as `USER` in the blacklist was a leftover from a manual pre-fix addition -
  `seedBundledBlacklist()`'s upsert already corrects it to `BUNDLED` on the next app start; deliberately not
  turned into a reseed-on-every-visit mechanism, so a later manual re-add as `USER` will still stick. No new
  tests (established `AdaptKeyService`-internal glue gap). 736 unit tests total (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - the mid-text tear-down
  scenario specifically needs re-testing. See spec §114.
- **§113 (v0.8.77): D-172/D-181 - root cause found and fixed: "AKS" is a genuine bundled
  English dictionary entry.** D-172 (open since §108) finally closed by a fresh log with §109's diagnostic:
  `dictChoice.suppressAutocorrect=false knownInOtherLanguage=true` - the German-language classification was
  never at fault (`isForeign()`'s `minWords=2` gate really does return false for a single word, confirmed
  correct all along); the actual block was D-106 stage 2's cross-language shield, and `grep`-ing the actual
  asset confirms why: `dict_en.tsv` contains `AKS	18	PROPER_NOUN`, a genuine Wikipedia-derived English
  acronym entry. Architecturally identical to "due"/"sue" (§107) - a real word in another consulted language
  correctly protected from being corrected away, just for an entry the user never intends to type. Fixed the
  identical way: `"aks"` added to the existing `BUNDLED_GERMAN_BLACKLIST` (now `due`/`sue`/`ddr`/`aks`),
  seeded via the already-existing `seedBundledBlacklist()` - reaches every existing install automatically, no
  reimport needed. §109's "aks colours green" side finding (D-37 silently learning it, already fixed by
  D-178/§111's reimport) and this round's fix were two independent compounding causes - both are now
  resolved. No new tests (same established untested-glue gap as §107). 736 unit tests total (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19): "aks" now corrects to
  "als" problem-free.** D-172/D-181 fully closed. See spec §113.
- **§112 (v0.8.76): D-180 - saved credentials get their own reviewable list; a shared "Copy"
  option on every remove dialog.** The former top-level "Clear saved usernames & emails" action (D-142,
  `cat_info`) was all-or-nothing; user-designed replacement: new `CredentialsActivity` (settings screen,
  moved into `cat_dictionary` next to blacklist/learned-words/expiry) lists every `CredentialStore.all()`
  entry, a "Delete all" button (reuses D-142's existing confirm dialog unchanged), and per-entry removal via
  **long press** (not a plain tap - a saved credential is more sensitive than an ordinary word) opening a
  remove dialog with "Remove"/"Cancel" plus a neutral "Copy" button that writes the value to the clipboard.
  New `CredentialStore.forget(context, value)` removes one entry case-insensitively. The user generalised the
  copy idea immediately to the existing `BlacklistActivity`/`LearnedWordsActivity` remove dialogs too, which
  now share the same neutral "Copy" button via one shared string pair rather than per-screen duplicates.
  `SettingsActivity`'s now-dead `confirmClearCredentials()`/`CredentialStore` import removed. 3 new tests
  (`CredentialStoreRoboTest`: `forget()` scoping, case-insensitivity, no-op on unknown value). 736 unit tests
  total (733 + 3 new). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See
  spec §112.
- **§111 (v0.8.75): D-178/D-179 - one-time bundled-dictionary reimport flushes pre-D-177
  contamination; clearer remove-confirmation dialogs.** D-178: "aks" doesn't show up in the new Learned
  Words editor (§110) because it was learned on v0.8.73, before the split existed - it physically sits in
  `TABLE_WORDS` (the bundled table), indistinguishable from a real entry, since `learn()` had no separate
  table to write to yet at that point. New `BUNDLED_DICTIONARY_VERSION` (bumped to 1, tracked per-store in
  a new `meta` table separate from the schema `DATABASE_VERSION`) makes `DictionaryLoader.loadStores()` call
  new `SqliteDictionaryStore.resetBundledWords()` (wipes only `TABLE_WORDS`/`TABLE_BIGRAMS`, never the
  learned overlay/blacklist/pending marks) and reseed exactly once per store on the next load for every
  existing install - flushing "aks" and any similar pre-D-177 contamination back out, since it isn't a real
  dictionary word and the reseed won't reintroduce it. D-179: `BlacklistActivity`'s and
  `LearnedWordsActivity`'s remove-confirmation dialogs previously showed only the bare word with generic
  "Cancel"/"OK" - now titled with the actual action and word (`"Remove “%1$s”…"` / `"Forget “%1$s”?"`) and a
  positive button labelled "Remove"/"Forget" instead of "OK", across all three locales. 3 new tests
  (`SqliteDictionaryStoreRoboTest`: version default/round-trip, `resetBundledWords` scope). 733 unit tests
  total (730 + 3 new). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed. See
  spec §111.
- **§110 (v0.8.74): D-177 - learned words split into their own store, separate from the bundled
  dictionary; G-04/A-04 two-stage unlearn-then-provisional-blacklist; new Learned Words settings editor.**
  Grew out of two D-172 (§109) side observations ("diecVorschläge" wrongly blacklisted instead of unlearned;
  "aks" unreachable via G-04 at all since S-02 never suggests the current-input word) plus the user's own
  return to "ddr" (§107). User-designed, corrected mid-round twice by direct instruction: (1) a genuinely
  bundled word (`"ddr"`) has nothing to unlearn and goes straight to a permanent blacklist entry, unchanged
  from §107; (2) a self-taught word gets unlearned first and only *provisionally* marked pending, for a
  settings-editable `pendingBlacklistExpiryDays` window (default 7, 1-30) - only a genuine recurrence inside
  that window escalates it to a permanent `BlacklistCategory.USER` entry via `learnWord()`'s new
  `isPendingBlacklistRecurrence()` check; otherwise the mark simply expires and the word is an ordinary D-37
  candidate again; (3) the learned lexicon lives in a completely separate SQLite table
  (`TABLE_LEARNED`/`TABLE_LEARNED_BIGRAMS`/`TABLE_PENDING_BLACKLIST`, additive `CREATE TABLE IF NOT EXISTS`
  migration, no `DATABASE_VERSION` bump) rather than an `origin` column on the shared table as first proposed
  - "Ich würde das Wörterbuch absolut unangetastet lassen" - so a future bundled-dictionary update can never
  touch or reset what the user has personally taught the keyboard, and existing installs need no migration at
  all. New `LearnedWordsActivity` settings screen (mirrors `BlacklistActivity`, remove-only) makes every
  learned word reachable, including ones like "aks" that G-04 structurally cannot reach. All "does the
  keyboard know this" reads across both `DictionaryStore` implementations now merge bundled + learned sources
  transparently. 14 new tests (8 `InMemoryDictionaryStoreTest`, 6 `SqliteDictionaryStoreRoboTest`); all
  pre-existing tests in both files pass unchanged against the rewritten stores. 730 unit tests total (716 +
  14 new). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Settings/Activity side not yet
  device-confirmed. See spec §110.
- **§109 (v0.8.73): D-172 - English-language hypothesis ruled out by device confirmation +
  diagnostic log; `suppressAutocorrect=true` now confirmed as the actual block, source still unattributed;
  a second, self-inflicted finding surfaced along the way.** Log with §108's diagnostic showed
  `dict=GERMAN suppressAutocorrect=true autocorrected=null` - German confirmed active, `autocorrectFor()`
  never even called. Contradicts a full re-read of `LanguageClassifier.isForeign()`'s own `minWords=2` gate,
  which should make a single-word context return `false` immediately - not resolved by further hand-tracing,
  since the prior diagnostic conflated two independent `suppressAutocorrect` sources
  (A-03 language classification vs. D-106 stage 2 cross-language check) into one boolean and never logged
  `tokenContextBefore` itself. Both gaps closed: `finalizeAndCommit()` now logs `tokenContextBefore` and the
  two sources separately. **Second finding**: user noticed "aks" now colours green (S-05) and cannot be
  reached via G-04 drag-to-trash at all - explained (not guessed): repeated testing itself crossed D-37's
  `LEARN_THRESHOLD=2`, silently teaching the dictionary "aks" is real, purely as a side effect of the
  autocorrect block firing on each repeated attempt; unreachable via G-04 because S-02 never shows the
  current-input word as its own suggestion. Pointed to the existing `BlacklistActivity` (C-05) settings
  screen as the practical fix for the user's own device - no code change needed for that part. D-37's own
  learning threshold having no safety net against learning nonsense after 2 repetitions is left as its own
  open question. 716 unit tests (unchanged - diagnostic only). `:app:assembleDebug`/`:app:testDebugUnitTest`
  green. D-172 stays open pending the next captured log.
- **§108 (v0.8.72): D-172 ("aks"/"als") reproduced from a fresh device log, still no blocking gate
  found by hand-tracing - diagnostic added rather than guessing.** `finalizeAndCommit()` read in full this
  round (past where the original §105 trace stopped) to find the actual autocorrect-application logic.
  Every gate re-checked against this exact repro still comes back clear: length/frequency thresholds both
  pass, k/l confirmed keyboard-adjacent (cost 1), `RegularVerbInflection.isPlausibleInflection` traced by
  hand and returns false (no ending actually strips from a token ending in "s"), `LanguageClassifier.isForeign()`'s
  `minWords = 2` gate means a single-word context never even reaches statistical classification. One
  variable the diagnostic channel never showed: which dictionary (`dictChoice.language`) actually resolved
  as active - a concrete hypothesis (G-01 left on English, so the whole search ran against a lexicon with no
  "als" at all) would fully explain it with no bug anywhere, but isn't confirmed either way yet. Added
  `dictChoice.language`/`suppressAutocorrect`/`diacriticWord`/`autocorrected`/`rawCorrected` logging to
  `finalizeAndCommit()` to settle it on the next (reportedly easy) repro. 716 unit tests (unchanged -
  diagnostic-only). `:app:assembleDebug`/`:app:testDebugUnitTest` green. D-172 stays open.
- **§107 (v0.8.71): D-171/D-176 - "ddr" and D-164's cross-language shield both redesigned onto a
  real, data-driven A-04 blacklist, corrected mid-round twice by direct instruction.** User clarified "DDR"
  genuinely is a real German dictionary word (confirmed, frequency 4405) they almost never want against how
  often they type "der" - and revealed "due"/"sue" were never actually on a real blacklist at all, only on
  the narrower `CROSS_LANGUAGE_CONFUSABLES` Kotlin exception list from D-157/D-164. First proposed fix
  (blacklist "due"/"sue" in the *English* store) was wrong and corrected: the original design was always
  the *German* blacklist. Re-derived why that's actually correct: `knownInOtherLanguage()` only ever runs
  while German (or whichever language a token resolved to) is the active dictionary, so checking that
  dictionary's own blacklist inherently never engages once English is genuinely active - "due"/"sue" stay
  fully protected as real English words the moment the language actually switches, exactly matching
  `CROSS_LANGUAGE_CONFUSABLES`'s old guarantee, now via the general A-04 mechanism instead of a hardcoded
  list. Implemented: `knownInOtherLanguage()` now checks `dictionaryStore.isBlacklisted(token)`; new
  `BlacklistCategory.BUNDLED` (app-shipped, distinct from `USER`'s live drag-to-trash entries); new
  `installStores()`-driven `seedBundledBlacklist()` blacklists "due"/"sue"/"ddr" in German on every call
  (idempotent upsert, reaches an already-installed device immediately, no destructive `DATABASE_VERSION`
  reimport needed - see §106's own incidental finding on that reimport gap). "ddr" itself needs no
  cross-language logic at all - it's a real German word, so `isKnownWord()`'s existing
  `store.isKnownWord() && !isBlacklisted()` check alone makes it permanently non-"known" once blacklisted,
  regardless of how often it's typed afterward (D-37's pending-learn threshold cannot re-establish
  `isKnownWord` truth, since the blacklist AND-condition always wins) - Backspace-undo (A-07) is fully
  blacklist-agnostic and unaffected. 716 unit tests (unchanged - private service glue over already-tested
  store methods). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
- **§106 (v0.8.70): D-166 withdrawn (no repro, per direct instruction); D-170 fixed from a real
  device log; D-175 (credentials never learned) diagnosed as far as static reading allows, diagnostic
  logging added, not yet fixed.** **D-170**: in K9 Mail's email field, repositioning the caret mid-fragment
  (after "foo", before "."), Backspace, then typing "@" jumped the caret to the end and inserted "@" after
  "bar" instead of where intended - traced to `commitVerbatimFieldFragment()` never checking
  `composingCursor` at all (the login/urlMode branch in `finalizeAndCommit()` returns before its own
  D-119/D-120 mid-word check ever runs), so every delimiter in a login/email/URL field always landed after
  the whole fragment. Fixed by mirroring `splitComposingAtCaretAndCommit()`'s before/after split + arithmetic
  anchor directly inside the verbatim path. User's own alternative proposal (stop treating "@" as a
  delimiter) was declined and explained - "." would carry the identical latent bug, so the general fix was
  preferred over a per-character one. **D-175**: user's own email address, typed into a field with confirmed-
  active credential mode, is never learned. Their own hypothesis (suggestion bar disabled throughout email
  mode) checked and ruled out - `showCredentialSuggestions()` already renders real `CredentialStore` entries
  live whenever `loginFieldKind != NONE`; the bar isn't the problem. `captureCredentialIfLoginField()` traced
  end to end (called from both `handleEnter`/`onFinishInput`, thresholds/reset all look correct) - no gap
  found from static reading alone. Diagnostic logging added (every early-return reason plus the exact
  captured value/kind) rather than guessing further; stays open pending the next real occurrence's log.
  716 unit tests (unchanged - both changes are Android `InputConnection`/service glue).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. D-170 not yet device-confirmed.
- **§105 (v0.8.69): D-161 retuned (1000->500ms); large device-feedback batch - D-162/D-163/D-164
  fixed, D-173 answered as a design question, D-165-D-172 captured for their own rounds.** Every item
  traced against real code first, per this project's own convention. **D-162**: `SuggestionBarView`'s
  D-144 swipe-down-on-background fallback was unconditionally consuming every off-chip
  `ACTION_DOWN`/`ACTION_MOVE` regardless of direction, starving `HorizontalScrollView`'s own native scroll
  - fixed by always feeding `super.onTouchEvent()` first, only additionally claiming a confirmed downward
  swipe. **D-163**: `armShiftForNextWord()` had no `loginFieldKind`/`emailMode` check at all - email
  fields no longer auto-arm Shift (Caps Lock remains the user's own choice). **D-164**: "sue" too often won
  over a typed "die" - traced to the exact D-157 mechanism (a real English word shielded by
  `knownInOtherLanguage()`'s cross-language protection, with nothing in the German dictionary to correct it
  back); fixed by adding "sue" to `CROSS_LANGUAGE_CONFUSABLES` alongside "due", **not** by removing it from
  `dict_en.tsv` (an earlier in-round attempt to do that was corrected by the user and reverted before
  committing - "sue" remains a fully valid, protected English word everywhere else). **Incidental finding**:
  `DictionaryLoader` only seeds a store when empty, and `DATABASE_VERSION` has never been bumped despite
  several prior `.tsv` data edits (D-114, §68) - a future data-only dictionary fix needs a version bump (with
  its own real trade-off: wiping existing installs' learned words/blacklists) to reach an already-installed
  device at all. **D-173** (design question, answered not implemented): whether a lone/near-lone recognised
  word should take suggestion slot 1 - leaning against it, favours predictability (S-05's highlight already
  confirms recognition for free) over the marginal benefit of a less-empty bar. **Captured for later**:
  D-165 (mid-word split can destroy deliberately-typed capitalisation - `applySplit()` re-derives casing
  from generic rules, discarding what was typed), D-166 (why the same split only *previews*, never
  auto-applies - traced but genuinely inconclusive without a device repro), D-167 (the user's own proposed
  generalisation - an embedded mid-word capital should raise split confidence and be preserved - a real
  algorithm change needing its own design round), D-168 (popup alternatives never respect Shift/Caps Lock -
  root cause found in `drawPopupCell()`, needs careful scoping against symbol/TEXT-key popups before
  fixing), D-169 (email fields should also open the settings row + flash the credential button, timing
  needs its own design pass per the user's own flag), D-170 (a real caret-jump bug in email-mode fields -
  deliberately not guessed at, same class of bug §99-§101 needed real logs for), D-171 ("Ddr"/"Der" - the
  dictionary-side override is confirmed already correctly wired per D-113; likely the T-02/T-03 raw-touch
  path instead, needs a device log), D-172 ("aks"->"als" - every gate traced clears on paper; genuinely
  unresolved without a repro). 716 unit tests (unchanged - all three fixes are Android-glue/private-service
  changes, matching this project's established untested-layer precedent). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. None of D-162/D-163/D-164 device-confirmed yet.
- **§104 (v0.8.68): D-161 - self-healing recheck for a suspected WindowInsets timing race, per the
  user's own proposed mitigation.** Reported: sporadically, the keyboard appears stuck under the gesture
  nav bar on entering an app, jumping into place only on first touch - too rare to observe directly; the
  supplied device log had no window/insets evidence either way (the diagnostic channels only cover
  `onStartInput`/composing state). Candidate mechanism found by reading the code, explicitly **not**
  confirmed from a log: `onCreateInputView()`'s `onApplyWindowInsets` listener (§42/D-136) is the only place
  that sets the bottom/top padding keeping the keyboard clear of the gesture-nav/status-bar insets, and
  Android does not guarantee that callback fires before the freshly-created IME window's first visible
  frame - until it does, padding stays `0`, which would look exactly like the reported symptom, resolving on
  the next relayout (a touch). Implemented as the user proposed rather than trying to catch the exact race
  live: new `applyWindowInsetsPadding()` (shared computation, listener and recheck both call it) plus a
  one-shot `windowInsetsRecheckRunnable` scheduled from `onStartInputView()` for `WINDOW_INSETS_RECHECK_DELAY_MS`
  (1000ms) later - reads the real current insets directly via `ViewCompat.getRootWindowInsets()` (no
  dependency on a further callback), corrects `inputRoot`'s padding only if it actually differs, and logs
  only then - doubling as the diagnostic instrumentation discussed previously. No new tests (Android view/
  window-insets glue over already-tested `androidx.core.view` APIs). 716 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Not yet device-confirmed** - D-161 stays open
  either way (a silent recheck could mean the hypothesis is wrong, or just that it hasn't recurred yet; a
  logged correction would confirm both the mechanism and the fix at once).
- **§103 (v0.8.67): D-160 implemented - expensive suggestion fallbacks debounced off the hot path,
  design approved by the user (option 1).** The user's follow-up question (must the compound fuzzy search
  be dropped entirely?) answered no - the debounce removes the cost, not the feature: cheap prefix/fuzzy
  searches stay synchronous per keystroke; the expensive empty-candidates escalation (D-116 compound split
  incl. inner fuzzy pass, D-117 wide fuzzy, D-131 raw-coordinate) runs in one deferred pass only once the
  token has been stable for `EXPENSIVE_SUGGESTION_DELAY_MS` (200ms, retunable starting value). Mechanics:
  `SuggestionProvider.suggestionsFor()` gained `includeExpensiveFallbacks: Boolean = true` (default keeps
  every existing caller/test unchanged); the hot path passes `false` and schedules
  `expensiveSuggestionRunnable`; staleness is double-guarded (every fresh refresh cancels before its early
  returns, and the runnable re-checks `composing` equals its scheduled token). Deliberately untouched: the
  §47 `trySplit` preview and the per-keystroke pending-chip work - moderate constant costs, named as the
  next lever if residual long-word lag remains. 3 new tests (`DictionarySuggestionProviderTest`). 716 unit
  tests (was 713; +3). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed -
  awaits the same testing round as §101's D-139 fix.
- **§102 CAPTURED (v0.8.66, no code change): D-160 - strong typing lag on `gesamtparteilichen`,
  traced hypothesis, not yet implemented.** From a v0.8.65 device log: **no D-139 mechanism present** (zero
  wipes, composing stream position-perfect) - a distinct performance problem. Evidence chain: selection
  callbacks starved for ~2.5s of continuous typing then burst all at once right after the commit (ordinary
  Handler messages starving while prioritised touch input keeps preempting = AdaptKey's own main thread had
  no idle time); the app itself was demonstrably responsive (per-keystroke synchronous reads in
  `updateComposing` never stalled). Root-cause hypothesis, code-verified: `suggestionsFor()`'s expensive
  fallbacks (CompoundSplit incl. inner fuzzy pass, cost-4 `wideFuzzyNeighbours` over the whole first-char
  bucket, `rawCoordinateCorrection`) are all gated on `candidates.isEmpty()` - which for a long unknown
  compound is true on *every* keystroke, so the worst-case token runs the full chain per keystroke, plus
  `TokenRepair.trySplit` per keystroke in `updateComposing` (§47 preview). D-138/D-153 precedent, typing-
  forward analogue. Proposed direction (needs design Go): debounce the expensive empty-candidates tail
  (~150-250ms stability, S-04 precedent); optionally an instrumentation-first round via the existing
  diagnostic channel to confirm millisecond attribution. **Incidental §101 validation**: the log shows a
  truth-read resolving `anchor=143` while stale callbacks still reported 137-142 - reads see reality while
  callbacks lag, §101's exact premise, observed live. 713 unit tests (unchanged).
- **§101 DONE (v0.8.66): D-139 second half - truth verification replaces positional equality in
  `onUpdateSelection`.** §100 device-tested negative (still reproduced); per the project's own rule the
  diagnosis was re-questioned from two fresh device logs, not patched. Both logs confirm §100's local
  anchor tracking works exactly as built - but the equality check consuming it was still timing-naive:
  **log 1** shows a commit's lagging callback echoes arriving after the next token already composes
  (reported positions describe superseded states); **log 2** shows a genuine D-62 mid-word tap whose
  reclaim batch the Gemini search field does *not* coalesce, echoing the `deleteSurroundingText` step's
  transient caret as its own update - which lies *outside* the interval [old, expected], so the AOSP-style
  "belated update" heuristic proposed after log 1 alone would NOT have covered it (waiting for log 2 before
  designing prevented a second insufficient fix). Fix: a mismatching callback is now verified against
  ground truth before reacting - synchronous `InputConnection` reads are answered only after all
  previously-sent mutations applied, so `getExtractedText()` sees reality regardless of queue lag; reality
  at the expected caret = stale echo of our own edit (ignore), reality elsewhere = genuinely external
  (reset exactly as before). New pure `SelectionTruth.isAtExpectedCaret()` shared by both stages (reported
  positions first, read-free common case; ground truth only on mismatch). Trade-offs accepted: one extra
  IPC read only for mismatching callbacks; null-`getExtractedText` editors fall into §100's conservative
  do-nothing branch. `CallbackBurstGuard` and `AdaptKeyJitter` diagnostics stay until device-confirmed;
  EXTERNAL wipes now log expected+actual so a further negative round is attributable. 7 new tests
  (`SelectionTruthTest`, both observed mismatch shapes as named cases). 713 unit tests (was 706; +7).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Not yet device-confirmed - D-139 stays open until
  the user's repro attempts hold up on device.**
- **§100 (v0.8.65): D-139 first half - `composingAnchor` now resolved locally for every composing
  token, not only the D-62 mid-word case.** User pushback on §99's diagnosis was correctly on-target: they
  suspected the mid-word-correction machinery, and the logs confirm it as the **amplifier** (§58's
  `reclaimWordAtCaret()` turns one transient misdetection into an unbounded loop) but not the **trigger**
  (`composingAnchor == -1` in every affected log line - always ordinary end-of-word typing, matching the
  user's own recollection that it wasn't a deliberate mid-word correction). User explicitly asked for the
  thorough fix over tuning §58 alone, reasoning that a half-fix would surface later as unrelated-looking
  phantoms (words silently skipping autocorrect/capitalisation/learning). Fix: `reclaimSurroundingWord()`
  now resolves `composingAnchor` via the existing, already-tested `ComposingAnchor.resolve()` (D-87)
  unconditionally - for a brand-new word, a before-only reclaim, or a genuine mid-word reclaim alike - not
  only when a mid-word "after" fragment exists. `onUpdateSelection()`'s `ownEdit` check no longer references
  `candidatesEnd` (the target editor's own, remotely-reported composing span) at all; when the anchor is
  genuinely unresolved (`getExtractedText()` failure, rare) the check now deliberately does nothing rather
  than guess and wipe. Free side-effect: `splitComposingAtCaretAndCommit()`'s (D-119/D-120/D-122) own
  arithmetic anchor propagation for its re-seeded "after" half was silently gated on the same
  now-almost-always-true `beforeAnchor >= 0` condition - fixed with no code change there at all. 1 new test
  (`ComposingAnchorTest`, the `reclaimedBeforeLength = 0` boundary). 706 unit tests (was 705; +1).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Not yet device-confirmed** - D-139 has had multiple
  "fixed but not actually fixed" rounds before (§76, §95/D-149), so stays open pending a real-device repro
  attempt, even though this round is a traced, code-verified root-cause fix rather than a guess.
- **§99 (v0.8.64): D-139 root cause found from three real device logs the user captured and traced against
  the actual code - not yet fixed; D-158 follow-up (email-only `.net` TLD) and a diagnostic-log description
  cleanup implemented.** **D-139**: `onUpdateSelection()`'s `ownEdit` check
  (`AdaptKeyService.kt:782-784`) derives the expected cursor purely locally only for the D-62 mid-word case;
  for the ordinary end-of-word case (`composingAnchor == -1`, the overwhelming majority of every keystroke)
  it falls back to `candidatesEnd`, a value the *target editor* reports back, not anything AdaptKey tracks
  itself - Android does not guarantee this is synchronised with the selection-update stream. When a callback
  right after the service's own `setComposingText()` reports `candidatesEnd = -1` (a real, observed race),
  the edit is misjudged as external, wiping the just-typed token via `finishComposingText()`+`clearComposing()`;
  because `composing` is then empty, the very next callback calls `reclaimWordAtCaret()`, which re-derives it
  via *another* `setComposingText()` - subject to the identical race, so it fails identically every retry,
  producing an unbounded self-perpetuating loop (~5-10 iterations/second in the captured logs, well under
  `CallbackBurstGuard`'s >40/200ms trip threshold, so §76's circuit breaker never engages) that only ends on
  a focus change. Reproduced in at least Google Keep and Signal - not app-specific. A fix direction was
  proposed (extend the mid-word path's purely-local cursor tracking to the ordinary case too, instead of
  ever trusting the remote-reported `candidatesEnd`) but **deliberately not implemented** - a foundational
  change to the composing pipeline's self-recognition mechanism, left for the user's own decision on
  direction per this project's established convention for non-trivial design changes. **D-158 follow-up**:
  the email keyboard's period-key popup gained a fourth, email-only `.net` entry directly before `.org` -
  confirmed with the user this must NOT also apply to URL mode - via new `UrlLocale.emailPeriodAlternatives()`
  (URL mode's own `periodAlternatives()` is untouched); `AdaptKeyboardView.preSelectedIndexFor()`'s ccTLD
  lookup now also excludes `.net`, or a ccTLD-less locale's email keyboard would wrongly pre-select `.net`
  instead of falling back to `.com`. **Diagnostic-log description**: `d_diag_enabled_summary` (all three
  locales) no longer names the D-139 jitter specifically (generic "rare glitches" instead - naming a still-
  open bug in a Settings description was premature) and the password-field exclusion is now its own short,
  separate sentence instead of the last clause of one long paragraph. 5 new/changed tests (`UrlLocaleTest`
  +2, `KeyboardLayoutTest`: 1 replaced + 1 added). 705 unit tests (was 702; +3). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. Not yet device-confirmed; D-139 stays open pending the user's decision.
- **§98 DONE (v0.8.63): D-159 - robust (Huber-style) downweighting for the T-03 touch-zone learning model,
  discussed and designed with the user before implementation.** Each recorded tap is now weighted by how
  far it falls from the key's *currently learned* expected strike point relative to its learned spread
  (`OffsetModel.weightFor()`, the same Gaussian shape `logLikelihood()` already scores candidates with) -
  a wild outlier contributes less to the running mean, floored at `MIN_SAMPLE_WEIGHT = 0.1` so a
  *persistent* run of similarly-off taps still pulls the mean there over time, just more gradually. User's
  own follow-up correctly identified that no separate "not enough data" gate was needed for this (a
  completed K-01 calibration already seeds every key with `count = 25`, well above `MIN_VARIANCE_SAMPLES`)
  - the weighting simply reuses `variance()`'s own existing seeded/geometric fallback. `record()` now
  returns the weight it applied; `unrecord()` (D-140) takes it back as a required parameter and performs
  the exact weighted-Welford inverse - required real plumbing through `TapPoint` (new `weight` field),
  `AdaptKeyboardView.OnKeyListener`/`AdaptKeyService.handleKey()` (new `weight` parameter), and
  `RawCorrectionUndo` (new `weight` field), not just an `OffsetModel`-internal change. `OffsetStore`
  persists the new `weightSum` field with a backward-compatible migration default; `PatternSeed` needed no
  code change at all (its named-argument `Stat(count = SEED_COUNT, ...)` already picks up the correct
  default). **Honest caveat documented**: `merge()`'s old "sequential recording equals split-then-merged"
  equivalence no longer holds in general (an inherent, expected consequence of per-sample state-dependent
  weighting, not a bug) - `combine()` still performs a correct weighted parallel merge of two already-
  computed `Stat`s, just not a reproduction of what one continuous stream would have produced; in practice
  `merge()` is only ever used to fold a calibration seed into the persisted model, never a literal tap-
  stream split, so nothing relied on the old property. `OffsetModelTest.kt` reworked - two tests that
  asserted exact hand-computed unweighted arithmetic were replaced with property-based tests demonstrating
  the actual intended behaviour (single-sample exactness, outlier discounting, sustained-drift-still-wins,
  weighted-merge-formula correctness). 702 unit tests (was 698; +4). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. Not yet device-tested - `MIN_SAMPLE_WEIGHT`/`DEFAULT_SIGMA_FACTOR` are
  starting points, easy to retune later.
- **§97 DONE (v0.8.62): D-158 - dedicated email-mode keyboard layout, its own round per explicit deferral.**
  New `KeyboardLayout.emailBottomRow()` (shared by `KeyboardLayout`/`GreekLayout`, mirrors D-143's
  `urlBottomRow` precisely): `[?123] [@] [-] [space, narrow] [.] [⏎]` - `@` takes over the comma key's
  primary slot (comma demoted into `EMAIL_AT_ALTERNATIVES = COMMA_ALTERNATIVES`, no new list needed, same
  trick as D-143's `/`); a new dash key (`-`, `_` as its single D-01 secondary) is funded entirely from the
  space key's own width (`EMAIL_DASH_KEY_WEIGHT` + `EMAIL_SPACE_WEIGHT` sum to exactly the ordinary
  `spaceWeight`, so the row's total width is unchanged, unlike URL mode's row-grows-overall approach); the
  period key reuses `UrlLocale.periodAlternatives()` byte-for-byte, same as URL mode. Detection via new
  `isEmailField()` (mirrors `isUrlField`'s direct-`InputType` check, not derived from `loginFieldKind` -
  needed pushed to the view before `setSurface()` runs, same ordering constraint `urlMode` already has). New
  `AdaptKeyboardView.emailMode`/`KeyGesture` `emailMode` param, both mirroring `urlMode` exactly. Needed
  **no** autocorrect/suggestion-suppression code at all - a reliably-detected EMAIL field already fully
  short-circuits through the pre-existing D-142 credential pipeline, which also means the domain-completion
  suggestion bar already works for free. 8 new tests (`KeyboardLayoutTest` 6, `GreekLayoutTest` 1,
  `KeyGestureTest` 1, all mirroring the existing D-143 URL-mode cases one-for-one). 698 unit tests (was 690;
  +8). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-tested - the new key weights are a
  considered starting guess, easy to retune (§36/§37/§53/§89 precedent).
- **§96 DONE (v0.8.61): §95's whole backlog cleared - D-150 through D-157, one requested round.** D-158
  (email-mode keyboard) deliberately deferred to its own round. **D-150**: diagnostic log window 5min→1min;
  `AdaptKeyService.diag()` now unconditionally skips both logcat and the in-app ring buffer while the focused
  field is a password (one choke point, not per-call-site checks); the password exclusion is now also spelled
  out in `d_diag_enabled_summary` (all three locales), per explicit follow-up request. **D-151**: the log
  viewer's Share/Copy/Clear row was the one settings screen missing the `CalibrationActivity`/`TouchModelActivity`
  edge-to-edge window-insets pattern - fixed identically. **D-152 (regression)**: traced to a real gap in
  §95's own D-149 fix - `selectionCollapsed` wasn't reset on `onStartInput()`, so it could stay stale from the
  *previous* field and let the same §41-misfire class of bug through for a freshly focused field's first word;
  now reset alongside every other per-field state. **D-153 (regression)**: root-caused to two uncached
  per-keystroke dictionary lookups D-138 never actually covered - `updateComposing()`'s own colour-preview
  calls, and `refreshSuggestions()`'s `suggestionsFor()` (grew heavier since D-138 via D-116/D-147) - both now
  skip during a backspace-repeat tick via the existing `duringRepeat` flag. **D-154/D-155**: `uber`/`fur`
  weren't autocorrected to `über`/`für` - confirmed via the bundled dictionaries that `fur` is a real English
  word (blocking it via D-106 stage 2) and `uber` is in neither dictionary (plausibly caught by the A-03
  foreign-language classifier instead); `diacriticRestoration()` itself was never broken (new test confirms
  it). Fixed by giving diacritic restoration its own gate, independent of both suppressors, restricted to
  German mode - and it now wins as the actual correction, not just an A-05 split-veto as before. **D-156**: a
  live D-24 touch-zone-visualisation toggle added to the settings row (🎯, next to clear-clipboard) - the
  overlay previously only ever worked on a separate, non-live preview keyboard. **D-157**: `due` no longer
  blocked from autocorrecting to `die` - root-caused to D-106 stage 2's cross-language protection gating off
  the call to `autocorrectFor()` before §44/D-113's already-correct German-dictionary override logic (already
  tested, already returns "die") ever got a chance to run; fixed with the confirmed-recommended small
  `CROSS_LANGUAGE_CONFUSABLES` exception set, not a new commit path. 1 new test
  (`DictionarySuggestionProviderTest`, uber/fur). 690 unit tests (was 689; +1).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. None of this round confirmed on device yet.
- **§95 DONE (v0.8.60): D-149 - a real bug found and fixed from a precise device log the user captured
  themselves.** A D-62 mid-word reclaim's Backspace (e.g. tapping into `diecwird` right after the `c`, then
  Backspace) could wipe the *entire* reclaimed word via `handleBackspace()`'s §41 selection-delete branch,
  instead of removing the single intended character - traced to that branch trusting a bare
  `InputConnection.getSelectedText(0)` call with no cross-check, which returned non-empty on the device tested
  (Google Keep) even though the IME's own `onUpdateSelection` had just confirmed a collapsed caret. Fixed with
  a new `selectionCollapsed` flag (set from `onUpdateSelection`, the authoritative source) gating that branch,
  so a real drag-selection still works exactly as before while this false-positive case no longer fires.
  Directly matches the "shaking" symptom the user reported as part of D-139 and is a plausible contributor to
  it, though D-139 itself is not declared closed - it has had multiple root causes traced over this project's
  history and this is one concrete mechanism, not a claim that every remaining report is now explained. Nine
  new backlog items captured (D-150…D-158, see spec §95) - diagnostic log window too long, log-viewer controls
  under the notch, two regressions (first-word delete, backspace-hold jerkiness), `uber`/`fur` umlaut-
  autocorrect gaps, a touch-zone-visualisation toggle, a `due`-typo blacklist proposal (with a recommended
  design direction, not yet built), and an email-mode keyboard layout - none implemented this round. No new
  unit tests (Android `InputConnection` service glue, the established gap for this class). 689 unit tests
  (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet re-confirmed on device.
- **§95 addendum (still v0.8.60, no code change): D-139 typing-speed hint recorded; D-150 amended with a
  password-field logging requirement; D-157 confirmed matching the user's intent exactly.** The user reports
  the jitter seems to occur more often when typing faster - noted for the next repro, not investigated yet.
  D-150 (the 5-minute diagnostic log window) now explicitly requires that logging **never** happen while
  typing into a password field, regardless of the `enabled` setting - not implemented, needs a field-type
  guard (likely reusing `LoginFieldDetector`'s password check). D-157's recommended design (a small
  `knownInOtherLanguage()` exception list, not a new blacklist mechanism) was confirmed by the user as exactly
  what they meant. 689 unit tests (unchanged).
- **§95 addendum 2 (still v0.8.60, no code change): D-150 further amended** - the password exclusion must also
  be spelled out in the Settings UI text (`d_diag_enabled_summary`), not just hold true in code, so someone
  other than the user can enable the toggle without reading the source. Not implemented yet.
- **§94 (still v0.8.59, no code change): D-110, D-141, D-129 CONFIRMED WORKING on device** - §86's
  `ShiftGrace.autoArmAtWordStart()` fix, §81's `TimePattern` trailing-whitespace fix, and §83's
  `SIGN_FLIP_GLYPH` corner hint (third pass) are all closed. 689 unit tests (unchanged).
- **D-144 and D-145 CONFIRMED WORKING on device** (user confirmation after §93, no code change) - swipe-down
  now reliably dismisses/closes from anywhere, including empty bar/row background; the URL-mode period
  popup's centred entry is correctly the locale-specific TLD.
- **§93 DONE (v0.8.59): both §92 fixes were still wrong, both re-traced from scratch and actually fixed.**
  **D-144**: `onTouchEvent()` claiming `ACTION_DOWN` (§92's fix) wasn't enough - re-traced against Android's
  own `ViewGroup.dispatchTouchEvent()` contract and found the real mechanism: `onInterceptTouchEvent()` is
  only re-consulted for events *after* `ACTION_DOWN` when some child already claimed the gesture
  (`mFirstTouchTarget != null`); on empty bar/row background nothing claims it, so `onInterceptTouchEvent()`
  is *never called again* for that gesture, and the swipe-direction detection living inside it (correct as
  it was) is structurally unreachable there. Fixed by duplicating that same detection directly into
  `onTouchEvent()`'s own fallback branch (reached only when nothing else claimed the gesture) in both
  `SuggestionBarView` and `SettingsRowView` - `ACTION_MOVE` there now runs the identical `SwipeGesture`
  check and sets the shared `swipeDownIntercepting` flag, after which the existing (already-correct)
  confirm-on-release branch picks it up exactly as it already did for the chip/button-claimed case.
  **D-145**: "the locale entry must be in the middle" meant the *pre-selected, centred-over-the-stem*
  position (this key's own original `! . ?` convention - `.` at the true middle, not leading), not "index
  0" as §91 implemented - device feedback confirmed `.de, .com, .org` when `.com, .de, .org` was wanted.
  Fixed at both ends: `UrlLocale.periodAlternatives()` now returns the ccTLD in the *middle* of the list;
  `AdaptKeyboardView.preSelectedIndexFor()` gained a small, structurally-scoped special case for this one
  key (identifies the ccTLD entry as "neither `.com` nor `.org`", not by a hardcoded index, so it stays
  correct regardless of list shape) so it is what actually gets pre-selected instead of the generic
  index-0 fallback. 689 unit tests (unchanged - existing tests updated, no new ones needed).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet re-confirmed on device.
- **§92 DONE (v0.8.58): confirmations + two follow-up fixes.** D-146 (double space) and the single-word-plus-
  space backspace case **confirmed working**; D-148 **withdrawn** (user can no longer reproduce either
  backspace-after-suggestion report, will re-open with a fresh repro if it recurs - nothing was implemented).
  **D-145 corrected**: the D-01 popup centres its *pre-selected* (index-0) cell directly over the stem key -
  the locale's own ccTLD must be that entry for every locale, not only non-English ones, since it's the one
  reachable with zero finger movement. `UrlLocale.periodAlternatives()` no longer branches on English vs.
  other - the ccTLD always leads when one resolves (e.g. `en_GB` now leads with `.co.uk`, not `.com`),
  falling back to `.com` only when there's no idiomatic ccTLD at all (`en_US`). **D-144 follow-up fixed**:
  swipe-down never registered when starting on empty bar/row background (only worked starting on a chip/
  button) - root cause, found from a precise repro: a plain, non-clickable `View`/`ViewGroup`'s default
  `onTouchEvent()` declines an `ACTION_DOWN` no child claimed, and once a gesture's first event goes
  unclaimed by anything in a subtree, later events never reach that subtree's `onInterceptTouchEvent()`
  again - a chip/button claims `ACTION_DOWN` independently (clickable), which is why those worked. Fixed in
  both `SuggestionBarView` and `SettingsRowView`: `onTouchEvent()` now claims `ACTION_DOWN` unconditionally
  as its own fallback, reached only when no clickable descendant already claimed it - chip/button taps are
  unaffected. No new tests (Android touch-dispatch glue). 689 unit tests (was 688; +1 `UrlLocaleTest`).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet re-confirmed on device.
- **§91 DONE (v0.8.57): device-feedback batch on §89/§90 - D-145/D-146/D-147 fixed, D-144's settings-row half
  still open, D-148 captured pending clarification.** **D-145**: two URL-mode popup corrections - `/`'s and
  `.`'s own popups no longer redundantly list themselves (`URL_SLASH_ALTERNATIVES` is now exactly
  `COMMA_ALTERNATIVES`, `UrlLocale.periodAlternatives()` drops the bare `.`); popup cells now size to their
  own text content (`AdaptKeyboardView.popupCellWidthFor()`, `HorizontalLongPressPopup` generalised from a
  single `cellWidth` to `cellWidths: List<Float>`) instead of a uniform 40dp, fixing both the URL keys and
  (for free) the calculator page's `sin`/`cos`/`tan`/`log` popup. **D-146**: applying a suggestion mid-text no
  longer leaves a double space - `onSuggestionClicked()` now checks `getTextAfterCursor(1, 0)` before adding
  its own trailing space. **D-147**: suggestions now reach a dictionary word even when its umlaut/ß was
  skipped while typing (`tatsachl` → `tatsächlich`) - new `Umlaut.unfoldCandidates()` (the reverse of
  `Umlaut.fold()`) tried as extra prefix queries in `DictionarySuggestionProvider.suggestionsFor()`; confirmed
  backed by the original spec's own founding "umlauts are ordinary characters" principle, which explicitly
  names suggestions as a feature it must shape - re-checked per direct instruction, not assumed. **D-144
  follow-up**: settings-row swipe-down still doesn't react on device; re-verified the code is structurally
  identical to the suggestion bar's (confirmed-working) fix - no bug found, needs a sharper repro. **D-148**:
  captured, not implemented - two Backspace-after-suggestion reports traced against the actual A-07/bar-tap
  code (bar-tap already clears A-07 undo state, so a plain Backspace should already just remove the trailing
  space; A-07 itself deliberately restores the typed word after a *silent* autocorrect, by long-standing
  design) - genuinely unclear whether this needs a sharper repro or an actual, confirmed change to A-07's own
  behaviour; left undesigned pending the user's own read, matching the D-107 precedent for a foundational
  behaviour change. 9 new tests (`UmlautTest` 7, `DictionarySuggestionProviderTest` 2) plus updated
  `HorizontalLongPressPopupTest`/`KeyboardLayoutTest`/`UrlLocaleTest`. 688 unit tests (was 675).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. D-145/D-146/D-147 not yet re-confirmed on device.
- **§90 DONE (v0.8.56): D-144 fixed - swipe-down-to-dismiss now also reacts on the suggestion bar and the
  settings row, not only the keyboard body.** Root-caused first: `AdaptKeyboardView.resolveSwipe()` was the
  *only* place in the app that ever recognised a swipe at all - `SuggestionBarView` only intercepted G-04's
  upward drag-to-trash, `SettingsRowView` had no touch handling beyond its buttons' plain clicks - so a swipe
  starting on either sibling view never reached the keyboard body's handler (no Android mechanism forwards it
  there). Fixed by giving both views the same two-stage claim-early ([gesture.SwipeGesture], small
  touch-slop)/confirm-late (110dp, matching `fieldSwipeThresholdPx`) interception `AdaptKeyboardView`/G-04
  already used - no new pure logic, both reuse the already-tested `SwipeGesture`/`SwipeDirection` classifier
  directly. `AdaptKeyService.handleSwipe()`'s `DISMISS_KEYBOARD` branch extracted into one shared
  `dismissKeyboardOrCloseSettingsRow()`, now called from all three sources (keyboard body, new
  `SuggestionBarView.onSwipeDown`, new `SettingsRowView.onSwipeDown`) instead of being duplicated.
  **Investigated, not reproduced**: the report's own "(exkl. Space)" aside - traced `KeyGesture.resolve()`'s
  unconditional `DOWN -> DISMISS_KEYBOARD` check and `resolveSwipe()`'s threshold math, both keyCode-agnostic
  for a vertical swipe; found no code path that treats the space bar any differently. Reported back honestly
  as "traced, not found" rather than guessed at - needs its own repro if it persists (D-118 precedent for this
  kind of outcome). No new tests - Android touch-dispatch glue over the already-pure/tested classifier, same
  class of gap as D-64's own (never separately unit-tested) drag-to-trash interception. 675 unit tests
  (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
- **§89 DONE (v0.8.55): D-143 implemented - a dedicated URL-mode bottom row, planned with the user before
  implementation.** Auto-opens (not part of the D-19 swipe cycle) for `TYPE_TEXT_VARIATION_URI` fields, on
  the letters surface (not a new page - a domain/path still needs the full alphabet). New shared
  `KeyboardLayout.urlBottomRow()` (also used by `GreekLayout` - URL entry doesn't depend on the active
  typing alphabet): `[?123] [https://] [www.] [/] [space, narrow] [.] [⏎]`. `/` takes over the comma key's
  primary slot (comma demoted to its own popup, unchanged otherwise); new `https://`/`www.` `KeyCode.TEXT`
  keys (§53 precedent) funded by shrinking the now-barely-needed space key; the period key's popup becomes a
  locale-resolved TLD list (new `UrlLocale`, mirrors `CalculatorLocale`'s JDK-locale-driven approach, plus a
  2-entry `CCTLD_OVERRIDES` map for GB (`.co.uk`) and US (no idiomatic ccTLD)) instead of `! . ?`.
  Autocorrect/suggestions/capitalisation suppressed entirely by widening D-142's existing login-field
  short-circuit in `finalizeAndCommit()` to also cover `urlMode` (renamed `commitLoginFieldFragment()` →
  `commitVerbatimFieldFragment()`, since the function itself was already feature-agnostic); a new, simpler
  bypass in `refreshSuggestions()` keeps the bar empty (no credential-style list of its own). G-01's
  language swipe on the (narrow) URL-mode space key falls through to the ordinary D-19 surface swipe instead,
  extending `KeyGesture`'s existing D-92 surface check. **Real bug found and fixed while verifying the new
  `TYPE_TEXT_VARIATION_URI` check against the real SDK (`javap`, not guessed)**: `LoginFieldDetector`'s five
  variation constants had the `TYPE_CLASS_TEXT` bit wrongly baked in, so `classify()` could never match a
  real field's masked `inputType` and silently returned `NONE` for every field - plausibly the real
  explanation for §85's `finanzen.net zero` report, not only the documented "no username variation exists"
  limitation. Fixed (5 constants + the test literals that encoded the same mistake) - not yet device-
  confirmed whether this alone resolves §85, since the weak-signal keyword fallback there still applies
  regardless. 15 new unit tests (`UrlLocaleTest`, `KeyboardLayoutTest`, `GreekLayoutTest`, `KeyGestureTest`).
  675 unit tests (was 660; +15). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-tested -
  the new key weights are a considered starting guess, easy to retune (§36/§37/§53 precedent).
- **§88 DONE (v0.8.54): D-139/D-110 - in-app diagnostic log replaces the need for `adb logcat`.** Requested
  directly - §87's logcat approach needs the phone tethered to a PC, impractical since D-139 happens during
  normal daily use, not a desk-testing session. New `diagnostics/` package: `DiagnosticRingBuffer` (pure, 7
  tests) is a time-windowed FIFO pruned relative to its own newest entry; `DiagnosticLog` wraps it as an
  Android singleton with a real monotonic clock and a settings-driven `enabled` flag (off by default, clears
  on disable). New "Diagnostics" settings category: a toggle (`d_diag_enabled`, full
  RawSettings/AdaptSettings/SettingsMapper/SettingsStore pipeline) + a `DiagnosticLogActivity` viewer
  (refreshes on every `onResume()`, entries shown with relative age, `Share`/`Copy`/`Clear` actions - Share
  mirrors the existing D-09 `ACTION_SEND` precedent, no storage permission). §87's seven `Log.d`/`Log.w` call
  sites now route through one new `AdaptKeyService.diag()` helper into both logcat and the in-app log. 8 new
  unit tests. 660 unit tests (was 652; +8). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-confirmed - next jitter occurrence should be catchable via Settings → Diagnostics, no PC needed.
- **§87 DONE (v0.8.53): D-139 - diagnostic logging added while typing.** Requested directly - the reported
  text-jitter/character-scramble happens fairly often for the user but isn't immediately reproducible with
  the same text, matching the suspected timing-dependent `onUpdateSelection()` cascade from §73/§76. New
  `AdaptKeyJitter` log tag (separate from §86's `AdaptKey` tag) logs every `onUpdateSelection()` call (old/new
  selection, composing state, ownEdit result, and the exact composing string right before a wipe), every
  `CallbackBurstGuard` trip (now `Log.w`, previously silent), every actual `reclaimSurroundingWord()` splice,
  every `updateComposing()` push (the literal string sent to the field), and `finalizeAndCommit()`'s
  typed-vs-committed pair - `adb logcat -s AdaptKeyJitter:D` while typing to finally catch it in the act. Not
  gated behind any flag (logcat-only, never transmitted, same bar as D-09's own now-removed diagnostic) -
  temporary, to be removed once D-139 is closed. No new tests (pure logging). 652 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green.
- **§86 DONE (v0.8.52): D-110 picked up - a real bug found and fixed, honestly not claimed as the full
  explanation.** Both candidate causes named in D-110's own original write-up were ruled out by direct code
  check (no `NO_SUGGESTIONS`/filter-variant handling exists anywhere; `CapitalisationEngine` already consults
  `sentenceStart` independently of `capsMode`). Found instead: `ShiftGrace.autoArmAtWordStart()` silently
  never armed Shift at a sentence start for `CapsMode.NONE` (a field declaring no caps flag at all - a
  plausible shape for a plain eBay Kleinanzeigen message field) - even `CapsMode.SENTENCES` did. Contradicted
  the project's own stated design and was even encoded as *intended* in an existing test. Fixed by folding
  `NONE` into the same branch as `SENTENCES`. **Caveat stated plainly**: tracing `CapitalisationEngine`'s
  commit-time logic suggests the final committed word should already have been correct regardless of this
  bug - so this confirms/fixes the live-typed-character and Shift-key visual symptom, but is not proven to be
  the *whole* explanation for "the committed word never capitalises." A temporary `Log.d("AdaptKey", ...)` in
  `onStartInput()` now logs the real `EditorInfo` for every focused field (`adb logcat -s AdaptKey:D`) to
  finally get the data point the original write-up said was needed - to be removed once D-110 is closed for
  good. 1 existing `ShiftGraceTest` case corrected (was asserting the bug as intended). 652 unit tests
  (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed either way.
- **§85 DONE (v0.8.51): D-142 follow-up - weak-signal detection now also covers email, not just username.**
  Reported: a `finanzen.net zero` field labelled "E-Mail-Adresse" didn't activate credential mode at all.
  Real gap found in code (not guessed): `LoginFieldDetector`'s weak-signal fallback only ever checked
  username keywords, never email ones - so a purely "E-Mail-Adresse"-labelled field could never have matched
  it regardless of device behaviour. `hasWeakUsernameSignal(): Boolean` -> `weakSignalKind(): LoginFieldKind`,
  now checks a new `EMAIL_KEYWORDS` list first. New `credentialModeManuallyActivated` flag lets the settings-
  row toggle activate the *specific* kind the weak signal suggested (so a nudged email field gets real domain
  completion, not a generic username fallback) while still only letting a manually-activated mode be toggled
  back off. **Not claimed as a confirmed fix** - can't tell from here whether `InputType` detection itself is
  also failing for this app; reported back for another device round. 3 new/expanded
  `LoginFieldDetectorTest` cases. 652 unit tests (was 649; +3). `:app:assembleDebug`/`:app:testDebugUnitTest`
  green.
- **§84 DONE (v0.8.50): §83's clipboard+trash badge dropped its contrast pill background** per device
  feedback - the badge reads clearly on its own. One-line change in `SettingsRowView.badgedButtonFor()`. No
  new tests. 649 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
- **§83 DONE (v0.8.49): two icon fixes, reported while D-142 was being device-tested.** **D-129 (third
  pass)**: the calculator minus key's sign-flip corner hint reused D-98's generic `MORE_ALTERNATIVES_GLYPH`
  ("◢", "a popup awaits") - misleading, since no popup ever appears for this key. New `SIGN_FLIP_GLYPH = "±"`
  (a real plus/minus ligature) replaces it. **D-70/§70 follow-up**: the settings row's clear-clipboard button
  (bare 🗑) gave no clue what it clears - new `SettingsRowView.badgedButtonFor()` renders the clipboard glyph
  📋 with a small 🗑 badge overlaid bottom-right (its own pill background), the practical equivalent of the
  requested "ligature" (no true combined-glyph exists for this emoji pair). Both are private View/drawing
  code, no new tests - the established gap for this layer. 649 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
- **§82 DONE (v0.8.48): D-142 - login-field credential learning & suggestions, full round.** New `credential/`
  package: `LoginFieldKind`/`LoginFieldDetector`/`CredentialEntry`/`CredentialRanking` (pure) +
  `CredentialStore` (SharedPreferences/JSON, own private file, entirely separate from the ordinary
  dictionary). **Real constraint found mid-implementation, surfaced to the user rather than guessed around**:
  `EditorInfo` has no `autofillHints` field at all (confirmed via `javap` against the real `android-35`
  `android.jar`) - so `InputType`'s variation bits are the only reliable field-level signal, and they have no
  variation for "username" at all. **Agreed compromise**: EMAIL fields are fully automatic (reliable
  `InputType` detection); PASSWORD fields are reliably detected and always excluded (never learned, never
  suggested into); USERNAME has no reliable signal, so it is reached only via a new weak
  `hintText`/`fieldName`-keyword signal (nudges - auto-opens the settings row and flashes a new 4th button,
  leftmost, ahead of emoji - never silently switches behaviour) or the same button's manual toggle for a
  field the detection missed entirely. Learning is immediate (no D-37 threshold), captured from
  `handleEnter()` and `onFinishInput()` by reading the field's real committed text (not `composing`, since
  every `.`/`@`/`-`/`_` is its own delimiter under the ordinary token model) - a new
  `commitLoginFieldFragment()` short-circuits `finalizeAndCommit()` for every login-relevant field so no
  fragment is ever autocorrected/capitalised/learned into the ordinary dictionary. Suggestions replace the
  entire ordinary pipeline (new `SuggestionController.Kind.CREDENTIAL`, mirrors the D-36 clipboard-chip
  precedent) - the credential list for USERNAME/EMAIL, nothing at all for PASSWORD, and the user's own
  most-used email domains (summed frequency across aliases) once `@` is typed in an EMAIL field. A new
  single-confirm settings action clears the whole store. 27 new tests (`LoginFieldDetectorTest`,
  `CredentialRankingTest`, `CredentialStoreRoboTest`). 649 unit tests (was 622; +27).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - real login forms are
  exactly what this needs to be judged against.
- **§81 DONE (v0.8.47): D-140 - rejected autocorrect/split commits are now un-learned, not just visually
  reverted; D-141 - "Uhr" suggestion no longer appears/applies before the trailing space.** **D-140**: new
  `DictionaryStore.unlearn()` (symmetric inverse of `learn()`, removes an entry once its count hits zero) +
  `learnWord()` now returns a `LearnOutcome` captured into the A-07 undo state, so `performAutocorrectUndo()`
  precisely reverses whatever dictionary frequency/bigram reinforcement the rejected commit persisted (not
  just the old, incomplete `PendingLearnStore.decrement` call, which never touched the common case of an
  already-known correction target). Separately, new `OffsetModel.unrecord()` (exact Welford inverse) reverses
  T-03 touch-model training, but **only** for the D-39 raw-coordinate-correction path specifically (found via
  a new `rawCorrectionUndoFor()`, capturing the single substituted tap before `clearComposing()` wipes it) -
  deliberately **not** for ordinary dictionary/diacritic/split corrections, since those are purely linguistic
  decisions with no bearing on which key was actually touched, and blanket-unlearning them would discard good
  training data. **D-141**: `TimePattern`'s regex now requires at least one trailing whitespace character
  (`\s+$`, was `\s*$`), so the format is not even checked until the delimiting space after the time has
  actually been typed - fixes both the premature suggestion and the missing-space-on-apply bug in one change.
  Also: §3's T-03/T-04 spec sections swapped in reading order (T-04 now precedes T-03, matching that
  `PatternSeed` seeds T-03 from the T-04 pattern choice) - documentation only, IDs unchanged. 622 unit tests
  (was 612; +10: 4 `OffsetModelTest`, 3 `InMemoryDictionaryStoreTest`, 2 `SqliteDictionaryStoreRoboTest`, 1
  `TimePatternTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
- **§80 (still v0.8.46, no code change): two new backlog items captured, not started** - D-142 (credential-
  field learning: immediate save for username/email input, its own separate suggestion list exclusive to
  login-type fields, live frequency-ranked email-domain completion after `@`) and D-143 (a dedicated URL-entry
  keyboard layout, explicitly capture-only per the user's own instruction). See spec §80 for the full write-up.
- **§79 (still v0.8.46, no code change): D-135 struck from the backlog** per explicit user instruction - no
  longer tracked as an open item; the already-shipped §67 Autofill implementation and §74's negative
  device-test writeup are otherwise untouched. 612 unit tests (unchanged).
- **§78 DONE (v0.8.46): app version shown in Settings; D-128 relocated and rewritten.** New static
  `info_version` preference in the settings Info category, summary read live at runtime from
  `PackageManager.getPackageInfo(...).versionName` (not a hand-maintained string, which would drift from
  `app/build.gradle.kts`). **D-128**: two direct corrections. Placement - the §75 callout lived inside
  `FeatureOverviewActivity`, requiring a tap to reach; moved onto the existing `d89_feature_overview`
  preference's own `summary`, visible directly on the settings main screen with no navigation (removed the
  now-unused callout layout block and `d128_title`/`d128_summary` strings). Content - the §75 text described
  generic dictionary-learning/LLM-fade-out, not distinctive from other keyboards; rewritten to name what's
  actually unique: raw per-character touch coordinates are recorded (T-02), each key's hit zone is
  continuously reshaped from that data (T-03), seeded from the typing style chosen at setup, not a blank
  slate (K-01/T-04 `PatternSeed`). No new tests. 612 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.
- **§77 DONE (v0.8.45): D-132 and D-137 follow-up fixes - neither §75 fix actually worked, both re-traced
  from scratch.** **D-137**: root cause was that a *standalone* typed digit never reaches
  `showNextWordPredictions()` at all - `finalizeAndCommit()`'s empty-composing branch treats the digit
  itself as the delimiter and returns early via a separate code path, since composing is empty at every
  keystroke of "14:30" (a digit only joins an existing composing token per D-40's mid-word case). New
  `showTimeSuggestion(ic)` is now called from *that* branch instead. **D-132**: root cause was never the
  row's background (which happens to be the exact same colour as the root view's own background, so §75's
  fix was invisible) - it was the row's `layoutParams.height` jumping to full size *instantly*, visibly
  shoving the suggestion bar/keyboard (siblings in the same `LinearLayout`) down before any animation ran.
  `SettingsRowView.animateHeight()` now genuinely animates the height itself
  (`ValueAnimator.ofInt`+`requestLayout()` per frame); `content` switched from `MATCH_PARENT` to a fixed,
  bottom-anchored height so clipping alone produces the "buttons rising into place" reveal, no separate
  `translationY` needed. No new tests (Android view/animation glue). 612 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Neither re-confirmed on device yet.
- **§76 DONE (v0.8.44): D-139 investigated - no root cause confirmed, a defensive circuit breaker added.**
  Still no repro for the reported "text jitters, characters get scrambled" glitch. Traced
  `onUpdateSelection()`/`reclaimWordAtCaret()`/`reclaimSurroundingWord()`/`splitComposingAtCaretAndCommit()`
  end to end for a self-triggering-callback mechanism (this project's own established failure class, per
  §32's D-87): found two concrete, real structural risks (`onUpdateSelection()`'s `composing.isEmpty()`
  branch calls `reclaimWordAtCaret()` with no "is this our own edit" guard at all; the `!ownEdit` branch's own
  `finishComposingText()` call could in principle re-trigger the same callback) but could not prove either
  one actually cascades without being able to observe real callback timing (no emulator/`InputConnection`
  shadow here). Added new pure `CallbackBurstGuard` (5 tests, sliding-window call counter) at the top of
  `onUpdateSelection()`: if it fires >40 times within 200ms, every reactive branch is skipped for that call,
  breaking a possible runaway cascade rather than letting it continue - verified this stays clear of even a
  sustained 45ms-tick backspace-hold (§59). **Explicitly not claimed as a fix** - a structural hardening
  measure for a real risk, kept clearly separate from a confirmed diagnosis. D-139 stays open pending an
  actual repro. 612 unit tests (was 607; +5). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
- **§75 DONE (v0.8.43): batch release - D-117, D-126, D-127/D-128, D-129, D-131, D-132, D-133, D-136, D-137.**
  **D-117**: a wider-budget, suggestion-only fuzzy fallback (`wideFuzzyNeighbours`, cost 4, tokens ≥6 chars)
  for multi-typo words beyond D-28's normal budget ("erkamm"→"erkannt"), tried only once every cheaper search
  found nothing. **D-126**: the uninstall action already existed (`Tier3ModelActivity.removeModel()`, just
  never marked confirmed); added the missing independent enable/disable toggle (`tier3Enabled`, default on,
  full settings pipeline), `reconcileTier3Provider()` now gates on it and reacts live via `applySettings()`.
  **D-127/D-128**: consolidated "what learns when" reference (T-03/D-37/D-13/T-04/§9 tier-3 reinforcement -
  see spec §75 for the full write-up) plus a compact, prominent callout added to the existing D-89
  `FeatureOverviewActivity` summarising it for users. **D-129**: the calculator minus key's sign-flip
  long-press now shows a corner-hint glyph (reuses D-98's generic "◢" cue, keyed off the key's own identity
  rather than hint/alternatives, which would have wrongly rerouted its commit path). **D-131**: D-39's
  raw-coordinate correction is now live while composing too, not just at commit time - refactored suggestion
  assembly so it and D-122's split suggestion both stay out of the tier-1 candidates list `SuggestionMerger`
  normalises against (shared `extras: List<Suggestion>`, appended only at display time). **D-132**: the
  settings row's background moved from the outer view onto its animated `content` child, so the whole reveal
  (background + buttons) now slides as one piece instead of the background popping in at full height first.
  **D-133**: `OffsetModel.Candidate` gained an optional `maxDownwardOffsetFactor`, applied to the bottom
  letter row (`c v b n m`, factor 0.25) on top of D-109's isotropic cap - verified end-to-end via `resolve()`,
  not just that the parameter exists. **D-136**: `WindowInsetsControllerCompat.isAppearanceLightNavigationBars
  = true` (unconditional - AdaptKey's keyboard background has no dark variant) fixes the suspected missing
  system-bar-appearance flag. **D-137**: a typed time always adds "Uhr" as a next-word prediction (new pure
  `TimePattern.endsWithTime()`), same maximal-score-kept-out-of-normalisation approach as D-122, now shared
  via `MAX_PRIORITY_SUGGESTION_SCORE`. 607 unit tests (was 592; +15). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. None of this round's Android/service/view-glue items have been confirmed on
  a real device yet.
- **§74 (still v0.8.42, no code change this entry): device-feedback status update.** **Confirmed working:**
  D-122, the §72 caret-position fix, and (blanket confirmation) D-106 stages 1+2, D-111/D-112, D-113. **D-135
  negative result:** no Autofill inline suggestion appeared for the username field in "finanzen.net zero"
  (a brokerage app). Not traced - no device logs available here, and at least 4 genuinely different causes
  would look identical from the outside (no saved credential yet; the app itself opting the field out of
  autofill, plausible and arguably likely for a finance app's login field specifically; the configured
  autofill service not supporting inline suggestions; an actual AdaptKey-side bug). Deliberately not guessed
  at - see spec §74 for the specific follow-up questions that would narrow it down.
- **§70 clear-clipboard button CONFIRMED WORKING on device** (user confirmation, no code change).
- **§73 CAPTURED (still v0.8.42, no code change this entry): D-139, intermittent text jitter/character**
  **scramble, no repro yet.** Reported as occasional and disruptive (only recoverable by clearing everything),
  first noticed roughly 4-5 builds ago, no precise version pinned. No repro exists yet - user will report back
  once one is found. Recorded honest, clearly-labelled speculation only (no fix attempted, no diagnosis): the
  strongest candidate is an `onUpdateSelection()` feedback loop in the D-62/D-87 mid-word-reclaim machinery
  (self-issued edit not recognised as "own", re-triggering itself) - an area with recent real churn (D-116,
  D-122, §72), though the user's own timeline points earlier than that. See spec §73 for the full write-up and
  what would help most once a repro exists.
- **§72 DONE (v0.8.42): bug fixed - mid-word SPACE left the caret before, not after, the inserted space.**
  Precise repro: type "Testcwort", caret after the `c`, Backspace it, then SPACE - the space is inserted
  correctly but the caret stays before it. Root cause traced to `splitComposingAtCaretAndCommit()` (the
  D-119/D-120 mid-word-split path): the "after" half's new anchor was computed by re-reading
  `InputConnection.getExtractedText()` *after* several prior same-batch `InputConnection` mutations (the
  recursive "before"-half finalise's own `setComposingText`/`finishComposingText`/`commitText`) - the one
  same-batch state read in this class that reads *after* mutating rather than before. Confirmed via the
  repro itself: "Test" doesn't even change length under capitalisation here, ruling out an autocorrect-
  length-mismatch explanation and pointing at the read itself. Fixed by computing the anchor **
  arithmetically** instead: `finalizeAndCommit()` and its delegates (`commitVerbatim`/`applyMerge`/
  `applySplit`) now return the net character count they actually committed, so
  `splitComposingAtCaretAndCommit()` can derive `beforeAnchor + committedLength` with no read-back at all.
  No new tests (Android/`InputConnection` glue, existing gap). 592 unit tests (unchanged).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet re-confirmed on device.
- **§71 DONE (v0.8.41): D-122 implemented - mid-word connector-split suggestion.** First real repro
  ("Testvwort hallo", caret placed inside "Testvwort", expected a "Test Wort" suggestion, bar stayed empty).
  Root cause, two compounding correct-for-their-own-reasons behaviours: `TokenRepair.trySplit()`'s §45
  bigram-co-occurrence gate applies uniformly, so "test"/"wort" (no recorded co-occurrence) never even
  reaches a candidate; and even a found split was never wired into the suggestion bar at all (only the live
  colour preview, itself suppressed during mid-word editing, or the silent A-05 auto-commit). New
  `TokenRepair.splitAtUnresolvedConnector()` (connector-drop only, no bigram gate) is consulted only while
  `isEditingMidWord()` is true (the deliberate-re-edit signal is what makes relaxing the gate safe);
  `midWordConnectorSplitSuggestion()` pre-capitalises it and adds it to the bar with a deliberately maximal
  score (kept out of the tier-1 `candidates` list itself, to avoid skewing `SuggestionMerger`'s own
  normalisation - appended only at display time). Suggestion-only like D-116 - tapping it delegates to the
  existing `applySplit()` (per-half capitalisation, A-07 undo, D-13 learning all reused, not reimplemented).
  592 unit tests (was 586; +6 `TokenRepairTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not
  yet device-tested.
- **D-116 CONFIRMED WORKING on device** (user confirmation, no code change - see §69).
- **§70 DONE (v0.8.40): settings row gained a third button - clear clipboard.** New idea, not from any prior
  backlog entry. `SettingsRowView.clearClipboardButton` (glyph `🗑`, matching the row's existing plain-`TextView`
  glyph style - `😊`/`⚙` - no drawable asset needed) sits immediately left of the settings gear via the same
  `Gravity.END` offset approach. Wired to a new `AdaptKeyService.clearClipboardFromSettingsRow()`, which closes
  the row and calls the pre-existing `clearClipboard()` (already used by D-36/D-38's post-paste auto-clear) -
  no new clearing logic, just a new direct trigger. Follows the established "tapping a row button closes the
  row" convention. Known, accepted minor edge case: an already-showing D-36 paste chip isn't retroactively
  hidden (only re-evaluated on field open) - harmless (pastes nothing once genuinely empty), not new (same as
  any external clear), not addressed this round. No new tests - Android glue, same established gap as the rest
  of `SettingsRowView`. 586 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not
  yet device-tested.
- **§69 DONE (v0.8.39): D-124 closed (not a MIME bug - Total Commander puts a genuine plain-text placeholder
  string on the clipboard, indistinguishable from real pasteable text; accepted as a known limitation, no
  app-specific hack); D-116 implemented (unhyphenated-compound recognition).** Also: **D-114 confirmed
  working; D-119/D-120/D-121/D-123 provisionally confirmed** on device. **D-116**: new pure
  `dictionary/CompoundSplit` peels a known noun (≥4 chars) off the front of an unknown token, optionally
  followed by a German Fugenelement, such that the remainder resolves (known, or a cost-1 correction of a
  known word) - e.g. "Beitragsjahreb" -> "Beitrag" + "s" + "jahren" ("Beitragsjahren" itself is too rare for
  the dictionary). Wired into `DictionarySuggestionProvider.suggestionsFor()`, gated on `candidates.isEmpty()`
  (only runs once ordinary prefix/fuzzy matching found nothing, both because that's when it's actually needed
  and to keep its extra store lookups off the common keystroke path - see D-138). Nouns-only for now (the
  user's own explicit, revisitable scoping call) and deliberately **suggestion-only** - never wired into
  `autocorrectFor()`/`highConfidenceCorrection()` - since the split point can be genuinely ambiguous (the
  classic "Wachstube" German compound-splitting counterexample); a wrong guess must only ever be offered, not
  silently applied. Only a single split point is tried (no recursion into further parts), and a typo inside
  the first part itself is out of scope. 586 unit tests (was 575; +11: 8 `CompoundSplitTest`, 3
  `DictionarySuggestionProviderTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
  device-tested - real compound coverage/false-positive rate needs everyday typing to judge.
- **§68 DONE (v0.8.38): device feedback on §64-§67 - D-105/D-108/D-130/D-138 CONFIRMED WORKING; D-124
  follow-up fix; D-111 clarified as D-115's own mechanism, not a preview bug.** **D-124**: the §66 fix didn't
  resolve it on device - re-traced, found the MIME check only ever ran on the URI branch
  (`resolveClipboardText()`'s `item.uri ?: return item.coerceToText(this)` came *before* the check); whatever
  copied the reported APK apparently puts its identity in `ClipData.Item.getText()` instead of `getUri()`,
  bypassing the check entirely. Fixed by moving the MIME check to run first, unconditionally - not yet
  re-confirmed on device. **D-111**: "stelle" previewing as "Stelle" but still auto-applying turned out to be
  D-115's exact mechanism, not a gap in the preview - confirmed via `dict_de.tsv`: `Stelle` was tagged plain
  `NOUN` (should be `NOUN,OTHER`, since "stelle" is a genuinely common verb reading too - "ich stelle").
  Fixed identically to `Stimmen`. **Found 141 more candidates via a systematic dictionary scan** (every
  `NOUN`-tagged word whose lowercase form is also a plausible verb "ich"-form) - deliberately **not**
  bulk-applied, since many are lopsided enough (`Ende` 15847 vs. `enden` 630) that blanket-fixing them would
  trade a rare mis-capitalisation for real friction on an overwhelmingly-common, currently-correct
  auto-capitalisation; needs the user's own call on scope (as-reported only / apply all / a frequency-ratio
  cutoff) before touching the rest. 575 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest`
  green.
- **§67 DONE (v0.8.37): D-135 implemented - Autofill inline suggestions (Google Password Manager or whatever
  autofill service the device has configured).** New `androidx.autofill:autofill:1.3.0` dependency (small
  compat helper, no minSdk impact) + `res/xml/method.xml`'s `android:supportsInlineSuggestions="true"`.
  `AdaptKeyService` gained two `@RequiresApi(Build.VERSION_CODES.R)` overrides -
  `onCreateInlineSuggestionsRequest()` (builds the request/style spec) and `onInlineSuggestionsResponse()`
  (inflates each opaque `InlineSuggestion` async, adds it to the new `InlineSuggestionsBarView`). **Caught a
  real mistake before it shipped**: the `InlineSuggestionUi` class initially assumed to live at
  `androidx.autofill.inline.suggestions` (a guess from memory, this API having never been used in this
  project before) actually lives at `androidx.autofill.inline.v1` - caught immediately by the compiler, then
  confirmed properly by `unzip`+`javap`-inspecting the actual resolved `autofill-1.3.0.aar`'s class list
  rather than guessing again, before fixing the import. New `InlineSuggestionsBarView` (plain
  `HorizontalScrollView`+`LinearLayout`, not a custom-drawn view - each suggestion is a real, opaque platform
  `View` AdaptKey can only place, never read or restyle, the documented privacy boundary) occupies the same
  row as the ordinary suggestion bar, toggling with it. `resetInlineSuggestions()` clears stale suggestions
  on every fresh field via `onStartInputView()`. No settings toggle added this round (purely additive,
  invisible unless a real suggestion exists - can add one later if device use suggests it's wanted). No new
  tests (Android/platform-callback glue only, consistent with the established testing gap) - **this entire
  feature is unverified beyond compiling; needs a real Android 11+ device with an autofill service configured
  to confirm anything actually renders.** 575 unit tests (unchanged). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green.
- **§66 DONE (v0.8.36): D-124, D-115/D-125, D-130, D-138 implemented; D-135 researched (confirmed feasible,
  not implemented).** **D-138 traced first, not guessed**: backspace-hold jerkiness root-caused to this
  session's own added per-keystroke SQLite lookups (D-106 stage 2's `knownInOtherLanguage`, D-111/D-112's
  capitalisation preview) running on every `handleBackspaceRepeat()` tick via `deleteComposingChar()` →
  `refreshSuggestions()`, competing with the hold's 45ms fastest interval (§59) - fixed with a `duringRepeat`
  parameter that skips those two additions (not the actual autocorrect protection, which still fully applies
  at commit time) for repeat ticks only. **D-124**: the real bug was `ClipDescription.hasMimeType()`'s own
  documented wildcard semantics - a clip declaring the generic `MIMETYPE_UNKNOWN` (some file-sharing paths use
  this, apparently including whatever shared the app's own APK) matches *any* requested pattern including
  `text/*`; fixed with a direct concrete-MIME-type check bypassing `hasMimeType` entirely. Also nearly
  re-introduced §60's own KDoc nested-block-comment bug while documenting the fix (a literal wildcard glyph
  sequence in a doc comment) - caught and reworded before it compiled wrong. **D-115/D-125**: new pure
  `RegularVerbInflection` recognises an unknown token as a plausible regular verb inflection of a *known*
  infinitive (strips a present-tense/preterite personal ending, checks whether `stem + "en"` is known) and
  protects it from autocorrect unconditionally (no ratio-override, since it has no recorded frequency of its
  own) - generalises D-115's two hand-patched words into an actual pattern-based rule, exactly as D-125 asked.
  **D-130**: `trackSustainedEnglishUsage()` promotes A-03's per-token English routing to a real
  `LanguageCycle`-style active-language switch after 5 consecutive English commits (D-106 stage 2's own
  cross-language autocorrect protection is untouched - a separate concern); `selectActiveDictionary()` now
  returns the full `DictChoice` (was a bare Boolean) so the caller can read which language a token was actually
  routed to. New `AdaptKeyboardView.beginLanguageChangeFade()` - a 260ms two-stage alpha cross-fade of the
  space bar's own label - acknowledges every language change, manual (G-01) or automatic. **D-135**: researched
  against current official Android docs (not assumed from memory) - genuinely feasible for a third-party IME
  via the public Autofill Framework inline-suggestions API (`onCreateInlineSuggestionsRequest`/
  `onInlineSuggestionsResponse`, `android:supportsInlineSuggestions`), works with whatever autofill provider the
  device has configured (not Google-specific), but needs API 30+ (AdaptKey's `minSdk` is 26, so this would be
  version-gated, not a `minSdk` bump) and the IME would only ever render opaque, platform-drawn suggestion
  views - never see the actual username/password content itself. Confirmed feasible with a clear shape; not
  implemented, left for its own future round given its size. 575 unit tests (was 567; +8: 7
  `RegularVerbInflectionTest`, 1 `DictionarySuggestionProviderTest`). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green.
- **§65 CAPTURED (still v0.8.35, no code change this entry): post-§64 real-device feedback, D-124…D-138.**
  Captured only, per explicit instruction - implementation planned as a separate later round. Highlights:
  **D-124** re-opens §60 as a bug - the clipboard MIME-type filter didn't block the app's own APK file from
  being offered via Quick Paste, not yet re-traced. **D-125** generalises D-115 into an actual rule request:
  unknown *regular* verb inflections ("beurteilst"→wrongly "beurteilt") shouldn't need every form in the
  dictionary, just recognition of the regular/productive conjugation patterns as "protect, don't necessarily
  suggest". **D-126** tier-3 needs an uninstall action and an independent enable/disable toggle (currently
  "installed" and "active" are the same state). **D-127/D-128**: document "what learns when" internally, then
  surface a compact, prominent settings explanation of what AdaptKey adapts/learns (name justification),
  covering raw-touch and the tier-3 LLM fade-out explicitly - possibly extending the D-89 feature-overview
  screen. **D-129** the calculator minus key's sign-flip (§31) needs a corner-hint glyph after all (§31
  explicitly accepted no-hint as a trade-off at the time). **D-130**: promote A-03's soft per-token English
  routing to a real D-106-stage-1 active-language switch after five consecutive English words (not just two),
  plus a space-bar acknowledgement animation (glow sweep or fade) for every language change, automatic or
  manual. **D-131**: raw-coordinate correction (D-39) should become visible earlier while still typing, not
  only at commit time. **D-132**: the settings-row slide-in (§50) should animate as a real slide-up, not pop in
  instantly. **D-133**: a harder, direction-specific (not just isotropic) bound on the bottom row's downward
  touch-zone drift, refining D-109. **D-134**: SPACE sometimes seems to get swallowed after a full stop -
  pattern only, no repro yet, possibly related to D-119/D-120 or pre-existing. **D-135**: Google Password
  Manager suggestions in the bar - a big, speculative idea explicitly flagged by the user as maybe needing
  Android Autofill/`InlineSuggestion` APIs this project doesn't yet use, needs feasibility research before any
  design commitment. **D-136**: gesture-area system controls have poor contrast versus Gboard - suspected
  missing `APPEARANCE_LIGHT_NAVIGATION_BARS`-style window-insets flag, not yet confirmed. **D-137**: a typed
  time (`14:30`) should always suggest "Uhr" as the next word (D-43 pattern addition). **D-138**: backspace-hold
  now feels jerky - flagged as a possible regression from this same session's own §64 `onTouchEvent()` changes
  (D-108's `movedOutsideKey` check, D-109's record/classify reordering), explicitly not yet traced or guessed
  at - needs investigation as the first order of business next round.
- **§64 DONE (v0.8.35): a large batch release from §61/§62's backlog - D-105, D-108, D-109, D-111-D-115,
  D-119-D-121, D-123. D-118 struck outright (not reproducible, no repro forthcoming); D-122 deliberately left
  unimplemented (still just an unconfirmed design idea, no trace done - this project's "trace before fixing"
  rule).** Layout: **D-105** every main-page digit key (both the Latin and Greek layouts) now has a second D-01
  alternative - its own superscript form alongside the existing shifted symbol - with `0` reversed for being an
  edge key (§34 precedent). Touch/gesture: **D-108** long-press now only cancels once the touch leaves the
  pressed key's own bounds, not the much smaller system touch-slop; **D-109** T-03 learning got two bounds - an
  ambiguous tap (T-05) is no longer recorded into the model at all (this was the actual mechanism behind
  bottom-row keys drifting toward space), `DEFAULT_MAX_OFFSET_FACTOR` tightened 0.9→0.5, and the D-24
  visualisation now shows the same capped offset the model's own resolution actually uses instead of a raw,
  more-extreme-looking uncapped value. Suggestions/capitalisation: **D-111/D-112** a pending capitalisation
  change (an ordinary noun about to auto-capitalise, or a spelling correction that must also carry the right
  case, e.g. "Fur"→"Für" not "für") now reuses the existing S-06 mechanism to preview live, before it applies -
  flagged as a broad behaviour change worth confirming doesn't feel noisy on-device, since German capitalises
  most nouns; **D-113** §44's ratio-override now also requires a cost-1 adjacent-key edit, not the full two-edit
  budget, fixing "spreche"→"Sprache" (cost-2, was a regression of §44's own fix) without touching "due"/"ddr"
  (both cost-1, unaffected); **D-114** a new `minAutocorrectFrequency` (opt-in, default 0 so ~15 existing tests
  with small synthetic frequencies stay unaffected; production wires 300) drops low-confidence candidates like
  "Virgin" (62) from autocorrect entirely, plus `vorhin` added to `dict_de.tsv` (was genuinely missing);
  **D-115** turned out to be two different causes - "merke" was a genuine missing-word gap (added, freq 65,
  protected by A-01 automatically once known), "stimmen"→"Stimmen" was **not** fixable by the case-only-
  correction idea discussed in §63 at all (that idea doesn't even reach this bug) - traced to
  `CapitalisationEngine`'s §6 rule 3 trusting a dictionary POS tag that mis-classified a genuine noun/verb
  homograph as unambiguous `NOUN`; fixed with a one-line data change (`NOUN`→`NOUN,OTHER`) that lets the
  *already-existing* §6 rule 5 (ambiguous → suggest, don't auto-apply) do its job. Mid-word correction (§58
  follow-ons): **D-119** SPACE mid-word now splits the token and inserts a literal space instead of finalising
  the whole word; **D-120** the same generalised fix (moved inside `finalizeAndCommit()` itself, not
  SPACE-specific) also fixes punctuation/long-press-symbol delimiters landing at the caret instead of always at
  the token's end; **D-121** `handleShift()` reordered to check Caps-Lock-off before the G-05 word-end gesture,
  and G-05 now requires the caret to genuinely be at the token's own end, not just "composing is non-empty" -
  one bug, fixes both the mid-word-Backspace-then-Shift and the Caps-Lock-off-mid-word repros; **D-123** a
  one-shot guard flag stops a suggestion-bar tap's own echo callback from clearing D-29's space-eating flag
  before the user's next punctuation keystroke ever arrives. 567 unit tests (was 560; +7: 4
  `KeyboardLayoutTest`/`GreekLayoutTest`, 3 `DictionarySuggestionProviderTest`). `:app:assembleDebug`/
  `:app:testDebugUnitTest` green. **Nothing in this round has been confirmed on a real device** - most of it is
  Android view/service glue this environment cannot exercise end-to-end (no emulator, no `InputConnection`
  Robolectric shadow).
- **§63 DONE (v0.8.34): D-106 stages 1+2 implemented - English promoted to a real, selectable keyboard
  language (QWERTY) plus cross-dictionary loanword protection.** New pure `language/LanguageCycle`
  (German→English→Greek, wrapping `next()`/`previous()`, 3 tests) drives the G-01 swipe direction-aware now
  that there are three languages, not two; `resolveDict()` forces the English dictionary/capitalisation/store
  unconditionally while active (mirroring the existing Greek branch), instead of only ever reaching English via
  A-03 auto-detection. `KeyboardLayout.rows()` gained a `qwerty` parameter (German QWERTZ vs. English QWERTY -
  only the `y`/`z` position differs, every hint/alternative/weight is shared), threaded through
  `AdaptKeyboardView`'s new `qwerty` property the same way `greek` already works. Known, accepted, self-healing
  trade-off: T-03's per-key offset model keys by character id (`"c:y"`/`"c:z"`), not screen position, so
  switching languages briefly carries over the wrong learned zone for just these two keys until ordinary typing
  re-adapts them - not worth a bigger id/migration fix for that. **Stage 2**: new
  `AdaptKeyService.knownInOtherLanguage(token)` checks every bundled language's dictionary (not just the active
  one) before allowing autocorrect - an embedded loanword like "Word" is now protected exactly like a
  locally-known word instead of being silently corrected to "wird". Wired asymmetrically: folded straight into
  `finalizeAndCommit()`'s existing `suppressAutocorrect` flag (so the A-05 split veto and every correction
  lookup downstream is covered for free), but kept separate in `refreshSuggestions()` (only the pending-
  autocorrect chip is suppressed there, not the whole suggestion bar - the active language's own completions
  must keep showing). **Also recorded a correction to the D-115 discussion**: traced end-to-end before
  implementing anything and found the previously-agreed "block pure-case-only corrections" idea does not
  actually fix "stimmen"→"Stimmen" - `bestCorrection()`'s own candidate filter already excludes case-only
  candidates structurally, so the real mechanism is `CapitalisationEngine`'s §6-rule-3 pure-noun auto-capitalise
  trusting a dictionary POS tag that itself mis-classifies this known homograph as unambiguous `NOUN` instead of
  the documented "mixed → ambiguous" case - §6 rule 5 (suggestion-only, no auto-apply) already exists and would
  already do the right thing once the *data* is fixed; this is a dictionary-build item, not a new runtime rule.
  Deferred alongside the Ablaut-generation idea (D-115), not started. **Stage 3 (installable languages beyond
  the three bundled) confirmed still worth keeping as a later item; its Greeklish-transliteration sub-item was
  struck entirely** (not deferred) per explicit user instruction - the handful of Greek loanwords anyone
  actually wants can just be learned via ordinary typing, and an English-convention transliteration mapping
  would often be wrong for German-flavoured Greek anyway. 560 unit tests (was 554; +6: 3 `KeyboardLayoutTest`
  qwerty cases, 3 `LanguageCycleTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
- **§61/§62 CAPTURED (still v0.8.33, no code change this entry, HEAD `e1e70bd` before this commit):** a large
  batch of new backlog from the user, split into two spec sections per the usual large-batch rule (capture,
  don't implement until released). **§61** (layout/architecture/touch-model, D-105…D-110): every main-page
  digit key gets a second alt (its own superscript digit, so every digit becomes a real D-01 popup, with the
  edge-anchored `0` needing the same reversed-order treatment §34 gave `p`); a big deferred language-
  architecture idea (English as an explicit selectable keyboard language like Greek, consulting every
  active/selectable language's dictionary plus always English while typing so embedded loanwords like "Word"
  aren't autocorrected away, and installable/activatable languages beyond the three bundled ones - explicitly
  not to be implemented from this capture alone, needs its own design pass like D-68/D-92 did); the S-05/C-04
  recognised-word highlight's semantics are under user reconsideration (possibly reversed to "green means about
  to be corrected" instead of today's "green means safe, no correction planned") - deliberately just flagged,
  not designed, per explicit instruction; long-press should tolerate a small in-key smear without cancelling;
  T-03 offset-model learning needs hard bounds (bottom-row keys drifting toward space from repeated mistaps,
  and a general drift cap - the user's own `j` key has drifted too far left within the existing per-axis-size
  cap); and an unexplained editor-specific auto-cap failure in the eBay-Kleinanzeigen app, not yet traced.
  **§62** is the user-requested "stop chasing individual words, find the general rules" master plan for a large
  batch of autocorrect/suggestion reports, organised into six themes (A: a pending change must be visible
  before it applies; B: false-positive corrections firing despite genuine ambiguity; C: dictionary
  coverage/morphology gaps; D: one report investigated and found not reproducible as described; E: four §58
  mid-word-correction follow-on bugs, all traced to the same mechanism; F: suggestion-accept-vs-punctuation).
  Several of §62's items were checked directly against the real bundled `dict_de.tsv` and the actual
  `AdaptKeyService.kt`/`DictionarySuggestionProvider.kt`/`KeyboardProximity.kt` code before writing, not
  guessed: confirmed **`spreche`/`Sprache` (D-113)** is §44's own 50x ratio-override firing on an unrelated
  homograph pair (7228 vs. 49 ≈ 147x) rather than a typo relationship - a real regression from §44's own fix;
  confirmed **`vorhin`/`Virgin` (D-114)** - `vorhin` is simply absent from the dictionary, `Virgin` is an
  English-proper-noun artefact of the Wikipedia corpus; confirmed **`merke`/`Marke` and `stimmen`/`Stimmen`
  (D-115)** - the lowercase verb forms are entirely missing, directly answering the user's own question: the
  dictionary build's POS-from-casing heuristic collapses a case-homograph pair into one surface form, silently
  discarding the other rather than keeping both or even flagging that both exist; confirmed **single "c"→"x"
  (D-118) is NOT currently reproducible** - `MIN_AUTOCORRECT_LENGTH = 2` blocks any single-character token
  before autocorrect runs at all, reported back rather than captured as a fix target; and confirmed, by reading
  `AdaptKeyService.kt` directly, all four §58-mid-word-correction bugs (D-119 SPACE mid-word wrongly finalises
  instead of inserting a literal space; D-120 mid-word punctuation always commits at the composing token's
  *end*, never at the actual caret, since `setComposingText(text, 1)` always parks the cursor there; D-121
  `handleShift()` checks `composing.isNotEmpty()` before the Caps-Lock-off branch and never checks the caret is
  genuinely at the token's end, so both a mid-word Backspace-then-Shift and a Caps-Lock-off Shift wrongly
  re-trigger the G-05 word-end gesture instead - one bug, two repros; D-123 suggestion-bar-tap's trailing space
  plausibly gets cleared by `reclaimWordAtCaret()`'s unconditional `pendingSuggestionSpace = false` before the
  D-29 punctuation check ever runs, though this one is code-grounded, not runtime-confirmed). Remaining §62
  items (D-111/D-112 visibility/case-propagation, D-116 compound-word recognition, D-117 too-late recognition,
  D-122 split-priority during mid-word correction) are captured as open design questions, not yet traced to a
  specific cause. **Nothing in §61/§62 is implemented - captured only, per the usual large-batch rule.**
- HEAD: `47bbf10` — v0.8.33 (§60). Working tree unchanged, this entry is a docs-only status update (no version
  bump). **Spec §12/§13/§14 complete.** §28-§60 implemented. **§58 (D-62/D-87 mid-word live correction)
  CONFIRMED WORKING on a real device** - closes out a saga that spanned D-84, §32's two D-87 fixes, §46's
  "no further defect found" investigation, and finally §58's actual missing-trigger fix; this is the first
  explicit user confirmation of the whole D-62 mechanism working end to end. §54's suggestion-accepted
  feedback also confirmed working (after §55's fix), iterated twice more per real-device feedback (§56, §57).
  **§59 (backspace-hold ~9% speed-up) CONFIRMED WORKING.** §60 (clipboard file detection) working well in
  initial testing but deliberately left open, not marked confirmed - the user wants it to prove itself more
  broadly before treating it as settled.
  **Every named backlog item is closed** (§26's D-88 via §54/§56/§57; §27's D-95/D-103/D-104 via §48/§53).
- **Versioning jumped from 0.7.54 to 0.8.3 on 2026-07-13** (user's deliberate call, see prior entry in git
  history) - the D-92/D-100/D-102 calculator/symbol-page redesign is the new 0.8 milestone. Still only the
  third digit bumps per APK going forward. `versionCode` counts up by 1 regardless of the version name
  (doesn't try to encode it - `8*10+3` would be lower than the outgoing value).
- Unit tests: **554 green** (`:app:testDebugUnitTest`, incl. Robolectric); `:app:assembleDebug` green (no
  warnings). `origin/main` confirmed up to date with local HEAD `47bbf10` (`git fetch` + rev-list check) as of
  the §60 round; not pushed without confirmation.
- **§60 DONE (v0.8.33): the D-36 paste chip no longer risks offering binary file content as garbled text.**
  A `ClipData.Item` carries either inline `text` (a plain copy) or a `uri` (a real file, e.g. from a Files
  app) - detectable for free from the clip's own declared `ClipDescription` MIME type, no I/O needed. New
  `resolveClipboardText()`: a plain text item behaves exactly as before; a URI item is only offered when
  `clip.description.hasMimeType("text/*")` (`.txt`/`.md`/any other text file), read directly via
  `contentResolver.openInputStream()` and capped at 512 chars (only the chip's own 24-char preview needs the
  content - the actual paste still goes through §38's native paste action, resolved by the target app, not
  by this read); anything else (image, PDF, unrecognised binary) suppresses the chip entirely. Caught a real
  bug while writing the KDoc: a backticked `` `text/*` `` in prose contains a literal `/*`, which Kotlin's
  *nestable* block comments treat as opening a nested comment - silently swallowing the rest of the file as
  commentary and causing a cascade of "unresolved reference" compile errors far from the actual mistake.
  Reworded to avoid a literal `/*` inside any comment.
- **§59 DONE (v0.8.32): backspace-hold repeat sped up ~9%**, both character-wise and word-wise, per request
  (felt "a touch too slow" by roughly 5-10%). `BackspaceRepeat.CHAR_START_DELAY_MS` 165→150ms,
  `CHAR_MIN_DELAY_MS` 50→45ms (scaled together so the decay curve's shape stays the same), `WORD_DELAY_MS`
  330→300ms. `INITIAL_DELAY_MS` (before a hold starts repeating at all) untouched - the request was about the
  repeat cadence, not the trigger delay.
- **§58 DONE (v0.8.31): found and fixed a real gap in D-62 mid-word live correction - not another bug in the
  reclaim mechanism itself (§32/§46 had already ruled that out), but a missing trigger for it entirely.**
  Two precise repro cases (re-editing a word via Backspace right after committing it; a plain tap into the
  middle of an existing word) both traced to the same cause: `reclaimSurroundingWord()` was only ever called
  from a keystroke handler, never in response to the caret simply landing on a word with nothing typed -
  confirmed via grep that no other call site existed. Fixed with a new `reclaimWordAtCaret()`, called from
  `onUpdateSelection()`'s previously-inert `composing.isEmpty()` branch whenever the selection collapses to a
  single caret position - runs the same reclaim-then-render sequence a keystroke would, batched the same way
  for the same D-87 reason, self-limiting against re-entrancy (composing becomes non-empty synchronously the
  moment a reclaim happens). Also answered a separate question: searched the dictionary/capitalisation/
  suggestion/language packages for hardcoded word-specific exception logic - found none; the only literal
  word lists are `SeedData.kt`'s bootstrap dictionary and `StubSuggestionProvider`'s fallback list, both
  legitimate placeholder *data*, not exception *rules*. Not yet confirmed on a real device. Accepted trade-
  off worth flagging: this makes every tap into any existing word start a live-edit session for it, not only
  a deliberate correction tap - matches what was asked, but worth confirming it feels right in practice.
- **§57 DONE (v0.8.30): the flying word now starts from the accepted suggestion's own position, not the
  bar's fixed centre.** New `SuggestionBarView.originXFor(word)` looks up the currently displayed chip whose
  word matches (case-insensitively) and uses its horizontal centre; falls back to the bar's centre - the
  previous §56 behaviour - only when no chip matches. One lookup naturally covers both trigger paths: a
  bar-tap acceptance finds its own just-tapped chip (still present, since the flight starts before the bar's
  next refresh); an autocorrect-on-commit acceptance finds the top suggestion chip whenever it happens to
  match the committed word (the common case). Rise distance/duration/shrink unchanged - confirmed already
  right as-is.
- **§56 DONE (v0.8.29): D-88 refined per device-tested feedback.** Sound: attack shortened to under 1ms plus
  a brief onset transient (sharper "Plöpp" not "Blöpp"), peak amplitude +10% (0.75 → 0.825) - same 900→140Hz
- **§56 DONE (v0.8.29): D-88 refined per device-tested feedback.** Sound: attack shortened to under 1ms plus
  a brief onset transient (sharper "Plöpp" not "Blöpp"), peak amplitude +10% (0.75 → 0.825) - same 900→140Hz
  pitch-drop shape otherwise. Animation: replaced the flat bar-flash with `SuggestionBarView.flyAccepted()` -
  the accepted word rises 34dp from the bar's centre, shrinking and fading over 380ms, exiting past the bar's
  own top edge (reaches the screen via `root`'s existing `clipChildren = false`, the same mechanism the D-53
  long-press popup already relies on) - explained to the user first that an IME genuinely cannot draw into
  the target app's own text field, so "flies into the text" concretely means "flies up and out of the
  keyboard", not literally crossing into the app's window. Decoupled from sound entirely: the flight always
  plays now; the plop sample plays *additionally* only when D-05 key sound is on (previously either/or).
  Also: renamed the D-59 combined-key setting from "?123 key" to "?123/ABC key" (all three locales).
- **§55 DONE (v0.8.28): fixed §54's flash overlay turning the whole suggestion bar solid black.** Reported
  immediately after §54 shipped. `SuggestionBarView`'s new `flashPaint` was declared with no initial colour/
  alpha; `android.graphics.Paint()` defaults to opaque black (alpha 255) until set otherwise, so `draw()`'s
  `flashPaint.alpha > 0` check was true from the very first frame, long before any acceptance had happened -
  `flashAccepted()`'s animator only ever set colour/alpha *while animating*, nothing ever initialised it
  transparent. One-line fix: `Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 0 }` at declaration.
- **§54 DONE (v0.8.27): implemented D-88 - feedback when a correction/suggestion is accepted.** Previously
  silent. Sound on (D-05): a new, distinct "plop" sample (`AdaptKeyboardView.playSuggestionAcceptedSound()`,
  same shared `SoundPool` as the click sample) - synthesised from scratch with a small Python script (stdlib
  `wave` only) since no suitable sample/tooling existed in this environment: a 130 ms exponential pitch sweep
  900 Hz → 140 Hz, bundled as `res/raw/suggestion_accept.wav`. Sound off: a 280 ms fading highlight flash on
  the suggestion bar instead (`SuggestionBarView.flashAccepted()`), reusing the C-04/S-05 highlight green via
  a new `flashColor` the service pushes from the user's actual configured colour. **Found and fixed a real,
  pre-existing bug while adding the second sample**: `SoundPool.setOnLoadCompleteListener()` only keeps the
  most-recently-registered listener - a second `ensure*Loaded()` following the original's exact pattern would
  have silently broken the first sample's "loaded" detection depending on call order. Fixed by installing one
  shared listener (checking both ids) once, at the pool's own lazy creation. Trigger points:
  `finalizeAndCommit()`'s existing `finalWord != typed` branch (same check S-05's undo-tracking already uses)
  and the `SuggestionController.Kind.NORMAL` suggestion-bar-tap branch. No new unit tests - all glue, no new
  pure logic. See spec §54 for full detail.
- **§53 DONE (v0.8.26): implemented D-103 (calculator `sin` key, D-01 popup with `cos`/`tan`/`log`) and
  D-104 (calculator `deg` key, single hint `rad`), and closed D-95 (settings-shortcut key) as resolved by
  §48's gear icon, no separate key needed.** Neither existing `KeyCode` fit a key whose tap commits a literal
  multi-character label (`CHAR` is hard-limited to one `Char`) - added `KeyCode.TEXT`, committing `key.label`
  via `finalizeAndCommit()`. Extended four places that were generic over "has a hint/alternatives" to also
  recognise `TEXT`, not just `CHAR` (`KeyboardLayout.hasLongPressAction()`, `AdaptKeyboardView.
  popupAlternativesFor()`/`preSelectedIndexFor()`, `Key.id` for the T-03 offset model - a bare `code.name`
  would have collided between `sin` and `deg`, now `"t:$label"`). Caught and fixed a real bug before
  shipping: `cos`/`tan`/`log` are ordinary Latin letters, so §35's `AlternativeScript.extendsWord()` guard
  would have treated them as genuine word text and let them extend the composing token - the same class of
  bug §35 fixed for the Greek math-symbol popup, here via a different (non-Greek) route. Fixed by threading
  the popup's originating key through `handleLongPressAlternative()`/`commitLongPressSymbol()` (previously
  discarded at the listener call site) so a `TEXT` key's alternatives always commit as symbols regardless of
  script. Placement: appended to calculator row 1, double-weighted versus that row's single-glyph keys since
  a three-letter label needs more room - a starting guess, not device-verified, easy to retune (§36/§37
  precedent). See spec §53 for full detail.
- **§52 DONE (v0.8.25, documentation only, no code change): records the user-supplied rationale for why
  the settings row's emoji button needs no setting at all (§51's removal).** The old D-18 toggle existed
  because the combined key permanently occupied a scarce main-keyboard slot for a low-value feature (most
  messaging apps ship their own emoji picker); the §48 row costs no permanent slot (reached by a swipe,
  exists only while open), so that trade-off - and the reason for a toggle - is gone. Confirms in code that
  the main keyboard's combined key now depends on `symbolKeyEnabled` (D-59) alone: `AdaptKeyboardView.isHiddenKey()`
  has no `emojiEnabled` counterpart left, `PanelNavigation.onCombinedKeyTap()` takes no emoji parameter.
  `symbolKeyEnabled` itself is unchanged - a separate concern (the combined key's own numeric/symbol
  function), not affected by the emoji button's now-settingless existence in the row.
- **§51 DONE (v0.8.24): §48 refined per user device-testing feedback - confirmed §47 and §48's core
  behaviour both work.** Two corrections: (1) the settings row now sits above the suggestion bar (topmost
  row while open), not between it and the keyboard - `root.addView()` order changed in
  `onCreateInputView()`; purely cosmetic, no technical reason favoured the old placement since the row's
  own open()/close() reveal animation is self-contained and position-independent. (2) the row's emoji
  button is now unconditionally visible - `SettingsRowView.emojiButtonEnabled` (tied to
  `symbolKeyEnabled`, §50's own reading of §48's "exclusively via symbolKeyEnabled" text) is removed
  entirely; user feedback was that gating it on the unrelated combined-key setting no longer made sense
  once the button had its own dedicated row. §50's un-prompted design call (tapping either row button
  closes the row) was explicitly confirmed as wanted, unchanged. See spec §51 for the full writeup.
- **§50 DONE (v0.8.23): §48 implemented - swipe-up settings row (gear + emoji button), resolving §26's old
  backlog entry.** New `SettingsRowView` (`app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/`), an
  ordinary `FrameLayout` inserted into the root layout between the suggestion bar and the keyboard container
  - deliberately *not* built on `AdaptKeyboardView`'s D-86/D-94 row-growth machinery, which animates row
  count within that one Canvas-drawn view, not a second view stacked above it (confirming §48's own
  suspicion this might not transfer directly). `open()`/`close()` jump the reserved layout height
  immediately (D-86 precedent in spirit) and animate only the row's inner content via `translationY`,
  clipped by the row's own bounds (a plain `ViewGroup`'s default `clipChildren`, no bespoke transition
  needed). Gestures: `GestureAction.OPEN_SETTINGS_ROW` (new) fires for an upward swipe anywhere except the
  combined key (`KeyGesture`); a downward swipe while the row is open is re-routed to close it first inside
  `AdaptKeyService.handleSwipe()`'s existing `DISMISS_KEYBOARD` branch (no new `GestureAction` for closing -
  `KeyGesture.resolve()` stays a pure function with no row-open state to gate on). Design call beyond §48's
  own spec text: the combined `?123`/emoji key's D-18 dual purpose is retired outright, always a plain
  `?123` toggle now (`PanelNavigation.onCombinedKeyTap()` lost its `emojiEnabled` parameter;
  `AdaptKeyboardView` lost its own `emojiEnabled` field) - keeping the old behaviour would have needed a
  replacement setting for the one §48 itself retires. `AdaptSettings.emojiPanelEnabled` removed end to end
  (`SettingsMapper`, `SettingsStore.KEY_EMOJI_PANEL`, `settings_preferences.xml`, all three locale
  `strings.xml`); the row's emoji button visibility is now driven by `symbolKeyEnabled` per §48's own
  instruction. Both row buttons close the row before acting. See spec §50 for the full writeup. `KeyGesture`/
  `PanelNavigation` (pure) tested via `KeyGestureTest`/`PanelNavigationTest`; `SettingsRowView` itself is
  Android view/animation glue with no decision logic of its own, untested per this project's established,
  documented limitation (no emulator/device in this environment).
- **§49 DONE (v0.8.22): §47 implemented - live colour preview of a pending A-05 split while composing.**
  `AdaptKeyService.updateComposing()` now calls a new `splitPreview(ic, text)` (calling
  `TokenRepair.trySplit()` directly, same as `shouldHighlightComposing()` already does inline - no cache,
  since every call site calls `updateComposing()` *before* `refreshSuggestions()`, so a cache written by the
  latter would always be one keystroke stale) and, when a split applies, colours the two halves via the new
  `SplitResult.spanRanges(token)`, which recovers the drop-vs-missed-space strategy from length arithmetic
  alone rather than a new wrapper type. `shouldHighlightComposing()` and the new `splitPreview()` now share
  an extracted `isEditingMidWord(ic)` helper (D-26's mid-word gate); mutual exclusivity with the single-word
  highlight falls out for free since `trySplit()` already rejects an already-known whole token. See spec §49
  for the full root-cause writeup of both deviations from §47's suggested shape. Tested directly on
  `SplitResult.spanRanges()` (`TokenRepairTest`); the `updateComposing()` Spannable wiring itself is Android
  glue, untested per this project's established, documented limitation (no emulator/device in this
  environment).
- **§46-§48 DONE (v0.8.21, documentation only, no code change):** §46 - third full audit of the D-62/D-87
  mid-word correction path found no further code-level defect; confirmed (by inspecting the actual jar)
  Robolectric 4.14.1 has no shadow support for `InputMethodService`/`InputConnection` at all, only
  `InputMethodManager` - a real tooling gap, not an effort shortfall, so no automated regression test could
  be built for this area; still needs real-device confirmation. §47 - detailed backlog write-up for live
  green-colouring a pending A-05 split while still composing (two real gaps: split detection currently only
  runs at commit time, and composing-text colouring is currently single-span only). §48 - detailed backlog
  write-up resolving §26's old swipe-up-settings-row item: upward swipe anywhere reveals an animated row
  (emoji button left, settings gear right, clipped-slide-up animation), downward swipe closes the row first
  and only a second downward swipe falls through to the existing G-03 dismiss-keyboard gesture, and
  `AdaptSettings.emojiPanelEnabled` can be retired in favour of the existing `symbolKeyEnabled` (?123/ABC)
  setting governing the emoji button too. See spec §46/§47/§48 for full detail.
- **§45 DONE (v0.8.20): fixed A-05 word-splitting into meaningless fragments (e.g. "meinst" → "mei St").**
  `TokenRepair.trySplit()`'s drop-a-character strategy had no bigram co-occurrence requirement at all -
  unlike the missed-space strategy, which already required one - so any token containing an over-space
  letter (`c`/`v`/`b`/`n`/`m`) whose two halves both happened to exist *somewhere* in the dictionary, however
  obscure, got split with zero evidence they're ever used together. Verified against the real assets: `mei`
  (freq 16, rare) + `st` (freq 5939, "Sankt") have zero recorded co-occurrences in `bigram_de.tsv`. Fixed by
  applying the bigram-co-occurrence filter uniformly to both strategies before ranking, not only the
  missed-space one - confirmed this doesn't regress the strategy's own motivating case (`und<c>das` → `und`
  `das`, 10,165 real co-occurrences) or the existing `immernoch` comparison test.
- **§43-§44 DONE (v0.8.19): two systemic autocorrect priority bugs, both root-caused against the real
  bundled assets (not guessed) and fixed at the rule level, with regression tests for the mechanism, not the
  reported words.** §43: `LanguageClassifier` could misclassify a single common word as foreign purely from
  n-gram noise (~30% of a 27-word German sample misfired, incl. the reported "Hallo" → English) - fixed with
  a new `minWords = 2` gate, independent of the existing `minNgrams` gate. This also explains the reported
  "switched to English mode": there is no third persisted language mode: `resolveDict()` just re-points the
  dictionary/capitalisation pipeline per-token, which reads as a mode switch once German capitalisation stops
  applying. §44: `DictionarySuggestionProvider`'s A-01 "known word" guard was an absolute veto with no sense
  of proportion - "due" (dictionary frequency 24) permanently blocked any correction to "die" (889,897)
  simply for existing in the dictionary at all, same for "ddr" (4,405) vs. "der" (1,004,234); `u`/`i` are
  QWERTZ-adjacent keys, so this is an ordinary mistap the system should already have handled. Fixed with a new
  `SuggestionProvider.shouldOverrideKnownWord(word, candidate)` (default false), true only when `candidate` is
  ≥50x more frequent - verified an ordinary, comparably-common word pair stays fully protected. Also confirmed
  and fixed the same bug independently duplicated in `AdaptKeyService.rawCoordinateCorrection()` (D-39/T-02/
  T-03) - explaining why the user never noticed it engaging for these exact cases; the underlying raw-tap
  machinery itself (`OffsetModel`, trained continuously from ordinary typing, not just K-01 calibration) is
  fine. Both fixes documented in spec §43/§44 with the full empirical trace.
- **§39-§42 DONE (v0.8.18): batch of 4 small fixes.** §39 dropped `/` from the letters page's comma-key
  popup (redundant with the `7` key's hint, D-96). §40: the D-36 paste chip is no longer offered once the
  clip is older than 5 minutes (`ClipDescription.getTimestamp()`, new `ClipboardPreview.isFresh()`). §41
  fixed a real bug: Backspace on a text selection deleted the character before the cursor instead of the
  selection - `handleBackspace()` never checked `InputConnection.getSelectedText()` at all; fixed by
  checking it first and replacing a real selection via `commitText("", 1)`. §42 fixed the system-gesture-
  inset strip below the keyboard showing whatever sat behind the IME window (it fell outside every child's
  bounds, so had no background of its own) - one line, `root.setBackgroundColor(R.color.keyboard_background)`.
- **§38 DONE (v0.8.17): D-36 clipboard paste reworked - native paste action, sensitive-only auto-clear.**
  Reported: the D-36 paste chip's `commitText()` loses app-specific paste behaviour (Google Keep splits a
  pasted multi-line list into one entry per line via its own native "Paste" handling, but not via plain
  committed text) - confirmed as a real, if surprising, side effect of *how* text arrives, not a Keep bug;
  a further ask (splitting first-line-into-title on Keep's list-note title field) is confirmed **not
  achievable from the keyboard at all** - no IME channel exists to target multiple app-internal UI elements.
  Reverted the D-36/D-60 paste chip from `commitText()` back to
  `InputConnection.performContextMenuAction(android.R.id.paste)`, addressing D-36/D-60's two original
  objections differently: "not honoured by every field" is **not fixed** (no way to detect after the fact
  whether a native paste happened, so no automatic `commitText()` fallback is possible) and accepted as a
  known, open risk; "races the immediate `clearClipboard()`" **is fixed**, by deferring the clear
  `CLIPBOARD_CLEAR_DELAY_MS` (300 ms, via the service's existing `Handler.postDelayed`, same pattern as
  `resortRunnable`) instead of firing it synchronously, with a re-check that the clipboard still holds what
  was pasted before actually clearing (skips if something else was copied meanwhile). Also: the clipboard is
  now only ever auto-cleared when the content is flagged sensitive (`ClipDescription.EXTRA_IS_SENSITIVE`,
  Android 13+, set by whichever app *copied* it - not content-pattern-matched, not based on the current paste
  target's field type; almost certainly the same mechanism behind GBoard's bullet-masked clipboard preview,
  and already used for AdaptKey's own D-36 chip preview) - ordinary clipboard content is preserved across a
  paste instead of being unconditionally wiped. No new unit tests - this is `AdaptKeyService`
  `ClipboardManager`/`InputConnection`/`Handler` glue, an existing documented testing gap.
- **§37 DONE (v0.8.16): row 5's `0` retuned, less oversized.** §36's `ABC_DECIMAL_WEIGHT = 0.5f` made `0`
  twice as wide as an ordinary digit key - reported as too large. Retuned to `0.75f`, giving `0` a more
  modest `1.5f` width; `0`'s own weight is derived from the constant, so this was a one-line change, and
  centring under `2` still holds (only equality between `ABC` and the decimal separator's weights matters).
- **§36 DONE (v0.8.15): calculator row 5 rearranged, `0` centred under `2`.** A friend's suggestion (0 on a
  phone dial pad / numpad sits centred, not under `1`). New order `ABC` `0` `,` (was `0` `,` `ABC`), with
  `ABC` and the decimal separator narrowed to the same weight (`SymbolLayout.ABC_DECIMAL_WEIGHT`, now `0.75f`
  per §37) and `0` widened to fill the freed space. Pure [Key.weight] change - `AdaptKeyboardView`'s row
  layout was already a plain proportional split, no structural work needed. Key fact worth remembering: two
  *equal* flanking weights always centre the cell between them on the row's own centre regardless of the
  shared value, which is exactly why §37 could retune it freely without re-deriving the centring math.
- **§35 DONE (v0.8.14): fixed auto-capitalisation of the Greek math-symbol popup.** Picking π/α/β/γ/δ/λ/ω
  from the `p` key's popup (D-99, math symbols on the Latin keyboard, not Greek-mode text) could get
  silently upper-cased by the ordinary auto-cap-at-word-start feature - wrong, since case is semantically
  significant for these (`Π` ≠ `π`), unlike an ordinary word. Root cause: `commitLongPressSymbol()` decided
  "extends the composing word, subject to case" purely via `isLetter()`, which is equally true for these
  Greek symbols and for G-01's genuine Greek accented vowels - nothing distinguished "real language letter"
  from "letter borrowed as a symbol". Fixed with a new pure, unit-tested
  `AlternativeScript.extendsWord(symbol, activeLanguageIsGreek)` (`keyboard` package, JDK
  `Character.UnicodeScript` under the hood - no hand-maintained character list): true only for a genuine
  letter that is either picked while actually in Greek mode, or not Greek script at all (so German umlauts
  are unaffected). When false, the symbol now routes through `finalizeAndCommit()` instead - the same
  verbatim, no-case-transform path plain punctuation already uses - which also fixes a related gap: these
  symbols no longer get folded into the same composing token as surrounding text, matching how the
  calculator page's own π key (§31, direct tap) already commits standalone.
- **§34 DONE (v0.8.13): letters-page `p` key's Greek-letter alternatives reversed.** `p` sits at the top
  row's right edge, so its popup gets clamped and grows leftward instead of the usual rightward - since the
  row always draws in list order, `π` (the key's own character) ended up at the far end instead of nearest
  the finger. Fixed by reversing the order only at that call site (`KeyboardLayout.topRowKey()`:
  `PI_ALTERNATIVES.reversed()`), leaving the shared `PI_ALTERNATIVES` constant (also used by the calculator
  page's `π` key, which isn't at an edge) untouched. Also answered the accompanying design question (could
  the layout auto-sort outward from the pivot instead of manual per-key reversal) - feasible but a real
  change to the popup draw/select/commit pipeline; not worth it for a single edge-anchored key, revisit if a
  second one shows up. See spec §34 for the full write-up.
- **§34 DONE (v0.8.13): letters-page `p` key's Greek-letter alternatives reversed.** `p` sits at the top
  row's right edge, so its popup gets clamped and grows leftward instead of the usual rightward - since the
  row always draws in list order, `π` (the key's own character) ended up at the far end instead of nearest
  the finger. Fixed by reversing the order only at that call site (`KeyboardLayout.topRowKey()`:
  `PI_ALTERNATIVES.reversed()`), leaving the shared `PI_ALTERNATIVES` constant (also used by the calculator
  page's `π` key, which isn't at an edge) untouched. Also answered the accompanying design question (could
  the layout auto-sort outward from the pivot instead of manual per-key reversal) - feasible but a real
  change to the popup draw/select/commit pipeline; not worth it for a single edge-anchored key, revisit if a
  second one shows up. See spec §34 for the full write-up.
- **§33 DONE (v0.8.12):** page 2 row 4 `°` moved ahead of `-` (`# ' ^ - ° + × ÷ * ±` → `# ' ^ ° - + × ÷ * ±`).
  Also confirmed (no code change needed): phone-number/similar fields already auto-open the calculator page,
  via §31's `initialSurfaceFor()`.
- **§32 DONE (v0.8.11): two D-87 root-cause fixes for mid-word live correction (D-62).** User reported
  suggestions not activating mid-word, the caret jumping to word/sentence end while typing, and words being
  swallowed. Found two independent, real bugs in `reclaimSurroundingWord()`'s neighbourhood, both verified by
  manual API-contract trace (no device/emulator available to reproduce live) - **needs real-device
  confirmation before considering D-87 closed**:
  1. The mid-word composing anchor used `ExtractedText.selectionStart` as if it were an absolute document
     position; per the API it's relative to the extracted chunk and needs `ExtractedText.startOffset` added.
     Silently correct in short fields (`startOffset` always 0), silently wrong in long/paginated ones. Fixed
     via a new pure `ComposingAnchor.resolve()` (top-level package, `WordBoundary`/`SentenceBoundary`
     pattern) plus 3 new unit tests.
  2. `reclaimSurroundingWord()`'s own `deleteSurroundingText()` ran as a standalone, unbatched
     `InputConnection` call before the later `updateComposing()` batch - its own `onUpdateSelection` callback
     could arrive after `composing`/`composingAnchor` had already advanced, reporting a stale cursor that
     mismatches `onUpdateSelection`'s "own edit" check and wipes the in-progress token. Timing-dependent, not
     field-length dependent - the more likely explanation for a reliably-reproducible bug. Fixed by wrapping
     the whole reclaim→insert→`updateComposing()` sequence in one outer `beginBatchEdit()`/`endBatchEdit()` at
     both call sites (`handleKey()`'s `CHAR` branch, `appendLongPressLetter()`); nested batch edits are an
     explicitly supported Android pattern. See spec §32 for full detail.
- **Sign-flip on the calculator's minus key DONE (v0.8.10):** the §31 backlog item, confirmed and requested
  directly. Long-press `−` flips the sign of the number before the caret - operates on committed text (not
  composing state, since digits commit immediately on this page). New pure `SignFlip` object (`keyboard`
  package, mirrors `WordBoundary`/`SentenceBoundary`) computes the edit; `SymbolLayout.MINUS_SIGN` exposed;
  `KeyboardLayout.hasLongPressAction()` gained a dedicated case for this hint-less, alternatives-less key;
  `AdaptKeyService.handleLongPress()` intercepts it before the generic commit-text path. Trade-off accepted:
  no corner-glyph visual cue, since giving it a hint would route it through the wrong (commit-text) pipeline.
- **§31 DONE (v0.8.9):** page 2 row 1 bullet moved ahead of `@`; row 4 `°` moved ahead of `+` and `^` inserted
  at position 3 (now 10 keys, matching rows 2/3's width). **Real bug fixed**: calculator-page `π` (and page
  2's `ƒ`) always got auto-capitalised to `Π`/`Ƒ` - root cause was the ordinary auto-cap-at-field-start
  feature (`armShiftForNextWord`) combined with two `uppercaseChar()` call sites keying off bare
  `Char.isLetter()` (true for Greek/Latin-hook letters) instead of "is this key actually on the letters
  surface". Fixed in both `AdaptKeyService.kt` (commit path, introduced `isWordLetter = raw.isLetter() &&
  surface == InputSurface.LETTERS`, also fixing a related gap where π was wrongly treated as continuing a
  composing word) and `AdaptKeyboardView.labelFor()` (display path, added the same surface check plus a
  missing `ch.isLetter()` guard). Calculator's `0` key gained a `#` long-press hint (phone-number fields).
  New: `AdaptKeyService.onStartInput()` now opens straight to the calculator page for
  `TYPE_CLASS_PHONE`/`TYPE_CLASS_NUMBER`/`TYPE_CLASS_DATETIME` fields via a new `initialSurfaceFor()` helper,
  instead of always defaulting to the letters surface. A calculator minus-key long-press sign-flip was raised
  tentatively and captured as backlog (spec §31), not implemented this round.
- **Page 2's ABC slot now reserved when hidden too (v0.8.8):** previously used D-93's omit-and-grow (space
  grows to fill the gap when ABC is hidden); now matches page 1's D-100-corrected reserved-slot treatment, so
  space stays its normal size instead of looking oversized. `SymbolLayout.rows()`/`catchAllRows()` dropped
  the `symbolKeyEnabled` parameter entirely - both pages' `ABC` key is now always present in the built rows;
  `AdaptKeyboardView.isHiddenKey()`'s `KeyCode.LETTERS` condition dropped its `symbolPage == 1` restriction,
  applying uniformly to whichever symbol page is showing.
- **Corrected the row-4 reshuffle (v0.8.7):** §30's "`+` moved to the end of row 4" was a misreading - it was
  always `±` (plus-minus), not the plain `+`. `+` is back in its original spot in page 2's row 4 (after `-`,
  before `°`); `±` moved out of row 1 to the end of row 4 instead. Row 1 is now 7 symbols (`€ @ • © ® Ø ƒ`);
  row 4 is now 9 (`# ' - + ° × ÷ * ±`).
- **Operator column reordered (v0.8.6):** the calculator page's right-hand operator column (4th cell of rows
  2-5) reordered top to bottom from `÷ × − +` to `+ − × ÷`. Only the operator identity per row changed;
  digits and each row's own column cell (space/currency/=/enter) stayed put. Each operator kept its own
  alt-popup wherever it moved.
- **§30 DONE (v0.8.5):** further follow-up on §29, before device testing:
  - Bracket popup order: square before curly (`( [ { <` / `) ] } >`), shared by both pages' bracket keys.
  - Page 2: dedicated `_` key dropped (redundant with `-`'s own `_` alt); `+` moved to the end of row 4; `€`
    moved out of row 4 to the start of row 1 (keeps its currency popup); apostrophe moved from row 1 to row
    4 (position 2, after `#`, roughly under row 3's `"`); `®` added next to `©`; `Ø` ("Durchschnitt") added.
  - Letters page: `f`→`ƒ` (function symbol, was unassigned; added to `LetterHints.PALETTE` too), also added
    to page 2 row 1. `o` gained `Ø` as a second long-press alternative alongside its umlaut (chosen as the
    intuitive host - visually a stylised O) - **caught and fixed a bug while building this**: `o` is in the
    *top* row, not the middle row, initially wired to the wrong row's key-builder; an internal test caught
    it before it shipped.
  - Calculator page: the π key was plain (no popup at all) - now reuses `KeyboardLayout.PI_ALTERNATIVES`
    (exposed, was `private`) for the same Greek-letter popup as the letters page's π. The user described an
    "obscure case-toggle function" firing there instead - no separate bug found in the shift/case code;
    flagged to confirm resolved on next device test, since the exact prior behaviour wasn't independently
    reproduced.
- **§29 DONE (v0.8.4):** correction round on §28, caught by the user before any device testing:
  - **D-101 corrected**: `(`/`)` are two separate keys again (not merged) - `(` gets
    `OPEN_BRACKET_ALTERNATIVES = ( { [ <`, `)` gets `CLOSE_BRACKET_ALTERNATIVES = ) } ] >`. Page 1 row 1 back
    to 8 symbols.
  - **D-100 corrected**: page 1's `ABC` key must stay a *reserved* slot when `symbolKeyEnabled` is off, not
    omitted - D-93's omit-and-grow pattern breaks D-100's cross-row column/grid alignment. `SymbolLayout`
    always emits it now; `AdaptKeyboardView`'s `isHiddenSymbolKey` (renamed `isHiddenKey`) gained a second
    condition covering it (drawn as nothing, taps inert - same mechanism as the D-59 combined-key case). Page
    2's own `ABC` is unaffected (not part of a column layout).
  - **D-100 also**: swapped `ABC` and `=` - `=` now lives in the always-visible right column (row 4), `ABC`
    moved into the digit grid (row 5, under `3`, still gated via the reserved-slot mechanism).
  - **D-102 corrected/extended**: page 2's `(`/`)` (in the shifted-symbols row) get the same bracket-family
    popups as page 1's; row 4's `€` also gets the common-currency popup (un-reversed order, `€ $ £ ¥`, since
    it sits on the left and should grow rightward); row 4's `-` gets `_` as a single alt.
- **D-102 DONE (v0.7.54, spec §28):** page 2 grew from 3 rows to 5. Row 1 keeps the leftover symbols (now 6,
  not 7 - the double quote moved out) + backspace. New row 2: a fixed digit row, unconditional, independent
  of C-09 (reverses D-92's original decision for this page). New row 3: the main number row's shifted symbols
  (`! " § $ % & / ( ) =`), directly tappable. New row 4: the main letter page's alt-hint symbols
  (`€ # - + ° × ÷ *`), with `@`/`/` skipped to avoid same-page duplicates (already covered by rows 1/3); `π`'s
  Greek popup (D-99) stays on the letters page, not duplicated. Row 5 (ABC/space/enter) unchanged. New test
  verifies no character repeats anywhere on the page. Row 4 is explicitly a first draft per the user's own
  "schauen wir, wie sich das ergibt und sortieren ggf. nochmal um" - not yet device-tested.
- **D-100 DONE (v0.7.53, spec §28):** page 1 restructured - narrower digit block, new right-hand column
  (`⌫`/`space`/`¤`/optional `ABC`/`⏎`, one per row from row 1 to row 5), digit grid grows a 4th row
  (`0`/decimal-sep/`=`/`+` under `1`/`2`/`3`/operator). Column cells share one weight
  (`CALC_COLUMN_WEIGHT = 1f`) so rows 2-5 line up automatically (same cell count per row); row 1's `⌫` is the
  confirmed exception, keeping `backspaceWeight`. Page-toggle key removed entirely from **both** pages
  (`KeyCode.SYMBOL_PAGE`, `SymbolLayout.togglePage()`/`pageLabel()`, and the service's tap handler all deleted
  as dead code) - both pages now reachable only via D-19/D-91 swipe or the `ABC` detour. Also, proactively per
  the user's own flag: `CalculatorLocale.COMMON_CURRENCY_SYMBOLS` reversed to `¥ £ $ €` so the popup biases
  toward growing leftward now that `¤` sits at the right edge (the existing `HorizontalLongPressPopup`
  edge-clamp already kept it on-screen, but was shoving it away from the stem key). Not yet device-tested -
  the most layout-heavy change of the whole §27/§28 round.
- **D-94 DONE (v0.7.52, spec §28):** fixed the slide-animation direction regression from D-91. Root cause:
  `AdaptKeyService.handleSwipe()` derived the animation's `forward` flag from the resulting *action*
  (NEXT/PREV) rather than the *physical swipe direction* - these always coincided before D-91 (right swipe was
  always NEXT), so D-91 silently decoupled them. Fix: `forward = direction == SwipeDirection.RIGHT` for the
  animation call in both branches; `PanelNavigation.swipePage()`'s own action-based `forward` (which page to
  land on) is unchanged. No new test - `AdaptKeyService` swipe-handling glue is an existing documented gap.
- **D-101 DONE (v0.7.51, spec §28):** page 1's `)` key removed; `(` now carries the whole bracket family
  (`( ) { } [ ] < >`) as its D-01 popup. Row 1 shrank from 8 to 7 symbols
  (`CALC_ROW1_SYMBOL_COUNT` updated). Page 2's bracket row deliberately untouched - its removal is D-102, not
  yet released.
- **D-98 DONE (v0.7.50, spec §28):** currency popup reordered to `€ $ £ ¥`
  (`CalculatorLocale.COMMON_CURRENCY_SYMBOLS`). New generic corner indicator, `MORE_ALTERNATIVES_GLYPH = "◢"`,
  drawn by `AdaptKeyboardView.drawKeys()` for any key with no single hint but a D-01 multi-alternative popup
  (comma, period, ×/÷/=/currency/`(` on the calculator page, etc.) - previously these drew no corner cue at
  all. The right-to-left popup-growth question (tied to D-100's column move) stays open until D-100 lands.
  Not yet device-tested.
- **D-97 DONE (v0.7.49, spec §28):** `AdaptKeyboardView.labelFor()`'s space-bar language label (D-03) now
  only applies on the letters surface; any other surface's space key shows a new plain `SPACE_GLYPH = "␣"`
  (U+2423 OPEN BOX) instead. Scoped to "not letters" for consistency with the D-92 follow-up's G-01 gesture
  fix - also fixes page 2's space key, not just the calculator page the user explicitly mentioned. No new
  test (private drawing-only View glue, an existing documented gap). Not yet device-tested.
- **D-96/D-99 DONE (v0.7.48, spec §28):** `KeyboardLayout.DEFAULT_LETTER_HINTS` reorganised - `x`→`×`, `c`→`÷`,
  `v`→`/`, `b`→`*` (replacing D-90's `b`→`×`/`v`→`÷`; `x`/`c` were previously unassigned). The `p`→`π` key
  gained a Greek-letter D-01 popup (`π α β γ δ λ ω`) via a new private `topRowKey()` helper in
  `KeyboardLayout.rows()`, gated so a user-reassigned `p` (via C-08) keeps its own plain hint instead of the
  Greek popup. No view-layer change needed - the popup mechanism was already generic. Not yet device-tested.
- **§27 CAPTURED, NOT STARTED (still v0.7.47, no code changes this entry):** device round 15 on the D-89…D-92
  batch. D-91 confirmed working. New items: D-94 (bug - the D-91 direction fix didn't reach the slide
  animation), D-95 (parked idea - a Settings-shortcut key, no placement decided), D-96 (main page: reorganise
  v/b/x/c alt-hints - x→×, c→÷, v→/, b→*), D-97 (calculator space key shouldn't show the language label),
  D-98 (currency popup reorder to €$£¥ + a new "more alternatives" corner-hint indicator that doesn't exist
  yet), D-99 (π key gets a Greek-letter alt-popup: α β γ δ λ ω), D-100 (the big one - narrower digit block,
  new persistent right-hand column for ⌫/space/¤/ABC/⏎ with a real per-row-weight technical challenge to
  match column width, digit grid grows to 4×4 with 0/,/=/+ on a new bottom row, page-toggle key removed
  entirely from both pages), D-101 (`(`/`)` merge into one key with the full bracket family as its popup),
  D-102 (page 2 upgraded into a real number-row alternative for C-09-off users - fixed digit row always shown,
  a row of the main number row's shifted symbols, and the main page's letter alt-hints distributed across the
  rest - explicitly flagged by the user as a draft to re-sort once built), D-103 (calculator page: a `sin` key
  with alt `cos`/`tan`/`log`, no placement decided yet), D-104 (calculator page: a deg/rad toggle key, same
  status as D-103 - both explicitly parked until D-100's reshuffle exists to fit them into).
- **D-92 follow-up DONE (v0.7.47):** corrected D-92's currency/decimal-separator design. The user pointed out
  both the keyboard-selected language *and* the system-selected language/region were meant to drive these
  keys, with the system as the tiebreaker when the keyboard language doesn't disambiguate (German and Greek
  never do). New pure `CalculatorLocale` object resolves a `java.util.Locale` into currency glyph/popup +
  decimal/thousands separators using the JDK's own `DecimalFormatSymbols`/`Currency` locale data (no
  hand-maintained table) - verified for en_US ($/.), en_GB (£/.), de_DE (€/,), el_GR (€/,), ja_JP (fullwidth
  ¥), plus a fallback to € for locales without a resolvable single-glyph currency. `SymbolLayout.rows()`
  gained a `locale` parameter (calculator page only); `AdaptKeyboardView.systemLocale` threads it through the
  same way as `symbolKeyEnabled`; `AdaptKeyService.applySettings()` sets it from
  `resources.configuration.locales[0]` - the device's actual system locale, not the DE/EL keyboard toggle.
- **D-92 DONE (v0.7.46):** rebuilt `SymbolLayout` into a real calculator page 1 (digit block 7-8-9/4-5-6/1-2-3,
  operators with alt-popups `× → * × ·`, `÷ → / ÷ :`, `= → = → ≈ ≙`, this page's own `²`/`³` hints on 2/3, a
  consolidated `¤` currency key, `,` decimal separator with a thousands-separator hint (both later corrected
  to be locale-aware, see the follow-up above), a deliberately smaller inline space key) and a leftover
  catch-all page 2 (`@ _ " ' • © ±` + bracket family, no number row, no sentence punctuation, normal-sized
  space). Space stays inline left of Enter per the user's explicit call, not a new stacked-above-Enter
  layout-engine feature. Also, per the user's addendum while making that call: `KeyGesture.resolve()` gained a
  `surface` parameter so the G-01 space-bar language swipe only fires on the letters surface - the symbol
  pages' space key (old and new) now falls through to the ordinary D-19 surface swipe instead, fixing a latent
  bug where it inadvertently switched language. D-93's `symbolKeyEnabled` gating carries over to both new
  pages. Not yet device-tested - this is the item most in need of real-device visual/ergonomic feedback of the
  whole session (crowded row 5, digit-block proportions, popup readability).
- **D-91 DONE (v0.7.45):** reversed which `SwipeDirection` maps to `SWITCH_SURFACE_NEXT` vs.
  `SWITCH_SURFACE_PREV` in `KeyGesture.surfaceSwipe()` - left now advances letters → symbols-1 → symbols-2,
  right goes back. G-01 (space-bar language swipe) and G-02 (delete-word swipe) are untouched; the D-76/D-86
  slide-direction plumbing in `AdaptKeyboardView.switchPage()` needed no change, as anticipated.
- **D-90 DONE (v0.7.44):** `KeyboardLayout.DEFAULT_LETTER_HINTS` gained `p`→`π`, `b`→`×`, `v`→`÷` alongside
  the existing AltGr-style set; `π` was also added to the C-08 editor's `LetterHints.PALETTE` so it stays
  pickable for other keys and the palette-covers-all-defaults invariant test keeps holding.
- **D-89 DONE (v0.7.43):** new `FeatureOverviewActivity` ("Was AdaptKey alles kann"), reachable from a new
  entry in the settings screen's Info & Privacy category (right after "Show introduction again"). Renders a
  scrollable list of 18 curated feature entries (adaptive touch model, diacritic-aware autocorrect,
  neighbour-key/space correction, autocorrect undo, long-press popups, alt-key hints, the number row, the
  D-19 swipe, G-01…G-05 gestures, quick paste, next-word prediction, the optional tier-3 mini-AI, the three
  auto-detected languages, provably-offline), each with a one/two-sentence description, in all three locales
  (EN/DE/EL). The list itself lives in a pure `FeatureCatalog` object (string-resource-id pairs), unit-tested
  without Robolectric (`FeatureCatalogTest`); `FeatureOverviewActivity` just inflates it into title/description
  view pairs, covered by a Robolectric test (`FeatureOverviewActivityRoboTest`) checking one pair per entry
  renders. Not yet device-tested.
- **D-93 DONE (v0.7.42):** on the symbol/numeric pages (L-03), the dedicated page-toggle (`1/2`/`2/2`) and
  back-to-letters (`ABC`) keys are now omitted whenever D-59's `symbolKeyEnabled` setting is off - they are
  redundant with the D-19 full-field swipe, which already reaches every surface regardless of that setting.
  `SymbolLayout.rows()` gained a `symbolKeyEnabled: Boolean = true` parameter; when `false` the two keys are
  simply left out of their row's key list, and the remaining keys in that row grow to fill the freed space
  automatically (row layout already divides row width proportionally by the sum of the row's key weights, so
  no separate weight recalculation was needed). `AdaptKeyboardView.rowsFor()` now threads its existing
  `symbolKeyEnabled` property through to `SymbolLayout.rows()`. A small, directly-approved, self-contained
  fix - not part of the §26 backlog batch. Not yet device-tested.
- **§26 CAPTURED, NOT STARTED (still v0.7.41, no code changes this entry):** D-86 confirmed working
  ("Animation ist jetzt perfekt"); D-71/D-73 touch-zone precision confirmed good, next focus area named:
  autocorrect quality. User then handed over a large backlog batch explicitly framed as "für die
  Specs/das Backlog" (invoking the usual capture-first-then-wait-for-release rule) - all captured in
  spec.md §26, nothing implemented yet:
  - **D-87** (bug, re-opened): mid-word live correction (D-62) still does not activate at all, despite
    D-84's fix - that fix was for a real, logically-confirmed bug (duplicated `tokenContextBefore`
    suppressing suggestions), but apparently did not fully resolve what the user is seeing. Needs a fresh
    investigation with a precise repro before another fix attempt.
  - **D-88**: feedback (a distinct "plop" sound when D-05 sound is on, a micro-animation when it's off) on
    accepting a suggestion/autocorrect - currently silent and easy to miss.
  - **D-89**: a full "all features" settings page - the feature surface has grown large and easy to miss.
  - **D-90**: three new `DEFAULT_LETTER_HINTS` alt-keys on the main page - `p→π`, `b→×`, `v→÷`.
  - **D-91**: reverse which swipe direction (`KeyGesture.surfaceSwipe`) advances vs. goes back through the
    D-19 page cycle - small, self-contained, easily reversible once released.
  - **D-92**: the big one - redesign both `?123` pages. First becomes a real calculator layout (digits in
    a proper 7-8-9/4-5-6/1-2-3/0 block, arithmetic operators with their own alt-popups, `=` with
    `→`/`≈`/`≙` alts, this page's own `2`/`3` get `²`/`³`, a new locale-aware consolidated currency key
    with a `$`/`£`/`¥` popup replacing the old two separate currency keys, a language-dependent decimal
    separator with the thousands separator as its alt, and a smaller space key). Second page becomes a
    leftover/catch-all for whatever isn't reachable elsewhere any more (not many characters left over) plus
    a few proposed filler additions (`•` `©` `±` and bracket pairs), explicitly never showing the number
    row regardless of the C-09 setting. A full proposed ASCII layout for the calculator page is in
    spec.md §26 D-92, flagged as a first draft to refine together once implementation starts - one open
    question already flagged there: the "space directly above Enter" placement as requested would need a
    small layout-engine extension (partial-row-height keys) that doesn't exist yet; proposed a same-row
    inline-adjacent placement as a pragmatic stand-in pending the user's call.
- **D-86 DONE (v0.7.41):** implemented the refinement flagged just below. `AdaptKeyboardView.switchPage()`
  now compares the target page's row count against the current one (new `rowsFor()`, extracted from
  `rebuildRows()`) and resizes *immediately* before the slide starts when growing into more rows (the
  incoming page is never short on space), while still deferring the resize until the slide ends when
  shrinking into fewer (D-76's original mid-slide-jump fix, unchanged for this direction).
  `layoutKeys()` also now lays rows out bottom-up (anchored above `paddingBottom`) instead of top-down, so
  whichever page is momentarily shorter than the view's current (not-yet-resized) height keeps its
  bottom-most row (space/enter) pinned to its usual position instead of the whole page jumping to sit at
  the container's top edge - exactly reproduces the old top-down result once the height matches again (a
  no-op outside the transient mismatch window). D-82's clip-to-bounds fix is kept as a last-resort safety
  net for the brief window `requestLayout()`'s async re-measure still leaves even for the "resize
  immediately" case. Not yet device-tested.
- **§25 D-85 DONE, D-82 CONFIRMED FIXED + a flagged refinement idea (v0.7.40):**
  - **D-82 confirmed fixed** - user verified no more bleed into the gesture area.
  - **D-82 refinement idea (not implemented, awaiting confirmation)**: user proposed either always drawing
    pages bottom-aligned (space row at a fixed position, only the top edge moves with row count), or - if
    that alone is not enough - resizing *before* the slide when the target page is taller and *after* it
    when the target page is shorter, instead of D-76/D-82's blanket "always defer the resize, clip during
    the animation". The resize-order idea is a genuinely cleaner fix for the underlying cause than the clip
    workaround (a growing page would never need to be clipped/invisible at all), while bottom-alignment
    alone would likely just relocate where any residual overflow lands (towards the suggestion bar, where
    D-53's popups already intentionally overflow) rather than eliminate the timing issue outright. Flagged
    to the user as worth doing but not started - pure discussion so far.
  - **D-85**: `CLICK_VOLUME` halved again, `0.3` (D-83) → `0.15` - D-83 was a clear improvement but still
    not "dezent" (subtle) enough per the user. **Confirmed by the user: "perfekt" at 0.15; the D-83 latency
    fixes (48kHz sample, eager decode, `USAGE_GAME`) also confirmed "ziemlich erträglich" (quite
    tolerable).** D-70/D-83/D-85 sound work is done unless something regresses.
- **§24 D-82…D-84 DONE + D-09 REMOVED (v0.7.39), plus a device-feedback status sweep:**
  - **D-82** (D-76 follow-up: slide still bled into the gesture area): the deferred `requestLayout()` from
    D-76 freezes the view's measured height at the *outgoing* page's height for the whole animation: when
    growing into a page with more rows, the incoming page's extra row drew below the still-old bounds and,
    since the container disables `clipChildren` for the D-53 popup overflow, bled straight through into the
    reserved bottom gesture-nav inset padding. Fixed by explicitly clipping key drawing to the view's own
    current bounds in `onDraw` (slide and plain case both), leaving the long-press popup outside that clip
    so D-53's own overflow keeps working. Cost: a growing page's extra row is briefly clipped (invisible)
    until the resize lands at the end of the slide, rather than bleeding into the gesture area.
  - **D-83** (D-70 follow-up: sound too loud + slightly delayed, both confirmed good in character):
    `CLICK_VOLUME` 0.9 → 0.3 (SoundPool volume is linear, not dB - a much bigger perceived cut than the
    numbers alone suggest). For the delay: re-encoded the sample at 48 kHz (many devices mix natively at
    48 kHz; 44.1 kHz could need on-the-fly resampling), moved the async `SoundPool` decode to fire the
    moment the D-05 setting turns on instead of lazily on the first key press, and switched
    `AudioAttributes` usage from `USAGE_ASSISTANCE_SONIFICATION` to `USAGE_GAME` (SoundPool's own
    documented low-latency recommendation for one-shot effects).
  - **D-84** (D-62 bug: mid-word editing produced **no suggestions at all**): `reclaimSurroundingWord()`
    captured `tokenContextBefore` before deleting the reclaimed fragment from the real editable, but never
    trimmed it afterwards - so the fragment ended up duplicated (once in `tokenContextBefore`'s tail, once
    inside `composing`), corrupting every `"$tokenContextBefore $typed"`-style string built later and
    silently tripping the A-03 language classifier into treating the context as foreign, suppressing
    suggestions and autocorrect entirely - specific to the mid-word reclaim path, which is exactly what the
    user reported. Fixed by trimming `tokenContextBefore` right after the reclaim.
  - **D-09 removed entirely** (explicit instruction: "bitte komplett aufräumen"): `RawTapRecorder`,
    `CalibrationSentences`, `CalibrationSession` and their tests, `AdaptKeyboardView.OnRawTapListener` /
    `onRawTapListener`, the `recordRawTaps` setting across the whole settings stack, and the `d09_*` strings
    (all 3 locales) are all deleted rather than repurposed - test count dropped from 505 to 488 as a result
    (the removed classes' own tests went with them, not a coverage loss).
  - **Device-feedback status from this round** (no code changes for these, just recording the verdicts):
    D-58/D-76 slide direction confirmed correct and no longer wobbly (D-82 above addresses the residual
    gesture-area bleed); **D-61 confirmed working** (Enter honours the editor's IME action); **D-64
    accepted as-is** ("nicht ganz intuitiv, aber daran kann man sich gewöhnen" - no further changes
    planned); **D-67/D-69 confirmed correct** ("kleiben" now correctly stays "kleiben" rather than being
    auto-corrected or split, since it is genuinely ambiguous); **D-66/D-75 vibration confirmed STILL not
    firing** despite the VibratorManager + `USAGE_TOUCH` migration - three targeted API-level fixes across
    three rounds (D-06, D-34, D-66/D-75) have not resolved it, so this needs a device-side diagnostic next
    (does *any* app vibrate on this device at all; is there a system-level "vibration & haptics" master
    toggle involved) rather than another blind code change.
- **§23 D-81 DONE (v0.7.38):** the same cutout/status-bar gap as D-80, but for the first-run onboarding
  panel - while shown, `AdaptKeyService` stretches the whole input view to the full screen height
  (`setOnboardingShown()`), so its top can reach a front-camera cutout too, but the insets listener only
  ever padded the bottom. Fixed identically (also inset for `statusBars()` / `displayCutout()`); during
  ordinary typing the view never reaches the top inset region, so this adds zero extra padding there and
  cannot push the normal keyboard down.
- **D-74 RESOLVED (cause unconfirmed) + §23 D-80 DONE (v0.7.37):**
  - **D-74**: user confirmed the switch now works correctly, seeded zones look right, deliberate D-68/D-71
    overlap into neighbouring keys noted as expected (not a bug). None of the intervening changes (§21/§22:
    label removal, dead-row cleanup, rename, two new tests) touched the actual persist/load mechanism, so
    the fix can't be attributed with certainty - most likely the original D-74 service-side staleness guard
    reaching the user's device only now, though a one-off render/device quirk during the earlier failing
    attempts can't be ruled out either.
  - **D-80** (`TouchModelActivity`'s D-24 intro text sits under the front-camera cutout): root cause was
    identical to a pre-existing gap in `CalibrationActivity` too - both screens' window-insets handling only
    padded for the *bottom* inset (nav bar / gesture pill, for the keyboard preview), never the *top*
    (status bar / display cutout), so edge-to-edge drawing let text start right under a front camera. Both
    now also inset for `WindowInsetsCompat.Type.statusBars()` / `.displayCutout()`. Also gave the D-24 intro
    the more spacious two-paragraph treatment requested: split into `d24_intro_1` / `d24_intro_2` (replacing
    the single `d24_intro` string, all 3 locales), larger font, more margin - there being little text and
    plenty of vertical space to use above the keyboard preview.
- **§22 D-79 DONE, D-74 STILL UNRESOLVED - now tested at two levels (v0.7.36):**
  - **D-79**: renamed "Typing pattern" (DE "Tippmuster") to "Typing style" ("Tipp-Stil") everywhere it's
    user-facing (onboarding, calibration screen, settings entry, reset dialogs) - the old name was ambiguous
    with the D-24 learned-touch-zones concept. The German D-24 screen, which had confusingly reused the same
    word ("Tippmuster anzeigen") for that different concept, is now "Trefferzonen anzeigen" (matching
    EN/EL, which already used a different word there). EN/EL string *values* changed; no resource key
    renames, so no code changes needed beyond the strings themselves.
  - **D-74 follow-up**: user confirmed the stale-mask bug reproduces on the very first pattern-switch
    attempt, and every one after - ruling out any theory tied to substantial prior real-usage data or repeat
    switches specifically. Added a second regression test, `CalibrationActivityRoboTest`, one level more
    realistic than the first (`OffsetStoreRoboTest`, §21): drives the actual `CalibrationActivity` UI via
    Robolectric's `ActivityController` - builds the real activity, taps the real "Both Thumbs" button via
    `performClick()` (not calling `persistPattern()` directly), then opens a real `TouchModelActivity` and
    inspects its keyboard's live `offsetModel`. This exercises real layout/measurement timing and the actual
    click-listener wiring that a plain unit test can't reach. **It also passes** - number row seeded (D-72
    confirmed working), pattern and model both correctly persisted and reflected on the very first attempt.
    Two independent tests at different levels now both pass; root cause still not found in this code path.
    Told the user this plainly and asked for a screenshot or a precise description of which key's zone looks
    wrong and how, since code review and testing have been exhausted here.
- **§21 D-77 / D-78 DONE, D-74 follow-up INVESTIGATED BUT UNRESOLVED (v0.7.35):**
  - **D-77**: removed the "(recommended)" suffix from the Two Thumbs calibration button - "helps nobody,
    actively unhelpful for anyone whose real pattern differs"; the silent skip-default (D-73) already covers
    the intent. Plain "Both thumbs" label again; the now-unused `t04_pattern_two_thumbs_recommended` string
    (all 3 locales) removed.
  - **D-78** (cleanup, found while investigating D-74's follow-up below): the settings screen had two
    separate "Typing pattern" rows - the real one (`k01_calibration`, opens `CalibrationActivity`) and a
    second, `t04_detected`, permanently `selectable="false"` with no click handler at all, i.e. fully inert,
    left over from before D-68's calibration rewrite. Removed entirely (preference, its now-empty
    "Typing pattern (T-04)" category, the `SettingsActivity` code keeping its summary in sync via
    `patternLabel()`, the now-unused `TypingPattern` import, and the 3 now-dead strings) - `k01_calibration`
    already covers it.
  - **D-74 follow-up - root cause still not found**: user reports the touch-zone mask still shows stale
    data after a pattern switch, checked via Settings → "Show typing pattern" (D-24). Direct questioning
    established the *entire* repro happens inside the Settings app - pick pattern → dialog OK → Settings →
    "Show typing pattern" - **with no app switch and no live keyboard/IME involvement anywhere in the
    sequence**. This rules out D-74's fix as the relevant mechanism here (it only guards
    `AdaptKeyService`'s own long-lived in-memory model against a *service*-side stale save; the IME is never
    in this loop at all). Wrote a new regression test
    (`OffsetStoreRoboTest.\`switching pattern fully replaces previously learned data\``) that reproduces
    `CalibrationActivity.persistPattern()`'s exact save sequence - including seeding an old model with 500
    *real recorded taps* first (not just a stale seed) to match "previously learned zones" - followed by
    `TouchModelActivity`'s exact load; **it passes**, proving the persist/load round trip is correct at the
    store layer. The bug, if it's still there, is somewhere neither code review nor this test have reached -
    told the user this and asked for a more specific repro (first switch of a session vs. a later one; which
    exact key still looks wrong).
- **§20 D-76 DONE (v0.7.34, D-58 follow-up, both bugs - device-verification pending):**
  `AdaptKeyboardView.switchPage()` had two bugs in the D-58 page-slide:
  - **Backwards direction**: `slideSign` was assigned the wrong sign - a forward transition slid the new
    page in from the right and the outgoing page out to the left, the opposite of the usual page-swipe
    "content follows the finger" feel (a forward/right swipe should drag the current page off to the
    **right**, revealing the new one from the **left**, like a photo gallery). Fixed by flipping the sign.
  - **Wobbly / not-one-piece slide**: letters (5 rows with the C-09 number row shown, the default) and the
    numeric/symbol layer (always 4 rows - the number row is not optional there) have different row counts,
    so `rebuildRows()`'s `requestLayout()` resized the whole view **immediately** on a page switch, while
    the slide animation was still running - everything below the row-count difference, most visibly the
    bottom-most space row, visibly jumped mid-slide. Fixed with a new `deferRequestLayout` flag (an
    overridden `requestLayout()` that no-ops while true): the resize from `surface =` / `symbolPage =` is
    now suppressed for the duration of the slide and only applied once the animation's `onAnimationEnd`
    fires (both on natural completion and on `cancel()`, e.g. a rapid double-swipe).
  - No new tests - pure View/animation glue (`ValueAnimator`, `requestLayout()` timing), consistent with
    how this layer is tested elsewhere.
- **§19 D-69…D-75 DONE (v0.7.33, round-8 device feedback - device-verification pending):**
  - **D-69** (autocorrect bug, "immernoch" → "immer och" instead of "immer noch"): `TokenRepair.trySplit`'s
    "drop a character" strategy returned unconditionally whenever it found any candidate at all, without
    ever trying (or comparing against) the "fully missed space" strategy - `n` sits over the space bar, so
    "immer"+"och" satisfied the drop path and short-circuited before "immer"+"noch" (found by the
    missed-space path, an extremely strong bigram) was even considered. Fixed by evaluating both strategies
    and letting the single best-scoring candidate across both win, instead of the drop path having
    unconditional priority. Distinct from D-67 (no high-confidence single-word correction exists for
    "immernoch", so that veto doesn't apply here) - a new failure mode in the same split pipeline.
  - **D-70** (key sound is a digital beep, should read as a typewriter): `ToneGenerator` can only render a
    pure/dual sine DTMF-style tone, so no amount of tone-constant tweaking could ever sound mechanical.
    Replaced with a short (~45ms) synthesised click sample (`res/raw/key_click.wav` - broadband noise burst
    plus two fast-decaying resonant partials around 2.2-3.8kHz, generated with a small Python script, not
    sourced/licensed audio) played via `SoundPool` (async-loaded on first use, released in
    `onDetachedFromWindow`) instead of `ToneGenerator`.
  - **D-71** (PatternSeed direction bias): D-68 deliberately left the systematic-offset *direction* at zero
    ("a personal habit with no evidence to seed from"); on reflection this was the wrong default for a
    one-sided pattern specifically - reaching for a far key is a fixed-pivot arc that physically tends to
    undershoot the farther it reaches, which is closer to a physical constant than a personal habit.
    `PatternSeed.shapeFor()` now also seeds `meanDx` (growing with reach, pointed back towards home) and,
    for a thumb reaching the top row, `meanDy` (pointed down towards the home row) - on top of the existing
    spread widening, which is unchanged. Two-thumb typing still gets no directional bias, matching its flat
    spread. New `Shape` data class bundles all four seeded quantities; 5 new/replaced unit tests.
  - **D-72** (number row missing from calibration seeding, bug): `CalibrationActivity`'s embedded preview
    keyboard had `showNumberRow = false`, so the number row's keys never appeared in the
    `charKeyGeometry()` list handed to `PatternSeed.seed()` at all - not a deliberate special-case, just an
    oversight, since the live keyboard shows the number row by default (C-09). Now `true`; no PatternSeed
    changes were needed since its shape formula is already purely geometric per key.
  - **D-73** (typing-pattern reorder + skip-defaults-to-two-thumbs): `TypingPattern` enum and the
    calibration screen's button order are now Both Thumbs, Right Thumb, Left Thumb, Right Index Finger,
    Left Index Finger (by real-world prevalence, author's own left-index-finger pattern deliberately last).
    The "Both Thumbs" button label also gains a "(recommended)" suffix. `CalibrationActivity.applyPattern()`
    was split into `persistPattern()` (the seeding side effects) + the feedback dialog, so the Skip button
    can now call `persistPattern(TWO_THUMBS)` directly (no dialog) instead of doing nothing - skipping no
    longer leaves the model unseeded.
  - **D-74** (stale touch-zone mask after a pattern switch, bug): root cause was **not** in
    `TouchModelActivity` (which does always reload fresh on `onCreate`) but in the long-lived
    `AdaptKeyService`: it holds its own in-memory `offsetModel`, only refreshed when a genuinely new field
    is focused (`reloadOffsetModel()`); if the calibration screen replaces the persisted model on disk while
    the service sits resident without refocusing a field in between, the service's next `onFinishInput` /
    `onDestroy` save silently clobbered the fresh calibration with its stale in-memory copy. New
    `offsetModelPattern` field tracks which pattern the held model was loaded under; new
    `persistOffsetModel()` (replacing both direct `OffsetStore.save()` call sites) checks whether the
    persisted pattern still matches before saving, and adopts the fresh model instead of saving over it when
    it doesn't.
  - **D-75** (vibration still doesn't fire, D-66 follow-up): the D-66 VibratorManager migration alone didn't
    fix it. Further suspect, per the spec's own list of leads: a plain `vibrate(VibrationEffect)` call with
    no usage attributes falls into an unclassified vibration category some OEM vibration-intensity settings
    scale to zero, independently of the (already-bypassed) "touch vibration" toggle -
    `VibrationAttributes.USAGE_TOUCH` (API 33+, Android's own documented category for on-screen-keyboard UI
    feedback) is now requested explicitly where available. **Not confirmed working** - this may turn out to
    be a device/OS-level restriction outside the app's control; needs another device round.
  - No Robolectric/service-level tests added for D-72/D-73/D-74/D-75 (Android view/service/hardware glue,
    consistent with how this layer is tested elsewhere - see D-39); D-69 and D-71 are pure logic with full
    unit-test coverage.
- **§17 D-64 / D-66 DONE (v0.7.32, both bugs - device-verification pending):**
  - **D-64** (suggestion-bar drag-to-trash stopped working): root cause was a touch-arbitration race, not
    the G-04 gesture logic itself (`DragToTrash.isArmed` was untouched and still fully tested/correct).
    `SuggestionBarView` only tried to intercept the drag once it crossed the full `dragThresholdPx` (48dp),
    but its parent `HorizontalScrollView` claims a gesture for its own horizontal scroll at the much
    smaller *system* touch slop (a handful of px) - so on a real device, any natural hand tremor gave the
    scroll view a same-or-earlier chance to steal the gesture, and the 48dp vertical drag could never win
    the race. Fix: intercept as soon as the drag is already vertical-dominant past a new, much smaller
    `interceptThresholdPx` (`ViewConfiguration.scaledTouchSlop`, matching what the scroll view itself
    reacts to), while still gating the visible trash-zone / commit-on-release decision on the original
    48dp `dragThresholdPx` - claiming the gesture early is not the same as arming it.
  - **D-66** (key-press vibration never fires): `AdaptKeyboardView`'s `vibrator` obtained
    `Vibrator` via `Context.getSystemService(Vibrator::class.java)`, which is deprecated from API 31 (S)
    onward in favour of `VibratorManager.getDefaultVibrator()` - the spec's own suspect list named exactly
    this ("VibratorManager vs Vibrator"). Now resolves through `VibratorManager` on API 31+ and the legacy
    path below that, and the `vibrate()` call itself is wrapped in `runCatching` (matching the existing
    `ToneGenerator` pattern in the same file) so a vendor-specific haptics failure can never take down key
    handling. Also fixed a stale doc comment left over from the earlier `performHapticFeedback` approach
    (superseded before D-06/D-34, per the `vibrator` field's own comment) that still claimed the feedback
    "routes through the window system" when the code has gone straight to hardware for a while.
  - No new unit tests - both are Android view/hardware glue over already-tested pure logic
    (`DragToTrash.isArmed`), consistent with how the rest of this layer is tested (see D-39).
- **§18 D-58 / D-62 / D-67 DONE (v0.7.31):** three of the round-7 backlog items.
  - **D-58** (page-change animation): [AdaptKeyboardView] gained a self-drawn slide transition -
    `switchPage(surface, symbolPage, forward)` snapshots the outgoing page's key geometry, swaps to the new
    page, then runs a 180 ms `ValueAnimator` translating the outgoing page off one edge while the new page
    slides in from the other; `forward` (new page arrives from the right) mirrors the actual trigger - the
    D-19 swipe's own direction, "entering the numeric layer" for the combined-key tap/long-press, and
    "page number increases" for the 1/2 toggle key. `AdaptKeyService.setSurface()` grew `forward` /
    `targetSymbolPage` parameters (sensible defaults for ordinary callers) so every surface/page switch now
    routes through the animated `switchPage()` instead of the old instant property swap.
  - **D-62** (mid-word live suggestions): the caret landing inside (or against) an already-committed word
    and typing there previously only ever composed the fragment typed from that point on - autocorrect and
    suggestions never saw the rest of the word. New pure `WordExtent.reclaim(before, after)` (`gesture`
    package, tested) finds the letter-runs touching the caret on each side; `AdaptKeyService.reclaimSurroundingWord()`
    deletes them from the real editable and re-seeds `composing` with the whole word (before + after) the
    moment a new token starts mid-word, so `finalizeAndCommit`'s `typed = composing.toString()` - and every
    suggestion/autocorrect call downstream of it - now sees the complete word, not a fragment. This needed a
    real "logical edit point" inside `composing` for the first time (previously typing always just appended
    at the end): new `composingCursor` (index) / `composingAnchor` (absolute document offset of the
    composing region's start, -1 when no reclaimed tail is in play) fields, a new `insertComposingChar()` /
    position-aware `deleteComposingChar()` pair, and a `updateComposing()` follow-up `setSelection()` call
    (batched with the preceding `setComposingText()` via `beginBatchEdit`/`endBatchEdit` so the app never
    observes the transient end-of-text cursor `setComposingText` alone would produce - which would otherwise
    fail the `onUpdateSelection` "is this our own edit" check and wipe the token mid-update). That check
    itself was generalised from "cursor collapsed at the end of the composing region" to "at the tracked
    edit point," since a deliberate mid-word placement no longer sits at the end. All nine composing-reset
    call sites were consolidated into one `clearComposing()` so the two new fields can never be forgotten at
    a reset. No service-level test added (consistent with the rest of the correction pipeline, see D-39) -
    the new pure logic (`WordExtent`) is tested directly.
  - **D-67** (word-splitting bug, "kleiben"→"klei en" instead of "kleinen"): the A-05 "drop a character"
    split path had no confidence gate at all, unlike the "fully missed space" branch. Generalised the D-48
    "a diacritic restoration beats a split" veto: new `SuggestionProvider.highConfidenceCorrection()`
    (default delegates to `autocorrectFor`) restricts `DictionarySuggestionProvider`'s existing cost-ranked
    candidate search (refactored into a shared `bestCorrection(maxCost)`) to `ADJACENT_SUB_COST` (a single
    neighbouring-key substitution or better, cost ≤ 1) instead of the full `MAX_CORRECTION_COST` (≤ 2)
    autocorrect budget; `AdaptKeyService.finalizeAndCommit()` now vetoes `TokenRepair.trySplit()` whenever
    either the diacritic restoration or this high-confidence correction fires. `b`/`n` are QWERTZ-adjacent,
    so "kleiben" now autocorrects to "kleinen" instead of splitting.
- **D-68 DONE (v0.7.30, big one - device-verification pending):** replaced the three-sentence auto-detected
  calibration (T-04) with an explicit typing-pattern picker that seeds sensible initial per-key touch zones
  directly - the auto-detection could not reliably classify from so little data and, when wrong, seeded
  zones badly with no way to recover quickly (the offset model has no forgetting mechanism).
  - **`TypingPattern`** now has 5 real patterns (`LEFT_INDEX_FINGER`, `RIGHT_INDEX_FINGER`, `LEFT_THUMB`,
    `RIGHT_THUMB`, `TWO_THUMBS`) + `UNKNOWN` (not chosen yet); the old single `THUMB` catch-all is gone.
  - **New `PatternSeed`** (`touch` package, pure/tested - 11 new tests): for a one-sided pattern, seeds each
    key's spread tight near that hand's "home" third, widening (and deliberately smearing into the
    neighbouring key) toward the far side; thumbs start less precise even at home and widen further, **plus**
    extra widening the closer a key is to the top row (a thumb's hardest reach is the far top corner, not
    just sideways); `TWO_THUMBS` is flat and uniformly wider with no positional skew at all. The systematic
    offset *direction* is deliberately left at zero for the seed - only real usage should discover whether a
    user over- or undershoots; only the *spread* is seeded, since that is directly grounded in the pattern's
    geometry. Seeded at a sample count just above the model's warmup threshold, so it's trusted immediately
    but still readily overridden by a user's first real taps.
  - **New `OffsetModel.rankedCandidates`** (added for D-39) is reused implicitly via the seed's construction
    (`OffsetModel.restore`); no change to `OffsetModel` itself beyond that existing addition.
  - **`CalibrationActivity`** fully rewritten: five pattern buttons instead of a 2-3 sentence typing
    exercise; picking one calls `PatternSeed.seed()` + `OffsetModel.restore()` and **replaces** (not merges)
    the persisted model via `OffsetStore.save()`, then presets the D-16 key-enlargement default
    (`SettingsStore.applyPatternEnlargement`, extended for the two new thumb patterns) and shows the D-24
    touch-zone visualisation as immediate feedback. Reachable from onboarding (unchanged step-sequencing,
    only the body text changed) and any time later via Settings → "Typing pattern" (the `t04_detected` row,
    retitled from "Detected typing pattern" to a tap target).
  - **Removed entirely** ("ersatzlos" per explicit instruction): `TypingPatternClassifier`,
    `TypingPatternAnalysis` (+ their tests), and `AdaptKeyService.persistTypingPattern()` (the live
    re-classification on every field focus/destroy). The dead `t04_pattern_override` `ListPreference` (never
    actually wired to anything, confirmed by grep) and its arrays are gone too.
  - **Side effect, flagged as a follow-up (task #68):** D-09 raw-tap recording had no sentence exercise left
    to record against, so its settings toggle was removed from the UI; the underlying `RawTapRecorder` /
    `CalibrationSentences` / `CalibrationSession` classes and the `recordRawTaps` setting plumbing were
    deliberately left in place (unused but harmless, still compiling/tested) rather than torn out under time
    pressure - needs a decision (delete, or repurpose to record during live typing instead).
  - Onboarding welcome text, onboarding calibration step, and all `k01_*`/`t04_*` strings reworded (EN/DE/EL)
    for the new "tell us your pattern" framing.
  - **Not yet verified on a real device** - the shape-function constants (home fractions, spread factors,
    top-row gain) are a considered starting point per the design discussion, expected to need tuning after
    a first real test (user has a known strong left-hand rightward-edge drift to check against).
- **D-49 DONE (v0.7.29):** the onboarding welcome text (EN/DE/EL) now foregrounds D-39 as the headline USP -
  "reads your actual taps, not just your typing" / recovers a word even when a slip landed on a completely
  wrong key, not just a neighbouring one - and the "learns as you go" bullet now explicitly says the
  continuous learning (T-03/T-04, D-37) is gentle/damped: only a steady pattern moves anything, never a
  single slip. Spec D-49 updated to match.
- **D-39 DONE (v0.7.28, the USP):** raw-coordinate per-character correction. New `TapPoint(x,y)`
  (`touch` package) retains each composing character's raw `ACTION_DOWN` (T-02); `AdaptKeyService` now keeps
  a `composingTaps` list in lockstep with `composing`/`composingFlags` (mutated at every site that mutates
  `composingFlags` - clear/add/removeAt). New `OffsetModel.rankedCandidates(candidates, x, y)` exposes the
  model's full best-first ranking with a comparable score (log-likelihood after warmup, negated squared
  distance during warmup) - a pure addition alongside `resolve()`, which still only returns the single best
  match. New pure `RawCoordinateCorrection.respellings(token, taps, keyCandidates, offsetModel)` (`suggestion`
  package): for each character position, finds the *runner-up* key under the tap's actual raw position (not
  the static QWERTZ adjacency map D-28/D-38/D-41 already use) and proposes a one-position substitution,
  returning all respellings ordered by how close the runner-up came to beating the key that was actually
  chosen. Wired into `finalizeAndCommit` as a **fallback**: only tried when the token is not already a known
  word (A-01 preserved explicitly) and the ordinary edit-distance `autocorrectFor` found nothing; the first
  respelling that is a known, non-blacklisted word wins. This recovers slips where the tap was genuinely
  ambiguous between two keys and the wrong one was picked - something the static adjacency map cannot see,
  since it never looks at where the tap actually landed. Heavily garbled tokens (many typos at once) are left
  to tier-3, unchanged, per spec. 12 new unit tests (`OffsetModelTest`, `RawCoordinateCorrectionTest`); no
  service-level (Robolectric) test added - `rawCoordinateCorrection()` in `AdaptKeyService` is thin glue over
  the two pure/tested units, consistent with how the rest of the correction pipeline is tested.
- **Spec §17 — popup-overlap + paste + Enter (v0.7.27):** **D-53 addendum** popups now draw *above* the key
  and overlap the suggestion bar instead of flipping below - the service sets `clipChildren=false` /
  `clipToPadding=false` on the keyboard container + root so the keyboard's popup (drawn after the bar) is not
  clipped and lands on top. **D-54** nudge reduced to `dp(5)` (a half-cell was too much). **D-60** Quick Paste
  now commits the clipboard text directly (`ic.commitText`) then clears, instead of firing
  `performContextMenuAction(paste)` which raced the clear and left fields empty. **D-61** Enter honours the
  editor IME action: multi-line fields still insert `\n`; single-line fields commit the pending word then
  `performEditorAction(Go/Search/Send/Done)`, or send a real `KEYCODE_ENTER` when the field declares no action
  (browser address bar / YouTube search now submit). **D-66** (NEW backlog) key vibration still does not fire
  on device - re-investigate the haptic path. **Still open in §17:** D-58 (page-change animation), D-62
  (mid-word live suggestions), D-64 (suggestion drag-to-trash broken).
- **Spec §18 (round 7) = NEW backlog, captured, not started** (D-67, D-68 + a follow-up task): **D-67** (bug)
  word splitting fires too eagerly - `kleiben`→`klei en` instead of autocorrecting to `kleinen`; root cause
  confirmed in code: `TokenRepair.trySplit`'s drop-a-character branch (`OVER_SPACE_LETTERS = c,v,b,n,m`) has
  no bigram/frequency confidence gate at all, so a split can beat a much stronger single-word correction -
  needs the D-48 veto generalised (prefer a low-cost autocorrect candidate over a split). **D-68** (big idea,
  explicitly deferred) rethink T-03/T-04 calibration: the three-sentence calibration misdetected the user's
  typing pattern outright and visibly skewed touch zones; proposed direction is an explicit onboarding
  question about typing pattern instead, deriving initial per-key zones directly from the chosen pattern
  (tight/centred near the typing hand's home third, widening/smearing toward the far side; symmetric for
  two-thumb), with any later refinement from real usage made much more sluggish than today. Not to be
  implemented without a separate design pass. Also noted: recheck whether a suspected notes-app-specific
  Enter special-case is still needed now that D-61 is in - searched the codebase, found no such special-case
  currently in `AdaptKeyService`, so this needs the user's pointer to the specific app/behaviour next time.
- **Spec §17 — Geometry DONE (v0.7.26):** **D-53** the number row now long-presses like every key - the
  digit popups showed nothing because the popup was drawn *above* the key, off the top of the view; `openPopup`
  now flips the row **below** the key when there is no room above (so top-row/number-row popups are visible).
  **D-54** single-cell popups are nudged ~a half-cell (`popupSingleNudgePx`) towards the keyboard centre
  (right on the left half, left on the right half) so the finger does not cover them; multi-cell popups stay
  centred (D-44). **D-55** two new settings insert extra vertical spacing below the number row and above the
  space row (`d55_space_below_number_row` / `d55_space_above_space_row`, default 7, slider 0-25, clamped in
  `SettingsMapper`; view sums per-row extra gaps in `onMeasure`/`layoutKeys`). **D-59** the combined `?123`
  key can be disabled (`d59_symbol_key`, default on); with it off **and** the emoji panel off the key is drawn
  blank and its slot is inert but reserved (`isHiddenSymbolKey`, `resolveKey` returns null on it). Full
  settings plumbing (AdaptSettings/RawSettings/SettingsMapper/SettingsStore/prefs XML + EN/DE/EL strings)
  and `applySettings` wiring; new `SettingsMapper` clamp/passthrough tests.
- **Spec §17 — D-65/D-63 DONE (v0.7.25, HIGH):** root cause of `konnen`→`kannen` was **candidate
  reachability**, not cost ranking: the SQLite `correctionCandidates` bucket query had a `LIMIT` but no
  `ORDER BY`, so rows came back in `wkey` order; German umlaut letters (ö = U+00F6 …) sort after all of a-z,
  so a common umlaut word (`können`) fell past the per-bucket LIMIT while a rare shape-alike (`kannen` =
  Kannen) survived - and cost ranking never saw `können`. Fix = **`ORDER BY freq DESC`** before the LIMIT, so
  high-frequency umlaut words stay reachable. This fixes D-65 and satisfies D-63: `mussen`→`müssen`,
  `hoflich`→`höflich`, `weis`→`weiß` auto-correct (fold-distance in budget, now reachable); `konnten` only
  *suggests* `könnten` because `konnten` is itself known (A-01 already blocks auto-correcting a valid word).
  New Robolectric guard seeds 400 filler `k…` words to overflow the bucket LIMIT and proves `können` is only
  reachable with the frequency ordering (verified failing without the fix). No schema/DB-version change.
- **Spec §17 — Quick Wins DONE (v0.7.24):** **D-51** period popup order → `! . ?` (full stop still centred);
  **D-52** number row is full key height again (reverted D-42: removed `numberRowHeightPx`/`rowHeight()`,
  `onMeasure`/`layoutKeys` back to a single `rowHeightPx`); **D-56** backspace single-char phase a touch
  quicker (`CHAR_START_DELAY_MS` 190→165, `CHAR_MIN_DELAY_MS` 65→50; word-wise unchanged); **D-57** horizontal
  page swipe -15% (`PAGE_SWIPE_FACTOR`), space-bar language swipe +15% (`SPACE_SWIPE_FACTOR`), vertical
  swipe-down/-up untouched (`resolveSwipe` now branches per gesture).
- **Spec §16 — D-43 DONE (v0.7.23):** next-word prediction fills the (otherwise empty) bar after a commit -
  new `DictionaryStore.nextWords(prev, limit)` (bigram successors, canonical case; InMemory + SQLite JOIN),
  `SuggestionProvider.nextWordSuggestions(prev)` (blacklist-filtered, count-scored), and
  `AdaptKeyService.showNextWordPredictions()` wired into every commit path (finalize, verbatim, split, merge,
  suggestion-tap, autocorrect-undo). Baseline is tier-1 bigrams; tier-3 refines the moment typing resumes.
  Fixed a latent bug found en route: `InMemoryDictionaryStore.bigramKey` joined halves with a literal NUL
  byte (`" "`), so the new prefix scan missed them - now a shared `BIGRAM_SEPARATOR = " "` constant.
- **Spec §17 (round 6) = NEW backlog, captured, not started** (D-51…D-65). Device feedback on v0.7.22/
  v0.7.23; alt-key popups (D-44) praised. Items: **D-51** period order → `! . ?`; **D-52** revert D-42 (number
  row = full key height again); **D-53** number row must long-press like every key (show its shifted-symbol
  alt popup - currently none); **D-54** single-alt popups nudged ~2 units toward keyboard centre (right on
  left half, left on right half); **D-55** two new settings: extra space below number row + above space row,
  default 7, slider 0-25; **D-56** backspace single-char phase a touch faster; **D-57** page-swipe ~15% less,
  but space-bar swipe ~15% more (swipe-down unchanged); **D-58** page-change slide animation (like swipe-down);
  **D-59** optional `?123` key (default on; when off AND emoji off → button gone but slot reserved); **D-60**
  (bug) Quick Paste does nothing - clipboard likely cleared before paste; **D-61** (bug) Enter must honour the
  editor IME action (Go/Search/Send) in address bars / search fields, not emit a newline; **D-62** mid-word
  editing must give live whole-word suggestions (+ LLM context); **D-63** missing umlaut/ß → suggest up front
  + auto-correct when unambiguous (`mussen`→`müssen`, but `konnten` only suggests `könnten`); **D-64** (bug)
  suggestion-bar word can no longer be dragged to trash; **D-65** (bug, HIGH) `konnen` auto-corrects to
  `kannen` (must be `können`) - the D-63 case, likely a D-41/D-48 ricochet. D-63/D-65 are the umlaut-first-
  class core-comfort theme; D-39 (raw-coordinate correction) still deferred to Saturday.
- **Spec §15 (round 4) status:** DONE = D-30 freeze bug, D-31 backspace speed, D-32 long-press delay+setting,
  D-33 popup bottom-align, D-34 vibration, D-35 swipe thresholds, D-38 correction quality, D-36 direct paste,
  D-40 digit-in-word, D-37 less-eager learning. **OPEN = only D-39** raw-coordinate per-character correction.
- **Spec §16 (round 5) — Batch A DONE (v0.7.21):** **D-42** number-row shorter than letter rows so its
  touch zone no longer bleeds down (per-row `rowHeight()`, `numberRowHeightPx = dp(44f)` vs `dp(54f)`;
  `onMeasure`/`layoutKeys` now sum per-row heights); **D-45** re-arm auto-capital when a backspace leaves the
  cursor back at a sentence start (`handleBackspace` else-branch); **D-46** field swipe must travel ~3
  key-widths (`3 * width/10`, floored at the old fixed threshold) in `resolveSwipe`; **D-47** combined key
  drops the 😊 glyph → reads `?123` when emoji off (`emojiEnabled` on the view, pushed from `applySettings`,
  corner hint suppressed); **D-50** suggestion bar stays permanently `VISIBLE` even when empty (no layout
  jump).
- **Spec §16 — Batch B/D-44 DONE (v0.7.22):** **D-41** the number row is now part of `KeyboardProximity`
  (four rows), so a digit is an ordinary neighbour character (`8` sits above the `i`/`o` gap → `W8rt` is a
  cheap `8`→`o` slip from `Wort`); **D-48** a token that is a real word once its German diacritics are
  restored is first-class and vetoes the A-05 split — new `SuggestionProvider.diacriticRestoration()`
  (fold-equal known word) is checked before `trySplit`, so `konnen`→`können`, never `ko nen` (also `russ`→
  `ruß`); **D-44** the long-press popup is now **horizontal**: a row of cells centred over the stem, the
  key's own glyph pre-selected, finger slides left/right **below** the row to pick (new pure-geometry
  `HorizontalLongPressPopup` replaces `VerticalLongPressPopup`; period `? . !`, comma `- , : ; / _`).
  **Still open in §16:** **D-43** next-word prediction (bigram baseline + LLM), **D-49** raw-recording as a
  headline onboarding USP. The big architectural item **D-39** (raw-coordinate correction) is the confirmed
  next focus.
- **§13 round-2 status:** DONE across v0.7.8/v0.7.9 = K-01 inset, D-11/D-12 suggestions, D-15 Caps Lock,
  D-19/D-20 swipes, **D-04 flash speed, D-14 long-press popup feedback, C-04 defaults, D-21 cell padding,
  D-07 faster hold, A-07 split-undo**. STILL OPEN in §13: D-13 (word training), D-16 (pattern-driven key
  enlargement), D-17 (onboarding USP text), D-18 (emoji panel toggle), D-07 last-word-of-line (device repro).
- **Spec §14 (device-feedback round 3, v0.7.8 testing) = NEW backlog, captured, not started.** Precision
  keeps improving. Refinements: D-04 flash *still* too slow (→ shortened again this round), D-05/D-06
  sound+haptic don't fire on device (bug), C-04→**D-25** colour the text not the background, T-04/K-01
  calibration produced UNKNOWN (must reliably report a result). New: **D-22** punctuation reorg (period=.!?,
  comma=,;:-_/), **D-23** vertical long-press popup (primary top-left preselected, alts stacked bottom→top
  above the finger), **D-24** touch-pattern visualisation (coloured circles at expected strike point),
  **I18N** localise app strings (EN+EL+DE, system language), **D-26** mid-word edit wrongly colours the
  correction chars (bug), **D-27** space-bar top edge registers the key above / c-v (bug, HIGH), **D-28**
  proximity + distance-2 correction (komplezz→komplett), **D-29** punctuation after an accepted suggestion
  eats the trailing space. Tasks #18–#26 mirror these.
- **A-05 split (v0.7.4):** a character is dropped only when it is a T-05 flag OR a letter over the space bar
  (`TokenRepair.OVER_SPACE_LETTERS` = c/v/b/n/m) — works without calibration; missed-space keeps the
  co-occurrence bigram gate. **Reset switch (v0.7.5):** `OffsetStore.clear()` + a two-dialog (first + final)
  confirm in Settings → Kalibrierung resets the offset model (T-03) + typing pattern (T-04); not the dictionary.
- **Env note:** the user granted standing permission (2026-07-05) for build/SDK/tests/`git add`/local commits
  — do them without asking; a broad allowlist was added to `/d/workspace-ai/.claude/settings.local.json`
  (gitignored, outside the repo). Only `git push` still prompts (outward-facing).
- **Device round 2 fixes (Pixel 9a):** autocorrect "word-hacking" (A-05 missed-space split now requires a real
  co-occurrence bigram, `TokenRepair.MIN_SPLIT_BIGRAM` — killed "Luste"→"Lu ste"); **mid-sentence caret jump**
  (added `onUpdateSelection` → finish composing + reset token state on user caret moves); **suggestion bar was
  completely missing** (dropped the legacy `onCreateCandidatesView`/`setCandidatesViewShown`; the strip is now
  EMBEDDED as a 44dp row above the keyboard in the input-view root, toggled visible when there are items); ß
  long-press on s; D-08 (deleting whitespace after a capital shifts to lowercase). Spec §12 (D-01…D-10) captures
  the remaining feature requests.
- **Spec §12 D-series (device-feedback round 1):** **COMPLETE.** v0.7.6: D-03 space bar shows language, D-04
  space/special-key tap flash, D-07 accelerating backspace-on-hold, D-10 backspace at start of entry.
  v0.7.7: D-01/D-02 multi-alternative long-press popup + full-stop punctuation list, D-05/D-06 optional
  key sound + haptics, D-09 raw-tap recording. All D-items need on-device confirmation (no emulator here).
- **Spec §13 (device-feedback round 2, v0.7.7 testing) = the NEW open backlog.** Precision is now much
  better. Captured in spec §13:
  - **Refinements/bugs:** K-01 calibration screen missing the gesture-bar inset (blocker — couldn't
    calibrate); D-04 flash far too slow (double-tap invisible → shorten); D-07 too slow + word-wise delete
    stops before the line's last word; A-07 undo doesn't revert an A-05 split; C-04 highlight should default
    ON with a lighter green.
  - **New features D-11..D-21:** D-11 earlier/more-frequent suggestions (after 1–2 letters); D-12 fuzzy +
    umlaut-aware candidates (mut→mit, grun→grün, Defaukt→Default; and don't highlight nonsense as known);
    D-13 user word training / add-to-dictionary (stop "Backspace"→"Back Space"); D-14 in-keyboard long-press
    feedback (flash/popup, not just haptic); D-15 double-tap Shift = Caps Lock; D-16 pattern-driven default
    key enlargement (left→backspace, right→Shift, adjustable); D-17 expand onboarding USP text; D-18 emoji
    panel toggle (default on, off ⇒ combined key = ?123 only); D-19 full-field swipe to switch surfaces/pages
    (separate from the space-bar language swipe); D-20 larger gesture thresholds for dismiss + page swipe
    (space-bar swipe stays small); D-21 key cell padding (Gboard-like).
  - Also still open from round 1: confirm the embedded suggestion bar on device; LLM decode loop unverified.
- **Device-feedback fixes (Pixel 9a):** typing-lag (autocorrect no longer scans all 120k words —
  `DictionaryStore.correctionCandidates`, SQLite indexed; a Robolectric test caught a text-vs-int BETWEEN
  bug); edge-to-edge insets (keyboard padded above the gesture pill / IME-switch); **umlaut long-press**
  a/o/u→ä/ö/ü (+ß in the C-08 palette); **backspace shift-restore** (Addendum G-05, deleting an uppercase
  char re-arms Shift); **first-run onboarding** panel above the keyboard (`onboarding/` — pure `Onboarding`
  steps + `OnboardingStore` + `OnboardingView`; WELCOME→MODEL_IMPORT→CALIBRATION; shown on first keyboard
  use, re-showable via an Info-category preference — fixes that the calibration offer never appeared). All
  need on-device confirmation.
- **Next (device-only-verifiable UI):** Gboard-style long-press popup for keys with *multiple* alternatives
  (finger-tracking selection; single-alternative keys keep the immediate-apply behaviour).
- **On-device fixes (Pixel 9a testing):** (1) typing lag was `autocorrectFor` scanning all ~120k words per
  keystroke on the main thread → new `DictionaryStore.correctionCandidates` (SQLite: indexed first-char
  range + length±1; default = whole lexicon), provider filters edit-distance before any DB query; a
  Robolectric SQLite test caught a text-vs-integer `BETWEEN` bug (would've made autocorrect return nothing)
  — fixed by inlining integer bounds. (2) edge-to-edge (targetSdk 35): the gesture pill / IME-switch button
  overlapped the bottom row → `onCreateInputView` pads up by the navigation-bar/gesture inset. Both need
  on-device confirmation.
  Debug APK ~43 MB (onnxruntime native libs, arm64-v8a + armeabi-v7a only).
- **Robolectric** now runs Android glue on the JVM here (no emulator — this environment has no hardware
  virtualization, `HyperVisorPresent=False`, so an Android emulator cannot boot). JUnit4 Robolectric tests
  run via the vintage engine alongside Jupiter; `unitTests.isIncludeAndroidResources = true` for assets.
  Covers: bundled `tier3/` tokenizer assets → reference-matching tokenizer end-to-end
  (`Tier3TokenizerLoaderRoboTest`); `SettingsStore` prefs IO + clamp (`SettingsStoreRoboTest`); model
  install/storage lifecycle (`Tier3ModelStorageRoboTest`). Native ONNX inference stays device-only.
- **Licensing/meta:** project is **GPL-3.0-or-later** (`LICENSE` = verbatim GPLv3); every `.kt` file has an
  SPDX header (`SPDX-License-Identifier: GPL-3.0-or-later` + `Copyright (C) 2026 Froehlich Media`);
  `README.md` leads with the *provably-offline* story (no `INTERNET` permission — manifest-verifiable);
  `THIRD-PARTY-LICENSES.md` lists code libs; `CREDITS.md` covers bundled data. A settings "Info &
  Datenschutz" category (shown first — the launcher/IME settings Activity is the one screen a keyboard has)
  states the no-internet/no-tracking guarantee + the licence.
- Architecture rule in force: pure, Android-free logic (recognition / thresholds /
  policy) lives in its own fully unit-tested classes; the Android layers
  (Activity / View / Service / SQLite DAO / SettingsStore IO) stay thin and are
  earmarked for instrumented tests.

## Done

### §15 D-37 less-eager learning (v0.7.20)
- **Count-based promotion:** new `dictionary/PendingLearnStore` (own private SharedPreferences file — no
  SQLite migration). `learnWord` now reinforces a word already in the dictionary immediately, but a
  genuinely **new** word is only counted up and promoted to the learned lexicon after `LEARN_THRESHOLD` (=2)
  commits — so a one-off typo (e.g. "asdf") is no longer learned as a real word on the first accept.
- **Un-learn on undo:** an A-07 autocorrect-undo decrements the rejected correction's pending count and
  counts up the word the user insisted on (promoted after repeated insistence). A **split**-undo (D-13) still
  promotes the rejoined word **immediately** via the new `learnWordStrong` (a deliberate correction is
  authoritative — so "Backspace" is still trained in one undo).
- Robolectric `PendingLearnStoreRoboTest` covers the increment/decrement/clear IO.
- **Only §15 item left: D-39** (raw-coordinate per-character correction) — a larger architectural piece.

### §15 D-36 direct paste + D-40 digit-in-word (v0.7.19)
- **D-36 direct paste:** when a field opens and the clipboard holds text, a 📋 chip appears in the suggestion
  bar (`SuggestionController.Kind.CLIPBOARD`); tapping it runs the **exact system paste**
  (`ic.performContextMenuAction(android.R.id.paste)`), then **clears the clipboard** (esp. passwords). Sensitive
  content is masked (`ClipDescription.EXTRA_IS_SENSITIVE`, API 33+) — shown as bullets, never revealed. Pure
  `suggestion/ClipboardPreview` (mask/truncate/collapse, unit-tested); typing replaces the chip with normal
  suggestions.
- **D-40 digit-in-word:** a digit typed between letters (composing non-empty) now stays in the token instead
  of delimiting, so existing correction fixes it — `W8rt` → `Wort` (the digit is one substitution from the
  neighbouring letter). A leading/standalone digit keeps its normal behaviour.
- **Rescoped from D-38 note:** the user pushed back — `W8rt` IS handled now (D-40), and `Stabdsrx` (multi-typo)
  is captured as **D-39** (raw-coordinate per-character correction, walking each char's retained raw tap to the
  intended neighbour key; very garbled cases may go to the tier-3 LLM). D-39 still open.

### §15 D-38 correction quality: first-char / umlaut-initial / cost-ranked (v0.7.18)
- **First-char + umlaut-initial candidates:** `DictionaryStore.correctionCandidates(token, firstChars)` (new
  overload; SQLite searches one indexed bucket per first char) + provider `candidateFirstChars` = the token's
  own initial letter plus its keyboard neighbours and, for a/o/u, the umlaut variant. So a first-key typo
  (`eerden`→`werden`) and a missing initial umlaut (`Uberblick`→`Überblick`) are now reachable.
- **Cost-ranked autocorrect:** `autocorrectFor` now ranks by lowest proximity-weighted edit cost first,
  frequency only as a tiebreak (`correctionCost` extracted from `isCloseMatch`), so `dasy`→`dass` (one
  adjacent edit) beats the far more frequent `das` (a deletion).
- Still open in §15: **D-36** direct paste (feature), **D-37** less-eager count-based learning + un-learn on
  undo. Heavily-garbled inputs (`Stabdsrx`→`Standard`) and digit-in-word (`W8rt`→`Wort`, a tokenisation
  issue) remain beyond the current budget/scope — noted.

### §15 round-4 bugs: D-30…D-35 (v0.7.17)
- **D-30 (critical bug) keyboard freeze after held backspace:** `backspaceRepeated` was only reset on a
  DELETE press, so after a hold it stayed true and the ACTION_UP tap-guard (`!backspaceRepeated`) swallowed
  **every** subsequent tap (incl. backspace). Fixed by resetting it on every ACTION_DOWN. (This was the real
  cause of the earlier "last word of the line" report — the whole keyboard was frozen.)
- **D-31 backspace-hold speed:** `BackspaceRepeat` reworked — char-wise now starts at 190 ms and accelerates
  only **moderately** (factor 0.90) to a 65 ms floor; word-wise runs at a clearly **slower** fixed 330 ms.
  The service drives the cadence: `onBackspaceRepeat(step)` now returns the next delay, computed from the
  running deletion count, so the char→word transition is a visible slow-down.
- **D-32 long-press delay + setting:** the view's `longPressDelayMs` is settable (default 80 % of the system
  timeout = ~20 % shorter). New `d32_longpress_delay_ms` slider (150–600, default 320) through the settings
  pipeline + strings ×3.
- **D-33 popup primary bottom-aligned:** the offset primary cell now sits level with the bottom of the
  secondary column (near the finger) instead of top-left.
- **D-34 vibration:** the haptic pulse was 18 ms (imperceptible) → 40 ms; VIBRATE is a normal auto-granted
  permission (no runtime step). Now actually felt.
- **D-35 swipe thresholds:** raised markedly — field gestures (dismiss-down / page swipe / word-delete)
  64→**110 dp**, space-bar language swipe 28→**44 dp**, so a faint motion no longer triggers them.

### Nice-to-haves: persist language / Greek diaeresis / language-aware blacklist (v0.7.16)
- **Persist active language (G-01):** new `language/ActiveLanguageStore` (own private prefs file); the
  service loads it in `onCreate` and saves it on each `toggleLanguage`, so the chosen alphabet
  (German/Greek) survives a service restart.
- **Greek diaeresis (ϊ / ϋ):** now that the long-press popup is multi-alternative (D-23), the ι and υ keys
  carry `[tonos, diaeresis]` (`GreekLayout.DIAERESIS`, new `letterKey` helper): tonos is the pre-selected
  primary, diaeresis the cell above. Other vowels keep the single tonos.
- **Language-aware blacklist editor (C-05):** the editor opened the legacy `adaptkey_dictionary.db` instead
  of the per-language DBs the keyboard actually uses — so edits had no effect. Now a language selector
  (DE/EN/EL) reopens the matching store via the now-public `DictionaryLoader.databaseName(language)`, so the
  blacklist is edited in the store the keyboard reads.

### §13 finish: D-13 / D-18 / D-17 / D-16 (v0.7.15)
- **D-13 user word training:** undoing a wrong A-05 split (the A-07 backspace) now **learns** the rejoined
  word (`undoWasSplit` flag → `learnWord` in `performAutocorrectUndo`), so a real word the splitter mangled
  (e.g. "Backspace" → "Back Space") is trained and never split/autocorrected again (A-01). Discoverable via
  the existing backspace-undo flow; typing + one undo teaches the word.
- **D-18 emoji panel toggle:** setting `d18_emoji_panel` (default on) through the settings pipeline + a
  Layout-category switch. When off, `PanelNavigation.onCombinedKeyTap(current, emojiEnabled=false)` makes the
  combined key a pure ?123 toggle (letters ↔ symbols), no emoji panel.
- **D-17 onboarding USP text:** expanded the welcome body (all 3 locales) with the strongest USPs — provably
  offline / no-internet-permission, per-finger adaptation + calibration, smart neighbour-key/umlaut correction
  (komplezz→komplett) + space repair, learns-as-you-go, DE/EN/EL dictionaries, optional on-device mini-AI,
  GPLv3 no-ads-no-accounts.
- **D-16 pattern-driven key enlargement:** `KeyProportions` gained `shiftBaseWeight` + `shiftExtra` (computed
  `shiftWeight`, mirroring backspace); `thirdRowLetterWeight` now takes both surcharges from the letters so
  the row width is preserved. New `c01_shift_extra` slider (settings pipeline + pref + strings ×3). After a
  calibration, `SettingsStore.applyPatternEnlargement` presets the enlargement from the detected hand:
  left-index → enlarged backspace, right-index → enlarged shift (THUMB/UNKNOWN untouched); user-adjustable.
- **Only §13 item left open:** D-07 "word-wise delete stops before the line's last word" — not reproducible
  from the pure logic; needs on-device repro.

### Round-3: I18N — English default + German/Greek locales (v0.7.13 / v0.7.14)
- **§14 I18N:** all app-chrome strings localised. **English is the default** (`res/values/strings.xml`), so
  any unknown system language falls back to English; German is `res/values-de/`, Greek `res/values-el/`.
  (v0.7.13 first shipped German-as-default with a `values-en`; v0.7.14 flipped it to English-as-default per
  user requirement.) The C-04 colour-label array is localised too (`values*/arrays.xml`). ~90 strings each
  (settings, onboarding, dialogs, calibration, D-24). No hardcoded UI literals remain (the only in-code
  string is the language-name space-bar/toast label, an intentional endonym). This completes spec §14.

### Round-3: calibration reliability (T-04/K-01) + D-24 touch-model visualisation (v0.7.12)
- **T-04/K-01 calibration now produces a result:** the live classifier is deliberately conservative
  (lateral threshold 0.18) so a real-but-modest hand bias fell through to UNKNOWN. Added
  `TypingPatternClassifier.forCalibration()` — a decisive preset (lateral threshold 0.05, min 20 taps) used
  only by `CalibrationActivity.detectPattern`; live info-only detection stays conservative. Also lengthened
  `CalibrationSentences` 3→5 (more taps → stabler bias). Result is stated plainly via the existing
  pattern-specific feedback strings.
- **D-24 touch-model visualisation:** pure `OffsetModel.spreadFor(id)` (mean offset + per-axis std dev);
  `AdaptKeyboardView.showTouchModel` overlay draws a translucent spread ellipse + a strike-point dot over
  each trained char key. New `TouchModelActivity` (+ layout, manifest, strings) hosts a keyboard with the
  overlay over the persisted model; reachable from a new settings entry (Kalibrierung category) and as the
  calibration result (the feedback dialog's neutral button → visualisation, or → D-09 export when recording).

### Round-3 popup rework: D-22 punctuation split + D-23 vertical popup (v0.7.11)
- **D-22:** the full-stop key now carries only the sentence terminators (`KeyboardLayout.PERIOD_ALTERNATIVES`
  = `. ! ?`); the comma key carries the clause punctuation (`COMMA_ALTERNATIVES` = `, ; : - _ /`). Index 0 of
  each is the key's own char (the primary). Both Latin and Greek layouts.
- **D-23 vertical popup:** replaced the horizontal `LongPressPopup` with pure `VerticalLongPressPopup`
  (y→index). The popup now draws the primary (index 0) as a cell offset at the top-left (pre-selected) and
  the secondaries stacked in a column directly above the finger, bottom-to-top (so on the period key `!` is
  directly above the finger and `?` above it; on the comma key `,` is the top-left default and `; : - _ /`
  run bottom-to-top). Selection follows the finger's vertical position (`updatePopupSelection(event.y)`); a
  single-alternative key (umlaut/AltGr) shows one preview cell above the key. Cells are clamped into the view.

### Round-3 bugs + quality: D-27 / D-05-06 / D-25-26 / D-29 / D-28 (v0.7.10)
- **D-27 space-bar top edge (bug):** `resolveKey` now short-circuits to SPACE when the raw point is inside
  the space-bar rect, so the offset model can't pull a clear space tap up to c/v. T-05 flagging unchanged.
- **D-05/D-06 sound + haptics (bug):** the toggles were silenced by the system touch-sound/vibration
  settings. Now authoritative: sound via a short `ToneGenerator` click, haptic via the `Vibrator` directly
  (added the `VIBRATE` permission — normal, non-privacy; the no-INTERNET/no-storage guarantee is unchanged).
  ToneGenerator released in `onDetachedFromWindow`.
- **D-25 text colour / D-26 mid-word bug:** the recognised-word highlight is now a `ForegroundColorSpan`
  (colours the text, not the background); default colour reverted to a readable medium green `#2E7D32`.
  `shouldHighlightComposing` skips the colour when a letter follows the cursor (mid-word edit), so the two
  correction characters are no longer coloured.
- **D-29 punctuation eats accepted-suggestion space:** accepting a suggestion arms `pendingSuggestionSpace`;
  the immediately following sentence/clause punctuation (`SPACE_EATING_PUNCTUATION` = `.,!?;:)`) deletes the
  auto-added trailing space. One-shot — cleared as soon as a letter is typed or on a new field; spaces
  before typed punctuation are never stripped in general.
- **D-28 proximity + distance-2 correction:** new pure `suggestion/KeyboardProximity` (QWERTZ adjacency map)
  + `EditDistance.weightedDistance` (weighted Levenshtein). The provider's fuzzy/autocorrect matching now
  accepts a candidate up to a total cost of 2, where a neighbouring-key substitution costs 1 and any other
  sub / indel costs 2 — so `komplezz`→`komplett` (two adjacent z→t slips) is caught, while two unrelated
  edits are rejected. Distance-1 behaviour is unchanged. **Works without the LLM** (the answer to the user's
  question: tier-3 adds context prediction on top, but typo correction must and does work offline).

### Round-2 slice 2: D-04 / D-14 / C-04 / D-21 / D-07 / A-07 (v0.7.9)
- **D-04 flash (shortened again):** `flashDurationMs` 80→45→**28 ms** (Gboard-like; §14 D-28 still flagged
  it as too slow after 45, so this round goes to 28 — device-tune further if needed).
- **D-14 in-keyboard long-press feedback:** the long-press popup now shows for **single-alternative** keys
  too (umlauts, ß, AltGr, Greek tonos) — a one-cell preview bubble that confirms the press and commits on
  release, Gboard-style. `AdaptKeyboardView.popupAlternativesFor(key)` = `key.alternatives` else the single
  `key.hint`; `openPopup(key, alternatives)`; only a key with no secondary (the ?123 key) falls back to the
  listener action. (The vertical layout + punctuation reorg is the separate §14 D-22/D-23, not this.)
- **C-04 defaults:** recognised-word highlight now defaults **on** (`SettingsStore` default + pref
  `c04_highlight_enabled` true), default colour a lighter green `#FF81C784` (`SuggestionConfig.DEFAULT_
  HIGHLIGHT_COLOR`, arrays green preset, pref default). NB §14 D-25 supersedes this to colour the *text*
  not the background — next round.
- **D-21 key cell padding:** `gapPx` 3→**5 dp** between keys.
- **D-07 faster backspace hold:** `BackspaceRepeat` tuned — INITIAL 400→260, START 200→120, MIN 45→28,
  ACCEL 0.82→0.78. (The word-mode "stops before the line's last word" report still needs device repro.)
- **A-07 split-undo:** a backspace immediately after an A-05 retroactive split now rejoins the two words
  into the originally typed token — `applySplit` takes the typed token and arms the existing
  `undoTyped/undoCommitted/undoDelimiter` state (commits `left␣right`, undo restores `typed`). Merge (A-06)
  undo is still not wired (needs to re-insert the removed space).

### Round-2 slice: K-01 inset / D-11-D-12 / D-15 / D-19-D-20 (v0.7.8)
- **K-01 calibration inset (blocker fixed):** `CalibrationActivity` now applies the bottom
  navigation/gesture inset to its root (`ViewCompat.setOnApplyWindowInsetsListener`, bottom only — the
  AppCompat ActionBar handles the top), so the embedded keyboard sits above the gesture pill and
  calibration is usable. Layout root got an id (`calibration_root`).
- **D-11/D-12 earlier + fuzzy suggestions:** `DictionarySuggestionProvider.suggestionsFor` now merges
  prefix completions (shown from the 1st letter) with **fuzzy neighbours** (from the 3rd letter): single
  edit / umlaut-folded matches via the new pure `suggestion/Umlaut.fold` (ä→a, ö→o, ü→u, ß→ss) + the
  existing bounded `correctionCandidates`. So a valid-but-wrong "mut" still offers "mit", "grun"→"grün",
  "defaukt"→"Default". `autocorrectFor` is likewise umlaut-aware now. (Note: a junk-in-dict word being
  highlighted as "known" — the "Defaukt was green" report — is a data issue tied to C-04, not fixed here;
  the constructive part, offering "Default", is.)
- **D-15 Caps Lock:** double-tap Shift (within `DOUBLE_TAP_SHIFT_MS`=300) engages a persistent
  `AdaptKeyboardView.capsLock`; a further Shift press releases it. The Shift key shows "⇪" while locked,
  letters render/commit uppercase (`isUpperArmed()` = shifted || capsLock), `consumeShift` leaves capsLock
  intact, and a new field resets it.
- **D-19/D-20 swipes:** a full-field horizontal swipe now cycles the surface/page
  (letters → symbols-1 → symbols-2, wrapping) via new `GestureAction.SWITCH_SURFACE_NEXT/PREV`,
  `KeyGesture.surfaceSwipe`, pure `PanelNavigation.swipePage`, and service `applySwipePage` — separate from
  the space-bar language swipe (G-01) and the backspace word-delete (G-02). D-20: the view now uses
  per-gesture thresholds (`resolveSwipe`): the small `spaceSwipeThresholdPx`=28dp only for the space-bar
  language swipe, the larger `fieldSwipeThresholdPx`=64dp for dismiss-down / surface-swipe / word-delete, so
  a faint down-swipe no longer hides the keyboard.

### Device-feedback batch D-01 / D-02 / D-05 / D-06 / D-09 (v0.7.7)
- **D-01/D-02 multi-alternative long-press popup:** `Key` gained `alternatives: List<String>`; a key with
  **≥2** alternatives opens a Gboard-style popup on long-press (finger slides to select, release commits),
  while ≤1 keeps the immediate-apply behaviour (umlauts, ß, Greek tonos). The full-stop key carries
  `KeyboardLayout.PERIOD_ALTERNATIVES` = `. ! ? , ; : - _ /` (D-02, full stop pre-selected = index 0),
  shared by `GreekLayout`. Pure `keyboard/LongPressPopup.selectedIndex(pointerX, popupLeft, cellWidth,
  count)` (unit-tested) does the x→cell maths; the view (`AdaptKeyboardView`) owns the popup state, draws
  it above the key (clamped into view bounds, cells shrink if the row is too wide), tracks ACTION_MOVE to
  re-highlight and commits on ACTION_UP via a new `OnLongPressPopupListener`. `hasLongPressAction` now also
  true for `alternatives.size >= 2`; `scheduleLongPress` branches popup-vs-single. Service:
  `handleLongPressAlternative` → shared `commitLongPressSymbol` (letter → append into word, else
  finalise+commit like a delimiter) — the old single-secondary `handleLongPress` CHAR path was refactored
  to reuse it.
- **D-05/D-06 optional key sound + haptics (default off):** two settings (`d05_key_sound`, `d06_key_haptics`)
  through `RawSettings`/`AdaptSettings`/`SettingsMapper`/`SettingsStore` + a new "Tasten-Rückmeldung"
  preference category (two switches). The view holds `soundEnabled`/`hapticsEnabled` (pushed in
  `applySettings`) and fires on ACTION_DOWN: `AudioManager.playSoundEffect(FX_KEYPRESS_STANDARD)` (lazy
  AudioManager) and `performHapticFeedback(KEYBOARD_TAP)`. **No VIBRATE permission** — `performHapticFeedback`
  routes through the window system, preserving the minimal-permission stance (dropped the deprecated
  `FLAG_IGNORE_GLOBAL_SETTING` to stay warning-free).
- **D-09 raw-tap recording (opt-in diagnostic):** setting `d09_record_raw_taps` (default off, in the
  Kalibrierung category). Pure `touch/RawTapRecorder` + `RawTap` (unit-tested): collects
  expected-char/resolved-key/key-centre/contact-point per tap, exposes per-axis deviation, serialises to a
  TSV table (locale-independent 2-decimal coords). New view `OnRawTapListener` fires at ACTION_DOWN with the
  raw point + resolved key centre; `CalibrationActivity` (when enabled) pairs each with the sentence's
  expected char and, on finish, offers a neutral "Tipp-Daten exportieren" dialog button that shares the TSV
  as `ACTION_SEND` text (no storage permission needed). Lets an uncalibrated finger's systematic offset be
  analysed offline.
- **Device-only verification:** popup rendering/geometry + finger-tracking, the punctuation set, the
  sound/haptic feel, and the D-09 export flow all need a pass on the Pixel 9a. Pure cores (`LongPressPopup`,
  `RawTapRecorder`, the settings mapping) are JVM-unit-tested; the view/activity glue is Android-only.

### Device-feedback batch D-03 / D-04 / D-07 / D-10 (v0.7.6)
- **D-03 space bar shows the language:** `AdaptKeyboardView.spaceLabel` (a view property, drawn for
  `KeyCode.SPACE` in `labelFor`, so it survives layout rebuilds); the service pushes the current input
  language's label via `updateSpaceLabel()` → `languageLabel(activeLanguage)` ("Deutsch" / "Ελληνικά";
  "English" mapped for completeness but never the active alphabet — English is only auto-detected for
  autocorrect). Called from `onCreateInputView`, `onStartInputView` and `toggleLanguage` (G-01), so the
  label tracks the space-swipe switch. The G-01 toast now reuses `languageLabel`.
- **D-04 space bar (and every special key) flashes on press:** root cause was `pressedKeyPaint` using
  `key_background_special` — the *same* colour the special keys already draw at rest, so space/shift/enter
  showed no change when pressed. Added a distinct `key_background_pressed` (#A6C8FF) colour for the pressed
  paint. Plus a **post-release flash** (`flash(key)` / `flashKey` held for `flashDurationMs` = 80 ms via
  the existing handler) so even a sub-frame quick tap is visibly acknowledged; `onDraw` highlights
  `pressedKey || flashKey`; `cancelFlash()` on the next ACTION_DOWN. Flash fires on the tap-emit path only
  (not on swipe/long-press).
- **D-07 accelerating backspace-on-hold:** pure `keyboard/BackspaceRepeat` (unit-tested, 6 tests):
  `INITIAL_DELAY_MS` = 400 before the first repeat, `nextDelayMs(step)` geometric decay from
  `START_DELAY_MS` 200 → floor `MIN_DELAY_MS` 45 (factor 0.82), and `deletesWord(charsDeleted)` switches to
  word-wise once `WORD_MODE_AFTER_CHARS` = 18 chars (~3 words) have gone. The view owns the timer
  (`scheduleBackspaceRepeat` on ACTION_DOWN of `KeyCode.DELETE`, cancelled on UP/CANCEL/move-beyond-slop —
  a move is a G-02 swipe, not a hold) and fires `OnBackspaceRepeatListener.onBackspaceRepeat(step)`;
  `backspaceRepeated` suppresses the would-be single-delete tap on release so a hold never double-counts.
  The service's `handleBackspaceRepeat(step)` resets on step 0, deletes composing chars first, then
  committed text char-wise (via the shared `deleteOneBefore`) and word-wise (via `WordBoundary`) past the
  threshold; `backspaceHeldChars` tracks the count.
- **D-10 backspace at start of entry:** the single-char delete path is now `deleteOneBefore(ic)` — when
  `getTextBeforeCursor(1)` is empty (cursor at the very start of the editable) it sends a real DEL key
  event (`sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`) so the editor can join with the previous line/entry
  (Google-Keep-style), instead of the old no-op `deleteSurroundingText(1,0)`. Used by both the normal
  backspace and the D-07 repeat. A newline within one field is still a normal in-editable delete (joins
  lines) as before; only a truly empty prefix triggers the DEL fallback.
- **Device-only verification** (this environment cannot run an emulator): the space-bar label rendering,
  the flash timing feel, the backspace acceleration curve / word-switch threshold, and the D-10 DEL
  fallback across real editors (Keep notes, chat apps) all need a pass on the Pixel 9a. `BackspaceRepeat`
  itself is fully JVM-unit-tested; the view/service glue is Android-only.

### Layout (L-)
- L-01 base QWERTZ; L-02/L-04 configurable key proportions (`keyboard/KeyProportions`,
  backspace surcharge taken from third-row neighbours, narrower space / wider comma·period);
  L-06 persistent number row (toggle).
- L-05/L-06 long-press secondary symbols ACTIVE: primary char emitted on `ACTION_UP`
  so a long-press past the system timeout can emit the secondary; `KeyboardLayout.longPressSymbol`.

### Touch & learning (T-)
- T-01 `ACTION_DOWN` is the canonical tap point.
- T-03 personal offset model (`touch/OffsetModel`): per-key 2D Welford Gaussian of
  `touch − centre` deviations, warm-up → geometry fallback, offset capped to key size;
  `cappedMeanOffset`; persisted via `touch/OffsetStore` (SharedPreferences JSON).
  Also tracks a per-key mean contact area (`MotionEvent.getSize()`, only when > 0).
- T-04 typing-pattern detection (info-only, no functional effect): pure
  `touch/TypingPattern` enum + `touch/TypingPatternClassifier` (over normalised
  `KeySample`s: lateral bias / left-right asymmetry → index finger; low lateral +
  large contact area + vertically centred → thumb; needs area evidence, else UNKNOWN;
  < 40 taps → UNKNOWN) + `touch/TypingPatternAnalysis` (OffsetModel + geometry → samples).
  Service re-derives and persists the detected pattern after each model save; shown in
  Settings with a manual override (override stored, no consumer yet since info-only).
- T-05 space/letter ambiguity bands (`touch/AmbiguityBands`): lower edge band of
  c/v/b/n/m → SPACE_AMBIGUOUS; upper edge band of space → LETTER_AMBIGUOUS with nearest
  letter inferred; band depth scales with C-01 and is refined by the offset model.

### Suggestions (S-)
- S-01..S-06 suggestion bar: `suggestion/SuggestionController` (pure policy: sort+cap,
  drop verbatim-of-input, position stabilisation, re-sort on pause, pinned verbatim chip),
  `suggestion/SuggestionConfig` (C-02/C-03/C-04, validated), `SuggestionProvider` interface,
  `suggestion/SuggestionBarView` (scrollable chips via `onCreateCandidatesView`).

### Dictionary & autocorrect (A-, B-)
- Dictionary infrastructure: `dictionary/DictionaryStore` (`InMemoryDictionaryStore` pure +
  `SqliteDictionaryStore`: words / bigrams / blacklist tables, umlaut-safe lowercase key,
  survives updates A-04). `dictionary/DictionarySuggestionProvider` = tier-1 (prefix
  completion ranked by unigram freq + bigram bonus, A-01 known-words-never-autocorrected,
  A-04 blacklist filtering, single-edit autocorrect via `suggestion/EditDistance`).
- Autocorrect applied on delimiter; A-07 post-commit undo (one-keystroke armed backspace).
- Capitalisation §6 (`capitalisation/CapitalisationEngine`): explicit input wins,
  editor `CapsMode`, sentence start, proper/pure nouns, ambiguous nouns left as-is,
  B-02 post-hyphen lowercasing. B-01 hyphen as segment separator.
- A-05/A-06 retroactive split/merge (`dictionary/TokenRepair` over DictionaryStore):
  A-05 split a flagged char or insert a missed space (each half a known word, A-01 protected);
  A-06 prepend an inferred letter and merge when the candidate is known or the bigram is strong.

### Capitalisation shift behaviour (C-07, G-05)
- C-07 shift-grace (`capitalisation/ShiftGrace`): auto-arms Shift at word start for field
  mandates, guards a "surprising" mid-sentence arm against an immediate disarming Shift press
  for `shiftGraceWindowMs`; a press accepted after the window neutralises the field mandate
  for that token (linguistic noun/proper/sentence rules still apply).
- G-05 retroactive capitalisation at word end (`capitalisation/WordEndShift`): first-char
  case flip + next-key resolution table; a kept toggle / camelCase locks the token so the
  commit bypasses autocorrect + §6 + token-repair.

### Gestures (G-)
- G-01..G-05 complete (`gesture/` package). Swipe G-01/G-02/G-03 (`SwipeGesture`,
  `KeyGesture`, `WordBoundary`): G-02 delete whole word, G-03 dismiss keyboard,
  G-01 language switch recognised + consumed but a documented no-op stub (needs A-03 /
  a 2nd-language dictionary). G-04 drag-to-trash (`DragToTrash`): upward drag on a
  suggestion chip blacklists the word (USER). G-05 see above.

### Settings (C-)
- Settings UI C-01..C-09 (`settings/` package): `AdaptSettings` / `RawSettings` /
  `SettingsMapper` (pure, clamps to §10 ranges before validation), `SettingsStore`
  (reads the default SharedPreferences the androidx.preference screen writes),
  `SettingsActivity` (`PreferenceFragmentCompat` over `res/xml/settings_preferences.xml`).
  C-05 blacklist editor (`BlacklistActivity`), C-08 per-key secondary-symbol editor
  (`settings/LetterHints` pure + `LetterHintsActivity`), C-06 shown disabled (no LLM tier).
  T-04 "Tippmuster" category: read-only detected pattern + manual override.
- `AdaptKeyService` wires everything live; applies settings on input view start and via a
  `OnSharedPreferenceChangeListener`.

### Calibration / onboarding (K-)
- K-01 calibration (skippable): `CalibrationActivity` (settings package) hosts an embedded
  `AdaptKeyboardView` (number row off) whose `onKey` drives a pure `touch/CalibrationSession`
  (sentence index + typed buffer + `advance`) over `touch/CalibrationSentences` (3 ASCII-only German
  pangrams covering a–z; no autocorrect / no suggestion bar). The view's offset model is a dedicated
  `OffsetModel(warmupSamples = Long.MAX_VALUE)` so resolution stays pure-geometry for the whole
  session (each tap trains the physically-hit key). On finish the calibration model is **merged**
  (not replaced) into the persisted one via new pure `OffsetModel.merge` (parallel/Chan Welford
  combine of the per-key sufficient stats; contact-area mean = count-weighted), so a repeat from
  Settings adds to learned data; then T-04 is re-derived + persisted and a pattern-specific feedback
  dialog (§2.1) is shown. Reachable from a "Kalibrierung (K-01)" settings entry (repeatable) and a
  one-time first-launch offer dialog (`k01_calibration_offered` flag in default prefs). Service now
  reloads the offset model in `onStartInputView` (when `!restarting`) so a calibration done while the
  service was resident is adopted (storage is current there — saved on the prior `onFinishInput`).

### §6 sentence-start rules (comma-line C-10 + abbreviations/enumerators)
- Two new §6 capitalisation rules, replacing the old `endsAtSentenceBoundary` heuristic with a pure,
  unit-tested `capitalisation/SentenceBoundary.isSentenceStart(before, suppressAfterCommaLine)`:
  - **No auto-capital after a comma-terminated line (C-10, default on):** the content line following a
    line whose last non-whitespace char is a comma is not a sentence start — covers the German e-mail
    salutation (`Hallo Max Mustermann,` ⏎⏎ `danke …` → "danke" lower-case). Blank lines between are
    skipped (they are part of the trailing-whitespace run). Bound to a boolean setting C-10.
  - **No sentence start after a known abbreviation or enumerator:** a period ending `usw.`/`Nr.`/`z.B.`
    (pure `capitalisation/Abbreviations`, a curated German set, case-insensitive) or an enumerator
    `1.`/`10.` (regex) is not a sentence terminator, so the next word keeps its case.
- **Behaviour change (documented in the spec too):** a **line start now counts as a sentence start**
  (a deliberate newline capitalises the next word, like common mobile keyboards) — this is what makes
  the comma exception meaningful. Previously only `.`/`!`/`?`+whitespace triggered a sentence start.
  Preserved: a `.` with no following whitespace is still not a boundary.
- Settings plumbing: C-10 added through `AdaptSettings` / `RawSettings` / `SettingsMapper` /
  `SettingsStore` (`c10_comma_line_not_sentence_start`, default true) + a `SwitchPreferenceCompat` in
  the Großschreibung category. Service reads `settings.commaLineNotSentenceStart` in
  `captureTokenContext` and `sentenceStartBefore`.
- Scope note: the abbreviation rule uses a **curated built-in set + enumerator regex**. Spaced forms
  written with an internal space (`z. B.`) — only the space-free `z.B.` is recognised — and
  *user-learned* abbreviations are a later extension (would need an abbreviation flag on dict entries;
  the Wikipedia dicts strip periods so they carry none today).
- 301 unit tests (was 286; +15: `AbbreviationsTest`, `SentenceBoundaryTest`, +1 `SettingsMapperTest`).
  `:app:assembleDebug` green.

### Real multilingual dictionaries (A-03 fully realised: DE / EN / EL)
- Replaces the tiny `SeedData` placeholder with **real, large Wikipedia-derived lexicons** for German,
  English and Greek, and completes A-03: instead of merely *suppressing* German autocorrect on foreign
  text, the keyboard now **switches the whole lexicon per language** — German default, English
  auto-detected, Greek in the G-01 Greek mode — so each language gets its own suggestions, autocorrect
  and capitalisation.
- **Data / licence (user decision):** built from **Wikipedia** (DE/EN/EL `pages-articles` dumps),
  licence **CC-BY-SA 4.0**. The user is putting the whole app under GPL and confirmed CC-BY-SA is fine
  because the ShareAlike obligation lands only on the *data files* (which ship in the repo + APK), not
  the code; a root `CREDITS.md` records the attribution. (Chosen over CC-BY Tatoeba, which had too
  little Greek; Leipzig Corpora — ideal CC-BY — was unreachable from this environment.)
- **Builder** (throwaway dev tool, not shipped): `scratchpad/build_dict.py` streams each bz2 dump,
  decompresses incrementally and STOPS after ~500 MB decompressed (so only ~150 MB is actually
  downloaded per language, not the 0.6–25 GB full dumps), crudely strips wikitext, tokenises with
  sentence-initial tracking, and emits per language `dict_<code>.tsv` (`word\tfreq\tPOS`, top 120k) +
  `bigram_<code>.tsv` (`prev\tword\tcount`, top 80k). Assets total ~12 MB; APK ~11 MB.
- **POS from casing (no external POS lexicon):** part-of-speech is derived from *casing statistics in
  running (non-sentence-initial) text* — German: a word mostly capitalised mid-sentence → `NOUN`, mixed
  → ambiguous `{NOUN, OTHER}`; English/Greek: almost-always-capitalised → `PROPER_NOUN`, else `OTHER`.
  This drives §6 exactly right per language (German capitalises common nouns, English/Greek only proper
  nouns + sentence start) with zero extra data. Canonical surface case = most frequent non-initial form
  (so `Haus`/`Berlin` come out capitalised, `das` lower-case). Verified: DE top words + `Jahr/Stadt/
  Deutschland` as NOUN; EN `United/American/British` as PROPER_NOUN; EL function words + `Ελλάδα/Αθήνα`.
- **Kotlin — minimal-interface approach:** the `DictionaryStore` interface is UNCHANGED; instead there
  are **three single-language `SqliteDictionaryStore`s** (new optional `databaseName` ctor param →
  `adaptkey_dictionary_<code>.db`) plus a per-language provider + capitalisation engine. Pure, tested
  `dictionary/DictionaryAssetParser` (word/bigram TSV → `WordEntry`/`Bigram`, `\r`-tolerant, skips
  junk). `SqliteDictionaryStore.bulkImport(words, bigrams)` does the first-run seed in one transaction.
  Android-only `dictionary/DictionaryLoader` builds the three stores and seeds each from its asset when
  empty (German falls back to `SeedData` if its asset is missing).
- **Service routing:** `stores`/`providers`/`engines` maps keyed by `Language`; the active
  `provider`/`capitalisation`/`dictionaryStore` fields are re-pointed per token by
  `selectActiveDictionary(context)` → `resolveDict`: Greek mode → EL; else `isForeign` false → DE;
  confidently English → EN; confidently other-foreign (e.g. French, no lexicon) → DE store but
  autocorrect suppressed + no suggestions. Applied in `finalizeAndCommit` and `refreshSuggestions`
  (the old `germanAutocorrectSuppressed` gate is gone). `LANGUAGE_WINDOW = 5` trailing words.
- **First-run ANR avoided:** importing ~0.5M rows into SQLite on the IMS main thread would ANR, so
  `onCreate` installs instant empty `InMemoryDictionaryStore`s and `loadDictionariesAsync()` loads the
  real SQLite stores on a background thread, swapping them in on the main thread via `installStores`.
  Until the (first-run-only) import finishes there are simply no suggestions — graceful.
- 286 unit tests (was 279; +7 `DictionaryAssetParserTest`). The Greek-input package's earlier
  "Greek committed raw / no Greek dictionary" note is now SUPERSEDED — Greek has a real lexicon and its
  own suggestions/autocorrect. `:app:assembleDebug` green; all six dict assets packaged.

### Emoji / symbol panel (L-03)
- The bottom-row combined key (`KeyCode.SYMBOL`, label 😊, corner hint "123") now does two things:
  tap opens the emoji panel; long-press **or** an upward swipe switches to the numeric/symbol layer.
  Downward swipe still dismisses the keyboard from anywhere (G-03) — no collision, since it's the
  opposite `SwipeDirection` in the same `KeyGesture.resolve` dispatch.
- Pure layer/panel state: `keyboard/InputSurface` (LETTERS/SYMBOLS/EMOJI) + `keyboard/PanelNavigation`
  (tap-transition table + long-press/swipe-up → SYMBOLS, both unit-tested).
- Pure `keyboard/SymbolLayout`: 2-page `?123` keymap mirroring the letter layout's row shape/weights
  (`KeyProportions` reused unchanged) — digits row + 10 symbols + page-toggle (`KeyCode.SYMBOL_PAGE`,
  `SymbolLayout.togglePage`) + backspace + `KeyCode.LETTERS` ("ABC", returns to letters) + comma/space/
  period/enter. `AdaptKeyboardView` gained `surface`/`symbolPage` vars that pick `KeyboardLayout.rows`
  vs. `SymbolLayout.rows` in `rebuildRows()`.
- `AdaptKeyboardView.OnLongPressListener` signature changed from `(symbol: String)` to `(key: Key)` so
  the service can dispatch by `key.code` (CHAR → commit `key.hint` as before; SYMBOL → switch layer).
  New `KeyboardLayout.hasLongPressAction(key)` (CHAR-with-hint, or SYMBOL) replaces the old
  hint-presence check for scheduling; `longPressSymbol` is untouched (still CHAR-only, still used by
  the service for the actual text to commit).
- Emoji dataset: **asset-based** (user chose the larger option over a hardcoded short list) —
  `app/src/main/assets/emoji_dataset.tsv`, 507 curated well-established single-codepoint emoji across
  7 categories (`emoji/EmojiCategory`), one `<CATEGORY>\t<emoji>` line each. Pure
  `emoji/EmojiDatasetParser` (Android-free, unit-tested: unknown categories / blank / malformed lines
  are skipped, never crash) + `emoji/EmojiDataset` (`Map<EmojiCategory, List<String>>`) are parsed from
  raw text handed in by the Android-only `emoji/EmojiDatasetLoader` (`context.assets.open`, falls back
  to `EmojiDataset.EMPTY` on any failure). Validated at authoring time with a PowerShell pass
  (`StringInfo.GetTextElementEnumerator` per entry = exactly one grapheme cluster; no cross-category
  dupes) since there is no way to visually render emoji in this environment.
- MRU recents: pure `emoji/RecentEmojis.recordUse(current, emoji, maxSize=30)` (move-to-front, dedup,
  cap; unit-tested), persisted Android-side by `emoji/RecentEmojiStore` (JSON array in
  SharedPreferences, mirrors the existing `OffsetStore` pattern).
- `emoji/EmojiPanelView` (Android-only, thin): `LinearLayout` with a `HorizontalScrollView` tab bar
  (back-to-letters ⌨, recent 🕐, then one tab per category using `EmojiCategory.icon`) over a
  `ScrollView`-wrapped `GridLayout` (7 columns) of emoji cells; selecting an emoji fires
  `OnEmojiSelectedListener`, the tab bar's first button fires `OnBackListener`.
- Service wiring: `onCreateInputView` now returns a `FrameLayout` holding both the `AdaptKeyboardView`
  and the `EmojiPanelView` (panel starts `GONE`); `setSurface(next)` toggles visibility of both, pushes
  `surface`/`symbolPage` into the keyboard view, and resets `symbolPage` to 1 whenever leaving SYMBOLS.
  `commitEmoji(emoji)`: per spec, first `finalizeAndCommit(ic, "")` (finalises any in-progress composing
  token exactly like a delimiter, with an empty delimiter), **then** `ic.commitText(emoji, 1)`, then
  records the MRU use and pushes the updated list back into the panel. `onStartInput` resets
  `surface` to LETTERS for every new field. `CalibrationActivity`'s key switch got a no-op arm for the
  three new `KeyCode`s (irrelevant to calibration).
- 243 unit tests total (was 217; +26: `PanelNavigationTest`, `SymbolLayoutTest`,
  `EmojiDatasetParserTest`, `RecentEmojisTest`, plus additions to `KeyboardLayoutTest`/`KeyGestureTest`).
  `:app:assembleDebug` green, asset confirmed packaged into `app-debug.apk`.

### Language detection (A-03)
- **NOT the spec's literal fastText/ONNX.** After discussing it with the user we deliberately went with
  a **pure-Kotlin character-trigram classifier** instead of a `.ftz` binary + ONNX Runtime. Reasons:
  (1) runs fully offline just like fastText would (privacy identical — the app never needs internet),
  (2) no native dependency / no APK bloat, (3) the whole thing is JVM-unit-testable in the project's
  established style, and (4) the profiles are **transparent, inspectable data derived from open,
  non-Facebook corpora** (the user explicitly distrusts Facebook, even OSS). Same interface, so a real
  model could replace it later. **Honesty caveat recorded in code:** the *plumbing* is unit-tested; the
  *model accuracy* is validated by an evaluation suite over a held-out corpus (a real eval, honestly
  labelled — NOT "unit tests prove the model").
- Languages: **DE, EN, EL (Greek)** + Latin neighbours **FR, ES, IT, NL, PT** (the user writes DE+EN,
  occasional Greek, wife writes Greek often → Greek matters). Greek is decided by **script** (disjoint
  Unicode block) via `language/ScriptDetector.greekFraction` — trivial and bulletproof, no n-gram needed.
- Corpus/profiles: built from the **UDHR** (public domain) via a scratchpad Python builder
  (`scratchpad/build_profiles.py`) that downloads the eight UDHR texts from the `eric-muller/udhr` GitHub
  mirror, does a deterministic 80/20 train/eval split per language, and emits Cavnar-Trenkle profiles
  (top-200 bi+trigrams, rank order) to `app/src/main/assets/language_profiles.tsv` (`<code>\t<ngram>`,
  1600 lines) plus a held-out eval corpus to `app/src/test/resources/language_eval.tsv` (116 sentences).
  The builder is a throwaway dev tool, not shipped; the asset + eval corpus are committed.
- Pure package `language/` (all unit-tested): `Language` enum; `CharNgrams` (normalize + bi/trigram
  counts + ranked profile — **normalization is byte-for-byte identical to the Python builder**, the one
  correctness-critical parity point); `CharNgramProfile` (ngram→rank map); `LanguageProfileParser`
  (`\t`-split, trailing-space-in-ngram significant, `\r`-tolerant, unknown codes skipped); `ScriptDetector`;
  `LanguageClassifier` (Greek script fast-path → out-of-place distance over profiles → argmin; `classify`,
  `classifyRecent(wordWindow)`, and the guard `isForeign`). Android-only `LanguageProfileLoader` (asset →
  parser → classifier; empty-profiles fallback = every result UNKNOWN = guard is a safe no-op).
- **Guard design gotcha (important):** `isForeign` does NOT use the general top-2 confidence — for
  closely-related languages (PT vs ES/IT) that gap is tiny even when German is nowhere close, so a
  confidence gate flagged only ~58% of non-German. Instead `isForeign` measures **German's own margin**:
  fire only when some language wins *and* German's out-of-place distance is ≥ `germanMargin` (0.15)
  worse than the winner's. This is the right question ("is this German?") and is conservative — UNKNOWN
  / borderline → not foreign, so German autocorrect stays on by default and is never wrongly disabled.
- Evaluation (`LanguageDetectionEvaluationTest`, honestly labelled as a **same-domain** UDHR held-out
  split, not open-domain proof): overall argmin accuracy asserted ≥ 0.90 (actual well above), Greek
  100% via the script path, German flagged foreign ≤ 1/15, and ≥ 0.85 of non-German sentences flagged.
- Service wiring: `languageClassifier` loaded in `onCreate` (defaults to the empty/no-op classifier).
  `captureTokenContext` now also stashes `tokenContextBefore`; new `germanAutocorrectSuppressed(typed)` =
  `classifier.isForeign("$tokenContextBefore $typed")`. Applied in **two** spots: `finalizeAndCommit`
  (commit `typed` verbatim instead of the German autocorrect) and `refreshSuggestions` (drop the pending
  autocorrect chip). §6 capitalisation is left as-is — it is driven by German-dictionary POS tags, so a
  foreign word (absent from the dict) is not force-capitalised anyway; noted as an accepted limitation.
- **Scope line the user agreed to:** this session is *detection + German-autocorrect guard only*. Full
  **Greek input** (a Greek layout + Greek dictionary + the real G-01 language switch) is a deliberate
  separate next package; G-01's swipe remains the documented no-op stub for now.
- 270 unit tests total (was 243; +27 across `CharNgramsTest`, `ScriptDetectorTest`,
  `LanguageProfileParserTest`, `LanguageClassifierTest`, `LanguageDetectionEvaluationTest`).
  `:app:assembleDebug` green; `language_profiles.tsv` confirmed packaged into `app-debug.apk`.

### Greek input (G-01 real language switch)
- The G-01 space-bar swipe (left **or** right — both toggle, with only two languages) now really
  switches the input alphabet between **German (Latin QWERTZ)** and **Greek**, replacing the old
  documented no-op stub. A short toast (`Ελληνικά` / `Deutsch`) confirms the switch. `AdaptKeyService`
  holds `activeLanguage` (default German, kept for the service lifetime, re-synced onto the view in
  `onStartInputView`); `toggleLanguage(ic)` finalises any in-progress token in the *current* language
  first, flips `activeLanguage`, and sets `keyboardView.greek`.
- Pure `keyboard/GreekLayout` (unit-tested): the standard modern-Greek touch rows
  (`ςερτυθιοπ` / `ασδφγηξκλ` / `ζχψωβνμ`), with the number row and the whole control/bottom row
  (shift, combined emoji-?123 key, comma/space/period/enter) mirroring `KeyboardLayout` exactly and
  reusing the same `KeyProportions`. So switching alphabets only swaps the letters; emoji, ?123 and all
  gestures work identically in Greek.
- **Accents (tonos):** stressed vowels carry their accented form as the `Key.hint` long-press secondary
  (α→ά, ε→έ, η→ή, ι→ί, ο→ό, υ→ύ, ω→ώ; `GreekLayout.ACCENTS`). Because a hint that is itself a **letter**
  must extend the word rather than delimit it, `handleLongPress` now checks `symbol.all { isLetter() }`:
  letter secondaries go through the new `appendLongPressLetter` (appends into the composing token,
  honours Shift for the upper-case accented form, `TapAmbiguity.NONE` flag), while the existing
  non-letter secondaries (@, €, !, /, …) still commit as a delimiter as before. Diaeresis forms (ϊ, ϋ)
  are deliberately omitted for now — one long-press slot per key, tonos is far more common.
- **Greek was committed raw in that package — NOW SUPERSEDED** by the real multilingual dictionaries
  section above: Greek has its own Wikipedia lexicon, so Greek mode gives Greek suggestions/autocorrect,
  and the `germanAutocorrectSuppressed` gate was replaced by per-language `selectActiveDictionary`.
- The `AdaptKeyboardView` gained a `greek` toggle that picks `GreekLayout.rows` vs `KeyboardLayout.rows`
  in `rebuildRows()` (same shape as the L-03 `surface` switch). `KeyboardLayout.hasLongPressAction` /
  `longPressSymbol` are generic over `Key`, so they work on Greek keys unchanged.
- 279 unit tests (was 270; +9 `GreekLayoutTest`). `:app:assembleDebug` green. Greek keyboard/accent/
  switch View+Service glue is Android-only → instrumented-test backlog.

### Tier-3 mini-LLM orchestration (C-06, §9 — architecture first, real model deferred)

- **User decision (Option A):** built the *pure, fully unit-tested tier-3 orchestration* behind a
  pluggable backend with a **no-op default**, and left the heavy native ONNX Runtime / Gemma-Nano model
  as a documented, optional, instrumented-test follow-on. Rationale (agreed): a real on-device LLM is a
  hundreds-of-MB model + native dep, only instrumented-testable, unobtainable/unverifiable in this
  environment, and clashes with the project's pure-logic style and the earlier A-03 decision to avoid
  ONNX/fastText. The app also stays 100% offline (no runtime download). With the stub backend the whole
  pipeline is **inert** — observable behaviour is byte-for-byte the tier-1-only build.
- **Pure package `prediction/` (all JVM-unit-tested):**
  - `Tier3Provider` (interface: `isAvailable` + `predict(Tier3Request): Tier3Result`) + the inert
    `NoopTier3Provider` (default backend, `isAvailable=false`). This is the seam a real ONNX backend
    slots behind later, unchanged.
  - `Tier3Request` (input, previousWord, **full `sentence`** — A-02: the LLM sees the whole running
    context, not a punctuation-truncated fragment) / `Tier3Result` (`Tier3Suggestion`s +
    optional `CapitalisationProposal`).
  - `Tier1Confidence` — pure rank-margin measure: top score ÷ Σ scores (one dominant candidate → ~1.0,
    many similar → low, empty → 0.0). This is the value the C-06 threshold gates on.
  - `LlmActivationThreshold` — the C-06 setting as an enum LOW/MEDIUM/HIGH carrying the tier-1-confidence
    threshold (0.25/0.50/0.75; default MEDIUM); higher = LLM more eager. `fromKey` is the validation/clamp
    point (unknown/blank/null → MEDIUM).
  - `Tier3Activation.shouldActivate(conf, threshold, available)` — pure gate (available && conf < thr).
  - `SuggestionMerger` — pure tier-1+tier-3 merge: tier-1 scores normalised to [0,1] vs their own max,
    tier-3 confidence added (agreement between tiers floats a word to the top), stable order, capped.
  - `HighCertaintyCapitalisation.forcesUpper(proposal, word, min=0.85)` — the §6 **rule-6 LLM exception**
    reduced to one boolean; only lifts a word to upper-case at high certainty, never lowercases.
  - `AdaptiveLearning.learningSignal(committed, tier3, tier1KnewWord, min=0.6)` — §9 feedback: a confident
    LLM word the n-gram didn't know is returned as a reinforcement signal (else null).
  - `Tier3Orchestrator(provider = NoopTier3Provider)` — composes the above: computes tier-1 confidence,
    gates on C-06, consults the backend when activated, merges, and returns a `Tier3Outcome`
    (suggestions / capitalisation proposal / activated / tier1Confidence / raw tier3). Not activated →
    returns the tier-1 list **unchanged** (same reference), so the no-op path is a true identity.
- **§6 rule-6 hook:** `CapitalisationEngine.capitalise` gained an optional `llmForcesUpper: Boolean = false`
  placed just below proper/pure-noun and above the ambiguous-noun/`else` arms — so it can capitalise an
  otherwise-lowercased ambiguous or unknown word, but the `afterHyphen` (B-02) and sentence-start arms
  short-circuit first, so it never overrides B-02. Default `false` → all existing §6 tests unchanged.
- **C-06 setting plumbing:** `llmActivationThreshold` added through `AdaptSettings` / `RawSettings`
  (`llmThresholdKey: String?`) / `SettingsMapper.toLlmActivationThreshold` (the tested clamp via
  `LlmActivationThreshold.fromKey`) / `SettingsStore` (`c06_llm_threshold`). The previously **disabled**
  C-06 preference is now a real `ListPreference` (Niedrig/Mittel/Hoch → low/medium/high,
  `useSimpleSummaryProvider`, default medium) in the Großschreibung category; the service picks it up via
  the existing `OnSharedPreferenceChangeListener`.
- **Service wiring (thin, inert under the stub):** `AdaptKeyService` holds a `Tier3Orchestrator()` and
  the last token's `lastTier3Result` / `lastCapProposal`. `refreshSuggestions` runs the orchestrator
  (`sentence = "$tokenContextBefore$input"`, A-02) and feeds `outcome.suggestions` to the controller;
  `finalizeAndCommit` passes `HighCertaintyCapitalisation.forcesUpper(...)` into `capitalise` and, after
  `learnWord`, calls `reinforceFromTier3` (the §9 feedback). All three read as identity with the no-op
  backend (empty result, null proposal, no signal). `clearSuggestions` resets the tier-3 token state.
- 345 unit tests (was 301; +44 across `Tier1ConfidenceTest`, `LlmActivationThresholdTest`,
  `Tier3ActivationTest`, `SuggestionMergerTest`, `HighCertaintyCapitalisationTest`, `AdaptiveLearningTest`,
  `NoopTier3ProviderTest`, `Tier3OrchestratorTest`, +3 `CapitalisationEngineTest`, +2 `SettingsMapperTest`).
  `:app:assembleDebug` green.

### Tier-3 real backend — model chosen + ONNX-route groundwork (pure, verified)

- **Model (user endorsed "Apache + ONNX Runtime"):** `HuggingFaceTB/SmolLM2-360M-Instruct` — Apache-2.0
  (clean GPL fit, non-Facebook, non-gated), `LlamaForCausalLM`, vocab 49152, 32 layers, GQA 15/5 heads,
  hidden 960, rope_theta 100000, `tie_word_embeddings`, max_pos 8192. Its HF repo already ships ONNX
  exports, so the Python conversion is **skippable**. Downloaded (curl; net + git-lfs work here) to
  `D:\workspace-ai\models\SmolLM2-360M-Instruct\` — OUTSIDE the app repo, NOT in git/APK:
  `onnx/model_q4f16.onnx` (273 MB, 4-bit + fp16, fp16 KV-cache) + tokenizer/config files.
- **Runtime route decided by availability:** `onnxruntime-genai` is **not on Maven Central** (only plain
  `onnxruntime-android` 1.22.0 + `onnxruntime-extensions-android` 0.13.0 are). So the buildable path is
  **plain ONNX Runtime**, meaning the app owns the tokenizer + the autoregressive decode loop.
- **Confirmed ONNX graph I/O** (`model_q4f16.onnx`): inputs `input_ids`, `attention_mask`,
  `past_key_values.{0..31}.key/value`; outputs `logits`, `present.{0..31}.key/value` (32 layers, GQA
  5 KV-heads, head-dim 64, fp16 KV-cache). No `position_ids` input.
- **Pure ONNX-backend core (`prediction/`, all JVM-unit-tested):**
  - `Tier3Prompt` — continuation prompt = the full running context (A-02), capped to a trailing window.
  - `Tier3ResponseParser` — raw continuation → ranked candidate words (completes the current token, or
    yields next-word predictions); confidence is rank-derived (a scored decode can supply real probs).
  - `Tier3Decoding` — validated per-activation limits (maxNewTokens / numCandidates), tiny by default.
  - `Tier3ModelFiles` — the private model dir needs only the user-provided `model.onnx` (the tokenizer
    is bundled in the APK, see below); presence check over `File` drives `isAvailable`.
  - `VocabJson` — minimal pure parser for a flat `vocab.json` (token→id, JSON string-escape + raw UTF-8).
  - `Tier3TokenizerParser` — assembles a `BpeTokenizer` from `vocab.json` + `merges.txt` text (resolves the
    SmolLM2 special-token ids from the vocab; skips the `#version` header; CRLF-tolerant). Pure/testable;
    the Android layer only reads the files and calls it.
- **Pure byte-level BPE tokenizer (`prediction/onnx/`, user chose hand-rolled Kotlin over a native lib):**
  - `ByteLevel` — the reversible GPT-2 byte↔char mapping.
  - `BpeTokenizer` — full SmolLM2/GPT-2 pipeline: `individual_digits` split, the GPT-2 pre-tokenisation
    regex (`UNICODE_CHARACTER_CLASS`), byte-level encoding, rank-ordered merges, special-token isolation;
    `encode`/`decode` over plain vocab + merges + specials (the Android loader supplies these from
    `tokenizer.json`).
  - **Parity is VERIFIED, not deferred:** `BpeTokenizerParityTest` checks the Kotlin tokenizer against
    golden `text→ids` vectors generated by the real HF `tokenizers` (0.23.1, works on the machine's
    Python 3.14) for the actual SmolLM2 vocabulary — German umlauts, Greek, digits, punctuation,
    newlines, leading spaces all match **byte-for-byte**, and decode round-trips. The vocab/merges/golden
    data live under `src/test/resources/tokenizer/` (test-only, ~1.2 MB, not in the APK); generator is
    `scratchpad/gen_tokenizer_golden.py` (dev-only).
- **Bundled tokenizer + model-import UX (browser + SAF, user choice):** `vocab.json` + `merges.txt`
  (~1.3 MB, Apache-2.0) are bundled in `assets/tier3/` (CREDITS updated), so the user imports **only** the
  single 273 MB `model.onnx`. Delivery needs **no permissions**: the app has no `INTERNET` (so it is
  *provably* offline — verifiable in the manifest) and no storage permission. `Tier3ModelActivity`
  (settings → Großschreibung category) delegates the download to the browser (`ACTION_VIEW` to the model's
  public HF URL) and imports the picked file via SAF (`ACTION_OPEN_DOCUMENT`), copying it off the UI thread
  through the pure, unit-tested `Tier3ModelInstaller` (atomic temp `.part` + rename → an interrupted import
  never looks complete) into the app-private `Tier3ModelStorage.modelDir` (`filesDir/tier3-model`).
  Android glue (`Tier3ModelActivity`, `Tier3ModelStorage`) is compile-verified / instrumented-test territory.
- 392 unit tests (was 345; +19 pure core; +12 tokenizer; +11 loader; +6 `Tier3ModelInstallerTest`,
  `Tier3ModelFilesTest` retargeted to the single-file layout). `:app:assembleDebug` green; tier3 assets
  confirmed in `app-debug.apk`.

### Tier-3 inference runtime — WIRED (compile-verified; device-verification pending)

- **Dependency:** `onnxruntime-android` 1.22.0. `abiFilters` restricted to `arm64-v8a` + `armeabi-v7a`
  (drops the emulator-only x86/x86_64 native libs; debug APK 87 MB → 43 MB). Tier-3 therefore needs an
  **arm device**, not an x86_64 emulator, to test.
- **`Fp16`** (pure, unit-tested): IEEE-754 half→float, for reading the fp16 logits (subnormal/inf/NaN
  covered) — extracted so it is JVM-testable without the ONNX dependency.
- **`OnnxCausalLmSession`** (Android, ONNX Runtime): greedy autoregressive decode loop against the
  confirmed I/O (`input_ids`/`attention_mask`/`past_key_values.{0..31}.key/value` → `logits`/`present.*`).
  Key trick: the fp16 KV cache is only **shuttled** (`present.*` fed straight back as `past_key_values.*`
  via the retained `OrtSession.Result`), never read/converted; only the last-position logits are read
  (fp16→float) for the argmax. Empty initial past = fp16 tensors of shape `[1,5,0,64]`.
- **`Tier3TokenizerLoader`** (Android): reads the bundled `assets/tier3/` files → the pure
  `Tier3TokenizerParser`.
- **`OnnxTier3Provider`**: prompt → tokenize → `generate` → decode → `Tier3ResponseParser`; stop tokens =
  `<|endoftext|>`/`<|im_end|>`. `createIfAvailable` returns null when no model is installed.
- **Service wiring:** `loadTier3ProviderAsync` builds the provider off-thread (heavy session init) and
  swaps in a `Tier3Orchestrator(provider)`. When a real backend is active, `refreshSuggestions` shows the
  tier-1 suggestions immediately and runs tier-3 on a **single-thread executor** (`OrtSession.run` is not
  concurrent-safe) with a volatile sequence guard (skip stale tokens; discard late results). Reconciled on
  `onStartInputView` (picks up an import / drops on removal), executor + provider closed on `onDestroy`.
- **NB device-only:** the decode loop / session / tensor lifecycle / fp16 read are **compile-checked only**
  (no emulator/ONNX runtime here). Runtime correctness — and per-token latency/battery — must be validated
  on a real arm device; iterate on device logs. Also to tune on device: whether greedy is enough, prompt
  windowing, and whether the single big-model latency is acceptable per activation.

## Remaining (per spec §11)

- **Tier-3 — device work only:** validate + tune the inference runtime above on a real arm device (import
  the model via Settings → Info/Großschreibung → "Mini-LLM-Modell"), add instrumented tests, and confirm
  latency/battery. Everything code-side (orchestration, C-06 setting, §6 rule-6 hook, adaptive-learning,
  tokenizer + parity, model-import UX, ONNX session/provider/wiring) is built. Model already at
  `D:\workspace-ai\models\SmolLM2-360M-Instruct\`. Possible optimisation: ABI splits / app bundle so each
  device only pulls its own native lib (~18 MB arm64), and quantise/prune further if latency is high.
- Optional: a real fastText/ONNX model behind the same `LanguageClassifier` interface, if ever wanted.
- Verify/tune the first-run dictionary import time on a real device.
- **Stale-doc cleanup (found while answering "what's still open", not re-derived from anything new):** this
  section previously also listed "persist `activeLanguage` across service restarts", "Greek diaeresis (ϊ/ϋ)
  input" and "a language-aware C-05 blacklist editor" as nice-to-haves - all three were already implemented
  long ago (`ActiveLanguageStore`; the G-01 Greek-input package, v0.7.16; `BlacklistActivity`'s language
  spinner, also v0.7.16) and confirmed still present in the current code. Removed rather than left to
  mislead a future session into re-implementing them.

## Testing gaps

- Android-only layers (Activity / View / Service / `SqliteDictionaryStore` DAO /
  `SettingsStore` IO) still lack instrumented tests. Notably untested glue:
  the T-05/A-05/A-06 service wiring (flag retention, merge/split `InputConnection` surgery),
  the swipe/drag/word-end-shift View+Service glue, the T-04 View/Service glue
  (`event.size` capture, `charKeyGeometry`, `persistTypingPattern`), and the K-01 `CalibrationActivity`
  glue (key→session driving, merge-on-finish, feedback dialog) + the service offset-model reload.
  L-03 adds: `EmojiPanelView` (tab selection, grid population, back/emoji click wiring),
  `EmojiDatasetLoader` (asset read/fallback), `RecentEmojiStore` (JSON persistence) and the service's
  container/`setSurface` visibility toggling. A-03 adds: `LanguageProfileLoader` (asset read) and the
  service's per-language `selectActiveDictionary` routing (the pure classifier itself is fully
  JVM-tested + evaluated). Real-dictionaries adds: `DictionaryLoader` (3-store build + first-run asset
  import), `SqliteDictionaryStore.bulkImport` (transaction), and the async `loadDictionariesAsync` /
  `installStores` swap (background import → main-thread install); the pure `DictionaryAssetParser` is
  unit-tested. Tier-3 adds: the `AdaptKeyService` orchestrator glue (`refreshSuggestions` merge,
  `finalizeAndCommit` §6 rule-6 hook + `reinforceFromTier3`, `lastTier3Result`/`lastCapProposal`
  lifecycle) — inert with the no-op backend, so nothing observable to test until a real backend lands;
  the whole `prediction/` package is fully JVM-unit-tested.

## Notes / gotchas

- JUnit 5 (Jupiter). For `assertThrows`, import `org.junit.jupiter.api.Assertions.assertThrows`
  and use the `(Class, executable)` form, or Kotlin picks the reified overload and fails to compile.
- Contact area comes from `MotionEvent.getSize()` (Float); Float→Double rounding means
  contact-area test tolerances are ~1e-6, not 1e-9.
- Kotlin style: same-line braces, no spaces inside parentheses, 4-space indent,
  stdlib `require()` / `use {}` (not INFOLOG Commons), KDoc on non-trivial public/protected API.
- Minimal diff; do not reformat unrelated code; no unused imports / Kotlin warnings.
- A-03 parity gotcha: `language/CharNgrams.normalize` MUST stay byte-for-byte identical to the Python
  profile builder (`scratchpad/build_profiles.py`), else runtime n-grams won't line up with the stored
  profiles and accuracy collapses. The evaluation test doubles as a cross-check of that parity.
- Python on this machine: `/c/Program Files/Python314/python` (the `python3`/`python` aliases are the
  broken Windows-Store stubs). Git-Bash path conversion mangles `/c/...` args unless you leave it on —
  don't set `MSYS_NO_PATHCONV=1`, it made Python prepend the wrong drive.

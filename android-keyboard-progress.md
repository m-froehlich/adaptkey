# AdaptKey — Implementation Progress

Living status of the AdaptKey IME implementation, kept next to
[`android-keyboard-spec.md`](android-keyboard-spec.md). Requirement IDs
(`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`) refer to that spec. Update this file
whenever a component lands so it does not have to be restated in every prompt.

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
- Command (Git Bash, from the project dir):
  ```
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME=/d/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest
  ```
- A post-write hook normalises blank lines / CRLF and may reformat files after edits
  (do not fight it).

## Current State

- HEAD: `2b3f6ea` — v0.8.3 (version bump). Working tree = **v0.8.4**, §29 corrections below, not yet
  committed. **Spec §12/§13/§14 complete.** §28 (D-96…D-102, D-94) fully implemented; §29 is a same-session
  correction round on top of it, before any device testing happened. §26's D-87/D-88 and the rest of §27
  (D-95, D-103, D-104) remain backlog-only.
- **Versioning jumped from 0.7.54 to 0.8.3 on 2026-07-13** (user's deliberate call, see prior entry in git
  history) - the D-92/D-100/D-102 calculator/symbol-page redesign is the new 0.8 milestone. Still only the
  third digit bumps per APK going forward. `versionCode` counts up by 1 regardless of the version name
  (doesn't try to encode it - `8*10+3` would be lower than the outgoing value).
- Unit tests: **518 green** (`:app:testDebugUnitTest`, incl. Robolectric); `:app:assembleDebug` green (no
  warnings). `origin/main` is 15 commits behind; this session's §29 correction once committed makes it 16 -
  awaiting push.
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
- Nice-to-haves: persist `activeLanguage` across service restarts; Greek diaeresis (ϊ/ϋ) input; a C-05
  blacklist editor that is language-aware (currently operates on the active store); verify/tune the
  first-run dictionary import time on a real device.

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

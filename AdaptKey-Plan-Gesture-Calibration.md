# AdaptKey — Plan: Gesture/Long-Press Calibration for Slow Users (D-508)

**Status:** PROPOSED — analysis done, no code yet. User decision pending on §9's open points.
Origin: community report (2026-10-30) + user's two directives: the *whole* solution (profiles), not a
patch; and the D-380 smear protection is deliberately dropped for the space bar (its rationale never
applied to that key).

## 1. Motivation — the report, and what the code showed

Community report (verbatim, condensed): slow swiping on the space bar activates the mouse gesture
(D-401 cursor control); an option to activate it later would fix that; the same presumably carries
over to long-pressing ordinary keys; one profile setting selected after install could fix both at
once.

Code reading (AdaptKeyboardView.kt, as of v1.2.93) confirmed the report and sharpened it:

1. **The D-401 arm uses no motion evidence.** `ACTION_DOWN` on Space schedules the arm after
   `longPressDelayMs` (default **320 ms**, `d32_longpress_delay_ms` 150–600). The `ACTION_MOVE`
   cancel gate requires *both* "finger left the key's bounds" *and* "travel already clears the
   gesture's own swipe distance" (D-380). For Space's horizontal G-01 that is
   `44 dp × 1.15 ≈ 50.6 dp` (D-57). A space swipe slower than ~2.5 cm/s — or one whose first
   50.6 dp stay inside the wide key — never cancels the arm; at 320 ms the cursor gesture arms
   mid-swipe, `cursorControlActive` claims the stream, and at release `longPressFired` blocks both
   tap and swipe. The G-01 language switch is lost, exactly as reported.
   The spec's D-401-followup rationale ("the two never actually compete") only holds for swipes
   faster than 320 ms — this report is the counter-case.
2. **The slow space tap is even worse than reported:** any Space touch held > 320 ms with sub-slop
   movement arms the gesture and the release types **no space at all** (the ordinary path is skipped
   once `longPressFired` is true).
3. **Tension with existing tunings:** D-32 deliberately shortened the long-press (fast popups) while
   D-57 deliberately lengthened the space-swipe distance (fewer accidental switches). Together they
   widened the window in which slow users lose their swipes — the two goals fight each other through
   the shared 320 ms constant.
4. **"Carries over to long pressing a button":** the same 320 ms governs AltGr popups (L-05) and
   Caps Lock (G-06). A slow letter press > 320 ms opens a popup; a slow Shift press engages Caps
   Lock. By design, but the default was tuned for speed, and the swipe thresholds (44 dp / 110 dp /
   factors 0.85 / 1.15) are **hardcoded** — no user-facing calibration exists for any of them.

## 2. Agreed framework decisions (user's calls, before this plan)

1. **Whole solution:** a calibration *profile* mechanism, not just the space-bar patch.
2. **D-380's smear protection is dropped for the space bar** (user decision): its purpose — letting
   a smeared hold still open a letter key's popup — has no meaning on a key with no popup whose
   width lets real gestures stay inside it.
3. New requirement number: **D-508** (next free after D-507).
4. One version bump for the whole feature (version-bump rule), docs-only commits carry none.
5. New user-facing strings in English + all 31 locales (`UiLocaleConsistencyTest` enforces);
   new preference keys go into the backup key list.

## 3. Part A — fix the space-bar arm (root cause, small, independently shippable)

**A1. Cancel the pending Space long-press on motion, not on swipe distance.**
In `AdaptKeyboardView`'s `ACTION_MOVE` cancel gate, the Space key gets its own condition replacing
the `swipeDirectionIfFarEnough` requirement:

- cancel when the finger **leaves the key's bounds** (today's condition, now *without* the distance
  requirement), **or**
- cancel when movement **exceeds the system touch slop**, even while still inside the key.

Effects:

- Any space swipe at any speed now disarms the cursor gesture before 320 ms — including swipes that
  never leave the key. G-01 wins at every tempo.
- Mirrors the existing backspace-repeat cancel (already `movedBeyondSlop`-based), so the "moving
  finger = no hold intent" rule becomes uniform across keys.
- Deliberate cursor-gesture use is unaffected: hold still until the haptic (320 ms), *then* drag —
  sub-slop wobble still arms.
- Remaining arming case: a completely still hold > `longPressDelayMs` inside the key. That is the
  genuine cursor-gesture entry, by design.

**A2. "Empty arm" rescue (recommended, user to confirm — §9.1).**
When the cursor gesture armed and the sequence ends without ever having moved beyond slop, while
still in Stage CURSOR (the 800 ms stillness timer never promoted it to SELECTION), the release
commits an ordinary Space instead of entering the lift-grace. Rationale: a cursor-gesture user who
never moved has accomplished nothing, and a slow tap loses its Space today (finding 2 above). Users
who want the crosshair visible will move or re-touch inside the grace window — both unaffected.

## 4. Part B — make the swipe thresholds settings-backed

Two scaling preferences, percentages over today's hardcoded bases (no absolute dp; keeps the
existing key-width fallback intact):

| Key | Scales | Range (proposed) | Default |
|---|---|---|---|
| `d508_space_swipe_scale` | `spaceSwipeThresholdPx` (44 dp, ×1.15) | 80–160 % | 100 |
| `d508_field_swipe_scale` | `fieldRequired` = max(110 dp, 3 key widths), incl. ×0.85 page factor | 80–160 % | 100 |

Implementation, following the established seams:

- `AdaptKeyboardView`: two new public vars `spaceSwipeScaleFactor` / `fieldSwipeScaleFactor`
  (default 1.0), applied inside `swipeDirectionIfFarEnough()` — same pattern as `longPressDelayMs`.
- `SettingsStore`: keys + defaults + backup key list entries.
- `SettingsMapper`: MIN/MAX constants + `coerceIn` clamping (unit-tested there).
- `AdaptKeyService.applySettings()`: push the factors into the view.
- UI: two `LabeledSeekBarPreference` rows in the gesture category (between `d401_cursor_control`
  and `d19_surface_swipe`), title + summary in all 32 string files.

## 5. Part C — calibration profiles

**Model: apply-once, no persistent profile state (D-301 lesson).** A profile is an action that
*writes* concrete values into the existing sliders; the sliders remain the single source of truth.
No second truth layer above them — D-301 removed exactly such an override layer because it collided
with defaults. Benefits: backup/import needs nothing new; the service reload path is untouched;
nothing to migrate later.

Proposed profile values (starting points, device-tuning expected — G-08 "calibration status"
precedent):

| Parameter | Standard (today) | Bedächtig (deliberate) | Flink (quick) |
|---|---|---|---|
| Long-press delay (`d32`) | 320 ms | 500 ms | 220 ms |
| Double-tap delay (shared G-05/D-348/D-361 window) | 400 ms | 550 ms | 300 ms |
| Space swipe scale (`d508_space_swipe_scale`) | 100 % | 135 % | 85 % |
| Field swipe scale (`d508_field_swipe_scale`) | 100 % | 130 % | 85 % |
| Backspace repeat initial delay (D-07) | 400 ms | 600 ms | 350 ms |

Deliberately **not** profiled: the D-401 toggle (stays its own switch), touch-zone learning
(D-474), shift grace (C-07).

Implementation pieces:

1. **`BackspaceRepeat.INITIAL_DELAY_MS` becomes a parameter** (`nextDelayMs(charsDeleted,
   initialDelayMs)` with the current constant as default) so the service can pass the profiled
   value; the policy object stays pure and unit-tested.
2. **Settings entry point:** a compact "Voreinstellungen" row (three apply choices:
   Standard / Bedächtig / Flink) directly above the long-press-delay slider in the Layout category.
   Applying writes the profiled values into the existing preference keys; no profile key is stored.
3. **Onboarding ("selected after the install", per the report):** integrate the profile question
   into the existing `CalibrationActivity` (K-01) rather than a new `OnboardingStep` — the user is
   already asked "how do you type" there, the onboarding sequence stays four steps, and the screen
   stays reachable from Settings afterwards. Skip behaves like today's calibration skip: silently
   Standard, never overwriting anything.
4. **Existing installs are untouched:** no stored keys change meaning, defaults resolve to today's
   values, no migration. The profile is only *suggested* during onboarding (new installs) and
   available in Settings for everyone else.

## 6. Tests

- `SettingsMapperTest`: new clamps, defaults, boundary values for both scales.
- `BackspaceRepeat` tests: parameterised initial delay (existing constant path unchanged).
- `UiLocaleConsistencyTest`: enforces the new strings in every locale.
- `Onboarding` sequence tests unchanged (variant chosen in §9.4 keeps the step list as-is).
- `AdaptKeyboardView` touch paths stay in the established Android-glue no-test convention.

## 7. Device checklist (after implementation)

1. Language swipe on Space at ~1 cm/s → switches language, no cursor gesture (Part A).
2. Slow space tap > profile's long-press delay, no movement → Space commits (Part A2) / behaves as
   today if A2 declined.
3. Deliberate cursor gesture: hold Space still to the haptic, then drag → works, incl. sub-slop
   wobble during the hold.
4. Dismiss-down, page swipe, backspace word-delete at Standard vs. Bedächtig → clearly different
   distances, all still reachable within the 80–160 % range.
5. Apply Bedächtig in Settings → sliders show the profiled values; a manual slider change survives
   backup export/import; popup arming on letter keys unchanged (D-380 untouched there).
6. Fresh install → onboarding calibration step offers the profile; Skip → Standard. Existing
   install updated → behaviour byte-identical to before.

## 8. Round structure / commits

| Round | Content | Commit |
|---|---|---|
| 1 | Part A (A1 + A2) | code commit |
| 2 | Part B (scales, mapper, backup, strings ×32) | code commit |
| 3 | Part C (BackspaceRepeat param, profile apply, CalibrationActivity, strings ×32) | code commit |
| 4 | Spec addenda (G-01, G-08/D-401 arm rule + A2, K-01 profile note), Progress entry, History §, this plan → CLOSED; version bump | docs + bump commit |

Release per RELEASE RECIPE after device confirmation.

## 9. Open points — user decisions before starting

1. **A2 "empty arm" rescue: yes/no?** Recommendation: yes (closes the swallowed-space finding).
2. **Flink profile** included, or only Standard/Bedächtig? Recommendation: include (a two-option
   "profile" reads as a bug report waiting to happen).
3. **Profile value table (§5)** as starting points approved? (Bedächtig long-press 500 ms stays
   within the existing 600 ms slider cap; no range changes needed.)
4. **Onboarding placement:** integrate into `CalibrationActivity` (recommended) vs. a dedicated
   `OnboardingStep.INPUT_SPEED`.
5. **Backspace repeat initial delay** in the profile scope, or dropped to keep round 3 smaller?
   Recommendation: keep — it is the third leg of the "slow press" complaint family.

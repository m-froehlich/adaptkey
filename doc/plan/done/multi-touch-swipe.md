# AdaptKey — Plan: Multi-Touch Tap Composition Bug (proposed D-495)

**Status:** IMPLEMENTED end to end (2026-10-26, one session; see §8 for the exact state). Build + all
unit tests green (1728 total = 1719 + 9 new, 0 failures; `:app:assembleRelease`/`:app:testDebugUnitTest`),
shipped as §514 / v1.2.74 (versionCode 570). **Not device-confirmed** - the overlap repro needs real
two-finger/rolling typing on the device ("a + sehr schnelles SPACE", fast vertical typing for the
dismiss variant); the cursor-control path with a deliberate second finger is worth one try as well.

## 1. Motivation — the verified causal chain

User report (also reported by the community, and observed by the user in another shape): tapping `a`
and then, very quickly, SPACE flips the keyboard to the numbers/symbols page. Fast vertical typing
(top row → bottom row) makes the keyboard dismiss outright.

The chain, traced through code (line numbers as of v1.2.73 / versionCode 569 — re-locate before trusting):

1. `AdaptKeyboardView.onTouchEvent()` (AdaptKeyboardView.kt:1214) switches on `event.actionMasked`
   only. At `ACTION_DOWN` it records `pressedKey` and `downX`/`downY`; at `ACTION_UP` it calls
   `resolveSwipe(key, event.x - downX, event.y - downY)` (line 1351).
2. `SwipeGesture.classify` (SwipeGesture.kt:28) judges **only** the displacement's magnitude and
   dominant axis. There is no continuity requirement and no notion of *which finger* produced the
   coordinates.
3. **Nothing in the entire codebase handles multi-touch** — no `pointerCount` check, no
   `ACTION_POINTER_DOWN` / `ACTION_POINTER_UP` case anywhere (verified by search). Both actions fall
   through the `when` to `super.onTouchEvent()` unnoticed.
4. So with overlapping taps (the second finger lands before the first lifts — routine in fast
   rolling/two-thumb typing): `DOWN`(finger 1 on `a`) → `POINTER_DOWN`(finger 2 on SPACE, unhandled) →
   `POINTER_UP`(finger 1, unhandled — the `a` tap is silently lost) → `UP`(finger 2). At that final
   `ACTION_UP`, `pressedKey` is still `a`, but `event.x`/`event.y` are the **SPACE** position of the
   *second* finger.
5. `event.x - downX` is therefore the distance between two different fingers' taps — `a` (bottom-left)
   vs. SPACE (bottom-centre/right) is several hundred pixels horizontally. That clears
   `swipeDirectionIfFarEnough`'s field threshold (D-46/D-57, ~3 key widths × 0.85) and classifies as a
   horizontal swipe → D-19 page cycle (`KeyGesture.surfaceSwipe`) → the reported numbers/symbols page
   flip. The vertical variant (top-row → bottom-row overlap) yields `UP`/`DOWN` → §48 settings row /
   L-03 symbol layer or G-03 `DISMISS_KEYBOARD` — the "Tastatur verschwindet plötzlich" reports. The
   same composition also threatens `commitPopupSelection` (a second finger's release could commit an
   open alt-chars popup) and the D-401 cursor-control drag (its `DOWN` branch would even reset the
   origin mid-gesture to the second finger's position).
6. With strictly sequential taps (no overlap) the bug cannot occur — each stream's `downX` is its own.
   The overlap is the only composition path, which is exactly why it only fires "when typing very
   fast".

## 2. Agreed decisions

1. **Invariant (the user's own rule, verbatim):** a swipe is always **one continuous single-pointer
   motion**. Two separate taps can never compose into a swipe displacement, under any timing.
2. A second pointer landing while a key-touch sequence is live **cancels that sequence** — no tap, no
   swipe, no popup commit from it. The first tap of an overlapped pair stays lost (that is today's
   behaviour too); rescuing it is recorded as a possible future enhancement, deliberately not done now.
3. **Continuity gate (defence in depth):** a release may only classify as a swipe when at least one
   `ACTION_MOVE` beyond the touch slop was observed in its own sequence. A release whose sequence never
   moved can only ever be a tap. Real swipes always deliver MOVE events (even the short G-01 language
   swipe clears the slop by a wide margin), so genuine gestures are unaffected.
4. Taps are never gated by the continuity gate — a stationary tap must keep firing.
5. Thresholds, directions and the whole `KeyGesture` policy layer stay untouched; this fix is purely
   about *which events* may reach that policy.
6. The same hygiene goes into every touch handler that classifies displacement against a remembered
   down-point: the keyboard body, the D-401 cursor-control drag, `ExtraRowView`'s swipe-down, and
   `SuggestionBarView`'s drag/reject/swipe-down claims.
7. The pointer rule lives in one new **pure** class (no Android imports) with unit tests, following the
   project's pure-policy convention (`SwipeGesture`, `KeyGesture`, `BackspaceRepeat`,
   `CursorControlGesture`).

## 3. Design

### 3.1 `gesture/TouchSequenceTracker` (new, pure)

Tracks exactly one pointer per touch stream and answers the three questions the views actually have:

- `onDown(pointerId)` — a stream begins; the named pointer owns it. Clears all per-stream state.
- `onExtraPointerDown()` — a second pointer landed: the sequence is **dead** (never live again until
  the next `onDown`). Its memory of `trackedPointerId`/motion is kept only so `ownsRelease` stays
  false; nothing about a dead sequence can be re-armed by later events.
- `onMoveBeyondSlop()` — records `motionWasObserved` for a **live** sequence only (dead ones record
  nothing, so the continuity gate can never be satisfied from another finger's movement).
- `ownsRelease(pointerId)` — true only for a live sequence's own pointer: the only release that may
  commit a tap, a swipe, or a popup selection.
- `motionWasObserved` — the continuity gate's flag, read by the swipe path only.
- `end()` — full reset (stream over, or a mode like cursor control hands the stream back).

No Android types; `actionIndex`/pointer-id resolution stays at the call sites where the `MotionEvent`
lives.

### 3.2 `AdaptKeyboardView.onTouchEvent`

- New field `touchTracker = TouchSequenceTracker()`.
- `ACTION_DOWN`: `touchTracker.onDown(event.getPointerId(event.actionIndex))` as the very first
  statement (before `resolveKey`'s nullable early-return, so unclaimed streams are tracked too).
- `ACTION_MOVE` (after the popup early-return, before the existing smear checks): if
  `movedBeyondSlop(event.x, event.y)` then `touchTracker.onMoveBeyondSlop()`.
- New `ACTION_POINTER_DOWN` case: if a sequence is live, run the extracted
  `cancelActiveTouchSequence()` (the existing `ACTION_CANCEL` body: pending long-press, backspace
  repeat, popup, pressed state, invalidate — now including `touchTracker.end()`); return true. This
  closes the popup-commit hole from §1.5 at the same time.
- New `ACTION_POINTER_UP` case: `return true` (a single-pointer stream never delivers it; a
  multi-touch stream's sequence is already dead — the case exists so mid-stream events stop falling
  through to `super`).
- `ACTION_UP`: compute `ownsRelease` and `motionWasObserved` **before** `touchTracker.end()`. The
  popup commit requires `ownsRelease`; the tap/swipe dispatch requires `ownsRelease`; `resolveSwipe`
  runs only when `motionWasObserved`, otherwise the release is treated exactly like today's
  below-threshold case (not consumed → flash + posted tap).
- `ACTION_CANCEL`: same extracted `cancelActiveTouchSequence()`.

### 3.3 `AdaptKeyboardView.handleCursorControlTouch` (D-401)

The mode routes every event while armed, so it gets the same tracker (shared field):

- `ACTION_DOWN` (grace re-touch): re-`onDown` with the new pointer — today this branch would silently
  reset the origin to a *second* finger's position mid-gesture.
- New `ACTION_POINTER_DOWN` case: `endCursorControl()` — steering with a second finger is not a
  supported gesture; ending is the predictable outcome. The mode never sees a multi-pointer stream
  afterwards.
- `ACTION_MOVE` / `ACTION_UP`: only act when the event belongs to the tracked pointer
  (`findPointerIndex`; ignore foreign). `ACTION_UP`/`ACTION_CANCEL` end the tracker alongside the mode.

### 3.4 `ExtraRowView`

Owns a tracker instance used by both its `onInterceptTouchEvent` and `onTouchEvent`:

- `ACTION_DOWN` (intercept): `onDown(pointer 0)`; the existing `touchDownX/Y` stay.
- `ACTION_MOVE`: classify only for a live sequence, and only against the tracked pointer's own
  coordinates (`findPointerIndex` → `getX/getY`), so a second finger's position can never be measured
  against the first finger's origin. A claimed swipe-down is dropped when a second pointer lands.
- `ACTION_POINTER_DOWN`: tracker dead; `swipeDownIntercepting = false` (claimed branch included).
- `ACTION_UP` (claimed branch): fire `onSwipeDown` only when `ownsRelease` and the direction still
  holds at the tracked pointer's coordinates.

### 3.5 `SuggestionBarView`

Same pattern for its three claimed gestures (G-04 drag, D-488 reject drag, D-144 swipe-down) and the
unclaimed fallback swipe-down detection:

- `onInterceptTouchEvent` `ACTION_DOWN`: `onDown(pointer 0)`.
- `ACTION_MOVE`: all displacement classification is skipped for a dead sequence, and claims
  (`dragIntercepting` / `rejectIntercepting` / `swipeDownIntercepting`) can never be armed from another
  finger's coordinates; coordinates resolve via the tracked pointer where a claim is live.
- `ACTION_POINTER_DOWN`: drop any claimed state (`cancelDrag()` / `swipeDownIntercepting = false`).
- Claimed `ACTION_UP` branches: act only when `ownsRelease`.

### 3.6 Explicit non-changes

- No threshold, direction or `KeyGesture` mapping changes.
- The first tap of an overlapped pair is still dropped (today's behaviour). The conservative call per
  discussion; committing it eagerly is a separate future decision with its own edge cases.
- `AdaptKeyService.handleSwipe` / the D-19 cycle are untouched.
- No setting: this is a correctness invariant, not a preference.

## 4. Affected code sites

- `app/src/main/kotlin/de/froehlichmedia/adaptkey/gesture/TouchSequenceTracker.kt` — new pure class.
- `app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/AdaptKeyboardView.kt` — tracker field,
  `onTouchEvent` branches (DOWN/MOVE/POINTER_DOWN/POINTER_UP/UP/CANCEL), extracted
  `cancelActiveTouchSequence()`, `handleCursorControlTouch` pointer hygiene.
- `app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/ExtraRowView.kt` — tracker + guard.
- `app/src/main/kotlin/de/froehlichmedia/adaptkey/suggestion/SuggestionBarView.kt` — tracker + guard.
- `app/src/test/kotlin/de/froehlichmedia/adaptkey/gesture/TouchSequenceTrackerTest.kt` — new tests.
- `AdaptKey-Spec.md` §4 — one addendum stating the D-495 invariant.
- `AdaptKey-Progress.md` — the §514 round entry.
- `app/build.gradle.kts` — versionCode 569 → 570, versionName "1.2.73" → "1.2.74".

## 5. Tests

`TouchSequenceTrackerTest` (pure, JUnit; mirrors the bug report as scenarios):

1. Single-finger tap: down → up owns the release; `motionWasObserved` false.
2. Single-finger swipe: down → move beyond slop → up owns release, motion observed.
3. **The reported repro**: `a`-down → extra pointer down (SPACE finger) → (first finger lifts) →
   foreign up: `ownsRelease` false for both pointers, motion never observed — no swipe, no tap.
4. Foreign pointer never owns a release (even without cancellation).
5. A dead (multi-touch) sequence records no motion even when the surviving finger moves beyond slop.
6. `end()` fully resets; the next stream starts clean.
7. Sequential taps: down/up, down/up — each release owns its own stream (no cross-stream state).
8. `onExtraPointerDown` without a live sequence is a safe no-op.

Existing suites must stay green unchanged; the policy layer (`SwipeGesture`, `KeyGesture`) is not
touched, so no existing test needs edits. `AdaptKeyboardView` itself remains in the project's
established untested Android-glue layer (no Robolectric test exists for it today; device confirmation
follows the usual round).

## 6. Docs / release touchpoints

- Spec §4 addendum (D-495): a swipe/tap is a single-pointer, continuous motion; overlapping pointers
  cancel the in-flight key gesture; a release without observed movement can never swipe.
- `AdaptKey-Progress.md` §514 round entry (root cause, fix shape, tests, build, version).
- Version bump 569 → 570 / "1.2.74" per the per-round convention.
- No History.md section: recent rounds are recorded in Progress.md (the file's own tail convention).

## 7. Implementation order

1. `TouchSequenceTracker` + its unit tests (pure, compiles standalone).
2. `AdaptKeyboardView` wiring (body + cursor control).
3. `ExtraRowView` guard.
4. `SuggestionBarView` guard.
5. Full `:app:testDebugUnitTest` + `:app:assembleRelease`.
6. Version bump, spec addendum, Progress §514, this file's status/snapshot.

## 8. Implementation snapshot (2026-10-26, end of session)

All seven order steps landed as planned, with two design refinements worth recording:

- The tracker's `onExtraPointerDown()` fully clears `trackedPointerId` (plan §3.1 said it "keeps its
  memory" - the implementation does not need to: `ownsRelease` compares against the sentinel, so a dead
  sequence owns nothing, and a stale id can never re-match). `hasSequence` doubles as the "live" flag
  the plan's `live` described.
- `SuggestionBarView`'s claimed G-04 drag release recomputes its zone from the release coordinates
  (`zoneFor(...)` at UP) instead of trusting the last MOVE's zone state - the same
  "direction must still hold at release" two-stage pattern D-144 already documents, so ownership and the
  decisive coordinates come from one place.

Landed exactly as planned otherwise:

- `gesture/TouchSequenceTracker.kt` (pure, no Android imports) + `TouchSequenceTrackerTest` (+9: single
  tap, single swipe, the reported overlap repro, foreign pointer, dead-sequence motion, continuity
  false, end() reset, sequential taps, safe no-op extra-down).
- `AdaptKeyboardView`: tracker field + import; ACTION_DOWN tracks the pointer before `resolveKey`'s
  early-return; ACTION_MOVE notes moves beyond slop (after the popup early-return, which never swipes);
  new ACTION_POINTER_DOWN (cancels via the new `cancelActiveTouchSequence()`, the extracted ACTION_CANCEL
  body incl. `touchTracker.end()` - closes the alt-chars popup-commit hole from §1.5 too) and
  ACTION_POINTER_UP cases; ACTION_UP reads ownership + continuity before ending the stream, guards
  popup/tap/swipe dispatch on ownership, runs `resolveSwipe` only when motion was observed;
  `handleCursorControlTouch`: grace re-touch re-tracks, ACTION_POINTER_DOWN ends the mode, MOVE/UP use
  `findPointerIndex`-resolved coordinates, `endCursorControl()` ends the tracker (covering the external
  `dismissCursorControl()` path as well).
- `ExtraRowView`: tracker + tracked-pointer coordinates in both intercept-MOVE and the fallback MOVE;
  ACTION_POINTER_DOWN drops a claimed swipe-down; claimed UP requires ownership.
- `SuggestionBarView`: tracker + guards on all three claimed gestures (reject MOVE/UP, drag MOVE/UP with
  release-coordinate zone recompute, swipe-down UP) plus the unclaimed fallback MOVE; every claimed
  branch drops on ACTION_POINTER_DOWN; `cancelDrag()` reused as the single drop point.
- Spec §4: D-495 addendum (the invariant, the closed repros, the continuity gate, the conservative
  first-tap decision). Progress.md: §514 round entry. Version bump 569 -> 570 / "1.2.73" -> "1.2.74".
- Verification: 1728 tests (1719 + 9), 0 failures; `:app:testDebugUnitTest` + `:app:assembleRelease`
  green (AdaptKey.apk rebuilt). Not committed/tagged - the release round stays with the user, per
  convention.

**Review follow-up (same session):** a six-track review of the uncommitted tree found one D-495 gap —
`ExtraRowView`'s D-144 fallback `when` lacked the `ACTION_POINTER_DOWN` kill its intercept path and
`SuggestionBarView`'s fallback already had, so a gap-started swipe-down could still arm/fire across a
second finger's overlap. Fixed in the fallback branch. The review's other five findings concerned the
§513 round's code, not this plan's touch fix; per explicit user decision they were fixed too but
documented in their own round — §515 (D-496), see Progress.md.

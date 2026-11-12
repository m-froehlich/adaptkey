# Plan: Fast-typing overlapped-tap drop (D-528) + redundant commit-path suggestion work (D-529/D-530)

Status: IMPLEMENTED end to end (2026-11-12, one session). Build + all unit tests green (1934 total =
1926 + 8 new, 0 failures; `:app:assembleRelease`/`:app:testDebugUnitTest`), shipped as §546 / v1.3.7
(versionCode 607). **Not device-confirmed** - the overlap rescue needs real two-thumb/rolling typing;
the new `dropTap`/`tapParked` diagnostics decide in the field whether D-528 covers the reported
drops or an `ACTION_CANCEL`-source drop (now visible too) needs its own round.

## Problem

Community report (Pixel 9a): "if I type fast, it gets stuck in every 2-4 words and skip 3-4 letter".
The accompanying repro log proves the letter-loss class directly: at `-62.2s` a tap on `a` is fully
recognised (`rawTap` logged, haptic played, key cleanly resolved, `ambiguity=NONE`) but never processed:
no flash painted, no `handleKey` (whose D-217 `finally` logs unconditionally), composing never became
`"wa"`, and the committed field text confirms `"w" `. The tap died in `AdaptKeyboardView.onTouchEvent`
between ACTION_DOWN and the posted key handling. Earlier drops are visible in the same log's
`tokenContextBefore` (`"is seems"`, `"wprking"`). The same log shows the "stuck" half: a key processed
~200 ms late with its flash never painted (frame stall), `SPACE processed in 52ms`, `DELETE in 101ms`.

## Root cause

Every path that can silently drop a tap **after** the D-243 `rawTap` log produces zero diagnostic
output, and one of them is deliberate policy: D-495 (multi-touch-swipe) kills the whole touch sequence
when a second pointer lands while a press is live - `TouchSequenceTracker.onExtraPointerDown()`, whose
own KDoc already notes "the first tap of an overlapped pair stays lost (rescuing it is a separate
future decision)". Overlapping taps are routine in fast rolling/two-thumb typing, so the conservative
kill is the dominant suspected cause of the reported drops; `ACTION_CANCEL`/dead-release drops share
the same invisibility, which is why the device log cannot distinguish them.

## Design

### D-528 - rescue the overlapped first tap (gated parked press) + drop diagnostics

At `ACTION_POINTER_DOWN`, when the live press is still a clean tap candidate, **park** it intact
(pointer id, key, rect, downX/Y, ambiguity, record weight/size, downAtMs) instead of killing the
sequence, then begin the incoming pointer's press through the ordinary `ACTION_DOWN` body (extracted
as `beginPress`). Each press commits at its own pointer's lift as a plain tap (`ACTION_POINTER_UP`
gains a parked-commit branch; `ACTION_UP` handles a parked lift when the parked finger is last).
A parked press never swipes and its long-press arm is cancelled at park time - the D-495 fix
("two taps must never compose into one swipe") is preserved by construction: swipe classification
stays per-press and parked presses are tap-only.

Refusals keep today's D-495 kill exactly (extracted as a pure policy, `gesture/OverlapRescuePolicy`,
unit-tested): no live press, a press already parked (one rescue depth), long-press already fired, a
popup open, a backspace hold (D-07 repeat is a hold feature), or a **claimed** glide stroke
(plan §4.1.3: a second finger kills the stroke). An *unclaimed* glide stroke is not a refusal - the
stroke dies silently with the park (`resetGlideStroke()`), and the tap commits.

`ACTION_MOVE` becomes pointer-aware: with a live sequence the active press is fed its own pointer's
coordinates (`findPointerIndex`, the exact pattern the D-401 cursor-control machine already uses);
without one, behaviour is unchanged. `armCursorControl()` and `cancelActiveTouchSequence()` clear any
parked press (with its own log line).

Diagnostics (D-243 style, `logTouch`): `dropTap: reason=... key=... heldMs=...` at every silent exit
(`second_pointer_down refused=<reason>`, `sequence_cancelled`, `dead_release`,
`cursor_control_armed`) and `tapParked: key=...` on a rescue. The service's `handleKey`
`currentInputConnection ?: return` bail (before its `finally`) gets its own log line.

### D-529 - skip identical suggestion-bar pushes

After every word commit the bar receives the same content 2-4 times (`showNextWordPredictions` and
`armShiftForNextWord` both call `showSuggestions()`; the debounced Shift re-derivation adds more) -
visible as repeated identical `showSuggestions:` log lines in the repro log. `setSuggestionBarItems()`
is the one choke point every update funnels through (D-267/D-323); it now skips an identical
consecutive effective list (blacklist takeover included via the effective list). The D-50 always-
visible bar makes skipping visibility-safe.

### D-530 - one shared lookback read for the time/speed-unit triggers

`timeSuggestion()`/`speedUnitSuffixSuggestion()` (and their empty-composing twins) each issue their
own `getTextBeforeCursor` on every commit. Both patterns are end-anchored (`$`), so a single
`TIME_LOOKBACK` read serves both checks - the longer window can only make the `(?<![\p{L}])km/$`
lookbehind *more* correct. `SPEED_UNIT_LOOKBACK` is removed.

## Test plan

- New `OverlapRescuePolicyTest`: the community repro as a scenario (clean tap live, second pointer
  lands -> park), every refusal reason, and the claimed-glide/backspace-hold/popup cases.
- Existing `TouchSequenceTrackerTest` unchanged and green.
- `:app:testDebugUnitTest` and `:app:assembleRelease` green.
- Device validation is deliberately open: the overlap scenario needs real two-thumb/rolling typing.
  The new `dropTap` lines decide in the field whether D-528 covers the reported drops or whether an
  `ACTION_CANCEL`-source drop (same logs will now show it) needs its own round.

## Release

versionCode 606 -> 607, versionName 1.3.6 -> 1.3.7.

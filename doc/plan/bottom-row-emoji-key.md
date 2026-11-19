# AdaptKey — Plan: Dedicated Bottom-Row Emoji Key (D-555)

**Status:** PLANNED AND AGREED end to end (2026-11-19), implementation NOT started - "Du sollst
aber weiterhin nur planen. Noch nicht bauen." The design is fully pinned (§2), including the
funding rule for already-stored C-01 values (§3.1 variant A, the user's pick). Build starts only
on a fresh, explicit go.

**Identifiers re-pinned (2026-11-19, same day):** parallel rounds landed while this plan was being
written - the feature-tips plan (`feature-tips.md`) claims D-546..D-553, and the swipe-up number-row
round took D-554 / v1.3.24 / History §562. This plan is therefore D-555, its setting key
`d555_bottom_row_emoji_key`, and its version/section numbers say "next free at bump time" rather
than hard-coding ones that parallel sessions can consume.

## 1. Motivation

The bottom-row combined key's slot (L-03/D-507, C-27) carries `!?#` / Emoji / Off. The emoji panel is
otherwise reachable only through the extra row (R-01's always-visible button) or by setting C-27 to
Emoji (which retires the `!?#` key entirely). Until now the working assumption was "no space in the
row, and not important enough - the extra row covers it, and app emojis hardly matter."

The user has revised that assessment: emojis DO matter in some situations, and the row does have the
space - "wenn man die verbreiterten Buttons in der Reihe schmaler macht, ist der Platz da." The request:
a dedicated emoji button right of the `!?#` key on the letters page, behind a setting that is **default
ON**.

This is the same shape of decision as D-507's Emoji mode itself (a deliberately redundant entry point
beside the extra row); the change is that the redundancy is now judged worth row space.

## 2. Agreed decisions

1. **Build** with setting `d555_bottom_row_emoji_key`, boolean switch, **default ON**, Layout category.
2. **Funding (variant A, §3.1, user-confirmed 2026-11-19):** the emoji key's width is funded from
   the L-02 widening - comma and period give up exactly their surplus above the letter weight,
   never below their own configured value and never below the floor. The space bar keeps its exact
   weight. Emoji key weight = 0.6 (a single, tunable constant). Supersedes the originally answered
   "proportional" rule, which misbehaves for stored values below ~1.24 (the user's own
   existing-installation question surfaced this).
3. **C-27 interplay:** when the combined key's slot is already `EMOJI`, the dedicated key is **hidden**
   (the combined key itself then shows the 😊 glyph and opens the panel - a second adjacent 😊 key
   would be redundant; the row looks exactly as today).

Derived, not separately agreed but implied by the request and house conventions:

4. The key appears on **every** letter geometry (QWERTZ/QWERTY/AZERTY/Greek/Serbian/Jcuken/Azerbaijani)
   - universal like the number row (C-09), never per-layout gated.
5. Only the ordinary letters bottom row carries it - URL mode (E-01/U-01), email mode (D-158) and the
   symbol pages (L-08) keep their existing bottom rows; the extra-row button (R-01) stays, un-gated
   (the D-472 return button lives there and must stay reachable while the panel shows).
6. No new setting interplay beyond decision 3: C-09 (number row), C-01 sliders, D-19, G-03, glide
   (G-09) are untouched.

## 3. Design

### 3.1 The key and its width (pure funding math, `KeyProportions`)

Today's letters bottom row: `!?#` 1.3 + comma 1.4 + space 3.2 + period 1.4 + Enter 1.3 = 8.6
(a letter key = 1.0). The "verbreiterten Buttons" are exactly L-02's comma and period. The C-01
sliders store comma/period as absolute weights in **0.5 – 3.0** (default 1.4) - a value a user has
touched once stays in SharedPreferences forever, and a backup imports it onto other devices. Any
funding rule therefore has to be defined over the *stored* value, not just the default.

**Variant A - surplus funding above the letter-width floor (the pinned rule, user-confirmed
2026-11-19).**
The keys give back exactly the width they carry above the letter weight (`letterWeight`, 1.0);
they are never pushed below their own configured weight, never below the floor, and never silently
widened. Emoji weight is a fixed 0.6:

```
surplus(c) = max(0, comma  - letterWeight)
surplus(p) = max(0, period - letterWeight)
S          = surplus(c) + surplus(p)
funded     = min(EMOJI_KEY_WEIGHT, S)                  // EMOJI_KEY_WEIGHT = 0.6
comma'     = comma  - surplus(c)/S * funded            // (only when S > 0)
period'    = period - surplus(p)/S * funded
```

If `S < 0.6` (both keys carry little or no widening), the residual `0.6 - funded` is simply added
to the row's total weight - i.e. every key of the row shrinks proportionally by the same tiny
factor. No key is ever widened by the rule, and the space bar is never specifically touched.

| Stored comma/period | before | with the emoji key | note |
|---|---|---|---|
| 1.4 / 1.4 (defaults, stored or not) | 1.4 | **1.10** | surplus 0.8 ≥ 0.6, fully funded |
| 1.8 / 1.8 (deliberately widened) | 1.8 | **1.50** | deliberate widening is partly kept |
| 1.2 / 1.2 (narrowed) | 1.2 | **1.00** (floor) | residual 0.2 → whole row −2.3 % |
| 0.5 / 0.5 (slider minimum) | 0.5 | **0.50** (untouched!) | residual 0.6 → whole row −6.5 % |
| 0.5 / 3.0 (asymmetric) | 0.5 / 3.0 | **0.50 / 2.40** | only the wide key's surplus is spent |

Worst-case global shrink is bounded: the residual is at most 0.6 against a minimum possible row
total of 4.6 → ≤ ~11 %, and exactly 0 % for every configuration with default-or-wider punctuation.

**Variant B - plain proportional scaling (the originally answered rule; superseded).**
Both keys × `(c+p)/(c+p+0.6)`. Defaults: 1.4 → ~1.15. Fails for stored values: at 1.2/1.2 the keys
land at ~0.99 - below letter width; at the 0.5 slider minimum they collapse to ~0.31 and the emoji
key (0.6) would be the *widest* key of the row. A pure floor-clamp variant does not help either:
clamping upward silently *widens* a deliberately narrow comma (0.5 → 1.0) the moment the setting is
on - overriding stored user config in the wrong direction.

**Variant C - fixed absolute deduction (rejected).** Each key −0.3 with clamping: simple, but the
clamp again produces visible values that differ from what the sliders say, and widened
configurations give up proportionally less than narrowed ones.

**Implementation shape:** one pure, unit-tested function in `KeyProportions` implementing variant A,
e.g. `fundedBottomRow(commaWeight, periodWeight)` returning the funded `(comma, emoji, period)`
triple; the layout builders consume it when the emoji key is shown. Tuning is a single constant
(`BOTTOM_ROW_EMOJI_KEY_WEIGHT`), same considered-starting-point status as every other threshold
constant; retune on device feedback only.

### 3.2 Layout builders (`KeyboardLayout` + the five sibling layouts)

- New shared `KeyboardLayout.lettersBottomRow(proportions, showEmojiKey: Boolean)`: `!?#` (unchanged),
  then `if (showEmojiKey) emojiKey()`, then comma/space/period/Enter exactly as today. Mirrors the
  existing shared-`urlBottomRow`/`emailBottomRow` pattern; the emoji key is
  `Key(label = "😊", code = KeyCode.EMOJI, weight = …)` - **no corner hints, no alternatives, no
  popup** (a long-press has no action, see 3.4).
- `KeyboardLayout.rows(...)`, `AzertyLayout.rows(...)`, `GreekLayout.rows(...)`,
  `SerbianLayout.rows(...)`, `JcukenLayout.rows(...)`, `AzerbaijaniLayout.rows(...)` each gain a
  trailing `showEmojiKey: Boolean = false` parameter (default false keeps every existing call site
  and existing test green) and delegate their ordinary-letters bottom row to the shared builder.
  Their URL/email branches are untouched.
- New `KeyCode.EMOJI` in `Key.kt` (control key; `Key.id` = `"EMOJI"`, so T-03 seeds and learns it
  like any other control key).

### 3.3 Visibility resolution (service-side, one place)

- `AdaptSettings` gains `showBottomRowEmojiKey: Boolean = true` (raw pass-through in
  `RawSettings`/`SettingsMapper` - the pure layer adds no logic).
- The service pushes the **effective** flag in `applySettings()`:
  `view.showEmojiKey = s.showBottomRowEmojiKey && s.combinedKeyMode != CombinedKeyMode.EMOJI`
  (decision 3). The view just renders what it is told - the layouts never see `CombinedKeyMode`.

### 3.4 Dispatch and gestures (all generic, no special cases)

- Tap: `handleKey()` gains `KeyCode.EMOJI -> setSurface(InputSurface.EMOJI)` - the same one choke
  point the extra-row button and C-27's emoji mode already drive, so the D-507 rules (panel opening
  brings the extra row with it, D-472 return button reachable) apply automatically. No toggle logic:
  the keyboard view is GONE while the panel shows, so the key can never be tapped while open.
- Long-press: no action. `KeyboardLayout.hasLongPressAction()` lists EMOJI nowhere → the long-press
  arm never fires (no popup, no D-507-style leak to guard). `handleLongPress()` needs no new branch.
- Gestures (`KeyGesture.resolve`): EMOJI falls through the generic branches by design - UP reveals
  the extra row (§48), horizontal is the ordinary D-19 surface swipe, DOWN dismisses (G-03). Pin
  this deliberate generic behaviour in a test; no code change.
- Everything else consumes the key generically: sticky Backspace (D-361) derives neighbours from the
  rendered layout; glide (G-09) only starts on letter keys, so a stroke from the emoji key never
  claims and resolves as tap/swipe like from comma today; the offset caps are the generic ones
  (D-109 0.5 / D-397 0.3 vertical) - the same treatment comma/period already have, since
  `BOTTOM_ROW_LETTERS` (D-133) does not include them either.
- Rendering: `labelFor()` needs no change (EMOJI is not CHAR → no uppercase transform; the label
  `😊` falls through `else -> key.label`). Non-CHAR keys wear the muted function-key styling -
  consistent with its row neighbours.

### 3.5 Settings chain

- `SettingsStore`: `KEY_SHOW_BOTTOM_ROW_EMOJI_KEY = "d555_bottom_row_emoji_key"`,
  `DEF_SHOW_BOTTOM_ROW_EMOJI_KEY = true`, plain `getBoolean` in `load()`, and the key inserted into
  `EXPORT_SETTINGS_KEY_ORDER` directly after `KEY_COMBINED_KEY_MODE` (display order - it will sit
  directly after the d507 row in the XML).
- `settings_preferences.xml`: `SwitchPreferenceCompat`, `android:defaultValue="true"`, placed in the
  Layout category directly after the d507 combined-key row.
- Strings: `d555_emoji_key_title` / `d555_emoji_key_summary` in `values/` + all 31 locale files
  (AI drafts per the D-478 precedent; `UiLocaleConsistencyTest` enforces presence). Baseline:
  - EN title "Emoji key"; EN summary: "Show an emoji key (😊) directly right of the special-characters
    key. Comma and full stop give up their extra width above a letter's width to make room. Off:
    bottom row as before."
  - DE title "Emoji-Taste"; DE summary: "Emoji-Taste (😊) direkt rechts neben der Sonderzeichen-Taste
    anzeigen. Komma und Punkt geben dafür ihre Verbreiterung über der Buchstabenbreite ab. Aus:
    untere Reihe wie bisher."
- Backup import (Y-01): settings import is a generic upsert over the exported section - verify
  during implementation that no per-key importer list needs the new key; expect none.

### 3.6 Versioning

Version bump at round end, edited BEFORE the build (house rule, the user's corrected
build-then-bump order). **Never assume the number:** parallel sessions consume versions - the
swipe-up round took 1.3.24/624 while this plan was being written. At plan time the next free
version is **1.3.25 (versionCode 625)**; re-read `app/build.gradle.kts` immediately before the bump
and take the next free one.

### 3.7 Existing installations and stored C-01 values (the refinement's answer)

The user's question: what happens on an installation whose comma/period (and space) weights are
already stored? Answer, pinned as design:

1. **Stored values are never rewritten.** The funding is a pure build-time overlay: every layout
   build reads the current comma/period weights and derives the funded triple. Nothing is written
   back to SharedPreferences, the sliders keep showing exactly the stored value (e.g. 1.40), and
   switching the emoji key OFF rebuilds the row byte-identically to pre-D-555 (the D-301 lesson:
   no derived value ever becomes a second stored source of truth).
2. **"Already set" vs. "never touched" is indistinguishable by design.** `SettingsStore.load()`
   reads `p.getInt(KEY, DEF)` - a stored 140 and an untouched default 140 produce the same
   `KeyProportions`. The funding rule therefore behaves identically for both; there is no
   migration, no one-time write, no stored/default branch anywhere.
3. **Deliberately narrow configurations are respected, not corrected.** Variant A touches a key
   below letter width not at all (0.5 stays 0.5); the emoji key is then funded from the sibling's
   surplus, or - if neither key has surplus - from a tiny proportional share of the whole row
   (§3.1's table, worst case ≤ ~11 %, 0 % at defaults).
4. **The new boolean itself needs no migration.** `d555_bottom_row_emoji_key` follows the D-490/
   D-515 pattern: never written → default ON applies; an old backup imported onto the new build
   simply carries no value for it (default ON); new exports carry it; import is the generic
   settings upsert.
5. **Touch-model adaptation (cosmetic, self-healing).** For default rows the comma key's left edge
   moves right by the emoji key's slot and comma/period get narrower - the T-03 personal zones for
   those keys re-converge within ordinary use (the same adaptation every layout change already
   went through, e.g. D-515's apostrophe key). Worth a line in the device checklist, not code.

## 4. Deliberately unchanged

- The extra-row emoji button (R-01) - stays visible and un-gated; it becomes the third entry point
  and keeps the D-472 return-button role. One sentence in the spec's R-01 section records this.
- URL/email/symbol-page bottom rows; the D-531 page chain and its labels; `PanelNavigation` (no new
  transition - the emoji surface entry already exists); D-19/G-03; glide; C-01..C-28 semantics.
- No C-number in §20's table: the setting is a D-number-only switch, same as D-515.

## 5. Tests (per the pure-policy convention)

1. `KeyProportionsTest` +funding (variant A): each rule row of §3.1's table pinned - defaults fully
   funded (1.4 → 1.10, total row weight unchanged); deliberate widening partly kept (1.8 → 1.50);
   narrow keys stop exactly at the floor (1.2 → 1.00, residual added to the row total); slider-
   minimum keys completely untouched (0.5 → 0.50); asymmetric configs spend only the wide key's
   surplus (0.5/3.0 → 0.5/2.40); a key is never widened by the rule; emoji weight constant 0.6.
2. `KeyboardLayoutTest`: default `rows()` (no flag) is byte-identical to today - no EMOJI key (the
   guard that keeps every existing test green); with `showEmojiKey=true` the bottom row is
   `[SYMBOL, EMOJI, comma, space, period, ENTER]`, the emoji key has no hints/alternatives, comma/
   period carry the funded weights, and the row's total weight equals the unfunded total;
   `urlMode=true` / `emailMode=true` rows contain no EMOJI key even with the flag on.
3. One compact test each in `AzertyLayoutTest` / `GreekLayoutTest` / `SerbianLayoutTest` /
   `JcukenLayoutTest` / `AzerbaijaniLayoutTest`: flag on → EMOJI key at bottom-row index 1.
4. `KeyGestureTest`: EMOJI + UP → `OPEN_SETTINGS_ROW`, EMOJI + LEFT/RIGHT → surface swipe, DOWN →
   `DISMISS_KEYBOARD` (pinning 3.4's "all generic" decision).
5. Settings mapper test: default true; stored false → false (raw pass-through).
6. `UiLocaleConsistencyTest` must stay green (string presence in all 32 files).

## 6. Implementation checklist (files)

1. `Key.kt` - add `KeyCode.EMOJI`.
2. `KeyProportions.kt` - `BOTTOM_ROW_EMOJI_KEY_WEIGHT` + the pure variant-A funding function
   (`fundedBottomRow(...)`, §3.1).
3. `KeyboardLayout.kt` - `lettersBottomRow()` + `rows(..., showEmojiKey)` (+ `EMOJI_KEY_LABEL`).
4. `AzertyLayout.kt`, `GreekLayout.kt`, `SerbianLayout.kt`, `JcukenLayout.kt`,
   `AzerbaijaniLayout.kt` - parameter + shared builder for the ordinary bottom row.
5. `AdaptKeyboardView.kt` - `var showEmojiKey` (rebuildRows setter) + pass-through in `rowsFor()`.
6. `AdaptKeyService.kt` - `handleKey` EMOJI branch; `applySettings` push with the C-27 rule.
7. `AdaptSettings.kt`, `RawSettings` (SettingsMapper.kt), `SettingsMapper.kt`, `SettingsStore.kt`.
8. `res/xml/settings_preferences.xml` + `values*/strings.xml` (32 files, scripted like
   `scratchpad/d515_strings.py`).
9. Tests per §5.
10. Docs: spec L-03 D-555 addendum + R-01 sentence; Progress stub; the next free History section
    (§563 as of plan time - D-554 took §562; locate the newest `## §` heading before appending);
    plan moved to the done folder at round end (`git mv`, nothing else).
11. Version bump (§3.6), then `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
    ANDROID_HOME=/d/Android/Sdk ./gradlew :app:assembleRelease :app:testDebugUnitTest`.
12. Commits: (a) feature + tests, (b) version bump, (c) docs - per the one-commit-per-step rule.

Sanity greps before finishing: no remaining 5-key bottom-row assumptions on the letters surface
(grep `bottomRow[` in tests + main); `labelFor`/`onDraw` need no EMOJI branch (verify, don't assume).

## 7. Device checklist (user)

1. Letters page: 😊 sits right of `!?#`; tap opens the emoji panel; the extra row comes with it and
   the D-472 return button works.
2. Comma/period visibly narrower but still wider than a letter key (at stored defaults); with
   customized slider values the rule from §3.1 applies - deliberately narrow keys stay as they are;
   space bar width unchanged; typing feel unchanged (no new comma/space confusion).
3. Settings switch off → row identical to the pre-D-555 build; on → key returns (live, via the
   settings listener).
4. C-27 = Emoji → exactly ONE 😊 (the combined key), no duplicate.
5. URL field, email field, symbol pages: no emoji key there.
6. At least one non-QWERTZ geometry (pack language) shows the key too.
7. `!?#` ↔ 😊 mis-tap boundary feels acceptable; offset model adapts after a short while.
8. Backup export/import carries `d555_bottom_row_emoji_key`.

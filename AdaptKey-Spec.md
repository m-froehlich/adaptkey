# AdaptKey - Requirements Specification

**Platform:** Android (Kotlin, InputMethodService)
**Base:** AOSP LatinIME (conceptual)
**Status:** Implemented, actively maintained (not a concept document - see [`AdaptKey-Progress.md`](AdaptKey-Progress.md) for the current build/version/test-count status; never frozen here)

This document describes the app's **current, crystallised feature set only** - what AdaptKey does today.
The reasoning behind *why* each feature looks the way it does - device-feedback rounds, rejected
alternatives, bugs traced and fixed - lives separately in [`AdaptKey-History.md`](AdaptKey-History.md),
an append-only design journal. Requirement IDs (`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`, plus the newer
`E-`/`U-`/`P-`/`W-`/`R-`/`V-`/`N-`/`X-`/`Y-` domains below) are shared between both documents, so a `D-xx`/`§N`
citation in the history can always be traced back to the requirement it ultimately shaped.

---

## 1. Motivation

Every available Android keyboard implicitly assumes a particular input gesture - thumb typing, two-handed use, or a right-handed index finger. No existing product learns neutrally how a given user actually types. Workarounds such as shifted mini-layouts are not an acceptable solution.

Beyond this, off-the-shelf keyboards fail systematically at context-sensitive capitalisation, hyphenated compound words, punctuation as a prediction signal, and stability of the suggestion bar.

### Guiding Principle - Umlauts Are Ordinary Characters (the spirit of the app)

`ä ö ü ß` (and, by extension, the accented characters of every supported language) must be treated as
completely **normal characters** everywhere in the app. They must never come as a surprise to any feature,
never throw the autocorrect off its stride, and never produce an effect that a German user would find
unexpected. For a German, an umlaut is not exotic - a keyboard that stumbles over `ä`/`ö`/`ü`/`ß` (fails to
correct `konnen` → `können`, splits a word rather than restoring its umlaut, ranks a diacritic-less form
oddly, etc.) is simply broken. This principle overrides convenience shortcuts and will shape several
features (correction ranking, folding, split-vs-correct priority, suggestions).

Two independent, equally legitimate conventions exist for typing a diacritic character without holding for
it, and both must be recognised as an exact (not merely close) match: the formal orthographic substitution
(`ß` → `"ss"`, e.g. `"straße"`/`"strasse"`, the spelling used when `ß` is genuinely unavailable) **and** this
app's own long-press-alternative shorthand (`ß` sits as the long-press alternative directly on the `s` key,
mirroring `ä`/`ö`/`ü` on `a`/`o`/`u` - a lazily-tapped bare `s` is a direct stand-in for it, e.g.
`"gruse"` for `"Grüße"`). Neither convention may crowd out the other, and a match under either must never be
demoted to a frequency-ranked fuzzy candidate - it is exact, and must be offered as such (see A-01/S-06).

### Guiding Principle - `onUpdateSelection`'s Self-Recognition Mechanism Is Foundational

Getting `AdaptKeyService.onUpdateSelection()` to correctly recognise the IME's own edits took three full
device-log tracing rounds against two independently observed, genuinely different race shapes (a lagging
commit echo; an un-coalesced batch edit's transient intermediate state, from an editor that does not merge
the reclaim's delete/set/select sequence into one callback). Not a blanket "don't touch this file" - but
any future change that touches composing state, `onUpdateSelection`, `reclaimSurroundingWord`, or the
batch-edit sequencing in `AdaptKeyService` must keep these three properties intact, and should be weighed
against them before shipping (the full history entries for this fix are the cheap, sufficient check - not
an exhaustive whole-file re-audit every time):

1. `composingAnchor` (the absolute document offset of the composing region's start) must be resolved for
   *every* composing token, not only a mid-word one - `reclaimSurroundingWord()` reads it unconditionally,
   before any mutation in that same call.
2. `onUpdateSelection`'s `ownEdit`/truth check must never decide anything from the target editor's own
   remotely-reported `candidatesStart`/`candidatesEnd` - that value is not guaranteed to arrive in sync
   with the selection-update stream.
3. A callback whose reported positions mismatch the expected caret must be verified against a synchronous
   ground-truth read (`getExtractedText()`) before the composing state is torn down - simple positional
   equality against only the latest expected value is insufficient, proven by two distinct real echo
   shapes that a naive "belated update" interval heuristic (considered and rejected) would *not* both have
   caught.

If a change would make any of these three no longer hold, stop and re-derive against the original device
logs (see `AdaptKey-History.md`) rather than guessing - this class of bug reproduces silently and took real
device logs, not code review alone, to find each time.

---

## 2. Layout & Key Assignment

### L-01 - Base Layout: Language-Conditional
German uses the Gboard QWERTZ layout as its base - key sizes, spacing, and access to special characters -
except where this specification explicitly diverges. English uses its own QWERTY row layout (differing
from German only in the `y`/`z` position); every further Latin-script language (see §9's language packs) also
uses this same QWERTY row geometry, unless its own physical-layout convention genuinely differs (French's
AZERTY being the obvious case) - `LayoutRegistry` maps a language to its geometry, defaulting to QWERTY for
anything without its own entry. A language that fits an existing geometry needs no dedicated layout code at
all, only a dictionary and its own AltGr hint set (L-05/D-281); a language needing its own geometry (a new
Latin arrangement, or a genuinely different alphabet) does need new, compiled layout code - see
`AdaptKey-Language-Contribution-Guide.md` for exactly which case applies and why an existing `Language` enum
entry does not by itself guarantee the geometry question is already settled. Greek uses a dedicated layout
(see L-02 in §4/G-01 for language switching), the one non-Latin exception today. Because the personal offset
model (T-03) is keyed by character identity, `y`/`z` carry over their German-position offsets for a short,
self-healing period immediately after switching to English.

### L-02 - Narrower Space Bar, Wider Comma & Full Stop
The space bar is narrower than the Gboard default; the comma and full-stop keys are widened accordingly.
Two additional spacing sliders control the gap below the number row and the gap above the space-bar row
(default 7 dp each, range 0-25 dp), specifically to reduce mis-taps in that zone. The full-stop key's
long-press popup and the comma key's long-press popup are separate lists: full stop offers `! . ?`, comma
offers `- , : ; / _`. All proportions and spacings are user-configurable (C-01).

### L-03 - Symbol/Numeric-Layer Key
A single key in the bottom row switches to the numeric/symbol layer (`?123`) - a tap opens it, a further
tap on its "ABC" key returns to letters. The key's slot is governed solely by a setting (`symbolKeyEnabled`,
default on); when disabled, the slot stays reserved but inert rather than collapsing the row. The emoji
function does **not** live on this key - it is reached exclusively via the always-visible emoji button in
the swipe-up extra row (§14, R-01), which needs no setting of its own since it costs no keyboard-row space
while closed.

**Emoji search (D-317):** a dedicated magnifying-glass tab in the emoji panel's own tab bar (alongside the
category tabs and the recent-emoji tab) starts a live text search over every bundled emoji's own search
terms - both German and English, derived from Unicode CLDR - so a colloquial term (e.g. English "poop") and
a formal one (e.g. German "Kothaufen") both find the same entry. Tapping it leaves the emoji panel's own
grid and switches back to the ordinary letter keyboard for typing, with the live matches shown where the
suggestion bar's ordinary word suggestions normally appear (S-01's own "the bar's slot can show alternate
content" precedent, alongside Autofill/credentials) - keys typed while searching never reach the actual
document; only Space, Backspace and letters/digits/punctuation are active (no long-press popups, no swipe
gestures, no symbol layer) while search is active. Tapping a result commits it exactly like an ordinary
emoji-panel tap and leaves search mode; a dedicated cancel button next to the suggestion bar, and Enter, are
the two other ways back to ordinary typing - search mode is also defensively cleared whenever the field
itself is left, so it can never carry over into the next field. D-318: the back and search tabs are drawn
with a visible rounded button frame (unlike the plain category/recent tabs), so the two actions read as
buttons rather than one more tab to select; the query typed so far is always shown as its own pinned,
italicised, non-tappable chip at the left of the result list (even while still empty, right when search
mode is entered), so search never runs with no visible feedback of what has been typed.

### L-04 - Enlarged Backspace
The backspace key is widened relative to the Gboard default (recommended ~10%) to reduce mis-hits. The
additional width is taken from its neighbours within the third row and is independent of the bottom-row
layout. The exact proportion is user-configurable (C-01). The chosen typing style (T-04) pre-selects
whether the extra width favours the backspace side or the Shift side by default, matching which hand/finger
is expected to reach that corner.

### L-05 - Secondary Symbols via Long-Press (AltGr-Style)
Frequently used symbols are placed as secondary characters on letter keys, reachable by long-press and shown
as a small hint glyph in the corner of the key - analogous to the AltGr labels on a hardware QWERTZ
keyboard. The default mapping is now per-language (D-281) rather than one single set for every language: it
ships as data alongside the dictionary (bundled for English, part of the language pack for every other
language - see `AdaptKey-Language-Contribution-Guide.md`), precisely because a set tuned for German
(umlauts/ß on their own keys) is not what a language without those characters needs. German's own mapping -
still the fallback used whenever no per-language set is available - remains: `@`/Q, `€`/E, `#`/H, `-`/M,
`+`/N, `°`/D, `×`/X, `÷`/C, `/`/V, `*`/B, `ƒ`/F, `π`/P, `ä`/A, `ö`/O, `ü`/U, `ß`/S, plus a Greek-letter popup
(`α β γ δ λ ω`) and a second alternative on `O` (`ö` then `Ø`) - Greek itself does not use this mechanism at
all, having its own accent system instead (§4/G-01). Symbols already carried by the number row (L-06) - such
as `/` and `&` - are not duplicated here. Keys with more than one secondary alternative show a full popup
(horizontal, centred over the stem key, growing into available screen space), not just a single-tap hint; a
key with several alternatives but no natural single hint glyph shows a small corner "more alternatives"
triangle instead. Whenever the popup would run out of room to grow towards one side (a key near that edge of
the row) it is drawn reversed instead (D-282), so the key's own primary glyph still ends up nearest the
finger rather than pushed away by the clamp - determined dynamically from the actual popup width and the
key's own screen position, not by which specific key it happens to be, so this holds for any key's popup
regardless of what a language pack's own data puts on it. The popup tolerates a small amount of finger
movement during the hold without cancelling or mis-selecting. The mapping is always the active language's
own default; it is not user-configurable (D-301: a former per-key override editor combined incorrectly with
the per-language default above, silently reintroducing cross-language hint bleed the moment any single
symbol was customised, so the override layer was removed entirely rather than fixed). Together with L-03 and
L-06 this removes the need to switch to `?123` for most everyday input.

### L-06 - Persistent Number Row
A dedicated number row (`1`-`0`) is shown at the top of the alpha view by default and can be toggled off in
Settings (C-09). It behaves like an ordinary key row (full height, its own long-press popups): each digit
carries two long-press alternatives - its German-QWERTZ shifted symbol (`1`→`!`, `2`→`"`, `3`→`§`, `4`→`$`,
`5`→`%`, `6`→`&`, `7`→`/`, `8`→`(`, `9`→`)`, `0`→`=`) and its own superscript form (e.g. `2`→`²`) - reached
via the same multi-alternative popup as L-05. D-382: the `2` key additionally offers an apostrophe (`'`) and
its own subscript (`₂`) as a second and fourth alternative - D-417 reordered the popup to
`"` / `'` / `²` / `₂` (apostrophe ahead of the superscript, subscript last) - unlike every other digit's
plain two-alternative popup. C-09 governs only the main letters page; the dedicated URL-mode and email-mode
bottom rows never show
a number row at all, and symbol page 2 (§15) always shows a digit row regardless of C-09, as a guaranteed
fallback when the main row is off.

### L-07 - Calculator Layer
A dedicated calculator layout is reachable from the symbol pages (§15): a 4×4 digit/operator grid with a
persistent right-hand column (backspace, space, currency glyph, `ABC`, Enter), plus `sin`/`cos`/`tan`/`log`
and `deg`/`rad` keys. D-394: the digit block reads `1 2 3` / `4 5 6` / `7 8 9` / `0` top to bottom - the
now-converged phone-style order - rather than the calculator-style `7 8 9` / `4 5 6` / `1 2 3` / `0` order it
originally shipped with; the operator column (`+ − × ÷` top to bottom) and every other key's row position are
unaffected by this mirror. The minus key's sign flips via long-press. Currency glyph and decimal/thousands
separators are resolved from the device locale.

### L-08 - Catch-All Symbol Page
A second symbol page always shows a digit row plus shifted symbols as a guaranteed fallback for when L-06's
main-page number row is disabled.

---

## 3. Touch Capture & Learning Model

### T-01 - ACTION_DOWN as the Canonical Tap Point
The initial contact point (`MotionEvent.ACTION_DOWN`) is used as the authoritative tap coordinate. Subsequent movement up to `ACTION_UP` does not influence key resolution and does not trigger implicit swipe behaviour.

### T-02 - Retroactive Character Correction
The most recently entered character is held in composing state until sufficient context is available (e.g.
the start of the next word). If context subsequently points to a more probable neighbouring character, the
entry is corrected via `setComposingText`/`commitText`. The raw touch coordinate is retained until the final
decision is made. This extends to a raw-coordinate fallback: while composing, the token's own retained raw
taps are walked and the geometrically nearest neighbouring key is considered live, not only at commit time.

### T-04 - Typing Style (Explicit Selection, Load-Bearing for T-03)
The typing style is **not** auto-detected from taps - it is chosen explicitly during onboarding/calibration
(K-01) from an ordered list: Both Thumbs, Right Thumb, Left Thumb, Right Index, Left Index (ordered by
real-world prevalence; no option is marked "recommended"). Skipping the picker silently seeds "Both Thumbs"
rather than leaving the model cold. The chosen style directly seeds T-03's per-key offset model: home-row
keys get tight, centred zones; keys further from the typing hand get progressively wider zones with a
directional bias shifting the expected strike point back toward home, reflecting the physical arc of a
reaching finger/thumb ("Both Thumbs" keeps a flat, unbiased spread). This is a load-bearing input to the
touch model, not merely informational - it materially shapes T-03's starting state and is listed before
T-03 here for that reason.

### T-03 - Personal Offset Model
All tap events are persisted as tuples `(touch_x, touch_y, confirmed_key)`, seeded initially by T-04's chosen
typing style. A 2D Gaussian offset kernel is computed per key to compensate for each user's individual
systematic deviation, using a Huber-style robust weighted update: a tap far from the currently-learned
expected point contributes less to the mean (weight floored at 0.1, so a sustained pattern of similar
off-taps still eventually moves the zone, just more gradually than an ordinary running mean). The isotropic
offset is capped (`maxOffsetFactor = 0.5` of key size); the bottom row (`c v b n m`) additionally has a
tighter, direction-specific downward cap (0.25) so a key's zone can never drift down into the space bar's
territory. A tap flagged as T-05-ambiguous is never used as training data. The model exposes an exact
algebraic inverse (`unrecord()`) of a single weighted update, used narrowly by A-07's undo to reverse one
specific raw-coordinate correction. The model makes no assumption about handedness or finger beyond its
T-04 seed and continues to adapt from every confirmed word.

### T-05 - Space/Letter Confusion in the Bottom Row
The boundary between the space bar and the bottom letter row (`c v b n m`) is treated as a high-risk zone for swapped space/letter input - a risk aggravated by the narrower space bar (L-02). Two ambiguous bands are defined:

- **Space-ambiguous tap:** A `MotionEvent.ACTION_DOWN` in the lower edge band of `c`, `v`, `b`, `n`, or `m`. The committed letter may in fact be an intended space.
- **Letter-ambiguous tap:** An `ACTION_DOWN` in the upper edge band of the space bar. The committed space may in fact be an intended letter; the nearest letter key is inferred from the tap's x-coordinate via the offset model (T-03).

Taps in either band are flagged accordingly and their raw coordinates are retained (T-02), so that a later
token-level decision can retroactively replace a letter with a space (A-05) or a space with a letter (A-06).
The band widths scale with the configured key proportions (C-01) and are refined by the personal offset
model. A flagged tap is excluded from T-03's own training data (see T-03).

### K-01 - Initial Calibration / Typing-Style Selection *(skippable)*
On first launch, the user is offered a short setup step: pick a typing style from T-04's list. There is no
sentence-typing exercise - the style choice alone seeds the offset model (T-03). Skipping silently applies
"Both Thumbs"; the model still converges during normal use, just somewhat more slowly. The step can be
repeated at any time from Settings. The touch-zone visualisation screen (§17, T-06) doubles as the result
view after a (re-)selection.

---

## 4. Gestures

### G-01 - Language Switch
Swiping left or right on the space bar cycles the active input language through English (always first, the
one language always available) plus every currently installed language pack (§9), in a fixed declaration
order (direction determines forward/back) - German and Greek before D-280 were the fixed, only-possible
result of this cycle; installing or removing a language pack now changes what the cycle actually contains.
The gesture only fires on the letters surface's own space key; the
symbol/calculator pages and the dedicated URL-mode/email-mode space keys have no language of their own, so a
swipe there falls through to the ordinary page-switch gesture (G-06... see D-19-style full-field swipe,
§4 note below) instead. In addition to this manual switch, the active language switches automatically after
a configurable number of consecutive words committed in a different language than the one currently active
(C-23, D-398: a slider from 0 to 8, default 5 - a stored `0` disables the automatic switch entirely, leaving
only the manual swipe); every switch - manual or automatic - cross-fades the space-bar's language label
(260 ms) as a visible acknowledgement.

**D-400: switching the active language does not, by itself, switch the physical key layout.** The layout
(`LayoutRegistry.kindFor`) is pinned to the device's own system language, independent of whichever language
is actually active for dictionary/suggestion purposes - so neither the automatic switch above nor an
ordinary manual swipe between two Latin-script languages ever rearranges keys the user did not ask to
rearrange (system language German, active language cycling between German/English/French: always QWERTZ).
One exception: switching into a language with its own non-Latin alphabet (Greek, the only one today) always
shows that language's own dedicated layout, since it would otherwise be physically impossible to type at
all - reachable only via an explicit G-01 swipe, never the automatic promotion above (which only ever
targets English, and can only ever fire from an already-Latin active language, since typing the English
words that would trigger it is itself impossible on a non-Latin layout). Symmetrically, when the system
language itself has no sensible Latin layout to fall back to (the system language is Greek, or a language
this app has no layout convention for at all) and the user explicitly switches to a genuine Latin-script
language, the layout follows *that* language's own convention directly rather than defaulting to plain
QWERTY - an explicit switch is already a deliberate choice, so it may as well land on the right layout.

### G-02 - Delete Whole Word
Swiping left on the backspace key deletes the entire previous word rather than a single character. This is independent of - and takes priority in recognition over - the accelerating repeat-on-hold behaviour of a plain backspace press/hold.

### G-03 - Dismiss Keyboard
Swiping downward dismisses the keyboard - recognised on the keyboard body, the suggestion bar, and the
swipe-up extra row (§14) alike. When the extra row is open, a downward swipe closes the row first; only a
second downward swipe (with the row already closed) dismisses the keyboard.

### G-04 - Remove/Un-learn a Suggestion (Drag-to-Trash)
A suggestion in the suggestion bar can be dragged upward into a trash area that appears during the drag; a
simple swipe is not sufficient - the deliberate drag-and-drop action prevents accidental removal. The
outcome depends on the word's origin: a bundled dictionary word is blacklisted immediately (A-04). A word
the keyboard learned from the user's own typing is instead un-learned and marked provisionally pending
blacklist for a configurable window (default 7 days, editable 1-30); only a genuine recurrence within that
window escalates it to a permanent blacklist entry, so a single accidental drag does not permanently condemn
a self-taught word. Because a word matching the current input can never appear as a suggestion (S-02), a
dedicated review screen (§13, W-01) exists as the only way to remove such a word directly. The W-03 promotion
confirmation chip is the one exception to this single-zone behaviour, offering an explicit two-zone drag of
its own instead - see §13.

### G-05 - Double-Tap Shift to Toggle Word-Start Capitalisation
A double-tap on Shift (two presses within the configurable double-tap delay, default 400 ms, range 200-800 ms)
toggles the capitalisation of the current word's first character — in both directions: "Upper" becomes
"upper", "lower" becomes "Lower". The flip is **immediately visible** in the composing text, and the token is
marked case-locked so the next delimiter or letter commits it verbatim — bypassing autocorrect, §6
capitalisation, and single-word correction entirely. There is no camelCase continuation, and the three Shift
intents (Caps Lock via long-press, word-start toggle via double-tap, next-letter case via single tap) no
longer compete with each other.

The token stays composing after the toggle, so a further double-tap toggles the same word again (upper →
lower → upper …), regardless of where the caret sits within it. When no word is currently composing but one
genuinely touches the caret, the double-tap first reclaims it (D-62) before toggling — see the D-414-followup
addendum below; this works independent of claim state, including where the reactive reclaim-on-caret-move is
suppressed (D-351). Only when nothing at all touches the caret does the double-tap fall back to simply
disarming the first tap's ordinary Shift arm — a no-op beyond cancelling the one-shot uppercase.

The double-tap delay is configurable via a slider in the Layout settings category, placed directly below the
long-press delay slider. Default: 400 ms. Range: 200-800 ms, in 10 ms steps.

Committing a case-locked token (one toggled via this gesture) still bypasses autocorrect, §6 capitalisation,
and single-word correction entirely — "the user has hand-finished it" — **except** for A-05 retroactive word
splitting: a case lock speaks only to the first character's own casing, not to whether the token is genuinely
one word at all, so a genuine two-word split is still found and applied. A token that does split this way is
no longer case-locked — the split halves go through the ordinary per-half capitalisation like any other A-05
split.

### G-06 - Long-Press Shift for Caps Lock
Holding the Shift key past the long-press timeout engages Caps Lock (persistent uppercase). A simple tap on
Shift while Caps Lock is engaged releases it. When Caps Lock engages, a short vibration confirms it — this
haptic feedback is governed by a separate setting ("Caps Lock vibration", default on) in the Feedback
category, independent of whether the per-key vibration (D-06) is enabled. The vibration uses the same direct
`Vibrator` path as D-06, bypassing the system "touch vibration" toggle.

D-392: releasing Caps Lock re-derives Shift fresh from the real caret position exactly like every other
position-reached event does ([armShiftForNextWord] — D-313/D-406's own mechanism), rather than unconditionally
clearing it. Releasing Caps Lock right at a genuine sentence start now correctly leaves the next letter
auto-armed instead of forcing it lowercase. Scoped to a genuine word boundary (nothing composing) - releasing
Caps Lock mid-word (e.g. partway through an ALL-CAPS word) has no well-defined "next word" position to
re-derive against, so that case keeps the plain disarm.

### Addendum to G-05 - Shift State After Backspace, and After a Caret Tap Into Existing Text
D-406: a position is reached one of two ways, each with its own rule - both ultimately answered by
[armShiftForNextWord], §6 rule 2's own live-arming mechanism, reused verbatim rather than re-derived
separately:

- **Deleting a letter** is a deliberate exception to ordinary context derivation: the deleted letter's own
  case is the only signal. An uppercase letter deleted arms Shift on - the next keystroke reproduces an
  uppercase character, the case information is carried by the deleted character itself. A lowercase letter
  deleted sets Shift off outright, unconditionally - not merely "left as it was" (a stale "on" from
  elsewhere must not survive past deleting an ordinary lowercase letter).
- **Deleting anything else** (punctuation, whitespace, a digit) is not special at all - the resulting
  position gets the same context re-derivation any other newly-reached position does (see below), in both
  directions: on when the resulting position genuinely is a sentence start, off otherwise. No separate
  hard-coded "off" step is needed first - re-deriving from context alone already answers both directions
  correctly.

D-313 / D-406: reaching a new caret position with **no keystroke of its own** - a tap into an already-typed
word (§58/D-62's reclaim), a drag, an arrow-key move, or an external caret change while a composing token
was still active - re-derives Shift fresh from the caret's own new position exactly the same way, via
[armShiftForNextWord]: mostly lowercase, uppercase only where the position genuinely is a line/sentence
start. D-313 originally fixed only the "composing was already empty" side of this (the reactive
[reclaimWordAtCaret] path); D-406 closed the other side - an external caret move away while a composing
token was *still* active (handled inline in `onUpdateSelection`, not via [reclaimWordAtCaret] at all) never
re-derived Shift either, the identical class of stale-state bug reaching the position by a different route.
D-406 also fixed a related timing gap: a Backspace pressed faster than [reclaimWordAtCaret]'s own debounce
window could still hit a now-stale pending-auto-space guard in `handleBackspace()` and be silently swallowed
- consuming the flag without deleting anything and without ever reaching the delete-driven re-derivation
above. (D-416 removed that guard entirely along with the eager auto-space mechanism it protected - this
specific timing gap can no longer recur, since there is no stored flag left to go stale.) All three symptoms were reported together from one real repro: typing a sentence-ending period (which
arms Shift for the next word), tapping back into the previous word to fix a typo, and backspacing to correct
it - the first Backspace was silently swallowed, the second deleted correctly but left Shift wrongly armed,
and the wrongly-capitalised state then persisted even after tapping back to the genuine sentence start.

### Addendum to G-05 - Double-Tap Works Independent of Claim State
D-414-followup: the toggle previously only flipped the currently-*composing* word's first character, so it
silently did nothing when tapping into an existing word in a field where the reactive caret-move reclaim is
suppressed (Gemini, D-351) — the word never became composing on its own there, only an explicit action (the
Reclaim chip, a Backspace) reclaimed it. The toggle now reclaims first whenever nothing is composing yet: the
same unconditional, suppression-bypassing [reclaimWordAtCaret] the Reclaim chip and a plain Backspace already
use for exactly this kind of deliberate user action. A word genuinely touching the caret is reclaimed and
toggled in the same tap-pair; with nothing to reclaim, the call is a safe no-op and the toggle correctly does
nothing, unchanged from before.

### Addendum to G-05 - An Opening Quote or Bracket Never Disturbs Shift
D-378: typing an opening quote or bracket (`"`/`(`/`[`/`{`/`<`) with nothing composing must not change
whatever Shift state already correctly reflects the position it is about to introduce a word into - neither
an auto-armed capital at a genuine sentence start nor an explicit Shift press the user made specifically to
capitalise the upcoming quoted/bracketed word. The general "re-derive fresh on every word boundary" mechanism
([armShiftForNextWord]) is deliberately *not* invoked at all for one of these characters: re-deriving would
either wrongly de-arm an already-correct auto-arm (the sentence-boundary check sees the opener itself as
still-mid-token, not yet followed by real whitespace) or silently overwrite an explicit press with the
freshly-derived value, which is usually "off" mid-sentence. The plain apostrophe is deliberately excluded -
unlike these, it genuinely closes a quote as often as it opens one, so treating it as an opener would risk the
opposite mistake.

D-378-followup: still reproduced after the fix above, which only guarded [finalizeAndCommit]'s own
composing-*empty* commit branch - every other commit branch it has (a token-repair split, an A-06 merge, the
ordinary word-commit path, verbatim commit, the A-07 undo retry) called [armShiftForNextWord] directly and
still clobbered Shift, confirmed once a real device report showed the bug persisting after the first,
too-narrow fix. All of them now funnel through one shared choke point instead
(`armShiftForNextWordUnlessOpener`), so a future new commit branch cannot reintroduce this same gap by simply
calling [armShiftForNextWord] directly.

D-378-followup (v2): a real device log (identical in shape to D-373-followup's own v2) showed
`armShiftForNextWordUnlessOpener` itself was already working correctly at commit time - Shift genuinely stayed
untouched right when the opener committed - but the debounced D-62 reactive reclaim
([reclaimWordAtCaret]) fires ~100ms later (composing is empty right after an opener too, the same trigger
condition) and calls the raw [armShiftForNextWord] directly for its own, unrelated D-313 reason (a caret
landing on an existing word), with no awareness of the opener at all - silently turning Shift back off a
moment after the commit correctly left it alone. A new one-shot flag, consumed exactly like the existing
`shiftArmedByDelete`/D-373-followup's own `tokenShiftLiveArmed` guards right next to it, carries the "this was
just an opener" decision forward to that one call site.

---

## 5. Suggestion Bar

### S-01 - Horizontally Scrollable, Configurable Maximum
The bar shows as many ordinary (tier-1/tier-3) suggestions as are meaningful and scrolls horizontally.
Default: 8 entries. Maximum is user-configurable (range 3-10, D-399: widened from a 6 floor). Sorted by descending
probability; the most probable suggestion appears at the far left. "Probability" is not raw dictionary
frequency alone: closeness to what has actually been typed so far weighs in too, always as a soft
preference (a much closer candidate generally outranks a farther one even when the farther one is far more
frequent, though an overwhelmingly more frequent farther candidate can still occasionally win - never an
absolute cost-first rule), in two distinct forms depending on the candidate's own source. For a
correction/fuzzy candidate, closeness means edit distance from the typed token. For a plain prefix
completion (D-272), closeness means how many additional characters the candidate still needs beyond the
prefix already typed - within a shared-prefix word family, the completion needing fewer further characters
generally wins even over a more frequent, longer sibling (e.g. `"natürlich"` before `"natürliche"` for a
typed `"natürl"`), though this only distinguishes candidates up to a handful of extra characters; two
candidates that both still need substantially more than that fall back to plain, undistorted frequency
ranking against each other, so a genuinely longer completion is never crowded out purely for its length once
both are already "far" in this sense.

D-411: "frequency" for ranking purposes is not a learned word's own raw personal-use count either - that
count (single/low-double digits in practice) would never meaningfully compete against the bundled
dictionary's own corpus-scale frequencies (up to roughly 1,000,000) otherwise, so a genuinely often-typed
personal word could never rank near the front no matter how often it is used. Instead it is log-scaled to a
value that competes naturally alongside bundled frequencies once genuinely well-established (calibrated so
a word used 50 times reaches a value comparable to an ordinary, moderately common dictionary word - not
automatically ahead of a common one), with a further, temporary boost (×1.5) while the word was last
touched within the past 14 days - a word used heavily long ago settles back to its plain scaled value once
that window passes, rather than staying inflated forever. Deliberately ranking-only: this adjustment plays
no part in any correctness decision (A-01's known-word-override check, S-06's pending-replacement logic) -
those still read a word's real, unscaled frequency directly, exactly as before.

The bar's row is always reserved even when momentarily empty.
Depending on the focused field, the bar's slot can instead show a platform-rendered Autofill
inline-suggestions row, or a credential-suggestion list (§12) - S-01's own scrolling/sorting behaviour
describes the ordinary dictionary/tier-3 case specifically.

### S-02 - No Duplicate of the Current Input
The word exactly as typed does not appear as a suggestion. That slot belongs to genuine alternatives.
Exception: when an autocorrection is pending, the verbatim input is surfaced as defined in S-06. This is the
only case in which the typed word appears in the bar. (A direct consequence: a word matching the current
input can never be reached via drag-to-trash - see G-04/W-01.)

### S-03 - Position Stabilisation
The visible top-N ordinary suggestions may not change position while the user is actively typing. New
suggestions are appended at the right end of the scrollable list. Re-sorting only occurs after a
configurable pause with no input. Several newer suggestion kinds are deliberately built outside this
mechanism so as not to distort it: a mid-word connector-split suggestion (A-10), the raw-coordinate
suggestion (T-02), the time-pattern "Uhr" suggestion (S-08), the credential list (§12), and the B-03
hyphen-compound chip are all appended at display time with a maximal score (or, for the hyphen-compound chip,
pinned ahead of everything else without a score at all) rather than participating in the ordinary
ranking/stabilisation.

### S-04 - Re-sort Delay *(configurable)*
Slider from 0 to 600 ms. A value of 0 means immediate re-sorting. Default: 300 ms.

### S-05 - Word Confirmation Highlight *(configurable, default on)*
The word currently being typed is highlighted, via font colour, as soon as the system recognises it as
correct and complete (no correction planned). The highlight clears on the next delimiter (space,
punctuation, hyphen). While a retroactive split (A-05) is pending, a live two-span colour preview is shown
instead - both halves coloured, the dropped/boundary character left uncoloured - mutually exclusive with the
ordinary single-word highlight. During a fast continuous typing burst, recomputation is debounced (~200 ms
after the token stops changing) for performance, so the highlight can lag briefly in that case. Feature is
toggleable; colour is user-selectable. Confirmed by the user (see `AdaptKey-History.md`) that green meaning
"safe, no correction planned" is the correct, settled reading - the alternative ("about to be
auto-corrected") is not adopted.

D-416 applies the same "quiet, automatic confirmation" philosophy to a different, non-word state: a small dot
above the space key's own label while A-12's deferred auto-space is genuinely pending at the caret - not
configurable, not S-05's own colour setting, but drawn from the same instinct that the system should visibly
(if quietly) confirm what it is about to do automatically, rather than leaving it entirely invisible until the
next keystroke resolves it.

### S-06 - Verbatim "Keep As Typed" Affordance
Whenever autocorrect intends to replace the current token on the next delimiter - including a pending
capitalisation-only change (e.g. an ordinary noun about to be auto-capitalised) - the literal typed string
is surfaced as a dedicated, visually distinct
chip (e.g. in quotation marks), pinned at the far left of the suggestion bar and exempt from position
stabilisation (S-03). The pending replacement is shown alongside it. Tapping the verbatim chip commits the
literal input and cancels the pending change for this occurrence. This is the only case in which S-02 is
overridden. Repeated verbatim confirmation of the same string feeds a learning signal and may add it to the
personal dictionary (cf. B-03), after which it is no longer autocorrected. This affordance mirrors the
ordinary autocorrect candidate and a pending diacritic restoration alike (see the "Umlauts Are Ordinary
Characters" guiding principle) - the latter takes precedence whenever both would apply, matching what
actually wins at commit time - but it does not preview the rarer raw-coordinate fallback (T-02) or a
high-certainty tier-3 capitalisation exception.

D-364: the pending-replacement chip and S-01's own ordinary ranked list compare candidates
case-insensitively when deciding whether a word is already shown - the replacement is previewed already
cased as it would actually commit (D-111/D-112, directly above), while the ordinary list still holds each
word in its raw, uncapitalised dictionary form; without this, the same underlying correction could appear
twice (e.g. "Text" as the replacement, "text" - not yet recognised as the same word - as an ordinary
suggestion too).

### S-07 - Next-Word Prediction
A genuine next-word suggestion (not merely a completion of the current token) is offered before the next
word is typed, using a bigram baseline elevated by tier-3 when available. When a personal (learned-only, no
bundled seed) two-word-context trigram match also exists, it is preferred over the bigram baseline via a
Stupid Backoff blend (D-246): a candidate with a real trigram match scores by its own trigram count (D-429:
rescaled and recency-boosted, not raw); a candidate reached only through the bigram signal is discounted, so
a trigram match generally - but not absolutely - wins over a merely more frequent bigram-only candidate (an
overwhelmingly more frequent bigram-only word can still outrank a barely-seen trigram one, matching this
app's established soft-preference
philosophy for a more-specific-but-sparse signal, see S-01/A-05). The trigram table starts empty and grows
purely from the user's own typing (no bundled trigram data is shipped); a two-word context resets everywhere
the existing one-word bigram context already does (field change, an external caret move, the G-02 whole-word
delete).

D-365: a self-learned bigram's own raw reinforcement count (+1 per use) is rescaled the same way D-411
already rescales a learned unigram - log-scaled so a well-established personal pairing (50 uses, matching
D-411's own reference point) competes fairly against the bundled corpus's own bigram counts, instead of an
unscaled flat sum letting an unrelated but merely-common bundled pairing swamp it outright. Ranking only,
same as D-411 - never a gate (A-06's own bigram-count merge threshold keeps reading the raw, unscaled count
directly).

D-366: this Stupid Backoff blend is no longer limited to the blank-slate prediction shown before a word is
typed - the same personal trigram signal now also elevates a matching candidate's ranking in the ordinary
prefix-completion/fuzzy-suggestion list while the word is actively being typed, and in autocorrect's own
candidate ordering, rather than silently falling back to the plain (now-rescaled) bigram signal the moment
the first letter lands.

D-429: a learned bigram and a learned trigram each now carry their own last-touched timestamp too (the same
`last_touched` tracking D-388 already established for individual learned words), and D-365/D-411's own
recency boost (an additional ×1.5 while touched within the past 14 days, settling back to the plain scaled
value once that window passes) now applies to both alongside the existing frequency rescaling - a
frequently-used bigram/trigram pairing that has gone quiet is no longer permanently as competitive as one
still in active use. This also closed the one remaining place still scoring a trigram match by its literal,
unscaled raw count directly (S-07's own blank-slate prediction before a word is typed) - it now goes through
the identical rescaled, recency-aware value every other n-gram contribution in this ranking already uses.

D-327: the bigram and trigram context is learned for *every* committed word, including a bundled word typed
in its own already-canonical casing (W-04) - only its *unigram* frequency is deliberately not reinforced (to
keep the Learned Words editor free of plain vocabulary), but which word follows it is still recorded, so
next-word prediction accumulates for ordinary everyday vocabulary ("Mein" -> "Schatz") the same way it does
for a genuinely self-taught word. Before D-327, the unigram-skip path also skipped the n-gram context
entirely, so no next-word prediction ever built up for bundled vocabulary typed in its canonical casing -
defeating S-07's own purpose for the most common words in the language.

### S-08 - Time-Pattern "Uhr" Suggestion
A typed time in `HH:MM ` form (trailing space required) always suggests the German word "Uhr" as a
completion, independent of the ordinary dictionary/n-gram ranking - only while German is the active language
(D-410: previously offered regardless of active language, a genuine cross-language leak, not a deliberate
design choice).

### S-12 - "km/h" Speed-Unit Completion (D-376)
Two independent trigger points, both offered with a deliberately maximal score so either always sorts first,
mirroring S-08's own reasoning - `"km/h"`, containing `/`, can never itself be a tokenisable dictionary word
the ordinary candidate search could find on its own. Unlike S-08's "Uhr", not gated by active language -
`"km/h"` is the identical SI notation in German and English alike, not a genuinely language-specific word.

- **Trigger 1**: the composing token is exactly `"km"` - `"km/h"` is offered as a completion competing
  directly with the plain `"km"` candidate.
- **Trigger 2**: `"km/"` has just been committed (the user typed the `/` themselves instead of tapping the
  Trigger-1 chip) - the glued remainder `"h"` is offered, with no leading space inserted (the `/` is already
  there), completing to `"km/h"`.

### S-09 - Early Typo Recovery via Neighbour-Prefix Escalation (D-328)
A single keyboard-neighbour typo early in a long word ("vetmut…" for "vermut…") is invisible to both the
literal prefix scan (no shared prefix) and the full-token edit-distance search (still far out of budget while
the word is only partially typed), so the intended completion would not surface until the whole token was
typed. D-328 adds an escalation: when the literal (and umlaut-unfolded, D-144) prefix found nothing at all
and the token is at least 5 characters long, prefix completions of every single-position neighbour-substituted
prefix are also tried (e.g. "vetmut" → "vermut" → "vermutlich"), each fed back through the same
Umlaut.unfoldCandidates + unigramsByPrefix loop, so a typo plus a missing umlaut is resolved together
("twtsachl" → "tatsächlich"). Digit neighbours are skipped (never a word-initial letter); the number of
variants is capped. Suggestion-only by construction — S-02 (never the exact input) and A-04 (blacklist) apply
unchanged. Runs in the deferred/background pass (no main-thread cost), so it adds no per-keystroke latency.

### S-10 - Manual Reclaim Chip
D-62's reclaim (§58) normally fires reactively the moment the caret lands on an existing word - but D-351/
D-351-followup found real fields (Gemini's search field; Total Commander's rename-date field) whose own
cursor-handle rendering cannot tolerate that reactive composing-region change, so the reactive mechanism is
deliberately suppressed there (`reclaimOnCaretMoveSuppressed`). This chip is the explicit, always-available
workaround: tapping it fires the same underlying reclaim immediately (no debounce - a deliberate single tap
needs none) and unconditionally, ignoring the suppression entirely, since bypassing it is the chip's whole
purpose. Not gated to the suppressed fields specifically; available in every field as a general manual
trigger.

D-414 originally shipped this as a dedicated button in the extra row (§14); D-414-followup migrated it into
the suggestion bar itself as a true *visibility* toggle rather than an enabled/disabled one - shown only when
the bar would otherwise be empty (never displaces a real suggestion) and a reclaim is genuinely possible right
now (composing empty, a real word touches the caret on either side), gone entirely rather than dimmed
otherwise. The chip and S-07's own post-commit next-word predictions are structurally close to mutually
exclusive already: an ordinary commit leaves a delimiter, not a letter, touching the caret, so the two are
almost never both live at once. Its visibility is tracked by a debounced check that is deliberately *never*
gated by `reclaimOnCaretMoveSuppressed` - only the reactive *reclaim itself* is suppressed in Gemini/Total
Commander, not this read-only check, so the chip correctly appears exactly where it is needed (a plain tap
into an older word there) instead of staying hidden until some unrelated action happens to trigger it.

D-414-followup (v2): the visibility check reads the caret's surrounding text via one atomic
`getExtractedText()` round-trip fed into `WordExtent.reclaim()` - the same mechanism the real reclaim commits
with - rather than two separate `getTextBeforeCursor`/`getTextAfterCursor` calls, which could observe two
different document snapshots on a fast-moving caret (the same class of staleness D-347 v2 already fixed for
the real reclaim itself) and briefly show the chip with nothing genuinely at the caret.

Separately, D-62's own reclaim now also runs, deliberately, in three more cases that used to stay suppressed
too: a single (non-held) Backspace always reclaims immediately, even in a suppressed field - the suppression
exists only for the cursor-handle-*drag* case a single keystroke never triggers; a genuine backspace *hold*
reclaims exactly once, when the hold ends, never on every repeat tick (reclaiming per tick would reintroduce a
real, previously-felt performance cost for an unrelated reason, D-138); and G-05's double-tap-Shift toggle
reclaims first when nothing is composing yet (see that addendum).

A Reclaim/Cycle combination (tapping again to cycle through suggestion candidates once already reclaimed) was
discussed and deliberately not pursued - a real design problem surfaced (a cycled candidate would need its
own frozen candidate-list snapshot, independent of the live suggestion recomputation the changing composing
text would otherwise trigger, plus open questions around Backspace-during-cycle and casing) with no
sufficiently clean resolution yet. Left for a possible future round with a cleaner concept, not implemented.

D-421: two further gaps, both reported from real Google Keep use. First, a field's own *initial* caret
position - the very first tap that focuses a field and places the caret at the same time - is delivered only
through `EditorInfo`'s `initialSelStart`/`initialSelEnd`, never guaranteed through a subsequent
`onUpdateSelection` callback; the reactive reclaim was only ever wired to that callback, so a word already
touching the caret the first time a field was focused was never reclaimed until the caret moved a *second*
time. `onStartInputView` now schedules the same reclaim (and the chip's own visibility refresh) from the
field's initial selection directly, through the identical debounced path the reactive caret-move case already
uses. Second, because that first, never-auto-reclaimed position left the chip visibly showing, a subsequent
tap into a *different* word - which does reclaim normally - read as the chip flashing on and immediately off,
though the true general cause is broader than that one leftover case: the chip's own visibility check is
re-evaluated live on every suggestion-bar render, with no awareness of whether an automatic reclaim for the
same position is already scheduled and about to resolve a moment later. A new `reclaimPending` flag, set for
the duration of the reactive reclaim's own debounce window (never set at all in a suppressed field, where
nothing is ever automatically pending), now suppresses the chip for exactly that window, closing the race
regardless of which caret move triggered it.

D-421-followup: the flash still reproduced - the `reclaimPending` guard above only protects the *debounced*
path, but tapping from one still-composing word straight onto another reaches a genuinely different,
synchronous path (`onUpdateSelection`'s own "external caret move while composing" handling, D-406): it clears
the just-abandoned word's composing state (resetting `reclaimPending` to false along with it) and calls
`armShiftForNextWord` immediately, whose own `showSuggestions()` call renders with the new position not yet
reclaimed at all - `scheduleReclaimAndChipRefresh()` was never wired into this path in the first place, so no
reclaim was actually pending yet for the chip's own guard to catch. Now schedules the identical debounced
reclaim/chip-refresh here too (gated on a genuinely collapsed caret, mirroring the "composing already empty"
path's own guard) before the existing immediate re-arm, so the chip's own next render already sees a pending
reclaim rather than a resolved-but-untouched position.

D-421-followup (v2) - regression: `onStartInputView` scheduling the reclaim/chip-refresh unconditionally on
every fresh field broke D-36's own direct-paste chip and D-142's own credential list - both are built directly
against the suggestion bar, deliberately bypassing the ordinary `showSuggestions()`/`SuggestionController`
pipeline entirely (D-142's own KDoc), but the scheduled debounce's own `reclaimEnabledRunnable` still
unconditionally re-renders through exactly that ordinary pipeline ~100ms later, which knows nothing about
either special chip and silently wiped whichever one had just been shown back to an empty bar. Fixed by
gating the scheduling on whether the field's own initial display already claimed the bar - a field with a
credential list or paste chip showing has nothing useful left for the reclaim/chip-refresh to add, and a
genuinely empty field (the only case reclaim on initial focus could ever matter for) never has one of these
special chips to protect against in the first place.

### S-11 - Dual-Casing Chips for a Genuinely Ambiguous Noun (D-404-followup)
§6 rule 5 leaves a genuinely ambiguous noun (`CapitalisationEngine.isAmbiguousCasing`, `NOUN,OTHER` - e.g.
"Weg"/"weg") with no automatic casing correction, since context alone cannot decide which is meant - but the
ordinary suggestion pipeline still only ever showed *one* casing of such a word, since capitalisation is
derived purely from the live typing context at render time, never from a candidate's own already-cased form;
both collapsed to the identical ambiguous-default-lowercase text. A dedicated, unranked chip slot (mirroring
how the "Gelernt: X"/credential chips already bypass ranking entirely) now offers both casings independently
while the composing token is only a genuine *prefix* of the ambiguous word, so either can be picked while
still typing; once the token exactly matches the ambiguous word (case-insensitively), only the *other* casing
is offered - the one actually typed is never duplicated as its own suggestion (S-02), unconditionally, with no
exception for the autocorrect setting being off. Scans the call's own already-fetched ranked candidates rather
than issuing a separate dictionary query, so an ambiguous word only ever gets its dual chips when the ordinary
search already considered it relevant enough to surface - naturally subject to the same suggestion-bar
slot-budget crowding as everything else, with no extra ranking logic of its own. A tap commits the chip's own
already-cased word verbatim, never re-run through `capitalise()` (which would derive a different casing from
context and silently discard the very choice the chip exists to offer).

---

## 6. Capitalisation

### Autocorrect Hierarchy

1. **Explicit user input always wins.** The token's own first character, exactly as it stands once typed, is never changed by any rule below - symmetric in both directions (D-405): an explicit uppercase is never lowercased, and an explicit lowercase is never uppercased either.
2. **Sentence start is a *live* typing aid only, never a commit-time correction (D-405).** The first word after `.`, `!`, or `?` followed by a space - or after a deliberate line break, which also begins a new sentence (see the comma-terminated-line exception below) - has Shift pre-armed *before* the word is typed, so an ordinary "forgot to capitalise" keystroke already lands upper-case without any further action. This is the entire mechanism: once a token reaches commit, whatever casing it already carries **is** the answer, under rule 1 above - there is no second, independent re-derivation from sentence position at commit time any more. A user who explicitly Shift-disarms the pre-armed capital and types lower-case on purpose gets exactly that, permanently, not silently overwritten back. (Before D-405, this rule doubled as a commit-time override with no protection for a deliberate lower-case choice - the change is a bug fix, not a new feature: rule 1's "explicit input always wins" was never actually symmetric until now.)
3. **Pure nouns:** A word that exists exclusively as a noun (no verb, adjective, or preposition form) is capitalised automatically.
4. **Known proper nouns:** Are capitalised.
5. **Ambiguous words** (existing as both a noun and another part of speech): No automatic correction. Capitalisation is offered as a suggestion in the bar (S-06). Example: "gegenüber" (opposite/facing) - its prepositional use is far more frequent than its nominal use, so no intervention occurs.
6. **LLM exception:** If the sentence context is unambiguously nominal and LLM confidence is high, the LLM may capitalise - never as a default, only at high certainty.

Part-of-speech annotations and frequency distributions are derived from the bundled dictionary. Rules 3/4
depend on this tagging being unambiguous; a genuine noun/verb homograph mis-tagged as a pure noun will
mis-fire rule 3. This is a recognised, only partially addressed data-quality issue (a handful of confirmed
homograph pairs have been re-tagged to the ambiguous case; a scan found roughly 140 further candidate words
of the same shape, deliberately left unfixed as an open safety-vs-fluency product decision, not a bug to be
silently resolved).

Rules 3/4's outcome is now visible in the suggestion bar *before* it silently applies, via S-06's extended
scope covering pending capitalisation-only changes - not only pending spelling substitutions. Rule 2 has no
equivalent pending state to preview any more (D-405) - it is resolved live, before the word is even typed,
so there is nothing left pending by the time a suggestion could show one.

This entire section (all of §6) is bypassed for email-mode, URL-mode, and login-field fragments (§10-§12),
and for one of the app's *own* fields that explicitly declares itself opted out of suggestions
(`TYPE_TEXT_FLAG_NO_SUGGESTIONS`, the standard Android signal for this - e.g. the Learned-Words casing-edit
field, W-01): all of these commit verbatim, with no capitalisation transform, autocorrect, or dictionary
learning of any kind. D-303: the `TYPE_TEXT_FLAG_NO_SUGGESTIONS` check is scoped to the app's own package -
several real third-party apps (confirmed: Google Keep's own note-body field) set this exact flag for
unrelated reasons of their own (typically only to suppress Android's native spell-checker underline), so the
bypass must never fire for a field outside this app or it silently disables AdaptKey's own suggestions/
autocorrect/capitalisation in ordinary third-party apps.

### Addendum to G-05 - Shift State After Backspace
See §4.

### Editor-Mandated Capitalisation
When the target field declares `TYPE_TEXT_FLAG_CAP_SENTENCES`, `TYPE_TEXT_FLAG_CAP_WORDS`, or `TYPE_TEXT_FLAG_CAP_CHARACTERS` (read from `EditorInfo` and queried per position via `InputConnection.getCursorCapsMode()`), the case the field requires is treated as an authoritative baseline, ranking directly below explicit user input (hierarchy rule 1). The remaining linguistic rules (nouns, proper nouns, ambiguous words) never lowercase a position the field requires to be capitalised. A field reporting no caps flag at all (`CapsMode.NONE`) still gets Shift live-armed at a genuine sentence start, exactly as if the field had requested sentence-case (D-405: this is the same live-arming mechanism rule 2 above now relies on exclusively, not a separate commit-time behaviour) - AdaptKey's own rules apply regardless of what the target field requests, unless overridden by the email/URL/login bypass above.

### Delayed Shift Against Surprising Field Capitalisation *(configurable)*
With `TYPE_TEXT_FLAG_CAP_WORDS` or `TYPE_TEXT_FLAG_CAP_CHARACTERS`, every word start is auto-armed to uppercase - including mid-sentence words, which is surprising relative to normal typing. A user who reflexively presses Shift to capitalise the next word would instead toggle the already-armed uppercase back to lowercase. To prevent this, a Shift press that would switch from a field-mandated uppercase to lowercase is ignored during a short grace window after the word start. Once the window elapses, Shift toggles normally, so a deliberate lowercase override remains possible. The guard applies only to field-mandated capitalisation outside a regular sentence start; ordinary sentence-start capitalisation is unaffected. The window length is configurable via a slider from 0 to 500 ms (C-07); a value of 0 disables the guard entirely.

### No Sentence Start After a Comma-Terminated Line *(configurable, default on)*
In multi-line text (typically e-mails), a line whose last non-whitespace character is a comma does **not** end a sentence: the next line that contains text - blank lines in between are skipped - is **not** treated as a sentence start and is therefore **not** auto-capitalised. This covers the standard German salutation, where the greeting line ends in a comma and the body continues in lower case:

```
Hallo Max Mustermann,

danke für deine schnelle Antwort.
```

Here "danke" must stay lower case. No semantic recognition of the salutation is required; the purely structural rule *"a content line immediately following a comma-terminated line is not a sentence start"* is sufficient, and this pattern does not occur in any other everyday context. The remaining linguistic rules still apply (nouns and proper nouns are still capitalised; explicit user input still wins). This behaviour is bound to a boolean setting (C-10) so it can be disabled, but the **default is on**.

### No Sentence Start After Known Abbreviations and Enumerators
A period does **not** start a new sentence when it terminates a known abbreviation or an enumerator, so the following word is **not** auto-capitalised (unless another rule - a noun, a proper noun, or explicit user input - independently applies).

- **Known abbreviations:** abbreviations whose canonical form always ends in a period, either already present in the dictionary (e.g. `usw.`, `z. B.`, `d. h.`, `ca.`, `Nr.`) or learned from the user's own input. Such an entry is flagged as *abbreviation* so its trailing period is understood as part of the token rather than a sentence terminator.
- **Enumerators:** a run of digits followed by a period at a list position (e.g. `1.`, `2.`, `10.`) is an ordinal / list marker, not a sentence end.

---

## 7. Autocorrect Policy

### A-01 - Valid Words Are Protected, With a Bounded Override
If the typed word exists in the dictionary, no automatic substitution occurs by default - even if a similar
word is more frequent. This is not an absolute veto: a known word can be overridden by a correction
candidate that is (a) a cost-1 (single adjacent-key) edit, **and** (b) confident enough per D-353's own
graduated confidence measure (§36) - originally a flat "100× more frequent" bar (D-244, raised from an
original 50× after a real regression, see `AdaptKey-History.md`), now a log-scaled ratio score compared
against C-22's own configurable threshold, calibrated to the same real cases the flat bar was. An
unknown-but-plausible regular-verb inflection of a known infinitive (e.g. "beurteilst" from "beurteilen")
is protected unconditionally, with no ratio check, since it has no independent frequency to compare. The
same protection applies to an unknown-but-plausible regular adjective comparative/superlative of a known
positive (e.g. "zuversichtlicher" from "zuversichtlich") - excluding a positive that resolves to a noun,
since German nouns take no comparative/superlative degree at all. This
protection is removed entirely for a blacklisted word (A-04) - the bundled blacklist deliberately includes a
few cross-language confusables (e.g. `due`, `sue`, `ddr`, `aks`) specifically so ordinary autocorrect can win
over them despite each being a genuine dictionary entry in some language.

D-403: the 100× override above never fires against a word the user has personally taught the keyboard -
whether fully self-taught (W-01) or a deliberately different-cased override of an otherwise-bundled entry
(W-04) - regardless of how low its own frequency is. This is a deliberate exception to the ratio itself, not
merely a higher bar: a freshly-learned word's own frequency is set to exactly its reinforcement count
(starting at 1, see W-02), a fundamentally different kind of number than a bundled dictionary's real corpus
frequency, the thing the 100× ratio was actually calibrated against - without this exception, almost any
newly-promoted word (an acronym, an abbreviation) stayed permanently vulnerable to being silently corrected
away by any ordinary, moderately common cost-1-adjacent word, no matter how many times it had already been
deliberately taught.

### A-02 - Punctuation Is Not a Context Reset
A comma does not clear the prediction context. N-gram patterns such as `", dass"` or `", die"` are trained and suggested as independent entries. The mini-LLM sees the full sentence and is not subject to this limitation in any case.

### A-03 - Language Detection from Context
A character-trigram classifier identifies the current writing language from recent words, gated to at least
two words of context (a single word's own sample is too statistically noisy). Suggestions and autocorrect
are filtered per language. The classifier's own per-token routing interacts with the explicit active-language
toggle (G-01): a configurable run of consecutive words classified in a different language than the one
currently active (C-23, D-398, default 5) triggers a real language switch, not merely per-token filtering -
or never does, when that threshold is set to 0 (off).

### A-04 - Blacklist for Unwanted Words
Words can be permanently excluded from suggestions and autocorrect. The blacklist is persisted in SQLite and
survives app updates, with a category distinction between a bundled entry and a user-added one. A small
bundled cross-language-confusables set (`due`, `sue`, `ddr`, `aks`) is seeded by default (see A-01), alongside
a curated set of pre-1996-spelling-reform relics of otherwise ordinary common words (e.g. `daß`, `muß`,
`Rußland`) - hand-picked against the bundled dictionary's own real corpus frequencies (kept only where the
modern spelling is demonstrably the dominant, living form in the same corpus), deliberately excluding any
proper noun/surname/place name sharing the same ß-vs-ss shape, whose own spelling is not an error to correct.
Blacklisting keeps a word typeable/known (quoting genuinely old text still works) while it can never surface
as its own suggestion again. The blacklist editor (C-05) shows only user-added entries by default - a bundled
entry is rarely of interest and should rarely be removed at all - with an explicit toggle to reveal bundled
entries too. Its own language selector opens on whichever language the keyboard itself currently/last had
active (G-01), not always the first entry in the list - the same default the Learned Words editor (W-01) also
uses. Also interacts with the provisional pending-blacklist mechanism for a self-taught word
(G-04/W-01). The exclusion is lasting across every learning pipeline, not merely suggestions/autocorrect: a
blacklisted word is never counted toward W-02's promotion threshold and never force-learned (D-13) either, so
simply retyping it enough times cannot silently promote it straight back into the learned dictionary.

### A-05 - Retroactive Word Split on Missed Space
D-410: the inseparable-verb-prefix veto, the feminine-`-in` exception, and the regular-verb/adjective-
inflection protections described below are all specifically German grammar - they apply only while German is
the active language (`LanguageRules`, resolved per active language; every other language gets a no-op that
never vetoes a split on these grounds). Before D-410 these ran unconditionally regardless of active language.

When the current token is not a valid word and is not a plausible regular-verb inflection or adjective
comparative/superlative (see A-01), and it can be split into two dictionary words at (or near) the position
of a space-ambiguous tap (T-05) or a fully missed
space, the offending character is replaced with a space (or a space is inserted) and the token is committed
as two words. The current gating: each half must individually clear a minimum frequency floor; a split where
**both** halves are nouns is rejected (a function-word-plus-noun pair, e.g. "und"/"das", is accepted); prior
bigram co-occurrence between the two halves is a scoring signal only, never a gate, so a first-time-typed
compound typo can still split correctly even with no recorded history. A left half that is one of a fixed set
of German inseparable verb prefixes or productive negation/intensifying prefixes (`ver-`, `zer-`, `ent-`,
`emp-`, `be-`, `ge-`, `miss-`, `er-`, `un-`, `ur-`, `wider-`) is rejected outright (e.g. "unglücklich" is never
split into "un"/"glücklich") - deliberately excluding the variable separable-or-inseparable prefixes
(`über-`/`um-`/`durch-`/`unter-`/`voll-`/`hinter-`/`wieder-`), each of which is also, itself, a common
standalone German word, so blocking them would reject far more genuine two-word missed-space splits than the
compound-prefix false positives it would prevent (`"wieder holen"` vs. `"wiederholen"` being the textbook
case). A prefix in the protected set is itself exempted from this rule once its own standalone dictionary
frequency exceeds a fixed ceiling - `er-` (the pronoun) is the one case in practice, since blocking it
unconditionally would prevent a genuine split like "erkommt" -> "er kommt". Symmetrically, a right half of
exactly `-in` is rejected when the left half is a plausible German masculine agent/relation noun stem - the
single most productive German feminisation pattern (a known noun ending in `-er`: "Lehrerin", "Spielerin",
"Fahrerin") plus a curated set of common non-`-er` stems (e.g. "Arzt"/"Chef"/"Koch"/"Nachbar"/"Student") -
so "Schneiderin"/"Zuschauerin"-shaped words are never torn into a noun plus the standalone preposition "in".
Unlike the left-prefix case, no frequency-ceiling exemption applies here: "in" is always an extremely common
standalone word regardless of context, so the same mechanism cannot separate a genuine "noun in" phrase from
a feminine-noun false positive - checked, not guessed. Half resolution also tries
umlaut/ß-unfolding (e.g. "uber" resolves via "über") before giving up, per the umlaut guiding principle - the
restored, diacritic-complete spelling is what actually gets committed for that half (e.g. "gehortes" ->
"gehört es", not the literal "gehort es"), while the still-composing preview's own colour spans (S-05) stay
positioned over the literal typed characters, since that is what is actually on screen while the token is
still being edited. A split is vetoed if it would lose to a high-confidence single-word correction instead.
A live two-span colour preview is shown while composing (S-05). Spatial proximity alone is never sufficient -
a valid linguistic split is required.

D-352: a configurable mode (C-21) governs how eagerly this whole mechanism may act, independent of the
gating above - **Automatic** (the default, described above), **Chip only** (a found split is never applied
silently; it is instead offered as a pinned, position-1 suggestion chip - S-01's own "a mid-word connector
split" precedent, D-122 - so the user decides), or **Off** (the mechanism does not run at all, no chip
either). Scoped to A-05 alone; A-06 and D-122's own mid-word connector-split suggestion (triggered only by
the user deliberately re-editing an existing word, never by ordinary forward typing) are unaffected.

D-306: the "not both nouns" gate is only as good as the bundled dictionaries' own part-of-speech tags - a
mistagged or untagged entry can defeat it. Reported directly: a learned compound ("Tippstil") split into
"Tipp"/"til" because "til" (a rare given-name fragment, frequency 26) carried no proper-noun tag at all.
Root-caused to a bundled-dictionary data-quality issue, not a code bug: both `dict_de.tsv`/`dict_en.tsv` are
Wikipedia-corpus extractions and carried thousands of untagged rows (markup tokens, foreign proper nouns,
scientific epithets, software names) that could never be rejected by this gate in the first place, since an
empty part-of-speech set can never match "noun". Cleaned up once (D-306) - every remaining entry in both
dictionaries carries a valid tag - but a mistagged-yet-tagged entry (like "til" itself, retagged `NOUN`) could
still recur; a bundled-dictionary contribution should always carry a real part-of-speech tag, never leave it
blank, per `AdaptKey-Language-Contribution-Guide.md`.

D-420: the left half's own capitalisation context now derives from the original typed token's first
character, not from `SplitResult.resolvedLeft` (always lower-case by contract). Reported directly: a
sentence-start "Komischerweise" split into "komischer weise" instead of "Komischer Weise" - the split's own
first-character explicit-capital signal was silently discarded, since it was being probed against the
already-lower-cased left half instead of the real typed text. Closes the previously-open design question
here (a split now preserves whatever capitalisation the user actually had at the split point - a sentence-
start-armed capital or a deliberately hand-typed one are structurally identical signals) without needing any
sentence-start-specific logic: Rule 1 ("explicit user input always wins") simply reaches the left half
correctly now. The right half is never a sentence start by construction, so its own context is unaffected.

D-404-followup: **a split may now only be attempted once every other correction mechanism has already had its
own chance and found nothing** - reported directly from a real repro ("Trobaner" splitting at the "b" instead
of correcting to "Trojaner", a genuine adjacent-key slip T-02's own raw-coordinate search would have found).
The raw-coordinate-correction search (T-02/T-03) is now computed *before* the split decision (previously
computed only afterwards, as a last resort once a split had already had its own, unprotected chance to win),
and a non-null result vetoes the split exactly like `bestCorrection()`'s own high-confidence flag already did -
a split must never win merely because it was decided first, only because nothing safer was actually available.

D-404-followup: a curated set of common German adverbs/particles that frequently form the first element of a
genuine compound noun (`schon`, `wohl`, `hoch`, `tief`, `voll`, `halb`, `fern`, `nah`, `kaum`, `fast`) is
rejected as a split's left half whenever the right half is itself a known noun (e.g. "Schonfenster" is never
split into "schon"/"Fenster") - bound to the German `LanguageRules` implementation (D-410), like every other
grammar-specific veto in this section, not applied cross-language. Unlike the inseparable-verb-prefix set
above, each of these is also an entirely ordinary standalone word, so the veto only fires when immediately
glued to a noun - the shape a genuine compound takes, since normal phrase syntax requires an inflected ending
before a noun ("volles Glas", never bare "voll Glas"); the same particle followed by something that is not a
noun ("schon gut") still splits normally. Curated by hand, not algorithmically derived, and not claimed to be
exhaustive.

### A-06 - Retroactive Word Merge on Spurious Space
The inverse of A-05. When a space was registered from a letter-ambiguous tap (T-05) and the following token is not a valid word, the system tests whether removing that space and prepending the letter inferred from the tap's x-coordinate yields a valid or high-probability word. If so, the spurious space is removed and the reconstructed word is committed (e.g. `aber  ald` -> `aber bald`, where the intended `b` landed on the space bar). As with A-05, a valid linguistic result is mandatory; the spatial signal only nominates the candidate.

### A-07 - Post-Commit Autocorrect Undo
After a commit that involved any correction - spelling autocorrect, diacritic/umlaut restoration, or an A-05
split - a backspace issued after the commit restores the originally typed text, including rejoining a split
back into one word. One deliberate exception widens this beyond "a correction": accepting a B-03
hyphen-compound chip also arms this same window, even though it replaces typed text with something the user
chose rather than something the app silently changed - see B-03 for why. The undo window survives any number
of intervening **whitespace** keystrokes (Space,
Enter) rather than closing on the very next keystroke regardless of kind - only a genuine non-whitespace
character (a letter, digit, or punctuation mark) closes it, or the caret being moved away explicitly (see
below). While any such whitespace still sits between the caret and the committed text, a Backspace removes
only that one character ordinarily - exactly as any other Backspace would - and the window stays armed;
only once the caret is genuinely back at the committed text itself, with nothing left in between, does a
further Backspace trigger the actual revert. This is why an accidental Enter reached for while meaning
Backspace, or several of them, does not permanently forfeit the revert - each is simply backspaced away
normally first, the revert only firing once, at the real boundary. This applies only to a plain tap after the
commit; it does not affect the whole-word delete gesture (G-02) or the ordinary shift-state-after-backspace
behaviour (Addendum to G-05). The undo also:

- **Un-learns the dictionary side** exactly, symmetrically reversing whatever unigram/bigram reinforcement
  or brand-new-entry creation that commit had just caused, deleting an entry outright if its count reaches
  zero rather than leaving a zero-frequency ghost. A split's undo reverses both halves.
- **Un-learns the touch model, but only narrowly:** only if the correction came from the raw-coordinate
  fallback (T-02) is the one substituted tap position reversed via T-03's exact algebraic inverse. An
  ordinary spelling/diacritic/split correction never touches T-03, since it carries no information about
  which physical key was mistouched.
- **Decrements the pending-learn counter** correctly, respecting the variable threshold (W-02: 2 for an
  ordinary word, 4 for a token recognised as a suspected unsplit compound or carrying an embedded mid-word
  capital).

D-403/D-359: the revert itself learns nothing towards the originally-typed word - not the unigram, not even
its n-gram context (the next confirmed retry, below, learns that anyway, so nothing is lost by not doing it
twice). Instead, exactly one further commit of that same, unchanged text is granted a single unimpeded
attempt: every correction mechanism (autocorrect, diacritic restoration, A-05 split, A-06 merge) is bypassed
for it, so it reaches the ordinary commit-time learning pipeline uncorrected - an ordinary +1 towards
promotion, identical to any other first-time, never-corrected word, never a shortcut around W-02's usual
threshold. This one-shot protection is consumed by the very next commit regardless of whether it actually
matches the reverted text - typing anything different in the meantime simply is not protected, and ordinary
correction rules apply to it immediately. (This exception is scoped to an ordinary corrected-word revert; a
revert of a wrongly-applied A-05 split still force-learns the rejoined word immediately, as it always has.)

This mechanism depends on the `onUpdateSelection` self-recognition guiding principle (§1) and on a correct
single-character delete immediately after a mid-word reclaim (never deleting the whole reclaimed word) - both
must be kept intact by any future change in this area.

A genuine, non-collapsed text selection always takes priority over this mechanism - and over every other
pending special state (A-06's pending merge) - regardless of what happened just before it. (A-12's own
deferred space, D-416, needs no comparable carve-out here - it is never physically present to begin with, so
there is nothing a selection could collide with.) Backspace on a selection bluntly deletes it; any other key
bluntly replaces it. Neither
triggers the undo (or any other special behaviour) for that one keystroke; ordinary handling, including
whatever pending state a *later* commit newly arms, resumes cleanly from the next keystroke.

D-416: an A-05 split that also happens to end a sentence (e.g. `"ehvnicht."` -> `"eh nicht."`) needs no special
priority rule against this mechanism any more - A-12's deferred space is never physically in the document to
compete with the undo window's own armed tail in the first place, so the ordinary Backspace/undo handling
above already does the right thing unassisted: the first Backspace deletes the bare trailing mark itself (per
A-12's own new Backspace behaviour), a later one lands exactly at the split boundary and triggers the undo
normally. (Before D-416, the eager model's own physically-inserted space needed a dedicated carve-out here -
removed along with the eager mechanism itself, not merely relocated.)

Moving the caret away from an undo-eligible commit explicitly - a tap elsewhere - never lets a later Backspace
revert the wrong text. Rather than trying to catch the exact moment the caret moves (unreliable - an ordinary
edit can itself generate more than one reactive selection-update callback, so a single "was this expected"
check can be consumed by the wrong one and misfire), the revert verifies directly against the real document
immediately before ever touching it: only if the text right before the caret still exactly matches what the
window armed does the revert proceed; otherwise it is discarded outright and the Backspace falls back to an
ordinary single-character delete instead, exactly as if no window had ever been armed. This also covers any
other way the document could have changed underneath the window, not only an explicit tap.

### A-08 - Compound-Word Peeling (Suggestion Only)
For an unrecognised token, a known noun (4+ characters), optionally preceded by a German linking element
(Fugenelement), is peeled off the token's front and offered as a compound-split suggestion. Never applied
automatically.

### A-09 - Wide Fuzzy Match for Long Unknown Tokens (Suggestion Only)
A long unrecognised token is additionally matched against the dictionary with a wider edit-cost budget
(cost-4) than the ordinary correction search uses. Suggestion only, never auto-applied.

### A-13 - Missed-Backspace Recovery (Suggestion Only, D-377)
Recovers a badly garbled token where a Backspace was missed and a neighbouring key was hit instead (e.g.
`"welxmche"` for `"welche"` - an accidental `x` for `c`, then a Backspace attempt that landed on `m` instead).
Deliberately evidence-gated at the input level rather than a generic dictionary-coincidence search (a wider
A-09 candidate window was considered and rejected on false-positive-risk grounds - removing arbitrary
character pairs until something spells a known word has no real connection to what the user actually typed):
a token position only ever becomes a removal candidate when the *real recorded tap* that produced it landed
within one key's own width/height of Backspace's actual on-screen position - "one key away" is sufficient,
resolution-independent by construction (scales with the key's own measured size, not a fixed pixel radius).
Only that character and its immediate predecessor are ever removed together (mirroring what a real Backspace
press would have deleted right there), never an arbitrary window.

Gated identically to A-09: only attempted once the ordinary/fuzzy dictionary search has found nothing
*obvious* for the token - a real prefix completion (including a D-144 umlaut-unfolded or D-328 neighbour-
substituted-prefix variant), a close (cost ≤ 2) fuzzy match, or a recognised unhyphenated compound - and only
during the deferred/expensive fallback pass, never on the hot per-keystroke path. D-431: this is deliberately
*not* the same as the suggestion bar ending up completely empty - something can almost always be suggested,
including via A-09's own wider (cost ≤ 4), suggestion-only, "never trusted enough for autocorrect" last-resort
fallback. A coincidental last-resort match must not by itself veto A-13's own evidence-based recovery (e.g.
the `"welxmche"` example above: a genuine but rare German word, `"welsche"`, sits at a cheaper generic edit
distance than the correct `"welche"` reconstruction, purely because one of its differing letters happens to
sit on a keyboard-adjacent key - a coincidence with no bearing on what A-13's own tap-geometry evidence
already establishes). When both a last-resort match and an A-13 recovery exist side by side, both are offered
- A-13's own chip is simply pinned ahead, not exclusive. Suggestion only, exactly like A-09 - deliberately
never wired into the silent-correction chain, since even strong input-level evidence is not judged safe
enough to auto-apply without confirmation.

### A-10 - Mid-Word Connector-Split Suggestion
While re-editing mid-word, an unresolved bottom-row connector character (`c v b n m`, per T-05) between two
otherwise-recognisable halves is offered directly as a split suggestion, bypassing the ordinary bar-evidence
ranking entirely.

### A-11 - Backspacing Back Into a Recently-Learned Word Un-Teaches It
The last few words the app's own learning pipeline actually acted on (reinforced, counted toward promotion,
or promoted - whichever of A-07's own three outcomes applies) are remembered a while past the moment they were
committed. Whenever a plain backspace subsequently lands with the caret sitting right at the end of one of
these words again, it is un-learned on the spot - exactly the same reversal A-07 performs (count decremented,
an entry removed outright once its count reaches zero), just triggered differently. Unlike A-07, this is not
tied to the one keystroke directly after the commit: it survives any number of intervening keystrokes and
fires whenever the caret eventually returns, most commonly after backspacing back through one or more stray
line breaks that prematurely committed (and so, by the ordinary learning pipeline, also reinforced) a
half-typed word. No judgement about whether the word "looks like" a real one is ever made - a word that was
never actually reinforced or counted (e.g. one already shipped in the bundled dictionary) simply has nothing
to undo. A "Gelernt: X" chip (W-03) still showing for a word that gets un-learned this way is removed from the
bar immediately. Never interferes with an A-07 undo already in progress right after a commit - the two never
compete for the same keystroke.

This is a plain, symmetric reversal only - it never marks the word pending-blacklist and never blacklists it,
unlike a deliberate forget through drag-to-trash (G-04) or a removal from the Learned Words review screen
(W-01), both of which *do* mark the word pending-blacklist as a deliberate-intent signal (D-254: the "Gelernt:
X" chip's own shallow "Vergessen" zone, W-03, is deliberately exempt from this too now, for the same reason -
see W-03). Backspacing back into a word is the opposite kind of signal - typically just a typo/false-start
being cleaned up mid-flow, with no implication the word itself is unwanted - so treating it as a recurrence
candidate would risk escalating an ordinary word straight to a permanent blacklist entry the next time it is
typed (and, for the motivating half-typed-word case, it usually *is* typed
again immediately afterwards, correctly this time).

### A-12 - Deferred Auto-Space After Sentence Punctuation, With a Punctuation-Run Mode
D-416 replaced this feature's original eager mechanism (auto-inserting a real space the instant the mark
commits) with a **deferred** one: a sentence-ending punctuation mark (`.`, `!`, or `?`) - and, since D-320, a
comma too, treated identically for the space itself (see below for the one respect in which it differs) -
inserts nothing physically when it commits. It only arms a standing mode - the position right after the mark
is a genuine pending boundary, exactly like §6 rule 2's own live capitalisation pre-arm already works, just
now covering the space as well as the capital. The space (and, for `.`/`!`/`?`, the capital) is materialised
only once the very next real keystroke resolves what actually belongs there:

- **A letter or digit** that genuinely starts a new word: a real space is inserted first, then the character
  (capitalised per the already-armed Shift state, same as always). **Exception (D-416-followup):** for a
  genuine sentence-ending mark (`.`/`!`/`?`, never a comma - a comma never arms a capital in the first place),
  if Shift's own auto-armed capital has been explicitly disarmed right there before the letter is typed, no
  space is materialised either - continuing directly with no separator, on the understanding that a deliberate
  lower-case override at exactly this position means "carry on, do not start a new, separated word". Whether
  Shift is currently armed is read live at the same moment the space decision itself is made, no state stored.
  **D-404-followup refinement:** Shift being off is only treated as that deliberate override when this position
  would genuinely have auto-armed a capital in the first place (re-derived fresh here, the same
  `sentenceStartBefore` check `armShiftForNextWord` itself uses) - for a known abbreviation/enumerator (§6's
  own "No Sentence Start After Known Abbreviations and Enumerators" rule, e.g. `"bzgl."`), Shift is correctly
  never armed to begin with, so there is nothing to override there: the space still materialises normally,
  even though Caps stays off, since two genuinely separate tokens are still being typed. Reported directly: the
  space-key's own pending dot stayed lit (a space genuinely was still pending) but no space actually
  materialised once the next letter arrived after a protected abbreviation - the original version could not
  tell "never armed" apart from "deliberately overridden".
- **Another mark from the same set**: glues directly onto the previous one (`"!?"`, not `"! ?"`) - no space
  is ever materialised between them, and the mode re-arms at the new position, so a run of any length
  (`"!?!"`, `"..."`, `".,"`) still glues together exactly as before, the eventual space only ever trailing the
  whole run.
- **A digit that continues a decimal number in progress** (D-320): glues directly onto the mark instead of
  getting a leading space - `"3"` `"."` `"1"` `"4"` (or the German `"3"` `","` `"14"`) comes out as
  `"3.14"`/`"3,14"`, not `"3. 14"`/`"3, 14"` - only when the mark was specifically `.` or `,` (never `!`/`?`,
  which carry no numeric meaning) **and** the character immediately before the mark was itself a digit. This
  is a soft, position-based heuristic, not a semantic one: a genuine new sentence that happens to start with a
  bare digit immediately after a numbered enumerator (e.g. `"Kapitel 3."` followed by a fresh sentence
  `"2 Punkte..."`) is a rare, accepted false-glue risk, matching this app's established soft-preference
  philosophy elsewhere (S-01/A-05/S-07) rather than an absolute rule.
- **A closing double-quote** (D-370): glues directly onto the mark too, exactly like another mark from the same
  set - materialising the space here would land it inside the quotes (`"Ja. "` instead of `"Ja."`). The pending
  mode carries forward past the quote rather than resolving at it: the very next keystroke that actually starts
  a new word (or continues the run) still sees the mark underneath the quote and materialises the space (and,
  where applicable, the capital) exactly as if the quote had never been typed. A quote landing directly after a
  bare mark with nothing composing is structurally unambiguous as closing - opening a new quote there with no
  space in between does not occur in ordinary text - so no open/close tracking is needed to make this call.
- **An explicit Space**: needs no special handling at all - nothing was physically written to reconcile, so it
  is simply an ordinary Space press.
- **A Backspace**, with nothing typed since the mark committed: deletes the mark itself directly, on the very
  first press - there is no phantom space in the way any more to consume first. This is the one deliberate,
  permanent behaviour change from the eager mechanism this replaced.
- **Enter**, or the caret moving away/the field being left with nothing typed since: nothing was ever
  physically written, so there is nothing to clean up or leave dangling - a bare mark simply sits as the last
  character typed, exactly as if any other character had been typed there instead.

For `.`/`!`/`?`, capitalisation is armed for the next word immediately on committing the mark - safe to do
right away under this deferred model, unlike the eager one, since the position already counts as a pending
sentence boundary the moment the mark lands (see [SentenceBoundary.withPendingTerminatorSpace]). A comma's
own pending space never arms capitalisation - comma was never one of [SentenceBoundary]'s own terminator
characters, so a word typed straight after a comma's (eventually materialised) space is capitalised no
differently than after any ordinary Space, matching how a comma is never a sentence terminator regardless of
what triggers the space after it. Does not apply inside a login/URL field (E-01/U-01/P-01) - a `.` inside an
e-mail address or domain name must never grow an uninvited space into the middle of it - nor when the
punctuation lands mid-word (re-editing an existing token, D-119/D-120's own split-at-caret case).

A small, quiet dot above the space key's own label (see S-05's own "quiet confirmation" precedent) is the one
on-screen indication that the pending state is armed at all, since - unlike the former eager mechanism - there
is otherwise nothing visible to confirm it before the next real keystroke resolves it.

D-416-followup: the dot is also re-evaluated - and correctly cleared - after a plain caret move away from the
punctuation mark with nothing typed (a tap elsewhere, a drag), not only once a new word starts composing. It
previously stayed lit indefinitely in that case, since nothing re-checked it outside a commit/field-entry or
an actual keystroke starting a new word. Refreshed on the same debounced, composing-empty-caret-move cadence
S-10's own Reclaim chip already uses.

D-370: the dot also stays lit through a closing double-quote committed right after the mark, matching the
closing-quote bullet above - the space really is still pending at that point, only deferred past the quote, not
resolved, so the dot going dark there would be actively misleading.

---

## 8. Hyphen Handling

### B-01 - Hyphen as a Segment Separator
A hyphen splits a compound word into independent tokens for dictionary lookup, the learning model, and the
suggestion bar. "XYZ-Anteil" trains "Anteil", not "XYZ-Anteil" as a unit. A hyphen-committed fragment is
treated identically to a space-committed one by both bigram learning and next-word prediction (S-07).

### B-02 - No Automatic Capitalisation After a Hyphen
The segment following a hyphen is lowercase by default at commit time. Exception: the segment is a known
proper noun. A suggestion-bar chip for any candidate always displays its capitalised form through the same
§6 pipeline the commit path uses, so a chip can never visually disagree with what will actually be committed.

D-373: a second, independent exception - a segment whose own predecessor in the same hyphen chain was itself
capitalised also capitalises, even when the segment itself carries no proper-noun tag of its own ("Nord" in
"München-Nord", never independently tagged proper, still capitalises because "München" was). Two different
checks depending on *why* the predecessor was capitalised, a deliberate hybrid design: if the predecessor was
itself at a sentence start, a bare capital there proves nothing about its own grammatical status (any word can
open a sentence) - only propagates when the predecessor is independently a known noun/proper noun in the
dictionary, the same signal the plain B-02 exception above already uses. Anywhere else in the chain, the
predecessor's capital is trusted directly - a hyphen chain reads as one unit, so once one segment's
capitalisation is settled (however it got there, including a further D-373 propagation earlier in a longer
chain), the next segment should agree.

D-373-followup: reported not working at all - the fix above only ever changed the eventual *committed*
casing, never what Shift shows armed while the next segment is still being typed, so a user watching the
keyboard (rather than the finished word) saw nothing happen. Shift is now live-armed too, the moment the
hyphen's own next segment starts composing, mirroring what a genuine sentence start already does - but
deliberately only for the non-sentence-start branch above, the one that needs no dictionary lookup at all
(the identical raw signal the commit-time check itself uses there). The sentence-start branch still needs its
real dictionary check and stays commit-time-only, exactly like B-02's own original proper-noun exception
already was, silently capitalising at commit without ever live-arming Shift either - an existing, already-
accepted asymmetry this follow-up does not change.

D-373-followup (v2): a real device log showed the live-arm above was actually taking effect - the next
letter *did* commit correctly capitalised - but the keyboard's own Shift indicator visibly flickered on then
off in between, reading as "not working" even though the end result was right. Root cause: the debounced D-62
reactive reclaim ([reclaimWordAtCaret]) fires ~100ms after the hyphen commits (composing is empty there too,
the same trigger condition the live-arm above uses) and calls the same context-capture a second time - which
re-arms Shift correctly - immediately followed by its own, differently-motivated re-derivation
([armShiftForNextWord], D-313's reactive purpose for a caret landing on an existing word), which silently
overwrote it back to the generic "not a sentence start" answer. A new one-shot flag, consumed exactly like the
existing [shiftArmedByDelete] guard right next to it, tells that re-derivation to stand down when the arm it
would otherwise clobber was this specific hyphen-propagation case.

### B-03 - Proactive Completion Of A Repeated Hyphen-Compound
Each individual segment of a hyphen-joined chain is still learned/suggested exactly as B-01 already
describes - unchanged. Independently, the whole chain itself (two segments, or more - "Rhein-Main-Gebiet" is
no different in kind from "Trogata-Team", just longer) is *also* learned as one combined unit once it has been
typed out in full, unbroken by any non-hyphen delimiter, at least twice. Deliberately lower than W-02's own
"suspected compound" threshold (4): that higher bar exists to protect against a heuristic *guessing* wrong
about an ordinary token, whereas a literal, deliberately-typed hyphen chain repeated in exactly this spelling
carries no such false-positive risk - an accidental match is vanishingly unlikely. There is no cap on how many
segments a chain may have; a longer chain simply needs more hyphens typed in an unbroken run to close.

Once promoted, the full compound is offered proactively while typing a *prefix of its first segment alone* -
e.g. typing "Trog" alone can already surface the complete "Trogata-Team" as a suggestion, before its own
hyphen has been typed at all. This suggestion is a dedicated, always-pinned chip shown ahead of every ordinary
suggestion for the same token - never competing with them by score, since a completion needing this many
additional characters would otherwise be discounted far too aggressively by S-01's own prefix-distance
ranking to reliably win. The first time a given compound is promoted, it gets the same "Gelernt: X"
confirmation (W-03) an ordinary word's own promotion does.

Tapping the compound chip commits it in full and, uniquely among every suggestion kind in this app, arms a
dedicated A-07-style undo window of its own: an immediate Backspace right after reverts exactly that
acceptance (restoring the partial prefix that was actually composing beforehand) and un-reinforces the
compound's own count by exactly what accepting it just added. This is a deliberate exception to the rule that
an ordinary suggestion-bar tap is never undoable (the user's own deliberate choice, not a correction) - a
proactive whole-compound completion is a large enough, more surprising insertion that an immediate "take it
back" is worth the exception.

Typing the segments out normally (not via the chip) never triggers this undo, and never lets A-11 (backspacing
back into a recently-learned word) un-teach the compound's own count either - only each individual segment's
own count is affected the same way it always has been. The compound's own count is only ever reversed through
the chip's own dedicated undo above.

### B-04 - Underscore as a Word-Joining Character, Never a Separator
Unlike every other punctuation mark - including the hyphen (B-01) - the underscore never ends the composing
token: it extends it exactly like a letter, so a technical identifier (`MEINE_VARIABLE`) composes as a single
token rather than being torn into fragments at every underscore. A token containing `_` is additionally treated
as fully opaque technical content: no autocorrect, no fuzzy/compound-split suggestion, no §6 capitalisation
transform, and no personal-dictionary learning of any kind applies to it - stronger than the hyphen's own
segment-by-segment treatment, and closer in spirit to how an email/URL/login fragment bypasses §6 entirely
(§10-§12), but scoped by token shape (contains `_`) rather than by field type.

---

## 9. Word Prediction & Adaptive Learning

### Three-Tier Architecture

| Tier | Technology | Latency | Activation |
|---|---|---|---|
| 1 - N-gram | Personal dictionary (SQLite) | <5 ms | Always active |
| 2 - Language detection | Character-trigram classifier (on-device) | <10 ms | After punctuation, on suspected language switch |
| 3 - Mini-LLM | ONNX Runtime | variable | When N-gram confidence falls below threshold, and only if enabled and installed |

The tier-3 model is not bundled with the app: it is installed by the user via a browser download plus a
system file picker (no `INTERNET` permission is used by the app itself), and can be independently enabled
or disabled (default on) regardless of whether a model file happens to be installed.

### Language Packs *(D-280)*
Only English's dictionary (tier 1) ships inside the app - every further language's dictionary, including
German's and Greek's, is an optional install, mirroring the tier-3 model's own browser-download-plus-
file-picker mechanism exactly (no `INTERNET` permission needed for this either). A pack also carries that
language's own AltGr/long-press hint set (L-05/D-281) alongside its dictionary, so a language's typing
experience - not only its dictionary - is tailored to it, not a one-size-fits-all German-derived default.
A settings screen (reachable at any time, not only during onboarding) lists every language with a real,
hosted pack, shows whether it is currently installed, and offers install/remove per language; installing or
removing one takes effect immediately, without restarting the keyboard. First-run onboarding offers the same
install step, pre-suggesting a language purely from the device's own configured system languages (no network
call - the app already knows in code which languages it could ever offer). See
[`AdaptKey-Language-Contribution-Guide.md`](AdaptKey-Language-Contribution-Guide.md) for exactly what a new
language needs (a dictionary and hint set always; a new compiled keyboard layout only when no existing row
geometry - QWERTY, QWERTZ, ... - is suitable, e.g. a language expecting AZERTY or a non-Latin alphabet - see
L-01) and how to contribute one.

D-307: since the app has no `INTERNET` permission at all, a language pack's own update state is tracked
entirely locally - each catalog entry carries a version number, bumped by hand whenever its hosted content
actually changes (e.g. D-306's dictionary-data cleanup), and compared against whatever version was recorded
at the last successful install. An already-installed pack whose catalog version has since moved on shows
"update available" instead of "installed" - a lightweight, no-download-needed hint, requiring an AdaptKey app
release to move (the version lives in this app's own compiled code) - no separate "check for updates" network
step, and no polling. An install predating this mechanism is simply assumed to be version 1 (every pack's own
starting version), so it reads correctly the moment its catalog version moves past 1, without needing any
migration step of its own.

D-308: the *authoritative* version is not this compiled-in hint, but a `version.txt` entry inside the
archive itself, so a language pack can be revised by its own maintainer (e.g. a community contribution)
without requiring a new AdaptKey release at all. The Download/Import buttons stay available for every
language at any time, even one that already reads as "installed" with no hint of an update - re-importing
always re-reads the freshly downloaded archive's own version and only actually applies it (overwriting the
installed files, reseeding the dictionary database) when that version is strictly newer than what was last
recorded; otherwise nothing on disk changes and the user is told it is already current. An archive with no
version file at all is treated as version 1, matching a first install's own default - see the language
contribution guide for why this makes omitting the file a real, if not fatal, mistake for a maintainer who
intends to ever revise their pack later.

D-310: every file inside a language-pack archive (`dict.tsv`/`bigram.tsv`/`hints.tsv`/`version.txt`) uses a
fixed, un-suffixed name - a downloaded pack is extracted into its own per-language subfolder on the device
rather than a shared flat directory, and the same fixed names are used for the bundled-English assets too
(one naming convention regardless of whether a language ships inside the APK or is downloaded separately).

### Adaptive Learning
A tier-3-confident-but-tier-1-unknown word feeds back into the same dictionary-learning pipeline as any
other confirmed word. Because that learning signal is only generated when tier-1 didn't already know the
word, the LLM is invoked less and less often as the dictionary absorbs what it knows - an emergent behaviour,
not an explicit schedule. Battery load therefore decreases automatically as usage grows.

---

## 10. Email Mode

### E-01 - Email-Mode Keyboard
A dedicated bottom row (`?123`, `@`, `-`, a narrow space, `.`, Enter) replaces the ordinary bottom row for a
field detected as an email address. Detection is reliable for a field whose `InputType` variation declares
`TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`WEB_EMAIL_ADDRESS`; for an ambiguous field, a weak hint-text keyword
signal (checked across supported UI languages) nudges the extra row open and flashes a manual toggle rather
than auto-activating. `@` takes the comma key's primary slot (comma is demoted to its own popup); the dash
key is funded from the space key's own width, not by growing the row, and carries `_` as its single
alternative. The period key's long-press popup is a locale-resolved TLD list (`.com`, the device country's
own ccTLD, `.net`, `.org`) with the ccTLD pre-selected at the popup's centred position. The space key shows
only a plain glyph (never the language label) and the language-switch swipe (G-01) is disabled on it. All
fragments commit verbatim: no autocorrect, no §6 capitalisation, no learning, and Shift is never auto-armed.

---

## 11. URL Mode

### U-01 - URL-Mode Keyboard
A dedicated bottom row (`?123`, `https://`, `www.`, `/`, a narrow space, `.`, Enter) replaces the ordinary
bottom row for a field detected via `TYPE_TEXT_VARIATION_URI`, sharing the same locale-resolved TLD popup
mechanism as email mode (E-01). A manual toggle lets the user switch back to the ordinary letter keyboard
while still inside a URL field (e.g. to type a search query in a browser's address bar); it defaults on
whenever a URL field is entered and resets per field visit. Autocorrect and suggestions are fully suppressed
(the bar stays empty), but the clipboard-paste chip (§16, V-01) is not suppressed.

---

## 12. Credential & Privacy Management

### P-01 - Login-Field Detection
A focused field is classified as one of: none, username, email, or password. Email and password variants
are detected reliably from `InputType`; username has no reliable field-level signal and relies on a weak
hint-text keyword match or a manual extra-row toggle (§14, R-01).

### P-02 - Separate Credential Learning & Storage
Username/email values typed into a detected login field are learned immediately (no threshold) into a
credential store entirely separate from the ordinary dictionary; while such a field is focused, its
suggestions are drawn exclusively from this store, bypassing the ordinary dictionary/tier-1/tier-3 pipeline.
A password field is never learned from and never offered suggestions, unconditionally. After `@` in an
email-shaped login field, live frequency-ranked domain completion is offered from previously seen domains.

### P-03 - Opt-In Contact-Derived Email Suggestions *(default off)*
When enabled, on-device contacts are read live (never persisted) to widen email-suggestion ranking. The
permission is requested only from the Settings screen, never proactively while typing.

### P-04 - Never Save Credentials *(default off)*
When enabled, stops recording any *new* credential entries; does not remove or hide already-stored entries
or suggestions.

### P-05 - Credentials Review Screen
A dedicated settings screen lists stored credential entries, with a "Delete all" action and a per-entry
long-press remove action offering a "Copy" option first.

### P-06 - Autofill Inline Suggestions
On API 30+, a field P-01 classifies as a real credential field (username/e-mail/password -
`loginFieldKind != NONE`) can render the platform's own opaque, platform-drawn inline-suggestions view in the
suggestion bar's slot (e.g. surfacing a password manager's own entries) instead of AdaptKey's ordinary
suggestions. D-326: inline suggestions are never even requested for an ordinary field - a real device log
traced an active autofill service answering, unstably, for an entirely ordinary text field (Signal's message
compose box), repeatedly evicting whatever the ordinary bar was showing (e.g. a V-01 clipboard chip);
`loginFieldKind` is already known synchronously by the time the request would be built, so declining outright
for a non-credential field is both simpler and stronger than merely delaying the takeover - there is then
nothing left that could ever evict the ordinary bar for that field. D-325: as a remaining safety net for a
genuine credential field itself, a non-empty response only actually takes over the slot once it has gone
unchallenged (by a further response, empty or not) for a short debounce window, in case even a real
credential-field responder answers unstably - AdaptKey has no way to inspect what an inline suggestion
actually renders (the view is opaque, drawn remotely by the autofill service itself), so response *stability*
is the closest available proxy for "is this worth displaying" rather than any content inspection. A genuinely
stable suggestion (the ordinary case this feature exists for) is simply delayed by the debounce window before
still winning.

---

## 13. Learned-Words Management

### W-01 - Separate Learned-Words Store
Every word the keyboard has personally learned from typing lives in its own store, structurally isolated
from the bundled dictionary so that a bundled-dictionary update or reimport can never affect it. A dedicated
review/edit screen lists and allows removing learned words directly - the only way to remove a word that
currently matches the live input, since S-02 prevents such a word from ever appearing as a suggestion (and
therefore from ever being reachable via drag-to-trash, G-04). Tapping an entry also allows its own casing to
be corrected in place (e.g. fixing a preferred all-caps acronym spelling that was learned with the wrong
casing) - deliberately restricted to a casing-only edit: the save action stays disabled unless the edited text
is case-insensitively identical to the original entry, so this can never be used to substitute an entirely
different word under the original's own frequency/history. A genuinely different word still has to be typed
and learned normally. The screen's own language selector opens on whichever language the keyboard itself
currently/last had active (G-01), not always the first entry in the list.

D-388: a second selector next to it sorts the list either alphanumerically (case-insensitive, locale-aware -
an umlaut/accent sorts at its natural alphabetic position, not by raw byte value) or by recency (most
recently touched - promoted, reinforced, re-cased, or restored from a backup - first), defaulting to
alphanumeric (this screen's primary use is browsing/searching for a specific word, not reviewing recent
activity). Frequency is not shown at all - internal bookkeeping the promotion threshold uses, not something a
user reviewing this list needs to see - and is not offered as a sort option either. The Blacklist editor
(C-05) and the saved-credentials screen (D-142) are both always sorted the same alphanumeric, case-insensitive
way, with no picker of their own - both stay small enough that browsing them any other way was never asked
for.

### W-02 - Variable Learn-Promotion Threshold
A word is ordinarily promoted from "pending" to permanently learned after 2 uncorrected occurrences. A token
recognised as a suspected unsplit compound, or one carrying an embedded mid-word capital, instead requires 4
occurrences before promotion - both checks are re-evaluated fresh each time (nothing cached), so a later
change in the detection logic reclassifies an already-pending word retroactively. A-07's undo correctly
decrements this counter regardless of which threshold applies. A single letter is never eligible for learning
at all (D-247) - the most common source is a fragment left behind by an unintended Enter mid-word, not
anything meant to be learned.

D-388: a word's frequency, once promoted, starts at however many times it was actually seen while pending
(2 or 4, per the threshold above) rather than always resetting to 1 - a small fidelity fix with no effect on
promotion timing itself, only on the starting count a freshly-promoted word carries forward.

### W-03 - "Gelernt: X" Promotion Confirmation *(D-247)*
The moment a word is genuinely promoted to the learned dictionary (W-02's threshold crossed this exact
commit - never on an ordinary reinforcement of an already-learned word, which would fire constantly and add
no information) shows a dedicated, distinctly-coloured "Gelernt: X" chip in the suggestion bar, pinned ahead
of the ordinary next-word predictions rather than participating in their ranking. A plain tap dismisses it
(nothing changes - "doing nothing" already means "stays learned", matching the rest of this affordance's own
logic). Dragging it upward arms a two-zone variant of G-04's own drag-to-trash gesture, distinct from the
single-zone behaviour every ordinary suggestion keeps: a shallow zone unlearns the word only - a plain,
consequence-free forget with no blacklist involvement of any kind, not even the provisional pending-blacklist
mark G-04's own ordinary drag-to-trash uses for a self-taught word (D-254: deliberately different here, not a
mirror of G-04 as originally designed - this chip already offers its own explicit, immediate path to
permanence one zone down, which makes G-04's own recurrence-escalation safety net redundant on this shallow
zone, and actively surprising in practice: a word freshly promoted via a premature commit is often retyped
correctly again moments later, and treating that as a "recurrence" of an unwanted word silently ended it up
permanently blacklisted); a deeper zone blacklists it immediately and permanently, bypassing G-04's own
bundled-vs-self-taught origin check entirely (a freshly-promoted word is never bundled, so that check would
otherwise always resolve to the shallow outcome regardless of how far the drag travelled) - deliberately
stronger than the shallow zone, for when the user is certain the word should never be reconsidered even if it
recurs.

### W-04 - Case-Sensitive Override of a Bundled Word's Own Casing
A bundled word typed in its own already-canonical casing has nothing to learn (unchanged - see A-04's own
learning-pipeline exclusions for the analogous blacklist case) - except that its *n-gram context* (which
word follows it) is still recorded (D-327, see S-07), so next-word prediction works for ordinary bundled
vocabulary too. Typed persistently in a *different* casing
(e.g. a preferred all-caps acronym spelling - "MSCI", "MCU" - the bundled asset happens to store differently),
it is instead promoted through the same W-02 threshold as any other not-yet-known word, becoming its own
case-sensitive learned entry. Once such an override exists, it - not the bundled entry - is what the
suggestion bar offers and what an edit-distance correction resolves to; the bundled entry's own frequency and
part-of-speech tags still contribute to ranking underneath it. A-01's own case-insensitive "is this word
known" check is unaffected either way - both the bundled and the overriding casing are always recognised
regardless of which one the user happens to type on a given occasion.

A casing difference confined to the word's first character does *not* count as a persistently different
casing and is never promoted this way - every mechanism that can recase a word (live sentence-start Shift
arming, D-405; a pure/proper noun via §6's own hierarchy; an editor's field mandate) only ever touches that
one character, so an ordinary bundled-lowercase word
that merely happened to start a sentence (`"das"` -> `"Das"`) must not be mistaken for a deliberate override
and start counting toward promotion. Only a difference reaching beyond the first character (as any genuine
deliberately-typed casing like an acronym does) is eligible.

### W-05 - Automatic Expiry of Unused Learned Words *(D-389, C-24)*
A learned word that has gone untouched (never reinforced by a further commit, re-cased, or otherwise
re-learned) longer than a configurable window is automatically un-learned - the same permanent, irreversible
removal G-04's drag-to-trash performs, not a mere frequency demotion. "Untouched" reads the same
`last_touched` stamp D-388's own recency sort already uses, so ordinary continued use of a word resets its
own clock for free; nothing separate tracks "usage count" for this purpose.

The window is a coarse choice (C-24: 1 month / 4 months / 1 year / never, in that order), not a raw
duration control, per the user's own explicit preference - default **"never"**: expiring the user's own
accumulated vocabulary is an opt-in behaviour, never something that happens silently unless deliberately
turned on. Shown as concrete durations, not abstract labels - the user explicitly asked to see plainly how
long an entry actually survives, not have to infer it. What is actually *stored*, however, stays the
abstract level (`early`/`medium`/`late`/`never`), deliberately decoupled from the displayed duration text -
if the concrete meaning of a level is ever redefined in a later app update, a device that already has e.g.
`medium` saved picks up the new meaning immediately, with no migration needed; only the hand-maintained label
strings need updating alongside the code change to keep what is shown in sync with what actually happens.
The setting sits directly beneath the Learned Words editor's own entry in the Dictionary settings category,
immediately visible from the one place a user would think to look for it, and (D-419) shows its own
currently selected duration directly in the settings list, not only once its dialog is opened.

Runs as a small, once-a-day housekeeping sweep (throttled against the real time it last actually ran, not
this service's own process lifecycle), across every installed language's own learned-word store, not only
the currently active one - each language accumulates its own learned vocabulary independently (§10/D-280),
so a language not currently in use would otherwise never have its own stale entries swept at all. Deliberately
scoped to individual learned words alone, not the learned bigram/trigram tables (S-07) - D-429 gave those
their own `last_touched` column too, but only for S-07's own ranking-recency boost (D-365/D-411's mechanism);
extending this expiry sweep itself to bigrams/trigrams remains a deliberately separate, not-yet-built
extension.

D-389-followup: a word that is part of a W-04-style word family (linked via the same base-form link D-404's
family reprocessing/lookup-linker/manual "Grundform" edit establishes) is never expired on its own - explicit
user request, so a single stale member does not tear a hole in an otherwise-alive family. A family's own
effective last-touched moment is the *most recent* stamp among all its members; the whole family is only
un-learned together, once every single member has individually gone untouched past the window. In practice
this means a frequently-used family member (e.g. a common inflected form) indefinitely keeps its rarer
siblings alive too - the intended effect, not a side effect to work around. A word with no family link at
all (the common case) is simply a family of one, expiring exactly as it would without this rule.

---

## 14. Extra Row (Swipe-Up Panel)

### R-01 - Extra Row
An upward swipe anywhere on the keyboard reveals a row sitting above the suggestion bar, hosting: an
always-visible emoji-panel button (no setting gates it - opening the emoji panel is described under L-03),
a settings-app shortcut, a session-only touch-zone-visualisation toggle (§17, T-06), a manual credential-mode
toggle (also flashes on a weak login-field signal, §12), and a manual URL-mode toggle (visible only while a
URL field is focused, §11). A downward swipe closes the row first, and a second downward swipe (with the row
already closed) dismisses the keyboard (G-03). Activating a button in the row does not auto-close it. The
clear-clipboard button that used to live here now sits in the suggestion bar itself instead (§16, V-03) - as
does the manual Reclaim chip that used to live here too (D-414/D-414-followup, §5, S-10).

---

## 15. Symbol Layer & Calculator

See L-03 (`?123` key), L-06/L-08 (number row / catch-all page), and L-07 (calculator layer) in §2.

---

## 16. Clipboard Paste

### V-01 - Direct-Paste Chip
When the clipboard holds content, a direct-paste chip occupies the suggestion bar's slot, using the
target field's native paste action (rather than committing the clipboard text as a literal string) so that
app-specific paste behaviour is preserved. The chip is suppressed for a stale clip (older than 5 minutes)
and for a genuine non-text file clip. A sensitive clip (flagged by the platform) shows a masked preview.
Clipboard content is auto-cleared after a deferred, content-matched check for sensitive material. A
dedicated clear-clipboard button lives in the extra row (§14, R-01).

### V-02 - "Erste Zeile" and "Erster Code" Chips
Alongside V-01's own whole-clipboard chip, up to two further chips can appear in the same bar slot, each
shown only when its own extraction actually differs from the full clipboard text (so a single-line/
single-token clipboard does not grow redundant duplicate chips): "Erste Zeile" commits just the clipboard's
first non-blank line; "Erster Code" commits the first plausible "code" token, found via a chain of
specialised parsers tried in order (a URL-aware parser first, extracting a query-parameter's value or the
last path segment; a generic fallback last, pulling the first contiguous alphanumeric run, ignoring
surrounding punctuation). Unlike V-01's native-paste action, both commit the extracted text directly, since a
native paste cannot paste only part of the clipboard. Sensitive-clip masking and the post-paste auto-clear
both apply identically to these chips. An explicitly iterative feature - the "code" extraction in particular
is expected to need further tuning as real examples surface, not a closed, exhaustively-designed parser.

### V-03 - Clipboard-Bar Clear Button
Whenever the suggestion bar shows at least one clipboard chip (V-01/V-02), a dedicated clear button occupies
the bar's own right edge, reserved space so the chips themselves keep only the row's remaining, still
horizontally-scrollable width. Tapping it wipes the clipboard (same effect as R-01's former clear-clipboard
button, which this replaces) and the bar reverts to its own ordinary state, since there is nothing left to
offer a chip for. Not shown at any other time - no space is reserved for it while the bar holds ordinary
suggestions, a credential list, or a "Gelernt: X" chip.

### V-04 - Clipboard Peek Button (D-36-followup)
V-01's own direct-paste chip only ever appears once, right when a field first opens with the clipboard
already holding a fresh clip - typing anything replaces it with ordinary suggestions, with no way back to it
short of leaving and re-entering the field. A second button occupies the exact same square V-03's clear
button does (the two are mutually exclusive: whichever applies at a given moment takes the slot) whenever the
clipboard still holds a clip fresher than V-01's own 5-minute window and the chips are not already showing -
independent of composing state, so it stays reachable mid-text, not only at field-open. Tapping it finalises
any word currently being typed (exactly as a language switch already does) and shows the V-01/V-02 chips
exclusively, with V-03's clear button taking the shared square back. The bar reverts to its own ordinary
state on its own the moment the caret next moves - no dedicated "close peek mode" affordance is needed, since
the existing caret-settle suggestion refresh already overwrites whatever was shown before it. The freshness
check is re-run whenever the answer could plausibly have changed (a fresh field opening, the clipboard
content itself changing, the caret settling on a new position, and V-03's own clear button being tapped -
D-428) rather than on every keystroke, so the button never costs a live clipboard query on the hot typing
path.

D-428: tapping V-03's clear button while V-04's peek chips are showing must not let this button flash back
visible for a moment before disappearing again - the freshness check above is re-run synchronously right
after clearing the clipboard, rather than waiting for the asynchronous clipboard-change notification the
ordinary case relies on, which would otherwise still report the clip as fresh for one render.

---

## 17. Touch-Pattern Visualisation

### T-06 - Touch-Zone Overlay
A settings screen and a live, on-keyboard toggle draw a coloured circle at each key's currently-learned
expected strike point (radius proportional to variance), sourced directly from T-03's model. Doubles as the
result view after a typing-style (re-)selection (K-01).

---

## 18. App Localisation (I18N)

### N-01 - App-Chrome Localisation
The app's own UI strings (settings, onboarding, dialogs, calibration) are localised into English and Greek
in addition to German, selected by the system's own language setting - a separate concern from which
language(s) the keyboard itself types in (§9/G-01).

---

## 19. Diagnostic Logging

### X-01 - In-App Diagnostic Log
A settings-gated (default off) ring buffer logs composing/selection/commit state and key-vibration outcomes,
retained for a short rolling window, viewable/copyable/exportable entirely on-device without a PC or `adb`.
Unconditionally excludes any content typed into a password field, regardless of the toggle state.

---

## 20. Configurable Parameters

| ID | Parameter | Type | Default |
|---|---|---|---|
| C-01 | Key proportions (space bar / comma / full stop / backspace) + the two L-02 spacing sliders | Slider | Gboard-like |
| C-02 | Suggestion re-sort delay | 0-600 ms | 300 ms |
| C-03 | Maximum number of suggestions | Integer (3-10, D-399: widened from a 6 floor) | 8 |
| C-04 | Word confirmation highlight colour, or "no highlighting" (D-298: folds the former separate on/off toggle into this one list); the settings row and its picker dialog preview each colour directly as its own entry's text colour (D-302) | Colour, or off | Green |
| C-05 | Word blacklist | List + categories (bundled/user), editor defaults to user-only view | Seeded with a small bundled confusables + archaic-spelling set |
| C-06 | LLM activation threshold, or "disabled" (D-297: folds the former separate tier-3-enabled toggle into this one list) | N-gram confidence value, or off | medium |
| C-07 | Shift grace window vs. surprising field capitalisation | 0-500 ms | 300 ms |
| C-09 | Persistent number row (with shifted-symbol long-press) | On/Off | On |
| C-10 | No sentence start after a comma-terminated line (§6, e-mail salutation) | On/Off | On |
| C-11 | Long-press popup delay | Slider | see platform default |
| C-12 | Key sound | On/Off | Off |
| C-13 | Haptic feedback | On/Off | Off |
| C-15 | Diagnostic log enabled | On/Off | Off |
| C-16 | Never save credentials | On/Off | Off |
| C-17 | Contact-derived email suggestions | On/Off (permission-gated) | Off |
| C-18 | Pending-blacklist expiry window (G-04) | Days (1-30) | 7 |
| C-19 | Installed language packs (§9) | Install/remove per language | English only |
| C-20 | Double-tap Backspace for autocorrect revert (D-348) | On/Off | Off |
| C-21 | Auto-split mode (A-05, D-352) | Automatic / Chip only / Off | Automatic |
| C-22 | Autocorrect (A-01, §36, D-353/D-407) | Off / Cautious / Medium / Aggressive | Medium |
| C-23 | Automatic language switch threshold (G-01, D-130/D-398) | Consecutive foreign words (0-8, 0 = off) | 5 |
| C-24 | Learned-word expiry window (W-05, D-389) | 1 month / 4 months / 1 year / Never | Never |

Individual feature sections above also document domain-specific, non-configurable defaults (e.g. the
calculator layout's fixed key weights) that intentionally are not exposed here.

D-419: any setting offered as a discrete list of named choices (C-04, C-06, C-24 above; C-21/C-22's own
`LabeledSeekBarPreference` sliders already show their current position inline on the slider itself) shows
its own currently selected entry directly in the settings screen's main list, not only once its own dialog
is opened - explicit user request, since otherwise the only way to check a setting is to tap into it. C-04
does this via its own bespoke coloured-summary preview (D-302); every other such setting appends a fully
**bold** "Currently: X" line beneath its own static, plain-text description instead - bold rather than
plain, so it visibly stands apart from the description above it instead of blending in (also an explicit
user request).

---

## 21. Backup & Restore

### Y-01 - Export / Import (D-278)
A dedicated Settings screen exports every genuinely personal piece of state - every configurable parameter
(§20), the saved username/email store (§12/P-02), and each installed language's own learned-word overlay
(W-01), learned bigrams/trigrams (S-07), user-added blacklist entries (A-04), and still-pending provisional-
blacklist marks (G-04/W-01) - into a single JSON file, and imports one back in. Deliberately excludes the
bundled dictionary/bigram data and bundled/pre-1996-spelling blacklist entries (A-04): those already ship
with the app or a language pack and would only bloat the file with data the target device already has by
other means.

Import is additive, never a wholesale replace, for every kind of data it touches - a learned word's or
bigram's/trigram's count is summed with whatever this device already has for it (mirroring how the ordinary
learning pipeline itself accumulates); a blacklist entry or setting is upserted; a still-pending blacklist
mark is only adopted when this device has no mark of its own yet for that word (so a re-import can never make
an already-pending word look freshly marked, which would delay G-04's expiry window). A configurable
parameter (§20) is the one exception where "import" and "overwrite" are the same operation, since a single-
valued preference has no meaningful separate merge behaviour - the imported value simply wins, exactly as if
the user had changed it themselves in Settings.

A language section only ever exists in an *exported* file for a language the exporting device actually had
installed at export time. On *import*, that section is only applied if the *importing* device currently has
that same language installed (English, always bundled, always counts as installed) - a language pack the
importing device lacks is skipped outright, listed back to the user, and never force-installed or silently
discarded. This matters because every per-language personal store lives in that language's own SQLite
database file, the same one a language-pack removal deletes outright (§9) - so the alternative (creating a
store for an uninstalled language) would either silently resurrect data with no dictionary behind it, or
require inventing a wholly separate "orphaned import" state this feature does not otherwise need.

The file's own format carries a schema version, independent of the app's `versionName`, bumped only if the
JSON shape itself ever changes; a file whose version is newer than the running app understands is refused
outright, with nothing applied, rather than partially imported.

D-304: the exported configurable-parameter section is written in the settings screen's own current display
order (§20), not an arbitrary storage-order dump, so an exported file reads the way the screen itself does.
Two internal flags are never included in an export and never applied on import even if present in an older
or hand-edited file: the diagnostics-logging toggle (§20) - whether a given device records a diagnostic log
is that device's own debugging aid, not a preference a backup should carry to a second device - and the
one-time K-01 calibration-offer flag, whose own underlying calibration data never travels with a backup
either (it lives in its own, separate, never-exported preferences file), so carrying only the "already
offered" flag would silently suppress that one-time offer on a device that has no calibration data behind it.

Stored and transmitted as plain, unencrypted JSON, by explicit user decision: a saved username/email alone is
not considered sensitive enough to warrant it, and password values are never stored by this app in the first
place regardless of this setting (P-02).

---

## 22. Language Pack Update Safety (D-334)

D-334: installing a newer language-pack archive (§9, D-280) must never destroy the user's own learned data,
and the "update available" hint must clear once the user has actually checked, even when the hosted archive
turns out to be stale relative to the compiled-in catalog version.

- **Learned-data preservation on pack update.** Reseeding a pack's bundled tables
  (`TABLE_WORDS`/`TABLE_BIGRAMS`) after a real update wipes only those seeded tables and rewrites them from
  the freshly-imported TSV files - the learned overlay (`TABLE_LEARNED`/`TABLE_LEARNED_BIGRAMS`/
  `TABLE_LEARNED_TRIGRAMS`), the blacklist, and the pending-blacklist marks all stay untouched, exactly as the
  bundled-language reseed (§9, D-178) already does. A whole-database wipe (`deleteDatabase`) is never used for
  an update of an already-installed language - only for a clean removal (§9), where losing learned data is the
  explicit intent.

- **Update-hint suppression after a stale check.** When the user imports an archive that turns out not to be
  newer than what is already installed (the hosted `.zip` is behind the compiled-in catalog version, a known
  operational gap while a rebuilt archive is awaiting its push to `origin/main`), the "update available" hint is
  suppressed for that exact catalog version. The hint re-arms automatically once a future app release raises the
  catalog version past what was dismissed - the user is never permanently stuck with a hint they cannot clear,
  nor silently shown one they already dismissed.

---

## 23. Backspace-Shift Carry (D-335)

D-335: deleting an uppercase character must leave Shift armed so the next keystroke reproduces an uppercase
character (the G-05 addendum). The reactive `onUpdateSelection`-triggered reclaim path (§58) must not
overwrite that delete-derived Shift state with a fresh sentence-start derivation, since the caret sitting
where a capital just was is not a sentence start in the common case. A one-shot flag (`shiftArmedByDelete`)
records that the current armed state came from a backspace, consumed by the next `reclaimWordAtCaret` to skip
its own `armShiftForNextWord` derivation; a subsequent genuine caret move re-derives Shift normally (D-313).

---

## 24. Extended Letter Long-Press Popups (D-336)

D-336: the German QWERTZ layout (and AZERTY, which reuses the same letter-key builder) gained additional
long-press popup alternatives on three letters, appended after each key's existing single secondary hint:

- **a** — `ä` (umlaut, the existing corner hint) plus the Nordic ligatures `æ` and `å`.
- **e** — `€` (currency, the existing corner hint) plus the French diacritics `é`, `è`, `ê`, `ë`.
- **n** — `+` (plus, the existing corner hint) plus the Spanish tilde-n `ñ`.

Each popup pre-selects the key's own secondary hint (the first entry), so a straight-up release still types
the familiar secondary. The remaining entries are reached by sliding. Upper-case forms (`Æ`/`Å`, `É`/`È`/`Ê`/
`Ë`, `Ñ`) are produced automatically when Shift or Caps Lock is armed — the existing case machinery
(`popupDisplayTextFor` for display, `appendLongPressLetter` via `isUpperArmed()` for commit) already
uppercases genuine Latin letters, no per-entry casing data was needed. A user who reassigns a key via the
C-08 editor loses the popup (same invariant as the existing `p`/`o` popups).

The row-agnostic `letterKey` builder (generalised from the former top-row-only `topRowKey`) applies the
(char, hint) → alternatives decision regardless of which row a letter sits in — `a` is QWERTZ middle row but
AZERTY top row, `n` is third row, `e` is top row; the popup set never depended on row position.

---

## 25. Caps-Lock Border Highlight (D-337)

D-337: while Caps Lock is engaged, the Shift key draws a bold stroked border (accent blue `#1565C0`,
3dp stroke) around itself — visually distinct from the momentary light-blue fill flash of a plain key press
(`key_background_pressed` `#A6C8FF`). The border is an overlay stroke only; the key's size, position and
hit-target are unchanged, so neighbour keys are unaffected. Drawn after the background fill and before the
label glyph, so the `⇪` lock indicator stays clear. A brief press still flashes with `pressedKeyPaint` as
before; the persistent border is the additional Caps-Lock-only cue.

---

## 26. Backspace Hold — Slide-Off Continuation (D-340)

D-340: when the user holds backspace and slides their finger off the key without lifting it, the
accelerating backspace-repeat behaviour continues as long as the finger remains in contact with the
touch surface anywhere on the keyboard, only stopping once the finger is actually lifted (`ACTION_UP` /
`ACTION_CANCEL`). A slide-off never aborts the repeat mid-deletion — broadly equivalent to how a
hardware key's auto-repeat stays active while any part of the finger remains down. The repeat's
slide-off cancellation (which previously fired on the system touch-slop, so a small smear already
killed the hold) is now gated on `!backspaceRepeated`: only before the first repeat tick has fired can
a finger movement still cancel it (matching the G-02 word-delete swipe gesture's own prompt
recognition). Refines D-07/D-31/D-56.

---

## 27. Additional Long-Press `à`/`À` on the 'a' Key (D-341)

D-341: the German QWERTZ layout's `a` key has `à` (lowercase) and `À` (uppercase) as an additional
alternative at the end of its long-press popup, after the existing entries (`ä`, `æ`, `å` and their
upper-case forms). Extends D-336's popup set.

---

## 28. German Dictionary — NOUN_OR_VERB Rework (D-342)

D-342: the German bundled dictionary must be reworked so that every word that can serve as both a verb
and a noun is:
- stored in lowercase as the base form,
- attributed with a new compound tag `NOUN_OR_VERB`.

Verb nominalisations (e.g. "Stelle" as the noun derived from "stellen") must be handled the same way.
The goal is to eliminate unwanted automatic capitalisation corrections for words in this class: when a
token like "stelle" is typed and the dictionary knows it can be both the imperative verb "stelle" (lowercase)
and the noun "Stelle" (uppercase), no single autocorrect direction is unambiguous — instead the word is
handled like the existing §6 rule 5 (ambiguous words): two suggestion chips (lowercase and capitalised)
appear, and no automatic capitalisation correction is applied. This is a data-driven fix, not primarily a
code change; the `PartOfSpeech` enum and the split/autocorrect gates that consult POS tags may need
extension to recognise the new compound tag. See also the existing §6 rule 5 / A-05 "not both nouns" gate.

---

## 29. Caps Lock Vibration — Direct Vibrator, Subtle Effect (D-343)

D-343: the short vibration that confirms Caps Lock engagement (G-06/D-337) must use the direct `Vibrator`
path (bypassing the system's own touch-vibration toggle), so it fires even when system haptics are
disabled. The vibration effect itself must be distinctly more subtle than the ordinary per-key haptic
(D-06) — a shorter pulse, lower amplitude, or both — so the user can feel the confirmation without it
being startling. Refines G-06/D-337.

---

## 30. Download Directory Control for Dictionaries / LLM Model (D-344 / D-386 / D-413)

The app has no internet permission, so downloading the language pack / tier-3 model happens in the user's
own browser; "Import" then needs to get at that downloaded file. Current, crystallised mechanism: a plain
`ACTION_OPEN_DOCUMENT` single-file picker, launched fresh on every "Import" tap - the user picks the exact
file each time, the same system-picker consent flow used throughout, needing no manifest-declared
`<uses-permission>` (the app's own "no internet, no storage permission" story is unaffected). `Intent.
EXTRA_INITIAL_URI` best-effort-hints the picker to open directly in Downloads (silently ignored by a picker
that does not recognise the hint's exact document-ID shape - never an error); the intent requests
`FLAG_GRANT_WRITE_URI_PERMISSION` so the post-import cleanup below can actually delete the picked document.

**D-344/D-386, tried and reverted (D-413).** An intermediate design used `ACTION_OPEN_DOCUMENT_TREE` (a
one-time folder grant, persisted and reused by both `LanguagePacksActivity` and `Tier3ModelActivity`) plus
automatic filename-pattern resolution within that folder (`DuplicateDownloadMatcher`, tolerating a
browser-inserted `" (N)"` duplicate suffix) - built specifically so the user would never have to hunt for a
file Samsung One UI's own download sandboxing had silently renamed. On a real device this failed outright:
a recent Android version (further tightened by Samsung's own sandboxing) refuses to let `ACTION_OPEN_
DOCUMENT_TREE` grant the Downloads folder itself at all ("Dieser Ordner kann nicht verwendet werden. Zum
Schutz deiner Daten einen anderen Ordner auswählen.") - a platform-level SAF restriction on exactly the
folder the whole mechanism needed, not a bug in the app's own resolution logic. D-413 reverted to the direct
single-file picker described above; `download.DownloadFolderStore` (the persisted tree grant) and
`download.DuplicateDownloadMatcher` (the duplicate-suffix matching) are gone, along with the upfront "why
we need a folder" rationale dialog a folder-grant request warranted but a single-file pick does not.

**Cleanup (kept from D-386).** `download.DownloadFileSupport.deleteIfRecentlyCreated()` deletes the picked
file via `DocumentsContract.deleteDocument` when its own `COLUMN_LAST_MODIFIED` is no more than
`DELETE_MAX_AGE_MILLIS` (60 seconds, the user's own explicit figure) old - a failed/aborted import leaves the
file alone (for inspection), but a genuinely successful one cleans up immediately. This still works
unchanged against a single `ACTION_OPEN_DOCUMENT` result, needing no folder grant of its own.

**D-386-followup, kept as a safety net even after D-413.** A manually re-picked file is no stronger a
guarantee of correctness than the automatic resolution it replaced - a stale same-named copy from an earlier
download, or the wrong language's archive, are both still one wrong tap away - so both content-level checks
introduced for the automatic-resolution design remain in place for language packs specifically (the LLM
model has no version/language concept to check against):
- **Staleness is reported accurately, not collapsed into one message.** `LanguagePackInstaller.
  compareVersions(packVersion, installedVersion)` (pure, unit-tested) distinguishes three outcomes -
  `INSTALL` (nothing installed yet, or the picked archive is strictly newer), `ALREADY_CURRENT` (exactly the
  same version), and `OLDER_THAN_INSTALLED` (the picked archive is strictly *older*) - where a prior `<=`
  check collapsed the latter two into the same "already up to date" message. An accidental downgrade is now
  surfaced distinctly, never silently treated as "nothing to do".
- **Language identity is verified, not assumed.** `version.txt` gained an optional second line - the pack's
  own declared language code (`"de"`, `"el"`, ...) - which `LanguagePackInstaller.parse()` cross-checks
  against the language actually being imported, throwing `LanguageMismatchException` (a distinct, specific
  "wrong language" message, not a generic import failure) on an actual mismatch. Tolerant of an archive with
  no second line at all (every archive built before this convention existed - never rejected on a missing
  declaration, only a present and wrong one). Both hosted packs (German, Greek) were rebuilt with this line
  (`dictionaries/de/version.txt` 33 -> 34, `dictionaries/el/version.txt` 1 -> 2 - `dict.tsv`/`bigram.tsv`/
  `hints.tsv` themselves byte-identical, verified after rebuild); the language-contribution guide documents
  the format for future community packs.

**Considered and explicitly declined (D-344's original two alternatives to a picker-based fix)**: HTTP
header control (`Content-Disposition`/`Content-Type` tuning on the hosted download) - the app does not
control every possible hosting surface a future community-contributed language pack might use (D-344's own
motivating case was GitHub raw content URLs). A raw-repository-path/Release-Asset approach was similarly
declined - the actual problem was never *where* the file is hosted, only *getting at* it again after the
browser saves it, which any picker-based approach already solves regardless of hosting.

---

## 31. Dictionary Noise — "Bri" Blacklist + Candidate Scan (D-345)

D-345: "Bri" must be added to the bundled blacklist or removed from the bundled German dictionary
outright — it is not a real word and at best an obscure abbreviation, causing only false-positive
autocorrect/split interference. More broadly, the bundled German (and English) dictionaries should be
scanned for every remaining Wikipedia-corpus-extraction-noise entry of the same class: fragments, obscure
acronyms, and markup tokens that carry no real linguistic value. Each candidate should be listed and
confirmed with the user before removal (to avoid silently dropping a genuine rare word someone might type).
Truly obscure acronyms can always be learned individually by the user on demand (D-13/W-01). Extends the
data-quality work started in D-306.

---

## 32. Fuzzy Search Loading Indicator in the Suggestion Bar (D-346)

D-346: when S-09's neighbour-prefix escalation (D-328) — or any other deferred/background fuzzy search —
is still running and has not yet returned a result for the current token, and the hot path found
nothing (the bar would otherwise be empty), the suggestion bar shows a loading placeholder pinned in the
bar's slot. Once the background search either finds a candidate or definitively finds nothing, the
indicator is replaced by the ordinary bar content (or an empty bar). The indicator never appears alongside
real suggestions, never blocks user input, and never delays the next keystroke.

D-362: the placeholder's own visual treatment - bold, noticeably larger than an ordinary suggestion, in a
dedicated warm amber, and animating through `...`/`..`/`.`/`..`/`...` (bouncing, not cycling forward-only)
every 400 ms - deliberately replaces the original static, italic, muted-grey "…" (confirmed too subtle to
actually notice next to ordinary suggestions of the same size and weight). The colour is its own, distinct
from every other chip's meaning - not S-06's verbatim-chip blue, not the muted grey still used for the
emoji-search-query chip. The animation starts full (three dots) and bounces down to one and back, rather
than starting at one dot and cycling up - the chip is often visible only briefly, and a forward-only cycle
was, in practice, mostly seen frozen at a single lone dot, reading as a rendering glitch rather than motion.

---

## 33. Gemini App — Cursor Nub Keeps Disappearing (D-347)

D-347: in the Gemini app (Google's own conversational AI app), the cursor-positioning nub (the small
handle above the insertion caret used to drag the cursor around) flickers briefly and then disappears,
making cursor positioning effectively unusable. Reported by the user on their device, observed with
AdaptKey as the active IME.

A related, more serious data-loss bug was found and fixed from a real device log captured while
reproducing this: dragging the nub could corrupt the surrounding text outright (e.g. `"Test test. "`
losing its space and gaining a duplicated character). Root-caused to `reclaimSurroundingWord()` (§58)
combining a `before` fragment with an `after` fragment read from two different caret positions - the live
caret can genuinely move again, mid-drag, between two separate `InputConnection` reads. A first fix
narrowed the gap (reading `before` immediately before `after`/the anchor) but a second real device log
showed the identical corruption again - two calls issued back-to-back in source are still two independent
round-trips, with a real gap a fast drag can still move through. Fixed for real by deriving `before` and
`after` from the single `getExtractedText()` call already made for the anchor (its own `.text`, sliced at
its own `.selectionStart`/`.selectionEnd`) - one atomic read instead of several, so both fragments
provably describe the same document snapshot. Confirmed on device: the corruption is gone.

The original flicker/disappear symptom itself turned out to be a distinct, real issue with the same root
cause class: the D-62 reactive reclaim (`reclaimWordAtCaret()`) fired synchronously on every single
intermediate caret position reported while the nub was being dragged, and each firing that found a word to
reclaim called `setComposingRegion()` on the real, already-committed text - the Gemini field's own
drag-handle tracking stalls whenever that happens (confirmed: a position where nothing is reclaimed, e.g.
right after a period, never stalls). Debouncing the reclaim (100ms, tuned down from the usual 200ms
elsewhere in this app since a drag reports positions much faster than fluent typing does) removed the
churn *during* continuous movement, but confirmed on device: even the debounced call, the moment it
actually fires once the caret briefly settles, still stops the drag dead right there - Gemini's own handle
appears unable to tolerate a `setComposingRegion()` call at all while a drag might still be in progress,
regardless of timing.

**D-351: the reactive caret-move reclaim is now suppressed entirely for this one field**, identified by
package name (`com.google.android.googlequicksearchbox`) - no structural `EditorInfo` signal distinguishes
"this field's handle cannot tolerate a composing-region change" from an ordinary one, and no other tested
app has shown this behaviour. Typing-triggered reclaim (mid-word live correction once a character is
actually typed) is unaffected everywhere, including Gemini; only the D-62 "instant live correction the
moment the caret lands on a word with nothing typed" convenience is lost in this one app. Explicitly a
special case to keep watching, not a settled, final answer - the user is monitoring real-world use before
deciding whether anything further is needed.

---

## 34. Optional Double-Tap Backspace for Autocorrect Revert (D-348)

D-348: an optional setting (default off) changes A-07's post-commit undo trigger from a single Backspace
to a double-tap — two Backspace presses within the existing `doubleTapDelayMs` window (G-05, 200-800 ms,
default 400 ms). When on, the first Backspace at the armed undo tail is an ordinary single-character delete
of the delimiter (D-348 v4: not a no-op — matches the plain single-tap behaviour exactly for that one
character) while the window stays armed; only the second Backspace within the window fires the actual
revert. Trailing whitespace beyond the armed tail is still consumed ordinarily by the first press (same as
the single-tap mode's own D-286/D-277 whitespace consumption). When off (the default), the original
single-Backspace revert behaviour is unchanged. The motivation: a single-Backspace revert can conflict
with the user's intent to simply delete one character, and a deliberate double-tap is a clearer, less
surprising trigger for an action that replaces an entire word.

D-358: the delimiter a word was committed with is not always whitespace — a word committed directly by a
punctuation mark (e.g. a period) has that punctuation as its own delimiter. The first tap's "keep the window
armed" decision is judged against the delimiter's own real character, whatever it is, not merely "is it
whitespace" — otherwise a punctuation-committed word's very first tap wrongly reads as eating into real
content and discards the window before a second tap ever gets the chance to revert it.

---

## 35. Sentence-Start Capitalisation Is A Live-Only Mechanism, Never A Commit-Time Override (D-405)

D-405: before this change, §6 hierarchy rule 1 ("explicit user input always wins") only ever protected an
*uppercase* choice - a token starting a sentence/line was force-capitalised again at commit regardless of
what was actually typed, silently overwriting a deliberate lower-case choice (e.g. explicitly Shift-disarming
a pre-armed capital and typing lower-case on purpose). Root-caused from a real device log: composing stayed
`"test"` throughout typing, yet the committed result was `"Test"` anyway.

Fixed by making rule 1 genuinely symmetric: sentence/line-start capitalisation is now applied **only** live,
by pre-arming Shift before the word is typed (the existing `armShiftForNextWord`/`ShiftGrace` mechanism,
unchanged) - the ordinary "forgot to capitalise" case is already fully handled by the time the first
keystroke lands, since the pre-armed Shift makes that keystroke land upper-case without any further user
action. `CapitalisationEngine.capitalise()` no longer independently re-derives capitalisation from sentence
position at commit time at all - whatever casing the token already carries by then is final, exactly like an
explicit uppercase already was. Every other §6 rule (editor-mandated `CapsMode`, proper/pure noun, B-02,
the LLM rule-6 exception) is unaffected - this is scoped specifically to the sentence-start mechanism.

A real trade-off, accepted deliberately: the removed commit-time check was also an unconditional backstop
for any *unrelated* gap in the live-arming path itself (this project has hit several such gaps before -
D-45, D-313, D-335) - such a gap would previously have been silently papered over by the commit-time
override; it now surfaces directly as a visible under-capitalised sentence start instead, which is
considered the more honest failure mode (traceable and fixable at its actual source, rather than masked).
If a genuine live-arming gap resurfaces, the fix is to trace and repair `armShiftForNextWord`/
`sentenceStartBefore`, not to reinstate a blanket commit-time override, which would silently reopen this
exact issue for every deliberate lower-case choice again.

D-406 is exactly this predicted scenario materialising - two genuine live-arming gaps, previously masked by
the removed commit-time override, surfaced directly once it was gone. See §37 for the fix (which stays true
to the "repair the live-arming path, don't reinstate the override" instruction above).

---

## 36. Autocorrect Confidence — A Unified, Graduated Measure Replacing Ad Hoc Gates (D-353/D-354/D-371/D-407)

D-353: the dictionary autocorrect previously decided whether to trust a candidate through several
independent, ad hoc gates evolved one at a time over many rounds - D-114/D-227's absolute frequency floor
(with a noun-tagged exemption), D-244's flat 100× A-01 override ratio, D-113's cost-2 override exclusion.
Each was individually correct against the case that motivated it, but the pile as a whole had no single
place answering "how confident are we in this specific correction" - which made both device-reported false
positives (e.g. D-244's own "Ohren"→"Ihren" regression) and the new C-22 setting below hard to reason about.

Replaced by `CorrectionConfidence`, a pure `[0, 1]` score computed one of two ways depending on the typed
token:

- **Unknown token** (no dictionary entry of its own): confidence is the edit cost (a cost-2 correction
  scores lower than an otherwise-identical cost-1 one) times how far the candidate's own frequency clears a
  reference point - a *much* higher reference for a noun-tagged candidate than a non-noun one, since a rare
  noun in the bundled Wikipedia-derived corpus is disproportionately a proper-noun artefact (D-227's own
  finding), while a common, correctly-recognised noun ("Jahren", A-08's own compound-rest correction) is not
  penalised once its own frequency clears that higher bar - a flat noun penalty would have punished both
  alike.
- **Known token** (A-01's own override case): confidence is the edit cost times a log-scaled function of how
  many times more frequent the candidate is than the typed word itself, replacing D-244's flat 100× cutoff -
  calibrated so the confirmed-bad "Ohren"/"Ihren" case (70×) sits below the score every C-22 level requires,
  while the confirmed-good "ddr"/"der" (228×) and "due"/"die" (37,000×+) cases clear it.

Two thresholds, both exposed through **C-22 (Autocorrect)** - Off / Cautious / Medium (default) / Aggressive -
govern what the score is used for: at or above the **auto-apply** threshold, the correction is applied
silently at commit exactly as before; at or above the (always lower) **chip-offer** threshold, it is still
surfaced as an ordinary suggestion-bar candidate even when it falls short of silent application - an unwanted
suggestion is merely ignorable, unlike an unwanted silent replacement. Medium reproduces the pre-D-353
behaviour exactly against every existing regression case; Cautious/Aggressive are new, bounded by one rule
that holds regardless of which level is chosen: **no level may silently apply a correction shape already
confirmed to be a false positive** (the "Ohren"/"Ihren" case specifically) - Aggressive only ever admits more
of the previously-*untested* grey zone above that floor, never reopens a confirmed mistake purely because a
more permissive level was chosen. A standing regression test asserts this floor directly against Aggressive
(the most permissive level), so a future retuning of the formula's own constants cannot silently reopen it
again without that test failing first.

D-407: **Off** merges in the former, separate D-234 "Autocorrect" toggle - one slider instead of two settings
that could otherwise disagree. It is not a fifth confidence level at all (`AutocorrectAggressiveness` itself
still has exactly the three real levels above) - it independently disables silent application altogether
([AdaptSettings.autocorrectEnabled], derived from the same one stored value), while the underlying confidence
level used for ranking suggestions/chips still falls back to Medium, so the suggestion bar stays just as
useful with autocorrect off as it always was. Rendered as a slider (`LabeledSeekBarPreference`) rather than a
list, narrow enough that the selected level's name still fits beside it.

D-354: a typed token that is not itself a dictionary word may still be a genuine, simply unlisted German word
- most commonly, one built from a common prefix (`ab-`, `an-`, `ent-`, `über-`, `wieder-`, and similar) the
dictionary happens not to carry every combination of. When a correction candidate changes that recognisable
leading prefix (e.g. "aberkennen" → "anerkennen": "ab-" replaced by "an-"), the candidate's score is capped
below every C-22 level's auto-apply threshold - deliberately a cap on the auto-relevant score alone, not a
reduction applied uniformly, so the candidate still clears every level's chip-offer threshold and remains an
ordinary tappable suggestion; it is only ever silent application that is foreclosed. This prefix set is
deliberately broader than A-05's own `ver-`/`zer-`/`ent-`/etc. list (§7) - that list exists to gate a *split*,
where the variable separable prefixes (`über-`/`um-`/`durch-`/etc.) are deliberately excluded because each is
also a common standalone word; that reasoning does not apply to a soft plausibility cap on a whole-word
substitution, so the broader set is used here without contradiction.

D-371: a typed token ending in a digit (a house/model/version number glued onto a word without a separating
space, e.g. "Str12") gets its own cap in `CorrectionConfidence.forUnknownToken`, deliberately placed
*differently* from D-354's: strictly between C-22's Medium and Aggressive auto-apply thresholds, rather than
below every level's. A digit suffix is not evidence the whole token is a plausible unlisted word the way a
recognised prefix is (D-354's reasoning) - it is simply a shape autocorrect has historically been too eager to
silently strip - so an otherwise high-confidence candidate may still auto-apply, but only at Aggressive, the
level whose user has explicitly opted into the most permissive silent-correction behaviour; Cautious/Medium
are held to a chip offer only (the cap sits comfortably above every level's chip-offer threshold, so the chip
is never suppressed either). Reuses C-22 rather than a dedicated setting - a deliberate choice discussed
directly: this is a graduated risk-tolerance question C-22 already models end to end, not a genuinely
independent axis that would need its own control.

---

## 37. Auto-Caps Is A Position Property, Not An Event - Closing D-405's Predicted Live-Arming Gaps (D-406)

D-406: a real device repro exposed that Auto-Caps had, in practice, drifted from the model §6/G-05 already
describe (a position's Shift state is derived fresh every time that position is reached) toward something
closer to "an event (typing punctuation, entering a field) sets a state that then persists" - the two are
usually indistinguishable, since the position most commonly *is* reached right after the triggering event,
but diverge sharply the moment a caret move or a delete lands somewhere else first. Three gaps, found by
tracing one real repro end to end (type a sentence-ending period, tap back into the previous word to fix a
typo, Backspace to correct it):

1. **A caret reaching a new position with no keystroke of its own** is only re-derived
   ([armShiftForNextWord]) on one of its two code paths. `reclaimWordAtCaret()` (§58/D-62, reached while no
   composing token was active) already did this correctly since D-313. The other path - an external caret
   move away while a composing token *was* still active, handled inline inside `onUpdateSelection` itself -
   never re-derived Shift at all; whatever state happened to be active before the move simply stayed.
2. **A stale pending-auto-space guard could swallow a Backspace entirely.** `handleBackspace()`'s existing
   D-262 guard (a Backspace right after a still-pending sentence-punctuation auto-space removes only that
   space) trusted the pending flag unconditionally. `reclaimWordAtCaret()`'s own debounce
   (`RECLAIM_DEBOUNCE_MS`, tuned for an unrelated reason, D-347/D-350) means a Backspace pressed faster than
   that window could still hit this guard with a caret that had already moved away - consuming the flag,
   deleting nothing, and returning before ever reaching the ordinary delete/re-derive path below. Fixed by
   verifying the caret is genuinely still at the auto-space position before intercepting the keystroke;
   otherwise the flag is treated as stale and the keystroke falls through to an ordinary Backspace. (D-416
   later removed this whole guard, flag and all, along with the eager auto-space mechanism it protected - see
   A-12 for the current, deferred model, which has no comparable stale-flag risk left to guard against.)
3. **Deleting a non-letter character had no re-derivation at all in the "off" direction.** The pre-existing
   D-45 fix only ever re-armed Shift *on* when deleting a character revealed a genuine sentence start
   (checked via one standalone, single-call-site re-check); there was no equivalent for the "not a sentence
   start" direction, so a stale "on" could survive deleting punctuation or whitespace unchanged.

The fix generalises rather than patches each symptom individually - `applyShiftAfterDelete()` (Addendum to
G-05) now has exactly two cases: a deleted **letter** is a genuine, deliberate exception to context
derivation (its own case is the only signal - uppercase arms Shift on, lowercase sets it off outright, never
merely "left as it was"); a deleted **anything else** (punctuation, whitespace, a digit) is not special at
all and gets the exact same [armShiftForNextWord] re-derivation every other newly-reached position gets, in
both directions. This single change subsumes D-45's own former standalone re-check (removed - it only ever
covered the "on" direction, from one call site) and applies consistently everywhere a character is deleted,
not only the one path D-45 originally patched.

Together with (1) and (2) above, every way a caret can reach a new position - typed forward, tapped, dragged,
moved by an arrow key, or backspaced into - now consistently answers the same question the same way: what
should Shift be *at this position*, derived fresh, never carried over from wherever it happened to be before.

---

## 38. Bundled-Dictionary Lemma Link (D-412)

D-412: `WordEntry` gained an optional `lemma` field - for a bundled entry that is itself an inflected
form of another bundled word (e.g. `ging` of `gehen`), that base form's own key; `null` for a base form
itself or any entry with no recorded link. Bundled-dictionary-only (`SqliteDictionaryStore`'s
`TABLE_WORDS`) - never set on a learned entry, and the learned table (`TABLE_LEARNED`) gained no such
column at all. `dict.tsv`'s word-line format gained a matching optional 4th column
(`<word>\t<freq>\t<pos>\t<lemma>`), parsed by `DictionaryAssetParser` exactly like the POS column: absent
or empty means no link, backward-compatible with every existing row in every language's asset.

Motivation: laid down as groundwork for D-404 Tier 1 (generative morphology) while the German dictionary's
verb-inflection tagging work (D-412's own sibling effort, see `AdaptKey-Progress.md`) is under way anyway -
the base-form link for each inflected form found during that work is recorded directly as a byproduct,
rather than needing a separate future pass to re-derive it. As of this writing the field has **no reader
anywhere in the app** - purely descriptive metadata until a future feature consumes it, the same status
D-368's own `VERB` tag had before this round.

D-404 Tier 1 (see `AdaptKey-Progress.md`) later used this same column, first for the complete noun/verb
paradigms and subsequently for adjectives: every declined/degree form generated for an already-bundled
adjective lemma (full 4-case x singular/plural x strong/weak/mixed declension across Positiv/Komparativ/
Superlativ) is lemma-linked here exactly like a noun or verb form, and the lemma's own row gained the
`ADJECTIVE` POS tag alongside whatever it already carried (`OTHER,ADJECTIVE` for a word like `schön`,
mirroring the existing `NOUN,OTHER` multi-tag convention). Still no code reader anywhere in the app for any
part of speech - remains descriptive metadata.

Migration mechanics deliberately mirror D-388's `last_touched` column precedent, not the more destructive
`onUpgrade`/`DATABASE_VERSION` path: this project never bumps `DATABASE_VERSION`, since `onUpgrade` drops
every table including `TABLE_LEARNED` (the user's real, irreplaceable learned words and blacklist) and
rebuilds it empty - a guarded `ALTER TABLE ADD COLUMN` in a new `ensureLemmaColumn()`, called unconditionally
from `init {}` exactly like `ensureLastTouchedColumn()`, is the safe, established pattern for evolving this
schema without ever risking that data. Unlike `last_touched`, no existing-row backfill is needed: `TABLE_WORDS`
is entirely reseedable from the language-pack asset (`resetBundledWords()` + `bulkImport()`), so a `NULL`
default for every pre-existing row is simply correct until the next reimport populates real links.

The lemma value itself is stored as a plain word string (matching how `dict.tsv`/the TSV parser already
treat the POS field, not as a `rowid`/integer foreign key) - `TABLE_WORDS.wkey` is already the indexed
primary key, so a text-keyed lookup is no slower in practice, and every other cross-reference in this schema
(`TABLE_BIGRAMS`, `TABLE_LEARNED_TRIGRAMS`, `TABLE_BLACKLIST`) already keys by the same lower-cased text
form rather than by rowid - a rowid-based reference would also not survive `TABLE_WORDS`'s routine
wipe-and-reseed cycle in any well-defined way, since SQLite gives no guarantee that rowids are stable across
a `DELETE`+bulk-`INSERT` pair.

---

## 39. Learned-Words Base-Form Consolidation — Tier 3, Non-LLM Path (D-404)

D-404's backlog item lists three tiers of inflection-awareness; this is Tier 3, scoped purely to the Learned
Words list (`TABLE_LEARNED`) and its own editor - not the bundled dictionary's ranking/override logic (Tier
2, untouched) and not full generative runtime morphology (Tier 1, untouched). Motivation: an inflected form
typed enough times to promote (e.g. a genitive "Hundes") used to sit in the learned list as its own,
unrelated-looking entry even after the base form ("Hund") was also learned - flooding the list with what is,
to the user, the same word.

`TABLE_LEARNED` gained its own `lemma` column (a guarded `ALTER TABLE ADD COLUMN`, `ensureLearnedLemmaColumn()`,
called unconditionally from `init {}` exactly like D-412's `ensureLemmaColumn()` for the bundled table -
`DATABASE_VERSION` is still never bumped, so the user's real, irreplaceable learned words are never at risk).
Unlike the bundled table, `TABLE_LEARNED` cannot simply be reseeded, so every pre-existing row gets a one-time,
maximally conservative consolidation pass right inside the migration - see below.

**Category ("unbekannt" until determined).** `WordEntry.partsOfSpeech` already defaults to the empty set; a
freshly learned word already started there. What's new is a single non-LLM heuristic in `AdaptKeyService.
learnWord()`/`learnWordStrong()`: a word typed capitalised at its first letter, but only when this happens
*mid-sentence* (`!tokenSentenceStart` - a sentence-start capital says nothing about the real category, since
every word gets one) and only while the category is still unset, is assumed to be a `NOUN` (`categoryHint`,
a new optional parameter on `DictionaryStore.learn()`). Never overrides an already-known category, and is
re-applied on every reinforcement, not only at first promotion - a word first learned at a sentence start
stays "unbekannt" and can still resolve itself the first time it is later typed mid-sentence. The editor
(§13) shows an entry with no category with a trailing asterisk, so a power user knows to check it.

**Base-form linking (`LearnedLemmaLinking`).** Extremely conservative and lookup-only, mirroring D-115/D-125's
existing `RegularVerbInflection` pattern (strip a closed set of endings, check whether the reconstructed
candidate is already known) but for the learned lexicon specifically, in both directions:
- *Forward* (`findLemma`): the word just learned is itself checked against a set of noun endings
  (genitive/dative/plural-style: `-s -es -e -en -er -n -nen -ern`), then a set of adjective declension/degree
  endings (D-404-followup: plain declined Positiv `-em`; Komparativ `-er -ere -eren -erem -erer -eres`;
  Superlativ, both the regular `-ste -sten -stem -ster -stes` family and the dental/sibilant-extended `-este
  -esten -estem -ester -estes` one, e.g. `heiß` -> `heißesten` - both tried unconditionally rather than
  picking the grammatically correct one for a given stem, since this lookup has no way to know it), and, when
  none of those match either, `RegularVerbInflection`'s own personal-ending set - if a stripped candidate
  already exists as its own `TABLE_LEARNED` entry, the new word links to it. Deliberately does not attempt
  e-elision (`dunkel` -> `dunkler`) - a plain suffix strip cannot recover an elided stem, so an e-eliding
  adjective falls outside this conservative linker's reach, the same kind of accepted scope boundary
  `RegularVerbInflection` already draws around strong/ablaut verbs.
- *Reverse* (`candidateInflections`): the word just learned may itself be the base a not-yet-linked existing
  entry is an inflection of (the "Hundes" learned first, "Hund" learned afterwards" case) - every plausible
  inflected form of the new word (noun and adjective endings, plus verb-personal forms when it looks like an
  infinitive) is checked against the existing lexicon, and any match still missing its own link is linked back
  to it.

All three endings families are tried unconditionally, rather than gating on a suspected category first -
a coincidental match against an unrelated learned word is possible in principle (the same accepted trade-off
`RegularVerbInflection` already documents for its own narrower case) but only ever links two words the user
has genuinely typed and had learned already, never fabricates an entry. A link, once set, is never overwritten
by a later automatic pass (only the editor's own manual "Grundform" correction can change it) - `putWordInternal`'s
own `INSERT OR REPLACE` semantics mean every `TABLE_LEARNED` write must now explicitly carry the entry's
current lemma forward (mirrors how `last_touched` and, for the bundled table, `lemma` itself already had to be
carried forward the same way), or an established link would be silently wiped on the very next reinforcement.

**Migration's own one-time pass.** After the `ALTER TABLE`, every existing row starts with `lemma = NULL`;
`ensureLearnedLemmaColumn()` then runs the *forward* check for every row against every other row already in
the table - lookup-only, same as the ongoing per-`learn()`-call check. Running it forward for every row
already covers both directions across the whole existing set (an inflected form's own row finds its base; a
base form's own row is found *by* the inflected form's row when that one runs its own check), so no separate
reverse sweep is needed here the way a single freshly-learned word needs one. A future LLM-aware reprocessing
pass superseding this conservative one when a tier-3 model is already installed at migration time is part of
the design (see below) but not yet implemented.

**Editor (§13).** The list itself now shows only entries with no base-form link (`lemma == null`) as their
own row - a linked inflected form no longer clutters the list once its base is also known. Known limitation:
a linked ("child") entry that was mis-linked has no direct path back into the editor's own tap-to-edit dialog
today, since it no longer appears as its own row - only reachable indirectly, by relinking the correct entry.
The existing tap-to-edit dialog (D-292/D-294) gained a category multi-select (one checkbox per
`PartOfSpeech` tag) and a "Grundform" dropdown (every other learned word, plus "unbekannt" for no link) -
`SqliteDictionaryStore.setLearnedCategories()`/`setLearnedLemma()`, both plain column updates, let a power
user review or correct either by hand. Framed explicitly as a power-user-only surface (low impact on a normal
user, who has no reason to open this screen at all) - the rough edge above was accepted on that basis rather
than building a "reveal hidden family members" affordance for it.

**With-LLM path (implemented).** When a real tier-3 backend is present, every genuine learn event (`LearnOutcome.
LEARNED`/`PROMOTED` - never `SKIPPED`/`PENDING`, nothing written yet to enrich) additionally dispatches
`AdaptKeyService.dispatchFamilyLearning()` on `tier3Executor` (never the IME thread - `Tier3Provider.predictFamily`
is synchronous and heavy, exactly like `predict` itself): the backend determines the word's full category *and*
its whole inflectional family in one pass, and all of it is learned together, linked - not merely opportunistic
lookup-and-link the way the non-LLM path above works. `Tier3Provider` gained a second task method,
`predictFamily(Tier3FamilyRequest): Tier3FamilyResult`, alongside its existing next-word-continuation `predict` -
kept as a fully separate request/result pair and prompt/parser (`Tier3FamilyPrompt`/`Tier3FamilyResponseParser`)
rather than overloading the continuation shape, since the two tasks need different prompting (an explicit
instruction + a rigid `KEY=value` answer format the model is primed to continue, vs. plain text continuation)
and different token budgets (the hard `Tier3Decoding.MAX_NEW_TOKENS` cap, not the tiny per-keystroke default -
this call happens once per learn event, not once per keystroke, so the extra latency is an accepted trade-off).
The default `Tier3Provider.predictFamily` implementation returns `Tier3FamilyResult.EMPTY`, so `NoopTier3Provider`
needs no override at all. `Tier3FamilyApplier.apply(store, result)` (pure over the `DictionaryStore` interface,
not any concrete store - unit-tested via `InMemoryDictionaryStore`) is the single shared operation both this
live path and the backfill pass below reduce to: learn every family form (creating or reinforcing exactly like
an ordinary `learn()` call, `result.category` as the `categoryHint`), then link every non-lemma form back to the
lemma via `DictionaryStore.setLearnedLemma` (promoted from a `SqliteDictionaryStore`-only method to the shared
interface, implemented in `InMemoryDictionaryStore` too, specifically so this Android-glue-free application
logic never needs to downcast the interface) - unless that form already carries its own link, so a prior manual
correction (the editor's own "Grundform" dropdown) or an earlier application of the same family is never
silently overridden.

"LLM installed" is treated as a state, not a history, via one unified mechanism covering both trigger
conditions the design called for ("LLM newly installed" and "LLM already installed at migration time"), neither
of which hooks into `ensureLearnedLemmaColumn()`'s own synchronous migration directly - that migration runs
inside `SqliteDictionaryStore.init {}`, which every store open goes through, so it must stay fast; heavy LLM
inference has no place there. Instead: each language's `TABLE_META` gained a `family_reprocess_version` key
(`familyReprocessVersion()`/`setFamilyReprocessVersion()`, mirroring `learnedCleanupVersion`'s own scheme, "0"
meaning the pass has never run for that store). `AdaptKeyService.maybeReprocessFamiliesAsync()` is called every
time `loadTier3ProviderAsync()` actually builds a real backend - which happens on *every* fresh service instance
that finds a model already installed, not only right after a fresh import - and, for every `SqliteDictionaryStore`
still behind the current version, reprocesses every learned word still missing a category or a lemma link
(`learnedWordsWithTimestamp()`, filtered) through `predictFamily` + `Tier3FamilyApplier.apply`, entirely on
`tier3Executor`, before bumping that store's own version. A cheap no-op on every later startup once the version
has already been bumped; no original sentence context is available for a backfilled word (`Tier3FamilyRequest.
sentence` defaults to empty - acceptable, since the request is really "lemmatise this word in isolation").

---

## 40. Acronym Protection - Never Autocorrected, Learns at the Ordinary Threshold (D-404-followup)

A word typed entirely in capitals ("ETF", "AVD" - at least two letters, so a lone capitalised initial does
not count) is the user's own deliberate, explicit signal for "this is an acronym" - recognised by a small,
shared, pure shape check, `suggestion.Acronym.isAcronym()`, with no dictionary lookup at all. Confirmed as a
genuine, currently-reproducible bug before this fix, not a hypothetical: run against the real bundled German
dictionary, typing `"etf"` was silently autocorrected to `"etc"` (a common, cost-1-QWERTZ-adjacent word) on
*every* attempt - since the acronym was never yet learned, W-02's own promotion counter for it could never
even begin to move (each commit became "etc", not "etf"), so the acronym could never be learned through
ordinary typing at all, no matter how many times the user retyped it.

Two call sites share this one check, deliberately kept in lockstep rather than duplicated:

- `DictionarySuggestionProvider.bestCorrection()` (backs `autocorrectFor`/`bestCorrectionFor`/
  `highConfidenceCorrection` - A-01/A-07's single choke point) refuses outright to ever autocorrect an
  acronym away - checked against the *original* typed casing, ahead of every other branch including the
  ordinary known-word-override check, and regardless of how confident or how much more frequent a rival
  candidate looks. This is an absolute veto, stronger than D-403's own `learnedCasingOf` exemption (which
  only protects a word already learned) - an acronym is protected on its very first, not-yet-learned typing
  too, which is the exact case the reproduced bug fell through.
- `AdaptKeyService.learnThresholdFor()` (W-02) checks the acronym signal first, ahead of both existing
  compound-suspicion signals (`hasEmbeddedCapital`/`looksLikeUnsplitCompound`) - an acronym is never treated
  as a suspected unsplit compound (its embedded capitals are exactly what make it an acronym, not evidence
  of a missing space, the same embedded-capital shape `hasEmbeddedCapital` alone cannot tell apart), so it
  promotes after the ordinary two repetitions (`LEARN_THRESHOLD`) rather than four
  (`COMPOUND_LEARN_THRESHOLD`).

Once learned this way, the acronym is found and offered normally under a later lower-case typing too - no
special handling needed, already an existing property of the store/suggestion design: a word's lookup key is
always its lower-cased form regardless of stored casing (`wkey`), so `"etf"` and `"ETF"` resolve to the same
row, and the suggestion filter (S-02) only ever drops a candidate that is an *exact* (case-sensitive) match of
the current input - `"ETF"` therefore still surfaces as an ordinary, unfiltered, high-priority suggestion chip
when `"etf"` is typed later, exactly like any other completion. Deliberately not silently auto-substituted for
the user on commit, consistent with A-01's own "explicit input is never silently changed" principle applied in
the other direction - the user taps the chip (or keeps typing) to get the correct casing, the same way any
other suggestion is accepted.

Accepted trade-off, confirmed with the user: a stuck Caps Lock would make every subsequent word match this
same all-uppercase shape, disabling ordinary typo-autocorrection for as long as it stays engaged - matches
how other mainstream keyboards already treat all-caps input, and the user explicitly weighed this against the
alternative (acronyms permanently vulnerable) and preferred it.

---

## 41. Suggestion-Chip Trailing Space Never Grows Into Adjacent Punctuation or a Hyphen (D-369)

D-369: accepting a suggestion-bar chip (S-01's ordinary word/completion chips, B-03's compound chip, and
S-11's dual-casing chip alike) mid-text - the composing token sits before real, already-typed text, most
commonly a D-62 reclaim - adds a real trailing space to continue typing, except when the document text
immediately following the composing token already makes one unwanted. This already covered genuine
whitespace (D-144/D-183); D-369 widens it to two further cases where a leading space would visibly break
what already follows:

- **Sentence/clause punctuation with no space in front of it** - the same set A-12/D-29 already treats as
  "hugs the previous word" (`.`, `,`, `!`, `?`, `;`, `:`, `)`). Reclaiming a word that already sits directly
  before one of these (e.g. fixing a typo in a sentence's last word before its full stop) must not shove a
  space in between them.
- **A hyphen (B-01)** - reclaiming a segment of an already-typed hyphen-compound (e.g. fixing a typo in
  `"Rhein"` within `"Rhein-Main-Gebiet"`) must not grow a space into the middle of the compound.

A closing quote is deliberately excluded, consistent with D-370's own resolution (open/close-quote
disambiguation was judged not worth the added complexity there) - not reopened here.

---

## Prerequisite

Android Studio with a configured Android SDK.

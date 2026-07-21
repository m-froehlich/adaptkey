# AdaptKey — Design & Device-Feedback History

This is the permanent, append-only decision journal for the AdaptKey IME. It records, in full detail, every
device-feedback round, bug diagnosis, and non-trivial design decision since the initial specification —
root causes, rejected alternatives, and the reasoning behind each choice.

- The **current, crystallised feature set** (what the app does today, without the history behind it) lives
  in [`AdaptKey-Spec.md`](AdaptKey-Spec.md). Requirement IDs (`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`/`D-`)
  are shared between both documents.
- The **running build/version/test-count status** lives in
  [`AdaptKey-Progress.md`](AdaptKey-Progress.md).
- This file is deliberately never rewritten or condensed — a later entry may supersede an earlier one's
  conclusion, but the earlier entry stays as-written. Append new `## §N - ...` sections here; never edit or
  fold an existing one.

This content used to live as sections 12+ of the original `android-keyboard-spec.md` (sections 1-11, the
pure feature spec, are now `AdaptKey-Spec.md`). Split on 2026-07-20 because the spec had grown into ~96%
historical narrative, crowding out the actual current-state requirements.

---

## 12. Device-Feedback Additions (from real-device testing, v0.7.x)

Requirements added after testing on hardware. IDs continue the existing scheme.

### D-01 - Multi-Alternative Long-Press Popup
A key with **more than one** secondary alternative shows, on long-press, a small popup (context menu) of all
alternatives, Gboard-style. The most common alternative is pre-selected; sliding the finger changes the
selection and releasing commits the highlighted one. A key with **exactly one** alternative shows **no** popup
and applies that alternative immediately on long-press (current behaviour, e.g. the umlauts on a/o/u, ß on s).

### D-02 - Full-Stop Long-Press Punctuation List
Long-press on the full-stop key presents (via D-01) the punctuation set in this order: `.` `!` `?` `,` `;` `:`
`-` `_` `/`. The full stop is pre-selected.

### D-03 - Space Bar Shows the Language
The space bar is **not** labelled "Space". It shows the currently selected input language (e.g. "Deutsch",
"English", "Ελληνικά"), which also doubles as a language indicator.

### D-04 - Space Bar Tap Feedback
The space bar briefly flashes/highlights on each key press, like the other keys, so a press is visibly
acknowledged.

### D-05 - Optional Key Sound *(configurable, default off)*
An optional click sound on key presses, off by default, toggled in the settings.

### D-06 - Optional Haptic Feedback *(configurable, default off)*
An optional short vibration on key presses, off by default, toggled in the settings.

### D-07 - Accelerating Backspace Repeat on Hold
Holding backspace deletes, after a short initial delay, character by character; the rate **starts slow and
accelerates** so the user is not surprised into deleting too much. After roughly three words deleted it switches
to **word-wise** deletion until backspace is released.

### D-08 - Backspace Shift-Restore Refinement (extends the G-05 addendum)
When an uppercase character is deleted and then the space immediately to its left is deleted, that space
deletion counts as deleting a **lowercase** character and shifts back to lowercase (the uppercase context ended
at the word boundary).

### D-09 - Raw-Tap Recording (diagnostic, opt-in)
The raw tap coordinates during a calibration/onboarding session can be recorded and exported, so mis-resolution
of touches (e.g. an uncalibrated finger) can be analysed offline.

### D-10 - Backspace at Start of a Line/Entry
A backspace at the very start of an entry should, where the editor allows, delete backwards across the boundary
(joining with the previous entry / line) as common keyboards do, rather than being a no-op.

---

## 13. Second Device-Feedback Round (from v0.7.7 testing)

A further round of on-device testing. Overall the tap precision is now *substantially* better and close to
usable. The following refinements to existing requirements and new requirements (IDs continue the D-series)
came out of it.

### Refinements to existing requirements

- **K-01 / edge-to-edge (blocker):** the **calibration screen** does not push its embedded keyboard up by the
  gesture-bar / navigation inset the way the IME view does (§ D device round 1), so the bottom row sits under
  the gesture pill and calibration could not be completed. The calibration keyboard must apply the same
  bottom inset padding as the live keyboard.
- **D-04 (flash too slow):** the key-press flash lingers far too long - a double-tap of the same key shows no
  visible second flash. The flash must be markedly shorter so rapid repeated taps each register visibly.
- **D-07 (too slow / wrong stop):** (a) backspace-on-hold deletes too slowly - the repeat must accelerate to a
  clearly faster rate; (b) word-wise deletion **stops before the last word of the line** instead of deleting
  it - the word-boundary handling at the start of a line must remove that final word too.
- **A-07 (undo misses splits):** a backspace immediately after an A-05 retroactive split does **not** undo the
  split (rejoin the two words). The post-commit undo must also revert a split, not only a plain autocorrect.
- **C-04 (defaults):** the recognised-word highlight must default **on** (many users watch for it), and the
  default green is too dark - use a lighter shade with better contrast.

### D-11 - Earlier and More Frequent Suggestions
Word suggestions currently appear too late and too rarely. Sensible completions must be offered already after
the **first or second letter** and consistently thereafter - many users type only until the intended word is
offered and then pick it, which must work reliably here.

### D-12 - Fuzzy, Umlaut-Aware Correction Candidates
Suggestions and autocorrect must include close single-edit and umlaut variants: `mut` must offer `mit`, `grun`
must offer `grün`, and a clearly mistyped `Defaukt` must be corrected/offered as `Default` rather than being
accepted. Relatedly, the word-recognition highlight (S-05) must **not** treat nonsense like `Defaukt` as a
known word.

### D-13 - User Word Training / Add-to-Dictionary
There must be a way for the user to teach the keyboard a word (e.g. `Backspace`), so it is offered, kept, and
in particular **not** retroactively split (A-05) into `Back Space`. This includes a discoverable action to add
the current/typed word to the personal dictionary.

### D-14 - In-Keyboard Long-Press Feedback
A long-press must produce visible feedback **on the keyboard itself** (a key flash/flicker, or - Gboard-style -
the popup briefly rising), plus optionally the D-05/D-06 sound/haptic, so the user sees that the long-press
registered even when looking at the keys rather than the text. This applies to single-alternative keys too,
which currently show nothing on the keyboard.

### D-15 - Double-Tap Shift = Caps Lock
Pressing Shift twice in quick succession engages Caps Lock (persistent uppercase) until Shift is pressed again.

### D-16 - Typing-Pattern-Driven Default Key Enlargement
The enlarged-backspace idea (L-04) should be mirrored for the Shift key. After calibration detects the typing
hand (T-04), the default enlargement is pre-set accordingly: a **left**-index typist gets the enlarged
**backspace** (right side), a **right**-index typist gets an enlarged **Shift** (left side). Both remain
user-adjustable afterwards.

### D-17 - Expanded Onboarding USP Text
The onboarding core-feature list is too terse. It should be expanded with the strongest USPs from this spec
(provably offline / no INTERNET permission, adaptive per-finger touch model, retroactive split/merge, the
on-device mini-LLM, real multilingual dictionaries, etc.) so a new user immediately understands what AdaptKey is.

### D-18 - Emoji Panel Toggle *(configurable, default on)*
The emoji panel is of limited use (messengers bring their own). A setting must allow disabling it - default on
so it is discoverable, but switchable off. When off, the combined key (L-03) is purely a `?123` numeric/symbol
key with no emoji panel.

### D-19 - Full-Field Swipe to Switch Pages
A left/right swipe **anywhere on the key field** switches between the surfaces/pages (letters ↔ symbols ↔
numbers). This must be cleanly separated from the space-bar left/right language swipe (G-01). The `?123` key is
kept for accessibility even if this makes it partly redundant.

### D-20 - Larger Gesture Thresholds
Swipe-down to dismiss (G-03) and the full-field left/right page swipe (D-19) currently trigger on too small a
motion - a faint downward swipe already hides the keyboard. They must require a clearly larger travel to
activate while staying practical. The space-bar language swipe (G-01) must stay proportional to the small space
bar and remain easy.

### D-21 - Key Cell Padding
Add a few pixels of cell padding (spacing) between keys, Gboard-style, to visually separate them.

---

## 14. Third Device-Feedback Round (from v0.7.8 testing)

More on-device findings. Several refine features from §13; the rest are new requirements (IDs continue the
D-series). Items marked *(bug)* are regressions/defects, not new scope.

### Refinements to existing requirements

- **D-04 - flash still too slow *(bug)*:** even after shortening, the key-press flash feels sluggish. It must
  be *much* shorter, matching Gboard's very brief key flash, so typing feels snappy.
- **D-05/D-06 - sound & haptic do not fire *(bug)*:** with both toggles enabled on device, no click sound is
  heard and no vibration is felt. The feedback path is not actually working and must be fixed (audio focus /
  `playSoundEffect` routing; haptic flags / an explicit `Vibrator` fallback).
- **C-04 / S-05 - colour the text, not the background (D-25):** a recognised word should be shown by changing
  its **font colour**, not a background highlight - it looks better. (Supersedes the background-span approach.)
- **T-04 / K-01 - calibration produced no result:** a full calibration run detected **UNKNOWN** (no pattern).
  The calibration may be too short and the classifier too conservative. Calibration must reliably produce a
  concrete, visible result and state it plainly ("I detected that you type with your left index finger").
  Investigate sample count / thresholds / sentence length.

### D-22 - Punctuation Reorganisation (period vs comma)
Split the current full-stop long-press set into two keys by meaning. The **full-stop key** carries the
sentence terminators only: primary `.` with alternatives `!` `?`. The **comma key** carries the rest as its
long-press set: primary `,` with alternatives `;` `:` `-` `_` `/`.

### D-23 - Vertical Long-Press Popup Layout
The long-press popup is laid out **vertically** above the finger, not horizontally. The primary character is
pre-selected but sits **top-left** of the key (offset, not directly above the finger). The remaining
alternatives are stacked **vertically above the finger**, ordered by priority bottom-to-top. So on the
full-stop key the cell directly above the finger is `!`, with `?` above it; on the comma key the comma is the
pre-selected top-left default and the alternatives run bottom-to-top `;` `:` `-` `_` `/`.

### D-24 - Touch-Pattern Visualisation *(new feature)*
Visualise the learned touch model: show the keyboard with a coloured circle over each key at the **expected
strike point** (the T-03 per-key mean offset, radius ~ its variance). Offer it as the **result screen of a
calibration** and as a toggle in the settings, so the user can see how their personal offsets look.

### I18N - Multilingual App Texts *(new feature)*
The app's own UI strings (settings, onboarding, dialogs, calibration) must be localised. At minimum **English**
and **Greek** in addition to German, selected by the **system language**. (The keyboard's *typing* languages
DE/EN/EL already exist; this is about the app-chrome strings via `res/values-en`, `values-el`.)

### D-26 - Mid-Word Correction Colours the Inserted Characters *(bug)*
When correcting inside an existing word, the two freshly typed correction characters can get coloured
(highlighted). The recognised-word colouring must not apply to a mid-word edit like this.

### D-27 - Space Bar Top-Edge Hit Target *(bug, high priority)*
Tapping the **top edge of the space bar** registers the key **above** it (c / v …), producing many wrong
letters instead of spaces. The space bar must reliably win at its own top edge. (This is the inverse of the
T-05 space/letter band and must not be swamped by it - a genuine space tap must stay a space.)

### D-28 - Proximity- and Distance-2-Aware Correction
Correction must consider **physical key adjacency** and a larger edit budget. `komplezz` must suggest
`komplett` - obvious to a human, and especially because `z` neighbours `t`. A single edit is not enough here
(two characters differ); the matcher needs edit-distance-2 and/or an adjacency-weighted cost so a typo on a
neighbouring key is cheap. (Open question the user raised: how much of this improves only once the optional
tier-3 LLM model is imported - to be answered.)

### D-29 - Punctuation After an Accepted Suggestion Eats the Trailing Space
Accepting (tapping) a suggestion inserts a trailing space, which is correct. But if the **very next** input is
a punctuation mark, that trailing space must be removed so the punctuation attaches to the word. This must
apply **only** immediately after a "suggestion accepted" action - spaces before a typed punctuation mark must
**not** be stripped in general.

---

## 15. Fourth Device-Feedback Round (from v0.7.16 testing)

Further on-device findings. `*(bug)*` marks defects.

### D-30 - Long-Press Backspace Freezes Input *(bug, critical)*
After using long-press (held) backspace and stopping, **no further input is possible** - not even a normal
backspace, so the line's last word can no longer be deleted either (this was the real cause behind the
earlier "stops before the last word" report). The keyboard must remain fully responsive after a held
backspace (the repeat-suppression state must be reset on the next touch).

### D-31 - Backspace-Hold Speed Re-tuning
Held backspace starts at a reasonable rate but then **accelerates far too much**, and it keeps that fast rate
when it switches to word-wise deletion - so half the text is gone in an instant. It must accelerate only
**very moderately** after the first few characters, and on the transition to **word-wise** deletion it must
slow down **clearly**, so the user can follow the deletion and stop at the right moment.

### D-32 - Long-Press Delay Too Long *(+ setting)*
The long-press (to reach the alt char / popup) takes too long. Shorten it by roughly **20 %**, and add a
**setting** for the long-press delay.

### D-33 - Popup Primary Cell Bottom-Aligned
In the D-23 popup, the offset primary cell (the key's own char) should be **bottom-aligned** - level with the
bottom of the secondary column, near the finger - rather than top-left. It works the same but feels more
intuitive.

### D-34 - Key Vibration Does Not Fire; Permission Half-Automatic *(bug)*
The key-press vibration (D-06) still does not fire. VIBRATE is a normal, install-time permission (no runtime
prompt), so enabling the setting must simply work - if any acquisition is needed it must happen
half-automatically when the toggle is enabled, never as a manual step. The vibration pulse must also be long
enough to actually be felt.

### D-35 - Swipe Thresholds Still Too Sensitive
Swipe-down (dismiss, G-03) and left/right (space-bar language G-01, full-field page D-19) still trigger on a
**tiny** motion - a faint down-swipe hides the keyboard, a faint right-swipe on space switches language, a
faint right-swipe elsewhere switches page. All must require a **clearly larger** (but still practical) travel.

### D-36 - Direct Paste from the Clipboard *(new feature)*
Like Gboard: when the clipboard holds content, show (part of) it where the suggestion bar is; a tap pastes it.
Passwords appear as `•••••`. Two improvements over Gboard: (1) **clear the clipboard after pasting**,
especially for passwords; (2) perform the **exact system paste action** (Gboard pastes in a way that behaves
subtly wrong).

### D-37 - Less Eager Learning *(with un-learn on undo)*
A word must **not** be fully learned after a single autocorrect/accept (that also learns wrongly-corrected
words). Instead: (1) a corrected word that is **reverted** (backspace-undo) must be **un-learned /
decremented**; (2) an accepted/autocorrected word is only **counted up**, and counts as learned once it has
been accepted **two or three times without being reverted**. It must stay comfortable and learn quickly, but
not over-eagerly. (Refines D-13; design the count/threshold sensibly.)

### D-38 - Correction Quality: First-Char, Umlaut-Initial, Cost-Ranked
Real misses to fix, beyond D-28: first-character typos and umlaut-initial words are never corrected because
the candidate set is keyed on the exact first character - broaden it (first-char neighbours + umlaut-folded
first char). And ranking must prefer a **lower-edit-cost** correction over a more frequent higher-cost one.
Examples (recognised input -> intended): `Stabdsrx` -> `Standard`, `Uberblick` -> `Überblick`,
`eerden` -> `werden`, `W8rt` -> `Wort`, and `dasy` -> `dass` (currently wrongly `das`).

### D-39 - Raw-Coordinate Per-Character Correction
Correction should not work from the committed characters alone but from the **retained raw tap coordinates**
per character (T-02 / T-05). For an unknown word, walk it character by character and, using each tap's raw
position and the offset model (T-03), consider the geometrically nearest **neighbouring** key that was
likely intended, generating candidate spellings to validate against the dictionary. Most single-key slips
are one position off, so this recovers many multi-typo words. Heavily garbled cases (e.g. `Stabdsrx` ->
`Standard`, where much went wrong at once) may instead be left to the tier-3 LLM, which tends to recognise
the intended word from context.

### D-40 - A Digit Inside a Word Is a Likely Typo
A digit typed **between letters** (mid-word, no separating space) is almost certainly an unwanted key -
digits are rarely intended mid-word. Such a digit must be kept in the composing token (not treated as a
delimiter) and corrected to a neighbouring letter like any other typo, so `W8rt` -> `Wort`. A digit at the
start of a token (or a standalone digit) keeps its normal behaviour.

---

## 16. Fifth Device-Feedback Round (from v0.7.20 testing)

More findings. `*(bug)*` marks defects. The overarching theme: digits and umlauts must be treated as
perfectly ordinary characters/keys, never as exotic special cases.

### D-41 - Digits Are Ordinary Neighbour Characters in Correction
A stray digit inside a word is nothing more than a mis-hit neighbouring key and must be corrected exactly
like a wrong neighbouring letter - not as a word separator that derails the suggestions into random
uppercase acronyms. Include the number row in the keyboard-adjacency map so a digit is a **cheap**
neighbour substitution (`8`↔`o` etc.), and make sure a mid-word digit no longer produces nonsense live
suggestions. (This should ideally fall out of D-39's raw-coordinate approach; the special-casing in D-40 was
the wrong direction.)

### D-42 - Number-Row Hit Zone Too Tall *(bug)*
The number-row keys react too far down - their touch zone feels like it reaches into the letter row below,
so a tap meant for the top letter row lands on the digit above it. Tighten the number-row hit target.

### D-43 - Next-Word Prediction in the Suggestion Bar
The suggestion bar should predict and offer the **next** word before it is typed - this is what makes a
keyboard feel alive. A bigram baseline (the stored n-gram continuations) works offline today; the tier-3 LLM
elevates it to real context-aware prediction.

### D-44 - Alt-Key Popups: Horizontal, Centred, Finger-Below Selection
Return the long-press popup to a **horizontal** layout, but better than the first version:
- It is centred **above the stem key**, and grows to the right (where there is room), extending at most one
  cell to the **left** of the stem.
- The **primary** character sits directly above the stem key and is pre-selected. For the full stop the row
  is `?` `.` `!` (full stop centred, `?` upper-left, `!` upper-right). For the comma the row is
  `-` `,` `:` `;` `/` `_` (comma centred over the stem, `-` the one cell to its left, the rest to the right).
- Selection follows the finger **swiping underneath** the popup (along the key row), not resting on the
  cells - so the finger never hides the choice; the highlight makes the selection clear.

### D-45 - Restore Auto-Capitalisation After Deleting a Punctuation at Line Start *(bug)*
At the start of a line the keyboard auto-shifts to uppercase. If the user types a punctuation mark there and
then deletes it, the auto-uppercase is **not** restored - which is intuitively wrong. This needs an explicit
exception to the general shift rules: deleting back to a sentence/line start re-arms the auto-capital.

### D-46 - Swipe Length Still Too Short
Even after D-35, a swipe still triggers on too little travel - a slight smear while pressing Shift already
switches the page. The surface/language swipe should require at least roughly **three key-widths** of travel.
To be tuned on device.

### D-47 - Combined Key Must Not Show the Emoji Glyph When the Panel Is Off *(bug)*
With the emoji panel disabled (D-18) the combined key correctly acts as `?123` only, but it still **displays
the 😊 emoji**. Its label must reflect the state - show `?123` (or similar), not the emoji.

### D-48 - Umlaut Words Are First-Class; a Fold Match Beats a Split
`konnen` must autocorrect to `können` - an obvious, missing-umlaut case (the umlaut fold makes it the exact
word). Instead the A-05 split wrongly wins and produces `ko nen`. A strong umlaut-fold / low-cost correction
must take priority over a retroactive split. Umlauts must be treated as entirely normal characters, never as
exotic - for a German user anything else is incomprehensible. Reorder the correction pipeline accordingly.

### D-49 - Raw Touch-Recording as a Headline Onboarding USP
The personal raw touch-recording / per-finger learning (T-03) - and, specifically, that it lets AdaptKey
recover a word from where a tap actually landed even when the wrong key was picked outright (D-39), not just
a neighbouring letter - is one of the app's absolute core features and USPs and must appear prominently on the
onboarding page, phrased so an ordinary user understands it (not in technical terms). The onboarding text must
also make clear that the continuous learning (T-03/T-04, D-37) is deliberately gentle/damped: it improves from
a steady pattern over time, but a single slip never noticeably shifts anything.

### D-50 - Suggestion Bar Stays Permanently Visible
The suggestion bar must keep its row at all times, even when it is momentarily empty, so the prompt slot
above it does not jump up and down as suggestions come and go. (With next-word prediction (D-43) it will
rarely be empty anyway.)

## §17 - Device-Feedback Round 6 (v0.7.22 / v0.7.23 testing)

The horizontal alt-key popups (D-44) are now excellent and highly usable. This round refines the keyboard
geometry, doubles down on the umlaut-first-class principle, and adds several bugs found on device.

### D-51 - Period Long-Press Order Is `! . ?`
The full-stop key's horizontal alt popup should read `! . ?` (exclamation, full stop, question mark), left to
right, with the full stop still the centred, pre-selected default. (Supersedes the D-44 `? . !` order.)

### D-52 - The Number Row Is a Full-Height Key Row Again
D-42 made the number row shorter, which only means the digits must be hit more precisely - the opposite of
helpful, and there was no real benefit. Revert it: the number row is exactly as tall as the letter rows.

### D-53 - The Number Row Long-Presses Like Every Other Key
The number row is not an add-on or a foreign body; it is an ordinary part of the whole keyboard and must
behave like one. In particular each digit must produce a long-press alt popup exactly as the letters do,
offering its shifted symbol (the QWERTZ `! " § $ % & / ( ) =` etc. already shown as the corner hint). Right
now the digits show no popup at all. Treat the row as first-class throughout, not as something bolted on.

### D-54 - Single-Alternative Popups Are Nudged Toward the Keyboard Centre
A one-cell alt popup should sit slightly offset from directly-above-the-key so the finger does not hide it:
about two units to the right for keys on the left half of the keyboard, and two units to the left for keys on
the right half. (Multi-cell popups keep the D-44 centred-over-stem behaviour.)

### D-55 - Two New Spacing Settings: Below the Number Row and Above the Space Row
Add two independent settings that insert extra vertical space (a) directly below the number row and (b)
directly above the space row. Default 7 (units/dp), slider range 0-25. This is expected to remove a great
many mis-taps and, above the space row, many accidental Enter presses.

### D-56 - Backspace Hold: Single-Character Phase Slightly Faster
The accelerating backspace hold (D-31) is almost perfect. Only the single-character deletion phase may be a
touch faster; the word-wise phase stays as it is.

### D-57 - Page-Swipe Width Retune (Space Bar Gets More)
The full-field page swipe (D-46) is now a bit too wide. Reduce the required travel by ~15% - except on the
space bar, where it may be ~15% more (its horizontal swipe is still too easy to trigger by accident). The
swipe-down-to-hide gesture is perfect as it is and must not change.

### D-58 - Page-Change Animation
Give the surface/page change a slide animation, like the swipe-down animation. It looks much better and, more
importantly, it makes the page change perceptible - right now the change is easy to miss.

### D-59 - Optional `?123` Key (Default On)
Add a setting to disable the combined `?123` key. It mostly gets in the way - it is easy to hit by accident
when reaching for Shift (the new D-55 spacing above the row will already help). Default on. When this setting
is **off AND** the emoji panel is also off, the button disappears entirely, but its slot stays reserved
(the surrounding keys do not grow into the gap).

### D-60 - Quick Paste Does Nothing (Bug)
The direct-paste chip (D-36) currently has no effect. Likely the clipboard is cleared too early (before the
paste action actually consumes it). Fix the ordering so the paste happens, then the clipboard is cleared.

### D-61 - Enter Does Not Submit in Browser Address Bars / Search Fields (Bug)
Pressing Enter in a browser address bar or a search field (e.g. YouTube) inserts a newline instead of
submitting. The Enter key must honour the editor's requested IME action (Go/Search/Send/Done) instead of
always emitting a newline.

### D-62 - Mid-Word Editing Produces Live Suggestions
When the caret is placed inside an existing word and the user types, the autocorrect / suggestions must behave
as if the whole word were being composed right now - the full extent of the word (and, where useful, the
surrounding context for the LLM) is taken into account, not just the fragment after the caret.

### D-63 - Missing Umlauts / ß: Suggest-Up-Front, Auto-Correct When Unambiguous
Every word typed without its umlaut or ß (`konnen`→`können`, `mussen`→`müssen`, `hoflich`→`höflich`,
`weis`→`weiß`) must (1) be recognised with its diacritics and offered near the front of the suggestion bar,
and (2) be auto-corrected when it is unambiguous - i.e. when no sensible non-diacritic word exists. So
`mussen` auto-corrects to `müssen`. But `konnten` must only *suggest* `könnten`, because `konnten` is itself a
valid word. Being able to skip the umlauts while typing and have the app restore them is a core comfort
feature and must be supported as strongly as possible. (Extends D-48 from "beats a split" to full
suggest+auto-correct behaviour.)

### D-64 - Suggestion-Bar Word Cannot Be Dragged to Trash (Bug)
Dragging a word out of the suggestion bar (to the trash / blacklist, G-04 / D-36-era gesture) does not work
any more. Restore the drag-to-trash from the suggestion bar.

### D-65 - `konnen` Auto-Corrects to `kannen` (Bug, HIGH)
`konnen` is currently auto-corrected to `kannen`, which is not a word; it must become `können`. Possibly a
ricochet from the D-41 digit-adjacency change or the D-48 pipeline reorder. This is the exact umlaut-first-
class case from D-63 and must be fixed together with it.

### D-66 - Key Vibration Does Not Fire At All (Bug)
The key-press vibration (D-06 / D-34) definitely does not fire on device, despite the earlier fixes. Low
priority for now, but the haptic path needs to be re-investigated (permission, VibratorManager vs Vibrator,
amplitude, or the toggle wiring).

## §18 - Device-Feedback Round 7 (v0.7.27 testing)

### D-67 - Word Splitting Fires Too Eagerly; Must Require High Confidence (Bug)
`kleiben` (a typo of `kleinen`) was split into `klei` + `en` instead of being auto-corrected to `kleinen`.
Neither `klei` nor `en` are words an ordinary user would recognise as legitimate splits, yet the split still
won. Root cause: the A-05 "drop a character" split path ([TokenRepair.trySplit], the branch keyed off
[TokenRepair.OVER_SPACE_LETTERS]) accepts a split purely because both halves are *technically* known
dictionary words - it has no bigram/frequency confidence gate at all (unlike the "fully missed space" branch,
which does require a minimum bigram count). A split must only win when its confidence is clearly higher than
the best available whole-word correction - in particular, a split must never beat a low-cost (single adjacent-
key edit or better) autocorrect candidate for the un-split token. `kleiben` → `kleinen` is exactly such a case
(`b` and `n` are adjacent keys). Generalise the D-48 "a fold match beats a split" veto: before accepting any
split, check whether the token already has a high-confidence single-word correction (low edit cost, e.g. a
proximity-aware cost of 0-1, and/or reasonable frequency) and prefer that over splitting.

### D-68 - Rethink Calibration: Ask the Typing Pattern Directly, Refine Only Slowly Over Time (Big Idea, Deferred)
The current three-sentence calibration (T-03/T-04) misclassified the user's typing pattern outright ("right
index finger" for someone who is not), which visibly skews the touch zones (e.g. `j`'s effective zone drifting
half onto `i` above it) and is a plausible cause of many reported mis-taps. Proposal to explore later: replace
automatic calibration with an explicit, simple onboarding question - "how do you type?" (two-thumb, left-index,
right-index, etc.) - and derive sensible **initial** touch zones directly from the chosen pattern, without
relying on a noisy few-sentence sample:
- For a one-handed/one-finger pattern (e.g. left index finger), keys in that hand's home third of the keyboard
  get a tight, centred zone; zones widen and smear away from the centre (toward the far side of the keyboard)
  the further a key is from that third, reflecting the increasing awkwardness of the reach. Keys at the far
  edge from the typing hand may even have their effective zone extend noticeably into the visually adjacent
  key's territory.
- The mirrored shape applies to the opposite-hand / opposite-finger patterns, and a thumb pattern gets its own
  (narrower, more central) shape.
- The "two thumbs" pattern is the exception: zones stay fairly centred/tight on every key, since a two-thumb
  typist reaches across the whole width symmetrically.
- The keyboard may still *refine* these initial zones from real usage over time (as today), but this drift
  must be made **much more sluggish** than currently - a single mistyped tap must never noticeably shift a
  zone; only a sustained, consistent pattern over many taps should move it, and only gradually.

This is a substantial redesign of the T-03/T-04 onboarding + calibration flow and is explicitly **not** meant
to be implemented immediately - captured here as the guiding idea; the concrete approach (exact shape
functions, migration/reset story for existing calibrations, whether calibration disappears entirely or
becomes an optional refinement step) needs its own design pass later.

## §19 - Device-Feedback Round 8 (v0.7.32 testing, D-68 follow-up)

### D-69 - Word Split Wins Merely By Being Found First, Not By Being the Best Match (Bug)
`immernoch` (the user's own deliberate, correct compound spelling of "immer noch") auto-corrects to
`immer och` instead of splitting as `immer noch`. Root cause: [TokenRepair.trySplit] tried the "drop a
character" strategy first and returned its result unconditionally whenever it found *any* valid candidate,
without ever trying (let alone comparing against) the "fully missed space" strategy - `n` sits over the
space bar, so `immer` + `och` is a technically valid drop candidate that already satisfies the drop path
and short-circuits it, even though `immer` + `noch` (found by the missed-space path, an extremely strong
bigram) would score far higher. This is a distinct bug from D-67 (there is no high-confidence single-word
correction for `immernoch`, so that veto does not apply here) - the fix is that both split strategies must
be evaluated and the single best-scoring candidate across both must win, not whichever strategy happens to
run first.

### D-70 - Key-Press Sound Should Read as a Typewriter, Not a Digital Beep
The optional key-press sound (D-05) is currently a `ToneGenerator` DTMF-style tone, which only reads as an
electronic "beep" - `ToneGenerator` cannot render anything but a pure/dual sine tone, so no tone selection
can make it sound mechanical. Replace it with a short, bundled percussive click sample (a broadband noise
burst plus a couple of fast-decaying resonant partials) reminiscent of a typewriter key strike, played via
`SoundPool` for low-latency, overlap-safe playback.

### D-71 - Pattern Seeding Must Also Predefine the Systematic Offset *Direction*, Not Just the Spread
D-68 deliberately left the systematic-offset direction at zero, reasoning that whether a user over- or
undershoots a far key was a personal habit with no evidence to seed from. On reflection this was the wrong
call for a one-sided finger/thumb pattern specifically: reaching for a key far from the typing hand's home
position is a fixed-pivot arc (the wrist for a finger, the base of the thumb for a thumb), and that arc
physically tends to fall *short* of the visual target the farther it reaches - not a personal habit to wait
and learn, but close to a physical constant for anyone typing this way. [PatternSeed] must therefore also
seed a directional bias (`meanDx`, and for a thumb reaching the top row also `meanDy`) shifting the expected
strike point back towards home, growing with reach, on top of the existing spread widening. Two-thumb
typing keeps no directional bias, matching its flat spread.

### D-72 - The Number Row Is Missing From Calibration Seeding (Bug)
[PatternSeed] never seeds the number row's keys, because [CalibrationActivity]'s embedded preview keyboard
explicitly hides the number row (`showNumberRow = false`) while reading `charKeyGeometry()` for seeding -
the row's keys simply never appear in the geometry list handed to `PatternSeed.seed()`. Since the live
keyboard shows the number row by default (C-09), most users end up with digit keys that have never been
calibrated at all. The calibration preview must show the number row too.

### D-73 - Reorder the Typing Patterns by Real-World Prevalence; Skip Also Applies a Sensible Default
Both thumbs is by far the most common typing pattern and should lead the list (currently ordered
finger-patterns-first, ending with two thumbs) - order: both thumbs, right thumb, left thumb, right index
finger, left index finger. Skipping the picker must not leave the model unseeded either: it should apply
"both thumbs" quietly (no confirmation dialog, unlike an explicit choice) as the sensible default for an
undecided user.

### D-74 - Touch-Pattern Visualisation Shows the Previous, Stale Pattern After a Pattern Switch (Bug)
After switching the typing pattern, opening "Show typing pattern" from the settings screen (D-24) still
shows the *previous*, now-replaced pattern's (skewed) zones, not the freshly seeded ones. Root cause: the
live keyboard service is long-lived and holds its own in-memory copy of the personal offset model, only
refreshed when a genuinely new input field is focused; if the calibration screen replaces the persisted
model on disk while the service is resident but never refocuses a field in between, the service's next
`onFinishInput` / `onDestroy` save silently clobbers the fresh calibration with its stale in-memory copy.
The service must detect that the persisted pattern has changed since it last loaded its model and adopt the
fresh one instead of saving over it.

### D-75 - Key-Press Vibration Still Does Not Fire (Bug, D-66 Follow-Up)
The D-66 fix (migrating from the deprecated `Context.getSystemService(Vibrator::class.java)` to
`VibratorManager` on API 31+) did not resolve it - vibration still does not fire on device. Further
suspect: a plain `Vibrator.vibrate(VibrationEffect)` call with no explicit usage attributes falls into an
unclassified vibration category that some OEM vibration-intensity settings scale to zero, independently of
the (already-bypassed) "touch vibration" toggle. `VibrationAttributes.USAGE_TOUCH` (API 33+) is the category
Android itself documents for on-screen-keyboard-style UI feedback and should be requested explicitly where
available. Still needs device confirmation - this may ultimately be a device/OS-level vibration-intensity
restriction outside the app's control.

## §20 - Device-Feedback Round 9 (v0.7.33 testing, D-58 follow-up)

### D-76 - Page-Slide Animation Runs Backwards and Resizes Mid-Slide (Bug)
Two bugs in the D-58 slide animation:
- The slide direction is inverted relative to the usual "content follows the finger" page-swipe feel (as
  in a photo gallery or any carousel): a forward swipe/tap must slide the outgoing page off in the swipe
  direction and bring the new page in from the *opposite* edge, not the other way around.
- The whole keyboard does not animate as one piece - at minimum the space row visibly moves separately,
  looking wobbly. Root cause: the letter and numeric/symbol surfaces do not have the same row count (the
  number row is optional on letters, C-09, but always shown on symbols), so `AdaptKeyboardView.rebuildRows()`
  calling `requestLayout()` immediately on a page switch resizes the whole view mid-slide - everything below
  the row-count difference, most visibly the bottom-most space row, visibly jumps out from under the
  still-running animation. The resize must be deferred until the slide animation completes.

## §21 - Device-Feedback Round 10 (v0.7.34 testing, D-73/D-74 follow-up)

### D-77 - Remove the "(Recommended)" Label From the Two-Thumbs Button
It helps nobody and is actively unhelpful for anyone whose real pattern is different (e.g. a left-handed
single-finger typist) - they still have to read every option regardless. Silently defaulting to "both
thumbs" when the picker is skipped (D-73) already covers the intent for the ~90% majority; anyone who types
differently makes their own explicit choice anyway. Plain "Both thumbs" label, no suffix.

### D-78 - Dead Duplicate "Typing Pattern" Settings Row (Cleanup, Found During D-74 Follow-Up Investigation)
The settings screen had two separate "Typing pattern" rows in different categories: `k01_calibration`
(Calibration category), properly wired to open [CalibrationActivity] and the one users actually use, and
`t04_detected` (a leftover "Typing pattern (T-04)" category), `android:selectable="false"` with no click
handler at all - inert, and confusingly showed the same title as the working row. Removed entirely
(preference, its now-empty category, the `SettingsActivity` code that kept its summary in sync, and the
now-unused strings) rather than wired up, since `k01_calibration` already does the job.

### D-74 Follow-Up - Stale Touch-Zone Mask After a Pattern Switch: Root Cause Still Not Found
Re-investigated after the D-74 fix (service-side stale-save guard) did not resolve it. Established via
direct questioning that the reported repro is entirely within the Settings app (Settings → "Typing pattern"
→ pick a pattern → dialog OK → Settings → "Show typing pattern"/D-24), with **no app switch and no live
keyboard/IME involvement at any point** - meaning D-74's fix (which only guards the long-lived `AdaptKeyService`
instance) cannot be the relevant mechanism for this specific repro, since the IME is never in the loop. A new
regression test ([OffsetStoreRoboTest]) reproduces the exact `CalibrationActivity.persistPattern()` →
`TouchModelActivity` load sequence, seeding a model with substantial prior *learned* data (500 recorded taps,
not just a previous seed) before switching pattern, and passes: the persisted model is provably fully
replaced and the fresh load reflects it, not the old data. The bug, if still present, must be somewhere
neither direct code review nor this test have reached yet - needs a more specific repro (e.g. does it
reproduce on the very first pattern switch of a session, or only on a second/later switch; which exact key
still looks wrong and how).

## §22 - Device-Feedback Round 11 (v0.7.35 testing)

### D-79 - Rename "Typing Pattern" to "Typing Style"
"Typing pattern" (German "Tippmuster") is ambiguous - unclear whether it means how you hold/type (T-04:
finger/thumb) or the learned touch zones themselves (D-24). Rename the T-04 concept to "Typing style"
("Tipp-Stil" in German) everywhere it appears as user-facing text (onboarding, the calibration screen, the
settings entry, the reset-learning dialogs). The German D-24 "show learned touch zones" screen, which had
confusingly reused the very same word ("Tippmuster anzeigen") for a different concept, is renamed to
"Trefferzonen anzeigen" (matching the English/Greek versions, which already used a different word - "touch
pattern" / «μοτίβο αφής» - for D-24 than for T-04).

### D-74 Follow-Up 2 - Confirmed: Reproduces on the Very First Switch, Every Time
User confirmed the bug reproduces immediately on the first-ever attempt to switch typing style, and on
every subsequent attempt - ruling out any "only after a second switch" or "only with substantial prior real
usage data" theory. Extended the investigation with a second regression test
([CalibrationActivityRoboTest]) that drives the *real* `CalibrationActivity` UI end to end (via Robolectric's
`ActivityController`, not just the pure store/model layer): builds the real activity, taps the real "Both
Thumbs" button via `performClick()`, then opens a real `TouchModelActivity` and inspects its keyboard's
`offsetModel` - exercising real layout/measurement timing, `findViewById`, and the actual click listener
wiring, none of which a plain unit test can catch. **This test also passes** - the number row is seeded
(confirming D-72), the persisted pattern and model are both correct, and the freshly opened
`TouchModelActivity` reflects them accurately. Two independent regression tests at different levels (pure
store round-trip, and full real-Activity-with-a-real-click) both pass; the root cause remains unfound in
this code path. Reported this back to the user and asked for a much more specific repro (ideally a
screenshot, or an exact description of which key's zone looks wrong and how).

### D-74 - Resolved (Unconfirmed Cause)
User confirmed the switch now works correctly and the seeded zones look right (including the deliberate
D-68/D-71 overlap into neighbouring keys' territory at the far reach - by design, not a bug). Neither of
this round's changes (§21/§22: label removal, dead-row cleanup, the rename, two new tests) touched the
actual persist/load mechanism, so the fix cannot be attributed with certainty to anything in this
conversation - most likely D-74's original service-side staleness guard, if the user's device had not yet
picked up that build during the earlier failing attempts, though a one-off device/render quirk cannot be
ruled out either.

## §23 - Device-Feedback Round 12 (v0.7.36 testing)

### D-80 - "Show Touch Zones" Intro Text Sits Under the Front-Camera Cutout
The [TouchModelActivity] (D-24) intro text starts right at the very top of an edge-to-edge screen, so it is
partially obscured by a front camera cutout. Root cause: the screen's window-insets handling (matching
[CalibrationActivity], which has the identical bug for its own top-of-screen intro text) only accounted for
the bottom inset (navigation bar / gesture pill, for the keyboard preview at the bottom); nothing padded the
top for the status bar / display cutout. Both screens now also inset for `WindowInsetsCompat.Type.statusBars()`
and `WindowInsetsCompat.Type.displayCutout()`. While fixing this, also gave the D-24 intro a more generous,
readable presentation as requested: split into two clearly separated paragraphs (`d24_intro_1` /
`d24_intro_2`, replacing the single `d24_intro` string), a larger font size, and more breathing room -
there being little text and plenty of empty space above the keyboard preview to use.

### D-81 - The Onboarding Panel Has the Same Cutout Gap
The same class of bug (D-80) also applies to the first-run onboarding panel: while it is shown,
`AdaptKeyService` stretches the whole input view (`root`) to the full screen height
(`setOnboardingShown()`), so its top can now also reach the status bar / a front-camera cutout - but its
`setOnApplyWindowInsetsListener` only ever padded the bottom (for the gesture-nav-anchored keyboard below).
Fixed the same way as D-80: also inset for `statusBars()` / `displayCutout()`. During ordinary typing
`root` only wraps the (bottom-anchored) keyboard and suggestion bar and never reaches the top inset region,
so this reports (and adds) zero extra padding there then, exactly mirroring how the bottom inset already
behaved regardless of onboarding state - no risk of adding unwanted blank space above the keyboard during
normal typing.

## §24 - Device-Feedback Round 13 (v0.7.38 testing)

### D-82 - Page-Slide Still Bleeds Into the Gesture Area (D-76 Follow-Up)
The D-76 wobble is gone, but the slide can still visibly draw into the bottom gesture-nav inset padding.
Root cause: D-76 deferred `requestLayout()` until the slide animation ends, so the view's own measured
height stays frozen at the *outgoing* page's height for the whole animation - when sliding into a page with
*more* rows (e.g. symbols, 4 rows, into letters, 5 rows with the number row shown), the incoming page's
extra row is positioned below the view's still-old, shorter bounds. Because the container disables
`clipChildren` (D-53, so the long-press popup can overflow upward over the suggestion bar), that overflow
was never clipped and bled straight through into the reserved bottom-inset padding. Fixed by explicitly
clipping the key-drawing canvas to the view's own current bounds during `onDraw` (both the slide and the
plain case), leaving the long-press popup draw call outside that clip so its own D-53 overflow keeps
working. The visible cost: a page that grows by a row briefly clips that row until the deferred resize
lands at the end of the slide, rather than letting it bleed into the gesture area.

### D-83 - Key-Click Sound: Too Loud and Slightly Delayed
Two follow-ups to D-70's typewriter-click sample, both confirmed good in character:
- **Volume**: `CLICK_VOLUME` cut from `0.9` to `0.3` (SoundPool's volume range is linear, not dB, so this
  is a much bigger perceived cut than the raw numbers suggest) - comfortably past the requested "at least
  50% quieter".
- **Perceived delay**: three changes, each addressing a plausible source of `SoundPool` startup/output
  latency: (1) the bundled sample is re-encoded at 48 kHz (many devices mix natively at 48 kHz; the
  previous 44.1 kHz file could need on-the-fly resampling), (2) the async decode is now kicked off the
  moment the D-05 sound setting turns on rather than lazily on the first key press, so there is no
  decode-in-progress window during actual typing, and (3) `AudioAttributes.USAGE_GAME` replaces
  `USAGE_ASSISTANCE_SONIFICATION` - the usage `SoundPool`'s own reference documentation recommends for
  low-latency one-shot effects, and measurably faster to start on device for this exact class of sound.

### D-84 - Mid-Word Editing (D-62) Produces No Suggestions At All (Bug)
D-62's `reclaimSurroundingWord()` captures `tokenContextBefore` (via `captureTokenContext()`) *before*
deleting the reclaimed "before" fragment from the real editable, but never trims it back afterwards - so
the reclaimed fragment ends up duplicated: once as `tokenContextBefore`'s trailing text, once inside
`composing` itself. Every `"$tokenContextBefore $typed"`-style string built later
(`refreshSuggestions()`, `finalizeAndCommit()`) therefore contained a repeated word fragment, which was
silently confusing the A-03 language classifier into misreading the context as foreign and suppressing
suggestions *and* autocorrect entirely for the rest of the token - exactly the reported "no suggestions
appear" symptom, and specific to the mid-word reclaim path (an ordinary end-of-word token never has this
duplication). Fixed by trimming the reclaimed length off `tokenContextBefore` right after the reclaim.

### D-09 - Raw-Tap Recording: Removed Entirely
Per explicit instruction ("bitte komplett aufräumen"), the D-09 diagnostic - orphaned since D-68 replaced
the sentence-based calibration it recorded against - is now gone rather than repurposed: `RawTapRecorder`,
`CalibrationSentences`, `CalibrationSession` and their tests, `AdaptKeyboardView.OnRawTapListener` /
`onRawTapListener` and its D-09 forwarding call, the `recordRawTaps` setting (`AdaptSettings`,
`RawSettings`, `SettingsMapper`, `SettingsStore`), and the `d09_*` strings (all 3 locales) are all deleted.

## §25 - Device-Feedback Round 14 (v0.7.39 testing)

### D-82 - Confirmed: No More Bleed
User confirmed the D-82 clip fix produces no more visible bleed into the gesture area. Also raised, purely
as a design discussion (not a defect report): why not always draw pages bottom-aligned so the space row
sits at a fixed position and only the top edge moves with row count, or - if that alone doesn't solve it -
resize *before* the slide when the target page is taller and *after* it when the target page is shorter
(instead of D-76's blanket "always defer, clip during"), so a growing page never needs to be clipped at
all. Flagged as a worthwhile refinement to pick up if the user wants it (see progress.md) - not implemented
yet, pending confirmation.

### D-85 - Key-Click Sound: Halved Again
D-83's 0.3 volume was a clear improvement but still not "dezent" (subtle) enough. `CLICK_VOLUME` halved
again to `0.15`. **Confirmed by the user: "perfekt", and the D-83 latency work "ziemlich erträglich"
(quite tolerable)** - the sound-feedback thread is done unless something regresses.

### D-86 - Page-Slide: Resize Before Growing, After Shrinking; Bottom-Aligned Rows
D-82's clip fix confirmed no more bleed into the gesture area, but user pointed out a related artifact it
doesn't address: shrinking into a page with *fewer* rows can make that page suddenly appear pinned to the
*top* edge of the still-larger (not yet resized) container, instead of staying anchored to the bottom where
the user expects the space/enter row to be. Two changes, layered on top of D-82's clip (kept as a
last-resort safety net against `requestLayout()`'s inherent async re-measure latency):
- `switchPage()` now compares the target page's row count against the current one (`rowsFor()`, extracted
  from `rebuildRows()`) and resizes *immediately*, before starting the slide, when growing into more rows -
  the incoming page is never short on space in the first place. Shrinking into fewer rows still defers the
  resize until the slide ends, exactly as D-76 originally fixed (avoiding a visible mid-slide jump).
- `layoutKeys()` now lays rows out bottom-up (anchored just above `paddingBottom`) instead of top-down.
  Whenever the view's current height does not (yet) match what `rows` needs - the transient window either
  direction change leaves during its animation - the bottom-most row stays pinned to its usual position,
  with any slack appearing as blank space above the top row. Exactly reproduces the previous top-down
  result once the height matches again (the steady state, which is when `onMeasure()` already sizes the
  view to that same content height), so this is a no-op outside the transient window.

## §26 - Backlog Captured, Not Yet Implemented (v0.7.41 planning round)

D-86 confirmed working ("Animation ist jetzt perfekt"); touch-zone precision from the D-71/D-73 typing-style
work is confirmed good. Next focus area named by the user: making autocorrect quality genuinely excellent.
This round's items are captured here as backlog, per the usual rule for a larger batch - **not started**
until explicitly released, except where noted.

### D-87 - Mid-Word Live Correction (D-62) Still Does Not Activate At All (Bug, Re-Opened)
Despite the D-84 fix (which addressed a real, confirmed bug - the duplicated `tokenContextBefore` silencing
suggestions/autocorrect), the user reports mid-word editing still never activates the live-correction
behaviour at all. D-84's fix was verified logically (the duplication bug it targeted was real and clearly
would have caused exactly the "no suggestions" symptom reported at the time), but something else is
apparently still preventing D-62 from working, or D-84 did not fully resolve the reported case. Needs a
fresh investigation pass - likely starting with a very precise repro description (exact keyboard/app used,
exact tap sequence, whether *any* visible reaction happens at all e.g. does the word even get reclaimed
into a colourable/editable token) rather than another blind fix, given the previous "fix" that logically
should have worked apparently didn't (fully) resolve it.

### D-88 - Feedback When a Suggestion Is Accepted
Currently silent and visually unremarkable - "das ist etwas trocken und man kriegt es nicht ausreichend
mit" (too dry, not noticeable enough). Two additions, chosen by whether D-05 key sound is enabled:
- **Sound on**: a distinct short "plop" sample (separate from the D-70/D-83/D-85 key-click sample) plays
  when a correction is accepted - i.e. on the autocorrect commit path in `finalizeAndCommit()` when the
  committed word differs from what was typed, and/or when a suggestion-bar item is tapped.
- **Sound off**: some kind of brief micro-animation on acceptance instead, so the change is still
  noticeable without audio. Exact treatment (e.g. a quick colour flash or scale pulse on the committed
  word, or in the suggestion bar itself) to be designed during implementation.

### D-89 - Settings: a Full Feature Overview Page
A new settings screen (or section) that lists every feature the keyboard actually has - "nur, wenn man von
den Features weiß, kann man auch alle davon richtig nutzen" (you can only really use a feature if you know
it exists). AdaptKey has accumulated a large, easy-to-miss feature surface (raw-coordinate correction,
diacritic-aware autocorrect, drag-to-trash, the D-19 swipe cycle, G-01…G-05 gestures, D-36 quick paste,
D-43 next-word prediction, the D-01 long-press popups, etc.) with no single place a user can discover them
all. Needs its own content/copy pass, not just a settings-row stub.

### D-90 - New Alt-Key Hints on the Main Letter Page: P → π, B → ×, V → ÷
Extends [KeyboardLayout.DEFAULT_LETTER_HINTS] (L-05 / C-08) with three more math symbols, reachable by
long-press exactly like the existing `q→@`, `e→€`, `h→#`, `m→-`, `n→+`, `d→°` hints: `p→π` (pi), `b→×`
(multiplication sign), `v→÷` (division sign). Independent of, and complementary to, the calculator-page
redesign below (D-92) - these stay reachable without leaving the letter page at all, and are unrelated to
whatever the calculator page's own `×` / `÷` keys look like.

### D-91 - Reverse the D-19 Horizontal Swipe Direction
Currently a **right** swipe (`SwipeDirection.RIGHT`) advances forward through the D-19 cycle (letters →
symbols page 1 → symbols page 2) and a **left** swipe goes back ([KeyGesture.surfaceSwipe]). User wants
this reversed: reaching the first special/symbol page should be the **right**-swipe direction instead of
the left. A small, self-contained, easily-reversible change - swap which [SwipeDirection] maps to
`SWITCH_SURFACE_NEXT` vs `SWITCH_SURFACE_PREV` in `KeyGesture.surfaceSwipe()`. Does **not** touch the
space-bar language-swipe direction (G-01, `LANGUAGE_PREV` / `LANGUAGE_NEXT`), which is unrelated and not
mentioned. D-76/D-86's `forward` slide-direction plumbing in `AdaptKeyboardView.switchPage()` already
mirrors whatever `forward` value it's given, so the visual slide direction will automatically follow this
change with no separate adjustment needed there.

### D-92 - Redesign Both Symbol Pages: a Real Calculator Layout, Then a Leftover-Character Catch-All
The two `?123` pages get properly differentiated instead of being two similar-looking generic symbol grids,
and the two separate currency keys (`€` as an `e` alt-hint, `£` as a dedicated page-2 key) become **one**
locale-aware currency key with a popup for the rest (D-92 folds in what was originally a separate ask).

**First page - a real calculator layout** (proposed starting design, explicitly meant to be refined
further once it's actually being built):

```
Row 1:  (    )    °    √    π    ~    &    |    [1/2]  ⌫
Row 2:  7    8    9    ÷
Row 3:  4    5    6    ×
Row 4:  1    2    3    −
Row 5:  ABC  [¤]  0    [,]  +    =    [space]  ⏎
```

- Digits large and grouped as a real block (7-8-9 / 4-5-6 / 1-2-3 / 0), not spread across a single row -
  "ein echtes Taschenrechner-Layout" (a genuine calculator layout).
- `×`'s own alt-popup: `*` (asterisk) and `·` (middle dot, the German "Malpunkt") - explicitly requested.
  `÷`'s alt-popup was not fully specified; proposing `/` and `:` to mirror it (needs confirmation).
- `=`'s alt-popup: `→` (arrow), `≈` (approximately equal, "= mit ~"), `≙` (U+2259 ESTIMATES, the German
  "entspricht"-sign, "Gleich mit Dach").
- This page's own `2` and `3` keys (not the main page's number row, which is untouched) get `²` / `³` as
  alt-hints.
- `[¤]` is the new consolidated currency key (see below): base glyph depends on the system/keyboard
  language (`€` for German), alt-popup offers the other common currencies - `$` and `£` foremost, plus a
  few more common ones (e.g. `¥`).
- `[,]` is the decimal separator, language-dependent (`,` in German, `.` in English); its alt is the
  language-dependent thousands separator (`.` in German, `,` in English).
- `[1/2]` keeps its existing role (D-19 / L-03, toggles the two special pages - now "calculator" and
  "extra characters" rather than "symbols page 1/2").
- `[space]`: explicitly requested smaller and "directly above the Enter key", staying bottom-right. Placed
  inline directly left of Enter in this draft instead, since a true stacked-above-Enter placement (a
  partial-row-height key) is not something the current row/weight-based layout engine supports yet - would
  need a small layout-engine extension. Flagging this as an open question to resolve during implementation:
  is the inline-adjacent placement an acceptable interpretation, or is the stacked layout worth building?

**Second page - leftover / catch-all characters, no number row** (independent of the C-09 number-row
setting, which only ever applies to the main letters page): everything not already reachable as an alt
somewhere else (a main-page letter hint, a comma/period popup entry, or now the calculator page) is largely
already covered once the above lands - the German-locale audit found only `@`, `_`, `"`, `'` genuinely left
over from the old two symbol pages (`:` `;` `!` `?` already live on the comma/period popups; `#` `§` `&`
already live as alt-hints or on the number row; `*` moves to the `×` popup; `(` `)` `°` `√` `π` `~` `|` `=`
`£` `¥` all move to the calculator page). Per the invitation to add common characters to fill the now
sparser page: proposing `•` `©` `±` and the bracket family `{ }` `[ ]` `< >` (useful for anyone typing code
snippets or math outside the calculator page) - open for adjustment once this page is actually in front of
someone. No sentence punctuation here by design - already well reachable on the main page.

### D-93 - Symbol Pages Drop `1/2` and `ABC` When the Combined `?123` Key Is Disabled *(implemented, v0.7.42)*
When D-59's `symbolKeyEnabled` setting is off, the dedicated page-toggle (`1/2`/`2/2`) and back-to-letters
(`ABC`) keys on the symbol/numeric pages (L-03) are redundant with the D-19 full-field swipe, which already
reaches every surface (letters, symbols page 1, symbols page 2) regardless of that setting. `SymbolLayout.rows()`
now takes a `symbolKeyEnabled` parameter (default `true`, matching prior behaviour) and simply omits those two
keys from their rows when it is `false`; the remaining keys in each row grow to fill the freed space
automatically, since row layout already divides row width proportionally by the sum of the row's key weights
(`AdaptKeyboardView.layoutKeys()`) - no separate weight recalculation was needed. A small, clearly-scoped,
directly-approved fix, implemented immediately rather than queued in the §26 backlog.

### D-89 - Implemented: Settings Feature Overview (v0.7.43)
Added a new `FeatureOverviewActivity` ("Was AdaptKey alles kann" / "What AdaptKey can do"), launched from a
new entry in the settings screen's Info & Privacy category, right after "Show introduction again". It renders
a scrollable list of 18 curated feature entries (title + one/two-sentence description each), covering the
adaptive touch model, diacritic-aware autocorrect, neighbour-key/space correction, post-commit autocorrect
undo, multi-alternative long-press popups, alt-key symbol hints, the persistent number row, the D-19
letters/symbols swipe, the G-01…G-05 gesture set (language switch, whole-word delete, dismiss, drag-to-trash,
retroactive capitalisation), quick paste, next-word prediction, the optional tier-3 mini-AI, the three
auto-detected languages, and the provably-offline guarantee. The entry list itself lives in a pure
`FeatureCatalog` object (a list of string-resource-id pairs) so it is unit-testable without Robolectric;
`FeatureOverviewActivity` just inflates it into simple title/description view pairs, covered by a Robolectric
test that checks one view pair per entry is actually rendered. Full text in all three locales (English,
German, Greek).

### D-90 - Implemented: New Alt-Key Hints P → π, B → ×, V → ÷ (v0.7.44)
Extended `KeyboardLayout.DEFAULT_LETTER_HINTS` with the three new math-symbol long-press hints exactly as
specced: `p`→`π`, `b`→`×`, `v`→`÷`, alongside the existing `q→@`, `e→€`, `h→#`, `m→-`, `n→+`, `d→°`. `π` was
also added to the C-08 editor's `LetterHints.PALETTE` (next to the already-present `×`/`÷`), so a user who
reassigns `p` elsewhere can still pick `π` for another key, and so the existing
`PALETTE.containsAll(DEFAULT_LETTER_HINTS.values)` invariant test keeps holding.

### D-91 - Implemented: Reversed the D-19 Swipe Direction (v0.7.45)
Swapped which `SwipeDirection` maps to `SWITCH_SURFACE_NEXT` vs. `SWITCH_SURFACE_PREV` in
`KeyGesture.surfaceSwipe()`: a **left** swipe now advances forward through the letters -> symbols-page-1 ->
symbols-page-2 cycle, and **right** goes back. The space-bar language swipe (G-01) and the delete-word swipe
(G-02) are untouched - only the `else` branch of `KeyGesture.resolve()` (ordinary letter keys and the
combined `?123` key's horizontal swipe) is affected. The visual slide direction
(`AdaptKeyboardView.switchPage()`'s `forward` plumbing from D-76/D-86) automatically follows, since it already
mirrors whatever `forward` value it is given - no separate change needed, exactly as anticipated when D-91
was captured.

### D-92 - Implemented: Redesigned Both Symbol Pages (v0.7.46)
Rebuilt `SymbolLayout` per the §26 draft. Before implementing, the user resolved the one open question the
draft had flagged: the space key stays **inline, directly left of Enter** (no layout-engine extension for
partial-height rows) - "machen wir das erstmal inline neben Enter und schauen, wie es wirkt. Umbauen können
wir es immer noch."

**Page 1 - calculator** (5 rows): row 1 is `( ) ° √ π ~ & | [1/2] ⌫` (8 everyday symbols, then the page
toggle, then backspace - reordered from the old convention of leading with the toggle, to match the D-92
draft exactly); rows 2-4 are the digit block `7 8 9 ÷` / `4 5 6 ×` / `1 2 3 −`, each cell equally weighted so
the three rows form a genuine grid; row 5 is `ABC ¤ 0 , + = [space] ⏎`. `×`'s alt-popup is `* × ·` (asterisk
and the German "Malpunkt" middle dot, as requested); `÷`'s mirrors it with `/ ÷ :` (not fully specified in the
draft, decided per the "needs confirmation" note there); `=`'s is `= → ≈ ≙` (arrow, "≈", and the German
"Gleich mit Dach"). This page's own `2`/`3` keys carry `²`/`³` as long-press hints (the main number row is
untouched). `¤` is the new consolidated currency key, and `,` is the decimal separator with `.` as its
thousands-separator hint - both **resolved from the device's system locale** (see the D-92 follow-up below;
an earlier version of this entry hardcoded both to `€`/comma, which the user corrected). The bottom row's
`[space]` is deliberately smaller than the normal space bar (a local `CALC_SPACE_WEIGHT = 1.5f`, not exposed
via `KeyProportions` since it is a fixed per-page design choice, not a user setting) - this is the most
crowded row in the whole keyboard.

**Page 2 - leftover catch-all** (3 rows, no number row regardless of C-09): row 1 is `@ _ " ' • © ± [1/2] ⌫`
(the German-locale audit's leftover characters); row 2 is the bracket family `{ } [ ] < >`; row 3 is
`ABC [space] ⏎` with the normal-sized space bar (this row is not crowded) and deliberately no comma/period -
already well reachable on the main page.

Both pages keep D-93's `symbolKeyEnabled` gating: the page-toggle and `ABC` keys are omitted when the
combined `?123` key is disabled, on both pages' row 1 and bottom row respectively.

**D-92 addendum, raised by the user while resolving the space-key question:** "Der Sprachwechsel ist hier
natürlich nicht nötig. Den brauchen wir nur auf der Hauptseite" - the G-01 space-bar language swipe should
only apply on the letters surface, not on the (now also present, and smaller) space key on the symbol pages.
`KeyGesture.resolve()` gained a `surface: InputSurface = InputSurface.LETTERS` parameter; on any surface other
than `LETTERS`, a horizontal swipe on `KeyCode.SPACE` now falls through to the ordinary D-19 surface-swipe
behaviour instead of switching language. `AdaptKeyService.handleSwipe()` passes its current `surface` through.
This also fixes the same latent behaviour on the pre-D-92 symbol pages, which already had a space key that
inadvertently switched language on a horizontal swipe.

### D-92 follow-up - Implemented: Currency/Decimal Separator Now Follow the System Locale (v0.7.47)
The initial D-92 implementation hardcoded the currency key to `€` and the decimal separator to comma,
reasoning that the app's two selectable keyboard alphabets (German, Greek) never disagree on either point.
The user corrected this: both the **keyboard-selected language** and the **system-selected language/region**
were meant to influence these keys, with an explicit fallback rule - "bei Tastatur-sprachlicher Gleichheit
(egal) auf das System als Entscheidung zurückfallen" (when the keyboard language is a tie, fall back to the
system for the decision). Since German and Greek always tie on this specific point, the practical result is
that the **system locale now always decides** - "Ein Amerikaner wird kein Verständnis für ein Komma und ein
€ Zeichen haben," and English already exists in AdaptKey as a language, "wenn auch subtiler" (via A-03
auto-detection, even though it is never a directly selectable keyboard alphabet).

Implementation: a new pure, JVM-testable `CalculatorLocale` object (`keyboard` package) resolves a
`java.util.Locale` into a `Format` (currency glyph + popup, decimal separator, thousands-separator hint)
using the JDK's own locale data - `java.text.DecimalFormatSymbols` and `java.util.Currency` - rather than a
hand-maintained table, so it is correct for locales well beyond the app's own DE/EN/EL dictionaries (verified
for `en_US` → `$`/`.`, `en_GB` → `£`/`.`, `de_DE` → `€`/`,`, `el_GR` → `€`/`,`, `ja_JP` → the fullwidth yen
sign). `Key.char` is a single `Char`, but not every locale's currency resolves to one glyph (a bare ISO code,
or a locale without a resolvable country); those fall back to `€` rather than truncating or crashing.
`SymbolLayout.rows()` gained a `locale: Locale = Locale.getDefault()` parameter (calculator page only);
`AdaptKeyboardView` gained a `systemLocale: Locale` property threaded through the same way as
`symbolKeyEnabled`; `AdaptKeyService.applySettings()` sets it from `resources.configuration.locales[0]` - the
device's actual system locale, deliberately not the app's own DE/EL keyboard-alphabet toggle.

---

## §27 - Device-Feedback Round 15 (v0.7.47 testing)

D-91 confirmed working ("Das funktioniert genau richtig") - a real device round on the D-89…D-92 batch,
mostly focused on the D-92 calculator/catch-all pages. Captured here as backlog, per the usual rule -
**not started** until explicitly released.

### D-94 - Bug: the D-91 Swipe Reversal Didn't Reach the Slide Animation
D-91 correctly reversed which direction (`SwipeDirection`) advances vs. goes back through the letters →
symbols-1 → symbols-2 cycle, and the user confirms the *function* is now exactly right. But the *visual*
slide animation still moves the old way. D-91's implementation assumed `AdaptKeyboardView.switchPage()`'s
`forward` plumbing (from D-76/D-86) would automatically follow the new mapping with no separate change - that
assumption was apparently wrong, or incomplete. Needs investigation: likely `applySwipePage()` /
`PanelNavigation.swipePage()` in `AdaptKeyService` still derive the slide's `forward` flag from the page-index
comparison (`targetSymbolPage > symbolPage` or similar) rather than from which *swipe direction* the user
actually made, so the two got decoupled once D-91 remapped direction to action independently of page order.

### D-95 - Idea, Parked: a Settings-Shortcut Key Somewhere on the Keyboard
A key that jumps straight to Settings would be convenient, but the user has no placement idea yet and
explicitly wants this only kept in mind for now ("behalten wir das erstmal nur im Hinterkopf") - not an
actionable backlog item until a placement makes sense (a spare slot might open up once D-100 below reshuffles
the calculator page, worth revisiting then).

### D-96 - Main Page: Reorganise the v / b (and New x / c) Alt-Hints
Current (`KeyboardLayout.DEFAULT_LETTER_HINTS`, from D-90): `b`→`×`, `v`→`÷`. New assignment:
- `x` → `×` (multiplication moves here)
- `c` → `÷` (division moves here)
- `v` → `/` (slash, new)
- `b` → `*` (asterisk, new)

`x` and `c` currently carry no hint at all, so this is a clean four-way reassignment, not a conflict. `π` on
`p` (D-90) is untouched.

### D-97 - Calculator Page: Space Key Shouldn't Show the Language Label
D-03 makes every `KeyCode.SPACE` key show the active input language ("Deutsch") via
`AdaptKeyboardView.labelFor()`'s `spaceLabel` branch - correct for the main letters page, but the calculator
page's smaller, inline space key (D-92) inherits it too, which makes no sense there (mirrors the same
surface-scoping mistake G-01 had before the D-92 follow-up fixed the *gesture* side). Wants the plain,
Gboard-style long rounded-rectangle space glyph ("▭"-like, exact glyph/icon TBD during implementation)
instead, regardless of active language. Likely needs `labelFor()` (or the caller) to know which surface a
given space key belongs to, similar to how `KeyGesture.resolve()` gained a `surface` parameter for D-92.

### D-98 - Calculator Page: Currency Popup Order + a "More Alternatives" Corner Indicator
Two asks:
1. Reorder the currency popup to `€ $ £ ¥` (currently `$ £ € ¥` from the original D-92 implementation).
2. A corner-hint glyph that signals "this key has more long-press alternatives" - **does not exist yet**: today
   `AdaptKeyboardView` only ever draws a key's own [Key.hint] string as the corner glyph (L-05/C-08); a
   multi-alternative key (D-01) with `alternatives.size >= 2` but `hint == null` - comma, period, the currency
   key, `×`/`÷`/`=` on the calculator page, etc. - currently draws **no** corner indicator at all. This would be
   a new, generically-applicable visual (small dot/triangle badge, exact treatment TBD), not specific to the
   currency key alone.

Also flagged, tied to D-100's layout move below: once the currency key relocates into the new right-hand
column, a popup that always grows rightward (D-44) may run out of room against the screen edge - the order
(or growth direction) may need to flip depending on the key's on-screen position. Needs resolving once D-100's
exact column position is implemented and actually on screen.

### D-99 - Main Page: Greek-Letter Alt-Popup on the π Key
The `p`→`π` key (D-90) gets a multi-alternative long-press popup (D-01) offering the Greek letters
`α β γ δ λ ω` (Alpha, Beta, Gamma, Delta, Lambda, Omega) alongside `π` itself.

### D-100 - Calculator Page: Narrower Digit Block, New Persistent Right-Hand Column, Digit-Grid 4th Row, Page-Toggle Key Removed
The biggest item this round - a further reshuffle of D-92's calculator page:

- **Narrow the digit block** to free a new column on the **right edge**, running the full height of the page
  (row 1 down to the bottom row), holding, top to bottom: `⌫` (already there), `[space]` (moved here, under
  backspace), `¤` (currency, moved here, under space), the optional `ABC` key (D-59-gated, moved here, under
  currency), `⏎` (already there, at the bottom). **All cells in this right-hand column must render at the
  same width as each other** ("alle Tasten bis runter zum Enter in derselben Breite").
  - Technically non-trivial: `AdaptKeyboardView.layoutKeys()` divides each row's own width proportionally by
    the *sum of that row's own key weights* - rows with very different total content (row 1's ~7-8 symbols vs.
    a 4-cell digit row) will not automatically give a same-*weight* trailing cell the same *pixel* width. The
    right column's weight per row will likely need computing from each row's own total content weight (so the
    column occupies the same *fraction* of every row), not just reusing one constant weight value.
- **The digit block gains a 4th row**, turning the operators + digits area into a genuine 4×4 grid:
  ```
  7  8  9  ÷
  4  5  6  ×
  1  2  3  −
  0  ,  =  +
  ```
  `0` under `1`, the decimal separator under `2`, `=` under `3`, and `+` under the operator column (÷/×/−).
  Every cell in this 4×4 grid renders at the same width as the column above it.
- **The page-toggle key (`1/2` on this page, `2/2` on page 2 - the user's note names it "die 2/2 Taste" but the
  surrounding text is entirely about page 1's layout, so this is read as shorthand for "the page-toggle key",
  to be confirmed) is removed entirely from both pages, without D-93's `symbolKeyEnabled` gating** - it's just
  gone, freeing its slot unconditionally. The two symbol pages become reachable **only** via the D-19/D-91
  full-field swipe, or the documented fallback detour: `ABC` back to the main letters page, then the combined
  `?123` key back to symbols page 1, then swipe to page 2.

### D-101 - Calculator Page: Bracket Family as the `(` Key's Alt-Popup
The separate `(` and `)` keys (row 1) become **one** key (exact base glyph/label TBD - `(` is the natural
choice) whose D-01 multi-alternative popup offers `)` plus the rest of the bracket family: `{` `}` `[` `]`
`<` `>`. This is what makes page 2's bracket row (D-102 below) redundant.

### D-102 - Page 2: a Real Number-Row Alternative + Full Letter-Hint Distribution
Page 2 gets upgraded into something genuinely useful for anyone who has turned the main page's number row off
(C-09) - not just a leftover catch-all:

- **Remove the bracket row** (`{ } [ ] < >`) - redundant once D-101 lands (reachable via the `(` key's popup).
- **Add back a fixed digit row** (`1234567890`), shown **unconditionally, independent of C-09** - this
  explicitly **reverses** D-92's original "no number row regardless of C-09" decision for this page. The
  intent: C-09 stays a main-page-only declutter option, and page 2 becomes the fallback place to reach digits
  (and, per the next point, their shifted symbols) when it's off.
- **A new row directly under the digit row**, holding all of the main number row's shifted-symbol long-press
  hints (`L-06`/`NUMBER_HINTS`: `! " § $ % & / ( ) =`) as plain, directly-tappable characters - not hidden
  behind a long-press, since the whole point is restoring easy access to them when the main number row (and
  its long-press hints) are hidden.
- **Distribute the main letter page's alt-hint symbols** (everything in `KeyboardLayout.DEFAULT_LETTER_HINTS`
  - `@ € # - + °` plus D-96's reorganised `× ÷ / *`, and `π`'s own new Greek-letter popup from D-99 stays
    where it is) across the rest of page 2's available space "sinnvoll" (sensibly) - **the user explicitly
    flags this as provisional**: "schauen wir, wie sich das ergibt und sortieren ggf. nochmal um" (we'll see
    how it turns out and re-sort if needed). Treat the first implementation as a draft layout to review
    together, not a final answer.

### D-103 - Calculator Page: a Trig-Function Key (sin, alt: cos / tan / log)
A key labelled `sin` whose D-01 multi-alternative long-press popup offers `cos`, `tan` and `log`. No
placement decided yet - the user explicitly wants it fit in wherever it reasonably ends up once D-100's
reshuffled calculator page exists ("versuche einfach, sie sinnvoll unterzubringen... dann schaue ich, ob mir
das gefällt" - just try to place it sensibly, then I'll see if I like it). Worth considering together with
D-100's new right-hand column and 4×4 grid, and with D-104 below, since both are calculator-page additions
competing for the same likely-tight space. Multi-character labels (`sin`/`cos`/`tan`/`log`) are new territory
for this key grid - every existing key is a single glyph - so this may also need a small label-width sanity
check once it's actually laid out.

### D-104 - Calculator Page: a deg/rad Toggle Key
Same status as D-103 - no placement decided, park it alongside D-103 and fit both in wherever makes sense once
D-100 lands, then get the user's read on it. Toggles the trig-function key's (D-103) angle unit; exact
key behaviour (a two-state toggle vs. its own popup) to be decided during implementation.

---

## §28 - §27 Release Round 1 (v0.7.48+)

User confirmed the D-100 "die 2/2 Taste" reading (§27) as correct, and clarified D-100 further: since every
digit-grid row except the first has the same cell count, the right-hand column's width will line up "optimal"
without needing special per-row weight math - noted for whenever D-100 is released. Released this round, in
this order: D-96, D-97, D-98, D-99, D-101, then D-94.

### D-96 - Implemented: Reorganised the v / b / x / c Alt-Hints (v0.7.48)
`KeyboardLayout.DEFAULT_LETTER_HINTS` now maps `x`→`×`, `c`→`÷`, `v`→`/`, `b`→`*`, replacing D-90's `b`→`×` /
`v`→`÷`. `x` and `c` carried no hint before, so this was a clean four-way reassignment. `p`→`π` (D-90) is
unaffected. All four reassigned letters live on the third row (`yxcvbnm`), so nothing moved between rows.

### D-99 - Implemented: Greek-Letter Popup on the π Key (v0.7.48)
The `p`→`π` key now carries a D-01 multi-alternative popup, `KeyboardLayout.PI_ALTERNATIVES = listOf("π", "α",
"β", "γ", "δ", "λ", "ω")`, alongside its existing corner hint. `KeyboardLayout.rows()` gained a private
`topRowKey()` helper that only attaches the popup when `letterHints['p']` still equals the default `π` hint -
a user who has reassigned `p` to something else via the C-08 editor keeps their own plain single-symbol
long-press instead of an unrelated Greek-letter popup. The underlying popup mechanism (`Key.alternatives`,
`AdaptKeyboardView.popupAlternativesFor`) is already fully generic across every layout (letters, symbol
pages), so no view-layer change was needed - only the key data itself.

### D-97 - Implemented: Symbol Pages' Space Key No Longer Shows the Language Label (v0.7.49)
`AdaptKeyboardView.labelFor()`'s D-03 branch (space bar shows the active input language) now checks
`surface == InputSurface.LETTERS` before using `spaceLabel`; on any other surface, `KeyCode.SPACE` falls
through to a new plain glyph, `SPACE_GLYPH = "␣"` (U+2423 OPEN BOX - the usual elongated-open-rectangle space
symbol the user described). Scoped to "not the letters surface" rather than "the calculator page
specifically", for consistency with the same surface-scoping the D-92 follow-up already applied to the G-01
gesture - this also fixes page 2's space key, which had the same latent issue even though only the calculator
page was explicitly mentioned. Purely a `labelFor()`/drawing change; no test added (private, drawing-only View
glue - an existing, documented testing gap for this class, not a new one).

### D-98 - Implemented: Currency Popup Order + "More Alternatives" Corner Indicator (v0.7.50)
1. `CalculatorLocale.COMMON_CURRENCY_SYMBOLS` reordered to `€ $ £ ¥` (was `$ £ € ¥`).
2. New generic corner indicator: `AdaptKeyboardView.drawKeys()` now draws a small triangle,
   `MORE_ALTERNATIVES_GLYPH = "◢"`, in a key's corner whenever it has no single [Key.hint] of its own but does
   have a D-01 multi-alternative popup (`alternatives.size >= 2`) - comma, period, and the calculator page's
   `×`/`÷`/`=`/currency/`(` keys all previously drew no corner cue at all. Applies generically to every layout,
   not just the currency key. Gated by the same `hintsEnabled`/`suppressHint` checks as the existing hint
   glyph. The right-to-left popup-growth question flagged alongside this item in §27 (tied to D-100's column
   move) is deferred until D-100 actually lands and the key's on-screen position is known.

### D-101 - Implemented: Bracket Family as the `(` Key's Alt-Popup (v0.7.51)
Page 1's row 1 drops the separate `)` key; `(` now carries `BRACKET_ALTERNATIVES = listOf("(", ")", "{", "}",
"[", "]", "<", ">")` as its D-01 popup (base included first, matching the `=`/period/comma convention). Row 1
goes from 8 symbols to 7 (`CALC_ROW1_SYMBOL_COUNT` updated accordingly, so `thirdRowLetterWeight` still
accounts for the actual cell count). Page 2's bracket row is untouched for now - removing it is D-102, not yet
released, so it stays as a (now momentarily redundant) fallback until then.

### D-94 - Implemented: Fixed the Slide-Animation Direction After D-91 (v0.7.52)
Root cause confirmed as suspected: `AdaptKeyService.handleSwipe()` derived the slide animation's `forward`
flag from the resulting *action* (`SWITCH_SURFACE_NEXT` → `true`, `SWITCH_SURFACE_PREV` → `false`), not from
the *physical swipe direction*. Before D-91 these always coincided (right swipe was always NEXT), so the bug
was invisible; D-91 decoupled them (left swipe is now NEXT), and the animation kept following the old
action-based logic - so it played backwards relative to the finger for exactly the cases D-91 changed.

Fix: both `SWITCH_SURFACE_NEXT`/`SWITCH_SURFACE_PREV` branches now pass `forward = direction ==
SwipeDirection.RIGHT` to `applySwipePage()` for the animation, while `PanelNavigation.swipePage()`'s own
`forward` argument (which page to land on) stays action-based, unchanged. These are two genuinely different
"forward" concepts that happened to share one boolean pre-D-91: which page to land on (action-based, D-91's
concern) and which way the slide visually plays (physical-direction-based, D-76's concern, per its own
comment - "forward slides the outgoing page off to the right ... matching a right swipe"). No new test - this
is `AdaptKeyService`'s swipe-handling glue, already a documented Android-service testing gap, not a new one.

### D-100 - Implemented: Narrower Digit Block, New Right-Hand Column, Page-Toggle Key Removed (v0.7.53)
Page 1 (calculator) restructured to the confirmed reading of the §27 draft:

- **Row 1**: the row-1 symbols (`( ° √ π ~ & |`), then `⌫` - the top of the new right-hand column. No more
  page-toggle key.
- **Rows 2-4**: the digit/operator grid (`7 8 9 ÷` / `4 5 6 ×` / `1 2 3 −`), each row ending with its column
  cell - `space` (row 2, moved here from the old bottom row), `¤` currency (row 3, likewise moved), the
  optional `ABC` (row 4, D-59/D-93-gated, likewise moved).
- **Row 5 (new)**: `0` under `1`, the decimal separator under `2`, `=` under `3`, `+` under the operator
  column, ending with `⏎`.

Column-width uniformity: rows 2-5 each have the same number of other cells (4), so giving the column cell in
each of those rows the same weight (`CALC_COLUMN_WEIGHT = 1f`, matching the plain digit-cell weight) makes
them all render at the same pixel width automatically - exactly the "wird sich das optimal ergeben" the user
predicted. Row 1's `⌫` is the confirmed exception (different cell composition) and keeps
`proportions.backspaceWeight` (L-04) rather than trying to force a match. The decimal separator also drops its
old `proportions.commaWeight` (previously wider, matching the letter page's comma) in favour of the same
uniform column/grid weight as its row-5 neighbours (`0`, `=`, `+`) and the digits above it.

Page-toggle key removed entirely from **both** pages (D-93's `symbolKeyEnabled` gating no longer applies to
it - it's just gone, unconditionally). Both pages now reachable only via the D-19/D-91 swipe or the `ABC`
detour back to letters. Dead code removed along with it: `KeyCode.SYMBOL_PAGE`, `SymbolLayout.togglePage()`,
`SymbolLayout.pageLabel()`, and the corresponding tap-handling branch in `AdaptKeyService`
(`PanelNavigation`'s own independent page-cycling logic was already the only thing driving D-19/D-91 - this
key's handler was its sole other caller).

**Currency popup order, addressed proactively per the user's own flag:** now that `¤` sits in the far-right
column, a D-44 popup pre-selected near the *start* of its list (the old `€ $ £ ¥` order, with the typical
German-locale base `€` at index 0) would want to grow almost entirely rightward from a key already at the
screen edge - `AdaptKeyboardView.openPopup()`'s existing edge-clamp (`HorizontalLongPressPopup.rowLeft()`)
keeps it on-screen, but shoves the whole row away from the stem key, decoupling the visible popup from where
the finger is. `CalculatorLocale.COMMON_CURRENCY_SYMBOLS` reversed to `¥ £ $ €` so the base glyph tends to
land near the *end* of the list instead, biasing the popup to grow leftward - into the room that's actually
there. The uncommon-currency fallback (`resolveCurrencyGlyph` returning something outside the common four)
now appends rather than prepends, for the same reason.

### D-102 - Implemented: Page 2 Upgraded into a Real Number-Row Alternative (v0.7.54)
Page 2 grew from 3 rows to 5, per the confirmed reading of the §27 draft:

- **Row 1** (leftover symbols): `@ _ ' • © ±` (six, not seven - the double quote moved to row 3 below to
  avoid the same glyph appearing twice on this page), then `⌫`. Still no page-toggle key (D-100).
- **Row 2 (new)**: a fixed digit row `1234567890`, shown **unconditionally, independent of C-09** - this
  deliberately reverses D-92's original "no number row regardless of C-09" decision for this page. Plain
  digits, no long-press hints - row 3 already puts their shifted symbols directly on the surface.
- **Row 3 (new)**: the main number row's shifted symbols in `1..0` order - `! " § $ % & / ( ) =` - as plain,
  directly-tappable characters rather than hidden behind a long-press, restoring easy access to them when the
  main number row (and its hints) are hidden.
- **Row 4 (new)**: the main letter page's alt-hint symbols, distributed here: `€ # - + ° × ÷ *`. Two
  exclusions to avoid a same-page duplicate: `@` (q's hint) is skipped, already covered by row 1; `/` (v's
  hint, D-96) is skipped, already covered by row 3 (7's shifted symbol). `π`'s own Greek-letter popup (D-99)
  stays on the letters page rather than being duplicated here. **Per the user's own framing, this is a first
  draft** - "schauen wir, wie sich das ergibt und sortieren ggf. nochmal um" - explicitly open to re-sorting
  once seen on a real device.
- **Row 5** (unchanged): optional `ABC`, `space`, `⏎`.

Verified no character repeats anywhere on the page (new test, `SymbolLayoutTest`). Not yet device-tested - the
letter-hint distribution (row 4) in particular is exactly the part flagged as provisional.

---

## §29 - Correction Round on D-101 / D-100 / D-102 (v0.8.4)

Before any device testing of §28's batch, the user caught a misreading of D-101 and requested a few more
refinements to D-100/D-102 while reviewing. All implemented in this round; no separate spec-then-wait cycle
since each point is small, self-contained, and directly approved in the same message.

### D-101 - Corrected: `(` and `)` Stay Two Separate Keys
The original D-101 implementation merged `(`/`)` into one key. That was a misunderstanding - the intent was
always **two** keys, each keeping its own D-01 popup for its own bracket family (opening vs. closing):
`OPEN_BRACKET_ALTERNATIVES = listOf("(", "{", "[", "<")`,
`CLOSE_BRACKET_ALTERNATIVES = listOf(")", "}", "]", ">")`. Page 1's row 1 goes back to 8 symbols (`()°√π~&|`).
The page 2 correction below applies the same treatment to the `(`/`)` that live in its shifted-symbols row.

### D-100 - Corrected: Page 1's `ABC` Slot Must Stay Reserved When Hidden, Not Collapse
D-93's established pattern - just omit a disabled key and let its row's remaining cells grow - is *wrong* for
page 1's `ABC` key specifically, because D-100 made every row's cell count part of a cross-row column/grid
alignment: omitting `ABC` narrows its row relative to its neighbours, breaking the grid. `SymbolLayout` now
always emits the `ABC` key on page 1 regardless of `symbolKeyEnabled`; `AdaptKeyboardView` hides it (drawing
nothing, taps inert) exactly the way it already treats the disabled combined `?123` key (D-59) - the shared
predicate was renamed `isHiddenKey` and gained a second condition: `KeyCode.LETTERS` on the `SYMBOLS` surface,
`symbolPage == 1`, `!symbolKeyEnabled`. Page 2's own (unrelated) `ABC` key keeps the D-93 omit-and-grow
behaviour unchanged - it isn't part of a column layout, so nothing there needed to change.

### D-100 - Also: Swapped `ABC` and `=`
While confirming the reserved-slot fix, the user asked to swap `ABC` and `=`: `=` now sits in the persistent
right-hand column (row 4, always visible, no gating - fitting, since a real calculator's `=` usually lives in
a prominent bottom-right position), and `ABC` moved into the digit grid instead, at row 5 under `3` (still
`symbolKeyEnabled`-gated via the reserved-slot mechanism above, just at a different position). Row 4 is now
`1 2 3 − =`; row 5 is `0 <decimal-sep> ABC + ⏎`.

### D-102 - Corrected/Extended: Bracket, Currency and Underscore Alts on Page 2
Three refinements to page 2, all additive (no structural change):
- The `(`/`)` in row 3 (the shifted-symbols row) get the same `OPEN_BRACKET_ALTERNATIVES` /
  `CLOSE_BRACKET_ALTERNATIVES` popups as page 1's bracket keys, per the D-101 correction above. This doesn't
  reduce their direct (short-tap) accessibility - row 3's whole point (D-102) - since the base character still
  commits on a plain tap; the popup is a purely additive long-press extra.
- Row 4's `€` (distributed from the letters page's `e` hint) also gets the common-currency popup - redundant
  with page 1's dedicated currency key, by explicit request. It sits on the *left* of the screen here, unlike
  page 1's, so its alternatives stay in the **un**-reversed order (`€ $ £ ¥`, base first) so the popup grows
  rightward, into the room that's actually there on this side.
- Row 4's `-` (distributed from the letters page's `m` hint) gets `_` as a single long-press alt.

---

## §30 - §29 Follow-Up: Bracket Order, Page-2 Reshuffle, New Symbols (v0.8.5)

Continued review of §29, before device testing.

### Bracket popup order - square before curly
`OPEN_BRACKET_ALTERNATIVES` / `CLOSE_BRACKET_ALTERNATIVES` (shared by page 1's `(`/`)` and page 2's `(`/`)`
in the shifted-symbols row) reordered to `( [ { <` / `) ] } >` - square brackets before curly braces.

### Page 2: dedicated underscore key dropped
Now redundant with `-`'s own `_` alt (added last round) - removed from row 1.

### Page 2: `+` to the end of row 4, `€` to the start of row 1
`+` moves to the last position of row 4 (the last content row); `€` moves out of row 4 entirely, to the
first position of row 1, keeping its common-currency popup.

### Page 2: apostrophe moved to row 4, second position
Was in row 1; now sits right after `#` in row 4 - which happens to put it roughly under row 3's own `"`,
directly above it.

### Page 2: `®` added next to `©`
The circled C and circled R sit together in row 1.

### Page 2 + letters page: `Ø` added ("Durchschnitt")
Added to page 2's row 1 (room was made by the removals above). Also added as a second long-press alternative
on the letters page's `o` key (`KeyboardLayout.O_ALTERNATIVES = listOf("ö", "Ø")`) - chosen as the host key
since Ø is visually a stylised O; gated the same reassignment-aware way as `p`'s Greek-letter popup, so a
user who has reassigned `o` via C-08 keeps their own plain hint. **Bug fixed while implementing this**: the
o key is in the *top* row (`qwertzuiop`), not the middle row (`asdfghjkl`) - an internal test caught a wrong
row index before this shipped.

### Letters page: `f` gets the function symbol, also added to page 2
`f` was still unassigned - `KeyboardLayout.DEFAULT_LETTER_HINTS` gained `f`→`ƒ` (also added to
`LetterHints.PALETTE`, keeping the C-08-editor-can-reproduce-every-default invariant). Also added to page
2's row 1.

### Calculator page: the π key gets its Greek-letter popup too
Previously plain (no `hint`, no `alternatives`) on page 1, unlike the letters page's `p`→π. Now reuses
`KeyboardLayout.PI_ALTERNATIVES` directly (exposed, was `private`) so both π keys stay in sync. The user
described an "obscure case-toggle function" firing instead of a proper popup on long-press - no separate
bug was found in the case/shift-handling code, so the working theory is that this was simply the absence of
a defined long-press action; giving the key `alternatives.size >= 2` routes it through the same D-01 popup
mechanism as every other multi-alternative key. Flagged as something to confirm is actually resolved once
next tested on a device, since the exact prior behaviour wasn't independently reproduced first.

### Calculator page: reordered the operator column - plus, minus, times, divide (v0.8.6)
The right-hand operator column (the 4th cell of rows 2-5, alongside the digit block) reordered top to bottom
from `÷ × − +` to `+ − × ÷`. Only the operator identity per row changed - the digits (`7 8 9` / `4 5 6` /
`1 2 3` / `0`) and each row's own right-hand-column cell (`space` / currency / `=` / `⏎`) stay exactly where
they were. Each operator keeps its own alt-popup wherever it now sits: `×` still offers `* × ·`, `÷` still
offers `/ ÷ :`. New row layout: row 2 `7 8 9 +`, row 3 `4 5 6 −`, row 4 `1 2 3 ×`, row 5
`0 <decimal-sep> ABC ÷`.

### Page 2: corrected the wrong symbol moved in the row-4 reshuffle (v0.8.7)
§30's "`+` moved to the end of row 4" was a misreading of the original ask - it was always meant to be `±`
(plus-minus), not the plain `+`. Corrected: `+` moves back to its original spot in row 4 (right after `-`,
before `°`); `±` moves out of row 1 to the very end of row 4 instead. Row 1 is now `€ @ • © ® Ø ƒ` (seven,
not eight); row 4 is now `# ' - + ° × ÷ * ±` (nine, not eight).

### Page 2: `ABC`'s slot next to `space` also reserved when hidden (v0.8.8)
Page 2's bottom row previously used D-93's original omit-and-grow pattern for its `ABC` key: hidden entirely
when the combined `?123` key is off, letting `space` grow to fill the gap. Per request, this now matches
page 1's D-100-corrected treatment instead - `ABC`'s slot stays reserved (drawn as nothing, tap inert) rather
than collapsing, so `space` stays its normal size instead of looking oversized when `ABC` is hidden.

Implementation: `SymbolLayout` no longer takes a `symbolKeyEnabled` parameter at all - both pages'
`ABC` key is now unconditionally present in the returned rows (page 1 already worked this way since the
D-100 correction; page 2's row 5 now does too). All of the visibility/interactivity logic lives in
`AdaptKeyboardView.isHiddenKey()`, whose `KeyCode.LETTERS` condition dropped its `symbolPage == 1`
restriction - it now applies uniformly to whichever symbol page is showing. Since `SymbolLayout` no longer
needs the setting, `AdaptKeyboardView.rowsFor()`'s call into it dropped the argument too.

---

## §31 - Case-Toggle Bug Fix, Page-2 Reorders, Phone-Field Auto-Select (v0.8.9)

### Page 2 row 1: bullet moved ahead of `@`
`CATCHALL_ROW1_SYMBOLS` reordered from `€ @ • © ® Ø ƒ` to `€ • @ © ® Ø ƒ`.

### Page 2 row 4: `°` moved ahead of `+`, `^` inserted at position 3
So the arithmetic operators (`- + × ÷`) aren't split apart by `°` sitting between them, and to fill the row
out to 10 keys - matching rows 2 and 3's width, since they now all have the same key count.
`CATCHALL_LETTER_HINTS` reordered/extended from `# ' - + ° × ÷ * ±` to `# ' ^ - ° + × ÷ * ±`.

### Bug fixed: symbol-page character keys were getting auto-capitalised
The calculator page's `π` key always produced `Π`; the same happened to page 2's `ƒ` key. Root cause,
confirmed by investigation: this is **not** a per-key case-toggle mechanic - it's the ordinary
auto-capitalise-at-sentence/field-start feature (`armShiftForNextWord`) arming `shifted = true`
automatically (no user Shift tap involved), combined with two independent, insufficiently-guarded
`uppercaseChar()` call sites that only checked `Char.isLetter()` - true for `π` and `ƒ` because they are
genuine Unicode letters (Greek `Ll`, Latin `Ll` respectively), even though on the `SYMBOLS` surface they
represent a constant and a symbol, not alphabetic text:
- **Commit path** (`AdaptKeyService.kt`, `handleKey()`'s `KeyCode.CHAR` branch): `raw.isLetter()` decided
  both whether the character continues a composing "word" (`extendsToken`) and whether it gets
  uppercased. Fixed by introducing `isWordLetter = raw.isLetter() && surface == InputSurface.LETTERS` and
  using that instead of the bare `isLetter()` check in both places. This also fixes a related, distinct gap:
  `π` was being treated as continuing a composing word (subject to autocorrect/suggestions) rather than
  committing immediately like the calculator page's other symbols (`°`, `√`, `~`, `&`, `|`).
- **Display-only path** (`AdaptKeyboardView.labelFor()`): had no `isLetter()` guard at all, just
  `shifted || capsLock`. Fixed by adding both `surface == InputSurface.LETTERS` and `ch.isLetter()` to the
  condition.

The fix is surface-based, not character-based - it doesn't special-case `π`/`ƒ`, so it also covers any future
symbol/calculator-page character that happens to have a Unicode case mapping. The letters-page `f`→`ƒ`
long-press hint is untouched - case genuinely applies there, same as every other letter hint (`a`→`ä` etc.).
No new test - this is `AdaptKeyService`/`AdaptKeyboardView` glue, an existing documented testing gap.

### Calculator page: `0` key gets a `#` long-press hint
For fields marked as a phone number (dial pads commonly need `#`/`*`).

### New: fields wanting digits auto-open the calculator page
`AdaptKeyService.onStartInput()` previously reset every field unconditionally to `InputSurface.LETTERS`. A
new `initialSurfaceFor(info: EditorInfo?)` inspects `info.inputType and InputType.TYPE_MASK_CLASS` and opens
straight to the calculator page (`InputSurface.SYMBOLS`, page 1) for `TYPE_CLASS_PHONE`, `TYPE_CLASS_NUMBER`,
and `TYPE_CLASS_DATETIME` fields - the three non-text input classes, all of which primarily want digits and
gain nothing from the full alphabetic keyboard, mirroring how stock Android/Gboard show a numeric-style
keyboard for these. `TYPE_CLASS_TEXT` (and no `EditorInfo` at all) still opens on `LETTERS` as before. No new
test - `onStartInput` is service-glue, an existing documented gap; the same pattern already applies to
`capsModeFor()` right next to it.

### Implemented: sign-flip on long-press of the calculator's minus key (v0.8.10)
Originally raised tentatively in §31, then explicitly confirmed and requested. Long-pressing `−` on the
calculator page now flips the sign of the number immediately before the caret (removes a leading
[`SymbolLayout.MINUS_SIGN`] if present, otherwise inserts one) - operating directly on the *committed* text
via `InputConnection`, not composing state, since digits on the calculator page commit immediately rather
than entering a composing token (confirmed during investigation: with `composing` always empty there, this
is the only viable approach).

The minus key carries neither a [Key.hint] nor [Key.alternatives] - unlike every other long-press secondary,
this is an *edit* action, not "commit this text", so routing it through the existing
hint/alternatives-commit pipeline (`commitLongPressSymbol`) would be wrong (it would literally type a `±`
character rather than editing the number). Instead: `SymbolLayout.MINUS_SIGN` (`'−'`, U+2212, not the ASCII
hyphen) is now a public constant; `KeyboardLayout.hasLongPressAction()` gained a dedicated case recognising
this specific key (mirroring how it already special-cases the combined `?123` key); `AdaptKeyService.
handleLongPress()` intercepts it before the generic hint-reading branch and calls a new
`flipSignBeforeCaret()`. The actual text-scanning logic lives in a new pure, unit-tested `SignFlip` object
(`keyboard` package, mirroring the existing `WordBoundary`/`SentenceBoundary` pattern) - `resultFor(before:
CharSequence)` scans back over digits and `,`/`.` (either decimal-separator character, since only the
number's *extent* matters here) to find where the number starts, and returns a `(deleteLength, insertText)`
edit, or null when there's no number directly before the caret. One accepted trade-off, noted rather than
worked around: since the key carries no hint, it gets no corner-glyph visual cue that long-press does
something - acceptable for a power-user bonus gesture the user already knows exists, and forcing one through
the existing hint-is-commit-text machinery would have caused the exact bug this design avoids.

## §32 - D-87 Root-Cause Fixes: Mid-Word Correction (v0.8.11)

Investigated the report that D-62 mid-word live correction "still doesn't activate", with three symptoms:
no suggestions while typing mid-word, the caret jumping to word/sentence end, and whole words being
swallowed. Found and fixed two independent bugs in the same code path, both around `reclaimSurroundingWord()`
(the D-62 mechanism that pulls a word's letters on both sides of the caret into `composing` when a new
composing token starts mid-word). No Robolectric integration test was built for either fix - a realistic
fake `InputConnection` that reproduces windowed `ExtractedText` and batch-edit callback timing is out of
scope for this environment (see below); both fixes were verified by careful manual trace against the
`InputConnection`/`ExtractedText` API contract, plus new pure unit tests for the extracted logic they touch.
**Neither fix has been confirmed on a real device yet** - this environment has no Android emulator (no
hardware virtualization available) and Robolectric cannot exercise real IPC timing.

### Bug 1: `ExtractedText.selectionStart` used as an absolute position
`reclaimSurroundingWord()` computed the mid-word composing anchor from
`ic.getExtractedText(ExtractedTextRequest(), 0)?.selectionStart` directly. Per the `ExtractedText` API
contract, `selectionStart` is relative to the *extracted chunk* (`ExtractedText.text`), not the whole
document - `ExtractedText.startOffset` must be added to get an absolute document position. Using
`selectionStart` alone happens to work in short/simple fields, where `startOffset` is always 0, but silently
miscomputes the anchor - and with it every later "is this our own edit" check in `onUpdateSelection` - in any
field long or paginated enough for the framework to window the extraction instead of returning the whole
document. This explains why the bug was previously reported as intermittent rather than reliably
reproducible (§27/§28's D-84 fix addressed a real, different bug - a `tokenContextBefore` duplication that
silenced suggestions entirely - but did not touch this anchor computation).

Fixed by extracting the arithmetic into a new pure, unit-tested `ComposingAnchor.resolve(extractedStartOffset,
extractedSelectionStart, reclaimedBeforeLength)` object (top-level package, mirroring `WordBoundary`/
`SentenceBoundary`), and using it in `reclaimSurroundingWord()`. Confirmed via `grep` this was the only
occurrence of the `getExtractedText()...selectionStart` pattern in `AdaptKeyService.kt`.

### Bug 2: the reclaim's `deleteSurroundingText()` was not batched with the composing update
`reclaimSurroundingWord()` calls `ic.deleteSurroundingText()` directly (to pull the reclaimed letters out of
the real, committed text) as a standalone `InputConnection` call, *before* `insertComposingChar()` and
`updateComposing()` run - and `updateComposing()`'s own `beginBatchEdit()`/`endBatchEdit()` wraps only its
own `setComposingText()`/`setSelection()` pair, not the earlier delete. Since `InputConnection` calls are
typically dispatched asynchronously to the app and `onUpdateSelection` callbacks arrive back on a later
message-loop turn, this unbatched delete can produce its own, independent `onUpdateSelection` callback that
arrives *after* `composing`/`composingAnchor`/`composingCursor` have already advanced past it (both the
delete and the later composing update happen synchronously within the same `handleKey()` call, well before
either callback fires) - reporting a stale, delete-time-only cursor position that no longer matches
`onUpdateSelection`'s `ownCursor` expectation. The mismatch trips the "not our edit" branch, which calls
`finishComposingText()` and `clearComposing()` - wiping the token this very keystroke was building - even
though nothing the user did caused it. Unlike Bug 1, this is timing-dependent rather than field-length
dependent, so it plausibly explains a more reliably-reproducible version of all three symptoms: suggestions
that flash and vanish (or never appear) as the wipe races the suggestion refresh, the caret landing wherever
the stale callback's cursor position happened to be (which reads as "jumping to word/sentence end"), and
letters already reclaimed into `composing` disappearing along with it (reads as "words being swallowed").

Fixed by wrapping the whole reclaim-through-`updateComposing()` sequence in one outer
`beginBatchEdit()`/`endBatchEdit()` at both call sites that invoke `reclaimSurroundingWord()` - the ordinary
character path in `handleKey()`'s `KeyCode.CHAR` branch, and the long-press-letter path in
`appendLongPressLetter()`. Nesting batch edits is an explicitly supported Android pattern (the app only
dispatches once the outermost `endBatchEdit()` completes), so this composes safely with `updateComposing()`'s
own conditional inner batch. The app now coalesces the delete and the composing update into a single
transaction and reports only the final, consistent selection - eliminating the intermediate stale callback
entirely.

## §33 - Page 2 Row 4: `°`/`-` Swap, Phone-Field Auto-Select Confirmed (v0.8.12)

### Page 2 row 4: `°` moved ahead of `-`
`CATCHALL_LETTER_HINTS` reordered from `# ' ^ - ° + × ÷ * ±` to `# ' ^ ° - + × ÷ * ±`. `-` keeps its `_`
long-press alt regardless of position.

### Confirmed: phone-number (and similar) fields already auto-select the calculator page
Answering the question raised alongside this round's request: yes, and it was already implemented - §31
(v0.8.9) added `AdaptKeyService.initialSurfaceFor(info: EditorInfo?)`, called from `onStartInput()`, which
inspects `info.inputType and InputType.TYPE_MASK_CLASS` and opens straight to the calculator page
(`InputSurface.SYMBOLS`, page 1) for `TYPE_CLASS_PHONE`, `TYPE_CLASS_NUMBER`, and `TYPE_CLASS_DATETIME`
fields - covering phone-number fields and any other field an app marks as one of these non-text classes.
`TYPE_CLASS_TEXT` (and fields with no `EditorInfo` at all) still open on `LETTERS` as before. No action
needed this round; nothing changed.

## §34 - Letters-Page `p` Key: Reversed Alternatives Order (v0.8.13)

### Bug fixed: the `p` key's Greek-letter popup grew away from `π`, not towards it
`p` is the last key of the letters page's top row (`qwertzuiop`), hard against the keyboard's right edge.
`AdaptKeyboardView.openPopup()`/`HorizontalLongPressPopup.rowLeft()` centre the popup row over the pressed
key's stem, then clamp the whole row to stay on screen - for an edge key like `p`, the ideal (centred)
position always overflows off-screen, so the row gets clamped and visually grows leftward instead. Since the
row is always drawn in `Key.alternatives` list order (left to right, unconditionally), and `π` sat first in
`KeyboardLayout.PI_ALTERNATIVES` (`π α β γ δ λ ω`), clamping put `π` itself - the character actually printed
on the key - at the far end of the row, away from the finger, with the least-relevant alternative (`ω`)
right next to it.

Fixed by reversing the order specifically at the letters-page call site (`topRowKey()`:
`alternatives = PI_ALTERNATIVES.reversed()`), not the shared `PI_ALTERNATIVES` constant itself - the
calculator page's `π` key ([SymbolLayout], `CALC_ROW1_SYMBOLS = "()°√π~&|"`) sits roughly mid-row there, not
against an edge, so it doesn't have this problem and was left untouched. Pre-selection (which cell is
highlighted the instant the popup opens) is unaffected either way - `AdaptKeyboardView.preSelectedIndexFor()`
looks up the key's own character's *current* index in whatever list it's given, so it finds `π` correctly
regardless of order.

### Design question: could the layout itself sort outward from the pivot automatically?
Raised alongside this fix: instead of manually reversing an alternatives list per key based on where it
happens to land on screen, could `AdaptKeyboardView`'s popup layer sort/display outward from the anchor
automatically, so authors never have to think about screen position when writing a `Key.alternatives` list?

In principle yes: the row is already clamped for edge overflow (`rowLeft()`), so the same code already knows
whether a row got pushed away from its ideal (unclamped) centred position, and in which direction. The
missing piece is a de-coupling between *logical* order (as authored - "the base character first, then
alternatives in relevance order") and *visual* order (as drawn) that the view could resolve by mirroring the
list whenever the row is clamped opposite its usual growth direction. This would remove one recurring class
of authoring mistake (an edge key's alternatives silently facing the wrong way) at the cost of a real design
change: `drawLongPressPopup()`/`updatePopupSelection()`/`commitPopupSelection()` all currently index directly
into `popupAlternatives` in stored order, so "visual order" and "list order" would need to diverge somewhere
in that pipeline, and every existing edge-key alternatives list (there is currently exactly one, `p`) would
need re-auditing once the auto-mirroring lands, since the manual reversal done here would then double-flip.
**Not implemented this round** - worth doing if a second or third edge-anchored multi-alternative key shows
up and this manual-reversal-per-key pattern starts repeating; for a single instance, the explicit
`.reversed()` at the one call site that needs it (with the comment explaining why) is the smaller, more
inspectable change.

## §35 - Bug Fixed: Greek Math-Symbol Popup Got Auto-Capitalised (v0.8.14)

### Bug fixed: picking π/α/β/γ/δ/λ/ω from the `p` key's popup could insert an upper-case letter
Reported: inserting a Greek letter as a math symbol (not while in Greek input mode) at, say, the start of a
line produced the upper-case form (`Π`) even though the key itself clearly shows the lower-case glyph - and
for a mathematical constant/variable, case is meaningful (`Π` and `π` are different symbols), unlike an
ordinary word where auto-capitalising the first letter is invisible/correct.

Root cause: `AdaptKeyService.commitLongPressSymbol()` decided whether a picked long-press alternative should
extend the composing word (and so become subject to §6 capitalisation / Shift-arming, via
`appendLongPressLetter()`) purely by `symbol.all { it.isLetter() }`. That check was written for G-01 - a
Greek accented vowel picked while genuinely typing Greek text, where extending the word and following normal
case rules is exactly right - but the same `isLetter()` test is equally true for π/α/β/γ/δ/λ/ω, D-99's
Greek-letter popup added to the **Latin** keyboard's `p` key as a shortcut for mathematical symbols. Nothing
in the check distinguished "genuine language letter" from "a letter borrowed from a different script purely
as a symbol".

Fixed with a new pure, unit-tested `AlternativeScript.extendsWord(symbol, activeLanguageIsGreek)` (`keyboard`
package): true only when every character is a letter *and* either the keyboard is actually in Greek mode
(§28's `activeLanguage == Language.GREEK`, only ever true while `GreekLayout` - not `KeyboardLayout` - is on
screen, so this can never misfire the other way), or the letter is not itself Greek script
(`Character.UnicodeScript.of(...) == GREEK`, via the JDK's own Unicode script table - no hand-maintained
character list to keep in sync). `commitLongPressSymbol()` now calls this instead of the bare `isLetter()`
check. A German umlaut (`ä`, `ö`, `ü`) is unaffected either way - it is Latin script, so the Greek-script
exclusion never applies to it, and it keeps extending the word with normal case behaviour as before.

When the check now returns false, the symbol routes through `finalizeAndCommit()` instead - the same path
plain punctuation and the calculator page's non-letter symbols already use. This finalises whatever word was
in progress first (autocorrect/capitalisation apply to *that* normally), then commits the Greek symbol via
`ic.commitText(delimiter, 1)` - which every branch of `finalizeAndCommit()` already does completely verbatim,
with no case transform ever applied to the delimiter argument itself. This also fixes a second, related gap
noted only in passing here: previously such a symbol would have been folded into the *same* composing token
as whatever was typed around it (subject to autocorrect/suggestions as if it were part of a word); now it
commits standalone, matching how the calculator page's own π key (§31, direct tap) already behaves.

## §36 - Calculator Row 5: `0` Centred Under `2` (v0.8.15)

### Row 5 rearranged: `ABC` narrow under `1`, `0` centred (widened), decimal separator narrow under `3`
Raised via a friend's suggestion that `0` belongs centred under `2`, matching a phone dial pad / physical
numpad. Previous order was `0` (under `1`) `,` (under `2`) `ABC` (under `3`); now `ABC` `0` `,`, with `ABC`
and the decimal separator both narrowed to the same weight and `0` widened to fill the space between them.

This is a pure [Key.weight] change, not a structural one - [AdaptKeyboardView] already lays out each row as a
plain proportional (flexbox-style) split (`keyWidth = unit * key.weight`, `unit = usableWidth / totalWeight`
per row), so no per-key pixel math or hand-hacked positioning was needed. The centring itself rests on one
fact worth recording: the sum of two *equal* flanking weights always lands the cell between them on the
row's own centre, regardless of what that shared weight actually is - so the exact value chosen
(`ABC_DECIMAL_WEIGHT = 0.5f`, `SymbolLayout`) is somewhat arbitrary and could be tuned freely later without
re-deriving the centring; only the *equality* of the two flanking weights matters. `0`'s own weight is
computed as `CALC_DIGIT_COLUMN_WEIGHT - 2 * ABC_DECIMAL_WEIGHT` (`3 - 2*0.5 = 2`) rather than hardcoded, so
changing `ABC_DECIMAL_WEIGHT` alone keeps the row's total consistent with rows 2-4's three digit/operator
cells (still 3, so the right-hand column stays aligned) without a second edit. `ABC`'s existing reserved-slot
hiding (D-59/D-93/§29-follow-up: drawn as empty space, inert, when the combined `?123` key setting is off)
is unaffected by its new narrower weight - that mechanism only ever cared about the key's own slot staying
present, never its width.

## §37 - Row 5 Retuned: `0` Less Oversized (v0.8.16)

§36's `ABC_DECIMAL_WEIGHT = 0.5f` made `0` twice as wide as an ordinary digit key (weight `2f` against their
`1f`) - reported as visibly too large. Retuned to `0.75f`, giving `0` a more modest `1.5f` (1.5x) width
instead. As noted in §36, only one constant needed changing - `0`'s own weight is derived
(`CALC_DIGIT_COLUMN_WEIGHT - 2 * ABC_DECIMAL_WEIGHT`), and the centring under `2` holds for any value as long
as `ABC` and the decimal separator stay equal to each other, which they still do.

## §38 - D-36 Clipboard Paste Reworked: Native Paste Action, Sensitive-Only Auto-Clear (v0.8.17)

### Reported: the D-36 paste chip's `commitText()` approach loses app-specific paste behaviour
Pasting a multi-line list into Google Keep's note editor via the D-36 direct-paste chip produced a single
multi-line entry; pasting the same text via the field's own long-press "Paste" produced one list entry per
line. Investigated and confirmed: this is a real difference in *how* text arrives at the target app, not a
Keep bug. `InputConnection.commitText()` (D-36/D-60's approach since it landed) just inserts characters -
indistinguishable, from the target app's point of view, from someone typing very fast including newlines.
Keep's per-line list-entry splitting is almost certainly wired to its own native paste-handling code path
(the one `InputConnection.performContextMenuAction(android.R.id.paste)` would trigger, since that asks the
target editor to run its own "Paste" context-menu action - the same thing a long-press "Paste" runs), not to
"multi-line text arrived from somewhere." A related ask - pasting into a Keep list-note's *title* field
splitting the first line into the title and the rest into list entries - is confirmed **not achievable from
the keyboard side at all**: an IME's entire view of an app is "there is a text field, I can commit text into
it" - it has no channel to know an app maintains multiple separate UI elements (title + individually-rendered
list rows) a paste should be distributed across. That would need to be built into Keep's own paste handler.

### Reverted to `performContextMenuAction(paste)`, with the D-36/D-60 concerns addressed differently
D-36/D-60 originally moved *away* from `performContextMenuAction(paste)` for two reasons, both still true as
general facts, addressed (or knowingly accepted) differently now:

1. **"Not honoured by every field"** - in some fields/apps, the call silently does nothing. **Not fixed** -
   there is no reliable way for an IME to detect after the fact whether a native paste actually happened, so
   there is no way to build an automatic fallback to `commitText()`. Accepted as a known, open risk in
   exchange for the Notes-app benefit and (below) no longer wiping ordinary clipboard content; the user will
   report back if it causes trouble in practice.
2. **"Races the immediate `clearClipboard()`"** - the old code cleared the clipboard synchronously right
   after firing the paste action, but `performContextMenuAction()` triggers the target app's *own*
   asynchronous paste handling, which often had not actually read the clipboard yet by the time it was
   wiped - the original reason paste appeared to silently fail in several apps. **Fixed**: the clear is now
   deferred `CLIPBOARD_CLEAR_DELAY_MS` (300 ms) via the service's existing `Handler` (the same
   `postDelayed()` pattern already used for `resortRunnable`) instead of firing synchronously -
   `scheduleClipboardClear()`. 300 ms is generous relative to how quickly a target app's UI thread actually
   processes a plain-text paste (typically single-digit milliseconds), while still keeping a sensitive
   value's exposure window short. Before clearing, the callback re-reads the current clipboard text and
   compares it against what was pasted - if it no longer matches (the user copied something else in the
   meantime), the clear is skipped rather than silently wiping unrelated newer content.

### Also requested and implemented: only ever auto-clear *sensitive* content
Previously every paste cleared the clipboard unconditionally, "so pasted content - especially a password -
does not linger" (the original D-36 rationale) - which also discarded ordinary, harmless clipboard content
the user might have wanted to paste again elsewhere. Now `clearClipboard()` (via `scheduleClipboardClear()`)
only ever runs when the clip is flagged sensitive.

**How "sensitive" is determined** (a question raised alongside this fix): via the clipboard's own
[ClipDescription.EXTRA_IS_SENSITIVE] flag (Android 13/Tiramisu+), set by whichever app *copied* the content
(a password manager or browser flags it when the source field was a password field) - not by inspecting the
clipboard text's own content for password-like patterns, and not by the *current* paste target field's
`inputType`. There is no reliable signal at all pre-Tiramisu, or from a copying app that does not set the
flag. This is almost certainly also how GBoard decides to show a run of bullets instead of a text preview for
clipboard content - the masking has to happen the moment the clip preview is offered, before the user has
even chosen a target field, so it cannot be based on where the text will land. AdaptKey already read this
same flag for the D-36 chip's own bullet-masked preview ([ClipboardPreview]); the check is now factored into
a shared `isSensitiveClip()` used by both the preview and the new auto-clear decision.

## §39-§42 - Batch: Comma Popup, Clipboard Age, Selection Backspace, Gesture-Area Background (v0.8.18)

Four independent, small, low-risk fixes from the same feedback round, batched into one version bump/commit
given their size.

### §39 - Letters page: `/` dropped from the comma key's popup
`KeyboardLayout.COMMA_ALTERNATIVES` was `- , : ; / _`; `/` is redundant now that it also lives on the `7`
key's long-press hint (D-96), so it is dropped: `- , : ; _`.

### §40 - D-36 paste chip no longer offered once the clip is older than 5 minutes
A stale clipboard entry (copied long ago, likely forgotten about) should not keep resurfacing as a paste
suggestion every time a field opens. `ClipDescription.getTimestamp()` (`System.currentTimeMillis()` time
base, available since API 26 - AdaptKey's own minSdk, so no version gate is needed, unlike §38's
Tiramisu-only `EXTRA_IS_SENSITIVE`) gives the clip's creation time. New pure, unit-tested
`ClipboardPreview.isFresh(clipTimestampMs, nowMs)` (`MAX_AGE_MS = 5 * 60 * 1000L`), checked in
`showClipboardChipIfAvailable()` before anything else - a stale clip now shows no chip at all, exactly as if
the clipboard were empty.

### §41 - Bug fixed: Backspace on a text selection deleted a character instead of the selection
Selecting text (long-press + drag) and pressing Backspace deleted the single character before the cursor,
not the selection itself, unlike every other text editor. `handleBackspace()` never checked for an active,
non-collapsed selection at all - `deleteOneBefore()`'s unconditional `getTextBeforeCursor(1, 0)` /
`deleteSurroundingText(1, 0)` doesn't know or care whether a selection exists elsewhere in the field. Fixed
by checking `InputConnection.getSelectedText(0)` first, before either of `handleBackspace()`'s existing
branches (composing vs. not): a non-empty result means a real selection exists, which is replaced via
`commitText("", 1)` - the standard InputConnection idiom for "replace the current selection with nothing",
exactly what any ordinary keypress does to a selection. Any in-progress composing token is finished and
cleared first, since it cannot correspond to the (out-of-band) selected range. No new test - `InputConnection`
selection state is service glue, an existing documented gap.

### §42 - Fixed: the system-gesture-inset area below the keyboard showed whatever was behind the IME window
`AdaptKeyboardView` already paints its own background (`R.color.keyboard_background`) within its own bounds,
but the bottom padding added to `root` (the outer `LinearLayout`) to clear the Android 15 edge-to-edge
gesture-navigation pill sits *outside* every child's bounds - `root` itself had no background, so that strip
let whatever sat behind the IME window show through, looking wrong against the keyboard immediately above it.
Fixed with one line: `root.setBackgroundColor(R.color.keyboard_background)`, matching the keyboard's own.

## §43-§44 - Two Systemic Autocorrect Priority Bugs (v0.8.19)

Two concrete, reported failures ("Hallo" → "Hall" + apparent English-mode switch; "die"/"der" losing to
"due"/"ddr") were investigated and confirmed to share the session's stated strategy: both traced to a real,
generalisable rule defect, not a word-specific quirk - fixed at the rule level, verified against the actual
bundled dictionary/profile assets (not guessed), with regression tests capturing the mechanism rather than
the individual reported words.

### §43 - Bug fixed: a single common word could misclassify as a foreign language
Root cause, confirmed empirically against the bundled `language_profiles.tsv`: `LanguageClassifier.isForeign()`
/`classify()` only gated on raw n-gram *count* (`minNgrams = 8`) before attempting a Cavnar-Trenkle
out-of-place language decision - but a single isolated word's own n-gram sample is too statistically noisy to
trust for this comparison, *regardless of how many n-grams that one word happens to produce*. Empirical sweep
over 27 common German words (single word, no context) found roughly 30% misclassified as foreign: `Bitte` →
Italian, `heute` → Dutch, `Auto` → Italian, `Straße` → Italian, `werden` → Dutch, `trinkst` → English (this
one has 15 n-grams, nearly double the minimum), and `Hallo` → English (confidence only 0.20) - reproducing
the reported bug exactly, including the second-order symptom: once `resolveDict()` re-points `provider`/
`capitalisation`/`dictionaryStore` to English for the token, German capitalisation rules and suggestions stop
applying for it, which reads as "the keyboard switched to English mode" even though the persistent
letters/Greek toggle (`activeLanguage`) never actually changes - there is no third, persisted "English mode".
Every one of the misfiring words classified correctly the moment a second word joined it in the input.

Fixed with a new `minWords` constructor parameter (default 2, independent of `minNgrams`) on
`LanguageClassifier`, checked in both `classify()` and `isForeign()` right after the (unaffected) Greek
script fast-path - a lone word, however long, never reaches the Latin-script ranking at all. 2 new unit tests
demonstrate the mechanism directly: a single word that would unambiguously win a toy English profile
(`"over"`, literally present in the test's training text) is blocked with the default `minWords = 2`, and
correctly wins once paired with a second word.

### §44 - Bug fixed: A-01's "known word" protection had no sense of proportion
Root cause, confirmed against the bundled `dict_de.tsv`: `die` (frequency 889,897) and `der` (1,004,234) are
the two most common words in the corpus, yet autocorrect refused to ever propose them once the composing
token happened to land on `due` (frequency 24 - a rare loanword entry) or `ddr` (4,405 - the GDR
abbreviation) - both of which are themselves real, if obscure, dictionary entries. `DictionarySuggestionProvider.
bestCorrection()`'s A-01 guard (`if (isKnownWord(token)) return null`) is an absolute veto with no concept of
*how* known the word is - a coincidental 24-frequency loanword got exactly the same ironclad protection as a
genuinely common word, permanently shielding it from a single-substitution neighbour nearly 40,000x more
frequent. `u`/`i` are QWERTZ-adjacent keys, so a `due`-for-`die` mistap is a completely ordinary slip this
system should already handle - A-01 was actively preventing it.

Separately confirmed: this is also why the user never noticed `AdaptKeyService.rawCoordinateCorrection()`
(D-39/T-02/T-03) engaging for these cases - it carried its own, independent copy of the exact same absolute
"known word" veto, so it was blocked for precisely the same reason, for precisely the same words. (The
underlying raw-coordinate machinery itself is fine: `OffsetModel` trains continuously from ordinary typing via
`AdaptKeyboardView`'s `offsetModel?.record(...)` on every resolved tap, not only from the K-01 calibration
screen, and clears its warm-up threshold - 20 taps - almost immediately in normal use.)

Fixed by making the veto *relative* instead of absolute: a new `SuggestionProvider.shouldOverrideKnownWord
(word, candidate)` (default `false`, so `StubSuggestionProvider` is unaffected) returns true only when
`candidate` is at least `KNOWN_WORD_OVERRIDE_RATIO` (50x) more frequent than `word` -
`DictionarySuggestionProvider` implements it from the store's real frequencies; `bestCorrection()` and
`rawCoordinateCorrection()` both now consult this single, shared rule instead of each carrying their own
copy of the old absolute check, so the two paths can no longer silently disagree. The ratio is deliberately
extreme - verified against a "Bad" (100) vs. "Bat" (200) pair (only 2x apart) that an ordinary, comparably-
common word pair stays fully protected by A-01 exactly as before; only a genuinely lopsided, near-certain
"this was a coincidence" gap like `due`/`die` (~37,000x) or `ddr`/`der` (~228x) crosses the threshold.

## §45 - Bug Fixed: A-05 Word Splitting Could Chop a Word into Meaningless Fragments (v0.8.20)

### Reported: "meinst" (a normal German verb form) split into "mei" + "St"
Described as a general problem, not a one-off - "die Worttrennung muss wohl erst mit einer höheren Confidence
ausgeführt werden" (word splitting needs a higher confidence bar). Investigated and confirmed systemic, with
a precise, evidence-based root cause rather than a guess.

`TokenRepair.trySplit()` tries two independent strategies - dropping a single "hit letter instead of space"
character (`OVER_SPACE_LETTERS = c/v/b/n/m`, or a T-05-flagged tap), or inserting a space with nothing
dropped - and picks whichever valid candidate scores highest. The missed-space strategy already required the
two halves to be a real, co-occurring bigram (`MIN_SPLIT_BIGRAM`), specifically to stop a typo being cut into
two coincidentally-real dictionary fragments. **The drop-a-character strategy had no such requirement at
all** - only that both halves individually be *some* known dictionary word, however obscure.

`meinst` (m-e-i-n-s-t) has `n` at index 3, an over-space letter - dropping it yields `mei` + `st`. Verified
against the bundled `dict_de.tsv`: `mei` is a real but rare entry (frequency 16 - a proper noun / dialect
word), `st` a common abbreviation (5,939, "Sankt"). Both pass the "is this a known word" check individually,
so the drop candidate was accepted outright - and verified against `bigram_de.tsv`: `mei`+`st` has **zero**
recorded co-occurrences. (`mein`+`st`, the plausible "correct" split, also has zero - `meinst` simply isn't
covered by this dictionary's verb-conjugation entries at all, so the right outcome is no split, not a
different split.) This generalises well beyond the one reported word: any token containing `c`/`v`/`b`/`n`/`m`
whose two sides happen to both exist somewhere in the dictionary - true of an enormous number of ordinary
German words, given how many contain those letters - was vulnerable to being cut apart on zero co-occurrence
evidence, exactly the "höhere Confidence" gap the user diagnosed.

Fixed by applying the same bigram-co-occurrence filter to *both* strategies' candidates uniformly, instead of
only the missed-space one, before picking the highest-scoring survivor - removing the special case rather
than patching around the one reported word. Confirmed the fix doesn't regress the strategy's own motivating
case (`und<c>das` -> `und` + `das`, a real, extremely common bigram - 10,165 occurrences in the bundled
corpus) or the existing `immernoch` -> `immer noch` comparison test (the weaker `immer`+`och` drop candidate
now gets filtered out outright rather than merely losing a score comparison - same outcome, tighter
mechanism). New regression test reproduces the exact reported failure (`mei`/`st` known but never
co-occurring) rather than encoding the specific word as a special case.

## §46 - Mid-Word Live Correction: Third Investigation Pass, No Further Code Defect Found (v0.8.21)

Reported: mid-word live correction (D-62/D-87) "just doesn't work at all" even after §32's two D-87 fixes
this session (the `ExtractedText.startOffset` anchor bug and the unbatched-`deleteSurroundingText` race).
Asked to test intensively.

Re-audited the entire path end to end a third time - `WordExtent.reclaim()`, `reclaimSurroundingWord()`,
`insertComposingChar()`, `updateComposing()` (both its conditional inner batch and the outer batch §32 added),
`deleteComposingChar()`'s two branches (mid-composing and pulling in a real character before the reclaim),
and `onUpdateSelection()`'s `ownEdit` check - specifically looking for a *third* defect in the same family.
Found none: every read (`getTextBeforeCursor`/`getTextAfterCursor`) inside a batch edit is fine (batching only
governs coalesced writes, per the `InputConnection` contract, not read semantics); every write that changes
`composingAnchor` is either accompanied by a matching `InputConnection` call in the same batch or doesn't need
one; the single-operation batches in `deleteComposingChar()`'s `composingCursor == 0` branch are self-
consistent (the anchor adjustment and the one `deleteSurroundingText()` call move the cursor by the same
amount, so no callback can observe a mismatched state).

**Could not build a genuine automated regression test for this area.** Checked directly: Robolectric 4.14.1's
`shadows-framework` jar (inspected via `unzip -l`) ships a shadow for `InputMethodManager` only - there is no
shadow for `InputMethodService` (the class `AdaptKeyService` extends) or for `BaseInputConnection`/
`InputConnection` at all. Without either, there is no way to exercise the real, timing-sensitive callback
sequence (`beginBatchEdit`/`endBatchEdit`/`onUpdateSelection` ordering) that the two already-fixed D-87 bugs
specifically hinged on - a hand-rolled fake `InputConnection` would not reproduce the actual async Binder
callback timing that caused those bugs in the first place, so it would only provide false confidence, not
real verification. This is a genuine tooling gap in this environment (no Android emulator either - see prior
entries), not a shortcut.

**Not yet resolved; needs real-device confirmation to make further progress.** If the symptom persists on the
latest build, the most useful next report would specify: which of the three original symptoms still
reproduces (no suggestions / caret jump / word swallowed - possibly not all three anymore), and which app/
field it was tested in (a hint at whether `ExtractedText` windowing, i.e. non-zero `startOffset`, is actually
involved for that specific field, which the first D-87 fix targeted).

## §47 - Backlog: Live Colour Preview of a Pending A-05 Split

Requested feature, not implemented: a token that is about to be A-05-split when finalised should already show
that outcome live, while still composing - both resulting word-parts (minus whatever character would be
dropped as the mistaken "separator") coloured in the existing recognised-word green (the C-04/S-05 highlight,
setting-gated), exactly as a single whole recognised word already gets coloured before it autocorrects
(`shouldHighlightComposing()`). So a user typing a token that will end up split sees the split coming, not
just the final result after committing.

Not a small tweak - two real gaps to close:

1. **Split detection currently only runs at commit time.** `TokenRepair.trySplit()` is called from
   `finalizeAndCommit()`, once, when a delimiter is typed - never while the token is still composing. A live
   preview needs a "would this currently split if finalised right now" check run continuously (most likely
   from `refreshSuggestions()` or `updateComposing()`, alongside the existing per-keystroke work), not just
   once at the end. `TokenRepair.trySplit()`'s own logic is already reusable for this (it needs no state
   beyond the token itself, `spaceAmbiguousIndices` and `previousWord`, all already available where
   `refreshSuggestions()` runs) - the gap is purely that nothing currently *calls* it early.
2. **The composing text currently gets one colour span for the whole token, or none.** `updateComposing()`
   applies a single `ForegroundColorSpan` over the entire composing `SpannableString` when
   `shouldHighlightComposing()` says so. A split preview needs *two* separate spans - one over each half - and
   the dropped character (drop-strategy split) or the boundary itself (missed-space split) left uncoloured, so
   the visual matches "these two pieces, not this one word". `updateComposing()`'s `SpannableString` branch
   would need to switch from "colour everything" to "colour these two ranges" when a split preview is active,
   falling back to the existing single-span behaviour otherwise.

Suggested shape (not committed to): a new pure function, e.g. `SplitPreview.previewFor(token, ...): SplitResult?`
(thin wrapper reusing `TokenRepair.trySplit()`), called from `refreshSuggestions()`, its result cached
alongside the existing suggestion state so `updateComposing()` can read it without recomputing; `updateComposing()`
then builds either the existing single span or two spans depending on whether a preview is present. Interacts
with §35's Greek math-symbol fix and D-62's mid-word reclaim only in that both also touch composing-text
spans - worth double-checking span ranges stay correct when a reclaimed "after" fragment is also present.

## §48 - Backlog: Swipe-Up Settings Row (Gear + Emoji Button)

Requested feature, not implemented - resolves an existing backlog item (§26) with a concrete design:

- **Trigger:** an upward swipe anywhere on the keyboard (mirroring G-03's existing "downward swipe anywhere
  dismisses the keyboard" - `GestureAction.DISMISS_KEYBOARD`) reveals a new row above the keyboard.
- **Row contents:** the emoji button at the left edge, a settings gear icon at the right edge that opens the
  app's settings directly (`launchFromKeyboard`, the same mechanism already used for the calibration/model-
  import screens from onboarding).
- **Animation:** the row slides up into view, but is clipped at its own bottom edge against the top of the
  existing keyboard - it should read as emerging *from* the keyboard, not sliding in over it. The existing
  D-86 "growing into more rows resizes right away" handling (`rebuildRows()`/the slide-direction logic from
  D-94's fix) is the closest existing precedent for a keyboard-height change with animation and is the
  natural place to extend, though this is a genuinely new row *above* the keyboard rather than a change in
  row count within it, so it may need its own transition rather than reusing that path directly.
- **Dismiss interaction, layered on G-03:** a downward swipe while the row is showing animates the row away
  (closing it), *not* the whole keyboard. Only a *second* downward swipe - now that the row is already closed
  - falls through to the existing G-03 dismiss-keyboard behaviour. `GestureAction` would need a new value
  (e.g. `CLOSE_SETTINGS_ROW` or similar) that `handleSwipe()`'s downward-swipe handling checks for before
  falling back to `DISMISS_KEYBOARD`, gated on whether the row is currently open.
- **Settings consequence:** the standalone `AdaptSettings.emojiPanelEnabled` setting (and its corresponding UI
  toggle) can be retired - the emoji button's visibility becomes governed solely by the existing
  `symbolKeyEnabled` (?123/ABC key) setting, removing the dependency `PanelNavigation`/`AdaptKeyboardView`
  currently have on `emojiPanelEnabled` for deciding what the combined key does (L-03: "opens the emoji panel
  from the letter view, or the ?123 layer when the emoji panel is disabled"). Worth checking whether the
  combined key's dual-purpose behaviour still makes sense once the emoji button has its own dedicated,
  always-reachable home in the new row, or whether it simplifies to "always ?123" once this lands.

## §49 - §47 Implemented: Live Colour Preview of a Pending A-05 Split

Implements §47. Two deliberate deviations from that section's "suggested shape", both root-caused against
the actual call sites rather than guessed:

1. **No caching between `refreshSuggestions()` and `updateComposing()`.** §47 suggested computing the split
   preview in `refreshSuggestions()` and caching it for `updateComposing()` to read. Every call site
   (the `CHAR` key handler, `appendLongPressLetter()`, `deleteComposingChar()`) calls `updateComposing(ic)`
   *before* `refreshSuggestions()` for the same keystroke - a cache written by the latter would always be
   one keystroke stale when the former reads it. Instead, the new `AdaptKeyService.splitPreview(ic, text)`
   calls `tokenRepair.trySplit()` directly from inside `updateComposing()`, mirroring how
   `shouldHighlightComposing()` already computes its own answer inline rather than reading a cached one. This
   costs the same per-keystroke dictionary queries `trySplit()` already pays once at commit time
   (`finalizeAndCommit()`), now paid continuously while composing - bounded by token length, same order of
   magnitude as the lookups `refreshSuggestions()` already does every keystroke, so no new caching
   infrastructure was warranted.
2. **No new `SplitPreview` wrapper type.** §47 sketched a `SplitPreview.previewFor(...)` function reusing
   `TokenRepair.trySplit()`. `trySplit()` was reusable as-is; the only real gap was the two colour-span
   ranges within the *original* token, which needed the strategy (drop-a-character vs. missed-space) to
   know whether a one-character gap sits between the halves. Rather than have `trySplit()` track and return
   which strategy won, `SplitResult.spanRanges(token)` recovers it from length arithmetic alone: a
   drop-strategy split's `left.length + right.length` is always exactly `token.length - 1` (one dropped
   character), a missed-space split's is exactly `token.length` (contiguous) - see
   [TokenRepair.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/dictionary/TokenRepair.kt). No new file,
   tested directly on `SplitResult`.

`updateComposing()`'s single-span branch is now the `else` of a new `splitPreview(ic, text) != null` check;
the split-preview branch applies two `ForegroundColorSpan`s from `SplitResult.spanRanges()`. `splitPreview()`
is gated exactly like `shouldHighlightComposing()` (`config.highlightEnabled`, and D-26's mid-word rule) -
both now share the extracted `isEditingMidWord(ic)` helper instead of duplicating the "letter immediately
after the cursor" check. Mutual exclusivity between the split preview and the existing single-word highlight
falls out for free: `trySplit()` already returns null whenever the whole token is itself a known word, which
is `shouldHighlightComposing()`'s own trigger condition.

## §50 - §48 Implemented: Swipe-Up Settings Row (Gear + Emoji Button)

Implements §48. Follows the suggested shape's gesture design closely (an upward swipe anywhere but the
combined key opens the row via `KeyGesture`/`GestureAction.OPEN_SETTINGS_ROW`; a downward swipe while it is
open closes it first, gated in `AdaptKeyService.handleSwipe()` before falling back to `DISMISS_KEYBOARD`),
but resolves the two points §48 left open, and lands the animation differently from the D-86 precedent it
flagged as a possible mismatch:

1. **No new `CLOSE_SETTINGS_ROW` `GestureAction`.** `KeyGesture.resolve()` is a pure function with no row-
   open state to gate on - it always reports the plain `DISMISS_KEYBOARD` for a downward swipe, exactly as
   before. `AdaptKeyService.handleSwipe()`'s existing `DISMISS_KEYBOARD` branch re-routes to
   `closeSettingsRow()` when `settingsRow?.isOpen == true`, and only falls through to `requestHideSelf(0)`
   otherwise - the state check lives where the state actually is, without inventing an action value
   `KeyGesture` itself never produces.
2. **The combined `?123`/emoji key's dual purpose is retired - it is now always a plain `?123` toggle.**
   §48 flagged this as worth checking once the emoji button has its own dedicated home; keeping the old
   D-18 behaviour would have needed a replacement setting for something §48's own instructions retire
   outright (`emojiPanelEnabled`), so the combined key drops its emoji affordance entirely.
   `PanelNavigation.onCombinedKeyTap()` lost its `emojiEnabled` parameter (its body is now exactly D-18's
   old "disabled" branch, unconditionally); `AdaptKeyboardView` lost its own `emojiEnabled` field, and its
   three call sites (label, corner-hint suppression, hidden-slot check) collapsed to the `?123` case
   unconditionally. `emojiPanelEnabled` is removed end to end - `AdaptSettings`, `SettingsMapper`,
   `SettingsStore` (`KEY_EMOJI_PANEL`), and the `settings_preferences.xml` toggle (all three locale
   `strings.xml` variants updated to match) - and the settings row's emoji button visibility is now driven
   by `symbolKeyEnabled` instead, per §48's own instruction.

**The animation does not reuse `AdaptKeyboardView`'s D-86/D-94 row-growth machinery**, confirming §48's own
suspicion that it might not transfer directly: that machinery lives inside `AdaptKeyboardView`'s self-drawn
Canvas engine and animates *row count within the same view*, not a second, independent view stacked above
it. The new `SettingsRowView` ([SettingsRowView.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/SettingsRowView.kt))
is instead an ordinary `FrameLayout` inserted into `AdaptKeyService`'s root `LinearLayout` between the
suggestion bar and the keyboard container. `open()`/`close()` jump the reserved layout space to its target
height immediately (still the D-86 precedent in spirit - growing resizes right away, so nothing waits for
space) and then animate only the row's *inner content* via `translationY`, clipped to the row's own bounds
by a plain `ViewGroup`'s default `clipChildren` - no explicit `clipBounds` needed. This reads as the content
sliding up out of / back down into the space right above the keyboard, matching "clipped at its own bottom
edge against the top of the keyboard, emerging from it" without needing a bespoke transition class.

Both row buttons close the row before acting (`openEmojiPanelFromSettingsRow()` /
`openSettingsAppFromSettingsRow()` in `AdaptKeyService.kt`), a deliberate "tap an item, the menu closes"
UX call not spelled out in §48; `SettingsRowView.closeImmediately()` (no animation) resets the row on every
fresh `onStartInputView()`, so a row left open when the keyboard was dismissed some other way (e.g. the
Home button, not a down-swipe) never carries over into the next field.

Pure logic (`KeyGesture`/`PanelNavigation`) is unit-tested (`KeyGestureTest`, `PanelNavigationTest`);
`SettingsRowView` itself is Android view/animation glue with no decision logic of its own beyond what those
two already cover, left untested per this project's established, documented limitation (no emulator/device
in this environment).

## §51 - §48 Refined: Row Position + Unconditional Emoji Button (User Feedback)

Two corrections to §50's implementation, both direct user feedback after trying the built feature (device
build, not this environment's own testing - confirms §50's row-auto-closes-on-tap design call, made without
being spelled out in §48, was wanted as built):

1. **The settings row now sits above the suggestion bar, not between it and the keyboard.** §48's own text
   ("clipped at its own bottom edge against the top of the existing keyboard... emerging from the keyboard")
   was itself just the implementer's working assumption for "a new row above the keyboard" at spec-writing
   time, not a constraint anyone had weighed against alternatives - the user tried the shipped placement and
   asked for the row to be the topmost element instead, above the word-suggestion row. No technical reason
   favours the old placement: `SettingsRowView.open()`/`close()` reserve their own height and animate their
   own content via `translationY` regardless of where they sit in `AdaptKeyService`'s root `LinearLayout` -
   the reveal animation is entirely self-contained and position-independent. The only change is
   `root.addView()` order (`onboardingView`, `row`, `bar`, `container`) in `onCreateInputView()`; the
   "emerges from the keyboard" framing in `SettingsRowView`'s KDoc no longer quite fits (it now emerges from
   the very top of the input view, with the suggestion bar below it, not from the keyboard's edge) and was
   reworded rather than left stale.
2. **The row's emoji button is now unconditionally visible - `symbolKeyEnabled` no longer gates it.**
   §48's own text said the button's visibility "should run exclusively via `AdaptSettings.symbolKeyEnabled`"
   - implemented that way in §50 (`SettingsRowView.emojiButtonEnabled`, wired from `applySettings()`) - but
   the user reconsidered once they saw it built: with the emoji button now living in its own dedicated row
   (not the combined keyboard key `symbolKeyEnabled` actually governs), tying its visibility to that
   unrelated setting no longer made sense to them, and it is dropped entirely. `emojiButtonEnabled` is
   removed from `SettingsRowView` along with its `applySettings()` wiring; the button (and `settingsButton`)
   are always shown once the row is open. `AdaptSettings.symbolKeyEnabled`'s doc comment updated to drop the
   now-incorrect claim; the `d59_summary` string (all three locales) reverted to no longer mention the
   settings row.

No spec-writing lesson beyond the usual one: flavour text describing *how something should look* (§48's
"emerging from the keyboard", "exclusively via symbolKeyEnabled") is a proposal to validate against the
real, built thing, not a requirement to defend once it turns out the user pictured something else once they
could actually try it.

## §52 - Why the Emoji Button Needs No Setting (User Rationale for §51)

§51 dropped `SettingsRowView`'s emoji-button gate on `symbolKeyEnabled` as a user correction; this section
records the actual *why* behind it, as user-supplied reasoning, not just "the user asked" - so a future
reader (or session) does not misread the empty settings screen slot as an oversight or re-introduce a gate.

**Why the old `emojiPanelEnabled` toggle (D-18) existed at all:** the combined `?123`/emoji key sat on the
main keyboard, permanently occupying one of its limited key slots (L-03), while the emoji panel itself is
low-value to begin with - most messaging apps ship their own emoji picker, so AdaptKey's is rarely the one
actually used. That is a real space/value trade-off worth a toggle: a user who never opens AdaptKey's emoji
panel could reclaim the slot for `?123` alone instead.

**Why that trade-off no longer exists once the emoji button lives in the §48 settings row:** the row is
reached by an upward swipe and takes no permanent keyboard real estate - it exists only while open. There is
no scarce slot to reclaim, so the original reason for a toggle is gone; gating the row's emoji button behind
a setting anyway would just be an unnecessary extra decision for the user with no actual trade-off behind it.
Confirms `emojiButtonEnabled` (§51) was correctly removed outright, not merely defaulted differently, and
that no replacement setting of any kind should be reintroduced later without this same trade-off returning.

**Consequence for the combined key:** `symbolKeyEnabled` (D-59, still the one setting hiding the combined
`?123` key and reserving its slot) is now the *only* thing the main keyboard's combined key depends on -
confirmed in code, not just in this write-up: `AdaptKeyboardView.isHiddenKey()` gates the `SYMBOL` key on
`!symbolKeyEnabled` alone (no `emojiEnabled` counterpart left, per §51), and `PanelNavigation.onCombinedKeyTap()`
takes no emoji-related parameter at all (per §50). `symbolKeyEnabled` itself is unaffected by any of this and
remains exactly as documented (D-59) - it governs the combined key's own numeric/symbol function, which is
an entirely separate concern from the emoji button's now-settingless existence in the row.

## §53 - D-103 / D-104: Trig-Function and Angle-Unit Keys on the Calculator Page (v0.8.26)

Confirmed D-95 (§27's "settings-shortcut key somewhere on the keyboard" idea) is resolved by §48's swipe-up
settings row (the gear icon) - no separate key needed, closing that backlog item without further work.

### New: `sin` key (D-103) and `deg` key (D-104), appended to calculator row 1
`sin`'s D-01 popup offers `cos`/`tan`/`log` (base included, matching every other multi-alternative key's
convention); `deg`'s single hint offers `rad` - a plain single-hint key like the calculator's `0`→`#`, not a
stateful toggle remembering which unit was last chosen, so either unit is always reachable directly without
first cycling through the other (D-104 explicitly left "two-state toggle vs. its own popup" open; a
hint-based key reuses the existing single-hint mechanism outright rather than adding new toggle state).
Placed at the end of row 1 (`CALC_ROW1_SYMBOLS`'s "everyday symbols not already reachable" row), each given
double weight (`FUNCTION_KEY_WEIGHT = 2f`) relative to that row's single-glyph keys, since a three-letter
label needs more room to stay legible - a starting guess, not verified on a device, easy to retune later
(same approach as §36/§37's row-5 weight tuning).

### New `KeyCode.TEXT`: a key whose tap commits a literal multi-character label, not a single `Key.char`
Neither existing `KeyCode` fit: `CHAR` is hard-limited to one `Char` (`handleKey()`'s `CHAR` branch reads
`key.char ?: return` - a plain tap on a null-char key would be a silent no-op), and every other code is a
fixed control action. Added `KeyCode.TEXT`, whose plain tap commits `key.label` verbatim via
`finalizeAndCommit()` (finalising any in-progress token first, exactly like a punctuation delimiter or the
calculator page's other symbol keys - never word-extending). Four places already generic over "does this key
have a hint/alternatives" needed a small extension to also recognise `TEXT`, not just `CHAR`:
- `KeyboardLayout.hasLongPressAction()` - a `TEXT` key with a hint or 2+ alternatives now schedules a
  long-press exactly like a `CHAR` key does.
- `AdaptKeyboardView.popupAlternativesFor()` - a `TEXT` key's single `hint` is now offered as a one-item
  popup too, not only a `CHAR` key's.
- `AdaptKeyboardView.preSelectedIndexFor()` - falls back to `key.label` (not just `key.char?.toString()`)
  to find the key's own value within its alternatives, so `sin` pre-selects correctly among
  `sin`/`cos`/`tan`/`log` despite having no `Key.char` at all.
- `Key.id` (T-03 personal offset model key) - a bare `code.name` would have collided (`"TEXT"` for both `sin`
  and `deg`, violating the "unique within a single layout" contract); now `"t:$label"` for `TEXT` keys,
  mirroring `CHAR`'s `"c:$char"`.

**A real, non-obvious bug caught before it shipped:** `cos`/`tan`/`log` are ordinary Latin letters, so
`AlternativeScript.extendsWord()` (§35's Greek-vs-symbol guard) would have judged them "genuine language
text" and routed them through `appendLongPressLetter()` - extending the composing token and exposing them to
autocorrect/suggestions, exactly the bug §35 fixed for the Greek math-symbol popup, just triggered by a
different, non-Greek route this time. Fixed by threading the popup's originating key's `code` through
`handleLongPressAlternative()`/`commitLongPressSymbol()` (previously the key was discarded at the listener
call site - `{ _, alternative -> ... }`) so a `TEXT` key's alternatives always commit as symbols, regardless
of script - the `AlternativeScript` check is skipped outright for `sourceCode == KeyCode.TEXT`, not merely
one more input to it.

New tests: `SymbolLayoutTest` (row-1 contents, alternatives/hint, `Key.id` uniqueness) and
`KeyboardLayoutTest` (`hasLongPressAction` for both the multi-alternative and single-hint `TEXT` cases). The
`commitLongPressSymbol`/`AlternativeScript`-bypass fix itself is `AdaptKeyService` glue with no independently
testable pure logic beyond what `AlternativeScriptTest` (§35) already covers - not re-tested in isolation.

## §54 - D-88: Feedback When a Suggestion Is Accepted (v0.8.27)

Previously silent and visually unremarkable ("das ist etwas trocken und man kriegt es nicht ausreichend
mit"). Implemented both of D-88's two additions, chosen by the same D-05 key-sound setting the spec named:

**Sound on:** a distinct new "plop" sample, separate from the D-70/D-83/D-85 key-click sample, plays via
`AdaptKeyboardView.playSuggestionAcceptedSound()` - loaded into the same shared `SoundPool` lazily, exactly
like the click sample. No suitable sample existed in this environment (no audio-editing tooling, no network
fetch of third-party assets), so one was synthesised from scratch with a short Python script (stdlib `wave`
module only, no external dependencies or copyrighted material): a 130 ms exponential pitch sweep from 900 Hz
down to 140 Hz with a fast attack and exponential decay envelope - the classic "water-drop" plop shape -
matching the existing sample's format (mono, 16-bit PCM, 48 kHz). Bundled as `res/raw/suggestion_accept.wav`.

**Sound off:** a brief highlight flash on the suggestion bar instead (`SuggestionBarView.flashAccepted()`) -
fades from a peak alpha to fully transparent over 280 ms, painted as a full-bar overlay rect after the chips
(and the G-04 trash zone, when armed) so it is always visible. Uses the same green as the existing C-04/S-05
recognised-word highlight for visual consistency, via a new `flashColor` property the service pushes from the
user's actual configured `highlightColor` (not just its default) in `applySettings()`.

**Found and fixed one real bug in the existing `SoundPool` setup while adding the second sample:**
`SoundPool.setOnLoadCompleteListener()` keeps only the single most-recently-registered listener - the
original `ensureClickSoundLoaded()` installed one checking only `clickSoundId` each time it ran, which a
naive `ensureAcceptSoundLoaded()` following the exact same pattern would have silently replaced with one
checking only `acceptSoundId`, permanently losing the ability to ever detect the *click* sample finishing its
decode if the accept sample's loader happened to run afterward (order-dependent, so not always the same
failure). Fixed by installing one shared listener - checking both ids - once, at `soundPool`'s own lazy
creation, rather than repeatedly inside each `ensure*Loaded()`.

**Trigger points:** `finalizeAndCommit()`'s existing `finalWord != typed` branch (the same check that already
drives the S-05 undo-tracking, so "an accepted correction" is defined identically in both places) and the
`SuggestionController.Kind.NORMAL` suggestion-bar-tap branch (any bar suggestion, even one that happens to
match what was typed, per D-88's "and/or when a suggestion-bar item is tapped").

No new unit tests - `notifySuggestionAccepted()`, `playSuggestionAcceptedSound()` and `flashAccepted()` are
all Android `SoundPool`/animation/view glue with no independently testable pure logic, matching this
project's established, documented limitation (no emulator/device in this environment).

## §55 - Bug Fixed: §54's Flash Overlay Made the Suggestion Bar Solid Black (v0.8.28)

Reported immediately after §54: the suggestion bar was fully black, unreadable, regardless of whether an
acceptance had actually happened yet.

Root cause: `flashPaint` (`SuggestionBarView`) was declared as `Paint(Paint.ANTI_ALIAS_FLAG)` with no initial
colour or alpha. `android.graphics.Paint()` defaults to fully opaque black (`0xFF000000`, alpha 255) until
something sets it otherwise - `draw()`'s `if (flashPaint.alpha > 0)` was therefore true from the very first
frame the view ever drew, long before `flashAccepted()` had ever run once, painting a solid opaque black rect
over the whole bar permanently. `flashAccepted()`'s own animator only ever set `flashPaint`'s colour/alpha
*while actually animating* - there was no code path that ever left it transparent by default.

Fixed with one line: `Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 0 }` at declaration, so the overlay starts
fully transparent and only becomes visible for the duration of an actual flash.

## §56 - D-88 Refined: Sound Sharpened/Louder, Flash Replaced with a Flying Word, Settings Rename (v0.8.29)

Feedback on §54 after real-device testing: the plop sound works well but needed to sound more like a sharp
"Plöpp" than a duller "Blöpp" (and 10% louder); the flat bar-flash felt too minimal, not clearly conveying
"this is being applied to your text"; the animation should play independently of the sound setting, not only
as its fallback. Also: rename the D-59 combined-key setting to `?123/ABC-Taste`.

### Sound: sharper attack, 10% louder
Regenerated `suggestion_accept.wav` with the same synthesis approach (§54): attack shortened from 3 ms to
under 1 ms (a near-instant onset reads as a sharp plosive "P", not a softer "B"), a brief (~6 ms) higher-
frequency transient layered only at the very onset for the extra "crack" a true plosive attack has, and peak
amplitude raised from 0.75 to 0.825 (10% louder). Everything else (the 900→140 Hz pitch-drop shape, envelope,
sample rate/format) is unchanged.

### Animation: replaced the flat flash with a flying, shrinking, fading word
**Technical constraint clarified with the user first, since it changes what "flies into the text" can
concretely mean**: an IME can only draw within its own window - it has no way to draw into the *target app's*
text field, which is a separate window/process entirely. What is achievable, and what was built: the accepted
word rises from the middle of the suggestion bar, shrinking and fading out as it exits upward past the bar's
own top edge (`SuggestionBarView.flyAccepted(word)`, replacing `flashAccepted()`) - close to the visible top
of the keyboard, right below where the actual (external, undrawable) text field continues. Deliberately
subtle per explicit request ("dezent... damit sie nicht nach ein paar Mal nervt"): a modest 34dp rise, 380 ms
duration, DecelerateInterpolator easing. Negative y-values (above the bar's own allocated bounds) reach the
screen because `root` (`AdaptKeyService.onCreateInputView()`) already runs with `clipChildren = false` for
the same reason the existing D-53 long-press popup escapes upward over this same bar - a sibling view's
overflow past its own bounds is not clipped by a `ViewGroup` with `clipChildren = false`.

**Decoupled from sound entirely**, per explicit request: `notifySuggestionAccepted(word)` now always plays
the flight, and *additionally* plays the plop sample when key-press sound (D-05) happens to be on - no
longer an either/or gated on that setting.

### Settings: renamed the D-59 combined-key toggle
`d59_title` (`strings.xml`, all three locales) changed from "?123 key" / "?123-Taste" / "Πλήκτρο ?123" to
"?123/ABC key" / "?123/ABC-Taste" / "Πλήκτρο ?123/ABC" - the key's own two functions (numeric/symbol layer,
and the `ABC` return key it pairs with), not just one of them, now named in the setting itself.

## §57 - §56 Refined: the Flying Word Now Anchors to the Accepted Suggestion's Position (v0.8.30)

Feedback after device testing §56: the overall effect works well and the rise distance is exactly right as
is - but flying from the bar's fixed horizontal centre every time read as too disconnected from the actual
suggestion; flying from (at least approximately) the accepted suggestion's own on-screen position was asked
for instead, with an explicit allowance that an approximate position is fine if precise tracking is awkward.

New `SuggestionBarView.originXFor(word)`: looks up the currently displayed chip whose
`SuggestionController.DisplayItem.word` matches the accepted word (case-insensitively) and returns its
horizontal centre (in the bar's own coordinates, already accounting for `scrollX`); falls back to the bar's
centre - the previous, exact §56 behaviour - only when no matching chip is found, rather than trying harder
to guess a position. This single lookup naturally covers both trigger paths without needing separate
handling: a bar-tap acceptance finds its own just-tapped chip (`notifySuggestionAccepted()` fires before the
bar's next `setItems()` refresh, so the tapped chip is still there to find), and an autocorrect-on-commit
acceptance finds the top suggestion chip whenever the committed word happens to match what was already being
shown there (a common case, since the just-corrected word is usually also the top suggestion) - and falls
back gracefully to centre for whatever cases it doesn't (the bar showing something else, or being empty).

## §58 - Mid-Word Live Correction: a Real Gap Found (Not Just Bugs) - Reclaim on Caret Move (v0.8.31)

Two precise, concrete repro cases pinned down what §32/§46's investigations, focused on bugs in the
*existing* mechanism, had not: a genuine **missing trigger path**, not (only) a defect in the one that
exists.

1. Type a word (e.g. "hatte"), Space, then Backspace (to re-edit it) - the caret ends up touching the word,
   but no suggestion appears. Typing the word without ever pressing Space first works fine.
2. Type "Hatte ich", tap the caret into the middle of "Hatte" - no suggestion appears; at minimum "hätte"
   should be offered.

Traced both to the same root cause: `reclaimSurroundingWord()` (D-62) was **only ever called from a
keystroke handler** (`handleKey()`'s `CHAR` branch, `appendLongPressLetter()`) - never in response to the
caret simply *arriving* at a word with nothing typed. Confirmed by grep: no other call site existed. Case 1's
Backspace removes the space and leaves the caret touching "hatte", but backspace-when-composing-is-empty
(`deleteOneBefore()`) never reclaims anything - it is a plain delete, nothing more. Case 2's tap moves the
caret via a genuine external `onUpdateSelection` callback, which - before this fix - only ever handled the
already-composing case (deciding whether to wipe an in-progress token); the `composing.isEmpty()` branch was
a bare `return`, doing nothing at all. Neither repro case was ever reaching the reclaim mechanism, regardless
of §32's earlier fixes to that mechanism itself - the earlier "no further defect found" conclusion (§46) was
correct as far as it went, but was investigating the wrong question: not "is the reclaim broken" but "does
anything ever call it here at all".

**Fixed**: `onUpdateSelection()`'s `composing.isEmpty()` branch now calls a new `reclaimWordAtCaret()` for a
collapsed selection (`newSelStart == newSelEnd`) - it runs the exact same reclaim-then-render sequence
`handleKey()`'s `CHAR` branch runs when a new token starts mid-word (`captureTokenContext()`,
`resetWordEndShift()`, `reclaimSurroundingWord()`, `updateComposing()`, `refreshSuggestions()`), minus
inserting a character, since nothing was typed - and the same D-87 outer batch edit, for the same reason
(`reclaimSurroundingWord()`'s own `deleteSurroundingText()` must not reach the app as a standalone edit). A
no-op when the caret touches no word at all, exactly like the keystroke-triggered path already was. Self-
limiting against re-entrancy: the moment a reclaim actually happens, `composing` becomes non-empty
synchronously, so any callback this reclaim's own edits generate lands in the already-composing branch, not
back in this one.

**Also answers a separate question raised alongside these repros**: searched the dictionary/capitalisation/
suggestion/language packages for hardcoded word-specific exception logic (e.g. `if (word == "...")`-style
special-casing that should be generalised into real rules, matching this session's §43-§45 fixes). Found
none - the only literal word lists in that area are `SeedData.kt` (a tiny bootstrap dictionary used before
the real one loads) and `StubSuggestionProvider`'s fallback word list (explicitly documented as "the
placeholder" provider) - legitimate bootstrap/fallback *data*, not exception *rules* silently overriding the
general logic.

Verified by careful tracing of the exact call graph (confirmed via grep that no other reclaim call site
existed), not by observing it run at the time, consistent with this project's documented limitation (no
emulator/device in this environment) - but worth flagging as a real, accepted trade-off regardless: this
makes *every* tap into *any* existing word start a live-edit/suggestion session for it, not only a deliberate
"I want to fix this word" tap.

**CONFIRMED WORKING on a real device** (user, same day) - closes out the D-62/D-87 mid-word live correction
saga (D-84's real-but-partial fix, §32's two D-87 fixes, §46's "no further defect found" investigation, and
finally this section's actual missing-trigger fix).

## §59 - Backspace-Hold Repeat: ~9% Faster (v0.8.32)

Requested: the accelerating backspace-on-hold (D-07/D-31/D-56) felt a touch too slow, both for character-wise
and word-wise deletion, by roughly 5-10%.

`BackspaceRepeat`'s three cadence constants reduced by the same round-number proportion (~9%), so the decay
curve's *shape* is unchanged, only its overall speed: `CHAR_START_DELAY_MS` 165ms → 150ms, `CHAR_MIN_DELAY_MS`
50ms → 45ms (both scaled together, since scaling only one would have distorted how quickly the curve reaches
its floor), `WORD_DELAY_MS` 330ms → 300ms. `INITIAL_DELAY_MS` (400ms, the delay before holding starts
repeating at all) is untouched - the request was specifically about the repeat cadence once it starts, not
how long a hold takes to begin repeating. Existing `BackspaceRepeatTest` assertions reference the constants
symbolically, not their literal values, so nothing needed updating there.

**CONFIRMED WORKING** (user, same round as §58's confirmation).

## §60 - D-36 Paste Chip: File Clips No Longer Risk Pasting Binary Garbage as Text (v0.8.33)

Asked: can the keyboard tell when the clipboard holds a whole *file* (e.g. copied in a Files app), rather
than plain copied/selected text - and if so, only ever offer it via Quick Paste when it is itself a text
file (`.txt`, `.md`, and other recognisable text files), showing nothing at all otherwise.

**Yes, detectable without reading anything.** `ClipData.Item` carries either inline text (`getText()`) or a
URI (`getUri()`) pointing at real content - a plain copy (selected text, a typed string) always populates
`text`; a file copy (Files app, sharing a document) populates `uri` instead. The clip's own `ClipDescription`
already declares a MIME type for that URI - reading it needs no I/O at all, so the file-vs-text decision
itself is free. Android's `ClipDescription.hasMimeType()` supports the `"text/*"` wildcard natively (no
hand-rolled MIME parsing needed) - true for `.txt`/`.md`/`.csv`/etc., false for images, PDFs, and anything
unrecognised.

**Implemented**: new `AdaptKeyService.resolveClipboardText(clip, item)`, called from
`showClipboardChipIfAvailable()` in place of the previous direct `item.coerceToText(this)` call. A plain
text item (`item.uri == null`) behaves exactly as before - zero change to the common case. A URI item is only
offered when `clip.description.hasMimeType("text/*")`; anything else returns null, and the chip is suppressed
entirely, exactly as requested ("sollte dann einfach nichts anzeigen"). When it *is* a text file, its content
is read directly via `contentResolver.openInputStream(uri)` (bypassing `coerceToText()`'s own less-
predictable URI-resolution fallbacks), capped at `CLIPBOARD_FILE_PREVIEW_CHARS` (512) - a small, bounded read
regardless of the file's actual size, since only the chip's own already-truncated preview
(`ClipboardPreview.MAX_LENGTH`, 24 chars) needs the content at all. The *actual* paste, on tap, still goes
through §38's native `performContextMenuAction(paste)` - the target app resolves the real file itself, this
read is never used for the paste's own content, only the chip's preview label.

One real bug caught (and fixed) while writing this: the KDoc for `resolveClipboardText()` originally spelled
out the wildcard as `` `text/*` `` in backticked prose - but Kotlin block comments nest, so the literal `/*`
inside that backticked span opened a *nested* comment the actual closing `*/` only closed, leaving the outer
KDoc comment open and silently swallowing the rest of the file as commentary (a cascade of "unresolved
reference" errors at compile time, traced back to this one line). Reworded to avoid embedding a literal `/*`
inside any comment; the one real occurrence of the string `"text/*"` left in the file is inside an ordinary
string literal (`hasMimeType("text/*")`), where it is not treated as a comment token.

**Working well in initial testing, deliberately left open rather than marked confirmed** - the user wants
broader real-world use before treating this one as settled.

## §63 - D-106 Stages 1+2 Implemented: English as an Explicit Language, Cross-Dictionary Loanword Protection (v0.8.34)

Follows a design discussion resolving D-106 (§61) into three stages; stage 3 (installable additional
languages) is discussed below but deliberately not started, and its Greeklish-transliteration sub-item is
dropped outright per explicit instruction, not merely deferred.

### D-115 correction: the case-only-correction rule does not fix "stimmen"/"Stimmen" - traced to a different mechanism
Before implementing, the "never silently apply a pure-case-differing correction" idea discussed for D-115 was
checked directly against the code, not assumed: `DictionarySuggestionProvider.bestCorrection()`'s own candidate
filter (`it.lowercase() != token`) already structurally excludes a case-only-different candidate - "Stimmen"
can never even reach that function's candidate list for typed "stimmen" in the first place. The actual
mechanism is `CapitalisationEngine.capitalise()`'s §6 rule 3 ("pure noun → auto-capitalise"), which trusts
`store.partsOfSpeech(word)` - itself a case-insensitive lookup (`InMemoryDictionaryStore`/`SqliteDictionaryStore`
both `.lowercase()` the key). "stimmen" is tagged as pure `NOUN` in the shipped dictionary, not the documented
"mixed → ambiguous `{NOUN, OTHER}`" case the build is supposed to detect for exactly this kind of homograph -
so §6 rule 5 ("ambiguous words: no automatic correction, offered as a suggestion" - which already exists and
already does precisely what was wanted) never gets a chance to fire, purely because the underlying data
mis-classified the word as unambiguous. The fix for this specific mechanism is therefore a dictionary-build
ambiguity-detection improvement, not a new runtime rule; recorded here so a future session doesn't
re-implement the case-only-correction idea expecting it to also cover this case. That idea is kept for its
original purpose (D-111's visibility gap, and any future correction path that genuinely could offer a
case-only candidate) - it just isn't a fix for D-115's own reported symptom. Deferred alongside the Ablaut-
generation idea (D-115) as a data-quality item, not started this round.

### D-106 Stage 1 - English Promoted to a Real, Selectable Keyboard Language (QWERTY)
The per-language `stores`/`providers`/`engines` maps (`AdaptKeyService`) already carried a full English entry
from the earlier real-multilingual-dictionaries work - English was only ever reached via A-03's per-token
auto-detection, never as a deliberately chosen active alphabet the way German/Greek already were. Promoted it
to a third, explicit position in the G-01 cycle:

- New pure `language/LanguageCycle` object (`LanguageCycleTest`, 3 tests): the fixed order
  `German → English → Greek`, with `next()`/`previous()` wrapping around the ends - kept separate from
  `AdaptKeyService` so the stepping logic itself is JVM-unit-tested, matching this project's usual split
  between pure policy and Android glue.
- `AdaptKeyService.toggleLanguage()` now takes a `forward: Boolean` (the swipe's own direction), stepping via
  `LanguageCycle` instead of the old two-language `if (== GREEK) GERMAN else GREEK` flip;
  `handleSwipe()`'s `GestureAction.LANGUAGE_PREV`/`LANGUAGE_NEXT` branches (already distinct in `KeyGesture`,
  previously collapsed into one identical call since direction didn't matter for two languages) now pass their
  own direction through.
- `resolveDict()` gained an unconditional `activeLanguage == Language.ENGLISH` branch (mirroring the existing
  Greek one), so English becomes the *forced* dictionary/capitalisation/store while active, not merely a
  classifier fallback - the classifier-driven English routing for an auto-detected foreign context (while
  German is active) is untouched, since that's still exactly what it's for.
- **QWERTY layout**: German is QWERTZ; English keyboards are QWERTY, differing only in the `y`/`z` position.
  `KeyboardLayout.rows()` gained a `qwerty: Boolean = false` parameter selecting between two private row-string
  constants (`"qwertzuiop"`/`"qwertyuiop"` for the top row, `"yxcvbnm"`/`"zxcvbnm"` for the third) - every hint,
  alternative, weight and every other row is entirely unchanged and shared between both variants, since
  `letterHints`/`topRowKey()` key off the character itself, not its screen position.
  `AdaptKeyboardView` gained a parallel `qwerty` property (same `rebuildRows()`-on-set pattern as the existing
  `greek` property), threaded into `rowsFor()`'s `KeyboardLayout.rows(...)` call; both `.greek =` assignment
  sites in `AdaptKeyService` (`onStartInputView`, `toggleLanguage()`) gained a matching `.qwerty =` assignment.
- **Known, accepted trade-off, not fixed this round**: the personal touch-offset model (T-03) keys its learned
  per-key zones by `Key.id` (`"c:y"`/`"c:z"`), which is character-identity-based, not screen-position-based. Since
  `y` and `z` sit in different physical positions between QWERTZ and QWERTY, switching to English moves each
  letter's on-screen position while carrying over whatever offset was learned for it under German - a real, but
  narrow and self-healing effect (only these two keys, only right after a switch, and T-03's continuous learning
  already re-adapts from there exactly as it would after any other zone drift). Not worth a bigger fix (e.g.
  position-aware ids, which would also need migrating already-persisted calibration data) for two keys that
  settle back out through ordinary typing.
- `languageLabel()`'s doc comment and the surrounding KDoc updated to drop the old "only German and Greek are
  user-selectable" framing.

### D-106 Stage 2 - Cross-Dictionary Loanword Protection
New `AdaptKeyService.knownInOtherLanguage(token)`: true when the token is a known word in any *other* bundled
language's dictionary than the one currently active for it (`providers.any { (language, p) -> language !=
activeLanguage && p.isKnownWord(token) }`) - since `providers` already holds all three bundled languages
simultaneously (German/English/Greek, all reachable via the stage-1 cycle), this naturally covers "every
active-or-selectable language, plus always English" without needing a separate "which languages are installed"
concept yet (that's stage 3). Deliberately a plain existence check, not a cross-language suggestion-ranking
model - matching the scoped-down design agreed on discussion, since frequency counts from different corpora
aren't meaningfully comparable and the reported problem ("Word" wrongly autocorrected to "wird") only needs
"don't touch a word that's valid somewhere else", not a merged, ranked completion list.

Wired at the two existing A-03 `suppressAutocorrect`-style choke points, asymmetrically per what each one needs:

- `finalizeAndCommit()`: folded straight into the existing `suppressAutocorrect` flag (`selectActiveDictionary(...)
  || knownInOtherLanguage(typed)`) - every downstream check already gated on that flag (the A-05 split veto, the
  diacritic-restoration/high-confidence-correction lookups, the final `autocorrectFor`/`rawCoordinateCorrection`
  call) now also leaves a cross-language-known token completely alone, for free, with no other line needing to
  change.
- `refreshSuggestions()`: **not** folded into `suppressAutocorrect` there, since that flag's existing branch
  clears the *entire* suggestion bar (correct for "unsupported foreign language, show nothing" - A-03's original
  case - but wrong here, where the active language's own ordinary completions should keep showing). Instead only
  the pending-autocorrect chip is suppressed: `val pending = if (knownInOtherLanguage(input)) null else
  provider.autocorrectFor(...)`.

No new unit tests beyond `LanguageCycleTest` - `knownInOtherLanguage()`/the `finalizeAndCommit()`/
`refreshSuggestions()` wiring are `AdaptKeyService` glue over the already-tested `DictionarySuggestionProvider.
isKnownWord()`, consistent with this project's established Android-service testing gap (see D-39 precedent).

### D-106 Stage 3 - What's Left Without Greeklish
Confirmed still worth keeping, **without** the transliteration sub-item the user struck entirely (not just
deferred - reasoning recorded: the handful of Greek loanwords worth using can simply be learned over time
through ordinary typing, and an English/American-convention Greeklish mapping would often be outright wrong for
German-flavoured Greek transliteration anyway): **installable/activatable additional languages beyond the three
bundled ones.** Still captured as its own, later design item - reuse the tier-3 model's browser+SAF import
pattern (`Tier3ModelActivity`/`Tier3ModelInstaller`, no `INTERNET` permission needed) for delivery, define an
"installable language pack" data format, and add a settings screen to manage installed/active/cycle-reachable
languages. Not started. - Backlog Round 16, Part 1: Layout, Language Architecture, Touch-Model Bounds

Captured, **not started**, per the usual rule for a batch this size - released item by item later.

### D-105 - Every Main-Page Digit Key Gets Its Own Superscript Digit as a Second Alt
Extends L-06's existing shifted-symbol hint (each digit already carries its QWERTZ shifted symbol, e.g. `2`→`"`,
`3`→`§`) with a second long-press alternative: the digit's own superscript form (`²`, `³`, and, for
completeness, `¹`/`⁴`.../`⁰` on every other digit too - not only 2 and 3, which is the visually obvious case
since square/cube are the everyday ones, but "alle Zifferntasten" per the request). Every digit key therefore
goes from a single alternative (which - per D-14/D-53 - applies immediately on long-press with only a one-cell
preview) to **two** alternatives, which per D-01 means every digit now opens a real multi-cell selection popup
instead of applying its shifted symbol immediately. This is a genuine behaviour change for the whole number
row, not just a cosmetic addition, and needs deciding during implementation: which of the two alternatives is
index 0 (pre-selected), and in what order the two are offered. `0` sits at the number row's right edge, so -
exactly the problem §34 already fixed for the letters page's `p`→π key - its popup will want to grow left
instead of right once clamped to the screen edge; the same reversed-alternatives-at-this-one-call-site
treatment (not a change to any shared constant) applies here.

### D-106 - Language Architecture: Explicit English, Multi-Dictionary Consultation, Installable Languages (Big Idea, Deferred)
Two related asks, both pointing at the same underlying gap - AdaptKey currently has exactly one *active*
language pointer (German/Greek, toggled by the G-01 space-swipe) plus a separate, invisible A-03 classifier
that silently reassigns English per-token for autocorrect purposes, but English is never a real, user-selectable
keyboard alphabet the way German/Greek are, and there is no notion of "installing" or "activating" a language
beyond the three hardcoded, bundled ones:

1. **English as an explicit, selectable keyboard language, like Greek.** The user wants to cycle to English via
   the same G-01 mechanism (or an equivalent selector) that already switches German ↔ Greek, not have it stay a
   background auto-detection artifact.
2. **Embedded loanwords/code-switching must not get autocorrected away.** Typing English words inline in German
   text (e.g. "Word") currently has no protection - A-03's language classifier requires several words of context
   (§43's `minWords = 2` fix) before it suppresses German autocorrect for a whole *stretch* of foreign text, but
   a single embedded English word surrounded by German has no such signal and gets silently autocorrected to
   whatever German word is nearest (e.g. "Word" → "wird"). Proposed direction: the dictionaries of **every
   currently-active-or-selectable language** (i.e. everything reachable via the space-swipe cycle) **plus always
   English** should be consulted as valid-word candidates while typing, not just the one currently "active"
   language - so "Word" is found as a known word in the English dictionary and is accepted as a real candidate
   (removing the perceived ambiguity with "wird" and blocking the autocorrect), regardless of which alphabet is
   currently toggled. In the user's own case this would mean German + English (mandatory) + Greek, ideally
   including Greek transliteration (Greeklish) "if that isn't too much effort".
3. **Other languages should be installable/activatable later**, implying a real language-management concept
   (enable/disable/install a language) rather than a fixed three-language toggle baked into the code.

**Explicitly not to be implemented from this capture alone** - this is architecturally closer to D-68's
"rethink calibration" or D-92's calculator redesign: it touches the active-language toggle (G-01), the A-03
classifier/dictionary-selection plumbing (`selectActiveDictionary`/`resolveDict`), the settings surface, and
potentially the shipped dictionary asset set, and needs its own design pass (exact UI for "install/activate a
language", whether "consult N dictionaries simultaneously" changes the autocorrect/suggestion ranking model in
ways that need new tie-breaking rules, and what "always consult English" costs in per-keystroke lookup time)
before any code changes.

### D-107 - Recognised-Word Highlight (S-05/C-04) Semantics Under Reconsideration (Not Decided - Flag Only)
Raised, explicitly **not** to be designed or implemented yet ("deshalb erstmal nur vormerken") - the user is
reconsidering when the green highlight should apply at all. As currently specified (S-05), the highlight means
"this word is recognised as correct and complete, *no* correction is planned" - i.e. green currently signals
*safety*. The user's new framing is the opposite: green should apply *only when the word is about to be
auto-corrected*, i.e. signal an impending *change*, not "this is fine as typed". This is a full reversal of
current S-05 semantics, not a tweak, and the user is explicit that the exact rule isn't settled yet. Noted here
purely as a flag for a future design conversation - no interpretation, no implementation.

### D-108 - Long-Press Popup Must Tolerate a Small In-Key Smear
Reported: a slight finger smear while holding a key with an alt (e.g. the full-stop key) - without the finger
actually leaving the key's own bounds - can currently cancel the pending long-press instead of still opening
the popup. The request: as long as the touch has not left the key's own bounds, it must still count as a
held long-press and show the popup: likely the scheduled-long-press-cancellation logic reacts to the system's
small `ACTION_MOVE` touch-slop rather than to "did the finger leave this key's rect", the way the D-64
drag-to-trash investigation found a similar too-eager slop-based cancellation for a different gesture. Not
yet traced against the actual `scheduleLongPress`/move-handling code - captured as reported, to investigate
together with an implementation pass.

### D-109 - T-03 Offset-Model Learning Needs Hard Bounds
Two related reports, both about the personal touch-offset model (T-03) drifting a key's learned zone too far
from its true, safe position, purely from ordinary (if imprecise) real-world typing - not overridden
deliberately by the user, and visibly worse for them, confirmed by the D-24 touch-zone visualisation:

1. **Bottom-row keys (`c`/`v`/`b`/`n`/`m`, the row directly above the space bar) drift toward space** when the
   user repeatedly mistaps one of them instead of hitting space. Every such mistap that later gets silently
   corrected (the T-05 space-ambiguous flag already exists for exactly this class of tap, and A-05 already uses
   it to retroactively turn the wrong letter back into a space) is nonetheless still being fed into that key's
   own `OffsetModel` as if it were a genuine, confirmed hit for that letter - reinforcing exactly the wrong
   lesson. Proposed direction: a tap that is later resolved as "this was actually meant to be a space" (T-05
   space-ambiguous, subsequently split back to a space by A-05) should not be recorded as a positive training
   sample for the letter key it landed on - and even so, as a hard backstop, the model should never let a
   bottom-row key's zone drift so far toward space that it starts overlapping the space bar's own territory in
   the first place.
2. **General drift bound, beyond the specific space-adjacency case**: the user's own `j` key has drifted
   significantly leftward, to their detriment, well within whatever cap the model already applies
   ("offset capped to key size" per T-03) - the existing cap is evidently too loose in practice, or a
   directionally-biased pattern (from typing-pattern seeding, T-04/`PatternSeed`, or ordinary sustained real
   usage) can still walk a zone to an uncomfortable extreme even while staying inside it. Needs investigation
   into the current clamp value/shape and whether a materially tighter or shape-aware (not just per-axis-size)
   bound is warranted, independent of the bottom-row-specific fix above.

### D-110 - eBay Kleinanzeigen Input Field Never Auto-Capitalises (Investigate)
The message-input box in the (eBay) Kleinanzeigen app never capitalises the first letter, neither at the true
start of a line nor at a sentence start - both of which are AdaptKey's own rules (§6 hierarchy rule 2,
independent of whatever the field's own `EditorInfo` caps flags say) and should apply regardless of what the
target field requests. Not yet traced - candidate causes to check once picked up: whether this field reports an
`inputType`/variation (e.g. a filter or no-suggestions variant) that some existing capitalisation code path
mistakenly treats as "skip auto-cap entirely" rather than just "no field-mandated baseline", or whether
`getCursorCapsMode()`/`EditorInfo` from this specific app returns something AdaptKey's `CapitalisationEngine`
call site doesn't handle. Needs the actual field's reported `EditorInfo` (loggable during a debug session) to
make progress rather than guessing further.

## §62 - Backlog Round 16, Part 2: Autocorrect/Suggestion Quality - Master Plan

The user handed over a large batch of concrete autocorrect/suggestion reports with an explicit instruction on
how to work through them: **do not chase individual reported words** - find and fix the *general rules* behind
the pattern, the way §43 (a language-classifier confidence gate), §44 (a relative instead of absolute A-01
veto) and §45 (a uniform bigram-evidence requirement across both split strategies) already did this session.
Before writing this section, each report was checked directly against the bundled dictionary assets
(`app/src/main/assets/dict_de.tsv`) and/or the actual code (`AdaptKeyService.kt`,
`DictionarySuggestionProvider.kt`, `KeyboardProximity.kt`) rather than guessed at - several turned out to have a
concrete, confirmable cause already visible in the current build; others are dictionary-data findings; a couple
remain open questions needing a design decision. **Nothing in this section is implemented yet** - captured as a
batch, to be worked through and released deliberately, most likely roughly in the theme order below (visibility
gaps and the two confirmed regressions first, since those are the most self-contained; the mid-word-correction
cluster together, since all four share one function/mechanism; the dictionary-coverage questions last, since
those may need a data/build-pipeline change rather than a pure logic fix).

### Theme A - A Pending Change Must Be Visible Before It Applies

#### D-111 - A Silent Case-Changing Correction Is Never Shown as a Suggestion First
When autocorrect is about to change a word's *case* (most often: silently upper-casing a word the user
deliberately typed lower-case), that outcome currently never appears in the suggestion bar in advance - S-02's
"the word exactly as typed does not appear as a suggestion" rule (built for the ordinary "don't waste a slot on
a no-op" case) ends up hiding a case change that is very much *not* a no-op from the user's point of view. S-06
already carved out the one existing exception (a pending literal-substitution autocorrect gets a verbatim
"keep as typed" chip) - this needs a comparable exception for a pending *case-only* change: the target casing
must be visible in the bar before it is silently applied, not discovered only after the fact.

#### D-112 - A Correction's Own Case Must Match the Target Casing Context, Not Just the Dictionary's Canonical Form
Example given: typing "Fur" at a position that will be capitalised (sentence start, or an explicit mid-word
capital) currently offers/applies the dictionary's plain canonical form "für" - the correction should instead
be "Für", matching the same capitalisation context that would apply to any other word typed there. This looks
like an ordering problem between whichever step resolves the *best-matching dictionary word* (case-folded,
naturally, since umlaut/case folding is how the fuzzy match works at all) and whichever step applies §6's
capitalisation transform - the transform needs to run on the *chosen correction*, not only ever on the *raw
typed text*, or a corrected word can silently skip the capitalisation the identical, uncorrected word would
have received. Not yet traced to a specific call site; likely close to where `TokenRepair`/`autocorrectFor`
hands its answer back to `finalizeAndCommit()`, which is also where §6's `CapitalisationEngine` runs.

### Theme B - False-Positive Corrections: Ambiguity/Confidence Gating

#### D-113 - "spreche" → "Sprache": §44's Own Ratio-Override Fires on an Unrelated Homograph Pair
Checked directly against `dict_de.tsv`: **both words are separately known** - `Sprache` (freq 7228, NOUN) and
`spreche` (freq 49, OTHER/verb-form) each have their own dictionary row, so this is not a missing-word case like
D-115 below. The frequency ratio is ≈147x, comfortably past §44's `KNOWN_WORD_OVERRIDE_RATIO` (50x) - the very
rule §44 introduced specifically to let a hugely-more-frequent *typo neighbour* (`die` vs. `due`, `der` vs.
`ddr`) override A-01's known-word protection. Here it fires on two words that are not typo variants of each
other at all, merely a common noun and a rarer verb inflection that happen to sit within edit-distance budget
of each other (a single letter + case). §44's ratio-alone heuristic is evidently not sufficient by itself to
distinguish "this is almost certainly the intended typo-correction of a rare misspelling" (its original,
correct use) from "these are two entirely different, unrelated real words, one of which is just far more common
than the other" (this new false positive) - needs a sharper condition, not a bigger ratio or a word-specific
exception. Candidate angle to explore: a same-length/case-differs-only correction relative to a real known word
is much more likely to be this second, illegitimate class than a genuine spelling typo, and might need its own,
stricter rule (or outright exclusion from the override) rather than reusing the exact same ratio threshold as a
plain misspelling.

#### D-114 - "Vorhin" → "Virgin"/"vorhinein": Missing Dictionary Entry + No "Genuinely Ambiguous, Don't Correct" Gate
Checked directly against `dict_de.tsv`: **`vorhin` itself does not exist in the dictionary at all.** The only
near matches are `Virgin` (freq 62, NOUN - an English proper-noun artifact of the Wikipedia-derived corpus,
almost certainly from articles mentioning Virginia/Virgin Islands/the Virgin brand) and `Vorhinein` (freq 11,
NOUN, capitalised - itself a likely POS/casing-heuristic miscategorisation, since "vorhinein" is an ordinary
lower-case adverb in real German, not a proper noun; its low sample count plausibly let sentence-initial
capitalised occurrences dominate the "canonical surface case" heuristic - see A-03's build notes). Two separate
fixes fall out of this one report: (1) a straightforward dictionary-data gap (add `vorhin`, fix `Vorhinein`'s
casing/POS), and (2) the more general, more valuable fix - **there is currently no "none of the candidates is a
clearly dominant winner, so don't silently autocorrect at all" gate**. Even with the data gap fixed, the
underlying mechanism accepted a correction here despite two candidates of comparable low frequency (62 vs. 11)
and no dictionary entry for the actually-intended word - exactly the "keine Eindeutigkeit" complaint that
recurs across several of this round's reports (see also D-111-adjacent cases and D-113). Also flags a data-
quality question worth answering once the dictionary build is revisited: why does a rare English proper noun
like `Virgin` outrank a real German adverb form in the first place, and should the build pipeline filter or
down-weight low-value foreign proper-noun noise picked up incidentally from the German Wikipedia dump (ties
into D-106's broader multi-language-dictionary discussion).

### Theme C - Dictionary Coverage / Morphology

#### D-115 - "merke" → "Marke", "stimmen" → "Stimmen": Missing Verb-Inflection Entries, Not a Ranking Bug
Checked directly against `dict_de.tsv` - **neither `merke` nor `stimmen` (the verb forms) exist as their own
dictionary rows at all**; only `Marke` (freq 578, NOUN) and `Stimmen` (freq 2748, NOUN) do. Since the typed
word is not a known word in either case, A-01 never even engages - ordinary autocorrect just finds the nearest
known candidate (a one-letter substitution plus a case change, well within budget) and applies it, with nothing
to override. This directly answers the question raised alongside these two reports: **does the dictionary keep
both the noun and the verb form of a case-homograph, or do they collapse into one?** Confirmed: **they
collapse** - per A-03's build notes, part-of-speech is derived from casing statistics on a single,
case-insensitive dictionary key, with one "canonical surface case" chosen as whichever casing was more frequent
in the source corpus; the "mixed → ambiguous `{NOUN, OTHER}`" case the build is documented to detect apparently
still keeps only one *surface form* (one dictionary row) per key even when it flags the ambiguity, discarding
the minority-cased form's own separate existence as a known word entirely - exactly the gap the user suspected,
and now confirmed with real data rather than assumed. At minimum this needs the dictionary build to record that
*both* forms exist (the user's own suggested floor: "mindestens muss dort ein Vermerk stehen, dass es beide
gibt") so A-01 can protect the minority form even if only one surface spelling is ever offered as a completion;
ideally both surface forms become independently known words. This is a data/build-pipeline change
(`scratchpad/build_dict.py`, outside the shipped app), not a runtime logic fix - likely needs re-running the
dictionary build with the collapsing behaviour changed, not just patched at read time.

#### D-116 - Compound-Word/Fragment Recognition ("Beitragsjahren")
Typing "Nach 45 Beitragsjahreb" (a typo of "Beitragsjahren") is not recognised or corrected - plausibly because
the intended whole word does not exist in the dictionary at all (an uncommon compound, "Beitragsjahre" +
dative-plural "-n"), not because of any correction-quality bug. Raised by the user as a concrete example of a
broader idea: could AdaptKey recognise/correct compound words (or fragments of them) it has never seen as a
whole unit, by decomposing them the way B-01 already does for explicitly hyphenated compounds, but for
unhyphenated ones? This is a substantial, open-ended feature idea (German compound-splitting without a hyphen
to anchor on is a much harder problem than B-01's hyphen-segmented case), not a quick fix - captured here as a
question to think about, not a committed design.

#### D-117 - Words Recognised Too Late for a Garbled Prefix ("erkamm" → should already hint "erkannt")
General complaint: recognition/suggestion often kicks in far later than it could. Concrete example: typing
"erkamm" (multiple edits away from "erkannt": not a single adjacent-key substitution, more than one character
differs) should arguably already surface "erkannt" as a likely candidate at that point, the way a human reader
would guess it immediately. Flagged by the user themselves as a genuinely open question - is this achievable
with the existing tier-1 edit-distance/proximity approach at all, or does meaningfully "reading past" this many
simultaneous typos really need the tier-3 mini-LLM (still device-pending, see the "Remaining" section of
progress.md) rather than a tier-1 rule tweak? No commitment either way yet - needs a concrete decision on how
much fuzz budget/ranking change is worth trying at tier-1 before concluding this is genuinely LLM-only territory.

### Theme D - Investigated, Not Reproducible as Described

#### D-118 - Single "c" → "x": Not Reproducible in the Current Code as Described
Traced directly: `DictionarySuggestionProvider.bestCorrection()` has a hard `MIN_AUTOCORRECT_LENGTH = 2` floor
(`DictionarySuggestionProvider.kt:205,254`) that rejects any token shorter than 2 characters *before* any
candidate search, known-word check, or A-01 guard even runs - a standalone, single-character "c" cannot reach
autocorrect at all via this path. (For what it's worth, `KeyboardProximity`'s QWERTZ adjacency map does list `c`
and `x` as neighbours - `"yxcvbnm"` - so if this were reachable, `x` would indeed be a cheap substitution
candidate; it just isn't reachable as described.) Reported back rather than captured as a blind fix target -
needs a sharper repro to make progress: was the `c` truly a standalone token (bounded by spaces/punctuation on
both sides), or part of a longer word where a `c`-containing segment got corrected and only looked like an
isolated "c" in the moment it was observed?

### Theme E - Mid-Word Correction (§58) Follow-On Bugs

All four items below were traced directly against the current `AdaptKeyService.kt` and are confirmed causes,
not guesses - they all sit in or immediately around the same mid-word-editing mechanism §58 introduced
(`reclaimWordAtCaret()`, `composingCursor`, `handleShift()`), which makes sense as a cluster: §58 made "the
caret is sitting somewhere inside an existing word" a much more common, load-bearing state than it used to be,
and several other code paths were apparently never updated to account for the caret *not* being at the
composing token's end while that state is active.

#### D-119 - SPACE Mid-Word Wrongly Finalises Instead of Inserting a Literal Space
Repro: "Das ist ein Test.nächstes Wort", caret placed right after the period (mid-word relative to
`nächstes`, not at its end) specifically to insert a missing space there - pressing SPACE does not insert a
space at all; instead the whole word gets accepted/finalised and the caret jumps to its end. Confirmed:
`handleKey()`'s `KeyCode.SPACE` branch calls `finalizeAndCommit(ic, " ", ...)` **unconditionally** whenever
`composing` is non-empty, with no check of `composingCursor` against `composing.length` at all - so SPACE
always finalises the *entire* composing token as if the user had just finished typing it, regardless of where
in the word the caret actually is. The user's own generalisation looks correct: SPACE is probably never the
right trigger for "finalise this word" unless the caret genuinely sits at the word's own end (or, per the
user's more cautious floor, at least requires a letter to have just been typed at that position first) - a
mid-word SPACE should instead behave as an ordinary literal-space insertion that splits the token in two.

#### D-120 - Mid-Word Punctuation/Symbol Insertion Lands at the End of the Word, Not at the Caret
Repro: type "Ein", place the caret before the `E`, type a `"` (double quote, via a key's long-press
alternative) - the quote ends up *after* "Ein" (i.e. "Ein\""), not before the `E` where the caret visibly was.
Confirmed: `commitLongPressSymbol()`'s non-letter path (and `finalizeAndCommit()` generally, so this is not
specific to quotes or to long-press) commits the finalised word via
`ic.setComposingText(finalWord, 1)` - Android's `newCursorPosition = 1` places the cursor **after the entire
composing text**, unconditionally - and only then commits the delimiter at that (now end-of-word) cursor
position via `ic.commitText(delimiter, 1)`. There is no code path that commits a delimiter at the caret's
actual, pre-existing mid-word position (`composingCursor`) - every delimiter commit assumes "the caret is at
the end of what I'm about to finalise", which §58 broke the moment mid-word editing became a first-class,
common state.

#### D-121 - `handleShift()` Always Prefers the G-05 Word-End Gesture Over Caps-Lock-Off, and Doesn't Check the Caret Is Actually at the Word's End
Two reported symptoms trace to the exact same code (`AdaptKeyService.handleShift()`, one function), so this is
one bug with two triggers, not two:

1. **Backspace mid-word, then Shift, wrongly re-triggers G-05.** Repro: type "Verbinder", caret after the `V`,
   Backspace (removes the `V`, leaving "erbinder" with the caret now at the token's *start*, not its end), then
   Shift (intended, per the user, as an ordinary case toggle) - the following `e` gets capitalised
   unexpectedly. Confirmed: `handleShift()`'s very first check is `if (composing.isNotEmpty())`
   (`AdaptKeyService.kt:2147`) - not a comparison of `composingCursor` against `composing.length` - so *any*
   non-empty composing token routes straight into `handleWordEndShift()` → `flipFirstInComposing()`, which
   always flips `composing[0]` (the string's first character) regardless of where the caret actually sits.
   There is no "is the caret genuinely at this token's end" check anywhere in this path.
2. **Caps Lock, mid-word, Shift-to-turn-off wrongly re-triggers G-05 instead.** Repro: engage Caps Lock, type
   "MCU" (no Space yet - `composing` is still non-empty), press Shift meaning only to release Caps Lock -
   confirmed: the same `composing.isNotEmpty()` check at line 2147 is evaluated **before** the
   `view.capsLock` check further down the same function (lines 2153-2158), so the Caps-Lock-release branch is
   never reached at all while composing is non-empty; the G-05 word-end gesture fires instead, and Caps Lock
   silently remains engaged for the rest of the input.

Both symptoms need the same underlying fix: `handleShift()` needs to (a) check Caps-Lock-off intent before
falling into the G-05 branch, not after, and (b) gate the G-05 word-end gesture on the caret genuinely being at
the composing token's own end (`composingCursor == composing.length`, or equivalent), not merely on
`composing` being non-empty - the same kind of caret-position blind spot D-119/D-120 above already found in two
other call sites.

#### D-122 - Mid-Word Correction Should Raise Word-Split Priority When an Unresolved c/v/b Connector Is Detected
When A-05's word-splitting fails to catch a stray `c`/`v`/`b` (etc.) that should have been a space, the token is
left stuck together with the wrong letter as an accidental connector - the request is that entering *mid-word*
correction on such a token (§58) should recognise this specific shape (an otherwise-unresolved
`TokenRepair.OVER_SPACE_LETTERS` character sitting inside the composing token) and give the split candidate
noticeably higher priority than it would otherwise get, so the user gets an easy way to split the words apart
without much extra effort once they've gone back to fix it. Not yet investigated against
`TokenRepair`/`refreshSuggestions()`/the existing §49 live-split-preview machinery (which already calls
`trySplit()` continuously while composing, just for colouring, not yet as a promoted suggestion) - captured as
a design direction to explore together with an implementation pass, not a traced bug.

### Theme F - Suggestion-Accept vs. Immediately-Typed Punctuation

#### D-123 - Suggestion-Bar-Tap's Trailing Space May Not Survive to the Punctuation Check
D-29 (already implemented, v0.7.10) is supposed to cover exactly this: accepting a suggestion adds a trailing
space, and typing punctuation immediately afterward should eat that space. Traced the current wiring rather
than assuming it's simply missing: `onSuggestionClicked()`'s bar-tap path is the *only* site that arms
`pendingSuggestionSpace = true` (autocorrect-on-commit doesn't need to, since it commits the delimiter directly
without an extra space) and `finalizeAndCommit()`'s `composing.isEmpty()` branch is the only site that checks
and consumes it. A plausible gap: `onSuggestionClicked()` calls `commitText()` *then* `clearComposing()` *then*
sets the flag - but `commitText()` on a collapsed selection triggers the framework's `onUpdateSelection()`
callback, which (composing already empty at that point) unconditionally calls `reclaimWordAtCaret()`
(`AdaptKeyService.kt:571`), and `reclaimWordAtCaret()` resets `pendingSuggestionSpace = false`
(`AdaptKeyService.kt:606`) as one of its first steps, regardless of whether it finds anything to reclaim. If
that callback lands - as such callbacks normally do, asynchronously - before the user's next keystroke, D-29's
flag is cleared before the punctuation check ever gets a chance to run. **Plausible and code-grounded, not
runtime-confirmed** (no emulator in this environment, and the actual callback timing here is exactly the kind
of thing §32/§46 already found could only be confirmed on a real device) - worth checking first, before
assuming D-29 needs a redesign rather than this one ordering fix.

## §64 - Batch Release: D-105, D-108, D-109, D-111–D-115, D-119–D-121, D-123 (v0.8.35)

Released together per explicit user instruction, following the §63 discussion. **D-118 struck outright** (not
deferred) - confirmed not reproducible as described, no further repro was forthcoming. **D-122 deliberately not
implemented this round** - it was captured as an unconfirmed design idea, not a traced bug, and this round's
priority was items already root-caused; still open, needs a concrete repro/trace before an implementation
attempt, consistent with this project's "trace before fixing" rule.

### D-105 - Every Main-Page Digit Key Gets a Superscript Second Alternative
`KeyboardLayout.rows()`/`GreekLayout.rows()` both gained a `numberKey(c)` builder: every digit's existing
shifted-symbol hint (L-06) stays index 0 (pre-selected, unchanged behaviour for a straight long-press), with
its own superscript form (`¹²³⁴⁵⁶⁷⁸⁹⁰`) added as index 1 - every digit now opens a real D-01 two-cell popup
instead of applying its shifted symbol immediately. `0` sits at the number row's right edge - the same problem
§34 fixed for the letters page's `p` key - so its list is reversed at this one call site (not the shared
constants), keeping its own glyph nearest the finger once its popup clamps leftward. Applied identically to
both the German/English Latin layout and the Greek layout, since L-06's number row is otherwise shared/mirrored
between them. 4 new tests (`KeyboardLayoutTest`, `GreekLayoutTest`).

### D-108 - Long-Press Popup Now Tolerates a Small In-Key Smear
`AdaptKeyboardView` gained `pressedKeyRect` (the pressed key's own bounds, captured on `ACTION_DOWN` alongside
`pressedKey`) and a new `movedOutsideKey(x, y)` check. `ACTION_MOVE` now cancels a pending long-press only once
the touch has actually left the pressed key's rect, not once it clears the much smaller system touch-slop
(`movedBeyondSlop`) - which could cancel a long-press well before the finger left the key at all. Backspace-hold
cancellation is deliberately left on the original, tighter slop-based check (`movedBeyondSlop`), since a swipe
on backspace is a distinct, deliberate G-02 word-delete gesture that should still be recognised promptly, not
tolerated as a "smear". Android touch-handling glue, no new test - consistent with this project's documented
testing gap (no emulator/device here), same class as D-64's touch-arbitration fix.

### D-109 - T-03 Learning Now Has Bounds
Two independent changes, both aimed at the reported drift (bottom-row keys creeping toward space; the user's
own `j` key drifting too far left):
1. **Ambiguous taps no longer train the model at all.** `AdaptKeyboardView.onTouchEvent()`'s `ACTION_DOWN`
   branch now runs the T-05 `ambiguityBands.classify(...)` call *before* `offsetModel?.record(...)`, and only
   records the tap when `pendingAmbiguity.kind == TapAmbiguity.NONE`. A tap flagged `SPACE_AMBIGUOUS` (a
   bottom-row letter that might really have meant space) or `LETTER_AMBIGUOUS` (the reverse) is, by
   definition, not confidently the key it resolved to - training that key on it anyway was reinforcing exactly
   the wrong lesson every time a mistap got silently corrected elsewhere (T-05/A-05). This directly targets the
   reported bottom-row-into-space drift at its source, rather than trying to bound it after the fact.
2. **`OffsetModel.DEFAULT_MAX_OFFSET_FACTOR` tightened from 0.9 to 0.5** - the existing cap on the learned mean
   offset, as a fraction of a key's half-size. 0.9 let a learned strike point sit within 10% of a key's own
   edge, effectively crowding into the neighbouring key's territory; 0.5 is a more conservative starting point,
   not yet device-tuned further (a single constant, easy to retune later, same precedent as several other
   threshold tunings this project has needed). Also: `AdaptKeyboardView.drawTouchModel()` (the D-24
   visualisation) previously drew the *raw, uncapped* `Spread.meanDx`/`meanDy` as the strike-point centre - so
   the very visualisation the user was judging the drift from could show a **more extreme** drift than the
   model's own `resolve()` would ever actually apply (which already caps it via `logLikelihood()`). Fixed by
   having the visualisation use `OffsetModel.cappedMeanOffset()` (newly exposed `maxOffsetFactor` property) with
   the same per-key bound `logLikelihood()` computes, so the visualisation now accurately reflects what the
   model will really do. `spreadFor()` itself is left returning raw, uncapped statistics unchanged - its
   existing tests (`OffsetModelTest`, `CalibrationActivityRoboTest`, `OffsetStoreRoboTest`) rely on that exact
   contract for verifying the underlying sufficient statistics, independent of any display-time capping.

### D-111 / D-112 - A Pending Capitalisation Change Is Now Visible Before It Applies, in the Correct Case
`AdaptKeyService.refreshSuggestions()`'s `pending` (the S-06 "impending autocorrect" signal, previously only
ever a spelling correction from `provider.autocorrectFor()`) now runs that correction candidate (or the raw
input, if none) through the *same* §6 `capitalisation.capitalise(...)` call `finalizeAndCommit()` will
eventually apply, using the *already-tracked* live context (`contextFor(input)` - sentence-start/field-mandate
state that already exists per-token, no new tracking needed). Reusing the *existing* S-06 mechanism
(`SuggestionController.update(input, candidates, pending)`, unchanged) means no new UI code was needed at all:
- **D-111**: an ordinary noun about to be auto-capitalised (§6 rule 3/4) - previously invisible until the
  delimiter silently applied it - now shows as the S-06 verbatim/pending chip pair *while still composing*.
- **D-112**: a correction whose own case must follow the sentence/field context ("Fur" at a sentence start) now
  previews as "Für", not the dictionary's plain canonical "für" - the capitalisation transform runs on the
  *corrected* candidate, not only ever on the raw typed text.

Deliberately narrower than a byte-perfect preview of `finalizeAndCommit()`'s full pipeline: only
`autocorrectFor()`'s own candidate search is mirrored (umlaut/diacritic restoration is already free within it,
cost 0 once folded - see D-48/D-63), not the rarer raw-coordinate-correction fallback (D-39, needs real
composing taps/geometry, not worth computing every keystroke for a preview) or the §9 tier-3 `llmForcesUpper`
exception (defaults to `false` in the preview; the real commit-time value still applies correctly at commit).

**Heads-up, not yet device-tested**: because German capitalises essentially every common noun, this makes the
S-06 chip pair appear far more often than before - literally on most nouns typed lowercase, not only the
ambiguous/surprising cases that originally motivated the request. This is the deliberate, direct consequence of
what was asked ("es darf nicht überraschend geschehen, ohne je in den Vorschlägen aufzutauchen") rather than an
oversight - flagged clearly since it is a real, broad change to how often the suggestion bar's S-06 pair shows
up, worth confirming feels right in everyday typing rather than noisy, the same way several other features in
this project were refined after real-device feedback.

### D-113 - §44's Ratio-Override Restricted to Genuine Cost-1 Typos
`DictionarySuggestionProvider.bestCorrection()`'s A-01 override check now additionally requires
`best.cost <= ADJACENT_SUB_COST` (a single neighbouring-key substitution) before `shouldOverrideKnownWord()`'s
frequency-ratio check is even consulted - previously the ratio alone (`shouldOverrideKnownWord`, unchanged) could
override A-01 for *any* candidate within the full two-edit `MAX_CORRECTION_COST` budget. Verified against the
real bundled `KeyboardProximity` map: "due"→"die" and "ddr"→"der" (§44's original motivating cases) are both
cost-1 (u/i and d/e are QWERTZ-adjacent) and are unaffected; "spreche"→"Sprache" (this round's reported false
positive) is cost-2 (e/a are not adjacent) and no longer overrides, despite an even more extreme frequency ratio
(≈147x) than either original case. 2 new tests.

### D-114 - A Low-Confidence Autocorrect Candidate Is Never Silently Applied + the Missing Word Added
Two independent parts:
1. **New `minAutocorrectFrequency` constructor parameter on `DictionarySuggestionProvider`** (default `0`, so a
   plain `DictionarySuggestionProvider(store)` - including the shared test-suite instance - is entirely
   unaffected): a correction candidate below this absolute frequency is dropped from `bestCorrection()`'s
   candidate search outright, however good its edit cost otherwise looks. `AdaptKeyService`'s two real
   construction sites now pass `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY = 300L`, calibrated against the bundled
   `dict_de.tsv`: every legitimate correction target already relied on this session sits far above it
   (komplett 881, Sankt 968, Standard 1534, kleinen 3748, Wort 4084, können 23227, werden 93866), while the
   reported bad candidates sit far below it (Virgin 62, Vorhinein 11) - directly fixes "vorhin"→"Virgin"
   without needing any per-word special case. A candidate below the floor can still appear in the broader
   suggestion-bar prefix/fuzzy list (`suggestionsFor`, unaffected) - only the *silent autocorrect* path is
   gated. Deliberately a constructor parameter rather than a hardcoded constant: an absolute, corpus-scale
   floor would have broken over a dozen existing tests that use small, illustrative synthetic frequencies (40,
   50, 80, 200...) to test *relative* ranking, not realistic absolute frequencies - making it opt-in keeps that
   whole existing test suite meaningful unchanged, while production explicitly opts into the real floor. 2 new
   tests, including one confirming the no-floor default is unaffected.
2. **`vorhin` added to `dict_de.tsv`** (frequency 900, `OTHER`) - confirmed genuinely absent from the bundled
   dictionary (only `Virgin` and the correctly-capitalised `Vorhinein`, an "im Vorhinein" idiom, existed
   nearby). Calibrated against comparable temporal adverbs already in the corpus (`vorher` 1437, `jetzt` 1499)
   - a hand-inserted, clearly-documented data patch (no dictionary-build pipeline exists in this environment to
   regenerate a real corpus-derived frequency, see D-115 below), not a re-run of the actual build.

### D-115 - Two Different Root Causes, Not One: a Ranking Bug vs. a True Data Gap
Split into what the evidence actually showed, not treated as one problem:
- **"merke"→"Marke"**: `merke` (the verb form) was genuinely absent from `dict_de.tsv` entirely - not a
  case-collapse issue (the words differ by more than case: `merke` vs. `Marke`), a plain coverage gap. Added
  `merke	65	OTHER` next to its conjugation siblings (`merkt` 79, `merken` 72, `merkte` 56 - all similarly rare
  in this Wikipedia-derived, mostly-third-person corpus), comfortably below `Marke`'s 578 but a small enough
  ratio (≈8.9x, far under §44's 50x) that A-01 protects it outright once it exists as a known word at all - no
  override, no special rule needed.
- **"stimmen"→"Stimmen"**: **not** the same mechanism, and **not** something the §63-discussed case-only
  correction idea could ever have fixed - traced end-to-end (see §63's own correction note) to
  `CapitalisationEngine.capitalise()`'s §6 rule 3 (pure noun → auto-capitalise) trusting `store.partsOfSpeech()`,
  itself a case-insensitive lookup. `Stimmen` was tagged plain `NOUN` in the dictionary, not the documented
  "mixed → ambiguous `{NOUN, OTHER}`" case the build is supposed to produce for exactly this kind of
  case-homograph (the verb "stimmen" and the noun "Stimmen" are both genuinely common) - so §6 rule 5
  ("ambiguous: no auto-capitalisation, offered as a suggestion instead" - which **already exists and already
  does exactly what was wanted**) never got a chance to fire. Fixed with a one-line data change:
  `Stimmen	2748	NOUN` → `Stimmen	2748	NOUN,OTHER` in `dict_de.tsv`, directly answering the question raised in
  discussion (does the dictionary keep both forms, or do they collapse? - confirmed: they collapse into one
  row/one POS set, and the fix is exactly the "at least a marker that both exist" floor proposed) - no runtime
  logic changed at all for this one; §6's rule 5 now reaches it because the data correctly says "ambiguous".
- **Ablaut/irregular-verb generation, and any broader dictionary rebuild** remain explicitly deferred - noted in
  §63, not touched this round. No dictionary-build pipeline exists in this environment (`scratchpad/build_dict.py`
  was a throwaway dev tool, never committed) - both data edits above are hand-inserted, clearly-documented
  patches to the shipped TSV assets, not a re-run of the actual Wikipedia-corpus build.

### D-119 - SPACE Mid-Word Now Inserts a Literal Space Instead of Finalising the Whole Token
New `AdaptKeyService.splitComposingAtCaretAndCommit(ic, delimiter, spaceInferred)`, invoked from inside
`finalizeAndCommit()` itself (see D-120 below - the same fix covers both) whenever `composingCursor !=
composing.length`. Splits the composing token in two at the caret: the "before" half is finalised exactly like
an ordinary word (autocorrect, capitalisation, committed with the delimiter) via a **recursive**
`finalizeAndCommit()` call; the "after" half becomes a fresh composing token, re-seeded at the caret's own new
position via the same `composingAnchor`/`composingCursor` mid-word-edit-point mechanism `reclaimSurroundingWord()`
already established (D-62/§58) - so editing continues right where the caret was, never jumped to the token's
end. The real composing region is shrunk to just the "before" text first (`ic.setComposingText(beforeText, 1)`)
before any of this, so finalising it does not silently replace the *whole* existing before+after composing span
and discard the "after" half, which exists only in memory until re-seeded. `pendingMergeChar` is explicitly
restored before delegating, so the recursive "before" finalisation still sees it correctly (an edge case not
otherwise reachable, kept consistent rather than silently dropped). Android service glue, no new test -
consistent with this project's established `AdaptKeyService` testing gap (no `InputConnection` Robolectric
shadow, see §46).

### D-120 - Mid-Word Punctuation Now Lands at the Caret, Not Always at the Token's End
Same fix as D-119, generalised: the mid-word check now lives *inside* `finalizeAndCommit()` itself (moved from
being SPACE-specific), so **every** delimiter that funnels through it - typed punctuation, a long-press symbol
via `commitLongPressSymbol()` (this round's reported repro: a quote typed before an existing word, via long-press,
previously landed after it) - is fixed for free, with no change needed at any of those call sites. Root cause
confirmed via trace: `ic.setComposingText(finalWord, 1)` always places Android's cursor *after* the whole
composed text (`newCursorPosition = 1`), regardless of where the logical caret actually was - `finalizeAndCommit()`
never had a concept of "the caret is mid-token" before this round at all.

### D-121 - `handleShift()` Now Checks Caps-Lock-Off First and Requires the Caret to Genuinely Be at the Word's End
One bug, two reported repros (a mid-word Backspace-then-Shift wrongly re-triggering G-05; a Caps-Lock-off Shift
press mid-word wrongly re-triggering G-05 instead of releasing Caps Lock) - both traced to the same function.
`handleShift()` reordered so the Caps-Lock-release branch (D-15) is checked *before* the G-05 word-end branch,
and the G-05 branch itself now requires `composingCursor == composing.length` (genuinely at the token's own
end), not merely `composing.isNotEmpty()` - a mid-word Backspace can leave composing non-empty with the caret
elsewhere, which previously still (wrongly) triggered `handleWordEndShift()`'s first-letter flip. Android
service glue, no new test - same established gap as D-119/D-120.

### D-123 - Suggestion-Bar-Tap's Trailing Space Now Survives the Punctuation Check
New one-shot guard field `suppressNextReclaimSpaceReset`: set alongside `pendingSuggestionSpace = true` in
`onSuggestionClicked()`'s `Kind.NORMAL` branch, consumed (and cleared) by `reclaimWordAtCaret()` instead of that
function's previous unconditional `pendingSuggestionSpace = false` reset. Confirmed via trace (§63) that a
suggestion-bar tap's own `commitText()` generates an asynchronous `onUpdateSelection` callback which - composing
already empty by then - calls `reclaimWordAtCaret()` as a plain side effect of its *own* commit, not a genuine
subsequent tap elsewhere; without the guard, that call cleared D-29's space-eating flag before the user's next
keystroke (the punctuation mark it exists to react to) ever arrived. Scoped to exactly one reclaim call, so a
genuine subsequent caret move (a real tap elsewhere) still resets the flag normally, same as before. Android
service glue, no new test - same established gap.

567 unit tests (was 560; +7: 4 `KeyboardLayoutTest`/`GreekLayoutTest` D-105 cases, 3
`DictionarySuggestionProviderTest` D-113/D-114 cases). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
**None of this round's items have been confirmed on a real device yet** - several (D-108, D-109, D-111/D-112,
D-119-D-121, D-123) are Android view/service glue this environment cannot exercise end-to-end (no
emulator/device, no `InputConnection` Robolectric shadow, per the established, repeatedly-documented limitation
of this environment).

## §65 - Backlog Round 17: Post-§64 Device Feedback

Captured, **not started**, per the usual rule - the user explicitly asked for these to be collected now, with
implementation planned as a separate, later round.

### D-124 - §60's Clipboard MIME-Type Filter Doesn't Actually Block a Non-Text File (Bug, Re-Opened)
§60 (v0.8.33) was left deliberately unconfirmed pending broader real-world use - this is that use, and it
surfaced a real failure: copying the app's own APK file offers it via the D-36 Quick Paste chip, which
`resolveClipboardText()`'s `clip.description.hasMimeType("text/*")` check should have suppressed entirely (an
APK is `application/vnd.android.package-archive`, not `text/*`). Not yet re-traced against the current code -
possible leads for the next pass: whether the specific source app (wherever the APK was copied from) declares a
`ClipDescription` MIME type at all, whether it reports something unexpected `hasMimeType` still matches, or
whether the `item.uri == null` fast path (plain-text items, unconditionally allowed) is firing instead of the
URI branch for this particular copy for some reason.

### D-125 - An Unknown Inflected Verb Form Must Not Be Silently "Corrected" to the Dictionary's Base Form
Reported: "beurteilst" (2nd person singular of "beurteilen") auto-corrects to "beurteilt" (a different real
inflected form, 3rd person singular / past participle) - the same class of bug as D-115's "merke"/"stimmen",
but framed as a systemic morphology gap rather than one more missing word. The user's own proposed scope: **not**
every inflection of every verb needs to be in the dictionary (95% of the value, per their own estimate, doesn't
need that) - but the regular, highly-productive German conjugation *patterns* (weak/regular verb endings:
-e/-st/-t/-en/-t/-en present tense, the regular preterite/participle patterns) should be recognised as
"plausible enough to leave alone" even when the exact surface form isn't a dictionary entry, without necessarily
being *offered* as a suggestion. This is a generalisation of D-115's fix (which patched two specific words) into
an actual rule: something that recognises "this token is a regular conjugation of a stem the dictionary already
knows" and extends A-01-style protection to it, distinct from (and a real alternative to) a full corpus rebuild
or hand-adding every inflection. Not yet designed - needs its own pass on what "regular enough" means precisely
and where in the pipeline (a new check alongside `isKnownWord`, most likely) this would plug in.

### D-126 - Tier-3 Mini-LLM Needs an Uninstall Action and an Independent Enable/Disable Toggle
Once a model has been imported (`Tier3ModelActivity`/`Tier3ModelInstaller`/`Tier3ModelStorage`), there is
currently no way to remove it again, and no toggle to turn the LLM off/on independently of whether a model file
happens to be present - "installed" and "active" are currently the same state. Two asks: (1) a settings action
to delete the imported model file (freeing the ~270+ MB it occupies) and (2) a switch that disables tier-3
inference even while a model remains installed (e.g. to save battery/latency without having to re-import later).
Ties into `Tier3ModelStorage`/the C-06 settings plumbing - not yet scoped against the actual current code.

### D-127 - Document "What Learns When" (Dev Docs)
Requested as its own task: a clear, consolidated write-up of exactly which user actions feed which learning
mechanism and when (T-03 offset model per confirmed tap per D-109's new ambiguous-tap exclusion; D-37's
count-based dictionary promotion/un-learn on undo; T-04 pattern seeding; the tier-3 adaptive-learning signal,
§9). This is meant to land in the project's own docs (this spec/progress file), not user-facing copy - a
reference for future sessions and for writing D-128's user-facing summary accurately.

### D-128 - Settings Needs a Prominent, Compact Explanation of What Adapts and Learns
The app is named **AdaptKey** - the user wants the settings screen to prominently (but compactly) explain what
justifies that name: which actions teach the keyboard something and how, explicitly covering the T-03 raw-touch
model and the tier-3 mini-LLM's adaptive-learning fade-out (§9 - the LLM is consulted less as the n-gram model
learns from it). Possible shape floated by the user: extend the existing D-89 `FeatureOverviewActivity` ("Was
AdaptKey alles kann") with a short, dedicated summary of these core learning/adaptation features, rather than
building an entirely separate screen. Depends on D-127's consolidated internal write-up existing first, so the
user-facing summary is accurate rather than reconstructed from memory.

### D-129 - Calculator's Minus Key Needs a Corner-Hint Glyph for Its Sign-Flip Function
§31 (v0.8.10) added the sign-flip-on-long-press feature to the calculator page's `−` key, explicitly accepting
"no corner-glyph visual cue" as a trade-off at the time (the key carries neither a `hint` nor `alternatives`, so
routing it through the ordinary hint-drawing path would have wrongly turned the gesture into a text-commit
action - see §31's own write-up). Revisiting that trade-off: the user now wants some corner indicator so the
long-press function is discoverable, without accidentally routing it back through the commit-text machinery
that was the reason it has none today. Needs its own small design pass (a corner glyph driven by a condition
other than "has a hint/alternatives", most likely).

### D-130 - A Real Language Switch After Sustained English, Plus a Space-Bar Acknowledgement Animation
Two related asks:
1. **Promote the A-03 per-token English routing to a real D-106-stage-1 active-language switch after
   sustained English input.** Today, typing English inline while German is active only ever routes
   individual tokens to the English dictionary (A-03's classifier, `LANGUAGE_WINDOW = 5`) - it never actually
   flips `activeLanguage` the way the G-01 swipe does. The user observes this already works well for the one
   thing it was built for (D-106 stage 2's cross-language autocorrect suppression), but questions whether the
   current "soft" per-token routing is still worth keeping *as well* now that stage 2 exists, and proposes:
   after **five** consecutive English words (not the classifier's own 2-word window), perform a genuine
   `LanguageCycle`-style switch to English as the *active* language, not just a per-token dictionary
   substitution. This is a real behavioural change to A-03/D-106's interaction, not yet designed - needs
   deciding whether this replaces or layers on top of the existing classifier routing.
2. **Acknowledge every language change - automatic or manual (G-01 swipe) - with a visible animation on the
   space bar**, not just the current text label swap. Two alternatives floated: a glow sweep travelling
   bottom-to-top across the space key's text, or a fade-out of the current label followed by a fade-in of the
   new one. Purely a visual-polish request ("sieht nett aus und ist auffällig") - no functional change beyond
   making the existing D-03 space-bar label swap more noticeable.

### D-131 - Raw-Coordinate Correction Should Become Visible Earlier While Still Typing
D-39's raw-coordinate correction is confirmed working well now, but currently only ever applies at
`finalizeAndCommit()` time (as the last fallback, once ordinary edit-distance autocorrect finds nothing) - it
has no live presence during composing the way the ordinary suggestion list and the §49 split preview do. The
user wants its effect visible earlier - ideally before the last letter of the word is even typed - so a
raw-coordinate respelling becomes a live, incremental confidence signal rather than something that only ever
resolves at the final delimiter. Not yet designed - would need `refreshSuggestions()` to also consult
`RawCoordinateCorrection.respellings(...)` against the in-progress token (mirroring how D-111/D-112 already
pulled a piece of `finalizeAndCommit()`'s own logic forward into live suggestions), which needs `composingTaps`
data for the token-so-far rather than the whole finished word.

### D-132 - Settings-Row Slide-In Animation Needs Polish
§50's `SettingsRowView` currently reserves its height immediately and animates only its inner content via
`translationY` - per the user, this reads as the row and its buttons "popping in" instantly rather than
sliding, which "looks odd". Requested: the buttons together with the whole row should slide up from below as
one piece, or - the user's own pragmatic alternative - achieve the same visual via a clip-based reveal rather
than a translation animation. Needs its own implementation pass against the current `SettingsRowView.open()`.

### D-133 - Bottom-Row Touch Zones Need a Harder, Direction-Specific Bound (D-109 Follow-Up)
D-109 (§64) already stopped ambiguous taps from training the model at all and tightened the general
`maxOffsetFactor` cap from 0.9 to 0.5 - the user wants a stronger, explicitly *directional* guarantee on top of
that general isotropic bound specifically for the bottom row (`c v b n m`): its keys' effective zones must never
be allowed to drift downward past the vertical middle of the key, keeping a hard, guaranteed margin against the
space bar regardless of how the general cap is tuned. This is a stricter, axis-specific version of D-109's
existing fix, not yet designed against `OffsetModel`/`logLikelihood()`'s current isotropic-fraction cap.

### D-134 - SPACE Sometimes Seems to Get Swallowed After a Full Stop (Unconfirmed Pattern)
The user notices a recurring pattern - pressing SPACE right after a `.` sometimes has no visible effect - but
is not fully certain it isn't their own mis-tap, and has not pinned down a precise repro yet. Flagged as
"never happened on Gboard". Possible leads for whenever this gets a proper trace: interaction with D-29's
space-eating-punctuation logic (though that only ever removes a space *before* punctuation, not after), the
auto-capitalisation-arming that follows a sentence-ending period, or an interaction with this round's own new
D-119/D-120 mid-word-split logic (shipped the same week this pattern was first noticed) - genuinely unknown
which, if any, of these is responsible without a precise repro.

### D-135 - Password-Manager (Autofill) Suggestions in the Bar - Big Idea, Feasibility Unknown
Requested: when typing into a recognised username/email field, offer a saved username from the Google Password
Manager as a suggestion; in a recognised password field, offer the matching saved password. The user
explicitly flags their own uncertainty about feasibility ("ich weiß nicht, wie Gboard das genau macht... 
vielleicht benötigen wir hier Schnittstellen, die wir nicht haben"). This is almost certainly the Android
**Autofill Framework** (`InlineSuggestion`s an IME can render inline in its own suggestion strip, the mechanism
Gboard is known to use for exactly this) rather than anything the keyboard could build unassisted - needs
dedicated research into that API's actual capabilities/constraints (what a non-Gboard IME can access, whether
it needs any special OS-level trust/permission Gboard has and a third-party app wouldn't) before any design or
implementation commitment. Captured as a large, speculative idea, not a scoped task yet.

### D-136 - Gesture-Area Control Contrast Is Poor Compared to Gboard
§42 already fixed the gesture-nav-inset strip's *background* colour (it previously showed whatever sat behind
the IME window). The user now flags a related but distinct issue: on Gboard, the system's gesture-area controls
(the pill/back-indicator etc.) render as dark icons on a light-grey background with good contrast; on AdaptKey
they are barely visible. Suspected mechanism, not yet confirmed: Android's system bars have a light/dark
*appearance* flag (`WindowInsetsController.setSystemBarsAppearance`/`APPEARANCE_LIGHT_NAVIGATION_BARS`) that
controls whether the OS draws these control icons dark-on-light or light-on-dark, independent of whatever
background colour the app itself paints beneath them - if AdaptKey's window never sets this (or sets it
opposite to what its own light keyboard background needs), the system would keep drawing light-on-light or an
otherwise mismatched combination. Needs verifying against the actual `AdaptKeyService`/window setup before
concluding this is really the mechanism.

### D-137 - A Typed Time Should Always Suggest "Uhr" as the Next Word
German convention: a time written as 1-2 digits, a colon, then 2 digits (e.g. "14:30") is essentially always
followed by "Uhr". Requested: detect this pattern and always offer "Uhr" as a D-43 next-word prediction
regardless of what the bigram table happens to know about this exact digit token (which, being effectively
unique per occurrence, likely has weak or no bigram evidence today). Needs a pattern-detection addition to
`showNextWordPredictions()`/`DictionaryStore.nextWords()`'s call site, not a data fix.

### D-138 - Backspace-Hold Now Feels Jerky/Stuttery (Possible Regression - Needs Investigation, Not Guessed)
Reported right after this session's own §64 batch shipped: held backspace now feels like it "checks something
in between" rather than repeating smoothly. Flagged plainly as a suspect, not a conclusion: this same round
touched `AdaptKeyboardView.onTouchEvent()`'s `ACTION_DOWN`/`ACTION_MOVE` handling twice - D-108 added a new
`pressedKeyRect`/`movedOutsideKey()` check (though it only gates the long-press cancellation branch, not
backspace-repeat's own `movedBeyondSlop`-based cancellation, which was left unchanged) and D-109 reordered the
T-05 `ambiguityBands.classify()` call ahead of `offsetModel?.record()` and made the record call conditional.
Neither change was intended to touch backspace-repeat's own cadence (`BackspaceRepeat`/`scheduleBackspaceRepeat`
are untouched code), but the timing coincidence with this exact area of `onTouchEvent()` having just changed is
too close to ignore. Needs an actual trace through `onTouchEvent`'s `ACTION_DOWN` handling for `KeyCode.DELETE`
next round, not a blind revert or guess.

## §66 - Release Round: D-124, D-115/D-125, D-130, D-138 + D-135 Feasibility Research (v0.8.36)

### D-138 - Backspace-Hold Jerkiness: Traced (Not Guessed) to This Session's Own Added Per-Keystroke Lookups
Before touching anything, traced the actual call path rather than reverting blindly: `handleBackspaceRepeat()`
calls `deleteComposingChar()` on every repeat tick, which calls `refreshSuggestions()` same as an ordinary
keystroke. This session's own §64/§63 changes added real per-keystroke cost to that function - D-106 stage 2's
`knownInOtherLanguage()` (up to 3 dictionary lookups) and D-111/D-112's `capitalizedPreview` (1 more
`store.partsOfSpeech()` lookup) - none of which existed before this session. `SqliteDictionaryStore`'s
`wkey TEXT PRIMARY KEY` schema makes each individual lookup cheap (an indexed point lookup), but stacking up to
4 extra SQLite round-trips onto *every* tick of a hold whose fastest interval is 45 ms (§59) is a concrete,
plausible mechanism for exactly the reported "jerky, seems to check things in between" - not a guess, but also
not something this environment can profile to *prove* is the (sole) cause.

**Fix**: `deleteComposingChar()` gained a `duringRepeat: Boolean` parameter, threaded from
`handleBackspaceRepeat()`'s call site only (the ordinary single-tap `handleBackspace()` path is unaffected);
`refreshSuggestions(duringRepeat: Boolean = false)` skips `knownInOtherLanguage()`/`capitalizedPreview()`
entirely when `true`, falling back to the plain `provider.autocorrectFor()` result - matching this function's
pre-§63/§64 behaviour for exactly this one call path. Nothing about D-106 stage 2 or D-111/D-112's actual
*protection* is weakened - `finalizeAndCommit()`'s own checks (unaffected by this parameter) still fully apply
once the hold stops and a real commit happens; only the *live bar preview* during a fast-changing, transient
hold is skipped, which nobody is meaningfully reading mid-repeat anyway.

### D-124 - Clipboard MIME-Type Filter: the Real Bug Was `hasMimeType`'s Own Wildcard Semantics
Traced precisely, not re-guessed: `ClipDescription.hasMimeType(String)` is documented to treat a clip whose own
declared concrete type is the generic wildcard (`ClipDescription.MIMETYPE_UNKNOWN`, many file-sharing/share-sheet
paths use this instead of a specific type) as matching *any* requested pattern - including a `text` request.
Copying the app's own APK (or plausibly any file shared via a path that only declares this generic wildcard)
made `resolveClipboardText()`'s `hasMimeType("text/*")` check return true for content that is not text at all.

**Fix**: new `isTextMimeType(clip)` reads the clip's own declared MIME type(s) directly
(`ClipDescription.mimeTypeCount`/`getMimeType(i)`) and requires a genuine `text/`-prefixed type, explicitly
excluding `MIMETYPE_UNKNOWN` - bypassing `hasMimeType`'s wildcard-matching behaviour entirely rather than trying
to out-guess it. (While writing this fix, avoided re-introducing §60's own already-once-caught KDoc bug:
writing the literal wildcard glyph sequence inside a `/** */` doc comment opens a nested Kotlin block comment
and swallows the rest of the file - worded around it this time by referencing `ClipDescription.MIMETYPE_UNKNOWN`
by name instead of spelling the literal pattern in prose.)

### D-115 / D-125 - Generalised: Unknown Regular Verb Inflections Are Protected, Not Just Two Patched Words
New pure `dictionary/RegularVerbInflection` object (7 tests): recognises a token as a plausible regular ("weak")
German verb inflection of a *known* infinitive by stripping a candidate personal ending (present tense
`-e/-est/-st/-et/-t`, preterite `-te/-test/-ten/-tet`) and checking whether the reconstructed `stem + "en"` is
itself a known dictionary word - e.g. `beurteilst` strips `-st` to `beurteil`, and `beurteil` + `en` =
`beurteilen`, a known word, so `beurteilst` is protected even though it is not itself in the dictionary. This is
the generalisation D-125 asked for: not every inflection needs its own dictionary row (D-115's approach for
`merke`/`stimmen`), just recognising the *productive pattern* is enough to stop a wrong autocorrect, without
necessarily surfacing the form as a suggestion. Deliberately data-free (no dictionary changes) and pattern-only
- a coincidental match against an unrelated `-en`-ending known word is possible in principle but low-probability,
an accepted trade-off of a simple heuristic; strong/irregular ("ablaut") verbs and participles stay out of
scope, matching the "95% via patterns, not exhaustive coverage" framing the user gave this.

Wired into `DictionarySuggestionProvider.bestCorrection()`'s A-01 guard: a plausible inflection is protected
*unconditionally* (no §44/D-113 ratio-override applies), since - unlike a literal known word - it has no
recorded frequency of its own to compare against a correction candidate's; applying the ratio check to it would
trivially always fire (`0 * ratio <= anything`).

### D-130 - Sustained-English Real Language Switch + Space-Bar Change Animation
Two parts, both building directly on already-shipped mechanisms (D-106 stage 1's `LanguageCycle`/`qwerty`
layout, A-03's classifier):

1. **`AdaptKeyService.trackSustainedEnglishUsage()`**: a new `consecutiveEnglishWords` counter increments each
   time `finalizeAndCommit()` routes a commit to English while German/Greek stays the *active* alphabet (via
   `resolveDict()`'s classifier branch, now exposed by changing `selectActiveDictionary()`'s return type from a
   bare `Boolean` to the full `DictChoice` so the caller can read which language was actually chosen, not only
   whether autocorrect was suppressed); resets to `0` the moment a non-English word commits. At
   `SUSTAINED_ENGLISH_WORD_THRESHOLD = 5` consecutive English commits, it performs a real active-language
   switch - the same state changes `toggleLanguage()` already makes (`activeLanguage`, `ActiveLanguageStore`,
   `keyboardView.qwerty`, the space label, a toast) - rather than continuing to rely on per-token routing
   indefinitely. D-106 stage 2's cross-language autocorrect protection (the mechanism the user confirmed
   already "funktioniert wunderbar") is completely untouched by this - this is purely about *when the active
   alphabet itself flips*, a different, additive concern.
2. **`AdaptKeyboardView.beginLanguageChangeFade()`**: a 260 ms cross-fade of the space bar's own label text -
   the previous label fades out over the first half, the new one (already pushed via the existing `spaceLabel`
   property by the time this runs) fades back in over the second half - implemented as a plain alpha animation
   on the shared `textPaint` (reset to full opacity immediately after each draw, since the paint is shared
   across every key's label), not a custom shader/glow shader. Chosen over the alternative "glow sweep" framing
   the user offered, since a straightforward two-stage alpha fade is robust to implement correctly without a
   bespoke gradient/shader animation and the user explicitly offered it as an acceptable alternative. Wired
   into both `toggleLanguage()` (the manual G-01 swipe) and the new automatic switch above, so both trigger
   paths get the same acknowledgement.

### D-135 - Google Password Manager / Autofill Suggestions: Confirmed Feasible (Researched, Not Implemented)
Researched against the current, official Android documentation rather than assumed from memory (this is
exactly the kind of API surface that's easy to misremember or that could have changed): **yes, this is
genuinely feasible for a third-party IME**, via the public, documented Android **Autofill Framework inline
suggestions** mechanism (not anything Gboard-specific or requiring undocumented platform access) - and it would
work with whatever autofill provider the *device* has configured (Google Password Manager by default on most
phones, but equally Bitwarden/1Password/etc. if the user has set a different one), not something tied
specifically to Google's own service.

**How it works, and what it would take:**
- The IME's method metadata (`res/xml/method.xml`) needs `android:supportsInlineSuggestions="true"`.
- `AdaptKeyService` would need to override two `InputMethodService` methods:
  `onCreateInlineSuggestionsRequest()` (declares how many suggestions are wanted and an `InlinePresentationSpec`
  - size/style constraints only) and `onInlineSuggestionsResponse()` (receives an `InlineSuggestionsResponse`,
  inflates each `InlineSuggestion` into a real `InlineContentView` to display).
- **The IME never sees the actual credential data at all** - each suggestion is an opaque, platform-rendered
  `View` the autofill service itself draws (its own branding/lock-icon/biometric-prompt behaviour and all);
  AdaptKey could only control *where* and *how large* to place these views (and loosely nudge their text/colour
  style via the presentation spec), not read or restyle their content into AdaptKey's own chip look. This is a
  deliberate platform privacy boundary, not a limitation specific to a third-party IME versus Gboard.
- The platform decides *when* to call `onCreateInlineSuggestionsRequest()` at all, based on the target field's
  own declared autofill hints/`inputType` - AdaptKey would not need to build its own username/password field
  detection for this to trigger correctly.
- **Minimum API level: 30 (Android 11)** - AdaptKey's `minSdk` is 26, so this would need to be implemented as an
  optional, version-gated feature (a no-op below API 30), not a `minSdk` bump.
- Entirely dependent on the device actually having an autofill service configured/enabled at all; a no-op,
  graceful fallback when it isn't.

**Not implemented this round** - confirmed genuinely buildable with a clear shape, but a real scoped feature of
its own size (a new settings toggle likely wanted too, given the visual "foreign" rendering these suggestions
would necessarily have compared to AdaptKey's own chips) - a candidate for its own future implementation round,
not folded into this one.

Sources: [Integrate autofill with IMEs and autofill services (Android Developers)](https://developer.android.com/identity/autofill/ime-autofill).

575 unit tests (was 567 before this round; +8: 7 `RegularVerbInflectionTest` + 1
`DictionarySuggestionProviderTest` D-115/D-125 case). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
D-124/D-130/D-138 are Android service/view glue, no new tests, consistent with this project's established
testing gap; D-115/D-125's pure recognition logic is fully unit-tested.

## §67 - D-135 Implemented: Autofill Inline Suggestions (Password Manager) (v0.8.37)

Builds directly on §66's feasibility research - the documented, public Android Autofill inline-suggestions
API (API 30+), not anything Gboard-specific.

### New dependency and manifest declaration
`androidx.autofill:autofill:1.3.0` added (a small, plain compat helper library - `UiVersions`/
`InlineSuggestionUi` style-bundle builders; does not raise `minSdk`). `res/xml/method.xml` gained
`android:supportsInlineSuggestions="true"`, the flag that tells the platform AdaptKey wants to render these
inline rather than falling back to a system dropdown.

### `AdaptKeyService` - two new `@RequiresApi(Build.VERSION_CODES.R)` overrides
- `onCreateInlineSuggestionsRequest(uiExtras)`: builds one shared `InlinePresentationSpec` sized to the
  ordinary suggestion bar's own height (min `Size(0, 0)`, max full screen width × bar height) with the
  standard `UiVersions.INLINE_UI_VERSION_1` style bundle (`UiVersions.newStylesBuilder().addStyle(
  InlineSuggestionUi.newStyleBuilder().build()).build()` - **note**: the real class lives at
  `androidx.autofill.inline.v1.InlineSuggestionUi`, not the `androidx.autofill.inline.suggestions` package
  guessed initially from memory before verifying against the actual downloaded AAR's class list
  (`unzip`/`javap` against the resolved `autofill-1.3.0.aar` - checked directly rather than trusting
  recollection of an API surface this project had never used before). Requests up to
  `INLINE_SUGGESTION_MAX_COUNT` (3) suggestions.
- `onInlineSuggestionsResponse(response)`: for each returned `InlineSuggestion`, calls `.inflate(...)` (async,
  `mainExecutor`) and adds the resulting opaque `InlineContentView` to the new `inlineSuggestionsBar` once
  ready; returns `false` when the response is empty (nothing to show - reverts to the ordinary suggestion bar)
  and `true` once at least one inflated view has been added, per the API's own contract for "the IME is
  handling display itself".

### New `keyboard/InlineSuggestionsBarView`
A plain `HorizontalScrollView`-wrapped `LinearLayout` container (`addSuggestion(view)`/`clearSuggestions()`/
`hasSuggestions`) - deliberately an ordinary `ViewGroup`, not a custom `Canvas`-drawn view like
`AdaptKeyboardView`/`SuggestionBarView`, since each suggestion is a real, opaque `View` the platform itself
inflates and owns; AdaptKey can only place it, never draw or restyle its content (the documented privacy
boundary - AdaptKey never receives the underlying username/password text at any point). Occupies the exact
same row/slot as the ordinary suggestion bar (`root.addView(inlineBar, ...barHeight)`, right after `bar`),
toggling visibility with it: showing inline suggestions hides the ordinary bar and vice versa, since both
would otherwise compete for the same slot.

### Lifecycle wiring
New `resetInlineSuggestions()` (clears and hides the row, restores the ordinary bar) called from
`onStartInputView()` - every fresh field starts clean rather than carrying over a previous field's stale
suggestions; a new `onCreateInlineSuggestionsRequest()`/`onInlineSuggestionsResponse()` round-trip (driven
entirely by the platform, not by AdaptKey) supplies fresh ones, or none, for whatever is now focused.

### Scope notes
No new settings toggle this round (§66's write-up had speculated one might be wanted) - the feature is
purely additive and invisible unless the platform actually has a real suggestion to offer for the focused
field, so there is no real annoyance/behaviour-change downside to weigh against a toggle's own complexity;
can be added later if real-device use suggests otherwise. No new tests - both the view (a thin `ViewGroup`
wrapper with no decision logic) and the service overrides (platform callback glue, `@RequiresApi`-gated,
calling into `InlineSuggestion.inflate()` which cannot be exercised without a real device/autofill service)
are consistent with this project's established Android-glue testing gap - **this entire feature is
unverified beyond compiling and the API contract read from documentation + the actual resolved AAR's class
list; it needs a real Android 11+ device with an autofill service configured to confirm anything renders at
all.**

575 unit tests (unchanged - no new pure/testable logic this round). `:app:assembleDebug`/
`:app:testDebugUnitTest` green.

## §68 - Device Feedback on §64-§67: D-124 Follow-Up, D-111 Clarified as D-115's Pattern (v0.8.38)

Device confirmation on the §64-§67 batch: **D-105, D-108, D-130, D-138 confirmed working.** Two follow-ups:

### D-124 - Real Gap Found: the MIME Check Only Ever Covered the URI Branch
§66's fix (`isTextMimeType()`, checked in place of `ClipDescription.hasMimeType()`'s wildcard-matching
gotcha) did not fix the reported case - confirmed still broken on device. Re-traced rather than re-guessing:
`resolveClipboardText()`'s structure was `val uri = item.uri ?: return item.coerceToText(this)` *before* the
MIME check - so the check only ever ran on the URI branch. Whatever copied the APK onto the clipboard
apparently populates `ClipData.Item.getText()` directly (a filename, a content-URI string, or similar)
rather than `getUri()`, which takes the `?:` fallback straight to `item.coerceToText(this)` - completely
bypassing `isTextMimeType()`. Fixed by moving the MIME check to run first, unconditionally, before branching
on which of `getUri()`/`getText()` this item actually has. An ordinary text copy (`ClipData.newPlainText`, what
any normal "Copy" action produces) already always declares `text/plain`, so this costs the common case
nothing. **Not yet re-confirmed on device** - if this still doesn't resolve it, the next step needs to know
exactly which app/action was used to copy the APK, since two independent theories have now been tried and
ruled out or fixed.

### D-111 - Not a Preview Bug: "stelle"/"Stelle" Is D-115's Exact Mechanism, Confirmed
Reported: "stelle" previews as "Stelle" (D-111 working as designed) but still auto-applies - read at first as
"the preview doesn't prevent auto-apply", but traced before assuming a behavioural change was needed. Checked
`dict_de.tsv` directly: `Stelle` (freq 3497) was tagged plain `NOUN`, and `stelle` (the "ich"-form of
"stellen", a genuine, comparably common verb reading - "die Stelle" / "ich stelle") has no separate entry -
**exactly** D-115's `stimmen`/`Stimmen` mechanism (§6 rule 3 auto-capitalises because the dictionary never
flagged the ambiguity), not a gap in D-111's preview mechanism at all - the preview is doing its job (showing
what §6 will do); the thing it shows is what's wrong, for the same reason as before. Fixed identically:
`Stelle	3497	NOUN` -> `Stelle	3497	NOUN,OTHER` in `dict_de.tsv`.

**Systematic follow-up, not yet acted on beyond `Stelle`:** given this is now the second real report of the
identical pattern, scanned the whole bundled `dict_de.tsv` for the general shape (a `NOUN`-tagged word whose
lower-cased form, minus a trailing `e` plus `en`, is a separately known, non-`NOUN`-only word - i.e. every
noun that is *also* the plausible "ich"-form of a real verb). **Found 141 candidates.** Many are exactly the
`stimmen`/`stelle` class (comparably common in both readings - `Liebe`/`liebe`, `Sorge`/`sorge`,
`Suche`/`suche`, `Pflege`/`pflege`); some are much more lopsided (`Ende` 15847 vs. `enden` 630, `Ware` 263 vs.
the unrelated-but-textually-matching `waren` 31549) where the noun reading so overwhelmingly dominates that
blanket-marking them ambiguous would trade a rare false-positive fix for real, felt friction on very common,
currently-correct auto-capitalisation. **Deliberately not bulk-applied** - this is a real product trade-off
(safety against a rare mis-capitalisation vs. typing fluency for an overwhelmingly-common word), not a pure
correctness fix like the two already-confirmed cases, so it needs the user's own call on how far to go (fix
only as individually reported; apply everywhere regardless of the lopsidedness; or apply above some
frequency-ratio cutoff) rather than a unilateral bulk data change.

## §69 - D-124 Closed (Not a MIME Bug), D-116 Implemented: Unhyphenated-Compound Recognition (v0.8.39)

Device confirmation on the §64 batch: **D-114 confirmed working; D-119/D-120/D-121/D-123 provisionally
confirmed** ("scheinen zu klappen" - not yet stress-tested as thoroughly as the already-CONFIRMED §68 items).

### D-124 - Closed: Not the Suspected Mechanism at All, Not Fixable via MIME Detection
Traced with the concrete repro this time (copying the app's own APK via **Total Commander**): the Quick
Paste chip's own preview showed a plain, readable text label ("Total commander file li…", truncated at
`ClipboardPreview.MAX_LENGTH`) - not garbled binary content. This rules out both of the mechanisms the two
prior fixes (§66, §68) addressed: there is no non-text file being coerced into text here at all. Total
Commander evidently puts a **plain-text placeholder string** (most likely for its own internal drag/paste
protocol between TC instances) directly into `ClipData.Item`'s text, declared as genuine `text/plain` -
which, from the OS's and AdaptKey's point of view, **is** exactly what it claims to be: ordinary pasteable
text. No `ClipDescription` MIME check, however precise, can distinguish "genuine text a user wants to paste"
from "an app's own internal placeholder string that happens to also be plain text" - the two are
indistinguishable at the API level.

The only way to suppress this specific case would be a hardcoded match against Total Commander's own
placeholder string - explicitly rejected as exactly the kind of app-/string-specific hack this project
avoids elsewhere (D-118's "trace before fixing" and the general-rule-over-individual-word-patching
philosophy of §62 apply equally to app-specific patching). **Closed as a known, accepted limitation**, not a
bug: the D-36/§60 mechanism (detect and block a real file being offered as text) works correctly and remains
in place; a source app choosing to expose its own internal placeholder text as plain clipboard text is
outside what any MIME-based filter can address.

### D-116 - Unhyphenated Compound Recognition: Known Noun First Part + Correctable Rest (Suggestion-Only)
The reported case ("Beitragsjahreb", a typo of "Beitragsjahren") was never actually a correction-quality
bug: `b`/`n` is an ordinary QWERTZ-adjacent, cost-1 typo relationship (the same class D-67 already
autocorrects elsewhere) - the real obstacle is that "Beitragsjahren" itself is too rare a compound to be in
the bundled dictionary at all (unlike B-01's hyphenated case, there is no explicit compound boundary marker
to anchor a fix on). Full recursive decomposition-plus-fuzzy-matching was discussed and deliberately
rejected as disproportionately expensive and error-prone for the value it would add; the released design is
a narrower, single-split-point approach agreed on directly with the user:

**New pure `dictionary/CompoundSplit`** (8 tests): tries to peel a **known noun** (≥4 characters) off the
front of an unknown token, optionally followed by a German Fugenelement (`s`/`es`/`en`/`er`/`n`/`e`, or none
at all for a direct concatenation like `Haus`+`schuh`), such that the remainder (≥2 characters) is itself
accepted by a caller-supplied `resolveRest` callback. Searches the **longest** possible first part first (a
shorter one is more likely a spurious coincidental match - the classic German compound-splitting ambiguity,
e.g. "Wachstube" as "Wachs"+"tube" vs. "Wach"+"Stube", both individually valid), and only a single split
point is tried, deliberately not recursing into further parts - a typo inside the first part itself is out
of scope, matching how the request was framed ("den ersten Teil sauber erkannt").

**Wired into `DictionarySuggestionProvider.suggestionsFor()`** (3 new tests) via a new private
`compoundCandidate()`: the first-part check requires the candidate to be tagged `PartOfSpeech.NOUN` (the
user's own explicit scoping call - unsure whether nouns alone will cover enough cases long-term, but a
reasonable, revisitable starting point, and Fugenelemente are grammatically a noun-compounding phenomenon
anyway); the rest is resolved via `isKnownWord()` when already known, else `highConfidenceCorrection()` -
reusing D-67's existing cost-1 ceiling and D-114's frequency floor rather than inventing a separate
confidence policy for this feature. **Deliberately gated on `candidates.isEmpty()`** - the compound search
only ever runs once ordinary prefix/fuzzy matching found nothing at all, both because that is genuinely when
it is needed (a real compound has no single-word neighbour close enough for D-12 to find) and to keep its
handful of extra store lookups off the common keystroke path entirely - D-138 is this project's own standing
reminder that stacking several per-keystroke lookups onto every composing step is a real, previously-felt
cost, not a theoretical one.

**Suggestion-only by explicit user decision, never wired into `autocorrectFor()`/`highConfidenceCorrection()`
itself**: given the split point can be genuinely ambiguous (the Wachstube case above), a wrong guess must
only ever be offered, never silently applied - "dann ist man auf der sicheren Seite". Verified directly: a
constructed compound candidate appears in `suggestionsFor()` but `autocorrectFor()`/`highConfidenceCorrection()`
both still return null for the same input.

586 unit tests (was 575; +11: 8 `CompoundSplitTest`, 3 `DictionarySuggestionProviderTest`).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-tested - `CompoundSplit` itself is fully
pure/unit-tested, but real German compound coverage/false-positive rate can only really be judged against
everyday typing on a real device.

**D-116 CONFIRMED WORKING on device** (user confirmation, no code change).

## §70 - Settings Row: New Clear-Clipboard Button (v0.8.40)

New idea raised directly, not part of any prior backlog entry: a third button in the §48/§50 swipe-up
settings row, immediately left of the settings gear, that wipes the clipboard on tap.

`SettingsRowView` gained `clearClipboardButton` (glyph `🗑`, the standard wastebasket emoji - matching the
row's existing style of plain `TextView`s showing a Unicode glyph rather than a drawable asset, exactly like
the emoji button's `😊` and the gear's `⚙`; this project draws its own keyboard rather than depending on
bundled icon assets, and this row already followed that convention before this round) and a new
`OnClearClipboardClickListener`. Positioned via the same `Gravity.END`-anchored `FrameLayout` approach as the
settings gear, offset by the gear's own width plus one more button-margin so the two read as a distinct pair
at the row's right edge, leaving the emoji button alone at the left edge unchanged.

`AdaptKeyService` wires it to a new `clearClipboardFromSettingsRow()`, which closes the row and calls the
*already-existing* `clearClipboard()` - the same function the D-36/D-38 quick-paste flow already calls
automatically after a sensitive paste (`ClipboardManager.clearPrimaryClip()` on API 29+, a
`ClipData.newPlainText("", "")` overwrite below that) - now also reachable directly, on demand, via this
button. No new clearing logic was needed at all, only a new trigger for the existing one. Follows the
established "tapping either row button closes the row" convention (§50/§51) unchanged.

**Known, accepted minor edge case, not addressed this round**: the D-36 clipboard-paste chip is only
(re-)evaluated once, when a field opens (`showClipboardChipIfAvailable()` in `onStartInputView`) - clearing
the clipboard via this new button does not retroactively hide an already-showing paste chip for the current
field. Tapping a stale chip afterwards is harmless (it pastes nothing, since the clipboard is genuinely
empty by then) - the same latent behaviour already existed for any clipboard clear happening via another
route (a different app, the system's own clipboard UI); not a new risk introduced by this button, and not
worth adding a live clipboard-change listener for, unless device use suggests it is actually felt in
practice.

No new unit tests - `SettingsRowView` is Android view/animation glue with no decision logic of its own
(same established gap as the rest of the row, see §50), and `clearClipboardFromSettingsRow()` is a one-line
service-glue wrapper over the already-tested-by-precedent (Android glue, not pure logic) `clearClipboard()`.
586 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-tested.

## §71 - D-122 Implemented: Mid-Word Connector-Split Suggestion (v0.8.41)

First real repro for D-122, traced end-to-end before implementing anything. Repro: type "Testvwort hallo",
then place the caret inside "Testvwort" (mid-word re-edit) - expected a "Test Wort" suggestion (the
accidental `v` connector, one of [TokenRepair.OVER_SPACE_LETTERS]); the bar stayed completely empty instead.

**Root cause, two compounding, individually correct behaviours:** (1) `TokenRepair.trySplit()`'s §45
bigram-co-occurrence gate applies uniformly to the connector-drop strategy too - "test" and "wort" never
co-occur in the bundled corpus, so `trySplit("testvwort", …)` correctly finds nothing, by the same design
that keeps ordinary typos (`"meinst"` -> `"mei st"`) from being cut apart on zero evidence. (2) Even where
`trySplit()` *does* find a candidate, its result was never wired into the suggestion bar at all - only into
the live composing-text colour preview (§49's `splitPreview()`, itself deliberately suppressed during
mid-word editing by D-26's `isEditingMidWord` gate, to avoid mis-colouring a reclaimed fragment) or the
silent A-05 auto-commit at delimiter time. There was no middle path of "offer it, let the user decide" for a
split candidate at all, unlike ordinary autocorrect.

**Fix, matching the design agreed on directly:**

New `TokenRepair.splitAtUnresolvedConnector(token, previousWord)` (6 new tests): the connector-drop
strategy only, deliberately **without** `trySplit()`'s bigram-co-occurrence requirement - both halves must
still be known, non-blacklisted words (the existing `candidateAt`/`isWord`/`score` helpers, unchanged).
Consulted **only** while `AdaptKeyService.isEditingMidWord()` is true (mirroring D-26/§49's own gate) - the
user having deliberately tapped back into an existing word is a far stronger "please fix this" signal than
ordinary forward typing, which is exactly why the bigram gate can be relaxed here without reopening §45's
"any two known fragments get cut apart" false-positive problem for everyday typos.

New `AdaptKeyService.midWordConnectorSplitSuggestion(input)`, consulted from `refreshSuggestions()` (skipped
during `duringRepeat`, consistent with D-138): pre-capitalises the split exactly as `applySplit()` would
(`contextFor(split.left)` / `followingPartContext()`) and wraps it as a `Suggestion("$left $right",
SPLIT_SUGGESTION_SCORE)` with a deliberately maximal score (1e9 - comfortably above any real dictionary
frequency, but far below `Double.MAX_VALUE` to avoid any risk in downstream arithmetic) so it sorts first -
"noticeably higher priority", as D-122 originally asked. **Kept out of the `candidates` list itself** and
appended only at the point of display (`withSplitSuggestion()`, called at every `controller.update()` site):
feeding it into `candidates` would have made it `SuggestionMerger`'s own `maxTier1` normalisation baseline,
compressing every ordinary tier-1 score toward zero - a real, if subtle, side effect worth avoiding rather
than accepting.

**Suggestion-only, exactly like D-116** - never wired into `autocorrectFor()`/`highConfidenceCorrection()`,
so it can never silently apply on its own. Tapping it (`AdaptKeyService.onSuggestionClicked()`'s `NORMAL`
branch, extended to recognise a suggestion word containing a space - no other suggestion source in this
codebase ever produces one, a safe, unique signal) delegates to the *existing* `applySplit()` via new
`applyMidWordSplitSuggestion()`, so per-half capitalisation, A-07 undo arming (a backspace immediately after
reverts to "Testvwort") and D-13 learning all match the already-established, already-tested A-05 commit path
exactly, rather than a parallel reimplementation.

592 unit tests (was 586; +6 `TokenRepairTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-tested - the pure split-recognition logic is fully unit-tested, but the suggestion-bar wiring and tap
handling are `AdaptKeyService`/`InputConnection` glue, the established testing gap for this layer.

## §72 - Bug Fixed: Mid-Word SPACE Left the Caret Before, Not After, the Inserted Space (v0.8.42)

Reported directly, with a precise repro: type "Testcwort" (an unsplit double-word, `c` the accidental
connector), place the caret right after the `c`, delete it with Backspace, then press SPACE to re-insert it
as a real space - the space is correctly inserted, but the caret afterwards sits **before** it instead of
after it (i.e. still between "Test" and the space, not between the space and "wort").

**Root cause, traced (not guessed):** this is the D-119/D-120 `splitComposingAtCaretAndCommit()` path (a
mid-word SPACE splits the composing token at the caret). After the "before" half ("Test") is finalised via a
recursive `finalizeAndCommit()` call, the "after" half's new anchor was computed by **re-reading**
`InputConnection.getExtractedText()` - still inside the same outer batch edit, and *after* several prior
`InputConnection` mutations already issued within that same call (the recursive finalise's own
`setComposingText()`/`finishComposingText()`/`commitText()`). Every *other* same-batch state read in this
class reads *before* it mutates anything (`reclaimSurroundingWord()`'s own anchor read, for instance) - this
was the one place reading *after* several prior edits in the same batch, a markedly riskier pattern. Confirmed
this is the real mechanism (not a coincidence) by checking whether the reported repro could instead be an
autocorrect-length-mismatch artefact: it cannot - "Test" does not change length under §6 capitalisation here
(`test` -> `Test` is a pure case change), so even in this exact zero-length-delta case the caret still landed
one position short of where the space actually was - pointing squarely at the anchor read itself being stale
by exactly the space's own width, not at anything downstream.

**Fix:** the "after" half's anchor is now computed **arithmetically**, with no read-back at all.
`finalizeAndCommit()` (and every path it can delegate to - `commitVerbatim()`, `applyMerge()`, `applySplit()`,
and its own default inline commit) now returns the number of characters it actually, net, inserted into the
document (word length + delimiter length, minus one for `applyMerge()`'s own preceding-space deletion).
`splitComposingAtCaretAndCommit()` captures the "before" half's own anchor *before* recursing into
`finalizeAndCommit()`, then computes the "after" half's anchor as `beforeAnchor + committedLength` - a value
derived entirely from state this function already controls, immune to any question of whether a same-batch
`InputConnection` read reflects the edits just issued to it. This is a stronger fix than merely restructuring
the batching to force a flush before reading, since it removes the read-after-write dependency altogether
rather than trying to make it reliable.

No new unit tests - this is `AdaptKeyService`/`InputConnection` glue with no new pure logic (the changed
functions were already Android-glue, the established testing gap for this layer; the arithmetic itself is a
one-line addition, not a standalone unit worth extracting). 592 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet re-confirmed on device.

## §73 - Backlog Capture: D-139 (Intermittent Text Jitter / Character Scramble, No Repro Yet)

Captured, **not started** - no code change this entry, no repro yet either.

### D-139 - Occasionally the Whole Text Jitters and Its Characters Get Scrambled, Recoverable Only by Clearing Everything
Reported as an intermittent, disruptive glitch first noticed roughly four or five builds ago (no precise
version pinned yet): occasionally the entire text in the field visibly jitters and its characters end up
wildly out of order: the only practical recovery is to delete everything and start over. **No reliable repro
exists yet** - explicitly flagged as such by the user, who will report back once one is found. Nothing here
is implemented or even root-caused; this section only records the report plus honest, clearly-labelled
speculation about plausible mechanisms, to make it easier to recognise the cause quickly once a repro exists.

**Plausible mechanisms (speculation, not a diagnosis):**

- **An `onUpdateSelection()` feedback loop.** The D-62/D-87 mid-word-reclaim machinery
  (`reclaimWordAtCaret()`/`reclaimSurroundingWord()`) already depends on an "is this cursor movement genuinely
  the user's, or just the echo of our own just-issued edit" check to avoid re-triggering itself - this exact
  class of self-triggering has been the root cause of several past bugs in this area (§32's D-87 investigation,
  D-123's suggestion-tap echo). If that check ever fails to recognise a self-issued edit as its own, the
  callback could re-invoke `reclaimWordAtCaret()`, which edits the text again, which fires the callback again -
  an unbounded (or slowly-diverging) loop of small edits. This would plausibly *look* like the reported
  "jitter", and if the reclaimed range drifts wrong on each iteration, could plausibly scramble character
  order the way "durcheinander gewürfelt" describes. This mid-word-editing machinery has seen real churn very
  recently (D-116, D-122, §72's anchor-arithmetic fix) - not evidence those introduced this bug (the user's own
  timeline of "four or five builds ago" points earlier, and §72 specifically *reduced* risk in this exact area
  by removing a read-after-write dependency), but it is the area with the most recent activity and the
  strongest structural resemblance to the symptom, so it is the first place worth looking once a repro exists.
- **A specific target app/editor's `InputConnection` behaving unexpectedly under batched edits.** Precedent:
  D-110 (eBay Kleinanzeigen's auto-cap failure) is a confirmed instance of a specific app's `InputConnection`
  not behaving as this IME assumes. If the jitter only ever happens in one particular app, that would point
  here instead of at AdaptKey's own state machine.
- **Less likely, noted for completeness:** a stale async tier-3 result slipping past the `tier3RequestSeq`
  guard - the guard checks `composing.toString() == input` at apply time, which could coincidentally still
  match after the token changed and changed back. This would more plausibly cause a wrong *suggestion* than
  actual text corruption, so it is a weaker candidate than the two above.

**What would help most, once a repro is found:** which app/field it happened in; whether it correlates with
mid-word editing (tapping back into an already-typed word) specifically, or can happen during plain forward
typing too; whether it correlates with a held key (backspace-repeat) or a fast burst of keystrokes; and
whether disabling C-06 (the tier-3 mini-LLM, if a model is installed) changes anything. Per this project's own
rule, no fix will be attempted from speculation alone.

## §74 - Device Confirmations: D-106/D-111-D-113/D-122/§72; D-135 Negative Result (finanzen.net zero)

No code change this entry - device-feedback status update only.

**Confirmed working:** D-122 (mid-word connector-split suggestion), the §72 caret-position fix, and (a
blanket confirmation covering the remaining older, previously-unconfirmed items) D-106 stages 1+2 (English as
an explicit language, cross-dictionary loanword protection), D-111/D-112 (pending-capitalisation-change
preview, correction-follows-context casing) and D-113 (§44 ratio-override restricted to cost-1 typos).

### D-135 - Autofill Inline Suggestions: No Suggestion Shown for the Username Field in "finanzen.net zero"
Tested against a real login field (finanzen.net zero, a brokerage app) - no inline suggestion appeared for
the username field. Not yet traced - no device logs available in this environment, and several genuinely
different causes would all look identical from the outside ("nothing shows up"), so guessing at a fix would
likely just be wrong:

1. **No saved credential for this app in the configured autofill service** - the most mundane explanation
   and worth ruling out first: does the device's autofill service (Settings → Passwords/Autofill) actually
   have a saved login for finanzen.net zero already? With nothing saved, there is nothing for the platform to
   ever offer, regardless of anything AdaptKey does.
2. **The field itself may not be autofill-relevant per the app's own declaration** - per the Autofill
   Framework's own design (§66/§67), the platform decides *whether* to call
   `onCreateInlineSuggestionsRequest()` at all based on the target field's own declared `autofillHints`/
   `inputType`; AdaptKey has no influence over this. Notably, **finance/brokerage apps quite commonly disable
   autofill deliberately on login fields**, as an explicit anti-phishing/security measure - this would be a
   real, intentional restriction on the app's side, not a bug in AdaptKey at all, and is arguably the single
   most likely explanation given the specific *kind* of app this is.
3. **The configured autofill service might not support inline suggestions specifically** - some autofill
   providers only implement the older dropdown-style UI, not the inline-in-IME mechanism `onInlineSuggestions
   Response()` depends on; autofill being "enabled" is not the same as inline suggestions being supported.
4. **An actual bug in AdaptKey's own integration** - possible, though §67's implementation was checked against
   the real resolved `autofill-1.3.0.aar` class list rather than assumed; this whole feature was explicitly
   flagged as "unverified beyond compiling" pending exactly this kind of real-device test.

**Not fixed or further investigated this round** - deliberately not guessing among four candidates that would
look identical from the outside. To make progress, the next report would ideally answer: does the device's
autofill service have a saved login for this app already; what autofill service is configured (Google
Password Manager / a third-party one); and - the most diagnostic single test - does an inline suggestion
appear for *any* login field in *any* app with AdaptKey active, or has it never been observed working at all
yet. If it turns out to be case 2 (the app itself opts out), that would not be an AdaptKey bug to fix at all.

## §75 - Batch Release: D-117, D-126, D-127/D-128, D-129, D-131, D-132, D-133, D-136, D-137 (v0.8.43)

Released together per explicit user instruction, one round mirroring §64's own batch-release shape.

### D-117 - A Multi-Typo Word Beyond D-28's Budget Still Surfaces as a Suggestion
New `DictionarySuggestionProvider.wideFuzzyNeighbours()`: the same first-character-bucket search as D-12's
`fuzzyNeighbours()`, but at a wider edit-cost budget (`WIDE_CORRECTION_COST = 4` vs. the ordinary
`MAX_CORRECTION_COST = 2`), restricted to tokens of at least `MIN_WIDE_FUZZY_LENGTH = 6` characters (a wide
budget on a short token would match almost anything) and only ever tried once every cheaper search above
found nothing at all - the same `candidates.isEmpty()` fallback gate as D-116. Reaches the motivating example,
`erkamm` -> `erkannt` (two adjacent-key substitutions plus an insertion, cost 4). **Suggestion-only**: D-28's
own tight, autocorrect-grade budget is completely untouched, so this never silently applies on its own -
verified directly (`autocorrectFor`/`highConfidenceCorrection` both still return null for the same input).
Scope limitation, noted honestly rather than solved: still only searches the token's own first-character
bucket (its letter or a keyboard neighbour), so a token whose very first letter is *also* badly garbled stays
out of reach - a genuinely open question per D-117's own spec entry, not attempted here. 4 new tests.

### D-126 - Tier-3 Independent Enable Toggle (the Uninstall Action Already Existed)
Traced before implementing: `Tier3ModelActivity` already had a working "remove model" button
(`removeModel()` → `Tier3ModelInstaller.clear()`) - the uninstall half of D-126 was already done, just never
marked confirmed in the backlog. Implemented the other half: new `tier3Enabled: Boolean` (default on) through
the full settings pipeline (`RawSettings`/`AdaptSettings`/`SettingsMapper`/`SettingsStore`,
`d126_tier3_enabled`) plus a `SwitchPreferenceCompat` right below the model-import entry.
`AdaptKeyService.reconcileTier3Provider()` now gates on `settings.tier3Enabled && Tier3ModelStorage.
isModelInstalled(this)` instead of installed-state alone, and is called from `applySettings()` itself (removed
the now-redundant standalone call in `onStartInputView()`) so toggling the switch takes effect immediately
while the service is resident, not only on the next field focus - a model can now stay imported while
inference is switched off, e.g. to save battery/latency without deleting and later re-importing the ~270+ MB
file. 1 new test (`SettingsMapperTest`).

### D-127 / D-128 - What Learns When (Internal Reference + a User-Facing Settings Summary)

**D-127 - internal reference, consolidated here for future sessions:**
- **T-03 (`OffsetModel`)**: learns from every confirmed tap (`AdaptKeyboardView.onTouchEvent`'s `ACTION_DOWN`
  → `record()`) - except a tap D-109 flags as space/letter-ambiguous, which is never recorded at all (the
  root cause D-109 fixed). Bounded by D-109's isotropic cap and now D-133's bottom-row-specific downward cap.
- **D-37 (dictionary, `learnWord`)**: a word already known is reinforced immediately (frequency + bigram bump).
  A genuinely new word is only counted via `PendingLearnStore` and promoted to the real dictionary after
  `LEARN_THRESHOLD = 2` commits, so a one-off typo is never learned as if it were a real word on the first go.
- **D-13 (`learnWordStrong`)**: a *deliberate* correction (undoing a wrong A-05 split) promotes the rejoined
  word immediately, bypassing D-37's threshold - a deliberate correction is a much stronger, unambiguous
  signal than merely typing the same word twice.
- **T-04 (`PatternSeed`)**: not continuous learning - a one-time initial seed applied when a typing pattern is
  chosen in Settings (K-01), front-loading a plausible starting shape for T-03; the model keeps learning from
  real taps afterwards regardless of the seed.
- **§9 tier-3 adaptive learning (`reinforceFromTier3`)**: after a commit, a word the mini-LLM was confident
  about but tier-1's own dictionary didn't already know is fed back into the *same* D-37 pipeline as a
  reinforcement signal. This is the actual mechanism behind "the LLM is consulted less over time" - as the
  dictionary absorbs what the LLM already knows, tier-1's own confidence rises, and C-06's activation
  threshold (`Tier3Activation.shouldActivate`) simply triggers tier-3 less often for words it has already
  reinforced. Not a schedule, an emergent property of the two mechanisms feeding each other.

**D-128 - user-facing summary**: extended the existing D-89 `FeatureOverviewActivity` per the user's own
suggested shape, rather than a new screen - a compact, visually distinct callout (`key_background_special`
background, its own heading) inserted above the ordinary scrolling feature list, summarising D-127's write-up
in plain language: the touch model adjusts with every confirmed keystroke, corrected/new words join the
dictionary, and the optional mini-AI is consulted less as the dictionary learns to predict the same words
itself. New strings (`d128_title`/`d128_summary`, all three locales). No new tests - static layout/string
content, no decision logic.

### D-129 - Corner-Hint Glyph for the Calculator Minus Key's Sign-Flip
§31 (v0.8.10) deliberately shipped this key with neither a `hint` nor `alternatives`, since either would
have wrongly routed its long-press through the hint/alternatives commit-text pipeline instead of `SignFlip`.
New `AdaptKeyboardView.isSignFlipKey()` recognises the key by its own identity (`char == SymbolLayout.
MINUS_SIGN`, mirroring `KeyboardLayout.hasLongPressAction()`'s existing special case) and reuses D-98's
existing generic "more on long-press" corner glyph (`MORE_ALTERNATIVES_GLYPH`, "◢") - deliberately not a
bespoke sign-flip glyph a user might mistake for a committable character. No new tests - pure `onDraw` glue,
the established gap for this view.

### D-131 - Raw-Coordinate Correction (D-39) Now Live While Composing
`AdaptKeyService.refreshSuggestions()` now also consults the existing `rawCoordinateCorrection()` fallback -
previously only ever reached at commit time - once ordinary prefix/fuzzy/compound/wide-fuzzy candidates are
all still empty (`!duringRepeat`, consistent with D-138's own per-keystroke-cost concern for exactly this
class of extra lookup). Refactored the suggestion-assembly in `refreshSuggestions()`/`applyTier3Outcome()`:
D-122's split suggestion and this new raw-coordinate suggestion are both collected into one `extras: List
<Suggestion>`, appended only at display time (`candidates + extras` / `outcome.suggestions + extras`) rather
than fed into the tier-1 candidates `SuggestionMerger` itself normalises against - avoids exactly the
score-normalisation distortion a synthetic maximal score would otherwise cause there (a lesson already applied
to D-122, now generalised instead of duplicated). No new tests - `AdaptKeyService`/`InputConnection` glue over
the already-tested `RawCoordinateCorrection`, the established gap for this layer.

### D-132 - Settings Row: the Whole Reveal Now Slides as One Piece
Root cause of the "pops in" complaint: the row's own background lived on the *outer* view, whose
`layoutParams.height` jumps to its target instantly (the D-86 precedent, avoided for good reason - an
animated live-relayout would be expensive) - so the solid background appeared at full height immediately,
while only the buttons inside then slid up over 180ms. Fix: moved the background from the outer
`SettingsRowView` onto `content` (the child whose `translationY` already animates) - the instant height jump
is now invisible (nothing is painted at the outer view's own bounds), so the background and the buttons slide
together as one continuous reveal, matching what was asked without needing a live-relayout animation at all.
One-line change; no new tests - pure View/animation glue, the established gap for this view.

### D-133 - Bottom-Row Touch Zones: a Hard, Direction-Specific Downward Bound
New `OffsetModel.Candidate.maxDownwardOffsetFactor` (nullable, defaults to "use the model's own isotropic
`maxOffsetFactor`, same as every other direction") lets a caller tighten a specific candidate's downward (+y)
cap independently of D-109's general isotropic one; `cappedMeanOffset()` gained a matching optional
`maxAbsYDown` parameter for the D-24 visualisation to stay truthful (D-109's own established principle).
`AdaptKeyboardView` sets `BOTTOM_ROW_DOWNWARD_OFFSET_FACTOR = 0.25` (a considered, not-yet-device-tuned
starting point - the vertical midpoint between a bottom-row key's own centre and its bottom edge) for
`c`/`v`/`b`/`n`/`m`, reusing the already-existing `BOTTOM_ROW_LETTERS` constant (§64's T-05 ambiguity-band
set) rather than duplicating it. 3 new tests, including one proving the effect end-to-end via `resolve()`
(not merely that the parameter exists).

### D-136 - Gesture-Area System Control Contrast
Confirmed the suspected mechanism before fixing it (checked the actual window/insets setup, not assumed):
`AdaptKeyService` never touched the system-bars appearance flag at all, so the OS gesture-area controls
(pill/back indicator) rendered with whatever the window would otherwise inherit, independent of AdaptKey's
own painted background. Since `R.color.keyboard_background` has no dark-theme variant (always light), the
fix is unconditional: `WindowInsetsControllerCompat(window.window, root).isAppearanceLightNavigationBars =
true` in `onCreateInputView()`, alongside the existing D-42/edge-to-edge inset handling. No new tests -
window/service glue, the established gap for this layer.

### D-137 - A Typed Time Always Suggests "Uhr"
New pure `suggestion/TimePattern.endsWithTime()` (7 tests): a plain shape check (`\d{1,2}:\d{2}` at the end
of the text, optional trailing whitespace) - deliberately not range-validating real hours/minutes, since a
shape-only false positive is harmless and range-validation would add complexity for no real gain.
`AdaptKeyService.showNextWordPredictions()` now also checks the text immediately before the cursor and, when
it ends in a time, adds `Suggestion("Uhr", MAX_PRIORITY_SUGGESTION_SCORE)` - the same deliberately maximal,
tier-3-normalisation-avoiding scoring approach as D-122, generalised into a shared `MAX_PRIORITY_SUGGESTION_
SCORE` constant now that two independent features use it.

607 unit tests (was 592; +15: 7 `TimePatternTest`, 3 `OffsetModelTest`, 4 `DictionarySuggestionProviderTest`,
1 `SettingsMapperTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. None of this round's Android/
service/view-glue items (D-126, D-128, D-129, D-131, D-132, D-136) have been confirmed on a real device yet -
this environment's established, repeatedly-documented limitation (no emulator, no `InputConnection`
Robolectric shadow, no way to render the settings screen or observe system-bar icon contrast).

## §76 - D-139 Investigated: No Root Cause Confirmed, a Defensive Circuit Breaker Added (v0.8.44)

Followed up on D-139 (§73) by request. **No repro exists yet - nothing below is a confirmed fix.** This is a
code-reading pass over the most plausible mechanism identified in §73 (a self-triggering `onUpdateSelection`
cascade in the D-62/D-87 mid-word-reclaim machinery), reported honestly as what it is: a real, structurally
identifiable risk, not proof of what actually happened on-device.

### What was traced
Read `AdaptKeyService.onUpdateSelection()`, `reclaimWordAtCaret()`, `reclaimSurroundingWord()`,
`splitComposingAtCaretAndCommit()` and the mid-word composing-mutation paths end to end, specifically
looking for a way this project's own established failure mode (§32's D-87: a `deleteSurroundingText()` call
reaching the app as a standalone edit, its callback arriving after `composing`/`composingAnchor` had already
advanced, wiping the token it was itself building) could recur in a form severe enough to look like the
reported "text jitters, characters get scrambled" symptom.

**Found, concretely:**
- `onUpdateSelection()`'s `composing.isEmpty()` branch calls `reclaimWordAtCaret()` **unconditionally**
  whenever the selection collapses - unlike the non-empty branch, which gates on an explicit `ownEdit` check,
  this one has no "is this genuinely a new user action, or an echo of our own prior edit" guard at all.
- The `!ownEdit` branch itself calls `currentInputConnection?.finishComposingText()` - a **mutating** call
  issued from *inside* the callback handler, which could in principle produce its own follow-up
  `onUpdateSelection` invocation.
- Chained together, a caret-move callback could in principle re-trigger `reclaimWordAtCaret()` /
  `finishComposingText()` repeatedly, each iteration mutating the document again - which would plausibly
  present exactly as "the text jitters and its characters end up scrambled", the reported symptom.

**Not found:** a concrete, provable trigger for this chain actually re-entering itself - that depends on
Android's actual callback-delivery timing and the specific target app's `InputConnection` implementation,
neither of which this environment (no emulator, no `InputConnection` Robolectric shadow) can exercise to
prove one way or the other. Recomputing `composingAnchor` arithmetically (§72) rules out one instance of the
D-87 stale-read pattern but does not rule out this one, which is structurally different (a callback
triggering more `InputConnection` mutations, not a mutation being read back too early).

### What was added: a circuit breaker, not a fix
New pure `CallbackBurstGuard` (5 tests): a plain sliding-window call counter (`isBurst(nowMs)`), Android-free
so it is directly unit-testable. Wired into the very top of `onUpdateSelection()` - if this callback fires
more than `DEFAULT_LIMIT = 40` times within `DEFAULT_WINDOW_MS = 200`, every reactive branch below is skipped
for that call, stopping a possible cascade from continuing to escalate rather than letting it run unbounded.

Threshold reasoning, so a future session can retune it rather than guess again: a held backspace repeats as
fast as every 45ms (§59), so even a sustained hold manages only ~4-5 ticks within a 200ms window - comfortably
under 40, so ordinary heavy backspace use is untouched (verified directly: a test simulates 200ms of 45ms-tick
backspace-repeat and confirms it never trips the guard). A genuine unthrottled cascade - no deliberate delay
between steps at all, unlike a timer-driven repeat - would be expected to blow past 40 within the same 200ms
almost immediately. Deliberately conservative (wide margin against false positives) since a wrongly-tripped
guard would itself be a new, self-inflicted bug in ordinary use.

**Explicitly not claimed:** that this is what caused the reported symptom, or that it is now fixed. This is a
structural hardening measure for a real risk found by reading the code, kept clearly separate from a
confirmed diagnosis - D-139 stays open pending an actual repro.

5 new unit tests (`CallbackBurstGuardTest`). 612 unit tests total (was 607).
`:app:assembleDebug`/`:app:testDebugUnitTest` green.

## §77 - D-132 and D-137 Follow-Up Fixes: Neither Original Fix Actually Worked (v0.8.45)

Device feedback on §75: neither D-132 nor D-137 changed anything observable. Both re-traced from scratch
rather than adjusted blindly.

### D-137 - Root Cause: Digits Never Reach the Code Path That Was Fixed
The §75 fix wired the "Uhr" check into `showNextWordPredictions()` - but that function is only called from
deep inside `finalizeAndCommit()`'s *normal word-commit* path, which requires an actual composing **token**
to finalise. Traced the `KeyCode.CHAR` handler directly: a digit only ever extends a composing token when
`composing` is already non-empty (D-40, the mid-word-typo case); a **standalone** digit - typing "1", "4",
":", "3", "0" one at a time to form "14:30" - has `composing` empty at every single keystroke, so each one
instead calls `finalizeAndCommit(ic, raw.toString())` **treating the digit itself as the delimiter**, which
takes `finalizeAndCommit()`'s *separate* empty-composing branch (commit the character, `clearSuggestions()`,
return early) - the branch where `showNextWordPredictions()` is never called at all. The §75 fix was wired
into a code path a typed time structurally never reaches.

**Fix:** new `AdaptKeyService.showTimeSuggestion(ic)`, called from *inside* the empty-composing branch itself
(right after the character commits), replacing the unconditional `clearSuggestions()` with "show the Uhr
prediction if the just-committed text ends in a time, else clear as before". The original `showNextWordPredictions()`
wiring is left in place (harmless, not reachable by pure digit typing, but a small correctness margin for
any other route that might reach it).

### D-132 - Root Cause: the Row's Background Was Never the Problem
Re-examined the actual visible complaint ("poppt sofort rein und die Buttons sliden darauf nach oben") rather
than assuming the §75 diagnosis just needed a bigger version of the same fix. The row sits in the **same
parent `LinearLayout`** as the suggestion bar and keyboard below it - jumping `layoutParams.height` to the
target instantly (the D-86-inspired shortcut §75 kept) makes those *sibling views* visibly jump down by the
row's full height the moment the swipe is recognised, regardless of what happens to the row's own background
afterwards. Worse, `content`'s background (moved there in §75) is `R.color.keyboard_background` - **the
exact same colour as the root view's own background** (§42) - so it was never visually distinguishable from
what was already showing through in the first place; the §75 fix changed nothing a user could actually see.
D-86's "resize right away" shortcut is correct for `AdaptKeyboardView`'s own self-contained page-slide (no
siblings to disturb) but does not transfer to a row stacked above other views.

**Fix:** `SettingsRowView.animateHeight()` now genuinely animates `layoutParams.height` itself
(`ValueAnimator.ofInt` + `requestLayout()` on every frame - the standard Android pattern for an animatable
view height, since there is no dedicated height property) from 0 to `ROW_HEIGHT_DP` and back, so the
suggestion bar/keyboard shift down/up in the *same* motion as the row's own growth. `content` no longer
tracks the row's own (now-animating) bounds via `MATCH_PARENT`; it keeps a **fixed** `ROW_HEIGHT_DP` height,
anchored to the row's bottom edge (`Gravity.BOTTOM`). As the row's own smaller, animating bounds clip
`content` (a `ViewGroup`'s default `clipChildren`), progressively more of it becomes visible from the bottom
up as the row grows - which alone produces the "buttons rising into place" effect, with no separate
`translationY` animation needed at all (removed).

No new tests for either fix - both are Android view/animation glue, the established gap for this layer; 612
unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Neither has been re-confirmed on
device yet.

## §78 - Version Shown in Settings; D-128 Relocated and Rewritten (v0.8.46)

### New: App Version Shown in Settings
Requested directly. New static `info_version` `Preference` in the settings screen's Info category
(`selectable="false"`, same style as `info_privacy`/`info_license`), its summary set at runtime in
`SettingsFragment.onCreatePreferences()` from `PackageManager.getPackageInfo(packageName, 0).versionName` -
read live from the actual installed package rather than a hand-maintained string resource, which would
inevitably drift out of sync with the real `versionName` in `app/build.gradle.kts`. New `info_version_title`
string (all three locales).

### D-128 - Two Corrections, Both From Direct Feedback
**Placement**: the §75 callout lived inside `FeatureOverviewActivity`, reachable only after tapping into it -
not what was asked ("direkt in den Settings auf dem Haupt-Screen ... am besten im Text unter dem Button, der
in die Feature-Liste führt"). Removed that callout entirely (the `LinearLayout` block, `d128_title`/
`d128_summary` strings - both now unused) and moved its role onto the **existing** `d89_feature_overview`
preference's own `summary` text - the text already shown directly under that button on the settings screen,
with no extra navigation required.

**Content**: the §75 text described dictionary word-learning and the tier-3 LLM fading out - real mechanisms,
but not distinctive: most keyboards claim some version of "learns your words". Rewritten to name what is
actually unique to AdaptKey and was specifically asked for: the raw per-character touch coordinate is
recorded (T-02), not just which key it resolved to; each key's own hit zone is continuously, individually
re-shaped from that data (T-03); and the starting shape for that model comes from the typing style chosen
during setup (K-01/T-04's `PatternSeed`), not a blank slate. New `d89_summary` text (all three locales),
replacing the previous generic "a full overview of every feature" line.

No new tests - static settings-screen content and a runtime `PackageManager` read, no decision logic. 612
unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

## §79 - D-135 Struck From the Backlog

Per explicit user instruction, D-135 (password-manager/Autofill suggestions in the suggestion bar) is struck
from the open backlog for now - not investigated further, not scheduled. §67's already-shipped Autofill
inline-suggestions implementation and §74's negative device result (no inline suggestion for the username
field in "finanzen.net zero") are left exactly as documented; only the open backlog entry itself is dropped.

## §80 - Backlog Captured, Not Yet Implemented (Two New Requests)

### D-142 - Credential-Field Learning: Immediate Save, Its Own Suggestion List, Live Email-Domain Ranking
Raised directly, following a review of the T-03/D-37/tier-3 learning write-up (D-127): the ordinary learning
pipeline is wrong for login-type fields (username/email). Three related asks, explicitly scoped as a single
new feature, not started this round:

1. **Immediate save, no D-37 threshold.** Typing a username or email address into a recognised login field
   must be learned on the very first commit, not counted up over `LEARN_THRESHOLD` (2) occurrences like an
   ordinary new dictionary word (D-37) - a login value is deliberate, precise input by construction, the
   opposite of the "don't eagerly learn a one-off typo" reasoning `PendingLearnStore` exists for.
2. **A separate, specially-flagged store - not the ordinary dictionary.** These values need their own list
   (or an explicit flag on existing entries) so they never get mixed into everyday-word suggestions and
   autocorrect. Ordinary dictionary words are never something a user would want to type into a username/email
   field, and vice versa - offering them there is "sinnbefreit" (pointless) per the user's own framing.
3. **Login-field suggestions are exclusively this list**, not the ordinary dictionary/n-gram/tier-3
   suggestion pipeline, whenever the target field is recognised as a login-type field (candidate signal:
   `EditorInfo.inputType`'s `TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS`/
   `TYPE_TEXT_VARIATION_USERNAME`/`TYPE_TEXT_VARIATION_PASSWORD`/`autofillHints`, needs confirming against
   real login fields, not assumed).
4. **Live, frequency-ranked email-domain completion after `@`.** Once the user has typed `@` in a
   not-yet-known email address, offer the domains from their own saved credential entries, most-frequent
   first, as completions - directly motivated by the user's own workflow (one personal domain, many aliases
   on it).

Needs its own design pass before implementation - at minimum: the exact field-type detection signal (and its
false-positive/false-negative behaviour on real apps, the same caution D-135's writeup already flagged for
`EditorInfo`-based field detection in general); the storage shape (a new SQLite table vs. a flagged column on
the existing dictionary schema); whether "domain" parsing is naive (`substringAfter('@')`) or needs more
care; and privacy handling (this store would contain real usernames/email addresses, unlike the ordinary
dictionary - likely needs its own place in the existing "Info & Privacy" settings copy, and probably its own
clear/reset action alongside the existing T-03/dictionary reset). Not designed or scoped further than this.

### D-143 - A Dedicated Keyboard Layout for URL Entry Fields
Raised directly, explicitly to be captured only for now ("merke dir das bitte einmal vor als noch zu
planendes Feature") - no design or implementation this round. A URL-specific layout, in the spirit of how a
phone/number-class field already auto-opens the calculator page (`AdaptKeyService.initialSurfaceFor()`, §31):
likely direct access to `/`, `.`, `www`, `.com` and similar without a long-press detour, auto-selected for
`TYPE_TEXT_VARIATION_URI`-class fields. Needs its own design pass (exact key set, placement, whether it
reuses `SymbolLayout`'s machinery or is a genuinely new layout) before implementation.

## §81 - D-140 Implemented: Un-Learn on A-07 Undo (T-03 + Dictionary); D-141: "Uhr" Timing Fixed (v0.8.47)

### D-140 - Rejected Corrections Are Now Un-Learned, Not Just Reverted On Screen
Raised directly, following a review of the D-127 "what learns when" write-up: A-07's post-commit undo (a
single backspace right after a corrected/split commit) restored the visible text, but never reversed whatever
training that commit had already persisted - a rejected correction kept reinforcing the dictionary
frequency/bigram of the *wrong* word, and kept the T-03 touch model's (possibly wrong) key resolution,
forever. Both must be un-learned, or - per the user's own framing - the A-07 safety net "kann man sich auch
sparen" (might as well not exist).

**Dictionary (every autocorrect/split undo path):** new `DictionaryStore.unlearn(word, previousWord)` - the
exact symmetric inverse of `learn()`: decrements the unigram frequency and bigram count by one each, removing
the entry entirely once a count reaches zero (so a word `learn()` had just created, frequency 1, is fully
un-learned again, rather than left behind as a zero-frequency ghost `isKnownWord()` would still report as
known). `learnWord()` now returns a `LearnOutcome` (`SKIPPED`/`LEARNED`/`PENDING`) describing exactly what it
did, captured into the armed A-07 undo state (`undoLearnRecords` - one entry for a plain correction, two
(left/right) for a split, each with the context word it used); `performAutocorrectUndo()` reverses each via
the new `unlearnWord()` before re-learning the word the user insisted on, replacing the old, incomplete
`PendingLearnStore.decrement(committed)` call (which only ever helped the rare case where the wrongly-
committed word was itself still unpromoted - the *common* case, an already-known correction target getting
silently reinforced by the original commit, was never touched at all).

**T-03 touch model - scoped precisely, not blanket-applied:** the touch-offset model is *not* wrong every
time an autocorrect fires - most corrections (spelling, diacritic restoration, splits) are purely linguistic
decisions with no bearing on which physical key was actually touched, and un-learning correctly-resolved taps
just because a linguistic correction was rejected would quietly discard good training data. The model can
only be genuinely wrong in one specific path: D-39's raw-coordinate fallback, whose entire premise is "the
runner-up key under this exact tap was more likely than the one that got resolved." New
`OffsetModel.unrecord(id, centerX, centerY, x, y, size)` is the exact algebraic inverse of the Welford update
`record()` performs (not a heuristic - `record` then `unrecord` with the same arguments restores the prior
mean/variance/contact-area state precisely, including removing the key's entry entirely once its last sample
is reversed, so it reads as genuinely untrained again). `finalizeAndCommit()` now distinguishes whether
`autocorrectFor()` or the `rawCoordinateCorrection()` fallback produced the correction (previously collapsed
into one `?:` chain); when it was the raw-coordinate path, new `rawCorrectionUndoFor()` finds the single
character position where the correction actually differs from what was typed (D-39 only ever substitutes one
position) and captures that position's raw tap, resolved key id and key geometry, before `clearComposing()`
wipes `composingTaps`. On undo, only that one sample is reversed via `offsetModel.unrecord()` - never the
rest of the token's (correctly resolved) taps, and never anything at all for an ordinary
dictionary/diacritic/split correction, which sets no raw-correction undo in the first place.

New tests: `OffsetModelTest` (4, proving `record`/`unrecord` round-trip exactly, including the contact-area
mean), `InMemoryDictionaryStoreTest`/`SqliteDictionaryStoreRoboTest` (5, `learn`/`unlearn` symmetry for both a
brand-new word and a reinforced known word). The `AdaptKeyService` wiring itself (`rawCorrectionUndoFor`,
`unlearnWord`, the extended `performAutocorrectUndo`) is service/`InputConnection` glue with no independently
testable pure logic beyond what these cover - the established testing gap for this layer (no emulator/
`InputConnection` Robolectric shadow in this environment).

### D-141 - "Uhr" Suggestion No Longer Appears (or Applies) Before the Trailing Space
Reported directly: the D-137/§77 "Uhr" prediction appeared immediately after the last digit of a typed time
(e.g. right after "14:30"'s `0`), before any space had been typed - and tapping it then concatenated straight
onto the digits with no separating space at all ("14:30Uhr "). `TimePattern`'s regex previously allowed zero
or more trailing whitespace characters (`\s*$`), so it matched the bare digits alone. Changed to require **at
least one** trailing whitespace character (`\s+$`) - the format is deliberately not even checked until the
actual delimiting space has been typed, per the user's own framing ("vorher muss das Format gar nicht geprüft
werden"). Since the space is always already committed to the field by the time either call site
(`showTimeSuggestion()`'s empty-composing branch, and the `timeSuggestion()` fallback inside
`showNextWordPredictions()`) checks the pattern, this single regex change fixes both complaints at once: the
suggestion no longer appears prematurely, and applying it (`onSuggestionClicked()`'s unchanged
`commitText(word + " ", 1)`) now always lands after a real, already-present space. 2 `TimePatternTest` cases
updated to require trailing whitespace, 1 new case added confirming no match without it.

### Reordered T-03/T-04 in §3 (Documentation Only)
Per direct request, so the spec's own reading order matches the actual dependency: T-04 (typing pattern) now
precedes T-03 (personal offset model) in §3, since K-01's `PatternSeed` (D-68) seeds T-03 from the pattern
T-04 describes, not the reverse. Purely a section-order swap - the IDs themselves are unchanged (renumbering
would ripple through many code comments/tests that reference "T-03"/"T-04" by name), and, consistent with how
the rest of this document works, the original §3 text is left as the historical baseline; the actual current
behaviour (an explicit pattern picker, not the auto-detection §3 originally described) is already documented
correctly in D-68/§63 and later.

622 unit tests (was 612; +10). `:app:assembleDebug`/`:app:testDebugUnitTest` green. None of this round's
items have been device-confirmed yet.

## §82 - D-142 Implemented: Login-Field Credential Learning & Suggestions (v0.8.48)

Implements the §80 capture, with one real technical finding that reshaped its scope - surfaced to the user
before continuing, rather than silently narrowed or guessed around.

### Real constraint found: `EditorInfo` has no autofill-hint field at all
§80's own design leaned on `EditorInfo.autofillHints` for username detection - the compiler flagged it as
unresolved, then verified directly against the real `android-35` `android.jar` via `javap` (not assumed):
no such field exists on `EditorInfo` at all. Autofill hints live on the target app's own `View`, which an
IME never has a reference to. `InputType`'s variation bits remain the only reliable field-level signal, and
they distinguish email/password cleanly - but have no variation for "username" whatsoever, so a plain login
field is, at the `EditorInfo` level, indistinguishable from any other text field. Guessing from a bare
unvaried text field would have wrongly treated ordinary prose fields (chat, notes, search) as login fields
everywhere - explicitly rejected rather than shipped.

### Agreed compromise
- **EMAIL fields**: fully automatic ("obligatorisch") - reliably detected via
  `InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`WEB_EMAIL_ADDRESS`.
- **PASSWORD fields**: reliably detected and always excluded - never learned from, never suggested into.
- **USERNAME**: no reliable field-level signal exists, so it is never auto-activated from field type alone.
  Two ways in instead:
  1. A new **weak signal** (`LoginFieldDetector.hasWeakUsernameSignal()`) checks the field's own
     `EditorInfo.hintText`/`fieldName` (an app-supplied placeholder string, set only sometimes, often
     localised) against a small username-keyword list (EN/DE plus a couple of others). When it fires, the
     settings row auto-opens and its new credential-mode button flashes - a *nudge*, never a silent switch.
  2. A new **manual toggle**: a fourth settings-row button (leftmost, ahead of the emoji button) always lets
     the user switch the current field into credential mode by hand, for any field the detection missed
     entirely - swipe up, tap the key-glyph button.

### New `credential/` package (pure + Android glue, mirroring the dictionary/store split)
`LoginFieldKind` (NONE/USERNAME/EMAIL/PASSWORD), `LoginFieldDetector` (pure `classify()` +
`hasWeakUsernameSignal()`), `CredentialEntry`, `CredentialRanking` (pure: prefix-match suggestions + domain-
frequency ranking), `CredentialStore` (SharedPreferences/JSON, mirrors `RecentEmojiStore`'s pattern - its own
private file, entirely separate from the ordinary dictionary and every other store).

### Learning (D-142 point 1: immediate, no D-37 threshold)
`CredentialStore.learn()` always reinforces on the very first observation - deliberate credential input is
precise by construction, unlike an ordinary new dictionary word the D-37 threshold guards against learning
too eagerly. Captured from two points: `handleEnter()` (the common case, right before the field's own submit
action can navigate away or clear the field) and `onFinishInput()` (catches leaving the field some other way,
e.g. tapping an on-screen "Login" button) - a `credentialCaptured` flag stops the second from double-
reinforcing the same observation. Reads the field's own committed text via
`InputConnection.getTextBeforeCursor()`, not `composing` - necessary because `finalizeAndCommit()`'s ordinary
per-fragment delimiter model would otherwise autocorrect/capitalise an email address piecemeal as it is typed
(every `.`/`@`/`-`/`_` is its own delimiter under the existing token model). New `commitLoginFieldFragment()`
is a `finalizeAndCommit()` short-circuit for every login-relevant field (USERNAME/EMAIL/PASSWORD): commits
each fragment exactly as typed, no autocorrect, no §6 capitalisation, no dictionary learning at all - the
whole value is captured once, separately, never per-fragment into the ordinary dictionary.

### Suggestions (D-142 points 2/3: a separate list, exclusively shown)
`refreshSuggestions()` branches to a new `showCredentialSuggestions()` for any non-`NONE` `loginFieldKind`,
entirely bypassing the ordinary dictionary/tier-1/tier-3 pipeline - a normal word is never offered in a login
field. Built and pushed directly to the suggestion bar via a new `SuggestionController.Kind.CREDENTIAL`
(mirrors the existing D-36 `CLIPBOARD` chip precedent - bypasses `SuggestionController.update()`/`displayed()`
entirely, since S-03's position stabilisation exists to smooth prose-typing flicker a short, freshly-ranked
credential list has no need for). Tapping a credential suggestion deletes whatever is already typed in the
field first (the value-so-far may span several already-committed fragments plus the live composing one)
before committing the full tapped value, so it replaces rather than appends onto it. A PASSWORD field shows
nothing at all - offering suggestions while typing a password would be a pointless information leak, on top
of never being learned from in the first place.

### Domain completion (D-142 point 4)
Once the value typed so far in an EMAIL field contains `@`, `showCredentialSuggestions()` switches to
`CredentialRanking.emailDomainsFor()` - the user's own most-used domains (summed frequency across every alias
on that domain, so several less-frequent aliases on one domain still correctly outrank a single more-frequent
one elsewhere), completing the address instead of matching whole saved addresses.

### Privacy
A new settings action (Info & Privacy category, `d142_clear_credentials`) deletes the whole credential store
in one confirmation dialog (single-confirm, unlike `reset_learning`'s double-confirm - far lower-stakes,
nothing about typing quality is affected by clearing it, unlike wiping the whole learned touch model).

27 new unit tests (`LoginFieldDetectorTest`, `CredentialRankingTest`, `CredentialStoreRoboTest`).
`AdaptKeyService`'s own wiring (field capture, suggestion branching, the settings-row button) is
service/`InputConnection`/view glue with no independently testable pure logic beyond what these cover - the
established testing gap for this layer. 649 unit tests total (was 622). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. **Not yet device-confirmed** - this is exactly the area (real login forms,
real apps' actual `EditorInfo`/hint-text behaviour) that can only really be judged on a real device.

## §83 - Two Icon Fixes, Reported While D-142 Was Being Device-Tested (v0.8.49)

### D-129 (Third Pass): the Sign-Flip Corner Hint Reused the Wrong Cue
Reported directly: the calculator minus key's sign-flip corner hint (§75/§31) is identical to
`MORE_ALTERNATIVES_GLYPH` (D-98's "◢", "a D-01 alternatives popup awaits") - but no popup ever appears for
this key, so the cue actively promises something that never happens. §75's own write-up already reused
`MORE_ALTERNATIVES_GLYPH` deliberately, "rather than inventing a bespoke glyph a user might mistake for a
committable character" - a reasonable-sounding reason at the time that turned out wrong in practice: reusing
the *wrong* existing cue is worse than a purpose-built one. Fixed with a new `SIGN_FLIP_GLYPH = "±"` (the
plus/minus ligature the user asked for directly) - `AdaptKeyboardView.drawKeys()`'s corner-hint branch now
checks `isSignFlipKey(key)` before the generic multi-alternative case, drawing `±` via the same `hintPaint`
position an ordinary single-hint key already uses (not the "more alternatives" triangle's styling). Private
drawing-only `View` code, no new test - the established gap for this layer.

### D-70/§70 (Follow-Up): the Clear-Clipboard Button's Trash-Can Icon Was Unclear
Reported: the settings row's clear-clipboard button (a bare 🗑) gives no indication of *what* it clears
without already knowing - "man wird diesen Knopf vermutlich nie drücken" (nobody would ever press it blind).
Asked for a "ligature" of the clipboard glyph and a small overlaid trash can. A true combined-glyph ligature
does not exist for an arbitrary emoji pair (Unicode ZWJ sequences are only defined for a curated, fixed set
of combinations - clipboard + trash is not one of them), so the practical equivalent was built instead: new
`SettingsRowView.badgedButtonFor(baseGlyph, badgeGlyph, onClick)` renders `baseGlyph` (📋) filling the whole
button with `badgeGlyph` (🗑) as a small badge - its own circular `keyboard_background`-coloured pill, for
contrast against the button's own background - in the bottom-right corner, the same visual language as an
app icon's notification badge. `clearClipboardButton` now uses this instead of the plain `buttonFor("🗑")`;
every other row button (emoji, gear, credential-mode) is unaffected. Private `View`-construction code, no new
test - same established gap.

649 unit tests (unchanged - both are Android view/drawing glue with no new pure logic).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

## §84 - §83 Refined: Badge Contrast Background Dropped (v0.8.50)

Device feedback on §83's clipboard+trash badge: the overlaid 🗑 reads clearly on its own - the circular
`keyboard_background`-coloured pill drawn underneath it was unnecessary and looks better removed.
`SettingsRowView.badgedButtonFor()`'s `badge` `TextView` no longer sets a `background` at all; everything
else (position, size, margin) is unchanged. No new tests - same established View-glue gap.

## §85 - D-142 Follow-Up: Weak-Signal Detection Now Also Covers Email, Not Just Username (v0.8.51)

### Reported: a `finanzen.net zero` field labelled "E-Mail-Adresse" did not activate credential mode at all
Device-tested directly: the field's on-screen label was unambiguously "E-Mail-Adresse", yet neither the
reliable `InputType`-based detection nor the weak-signal nudge fired. The user could not tell from the device
alone whether the app's `InputType` genuinely lacks the email variation, or whether the weak-signal fallback
simply never fires at all - both remain open questions this environment cannot resolve without further device
feedback, but a concrete, real gap was found by re-reading the code rather than guessing blindly: **§80/§82's
weak-signal detector (`LoginFieldDetector.hasWeakUsernameSignal()`) only ever checked username-style
keywords ("username", "benutzername", …), never anything email-related at all.** A field labelled purely
"E-Mail-Adresse" - with no "username"/"login"/etc. wording anywhere - could never have triggered the nudge
regardless of what its `hintText`/`fieldName` said, even on a device where the fallback mechanism itself
works perfectly. This alone fully explains the reported symptom if `InputType` genuinely did not classify the
field (plausible - plenty of real apps use a plain text field with their own client-side email validation
instead of setting the OS-provided variation); it may not be the *only* mechanism at play, so the fix is
reported back for another device round rather than claimed as a confirmed complete resolution.

### Fixed: the weak signal now recognises email keywords too, not just username ones
`LoginFieldDetector.hasWeakUsernameSignal(hintText, fieldName): Boolean` is now
`weakSignalKind(hintText, fieldName): LoginFieldKind` - checks a new `EMAIL_KEYWORDS` list ("email",
"e-mail", "mailadresse", plus the German/Greek/Spanish/French equivalents already following this class's
existing multi-language pattern) before the existing `USERNAME_KEYWORDS` list, so "E-Mail-Adresse" itself now
matches directly. Email wins when a hint mentions both (e.g. "Username or email") - the more specific,
actionable read, since only `LoginFieldKind.EMAIL` gets domain completion.

`AdaptKeyService` threads the richer result through: `weakSignalKind: LoginFieldKind` (was
`weakUsernameSignal: Boolean`) still only drives the settings-row auto-open + button flash *nudge*, never
suggestion behaviour by itself, matching the original design exactly - only what it can activate *into*
changed. New `credentialModeManuallyActivated` flag lets `toggleCredentialModeFromSettingsRow()` activate the
field as the *specific* kind the weak signal suggested (so a nudged email field gets real domain completion
once confirmed, not just a generic username fallback) while still correctly toggling back off only a
manually-activated mode - a reliably-detected EMAIL/PASSWORD field stays non-user-toggleable exactly as
before.

3 new/expanded `LoginFieldDetectorTest` cases (including one named directly after the reported repro). 652
unit tests (was 649; +3). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Still needs a device round**
to learn whether this alone resolves the reported case, or whether `InputType` detection itself also needs a
closer look for this specific app.

## §86 - D-110 Picked Up: a Real, Confirmed Bug Found and Fixed - Not Claimed as the Full Explanation

Investigated per direct request ("schau mal, was du bei D-110 tun kannst"). Traced the two candidate causes
D-110's own original write-up named - both **ruled out directly against the actual code**, not guessed:

- "a filter/no-suggestions variant treated as skip-auto-cap-entirely": grepped `AdaptKeyService.kt` for any
  `TYPE_TEXT_FLAG_NO_SUGGESTIONS`/`TYPE_TEXT_VARIATION_FILTER` handling - none exists anywhere in the file.
  There is no code path that could special-case such a variant at all, so this is not the mechanism.
- "`getCursorCapsMode()`/`EditorInfo` mishandled at the `CapitalisationEngine` call site": `CapitalisationEngine.
  capitalise()` checks `context.capsMode` only for the `CHARACTERS`/`WORDS` special cases; `context.sentenceStart`
  (from the independent `SentenceBoundary` check) is consulted regardless of `capsMode`, exactly matching the
  spec's own stated intent ("AdaptKey's own rules... apply regardless of what the target field requests"). This
  call site is not the defect either.

### A real, separate bug found instead: `ShiftGrace.autoArmAtWordStart()` silently dropped sentence-start arming for `CapsMode.NONE`
```kotlin
CapsMode.WORDS, CapsMode.CHARACTERS -> true
CapsMode.SENTENCES -> sentenceStart
CapsMode.NONE -> false   // always false, regardless of sentenceStart - the actual bug
```
A field that declares **no** caps flag of its own at all (`CapsMode.NONE`) - exactly what a plain, unadorned
message box (a very plausible shape for eBay Kleinanzeigen's message field) would report - never armed Shift
at a sentence start, even though `CapsMode.SENTENCES` (the field *explicitly* asking for sentence-case) did.
This directly contradicts the project's own stated design and was even encoded as an *intended* test
(`ShiftGraceTest`'s `` `NONE never auto-arms` ``) rather than caught as a defect - the kind of thing that only
surfaces once a concrete failing app is reported. Fixed by folding `NONE` into the same branch as `SENTENCES`:
both now arm Shift exactly when `sentenceStart` is true, matching `CapitalisationEngine`'s own already-correct
independent handling.

**Honest caveat, not glossed over**: this is a confirmed, real bug, worth fixing on its own merits regardless
of D-110 - but tracing `CapitalisationEngine.capitalise()`'s commit-time logic shows the *final committed*
word should already have been corrected independently of this bug (`context.sentenceStart` is checked there
too, unconditionally, the moment `explicitFirstUpper` is false). So this fix should visibly correct the
*live-typed* character and the Shift key's own visual state at a sentence start in any field with no caps
flag - a real, felt difference either way - but it is **not proven** to be the complete explanation for "the
committed word itself never ends up capitalised" as D-110 originally described. It may be the whole story
(if something about this app's own `InputConnection` behaviour interacts with the visual/live-typed state in
a way not modelled here) or only part of it.

### A temporary diagnostic added to close the loop either way
D-110's own original write-up already named the blocker: "needs the actual field's reported `EditorInfo`
... to make progress rather than guessing further." A one-line `Log.d("AdaptKey", ...)` in `onStartInput()`
now logs `packageName`/`fieldName`/`inputType` (hex)/`hintText` for every focused field - retrievable via
`adb logcat -s AdaptKey:D` while focusing the eBay Kleinanzeigen field. This finally supplies the concrete
data point the investigation has been missing, regardless of whether the `ShiftGrace` fix alone resolves it.
Clearly labelled as temporary in its own comment - remove once D-110 is closed for good, mirroring how D-09's
own diagnostic was eventually torn out once it had served its purpose.

1 existing `ShiftGraceTest` case corrected (was asserting the bug as intended behaviour, now asserts the fix).
652 unit tests (unchanged - a one-line change to an already-tested pure function).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. **Not yet device-confirmed either way.**

## §87 - D-139: Diagnostic Logging Added While Typing (v0.8.53)

Requested directly, following §86's `AdaptKey:D` diagnostic - the user reports D-139 ("occasionally the whole
text jitters and its characters get scrambled") happens "nicht selten" (fairly often) but is not immediately
reproducible with the exact same text again, i.e. genuinely timing-dependent, exactly the shape §73/§76's own
suspected mechanism (a self-triggering `onUpdateSelection()` cascade in the D-62/D-87 mid-word-reclaim
machinery) would produce. A new `Log.d`/`Log.w` tag, `AdaptKeyJitter` (kept separate from D-110's `AdaptKey`
tag, so either can be filtered independently - `adb logcat -s AdaptKeyJitter:D` to watch only this), logs at
every point §76's own trace identified as load-bearing for the suspected cascade:

- **`onUpdateSelection()`** - every call: timestamp, old/new selection, `candidatesStart`/`candidatesEnd`,
  and the live `composing`/`composingAnchor`/`composingCursor` state. A second line logs the `ownEdit`
  determination and, when it comes out false, the exact `composing` string about to be wiped
  (`finishComposingText()` + `clearComposing()`) - precisely the branch that would produce a visible
  jitter/scramble if it fires when it should not.
- **`CallbackBurstGuard` tripping** - previously a silent early return; now an explicit `Log.w`, since this
  firing at all is close to a smoking gun for the suspected cascade actually happening.
- **`reclaimSurroundingWord()`** - every *actual* reclaim (not the common no-op): the `before`/`after`
  fragments it is about to splice into `composing`, right before the `deleteSurroundingText()` call §32/D-87
  already found risky once before.
- **`updateComposing()`** - the exact string pushed to the field as composing text on every call. A
  scrambled/reordered result should show up directly in this sequence of log lines, in order.
- **`finalizeAndCommit()`** - `typed` vs. the actual `finalWord` permanently written to the field, so a
  corruption that only shows up in the *committed* text (not just while composing) is still caught.

Deliberately not gated behind a settings toggle or `BuildConfig.DEBUG` check (consistent with §86's own
`AdaptKey` log) - `Log.d`/`Log.w` output stays on-device in logcat, never transmitted anywhere, matching the
app's existing "provably offline" posture; capturing it requires physical/`adb` access to the device, the
same bar D-09's own (now-removed) raw-tap-recording diagnostic set. Composing/committed text content is
logged verbatim, including whatever the user is actually typing - worth knowing before sharing a captured
logcat with anyone, though it never leaves the device on its own. Clearly labelled as temporary in every
comment - to be removed once D-139 is closed for good, the same lifecycle as D-09 and (eventually) §86's own
`AdaptKey` log.

No new tests - pure Android logging calls with no decision logic of their own. 652 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green.

## §88 - D-139/D-110: In-App Diagnostic Log Replaces the Need for `adb logcat` (v0.8.54)

Requested directly: §87's `adb logcat` approach requires the phone to stay tethered to a PC while a repro
happens - impractical for D-139 specifically, since the user reports the jitter happens somewhat often during
*normal* daily use, not during a deliberate desk-testing session, and re-tethering after the fact loses
whatever logcat already discarded. Asked for an in-app, self-managing alternative instead: a FIFO log that
drops anything older than 5 minutes on its own, toggled by a setting, viewable and exportable from within the
app.

### New `diagnostics/` package
- `DiagnosticEntry` - one recorded line (a monotonic timestamp + message).
- `DiagnosticRingBuffer` - pure, JVM-unit-tested (7 tests, synthetic timestamps): appends an entry and prunes
  everything older than a configurable retention window *relative to the newest entry's own timestamp*, so
  pruning behaves correctly regardless of real wall-clock gaps between recordings.
- `DiagnosticLog` - the Android-facing singleton wrapping it with the real clock
  (`SystemClock.elapsedRealtime()` - monotonic, resets on reboot, never a calendar timestamp), a 5-minute
  retention window, and an `enabled` flag mirroring the new setting (turning it off also clears whatever was
  recorded, not just stops adding to it). `record()` is a cheap no-op unless enabled, so call sites never need
  their own guard.

### Settings: a new "Diagnostics" category
A new `diagnosticLogEnabled: Boolean` (default off - it records raw typed text) threaded through the full
existing pipeline (`RawSettings`/`AdaptSettings`/`SettingsMapper`/`SettingsStore`, `d_diag_enabled`, applied
live in `applySettings()`), plus a second row (`d_diag_log`) opening the new `DiagnosticLogActivity`. Both
sit in the settings screen's own new category, kept separate from "Info & Privacy" for visibility, though the
toggle's own summary text states plainly what it records and that it is never written to disk or transmitted
unless explicitly shared.

### `DiagnosticLogActivity` - the viewer/export screen
Refreshes its content on every `onResume()`, not just `onCreate()` - so leaving the keyboard to reproduce an
issue and coming back to Settings picks up whatever was recorded meanwhile, no relaunch needed. Each entry is
rendered with a relative age (`-4.2s`, computed against the same monotonic clock) rather than a wall-clock
timestamp - directly answers "how long before I opened this screen did that happen" without the user having
to correlate calendar time by hand. Two export paths: `Intent.ACTION_SEND` (plain text, share-sheet chooser,
no storage permission - the same D-09 raw-tap-recording precedent this project has used before) and a direct
clipboard copy for pasting straight into a chat. A `Clear` button wipes the buffer on demand.

### §87's `Log.d`/`Log.w` calls now feed both logcat and the in-app log
New private `AdaptKeyService.diag(tag, message, warn)` routes every one of §86/§87's seven diagnostic call
sites (`onStartInput`, `onUpdateSelection` ×3, `finalizeAndCommit`, `updateComposing`,
`reclaimSurroundingWord`) to both `Log.d`/`Log.w` (kept for anyone who does have `adb` set up) and
`DiagnosticLog.record()` - one shared helper instead of duplicating the routing at each site. No call site's
own diagnostic content changed, only where it goes.

8 new unit tests (7 `DiagnosticRingBufferTest`, 1 `SettingsMapperTest`). 660 unit tests total (was 652).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. The Activity/settings-wiring itself is Android view/
service glue with no independently testable pure logic beyond `DiagnosticRingBuffer` - the established
testing gap for this layer. Not yet device-confirmed - next time the jitter happens, the in-app log (Settings
→ Diagnostics → "View / share diagnostic log") should have it, no PC required.

## §89 - D-143 Implemented: a Dedicated URL-Mode Bottom Row (v0.8.55)

Planned in a design conversation before implementation, per D-143's own "needs its own design pass" note
(§80). The main open questions from that capture were resolved directly with the user rather than guessed:
this is **not** a new `?123`-style page - it stays on the letters surface (a URL still needs the full
alphabet for its domain/path) and only swaps the bottom row, exactly like the existing `qwerty`/`greek`
toggles already swap other parts of the same layout without a separate `InputSurface`.

### New URL-mode bottom row, replacing the ordinary comma/space/period trio
`[?123] [https://] [www.] [/] [space, narrow] [.] [⏎]` - built by a new, shared
`KeyboardLayout.urlBottomRow()` (also called by `GreekLayout`, since URL entry doesn't depend on the active
typing alphabet - both layouts show identical URL keys):
- **`/` takes over the comma key's own primary position.** Its long-press popup is the original
  `COMMA_ALTERNATIVES` list with `/` simply prepended as the new pre-selected character - the comma swaps
  from primary to alternative, staying fully reachable, with nothing else about the popup changed.
- **A new `https://` key** (§53 `KeyCode.TEXT`, mirroring the calculator page's `sin`/`deg` keys), whose
  long-press popup offers the other everyday protocols: `http://`, `ftp://`, `file://`.
- **A new `www.` key** (also `KeyCode.TEXT`), with no long-press action of its own yet.
- **The space key shrinks drastically** (a URL practically never needs one) - the freed width funds the two
  new keys above; it is labelled with the plain space glyph (`SPACE_GLYPH`, already used by the `?123` pages
  for the same reason) rather than the active-language label, since it is both too narrow for that and not
  a meaningful place to switch language from (see the G-01 gesture point below).
- **The full-stop key's long-press popup is a locale-resolved TLD list instead of the ordinary sentence
  terminators** (`! . ?`) - a URL practically never needs `!`/`?`. New `UrlLocale` object (`keyboard`
  package) mirrors `CalculatorLocale`'s JDK-locale-driven approach rather than a hand-maintained worldwide
  table: English-language locales lead with the two generic TLDs (`.com`, `.org`), then the locale's own
  country-code TLD when one is idiomatically used for casual addresses; every other locale leads with its
  own ccTLD instead, then `.com`/`.org`. A small `CCTLD_OVERRIDES` map corrects the two cases this reasoning
  actually calls for, not an attempt at exhaustive worldwide coverage: Great Britain uses `.co.uk`, not the
  bare `.uk`, for ordinary sites, and the United States has no everyday casual ccTLD at all (`.us` is not how
  Americans think of their own sites, unlike `.de` for Germans). Resolved from the same `systemLocale`
  property `AdaptKeyboardView` already threads through for the calculator page's currency/decimal-separator
  keys (D-92) - no new locale plumbing needed.

### Trigger: auto-open only, not part of the D-19 swipe cycle
New `AdaptKeyService.isUrlField(info)` checks `EditorInfo.inputType` for `TYPE_TEXT_VARIATION_URI` (compared
against the real, unmasked `InputType` constant - see the bug note below), mirroring `initialSurfaceFor()`'s
existing phone/number/datetime-class check but as an orthogonal `urlMode` flag rather than a surface switch,
since it stays on `LETTERS`. Set fresh per field in `onStartInput()` (pushed to the view *before*
`setSurface()`, so its own `rebuildRows()` already reads the correct value) and defensively re-pushed in
`onStartInputView()` in case the view was recreated in between - the same two-point pattern
`greek`/`qwerty` already use for their own per-session state.

### Autocorrect/suggestions: suppressed entirely, reusing the D-142 credential-field precedent
`finalizeAndCommit()`'s existing login-field short-circuit (`if (loginFieldKind != LoginFieldKind.NONE)`)
now also covers `urlMode` - a domain/path is exactly as much "not prose" as a username/email value, and the
existing fragment-committer already did precisely what a URL needs (commit verbatim, no autocorrect, no §6
capitalisation, no token-repair, no dictionary learning), so no new commit logic was needed, only a wider
condition. Renamed `commitLoginFieldFragment()` → `commitVerbatimFieldFragment()` since it now serves both
features (the function itself was already login-agnostic - purely mechanical). `refreshSuggestions()` gets
its own new, simpler bypass (`if (urlMode) { clearSuggestions(); return }`) rather than reusing
`showCredentialSuggestions()` - a URL field has no candidate list of its own to offer, unlike a login field's
saved credentials, so the bar just stays empty. The D-36 clipboard-paste chip is deliberately **not**
suppressed - pasting a copied link into a URL field is exactly the kind of thing it exists for.

### G-01 language swipe: the URL-mode space key is treated like a symbol-page space key
`KeyGesture.resolve()` gained a `urlMode` parameter, extending the existing D-92 surface check
(`surface == LETTERS`) to `surface == LETTERS && !urlMode` for the space-key branch - a horizontal swipe on
the narrow URL-mode space key falls through to the ordinary D-19 surface swipe instead of switching
language, the same reasoning D-92 already applied to the smaller `?123`-page space keys ("no language of its
own to switch"), even though URL mode itself stays on the letters surface rather than a different one.

### Real bug found and fixed while building D-143's own field-variation check, not guessed
Verified `isUrlField()`'s bitmask logic against the real Android SDK first (`javap` against `android-35`'s
`InputType.class`, this project's established habit before trusting a constant): `TYPE_MASK_VARIATION`
(`0x0ff0`) does **not** include the `TYPE_CLASS_TEXT` bit (`0x0001`), so the value `classify()`-style code
actually receives from `inputType and TYPE_MASK_VARIATION` never carries that bit either. Checking
`LoginFieldDetector` for the same pattern (needed to avoid repeating whatever mistake it might have made)
found exactly that mistake: its five variation constants (`VARIATION_EMAIL_ADDRESS` etc.) had the class bit
baked in (`0x21` for EMAIL, when the real masked value is `0x20`) - so `classify()` could **never** match a
real field's variation and silently returned `LoginFieldKind.NONE` for every field, regardless of its actual
type. This plausibly explains §85's `finanzen.net zero` report on its own (not only the already-documented
"no variation exists for username at all" limitation) - the "reliable" EMAIL/PASSWORD detection was never
reliable in practice, only in its own unit tests (which passed the same class-bit-inclusive literals
directly, proving the constants self-consistent but never checking them against what a real masked
`EditorInfo.inputType` value actually looks like). Fixed: all five constants corrected to their bare,
class-bit-free values, verified against the real SDK (`TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 32`,
`_PASSWORD = 128`, `_VISIBLE_PASSWORD = 144`, `_WEB_EMAIL_ADDRESS = 208`, `_WEB_PASSWORD = 224`); the
existing `LoginFieldDetectorTest` literals corrected to match. **Not yet device-confirmed** whether this
alone resolves §85's report - the weak-signal email/username keyword fallback (§85's own fix) still applies
regardless and may have been doing all the real work on some devices; this fix means the *reliable* path can
finally also engage on devices where it was silently dead before.

15 new unit tests (`UrlLocaleTest` 6, `KeyboardLayoutTest` 6, `GreekLayoutTest` 2, `KeyGestureTest` 1). 675
unit tests total (was 660). `:app:assembleDebug`/`:app:testDebugUnitTest` green. The pure `UrlLocale`/
row-shape logic is fully unit-tested; the `AdaptKeyService` wiring (`isUrlField`/`onStartInput`/
`onStartInputView` pushes, the `finalizeAndCommit`/`refreshSuggestions` short-circuits) is service glue with
no independently testable logic beyond what those cover - the established testing gap for this layer. Not
yet device-tested - the new key weights (`https://`/`www.`/the narrower space) are a considered starting
guess, not tuned against a real screen, easy to retune later (same precedent as §36/§37/§53).

## §90 - D-144: Swipe-Down-to-Dismiss Now Also Reacts on the Suggestion Bar and the Settings Row (v0.8.56)

### Bug fixed: G-03's downward swipe only ever reacted on the keyboard's own key field
Reported directly: swiping down to close the §48 settings row (or, once already closed, to dismiss the
keyboard entirely) only worked when the swipe started on `AdaptKeyboardView` itself - not on the suggestion
bar (`SuggestionBarView`) or the settings row (`SettingsRowView`) stacked above it. Confirmed by reading the
actual touch-dispatch architecture rather than assuming: `AdaptKeyboardView.onTouchEvent()`'s own
`resolveSwipe()` is the *only* place in the whole app that ever recognised a swipe gesture at all -
`SuggestionBarView` only ever intercepted an upward drag (G-04, drag-to-trash) and otherwise deferred
everything to `HorizontalScrollView`'s own scrolling; `SettingsRowView` had no touch handling of its own
whatsoever beyond its buttons' plain click listeners. Since these are three separate sibling views in the
same root `LinearLayout`, a touch starting on either of the other two never reached
`AdaptKeyboardView`'s touch handler at all - there is no Android mechanism that would forward it there.

### Fix: the same pure `SwipeGesture` classifier, wired into both sibling views
Both `SuggestionBarView` and `SettingsRowView` gained the exact two-stage claim-early/confirm-late
interception pattern `AdaptKeyboardView.resolveSwipe()` and (for G-04) `SuggestionBarView` itself already
used: `onInterceptTouchEvent()`'s `ACTION_MOVE` branch claims the gesture (`swipeDownIntercepting = true`)
the moment `SwipeGesture.classify(dx, dy, interceptThresholdPx)` (the small system touch-slop) reports a
downward-dominant movement - stealing it away from whichever child would otherwise track it as a button
press or, for the suggestion bar, from `HorizontalScrollView`'s own horizontal-scroll interception; `
onTouchEvent()`'s `ACTION_UP` then only actually fires a new `OnSwipeDownListener.onSwipeDown()` once the
full travel re-confirms `SwipeDirection.DOWN` against a larger `swipeDownThresholdPx` (110dp, matching
`AdaptKeyboardView`'s own `fieldSwipeThresholdPx` so the gesture feels the same everywhere it can start).
No new pure logic was needed - both views reuse the already-tested `gesture.SwipeGesture`/`SwipeDirection`
classifier directly, the same one the keyboard body's own dismiss gesture is built on.

`SuggestionBarView`'s new interception fires regardless of `dragWord` (unlike G-04, which only ever arms
over an ordinary suggestion chip, never the verbatim one) - so a downward swipe starting over the verbatim
chip or an empty gap is caught too, matching "swipe down anywhere on the bar."

### Wiring: one shared dismiss function, not three copies
`AdaptKeyService.handleSwipe()`'s existing `GestureAction.DISMISS_KEYBOARD` branch (`if (settingsRow?.isOpen
== true) closeSettingsRow() else requestHideSelf(0)`) was extracted into a new private
`dismissKeyboardOrCloseSettingsRow()`, now called from all three sources: the keyboard body (unchanged
behaviour, just refactored), the new `SuggestionBarView.onSwipeDown`, and the new
`SettingsRowView.onSwipeDown` - so "close the row first, only dismiss on a second swipe once it's already
closed" is one rule in one place, not duplicated per view.

### Investigated but not changed: the reported "(exkl. Space)" exclusion
The report also described the previously-working scope as "the key field, excluding the space bar" - traced
this specifically before writing it off, since a real, separate space-bar-only exclusion would need its own
fix. Found no code path that treats `KeyCode.SPACE` any differently for a downward swipe: `KeyGesture.
resolve()`'s `if (direction == SwipeDirection.DOWN) return GestureAction.DISMISS_KEYBOARD` fires
unconditionally, before any key-specific branching (including the `KeyCode.SPACE` case further down, which
only ever special-cases *horizontal* swipes for G-01); `AdaptKeyboardView.resolveSwipe()`'s own threshold
computation is likewise keyCode-agnostic for a vertical swipe. Not dismissed as user error either - simply
not reproducible from the code as described, the same honest "traced, not found, needs a repro" outcome
this project has reached before for a code-grounded-but-unconfirmed report (D-118's precedent). If this
persists, it needs a repro isolating it from the (now fixed) suggestion-bar/settings-row gap.

No new unit tests - both changes are Android view/touch-dispatch glue reusing the already-tested,
already-pure `SwipeGesture`/`SwipeDirection` classifier, the established testing gap for this layer (same
class as D-64's own drag-to-trash interception, never itself unit-tested beyond the pure `DragToTrash`
object it wraps). 675 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

## §91 - Device Feedback on §89/§90: D-145 (URL Popup Fixes), D-146 (Double Space), D-147 (Umlaut Prefix), D-144 Follow-Up (v0.8.57)

Batch of device-feedback findings from testing §89/§90, released together per explicit user instruction.
Confirmed: swipe-down-to-dismiss now works on the suggestion bar (§90's fix); D-144's own settings-row half
still doesn't react on device - see its own section below.

### D-145 - Two Corrections to the D-143 URL-Mode Popups
Two related device-feedback points on the URL-mode keys' long-press popups:

1. **The slash and period keys' own popups still redundantly listed themselves.** `URL_SLASH_ALTERNATIVES`
   (§89) prepended `/` to the comma family, and `UrlLocale.periodAlternatives()` prepended the bare `.` to
   its TLD list - both defensible-looking at the time (matching the existing comma/period-family convention
   of including the key's own character), but wrong here specifically: a plain tap already commits `/` or
   `.`, so repeating either in its own long-press popup offers nothing new, just a wasted cell. Fixed:
   `URL_SLASH_ALTERNATIVES` is now exactly `COMMA_ALTERNATIVES` (no `/` entry at all - the comma still sits
   in its own established position within that list, simply no longer displacing anything);
   `UrlLocale.periodAlternatives()` now returns only the TLD suggestions, never the bare `.`. Both keys still
   pre-select sensibly on a straight release - `preSelectedIndexFor()`'s existing fallback-to-index-0
   behaviour (already used for keys like `o`'s umlaut popup, whose own character isn't in its alternatives
   list either) picks the first popup entry when the key's own character isn't found in it, so no separate
   change was needed there.
2. **Popup cells were a fixed 40dp wide, too narrow for wide content.** Every D-01 long-press popup
   (`AdaptKeyboardView.popupCellWidthPx`) used one uniform width for every cell, sized for a single glyph -
   fine for `!`/`.`/`?`, badly cramped for `https://`/`ftp://`/`.co.uk`. Fixed by sizing each cell to its own
   text content: `AdaptKeyboardView.popupCellWidthFor(text)` measures the string via the existing
   `textPaint` and floors it at a minimum (`popupCellMinWidthPx`, still 40dp, so a lone glyph keeps a
   comfortably tappable cell), computed once per popup open (`openPopup()`) into a new `popupCellWidths: List
   <Float>`. This needed `HorizontalLongPressPopup` (the pure row-layout/hit-testing object) generalised from
   a single `cellWidth: Float` to a `cellWidths: List<Float>` throughout - `rowLeft()` now sums the widths
   before the pre-selected cell to find its centre offset, and `selectedIndex()` walks the cumulative cell
   spans instead of dividing by a uniform width. Benefits every existing multi-alternative popup with
   multi-character content, not only the URL keys - the calculator page's `sin`/`cos`/`tan`/`log` popup (§53)
   was cramped the same way and is fixed by the same change, with no separate call site work.

9 new/updated unit tests (`HorizontalLongPressPopupTest` gained variable-width cases, `KeyboardLayoutTest`/
`UrlLocaleTest` updated for the no-self-char popups). No test regressions. `:app:assembleDebug`/
`:app:testDebugUnitTest` green. Not yet re-confirmed on device.

### D-146 - Applying a Suggestion Mid-Text No Longer Creates a Double Space
Reported: tapping a bar suggestion while editing a word that already has more text after it (not at the very
end of the field) left a doubled space behind. Root cause, confirmed directly in `onSuggestionClicked()`'s
`NORMAL` branch: `ic.commitText(word + " ", 1)` unconditionally appended its own trailing space, with no
check for whatever already followed the caret - correct at the end of a field (nothing follows), wrong
whenever real text already continues right after the reclaimed word (D-62 pulls the *whole* word being
edited into `composing`, but never the space *after* it, which stays untouched, real document content).
Fixed by checking `InputConnection.getTextAfterCursor(1, 0)` (read while the old composing span is still in
place, i.e. before the `commitText()` call, so it reflects the untouched, real surrounding text) - a space
already there means no second one is added. `pendingSuggestionSpace` (D-29's "punctuation eats the trailing
space" arming) is now only set when this code actually added its own space, not when one was already there -
eating a pre-existing, real document space on the next punctuation keystroke would have been a new, separate
bug. No new unit tests - `AdaptKeyService`/`InputConnection` glue with no independently testable pure logic,
the established gap for this layer.

### D-147 - Umlaut/ß-Aware Prefix Completion for Suggestions
Reported: typing `tatsachl` (skipping the umlaut, a deliberate typing shortcut - "ich bin oft einfach zu
faul, den Umlaut explizit zu tippen") never suggests `tatsächlich`, even though the prefix is otherwise
correct. Confirmed as a real, previously-unnoticed gap, not guessed: `DictionaryStore.unigramsByPrefix()`
(both `InMemoryDictionaryStore` and `SqliteDictionaryStore`) does a **literal** prefix match (`key.startsWith`
/ `wkey LIKE 'prefix%'`) with no umlaut/ß folding at all - and D-12's existing `fuzzyNeighbours()` umlaut
folding doesn't help either, since it only matches candidates close to the *same length* as the whole token,
never a genuinely shorter *prefix* of a longer word. Neither of the two existing suggestion mechanisms could
ever have reached this case. **Confirmed this is a real regression against this project's own founding
principle, not a new feature being requested**: per direct instruction, re-checked the original spec (§1,
before any device-feedback D-series existed) - the "Guiding Principle - Umlauts Are Ordinary Characters"
text explicitly lists "suggestions" among the features this principle is meant to shape, and states an
umlaut-blind keyboard is "simply broken" from a German user's perspective; literal-only prefix matching is
exactly that blind spot, just never exercised by a *prefix* (only ever by D-12's whole-word fuzzy matching)
until now.

**Fixed** with a new `Umlaut.unfoldCandidates(text)` (7 new tests) - the exact reverse of the existing
`Umlaut.fold()`: generates every plausible pre-folding spelling of a typed (already-lowercased) prefix, so
`"tatsachl"` yields `"tatsächl"` among its candidates (and `"tatsachl"` itself, always first - the
overwhelmingly common case where nothing needs unfolding costs exactly the one query it always did).
Combinatorial in the number of fold-eligible positions - every `a`/`o`/`u` may or may not have originally
been `ä`/`ö`/`ü`; every `ss` run (never a lone `s`, which `ß` never folds from) may or may not have
originally been `ß` - bounded by a generous cap (`MAX_CANDIDATES = 32`) that real typed prefixes are not
expected to reach in practice, only guarding against a pathological worst case.
`DictionarySuggestionProvider.suggestionsFor()`'s prefix-completion loop now queries `store.unigramsByPrefix()`
once per unfold candidate instead of once for the literal token alone, merging results (a word already found
via an earlier variant is not re-added). 2 new `DictionarySuggestionProviderTest` cases confirm both the
umlaut case (`tatsachl` → `tatsächlich`) and the ß case (`strass` → `Straße`, which needs the *`ss`→`ß`*
direction specifically, since `"strass"` is not even a literal prefix of `"straße"` at all).

9 new unit tests total (`UmlautTest` 7, `DictionarySuggestionProviderTest` 2). `:app:assembleDebug`/
`:app:testDebugUnitTest` green.

### D-144 Follow-Up - Settings-Row Swipe-Down Still Doesn't React (Investigated Again, Not Reproduced)
Reported: after §90's fix, swipe-down-to-dismiss works on the suggestion bar but still doesn't react on the
settings row itself. Re-read `SettingsRowView`'s `onInterceptTouchEvent()`/`onTouchEvent()` line by line
against `SuggestionBarView`'s own (confirmed-working) equivalent - the two are structurally identical for
the swipe-down path (claim early at the small system touch-slop via `SwipeGesture.classify()`, confirm late
at the 110dp threshold on release), and `SettingsRowView`'s bounds/layout were independently confirmed
correct by the fact that its buttons already register taps reliably. No asymmetry was found between the two
views' touch-dispatch logic, and Android's own documented touch-interception contract (once
`onInterceptTouchEvent()` claims a gesture, every subsequent event for that pointer routes to the same
view's `onTouchEvent()`, regardless of where the raw coordinate later ends up relative to that view's own
bounds) does not suggest a reason this would behave differently from the bar. **Not fixed - no code-level
cause found**, reported back rather than guessed at further (the same honest "traced, not found" outcome as
D-118 and this session's own "(exkl. Space)" investigation, §90). Needs a more specific repro to make
progress: does this reproduce with the row fully settled open (not mid-swipe-up-animation); does starting
the downward swipe directly on a button behave differently from starting it in a gap between buttons.

### D-148 - Captured, Not Designed: A-07 Backspace-After-Suggestion Semantics Need Clarification
Two related reports about Backspace behaviour after a suggestion is applied:

- "When I explicitly apply a suggestion [tap a bar chip], Backspace must not revert it - I usually just want
  to remove the automatically-inserted trailing space."
- "If only one word is in the field, followed by a space, and I press Backspace there, the whole word gets
  deleted immediately."

Traced both plausible mechanisms directly, not guessed: **a bar-tap** (`onSuggestionClicked()`'s `NORMAL`
branch) already calls `clearUndo()` at its own top and never re-arms `undoTyped`/A-07's undo state
afterwards - confirmed via every `undoTyped =` assignment site in the file, only two exist, both inside
`finalizeAndCommit()`'s own autocorrect-on-commit path and `applySplit()`/`applyMerge()`, never in
`onSuggestionClicked()`. So a plain Backspace right after a bar-tap should already fall through to the
ordinary `deleteOneBefore()` path (removes exactly the one trailing space character, via
`deleteSurroundingText(1, 0)`) - not a revert. **A-07 itself** (`performAutocorrectUndo()`), by contrast, is
a long-standing, deliberately-designed feature (since v0.7.9, refined by D-13/D-37/D-140): when a *silent*
autocorrect-on-commit fires (typing a delimiter after a word autocorrect changes), a single Backspace
*intentionally* restores the originally-typed word instead of deleting a character - this is not a bug, it
is what A-07 has always been asked to do. The two reports could describe either: (a) a case the tracing above
doesn't yet explain (needs a precise repro to find), or (b) a request to actually *change* A-07's own
long-standing behaviour so it never restores the typed word any more, only ever removing the trailing
delimiter, even after a genuine silent autocorrect. Given how foundational A-07 is, and this project's own
precedent of confirming a real behavioural change with the user before touching one (see D-107's still-open
highlight-semantics reversal, deliberately left undesigned until confirmed) - **not implemented either way
this round**, captured here pending clarification: is this about the bar-tap path specifically (in which
case a fresh, precise repro is needed, since the current code should already behave as wanted), or a genuine
request to change what a Backspace does after any autocorrect, silent or tapped?

## §92 - Confirmations on §91, D-144/D-145 Follow-Ups, D-148 Withdrawn (v0.8.58)

Device confirmation on §91's batch: **D-146 (double space mid-text) and the single-word-plus-space backspace
case both confirmed working.** **D-148 withdrawn** - the user can no longer reproduce either backspace-after-
suggestion report and will re-open it with a fresh repro if it recurs; nothing was implemented for it, matching
§91's own "captured pending clarification, not guessed at" stance.

Two precise repro refinements on the still-open items drove both fixes below - both traced to a real,
concrete, previously-unconsidered mechanism, not guessed at.

### D-145 - Corrected: the Locale-Driven TLD Must Be the *Pre-Selected* Entry, Not Merely Listed First in Prose Order
§91's `UrlLocale.periodAlternatives()` fix already dropped the redundant bare `.`, but kept the *original*
D-143 ordering rule ("English leads with the generic TLDs, then its own ccTLD") - reported back as still
wrong: since the D-01 popup always **centres its pre-selected cell directly over the stem key**
(`HorizontalLongPressPopup.rowLeft()`, and `preSelectedIndexFor()`'s existing index-0 fallback whenever the
key's own character isn't itself in the alternatives list, true here since `.` was just removed from it),
whichever entry sits at index 0 is the one reachable by a straight release with **zero finger movement** -
by far the most-favoured position, not merely "read first". The locale-specific ccTLD - precisely the one
suggestion tailored to *this* user's own country - must be that entry, for every locale, not only non-English
ones. Fixed by dropping the English/non-English branch entirely: `periodAlternatives()` now always leads with
the ccTLD when [UrlLocale] resolves one, falling back to `.com` only when it doesn't (e.g. `en_US`, which has
no idiomatic ccTLD at all per the existing `CCTLD_OVERRIDES` entry). `en_GB` now leads with `.co.uk` instead
of `.com` - the opposite of §89's original design intent, corrected per this direct clarification of *why*
pre-selection position matters more than list-reading order. Tests updated to match (`UrlLocaleTest`).

### D-144 Follow-Up - Fixed: Swipe-Down Never Registered on an Empty Bar/Row Background
§90/§91 traced the suggestion-bar and settings-row swipe-down interception logic repeatedly without finding a
defect, because the defect was not in that logic at all - it was one step earlier. Precise repro this round:
swipe-down works when the touch **starts on a suggestion chip or a settings-row button**, but not when it
starts on **empty bar background (no suggestions shown at all) or the row's own empty background (the gaps
between buttons)**. This pinpointed the real, well-known Android touch-dispatch gotcha: a plain,
non-clickable `View`/`ViewGroup`'s default `onTouchEvent()` returns `false` for an `ACTION_DOWN` no child
claimed either - and once a gesture's very first `ACTION_DOWN` goes unclaimed by *anything* in a view's own
subtree, later events in that same gesture are never delivered to that subtree's `onInterceptTouchEvent()`/
`onTouchEvent()` at all, regardless of how correct the interception logic itself is. A chip or a button is
independently clickable, so it *does* claim `ACTION_DOWN` - which is exactly why the gesture worked from
there. `SuggestionBarView`'s empty state has the identical mechanism, just via `HorizontalScrollView`'s own
built-in touch handling (which likewise only claims a touch that started on a child) rather than plain
`View`'s default - same root cause, same fix shape.

Fixed identically in both views: `onTouchEvent()` now explicitly claims `ACTION_DOWN` unconditionally
(`return true`) as its own fallback, reached only when no clickable descendant already claimed it first - a
touch that *does* land on a chip/button is completely unaffected, since dispatch already resolves to that
child long before this fallback is ever reached. This guarantees every gesture starting anywhere within
either view's own bounds is tracked from its very first event, so the existing (already-correct)
`onInterceptTouchEvent()` swipe-down detection now gets a genuine chance to run regardless of what, if
anything, happens to be under the finger at the start. No new unit tests - both changes are Android
touch-dispatch glue with no independently testable pure logic beyond the already-tested `SwipeGesture`, the
established gap for this layer.

689 unit tests (was 688; +1, `UrlLocaleTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
re-confirmed on device.

## §93 - D-144 Actually Fixed, D-145 Corrected Again: the Popup's Centre Position, Not List Order (v0.8.59)

Device feedback on §92: both of its fixes were still wrong, for two different, precisely-diagnosed reasons.

### D-144 Follow-Up - the Real Root Cause: `onInterceptTouchEvent()` Is Structurally Unreachable Once Nothing Claims `ACTION_DOWN`
§92's "claim `ACTION_DOWN` unconditionally in `onTouchEvent()`" fix did not resolve it - confirmed still
broken on an empty bar/row background. Re-traced against Android's own documented `ViewGroup.
dispatchTouchEvent()` contract rather than patching further blindly, and found the actual mechanism: for any
event **other than** `ACTION_DOWN`, the contract is (paraphrased from the real source):
```
intercepted = if (action == ACTION_DOWN || mFirstTouchTarget != null) onInterceptTouchEvent(ev) else true
```
`mFirstTouchTarget` is only non-null when some **child** claimed the gesture at `ACTION_DOWN`. When a touch
starts on empty background, no chip/button (child) claims it, so `mFirstTouchTarget` stays `null` for the
whole gesture - meaning `onInterceptTouchEvent()` is **never called again** for any later event in that same
gesture; every subsequent event goes straight to this view's own `onTouchEvent()`, bypassing
`onInterceptTouchEvent()` entirely, `intercepted` forced `true` unconditionally. §92's fix satisfied "claim
`ACTION_DOWN`" but the actual **swipe-direction detection itself** still only lived inside
`onInterceptTouchEvent()`'s `ACTION_MOVE` branch - a branch this exact scenario structurally can never reach,
however correct its own logic is. This is why the two-stage claim-early/confirm-late pattern worked
perfectly when a chip/button was under the finger (which *does* populate `mFirstTouchTarget`, keeping
`onInterceptTouchEvent()` in the loop) and never worked otherwise, regardless of which specific claiming
attempt was tried.

**Fixed properly**: both `SuggestionBarView.onTouchEvent()` and `SettingsRowView.onTouchEvent()` now
re-implement the *same* swipe-down detection directly in their own fallback branch (reached only when
nothing else - no child, no earlier interception - already claimed the gesture): `ACTION_DOWN` claims it,
`ACTION_MOVE` runs the identical `SwipeGesture.classify(..., interceptThresholdPx)` check and sets
`swipeDownIntercepting = true` on a match, after which the *existing*, already-correct `swipeDownIntercepting`
branch (checked earlier in the same function) picks up `ACTION_UP`/`ACTION_CANCEL` exactly as it already did
for the chip/button-claimed case - no duplicated confirm-threshold logic, just the missing detection step
restored to a path it can actually run on. No new unit tests - Android touch-dispatch glue over the
already-tested, already-pure `SwipeGesture`, the established gap for this layer.

### D-145 - Corrected Again: the Popup Centres Whichever Cell Is *Pre-Selected*, Not Whichever Is Listed First
§91 read "the locale-driven entry must be in the middle" as "must be index 0" and reordered
`periodAlternatives()` accordingly - device feedback confirmed this produced `.de, .com, .org` (ccTLD
leading) when `.com, .de, .org` (ccTLD in the visual *middle*) was actually wanted, matching this exact key's
own **original, pre-URL-mode** convention: the plain sentence-terminator popup (`! . ?`) already put `.`
- the key's own character - at the true middle of a 3-item list, flanked by `!`/`?`, not leading it. The
locale-driven TLD should occupy that *same* centred slot the key has always used, not the "leads the list"
slot some other keys (`https://`, `=`) use instead - these are two different, independently-valid
conventions already coexisting in this codebase, and the period key's own established one is the centred
kind.

**Fixed at both ends of the mechanism**: `UrlLocale.periodAlternatives()` now returns `[".com", ccTld, ".org"]`
(ccTLD in the middle) instead of leading with it; separately, `AdaptKeyboardView.preSelectedIndexFor()` -
whose existing generic rule ("pre-select the key's own character, or index 0 if not found in the list") would
otherwise pre-select `.com` for *any* locale, since this key's own `.` is deliberately never in the list at
all (D-144) - gained a small, precisely-scoped special case: for exactly this key (`KeyCode.CHAR`, `char ==
'.'`), when the generic self-match fails, pre-select whichever entry is neither `.com` nor `.org` (the
locale-specific one) instead of blindly falling back to index 0; only when *no* such entry exists (e.g.
`en_US`, no ccTLD at all) does it fall back to plain index 0 (`.com`). This composes correctly regardless of
where the ccTLD ends up sitting in the list - it doesn't hardcode "index 1", it *identifies* the ccTLD entry
structurally (the one that isn't a hardcoded generic literal), so the fix does not silently break if the list
shape changes again later. `UrlLocaleTest` updated to the corrected `[".com", ccTld, ".org"]` shape; no new
test for `preSelectedIndexFor` itself - private `AdaptKeyboardView` drawing/interaction logic, the established
testing gap for this class (no Robolectric shadow available in this environment).

689 unit tests (unchanged - existing tests updated, no new ones needed). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. Not yet re-confirmed on device.

## §94 - D-110, D-141, D-129 Confirmed Working on Device (No Code Change)

Device feedback: **D-110** (§86's `ShiftGrace.autoArmAtWordStart()` fix for `CapsMode.NONE`), **D-141** (§81's
`TimePattern` trailing-whitespace fix), and **D-129** (§83's `SIGN_FLIP_GLYPH` corner hint, third pass) all
confirmed working. No further action needed on any of the three; no code change this entry.

## §95 - D-149: a D-62 Mid-Word Reclaim's Backspace Could Wipe the Whole Word Instead of One Character; Nine New Backlog Items Captured (v0.8.60)

### D-149 - Bug fixed, traced from a precise device log: Backspace after a mid-word reclaim could delete the entire reclaimed word instead of one character, matching the reported D-139 "shaking" symptom

The user captured and supplied a full `AdaptKeyJitter` log for their own concrete repro of the reported
jitter/shake: type `diecwird` (an unrelated typing slip, out of scope here), place the caret right after the
`c`, press Backspace, then Space - the trailing text visibly "shook" instead of simply shifting, and the user
suspected this could also be the escalation mechanism behind the intermittent D-139 jitter itself. Traced end
to end against the actual log (not guessed): the tap collapses the caret at document offset 303 (`newSelStart
== newSelEnd`), which correctly triggers `reclaimWordAtCaret()` (D-62/§58) - `composing` becomes `"diecwird"`
with `composingAnchor = 299`, `composingCursor = 4` (the logical edit point right after the `c`), confirmed by
the following `onUpdateSelection` echo reporting the same collapsed `[303,303]`. The very next log line for
the Backspace itself, however, shows `composing` jumping straight to `""` and the cursor snapping to `299`
(the *anchor*, i.e. the whole reclaimed word's start) - with **no** `updateComposing:` line in between, which
only `deleteComposingChar()`'s normal per-character path would have logged.

That gap points squarely at `handleBackspace()`'s other branch: the §41 "real text selection" check, which
calls `InputConnection.getSelectedText(0)` completely ungated and, on a non-empty result, wipes the whole
selection via `commitText("", 1)` - previously correct for a genuine long-press-drag selection, but never
cross-checked against anything the IME itself already knows about the current selection state. On the device
tested (Google Keep, the app active for this whole log), `getSelectedText(0)` returned the full reclaimed word
non-empty at this exact point even though the immediately preceding `onUpdateSelection` - the IME's own
authoritative source for selection state - had just confirmed a collapsed caret; stock Android's
`BaseInputConnection.setComposingText()` never selects anything on its own, so this is attributed to a
Keep-specific interaction between its own (non-standard, rich-text) editor and the D-62 reclaim-then-collapse
sequence, not a generic platform behaviour - not independently provable further without a live device/logcat
session on that exact app, and stated as such rather than guessed at. The same `reclaimWordAtCaret()` path
worked correctly multiple times elsewhere in the very same log (e.g. the `engliscge`/`cWörter` sequence,
several Backspaces in a row via the normal single-character path) - the misfire is intermittent, which lines
up with D-139's own long-standing "occasional, not always reproducible" description and plausibly explains at
least part of it, though it is not claimed as the *whole* explanation.

**Fixed at the trust boundary, not by trying to out-guess Keep's internals**: a new `selectionCollapsed` flag
is set unconditionally at the top of `onUpdateSelection()` (`newSelStart == newSelEnd`) - the IME's own
confirmed reality - and `handleBackspace()` now only calls `getSelectedText(0)` at all when that flag says a
real selection is currently believed to exist; otherwise the ordinary `deleteComposingChar()`/`deleteOneBefore()`
paths run as before. A genuine long-press-drag selection is unaffected, since that is exactly how an IME
normally learns about a selection in the first place - it always reports its non-collapsed bounds via
`onUpdateSelection` before the user can press Backspace on it. No new unit tests - `InputConnection`/
`onUpdateSelection` service glue, the established gap for this class; the fix was verified by re-reading the
exact call sequence against the supplied log, not by guessing. D-139 is **not** declared closed by this entry -
it was reported as intermittent across multiple root causes over this project's history (§76's
`CallbackBurstGuard`, §87/§88's diagnostic logging), and this is one concrete, traced mechanism found and
fixed from a precise repro, not a claim that every remaining jitter report is now explained.

**Additional hint recorded, not yet investigated**: the user reports the jitter appears to correlate with
typing speed - occurring more often when typing faster. Consistent with (but not proof of) a timing/race
explanation for whatever remains of D-139 beyond D-149 - noted here for whenever the next repro/log arrives,
not acted on yet.

### Nine backlog items captured, not implemented this round

Reported together with the D-149 repro; captured per the user's own instruction to backlog them while this
round focused on D-149. None of the following has any code change yet:

- **D-150**: the D-139 in-app diagnostic log's 5-minute retention window (`DiagnosticRingBuffer`, §87/§88) is
  far more data than needed for a single repro - reduce to 1 minute. **Amended requirement, explicitly called
  out by the user**: logging must **never** happen while typing into a password field, regardless of whether
  diagnostics are enabled - the current `diag()` call sites (`onUpdateSelection`, `updateComposing`,
  `finalizeAndCommit`, etc.) have no field-type awareness at all today, so a password typed while diagnostics
  happen to be on would currently land in the (shareable/copyable) in-app log. Needs its own explicit guard,
  most likely keyed off the same `EditorInfo`/`InputType` password-variation check `LoginFieldDetector` (D-142)
  already has, checked independently of the `enabled` setting - not implemented yet. **Further amended,
  explicitly requested by the user**: the password exclusion must also be stated in the Settings UI itself
  (`cat_diagnostics`/`d_diag_enabled_summary` in `strings.xml`), not just hold true in code - so someone other
  than the user, who cannot read the source, can still enable the toggle with a clear conscience. The current
  `d_diag_enabled_summary` ("Records the text you type - never written to disk, never sent anywhere unless you
  explicitly share it below.") says nothing about password fields at all and must be revised alongside the
  actual guard, not left to imply a blanket "everything you type" once the guard exists.
- **D-151**: the `DiagnosticLogActivity` viewer's Share/Copy/Clear controls are unusable because they sit
  under the display notch.
- **D-152 (regression)**: the first word in a field is again deleted all at once instead of character-by-
  character - not yet re-traced against which recent change reintroduced this.
- **D-153 (regression)**: backspace-hold has become jerky/sluggish again, echoing D-138 - not yet re-traced.
- **D-154**: `uber` should autocorrect to `über` once the word is *finished* (a delimiter typed) - ambiguous
  while still composing, but the user's own reasoning is that at word-completion the umlaut-restoration
  candidate (`provider.diacriticRestoration`) is a single, extremely-high-confidence match while `uber` itself
  is not a word that should ever survive as typed - worth checking against `suppressAutocorrect`/
  `knownInOtherLanguage()` (D-106 stage 2) possibly protecting `uber` as a known English/German-loanword token.
- **D-155**: `fur` colours green (recognised as a correction target, i.e. `shouldHighlightComposing`/S-05 or
  the D-47 split-preview machinery) but is not actually autocorrected to `für` at commit - reported as an open
  question ("Warum?"), not yet traced; likely the same `suppressAutocorrect`/coloured-but-not-corrected gap as
  D-154, or a distinct issue in `autocorrectFor()`/`diacriticRestoration()` - needs its own trace before fixing.
- **D-156**: a live touch-zone visualisation toggle (the existing D-24 overlay) requested for the settings row,
  next to the D-70 clear-clipboard button.
- **D-157**: how to stop `due` (an all-too-common typo for `die`) from surviving as valid English in German
  mode. The user's own proposal: a per-dictionary blacklist of explicitly-rejected words, forced to
  autocorrect to the intended target, reversible via the existing A-07 undo with no relearning on reject.
  **User confirmed the recommendation below matches their intent exactly.** Recommendation for the eventual
  implementation, not yet built: rather than a new blacklist-plus-forced-
  replacement mechanism, add a small, explicit "confusable" override list consulted by the existing D-106
  stage-2 cross-language protection (`knownInOtherLanguage()`/`selectActiveDictionary()`) - e.g. `due` in a
  German context stops being treated as a protected known-English word, and the *already-existing* ordinary
  autocorrect pipeline (which already proposes `die` for `due` as a plain adjacent-key edit, §64's D-113
  precedent) does the rest, including A-07's already-correct no-relearn-on-undo behaviour, with no new commit
  path needed at all. A short, explicit, per-word list (like §68's per-word `NOUN,OTHER` tagging) rather than a
  general heuristic, since this is not a generalisable rule - most cross-language homographs (e.g. genuine
  loanwords) must keep their existing protection.
- **D-158**: a dedicated email-mode keyboard layout, mirroring D-143's URL-mode row: `@` takes over the comma
  key's primary slot (comma demoted to its own popup, mirroring how URL-mode treats `/`), a new key next to
  SPACE trades it some width for `-` (with `_` as its D-01 alternative), the period key looks and behaves
  exactly as in URL-mode, and SPACE carries only its plain glyph (no extra label) in this mode.

689 unit tests (unchanged - Android service glue, no new pure logic to cover). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. D-149 not yet re-confirmed on device.

## §96 - §95's Backlog Cleared: D-150 through D-157 (v0.8.61)

Requested as one round - D-150…D-157 from §95's backlog, all implemented and traced (not guessed) where a
root cause was needed. D-158 (email-mode keyboard) deliberately deferred to its own separate round per
explicit instruction.

### D-150 - diagnostic log window shortened to 1 minute; password fields never logged, in code and in the Settings text
`DiagnosticRingBuffer`'s `RETENTION_MS` dropped from 5 minutes to 1 (a single repro never needed more, and 5
minutes produced far more volume than expected). Separately and more importantly: `AdaptKeyService.diag()` -
the single choke point every `AdaptKeyJitter`/`AdaptKey` log call already funnelled through - now returns
immediately, before either `Log.d`/`Log.w` (logcat) or `DiagnosticLog.record()` (the in-app ring buffer), when
`loginFieldKind == LoginFieldKind.PASSWORD`. Reliable, since `LoginFieldKind.PASSWORD` is itself reliably
detected from `InputType` (§82) - one gate, not a per-call-site check every current and future `diag()` call
must remember individually, and unconditional (independent of `DiagnosticLog.enabled`, since a password must
never reach *either* destination, not just the shareable one). The exclusion is also spelled out directly in
`d_diag_enabled_summary` (`strings.xml`, all three locales - `values`/`values-de`/`values-el`) per the user's
own explicit follow-up request, so someone who did not read the source can still enable the toggle in good
conscience.

### D-151 - the log viewer's Share/Copy/Clear row was unreachable under the display notch
`DiagnosticLogActivity` was the one settings screen in this codebase that never applied the edge-to-edge
window-insets padding `CalibrationActivity` (§13/D-80) and `TouchModelActivity` already do - a fixed 16dp
`android:padding` on the root layout is not enough once Android 15 (`targetSdk 35`) draws the activity
edge-to-edge, so the button row pinned to the very top ended up under the status bar / camera cutout. Fixed
with the exact same `ViewCompat.setOnApplyWindowInsetsListener` pattern as its two siblings: real
`statusBars`/`displayCutout`/`navigationBars`/`systemGestures` insets, each floored at the original 16dp so
the layout never gets *tighter* than before on a device with small/no insets.

### D-152 - regression: the first word in a fresh field could be deleted all at once on a single Backspace
Traced to a genuine gap in §95's own D-149 fix, not a separate mechanism: the new `selectionCollapsed` flag is
only ever updated from `onUpdateSelection`, but a freshly focused field's *own* initial selection is delivered
via `EditorInfo`, not a guaranteed `onUpdateSelection` callback beforehand - so `selectionCollapsed` could stay
stale from whatever the *previous* field's last reported selection happened to be. Left stale as `false` (e.g.
the previous field ended on a genuine drag-selection), `handleBackspace()`'s D-149 guard would wrongly still
trust `getSelectedText()` for this field's very first Backspace - most visibly hit right after the field's
first word gets D-62-reclaimed on focus (exactly the same class of bug D-149 fixed, just reachable through a
staleness window D-149 itself left open). Fixed by resetting `selectionCollapsed = true` in `onStartInput()`,
alongside every other per-field state reset already there - a fresh field is assumed collapsed until its own
`onUpdateSelection` says otherwise, matching every other real editor's default caret state.

### D-153 - regression: backspace-hold became jerky again
Root-caused to two uncached per-keystroke dictionary lookups that D-138 (§64) never actually covered, both of
which grew measurably heavier since D-138 was written:
1. `updateComposing()`'s own `splitPreview()`/`shouldHighlightComposing()` colour-preview calls (`isKnownWord`/
   `trySplit`, each a real, uncached SQLite query against `SqliteDictionaryStore.entryOf()`) ran on *every*
   call, including every `handleBackspaceRepeat()` tick - D-138 only ever gated `refreshSuggestions()`'s own
   additions, never `updateComposing()`'s.
2. `refreshSuggestions()`'s `provider.suggestionsFor()` call (prefix scan + fuzzy-neighbour search, and
   conditionally a compound-split attempt) ran unconditionally too, and gained real additional cost after
   D-138 shipped - D-116's compound split and D-147's `Umlaut.unfoldCandidates` prefix search were both added
   later without ever being folded into the existing `duringRepeat` gate, despite `suggestionsFor()`'s own KDoc
   already naming D-138 as the standing lesson to apply.

Both `updateComposing()` and the `candidates` computation inside `refreshSuggestions()` now take the existing
`duringRepeat` flag (already threaded through `deleteComposingChar()`) and skip these specific calls during a
repeat tick - the bar/highlight they drive is changing every 45-330ms mid-repeat regardless (unreadable),
exactly the same reasoning D-138 already established for its own gated additions.

### D-154 / D-155 - "uber"/"fur" were never autocorrected to "über"/"für"
Confirmed against the bundled dictionaries directly (`grep`, not guessed): `fur` is a real English word
(`dict_en.tsv`, frequency 366); `uber` is in neither `dict_en.tsv` nor `dict_de.tsv` at all; `über`/`für` are
common German words. Two independent mechanisms were both silently blocking the umlaut restoration
`DictionarySuggestionProvider.diacriticRestoration()` already correctly computes (confirmed via a new test -
it already returns `"über"`/`"für"` given those two typed tokens, this was never a bug in the restoration
logic itself):
- **`fur`**: D-106 stage 2's `knownInOtherLanguage("fur")` returns true (the English dictionary knows it), so
  `finalizeAndCommit()`'s `suppressAutocorrect` flag was true, which gated `diacriticWord`'s computation off
  entirely.
- **`uber`**: not protected by `knownInOtherLanguage` (unknown everywhere), but plausibly caught by
  `resolveDict()`'s A-03 foreign-language classifier - a bare ASCII token missing its umlaut is exactly the
  n-gram shape that reads as non-German, setting `dictChoice.suppressAutocorrect = true` the same way. Not
  independently re-run against the live classifier in this environment (no device); named as the traced,
  plausible mechanism, not an unconditionally proven one - consistent with how this project already treats
  A-03 classifier edge cases (§43).

**Fixed by giving diacritic restoration its own, narrower gate**, matching the user's own reasoning: an ASCII
form only reachable by *dropping* a German umlaut is a stronger, more specific signal than either "the general
context looks foreign" or "this coincidentally spells a real word in some other bundled language" - this
project's own founding "umlauts are ordinary characters" principle says the restored form should win.
`diacriticWord` is now computed directly against the German provider (`providers.getValue(Language.GERMAN)`),
independent of `suppressAutocorrect`/`knownInOtherLanguage`, but only while `activeLanguage == Language.GERMAN`
(a user who explicitly G-01-switched to English/Greek does not want German umlaut restoration forced onto
their own explicit choice) - and, new this round, it now actually **wins as the correction itself** (`corrected
= diacriticWord ?: autocorrected ?: rawCorrected ?: typed`), not merely used to veto an A-05 split as before.
One new test (`DictionarySuggestionProviderTest`) locks in `diacriticRestoration("uber")` → `"über"` and
`diacriticRestoration("fur")` → `"für"` directly against the underlying provider. Flagged honestly as a
behaviour change worth confirming doesn't misfire on-device, same caveat class as D-111/D-112.

### D-156 - a live touch-zone-visualisation toggle in the settings row
`AdaptKeyboardView.showTouchModel` (D-24) previously only ever ran on a separate, non-live preview keyboard
inside `TouchModelActivity`/`CalibrationActivity` - never on the actual keyboard while typing in a real app.
New `SettingsRowView` button (🎯, placed left of the D-70 clear-clipboard button, same background-reflects-
state styling as the D-142 credential button) wired to a new `AdaptKeyService.toggleTouchZoneVisualizationFromSettingsRow()`,
which flips `keyboardView.showTouchModel` directly and closes the row (so the overlay is actually visible on
the keyboard underneath, not hidden behind the row itself). Session-only, like `credentialModeManuallyActivated`
- resets on the next keyboard presentation, no new persisted setting. No new tests - Android view/service glue,
the established gap for this layer.

### D-157 - "due" no longer protected from autocorrecting to "die" in German mode
Root-caused precisely, not guessed: `dict_de.tsv` already contains `due` (frequency 24, a rare loanword entry)
*and* `dict_en.tsv` contains it too (frequency 11775) - §44/D-113 (v0.8.19) already taught the German
dictionary's own `bestCorrection()`/`shouldOverrideKnownWord()` exactly how to override a rare known word for a
dramatically more frequent, single-adjacent-edit candidate (`autocorrectFor("due", null)` already returns
`"die"` directly against `DictionarySuggestionProvider`, confirmed by the pre-existing test from §44 itself) -
but D-106 stage 2's `knownInOtherLanguage("due")` (English also knows it) set `suppressAutocorrect = true` in
`finalizeAndCommit()`, which skipped the call to `autocorrectFor()` entirely before that already-correct logic
ever got a chance to run. Implemented exactly per the user's confirmed recommendation from §95: not a new
blacklist-plus-forced-replacement mechanism, but a short, explicit `CROSS_LANGUAGE_CONFUSABLES` exception set
(currently just `{"due"}`) consulted by `knownInOtherLanguage()` itself - letting the *already-existing*,
already-tested correction pipeline (including A-07's already-correct no-relearn-on-undo behaviour) do the rest,
with no new commit path needed at all. Deliberately a short, reviewed, per-word list (§68's per-word
`NOUN,OTHER` dictionary-tagging precedent) rather than a general heuristic, since most genuine cross-language
homographs (real loanwords) must keep their existing protection - only `knownInOtherLanguage()`'s "never
autocorrect it away" shield is affected; cross-language suggestions and the word's own in-language A-01
protection are untouched.

690 unit tests (was 689; +1 `DictionarySuggestionProviderTest`). `:app:assembleDebug`/`:app:testDebugUnitTest`
green. None of this round's Android/service-glue items (D-150-D-153, D-156) have been confirmed on a real
device yet; D-154/D-155/D-157 rely on dictionary-level logic that is directly unit-tested, but the end-to-end
on-device behaviour is likewise not yet re-confirmed.

## §97 - D-158: Dedicated Email-Mode Keyboard Layout (v0.8.62)

Implemented as its own round, deliberately deferred from §95/§96 per explicit instruction. Mirrors D-143's
URL-mode row precisely, per the user's own spec: `@` takes over the comma key's primary slot, a new dash key
funds itself from the space key's width, the period key behaves exactly like URL-mode's, and the space key
carries only its plain glyph.

**`KeyboardLayout.emailBottomRow(proportions, locale)`** (new, alongside the existing `urlBottomRow`, shared
by `KeyboardLayout` and `GreekLayout` exactly like it): `[?123] [@] [-] [space, narrow] [.] [⏎]`.
- **`@` key**: takes over the comma key's own primary position (`charKey('@', alternatives =
  EMAIL_AT_ALTERNATIVES, weight = proportions.commaWeight)`); `EMAIL_AT_ALTERNATIVES` is literally
  `COMMA_ALTERNATIVES` unchanged (comma itself is already in that list, so it is automatically demoted to a
  popup entry with no separate constant needed - the same trick `URL_SLASH_ALTERNATIVES` already uses for
  `/`).
- **New dash key**: `charKey('-', hint = "_", weight = EMAIL_DASH_KEY_WEIGHT)` - a single D-01 long-press
  secondary (`_`), the same AltGr-style single-hint pattern as e.g. `m` → `-` in `DEFAULT_LETTER_HINTS`, not
  a multi-cell popup. Funded from the space key's own width rather than growing the row overall (D-143's own
  approach for its protocol/www keys): `EMAIL_DASH_KEY_WEIGHT = 1.4f` + `EMAIL_SPACE_WEIGHT = 1.8f` sum to
  exactly `3.2` - the *ordinary* row's own `spaceWeight` - so the email row's total width matches the
  ordinary bottom row's exactly, just redistributed between the two keys, rather than URL mode's
  row-grows-overall approach.
- **Period key**: `charKey('.', alternatives = UrlLocale.periodAlternatives(locale), weight =
  proportions.periodWeight)` - byte-for-byte the same call `urlBottomRow` makes, since an email address ends
  in a domain/TLD exactly like a URL does.
- **`-`/`_` need no new delimiter handling**: both already commit verbatim with no autocorrect/learning for
  any `loginFieldKind != NONE` field (D-142's `commitVerbatimFieldFragment`, whose own KDoc already names
  `.`/`@`/`-`/`_` explicitly as "each its own delimiter under the ordinary token model") - an EMAIL field
  already satisfies that condition via the pre-existing, unrelated `loginFieldKind == EMAIL` classification,
  so the new dash key needed no companion service-layer change at all.

**Detection**: a new `AdaptKeyService.isEmailField(info)`, mirroring `isUrlField` exactly (checks
`InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS` directly against the
raw `EditorInfo`) rather than derived from `loginFieldKind` - purely because `loginFieldKind` is not computed
until after `setSurface()` runs in `onStartInput()`, and (like `urlMode`) this must be pushed to the keyboard
view *before* that call so its own `rebuildRows()` already reads the correct value; reordering the existing,
already-commented `loginFieldKind` computation was avoided as unnecessary risk for this. New service-level
`emailMode` field (mirrors `urlMode`'s own field exactly) drives only the keyboard-view layout
(`AdaptKeyboardView.emailMode`, new) and the G-01 space-swipe suppression (`KeyGesture.resolve()` gained an
`emailMode` parameter, identical reasoning to its existing `urlMode` one) - autocorrect/suggestion suppression
needed **no** new code at all, since a reliably-detected EMAIL field already fully short-circuits through the
pre-existing, unrelated D-142 credential pipeline (`loginFieldKind != LoginFieldKind.NONE`), which also means
an email field already shows the D-142 domain-completion suggestion bar for free.

**Space glyph**: `AdaptKeyboardView.labelFor()`'s existing `urlMode`-gated "space bar shows the plain glyph,
not the language label" branch (D-143) now also checks `emailMode`, exactly mirroring `urlMode`'s own two
conditions - no new logic, just the same rule extended to the new flag.

8 new unit tests: `KeyboardLayoutTest` (6, mirroring the existing D-143 URL-mode cases one-for-one, plus one
confirming `urlMode` wins if both flags were somehow set at once), `GreekLayoutTest` (1, confirms the Greek
layout's email row is identical to the Latin layout's, mirroring the existing urlMode parity test),
`KeyGestureTest` (1, confirms a horizontal swipe on the email-mode space key surface-switches rather than
language-switches). 698 unit tests (was 690; +8). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-tested - the new key weights (`EMAIL_DASH_KEY_WEIGHT`/`EMAIL_SPACE_WEIGHT`) are a considered starting
guess, easy to retune (§36/§37/§53/§89 precedent).

## §98 - D-159: Robust (Huber-Style) Downweighting for the Touch-Zone Learning Model (v0.8.63)

Requested directly by the user, out of concern that a single wildly mis-tapped-but-still-resolved touch
could disproportionately drag a key's learned zone off course, degrading precision over time. Design
discussed and agreed before implementation (per this project's own established convention for
non-trivial behaviour changes):

- **The idea**: weight each recorded tap's contribution to `OffsetModel`'s running mean/variance by how
  far it falls from the key's *currently learned* expected strike point, relative to the currently
  learned spread - a tap close to what the model already expects contributes close to full weight, a
  wildly-off one contributes much less (never zero - floored at `MIN_SAMPLE_WEIGHT = 0.1`, so a
  *persistent* run of similarly-off taps still pulls the mean there over time, just more gradually than
  an unweighted model would). This is standard robust (Huber-style) M-estimation, not an ad hoc invention.
- **User's own follow-up correction, confirmed correct on inspection**: an earlier proposal to gate the
  weighting behind `MIN_VARIANCE_SAMPLES` ("not enough real data yet to judge distance against") was
  dropped after checking `PatternSeed.seed()` directly - a completed K-01 calibration already seeds every
  key with `count = SEED_COUNT = 25` (comfortably above `MIN_VARIANCE_SAMPLES = 5`) *and* a synthetic
  variance derived from the chosen typing pattern, so a dedicated gate would almost never actually bind
  for a calibrated key. The weighting instead simply reuses `variance()`'s own pre-existing fallback
  (seeded/learned spread once available, otherwise a geometric estimate from the key's own size) with no
  new gate at all - simpler and more correct than the original proposal.

### Implementation
`OffsetModel.Stat` gained `weightSum: Double = count.toDouble()` (the Kotlin default expression
deliberately references `count`, so any `Stat` built without thinking about weighting at all - older
persisted data, hand-built seeds/tests - is automatically treated as "every sample had full weight",
which is exactly correct for that data). `record()` now computes a weight via new `weightFor()` (the same
Gaussian log-likelihood shape `logLikelihood()` already scores candidates with, as `exp(-0.5 * r²)`) before
folding the sample in via a weighted Welford update (West's algorithm), and **returns the weight applied**
- callers that may need to `unrecord()` this exact sample later must retain it, since it cannot be
re-derived afterwards once further taps have moved the key's mean/variance on. `record()` also gained
optional `halfWidth`/`halfHeight` parameters (defaulting to `50f`, matching this test suite's own existing
convention) feeding only the geometric variance fallback for a not-yet-trained key; every real
`AdaptKeyboardView` call site passes the key's actual `rect.width()/height() / 2f`.

`unrecord()` (D-140) now takes the weight as a required parameter and performs the exact algebraic inverse
of the *weighted* update (not a heuristic) - `spreadFor()`'s standard-deviation formula and `merge()`'s
parallel-combination formula (`combine()`) were both updated to use `weightSum` (the true effective sample
mass the weighted M2 sums were accumulated against) rather than the plain `count` as their divisor/merge
weight; `variance()` still gates *whether* to trust any computed variance on `count` (a question of "have
we observed enough real taps", independent of how much effective weight they carried).

**Threading the weight from tap to potential undo** required a real, traced-through plumbing change, not
just the `OffsetModel` API: `TapPoint` gained a `weight: Double = 1.0` field; `AdaptKeyboardView`'s
`OnKeyListener.onKey()` callback gained a `weight` parameter (captured at `ACTION_DOWN` right after the
`record()` call, carried to release exactly like the existing `pendingAmbiguity`); `AdaptKeyService.handleKey()`
gained a matching parameter, threaded into both `TapPoint(x, y, weight)` construction sites; the existing
`RawCorrectionUndo` (D-140's own capture struct for a D-39 raw-coordinate-correction undo) gained a `weight`
field, populated from the composing tap's own `.weight` and passed unchanged to `unrecord()`.

**Persistence**: `OffsetStore` persists/loads `weightSum` as a new trailing JSON array index (7), following
the exact precedent the T-04 contact-area fields (`sizeCount`/`meanSize`) already set for a length-gated
backward-compatible field addition - an older blob without it defaults `weightSum = count.toDouble()` on
load (every historically-recorded sample genuinely *was* unweighted, so this is the correct migration, not
just a convenient placeholder). `PatternSeed.seed()` needed **no code change** at all - it already
constructs `Stat(count = SEED_COUNT, ...)` with named arguments, so the new `weightSum` default expression
picks up the right value automatically.

### An honest, load-bearing caveat: `merge()`'s old "sequential equals split-then-merged" equivalence is gone
Under the previous plain (unweighted) Welford update, recording a stream of taps into one model was
mathematically identical to splitting them across two models and merging the halves (`OffsetModelTest` had
a dedicated test proving exactly this, `merge of split halves matches a single combined model`). This is
no longer true in general once per-sample weighting depends on each side's own independent *running*
state: a sample recorded into the "second half" model never saw the "first half"'s taps, so it cannot be
downweighted against them the way it would have been inside one continuous stream - and this breaks even
for a stream of *identical* deviations, since each branch's own very first sample is separately bootstrapped
against a fresh zero mean (traced by hand while writing the replacement test, not assumed). This is an
inherent, expected consequence of introducing robust per-sample downweighting - a well-known property of
recursive robust estimators generally, not a bug introduced here. `combine()` still performs a correct
*weighted* parallel combination of two already-computed `Stat` objects (each weighed by its own
`weightSum`) - verified directly by a new dedicated test - it just no longer claims that doing so
reproduces what continuous single-stream recording would have produced. In practice `merge()` is only ever
called to fold a `PatternSeed`-built calibration model into the persisted one (never a literal split of the
same real tap stream), so this was already more of an incidental nice-to-have property of the old
implementation than something anything actually relied on.

### Tests
`OffsetModelTest.kt` reworked throughout: the old `record accumulates mean deviation per key` and
`spreadFor reports the mean offset and per-axis standard deviation` tests asserted exact hand-computed
arithmetic-mean/variance values that the new weighting genuinely changes, so both were replaced rather than
patched - by property-based tests that directly demonstrate the *intended* behaviour instead (a single
sample always sets the mean exactly, since nothing exists yet to weigh it against; a wildly-off *second*
sample moves the mean less than a naive average would; a *sustained* run of 200 similarly-off samples still
pulls the mean close to it, confirming persistent drift is still learnable; `spreadFor`'s formula is
cross-checked against the model's own exposed `weightSum`/`m2` state rather than a hardcoded literal). Every
`unrecord()` call site across the file now captures and replays the exact weight `record()` returned. The
invalidated `merge of split halves` test was removed (see the caveat above) and replaced with a test of
`combine()`'s actual weighted-merge formula. 4 net new tests (some removed, more added):
`OffsetModelTest` +4. 702 unit tests (was 698; +4). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
Not yet device-tested - `MIN_SAMPLE_WEIGHT`/`DEFAULT_SIGMA_FACTOR` are considered starting points, easy to
retune later (established precedent for every such constant in this touch-model area).

## §99 - D-139 Root Cause Found (Design Decision Pending); D-158 Follow-Up: Email-Only `.net`; Diagnostic-Log Description Cleanup (v0.8.64)

### D-139 - root cause identified from three real device logs, not yet fixed
The user captured three genuine device logs reproducing the reported "text jitters, characters get
scrambled" glitch, then supplied all three for tracing against the real code (`AdaptKeyService.kt`), not
guessed. All three show the identical mechanism, always triggered right after a word commits and the next
character is typed. Root cause: `onUpdateSelection()`'s `ownEdit` check -

```kotlin
val ownCursor = if (composingAnchor >= 0) composingAnchor + composingCursor else candidatesEnd
val ownEdit = newSelStart == newSelEnd && candidatesEnd >= 0 && newSelStart == ownCursor
```

- only tracks the expected cursor position purely locally (immune to any outside timing) for the D-62
mid-word-edit case (`composingAnchor >= 0`). For the ordinary, by far most common case - typing at a
token's own end, `composingAnchor == -1` by construction - `ownCursor` falls back to `candidatesEnd`, a
value the *target editor* reports back per callback, not anything AdaptKey tracks itself. Android does not
guarantee this is synchronised 1:1 with the selection-update stream; a specific `onUpdateSelection` callback
immediately following the service's own `setComposingText()` call can report `candidatesEnd = -1` (the
editor's own composing-span bookkeeping momentarily lagging, or a stale/delayed echo of a prior edit
arriving out of order) even though `composing` genuinely holds live, correctly-typed text. The
`candidatesEnd >= 0` guard then misclassifies this entirely legitimate self-edit as external,
`finishComposingText()` + `clearComposing()` wipes the just-typed token, and because `composing` is now
empty the very next callback falls into the `composing.isEmpty()` branch and calls `reclaimWordAtCaret()`,
which re-derives the same text via *another* `setComposingText()` call - subject to the identical race, so
it fails identically every retry. The result is an unbounded self-perpetuating loop (finish → clear →
reclaim → re-compose → misjudged again), observed running at roughly 5-10 iterations/second for 7-9+
seconds straight in the captured logs, only ending on a focus change. `CallbackBurstGuard` (§76) never trips
- its threshold is >40 calls within 200ms, and this loop's rate is throttled by the real IPC round-trip of
each cycle to well under that. Reproduced in at least two different target apps (Google Keep, Signal) -
not app-specific. This is a different, deeper mechanism than either of §76's circuit breaker (a generic
defensive measure, not a diagnosis) or §95/D-149's fix (a real but unrelated bug in the same neighbourhood -
`selectionCollapsed` not being reset).

**Fix direction proposed, not implemented this round**: extend the same purely-local cursor-tracking the
mid-word path already uses (immune to this race by construction) to the ordinary end-of-word case too,
instead of ever trusting the remotely-reported `candidatesEnd` for `ownEdit` recognition. This is a
foundational change to the self-recognition mechanism the entire composing pipeline depends on, with real
trade-offs to weigh (e.g. how to track the composing start position locally for the common case, which
currently has no equivalent to `composingAnchor`) - per this project's own convention for non-trivial design
changes, deliberately left for a follow-up round pending the user's own decision on direction, rather than
implemented directly. No code changed for D-139 itself this round.

### D-158 follow-up - email-mode TLD popup gains `.net`, email-only
Requested directly: the email keyboard's period-key long-press popup needed a `.net` entry, placed directly
before `.org`. Confirmed with the user this is **email-only**, not shared with URL mode's own period popup
(which had reused `UrlLocale.periodAlternatives()` byte-for-byte since D-158/§97). New
`UrlLocale.emailPeriodAlternatives(locale)` returns `periodAlternatives()`'s own list with `.net` inserted
directly before `.org` (`[.com, ccTLD, .net, .org]`, or `[.com, .net, .org]` for a ccTLD-less locale);
`KeyboardLayout.emailBottomRow()` now calls it instead of `periodAlternatives()`, while `urlBottomRow()` is
untouched. `AdaptKeyboardView.preSelectedIndexFor()`'s ccTLD lookup (pre-selects whichever popup entry is
neither `.com` nor `.org`, so D-01's popup centres the locale's own most-relevant TLD directly over the stem
- see D-145) now also excludes `.net`, or a ccTLD-less locale's email keyboard would have wrongly
pre-selected `.net` instead of falling back to `.com`; because the ccTLD (when present) always precedes
`.net` in the new list, this only changes behaviour for the ccTLD-less fallback case, not the general one.
5 new/changed tests (`UrlLocaleTest` +2, `KeyboardLayoutTest`: 1 replaced + 1 added).

### Diagnostic-log description cleanup - no code change to behaviour
Two corrections to `d_diag_enabled_summary` (all three locales), requested directly: it must not reference
the reported jitter (D-139) by name - naming a specific, still-unresolved bug in a Settings description was
premature and potentially misleading once other rare issues might also show up in the same log - reworded
to the generic "rare glitches"; and the password-field exclusion, previously buried as the last clause of
one long paragraph, is now its own short, clearly separate sentence so it is not easy to miss. No behaviour
changed, description text only.

3 net new tests (`UrlLocaleTest` +2, `KeyboardLayoutTest` +1). 705 unit tests (was 702; +3).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. The `.net` addition and description rewrite are not yet
device-confirmed; D-139 remains open, now with a confirmed root cause and a proposed fix direction awaiting
the user's decision.

## §100 - D-139 Actually Fixed: `composingAnchor` Now Resolved Locally for Every Composing Token, Not Only Mid-Word (v0.8.65)

Follow-up to §99's diagnosis, same round of device logs. The user's own instinct, raised directly, was
right in substance but not in scope: they suspected the D-62 mid-word-correction machinery ("reclaimed"
composing) was implicated, and asked whether tuning it away would make the bug disappear. Tracing confirmed
the mid-word mechanism is real to the story, but not as the trigger - it is the **amplifier**.
`composingAnchor` is `-1` in every single affected line of all three logs, i.e. the misdetection always
hits during ordinary end-of-word typing, never a genuine mid-word edit (matching the user's own recollection
exactly). What §58's `reclaimWordAtCaret()` contributes is turning one transient misdetection into an
*unbounded* loop: once a spurious "not ownEdit" wipes a token, composing is empty with a collapsed caret -
precisely the condition `reclaimWordAtCaret()` reacts to - so it immediately re-derives the same text via
another `setComposingText()` call, subject to the identical race, forming the observed multi-second freeze.
Restricting §58 alone (the tuning the user asked about) would very likely have stopped that runaway
escalation, but would have left the underlying misdetection itself free to still fire occasionally during
completely ordinary typing - without the reclaim to rebuild it, that would silently early-commit the
in-progress word via the raw `finishComposingText()` path, **bypassing `finalizeAndCommit()` entirely**
(no autocorrect, no capitalisation, no dictionary learning for that one word) - a smaller but real
manifestation of the same defect, and exactly the kind of "half-fixed, chase phantoms for days later"
outcome the user explicitly asked to avoid. Fixed at the actual root instead, per the user's explicit
request for a thorough, one-time fix over a symptom patch.

**The fix**: `composingAnchor` - previously "the absolute document offset of the composing region's start,
tracked only while a D-62 mid-word reclaim's untouched 'after' fragment is in play, `-1` otherwise" - is now
resolved by `reclaimSurroundingWord()` unconditionally, for every case a composing token can start from: a
brand-new word with nothing adjacent, a reclaim of a preceding word with nothing following the caret, or a
genuine mid-word before+after reclaim. All three of `AdaptKeyService`'s composing-starting call sites
(`handleKey()`'s `CHAR` branch, `reclaimWordAtCaret()`, `appendLongPressLetter()`) already funnel through
`reclaimSurroundingWord()` before any character lands in a fresh composing buffer, so this one change covers
all of them uniformly - confirmed by walking every read/write site of `composingAnchor` in the file, per
this project's completeness convention, not just the two call sites the bug was found in. The anchor is
computed via the existing, already-tested `ComposingAnchor.resolve()` (D-87) - previously scoped to the
mid-word case only, now recognised as the general-purpose "where does composing start" primitive it always
was - read once via `InputConnection.getExtractedText()` **before** any mutation in the same call, matching
this class's own established read-before-mutating convention (the D-122 KDoc on
`splitComposingAtCaretAndCommit()` states the same principle explicitly). `onUpdateSelection()`'s `ownEdit`
check no longer references `candidatesEnd` at all - `candidatesEnd`/`candidatesStart` are now unused outside
the diagnostic log line, since the whole reason they were consulted (the *only* case `composingAnchor` was
`-1` while composing was non-empty) no longer exists. When `composingAnchor` is still unresolved (`-1`) -
now only possible if `getExtractedText()` itself fails, a rare editor-quirk case that previously aborted the
whole reclaim outright - `onUpdateSelection()` deliberately does nothing (neither treats the update as own
nor as foreign) rather than guess, since guessing wrong and wiping is the destructive branch; this is a
strictly more conservative failure mode than before, not merely a different one.

**Side effect confirmed, not incidental**: `splitComposingAtCaretAndCommit()` (D-119/D-120/D-122, the
mid-word-SPACE-split path) already computed its re-seeded "after" half's anchor arithmetically from the
"before" half's own `composingAnchor` (`beforeAnchor + committedLength`), but only produced a real anchor
when `beforeAnchor >= 0` - which, before this fix, was almost never true for an ordinary split (the "before"
half's own anchor was itself usually `-1`). That call site needed no code change at all: since
`composingAnchor` is now resolved for every token, `beforeAnchor` is now valid in the same cases, and the
existing arithmetic propagates correctly - a second, previously-silent instance of the same
`candidatesEnd`-dependent gap, fixed for free by fixing the root cause rather than each call site
individually.

**Trade-off accepted, stated plainly**: `reclaimSurroundingWord()` now calls `getExtractedText()` on every
new word (previously only when a genuine mid-word "after" fragment existed) - one extra lightweight IPC
round-trip per word, not per keystroke, in exchange for eliminating a reproducible multi-second input freeze.

1 new test (`ComposingAnchorTest`: the `reclaimedBeforeLength = 0` boundary, now a load-bearing case rather
than an untested corner). 706 unit tests (was 705; +1). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
**Not yet device-confirmed** - this class of bug has had multiple "fixed but not actually fixed" rounds
before (§76, §95/D-149), so D-139 stays open until the user reproduces the original repro steps again on a
real device and it holds up; this round is a traced, code-verified root-cause fix, not a guess, but device
confirmation is still the bar per this project's own convention.

## §101 - D-139, Second Half: Truth Verification Replaces Positional Equality in `onUpdateSelection` (v0.8.66)

§100 device-tested negative: the jitter still reproduced. Two fresh device logs supplied and traced from
scratch per this project's own rule - the diagnosis re-questioned, not the same approach patched. Both logs
confirm §100's mechanism itself works as built (`composingAnchor` resolves correctly for every token:
`anchor=0/4/9/…` throughout, where the pre-§100 logs showed `-1` everywhere), but expose that the *check
consuming it* was still timing-naive in a way §100's diagnosis had not covered. Two distinct trigger shapes,
one shared root:

- **Log 1 (ordinary fast typing, Gemini search field)**: after committing `habe ` the user types `g` before
  the commit's callbacks arrive; composing is locally `"g"` with `anchor=9`, expected caret 10. The commit's
  *lagging echoes* (`old=[8,8] new=[8,8]`, then `new=[9,9]` - descriptions of already-superseded intermediate
  states) then fail the `newSelStart == expected` equality and are misjudged as external caret moves.
- **Log 2 (genuine D-62 mid-word tap, same field)**: tapping into `angebucht` after the `a` reclaims
  correctly (`anchor=126`, `cursor=1`, expected 127), but the reclaim's batch
  (`deleteSurroundingText` -> `setComposingText` -> `setSelection`) is **not coalesced by this editor** -
  the delete step's transient state (caret 126) is echoed as its own update, `old=[127] new=[126]`, which
  fails the equality check too. Notably this echo lies *outside* the interval [old, expected] = [127,127] -
  so the initially-proposed AOSP-style "belated update" interval heuristic, considered after log 1 alone,
  would **not** have covered it. The user's instinct to wait for the second log before committing to a
  design prevented shipping a second insufficient fix.

Either misjudgement wipes the live token; §58's `reclaimWordAtCaret()` then rebuilds it through the same
race, forming the same self-perpetuating loop as before (~6 Hz for 5+ seconds in log 2, ended only by the
focus change). The shared root: **any classification computed from callback positions alone is guessing
about queue-lag and per-editor batch-coalescing behaviour** - callback streams are asynchronous descriptions
of the past, in whatever granularity the editor chooses.

**The fix - let reality decide instead of classifying echoes**: synchronous `InputConnection` reads, unlike
callbacks, are answered by the editor only after every previously-sent mutation has been applied, so a read
always sees the true current state regardless of how far the callback queue lags. `onUpdateSelection` now
verifies a mismatching callback against ground truth before reacting: read the actual selection via
`getExtractedText()` (absolute via the existing `ComposingAnchor.resolve()`, offset math shared, not
duplicated); if reality sits collapsed exactly at the expected caret, the callback was a stale echo of the
IME's own edit - ignore it, the newer local state is already correct; if reality is elsewhere, it is a
genuinely external change (user tap, app-side edit) and the composing state resets exactly as before. The
shared predicate is the new pure `SelectionTruth.isAtExpectedCaret()` (applied first to the callback's own
reported positions - the read-free, overwhelmingly common in-sync case - then to the ground truth). Both
log-1 and log-2 trigger shapes, and every future editor-specific echo shape, collapse into the same
question with the same reliable answer.

**Trade-offs, accepted deliberately**: one extra `getExtractedText()` IPC round-trip *only* for mismatching
callbacks (the in-sync common case stays read-free; mismatches occur a few times per word boundary under
lag, never per keystroke). Editors where `getExtractedText()` returns null fall into the same conservative
do-nothing branch as §100's unknown-anchor case - after a genuine external caret move there, composing
could stay stale for one keystroke; rare, and strictly less destructive than wiping a live token wrongly.
A genuinely external move landing *exactly* at the expected caret is indistinguishable from an echo and
gets ignored - harmless by construction, since state and reality agree. `CallbackBurstGuard` (§76) stays
untouched as a safety net. The `AdaptKeyJitter` diagnostics stay in until D-139 is finally closed on
device; the wipe branch now logs `EXTERNAL` with both expected and actual positions, and stale echoes log
their own distinct line, so a further negative round would be immediately attributable.

7 new tests (`SelectionTruthTest` - both observed mismatch shapes are encoded as named cases). 713 unit
tests (was 706; +7). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Not yet device-confirmed** -
same bar as §100: D-139 stays open until the user's own repro attempts hold up on a real device.

## §102 - Backlog Capture: D-160 (Typing Lag on Long Unknown Compounds - Per-Keystroke Pipeline Saturation, Traced Hypothesis)

Reported: strong lag while typing `gesamtparteilichen` (an 18-char compound, guaranteed absent from the
dictionary), captured in a device log still running v0.8.65. Not implemented this round - captured with a
traced hypothesis and a proposed fix direction, pending the user's call.

### What the log shows

**No D-139 mechanism anywhere in this log**: zero wipe lines, the composing stream is position-perfect
through the entire word (`anchor=124`, cursors 1-18 all consistent). This is a *different* problem:
performance, not state corruption.

The evidence chain for where the lag sits:

1. **Callback starvation followed by a burst.** Every keystroke up to cursor 12 gets its
   `onUpdateSelection` echo within ~100-200ms. From cursor 13 (`gesamtparteil`) through 18 - roughly 2.5
   seconds of continuous typing - **no callbacks arrive at all**; then, within ~90ms right after the SPACE
   commit, all six queued echoes (`new=137`…`142`) plus the commit's own pair arrive at once. Selection
   callbacks are ordinary Handler messages on the IME's own main thread, while touch input is delivered
   with input-pipeline priority - so this pattern is the signature of **AdaptKey's own main thread having
   no idle time between keystrokes** (messages starve, drain the instant typing pauses), not of the app
   lagging.
2. **The target app was demonstrably responsive throughout**: `updateComposing` performs a synchronous
   `getTextAfterCursor` read per keystroke (`isEditingMidWord`, §47 preview gate) - had the app's UI thread
   been stalled, those blocking reads would have stalled `updateComposing` itself; its log lines flowed at
   a steady ~300-400ms per keystroke the whole time.
3. That steady pace itself cannot distinguish "the user's real typing rhythm" from "queued taps being
   processed as fast as the pipeline allows" - but combined with the felt lag and the proven zero-idle main
   thread, queued taps are the consistent reading.

### Why this word specifically - the empty-candidates escalation

`DictionarySuggestionProvider.suggestionsFor()` runs per keystroke, and its expensive fallbacks are all
gated on `candidates.isEmpty()` - which for a long unknown compound like `gesamtpar…` is true on **every**
keystroke from ~7 chars on. So precisely the worst-case token runs the full chain each keystroke: umlaut-
unfold prefix scans, `fuzzyNeighbours` (first-char-bucket scan + per-candidate edit cost), **`CompoundSplit`
(which internally runs `highConfidenceCorrection(rest)` - a second full fuzzy pass)**, **`wideFuzzyNeighbours`
(cost-4 Levenshtein against the entire `g` first-char bucket - large in German - on an 18-char token)**, plus
`rawCoordinateCorrection` (D-131, also gated on empty candidates) and the D-106-stage-2 multi-dictionary
`knownInOtherLanguage` check for the pending autocorrect. Additionally `updateComposing` itself runs
`TokenRepair.trySplit` per keystroke (§47 split preview, O(token length) split points with store lookups
each) *before* pushing the letter to the field. D-138/D-153 are the standing precedent that stacking
per-keystroke lookups is a real, repeatedly-felt cost - this is the typing-forward analogue of the
backspace-hold case, with the additional twist that the expensive paths escalate exactly when the token is
long and unknown.

### Incidental finding - live confirmation of §101's premise (pre-§101 build)

At the moment the stale echo burst still reported positions 137-142, the reclaim's own synchronous
`getExtractedText` read had already resolved `anchor=143` - the true post-commit caret. Reads see reality
while callbacks lag: the exact property §101's truth verification is built on, observed live in an
unmodified v0.8.65 log. (The burst itself arrived while composing was empty, landing in the harmless
empty-branch no-ops - which is why this log shows lag but no jitter.)

### Proposed fix direction (not designed in detail, needs discussion)

Preferred candidate: **debounce the expensive empty-candidates tail**. Cheap work (prefix scan, ordinary
fuzzy) stays synchronous per keystroke; the expensive fallbacks (compound split, wide fuzzy,
raw-coordinate) run only once the token has been stable for ~150-250ms (`Handler.postDelayed`, S-04
resort-delay precedent) - during fast typing they never run, at the natural pause they still deliver.
Alternatives worth weighing: per-keystroke length caps on `trySplit`/preview work; negative-result caching
within a token. An instrumentation-first round (timing the preview and `suggestionsFor` durations via the
existing D-139 diagnostic channel) would confirm the millisecond attribution on-device before committing to
a design - the hypothesis above is traced from callback-queue behaviour, not from direct timings.

## §103 - D-160 Implemented: the Expensive Suggestion Fallbacks Are Debounced Off the Hot Path (v0.8.67)

§102's captured lag fixed as discussed and explicitly approved (option 1, the debounce). The user's design
question - whether the compound fuzzy search would have to be dropped entirely for being too expensive -
answered no, and that is the point of this design: the cost disappears from the hot path (while typing
fluently, where nobody reads the bar anyway - the same reasoning as D-138/D-153's `duringRepeat` gate), while
the feature keeps its full value at every natural pause, where a single run costs only tens of milliseconds
and is imperceptible off the typing path. Nothing is dropped, only deferred.

**Mechanics**: `SuggestionProvider.suggestionsFor()` gained an `includeExpensiveFallbacks: Boolean = true`
parameter - the default keeps full behaviour for every existing caller and test, so only the one hot path
changes. `DictionarySuggestionProvider` honours `false` by skipping exactly the two `candidates.isEmpty()`
blocks (D-116 compound split with its inner `highConfidenceCorrection` fuzzy pass, D-117 wide fuzzy);
`StubSuggestionProvider` ignores the flag (it has no expensive work). In `AdaptKeyService.refreshSuggestions()`
the per-keystroke call now passes `false`; when the cheap searches found nothing it schedules one deferred
pass (`expensiveSuggestionRunnable`, `EXPENSIVE_SUGGESTION_DELAY_MS = 200` - a starting value, easy to
retune) that re-runs the full pipeline with `true`. D-131's raw-coordinate suggestion, part of the same
empty-candidates escalation, waits for the same deferred pass. Staleness is handled twice over: every fresh
(non-deferred) refresh cancels a pending pass outright - placed before the function's early returns, so
URL/login/foreign-context bailouts cancel too - and the runnable itself re-checks that `composing` still
equals the token it was scheduled for (a commit, deletion, suggestion acceptance, or field change between
cancel points thus degrades to a no-op).

**Deliberately not touched this round**: the §47 split-colour preview (`TokenRepair.trySplit` per keystroke
inside `updateComposing`, ~15-20 store lookups for an 18-char token) and the per-keystroke
`autocorrectFor`/`knownInOtherLanguage` pending-chip work. Both are moderate, constant-shaped costs that
D-153 already partially gates; the §102 log attributes the saturation dominantly to the empty-candidates
escalation fixed here. Named as the next lever if any residual long-word lag remains on device.

3 new tests (`DictionarySuggestionProviderTest`: flag-off skips D-116, flag-off skips D-117, cheap results
identical under both flag values). 716 unit tests (was 713; +3). `:app:assembleDebug`/`:app:testDebugUnitTest`
green. Not yet device-confirmed - both this and §101's D-139 fix await the same testing round.

## §104 - D-161: Self-Healing Recheck for a Suspected WindowInsets Timing Race (v0.8.68)

Reported: sporadically, entering an app and bringing up the keyboard appears to render it stuck under the
gesture navigation bar, only "jumping" into its correct position on the first touch - too rare to have ever
been directly observed in the act. The device log supplied for it contained no evidence either way (the
`AdaptKey`/`AdaptKeyJitter` channels log only `onStartInput` and composing state, nothing about the view/
window lifecycle) - stated plainly rather than guessed past.

**Candidate mechanism, found by reading the code, not confirmed from a log**: the bottom (and, while
onboarding is shown, top) padding that keeps the keyboard above the gesture-nav/status-bar insets is set
exclusively inside `onCreateInputView()`'s `ViewCompat.setOnApplyWindowInsetsListener` callback (§42/D-136).
Android does not guarantee this callback fires before a freshly-created IME window's first visible frame -
a known, device-dependent timing gap. Until it fires, padding stays at its default `0`, which would render
the keyboard flush at the window's bottom edge - visually indistinguishable from "stuck in the gesture bar"
- and a subsequent touch event forcing a relayout would resolve it, matching "jumps up on first use"
exactly. Plausible and shaped right, but unconfirmed - the existing diagnostic channel cannot see window/
insets events at all, so this could not be verified from the log actually captured.

**User's proposed mitigation, implemented as suggested**: re-check the real state a fixed delay after the
keyboard is shown, and self-heal if it turns out wrong, rather than trying to catch the exact race live.
New `applyWindowInsetsPadding(view, insets)` extracts the listener's own padding computation (shared, not
duplicated) so the same math can be applied from a fresh, synchronous read. New
`windowInsetsRecheckRunnable`, scheduled from `onStartInputView()` (`handler.removeCallbacks` first, so
rapid app/field switching only leaves the most recent check pending) for `WINDOW_INSETS_RECHECK_DELAY_MS`
(1000ms, the user's own proposed value) later: reads the *current* real insets directly via
`ViewCompat.getRootWindowInsets()` - no dependency on any further callback firing - and only if the
padding it implies differs from what `inputRoot` currently has, applies it and logs the correction (channel
`AdaptKey`, warning level, before/after values). This doubles as the diagnostic instrumentation discussed
previously: a silent no-op on every ordinary occurrence would mean the hypothesis is wrong or the recheck
never actually catches the race in practice; a logged correction would be direct, on-device confirmation.

**Trade-offs, stated plainly**: this is a mitigation, not a proven root-cause fix - if the actual mechanism
differs from the hypothesis above, the symptom could still occur and simply resolve a second later than
observed (self-healing either way, but the diagnosis would then need revisiting). A real occurrence would
still be visible for up to ~1 second before the recheck corrects it - shorter than "wait for the user to
touch something," which was the previous, unbounded worst case. No new tests - Android view/window-inset
glue over already-tested `androidx.core.view` APIs, this project's established gap for this layer.
`:app:assembleDebug`/`:app:testDebugUnitTest` green (716 unit tests, unchanged). Not yet device-confirmed -
D-161 stays open either way: either the recheck never logs a correction (occurrence too rare to have
happened yet, or the hypothesis is wrong) or it does (confirming both the mechanism and the mitigation).

## §105 - D-161 Retuned; Large Device-Feedback Batch: Three Fixed, One Design Question Answered, Rest Captured (v0.8.69)

**D-161 retuned**: `WINDOW_INSETS_RECHECK_DELAY_MS` 1000->500ms per direct request - the user's own first
keystroke usually lands well before a full second elapses, which would make the correction land too late to
matter in practice.

A large batch of new device feedback followed, all traced against the real code before deciding what to do
with each - per this project's own established large-batch convention, implemented only what was
unambiguous and well-diagnosed, answered the one genuine design question directly, and captured the rest
rather than guessing at fixes for bugs that need a real repro or a design round first.

### D-162 - fixed: the suggestion bar could not be scrolled to reach overflowing suggestions
`SuggestionBarView` is a `HorizontalScrollView`, but its D-144 swipe-down-on-background fallback branch in
`onTouchEvent()` unconditionally returned `true` for every `ACTION_DOWN`/`ACTION_MOVE` that started off a
chip (empty background or a gap between chips) - regardless of direction. `super.onTouchEvent()` (native
horizontal scrolling) never got a chance to run at all for exactly that case. Fixed by always feeding
`super.onTouchEvent(ev)` the event first (so native scroll tracks and responds normally), only additionally
claiming it when the movement genuinely classifies as a downward swipe; everything else falls through to
whatever the scroll view itself decided. D-144's own swipe-down-to-dismiss is unaffected - still claimed
the moment it is confirmed.

### D-163 - fixed: no auto-capitalisation inside a recognised email field
`armShiftForNextWord()` (called on every field entry and after every commit) armed Shift per the ordinary
S6/C-07 field mandate unconditionally, with no `loginFieldKind`/`emailMode` check at all - an email address
is conventionally lower-case throughout, so this fought the user at field entry and after every `.`/`-`/`_`
segment. Fixed with a guard: while `loginFieldKind == EMAIL`, Shift is never auto-armed; a manually engaged
Caps Lock (D-15, the user's own explicit choice) is untouched.

### D-164 - fixed: "sue" too often replaced a typed "die" and never got corrected back
Reported precisely: typing "die" (lower-case) frequently lands as "Sue" (capitalised) instead, far too
often to be coincidence. Traced to the exact D-157 mechanism, not a new one: "sue" is not in the German
dictionary at all, so A-01 never protected it on the German side - the only thing standing between it and
correction was `knownInOtherLanguage()`'s cross-language loanword shield, which (correctly, for a genuine
loanword) refuses to autocorrect away any token that is a known word in any bundled dictionary, English
included. Once produced (most plausibly via T-02/T-03 raw-coordinate resolution reading the tap sequence as
s-u-e rather than d-i-e - s/d and u/i are each single QWERTZ-adjacent-key pairs), "sue" was then shielded
from ever being corrected back, exactly like D-157's original "due" case. Fixed the same way D-157 was
fixed - not by removing "sue" from dict_en.tsv (it remains a real, valid English word, "to sue" someone,
and should stay fully usable and protected as a genuine English word everywhere else) - by adding it to the
existing CROSS_LANGUAGE_CONFUSABLES exception list alongside "due". Ordinary edit-cost ranking then already
prefers "sie" (cost 1: only u/i differ, adjacent keys) over "die" (cost 2: two substitutions) with no extra
logic needed - matching exactly what the user asked for ("zuallererst auf 'sie' fallen"). An earlier
attempt this round to instead delete "sue" from dict_en.tsv was corrected by the user mid-round and
reverted before being committed - kept here as a matter of record, not because it shipped.

**Incidental finding, not acted on this round**: `DictionaryLoader.loadStores()` only seeds a language's
SQLite store from its bundled .tsv asset if the store is currently empty - an already-populated store from
a prior install is never re-seeded, and `SqliteDictionaryStore.DATABASE_VERSION` has stayed at 1 throughout
the project's history despite several prior direct dict_de.tsv/dict_en.tsv data edits (D-114's "vorhin",
Section 68's POS re-tags). Any data-only dictionary edit - like the reverted "sue"-removal attempt above
would have been - only reaches a fresh install or one where app data was cleared; an existing install's own
DB silently keeps the old data forever otherwise. D-164 itself is unaffected (it is compiled code in
AdaptKeyService.kt, not seeded data), but this is worth knowing before relying on a future .tsv-only fix
actually reaching an already-installed device without a version bump (which would also wipe that
installation's own learned words/frequencies and user-blacklist entries - a real trade-off, not a free
fix, and not something to do silently without discussion).

### D-173 - design question, answered directly: should a lone/near-lone recognised word take suggestion slot 1?
Asked directly: since S-02 already excludes the recognised, correctly-typed word from ever occupying a
suggestion slot (a deliberate "don't waste a slot on an echo of what's already typed" rule), should that be
relaxed when it is the only candidate left, or one of at most three? Assessment: leaning against it, even
the narrower "only when it is the sole remaining candidate" form. The bar's job is to offer alternatives;
S-05's live green highlight already confirms "this is a recognised, valid word" for free, without spending
any bar real estate - showing the same word again as a chip would partly duplicate that signal while also
reading as an actual suggestion (something to consider changing to), which it is not. The bigger cost is
predictability: "the bar never shows what I already typed" is a clean, always-true rule the user can rely
on; making it conditional on how many other candidates happen to exist trades that clean invariant for a
behaviour that is not obviously wrong, but also not obviously right, and is genuinely hard to predict from
the outside - exactly the "inconsistency trains the wrong expectation" concern the user's own question
raised. Not implemented; revisit if it turns out the empty bar itself reads as "broken" in practice rather
than merely quiet.

### Captured, not implemented this round - each needs a design round or a real device repro before fixing

- **D-165 - mid-word split can destroy deliberately-typed capitalisation.** Reported: "DiecVorschlaege",
  once split, becomes "die Vorschlaege" - losing the user's own explicit capital D. Traced: `applySplit()`
  recomputes both halves' casing purely from `capitalisation.capitalise(split.left/right,
  contextFor(...))` (generic sentence-position/POS rules) - `TokenRepair.trySplit()` itself lower-cases the
  whole token internally before searching, so the user's own typed casing is discarded entirely before a
  split candidate is even found, and never consulted again when re-applying case. Not yet fixed - see D-167.
- **D-166 - why does "DiecVorschlaege" only preview as splittable but never actually auto-split?** The live
  Section-47 preview (`splitPreview()`) and the actual commit-time split (`finalizeAndCommit()`) both call
  the identical `TokenRepair.trySplit()` - the only structural difference is that commit-time additionally
  vetoes the split whenever a `diacriticWord` or `highConfidenceWord` match exists (D-48/D-67). Statically
  tracing `diacriticRestoration()`/`highConfidenceCorrection()` against a 14-character nonsense compound
  like this suggests neither should plausibly match (both only search a bounded, first-character-bucketed
  candidate set for something close to the whole token) - but this was not verified against the actual
  repro on a device, and no other blocking gate was found in the static trace either. Genuinely unresolved -
  needs either a repro captured with the AdaptKeyJitter-style diagnostic (which does not currently cover
  this pipeline at all) or the user's own exact keystroke sequence to pin down precisely. Not guessed at
  further.
- **D-167 - proposed generalisation, needs a design round: an embedded mid-word capital should raise split
  confidence and be preserved, not be ignored.** Ties D-165/D-166 together - the user's own insight.
  Currently a capital letter appearing mid-token plays no role at all in `trySplit()`'s own search (it is
  lower-cased away first) or in confidence/ranking; the proposal is to use it as a genuine, generalisable
  signal both ways: raise confidence that a split point belongs exactly there, and preserve the user's own
  casing across the split instead of re-deriving it from generic rules. A real algorithm change to a
  currently pure, already-tested function (`TokenRepair.trySplit`/`WordExtent`) with real trade-offs (how
  much confidence boost, interaction with the existing Section-45 bigram-co-occurrence gate, whether it
  should also extend to D-116's compound split) - per this project's own convention for non-trivial design
  changes, deliberately left for its own discussion round rather than implemented from this capture alone.
- **D-168 - in Caps Lock / Shift, the umlaut alternatives in a key's long-press popup stay lower-case.**
  Root cause found: `AdaptKeyboardView.drawPopupCell()` draws each `popupAlternatives` entry's raw text
  completely unchanged - unlike the main key label (`labelFor()`, which does respect `shifted`/`capsLock`),
  no case transform is applied to the popup row at all, for any key. Not yet fixed: needs careful scoping
  before touching it, since `Key.alternatives`/`hint` also carries non-letter content that must never be
  case-transformed (symbol popups like the calculator's greek letters, Section-53's multi-character `TEXT`
  keys like `https://`) - the same "is this actually a word-forming letter in this context" distinction
  `AlternativeScript.extendsWord()` (Section 35) already makes for a related but different purpose. Scoping
  this correctly is the reason it was captured rather than patched in the same pass as D-162-D-164.
- **D-169 - entering a recognised email field should also open the settings row and flash the credential
  button, timed appropriately late.** Root cause of the current gap found: `weakSignalKind` (which drives
  the row auto-open + flash nudge in `onStartInputView`) is only ever computed when `loginFieldKind ==
  LoginFieldKind.NONE` - a reliably-detected EMAIL field (an InputType match, not a nudge) never sets it, so
  the row never opens for it today, even though `credentialModeActive` is still set correctly. The user's
  own request explicitly flags the timing needs care ("muss natuerlich entsprechend spaet stattfinden, auf
  diesem Pfad") - i.e. this is a genuine UX/sequencing design decision (when exactly relative to the row's
  own open animation, and whether an already-manually-toggled field should still re-flash), not a one-line
  fix - captured for its own discussion round rather than guessed at.
- **D-170 - email field: moving the caret after "foo." in "foo.bar", Backspace, then typing "@" jumps the
  caret to the end and inserts "@" after "bar" instead of where the caret was.** Not traced this round -
  explicitly the same class of caret/composing-state bug this exact session spent three rounds (Sections
  99-101) tracing from real device logs rather than guessing at, and email-mode fields run through their own
  largely separate `commitVerbatimFieldFragment()` path (D-142/D-158) where every `.`/`@`/`-`/`_` is its own
  delimiter under the ordinary token model - genuinely unfamiliar territory for the D-139 fix's own
  reasoning to be assumed to cover. Needs a device log of the exact repro before touching anything here.
- **D-171 - "Ddr" too often committed instead of "Der"; the user's own suspicion is the raw-touch model,
  not the dictionary.** Checked directly against the bundled data: DDR is a real dictionary entry (frequency
  4405, NOUN) - exactly the D-113 scenario (a known word overridden by Section-44's 50x-frequency-ratio rule
  at cost 1, "der" at ~1,004,234 is ~228x more frequent) - and D-113's own history explicitly names this
  pair as confirmed working after that fix. On paper, the dictionary-side path should already correct this.
  The user's own suspicion is more likely right: this probably lives in the T-02/T-03 raw-coordinate
  correction path instead (a systematic touch-zone drift for keys near d/e, or the raw correction fallback
  overriding what the dictionary path would otherwise have fixed) - not traced this round; needs a device
  log of the actual repro, per the user's own explicit request to examine the raw touch recording carefully
  before considering a blacklist as a last resort.
- **D-172 - why doesn't "aks" autocorrect to "als"?** Checked directly against the bundled data and every
  gate in `bestCorrection()`: "aks" is in neither dictionary (confirmed) so A-01 does not apply at all;
  length (3) clears MIN_AUTOCORRECT_LENGTH (2); k/l are confirmed keyboard-adjacent (KeyboardProximity's own
  row map), so the edit cost to "als" is exactly 1 - well inside MAX_CORRECTION_COST (2); "als"'s frequency
  (191,841) is far above minAutocorrectFrequency (300); "als" is not blacklisted. Every gate traced says
  this should already autocorrect. No blocking mechanism was found in the static trace - genuinely
  unresolved without either a repro captured via the diagnostic channel or the user's own exact context
  (e.g. whether tier 3 is installed and active, or whether the full token typed was actually longer than
  "aks" alone before the delimiter landed). Not guessed at further.

3 fixes, all Android-glue/private-service-method changes with no new pure logic - no new tests, matching
this project's established gap for this layer (`SuggestionBarView`'s touch handling, `armShiftForNextWord`'s
private field guard, and `knownInOtherLanguage`'s private exception set are all already-established
untested categories; `CROSS_LANGUAGE_CONFUSABLES` itself had no test for "due" either). 716 unit tests
(unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. None of D-162/D-163/D-164 have been
device-confirmed yet.

## §106 - D-166 Withdrawn; D-170 Fixed (Real Device Log); D-175 Diagnostic Added for Never-Saved Credentials (v0.8.70)

**D-166 withdrawn** (the "DiecVorschlaege" highlights-but-never-auto-splits question captured in §105) -
per direct instruction: the user cannot currently reproduce it and will re-open with a fresh repro if it
recurs, matching the D-148 precedent for this outcome. D-165 (mid-word split can destroy deliberately-typed
capitalisation) and D-167 (the generalised embedded-capital-as-confidence-signal design idea) are untouched
by this - the user's "streiche es" was specifically about the highlight/auto-split question, not those two.

### D-170 fixed: a delimiter typed mid-fragment in a login/email/URL field always landed after the whole fragment
Reported precisely and reproduced from a real device log: in K9 Mail's email compose field, typing
"foo.bar", moving the caret back to just after "foo" (before the "."), Backspace (deleting the "."), then
typing "@" - expected "foo@bar", the caret instead jumped to the very end and "@" landed after "bar"
("foobar@"). Traced end to end against the log's own position trail, not guessed: `finalizeAndCommit()`'s
D-142/D-143 login/urlMode branch returns *before* its own D-119/D-120 "is the caret mid-word" check ever
runs, straight into `commitVerbatimFieldFragment()` - which committed the *entire* composing fragment
verbatim, delimiter appended at its end, with zero awareness of `composingCursor`. So the moment the caret
sat anywhere but the fragment's own end when a delimiter arrived (a repositioned tap, exactly this repro),
the delimiter landed in the wrong place - a login/email/URL-field-specific gap the ordinary word path's
D-119/D-120 fix never covered, since it structurally cannot be reached from there. Fixed by mirroring
`splitComposingAtCaretAndCommit()`'s own already-correct before/after split and arithmetic anchor
computation (D-122's own no-post-mutation-read principle) directly inside `commitVerbatimFieldFragment()`,
without its recursive `finalizeAndCommit()`/autocorrect step, which a verbatim fragment never uses anyway.
The user's own proposal to instead simply stop treating "@" as a delimiter was considered and declined:
"." (and "-"/"_") would still carry the identical latent bug for anyone who edits mid-fragment with those
characters instead - fixing the general mechanism, not the one character, matches this project's own
long-standing "find the general rule" preference (§62) over a per-character workaround. No new tests -
Android `InputConnection` batch-edit glue, the same established gap as `splitComposingAtCaretAndCommit`
itself.

### D-175 (credential storage) - own email address in a confirmed-active credential field is never learned
Reported directly, alongside a proposed explanation: maybe the suggestion bar is simply suppressed
throughout email mode, so even a successfully-learned credential would never be offered. Checked against the
actual code: not the case - `refreshSuggestions()` already routes to `showCredentialSuggestions()` whenever
`loginFieldKind != NONE`, which reads `CredentialStore.all(this)` live on every composing update and would
render real entries if any existed. The suggestion-bar side is not disabled; the more likely explanation is
that nothing is actually being learned in the first place, or the wrong value is. Traced
`captureCredentialIfLoginField()` (called from both `handleEnter()` and `onFinishInput()`,
`MIN_CREDENTIAL_LENGTH = 2`, `credentialCaptured` correctly reset per field in `onStartInput`) end to end -
no gap was found by reading the code alone; every gate looks correct on paper, which is exactly the kind of
situation this project treats as "needs real data, not another guess." **Diagnostic added, same D-number**:
`captureCredentialIfLoginField()` now logs every early-return reason (`kind`/`credentialCaptured` state, a
null `getTextBeforeCursor` read, a too-short value) and the exact value and kind it ultimately learns, to
the `AdaptKey` channel - to be removed once D-175 is actually closed. Not yet fixed - waiting on the next
real occurrence's log before touching anything further.

1 fix (D-170) plus diagnostic-only instrumentation for D-175 - no new tests (Android `InputConnection`/glue,
consistent with the established gap for this class of change). 716 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. D-170 not yet device-confirmed; D-175 stays open
pending a captured log of the next occurrence.

## §107 - D-171/D-176: "ddr" and D-164's Cross-Language Shield Redesigned onto a Real, Data-Driven Blacklist (v0.8.71)

Corrected mid-round, twice, by direct instruction - a genuine design discussion, not a guess either time.

**Round 1 (this session's own D-164 fix, now superseded)**: "sue"/"due" were shielded from autocorrecting to
"sie"/"die" by `knownInOtherLanguage()`'s cross-language loanword protection, waived via a short hardcoded
Kotlin exception list, `CROSS_LANGUAGE_CONFUSABLES`.

**Round 2 (§105/§106's own D-171 capture)**: for "Ddr"/"Der" the user clarified precisely - "DDR" genuinely
is a real German dictionary entry (confirmed, frequency 4405, `NOUN`), just one the user almost never wants
against how often they type "der". Proposed putting "ddr" on "the same blacklist as due" - which surfaced a
real misunderstanding to correct: "due"/"sue" were never on the actual A-04 blacklist at all, only on the
separate, narrower `CROSS_LANGUAGE_CONFUSABLES` exception list. An initial reply proposed blacklisting
"due"/"sue" in the *English* store to unify everything under A-04 - **wrong**, and corrected by direct
instruction: the original design was always the *German* blacklist. Re-checked against the actual code and
confirmed correct once re-derived properly: `knownInOtherLanguage()` is only ever consulted while the
*active* dictionary is German (or whichever language the current token resolved to via
`selectActiveDictionary()` - `dictionaryStore` at that point already reflects it), so a block against
`dictionaryStore`'s own blacklist inherently never engages once the active language is genuinely English -
"due"/"sue" typed and meant as real English words stay fully protected the moment English is actually
selected, exactly as `CROSS_LANGUAGE_CONFUSABLES` already guaranteed, just via the general A-04 mechanism
instead of a bespoke Kotlin set. The proposed English-store blacklisting would **not** have had this
property (it would have also blocked "due"/"sue" while genuinely typing English) - the corrected,
German-store design was the right one from the start.

**Implemented**: `knownInOtherLanguage()` now checks `dictionaryStore.isBlacklisted(token)` (the active
dictionary's own A-04 blacklist) instead of the removed `CROSS_LANGUAGE_CONFUSABLES` set - a token
blacklisted in whichever language is currently active for it waives the cross-language shield, a strictly
more general mechanism (any bundled language could carry its own such entries the same way, not only
German) that also naturally extends to the settings-editable blacklist the user floated as a future idea,
with no further code change needed once that UI exists. New `BlacklistCategory.BUNDLED` (distinct from
`USER`'s live G-04 drag-to-trash additions) marks these as app-shipped, not user-added. New
`installStores()`-driven `seedBundledBlacklist()` blacklists `"due"`, `"sue"`, `"ddr"` in the German store on
every call - both the initial synchronous in-memory placeholder and the real SQLite store once
`loadDictionariesAsync()` finishes - so a fresh install, an existing one, and the transient in-memory stage
all end up with these entries. `blacklist()` is a plain upsert, so calling it every service start is cheap
and idempotent; critically, this reaches an **already-installed** device immediately, unlike the
`DictionaryLoader`/`DATABASE_VERSION` reimport path noted in §106, which would need a destructive full
reimport instead.

**"ddr" itself is a different, simpler case than "due"/"sue" - not a cross-language shield at all.** It is a
real German dictionary word, so `DictionarySuggestionProvider.isKnownWord()`'s own existing
`store.isKnownWord(word) && !store.isBlacklisted(word)` check is sufficient on its own: once blacklisted,
`isKnownWord("ddr")` is permanently `false` regardless of the word's own underlying frequency data (the
blacklist flag always wins), so `bestCorrection()`'s A-01 gate is skipped entirely and "der" (cost 1, vastly
more frequent) wins outright - no ratio-override even needed anymore. Verified this also satisfies the
user's stated expectations precisely: A-07's Backspace-undo is completely blacklist-agnostic (reverts
committed text unconditionally, regardless of why the correction fired), and `learnWord()`'s own D-37
pending-count promotion path (`PendingLearnStore.increment(...) >= LEARN_THRESHOLD`) - the one route by
which a *non*-blacklisted unknown word can eventually get learned from repetition - cannot ever re-establish
`isKnownWord("ddr") == true`, since the blacklist check is a separate, always-applied `AND NOT` condition
independent of whatever frequency accumulates underneath; "wird also so nie gelernt" holds precisely, not
approximately.

No new tests - `knownInOtherLanguage`/`seedBundledBlacklist` are private `AdaptKeyService` glue calling
already-tested `DictionaryStore.blacklist()`/`isKnownWord()`, the same established gap as D-157/D-164's own
(untested) exception list before it. 716 unit tests (unchanged). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. Not yet device-confirmed.

## §108 - D-172 ("aks"/"als"): Reproduced from a Device Log, Still No Blocking Gate Found - Diagnostic Added (v0.8.72)

A fresh, easy-to-reproduce device log confirmed the report precisely: typing "Aks" (sentence-initial, Google
Keep) committed unchanged - no correction to "Als" at all. `finalizeAndCommit()` was read in full this round
(past where §105's original trace stopped) to find the actual autocorrect-application logic, not only the
A-05 split veto - every gate it touches was re-checked by hand against this exact token and still comes back
clear: `MIN_AUTOCORRECT_LENGTH`/`MIN_AUTOCORRECT_CANDIDATE_FREQUENCY` (300, confirmed) both clear "als"
(frequency 191,841); `k`/`l` are confirmed keyboard-adjacent (cost 1); `RegularVerbInflection.isPlausibleInflection("aks", ...)`
traced by hand against every listed ending - none actually strip anything from a token ending in "s", so it
returns `false`, not a blocker; `LanguageClassifier.isForeign()` requires `minWords = 2` (checked directly in
the class) - a single-word context returns `false` immediately, before any statistical classification even
runs. Every one of these says "Aks" should correct to "Als".

**One variable the diagnostic channel has never shown**: which dictionary (`dictChoice.language`) was
actually resolved as active for this token. A concrete, checkable hypothesis: if G-01's active language was
English at the time (left there manually, or via D-130's sustained-English auto-promotion) rather than
German, the entire autocorrect search would run against the English lexicon, which has no entry for "als"
at all - fully explaining the observed behaviour with no bug anywhere. Not confirmed either way from this
log alone (it was never logged). Diagnostic added to `finalizeAndCommit()`: `dictChoice.language`,
`suppressAutocorrect`, and the `diacriticWord`/`autocorrected`/`rawCorrected` values are now all logged
together right where they are computed, so the next repro (reported as easy to reproduce) settles this
definitively instead of guessing further. No new tests (diagnostic logging only, matching every prior
instance of this pattern this session). 716 unit tests (unchanged). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. D-172 stays open pending the next captured log.

## §109 - D-172: the English-Language Hypothesis Ruled Out, `suppressAutocorrect` Confirmed True; a Second, Self-Inflicted Finding (v0.8.73)

User confirmed German was genuinely the active language, then supplied a repro with §108's diagnostic
already in place: `finalizeAndCommit: dict=GERMAN suppressAutocorrect=true diacriticWord=null
autocorrected=null rawCorrected=null`. This rules out the English-language hypothesis outright and pins the
block precisely: `autocorrectFor()` was never even called, because `suppressAutocorrect` was `true` before
it. This directly contradicts a hand-trace of `LanguageClassifier.isForeign()` (re-read in full this round,
not from memory) - its own `minWords = 2` gate should make a single-word context (`" Aks"`, since
`tokenContextBefore` should be empty for the first word of a fresh note) return `false` immediately, before
any statistical distance is even computed. The contradiction was not resolved by further hand-tracing -
`suppressAutocorrect` is itself an OR of two independent conditions
(`dictChoice.suppressAutocorrect` from A-03's language classification vs. `knownInOtherLanguage()`'s D-106
stage 2 cross-language check) that the prior diagnostic line conflated into one boolean, and
`tokenContextBefore`'s actual runtime value was never logged at all. Both gaps closed: `finalizeAndCommit()`
now logs `tokenContextBefore` and the two `suppressAutocorrect` sources separately, so the next repro
attributes the true value to its actual source instead of leaving both live hypotheses (a wrongly-triggered
foreign-language classification, or an unexpected cross-language/blacklist match) unresolved.

**Second, distinct finding surfaced along the way, from the user's own observation**: "aks" now colours
green (S-05, "recognised word") when it visibly had not before - and does not appear as a suggestion chip at
all, so G-04's drag-to-trash gesture can never reach it. Explained, not guessed: the repeated testing
itself (at least three prior verbatim, uncorrected commits of "Aks" across this investigation) crossed D-37's
own `LEARN_THRESHOLD = 2` - `learnWord()`'s `PendingLearnStore` promotion path silently taught the German
dictionary that "aks" is a real word, purely as a side effect of `suppressAutocorrect` blocking its
correction on each attempt. It cannot be reached via G-04 because S-02 never shows the word matching the
current input as its own suggestion. Practical fix pointed out: `BlacklistActivity` (C-05) already exists as
a settings-level dictionary/blacklist editor and can remove it directly, no code change needed for the
user's own device. Left as its own open question, not fixed this round: D-37's learning threshold has no
safety net at all against silently learning a nonsense token after only two repetitions - worth a design
round of its own if it turns out to matter beyond this self-inflicted testing scenario.

No new tests (diagnostic logging only). 716 unit tests (unchanged). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. D-172 stays open pending the next captured log, now with both
`suppressAutocorrect` sources and the raw context string visible.

## §110 - D-177: Learned Words Moved to a Separate Store; Two-Stage Unlearn-Then-Provisional-Blacklist; a New Learned-Words Editor (v0.8.74)

Grew directly out of two D-172 (§109) side observations and one more the user brought back to "ddr" (§107):
a testing artefact ("diecVorschläge") got blacklisted instead of merely unlearned when dragged to trash, and
"aks" - silently learned as a side effect of repeated failed corrections - could never be reached via G-04 at
all, since S-02 never offers the current-input word as its own suggestion. Both exposed the same underlying
gap: G-04's drag-to-trash and §107's A-04 blacklist had been conflated into a single mechanism, when they are
actually two different situations that need different treatment.

**The user's own design, arrived at over several corrected rounds, not guessed at once**:

1. **Two categories of "words that shouldn't have been learned" are not the same problem.** A genuinely
   bundled dictionary word the user rarely wants against a much more common neighbour (`"ddr"` vs. `"der"`,
   §107's own case) has nothing to unlearn - it must go straight to a permanent blacklist entry, exactly as
   §107 already implemented. A word the keyboard itself *learned* from the user's own typing
   (`"diecVorschläge"`, `"aks"`) is different: the first drag-to-trash should simply unlearn it via D-37's
   existing mechanism, not immediately blacklist it - a single accidental commit should not permanently
   condemn a word the user might legitimately type again.
2. **Only a genuine recurrence earns a permanent blacklist entry.** After unlearning, the word is marked
   *provisionally* pending for a configurable window (`pendingBlacklistExpiryDays`, default 7, settings-editable
   `SeekBarPreference` 1-30 - the user asked for this to be a real, visible settings value specifically so the
   mechanism itself is discoverable, not because frequent retuning is expected). If the same word crosses
   D-37's `LEARN_THRESHOLD` again inside that window, `learnWord()` now escalates it straight to a *permanent*
   `BlacklistCategory.USER` entry instead of re-learning it - the recurrence itself is the signal that this
   was not a one-off accidental commit. If the window lapses without a recurrence, the provisional mark
   simply disappears (checked and cleared lazily on the next `learnWord()` call for that word) and the word
   is once again an ordinary candidate for D-37 learning, exactly as if it had never been dragged to trash.
3. **"Blacklisting" a genuinely bundled word (`"ddr"`) has always meant something different from
   "unlearning" a self-taught one, so G-04 must branch on which kind of word it actually is.**
   `onBlacklistWord()` now checks `dictionaryStore.isBundledWord(word)` first: a bundled word is blacklisted
   immediately (§107's existing path, unchanged), a learned-only word is forgotten and provisionally marked
   instead (this round's new path).
4. **The learned lexicon must live in a store the bundled dictionary asset never touches, corrected mid-round
   by direct instruction.** An initial proposal added an `origin` column to the single shared words table,
   with a migration story for already-learned words on existing installs (treat them as bundled, a one-time
   inexact edge case). Overridden with the better design: "Ich würde das Wörterbuch absolut unangetastet
   lassen" - a fully separate table, so a future app update can ship a refreshed bundled dictionary without
   ever touching or resetting anything the user has personally taught the keyboard. This also eliminates the
   migration edge case outright: an existing install's learned words already live in the one place a bundled
   reimport can never reach, nothing to reconcile.
5. **A new settings screen makes every learned word reachable, including ones G-04 structurally cannot
   reach.** `"aks"` (§109) proved G-04's own suggestion-bar drag gesture cannot remove a word matching the
   current input (S-02), so it needed its own always-available editor, independent of what the suggestion bar
   happens to be showing at the time.

**Implemented, across `DictionaryStore`'s two backing implementations
(`SqliteDictionaryStore`/`InMemoryDictionaryStore`), `AdaptKeyService`, and settings:**

- `SqliteDictionaryStore` gains three new tables (`TABLE_LEARNED`, `TABLE_LEARNED_BIGRAMS`,
  `TABLE_PENDING_BLACKLIST`), created via `ensureLearnedSchema()` using `CREATE TABLE IF NOT EXISTS`, run
  unconditionally from both `init {}` and `onCreate()` - additive, non-destructive, and reaches
  already-installed devices immediately without bumping `DATABASE_VERSION` (which would trigger `onUpgrade()`'s
  destructive drop-and-recreate, wiping existing learned/blacklist data - the same established constraint
  §107 already worked around for its own bundled-blacklist seeding). `learn()`/`unlearn()` now write only to
  the learned tables; every "does the keyboard know this word" read (`entryOf`, `frequencyOf`, `isKnownWord`,
  `partsOfSpeech`, `unigramsByPrefix`, `bigramFrequency`, `nextWords`, `allKnownWords`,
  `correctionCandidates`) merges bundled and learned sources (summing frequencies, deduplicating word sets),
  so nothing else in the autocorrect/suggestion pipeline needs to know the split exists at all.
- `DictionaryStore` gains `forget(word)` (removes a learned word outright, regardless of accumulated
  frequency - unlike `unlearn()`, which only reverses a single learning event), `isBundledWord(word)`,
  `learnedWords()` (for the new editor), and `markPendingBlacklist()`/`pendingBlacklistedSince()`/
  `clearPendingBlacklist()` for the provisional-mark round-trip. `InMemoryDictionaryStore` mirrors the same
  split with plain `HashMap`s for unit testing.
- `AdaptKeyService.onBlacklistWord()` branches on `isBundledWord()` as described above.
  `learnWord()` gains an `isPendingBlacklistRecurrence(word)` check (comparing
  `pendingBlacklistedSince()` against `settings.pendingBlacklistExpiryDays`, clearing an expired mark as a
  side effect) ahead of the existing `PendingLearnStore` promotion branch: a recurrence forgets the word,
  blacklists it permanently, and clears the provisional mark, reporting `LearnOutcome.SKIPPED` (A-07's
  Backspace-undo correctly does nothing for `SKIPPED`, matching how a genuine promotion-into-blacklist should
  behave - there is nothing left to undo back to).
- New `LearnedWordsActivity` (settings screen, `d177_learned_words` entry under the dictionary category):
  a per-language spinner plus a list of every word in `learnedWords()`, tap-to-remove with confirmation,
  running the identical forget-then-provisionally-mark action as G-04's own learned-word branch. Structurally
  mirrors the existing `BlacklistActivity` (C-05), minus the add-word row - this screen is remove-only.
- New `pendingBlacklistExpiryDays` setting (`AdaptSettings`/`SettingsMapper`/`SettingsStore`, default 7,
  clamped 1-30) with its own `SeekBarPreference` in `settings_preferences.xml`, right after the existing
  blacklist entry.

14 new tests (8 `InMemoryDictionaryStoreTest`, 6 `SqliteDictionaryStoreRoboTest`) covering `isBundledWord`
against both bundled-only and also-learned words, `forget()` removing a multiply-reinforced learned word
outright while leaving a bundled word's own frequency untouched, `learnedWords()` excluding bundled entries
and sorting by descending frequency, the pending-blacklist mark/check/clear round-trip, and merged-frequency
correctness for a word that is both bundled and learned. All 14 existing `InMemoryDictionaryStoreTest` cases
and all existing `SqliteDictionaryStoreRoboTest` cases pass unchanged against the rewritten stores, confirming
the merge-preserving design: every existing `learn()`/`unlearn()` scenario nets to the identical merged
frequency it did before the split, just via two tables added together instead of one table incremented
directly. 730 unit tests total (716 + 14 new). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
`LearnedWordsActivity` and the new settings row are SQLite/Android-facing UI, consistent with the established
gap for this class of change (same as `BlacklistActivity` before it) - not yet device-confirmed.

## §111 - D-178/D-179: One-Time Bundled-Dictionary Reimport to Flush Pre-D-177 Contamination; Clearer Remove-Confirmation Dialogs (v0.8.75)

**D-178.** Directly reported: "aks" (§109's own self-inflicted D-37 finding) does not appear in the new
`LearnedWordsActivity` (§110) at all, so it cannot be removed there either. Root cause, not guessed: "aks"
was learned back on v0.8.73, *before* v0.8.74's own split existed - at that time `learn()` still wrote
straight into `TABLE_WORDS`, the bundled table, since there was no separate `TABLE_LEARNED` yet to write to
instead. So on this device "aks" is physically a row in `TABLE_WORDS`, byte-for-byte indistinguishable from a
real bundled entry - `isBundledWord("aks")` reports `true` (wrongly, for this one pre-existing case), and
`learnedWords()` correctly does not return it, since it never looks at `TABLE_WORDS` at all. This is exactly
the one-time migration edge case §110's own design discussion had already flagged as solved "for free" by the
separate-table design - true for everything learned *after* updating to v0.8.74, but not for what a prior
build had already written into the wrong table before that split existed.

The user reasoned through the fix directly: bump some version marker so the bundled dictionary gets replaced
in the DB, flushing the previously-learned words back out of it. Implemented as asked - `DictionaryLoader`
gains `BUNDLED_DICTIONARY_VERSION` (bumped to `1` this release, tracked per-store in a new tiny `meta` table,
deliberately separate from `SqliteDictionaryStore`'s own schema `DATABASE_VERSION`, which controls table
structure, not bundled content). New `SqliteDictionaryStore.resetBundledWords()` clears only `TABLE_WORDS`/
`TABLE_BIGRAMS` - never the learned overlay, the blacklist, or the pending-blacklist marks - and
`loadStores()` now calls it, then reseeds from the asset, exactly once per store whenever
`bundledContentVersion() < BUNDLED_DICTIONARY_VERSION`, before recording the new version. A fresh install
still goes through the existing `isEmpty()` seeding path unchanged and simply records the current version
directly, without ever calling `resetBundledWords()` on an empty table. Since "aks" is not a real dictionary
word, the reseed does not bring it back - it is gone from `TABLE_WORDS` once this release reaches the
device, with the genuinely-learned overlay (empty on this device, since v0.8.74 has not yet had a chance to
teach it anything real) left completely untouched, exactly as asked.

**D-179.** Directly reported: both remove-confirmation dialogs (`BlacklistActivity`, `LearnedWordsActivity`)
showed only the bare word as the message with generic "Cancel"/"OK" buttons - nothing in the dialog itself
said this was a delete operation. Fixed by giving each dialog a title that states the action and names the
word (`"Remove “%1$s” from the blacklist?"` / `"Forget “%1$s”?"`), and a positive button labelled with the
actual action ("Remove" / "Forget") instead of generic "OK" - a purely additive-strings change across all
three locales (`values`/`values-de`/`values-el`), no behavioural change to either activity beyond the dialog
text itself.

3 new tests (`SqliteDictionaryStoreRoboTest`): `bundledContentVersion()` defaults to 0 for a store that never
recorded one, `setBundledContentVersion()`/`bundledContentVersion()` round-trip, and `resetBundledWords()`
wipes only the bundled unigram/bigram tables while a learned word (with its own bigram) survives untouched.
D-179 is dialog text only, no new tests needed. 733 unit tests total (730 + 3 new).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Neither the reimport nor the dialog wording is yet
device-confirmed.

## §112 - D-180: Saved Credentials Get Their Own Reviewable List; a Shared "Copy" Option on Every Remove Dialog (v0.8.76)

Directly reported: the top-level "Clear saved usernames & emails" settings action (D-142) was all-or-nothing
- no way to review what had actually been learned, or remove just one wrong entry while keeping the rest,
unlike the C-05/§110 blacklist and learned-word editors it sat right next to in spirit. The user's own
design: turn it into a list screen first, structurally close to those two, with a "Delete all" button for
the old behaviour, per-entry removal via **long press** rather than a plain tap (a saved credential is more
sensitive than an ordinary word - a stray tap should not risk deleting one), and a third "Copy" option on
the remove dialog for pulling a value back out to the clipboard - the user immediately generalised this last
part to the existing blacklist/learned-word dialogs too ("Vermutlich ist das sogar auch sinnvoll für die
Lösch-Dialoge in den beiden anderen Listen"). Also requested: move the settings entry itself out of the
top-level Info & Privacy category into the Dictionary group, where the blacklist/learned-words/expiry
entries already live.

**Implemented, all as asked:**

- New `CredentialsActivity` (settings screen): lists every `CredentialStore.all()` entry (value, kind,
  frequency), a "Delete all" button (reusing D-142's existing confirm dialog/strings unchanged, just moved
  here from the top-level action), and `listView.setOnItemLongClickListener` opening a remove-confirmation
  dialog per entry - titled with the value, a "Remove" positive button, "Cancel", and a neutral "Copy"
  button that writes the value to the clipboard via `ClipboardManager` (the same pattern `DiagnosticLogActivity`
  already established for its own log-copy button) and toasts confirmation. The "Delete all" button disables
  itself when the list is empty, so it can never fire a no-op "cleared" toast over nothing.
- New `CredentialStore.forget(context, value)`: removes exactly one entry by its case-insensitive value
  (matching `learn()`'s own identity rule), complementing the existing delete-everything `clear()`.
- The former top-level `d142_clear_credentials` settings action (in `cat_info`, with its own click listener
  and confirm dialog in `SettingsActivity`) is gone; a plain navigating `Preference` (`d142_credentials`,
  matching the `c05_blacklist`/`d177_learned_words` pattern exactly - title, summary, `<intent>` to the new
  activity, no click-listener code in `SettingsActivity` at all) now lives inside `cat_dictionary`, appended
  after the pending-blacklist-expiry `SeekBarPreference`. `SettingsActivity`'s now-dead
  `confirmClearCredentials()` and its `CredentialStore` import were removed along with it.
- `BlacklistActivity` and `LearnedWordsActivity`'s own remove dialogs (§111/D-179's new titled versions)
  each gained the identical neutral "Copy" button, copying the word to the clipboard the same way. A single
  shared string pair (`copy_to_clipboard_action`/`copy_to_clipboard_done`) backs all three dialogs, rather
  than three near-identical per-screen copies.

3 new tests (`CredentialStoreRoboTest`): `forget()` removes only the matching entry, matches
case-insensitively (mirroring `learn()`'s own identity rule), and is a harmless no-op on a value that was
never learned. 736 unit tests total (733 + 3 new). `:app:assembleDebug`/`:app:testDebugUnitTest` green. New
settings screen and its long-press gesture not yet device-confirmed.

## §113 - D-172/D-181: Root Cause Found and Fixed - "AKS" Is a Genuine Bundled English Dictionary Entry (v0.8.77)

D-172, open since §108, finally closed by a fresh device log with §109's diagnostic in place:
`dictChoice.suppressAutocorrect=false knownInOtherLanguage=true`. The German-language classification was
never at fault - `LanguageClassifier.isForeign()`'s `minWords=2` gate really does return `false` for a
single-word context, exactly as re-verified by hand several times across §108/§109; every one of those
re-reads was correct. The actual block was always the *other* half of the OR'd `suppressAutocorrect`:
`knownInOtherLanguage("aks")`, D-106 stage 2's cross-language shield, checks whether the token is already a
known word in some *other* consulted language's dictionary - and it is. `grep`-ing the actual bundled asset
confirms it directly: `dict_en.tsv` contains `AKS	18	PROPER_NOUN` - a genuine Wikipedia-derived English
acronym entry, not stale data, not something D-177/§111's dictionary-reset touched (that only ever concerned
the *German* store's contamination, and "AKS" belongs in English on its own merits). §109's own "aks now
colours green" side finding (D-37 silently learning it after repeated failed corrections) had made the real
cause harder to isolate at the time, but is now fully explained too: two independent mechanisms were
compounding - D-37 pending-learn promotion made it *known* (fixed already, D-178/§111's reimport flushed
it), while D-106 stage 2's cross-language shield independently made it *uncorrectable* (this round's fix) -
solving one without the other would have left "Aks" silently uncorrected forever, exactly what the user kept
observing.

This is architecturally the identical failure mode as "due"/"sue" (§107): a token that is a real word in a
consulted *other* language must not be corrected away while some *other* active language is being typed
(mandatory English + every G-01-cycle language) - working exactly as designed, just for an entry ("AKS", the
acronym) the user never actually intends to type. Fixed the identical way: `"aks"` added to the existing
`BUNDLED_GERMAN_BLACKLIST` (now `due`/`sue`/`ddr`/`aks`), seeded into the German store by the already-existing,
idempotent `seedBundledBlacklist()` on every `installStores()` call - reaching every existing install
automatically, no reimport needed. `knownInOtherLanguage()`'s existing `if (dictionaryStore.isBlacklisted(token))
return false` early-out then bypasses the cross-language shield for "aks" specifically, letting the ordinary
autocorrect proceed to "als" - unlike "ddr", "aks" is not itself a real German dictionary word, so there is no
A-01 side effect to reason about at all, only the removal of an unwanted protection.

No new tests - `knownInOtherLanguage`/`seedBundledBlacklist`/`BUNDLED_GERMAN_BLACKLIST` are private
`AdaptKeyService` glue, the same established, untested gap as the rest of D-176/§107's own blacklist seeding.
736 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed
(2026-07-19): "aks" now corrects to "als" problem-free. D-172/D-181 fully closed.**

## §114 - D-182: `reclaimSurroundingWord()` Stops Deleting Text It Only Needs to Recolour (v0.8.78)

**D-181 side item (no code change)**: reported "aks" showing in the C-05 blacklist as `USER` rather than
`BUNDLED` - a leftover from manually adding it there by hand before D-181's code fix existed (the editor's
category picker defaults to whatever was last selected). `seedBundledBlacklist()`'s `INSERT OR REPLACE`
already always wins with `BUNDLED` the next time `AdaptKeyService.onCreate()` actually runs - which any
install/update naturally triggers, so no fix was needed here, only an explanation. Deliberately **not**
turned into a reseed-on-every-editor-visit mechanism - the user explicitly confirmed manually re-adding it
as `USER` later should stick, not be silently overwritten, matching this project's D-177 stance that a
removed entry's removal must actually hold.

**The real fix, D-182**: a follow-up repro (a fresh word typed alone, then a second word typed and deleted)
showed the "word deleted" symptom from §113's report was actually already gone on the current build - but a
new, related symptom remained: the cursor visibly jumps to the very start of the field for an instant during
the same reclaim. Traced precisely against the log: `reclaimSurroundingWord()` calls
`ic.deleteSurroundingText(...)` to remove the committed word from the document, then `updateComposing()`
calls `ic.setComposingText(...)` moments later to put it back as *styled* (S-05-coloured) composing text -
between those two calls the reclaimed span is briefly empty, and the cursor has nowhere to sit but position
0 (or, mid-text, the reclaim's own start offset) for that instant. The user asked directly whether a
"replace" function existed that would do this better - clarified that no single InputConnection call can
both target an arbitrary already-committed range *and* apply styled composing text in one step (styling only
attaches via `setComposingText`, which alone can only replace the current selection/cursor position, not an
arbitrary range elsewhere); `InputConnection.setComposingRegion(start, end)` - the API Android documents
specifically for "mark this already-existing text as being actively edited by the IME" - plus a follow-up
`setComposingText()` for the styling is the correct two-call sequence, not a delete-based workaround. (A
genuinely atomic single-call `replaceText()` exists but only from API 33 onward; skipped rather than adding
a version-gated fallback path purely to save one call on newer devices, given `setComposingRegion()` already
covers every supported API level cleanly.)

**A second, more serious log** (a real device capture from the same morning) showed this same delete-based
transient state causing actual data loss, not just a visual flash: tapping far back into already-committed
text landed mid-word in "geminibund" (a D-62 reclaim), and while the *first* stale echo of the reclaim's own
`deleteSurroundingText()` step was correctly recognised and ignored by §101's ground-truth verification, a
*third* differently-stale `onUpdateSelection` callback arrived shortly after, reporting yet another position
- and this time the synchronous ground-truth read **also** returned the same transient collapsed-at-anchor
position, causing §101's own EXTERNAL branch to fire and tear down the correctly-rebuilt `"geminibund"`
composing token entirely. This is the same root instability (the Gemini search field appears to echo a
delete-based edit's transient states unusually persistently) fooling even the ground-truth check itself,
not a new, independent bug - not fully proven to be resolved by this fix alone (a third echo reporting a
wholly different composing span, `[65,75]`, is harder to fully explain), but removing the delete-based
transient state removes the thing every observed echo in both logs was actually echoing.

**Fixed**: `reclaimSurroundingWord()` now calls `ic.setComposingRegion(anchor, anchor + before.length +
after.length)` to mark the already-existing text as composing in place - the characters are never removed
from the document at any point, so there is no empty/transient state left for a misbehaving editor to
observe, echo, or have a ground-truth read agree with. Falls back to the old `deleteSurroundingText()`
behaviour only when the anchor could not be resolved at all (`anchor < 0`, e.g. a failed `ExtractedText`
read) - the same already-conservative rare case this method already special-cases. `updateComposing()`
itself is unchanged: its `setComposingText()` call now simply restyles the region `setComposingRegion()` just
established, rather than re-inserting fresh characters into a moment-ago-empty span.

No new tests - `reclaimSurroundingWord()` is `AdaptKeyService`-internal `InputConnection` glue, the same
established gap as the rest of this class. 736 unit tests total (unchanged). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. Not yet device-confirmed - specifically needs the exact "tap far back
mid-word after a long gap" scenario re-tested, since that is the one where a genuine tear-down (not just a
visual flash) was observed.

## §115 - D-183/D-184/D-185: Three Backlog Items Captured, Not Implemented (v0.8.79)

Explicitly requested as backlog captures only ("nimm ins Backlog auf") - no tracing, diagnostic, or
implementation done this round; no code changed.

### D-183 - mid-text suggestion tap still inserts a superfluous space, including before a period
Reported: applying a suggestion-bar tap while the caret sits mid-text (not at the end of the document) still
inserts an extra space that should not be there, and doing so immediately before a period leaves a stray
space *before* the period - almost always wrong. Not yet traced against `handleSuggestionTap()`/the D-119/
D-120 mid-word-delimiter-split machinery or `SPACE_EATING_PUNCTUATION` (§29/D-29's own "absorb a trailing
space before punctuation" mechanism, which evidently is not reached or not sufficient for this specific
suggestion-tap path). Needs a real device log capturing the exact suggestion tapped, the caret position, and
the surrounding text before tracing further - not to be guessed at.

### D-184 - the typing "flash" highlight effect needs a shorter duration
Reported: the visual flash effect that plays while typing (S-05/§4-area word-recognition highlight, or
possibly a key-press flash - needs disambiguating against the actual animation code once picked up) feels
long enough to make typing seem sluggish, and specifically swallows the visual feedback of a double-tap
(two flashes overlapping/cancelling rather than reading as two distinct presses). Needs identifying the
exact animation/duration constant responsible (likely in `AdaptKeyboardView` or `SettingsRowView`'s existing
`FLASH_DURATION_MS`-style constants, though this may be an entirely different, key-press-specific effect not
yet located) before it can be safely shortened without breaking whatever readability need set the current
duration in the first place.

### D-185 - a togglable URL keyboard, defaulting on, with a normal-keyboard override for search-in-URL-bar use
The existing `urlMode` flag (D-142/D-143, `AdaptKeyService.isUrlField()` checking `EditorInfo.inputType` for
`TYPE_TEXT_VARIATION_URI`) is automatic today - there is no way to switch back to the ordinary letter
keyboard while a URL-variation field is focused. Requested: entering a URL field must also reveal the
settings row (mirroring how the row already surfaces its existing buttons) with a **new toggle button, shown
only while `urlMode` is active** (reserving no space in the row otherwise, exactly like the credential-mode
button's own always-shown-but-state-reflecting pattern would not fit here - this one must not appear at all
outside a URL field) - positioned second from the left, immediately after the existing credential-mode
button (`SettingsRowView`'s 🔑, currently the row's leftmost button). Defaults to on (URL keyboard active)
the moment a URL field is entered; toggling it off switches to the ordinary letter keyboard while still
inside the same URL field. Motivation given directly: a browser's URL bar (Chrome named specifically) can
also be used to type an ordinary search query, which needs the normal keyboard, not the URL-specialised one.
Needs a design pass on: exactly how "settings row must appear on URL-field entry" interacts with the row's
existing swipe-up/swipe-down show/hide gesture (§48/§69) and any other auto-show triggers already in place,
and how the per-field toggle state should reset (or not) between separate URL-field visits.

No code changes, no new tests. 736 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest`
green (unaffected).

## §116 - D-172/D-181 Device-Confirmed; D-184/D-185 Implemented (v0.8.80)

**D-172/D-181 device-confirmed**: "aks" now corrects to "als" problem-free, as reported. §113's fix is fully
closed - see that section's own updated final line.

### D-184 - key-press flash: root cause was structural, not the duration
This is the *third* round reporting the exact same "flash feels sluggish / a double-tap doesn't show two
distinct blinks" symptom (round 2 / §13 shortened it 80ms→28ms; round 3 / §14 shortened it again to today's
28ms, both times believing the constant itself was the fix). Per this project's own rule of re-questioning a
diagnosis after a "fixed" point gets negative feedback again rather than reapplying the same fix a third
time: traced `AdaptKeyboardView`'s actual touch state machine instead of shortening `flashDurationMs` again.
Root cause, confirmed directly from the code (not guessed): `ACTION_DOWN` already calls `cancelFlash()`
before setting `pressedKey = key`, but both happen synchronously in the same handler before the single
resulting `invalidate()` - so the screen never renders a genuine "off" frame between the previous tap's
afterglow ending and the new tap's press-highlight starting. For a same-key double-tap arriving within the
28ms afterglow window (a very realistic case for two fast presses of one letter, e.g. "ll"/"ss"), the key
paints as continuously highlighted across both taps, reading as one long flash rather than two distinct
ones - independent of how short `flashDurationMs` itself is, which is why shortening it twice already never
fixed this specific complaint.

Fixed by decoupling the *paint* of a repress from the underlying press/keystroke logic (`pressedKey` and all
touch handling - long-press, backspace-repeat, release - are set/read exactly as before, so this cannot drop
or delay an actual keystroke, only its highlight): `paintSuppressedKey` (`AdaptKeyboardView.kt`) masks the
one key that still has an active `flashKey` at the moment of a new `ACTION_DOWN`, forcing it to paint
unpressed for one `repressGapMs` (16ms, ~1 frame) window before the normal highlight resumes - a real,
render-visible off-then-on blink for the specific same-key-repress case, left untouched for every ordinary
(non-overlapping) tap. `flashDurationMs` itself was left at 28ms (already at the "Gboard-like, ~2 frames"
floor per §13/§14's own reasoning; a fourth reduction had nothing left to gain).

No new tests - `AdaptKeyboardView`'s touch/paint state machine is the same established untested Android-glue
gap as the rest of D-04/D-14/D-144's own history in this file. Not yet device-confirmed - this is a genuine
behavioural risk (untestable without an emulator/device) rather than a proven fix; if a same-key double-tap
still reads as one flash, or the added 16ms gap itself feels laggy, that is the next thing to trace.

### D-185 - togglable URL keyboard: built on the existing `urlMode` flag, no new state
`AdaptKeyService.urlMode` was already the single flag every consumer reads (`AdaptKeyboardView`'s D-143
bottom-row swap, `finalizeAndCommit`'s verbatim-commit branch, `refreshSuggestions`' empty-bar branch) - so
letting the new settings-row button flip `urlMode` directly, rather than introducing a second "is URL mode
overridden" variable, makes every one of those consumers respect a manual toggle for free, with nothing to
keep in sync. `onStartInput` already unconditionally recomputes `urlMode = isUrlField(info)` on every fresh
field focus, which is exactly D-185's own "defaults to on the moment a URL field is entered" requirement -
a prior visit's manual toggle-off never carries into the next field visit, without any extra reset code.

Button visibility ("shown only while `urlMode` is active", i.e. must not appear at all outside a URL field,
but must stay reachable after being toggled off so the user can turn it back on) is deliberately tracked by
`isUrlField(info)` - the field's own fixed type - rather than by the live, now-toggleable `urlMode` value
itself; conflating the two would have made the button disappear the moment it was used to switch off, with
no way back short of leaving the field. `SettingsRowView.urlModeButtonVisible` (visibility) and
`.urlModeActive` (background, mirrors `credentialModeActive`'s own styling) are therefore two independent
properties, pushed from `AdaptKeyService.onStartInputView` (`urlModeButtonVisible` from a fresh
`isUrlField(info)` check, `urlModeActive` from the current `urlMode`).

Layout: `SettingsRowView.content` is a `FrameLayout` (fixed per-child margins, not a flow layout), so
inserting a button "second from the left, immediately after the credential-mode button" - `emojiButton`'s
own existing slot - needs the display to actively move `emojiButton` aside rather than just adding a sibling.
`urlModeToggleButton` (🌐) permanently occupies `emojiButton`'s original slot (`GONE` by default so it
reserves no space); `urlModeButtonVisible`'s setter shifts `emojiButton`'s own `marginStart` one slot
further right for as long as the toggle button claims that slot, restoring it when not - both computed via a
new small `slotMarginStart(index)` helper instead of duplicating the existing hand-written margin arithmetic
a third time (credential/emoji/clear-clipboard/touch-zone were each already one own copy of the same
formula).

Row auto-open: mirrors the existing `weakSignalKind` precedent in `onStartInputView` exactly (open the row +
reveal a button on a relevant field focus) rather than inventing new interaction/gesture-conflict logic -
addresses the spec's own open question about interaction with the row's swipe-up/down gesture, since
`settingsRow?.closeImmediately()` already runs unconditionally first on every fresh field presentation and
the row's open()/close() state is otherwise untouched by this change. Unlike the *weak-signal* login case
(only opens on an unreliable signal), a URL field always opens the row on entry, even though `isUrlField`
is a fully reliable `InputType` check - without at least one guaranteed reveal, the new button would be
practically undiscoverable, since it lives nowhere else.

`AdaptKeyService.toggleUrlModeFromSettingsRow()`: flips `urlMode`, re-pushes it to the view and to
`settingsRow.urlModeActive`, then closes the row and refreshes suggestions - the same three-step shape as
the existing `toggleTouchZoneVisualizationFromSettingsRow()`/`toggleCredentialModeFromSettingsRow()`.

No new tests - `SettingsRowView`/`AdaptKeyService` field-focus and click-listener glue is the same
established untested Android-only gap as every other settings-row button (D-142/D-156/§69). 736 unit tests
total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19):
works as intended. D-185 closed.**

**D-184 device feedback (2026-07-19): no visible effect yet.** Left open, to be re-diagnosed in a future
round rather than patched again speculatively - see this project's own rule of re-questioning a diagnosis
after negative feedback, not re-tweaking the same fix a third/fourth time. The `paintSuppressedKey` repress
gap above is the current state, unconfirmed.

## §117 - D-183: Fixed - a Mid-Word Reclaim Leaves the Real Cursor Inside the Token, Not at Its End (v0.8.81)

D-183 was initially misdiagnosed as editor-specific: the first three repro logs supplied (Google Keep) mostly
showed an unrelated symptom (external caret jumps mid-composing, already tracked under D-139/§99-101's own
"Keep-specific interaction" precedent), not D-183 itself - no suggestion-bar tap appears in any of them. The
user clarified precisely and supplied a fourth log (Signal this time, "100% reproducible, egal wo") isolating
the *actual* mechanism: tap into the *middle* of an already-committed word (a D-62 reclaim), then apply a bar
suggestion to it - a space gets inserted right after the word even though real whitespace is already there.

**Root cause, confirmed directly from the code, not the editor**: `onSuggestionClicked()`'s `NORMAL` branch
already carried a D-144 guard for exactly this class of bug (`applying a suggestion mid-text ... must not add
a second space when one is already there`) - `ic.getTextAfterCursor(1, 0)?.firstOrNull()?.isWhitespace()`.
That check is correct only when the real `InputConnection` cursor sits at the *end* of the composing token,
which is true for ordinary end-of-typing suggestion acceptance (the case D-144 was written for) but **not**
for a D-62 mid-word reclaim: `composingCursor` there is the tap offset *inside* the token (e.g. the repro's
`composing="bdg"` reclaimed with `cursor=2`, i.e. tapped between 'd' and 'g'), so the real cursor sits mid-
token, not at its end. `getTextAfterCursor(1, 0)` at that position returns the token's *own* next character
(here, `'g'` - about to be overwritten by `commitText()` two lines later), never the real document text that
follows the *whole* token - so `alreadySpaced` was always `false` for a mid-word reclaim, regardless of what
actually followed, and the unconditional `" "` got appended next to whatever whitespace was already there.

Fixed by skipping past the token's own remaining characters before checking: `remainingComposingChars =
composing.length - composingCursor` (0 for the ordinary end-of-token case, reducing to the exact previous
check - not a behaviour change there), then `ic.getTextAfterCursor(remainingComposingChars + 1, 0)
?.getOrNull(remainingComposingChars)` reads the real character right after the token's true end regardless
of where inside it the cursor sits.

`applyMidWordSplitSuggestion()`/`applySplit()` (the D-122 mid-word-*connector-split* suggestion path, taken
only when a suggestion word itself contains a space) were not touched - they always pass a fixed `" "`
delimiter with no "already spaced" check of any kind, a related but separate gap; not fixed here since it
was never reproduced and is a different code path with its own A-05 auto-split callers to consider.

No new tests - `onSuggestionClicked`'s `InputConnection` interaction is the same established untested
Android-glue gap as the rest of this function (D-142/D-122/D-144 above it). 736 unit tests total (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. **Device-confirmed (2026-07-19): works as intended.
D-183 closed.**

## §118 - D-186/D-187/D-188: Learned-Overlay Flooding, Settings-Row Auto-Close, List-Activity Insets (v0.8.82)

Three items from the same device-feedback round; D-186 fixed forward + a historical cleanup migration,
D-187 and D-188 are straightforward behavioural/layout fixes with no design ambiguity worth discussing first.

### D-186 - the learned-words overlay was flooded with ordinary bundled vocabulary
Reported: the Learned Words editor (§110) filled up with plain dictionary words ("die", "du", "immer", ...),
making it useless as a review list of what was actually *taught*. Root cause, confirmed directly from the
code: `AdaptKeyService.learnWord()`'s first branch (`provider.isKnownWord(word)` - true for *any* known word,
bundled or learned) unconditionally called `dictionaryStore.learn(word, context)` on every single commit of
an already-bundled word - `SqliteDictionaryStore.learn()`'s own KDoc *already* documented this as deliberate
(D-177: "always the learned table, regardless of whether word is also a bundled entry ... reinforcing an
already-bundled word ... adds/updates a small personal overlay ... for ranking"). That original design traded
a small ranking-personalisation signal for exactly this flooding, and the user has now weighed that trade-off
directly and rejected it: a bundled word must never be written to the learned overlay at all, in any casing.

Fixed forward: `learnWord()` now checks `dictionaryStore.isBundledWord(word)` (already existed on the
interface, already case-insensitive - `entryOfIn`'s lookup key is `word.lowercase()`) *before* consulting
`provider.isKnownWord()`, and returns `LearnOutcome.SKIPPED` immediately when true - nothing to learn about a
word the dictionary already ships with. The remaining `provider.isKnownWord()` branch now only ever fires for
a genuinely previously-*learned* word (not bundled), still reinforcing it as before. `learnWordStrong()`
(D-13's "undo a wrong split" promotion) got the same guard for consistency, though in practice it can only
ever receive an unknown word (A-05 only splits tokens that were unknown to begin with). `reinforceFromTier3()`
needed no change - `AdaptiveLearning.learningSignal()` already returns `null` whenever `tier1KnewWord` is
true, which already covers bundled words (`isKnownWord` merges both sources), so it never could have written
one.

Historical cleanup: fixing the write path does nothing for words already flooded into existing installs, so
- mirroring D-178/§111's own precedent exactly (`BUNDLED_DICTIONARY_VERSION` gating a one-time
`resetBundledWords()`) - a new `LEARNED_CLEANUP_VERSION` (`DictionaryLoader`) gates a one-time
`SqliteDictionaryStore.purgeBundledDuplicatesFromLearned()` per store on the next load, tracked separately
(`learnedCleanupVersion()`/`setLearnedCleanupVersion()`, its own `TABLE_META` key) from the bundled-reseed
version so the two migrations stay independent. Deletes every `TABLE_LEARNED`/`TABLE_LEARNED_BIGRAMS` row
whose key also exists in `TABLE_WORDS` - never touches `TABLE_WORDS`/`TABLE_BIGRAMS`, the blacklist, or a
genuinely self-taught word (one with no bundled counterpart).

3 new tests (`SqliteDictionaryStoreRoboTest`: `learnedCleanupVersion` default/round-trip,
`purgeBundledDuplicatesFromLearned` removes only the bundled duplicate). 739 unit tests total (736 + 3).
`AdaptKeyService.learnWord`/`learnWordStrong` themselves are the same established untested Android-glue gap
as the rest of that file. `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

### D-187 - the settings row auto-closed after every button tap
Reported, by direct instruction: the row should generally not hide itself right after a button confirms an
action - at minimum never for the toggle buttons, and for now not at all, pending further feedback on
whether any specific *action* button (not a toggle) should get it back. Mechanical fix, no design ambiguity:
every one of the six settings-row button handlers in `AdaptKeyService` (`openEmojiPanelFromSettingsRow`,
`openSettingsAppFromSettingsRow`, `clearClipboardFromSettingsRow`, `toggleTouchZoneVisualizationFromSettingsRow`,
`toggleCredentialModeFromSettingsRow`, `toggleUrlModeFromSettingsRow`) no longer wraps its action in
`closeSettingsRow { ... }` - the action now runs directly, and the row stays open. For the toggle buttons
this also *fixes* a real functional conflict the old code introduced for its own convenience:
`toggleTouchZoneVisualizationFromSettingsRow`'s auto-close existed specifically "so the overlay is actually
visible ... not hidden behind the row" - i.e. the previous design required closing the row to see the very
thing the toggle turns on, which is exactly the kind of side effect the user is now rejecting outright.
`closeSettingsRow()` itself is untouched - still used by the downward-swipe-to-dismiss gesture
(`dismissKeyboardOrCloseSettingsRow`) and the unconditional fresh-field reset in `onStartInputView`, neither
of which is "a button tap confirming an action".

No new tests - button-tap wiring is the same established untested Android-glue gap as every other settings-
row handler. 739 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

### D-188 - list-editor activities' controls slid under the notch/gesture-nav again
Reported: controls in the Learned Words and Credentials list editors were unreachable, pushed under the
display cutout/gesture-nav area. Same root cause as K-01's own edge-to-edge fix (§13): Android 15
(targetSdk 35) draws every activity edge-to-edge, and `BlacklistActivity`/`LearnedWordsActivity`/
`CredentialsActivity` (added D-112/D-177/D-180, all three structurally identical - language spinner/list
editors) never got `CalibrationActivity`'s own inset-padding fix, only a static `android:padding="16dp"` in
each layout XML. Fixed identically in all three (not just the two the report named - same layout, same bug):
each root `LinearLayout` gained an `android:id`, and `onCreate()` now installs the same
`ViewCompat.setOnApplyWindowInsetsListener` CalibrationActivity already uses (`maxOf(statusBars, cutout)` on
top, `maxOf(navigationBars, systemGestures)` on bottom) - added *on top of* the existing static 16dp rather
than replacing it, so spacing is unchanged wherever the inset itself is zero. Kept as three near-identical
inline blocks rather than extracting a shared helper, matching `CalibrationActivity`'s own existing inline
style rather than refactoring working code to introduce one.

No new tests - `WindowInsets` layout glue is Android-only, same class of gap as `CalibrationActivity`'s own
inset fix. 739 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

**Device-confirmed (2026-07-19): D-186/D-187/D-188 all work as intended. All three closed.**

## §119 - D-174 Closed: a Fast-Tearing-Down `InputConnection` Beat `onFinishInput`'s Own Read of It (v0.8.83)

D-174 (labelled D-175 in §106, where it was opened one day earlier as a diagnostic-logging-only round after
"no bug found by reading `captureCredentialIfLoginField` alone" - the same item, the code comment's own
number was a one-off typo never corrected until now) is now closed, from a real device log: the user's own
K9 Mail
recipient ("To:") field, `inputType` variation confirmed `0x20` (`TYPE_TEXT_VARIATION_EMAIL_ADDRESS`) -
`loginFieldKind` classification itself was never the problem, exactly as the prior static read had already
concluded. The user typed their own address in full (visible in the log only as separate composing
fragments between commits - not reproduced here or anywhere else in this repo, by explicit request).

**Root cause**: right as focus left the "To:" field for "Betreff" (Subject), the log shows a burst of five
`onUpdateSelection` callbacks within about 24ms, each hitting the D-139/§101 "mismatch ... ground truth
unavailable - ignored conservatively" branch (`ic.getExtractedText()` itself returning null) - K9 converts
the typed address into a recipient "chip" the instant the field loses focus, tearing down that field's
`InputConnection` faster than anything can read it. `onFinishInput()`'s own fallback call to
`captureCredentialIfLoginField()` runs immediately after, and its `ic.getTextBeforeCursor()` read hit that
exact same dead connection: `"getTextBeforeCursor returned null - kind=EMAIL"` - the value was genuinely
typed, genuinely classified, and then unrecoverable by the time anything tried to read it back out of the
document. Neither `handleEnter()`'s own capture path helps here - the user moved to the next field by
tapping it directly, never triggering the field's IME_ACTION submit at all, a completely ordinary way to
fill out a multi-field compose form.

**Fixed by no longer depending on being able to read the value back out of a possibly-already-dead
connection at all**: a new in-memory `credentialSnapshot` (`AdaptKeyService`, reset per field in
`onStartInput`) mirrors every fragment `commitVerbatimFieldFragment()` actually commits into a login-
relevant field (gated on `loginFieldKind != NONE`, so a pure `urlMode` field is skipped) - built from data
the service already assembles for the commit itself, never re-read from the `InputConnection`, so it cannot
be affected by the same connection dying moments later. `captureCredentialIfLoginField()` still tries
`ic.getTextBeforeCursor()` first (unchanged, cheap, and correct for the ordinary case where the connection
survives) and only falls back to `credentialSnapshot + composing` (the still-uncommitted tail, e.g. a
fragment cut short by the same fast teardown) when that read comes back null. The two sources are never
concatenated together on the success path - `finishComposingText()` immediately above the read already
finalises `composing` into the document, so `before` alone already contains it; appending `composing` again
there would double the tail.

No new tests - `InputConnection`/field-teardown timing is Android-only and not reproducible without the
exact app/timing that produced the original log; the same established gap as the rest of this function.
739 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

**Device-confirmed (2026-07-19): works as intended, closed.**

## §120 - D-189: `SettingsRowView` Renamed to `ExtraRowView`; Credential-Mode Reveal + Delayed Flash (v0.8.84)

Two items, requested together: a reliable-credential-field reveal bug found while testing D-174, and a
rename by direct request.

### Reveal bug - entering a reliably-classified credential field never showed the row
Reported: entering the E-Mail field sets credential mode, but the row never appears to show it. Root cause,
confirmed by re-reading `onStartInputView`'s own credential block: it only ever called `open()` (+
`flashCredentialModeButton()`) when `weakSignalKind != NONE` - the *unreliable*-signal nudge path (D-142).
A *reliably* classified EMAIL/PASSWORD field (`loginFieldKind` set directly from `InputType`, no nudge
needed) only ever updated `credentialModeActive`'s background colour, silently, exactly mirroring D-185's
own "must also reveal the row" requirement for `urlMode` - which already got this right, this path just
never did. Fixed: the row now opens whenever `loginFieldKind != NONE` **or** `weakSignalKind != NONE`
(mutually exclusive by construction - `weakSignalKind` is only ever computed while `loginFieldKind` is still
`NONE` - so this never double-opens). The credential button now also flashes on the reliable path, not only
the weak-signal one, so a field-entry reveal is never silent.

The flash must wait for the reveal to actually finish - a flash fired the instant `open()` returns would run
while the row is still mid-slide-in, before the button is even visible. `ExtraRowView.open()` gained an
`onOpened: () -> Unit = {}` parameter (mirroring `close()`'s existing `onClosed`), invoked once the height
animation completes (or synchronously if already open, same as `close()`'s own already-closed case) - the
call site is now `extraRow?.open { extraRow?.flashCredentialModeButton() }`.

### D-189 - `SettingsRowView` → `ExtraRowView`
Requested directly: the class/variable/function names all said "settings row", but the row long ago outgrew
being about the settings button alone (emoji, clear-clipboard, touch-zone, credential-mode and URL-mode
buttons all followed) - by the user's own description, still mentally reaching for "extra row" and having to
translate every time "settings row" came up. Mechanical rename, no behaviour change beyond the two items
above: `SettingsRowView.kt` → `ExtraRowView.kt` (class `SettingsRowView` → `ExtraRowView`), and every
`AdaptKeyService` identifier that said "SettingsRow"/"settingsRow" (the `extraRow` field, `openExtraRow()`/
`closeExtraRow()`/`dismissKeyboardOrCloseExtraRow()`, and every `...FromExtraRow()` button handler renamed
from `...FromSettingsRow()`) - `openSettingsAppFromExtraRow()` keeps "Settings" only where it genuinely
still means the Settings app (the gear button's own action), not the row. Prose "settings row" /
"settings-row" in `AdaptKeyService`'s own comments updated to "extra row" / "extra-row" throughout, for the
same reason - a half-renamed codebase would not actually fix the described mental friction. Historical
spec/progress entries before this section are deliberately left saying "settings row" - this file's own
append-only convention (§48 etc.) records what was true at the time, not retroactively rewritten.

No new tests - button-tap/reveal wiring is the same established untested Android-glue gap as every other
extra-row handler. 739 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
**Device-confirmed (2026-07-19): works as intended. D-189 closed.**

## §121 - D-190: "Never Save Credentials" Setting (v0.8.85)

Requested directly alongside a Contacts-permission discussion (recommended against for now - a keyboard
requesting `READ_CONTACTS` is a much heavier trust ask than for an ordinary app, and a runtime permission
dialog can only be shown from an `Activity`, not the IME `Service` itself, so the requested "ask on first
email field" convenience is not achievable the way it was imagined; a Settings-only opt-in toggle, matching
the existing Tier3ModelActivity/CalibrationActivity pattern, was recommended instead - not built this round,
this section covers only the credential-recording opt-out that followed). A privacy-conscious opt-out for
D-142's credential recording, explicitly scoped by the user to never affect passwords (already true
unconditionally in the code - `captureCredentialIfLoginField` excludes `LoginFieldKind.PASSWORD` before any
other check, regardless of this setting) and to default **off** (recording stays on by default, matching
the just-validated D-142/D-174 behaviour - a feature that was just confirmed working is not silently
disabled by a new setting's own default).

New `AdaptSettings.neverRecordCredentials` (default `false`), threaded through the established
`RawSettings`/`SettingsMapper`/`SettingsStore` pattern (`d190_never_record_credentials` preference key, a
plain passthrough boolean - no clamping needed) exactly like `tier3Enabled`/`diagnosticLogEnabled`. New
`SwitchPreferenceCompat` in the `cat_dictionary` category, directly below the existing "Saved usernames &
emails" entry, in all three shipped locales (DE/EN/EL) - title + a summary spelling out the password
exclusion and that already-saved values keep being suggested (this setting only stops *new* ones from being
added, it does not suppress suggestions from what is already stored).

Gates two sites in `AdaptKeyService`, both already established from D-174: `captureCredentialIfLoginField()`
now also skips (alongside its existing `NONE`/`PASSWORD`/`credentialCaptured` early-return) when the setting
is on, and `commitVerbatimFieldFragment()` skips accumulating into `credentialSnapshot` in the same case -
no point holding an in-memory copy of a value that will never be read back out of it. `showCredentialSuggestions()`
is untouched by design - suggesting an already-saved value is a different operation from recording a new
one, and the setting's own name/summary is scoped to recording only.

1 new test (`SettingsMapperTest`: passthrough default/round-trip, mirroring the existing
`tier3Enabled`/`diagnosticLogEnabled` tests exactly). 740 unit tests total (739 + 1). `AdaptKeyService`'s own
two gated call sites are the same established untested Android-glue gap as the rest of D-142/D-174.
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

**Device-confirmed (2026-07-19): works as intended, closed.**

## §122 - D-191: Opt-In Contact-Derived Email Suggestions (v0.8.86)

Follow-up to §121's Contacts-permission discussion: the user weighed the "keyboard asking for Contacts"
trust cost directly and confirmed the recommended shape - opt-in only, permission requested exactly at the
moment the Settings toggle is switched on, never proactively on first use of an email field (confirmed not
buildable that way regardless - only an `Activity` can show the runtime permission dialog, not the IME's own
`Service`). Contacts are read live at suggestion time and never persisted locally, by explicit request -
storing them would be redundant with the address book itself.

**Manifest**: `READ_CONTACTS` added (a real, visible change to the app's permission footprint - the first
one beyond `VIBRATE`, still declaring no `INTERNET` either way, so the provably-offline guarantee is
unaffected). Declaring it in the manifest does not itself request or grant anything; nothing changes for a
user who never turns the new setting on.

**Settings (D-191)**: new `AdaptSettings.contactsSuggestionsEnabled` (default off), threaded through
`RawSettings`/`SettingsMapper`/`SettingsStore` exactly like every other simple boolean setting
(`d191_contacts_suggestions_enabled`); a `SwitchPreferenceCompat` in `cat_dictionary` below D-190's own
toggle, all three locales, summary spelling out the live-read/never-stored point directly.

**Permission flow lives entirely in `SettingsActivity`** (not the keyboard service - explained in that
class's own new KDoc, referenced from the manifest comment too): a `registerForActivityResult(RequestPermission())`
launcher, registered unconditionally at Fragment construction per the documented contract. The toggle's
`setOnPreferenceChangeListener` intercepts a switch-to-on when the permission is not already granted -
blocks the immediate flip (`return false`) and launches the system dialog instead; the launcher's own
callback resolves the switch to its true final state (`isChecked = true` on grant, `= false` + an explanatory
toast on denial). Already-granted (including a user re-enabling the setting later, or after a permission
already held from before) and switching off both skip the dialog entirely and go straight through - a
previously granted permission is never re-asked for, matching the "don't nag" requirement directly.

**Reading contacts (`AdaptKeyService`)**: `loadContactEmailsAsync()` re-checks both the setting and the live
`READ_CONTACTS` grant (defence in depth - the permission could have been revoked via system settings after
being granted) before doing anything. Queries `ContactsContract.CommonDataKinds.Email.CONTENT_URI` on a new
dedicated `contactsExecutor` (mirrors `tier3Executor`'s own single-thread pattern), capped at
`CONTACT_EMAIL_LIMIT` (2000) rows, applying the result to `contactEmailCache` back on the main thread via
`handler.post`. Triggered once per EMAIL field focus (`onStartInput`, alongside the existing credential-field
reset block), **not per keystroke** - deliberately mirrors D-160's own "an external, unpredictably-sized read
has no business on the hot path" reasoning; a synchronous full-address-book read on every keystroke (or even
just on field focus) risked exactly the jank D-160 already fixed for a different expensive-lookup source.

`showCredentialSuggestions()` now sources its ranking input from a new `mergedCredentialEntries()` instead of
`CredentialStore.all(this)` directly: cached contact addresses are wrapped as ordinary, never-persisted
`CredentialEntry(value, EMAIL, frequency = 0)` values and appended, deduplicated by value against the real
stored entries (a real, learned entry - with genuine frequency - always wins over a same-valued contact
entry) so nothing is ever offered twice. Flowing through the existing `CredentialRanking.suggestionsFor`/
`emailDomainsFor` unchanged means the `@`-domain-completion path picks up contact domains too, for free -
no second ranking path was needed. `showCredentialSuggestions()`'s own `PASSWORD` short-circuit is untouched
(contacts never apply there either, matching D-190's own password exclusion).

1 new test (`SettingsMapperTest`: `contactsSuggestionsEnabled` passthrough default/round-trip, mirroring
D-190's own from §121 exactly). 741 unit tests total (740 + 1).
`AdaptKeyService`'s `loadContactEmailsAsync`/`mergedCredentialEntries` and `SettingsActivity`'s permission
flow are Android-only glue (`ContentResolver`, `ActivityResultLauncher`), the same established untested gap
as the rest of D-142. `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - in
particular the actual on-device permission-dialog flow and a real address-book read need a real test pass.

**Device-confirmed (2026-07-19): works as intended, including the permission-dialog flow and a real
address-book read. Closed.**

## §123 - D-192: Feature Catalog Refreshed; "Learn More" Hint; D-191 Summary Trimmed (v0.8.87)

### D-192 - the feature-overview catalog was stale since D-89 (v0.7.43)
Asked directly: is the Settings feature list still current? Checked via `git log` on `FeatureCatalog.kt`
itself - untouched since the commit that created it (D-89, v0.7.43); the app is now at v0.8.87. Confirmed
stale by comparing the 18 existing entries against every spec section added since, filtering out pure bug
fixes/refinements (not catalog-worthy) from genuine new user-facing capability. Eight were missing:
credential memory (D-142/D-180/D-190, merged into one entry - detection+suggestion+review+opt-out reads as
one coherent capability from the user's side), contact-derived email suggestions (D-191, kept separate - its
own distinct opt-in/permission story), the URL/email dedicated keyboards (D-143/D-158/D-185, merged - same
underlying idea applied to two field types), the learned-words editor (D-177), password-manager autofill
suggestions (D-135), unhyphenated-compound recognition (D-116), the mid-word connector-split suggestion
(D-122), and the extra row itself (§48ff - swipe up for emoji/clear-clipboard/touch-zone-visualisation, never
catalogued even though it has existed since v0.8.40). `FeatureCatalog.ENTRIES` grew from 18 to 26
(`d89_f19` … `d89_f26`, continuing the existing numbering - one catalog, not a new feature of its own), all
three shipped locales (DE/EN/EL). `FeatureCatalogTest`'s own assertions are entry-count-agnostic, so no test
changes were needed; the growth is exercised by the existing "not empty, every entry has both strings, no
duplicate resource ids" checks automatically.

### D-192 also - a "Learn more" hint on the feature-overview settings entry
Requested alongside the catalog refresh: the entry itself gave no visual cue that tapping it opens something
worth reading. A plain XML `android:summary` string cannot carry a partial colour span on its own, so - like
`info_version`'s own already-established pattern of setting a preference's summary programmatically -
`SettingsActivity.onCreatePreferences()` now builds a `SpannableString` (base `d89_summary` text + a new
`d89_learn_more` string on its own line) and colours only the added line via `ForegroundColorSpan` over a
new `R.color.link_text` (`#1565C0` - the same blue already used for `suggestion_verbatim_text` /
`ic_launcher_background`, reused under its own semantic name rather than repurposing an unrelated one).

### D-191 summary trimmed
The `d191_contacts_suggestions_summary` string's "that would be redundant with your address book" clause -
originally written as much to explain the design *to the assistant working on it* as to the end user - was
struck from all three locales per direct feedback that it cluttered the paragraph without adding anything a
user needs. `AdaptSettings.contactsSuggestionsEnabled`'s own KDoc keeps the fuller reasoning; that is
developer-facing documentation, not the user-facing string, and was correctly left alone.

No new tests beyond the string/resource changes already covered by `FeatureCatalogTest`'s existing
assertions. 741 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

**Device-confirmed (2026-07-19): works as intended, closed.**

## §124 - D-193: Diagnostic Logging for the Still-Never-Working Key Vibration (v0.8.88)

Asked directly whether to revisit D-06/D-34/D-66/D-75 (key-press vibration - optional, default off, never
once confirmed working across three separate device-feedback rounds and three different fix hypotheses:
the VIBRATE permission, migrating `Vibrator` to `VibratorManager`, and finally requesting
`VibrationAttributes.USAGE_TOUCH`). Per this project's own rule of re-questioning a diagnosis after
"already fixed" negative feedback rather than patching the same approach again: a fourth blind hypothesis
was rejected in favour of diagnostics first, since none of the three prior rounds were ever backed by an
actual device log of what happens at runtime.

**Root gap found by re-reading the code, not guessed**: `playKeyFeedback()`'s entire vibration path
(`AdaptKeyboardView.kt`) was already wrapped in a `runCatching { }` (D-66's own defensive guard against a
`SecurityException`/vendor failure taking down key handling) - but the block silently discarded whatever it
caught, with zero logging. If `vibrate()` has been throwing this whole time, D-34/D-66/D-75 could never
have found that out from any log, real device or otherwise - the exception itself was invisible by
construction.

**Fixed** (diagnostics only, no behavioural change): `hasVibrator()` is now checked and logged *before*
entering the `runCatching` (previously silently absorbed into "nothing happened" if false); the
`runCatching` block itself now logs which `VibrationAttributes` path fired (`USAGE_TOUCH` on API 33+, plain
`vibrate()` below that) on success, and - the actual gap - `.onFailure` now logs the exception type and
message instead of discarding it. New `AdaptKeyboardView.logHaptics()` mirrors `AdaptKeyService.diag()`'s
own dual-output shape (logcat `AdaptKeyHaptics` tag + the in-app rolling `DiagnosticLog`, Settings ->
Diagnostics, no PC/USB tether needed) but skips that function's password-field guard - haptics fire
identically regardless of field kind, and no message here ever carries typed content.

No new tests - Android `Vibrator`/logging glue, the same established untested gap as the rest of D-06/D-139.
741 unit tests total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Needs a real device
repro next: enable the D-139/D-110 diagnostic log (Settings) and the D-06 vibration toggle, type a few keys,
then share the log - whichever of the three logged outcomes actually shows up finally answers whether this
is a code bug or an OS/OEM-level vibration-intensity restriction outside the app's control.

**Resolved (2026-07-19, device log + confirmation)**: the repro log showed `vibrate() called with
USAGE_TOUCH` on every single keystroke, with no `hasVibrator()` failure and no exception ever logged -
proving the app's own code path was correct all along (right API, right attributes, no silent failure to
find). Confirmed by the user: the actual cause was a central *system* setting on the device (outside the
app's control), not a code defect - exactly the outcome D-75 had already flagged as the leading suspect,
now confirmed rather than assumed. **D-06/D-34/D-66/D-75/D-193 are all closed.** The D-193 diagnostic
logging (`AdaptKeyboardView.logHaptics()`) is left in place rather than stripped out immediately - low-risk,
already proven useful for exactly this class of "silently does nothing" report, and removing it is a purely
mechanical follow-up if it is ever judged no longer worth keeping.

## §125 - D-194: Typing/Backspace-Hold Sluggishness Traced to Three Unthrottled Per-Keystroke Lookups (v0.8.89)

Reported: typing and held-Backspace both feel sluggish, worsening with the length of the still-uncommitted
composing token - most noticeably for a long, deliberately non-word letter sequence that never resolves to a
known word. Also volunteered as the likely real explanation for D-184 (§115/§116, "flash" highlight tuning
that had "no visible effect" on device) rather than a separate bug.

**Root cause, traced from code, not guessed** - three independent gaps, none covered by D-160's (§103)
existing per-keystroke debounce, which only ever covered `refreshSuggestions()`'s own *expensive*-fallback
tier (D-116/D-117/D-131):

1. **`updateComposing()`'s S-05/§47 split-preview/highlight colouring** called `TokenRepair.trySplit()` /
   `shouldHighlightComposing()` synchronously on every keystroke, never debounced at all. `trySplit()` only
   short-circuits when the *whole* token is already a known word - never true for a garbled sequence - so it
   ran a full scan over every possible split point (`O(token length)`), each doing multiple `isKnownWord()` /
   `isBlacklisted()` / `frequencyOf()` / `bigramFrequency()` store round-trips. Directly explains "gets worse
   with length, worst for text that never becomes a real word".
2. **`DictionarySuggestionProvider.suggestionsFor()`'s base `fuzzyNeighbours()` tier (D-12)** ran an
   uncapped `EditDistance.weightedDistance()` (a full Levenshtein DP, no early cutoff) against every
   candidate in its store bucket, on every keystroke from token length 3 up - and since the SQL bucket query
   bounds candidate length to within one character of the token's own length, the DP cost itself scaled with
   the token length too.
3. **Backspace-hold's own `duringRepeat` gate (D-138/§101, D-153/§103) had a gap**: `refreshSuggestions()`
   still called `provider.autocorrectFor()` unconditionally for the pending-autocorrect preview chip even
   with `duringRepeat = true`, despite the surrounding KDoc explicitly describing this whole branch as
   existing to skip exactly this class of expensive per-tick lookup - missed when that gate was introduced,
   never covered by D-138's or D-153's own additions.

Design discussed with the user before implementing (three named trade-offs: debounce the colouring like
D-160 already does for suggestions vs. a hard token-length cutoff vs. a banded/early-exit edit distance).
**Decided**: debounce the colouring (a hard length cutoff was rejected - unhyphenated three-part compounds
get long fast and should keep working); band the edit distance regardless of debounce, since it is a pure
optimisation with no behavioural change either way. **D-195** (a fourth idea, raised and shelved, not
implemented): caching which composing-token prefixes are already confirmed compound-component candidates -
covering both `TokenRepair.trySplit()` (the A-05/§47 colouring mechanism this round touched) and
`DictionarySuggestionProvider.compoundCandidate()`/`CompoundSplit` (D-116's actual noun-first-part
recognition, which the user pointed out is the closer match for "compound component" than `trySplit()`'s more
general missed-space/typo logic) - so a stable left half is never re-derived letter-by-letter as the token
grows. Two reasons it was shelved rather than implemented: (1) it introduces new per-composing-token mutable
state with its own invalidation rules (forward typing vs. backspace vs. D-62 mid-word reclaim), exactly the
kind of correctness-sensitive addition this project's own §99-§101 guardrail exists for; (2) the user's own
follow-up objection, which holds up under scrutiny - as more characters are typed, the *actual* best split
boundary can turn out to sit one or two characters later than an earlier position that had already looked
like a confirmed valid left half (a shorter valid word/prefix later superseded by a longer, better one, or by
D-116's own Fugenelement/rest-correction logic settling on a different split once more of the rest is known).
A naive "confirmed = cached forever" scheme would risk locking in that earlier, inferior boundary instead of
re-ranking against a fresher alternative once one appears - not merely an implementation detail to patch
around, but a real tension with the whole idea of caching a "confirmed" boundary at all. Captured for its own
round if it comes up again, not implemented now; A (§125's debounce) and C (§125's banding) are considered
sufficient for now.

**Fixed**:

- `EditDistance.weightedDistance()` gained an optional `maxCost` parameter implementing Ukkonen's banded edit
  distance: a cell more than `maxCost / indelCost` off the diagonal cannot possibly resolve to a distance
  `<= maxCost` (every `substitutionCost` in this codebase returns `>= 0`, never negative), so those cells are
  never evaluated at all - `O(a.length * b.length)` becomes `O(max(a.length, b.length) * band)`, a
  constant-width band instead of one that grows with the strings. Every call site
  (`DictionarySuggestionProvider.correctionCost()` and its three callers - `isCloseMatch`/`bestCorrection`/
  `wideFuzzyNeighbours`) now threads its own real comparison ceiling straight through as `maxCost`, so a
  candidate that would have qualified still does - the banding only ever discards work that could never have
  produced a qualifying result. Omitting `maxCost` keeps the exact, unbounded distance (used nowhere in this
  app now, but kept as the documented no-band default).
- New `ComposingPreview` cache (`composingPreviewFor` / `composingPreview`) mirrors D-160's own
  `expensiveSuggestionToken` shape: `updateComposing()` now reads the *cached* split/highlight result for the
  exact current text instead of computing it inline, and (outside `duringRepeat`) calls the new
  `scheduleComposingPreviewRefresh()`, which debounces a real recompute (`composingPreviewRunnable`) by the
  same `EXPENSIVE_SUGGESTION_DELAY_MS` (200ms) D-160 already established. During continuous typing the cache
  key never matches the just-changed text, so the token renders uncoloured until typing actually pauses -
  the trade-off the user explicitly accepted ("looking elsewhere while typing fast, won't notice a delayed
  colour"). `clearComposing()` cancels/resets the cache alongside the rest of the per-token state, though
  this is a hygiene measure, not a correctness requirement - a stale cache entry can never apply to the
  wrong, current token, since every read compares against the live `composing` text first.
- `refreshSuggestions()`'s `duringRepeat` branch now returns `pending = null` instead of calling
  `provider.autocorrectFor()` - closing the D-138/D-153 gap directly, with no design trade-off (the pending
  chip was already established as unreadable mid-repeat for every other lookup this branch skips).

5 new tests (`EditDistanceTest`: banded-vs-unbounded agreement within budget, correct "exceeds maxCost"
signalling when the band is too narrow, a length-difference-wider-than-band case, and a longer-token
regression case shaped like the reported scenario). `TokenRepair`/`DictionarySuggestionProvider`'s existing
tests pass unchanged (banding is transparent to every caller that already only compares against a fixed
ceiling). `AdaptKeyService`'s own changes are untouched-by-tests Android/`InputConnection` glue, the
established gap for this class. 746 unit tests total (741 + 5). `:app:assembleDebug`/`:app:testDebugUnitTest`
green. Not yet device-confirmed - awaits a real typing/backspace-hold session, ideally including a long
garbled non-word token (the reported worst case) and a genuine long compound (to confirm split-preview
colouring still eventually appears, just debounced). D-184 (§115/§116) stays open pending the same session -
if the flash issue was actually a symptom of this main-thread saturation rather than the animation constant,
it should improve alongside typing responsiveness rather than needing its own further change.

## §126 - D-196/D-197/D-198: Three More Root-Caused Fixes; D-167/D-199/B-03 Captured as Design Discussions (v0.8.90)

A batch of seven items from one device-feedback round. Three were traced to real, code-confirmed root causes
and fixed; three are new-mechanism/weighting decisions with genuine trade-offs, presented as design
discussions per this project's own convention rather than implemented speculatively.

### Fixed

**D-196 - suggestion-chip case mismatch** (reported before, never actually tracked/fixed - "ist wohl
runtergefallen"). Root cause traced (not guessed): `SuggestionController.displayed()`'s ordinary
(`Kind.NORMAL`) entries show the dictionary's raw stored canonical case, never run through
`CapitalisationEngine` - while `onSuggestionClicked()` (the commit path) already recomputes the full §6
hierarchy via `capitalisation.capitalise(item.word, contextFor(...))` before actually inserting it. The
S-06 pending-autocorrect chip (D-111/D-112) already got this treatment; the ordinary candidates list never
did - a sibling case patched, this one silently wasn't. Concretely reproducible from code alone: typing
`Bahn-h` and tapping the bar's `Haus` (dictionary-stored capitalised, a pure noun) commits `haus`
(B-02: lowercase after a hyphen unless a proper noun) - chip and document disagree. Fixed by capitalising
every `Kind.NORMAL` item's *display text* (never its `.word`, which the controller's own S-02/S-03
identity/dedup logic still needs raw and untouched) in `AdaptKeyService.showSuggestions()`, using the exact
same `capitalisation.capitalise(item.word, contextFor(composing.toString()))` call the commit path already
uses - so display and commit can no longer diverge, by construction, not by re-deriving the rule twice.
`SuggestionController` itself stays untouched (still free of any Android/capitalisation dependency, still
independently JVM-tested). Re-running the already-capitalised S-06 replacement through the same call is a
safe no-op (`capitalise()` is a pure function of word + context).

**D-197 - "Gruße" never restored to "Grüße"; wrong suggestion "Große" instead.** Root cause traced by hand
from the actual fold rules, cost constants, and bundled dictionary data (not guessed): `Umlaut.fold("Gruße")`
and `Umlaut.fold("Grüße")` both produce `"grusse"` - an exact match, cost 0 - so the fold/cost math itself
already favours "Grüße" correctly. The real bug: `DictionaryStore.correctionCandidates()`'s per-bucket SQL
query is frequency-truncated (`CANDIDATE_LIMIT`/bucket-count, D-38/D-65) to keep the *weighted edit-distance*
search affordable on the per-keystroke hot path (§125) - but `diacriticRestoration()` reuses that same
truncated query for an entirely different kind of check (an exact fold-equality comparison, not a DP), so a
rare-but-correctly-spelled word ("Grüße", frequency 18) gets silently crowded out of the candidate set by
hundreds of more common same-bucket words before the comparison ever runs, while "Große" (frequency 11204)
survives the cut and wins by elimination, not by genuinely lower edit cost. Fixed with a new
`DictionaryStore.diacriticCandidates()` (default delegates to `correctionCandidates`, harmless for the
already-unbounded in-memory store; the SQLite store overrides it with the identical indexed bucket query but
no per-bucket cap at all - safe because the per-candidate check afterward is a cheap string comparison, not
an edit-distance DP, so completeness costs almost nothing here unlike the case §125 was about).
`diacriticRestoration()` now calls it instead of `correctionCandidates()`. 1 new test
(`SqliteDictionaryStoreRoboTest`: confirms `diacriticCandidates()` returns a candidate `correctionCandidates()`
truncates away under an artificially narrowed per-bucket limit).

**D-198 - email keyboard's period-key popup pre-selects `.net` instead of the locale's own `.de`.**
Root cause: plausible and well-evidenced from code, not pixel-confirmed on a real device. The period key sits
in the same position (second-from-last, right before Enter) in both URL mode (3-entry `.com`/ccTLD/`.org`
popup) and email mode (4-entry `.com`/ccTLD/`.net`/`.org`, D-158) - email's extra `.net` entry makes its popup
row measurably wider, and `HorizontalLongPressPopup.rowLeft()` clamps the row to stay on-screen when centring
it over a key that close to the right edge would run past the edge, which shifts the *whole* row - including
which cell actually sits above the key - away from `preSelectedIndexFor()`'s own logical pre-selection. Since
`ACTION_MOVE` was previously re-deriving the popup selection from the raw pointer x on *every* move event with
no threshold at all, even the incidental sub-pixel jitter real touch input delivers during an ostensibly
stationary hold could flip the selection to whatever cell the clamped layout put under the finger instead -
matching the report exactly ("`.net` ist hier etwas mehr über der Basis-Taste"). Fixed generally rather than
patched to this one popup: `ACTION_MOVE` now only calls `updatePopupSelection()` once the finger has moved
beyond the system touch slop from the original `ACTION_DOWN` (mirrors `movedBeyondSlop()`, already established
for the identical D-108 long-press-smear tolerance a few lines below) - so a genuinely stationary hold now
reliably keeps whichever cell was logically pre-selected, regardless of whether the row was clamped, for every
long-press popup in the app, not only this one. No new tests (Android touch-event glue, established gap).

3 new tests total across the three fixes. 747 unit tests total (746 + 1, the diacritic-candidates regression
- the other two fixes touch only untested Android glue). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
None of the three yet device-confirmed.

### Captured as design discussions (not implemented)

**D-167** (open since §102/§105, spec-captured design question, now revisited): an embedded mid-word capital
(`"diecVorschläge"`) should raise the A-05 split confidence enough to apply far more often already while
typing/right after the delimiter, not needing a later manual drag-to-split. `TokenRepair.trySplit()` currently
requires both halves to be known, non-blacklisted words **and** a real bigram co-occurrence
(`MIN_SPLIT_BIGRAM`, §45's guard against "any two known fragments get cut apart" false positives). Two
directions discussed, not decided: (A) keep the bigram gate exactly as-is, but give a split whose right half
starts with an embedded capital a large score bonus so it wins ranking against competing candidates - safe,
conservative, but still blocked whenever the bigram itself is rare/unseen; (B) relax or bypass
`MIN_SPLIT_BIGRAM` specifically for this case, mirroring D-122's own precedent (a narrow, strongly-signalled
exception is safe where a blanket relaxation reopens §45's original problem) - likely narrowed further to
"right half is a plausible noun" (reusing D-116's `isKnownNoun` concept) rather than merely capitalised, since
an embedded capital alone can also just be a Shift mis-hit with no real word boundary intended. Awaiting the
user's choice between (A)/(B)/a combination before implementing.

**D-199** (strategy proposal, explicitly requested, not implemented): autocorrect silently reapplying a
correction the user just explicitly rejected via A-07 undo, e.g. `"Autp."` -> `"Auto."`, Backspace (A-07
reverts to `"Autp."`), Backspace again (deletes the period), Space (`"Autp"` -> `"Auto"` again, unchanged).
Traced end to end: `performAutocorrectUndo()` fully restores the raw typed text and delimiter, reverses the
dictionary/bigram learning and any raw-coordinate touch training, and calls `clearUndo()` immediately - by the
time the user retypes the delimiter, `finalizeAndCommit()` runs the ordinary pipeline completely fresh, with
zero memory that this exact `(typed, corrected)` pair was just rejected. No existing suppression/cooldown
mechanism was found anywhere in the codebase (checked: A-04/D-177's word-level blacklist-on-recurrence is a
different, opposite-direction mechanism - promoting a *repeated* word to blacklisted, not suppressing a
*just-rejected* correction once). Proposed strategy: extend the existing single-slot A-07 undo state (already
"armed for one keystroke" by construction, matching this class's established pattern) instead of clearing it
immediately on undo - keep `(undoTyped, undoCommitted)` around as a one-shot suppression that
`finalizeAndCommit()`'s autocorrect-selection step checks first; a matching `(typed, corrected)` pair commits
verbatim once instead of re-applying, then the slot clears for real. Scoped to expire the moment *any other*
word gets committed (not a time constant), so a later, unrelated encounter with the same typo on some other
day still autocorrects normally - a narrow, session-scoped exception, not a systemic weakening of autocorrect
confidence. Awaiting go before implementing (touches the same commit hot path §99-§101's guardrail covers).

**B-03 / D-200** (spec-documented design intent since the original spec, never implemented; revisited with
two concrete strategies from the user, e.g. `"E-Mail-Adresse"`). Researched first (not guessed): a hyphen is
already an ordinary delimiter (B-01) - bigram learning and D-43 next-word predictions already fire identically
after a hyphen-committed fragment as after a space, no code treats them differently. So neither strategy needs
new *recording* infrastructure; both are about what the *suggestion* side does with data already being
collected. **Strategy 1** (chain next-word predictions across hyphen boundaries into one multi-part suggestion,
"auch drei- oder vierteilig"): open questions - how many bigram hops to chase and what confidence/count
threshold stops the chain at each hop; how a multi-part chip ranks against an ordinary single-word completion
in the same bar slot; UI/commit mechanics for a chip that types several hyphen-joined parts at once. **Strategy
2** (learn a whole hyphenated combination as one dictionary unit after repeated confirmation - this is exactly
B-03) - the user's own point: it needs strategy 1's per-hop tracking as its trigger signal ("muss es aber wie
in Vorschlag 1 zunächst als Verbindung gezählt werden"), i.e. it generalises D-37's "repeated typing promotes
a candidate to known" and D-110's "learned words live in their own store, separate from the bundled
dictionary" precedents from single words to hyphen-chains, rather than being an independent mechanism. Neither
implemented; recommended starting point (not yet agreed) is Strategy 1 first, since it needs no new storage,
with Strategy 2 as a natural follow-up once the chain-confidence signal it depends on is proven reliable.
Awaiting the user's direction on hop depth / promotion threshold / UI mechanics before implementing either.

## §127 - D-201 Fixed: Doubled Space Applying a Mid-Word Connector-Split Chip; D-202 Captured (v0.8.91)

**D-201 - fixed.** Reported repro: `"dervKinderarzt "` committed as one unsplit blob (D-167's own open gap -
the embedded-capital signal that would raise split confidence enough to auto-split at commit time doesn't
exist yet); re-editing mid-word and tapping the resulting `"der Kinderarzt"` D-122 connector-split chip
doubled the space that was already there. Root cause: exactly the gap §117/D-183 had already named but never
reproduced ("`applyMidWordSplitSuggestion()`/`applySplit()` ... have the same class of gap but were not
touched - never reproduced, different code path"), now reproduced. `onSuggestionClicked()`'s `Kind.NORMAL`
branch already computes the correct D-144/D-183 "is there already whitespace right after the (possibly
mid-word) composing token" check for the ordinary single-word path, but returned early into
`applyMidWordSplitSuggestion()` *before* that check for the D-122 multi-word candidate, which then called
`applySplit()` with a hardcoded `" "` delimiter unconditionally. Fixed by hoisting the existing check above
the branch split (identical for both branches, depends only on `composing`/`composingCursor`/the document,
never on `item.word`) and threading the computed delimiter (`" "` or `""`) into `applyMidWordSplitSuggestion()`
-> `applySplit()`; `pendingSuggestionSpace` (D-29) now also only arms when a space was actually added, mirroring
the ordinary branch. No new tests (`AdaptKeyService` `InputConnection` glue, established gap). 747 unit tests
total (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

**D-202 - captured as a design discussion, not implemented.** Proposal: a token suspected of being an
incorrectly-unsplit compound should need more repeated, uncorrected typing before D-37 promotes it from
`PendingLearnStore` into the real dictionary - 4 occurrences instead of the ordinary `LEARN_THRESHOLD = 2` -
so that repeatedly mistyping `"dervKinderarzt"` does not get it learned as if it were a genuine single word
before the user notices and starts fixing it. Investigated first (not guessed): the counter-decrement half of
the proposal ("ein Zurücknehmen muss den Counter zurückzählen") **already exists** and needs no new code -
`unlearnWord()`'s `PENDING` branch already calls `PendingLearnStore.decrement()` on every A-07 undo
(`AdaptKeyService.kt:3453`), so a rejected occurrence already counts back down, not merely resets. What is
missing: `LEARN_THRESHOLD` (`AdaptKeyService.kt:4001`) is a single fixed constant, consulted at exactly one
site (`learnWord()`'s `PendingLearnStore.increment(...) >= LEARN_THRESHOLD` check) - turning it into a
per-word decision evaluated fresh at that same call site, every time, is architecturally small (matches the
user's own "Live-Prüfung ... ermöglicht, dass es nachträglich durch Code-Updates anders klassifiziert werden
kann" requirement for free, simply by never persisting the classification anywhere). **The open question is
the detection heuristic itself** - what counts as "offensichtlich oder auch nur vermutlich zusammengesetzt":
(a) reuse `DictionarySuggestionProvider.compoundCandidate()`/`CompoundSplit.split()` as-is (D-116's own
existing "plausible compound" recognition - noun first part + Fugenelement + resolvable rest - the closest
already-existing match to "compound" in this codebase); (b) the D-167 embedded-mid-word-capital signal once
(if) that lands; (c) some OR-combination of both, matching the "offensichtlich [strict] oder vermutlich
[loose]" wording more literally. Scope also needs confirming: the proposal as read only touches the
not-yet-learned `PendingLearnStore` counting-up path, not the unconditional reinforcement of an
already-learned word (`learnWord()`'s `provider.isKnownWord(word)` branch has no threshold at all today) -
assumed out of scope unless the user says otherwise. Awaiting the user's choice of heuristic (and confirmation
of scope) before implementing.

## §128 - D-202 Implemented: Higher Learn Threshold for a Suspected Unsplit Compound (v0.8.92)

Follow-up to §127's captured design discussion. User's decision: heuristic (a) (D-116's own
`compoundCandidate()`/`CompoundSplit` recognition) is sufficient on its own - explicitly accepting that a
false positive only delays an ordinary word's promotion by a couple more repetitions, no real harm, traded
for fewer incorrectly glued-together compounds ending up learned. Heuristic (b) (an embedded mid-word
capital) may run first as a cheap pre-check, since it is a near-certain signal and, unlike (a), needs no
dictionary lookup at all - confirmed cheap (`word.drop(1).any { it.isUpperCase() }`, a plain string scan).
Scope confirmed as originally proposed: only the not-yet-learned `PendingLearnStore` counting-up path: an
already-learned word's unconditional reinforcement is untouched.

**Implemented**: new `SuggestionProvider.looksLikeUnsplitCompound(word)` (default `false` for
`StubSuggestionProvider`; `DictionarySuggestionProvider` implements it by reusing the existing
`compoundCandidate()` unchanged, context-free (`previousWord = null`) since this only feeds a boolean
learning-throttle decision, not a ranked suggestion). `AdaptKeyService` gained
`hasEmbeddedCapital(word)` (the cheap pre-check) and `learnThresholdFor(word)`, consulted at the one D-37
promotion site (`PendingLearnStore.increment(...) >= learnThresholdFor(word)`, replacing the previously
hard-coded `LEARN_THRESHOLD`): `hasEmbeddedCapital(word) || provider.looksLikeUnsplitCompound(word)` selects
the new `COMPOUND_LEARN_THRESHOLD = 4` over the ordinary `LEARN_THRESHOLD = 2`. Both checks are re-run fresh
on every promotion attempt, nothing classified is ever cached/persisted - a later change to either signal
reclassifies an already-pending word retroactively with no data migration, exactly as requested. The
counter-decrement-on-undo half needed no change at all: `PendingLearnStore.decrement()` was already wired
into `unlearnWord()`'s `PENDING` branch (A-07 undo), so a rejected occurrence already counts back down under
the new threshold too, unchanged. Explicitly independent of D-167 (still undecided, live split-*confidence*
while typing) - this only throttles learning, never auto-splits or suggests anything by itself.

3 new tests (`DictionarySuggestionProviderTest`: `looksLikeUnsplitCompound` true for a reconstructable
compound, false when no reconstruction exists, false for an already-known word). `hasEmbeddedCapital`/
`learnThresholdFor` themselves are untested `AdaptKeyService` glue (established gap - trivial pure logic, but
private members of an otherwise-untested class). 750 unit tests total (747 + 3). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. Not yet device-confirmed - needs a real repeated-mistyped-compound session to
see the higher threshold actually delay promotion as intended.

## §129 - D-203: The MIN_SPLIT_BIGRAM Gate Replaced After a Live Design Discussion (v0.8.93)

Grew out of D-167 (§126/§127/§128's own captured discussion, "diecVorschläge"/"dervKinderarzt" not
auto-splitting): asked directly why a deliberate mid-word re-edit ([TokenRepair.splitAtUnresolvedConnector],
D-122) finds "der" + "Kinderarzt" but ordinary commit-time typing ([TokenRepair.trySplit]) does not, on the
user's own suspicion that the reason "might not be what I assumed" - **confirmed against the bundled corpus,
not guessed**: `bigram_de.tsv` has zero recorded co-occurrences for `"der"`+`"kinderarzt"`, even though
`"Kinderarzt"` is a perfectly good known noun (frequency 14). `trySplit()`'s `MIN_SPLIT_BIGRAM` gate required
exactly this prior co-occurrence; `splitAtUnresolvedConnector()` never did. This reframed the whole
discussion: the real bottleneck was never about split *confidence* or an embedded-capital signal at all - a
first-time-typed compound typo has, by definition, never been recorded as two separately co-occurring words,
so `MIN_SPLIT_BIGRAM` rejected exactly the class of case this mechanism exists to catch. D-167's own
embedded-capital idea is explicitly kept in reserve for later, not abandoned.

**Redesigned from a live brainstorm, evaluated against real data before implementing** (§45's own motivating
case, `"meinst"` -> `"mei"` + `"st"`, used throughout as the regression test): four candidate replacement
mechanisms were discussed (frequency floor per half; a stricter minimum length - rejected outright, user's
own words: "wirklich schädlich"; grammatical pair-plausibility; demoting bigram from a gate to a scoring
signal only). Two calibration findings surfaced *during* implementation, checked against the bundled
`dict_de.tsv` rather than assumed correct on first principles:

- **Frequency floor alone cannot separate the motivating example**: "Mei" (16) and "St" (5939) are not
  meaningfully rarer than "Kinderarzt" (14) - the bundled dictionary is already pre-filtered at its own
  frequency floor of 8, so there is no genuinely "obscure" tier left within it to filter by frequency alone.
  Implemented anyway as a light, narrow trim of the bottom tier ([MIN_SPLIT_HALF_FREQUENCY] = 10) - real
  value, just a smaller one than initially hoped, with the pair-plausibility rule below doing the actual work
  for this case.
- **A naive "one half must be a noun" rule would have broken already-correct behaviour**: this project's own
  existing test suite documents `"und"` + `"das"`, `"aber"` + `"das"` (conjunction/pronoun pairs, tagged
  `OTHER`, no noun on either side) as valid, already-verified splits - a "must contain a noun" gate would have
  wrongly rejected them. Refined instead to reject only when **both** halves independently resolve to a noun
  (`isNoun` in the new code) - "Mei" (tagged `NOUN`+`OTHER`) and "St" (tagged `NOUN`) are both nouns and get
  rejected, while an article/pronoun/conjunction (`OTHER`) followed by a noun - an entirely ordinary German
  phrase shape - passes untouched. Grounded in the dictionary's real (coarse, 6-tag) `PartOfSpeech` set, not
  an invented finer-grained scheme it does not actually have.

**Implemented** (`TokenRepair.kt`, shared by both `trySplit()` and `splitAtUnresolvedConnector()` via the
common `candidateAt()`/`isAlreadyRecognised()` gates - so both benefit from every change, not just the
commit-time path):

1. A token that is a known word **or a plausible regular-verb inflection of one**
   (`RegularVerbInflection.isPlausibleInflection`, already trusted elsewhere for exactly this - D-115/D-125)
   is never split at all. Closes the historical "meinst" bug at its actual source, the same way A-01 already
   protects a literal dictionary word - `RegularVerbInflection` was never previously consulted by `TokenRepair`
   at all.
2. Each half must individually clear `MIN_SPLIT_HALF_FREQUENCY` (10), not merely exist.
3. A pair where both halves resolve to a noun (`PartOfSpeech.NOUN`/`PROPER_NOUN`) is rejected.
4. `MIN_SPLIT_BIGRAM` co-occurrence is **no longer a gate** - removed entirely; bigram frequency still feeds
   `score()`'s existing ranking exactly as before, so a candidate *with* co-occurrence evidence still wins
   over one without, but a plausible novel pairing is no longer rejected outright for lacking prior evidence.

Additionally, connected to the umlaut/ß-fold strategy per direct instruction: candidate half resolution
(`resolveWord()`) now tries `Umlaut.unfoldCandidates()` before giving up, so a half typed without its
diacritic (e.g. "uber") still resolves via its real spelling ("über") for eligibility/frequency/noun-pair
purposes - matching this project's founding "umlauts are ordinary characters" principle, previously never
applied inside `TokenRepair` at all. Scoped deliberately narrow: `SplitResult` still carries the literal
*typed* substrings, not the diacritic-restored form, so §47's span-colouring (which maps directly onto the
still-displayed composing text's exact characters) stays correct; restoring the diacritic in the actually
*committed* text as well would be a separate, further feature, not implemented this round.

6 new tests (`TokenRepairTest`, 23 -> 29: the inflection guard, the both-nouns rejection isolated from a
function-word-plus-noun pass, the frequency floor, umlaut-fold resolution, and two D-122 tests distinguishing
its now-narrower-but-still-real difference from `trySplit` - strategy scope, not evidence requirements, since
neither needs bigram co-occurrence any more). Every pre-existing test that depended on the old bigram gate was
rewritten to reflect the new behaviour, not merely patched to keep passing. 756 unit tests total (750 + 6).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - needs a real
"dervKinderarzt"-shaped repro to confirm the auto-split now actually fires at commit time. D-167's own
embedded-capital confidence idea remains captured, unimplemented, for a possible future round.

## §130 - D-107 Closed: S-05 Highlight Semantics Confirmed as Settled, No Reversal (v0.8.93, no code change)

D-107 (§62, "Recognised-Word Highlight (S-05/C-04) Semantics Under Reconsideration") was raised purely as a
flag - the user was weighing whether green should keep meaning "safe, no correction planned" (the S-05
semantics as specified and implemented all along) or flip to the opposite, "about to be auto-corrected". No
implementation was ever attempted either way; the spec's own S-05 section carried an explicit "not decided"
callout the entire time.

**User confirmed directly: the current behaviour - green means safe, no correction planned - works as
intended.** The reversal is not adopted. D-107 is closed; the spec's open-design-question callout is removed
in favour of a short confirmed-settled note. No code change, no new tests (nothing was ever built for the
alternative reading to begin with). 756 unit tests (unchanged).

## §131 - D-204: A Second, App-Specific ß Fold Convention - "gruse" for "Grüße" (v0.8.94)

Reported: `"vieie gruse"` never becomes `"viele Grüße"`; separately, `"viele Gruße"` (ß typed, no umlaut)
restores correctly at commit but the suggestion bar shows `"große"`/`"größe"` far ahead of `"Grüße"`.

**First-pass diagnosis (superseded, kept here for the trail): frequency floor + ranking.** Confirmed against
the real bundled data (not guessed) that `"Grüße"` has frequency 18 in `dict_de.tsv`, against `"große"`
(11204) and `"Größe"` (2769) - and that `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY = 300L` (D-114,
`AdaptKeyService.kt`) unconditionally drops any correction candidate below it, regardless of edit cost. For
`"gruse"` specifically, this looked like the root cause: the only edit-cost-eligible candidate (`"Grüße"`, a
single indel away under the existing ß→"ss" fold) was being vetoed by the frequency floor before ranking ever
mattered.

**User correction, and the actual root cause.** The user pointed out this diagnosis modelled the wrong typing
convention: `"gruse"` is not a two-edit typo of `"Grüße"` at all - it is a **lazy long-press omission**. This
app hosts `ß` as the long-press alternative directly on the `s` key (mirroring how `ä`/`ö`/`ü` already sit on
`a`/`o`/`u`), so a bare `s` tap is a direct, position-preserving stand-in for `ß`, exactly as a bare `u` tap
already is for `ü`. Checked character-by-character: `"gruse"` (g-r-u-s-e) and `"Grüße"` (G-r-ü-ß-e) line up
1:1 - no missing/extra character at all. This is an **exact** diacritic match, not a fuzzy 2-edit one, and
should never have reached the frequency-floored `bestCorrection()` path in the first place.

The mismatch: `Umlaut.fold()` maps `ß` → `"ss"` (the formal orthographic/Swiss substitution - a real,
independently tested convention: `"straße"`/`"strasse"`, `"ruß"`/`"russ"`), a *different* character count
than the app's own long-press-hosted `s`. Both conventions are legitimate and must coexist without either
crowding out the other.

**Fixed** (`Umlaut.kt`): new `foldToHostKey()` (identical to `fold()` except `ß` → single `s`) and
`foldVariants()` (both fold results, deduplicated - a plain word with no `ß` has exactly one variant).
`DictionarySuggestionProvider.diacriticRestoration()` now accepts either variant on the candidate side
(`Umlaut.foldVariants(lower).contains(folded)` instead of a single `Umlaut.fold(lower) == folded`) - the
existing `"russ"`→`"ruß"` convention is unaffected (regression-tested), `"gruse"`→`"Grüße"` is now an equally
exact match.

**Also fixed, the position-1 requirement**: `AdaptKeyService.refreshSuggestions()`'s `pending` (S-06 preview
chip) computation previously consulted only `provider.autocorrectFor()` - which *does* apply the D-114
frequency floor unconditionally, regardless of edit cost. A comment left from D-111/D-112 assumed a
diacritic-fold case was "already covered" there at cost 0; checked directly and confirmed false - the floor
applies before cost is even considered. `pending` now consults `diacriticRestoration()` first, mirroring
`finalizeAndCommit()`'s own existing `diacriticWord ?: autocorrected ?: ...` precedence, reusing
`SuggestionController`'s existing S-06 mechanism (the pending replacement and its verbatim chip are always
pinned ahead of the frequency-sorted list, exempt from S-03 stabilisation) - no new UI mechanism needed, this
was a wiring gap, not a missing feature.

6 new tests (`UmlautTest` 4, `DictionarySuggestionProviderTest` 2, incl. a `"gruse"`→`"Grüße"` case with the
real corpus frequencies and a regression check that `"russ"`→`"ruß"` still works unchanged). 762 unit tests
total (756 + 6). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

## D-205 - Captured, Not Designed: Ranking Should Weigh Edit/Touch Proximity, Not Just Frequency

Flagged by the user while discussing D-204, explicitly deferred ("dazu später") - not designed, not
implemented. The user's position: for a correction/fuzzy candidate, how close it is to the actual mistake
(keyboard-key distance, and ideally the raw touch-coordinate recording T-02/T-03) should matter more than how
often the candidate word is used overall - the opposite of today's `score()` in
`DictionarySuggestionProvider`, which ranks every fuzzy/prefix candidate purely by frequency (+ bigram bonus),
with no edit-cost or touch-proximity term at all. `bestCorrection()` already ranks cost-first for the
*autocorrect* decision (`compareBy({it.cost}, {-it.score})`) - the open question is whether/how to bring an
equivalent proximity-aware ranking to the general suggestion-bar candidate list (`suggestionsFor()`), which
today does not distinguish an exact-cost-0 candidate from a cost-2 one once both clear their respective gates.
No direction chosen yet - needs its own design conversation before any implementation.

## D-206 - Captured, Not Designed: Archaic/Pre-Reform Spelling Pollution in the Bundled Dictionary

Flagged by the user while discussing D-204, explicitly deferred - not designed, not implemented. The bundled
`dict_de.tsv` is built from a Wikipedia-derived corpus, which the user reports still contains pre-1996-reform
spellings (their example: `"daß"`, the old spelling of `"dass"`) alongside various other archaic forms -
unnecessarily inflating the dictionary, doing nothing for suggestion quality, and actively harmful when such
a form is itself offered as a suggestion (a modern German user typing `"dass"` should never see `"daß"`
proposed back at them). The user wants a plan, not a fix yet, covering at least two candidate directions:
purge such entries from the dictionary build outright, or flag them and rank them far down in suggestions/
autocorrect without removing them entirely (e.g. for the rare case of someone transcribing older text). The
user explicitly noted this overlaps with D-204's own ß/"ss" territory (old-reform spelling conventions and
the `ß`/`ss` fold-variant mechanism just built are closely related) - worth designing together, not in
isolation. No scope, detection heuristic, or mechanism chosen yet.

## §132 - D-206 Designed and Implemented: Archaic-Spelling Relics Blacklisted, Not Purged (v0.8.95)

D-206's own design conversation, grounded in the real bundled data before any direction was chosen (not
guessed): of `dict_de.tsv`'s 1648 `ß`-containing entries, 115 have a co-existing `ß`→`"ss"`-substituted
counterpart already in the dictionary too. Of *those*, 74 run in the direction the user's framing did not
anticipate - the `ß`-form dominates by a wide margin (`"große"` 11204 vs. `"grosse"` 213, `"außerdem"` 7890
vs. `"ausserdem"` 95, `"Straße"` 3782 vs. `"Strasse"` 92) - these are genuinely modern, correctly-spelled
long-vowel `ß` words whose rare `ss`-form is the Swiss-spelling convention, not a reform relic, and must never
be touched. Only 40 run the other way (the `ss`-form dominates - `"daß"` 868 vs. `"dass"` 61892, `"muß"` 149
vs. `"muss"` 6917, `"Fluß"` 14 vs. `"Fluss"` 1803) - the actual signature of a pre-1996-reform relic. Even
within those 40, two are outright coincidental collisions the naive substitution cannot distinguish
(`"Maße"` != `"Masse"`, `"Buße"` != `"Busse"` - genuinely different words, not a spelling pair at all), and
several are proper nouns/surnames/place names (`"Keßler"`, `"Reuß"`, `"Aßmann"`, `"Preßburg"`, `"Nußbaum"`,
`"Elsaß"`, `"Haußmann"`, `"Schloßberg"`/`"Schloßplatz"`) whose own spelling is a legitimate identity choice,
not an error - a blanket mechanical rule over all 40 would have been wrong for a meaningful fraction of it.
**User confirmed, on seeing these numbers, that the scope is far smaller than assumed** and approved the
proposed mechanism directly.

**Mechanism, confirmed by the user over the two candidates discussed**: reuse the existing `A-04` `BUNDLED`
blacklist (the same one seeded for `due`/`sue`/`ddr`/`aks`, D-157/D-172) rather than purging the dictionary
outright or inventing a new "derank" concept - keeps every relic typeable/known (quoting genuinely old text
still works) while it can never surface as its own suggestion again, is fully reversible, and needed no new
storage mechanism at all. A curated 25-word list (`daß`, `muß`, `mußt`, `mußte`, `müßte`, `wußte`, `läßt`,
`laß`, `laßt`, `Einfluß`, `Anschluß`, `Schluß`, `Fluß`, `Prozeß`, `Kongreß`, `Rußland`, `bewußt`,
`Bewußtsein`, `Bewußtseins`, `unbewußten`, `Haß`, `gewiß`, `Kuß`, `bißchen`, `häßlich`) - deliberately
excluding the collisions and the proper nouns/surnames/place names above - is now seeded into
`BUNDLED_GERMAN_BLACKLIST` alongside the pre-existing four entries. The pre-existing `ß`→`"ss"` fold
(`Umlaut.fold`, unrelated to D-204's newer host-key fold) already makes each of these a cost-0 match for its
modern form, so a live typing of one of them can still be silently autocorrected to the modern spelling via
the existing §44 known-word-override ratio, unaffected by the blacklist itself.

**Also addressed, per the user's own follow-up**: the `BlacklistActivity` editor (C-05) previously listed
every blacklisted word regardless of category. The user's own reasoning - a `BUNDLED` entry is rarely of
interest and should rarely be removed at all, "da muss man sich schon sehr sicher sein" - is now reflected
directly: the list defaults to `USER`-category entries only, with a new `blacklist_show_bundled` checkbox
(unchecked by default, all three locales) to reveal `BUNDLED` entries too when genuinely needed. Pure
client-side filtering in `refresh()` (`store.blacklistCategory(it) == BlacklistCategory.USER`) - no new store
API needed, `store.blacklistedWords()`/`blacklistCategory()` already existed.

No new unit tests - both changes are Android-facing (a private `AdaptKeyService` seed constant, an
`Activity`'s view/filter wiring), the established untested-glue gap for this layer; the underlying
`blacklist()`/`isBlacklisted()`/`blacklistCategory()` mechanics were already covered when D-107/D-157 first
built them. 762 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

## §133 - D-205 Designed and Implemented: Suggestion-Bar Ranking Now Discounts by Edit Cost (v0.8.96)

D-205's own design conversation. The user's position, restated for confirmation: for a correction/fuzzy
candidate, closeness to the actual mistake should generally matter more than raw dictionary frequency - the
opposite of `suggestionsFor()`'s existing `score()`, which ranks every fuzzy/prefix candidate purely by
frequency (+ bigram bonus), with no edit-cost term at all. Pointed out that `bestCorrection()` (behind
`autocorrectFor()`/`highConfidenceCorrection()`, the single-target *autocorrect* decision) already ranks
cost-first (`compareBy({it.cost}, {-it.score})`) - the gap is specifically the suggestion bar's own
multi-candidate ranking, `suggestionsFor()`.

**Scope discussion, two levels of ambition laid out**: (1) a self-contained, purely `DictionarySuggestionProvider`
side fix - rank the existing static-keyboard-adjacency edit cost first, frequency as tie-break, mirroring
`bestCorrection()`'s own proven approach; (2) a much bigger step - weigh the *actual recorded touch
coordinates* (T-02) against T-03's personalised per-key offset model, not just a static adjacency map. (2)
would need raw taps threaded into the (deliberately Android/touch-free, JVM-testable) provider or a merge
step in `AdaptKeyService` - a real architectural extension, not a resort. **User confirmed (1) first,
explicitly deferring (2)** - reasoning given directly: this app has fought typing sluggishness multiple times
before (D-153/D-160/D-194/§102/§125) and a heavier per-keystroke computation must not reopen that. (2) stays
captured for its own later round, this time with an explicit performance constraint attached.

**Hard cost-first vs. a softer weighted blend, also discussed**: `bestCorrection()`'s existing rule is a hard
lexicographic sort (cost strictly before frequency, no exception). Asked whether the bar should mirror that
exactly or allow a softer blend where an overwhelmingly more frequent farther candidate can still
occasionally win. **User chose the softer, weighted blend** ("Eine weichere Gewichtung klingt fair").

**Implemented** (`DictionarySuggestionProvider.kt`): `fuzzyNeighbours()`/`wideFuzzyNeighbours()` (D-12/D-117)
now return each candidate's own edit cost alongside it, not just a boolean pass/fail gate (the now-redundant
`isCloseMatch()` helper removed). A new `scoreWithCost()` discounts the existing `score()` multiplicatively
by `FUZZY_COST_DECAY^cost` (`FUZZY_COST_DECAY = 0.01`, a considered starting point calibrated against the
real bundled `dict_de.tsv` frequency range - roughly 8 to 1,000,000): a cost-1 candidate needs ~100x the
frequency of a cost-0 one to outrank it, a cost-2 candidate ~10,000x - both reachable at the corpus's real
extremes, so the preference is genuinely soft, not absolute. A plain prefix completion is unaffected (cost 0
by construction, decay factor 1). `bestCorrection()`/the autocorrect decision itself is untouched - its own
hard cost-first rule stays exactly as conservative as before, unrelated to this change.

2 new tests (`DictionarySuggestionProviderTest`): a closer, far-less-frequent fuzzy candidate outranking a
farther, more-frequent one at realistic corpus proportions, and a second case at the corpus's own frequency
extremes proving the rule is a soft preference (the farther candidate can still win when frequent enough) -
not a hard cost-first rule like `bestCorrection()`'s. 764 unit tests total (762 + 2).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - `FUZZY_COST_DECAY` is a
starting point, easy to retune in isolation without touching any call site. D-205's own touch-proximity
extension (item (2) above) stays captured, unimplemented, for a future round with its performance cost
considered up front.

## §134 - D-207/D-208/D-209: A Real Sluggishness Investigation, Traced to Three Distinct Causes, All Fixed (v0.8.97)

The user reported typing feeling sluggish "again", visible in the flash-highlight effect lagging, and
supplied real in-app diagnostic logs (`AdaptKeyJitter`/`AdaptKeyHaptics`) from several repeated typing
sessions for direct tracing, per this project's own "trace from real data, not a guess" convention.

**D-207 - the commit-time double search.** Precisely measured via the logs' own raw millisecond
timestamps (not the coarser rounded display): committing a longer/harder-to-resolve word costs a
consistent ~400ms right at the delimiter keystroke (`"Kiza"`->`"Liga"`, `"mitnehme"`), exactly where the
key's own flash must also render. Traced in `finalizeAndCommit()`: `highConfidenceCorrection()` and
`autocorrectFor()` were called independently on the *same* `typed` token, each running its own
`store.correctionCandidates()` bucket scan + per-candidate edit-distance computation - `highConfidenceWord`
itself is only ever used to decide whether to veto an A-05 split, never committed. Since
`autocorrectFor()`'s search (`MAX_CORRECTION_COST`) is a strict superset of `highConfidenceCorrection()`'s
tighter one (`ADJACENT_SUB_COST`) and the cost-first ranking (`compareBy({cost},{-score})`) means a cost-1
candidate always wins whichever search finds it, the two answers are always derivable from one search.
Fixed: `bestCorrection()` (private, `DictionarySuggestionProvider`) now returns the winning candidate's own
cost alongside it (was discarded after gating); new `SuggestionProvider.bestCorrectionFor()` (default
method, `Correction(word, highConfidence)`) answers both from a single search, overridden properly in
`DictionarySuggestionProvider`; `finalizeAndCommit()` now calls it once instead of
`highConfidenceCorrection()` + `autocorrectFor()` separately - also now correctly skipped entirely when
`diacriticWord != null` (previously computed anyway and thrown away, since a diacritic restoration already
vetoes the split regardless). `highConfidenceCorrection()`/`autocorrectFor()` themselves are unchanged and
still used elsewhere (e.g. `compoundCandidate()`'s own, genuinely different, `rest`-resolution call) - only
the one redundant call site in `finalizeAndCommit()` is fixed.

**D-208 - live per-keystroke fuzzy search scaling with token length.** A second log, repeating the same
"Glückwunsch"/"übrigens" typing without any suggestion taps this time, showed a *different*, mid-word
pattern: fluid for the first 1-2 characters, then consistently 300-500ms/keystroke for every remaining
character - confirmed by the user as felt and reproducible, worse than a one-off blip. Traced to
`DictionarySuggestionProvider.fuzzyNeighbours()`, called unconditionally on the hot path in
`suggestionsFor()` once the composing token reaches `MIN_FUZZY_LENGTH` (3) - a `store.correctionCandidates()`
bucket scan (up to ~2000 candidates worst case) plus a per-candidate edit-distance computation whose own
cost scales with the token's own, ever-growing length, on *every* keystroke from that point on - unlike
D-116/D-117/D-131, never gated behind D-160's `includeExpensiveFallbacks` debounce at all. Two red herrings
ruled out first, per direct user confirmation: a ~2.8s pause mid-"Glückwunsch" (a deliberate long-press to
reach `ü`, hosted on the `u` key) and an instant multi-character jump completing "Herzlichen" (a
suggestion-bar tap, a different commit path entirely) - both expected behaviour, not bugs.

Fixed: `fuzzyNeighbours()`'s own loop in `suggestionsFor()` moved behind `includeExpensiveFallbacks`, like
D-116/D-117 - but deliberately *not* also gated on `candidates.isEmpty()` like those, since D-12's own basic
behaviour ("mut" must still surface "mit" even though "mut" itself has its own prefix completions) would
otherwise silently regress. `AdaptKeyService.refreshSuggestions()`'s own deferred-pass scheduling
correspondingly drops its `candidates.isEmpty()` condition too - now always schedules the debounced pass
when the hot path ran without expensive fallbacks, so fuzzy matching gets its own chance once typing
settles for `EXPENSIVE_SUGGESTION_DELAY_MS` (200ms), regardless of whether prefix completion already found
something. During a fluent, uninterrupted typing burst the deferred pass keeps getting cancelled and
rescheduled (never actually running); at any natural pause it delivers - the exact same trade-off already
accepted for D-116/D-117/D-131 since D-160, now extended to ordinary fuzzy matching too.

**D-209 - the "Kita" bucket-cap starvation, found independently while discussing the felt lag.** User
report: typing "Kiza" (meaning "Kita") autocorrected to "Liga" instead. Confirmed against the real bundled
data: "Kita" (frequency 17) exists, but `correctionCandidatesInternal()`'s per-first-character-bucket LIMIT
(`CANDIDATE_LIMIT / firstChars.size`, ≈285 for a 7-char neighbour set) crowds it out of its own 'k'-bucket -
636 same-length 'k'-words exist, 389 of them more frequent than 17, exceeding the cap - so "Kita" never
even reaches the edit-distance comparison, while "Liga" (reached via the 'l' neighbour-key bucket) does.
The same class of bug D-197 already fixed for `diacriticCandidates()`, but not for the general
`correctionCandidates()` path `bestCorrection()`/`fuzzyNeighbours()`/`wideFuzzyNeighbours()` all share.
Deliberately **not** fixed by raising or removing the limit everywhere (that bucket search is exactly what
D-208 above just finished making cheaper by deferring) - fixed narrowly instead: the token's own *literal*
first-character bucket (where an ordinary, same-first-letter typo actually lives - the common case) is now
searched with no cap at all; the keyboard-neighbour buckets (reached only for a rarer first-key typo) keep
the existing per-bucket limit. `SqliteDictionaryStore.correctionCandidatesInternal()` gained an
`uncappedChar` parameter, applied only to the token's own first character; `correctionCandidates()` passes
its own first character as that argument. D-208's own deferral is what makes this safe to do at all -
uncapping the primary bucket while it still ran on every keystroke would have reopened the exact cost
concern D-208 just fixed.

**Order matters**: D-208 (defer fuzzy off the hot path) was implemented before D-209 (uncap the primary
bucket) specifically so the wider bucket search D-209 introduces only ever runs once per settling pause,
never per keystroke.

6 new tests (`DictionarySuggestionProviderTest`: 3 for `bestCorrectionFor()`'s cost-derivation agreement
with the two separate calls it replaces, 2 for D-208's fuzzy-gating behaviour, 1 existing test renamed for
accuracy; `SqliteDictionaryStoreRoboTest`: the existing D-197 comparison test adjusted to a genuine
neighbour-bucket scenario since the token's own bucket is no longer capped, plus 1 new test demonstrating
D-209's own primary-bucket fix directly with the real "Kita"-shaped frequency gap). 770 unit tests total
(764 + 6). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

## D-210 - Captured, Not Designed: A-05 Split Regression Producing Nonsense ("übrigebs" -> "übrig Ebs")

Flagged by the user while supplying the second sluggishness-investigation log, explicitly deferred so as
not to distract from the performance work ("das soll das aktuelle Thema nicht distracten") but named as a
"klare Regression" they are now seeing "häufiger" (more often) and want addressed as its own, dedicated
round. Observed directly in the log: typing `"übrigebs"` (itself a typo of `"übrigens"`, n->b) committed as
two separate tokens, `"übrig"` and `"Ebs"` - the latter capitalised and not a real word at all, i.e. an
A-05 split fired and produced a nonsensical result instead of a straightforward whole-word correction to
"übrigens". Not traced further this round - no root-cause hypothesis recorded yet, deliberately, since the
user asked to look at this "unbedingt" and separately. Needs its own device-log-driven investigation before
any fix is attempted, per this project's established convention for this class of bug.

## §135 - D-211: The Expensive Suggestion Search Moves Off the Main Thread Entirely (v0.8.98)

Follow-up to §134's own D-207/D-208/D-209 round, raised by the user directly: rather than debouncing the
expensive fallback search on the main thread (D-160's original design, still relied on by D-208), why not
run it on a background thread and simply discard the result if a newer keystroke has arrived by the time it
finishes? The user pre-empted the two real objections themselves: a forced thread kill is unsafe (correctly
recalling `Thread.stop()`'s Java 1.2 deprecation and doubting it is even reliably available on current
JDKs) and getting this right needs the in-flight search - and any late callback that "verspätet reinfunkt" -
to recognise itself as stale rather than overwrite newer state, proposing cooperative abort checks as the
safe replacement.

**Confirmed as the right direction, and better than §134's own D-208 fix, not just an addition to it**:
`handler` (`Handler(Looper.getMainLooper())`) - D-160's debounce only ever delayed *when* the expensive
search ran, never moved it off the main thread; once the delay elapsed it still blocked the UI thread (and
the key-press flash render) for however long the search took. A working precedent for exactly the proposed
shape already existed in this codebase: `tier3Executor` (`Executors.newSingleThreadExecutor()`) already
runs the mini-LLM prediction off-thread, checking a sequence number (`tier3RequestSeq`) once before starting
and once more before posting the result back via `handler.post` - the same "cooperative staleness check,
not a hard kill" shape the user arrived at independently. The one gap: tier3 only checks staleness *around*
its single expensive call, never *during* it - fine for one bounded inference call, insufficient for a
candidate-bucket scan that can now run to hundreds of rows (D-209 uncapped the primary bucket) with a
per-candidate edit-distance computation in between.

**Implemented**: `AdaptKeyService.expensiveSuggestionToken`/`expensiveSuggestionRunnable` (D-160's
`Handler.postDelayed` debounce) replaced entirely by `expensiveSuggestionExecutor`
(`Executors.newSingleThreadExecutor()`, mirroring `tier3Executor`) and an `AtomicInteger
expensiveSuggestionSeq` (proper cross-thread visibility, unlike a plain `var`). `refreshSuggestions()` bumps
the sequence on every fresh (non-deferred) call; the deferred dispatch now submits directly to the executor
(no artificial delay at all - a fast, continuous typing burst simply produces a string of near-instant
bail-outs instead of queued waiting, and a genuine pause gets its result the moment the search actually
finishes, not `EXPENSIVE_SUGGESTION_DELAY_MS` later on top of that). The background block checks staleness
once before calling `provider.suggestionsFor()` at all (a call already stale before it starts never touches
the database), and the result is checked a second time on the main thread, inside `handler.post`, against
both the sequence *and* `composing.toString() == input`, before ever being applied - mirroring tier3's own
double-check exactly.

**The mid-search gap tier3 doesn't have, closed via cooperative cancellation**: `SuggestionProvider.
suggestionsFor()` gained a new `isCancelled: () -> Boolean = { false }` parameter (default no-op, so every
existing caller/test is unaffected); `DictionarySuggestionProvider.fuzzyNeighbours()`/`wideFuzzyNeighbours()`
- the two candidate-bucket loops D-208/D-209 already identified as the actual expensive part - rewritten
from a lazy `mapNotNull` chain to an explicit loop that polls `isCancelled()` once per candidate and
`break`s early, returning whatever was already gathered (harmless either way, since the caller re-checks
staleness before ever applying it). `compoundCandidate()`'s own, much smaller, token-length-bounded search
was deliberately left unchanged - not worth the same treatment, since its own iteration count was never the
measured driver of the slowdown. Avoiding a request backlog under fast typing was also considered
explicitly: `Executors.newSingleThreadExecutor()` queues excess submissions rather than dropping them, but
each queued-then-dequeued stale task now bails via the cheap start-of-call sequence check before ever
reaching the database, so a queue of superseded requests costs only a handful of atomic reads, not repeated
full searches.

2 new tests (`DictionarySuggestionProviderTest`: the fuzzy scan stopping after the very first
`isCancelled()` poll returns true, and a control case confirming a never-cancelling poll changes nothing).
772 unit tests total (770 + 2). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed - this closes out the sluggishness investigation from §134/D-207/D-208/D-209 as one
coherent piece of work, per the user's own explicit request to see it through properly rather than leave it
half addressed. D-210 (the A-05 split regression) remains its own, separately deferred item.

## §136 - D-212: WAL Enabled - D-211 Alone Showed No Improvement, Root-Caused to Connection-Level Locking (v0.8.99)

The user re-ran §135/D-211's own repeated "Herzlichen Glückwunsch ... Geburtstag" test against the new
background-thread build and supplied a fresh log - direct quote: "Das hat leider noch so gar nicht
funktioniert." Measured via the log's own raw millisecond timestamps exactly as before: "Geburtstag" now
costs ~450ms/character, equal to or worse than the pre-D-211 baseline - D-211 alone did not help. (Two
apparent typos in the same log, `"Glückwunsvh"` for `"Glückwunsch"` and `"ubrigebd"` for `"übrigens"`, were
checked and are plausible ordinary adjacent-key mistypes - v/c and b/n, d/s are each real QWERTZ neighbours
- not evidence of the fix corrupting text; not treated as a finding.)

**Root-caused, not guessed**: `SqliteDictionaryStore` extends `SQLiteOpenHelper` with a single connection
(`writableDatabase`) and never called `enableWriteAheadLogging()` (confirmed by grep - zero hits before this
round). Without WAL, SQLite's default rollback-journal mode serialises *all* access to a connection across
threads, including pure reads - so D-211's background search and the main thread's own, still-synchronous
per-keystroke prefix-completion query (`unigramsByPrefix()`, never moved off the main thread, correctly so -
it is meant to stay instant) contend for the same lock. D-211 moved the expensive search off the main
thread's own call stack, but without WAL the main thread was still getting blocked *indirectly*, waiting on
that lock - and D-209's own uncapped primary bucket made the background query itself larger, so the
contention got worse, not better, explaining why this test if anything looked equal or slightly worse than
before D-211.

**Also discussed and deliberately deferred**: whether a running `rawQuery()` can be aborted mid-flight at
all. Confirmed genuinely possible via `android.os.CancellationSignal` (Android registers a real
`sqlite3_progress_handler` that can interrupt an in-progress query, not just gate before it starts) - but
wiring it through would mean threading an Android-specific type into `DictionarySuggestionProvider`/
`DictionaryStore`, both deliberately kept Android-free so they stay testable on the plain JVM without
Robolectric. The user's own instinct: this is very likely solvable without actually breaking that
abstraction for tests (e.g. a generic cancellation-token shape the Sqlite implementation happens to bridge
into a real `CancellationSignal`, while the in-memory one just ignores it) - agreed as plausible, but
deliberately not designed this round. Kept in reserve specifically for if a fresh device log after WAL
still shows a problem attributable to the query itself, not the connection-lock contention WAL now removes.
Separately confirmed, addressing the user's other question directly: `expensiveSuggestionExecutor` is
single-threaded, so queued-but-not-yet-started stale requests already drain near-instantly via the cheap
sequence check before ever touching the database - "10 parallel queries" was never actually possible with
this design; only the one request already mid-query when superseded pays for a single query's own duration,
a small, bounded cost distinct from the connection-locking problem WAL fixes.

**Fixed**: `SqliteDictionaryStore.onConfigure()` (new override) calls `database.enableWriteAheadLogging()`
before any table is created/opened - additive, no schema/version change, reaches every existing install
automatically on its next open. No new tests (Android/SQLite configuration, exercised indirectly by every
existing `SqliteDictionaryStoreRoboTest` case continuing to pass under WAL). 772 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - awaits the next repeat of
the same "Herzlichen Glückwunsch..." test to see what, if anything, is still left.

## §137 - D-213: The Real Dominant Cost Found - trySplit()'s Own Main-Thread Dictionary Reads (v0.8.100)

The user re-ran the exact same repeated test a third time against the D-212 (WAL) build - direct quote:
"Leider war es das noch gar nicht... Es ist eigentlich schlimmer denn je." Precisely measured via the log's
own raw timestamps exactly as the prior two rounds: "Geburtstag" still costs ~300-450ms/character - WAL
alone did not resolve it either. (Two more apparent typos in this log, `"Herzlicjen"` for `"Herzlichen"`
and `"Glückeunsch"` for `"Glückwunsch"`, were checked - h/j and w/e are both real QWERTZ neighbours,
plausible ordinary mistypes; the first was correctly raw-coordinate-corrected, the second was not, but that
is a dictionary-coverage question, not this round's concern.)

**Root-caused, not guessed - and it was not the path any of D-207/D-208/D-209/D-211/D-212 had touched at
all.** Re-examined `TokeRepair.candidateAt()` (called once per split position `trySplit()` tries) and
counted its own real cost: `resolveWord()` twice (each trying every `Umlaut.unfoldCandidates()` variant
against `store.isKnownWord()`), `frequencyOf()` three times (one of them redundant - already read once
earlier in the same call), `partsOfSpeech()` twice, `bigramFrequency()` once - up to ~8 separate SQLite
reads per split position. `trySplit()` itself tries every split position from `MIN_PART` to
`length - MIN_PART` (`MIN_PART = 2`), so a 10-character token like "Geburtstag" costs roughly 7 positions
x 8 reads = 50+ SQLite round-trips in a single call - for a token that has not even finished being typed
yet, since `composingPreviewRunnable` fires on every settled pause, not only at commit.

This is called from `composingPreviewRunnable` (the S-05/§47 live highlight/split-colour preview) - a
completely different code path from the suggestion-bar search all of D-207 through D-212 addressed. §125/
D-194's own original reasoning ("this preview is cheap enough... D-211's own background-thread treatment
was judged unnecessary for it") was never actually measured against real data and turned out to be wrong -
confirmed only now, after fixing the *other* path left this one as the sole remaining, now-dominant cost.

**Fixed, mirroring D-211/D-212's own shape exactly**: new `composingPreviewExecutor`
(`Executors.newSingleThreadExecutor()`, kept separate from `expensiveSuggestionExecutor` so the two searches
run concurrently with each other under WAL rather than queueing behind one another on a shared thread) and
reuses the existing `expensiveSuggestionSeq` as its staleness signal - a fresh keystroke invalidates both
kinds of deferred work identically, so a second counter would have been redundant. `composingPreviewRunnable`
now does only the cheap, `InputConnection`-dependent part on the main thread (`isEditingMidWord(ic)`, since
Android's `InputConnection` contract expects the IME's own main thread) and snapshots the mutable state the
background computation needs (`spaceAmbiguousIndices()` - `composingTaps`/`composingFlags` are mutable
fields the main thread keeps editing on every subsequent keystroke, so reading them from a background
thread without a snapshot first would have been a genuine data race, not just a staleness risk) before
dispatching `tokenRepair.trySplit()`/`provider.isKnownWord()` to the background executor; the result is
posted back via `handler.post`, re-checked for staleness, and only then applied via `updateComposing()`
with a freshly re-read `currentInputConnection` (the one captured before dispatch could have gone stale by
the time the background work finishes). `scheduleComposingPreviewRefresh()` also drops its own artificial
`EXPENSIVE_SUGGESTION_DELAY_MS` debounce entirely (dispatches via `handler.post` instead of `postDelayed`),
mirroring D-211's own reasoning: the background thread never blocks the main one, so nothing is left for a
fixed wait to protect against. The now-dead `shouldHighlightComposing()`/`splitPreview()` helper functions
(fully superseded by the inline computation in `composingPreviewRunnable`) and the now-unused
`EXPENSIVE_SUGGESTION_DELAY_MS` constant were removed outright rather than left as dead code.

No new unit tests (Android threading/`InputConnection` glue, the established gap for this layer -
`TokenRepair.trySplit()`'s own pure logic was already covered when D-45/D-203 built it, unchanged here).
772 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed -
this is the fourth round of the same investigation; awaits the next repeat of the same test.

## §138 - D-214/D-215/D-216: The Real Cost Was O(n×m) Computation, Not Stale Requests - Redundant Queries
Cut, Debounce Restored, trySplit() Cancellable (v0.8.101)

The user repeated the test a fifth time, deliberately typing *slowly* this time - and the same
per-character slowdown was still there, even though slow, deliberate typing can never build up a backlog of
stale/superseded background work for D-211/D-212/D-213's cancellation-and-cancel-on-restart machinery to
discard. Direct quote, correctly diagnosing the whole investigation's framing as wrong: "Das heißt, wir sind
möglicherweise etwas auf dem Holzweg. Das eigentliche Problem sind nicht veraltete Anfragen... Das eigentliche
Problem ist, dass je länger das Wort wird der Aufwand vermutlich O(n*m) ist." The user was right: every prior
round (D-207-D-213) had chased wasted/discarded work, but the dominant cost was genuine, non-wasted
computation - each `trySplit()` call for an n-character token really does try on the order of n split
positions, each running `candidateAt()`'s full store lookups, and that cost is paid in full even when the
result is never stale. The user proposed, and this round implements, three distinct fixes:

**(a) D-214 - cut the redundant SQLite re-fetches inside `candidateAt()` itself.** §137/D-213 had already
identified that `candidateAt()` cost ~8 separate store reads per split position (`resolveWord()` x2,
`frequencyOf()` x3, `partsOfSpeech()` x2, `bigramFrequency()`) but only moved that cost off the main thread,
never reduced it. Both `SqliteDictionaryStore` and `InMemoryDictionaryStore` already had an identical private
`entryOf(word): WordEntry?` helper internally merging the bundled+learned tables to answer `isKnownWord()`/
`frequencyOf()`/`partsOfSpeech()` - promoted to a new public `DictionaryStore.entryOf()` interface method
(placed after `partsOfSpeech()`, before `allKnownWords()`) so `TokenRepair` can fetch a word's full entry
once and reuse it. `TokenRepair.resolveWord()` now returns the resolved `WordEntry?` itself (not just the
canonical `String?`), and `candidateAt()`/`isNoun()`/`score()` were rewritten to consume that `WordEntry`
directly instead of re-querying the store per fact - collapsing ~8 reads per split position down to 2
(`resolveWord()` left/right) plus the two genuine `bigramFrequency()` calls `score()` still needs (bigram
counts are not part of `WordEntry` and have no cheaper combined form to fetch). Both concrete stores' private
`entryOf()` became `override fun entryOf()` with no logic change - purely a visibility change.

**(b) D-215 - the debounce delay is restored, but for a different reason than it was originally removed
for.** §135/D-211 and §137/D-213 dropped `EXPENSIVE_SUGGESTION_DELAY_MS` entirely on the reasoning that once
the expensive work ran on a background thread, nothing was left for a fixed wait to protect the main thread
against. That reasoning was correct as far as it went, but conflated two distinct things a debounce delay can
be for: protecting the main thread from blocking (genuinely solved by D-211/D-213's background threads), and
avoiding computation the user can never perceive because a fast subsequent keystroke will supersede it before
the result would even be shown (not solved by moving the work to a background thread at all - a background
thread still burns a full CPU core's worth of real work per keystroke while the user is typing quickly, work
whose result is thrown away unseen the moment the next key lands). The user's own framing: defer the
live-preview/split check the same way T-02/T-03 already defer raw-coordinate correction until a genuine
pause, since during fast typing nothing needs it yet. Re-added `EXPENSIVE_SUGGESTION_DELAY_MS = 200L` and
dispatch via `handler.postDelayed(..., EXPENSIVE_SUGGESTION_DELAY_MS)` again (instead of `handler.post`) for
both `expensiveSuggestionRunnable` (`refreshSuggestions()`'s scheduling block) and `composingPreviewRunnable`
(`scheduleComposingPreviewRefresh()`) - each still resets/removes any pending callback on every new keystroke
exactly as before, so the delay only ever fires once typing actually pauses for 200ms, at which point the
(still background-threaded, still cancellable) computation runs without ever having blocked the main thread
during the fast-typing stretch that preceded it. `clearComposing()` now also cancels
`expensiveSuggestionRunnable` (previously only cancelled `composingPreviewRunnable`), matching the pair
symmetrically. KDoc/comments on the affected fields and methods were corrected to state the real reason for
the delay (perceptibility/wasted-computation, not main-thread protection) rather than leave the now-inaccurate
D-211/D-213 reasoning in place.

**(c) D-216 - `trySplit()` gained its own cancellation check, independent of (a) and (b).** Even with (a)'s
reduced per-position cost and (b)'s debounce ensuring it only runs after a genuine pause, a single `trySplit()`
call for a long token can still, in principle, run long enough to be worth abandoning mid-scan if a fresh
keystroke arrives while it is running on the background thread - the user asked for this "auf jeden Fall"
regardless of how (a) and (b) turned out. `trySplit()` gained a new `isCancelled: () -> Boolean = { false }`
parameter (defaulting to a no-op, so every existing call site and every existing pure-logic unit test needed
no change) and its body was rewritten from the previous `.mapNotNull` chains over `dropIndices`/the missed-space
range to explicit loops, each checking `isCancelled()` before evaluating the next split position and, if
cancelled, returning the best candidate found so far (`candidates.maxByOrNull { it.second }?.first`) rather
than continuing the scan - mirroring D-211's existing cooperative-cancellation shape (a lambda polled between
units of work, never `Thread.stop()`). `AdaptKeyService.kt`'s `composingPreviewRunnable` now passes
`{ seq != expensiveSuggestionSeq.get() }` into `trySplit()`, reusing the same `expensiveSuggestionSeq`
staleness signal (b) and D-211/D-213 already share. `splitAtUnresolvedConnector()` was deliberately left
unchanged - it runs synchronously on the main thread only during mid-word re-edit, is restricted to
`OVER_SPACE_LETTERS` positions, and was judged out of scope for this round.

Two new unit tests added directly for (c), in `TokenRepairTest.kt`, both reusing the existing §128
`"undbald"` -> `"und"` + `"bald"` fixture: cancelling on the very first poll (`{ true }`) must yield `null`
rather than the split trySplit would otherwise find, and an explicit always-false `isCancelled` (`{ false }`)
must leave the result unchanged - proving the new parameter's cancellation path actually short-circuits the
scan, not merely that its default no-op is harmless. (a)'s `entryOf()`-based rewrite of `candidateAt()`/
`resolveWord()`/`isNoun()`/`score()` is exercised indirectly by every pre-existing `TokenRepairTest` case,
all of which continued to pass unchanged, confirming behaviour preservation. 774 unit tests (772 + 2).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed - awaits the user's next repeat
of the same on-device test.

## §139 - D-217: Diagnostics Now Show Actual Processing Time and Actual Flash-Paint Latency, Not Just Typing
Speed (v0.8.102)

Before re-testing D-214/D-215/D-216 on-device, the user asked for the diagnostics themselves to be sharpened:
"Ich glaube, derzeit kannst du nur sehen, wie schnell ich tippe, aber nicht, wie schnell der Buchstabe
tatsächlich verarbeitet wurde und nicht, wie schnell der flash-Effekt auf der Taste aktualisiert wurde." Correct
- every existing `AdaptKeyJitter`/`AdaptKeyHaptics` timestamp so far only records *when a call happened*, from
which typing cadence can be inferred, but nothing directly measures either (1) how long the keyboard's own
processing of a single tap actually took, or (2) how long the visual flash acknowledgement actually took to
reach the screen - exactly the two numbers this whole sluggishness investigation has been reasoning about
indirectly from proxies (SQLite query counts, O(n×m) estimates) rather than measuring directly.

**(1) Per-key processing time, in `AdaptKeyService.handleKey()`.** This is the single synchronous entry point
every tap reaches after `AdaptKeyboardView` resolves it (called from `onTouchEvent`'s `ACTION_UP`), and it does
all of the main-thread work a keystroke requires - `insertComposingChar()`, `updateComposing()`,
`refreshSuggestions()`'s own synchronous portion - before returning control to the looper. The whole existing
body (previously several independent `return` points: the ic-null guard, the A-07 undo branch, each `when`
arm) is now wrapped in a `try`/`finally`, mirroring `diag()`'s own single-choke-point reasoning and the
existing `ic.beginBatchEdit()`/`endBatchEdit()` try/finally already inside the `CHAR` branch - a start
timestamp (`handledAt = SystemClock.uptimeMillis()`) is captured once at entry, and the elapsed time is logged
unconditionally in `finally`, so every exit path is measured, not only the common one. New line, tag
`AdaptKeyHaptics` (reused rather than adding a third tag, since it is still per-key diagnostic output the user
already watches for): `handleKey: key=<code> '<char>' t=<start> processed in <n>ms`.

**(2) Flash-paint latency, in `AdaptKeyboardView`.** `flash(key)` (called from `ACTION_UP`, immediately before
the `onKeyListener?.onKey()` call that reaches (1) above) only *requests* the highlight - it sets `flashKey`
and calls `invalidate()`, which merely schedules a redraw for whenever the system next services this view's
draw pass; if the main thread is busy (e.g. still inside the very `handleKey()` call this same touch event
triggers, or a queued message ahead of it), the actual paint can lag well behind the request, and previously
nothing measured that gap at all - `invalidate()` returning promptly says nothing about when `onDraw()` will
actually run. Added `flashRequestedAt`/`flashTimingPending`; `flash()` stamps the request time and arms the
flag, and `drawKeys()` - at the exact point it selects `pressedKeyPaint` for `flashKey` - checks the flag,
logs the elapsed time since the request, and clears it, so exactly one line is emitted per flash (not one per
frame it stays lit for). `logHaptics()` gained an optional `prefix` parameter (default `"playKeyFeedback"`, so
its four existing call sites are unchanged) so this new call site can pass `prefix = "flash"` instead - the
old hardcoded `"playKeyFeedback: "` prefix would have mislabelled a line that has nothing to do with haptics.
New line, same `AdaptKeyHaptics` tag: `flash: flash painted key=<id> requested at <t>, visible after <n>ms`.

Together, a repro log now lets the two costs be told apart directly: a long `handleKey`-processing number
points at main-thread computation (the ongoing D-214/D-215/D-216 territory); a long flash-visible number with a
*short* handleKey number instead points at the main thread being busy with something else entirely (a queued
touch/draw message, GC, a different pending Runnable) rather than the keystroke's own processing.

Both are temporary diagnostics in the established D-139/D-193 style (dual-output via `diag()`/`logHaptics()` to
logcat and the in-app rolling log, no behavioural change) and will be removed once D-217 (and by extension the
whole sluggishness investigation) is closed. No new unit tests - purely Android View/`InputConnection`/logging
glue, the established untested gap for this layer; no pure logic changed. 774 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green.

## §140 - D-218: The Real Remaining Cost Found - the Impending-Autocorrect/Diacritic Preview Ran Unconditionally
on Every Keystroke, Untouched by D-207-D-217 (v0.8.103)

D-217's own new `AdaptKeyHaptics` `handleKey` timing log paid off immediately: a repeat of the standard test,
once fast and once deliberately slow, both showed `handleKey: ... processed in Nms` growing with the composing
token's own length regardless of typing speed - e.g. `"Glücjwunsvh"` (a nonsense string, since it never matches
a real word): 218 -> 298 -> 353 -> 407 -> 457 -> 458 -> 450 -> 615ms; `"Geburtstag"`: 231 -> 303 -> 212 -> 244 ->
500 -> 484 -> 283ms. The new `flash: ... visible after Nms` log tracked the same `handleKey` numbers almost
exactly (e.g. `'r'` processed in 170ms, flash visible after 172ms) - confirming the flash lag is not a separate
rendering-pipeline problem but a direct consequence of the same main-thread cost: the frame simply cannot be
painted until `handleKey()` returns control to the looper.

**Root-caused, not guessed - and genuinely untouched by D-207 through D-217, all of which addressed either the
suggestion-bar candidate search or the composing-preview split highlight.** `AdaptKeyService.refreshSuggestions()`
- called synchronously from every `handleKey()`, on every ordinary keystroke, never covered by the D-215
debounce - unconditionally computed `pending` (the S-06 "impending autocorrect" chip) via
`DictionarySuggestionProvider.diacriticRestoration()` and `autocorrectFor()`/`bestCorrection()`. Both run a real,
uncached store search per keystroke: `store.correctionCandidates()`/`store.diacriticCandidates()` (the latter,
per its own D-197 KDoc, "never frequency-truncated" - potentially larger than the already-large correction
bucket) scanned in full, with a banded edit-distance computation and a separate `store.frequencyOf()` query for
every candidate. Exactly the same cost shape D-214 already fixed inside `TokenRepair` - just at a completely
different call site nobody had measured directly before D-217's own timing log existed to point at it.

**Fixed by deferring the expensive half of `pending` into the existing D-211/D-215 background-executor/200ms-
debounce pipeline**, mirroring `expensiveSuggestionRunnable`'s own `suggestionsFor()` dispatch exactly:
`refreshSuggestions()`'s `pending` block now only applies `capitalisation.capitalise()` to a
`precomputedPendingCandidate` (new parameter, null on the hot/immediate path and every `duringRepeat` call) - so
the immediate keystroke path still shows the cheap, DB-free capitalisation-only preview (a sentence-start
capital, D-111/D-112) exactly as before, but the expensive whole-word autocorrect/diacritic replacement only
appears once the token has actually been stable for `EXPENSIVE_SUGGESTION_DELAY_MS`. The search itself moved
into a new `pendingCorrectionCandidate(input, previousWord, language)`, called from
`dispatchExpensiveSuggestionSearch()`'s existing background-executor block right alongside its `suggestionsFor()`
call, sharing the same staleness guard (`expensiveSuggestionSeq`) and the same single `handler.post` back to
`refreshSuggestions(includeExpensiveFallbacks = true, ...)` - so both results are applied together in one call,
rather than a second independent round trip. Capitalisation itself deliberately stays a main-thread-only step
(applied once the raw candidate is back), since it reads several mutable fields (`capsMode`, `tokenSentenceStart`,
`tokenAfterHyphen`, `fieldMandateOverridden` via `contextFor()`) that are not safe to read from a background
thread without the same snapshot-first discipline D-213 already established for `composingTaps`/`composingFlags`.
`knownInOtherLanguage()` gained an explicit `activeLang` parameter (defaulting to the live field, so its one
other call site in `finalizeAndCommit` is unchanged) for the same reason - `activeLanguage` is read once on the
main thread in `dispatchExpensiveSuggestionSearch()`, before the executor lambda starts, and passed through
rather than read live from the background thread.

As a side effect this also removes a standing redundancy: `pending` used to be recomputed a second time,
synchronously, when the deferred pass's own `refreshSuggestions(includeExpensiveFallbacks = true, ...)` call
landed back on the main thread - it now simply consumes the already-computed `precomputedPendingCandidate`
instead.

No new unit tests - `refreshSuggestions()`/`dispatchExpensiveSuggestionSearch()`/`pendingCorrectionCandidate()`
are Android-service glue depending on `InputConnection`/`Handler`/executor state, the established untested gap
for this layer; `DictionarySuggestionProvider.diacriticRestoration()`/`autocorrectFor()` themselves are unchanged
and already covered. 774 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed - awaits the user's next repeat of the same on-device test, this time expected to show `pending`
no longer growing `handleKey`'s own processing time on the hot path.

## §141 - D-219: the Flash Effect Decoupled from Key Processing and Shortened, at the User's Own Explicit
Request (v0.8.104)

Ahead of the next device re-test, the user asked for the flash acknowledgement itself to be reworked, on two
independent points: (1) it should paint completely independently of how long processing takes, not merely
"faster processing eventually makes the flash faster too" - even once D-207-D-218 are fully proven out, the
flash should never again be at the mercy of whatever the main thread happens to be doing that keystroke; and
(2) its ON duration itself still feels too long, wanting the GBoard-like snappiness of a popup that dismisses
almost instantly, not just an eventually-fast highlight.

**(1) Decoupling.** `AdaptKeyboardView.onTouchEvent()`'s `ACTION_UP` handler previously called `flash(key)`
(which calls `invalidate()`) and then, in the very same synchronous call, `onKeyListener?.onKey(...)` -
meaning the actual paint `invalidate()` merely *schedules* could not happen until this whole touch-event
dispatch (including `onKey()` -> `handleKey()` -> everything D-207-D-218 have been trying to speed up)
returned control to the looper. `onKeyListener?.onKey(...)` is now dispatched via `longPressHandler.post { }`
(the same general-purpose main-looper `Handler` this class already uses for every other deferred callback)
instead of called inline - `invalidate()`'s own `scheduleTraversals()` posts a sync barrier to the shared main
Looper's message queue ahead of any ordinary `Handler` message, which is exactly why `View.post()`-after-
`invalidate()` is Android's own standard pattern for "let this frame paint before doing the next (possibly
slow) thing": the pending frame is guaranteed to draw before the posted `onKey()` call even starts, regardless
of how long `handleKey()` takes. `downX`/`downY`/`pendingAmbiguity`/`pendingRecordWeight` are captured into
locals before posting (not read inside the deferred lambda) - these are mutable view fields the very next
tap's `ACTION_DOWN` could otherwise already have overwritten by the time the posted call actually runs; `key`
was already a local (`val key = pressedKey`) for the same reason. Ordering between consecutive keystrokes is
preserved regardless - both go through the same single-threaded `longPressHandler`, so two posted `onKey()`
calls still fire in the order they were posted.

**(2) Duration.** `flashDurationMs` lowered from 28ms to 16ms (one frame at a typical 60Hz refresh - the
shortest duration still reliably visible at all) - the user's own framing was that GBoard's acknowledgement is
a popup that dismisses almost instantly, and that snappiness itself (not merely avoiding a highlight that
*looks* sluggish) was the actual goal.

Together these make the flash's own timing fully independent of the still-ongoing D-207-D-218 processing-speed
work - D-217's own `flash: ... visible after Nms` log is the number to watch on the next repro: it should now
read close to one frame's worth of milliseconds regardless of what `handleKey: ... processed in Nms` says right
next to it, which was not true before this round (the two numbers had been tracking each other almost exactly).

No new unit tests - pure Android View touch/paint glue, the established untested gap for this layer; no pure
logic changed. 774 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.

## §142 - D-220: Diagnostics for the Last Remaining Slow Spot - finalizeAndCommit()'s Own Commit-Time Search
Chain (v0.8.105)

D-217/D-218/D-219 were device-confirmed a clear success: "Das hat insgesamt richtig gut funktioniert. Das
Flashing ist super knackig und die Tastatur wirkt kaum noch träge." A fresh pair of logs (fast and slow typing)
showed ordinary letters now processing in 17-55ms (down from the 200-600ms this whole investigation started
from) and flash latency consistently in the low single digits to ~17ms. The user's own "vielleicht hier und da
nochmal minimal" pointed precisely at one remaining, clearly identifiable pattern: every `handleKey` spike left
in either log sat exactly on a SPACE or punctuation delimiter (234/685/100/470/238/252/270ms), never on an
ordinary letter - i.e. exactly the keystrokes that trigger `finalizeAndCommit()`.

**Not yet root-caused with real numbers - only located to the right function.** `finalizeAndCommit()` runs a
chain of up to four real, uncached searches synchronously on every commit, none of which D-207-D-219 ever
touched (a commit's own correction has to be resolved before the delimiter is committed, so none of this chain
can simply be deferred the way the composing-time previews were): `diacriticRestoration()`,
`bestCorrectionFor()` (D-207's already-merged bestCorrection/highConfidence search), conditionally
`tokenRepair.trySplit()` (D-214-optimised, still real work), and conditionally `rawCoordinateCorrection()` (D-39)
when nothing else matched. The 685ms outlier (`"Glücjwunsvh"`/`"Glückwunsvh"`, a nonsense token across both
rounds' logs) is the worst case: none of the four stages find anything, so every one of them runs to
exhaustion. Per the project's own root-cause-before-fix convention, guessing which of the four stages actually
dominates would be exactly the mistake this whole investigation already corrected once (the D-211-era "stale
requests" framing) - so this round adds only measurement, no fix yet.

Each of the four stages is now individually timed (`SystemClock.uptimeMillis()` around each call, matching
D-217's own style) and reported via two new `AdaptKeyHaptics`-tagged `diag()` lines - one for the split-found
early return (`applySplit()` returns before the existing `dict=.../diacriticWord=...` diag further down is ever
reached), one for the ordinary path further down, reusing the same `diacriticMs`/`bestCorrectionMs`/`splitMs`
locals plus a new `rawCorrectedMs`: `finalizeAndCommit: timing diacriticMs=X bestCorrectionMs=Y splitMs=Z
rawCorrectedMs=W`. Temporary, D-139/D-193/D-217-style diagnostic - to be removed once D-220 is closed.

No new unit tests - pure diagnostic timing around existing, already-tested pure logic; no behaviour changed.
774 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Awaits the user's next
on-device repro of a slow SPACE/punctuation commit so the four stages' actual breakdown decides where D-220's
real fix should go.

## §143 - D-220 Fixed: bestCorrection() Queried frequencyOf() for Every Same-Bucket Candidate Before Checking
Whether Its Edit Cost Even Qualified (v0.8.106)

The D-220 timing log paid off immediately, one more time: a fresh pair of logs (fast and slow typing) showed
`diacriticMs`/`splitMs`/`rawCorrectedMs` consistently at 0-3ms, while `bestCorrectionMs` alone dominated every
slow commit - 186-404ms for `"Herzlichen"`, `"Glückwunsch"`, `"Geburtstag"` (all already-correct, common-
initial-letter words), against just 0-19ms for `"übrihens"`/`"zum"` (an uncommon initial letter and a short,
`suppressAutocorrect`-skipped token respectively).

**Root-caused, not guessed.** `DictionarySuggestionProvider.diacriticRestoration()` checks `isKnownWord(token)`
before doing anything else and returns immediately for an already-correct word - exactly why its own cost stayed
near zero for `"Herzlichen"` etc. `bestCorrection()` has no equivalent early exit: it only consults
`isKnownWord(token)` *after* the full search already ran, to decide whether A-01's known-word protection blocks
the result it found - the search itself runs unconditionally regardless of whether the token turns out to be
already known. Worse, inside that search's own `.mapNotNull{}`, `store.frequencyOf(candidate)` was called for
*every* candidate in the token's own first-character bucket - unconditionally, before the cheap, pure-CPU
`correctionCost()` check that would reject most of them was even consulted. D-209 deliberately left that bucket
uncapped (for correctness, so a low-frequency word in a large bucket is still found), so a common German initial
letter like H or G can hold hundreds of same-bucket entries - meaning hundreds of wasted SQLite round-trips on
every single commit of an already-correctly-spelled, common-initial-letter word.

**Fixed**: the `cost > maxCost` check now runs first inside the `.mapNotNull{}`, returning `null` immediately
(via `return@mapNotNull`) before `store.frequencyOf(candidate)` is ever called for a candidate already outside
the edit-cost budget; `frequencyOf()` is now only queried for the (typically far smaller) set of candidates that
already qualify by cost. Purely a reordering of two already-independent checks (`cost` never depended on
`frequency` or vice versa) - the final candidate set and the winning correction are unchanged, confirmed by
every existing `bestCorrection`/`autocorrectFor`/`bestCorrectionFor` test passing unmodified.

No new unit tests - a behaviour-preserving reordering of already-tested pure logic; the existing test suite is
exactly what confirms nothing changed. 774 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest`
green. Not yet device-confirmed - the D-220 timing log (`AdaptKeyHaptics`, `finalizeAndCommit: timing
diacriticMs=... bestCorrectionMs=... splitMs=... rawCorrectedMs=...`) stays in place for the next repro to show
`bestCorrectionMs` actually dropping for a common-initial-letter word.

## §144 - D-220 Device-Confirmed, D-221 Fixed: diacriticRestoration() Uncapped Every Bucket, Not Just the Ones
That Correctness Actually Required (v0.8.107)

A fresh pair of logs (fast typing, then deliberately more typos) confirmed D-220 first: `bestCorrectionMs` for
already-correct, common-initial-letter words (`"Herzlichen"`, `"Glückwunsch"`, `"Geburtstag"`) dropped from
186-404ms to 68-90ms (often 0-8ms for short/known-word-protected tokens) - the predicted 3-5x win, device-
confirmed. The user's own observation from the same log pointed at a second, distinct pattern: "Wenn man das
Wort ungünstig falsch tippt, dauert der Commit länger" - genuine typos of common-initial-letter words
(`"Glückwusf"`, `"Geburtstsf"`, `"Glückwunsfh"`, and a mid-typo split case) showed `diacriticMs` spiking to
176-213ms, while correctly-typed words kept it near zero.

**Root-caused, not guessed - and the obvious first fix idea turned out to be wrong, caught by re-reading the
existing test suite before touching anything.** `diacriticRestoration()`'s `isKnownWord(token)` early exit
(unlike `bestCorrection()`, see D-220) already skips the search entirely for a correctly-typed word - exactly
why `diacriticMs` stayed near zero for `"Herzlichen"` etc. For an actual typo, the early exit does not fire, so
`store.diacriticCandidates()` runs its full, deliberately unbounded (D-197) bucket scan - and unlike
`correctionCandidates()` (which, per D-209, uncaps only the token's own literal first-character bucket, keeping
every keyboard-neighbour/umlaut-variant bucket capped), `diacriticCandidates()` uncapped *every* searched bucket
via a blanket `perBucketLimit = Int.MAX_VALUE`. `candidateFirstChars()` (D-38) returns the token's own char plus
its keyboard neighbours plus, for a token starting with a/o/u, the umlaut variant (ä/ö/ü) - for a common
initial letter like G or H, that is several full-size buckets scanned without any limit at all, not just one.

The first fix idea considered - cap the diacriticCandidates() neighbour buckets exactly like
correctionCandidates() already does - was disproven by the existing
`diacriticCandidatesAreNotBoundedByFrequencyUnlikeCorrectionCandidatesInANeighbourBucket` test, which
*deliberately* asserted the opposite. The reason: unlike a keyboard-neighbour typo (a genuinely rare edge case
for correctionCandidates()), the umlaut-variant bucket is the *expected*, ordinary path for diacritic
restoration itself - a user typing the ASCII-folded form of an umlaut word (e.g. "uber" for "über") has their
token classified under the plain letter while the real word lives under its own umlaut. Capping that bucket
would have silently reintroduced the exact "Grüße" bug D-197 exists to prevent, just reached via the
umlaut-variant path instead of the primary-bucket path.

**Fixed with the narrower distinction the naive version missed**: `correctionCandidatesInternal()`'s
`uncappedChar: Char?` parameter became `uncappedChars: Set<Char>`, so more than one bucket can be exempted from
the cap. `diacriticCandidates()` now derives *two* uncapped chars - the token's own literal first character
(same as `correctionCandidates()`) and its umlaut variant (mirroring `candidateFirstChars()`'s own a/o/u -> ä/ö/ü
mapping locally, rather than threading it through the `DictionaryStore` interface for three fixed character
pairs) - while ordinary keyboard-neighbour buckets stay capped, exactly like `correctionCandidates()`.

Test coverage updated to match the narrower guarantee: the old neighbour-bucket test (which asserted the
now-removed blanket-uncap behaviour) was replaced by
`diacriticCandidatesAreBoundedByFrequencyInAnOrdinaryKeyboardNeighbourBucket` (proving a genuine keyboard-
neighbour bucket, unrelated to any umlaut variant, is capped exactly like `correctionCandidates()`'s own
already-tested case) and a new `diacriticCandidatesAreNotBoundedByFrequencyInTheUmlautVariantBucket` (proving
the umlaut-variant bucket specifically stays uncapped, the actual D-197 correctness guarantee this whole
function exists for). 775 unit tests (774 - 1 replaced + 2 new). `:app:assembleDebug`/`:app:testDebugUnitTest`
green. Not yet device-confirmed - awaits the user's next repro of a common-initial-letter typo to show
`diacriticMs` dropping while an umlaut-restoration case (e.g. "uber" -> "über") still resolves correctly.
